package guivnf.sanity_renewed.recovery;

import guivnf.sanity_renewed.SanityProcessor;
import guivnf.sanity_renewed.capability.Sanity;
import guivnf.sanity_renewed.config.ConfigProxy;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side coordinator of the Phase 2B Depression couplings:
 * <ul>
 *   <li><b>Medicine sanity restoration</b>: restores sanity every 120 ticks of an active Depression medicine effect,
 *       capped by the player's current {@code mentalHealthId} at tick time when {@code medicine_cap_by_illness} is on,
 *       without multiplying by {@code MentalTrait.medicineEffectMultiplier} unless configured, and requiring both
 *       {@code ANTI_MANIA} and {@code ANTI_DEPRESSION (amplifier >= 2)} for bipolar disorder ({@code id = 4}).</li>
 *   <li><b>Overdose sanity penalty and 200-tick medicine pause window</b>: deducts {@code 5 / 10 / 15 S} (scaled by
 *       {@code negative_multiplier}) on each real overdose event (including saturated {@code 3 -> 3}) and pauses only
 *       medicine-sourced sanity restoration for {@code overdose_pause_medicine_ticks} (default 200 ticks).</li>
 *   <li><b>Sleep sanity restoration</b>: restores {@code +25 S} (scaled by {@code positive_multiplier}) once per
 *       completed night-skipping sleep, independent of {@code SanitySourceGate.LEGACY_ACTIVE_SOURCES} and blocked by
 *       untreated insomnia or mania at both final award points.</li>
 * </ul>
 *
 * <p>Per-player state is keyed by {@link UUID} only (never holding strong {@link ServerPlayer} references) and is
 * cleaned up on player quit and server stop.
 */
public final class MedicineSanityCoupling
{
    private static final Map<UUID, MedicineSanityTracker> TRACKERS = new ConcurrentHashMap<>();

    private MedicineSanityCoupling() {}

    /** Records one actual Depression medicine effect invocation for consumption at this player's POST tick. */
    public static void recordMedicineEffectTick(
            ServerPlayer player,
            MedicineSanityTracker.MedicineEffectType type,
            int amplifier,
            int antiDepressionAmplifierAtAntiManiaTick)
    {
        MedicineSanityTracker tracker = TRACKERS.computeIfAbsent(player.getUUID(), ignored -> new MedicineSanityTracker());
        tracker.recordMedicineEffectTick(
                player.level().getGameTime(), type, amplifier, antiDepressionAmplifierAtAntiManiaTick);
    }

