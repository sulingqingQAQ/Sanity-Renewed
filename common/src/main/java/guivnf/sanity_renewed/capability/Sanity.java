package guivnf.sanity_renewed.capability;

import guivnf.sanity_renewed.ActiveSanitySources;
import guivnf.sanity_renewed.recovery.PtsdEdgeTracker;
import guivnf.sanity_renewed.util.MathHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-player sanity state of the Depression addon.
 *
 * <p><b>Units.</b> {@link #getSanity()} / {@link #setSanity(float)} work on the internal
 * <i>insanity</i> {@code i} in [0, 1] (0 = clear-headed, 1 = fully insane). The user-facing value is
 * {@code S = 100 * (1 - i)} (100 = clear-headed), see {@link #getSanityPoints()}.
 * This value is independent from Depression's mental health {@code h} and mood {@code e}: it is never
 * derived from them nor written back to them.
 *
 * <p><b>Persistence.</b> Own player NBT sub-tag {@code sanity_renewed:sanity} (see {@code MixinPlayer}).
 *
 * <p><b>Sync.</b> Server side only keeps bookkeeping (see {@link #pollSync(int)}); a packet is never sent
 * unconditionally per tick.
 */
public class Sanity implements ISanity, IPassiveSanity, IPersistentSanity
{
    /** Version of the {@code sanity_renewed:sanity} NBT layout (key {@code sanity.schema}). */
    public static final int NBT_SCHEMA = 1;

    /** Regular sync: at most one packet per this many ticks per player. */
    public static final int SYNC_MIN_INTERVAL_TICKS = 20;
    /** Regular sync: the value must have really moved by at least this much (insanity units, 0.05 S). */
    public static final float SYNC_MIN_DELTA = 0.0005f;
    /** A jump of at least this much (insanity units, 5 S) is pushed without waiting for the interval. */
    public static final float SYNC_BIG_JUMP = 0.05f;

    /** S = 100 * (1 - i). */
    public static float toPoints(float insanity)
    {
        return 100f * (1f - insanity);
    }

    /** i = 1 - S / 100. */
    public static float toInsanity(float points)
    {
        return 1f - points / 100f;
    }

    // server-side sync bookkeeping, never persisted
    private boolean m_syncNow = true;
    private float m_lastSentVal = Float.NaN;
    private int m_lastSyncTick;
    // server-side: player tickCount at which one extra forced packet is due (-1 = none), see requestSyncIn
    private int m_delayedSyncAt = -1;
    // server-side recovery cache (lit campfire nearby), never persisted
    private boolean m_campfireNear;
    private int m_campfireScanTick = Integer.MIN_VALUE;
    // server-side PTSD episode bookkeeping (PtsdSanityPenalty), never persisted and not copied on clone: a new player object starts a fresh baseline
    private final PtsdEdgeTracker m_ptsdEdge = new PtsdEdgeTracker();
    // client-side: player.tickCount when the last sync packet was applied (-1 = never)
    private int m_clientRecvTick = -1;

    private int m_emAngerTimer;
    private int m_garlandTimer;
    private int m_innerEntityKills;
    private int m_innerEntityKillDecay;
    private int m_sleepTicks;
    private int m_sleepGrace;
    private long m_sleepStartTime;
    private boolean m_sleepAwarded;
    private float m_sanityVal;
    private float m_passive;
    private Vec3 m_stuckMultiplier = Vec3.ZERO;

    private final int[] m_cds = new int[ActiveSanitySources.AMOUNT];
    private final Map<Integer, Integer> m_itemCds = new HashMap<>();
    private final Map<Integer, Integer> m_brokenBlocksCds = new HashMap<>();

    public Sanity()
    {
    }

    @Override
    public void serializeNBT(CompoundTag tag)
    {
        tag.putInt("sanity.schema", NBT_SCHEMA);
        tag.putFloat("sanity.sanity", m_sanityVal);
        tag.putInt("sanity.ender_man_anger_timer", m_emAngerTimer);
        tag.putInt("sanity.garland_timer", m_garlandTimer);
        tag.putInt("sanity.inner_entity_kills", m_innerEntityKills);
        tag.putInt("sanity.inner_entity_kill_decay", m_innerEntityKillDecay);

        // TODO: do lazy serialization instead
        tag.putInt("sanity.sleeping", m_cds[ActiveSanitySources.SLEEPING]);
        tag.putInt("sanity.baby_chicken_spawn", m_cds[ActiveSanitySources.SPAWNING_BABY_CHICKEN]);
        tag.putInt("sanity.animal_breeding", m_cds[ActiveSanitySources.BREEDING_ANIMALS]);
        tag.putInt("sanity.villager_trade", m_cds[ActiveSanitySources.VILLAGER_TRADE]);
        tag.putInt("sanity.shearing", m_cds[ActiveSanitySources.SHEARING]);
        tag.putInt("sanity.eating", m_cds[ActiveSanitySources.EATING]);
        tag.putInt("sanity.fishing", m_cds[ActiveSanitySources.FISHING]);
        tag.putInt("sanity.potting_flower", m_cds[ActiveSanitySources.POTTING_FLOWER]);

        serializeItemCds(tag);
        serializeBrokenBlocksCds(tag);
    }

    @Override
    public void deserializeNBT(CompoundTag tag)
    {
        setSanity(tag.getFloat("sanity.sanity"));
        setEnderManAngerTimer(tag.getInt("sanity.ender_man_anger_timer"));
        setGarlandTimer(tag.getInt("sanity.garland_timer"));
        setInnerEntityKills(tag.getInt("sanity.inner_entity_kills"));
        setInnerEntityKillDecay(tag.getInt("sanity.inner_entity_kill_decay"));

        m_cds[ActiveSanitySources.SLEEPING] = tag.getInt("sanity.sleeping");
        m_cds[ActiveSanitySources.SPAWNING_BABY_CHICKEN] = tag.getInt("sanity.baby_chicken_spawn");
        m_cds[ActiveSanitySources.BREEDING_ANIMALS] = tag.getInt("sanity.animal_breeding");
        m_cds[ActiveSanitySources.VILLAGER_TRADE] = tag.getInt("sanity.villager_trade");
        m_cds[ActiveSanitySources.SHEARING] = tag.getInt("sanity.shearing");
        m_cds[ActiveSanitySources.EATING] = tag.getInt("sanity.eating");
        m_cds[ActiveSanitySources.FISHING] = tag.getInt("sanity.fishing");
        m_cds[ActiveSanitySources.POTTING_FLOWER] = tag.getInt("sanity.potting_flower");

        deserializeItemCds(tag);
        deserializeBrokenBlocksCooldowns(tag);
    }

    public void serialize(FriendlyByteBuf buf)
    {
        buf.writeFloat(m_sanityVal);
        buf.writeFloat(m_passive);
    }

    public void deserialize(FriendlyByteBuf buf)
    {
        m_sanityVal = buf.readFloat();
        m_passive = buf.readFloat();
    }

    /** Internal insanity i in [0, 1]. */
    @Override
    public float getSanity()
    {
        return m_sanityVal;
    }

    /**
     * Raw write of the internal insanity i (clamped to [0, 1], non-finite input is ignored).
     * Does not touch the packet bookkeeping except for flagging a big jump.
     *
     * <p>Allowed callers: {@code SanityRecovery}, {@code PtsdSanityPenalty}, the debug commands, NBT loading, the client sync
     * receiver and the clone copy. Gameplay sources must not call this directly, they are closed
     * centrally by {@code SanitySourceGate}.
     */
    @Override
    public void setSanity(float value)
    {
        if (!Float.isFinite(value))
            return;
        float v = MathHelper.clampNorm(value);
        if (v == m_sanityVal)
            return;
        m_sanityVal = v;
        // NaN lastSent (never sent) compares false here; the first sync is already forced by m_syncNow
        if (Math.abs(v - m_lastSentVal) >= SYNC_BIG_JUMP)
            m_syncNow = true;
    }

    /** User-facing sanity S in [0, 100] (100 = clear-headed). */
    public float getSanityPoints()
    {
        return toPoints(m_sanityVal);
    }

    /** Raw write by user-facing S (clamped to [0, 100]). */
    public void setSanityPoints(float points)
    {
        if (!Float.isFinite(points))
            return;
        setSanity(toInsanity(MathHelper.clamp(points, 0f, 100f)));
    }

    @Override
    public float getPassiveIncrease()
    {
        return m_passive;
    }

    /** HUD arrow only: insanity change per tick of the most recent tick (negative = recovering). */
    @Override
    public void setPassiveIncrease(float value)
    {
        m_passive = value;
    }

    /** Server: ask for a packet on the next tick, ignoring the regular interval. */
    public void requestSync()
    {
        m_syncNow = true;
    }

    /**
     * Server: ask for exactly one extra forced packet once the player's {@code tickCount} reaches
     * {@code tickCount + delayTicks}. One-shot, not periodic; used after joining so that a first packet that
     * arrived before the client player object was ready (or got lost) is repeated once.
     */
    public void requestSyncIn(int tickCount, int delayTicks)
    {
        m_delayedSyncAt = tickCount + Math.max(delayTicks, 1);
    }

    /**
     * Server: decides whether a sync packet is due now and, if so, records it as sent.
     *
     * <p>Rules: a forced request (join, respawn copy, dimension change, debug write, big jump) is always
     * due, and so is the single delayed join repeat set by {@link #requestSyncIn}. Otherwise a packet is due
     * only if at least {@link #SYNC_MIN_INTERVAL_TICKS} ticks passed since the previous one <b>and</b> the
     * value really moved by {@link #SYNC_MIN_DELTA} (or reached 0 / 1).
     * So there is never a packet for an unchanged value and never more than one per second on the regular path.
     */
    public boolean pollSync(int tickCount)
    {
        boolean due = m_syncNow || (m_delayedSyncAt >= 0 && tickCount >= m_delayedSyncAt);
        if (!due && m_sanityVal != m_lastSentVal && tickCount - m_lastSyncTick >= SYNC_MIN_INTERVAL_TICKS)
        {
            due = Math.abs(m_sanityVal - m_lastSentVal) >= SYNC_MIN_DELTA || m_sanityVal <= 0f || m_sanityVal >= 1f;
        }
        if (due)
            markSynced(tickCount);
        return due;
    }

    /** Server: records that the current state was just pushed to the client. */
    public void markSynced(int tickCount)
    {
        m_syncNow = false;
        if (m_delayedSyncAt >= 0 && tickCount >= m_delayedSyncAt)
            m_delayedSyncAt = -1; // the delayed repeat has been served (by this packet)
        m_lastSentVal = m_sanityVal;
        m_lastSyncTick = tickCount;
    }

    /** Client: remember when the last sync packet arrived (see {@code SanityClientState}). */
    public void setClientReceivedTick(int tickCount)
    {
        m_clientRecvTick = tickCount;
    }

    public int getClientReceivedTick()
    {
        return m_clientRecvTick;
    }

    public boolean isCampfireNear()
    {
        return m_campfireNear;
    }

    public void setCampfireNear(boolean value)
    {
        m_campfireNear = value;
    }

    public int getCampfireScanTick()
    {
        return m_campfireScanTick;
    }

    public void setCampfireScanTick(int value)
    {
        m_campfireScanTick = value;
    }

    public PtsdEdgeTracker getPtsdEdge()
    {
        return m_ptsdEdge;
    }

    /**
     * Copies the persistent state of {@code other} into this instance (death / end-portal clone).
     * Nothing is added or subtracted, and a packet is requested for the new client-side player.
     */
    public void copyPersistentFrom(Sanity other)
    {
        CompoundTag tag = new CompoundTag();
        other.serializeNBT(tag);
        deserializeNBT(tag);
        requestSync();
    }

    @Override
    public int[] getActiveSourcesCooldowns()
    {
        return m_cds;
    }

    @Override
    public Map<Integer, Integer> getItemCooldowns()
    {
        return m_itemCds;
    }

    @Override
    public Map<Integer, Integer> getBrokenBlocksCooldowns()
    {
        return m_brokenBlocksCds;
    }

    @Override
    public void setEnderManAngerTimer(int value)
    {
        m_emAngerTimer = value;
    }

    @Override
    public int getEnderManAngerTimer()
    {
        return m_emAngerTimer;
    }

    @Override
    public void setGarlandTimer(int value)
    {
        m_garlandTimer = value;
    }

    @Override
    public int getGarlandTimer()
    {
        return m_garlandTimer;
    }

    @Override
    public void setInnerEntityKills(int value)
    {
        m_innerEntityKills = value;
    }

    @Override
    public int getInnerEntityKills()
    {
        return m_innerEntityKills;
    }

    @Override
    public void setInnerEntityKillDecay(int value)
    {
        m_innerEntityKillDecay = value;
    }

    @Override
    public int getInnerEntityKillDecay()
    {
        return m_innerEntityKillDecay;
    }

    public void setSleepTicks(int value)
    {
        m_sleepTicks = value;
    }

    public int getSleepTicks()
    {
        return m_sleepTicks;
    }

    public void setSleepGrace(int value)
    {
        m_sleepGrace = value;
    }

    public int getSleepGrace()
    {
        return m_sleepGrace;
    }

    public void setSleepStartTime(long value)
    {
        m_sleepStartTime = value;
    }

    public long getSleepStartTime()
    {
        return m_sleepStartTime;
    }

    public void setSleepAwarded(boolean value)
    {
        m_sleepAwarded = value;
    }

    public boolean isSleepAwarded()
    {
        return m_sleepAwarded;
    }

    @Override
    public void setStuckMotionMultiplier(Vec3 multiplier)
    {
        m_stuckMultiplier = multiplier;
    }

    @Override
    public Vec3 getStuckMotionMultiplier()
    {
        return m_stuckMultiplier;
    }

    private void serializeItemCds(CompoundTag tag)
    {
        long[] itemCds = new long[m_itemCds.size()];
        int i = 0;

        for (Map.Entry<Integer, Integer> entry : m_itemCds.entrySet())
        {
            long val = entry.getKey();
            val <<= Long.SIZE / 2;
            val |= entry.getValue();

            itemCds[i] = val;

            i++;
        }

        tag.putLongArray("sanity.item_cooldowns", itemCds);
    }

    private void deserializeItemCds(CompoundTag tag)
    {
        long[] itemCds = tag.getLongArray("sanity.item_cooldowns");
        m_itemCds.clear();

        for (long itemCd : itemCds)
        {
            m_itemCds.put((int)(itemCd >> Long.SIZE / 2), (int)itemCd);
        }
    }

    private void serializeBrokenBlocksCds(CompoundTag tag)
    {
        long[] brokenBlocksCds = new long[m_brokenBlocksCds.size()];
        int i = 0;

        for (Map.Entry<Integer, Integer> entry : m_brokenBlocksCds.entrySet())
        {
            long val = entry.getKey();
            val <<= Long.SIZE / 2;
            val |= entry.getValue();

            brokenBlocksCds[i] = val;

            i++;
        }

        tag.putLongArray("sanity.broken_blocks_cooldowns", brokenBlocksCds);
    }

    private void deserializeBrokenBlocksCooldowns(CompoundTag tag)
    {
        long[] brokenBlocksCds = tag.getLongArray("sanity.broken_blocks_cooldowns");
        m_brokenBlocksCds.clear();

        for (long blockCd : brokenBlocksCds)
        {
            m_brokenBlocksCds.put((int)(blockCd >> Long.SIZE / 2), (int)blockCd);
        }
    }
}