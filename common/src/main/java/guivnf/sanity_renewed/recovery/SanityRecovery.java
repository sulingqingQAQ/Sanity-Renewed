package guivnf.sanity_renewed.recovery;

import guivnf.sanity_renewed.capability.Sanity;
import guivnf.sanity_renewed.config.ConfigProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 1: the only two things that raise sanity by themselves. Server side, once per player tick.
 *
 * <ul>
 *   <li><b>Base recovery</b>, {@code +0.05 S/s} by default: only while {@link DepressionLink#isCalmForBaseRecovery}
 *       holds and the local light at the eyes is at least {@code base_min_light} (default 10, to be tuned).</li>
 *   <li><b>Campfire recovery</b>, {@code +0.10 S/s} by default: a lit campfire within {@code campfire_radius}
 *       blocks (default 4) with line of sight. Several campfires count once. It does not depend on the base
 *       safety gate (combat, PTSD, light) and there is no mood multiplier in phase 1.</li>
 * </ul>
 * The two add up when both apply. The original passive "campfire" block source is closed in
 * {@code SanitySourceGate}, so a campfire is never counted twice.
 *
 * <p>Rates are sanity points per second (S, 100 = clear-headed); one tick applies {@code rate / 20}.
 * Nothing here reads or writes Depression's mood or mental health values.
 */
public final class SanityRecovery
{
    /** The lit-campfire scan is cached for this many ticks per player (the result is a boolean, rates are exact). */
    private static final int CAMPFIRE_SCAN_INTERVAL_TICKS = 10;

    private SanityRecovery() {}

    /** Call for an alive, non-creative, non-spectator player. Updates sanity and the HUD arrow value. */
    public static void tick(ServerPlayer player, Sanity sanity)
    {
        ResourceLocation dim = player.level().dimension().location();

        float gainPerTick = 0f;

        float baseRate = ConfigProxy.getBaseRecoveryPerSecond(dim);
        if (baseRate > 0f && isBaseRecoveryAllowed(player, dim))
            gainPerTick += baseRate / 20f;

        float campfireRate = ConfigProxy.getCampfireRecoveryPerSecond(dim);
        if (campfireRate > 0f && isNearLitCampfire(player, sanity, dim))
            gainPerTick += campfireRate / 20f;

        float before = sanity.getSanity();
        if (gainPerTick > 0f)
            sanity.setSanity(before - gainPerTick / 100f); // S up by x points = insanity down by x / 100
        sanity.setPassiveIncrease(sanity.getSanity() - before); // <= 0 while recovering, 0 otherwise (also when clamped at S = 100)
    }

    private static boolean isBaseRecoveryAllowed(ServerPlayer player, ResourceLocation dim)
    {
        if (!DepressionLink.isCalmForBaseRecovery(player))
            return false;
        BlockPos eyes = BlockPos.containing(player.getEyePosition());
        return player.level().getMaxLocalRawBrightness(eyes) >= ConfigProxy.getBaseRecoveryMinLight(dim);
    }

    private static boolean isNearLitCampfire(ServerPlayer player, Sanity sanity, ResourceLocation dim)
    {
        int now = player.tickCount;
        int last = sanity.getCampfireScanTick();
        if (last == Integer.MIN_VALUE || now < last || now - last >= CAMPFIRE_SCAN_INTERVAL_TICKS)
        {
            sanity.setCampfireNear(scanForLitCampfire(player, ConfigProxy.getCampfireRadius(dim)));
            sanity.setCampfireScanTick(now);
        }
        return sanity.isCampfireNear();
    }

    private static boolean scanForLitCampfire(ServerPlayer player, float radius)
    {
        Level level = player.level();
        Vec3 feet = player.position();
        Vec3 eyes = player.getEyePosition();
        BlockPos origin = player.blockPosition();
        int reach = (int) Math.ceil(radius);
        double radiusSq = (double) radius * radius;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -reach; dx <= reach; ++dx)
        {
            for (int dy = -reach; dy <= reach; ++dy)
            {
                for (int dz = -reach; dz <= reach; ++dz)
                {
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    Vec3 center = pos.getCenter();
                    if (center.distanceToSqr(feet) > radiusSq || !level.hasChunkAt(pos))
                        continue;

                    BlockState state = level.getBlockState(pos);
                    if (!state.is(BlockTags.CAMPFIRES) || !state.hasProperty(CampfireBlock.LIT) || !state.getValue(CampfireBlock.LIT))
                        continue;

                    // line of sight to the middle of the block (above the 7/16 high campfire shape)
                    HitResult hit = level.clip(new ClipContext(eyes, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                    if (hit.getType() == HitResult.Type.MISS)
                        return true;
                }
            }
        }
        return false;
    }
}