    /**
     * Runs once per server player tick (after {@link SanityRecovery#tick} and {@link PtsdSanityPenalty#tick}) for an
     * alive, non-creative, non-spectator player.
     */
    public static void tick(ServerPlayer player, Sanity sanity)
    {
        if (player == null || sanity == null)
            return;

        ResourceLocation dim = player.level().dimension().location();
        long gameTime = player.level().getGameTime();
        DepressionLink.Snapshot snap = DepressionLink.readSnapshot(player);
        MedicineSanityTracker tracker = TRACKERS.computeIfAbsent(player.getUUID(), k -> new MedicineSanityTracker());
        MedicineSanityTracker.MedicineEffectTicks medicineTicks = tracker.consumeMedicineEffectTicks(gameTime);

        Long currentLastOdTime = snap.lastOdTime() == Long.MIN_VALUE ? null : Long.valueOf(snap.lastOdTime());
        boolean linkEnabled = ConfigProxy.getLinkEnabled(dim);

        float odNominal = tracker.stepOverdose(
                linkEnabled,
                ConfigProxy.getOverdosePenaltyEnabled(dim),
                snap.initialized(),
                snap.odCount(),
                currentLastOdTime,
                gameTime,
                ConfigProxy.getOverdosePenalty1(dim),
                ConfigProxy.getOverdosePenalty2(dim),
                ConfigProxy.getOverdosePenalty3(dim));
        if (odNominal > 0f)
        {
            double points = Math.min((double) odNominal * ConfigProxy.getNegMul(dim), 100d);
            sanity.setSanity(sanity.getSanity() + (float) (points / 100d));
        }

        Long effectiveOdTime = tracker.effectiveOverdoseTime(currentLastOdTime);
        int pauseWindow = ConfigProxy.getOverdosePauseMedicineTicks(dim);
        boolean pausedByOd = MedicineSanityTracker.isMedicinePaused(effectiveOdTime, gameTime, pauseWindow);

        int mentalHealthId = snap.initialized() ? snap.mentalHealthId() : -1;
        boolean bipolar = mentalHealthId == 4;
        boolean hasAntiDepressionForEval = bipolar
                ? medicineTicks.antiManiaTicked() && medicineTicks.antiDepressionAmplifierAtAntiManiaTick() >= 0
                : medicineTicks.antiDepressionTicked();
        int antiDepressionAmplifierForEval = bipolar
                ? medicineTicks.antiDepressionAmplifierAtAntiManiaTick()
                : medicineTicks.antiDepressionAmplifier();
        MedicineSanityTracker.MedicineEval eval = MedicineSanityTracker.evaluateMedicine(
                linkEnabled,
                ConfigProxy.getMedicineEffectEnabled(dim),
                pausedByOd,
                mentalHealthId,
                snap.traitMedicineMultiplier(),
                hasAntiDepressionForEval,
                antiDepressionAmplifierForEval,
                medicineTicks.antiManiaTicked(),
                medicineTicks.antiManiaAmplifier(),
                ConfigProxy.getMedicineCapByIllness(dim),
                ConfigProxy.getMedicineApplyTraitMultiplier(dim),
                ConfigProxy.getMedicineSanityTier1(dim),
                ConfigProxy.getMedicineSanityTier2(dim),
                ConfigProxy.getMedicineSanityTier3(dim));

        float medGain = MedicineSanityTracker.computeMedicineTickGain(
                eval, medicineTicks.antiDepressionTicked(), medicineTicks.antiManiaTicked());
        if (medGain > 0f)
        {
            tracker.recordMedicineGain(medGain);
            sanity.setSanity(sanity.getSanity() - medGain / 100f);
        }
    }

    /**
     * Checks all 2B sleep gates at the final award point and, when eligible, awards {@code sleep_restore_amount}
     * (scaled by {@code positive_multiplier}) once and marks {@code sleepAwarded = true}.
     */
    public static boolean tryAwardSleep(ServerPlayer player, Sanity sanity, boolean nightSkipped)
    {
        if (player == null || sanity == null)
            return false;

        ResourceLocation dim = player.level().dimension().location();
        DepressionLink.Snapshot snap = DepressionLink.readSnapshot(player);
        int block = MedicineSanityTracker.evaluateSleepBlock(
                ConfigProxy.getLinkEnabled(dim),
                ConfigProxy.getSleepRestoreEnabled(dim),
                snap.initialized(),
                Boolean.TRUE.equals(snap.isInsomnia()),
                snap.hasSleepiness(),
                snap.isMania(),
                ConfigProxy.getSleepManiaBlocksRestore(dim),
                sanity.isSleepAwarded(),
                sanity.getSleepTicks(),
                SanityProcessor.MIN_SLEEP_TICKS,
                nightSkipped);
        if (block != MedicineSanityTracker.SLEEP_ELIGIBLE)
        {
            if (nightSkipped && (block == MedicineSanityTracker.SLEEP_BLOCK_INSOMNIA
                    || block == MedicineSanityTracker.SLEEP_BLOCK_MANIA))
            {
                // A rejected completed sleep cannot become eligible later when the illness flags change.
                sanity.setSleepTicks(0);
                sanity.setSleepGrace(0);
            }
            return false;
        }

        sanity.setSleepAwarded(true);
        float nominal = ConfigProxy.getSleepRestoreAmount(dim);
        if (nominal > 0f)
        {
            double points = Math.min((double) nominal * ConfigProxy.getPosMul(dim), 100d);
            sanity.setSanity(sanity.getSanity() - (float) (points / 100d));
            MedicineSanityTracker tracker = TRACKERS.get(player.getUUID());
            if (tracker != null)
                tracker.recordSleepAward((float) points);
        }
        return true;
    }

