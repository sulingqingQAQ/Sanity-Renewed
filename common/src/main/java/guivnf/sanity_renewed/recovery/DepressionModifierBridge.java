package guivnf.sanity_renewed.recovery;

import net.depression.mental.MentalModifiers;
import net.minecraft.server.level.ServerPlayer;

/**
 * The one class that implements Depression's {@link MentalModifiers.Provider}. It is a class of its own and is only
 * reached through {@link DepressionLink#installModifiers()} after that method found {@code MentalModifiers}: a Depression
 * build without the hook then only means that this class is never loaded, instead of making the link to Depression fail
 * for the whole addon.
 */
final class DepressionModifierBridge implements MentalModifiers.Provider
{
    private static final DepressionModifierBridge INSTANCE = new DepressionModifierBridge();

    private DepressionModifierBridge() {}

    /** Registers the hook and returns the API version of the Depression build; any other than {@code expectedApi} stops start-up. */
    static int install(int expectedApi)
    {
        int api = MentalModifiers.apiVersion();
        if (api != expectedApi)
            throw new IllegalStateException("Sanity: Renewed was written for Depression's MentalModifiers api " + expectedApi
                    + " but this Depression build offers api " + api);
        MentalModifiers.register(INSTANCE);
        return api;
    }

    @Override
    public double negativeEmotionStabilizeMultiplier(ServerPlayer player)
    {
        return MoodCoupling.stabilizeMultiplier(player);
    }

    @Override
    public double emotionDamageMultiplier(ServerPlayer player)
    {
        return MoodCoupling.damageMultiplier(player);
    }
}
