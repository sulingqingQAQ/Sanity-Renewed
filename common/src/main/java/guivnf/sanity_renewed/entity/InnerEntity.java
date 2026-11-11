package guivnf.sanity_renewed.entity;

import guivnf.sanity_renewed.SanityProcessor;
import guivnf.sanity_renewed.capability.Sanity;
import guivnf.sanity_renewed.capability.SanityHolder;
import guivnf.sanity_renewed.config.ConfigProxy;
import guivnf.sanity_renewed.sound.SoundRegistry;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public abstract class InnerEntity extends Monster
{
    /**
     * Synced "target player" state, replacing the old {@code InnerEntityCapImpl} whose
     * serialize/deserialize methods were never called by anything (the field was effectively dead:
     * the targeted-player visibility branch never fired and the run animation never played).
     *
     * <p>{@code Optional<UUID>} is vanilla-serializable ({@link EntityDataSerializers#OPTIONAL_UUID})
     * and is replicated automatically with the entity's tracked data, on both loaders, without a
     * custom payload. The value is the UUID of the player this inner entity is currently hunting
     * (vanilla {@link #getTarget()}), or empty when it has no player target.
     */
    private static final EntityDataAccessor<Optional<UUID>> DATA_TARGET_UUID =
            SynchedEntityData.defineId(InnerEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    protected InnerEntity(EntityType<? extends Monster> entityType, Level level)
    {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(@NotNull SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(DATA_TARGET_UUID, Optional.empty());
    }

    /**
     * Server-side: publish the current target's player UUID into the synced field.
     * Called once per server tick from {@link #tick()} so the client sees the same "who is it
     * hunting" state the AI goal set via {@link #setTarget(LivingEntity)}.
     */
    private void syncTargetData()
    {
        if (level().isClientSide)
            return;
        LivingEntity target = getTarget();
        UUID targetUuid = target instanceof Player player ? player.getUUID() : null;
        Optional<UUID> stored = getEntityData().get(DATA_TARGET_UUID);
        if (targetUuid == null)
        {
            if (stored.isPresent())
                getEntityData().set(DATA_TARGET_UUID, Optional.empty());
        }
        else if (stored.isEmpty() || !targetUuid.equals(stored.get()))
        {
            getEntityData().set(DATA_TARGET_UUID, Optional.of(targetUuid));
        }
    }

    /** Client and server: the UUID of the player this entity is currently hunting, if any. */
    public Optional<UUID> getSyncedTargetUuid()
    {
        return getEntityData().get(DATA_TARGET_UUID);
    }

    @Override
    public void tick()
    {
        super.tick();
        syncTargetData();
    }

    public boolean isVisibleTo(Player player)
    {
        if (player == null || isInvisibleTo(player))
            return false;

        if (ConfigProxy.getSaneSeeInnerEntities(player.level().dimension().location())
                || player.isCreative() || player.isSpectator())
            return true;

        // The previously dead branch: now backed by real synced data, so a targeted player always
        // sees the thing hunting them even while their own insanity is still inside the 0.60-0.87
        // "visible but untouchable" window.
        if (getSyncedTargetUuid().map(uuid -> uuid.equals(player.getUUID())).orElse(false))
            return true;

        Sanity s = SanityHolder.get(player);
        return s != null && s.getSanity() >= SanityProcessor.SANITY_VISIBLE_THRESHOLD;
    }

    public boolean canBeAttackedBy(Player player)
    {
        if (player == null)
            return false;

        if (ConfigProxy.getSaneSeeInnerEntities(player.level().dimension().location())
                || player.isCreative() || player.isSpectator()
                || getTarget() == player)
            return true;

        Sanity s = SanityHolder.get(player);
        return s != null && s.getSanity() >= SanityProcessor.SANITY_TARGET_THRESHOLD;
    }

    @Override
    public boolean skipAttackInteraction(Entity entity)
    {
        if (entity instanceof Player player)
            return !canBeAttackedBy(player);
        return super.skipAttackInteraction(entity);
    }

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float amount)
    {
        if (!level().isClientSide && damageSource.getEntity() instanceof Player player && !canBeAttackedBy(player))
            return false;
        return super.hurt(damageSource, amount);
    }

    @Override
    public void die(@NotNull DamageSource damageSource)
    {
        if (!level().isClientSide && damageSource.getEntity() instanceof ServerPlayer player)
        {
            Sanity s = SanityHolder.get(player);
            if (s != null)
            {
                // Kept as-is in phase 3: the legacy reward is still centrally closed by
                // SanitySourceGate.LEGACY_ACTIVE_SOURCES until a later, separately-reviewed change
                // decides what killing an inner entity does under the Depression addon rules.
                SanityProcessor.addSanity(s, ConfigProxy.getInnerEntityKill(player.level().dimension().location()), player);
                s.setInnerEntityKills(s.getInnerEntityKills() + 1);
            }
        }
        super.die(damageSource);
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    protected void doPush(@NotNull Entity entity)
    {
    }

    @Override
    public boolean shouldDropExperience()
    {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSource)
    {
        return SoundRegistry.INNER_ENTITY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound()
    {
        return SoundRegistry.INNER_ENTITY_HURT.get();
    }
}