    /** Called on {@code PlayerEvent.PLAYER_JOIN} so pre-existing NBT {@code lastOdTime} is adopted as baseline. */
    public static void onPlayerJoin(ServerPlayer player)
    {
        if (player == null)
            return;
        DepressionLink.Snapshot snap = DepressionLink.readSnapshot(player);
        Long existingOd = snap.lastOdTime() == Long.MIN_VALUE ? null : Long.valueOf(snap.lastOdTime());
        MedicineSanityTracker tracker = TRACKERS.computeIfAbsent(player.getUUID(), k -> new MedicineSanityTracker());
        tracker.onJoin(player.level().getGameTime(), snap.initialized(), existingOd, snap.odCount());
    }

    /** Releases the UUID-keyed tracker when a player leaves the server. */
    public static void onPlayerQuit(ServerPlayer player)
    {
        if (player != null)
            TRACKERS.remove(player.getUUID());
    }

    /** Clears all tracked player entries on server stop. */
    public static void clearAll()
    {
        TRACKERS.clear();
    }

    /** Number of currently tracked player UUIDs (for diagnostics and offline leak verification). */
    public static int trackedPlayerCount()
    {
        return TRACKERS.size();
    }

    /** Read-only lookup of a player's tracker without creating or mutating an entry. */
    public static MedicineSanityTracker getTrackerReadOnly(UUID uuid)
    {
        return uuid == null ? null : TRACKERS.get(uuid);
    }

    /** Read-only diagnostic summary of a player's 2B medicine state for {@code /sanity link}. */
    public record MedicineDiag(
            Component stateText,
            Component illnessText,
            String stepText,
            Component capText,
            String lastGainText) {}

    /** Read-only diagnostic summary of a player's 2B overdose state for {@code /sanity link}. */
    public record OverdoseDiag(
            Component odCountText,
            Component sinceLastOdText,
            String pauseRemainingText,
            String lastPenaltyText) {}

    /** Read-only diagnostic summary of a player's 2B sleep state for {@code /sanity link}. */
    public record SleepDiag(
            Component statusText,
            String amountText,
            String sleepTicksText,
            String minSleepTicksText,
            Component awardedText,
            String lastAwardText) {}

