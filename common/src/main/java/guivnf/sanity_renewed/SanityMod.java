package guivnf.sanity_renewed;

import guivnf.sanity_renewed.config.ConfigManager;
import guivnf.sanity_renewed.entity.EntityRegistry;
import guivnf.sanity_renewed.item.ItemRegistry;
import guivnf.sanity_renewed.sound.SoundRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SanityMod
{
    public static final String MOD_ID = "sanity_renewed";
    public static final String MOD_NAME = "Sanity: Renewed";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    static
    {
        ConfigManager.init();
    }

    private SanityMod() {}

    public static void init()
    {
        ConfigManager.register();
        SoundRegistry.init();
        ItemRegistry.init();
        EntityRegistry.init();
        EntityRegistry.registerAttributes();
        guivnf.sanity_renewed.net.PacketHandler.init();
        guivnf.sanity_renewed.event.EventHandler.register();
        guivnf.sanity_renewed.recovery.DepressionLink.installModifiers();
        LOGGER.info("[{}] common init complete", MOD_ID);
    }

    public static void clientInit()
    {
        guivnf.sanity_renewed.net.PacketHandler.initClient();
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                EntityRegistry.ROTTING_STALKER,
                guivnf.sanity_renewed.client.render.RendererRottingStalker::new);
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                EntityRegistry.SNEAKING_TERROR,
                guivnf.sanity_renewed.client.render.RendererSneakingTerror::new);
        guivnf.sanity_renewed.client.ClientEventHandler.register();
        LOGGER.info("[{}] client init complete", MOD_ID);
    }
}
