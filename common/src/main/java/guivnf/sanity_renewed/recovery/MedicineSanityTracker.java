package guivnf.sanity_renewed.recovery;

import java.util.Objects;

/**
 * Pure logic (no Minecraft types) of the Phase 2B couplings:
 * <ul>
 *   <li><b>Medicine sanity restoration</b>: every actual {@code MobEffect.applyEffectTick} call for an active
 *       Depression medicine effect. For {@code mentalHealthId = 1..3}, the effective tier is
 *       {@code min(amplifier + 1, mentalHealthId)} when {@code medicine_cap_by_illness} is enabled; for bipolar
 *       ({@code mentalHealthId = 4}), both {@code ANTI_MANIA} and {@code ANTI_DEPRESSION (amplifier >= 2)} must be
 *       active simultaneously and restore one tier-1 step ({@code +0.05 S}) per 120 ticks.</li>
 *   <li><b>Overdose edge penalty and pause window</b>: each real overdose event in {@code MedicineItem.finishUsingItem}
 *       sets {@code lastOdTime = gameTime} and {@code odCount = 1..3}. Because {@code MedicineItem.getUseDuration} is
 *       32 ticks, two completions by the same player are at least 32 ticks apart and always produce a strictly newer
 *       {@code lastOdTime}, including saturated {@code 3 -> 3} overdoses. Normal dosing of another unactivated medicine
 *       resets {@code odCount = 0} without refunding past penalties or faking a new overdose. Following a real overdose,
 *       medicine-sourced sanity recovery is paused for {@code overdose_pause_medicine_ticks} (default 200 ticks, i.e.
 *       elapsed {@code 0..199} paused, {@code >= 200} resumed).</li>
 *   <li><b>Sleep sanity restoration gate</b>: checks {@code link_enabled}, {@code sleep_restore_enabled}, initialised
 *       Depression state, untreated insomnia ({@code !isSleepy && isInsomnia}), mania ({@code isMania} when
 *       {@code sleep_mania_blocks_restore} is enabled), {@code sleepAwarded}, {@code sleepTicks >= minSleepTicks} and
 *       night skip at both final award points.</li>
 * </ul>
 */
public final class MedicineSanityTracker
{
    public enum MedicineEffectType
    {
        ANTI_DEPRESSION,
        ANTI_MANIA
    }

    public record MedicineEffectTicks(
            boolean antiDepressionTicked,
            int antiDepressionAmplifier,
            boolean antiManiaTicked,
            int antiManiaAmplifier,
            int antiDepressionAmplifierAtAntiManiaTick)
    {
        public static final MedicineEffectTicks NONE = new MedicineEffectTicks(false, -1, false, -1, -1);
    }

    // Medicine diagnostic / evaluation status codes
    public static final int MED_ACTIVE_DEPRESSION = 0;
    public static final int MED_ACTIVE_BIPOLAR = 1;
    public static final int MED_DISABLED = 2;
    public static final int MED_NO_DEPRESSION_STATE = 3;
    public static final int MED_PAUSED_BY_OVERDOSE = 4;
    public static final int MED_INACTIVE_HEALTHY = 5;
    public static final int MED_INACTIVE_NO_EFFECT = 6;
    public static final int MED_INACTIVE_BIPOLAR_INCOMPLETE = 7;

    // Sleep diagnostic / gate block codes
    public static final int SLEEP_ELIGIBLE = 0;
    public static final int SLEEP_BLOCK_DISABLED = 1;
    public static final int SLEEP_BLOCK_NO_DEPRESSION_STATE = 2;
    public static final int SLEEP_BLOCK_INSOMNIA = 3;
    public static final int SLEEP_BLOCK_MANIA = 4;
    public static final int SLEEP_BLOCK_ALREADY_AWARDED = 5;
    public static final int SLEEP_BLOCK_NOT_ENOUGH_TICKS = 6;
    public static final int SLEEP_BLOCK_NIGHT_NOT_SKIPPED = 7;

    /**
     * Read-only evaluation of a player's current medicine restoration state and step gain per 120 ticks.
     *
     * @param status        one of {@code MED_*}
     * @param rawTier       unclamped medicine tier {@code min(amplifier + 1, 3)} for depression, {@code 1} for bipolar, {@code 0} when inactive
     * @param effectiveTier tier after applying {@code capByIllness} ({@code 1..3}, or {@code 0} when inactive)
     * @param capped        true when {@code capByIllness} reduced {@code effectiveTier} below {@code rawTier}
     * @param stepGain      sanity points (S) restored on each 120-tick effect trigger
     */
    public record MedicineEval(int status, int rawTier, int effectiveTier, boolean capped, float stepGain) {}

