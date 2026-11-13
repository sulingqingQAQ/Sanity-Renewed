package guivnf.sanity_renewed.mixin;

import guivnf.sanity_renewed.recovery.DepressionLink;
import guivnf.sanity_renewed.recovery.MedicineSanityCoupling;
import guivnf.sanity_renewed.recovery.MedicineSanityTracker.MedicineEffectType;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectInstance.class)
public abstract class MixinMobEffectInstance
{
    @Shadow
    public abstract Holder<MobEffect> getEffect();

    @Shadow
    public abstract int getAmplifier();

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/effect/MobEffect;applyEffectTick(Lnet/minecraft/world/entity/LivingEntity;I)Z",
                    shift = At.Shift.AFTER))
    private void sanityRenewed$afterEffectTick(
            LivingEntity entity,
            Runnable onExpiration,
            CallbackInfoReturnable<Boolean> cir)
    {
        if (!(entity instanceof ServerPlayer player))
            return;

        Holder<MobEffect> effect = getEffect();
        if (effect.is(DepressionLink.ANTI_DEPRESSION_ID))
        {
            MedicineSanityCoupling.recordMedicineEffectTick(
                    player, MedicineEffectType.ANTI_DEPRESSION, getAmplifier(), -1);
        }
        else if (effect.is(DepressionLink.ANTI_MANIA_ID))
        {
            MedicineSanityCoupling.recordMedicineEffectTick(
                    player,
                    MedicineEffectType.ANTI_MANIA,
                    getAmplifier(),
                    antiDepressionAmplifierAtThisTick(player));
        }
    }

    private static int antiDepressionAmplifierAtThisTick(ServerPlayer player)
    {
        for (MobEffectInstance activeEffect : player.getActiveEffects())
        {
            if (activeEffect.getEffect().is(DepressionLink.ANTI_DEPRESSION_ID))
                return activeEffect.getAmplifier();
        }
        return -1;
    }
}