    /** Computes the read-only medicine diagnostic fields for {@code /sanity link} without mutating any state. */
    public static MedicineDiag describeMedicineReadOnly(ServerPlayer player)
    {
        ResourceLocation dim = player.level().dimension().location();
        long gameTime = player.level().getGameTime();
        DepressionLink.Snapshot snap = DepressionLink.readSnapshot(player);
        MedicineSanityTracker tracker = TRACKERS.get(player.getUUID());

        Long currentLastOdTime = snap.lastOdTime() == Long.MIN_VALUE ? null : Long.valueOf(snap.lastOdTime());
        Long effectiveOdTime = tracker != null ? tracker.effectiveOverdoseTime(currentLastOdTime) : currentLastOdTime;
        int pauseRem = MedicineSanityTracker.pauseRemainingTicks(
                effectiveOdTime, gameTime, ConfigProxy.getOverdosePauseMedicineTicks(dim));

        int mentalHealthId = snap.initialized() ? snap.mentalHealthId() : -1;
        MedicineSanityTracker.MedicineEval eval = MedicineSanityTracker.evaluateMedicine(
                ConfigProxy.getLinkEnabled(dim),
                ConfigProxy.getMedicineEffectEnabled(dim),
                pauseRem > 0,
                mentalHealthId,
                snap.traitMedicineMultiplier(),
                snap.hasAntiDepression(),
                snap.antiDepressionAmplifier(),
                snap.hasAntiMania(),
                snap.antiManiaAmplifier(),
                ConfigProxy.getMedicineCapByIllness(dim),
                ConfigProxy.getMedicineApplyTraitMultiplier(dim),
                ConfigProxy.getMedicineSanityTier1(dim),
                ConfigProxy.getMedicineSanityTier2(dim),
                ConfigProxy.getMedicineSanityTier3(dim));

        Component stateText = switch (eval.status())
        {
            case MedicineSanityTracker.MED_ACTIVE_DEPRESSION -> Component.translatable("commands.sanity.link.medicine.status.active_depression");
            case MedicineSanityTracker.MED_ACTIVE_BIPOLAR -> Component.translatable("commands.sanity.link.medicine.status.active_bipolar");
            case MedicineSanityTracker.MED_DISABLED -> Component.translatable("commands.sanity.link.medicine.status.disabled");
            case MedicineSanityTracker.MED_NO_DEPRESSION_STATE -> Component.translatable("commands.sanity.link.medicine.status.no_state");
            case MedicineSanityTracker.MED_PAUSED_BY_OVERDOSE -> Component.translatable("commands.sanity.link.medicine.status.paused", pauseRem);
            case MedicineSanityTracker.MED_INACTIVE_HEALTHY -> Component.translatable("commands.sanity.link.medicine.status.inactive_healthy");
            case MedicineSanityTracker.MED_INACTIVE_BIPOLAR_INCOMPLETE -> Component.translatable("commands.sanity.link.medicine.status.inactive_bipolar_incomplete");
            case MedicineSanityTracker.MED_INACTIVE_NO_EFFECT -> Component.translatable("commands.sanity.link.medicine.status.inactive_no_effect");
            default -> throw new IllegalStateException("Unexpected medicine status: " + eval.status());
        };
        Component illnessText = snap.initialized()
                ? Component.literal(String.valueOf(snap.mentalHealthId()))
                : Component.translatable("commands.sanity.link.value.unavailable");
        String stepText = String.format("%.2f", eval.stepGain());
        Component capText;
        if (eval.status() == MedicineSanityTracker.MED_ACTIVE_DEPRESSION)
        {
            capText = eval.capped()
                    ? Component.translatable("commands.sanity.link.medicine.cap.capped", eval.rawTier(), eval.effectiveTier())
                    : Component.translatable("commands.sanity.link.medicine.cap.uncapped", eval.effectiveTier());
        }
        else if (eval.status() == MedicineSanityTracker.MED_ACTIVE_BIPOLAR)
        {
            capText = Component.translatable("commands.sanity.link.medicine.cap.bipolar_fixed");
        }
        else
        {
            capText = Component.translatable("commands.sanity.link.value.unavailable");
        }
        float lastGain = tracker == null ? 0f : tracker.getLastMedicineGain();
        return new MedicineDiag(stateText, illnessText, stepText, capText, String.format("%.2f", lastGain));
    }

    /** Computes the read-only overdose diagnostic fields for {@code /sanity link} without mutating any state. */
    public static OverdoseDiag describeOverdoseReadOnly(ServerPlayer player)
    {
        ResourceLocation dim = player.level().dimension().location();
        long gameTime = player.level().getGameTime();
        DepressionLink.Snapshot snap = DepressionLink.readSnapshot(player);
        MedicineSanityTracker tracker = TRACKERS.get(player.getUUID());

        Component odCountText = snap.initialized()
                ? Component.literal(String.valueOf(snap.odCount()))
                : Component.translatable("commands.sanity.link.value.unavailable");
        Long currentLastOdTime = snap.lastOdTime() == Long.MIN_VALUE ? null : Long.valueOf(snap.lastOdTime());
        Long effectiveOdTime = tracker != null ? tracker.effectiveOverdoseTime(currentLastOdTime) : currentLastOdTime;

        Component sinceText = effectiveOdTime == null
                ? Component.translatable("commands.sanity.link.overdose.never")
                : Component.translatable("commands.sanity.link.elapsed_ticks", gameTime - effectiveOdTime.longValue());
        int pauseRem = MedicineSanityTracker.pauseRemainingTicks(
                effectiveOdTime, gameTime, ConfigProxy.getOverdosePauseMedicineTicks(dim));
        float lastPenalty = tracker == null ? 0f : tracker.getLastOverdosePenalty();
        return new OverdoseDiag(
                odCountText,
                sinceText,
                String.valueOf(pauseRem),
                String.format("%.2f", lastPenalty));
    }