    private boolean baselineAdopted;
    private long joinGameTime = Long.MIN_VALUE;
    private Long lastHandledOdTime;
    private Long lastRealOdTime;
    private float lastMedicineGain;
    private float lastOverdosePenalty;
    private float lastSleepAward;

    private long medicineEffectGameTime = Long.MIN_VALUE;
    private boolean antiDepressionTicked;
    private int antiDepressionAmplifier = -1;
    private boolean antiManiaTicked;
    private int antiManiaAmplifier = -1;
    private int antiDepressionAmplifierAtAntiManiaTick = -1;

    /**
     * Remaining ticks of the medicine pause window after the latest real overdose at {@code lastOdTime}.
     * At {@code elapsed = 0..pauseWindowTicks - 1} (e.g. {@code 0..199} for a 200-tick window) returns
     * {@code pauseWindowTicks - elapsed} ({@code > 0}); at {@code elapsed >= pauseWindowTicks} (e.g. {@code 200})
     * returns {@code 0}.
     */
    public static int pauseRemainingTicks(Long lastOdTime, long currentGameTime, int pauseWindowTicks)
    {
        if (lastOdTime == null || pauseWindowTicks <= 0)
            return 0;
        long elapsed = currentGameTime - lastOdTime.longValue();
        if (elapsed < 0L || elapsed >= pauseWindowTicks)
            return 0;
        return (int) (pauseWindowTicks - elapsed);
    }

    /** True while medicine-sourced sanity recovery is paused by the overdose window. */
    public static boolean isMedicinePaused(Long lastOdTime, long currentGameTime, int pauseWindowTicks)
    {
        return pauseRemainingTicks(lastOdTime, currentGameTime, pauseWindowTicks) > 0;
    }

    /**
     * Evaluates the player's current medicine restoration state and per-120-tick step gain (read-only).
     */
    public static MedicineEval evaluateMedicine(
            boolean linkEnabled,
            boolean medicineEnabled,
            boolean pausedByOverdose,
            int mentalHealthId,
            double traitMultiplier,
            boolean hasAntiDepression,
            int antiDepressionAmplifier,
            boolean hasAntiMania,
            int antiManiaAmplifier,
            boolean capByIllness,
            boolean applyTraitMultiplier,
            float tier1,
            float tier2,
            float tier3)
    {
        if (!linkEnabled || !medicineEnabled)
            return new MedicineEval(MED_DISABLED, 0, 0, false, 0f);
        if (mentalHealthId < 0)
            return new MedicineEval(MED_NO_DEPRESSION_STATE, 0, 0, false, 0f);
        if (pausedByOverdose)
            return new MedicineEval(MED_PAUSED_BY_OVERDOSE, 0, 0, false, 0f);
        if (mentalHealthId == 0)
            return new MedicineEval(MED_INACTIVE_HEALTHY, 0, 0, false, 0f);

        double traitFactor = applyTraitMultiplier ? traitMultiplier : 1.0;

        if (mentalHealthId >= 1 && mentalHealthId <= 3)
        {
            if (!hasAntiDepression || antiDepressionAmplifier < 0)
                return new MedicineEval(MED_INACTIVE_NO_EFFECT, 0, 0, false, 0f);
            int rawTier = Math.min(antiDepressionAmplifier + 1, 3);
            int effectiveTier = capByIllness ? Math.min(rawTier, mentalHealthId) : rawTier;
            boolean capped = capByIllness && effectiveTier < rawTier;
            double base = pickTierGain(effectiveTier, tier1, tier2, tier3);
            float gain = (float) Math.max(0.0, base * traitFactor);
            return new MedicineEval(MED_ACTIVE_DEPRESSION, rawTier, effectiveTier, capped, gain);
        }

        if (mentalHealthId == 4)
        {
            if (!hasAntiMania || antiManiaAmplifier < 0 || !hasAntiDepression || antiDepressionAmplifier < 2)
            {
                int st = (hasAntiMania || hasAntiDepression) ? MED_INACTIVE_BIPOLAR_INCOMPLETE : MED_INACTIVE_NO_EFFECT;
                return new MedicineEval(st, 0, 0, false, 0f);
            }
            float gain = (float) Math.max(0.0, (double) tier1 * traitFactor);
            return new MedicineEval(MED_ACTIVE_BIPOLAR, 1, 1, false, gain);
        }

        return new MedicineEval(MED_INACTIVE_NO_EFFECT, 0, 0, false, 0f);
    }

