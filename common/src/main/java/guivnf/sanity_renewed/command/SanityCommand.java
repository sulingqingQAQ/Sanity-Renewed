package guivnf.sanity_renewed.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import guivnf.sanity_renewed.capability.Sanity;
import guivnf.sanity_renewed.SanityMod;
import guivnf.sanity_renewed.capability.SanityHolder;
import guivnf.sanity_renewed.config.ConfigManager;
import guivnf.sanity_renewed.config.ConfigProxy;
import guivnf.sanity_renewed.config.DimensionConfig;
import guivnf.sanity_renewed.config.LinkConfigSpec;
import guivnf.sanity_renewed.net.SanitySync;
import guivnf.sanity_renewed.recovery.DepressionLink;
import guivnf.sanity_renewed.recovery.MedicineSanityCoupling;
import guivnf.sanity_renewed.recovery.MoodCoupling;
import guivnf.sanity_renewed.recovery.PtsdEdgeTracker;
import guivnf.sanity_renewed.recovery.SanityRecovery;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;

public final class SanityCommand
{
    private static final SimpleCommandExceptionType ERROR_SOURCE_NEEDS_PLAYER = new SimpleCommandExceptionType(
            Component.translatableWithFallback(
                    "commands.sanity.player_required",
                    "/sanity get, set and add without targets need a player as executor, use <targets>"));

