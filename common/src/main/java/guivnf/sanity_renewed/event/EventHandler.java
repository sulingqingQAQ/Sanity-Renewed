package guivnf.sanity_renewed.event;

import guivnf.sanity_renewed.SanityProcessor;
import guivnf.sanity_renewed.SanitySourceGate;
import guivnf.sanity_renewed.capability.Sanity;
import guivnf.sanity_renewed.capability.SanityHolder;
import guivnf.sanity_renewed.command.SanityCommand;
import guivnf.sanity_renewed.entity.InnerEntitySpawner;
import guivnf.sanity_renewed.net.SanitySync;
import guivnf.sanity_renewed.passive.Jukebox;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class EventHandler
{
    private EventHandler() {}

    public static void register()
    {
        TickEvent.PLAYER_POST.register(EventHandler::onPlayerTick);
        TickEvent.SERVER_LEVEL_POST.register(SanityProcessor::tickLevel);

        // Legacy one-shot sources: not even listened to while they are closed (they are also closed
        // inside SanityProcessor, see SanitySourceGate).
        if (SanitySourceGate.LEGACY_ACTIVE_SOURCES)
        {
            EntityEvent.LIVING_HURT.register(EventHandler::onLivingHurt);
            EntityEvent.LIVING_DEATH.register(EventHandler::onLivingDeath);

            PlayerEvent.PLAYER_ADVANCEMENT.register(SanityProcessor::handlePlayerGotAdvancement);

            BlockEvent.BREAK.register((level, pos, state, player, xp) ->
            {
                if (player != null)
                    SanityProcessor.handlePlayerMinedBlock(
                            player, pos, state, state.getBlock(),
                            player.hasCorrectToolForDrops(state));
                return EventResult.pass();
            });
        }

        // Join: the first packet plus one repeat shortly after, so a first packet that reached the client
        // before its player object was ready (or got lost) heals itself. Registered once, here.
        PlayerEvent.PLAYER_JOIN.register(SanitySync::onPlayerJoin);

        // The client rebuilds its player object when the dimension changes: push the state again.
        PlayerEvent.CHANGE_DIMENSION.register((player, oldDim, newDim) ->
        {
            SanityProcessor.handlePlayerChangedDimensions(player);
            // The old world's spawn retry budget must not follow the player into the new dimension.
            if (player instanceof ServerPlayer sp)
                InnerEntitySpawner.onPlayerChangedDimension(sp);
        });

        // Death / end-portal return creates a new player object: carry the sanity over, unchanged.
        PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wonGame) ->
        {
            Sanity from = SanityHolder.get(oldPlayer);
            Sanity to = SanityHolder.get(newPlayer);
            if (from != null && to != null)
                to.copyPersistentFrom(from);

            // The UUID-keyed spawn cooldown is dropped together with the player object, except for a death respawn in
            // the dimension the player died in: that one keeps the remaining retry budget (the remaining number of
            // eligible spawn attempts, not wall-clock time -- it does not count down while dead). wonGame is the
            // end-portal return, which always drops it. Keep the !wonGame check: a respawn point forced into the End
            // makes that return END to END, which the dimension check alone would keep. See
            // InnerEntitySpawner#onPlayerClone.
            ServerPlayer oldSp = oldPlayer instanceof ServerPlayer o ? o : null;
            ServerPlayer newSp = newPlayer instanceof ServerPlayer n ? n : null;
            if (!wonGame && oldSp != null && newSp != null
                    && oldSp.level().dimension().equals(newSp.level().dimension()))
                return;
            InnerEntitySpawner.onPlayerClone(oldSp, newSp);
        });

        // Phase 3: release the UUID-keyed spawn cooldown when a player leaves so the map cannot grow
        // across reconnects (the old implementation held strong ServerPlayer references forever).
        PlayerEvent.PLAYER_QUIT.register(player ->
        {
            if (player instanceof ServerPlayer sp)
                InnerEntitySpawner.onPlayerQuit(sp);
        });
        // Only the end-portal return still has to clean up here: a normal death respawn keeps a same-dimension
        // player's remaining cooldown in the clone callback above, and firing the cleanup again for it would delete
        // what the clone callback just kept. The end-portal return reaches both callbacks; the repeated removal is
        // idempotent.
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, removalReason) ->
        {
            if (conqueredEnd && player instanceof ServerPlayer sp)
                InnerEntitySpawner.onPlayerChangedDimension(sp);
        });

        CommandRegistrationEvent.EVENT.register((dispatcher, registry, env) ->
                SanityCommand.register(dispatcher));

        LifecycleEvent.SERVER_STOPPING.register(server ->
        {
            Jukebox.JUKEBOXES.clear();
            Jukebox.UNSETTLING_JUKEBOXES.clear();
            InnerEntitySpawner.clearAll();
        });
    }

    private static void onPlayerTick(Player player)
    {
        if (player instanceof ServerPlayer sp)
            SanityProcessor.tickPlayer(sp);
    }

    private static EventResult onLivingHurt(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount)
    {
        if (entity instanceof ServerPlayer sp)
        {
            SanityProcessor.handlePlayerHurt(sp, amount);
        }
        else if (entity instanceof Animal animal && source.getEntity() instanceof ServerPlayer sp)
        {
            SanityProcessor.handlePlayerHurtAnimal(sp, animal, amount);
        }
        return EventResult.pass();
    }

    private static EventResult onLivingDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source)
    {
        if (entity instanceof TamableAnimal ta && ta.getOwnerUUID() != null)
        {
            Level level = entity.level();
            if (level instanceof net.minecraft.server.level.ServerLevel sl)
            {
                ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(ta.getOwnerUUID());
                if (owner != null)
                    SanityProcessor.handlePlayerPetDeath(owner, ta);
            }
        }
        return EventResult.pass();
    }
}
