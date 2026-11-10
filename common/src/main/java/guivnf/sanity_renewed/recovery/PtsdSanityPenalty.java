package guivnf.sanity_renewed.recovery;

import guivnf.sanity_renewed.SanityMod;
import guivnf.sanity_renewed.capability.Sanity;
import guivnf.sanity_renewed.config.ConfigProxy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * PTSD to sanity: one-off penalties when a PTSD episode reaches a higher level (rules in {@link PtsdEdgeTracker}).
 * This is the only thing of the 2A coupling that takes sanity away; nothing here runs per tick except comparing
 * Depression's tick counter with the last 20-tick window.
 *
 * <p>Depression recomputes {@code getCurrentMaxPTSDValue()} once per second block of its own tick, and
 * {@code trigger()} can raise it in between, so the value is only sampled once per window of Depression's
 * {@code tickCount}, never every tick. The penalty is written with {@link Sanity#setSanity}, not through the closed
 * {@code SanityProcessor.addSanity}.
 */
public final class PtsdSanityPenalty
{
    private PtsdSanityPenalty() {}

    /** Call for an alive, non-creative, non-spectator player, after {@link SanityRecovery#tick}. */
    public static void tick(ServerPlayer player, Sanity sanity)
    {
        ResourceLocation dim = player.level().dimension().location();
        PtsdEdgeTracker edge = sanity.getPtsdEdge();
        if (!ConfigProxy.getLinkEnabled(dim) || !ConfigProxy.getPtsdEnabled(dim))
        {
            edge.reset(); // switched on again later: the next sample is a fresh baseline, not a rise
            return;
        }

        // negative: no initialised Depression state, or it has not run its first tick yet (the PTSD value is not computed then)
        long mentalTicks = DepressionLink.readMentalTickCount(player);
        if (mentalTicks < 0L || !edge.acceptWindow(mentalTicks))
            return;

        int level = PtsdEdgeTracker.levelOf(DepressionLink.readCurrentMaxPtsd(player));
        float nominal = edge.sample(level, ConfigProxy.getPtsdRearmSeconds(dim), ConfigProxy.getPtsdPenaltyTable(dim));
        if (nominal <= 0f)
            return;

        // negative_multiplier is a balance knob for everything that costs sanity; in double and capped at 100 S so an extreme
        // multiplier cannot overflow to infinity (setSanity ignores non-finite values and the penalty would silently be lost)
        double points = Math.min((double) nominal * ConfigProxy.getNegMul(dim), 100d);
        sanity.setSanity(sanity.getSanity() + (float) (points / 100d)); // S down by x points = insanity up by x / 100
        if (ConfigProxy.getPtsdLog(dim))
            SanityMod.LOGGER.info("[{}] PTSD level {} reached: sanity -{} for {}", SanityMod.MOD_ID, level, points, player.getName().getString());
    }
}
