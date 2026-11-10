package guivnf.sanity_renewed.recovery;

import guivnf.sanity_renewed.capability.SanityHolder;
import guivnf.sanity_renewed.config.ConfigProxy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Mood and sanity talk to each other only through these multipliers (never by writing each other's value):
 * <ul>
 *   <li><b>mood to sanity</b>: {@link #baseRecoveryMultiplier} scales the BASE recovery of {@code SanityRecovery};
 *       it only slows or stops it, never takes sanity away, and the campfire is not scaled by it;</li>
 *   <li><b>sanity to mood</b>: {@link #stabilizeMultiplier} and {@link #damageMultiplier} are what Depression's
 *       {@code MentalModifiers} hook asks for, through {@code DepressionModifierBridge}.</li>
 * </ul>
 * Pure reads of config and state. No Depression class is named here, {@code DepressionLink} is the only reader.
 */
public final class MoodCoupling
{
    private MoodCoupling() {}

    /** Mood tier table of the design: multiplier of the base recovery, 1.0 when the coupling or this part is switched off. */
    public static float baseRecoveryMultiplier(ServerPlayer player, ResourceLocation dim)
    {
        if (!ConfigProxy.getLinkEnabled(dim) || !ConfigProxy.getMoodRecoveryEnabled(dim))
            return 1f;
        return MoodTables.recoveryMultiplier(
                DepressionLink.readEmotion(player),
                ConfigProxy.getMoodRecoveryE1(dim), ConfigProxy.getMoodRecoveryE2(dim), ConfigProxy.getMoodRecoveryE3(dim),
                ConfigProxy.getMoodRecoveryM0(dim), ConfigProxy.getMoodRecoveryM1(dim),
                ConfigProxy.getMoodRecoveryM2(dim), ConfigProxy.getMoodRecoveryM3(dim));
    }

    /**
     * Sanity tier 0..3 of the player, or -1 when sanity does not act on mood (coupling or this part switched off, or the
     * player object was already replaced). Server thread, called from Depression's tick and from the diagnostic command.
     */
    public static int sanityTierForMood(ServerPlayer player, ResourceLocation dim)
    {
        if (player.isRemoved() || !ConfigProxy.getLinkEnabled(dim) || !ConfigProxy.getSanityMoodEnabled(dim))
            return -1;
        return MoodTables.sanityTier(
                SanityHolder.get(player).getSanityPoints(),
                ConfigProxy.getSanityS1(dim), ConfigProxy.getSanityS2(dim), ConfigProxy.getSanityS3(dim));
    }

    /** Factor on how fast a negative mood fades (1.0 = Depression's own speed). */
    public static double stabilizeMultiplier(ServerPlayer player)
    {
        ResourceLocation dim = player.level().dimension().location();
        int tier = sanityTierForMood(player, dim);
        if (tier < 0)
            return 1d;
        return MoodTables.pickByTier(tier,
                ConfigProxy.getStabilizeM0(dim), ConfigProxy.getStabilizeM1(dim),
                ConfigProxy.getStabilizeM2(dim), ConfigProxy.getStabilizeM3(dim));
    }

    /** Factor on every psychological damage the player takes (1.0 = unchanged, never below 1.0 by config). */
    public static double damageMultiplier(ServerPlayer player)
    {
        ResourceLocation dim = player.level().dimension().location();
        int tier = sanityTierForMood(player, dim);
        if (tier < 0)
            return 1d;
        return MoodTables.pickByTier(tier,
                ConfigProxy.getDamageM0(dim), ConfigProxy.getDamageM1(dim),
                ConfigProxy.getDamageM2(dim), ConfigProxy.getDamageM3(dim));
    }
}
