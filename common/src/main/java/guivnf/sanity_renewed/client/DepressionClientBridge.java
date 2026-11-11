package guivnf.sanity_renewed.client;

import guivnf.sanity_renewed.SanityMod;

import java.lang.reflect.Field;

/**
 * Client-side, read-only bridge to Depression's client state.
 *
 * <p>Presentation-layer boundary (phase 3): Depression owns the PTSD onset effects (tinnitus,
 * panting, its own heartbeat, screen vignette and the {@code falseEntities} visual hallucinations),
 * while this addon owns the brain HUD, inner monologue, blood tendrils, post chain, ambience drone
 * and the inner-entity rendering. The two must not play two heartbeats at once: when Depression has
 * an active PTSD onset, this addon's heartbeat loop yields.
 *
 * <p>Depression is a hard dependency of the addon, but this lookup is still reflection-based on
 * purpose: it keeps all client code in this package free of compile-time client-class coupling,
 * degrades to "not active" if the class layout changes, and mirrors the lazy probe style of the
 * existing {@code compat.SurvivalCompat}. It never writes anything and only reads a primitive int.
 */
public final class DepressionClientBridge
{
    private static final String CLIENT_PTSD_MANAGER = "net.depression.client.ClientPTSDManager";
    private static final String ONSET_FIELD = "onsetLevel";

    private static Field onsetField;
    private static boolean probed;
    private static boolean unavailableReported;

    private DepressionClientBridge() {}

    /**
     * @return Depression's current PTSD onset level (0 = no onset, 1..4 = active), or 0 when
     *         Depression's client class cannot be read. Never throws.
     */
    public static int getPtsdOnsetLevel()
    {
        Field field = probe();
        if (field == null)
            return 0;
        try
        {
            Object value = field.get(null);
            return value instanceof Number n ? n.intValue() : 0;
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            reportOnce(e);
            return 0;
        }
    }

    /** True while Depression is running any PTSD onset level (its own heartbeat/breathing is active). */
    public static boolean isPtsdOnsetActive()
    {
        return getPtsdOnsetLevel() > 0;
    }

    private static Field probe()
    {
        if (probed)
            return onsetField;
        probed = true;
        try
        {
            Class<?> manager = Class.forName(CLIENT_PTSD_MANAGER);
            Field f = manager.getField(ONSET_FIELD);
            f.setAccessible(true);
            onsetField = f;
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            reportOnce(e);
            onsetField = null;
        }
        return onsetField;
    }

    private static void reportOnce(Throwable e)
    {
        if (!unavailableReported)
        {
            unavailableReported = true;
            SanityMod.LOGGER.warn("[{}] Depression client PTSD state not readable ({}); heartbeat yield disabled",
                    SanityMod.MOD_ID, e.toString());
        }
    }
}