    /** Computes the read-only sleep diagnostic fields for {@code /sanity link} without mutating any state. */
    public static SleepDiag describeSleepReadOnly(ServerPlayer player, Sanity sanity)
    {
        ResourceLocation dim = player.level().dimension().location();
        DepressionLink.Snapshot snap = DepressionLink.readSnapshot(player);
        MedicineSanityTracker tracker = TRACKERS.get(player.getUUID());

        boolean linkEnabled = ConfigProxy.getLinkEnabled(dim);
        boolean sleepEnabled = ConfigProxy.getSleepRestoreEnabled(dim);
        boolean maniaBlocks = ConfigProxy.getSleepManiaBlocksRestore(dim);

        boolean nightSkipped = player.level().getDayTime() - sanity.getSleepStartTime()
                > sanity.getSleepTicks() + SanityProcessor.SLEEP_SKIP_MARGIN;
        int sleepBlock = MedicineSanityTracker.evaluateSleepBlock(
                linkEnabled,
                sleepEnabled,
                snap.initialized(),
                Boolean.TRUE.equals(snap.isInsomnia()),
                snap.hasSleepiness(),
                snap.isMania(),
                maniaBlocks,
                sanity.isSleepAwarded(),
                sanity.getSleepTicks(),
                SanityProcessor.MIN_SLEEP_TICKS,
                nightSkipped);
        Component statusText = switch (sleepBlock)
        {
            case MedicineSanityTracker.SLEEP_ELIGIBLE -> Component.translatable("commands.sanity.link.sleep.status.eligible");
            case MedicineSanityTracker.SLEEP_BLOCK_DISABLED -> Component.translatable("commands.sanity.link.sleep.status.disabled");
            case MedicineSanityTracker.SLEEP_BLOCK_NO_DEPRESSION_STATE -> Component.translatable("commands.sanity.link.sleep.status.no_state");
            case MedicineSanityTracker.SLEEP_BLOCK_INSOMNIA -> Component.translatable("commands.sanity.link.sleep.status.insomnia");
            case MedicineSanityTracker.SLEEP_BLOCK_MANIA -> Component.translatable("commands.sanity.link.sleep.status.mania");
            case MedicineSanityTracker.SLEEP_BLOCK_ALREADY_AWARDED -> Component.translatable("commands.sanity.link.sleep.status.already_awarded");
            case MedicineSanityTracker.SLEEP_BLOCK_NOT_ENOUGH_TICKS -> Component.translatable(
                    "commands.sanity.link.sleep.status.not_enough_ticks", sanity.getSleepTicks(), SanityProcessor.MIN_SLEEP_TICKS);
            case MedicineSanityTracker.SLEEP_BLOCK_NIGHT_NOT_SKIPPED -> Component.translatable("commands.sanity.link.sleep.status.night_not_skipped");
            default -> throw new IllegalStateException("Unexpected sleep block code: " + sleepBlock);
        };

        double effectiveAmount = Math.min((double) ConfigProxy.getSleepRestoreAmount(dim) * ConfigProxy.getPosMul(dim), 100d);
        float lastAward = tracker == null ? 0f : tracker.getLastSleepAward();
        return new SleepDiag(
                statusText,
                String.format("%.2f", effectiveAmount),
                String.valueOf(sanity.getSleepTicks()),
                String.valueOf(SanityProcessor.MIN_SLEEP_TICKS),
                Component.translatable(sanity.isSleepAwarded()
                        ? "commands.sanity.link.boolean.yes"
                        : "commands.sanity.link.boolean.no"),
                String.format("%.2f", lastAward));
    }
}
