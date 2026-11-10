package guivnf.sanity_renewed.client;

import guivnf.sanity_renewed.capability.Sanity;
import guivnf.sanity_renewed.capability.SanityHolder;
import guivnf.sanity_renewed.config.ConfigProxy;
import guivnf.sanity_renewed.sound.SanitySoundInstances;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;

/**
 * Looped + jump-scare audio for the local player. Keeps the heartbeat and insanity
 * drone instances alive, smoothly fades their volume toward a target derived from
 * sanity, and sprinkles fake footsteps / hostile-ambient sounds at high sanity.
 *
 * <p>Stateful but lightweight; reset on level change.
 */
public final class SanityAmbience
{
    private static final int INSANITY_FADE_TICKS  = 20;
    private static final int HEARTBEAT_FADE_TICKS = 60;
    private static final int STEP_CD_MIN  = 600,  STEP_CD_MAX  = 1200;
    private static final int MISC_CD_MIN  = 800,  MISC_CD_MAX  = 1400;

    private static final RandomSource RAND = RandomSource.create();
    private static final SoundEvent[] MISC_SOUNDS = {
            SoundEvents.CREEPER_PRIMED,
            SoundEvents.TNT_PRIMED,
            SoundEvents.SKELETON_AMBIENT,
            SoundEvents.SKELETON_STEP,
            SoundEvents.ZOMBIE_AMBIENT,
            SoundEvents.ZOMBIE_STEP,
            SoundEvents.ENDERMAN_AMBIENT,
            SoundEvents.HOSTILE_BIG_FALL,
            SoundEvents.CHEST_OPEN,
            SoundEvents.CHEST_CLOSE,
            SoundEvents.WOODEN_DOOR_OPEN,
            SoundEvents.WOODEN_TRAPDOOR_OPEN,
            SoundEvents.WOLF_GROWL,
    };

    private static SanitySoundInstances.Insanity insanity;
    private static SanitySoundInstances.Heartbeat heartbeat;
    private static float insanityStart, heartbeatStart;
    private static float insanityTarget, heartbeatTarget;
    private static int   insanityProgress, heartbeatProgress;

    private static int stepCooldown = nextStep();
    private static int miscCooldown = nextMisc();
    private static int currentStepRemaining;
    private static SoundType currentStepSound;
    private static BlockPos  currentStepPos;

    private SanityAmbience() {}

    public static void reset()
    {
        if (insanity != null)  { insanity.stopNow();  insanity = null; }
        if (heartbeat != null) { heartbeat.stopNow(); heartbeat = null; }
        insanityStart = heartbeatStart = insanityTarget = heartbeatTarget = 0f;
        insanityProgress = heartbeatProgress = 0;
        stepCooldown = nextStep();
        miscCooldown = nextMisc();
    }