    private SanityCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("sanity")
                .requires(stack -> stack.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0f, 100f))
                                .executes(ctx -> setSanity(ctx.getSource(),
                                        Collections.singleton(requireSourcePlayer(ctx.getSource())),
                                        FloatArgumentType.getFloat(ctx, "value"))))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0f, 100f))
                                        .executes(ctx -> setSanity(ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                FloatArgumentType.getFloat(ctx, "value"))))))
                .then(Commands.literal("add")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(-100f, 100f))
                                .executes(ctx -> addSanity(ctx.getSource(),
                                        Collections.singleton(requireSourcePlayer(ctx.getSource())),
                                        FloatArgumentType.getFloat(ctx, "value"))))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", FloatArgumentType.floatArg(-100f, 100f))
                                        .executes(ctx -> addSanity(ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                FloatArgumentType.getFloat(ctx, "value"))))))
                .then(Commands.literal("get")
                        .executes(ctx -> getSanity(ctx.getSource(),
                                Collections.singleton(requireSourcePlayer(ctx.getSource()))))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> getSanity(ctx.getSource(),
                                        EntityArgument.getPlayers(ctx, "targets")))))
                .then(Commands.literal("link")
                        .requires(stack -> stack.hasPermission(2))
                        .executes(ctx -> linkInfo(ctx.getSource(),
                                Collections.singleton(requireSourcePlayer(ctx.getSource()))))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> linkInfo(ctx.getSource(),
                                        EntityArgument.getPlayers(ctx, "targets")))))
                .then(Commands.literal("config")
                        .then(Commands.literal("reload")
                                .executes(ctx -> reloadConfig(ctx.getSource())))));
    }

    // Debug commands. They work on the user-facing S (0..100, 100 = clear-headed), write the independent
    // sanity value directly (no Depression values involved) and push to the client right away.

    /** The executor of {@code /sanity get|set|add} without targets; console, command blocks and non-player entities get a clear error. */
    private static ServerPlayer requireSourcePlayer(CommandSourceStack stack) throws CommandSyntaxException
    {
        if (stack.getEntity() instanceof ServerPlayer player)
            return player;
        throw ERROR_SOURCE_NEEDS_PLAYER.create();
    }

    private static int getSanity(CommandSourceStack stack, Collection<? extends ServerPlayer> targets)
    {
        int last = 0;
        for (ServerPlayer player : targets)
        {
            Sanity s = SanityHolder.get(player);
            if (s == null) continue;
            float points = s.getSanityPoints();
            last = Math.round(points);
            stack.sendSuccess(() -> Component.translatable("commands.sanity.get.success", player.getDisplayName(), String.format("%.2f", points)), false);
        }
        return last;
    }

    private static int setSanity(CommandSourceStack stack, Collection<? extends ServerPlayer> targets, float value)
    {
        for (ServerPlayer player : targets)
        {
            Sanity s = SanityHolder.get(player);
            if (s == null) continue;
            s.setSanityPoints(value);
            SanitySync.pushNow(player, s);
        }
        if (targets.size() == 1)
            stack.sendSuccess(() -> Component.translatable("commands.sanity.set.success.single", targets.iterator().next().getDisplayName(), value), true);
        else
            stack.sendSuccess(() -> Component.translatable("commands.sanity.set.success.multiple", value, targets.size()), true);
        return (int) value;
    }

    private static int addSanity(CommandSourceStack stack, Collection<? extends ServerPlayer> targets, float value)
    {
        for (ServerPlayer player : targets)
        {
            Sanity s = SanityHolder.get(player);
            if (s == null) continue;
            s.setSanityPoints(s.getSanityPoints() + value);
            SanitySync.pushNow(player, s);
        }
        if (targets.size() == 1)
            stack.sendSuccess(() -> Component.translatable("commands.sanity.add.success.single", value, targets.iterator().next().getDisplayName()), true);
        else
            stack.sendSuccess(() -> Component.translatable("commands.sanity.add.success.multiple", targets.size(), value), true);
        return (int) value;
    }

    /**
     * Read-only report of the 2A coupling of the targets (mood, sanity, the three multipliers, what stops the base
     * recovery, PTSD level and the episode ledger). It never advances the PTSD window, never touches the ledger and
     * never changes sanity, so asking as often as you like cannot change what happens next.
     */
    private static int linkInfo(CommandSourceStack stack, Collection<? extends ServerPlayer> targets)
    {
        int shown = 0;
        for (ServerPlayer player : targets)
        {
            Sanity s = SanityHolder.get(player);
            if (s == null) continue;
            ResourceLocation dim = player.level().dimension().location();
            Component name = player.getDisplayName();

            double e = DepressionLink.readEmotion(player);
            String mood = Double.isNaN(e) ? "n/a" : String.format("%.2f", e);
            String points = String.format("%.2f", s.getSanityPoints());
            String recovery = String.format("%.2f", MoodCoupling.baseRecoveryMultiplier(player, dim));
            String fade = String.format("%.2f", MoodCoupling.stabilizeMultiplier(player));
            String damage = String.format("%.2f", MoodCoupling.damageMultiplier(player));
            String hook = DepressionLink.isModifiersRegistered() ? "connected" : "not connected";
            stack.sendSuccess(() -> Component.translatable("commands.sanity.link.mood", name, mood, points, recovery, fade, damage, hook), false);

            String block = switch (SanityRecovery.baseRecoveryBlock(player, dim))
            {
                case SanityRecovery.BLOCK_NONE -> "runs";
                case SanityRecovery.BLOCK_NOT_CALM -> "stopped (no Depression state, combat, player event or PTSD countdown)";
                default -> "stopped (too dark)";
            };
            String campfire = String.valueOf(s.isCampfireNear());
            stack.sendSuccess(() -> Component.translatable("commands.sanity.link.gate", name, block, campfire), false);

            long mentalTicks = DepressionLink.readMentalTickCount(player);
            String max = "n/a";
            String level = "n/a";
            if (mentalTicks >= 0L)
            {
                double ptsd = DepressionLink.readCurrentMaxPtsd(player);
                max = String.format("%.2f", ptsd);
                level = String.valueOf(PtsdEdgeTracker.levelOf(ptsd));
            }
            PtsdEdgeTracker edge = s.getPtsdEdge();
            String paid = edge.isArmed() ? String.valueOf(edge.getBilledLevel()) : "baseline pending";
            String calm = edge.getCalmSamples() + " of " + ConfigProxy.getPtsdRearmSeconds(dim);
            String maxText = max;
            String levelText = level;
            stack.sendSuccess(() -> Component.translatable("commands.sanity.link.ptsd", name, maxText, levelText, paid, calm), false);

            MedicineSanityCoupling.MedicineDiag med = MedicineSanityCoupling.describeMedicineReadOnly(player);
            stack.sendSuccess(() -> Component.translatable("commands.sanity.link.medicine",
                    name, med.stateText(), med.illnessText(), med.stepText(), med.capText(), med.lastGainText()), false);

            MedicineSanityCoupling.OverdoseDiag od = MedicineSanityCoupling.describeOverdoseReadOnly(player);
            stack.sendSuccess(() -> Component.translatable("commands.sanity.link.overdose",
                    name, od.odCountText(), od.sinceLastOdText(), od.pauseRemainingText(), od.lastPenaltyText()), false);

            MedicineSanityCoupling.SleepDiag sleep = MedicineSanityCoupling.describeSleepReadOnly(player, s);
            stack.sendSuccess(() -> Component.translatable("commands.sanity.link.sleep",
                    name, sleep.statusText(), sleep.amountText(), sleep.sleepTicksText(),
                    sleep.minSleepTicksText(), sleep.awardedText(), sleep.lastAwardText()), false);
            ++shown;
        }
        return shown;
    }

    /**
     * Reloads the dimension overrides. A reload whose [sanity.link] values are rejected leaves the previous
     * overrides in place and reports the reason.
     */
    private static int reloadConfig(CommandSourceStack stack) throws CommandSyntaxException
    {
        DimensionConfig.Snapshot before = DimensionConfig.snapshot();
        try
        {
            DimensionConfig.init();
            ConfigManager.validateLinkConfig();
        }
        catch (LinkConfigSpec.LinkConfigException e)
        {
            DimensionConfig.restore(before);
            SanityMod.LOGGER.error("[{}] dimension config reload rejected, the previous overrides stay active", SanityMod.MOD_ID, e);
            throw new SimpleCommandExceptionType(Component.literal("Dimension config reload rejected, the previous overrides stay active: " + e.getMessage())).create();
        }
        // Only an accepted reload resets the warn-once memo. The rejected branch above restored the previous
        // overrides, so the previous warnings are still the truthful ones and must not be re-armed.
        ConfigProxy.onConfigAccepted();
        stack.sendSuccess(() -> Component.translatable("commands.sanity.config.reload"), true);
        return 1;
    }
}
