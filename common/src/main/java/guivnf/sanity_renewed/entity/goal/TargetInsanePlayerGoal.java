package guivnf.sanity_renewed.entity.goal;

import guivnf.sanity_renewed.SanityProcessor;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class TargetInsanePlayerGoal extends TargetGoal
{
    private final float m_sanityThreshold;
    private boolean m_alertSameType;
    @Nullable
    private Class<?>[] m_toIgnoreAlert;
    private Player m_insanePlayer;

    public TargetInsanePlayerGoal(Mob mob, boolean mustSee, float sanityThreshold)
    {
        super(mob, mustSee);
        setFlags(EnumSet.of(Goal.Flag.TARGET));
        m_sanityThreshold = sanityThreshold;
    }

    public TargetInsanePlayerGoal(Mob mob, boolean mustSee)
    {
        this(mob, mustSee, -1f);
    }

    @Override
    public boolean canUse()
    {
        return (m_insanePlayer = getMostInsanePlayer()) != null;
    }

    @Override
    public boolean canContinueToUse()
    {
        return SanityProcessor.isValidInsaneTarget(mob.getTarget(), m_sanityThreshold) && super.canContinueToUse();
    }

    @Override
    public void start()
    {
        Player target = m_insanePlayer;
        if (target != null)
        {
            mob.setTarget(target);
            targetMob = mob.getTarget();
            if (m_alertSameType)
                alertOthers();
        }
        super.start();
    }

    public TargetInsanePlayerGoal setAlertOthers(Class<?>... toIgnore)
    {
        m_alertSameType = true;
        m_toIgnoreAlert = toIgnore;
        return this;
    }

    private Player getMostInsanePlayer()
    {
        // Phase 3: bounded by the mob's own follow range instead of a whole-level scan, so an inner
        // entity can no longer lock onto a player on the other side of the world.
        double followRange = getFollowDistance();
        return m_sanityThreshold < 0f
                ? SanityProcessor.getMostInsanePlayer(mob, SanityProcessor.SANITY_TARGET_THRESHOLD, followRange)
                : SanityProcessor.getMostInsanePlayer(mob, m_sanityThreshold, followRange);
    }

    protected void alertOthers()
    {
        double range = getFollowDistance();
        AABB box = AABB.unitCubeFromLowerCorner(mob.position()).inflate(range, 10.0D, range);
        List<? extends Mob> peers = mob.level().getEntitiesOfClass(mob.getClass(), box, EntitySelector.NO_SPECTATORS);
        for (Mob peer : peers)
        {
            if (peer == mob || peer.getTarget() != null) continue;
            if (mob instanceof TamableAnimal ta && peer instanceof TamableAnimal pa
                    && ta.getOwnerUUID() != null && !ta.getOwnerUUID().equals(pa.getOwnerUUID()))
                continue;
            if (peer.isAlliedTo(mob.getTarget())) continue;
            boolean ignore = false;
            if (m_toIgnoreAlert != null)
            {
                for (Class<?> c : m_toIgnoreAlert)
                {
                    if (peer.getClass() == c) { ignore = true; break; }
                }
            }
            if (!ignore)
                alertOther(peer, mob.getTarget());
        }
    }

    protected void alertOther(Mob mob, LivingEntity target)
    {
        mob.setTarget(target);
    }
}
