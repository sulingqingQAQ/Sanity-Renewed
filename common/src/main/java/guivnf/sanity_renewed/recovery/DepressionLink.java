package guivnf.sanity_renewed.recovery;

import guivnf.sanity_renewed.SanityMod;
import net.depression.mental.MentalStatus;
import net.depression.server.Registry;
import net.minecraft.server.level.ServerPlayer;

/**
 * The only place of the addon that touches Depression classes on the server (hard dependency).
 *
 * <p>Read-only: it looks at a few state flags and never reads, writes or converts Depression's mood
 * {@code e} / mental health {@code h}, and never calls {@code mentalHeal} / {@code mentalHurt}.
 * Everything fails closed: if the state is missing or the Depression API does not look like the
 * expected version, base recovery simply does not happen.
 */
public final class DepressionLink
{
    private static boolean apiErrorReported;

    private DepressionLink() {}

    /**
     * Server-side safety gate of the base recovery (everything except light, which the caller checks).
     * <ul>
     *   <li>Depression state exists and is initialised (mental trait chosen): a missing state means no recovery;</li>
     *   <li>not in combat ({@code combatCountdown <= 0}) and not in a Depression player event;</li>
     *   <li>no PTSD contact countdown running ({@code PTSDManager.hasRemaining() == false}).</li>
     * </ul>
     */
    public static boolean isCalmForBaseRecovery(ServerPlayer player)
    {
        try
        {
            MentalStatus status = Registry.mentalStatus.get(player.getUUID());
            if (status == null || status.mentalTrait == null)
                return false;
            if (status.combatCountdown > 0 || Registry.playerEventMap.containsKey(player.getUUID()))
                return false;
            return !status.ptsdManager.hasRemaining();
        }
        catch (LinkageError e)
        {
            if (!apiErrorReported)
            {
                apiErrorReported = true;
                SanityMod.LOGGER.error("[{}] Depression API does not match what this addon was built against; base sanity recovery is disabled", SanityMod.MOD_ID, e);
            }
            return false;
        }
    }
}
