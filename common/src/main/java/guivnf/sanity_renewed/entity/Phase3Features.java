package guivnf.sanity_renewed.entity;

import guivnf.sanity_renewed.SanityMod;
import guivnf.sanity_renewed.config.ConfigProxy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Phase 3 feature gate for inner entity (shadow creature) spawning.
 *
 * <p>Phase 1 shipped with {@code SanitySourceGate.INNER_ENTITY_SPAWNING = false}, i.e. no new inner
 * entities ever spawn and the first-phase default behaviour is preserved. Phase 3 must not silently
 * change that: spawning comes back only when the operator explicitly turns on the config switch
 * {@code sanity.phase3.inner_entity_spawning} (default {@code false}).
 *
 * <p>The gate is deliberately config-driven rather than a compile-time constant so that a dedicated
 * server can opt in without a rebuild, while a fresh install keeps the phase-1 behaviour exactly.
 * Everything this gate protects (the spawner tick call, the spawner's own early return) fails closed:
 * a missing config value, a wrong dimension entry or any config error leaves spawning off.
 */
public final class Phase3Features
{
    private static volatile boolean dimensionWarned;

    private Phase3Features() {}

    /**
     * Whether new inner entities may be spawned for this player in their current dimension.
     * Checks the explicit phase-3 config switch only; it never touches the legacy phase-1 constant.
     */
    public static boolean innerEntitySpawningEnabled(ServerPlayer player)
    {
        if (player == null)
            return false;
        ResourceLocation dim;
        try
        {
            dim = player.level().dimension().location();
        }
        catch (RuntimeException e)
        {
            if (!dimensionWarned)
            {
                dimensionWarned = true;
                SanityMod.LOGGER.warn("[{}] could not read the dimension of {} ({}), inner entity spawning stays off",
                        SanityMod.MOD_ID, player.getScoreboardName(), e.toString());
            }
            return false;
        }
        // Never throws: a missing, mistyped or unreadable value reads as false (see ConfigProxy).
        return ConfigProxy.getPhase3InnerEntitySpawning(dim);
    }
}
