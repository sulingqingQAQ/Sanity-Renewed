package guivnf.sanity_renewed.entity;

import guivnf.sanity_renewed.entity.goal.HurtByInsanePlayerGoal;
import guivnf.sanity_renewed.entity.goal.TargetInsanePlayerGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RottingStalker extends InnerEntity implements GeoEntity
{
    private final AnimatableInstanceCache m_animCache = GeckoLibUtil.createInstanceCache(this);

    public RottingStalker(EntityType<? extends Monster> entityType, Level level)
    {
        super(entityType, level);
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0d, true));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, .4d));
        this.targetSelector.addGoal(0, new TargetInsanePlayerGoal(this, false));
        this.targetSelector.addGoal(1, new HurtByInsanePlayerGoal(this));

        super.registerGoals();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar)
    {
        controllerRegistrar.add(new AnimationController<GeoAnimatable>(this, 0, state ->
        {
            if (attackAnim > 0.0f)
                return state.setAndContinue(DefaultAnimations.ATTACK_SWING);

            RawAnimation anim = DefaultAnimations.IDLE;
            if (state.isMoving())
                // Phase 3: target state now comes from the synched entity data instead of the
                // never-written InnerEntityCapImpl, so the RUN animation actually plays while hunting.
                anim = (getSyncedTargetUuid().isEmpty() || isInWater())
                        ? DefaultAnimations.WALK
                        : DefaultAnimations.RUN;
            return state.setAndContinue(anim);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return m_animCache;
    }

    public static AttributeSupplier.Builder buildAttributes()
    {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0d)
                .add(Attributes.FOLLOW_RANGE, 128.0d)
                .add(Attributes.ATTACK_DAMAGE, 8.0d)
                .add(Attributes.MOVEMENT_SPEED, 0.42d);
    }
}
