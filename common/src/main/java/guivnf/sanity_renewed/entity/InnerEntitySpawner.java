package guivnf.sanity_renewed.entity;

import guivnf.sanity_renewed.SanitySourceGate;
import guivnf.sanity_renewed.capability.Sanity;
import guivnf.sanity_renewed.capability.SanityHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InnerEntitySpawner
{
    private static final RandomSource RAND = RandomSource.create();

    public static int spawnRad = 20;
    public static int detectionRad = 40;
    public static int spawnTimeout = 20 * 20;

    public static final float SPAWN_THRESHOLD = .75f;
    public static final int BASE_MAX_NEARBY = 3;
    public static final int MAX_NEARBY_CAP = 10;
    public static final int KILLS_PER_EXTRA_SPAWN = 4;
    public static final int KILLS_PER_TIMEOUT_STEP = 5;
    public static final int MIN_SPAWN_TIMEOUT = 60;
    public static final Map<ServerPlayer, Integer> PLAYER_TO_SPAWN_TIMEOUT = new HashMap<>();

    private InnerEntitySpawner() {}

    private static int getHeightForSpawning(Level level, BlockPos blockPos, int radius)
    {
        BlockPos.MutableBlockPos mutable = blockPos.mutable();
        for (int i = 0; i < radius; i++)
        {
            if (!level.getBlockState(mutable).isAir() && level.getBlockState(mutable.move(Direction.UP)).isAir())
                return mutable.getY();
        }
        for (int i = 0; i < radius; i++)
        {
            if (level.getBlockState(mutable).isAir() && !level.getBlockState(mutable.move(Direction.DOWN)).isAir())
                return mutable.getY() - 1;
        }
        return 0;
    }

    public static boolean trySpawnForPlayer(ServerPlayer player)
    {
        // Phase 1: no new inner entities (spawn height, retry cooldown and cache cleanup are phase 3 work)
        if (!SanitySourceGate.INNER_ENTITY_SPAWNING)
            return false;

        if (player == null || player.isCreative() || player.isSpectator()
                || player.level().getDifficulty().equals(Difficulty.PEACEFUL))
            return false;

        PLAYER_TO_SPAWN_TIMEOUT.putIfAbsent(player, 0);
        int t = PLAYER_TO_SPAWN_TIMEOUT.get(player);
        if (t > 0)
        {
            PLAYER_TO_SPAWN_TIMEOUT.put(player, t - 1);
            return false;
        }

        Sanity s = SanityHolder.get(player);
        if (s == null) return false;

        int kills = s.getInnerEntityKills();
        int maxNearby = Math.min(BASE_MAX_NEARBY + kills / KILLS_PER_EXTRA_SPAWN, MAX_NEARBY_CAP);
        if (s.getSanity() < SPAWN_THRESHOLD
                || getInnerEntitiesInRadius(player.level(), player.blockPosition(), detectionRad).size() >= maxNearby)
            return false;

        int index = RAND.nextInt(EntityRegistry.INNER_ENTITIES.size());
        InnerEntity entity = EntityRegistry.INNER_ENTITIES.get(index).get().create(player.level());
        if (entity == null) return false;

        BlockPos trialPos = BlockPos.randomBetweenClosed(RAND, 1,
                player.blockPosition().getX() - spawnRad,
                player.blockPosition().getY(),
                player.blockPosition().getZ() - spawnRad,
                player.blockPosition().getX() + spawnRad,
                player.blockPosition().getY(),
                player.blockPosition().getZ() + spawnRad).iterator().next();
        int h = getHeightForSpawning(player.level(), trialPos, spawnRad);
        if (h == 0) return false;

        trialPos = new BlockPos(trialPos.getX(), h, trialPos.getZ());
        entity.setPos(new Vec3(trialPos.getX() + .5f, trialPos.getY() + .5f, trialPos.getZ() + .5f));
        ServerLevel serverLevel = (ServerLevel) player.level();
        if (entity.checkSpawnObstruction(serverLevel)
                && serverLevel.noCollision(entity)
                && serverLevel.tryAddFreshEntityWithPassengers(entity))
        {
            PLAYER_TO_SPAWN_TIMEOUT.put(player,
                    Math.max(spawnTimeout / (1 + kills / KILLS_PER_TIMEOUT_STEP), MIN_SPAWN_TIMEOUT));
            return true;
        }
        return false;
    }

    public static List<InnerEntity> getInnerEntitiesInRadius(Level level, BlockPos blockPos, int radius)
    {
        return level.getEntitiesOfClass(InnerEntity.class, new AABB(blockPos).inflate(radius));
    }
}
