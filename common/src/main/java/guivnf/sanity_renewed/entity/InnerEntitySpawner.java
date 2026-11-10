package guivnf.sanity_renewed.entity;

import guivnf.sanity_renewed.SanitySourceGate;
import guivnf.sanity_renewed.capability.Sanity;
import guivnf.sanity_renewed.capability.SanityHolder;
import guivnf.sanity_renewed.config.ConfigProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manual per-player spawner for inner entities (shadow creatures).
 *
 * <p>Phase 3 fixes over the original implementation:
 * <ul>
 *   <li><b>Spawn height scan</b>: the old {@code getHeightForSpawning} relied on {@code &&} short
 *       circuit side effects ({@code mutable.move()} never ran on air blocks) and returned
 *       {@code getY() - 1} on the downward branch, so on flat ground the entity was placed one block
 *       inside the terrain and the collision check rejected it — inner entities effectively never
 *       spawned in open terrain. The new {@link #findSpawnY} walks the column explicitly (no
 *       short-circuit movement), returns a sentinel on failure instead of the legal height 0, and
 *       prefers the vanilla {@link Heightmap#MOTION_BLOCKING_NO_LEAVES} surface.</li>
 *   <li><b>Failure cooldown</b>: a failed attempt now also arms the short cooldown
 *       {@code sanity.phase3.spawn_failure_cooldown_ticks} (default {@link #FAILURE_TIMEOUT_TICKS}),
 *       so an eligible player in an unsuitable spot is retried at most a few times per second
 *       instead of running the radius-40 entity query, entity creation and block scan on every tick.
 *       Success keeps the original longer, kill-count-scaled cooldown.</li>
 *   <li><b>UUID-keyed cooldown map</b>: {@link #PLAYER_TO_SPAWN_TIMEOUT} is keyed by {@link UUID}
 *       instead of holding a strong reference to the {@link ServerPlayer}. Entries are removed on
 *       quit, dimension change, clone (death/end-portal return) and server stop — see
 *       {@link #onPlayerQuit}, {@link #onPlayerChangedDimension}, {@link #onPlayerClone},
 *       {@link #clearAll}.</li>
 * </ul>
 *
 * <p>Spawning is still double-gated: the compile-time phase-1 {@link SanitySourceGate#INNER_ENTITY_SPAWNING}
 * constant must be opened by a phase-3 build, and even then the operator must explicitly enable
 * {@code sanity.phase3.inner_entity_spawning} (default off, see {@link Phase3Features}).
 */
public final class InnerEntitySpawner
{
    private static final RandomSource RAND = RandomSource.create();

    // The spawn and detection radii are sanity.phase3.spawn_radius / detection_radius (default 20 / 40, see ConfigProxy).
    public static int spawnTimeout = 20 * 20;

    public static final float SPAWN_THRESHOLD = .75f;
    public static final int BASE_MAX_NEARBY = 3;
    public static final int MAX_NEARBY_CAP = 10;
    public static final int KILLS_PER_EXTRA_SPAWN = 4;
    public static final int KILLS_PER_TIMEOUT_STEP = 5;
    public static final int MIN_SPAWN_TIMEOUT = 60;

    /**
     * Default short cooldown (ticks) armed after every failed spawn attempt so the per-tick cost is
     * bounded while a player stays eligible but the sampled spot is unsuitable. 20 ticks = 1 second.
     * The live value comes from {@code sanity.phase3.spawn_failure_cooldown_ticks}; this constant is
     * both the config default and the documented fallback.
     */
    public static final int FAILURE_TIMEOUT_TICKS = 20;

    /** Failure sentinel: negative, so the legal world height y = 0 is never mistaken for "no spot". */
    public static final int NO_SPAWN_Y = Integer.MIN_VALUE;

    /**
     * Per-player retry bookkeeping, keyed by player UUID (never by the player object, so a logged-out
     * player cannot be retained). Values are remaining ticks until the next attempt.
     */
    public static final Map<UUID, Integer> PLAYER_TO_SPAWN_TIMEOUT = new ConcurrentHashMap<>();

    private InnerEntitySpawner() {}

    /**
     * Finds a standing Y for an inner entity at the given column.
     *
     * <p>Replacement for the broken short-circuit scan. Strategy:
     * <ol>
     *   <li>Ask the loaded heightmap ({@code MOTION_BLOCKING_NO_LEAVES}) for the first block above the
     *       motion-blocking surface; the heightmap is maintained by the server and costs O(1).</li>
     *   <li>If no heightmap value is available (odd/unloaded chunk edge), fall back to an explicit
     *       column walk from {@code maxY} down to {@code minY}: the candidate is the first air block
     *       whose block below is solid enough to stand on. Every {@code move()} is its own statement,
     *       so there is no {@code &&} short circuit, and the returned Y is the air block itself
     *       (never {@code solidY - 1}).</li>
     * </ol>
     * Returns {@link #NO_SPAWN_Y} when the column offers no standing space within the vertical search
     * radius, instead of returning 0 (a perfectly legal spawn height in superflat worlds).
     */
    public static int findSpawnY(Level level, BlockPos column, int radius)
    {
        int minY = level.dimensionType().minY();
        int maxY = minY + level.dimensionType().height();

        if (level instanceof ServerLevel serverLevel && serverLevel.hasChunkAt(column))
        {
            // Heightmap semantics: getHeight returns the first Y above the surface, i.e. an air block
            // suitable as the entity's feet. Bounds-check it against the vertical search window.
            int surface = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            int windowBottom = Math.max(column.getY() - radius, minY + 1);
            int windowTop = Math.min(column.getY() + radius, maxY - 2);
            if (surface >= windowBottom && surface <= windowTop
                    && level.getBlockState(new BlockPos(column.getX(), surface, column.getZ())).isAir()
                    && level.getBlockState(new BlockPos(column.getX(), surface + 1, column.getZ())).isAir())
            {
                return surface;
            }
        }

        // Explicit fallback scan: bounded vertical window around the sampled Y, top-down.
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(column.getX(),
                Math.min(column.getY() + radius, maxY - 1), column.getZ());
        int bottom = Math.max(column.getY() - radius, minY + 1);
        while (cursor.getY() >= bottom)
        {
            BlockPos feet = cursor.immutable();
            BlockPos head = cursor.above();
            BlockPos ground = cursor.below();
            boolean feetFree = level.getBlockState(feet).isAir();
            boolean headFree = level.getBlockState(head).isAir();
            boolean groundSolid = level.getBlockState(ground).isCollisionShapeFullBlock(level, ground);
            if (feetFree && headFree && groundSolid)
                return cursor.getY();
            cursor.move(net.minecraft.core.Direction.DOWN, 1);
        }
        return NO_SPAWN_Y;
    }

    public static boolean trySpawnForPlayer(ServerPlayer player)
    {
        if (player == null)
            return false;

        // Phase 1 compile-time gate stays as the hard switch; a phase-3 build opens it in code.
        if (!SanitySourceGate.INNER_ENTITY_SPAWNING)
            return false;

        // Explicit, default-off operator switch (config). Without it the phase-1 default is preserved.
        if (!Phase3Features.innerEntitySpawningEnabled(player))
            return false;

        if (player.isCreative() || player.isSpectator()
                || player.level().getDifficulty().equals(Difficulty.PEACEFUL))
            return false;

        ResourceLocation dim = player.level().dimension().location();
        int spawnRadius = Math.max(1, ConfigProxy.getPhase3SpawnRadius(dim));
        int detectionRadius = Math.max(1, ConfigProxy.getPhase3DetectionRadius(dim));
        int failureCooldown = Math.max(1, ConfigProxy.getPhase3SpawnFailureCooldown(dim));

        UUID id = player.getUUID();
        Integer remaining = PLAYER_TO_SPAWN_TIMEOUT.get(id);
        if (remaining != null && remaining > 0)
        {
            // Count down per player; the map never holds a player reference.
            PLAYER_TO_SPAWN_TIMEOUT.put(id, remaining - 1);
            return false;
        }

        Sanity s = SanityHolder.get(player);
        if (s == null) return false;

        int kills = s.getInnerEntityKills();
        int maxNearby = Math.min(BASE_MAX_NEARBY + kills / KILLS_PER_EXTRA_SPAWN, MAX_NEARBY_CAP);
        if (s.getSanity() < SPAWN_THRESHOLD
                || getInnerEntitiesInRadius(player.level(), player.blockPosition(), detectionRadius).size() >= maxNearby)
            return false;

        BlockPos trialPos = BlockPos.randomBetweenClosed(RAND, 1,
                player.blockPosition().getX() - spawnRadius,
                player.blockPosition().getY(),
                player.blockPosition().getZ() - spawnRadius,
                player.blockPosition().getX() + spawnRadius,
                player.blockPosition().getY(),
                player.blockPosition().getZ() + spawnRadius).iterator().next();

        int y = findSpawnY(player.level(), trialPos, spawnRadius);
        if (y == NO_SPAWN_Y)
        {
            // Failed spot: arm the short failure cooldown instead of retrying on the next tick.
            PLAYER_TO_SPAWN_TIMEOUT.put(id, failureCooldown);
            return false;
        }

        // Entity creation happens only after the cheap world checks have passed.
        int index = RAND.nextInt(EntityRegistry.INNER_ENTITIES.size());
        InnerEntity entity = EntityRegistry.INNER_ENTITIES.get(index).get().create(player.level());
        if (entity == null)
        {
            PLAYER_TO_SPAWN_TIMEOUT.put(id, failureCooldown);
            return false;
        }

        BlockPos spawnPos = new BlockPos(trialPos.getX(), y, trialPos.getZ());
        entity.setPos(new Vec3(spawnPos.getX() + .5f, spawnPos.getY() + .5f, spawnPos.getZ() + .5f));
        ServerLevel serverLevel = (ServerLevel) player.level();
        if (entity.checkSpawnObstruction(serverLevel)
                && serverLevel.noCollision(entity)
                && serverLevel.tryAddFreshEntityWithPassengers(entity))
        {
            PLAYER_TO_SPAWN_TIMEOUT.put(id,
                    Math.max(spawnTimeout / (1 + kills / KILLS_PER_TIMEOUT_STEP), MIN_SPAWN_TIMEOUT));
            return true;
        }

        // Rejected by collision / obstruction / world add: do not spin every tick.
        PLAYER_TO_SPAWN_TIMEOUT.put(id, failureCooldown);
        return false;
    }

    /** PLAYER_QUIT: drop the entry so a logged-out player is never referenced. */
    public static void onPlayerQuit(ServerPlayer player)
    {
        if (player != null)
            PLAYER_TO_SPAWN_TIMEOUT.remove(player.getUUID());
    }

    /** CHANGE_DIMENSION: the old cooldown belonged to the old world's spawn context. */
    public static void onPlayerChangedDimension(ServerPlayer player)
    {
        if (player != null)
            PLAYER_TO_SPAWN_TIMEOUT.remove(player.getUUID());
    }

    /** PLAYER_CLONE (death / end-portal return): a fresh player object starts without a retry budget. */
    public static void onPlayerClone(ServerPlayer oldPlayer, ServerPlayer newPlayer)
    {
        if (oldPlayer != null)
            PLAYER_TO_SPAWN_TIMEOUT.remove(oldPlayer.getUUID());
        if (newPlayer != null)
            PLAYER_TO_SPAWN_TIMEOUT.remove(newPlayer.getUUID());
    }

    /** SERVER_STOPPING / singleplayer world unload: release everything. */
    public static void clearAll()
    {
        PLAYER_TO_SPAWN_TIMEOUT.clear();
    }

    public static List<InnerEntity> getInnerEntitiesInRadius(Level level, BlockPos blockPos, int radius)
    {
        return level.getEntitiesOfClass(InnerEntity.class, new AABB(blockPos).inflate(radius));
    }
}