    /**
     * Sanity points (S) gained on the current tick from active medicine effects.
     * Bipolar disorder ({@code MED_ACTIVE_BIPOLAR}) only triggers once per 120-tick cycle on {@code antiManiaTicked},
     * never twice even when both effects tick on the same frame.
     */
    public static float computeMedicineTickGain(MedicineEval eval, boolean antiDepressionTicked, boolean antiManiaTicked)
    {
        if (eval.status() == MED_ACTIVE_DEPRESSION && antiDepressionTicked)
            return eval.stepGain();
        if (eval.status() == MED_ACTIVE_BIPOLAR && antiManiaTicked)
            return eval.stepGain();
        return 0f;
    }

    /** Records an actual Depression medicine effect invocation for the current player world tick. */
    public void recordMedicineEffectTick(
            long gameTime,
            MedicineEffectType type,
            int amplifier,
            int antiDepressionAmplifierAtAntiManiaTick)
    {
        if (medicineEffectGameTime != gameTime)
        {
            clearMedicineEffectTicks();
            medicineEffectGameTime = gameTime;
        }

        switch (type)
        {
            case ANTI_DEPRESSION ->
            {
                antiDepressionTicked = true;
                antiDepressionAmplifier = amplifier;
            }
            case ANTI_MANIA ->
            {
                antiManiaTicked = true;
                antiManiaAmplifier = amplifier;
                this.antiDepressionAmplifierAtAntiManiaTick = antiDepressionAmplifierAtAntiManiaTick;
            }
        }
    }

    /** Consumes only effect invocations recorded in this exact world tick; stale events are discarded. */
    public MedicineEffectTicks consumeMedicineEffectTicks(long gameTime)
    {
        if (medicineEffectGameTime != gameTime)
        {
            clearMedicineEffectTicks();
            return MedicineEffectTicks.NONE;
        }

        MedicineEffectTicks ticks = new MedicineEffectTicks(
                antiDepressionTicked,
                antiDepressionAmplifier,
                antiManiaTicked,
                antiManiaAmplifier,
                antiDepressionAmplifierAtAntiManiaTick);
        clearMedicineEffectTicks();
        return ticks;
    }

    private void clearMedicineEffectTicks()
    {
        medicineEffectGameTime = Long.MIN_VALUE;
        antiDepressionTicked = false;
        antiDepressionAmplifier = -1;
        antiManiaTicked = false;
        antiManiaAmplifier = -1;
        antiDepressionAmplifierAtAntiManiaTick = -1;
    }

    private static double pickTierGain(int tier, float tier1, float tier2, float tier3)
    {
        return switch (tier)
        {
            case 1 -> tier1;
            case 2 -> tier2;
            default -> tier3;
        };
    }

    /** Nominal sanity penalty (S, before {@code negative_multiplier}) for the given {@code odCount}. */
    public static float penaltyForOdCount(int odCount, float penalty1, float penalty2, float penalty3)
    {
        if (odCount <= 0)
            return 0f;
        if (odCount == 1)
            return penalty1;
        if (odCount == 2)
            return penalty2;
        return penalty3;
    }

    /**
     * Evaluates whether a player can receive the sleep sanity reward right now (read-only).
     * Returns {@link #SLEEP_ELIGIBLE} ({@code 0}) when all gates pass, or a {@code SLEEP_BLOCK_*} code explaining the
     * first blocking reason.
     */
    public static int evaluateSleepBlock(
            boolean linkEnabled,
            boolean sleepRestoreEnabled,
            boolean hasDepressionState,
            boolean isInsomnia,
            boolean isSleepy,
            boolean isMania,
            boolean maniaBlocks,
            boolean alreadyAwarded,
            int sleepTicks,
            int minSleepTicks,
            boolean nightSkipped)
    {
        if (!linkEnabled || !sleepRestoreEnabled)
            return SLEEP_BLOCK_DISABLED;
        if (!hasDepressionState)
            return SLEEP_BLOCK_NO_DEPRESSION_STATE;
        if (!isSleepy && isInsomnia)
            return SLEEP_BLOCK_INSOMNIA;
        if (maniaBlocks && isMania)
            return SLEEP_BLOCK_MANIA;
        if (alreadyAwarded)
            return SLEEP_BLOCK_ALREADY_AWARDED;
        if (sleepTicks < minSleepTicks)
            return SLEEP_BLOCK_NOT_ENOUGH_TICKS;
        if (!nightSkipped)
            return SLEEP_BLOCK_NIGHT_NOT_SKIPPED;
        return SLEEP_ELIGIBLE;
    }

