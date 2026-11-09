package guivnf.sanity_renewed.net;

import guivnf.sanity_renewed.capability.Sanity;
import guivnf.sanity_renewed.capability.SanityHolder;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side entry points for pushing a player's sanity to their own client.
 * There is no unconditional per-tick packet: see {@link Sanity#pollSync(int)} for the rules.
 */
public final class SanitySync
{
    /** Ticks after joining at which the single extra join packet is sent (2 seconds). */
    public static final int JOIN_RESYNC_DELAY_TICKS = 40;

    private SanitySync() {}

    /**
     * {@code PLAYER_JOIN}: the player gets the normal first packet on their first tick, plus exactly one
     * repeat {@link #JOIN_RESYNC_DELAY_TICKS} ticks later, so a first packet that reached the client before
     * its player object existed (or got lost) heals itself. Nothing periodic and nothing per tick.
     */
    public static void onPlayerJoin(ServerPlayer player)
    {
        Sanity sanity = SanityHolder.get(player);
        if (sanity == null)
            return;
        sanity.requestSync();
        sanity.requestSyncIn(player.tickCount, JOIN_RESYNC_DELAY_TICKS);
    }

    /** Per-tick: sends only when a packet is due (forced request, or interval + real change). */
    public static void tick(ServerPlayer player, Sanity sanity)
    {
        if (sanity.pollSync(player.tickCount))
            PacketHandler.sendSanityToPlayer(player, sanity);
    }

    /** Debug / explicit: send the current state right now, ignoring the interval. */
    public static void pushNow(ServerPlayer player, Sanity sanity)
    {
        sanity.markSynced(player.tickCount);
        PacketHandler.sendSanityToPlayer(player, sanity);
    }
}
