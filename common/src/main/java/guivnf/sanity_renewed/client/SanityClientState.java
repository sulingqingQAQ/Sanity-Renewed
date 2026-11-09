package guivnf.sanity_renewed.client;

import guivnf.sanity_renewed.SanityMod;
import guivnf.sanity_renewed.capability.Sanity;
import guivnf.sanity_renewed.capability.SanityHolder;
import guivnf.sanity_renewed.config.ConfigProxy;
import guivnf.sanity_renewed.sound.SanitySoundInstances;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Per-tick view of the local player's sanity for the client. Owns the transient,
 * decorative state that drives the indicator / hints / blood-tendrils overlay so the
 * render layer can stay stateless.
 *
 * <p>Updated from {@link ClientEventHandler}'s tick callback.
 */
public final class SanityClientState
{
    public static final SanityClientState INSTANCE = new SanityClientState();

    private static final MutableComponent[] LOW_HINTS = new MutableComponent[12];
    private static final MutableComponent[] HIGH_HINTS = new MutableComponent[9];
    static
    {
        for (int i = 0; i < LOW_HINTS.length; i++)
            LOW_HINTS[i] = Component.translatable("gui." + SanityMod.MOD_ID + ".hint0" + i);
        for (int i = 0; i < HIGH_HINTS.length; i++)
            HIGH_HINTS[i] = Component.translatable("gui." + SanityMod.MOD_ID + ".hint1" + i);
    }

    private static final float PASSIVE_THRESHOLD = .0002f;
    /** Slightly above the longest regular sync gap (one packet per 20 ticks, +1 for rounding). */
    private static final int PASSIVE_STALE_TICKS = 30;
    private static final float BT_DELAY = 5f * 20;

    private final RandomSource rand = RandomSource.create();

    private float sanity;
    private float prevSanity;
    private float passiveIncrease;
    private float lastDelta;

    // indicator decoration
    private int   jitterX;
    private int   jitterY;
    private float flashTimer;
    private float flashGain;
    private float arrowTimer;

    // hint decoration
    private MutableComponent currentHint;
    private float hintCooldown;
    private float hintDuration;
    private float hintMaxDuration;
    private int   hintJitterX;
    private int   hintJitterY;

    // blood tendrils
    private float btAlpha;
    private float btTargetAlpha;
    private float btDelay;
    private float btSineTime;

    private SanityClientState() {}

    public float sanity()              { return sanity; }
    public float passiveIncrease()     { return passiveIncrease; }
    public float lastDelta()           { return lastDelta; }
    public float flashTimer()          { return flashTimer; }
    public float flashGain()           { return flashGain; }
    public float arrowTimer()          { return arrowTimer; }
    public int   indicatorJitterY()    { return jitterY; }
    public MutableComponent hint()     { return currentHint; }
    public float hintTimer()           { return hintDuration; }
    public float hintMaxTimer()        { return hintMaxDuration; }
    public int   hintJitterX()         { return hintJitterX; }
    public int   hintJitterY()         { return hintJitterY; }
    public float bloodTendrilAlpha()   { return btAlpha; }
    public static float passiveThreshold() { return PASSIVE_THRESHOLD; }

    public void onLevelChange()
    {
        sanity = prevSanity = passiveIncrease = lastDelta = 0f;
        jitterX = jitterY = hintJitterX = hintJitterY = 0;
        flashTimer = flashGain = arrowTimer = 0f;
        currentHint = null;
        hintCooldown = hintDuration = hintMaxDuration = 0f;
        btAlpha = btTargetAlpha = btDelay = btSineTime = 0f;
    }

    public void tick(float dt)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused() || player.isCreative() || player.isSpectator())
            return;

        Sanity s = SanityHolder.get(player);
        if (s == null) return;

        sanity = s.getSanity();
        // The server only sends on real change (at most once a second), so the passive arrow value is only
        // trusted for a short while after a packet; once recovery stops, no more packets arrive.
        int received = s.getClientReceivedTick();
        passiveIncrease = received >= 0 && player.tickCount - received <= PASSIVE_STALE_TICKS ? s.getPassiveIncrease() : 0f;
        lastDelta = sanity - prevSanity;

        tickIndicator(dt);
        tickHint(dt, player);
        tickBloodTendrils(dt, player);

        prevSanity = sanity;
    }

    private void tickIndicator(float dt)
    {
        if (flashTimer > 0) flashTimer -= dt;
        if (Math.abs(lastDelta) >= .01f) flashTimer = 20f;
        flashGain = flashTimer <= 0 ? 0 : flashGain + lastDelta;

        if (arrowTimer <= 0f) arrowTimer = 23.99f;
        if (passiveIncrease != 0f) arrowTimer -= dt;

        if (sanity >= .7f)
        {
            jitterX = rand.nextInt(3) - 1;
            jitterY = rand.nextInt(3) - 1;
            hintJitterX = rand.nextInt(3) - 1;
            hintJitterY = rand.nextInt(3) - 1;
        }
        else
        {
            jitterX = jitterY = hintJitterX = hintJitterY = 0;
        }
    }

    private void tickHint(float dt, LocalPlayer player)
    {
        if (sanity <= .4f) return;
        if (!ConfigProxy.getRenderHint(player.level().dimension().location())) return;

        if (hintCooldown <= 0f && hintDuration <= 0f)
        {
            boolean intense = sanity > .7f;
            MutableComponent[] pool = intense ? HIGH_HINTS : LOW_HINTS;
            int id = rand.nextInt(pool.length);
            currentHint = pool[id];
            hintCooldown = intense ? 600f : 2000f;
            hintDuration = hintMaxDuration = 199f;
            if (ConfigProxy.getPlaySounds(player.level().dimension().location()) && id == 0)
                SanitySoundInstances.playSwish();
        }
        if (hintDuration > 0f) hintDuration -= dt;
        else                   hintCooldown = Math.max(hintCooldown - dt, 0f);
    }

    private void tickBloodTendrils(float dt, LocalPlayer player)
    {
        var dim = player.level().dimension().location();
        boolean flash = ConfigProxy.getFlashBtOnShortBurst(dim);
        boolean passive = ConfigProxy.getRenderBtPassive(dim);
        if (!ConfigProxy.getRenderBtOverlay(dim) || !(flash || passive))
        {
            btAlpha = 0f;
            return;
        }

        if (lastDelta >= .002f && flash)
            btTargetAlpha = Mth.lerp(Mth.clamp(Mth.inverseLerp(lastDelta, .002f, .02f), 0f, 1f), .4f, .75f);

        if (passive)
            btDelay = Mth.clamp(btDelay + (passiveIncrease > 0f ? dt : -dt), 0f, BT_DELAY);

        if (btTargetAlpha > 0f && flash)
        {
            if (btAlpha < btTargetAlpha)
                btAlpha = Math.min(btAlpha + .5f, btTargetAlpha);
            else
                btTargetAlpha = 0f;
        }
        else if (btDelay >= BT_DELAY && passive)
        {
            if (btAlpha < .15f)
            {
                btSineTime = 0;
                btAlpha = Math.min(btAlpha + .1f, .15f);
            }
            else if (btAlpha > .3f)
            {
                btSineTime = Mth.PI / .2f;
                btAlpha = Math.max(btAlpha - .1f, .3f);
            }
            else
            {
                btAlpha = Mth.lerp((-Mth.cos(btSineTime * .2f) + 1f) * .5f, .15f, .3f);
                btSineTime += dt;
            }
        }
        else
        {
            btAlpha = Math.max(btAlpha - .1f, 0f);
        }
    }
}