    public static void tick()
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isCreative() || player.isSpectator()
                || !ConfigProxy.getPlaySounds(player.level().dimension().location()))
        {
            if (insanity != null)  insanity.stopNow();
            if (heartbeat != null) heartbeat.stopNow();
            return;
        }

        Sanity s = SanityHolder.get(player);
        if (s == null) return;

        ensureLoops(mc);
        float sanity = s.getSanity();
        float insanityFactor  = Mth.clamp(Mth.inverseLerp(sanity, .55f, .8f), 0f, 1f);
        float heartbeatFactor = Mth.clamp(Mth.inverseLerp(sanity, .7f,  .8f), 0f, 1f);

        // Phase 3 audio boundary: Depression plays its own heartbeat/breathing during a PTSD onset.
        // Yield this addon's heartbeat then (configurable) so the two loops never overlap. The
        // insanity drone and the fake footsteps keep playing regardless.
        ResourceLocation dim = player.level().dimension().location();
        if (heartbeatFactor > 0f
                && ConfigProxy.getHeartbeatYieldToDepression(dim)
                && DepressionClientBridge.isPtsdOnsetActive())
        {
            heartbeatFactor = 0f;
        }

        crossfade(insanity, insanityFactor, true);
        crossfade(heartbeat, heartbeatFactor, false);

        insanity.setEyePos(player.getEyePosition());
        heartbeat.setEyePos(player.getEyePosition());

        playFakeFootsteps(player, sanity);
    }

    private static void ensureLoops(Minecraft mc)
    {
        if (insanity == null || insanity.isStopped())
        {
            insanity = new SanitySoundInstances.Insanity();
            mc.getSoundManager().play(insanity);
        }
        if (heartbeat == null || heartbeat.isStopped())
        {
            heartbeat = new SanitySoundInstances.Heartbeat();
            mc.getSoundManager().play(heartbeat);
        }
    }

    private static void crossfade(Object inst, float target, boolean isInsanity)
    {
        int fadeTicks = isInsanity ? INSANITY_FADE_TICKS : HEARTBEAT_FADE_TICKS;
        float current = isInsanity ? insanity.factor : heartbeat.factor;
        float prevTarget = isInsanity ? insanityTarget : heartbeatTarget;
        int progress = isInsanity ? insanityProgress : heartbeatProgress;

        if (Math.abs(target - prevTarget) >= .05f)
        {
            if (isInsanity) { insanityTarget = target; insanityStart = current; insanityProgress = 0; }
            else            { heartbeatTarget = target; heartbeatStart = current; heartbeatProgress = 0; }
        }
        else if (progress > fadeTicks)
        {
            if (isInsanity) insanity.factor = target;
            else            heartbeat.factor = target;
            return;
        }
        else
        {
            if (isInsanity) insanityTarget = target;
            else            heartbeatTarget = target;
        }

        if (isInsanity)
        {
            insanity.factor = Mth.lerp((float) insanityProgress / fadeTicks, insanityStart, insanityTarget);
            insanityProgress++;
        }
        else
        {
            heartbeat.factor = Mth.lerp((float) heartbeatProgress / fadeTicks, heartbeatStart, heartbeatTarget);
            heartbeatProgress++;
        }
    }

    private static void playFakeFootsteps(LocalPlayer player, float sanity)
    {
        if (--stepCooldown <= 0)
        {
            stepCooldown = nextStep();
            if (sanity >= .4f)
            {
                stepCooldown = (int) (stepCooldown * (1f - Mth.clamp(Mth.inverseLerp(sanity, .4f, .9f), 0f, 1f) * .4f));
                currentStepSound = player.getBlockStateOn().getSoundType();
                currentStepPos = behindPlayer(player);
                currentStepRemaining = (RAND.nextInt(3) + 2) * 7;
            }
        }
        if (--miscCooldown <= 0)
        {
            miscCooldown = nextMisc();
            if (sanity >= .4f)
            {
                miscCooldown = (int) (miscCooldown * (1f - Mth.clamp(Mth.inverseLerp(sanity, .4f, .8f), 0f, 1f) * .5f));
                SoundEvent sound = MISC_SOUNDS[RAND.nextInt(MISC_SOUNDS.length)];
                player.level().playLocalSound(behindPlayer(player), sound, SoundSource.AMBIENT, 1f, .5f, false);
            }
        }
        if (currentStepRemaining > 0)
        {
            if (currentStepRemaining % 7 == 0 && currentStepSound != null && currentStepPos != null)
            {
                player.level().playLocalSound(
                        currentStepPos,
                        currentStepSound.getStepSound(),
                        SoundSource.AMBIENT,
                        currentStepSound.getVolume() * .5f,
                        currentStepSound.getPitch(),
                        false);
            }
            currentStepRemaining--;
        }
    }

    private static BlockPos behindPlayer(LocalPlayer player)
    {
        Vec3 v = player.position().add(player.getLookAngle().scale(-2f));
        return BlockPos.containing(v);
    }

    private static int nextStep() { return RAND.nextInt(STEP_CD_MAX - STEP_CD_MIN) + STEP_CD_MIN; }
    private static int nextMisc() { return RAND.nextInt(MISC_CD_MAX - MISC_CD_MIN) + MISC_CD_MIN; }
}
