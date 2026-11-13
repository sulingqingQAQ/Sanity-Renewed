package guivnf.sanity_renewed.recovery;

import guivnf.sanity_renewed.SanityMod;
import net.depression.mental.MentalStatus;
import net.depression.server.Registry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * The only place of the addon that reads Depression classes on the server (hard dependency); the multiplier hook that
 * Depression calls into lives in {@link DepressionModifierBridge}, which is only touched through {@link #installModifiers()}.
 *
 * <p>Read-only: it looks at a few state flags and values and never writes Depression's mood {@code e} / mental health
 * {@code h}, and never calls {@code mentalHeal} / {@code mentalHurt}. It reads {@code e} (base recovery multiplier),
 * the window counter {@code tickCount} and the PTSD maximum (PTSD penalty), and the 2B medicine / overdose / sleep
 * state ({@code mentalHealthId}, {@code odCount}, {@code lastOdTime}, {@code mentalIllness.isMania},
 * {@code mentalIllness.isInsomnia}, {@code mentalTrait.medicineEffectMultiplier}), and never reads or writes {@code h}.
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

    public static final ResourceLocation ANTI_DEPRESSION_ID = ResourceLocation.fromNamespaceAndPath("depression", "anti_depression");
    public static final ResourceLocation ANTI_MANIA_ID = ResourceLocation.fromNamespaceAndPath("depression", "anti_mania");
    public static final ResourceLocation SLEEPINESS_ID = ResourceLocation.fromNamespaceAndPath("depression", "sleepiness");

    private static boolean apiErrorReported;
    private static boolean modifiersRegistered;

    /**
     * Read-only snapshot of a player's 2B Depression state and medicine effects on the current tick.
     * {@code lastOdTime} is {@link Long#MIN_VALUE} when Depression's {@code MentalStatus.lastOdTime} is {@code null}.
     */
    public record Snapshot(
            boolean initialized,
            int mentalHealthId,
            double traitMedicineMultiplier,
            int odCount,
            long lastOdTime,
            boolean isMania,
            Boolean isInsomnia,
            boolean hasAntiDepression,
            int antiDepressionAmplifier,
            int antiDepressionDuration,
            boolean hasAntiMania,
            int antiManiaAmplifier,
            int antiManiaDuration,
            boolean hasSleepiness)
    {
        public static final Snapshot UNINITIALIZED = new Snapshot(
                false, 0, 1.0, 0, Long.MIN_VALUE, false, null,
                false, -1, 0, false, -1, 0, false);
    }

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
     * Reads a read-only 2B snapshot of the player's Depression illness, overdose state and active medicine effects.
     * Never writes any Depression field or calls {@code mentalHeal} / {@code mentalHurt}.
     */
    public static Snapshot readSnapshot(ServerPlayer player)
    {
        if (player == null)
            return Snapshot.UNINITIALIZED;
        try
        {
            MentalStatus status = Registry.mentalStatus.get(player.getUUID());
            if (status == null || status.mentalTrait == null || status.mentalIllness == null)
                return Snapshot.UNINITIALIZED;

            double traitMul = status.mentalTrait.medicineEffectMultiplier;
            long lastOd = status.mentalIllness.lastOdTime == null ? Long.MIN_VALUE : status.mentalIllness.lastOdTime;
            boolean isMania = status.mentalIllness.isMania;
            Boolean isInsomnia = status.mentalIllness.isInsomnia;

            boolean hasAntiDep = false;
            int antiDepAmp = -1;
            int antiDepDur = 0;
            boolean hasAntiMania = false;
            int antiManiaAmp = -1;
            int antiManiaDur = 0;
            boolean hasSleepiness = false;

            for (MobEffectInstance inst : player.getActiveEffects())
            {
                if (inst == null || inst.getDuration() <= 0)
                    continue;
                Holder<MobEffect> holder = inst.getEffect();
                if (holder == null)
                    continue;
                if (holder.is(ANTI_DEPRESSION_ID))
                {
                    hasAntiDep = true;
                    antiDepAmp = inst.getAmplifier();
                    antiDepDur = inst.getDuration();
                }
                else if (holder.is(ANTI_MANIA_ID))
                {
                    hasAntiMania = true;
                    antiManiaAmp = inst.getAmplifier();
                    antiManiaDur = inst.getDuration();
                }
                else if (holder.is(SLEEPINESS_ID))
                {
                    hasSleepiness = true;
                }
            }

            return new Snapshot(
                    true,
                    status.mentalIllness.mentalHealthId,
                    traitMul,
                    status.mentalIllness.odCount,
                    lastOd,
                    isMania,
                    isInsomnia,
                    hasAntiDep,
                    antiDepAmp,
                    antiDepDur,
                    hasAntiMania,
                    antiManiaAmp,
                    antiManiaDur,
                    hasSleepiness);
        }
        catch (LinkageError e)
        {
            if (!apiErrorReported)
            {
                apiErrorReported = true;
                SanityMod.LOGGER.error("[{}] Depression API does not match what this addon was built against; 2B medicine/sleep coupling is disabled", SanityMod.MOD_ID, e);
            }
            return Snapshot.UNINITIALIZED;
        }
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
            status.getField("mentalIllness");

            Class<?> trait = Class.forName("net.depression.mental.MentalTrait", false, loader);
            trait.getField("medicineEffectMultiplier");

            Class<?> illness = Class.forName("net.depression.mental.MentalIllness", false, loader);
            illness.getField("mentalHealthId");
            illness.getField("odCount");
            illness.getField("lastOdTime");
            illness.getField("isMania");
            illness.getField("isInsomnia");
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Sanity: Renewed needs a Depression build with the PTSD level API (PTSDManager.getCurrentMaxPTSDValue, "
                    + "PTSD_n_VALUE), the MentalStatus fields emotionValue / tickCount / ptsdManager / mentalTrait / mentalIllness, "
                    + "MentalTrait.medicineEffectMultiplier and MentalIllness.mentalHealthId / odCount / lastOdTime / isMania / isInsomnia; this one does not have them", e);
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
