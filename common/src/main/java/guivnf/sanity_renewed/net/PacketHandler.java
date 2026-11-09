package guivnf.sanity_renewed.net;

import guivnf.sanity_renewed.SanityMod;
import guivnf.sanity_renewed.capability.Sanity;
import guivnf.sanity_renewed.capability.SanityHolder;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class PacketHandler
{
    public static final ResourceLocation SANITY_SYNC = ResourceLocation.fromNamespaceAndPath(SanityMod.MOD_ID, "sanity_sync");

    private PacketHandler() {}

    public static void init()
    {
        if (Platform.getEnvironment() == Env.SERVER)
            NetworkManager.registerS2CPayloadType(SANITY_SYNC);
    }

    public static void initClient()
    {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SANITY_SYNC, (buf, ctx) ->
        {
            float sanity = buf.readFloat();
            float passive = buf.readFloat();
            ctx.queue(() ->
            {
                if (ctx.getEnvironment() != Env.CLIENT) return;
                Player player = ctx.getPlayer();
                if (player == null) return;
                Sanity s = SanityHolder.get(player);
                if (s == null) return;
                s.setSanity(sanity);
                s.setPassiveIncrease(passive);
                s.setClientReceivedTick(player.tickCount);
            });
        });
    }

    public static void sendSanityToPlayer(ServerPlayer player, Sanity sanity)
    {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        sanity.serialize(buf);
        NetworkManager.sendToPlayer(player, SANITY_SYNC, buf);
    }
}
