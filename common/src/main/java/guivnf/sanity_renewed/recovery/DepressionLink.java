package guivnf.sanity_renewed.recovery;

import guivnf.sanity_renewed.SanityMod;
import net.depression.mental.MentalStatus;
import net.depression.server.Registry;
import net.minecraft.server.level.ServerPlayer;

/**
 * The only place of the addon that reads Depression classes on the server (hard dependency); the multiplier hook that
 * Depression calls into lives in {@link DepressionModifierBridge}, which is only touched through {@link #installModifiers()}.
 *
 * <p>Read-only: it looks at a few state flags and values and never writes Depression's mood {@code e} / mental health
 * {@code h}, and never calls {@code mentalHeal} / {@code mentalHurt}. It reads {@code e} (base recovery multiplier),
 * the window counter {@code tickCount} and the PTSD maximum (PTSD penalty), and never reads {@code h}.
 *
 * <p>Two different kinds of "not there": a missing Depression state (the player has no {@link MentalStatus} yet or has
 * not chosen a mental trait) is a normal moment of the player's life cycle and is answered with a "not available"
 * value; a Depression build that lacks the API this addon needs is a setup error and stops the game at start-up
 * ({@link #installModifiers()}), it is not worked around while playing.
 */
public final class DepressionLink
{
    /** What this addon was written against; checked once at start-up. */
    private static final int MODIFIERS_API = 1;

    private static boolean apiErrorReported;
    private static boolean modifiersRegistered;

    private DepressionLink() {}

    /**
     * Server-side safety gate of the base recovery (everything except light, which the caller checks).
     * <ul>
     *   <li>Depression state exists and is initialised (mental trait chosen): a missing state means no recovery;</li>
     *   <li>not in combat ({@code combatCountdown <= 0}) and not in a Depression player event;</li>
     *   <li>no PTSD contact countdown running ({@code PTSDManager.hasRemaining() == false}).</li>
     * </ul>
     */
    public static boolean isCalmForBaseRecovery(ServerPlayer player)
    {
        try
        {
            MentalStatus status = Registry.mentalStatus.get(player.getUUID());
            if (status == null || status.mentalTrait == null)
                return false;
            if (status.combatCountdown > 0 || Registry.playerEventMap.containsKey(player.getUUID()))
                return false;
            return !status.ptsdManager.hasRemaining();
        }
        catch (LinkageError e)
        {
            if (!apiErrorReported)
            {
                apiErrorReported = true;
                SanityMod.LOGGER.error("[{}] Depression API does not match what this addon was built against; base sanity recovery is disabled", SanityMod.MOD_ID, e);
            }
            return false;
        }
    }

    /** Depression's mood e (-20..20), or NaN while the player has no initialised Depression state. */
    public static double readEmotion(ServerPlayer player)
    {
        MentalStatus status = Registry.mentalStatus.get(player.getUUID());
        if (status == null || status.mentalTrait == null)
            return Double.NaN;
        return status.emotionValue;
    }

    /**
     * Depression's tick counter of this player: -1 while there is no initialised state or it has not run its first tick
     * yet, otherwise the number of ticks it has run. The PTSD maximum is only meaningful from the first tick on.
     */
    public static long readMentalTickCount(ServerPlayer player)
    {
        MentalStatus status = Registry.mentalStatus.get(player.getUUID());
        if (status == null || status.mentalTrait == null)
            return -1L;
        return status.tickCount;
    }

    /** {@code PTSDManager.getCurrentMaxPTSDValue()} of a player whose {@link #readMentalTickCount} is not negative. */
    public static double readCurrentMaxPtsd(ServerPlayer player)
    {
        return Registry.mentalStatus.get(player.getUUID()).ptsdManager.getCurrentMaxPTSDValue();
    }

    /**
     * Start-up, called once from {@code SanityMod.init}: checks that the Depression API this addon reads is the expected
     * one (the game stops with a clear message otherwise), then registers the multiplier hook when this Depression build
     * has one. An older build without {@code MentalModifiers} is a supported setup: it logs one line and sanity then does
     * not touch mood; the other 2A links work.
     */
    public static void installModifiers()
    {
        verifyDepressionApi();

        try
        {
            Class.forName("net.depression.mental.MentalModifiers", false, DepressionLink.class.getClassLoader());
        }
        catch (ClassNotFoundException e)
        {
            SanityMod.LOGGER.warn("[{}] Depression modifiers unavailable: this Depression build has no MentalModifiers, sanity will not affect mood", SanityMod.MOD_ID);
            return;
        }

        int api = DepressionModifierBridge.install(MODIFIERS_API);
        modifiersRegistered = true;
        SanityMod.LOGGER.info("[{}] Depression modifiers registered (api {})", SanityMod.MOD_ID, api);
    }

    /** Whether Depression's multiplier hook is connected (false with a Depression build that has none). */
    public static boolean isModifiersRegistered()
    {
        return modifiersRegistered;
    }

    /**
     * Reflection on purpose: {@code PTSD_n_VALUE} are compile-time constants that would be baked into this class, so
     * a later change in Depression would go unnoticed. Class initialisation of Depression's classes is not forced
     * except for the constants' holder (a plain holder of statics).
     */
    private static void verifyDepressionApi()
    {
        ClassLoader loader = DepressionLink.class.getClassLoader();
        try
        {
            Class<?> ptsd = Class.forName("net.depression.mental.PTSDManager", false, loader);
            ptsd.getMethod("getCurrentMaxPTSDValue");
            requireConstant(ptsd, "PTSD_1_VALUE", PtsdEdgeTracker.LEVEL1);
            requireConstant(ptsd, "PTSD_2_VALUE", PtsdEdgeTracker.LEVEL2);
            requireConstant(ptsd, "PTSD_3_VALUE", PtsdEdgeTracker.LEVEL3);
            requireConstant(ptsd, "PTSD_4_VALUE", PtsdEdgeTracker.LEVEL4);

            Class<?> status = Class.forName("net.depression.mental.MentalStatus", false, loader);
            status.getField("emotionValue");
            status.getField("tickCount");
            status.getField("ptsdManager");
            status.getField("mentalTrait");
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Sanity: Renewed needs a Depression build with the PTSD level API (PTSDManager.getCurrentMaxPTSDValue, "
                    + "PTSD_n_VALUE) and the MentalStatus fields emotionValue / tickCount / ptsdManager / mentalTrait; this one does not have them", e);
        }
    }

    private static void requireConstant(Class<?> holder, String name, double expected) throws ReflectiveOperationException
    {
        double actual = holder.getField(name).getDouble(null);
        if (actual != expected)
            throw new IllegalStateException("Sanity: Renewed expects Depression's " + name + " to be " + expected + " but it is " + actual
                    + "; the PTSD level thresholds of this addon must be updated");
    }
}
