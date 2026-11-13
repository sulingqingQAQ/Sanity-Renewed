package guivnf.sanity_renewed.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Pure description (no Minecraft types) of the 42 {@code [sanity.link]} keys of the Depression addon (2A + 2B) and the
 * checks that run when a config is loaded or reloaded. A bad value is rejected right there with the key, the value and
 * where it came from; nothing is repaired or replaced by a default.
 *
 * <ul>
 *   <li>{@link #checkValue}: type, finiteness and range of one value. Used for dimension overrides, which are stored raw
 *       (the default config already goes through {@code defineInRange}).</li>
 *   <li>{@link #checkCombination}: the rules that span several keys (strictly descending thresholds, monotone PTSD
 *       penalty table) and per-key bounds for one fully resolved set of values, i.e. the default config or one dimension.</li>
 * </ul>
 * The table below is the single list that the default definitions in {@code ConfigDefault}, the proxies in
 * {@code ConfigManager} and the getters in {@code ConfigProxy} are checked against.
 */
public final class LinkConfigSpec
{
    /** Path prefix of the keys in the TOML files and in the proxy map. */
    public static final String PREFIX = "sanity.link.";

    public enum Kind { BOOLEAN, DOUBLE, INT }

    /** {@code def} / {@code min} / {@code max} are the numeric default and bounds (BOOLEAN: def is 0 or 1). */
    public record Entry(String key, Kind kind, double def, double min, double max) {}

    /** Thrown for any rejected value; the message names the key, the value and the origin. */
    public static final class LinkConfigException extends IllegalArgumentException
    {
        private static final long serialVersionUID = 1L;

        public LinkConfigException(String message)
        {
            super(message);
        }
    }

    public static final List<Entry> ENTRIES = List.of(
            flag("link_enabled", true),
            flag("mood_recovery_enabled", true),
            number("mood_recovery_e1", 0.0, -20.0, 20.0),
            number("mood_recovery_e2", -5.0, -20.0, 20.0),
            number("mood_recovery_e3", -10.0, -20.0, 20.0),
            number("mood_recovery_m0", 1.0, 0.0, 1.0),
            number("mood_recovery_m1", 0.75, 0.0, 1.0),
            number("mood_recovery_m2", 0.5, 0.0, 1.0),
            number("mood_recovery_m3", 0.0, 0.0, 1.0),
            flag("sanity_mood_enabled", true),
            number("sanity_s1", 70.0, 0.0, 100.0),
            number("sanity_s2", 40.0, 0.0, 100.0),
            number("sanity_s3", 20.0, 0.0, 100.0),
            number("stabilize_m0", 1.0, 0.0, 1.0),
            number("stabilize_m1", 0.75, 0.0, 1.0),
            number("stabilize_m2", 0.5, 0.0, 1.0),
            number("stabilize_m3", 0.25, 0.0, 1.0),
            number("damage_m0", 1.0, 1.0, 3.0),
            number("damage_m1", 1.0, 1.0, 3.0),
            number("damage_m2", 1.15, 1.0, 3.0),
            number("damage_m3", 1.30, 1.0, 3.0),
            flag("ptsd_enabled", true),
            number("ptsd_penalty_1", 2.0, 0.0, 100.0),
            number("ptsd_penalty_2", 4.0, 0.0, 100.0),
            number("ptsd_penalty_3", 6.0, 0.0, 100.0),
            number("ptsd_penalty_4", 8.0, 0.0, 100.0),
            whole("ptsd_rearm_seconds", 10, 0, 300),
            flag("ptsd_log", true),
            flag("medicine_effect_enabled", true),
            number("medicine_sanity_tier_1", 0.05, 0.0, 10.0),
            number("medicine_sanity_tier_2", 0.10, 0.0, 10.0),
            number("medicine_sanity_tier_3", 0.15, 0.0, 10.0),
            flag("medicine_cap_by_illness", true),
            flag("medicine_apply_trait_multiplier", false),
            flag("overdose_penalty_enabled", true),
            number("overdose_penalty_1", 5.0, 0.0, 100.0),
            number("overdose_penalty_2", 10.0, 0.0, 100.0),
            number("overdose_penalty_3", 15.0, 0.0, 100.0),
            whole("overdose_pause_medicine_ticks", 200, 0, 12000),
            flag("sleep_restore_enabled", true),
            number("sleep_restore_amount", 25.0, 0.0, 100.0),
            flag("sleep_mania_blocks_restore", true));

    private static final Map<String, Entry> BY_KEY = new HashMap<>();

    static
    {
        for (Entry entry : ENTRIES)
            BY_KEY.put(entry.key(), entry);
    }

    private LinkConfigSpec() {}

    private static Entry flag(String key, boolean def)
    {
        return new Entry(key, Kind.BOOLEAN, def ? 1 : 0, 0, 1);
    }

    private static Entry number(String key, double def, double min, double max)
    {
        return new Entry(key, Kind.DOUBLE, def, min, max);
    }

    private static Entry whole(String key, int def, int min, int max)
    {
        return new Entry(key, Kind.INT, def, min, max);
    }

    /** True for a key name without the prefix, e.g. {@code "mood_recovery_e1"}. */
    public static boolean isKey(String key)
    {
        return BY_KEY.containsKey(key);
    }

    /**
     * Checks one raw value read from a config file.
     *
     * @param key    key name without the {@link #PREFIX}
     * @param origin where the value comes from, for the message (e.g. a dimension file name)
     */
    public static void checkValue(String key, Object value, String origin)
    {
        Entry entry = BY_KEY.get(key);
        if (entry == null)
            throw new LinkConfigException(describe(key, value, origin) + ": unknown key");

        switch (entry.kind())
        {
            case BOOLEAN ->
            {
                if (!(value instanceof Boolean))
                    throw new LinkConfigException(describe(key, value, origin) + ": must be true or false");
            }
            case DOUBLE ->
            {
                if (!(value instanceof Double number))
                    throw new LinkConfigException(describe(key, value, origin)
                            + ": must be a decimal number, write 1.0 and not 1");
                if (!Double.isFinite(number) || number < entry.min() || number > entry.max())
                    throw new LinkConfigException(describe(key, value, origin)
                            + ": must be a finite number between " + entry.min() + " and " + entry.max());
            }
            case INT ->
            {
                if (!(value instanceof Integer number))
                    throw new LinkConfigException(describe(key, value, origin) + ": must be a whole number");
                if (number < entry.min() || number > entry.max())
                    throw new LinkConfigException(describe(key, value, origin)
                            + ": must be a whole number between " + (int) entry.min() + " and " + (int) entry.max());
            }
        }
    }

    /**
     * Checks the rules that involve several keys, for one fully resolved set of values (every value already passed
     * {@link #checkValue} or {@code defineInRange}).
     *
     * @param values key name without the {@link #PREFIX} to its resolved value
     * @param origin the default config or the dimension this set belongs to, for the message
     */
    public static void checkCombination(Function<String, Object> values, String origin)
    {
        for (Entry entry : ENTRIES)
            checkValue(entry.key(), values.apply(entry.key()), origin);

        double e1 = number(values, "mood_recovery_e1"), e2 = number(values, "mood_recovery_e2"), e3 = number(values, "mood_recovery_e3");
        if (!(e1 > e2 && e2 > e3))
            throw new LinkConfigException(origin + ": mood_recovery_e1 > mood_recovery_e2 > mood_recovery_e3 must hold, got "
                    + e1 + " / " + e2 + " / " + e3);

        double s1 = number(values, "sanity_s1"), s2 = number(values, "sanity_s2"), s3 = number(values, "sanity_s3");
        if (!(s1 > s2 && s2 > s3))
            throw new LinkConfigException(origin + ": sanity_s1 > sanity_s2 > sanity_s3 must hold, got "
                    + s1 + " / " + s2 + " / " + s3);

        double p1 = number(values, "ptsd_penalty_1"), p2 = number(values, "ptsd_penalty_2");
        double p3 = number(values, "ptsd_penalty_3"), p4 = number(values, "ptsd_penalty_4");
        if (!(Double.isFinite(p1) && Double.isFinite(p2) && Double.isFinite(p3) && Double.isFinite(p4)
                && 0.0 <= p1 && p1 <= p2 && p2 <= p3 && p3 <= p4))
            throw new LinkConfigException(origin + ": 0 <= ptsd_penalty_1 <= ptsd_penalty_2 <= ptsd_penalty_3 <= ptsd_penalty_4 must hold"
                    + " (each is the TOTAL paid when that level is reached), got " + p1 + " / " + p2 + " / " + p3 + " / " + p4);
    }

    private static double number(Function<String, Object> values, String key)
    {
        return (Double) values.apply(key);
    }

    private static String describe(String key, Object value, String origin)
    {
        return origin + ": " + PREFIX + key + " = " + value;
    }
}