    /**
     * Marks that the player just joined (or reconnected) at {@code gameTime}. Any pre-existing {@code lastOdTime}
     * from before join is adopted as the baseline and never charged again.
     */
    public void onJoin(long gameTime, boolean hasDepressionState, Long existingLastOdTime, int existingOdCount)
    {
        clearMedicineEffectTicks();
        if (hasDepressionState)
        {
            baselineAdopted = true;
            joinGameTime = Long.MIN_VALUE;
            lastHandledOdTime = existingLastOdTime;
            lastRealOdTime = (existingOdCount > 0) ? existingLastOdTime : null;
        }
        else
        {
            baselineAdopted = false;
            joinGameTime = gameTime;
            lastHandledOdTime = null;
            lastRealOdTime = null;
        }
    }

    /**
     * Steps the overdose edge detector for one player tick. Returns the nominal sanity points (S) to deduct on this
     * tick ({@code >= 0}).
     */
    public float stepOverdose(
            boolean linkEnabled,
            boolean overdoseEnabled,
            boolean hasDepressionState,
            int odCount,
            Long currentLastOdTime,
            long currentGameTime,
            float penalty1,
            float penalty2,
            float penalty3)
    {
        if (!linkEnabled || !overdoseEnabled || !hasDepressionState)
        {
            baselineAdopted = false;
            lastHandledOdTime = null;
            return 0f;
        }

        if (currentLastOdTime == null)
        {
            baselineAdopted = true;
            joinGameTime = Long.MIN_VALUE;
            lastHandledOdTime = null;
            lastRealOdTime = null;
            return 0f;
        }

        if (!baselineAdopted)
        {
            baselineAdopted = true;
            long joinAt = joinGameTime;
            joinGameTime = Long.MIN_VALUE;
            boolean preExistingFromJoin = (joinAt != Long.MIN_VALUE && currentLastOdTime.longValue() <= joinAt)
                    || currentLastOdTime.longValue() != currentGameTime;
            if (odCount <= 0 || preExistingFromJoin || Objects.equals(currentLastOdTime, lastHandledOdTime))
            {
                lastHandledOdTime = currentLastOdTime;
                if (odCount > 0)
                    lastRealOdTime = currentLastOdTime;
                return 0f;
            }
        }

        if (odCount > 0 && !currentLastOdTime.equals(lastHandledOdTime))
        {
            lastHandledOdTime = currentLastOdTime;
            lastRealOdTime = currentLastOdTime;
            float penalty = penaltyForOdCount(odCount, penalty1, penalty2, penalty3);
            if (penalty > 0f)
                lastOverdosePenalty = penalty;
            return penalty;
        }

        return 0f;
    }

    /**
     * Latest real overdose timestamp for computing the 200-tick medicine pause window. In Depression,
     * {@code MentalIllness.lastOdTime} is only written in the overdose branch of {@code MedicineItem.finishUsingItem}
     * and remains set even if mixing another medicine resets {@code odCount = 0}.
     */
    public Long effectiveOverdoseTime(Long depressionLastOdTime)
    {
        return depressionLastOdTime != null ? depressionLastOdTime : lastRealOdTime;
    }

    public void recordMedicineGain(float gain)
    {
        if (gain > 0f)
            lastMedicineGain = gain;
    }

    public void recordSleepAward(float award)
    {
        if (award > 0f)
            lastSleepAward = award;
    }

    public void reset()
    {
        clearMedicineEffectTicks();
        baselineAdopted = false;
        joinGameTime = Long.MIN_VALUE;
        lastHandledOdTime = null;
        lastRealOdTime = null;
        lastMedicineGain = 0f;
        lastOverdosePenalty = 0f;
        lastSleepAward = 0f;
    }

    public boolean isBaselineAdopted()
    {
        return baselineAdopted;
    }

    public Long getLastHandledOdTime()
    {
        return lastHandledOdTime;
    }

    public float getLastMedicineGain()
    {
        return lastMedicineGain;
    }

    public float getLastOverdosePenalty()
    {
        return lastOverdosePenalty;
    }

    public float getLastSleepAward()
    {
        return lastSleepAward;
    }
}
