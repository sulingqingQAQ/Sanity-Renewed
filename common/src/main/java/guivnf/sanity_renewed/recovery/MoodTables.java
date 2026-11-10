package guivnf.sanity_renewed.recovery;

/**
 * Pure functions (no Minecraft types) of the mood / sanity step tables and of the per-tick recovery sum.
 *
 * <p>Boundary convention (chosen to reproduce the design tables exactly):
 * <ul>
 *   <li>mood {@code e}: {@code e >= e1} gives tier 0, {@code e > e2} tier 1, {@code e > e3} tier 2, otherwise tier 3
 *       (defaults 0 / -5 / -10);</li>
 *   <li>sanity {@code S}: {@code S >= s1} gives tier 0, {@code >= s2} tier 1, {@code >= s3} tier 2, otherwise tier 3
 *       (defaults 70 / 40 / 20).</li>
 * </ul>
 * The thresholds of each table are validated as strictly descending when the config is loaded
 * (see {@code LinkConfigSpec}), these functions trust that.
 */
public final class MoodTables
{
    private MoodTables() {}

    /**
     * Float noise guard: S is stored as insanity {@code i = 1 - S/100} and read back as {@code 100 * (1 - i)};
     * {@code /sanity set 40} reads back as 39.999996. Without a tolerance an admin-typed boundary value lands one tier too low.
     */
    public static final float TIER_EPSILON = 1.0e-3f;

    /** Mood e to the multiplier of the BASE recovery only (never of the campfire). NaN (no Depression state) fails closed: 0. */
    public static float recoveryMultiplier(double e, double e1, double e2, double e3, float m0, float m1, float m2, float m3)
    {
        if (Double.isNaN(e)) return 0f;
        if (e >= e1) return m0;
        if (e > e2) return m1;
        if (e > e3) return m2;
        return m3;
    }

    /** Sanity S (points, 100 = clear, always finite on the server) to tier 0..3. */
    public static int sanityTier(float s, float s1, float s2, float s3)
    {
        float v = s + TIER_EPSILON;
        if (v >= s1) return 0;
        if (v >= s2) return 1;
        if (v >= s3) return 2;
        return 3;
    }

    /** The value of the given tier (0..3). */
    public static double pickByTier(int tier, double m0, double m1, double m2, double m3)
    {
        return switch (tier)
        {
            case 0 -> m0;
            case 1 -> m1;
            case 2 -> m2;
            default -> m3;
        };
    }

    /**
     * Sanity points gained in one tick. The mood multiplier belongs to the BASE recovery only,
     * the campfire term is independent of it (design point 1: a bad mood never switches the campfire off).
     *
     * @param baseAllowed  base recovery safety gate and light check passed
     * @param baseRate     base recovery, S per second
     * @param moodMul      result of {@link #recoveryMultiplier}, only used when {@code baseAllowed}
     * @param campfireNear a lit campfire is within reach
     * @param campfireRate campfire recovery, S per second
     */
    public static float gainPerTick(boolean baseAllowed, float baseRate, float moodMul, boolean campfireNear, float campfireRate)
    {
        float perSecond = 0f;
        if (baseAllowed)
            perSecond += baseRate * moodMul;
        if (campfireNear)
            perSecond += campfireRate;
        return perSecond / 20f;
    }
}
