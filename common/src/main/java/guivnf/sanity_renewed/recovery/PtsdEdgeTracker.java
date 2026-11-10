package guivnf.sanity_renewed.recovery;

/**
 * Pure logic (no Minecraft types) of the PTSD episode bookkeeping that turns Depression's PTSD level into
 * one-off sanity penalties. One instance per player ENTITY: a field of {@code Sanity}, server side, never
 * persisted, never copied on clone.
 *
 * <p>Accounting (decisions D1 / D2 of the 2A design):
 * <ul>
 *   <li><b>Difference accounting.</b> {@code penalty[k]} is the TOTAL nominal sanity paid when an episode has reached
 *       level k. Rising from {@code billed} to {@code level} costs {@code penalty[level] - penalty[billed]}, so
 *       0 to 4 in one step and 0 to 1 to 2 to 3 to 4 cost the same. The table is the one handed to each sample
 *       (current dimension / config), so after a config change the difference is taken in the new table.</li>
 *   <li><b>The ledger is only reset by a calm stretch.</b> {@code billed} is the highest level paid in the current
 *       episode. Dropping to a lower, non-zero level does not lower it; only {@code rearmSamples} CONSECUTIVE samples at
 *       level 0 do. So 4 to 2 to 4 pays once, and only a real recovery makes the next episode pay again.</li>
 * </ul>
 * The penalty table must be monotone ({@code 0 = p0 <= p1 <= ... <= p4}); that is checked when the config is loaded,
 * not here.
 */
public final class PtsdEdgeTracker
{
    /** Mirrors net.depression.mental.PTSDManager.PTSD_1_VALUE .. PTSD_4_VALUE (strict ">" in PTSDManager.tick); verified at startup. */
    public static final double LEVEL1 = 20d, LEVEL2 = 26d, LEVEL3 = 32d, LEVEL4 = 36d;
    /** Length of Depression's second-block in MentalStatus.tick (tickCount % 20 == 0). */
    public static final long WINDOW_TICKS = 20L;

    private static final long NO_WINDOW = Long.MIN_VALUE;

    private long lastWindow = NO_WINDOW;
    private boolean armed;     // false until the first sample was adopted as the baseline (join / respawn / re-enable)
    private int billed;        // highest level already paid for in the current episode, 0..4
    private int calmSamples;   // consecutive samples at level 0 since the last non-zero sample

    /** Same thresholds and the same strictness as PTSDManager.tick: > 20 / 26 / 32 / 36. NaN and <= 20 give 0. */
    public static int levelOf(double currentMax)
    {
        if (!(currentMax > LEVEL1)) return 0;
        if (currentMax > LEVEL4) return 4;
        if (currentMax > LEVEL3) return 3;
        if (currentMax > LEVEL2) return 2;
        return 1;
    }

    /**
     * True at most once per 20-tick window of Depression's MentalStatus.tickCount.
     * A counter below 0 means "MentalStatus has not run its first tick yet" (state loaded from NBT but the
     * first PTSDManager.tick has not computed currentMaxPTSDValue): ignored, so the window that carries
     * the persisted PTSD state is the one that becomes the baseline.
     * Uses inequality, not ">", so a replaced MentalStatus (counter restarts) is also handled.
     */
    public boolean acceptWindow(long mentalTickCount)
    {
        if (mentalTickCount < 0L) return false;
        long w = mentalTickCount / WINDOW_TICKS;
        if (w == lastWindow) return false;
        lastWindow = w;
        return true;
    }

    /**
     * Feed one window sample. Returns the nominal S points to take away right now (>= 0).
     *
     * @param level         result of {@link #levelOf(double)}
     * @param rearmSamples  consecutive level-0 samples (= seconds) after which the ledger is reset; 0 or 1 = at once
     * @param totalPenalty  penalty[0] = 0, penalty[k] = S points paid in total when an episode reaches level k
     *                      (design defaults 0, 2, 4, 6, 8), monotone
     */
    public float sample(int level, int rearmSamples, float[] totalPenalty)
    {
        if (!armed)
        {
            armed = true;
            billed = level;
            calmSamples = 0;
            return 0f;
        }
        if (level > billed)
        {
            float charge = totalPenalty[level] - totalPenalty[billed];
            billed = level;
            calmSamples = 0;
            return charge;
        }
        if (level > 0)
        {
            calmSamples = 0; // still (partly) active: a partial drop never lowers the ledger
            return 0f;
        }
        if (++calmSamples >= rearmSamples)
        {
            billed = 0;
            calmSamples = 0;
        }
        return 0f;
    }

    /** Forget everything: the next sample is adopted as the baseline again. */
    public void reset()
    {
        lastWindow = NO_WINDOW;
        armed = false;
        billed = 0;
        calmSamples = 0;
    }

    /** Highest level paid in the current episode (read-only, for diagnostics). */
    public int getBilledLevel()
    {
        return billed;
    }

    /** Consecutive level-0 samples counted towards the reset (read-only, for diagnostics). */
    public int getCalmSamples()
    {
        return calmSamples;
    }

    /** False until the first sample was adopted as the baseline (read-only, for diagnostics). */
    public boolean isArmed()
    {
        return armed;
    }
}
