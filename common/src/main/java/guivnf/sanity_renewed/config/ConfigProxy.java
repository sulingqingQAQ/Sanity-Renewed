package guivnf.sanity_renewed.config;

import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.Map;

public abstract class ConfigProxy
{
    public static float getPosMul(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.positive_multiplier", dim);
    }

    public static float getNegMul(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.negative_multiplier", dim);
    }

    /** Phase 1 base recovery, sanity points per second. */
    public static float getBaseRecoveryPerSecond(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.recovery.base_per_second", dim);
    }

    public static int getBaseRecoveryMinLight(ResourceLocation dim)
    {
        return ConfigManager.proxyi("sanity.recovery.base_min_light", dim);
    }

    /** Phase 1 campfire recovery, sanity points per second. */
    public static float getCampfireRecoveryPerSecond(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.recovery.campfire_per_second", dim);
    }

    public static float getCampfireRadius(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.recovery.campfire_radius", dim);
    }

    // ---- sanity.link (2A) ----

    public static boolean getLinkEnabled(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.link.link_enabled", dim);
    }

    public static boolean getMoodRecoveryEnabled(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.link.mood_recovery_enabled", dim);
    }

    public static float getMoodRecoveryE1(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.mood_recovery_e1", dim);
    }

    public static float getMoodRecoveryE2(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.mood_recovery_e2", dim);
    }

    public static float getMoodRecoveryE3(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.mood_recovery_e3", dim);
    }

    public static float getMoodRecoveryM0(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.mood_recovery_m0", dim);
    }

    public static float getMoodRecoveryM1(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.mood_recovery_m1", dim);
    }

    public static float getMoodRecoveryM2(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.mood_recovery_m2", dim);
    }

    public static float getMoodRecoveryM3(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.mood_recovery_m3", dim);
    }

    public static boolean getSanityMoodEnabled(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.link.sanity_mood_enabled", dim);
    }

    public static float getSanityS1(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.sanity_s1", dim);
    }

    public static float getSanityS2(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.sanity_s2", dim);
    }

    public static float getSanityS3(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.sanity_s3", dim);
    }

    public static float getStabilizeM0(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.stabilize_m0", dim);
    }

    public static float getStabilizeM1(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.stabilize_m1", dim);
    }

    public static float getStabilizeM2(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.stabilize_m2", dim);
    }

    public static float getStabilizeM3(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.stabilize_m3", dim);
    }

    public static float getDamageM0(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.damage_m0", dim);
    }

    public static float getDamageM1(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.damage_m1", dim);
    }

    public static float getDamageM2(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.damage_m2", dim);
    }

    public static float getDamageM3(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.damage_m3", dim);
    }

    public static boolean getPtsdEnabled(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.link.ptsd_enabled", dim);
    }

    public static float getPtsdPenalty1(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.ptsd_penalty_1", dim);
    }

    public static float getPtsdPenalty2(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.ptsd_penalty_2", dim);
    }

    public static float getPtsdPenalty3(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.ptsd_penalty_3", dim);
    }

    public static float getPtsdPenalty4(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.link.ptsd_penalty_4", dim);
    }

    public static int getPtsdRearmSeconds(ResourceLocation dim)
    {
        return ConfigManager.proxyi("sanity.link.ptsd_rearm_seconds", dim);
    }

    public static boolean getPtsdLog(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.link.ptsd_log", dim);
    }

    /** Totals of the PTSD penalty: index 0 = 0, index k = S points paid in total once an episode reaches level k. */
    public static float[] getPtsdPenaltyTable(ResourceLocation dim)
    {
        return new float[] { 0f, getPtsdPenalty1(dim), getPtsdPenalty2(dim), getPtsdPenalty3(dim), getPtsdPenalty4(dim) };
    }

    public static float getPassive(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.passive.passive", dim);
    }

    public static float getRaining(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.passive.raining", dim);
    }

    public static boolean getAffectInWater(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.passive.affect_in_water", dim);
    }

    public static boolean getAffectThirsty(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.passive.affect_thirsty", dim);
    }

    public static int getThirstyThreshold(ResourceLocation dim)
    {
        return ConfigManager.proxyi("sanity.passive.thirsty_threshold", dim);
    }

    public static float getThirsty(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.passive.thirsty", dim);
    }

    public static boolean getAffectHot(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.passive.affect_hot", dim);
    }

    public static float getHot(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.passive.hot", dim);
    }

    public static boolean getAffectCold(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.passive.affect_cold", dim);
    }

    public static float getCold(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.passive.cold", dim);
    }

    public static int getHungerThreshold(ResourceLocation dim)
    {
        return ConfigManager.proxyi("sanity.passive.hunger_threshold", dim);
    }

    public static float getHungry(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.passive.hungry", dim);
    }

    public static float getEnderManAnger(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.passive.ender_man_anger", dim);
    }

    public static float getPet(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.passive.pet", dim);
    }

    public static float getMonster(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.passive.monster", dim);
    }

    public static float getDarkness(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.passive.darkness", dim);
    }

    public static int getDarknessThreshold(ResourceLocation dim)
    {
        return ConfigManager.proxyi("sanity.passive.darkness_threshold", dim);
    }

    public static float getLightness(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.passive.lightness", dim);
    }

    public static int getLightnessThreshold(ResourceLocation dim)
    {
        return ConfigManager.proxyi("sanity.passive.lightness_threshold", dim);
    }

    public static float getBlockStuck(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.passive.block_stuck", dim);
    }

    public static float getDirtPath(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.passive.dirt_path", dim);
    }

    public static float getJukeboxPleasant(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.passive.jukebox_pleasant", dim);
    }

    public static float getJukeboxUnsettling(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.passive.jukebox_unsettling", dim);
    }

    public static List<ConfigPassiveBlock> getPassiveBlocks(ResourceLocation dim)
    {
        return ConfigManager.proxy("sanity.passive.blocks", dim);
    }

    public static float getSleeping(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.active.sleeping", dim);
    }

    public static float getHurtRatio(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.active.hurt_ratio", dim);
    }

    public static float getBabyChickenSpawning(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.active.baby_chicken_spawn", dim);
    }

    public static int getBabyChickenSpawningCooldown(ResourceLocation dim)
    {
        return ConfigManager.proxyd2i("sanity.active.baby_chicken_spawn_cd", dim);
    }

    public static float getAdvancement(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.active.advancement", dim);
    }

    public static float getAnimalBreeding(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.active.animal_breeding", dim);
    }

    public static int getAnimalBreedingCooldown(ResourceLocation dim)
    {
        return ConfigManager.proxyd2i("sanity.active.animal_breeding_cd", dim);
    }

    public static float getAnimalHurtRatio(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.active.animal_hurt_ratio", dim);
    }

    public static float getPetDeath(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.active.pet_death", dim);
    }

    public static float getVillagerTrade(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.active.villager_trade", dim);
    }

    public static int getVillagerTradeCooldown(ResourceLocation dim)
    {
        return ConfigManager.proxyd2i("sanity.active.villager_trade_cd", dim);
    }

    public static float getShearing(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.active.shearing", dim);
    }

    public static int getShearingCooldown(ResourceLocation dim)
    {
        return ConfigManager.proxyd2i("sanity.active.shearing_cd", dim);
    }

    public static float getEating(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.active.eating", dim);
    }

    public static int getEatingCooldown(ResourceLocation dim)
    {
        return ConfigManager.proxyd2i("sanity.active.eating_cd", dim);
    }

    public static float getFishing(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.active.fishing", dim);
    }

    public static int getFishingCooldown(ResourceLocation dim)
    {
        return ConfigManager.proxyd2i("sanity.active.fishing_cd", dim);
    }

    public static float getFarmlandTrample(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.active.farmland_trample", dim);
    }

    public static float getInnerEntityKill(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.active.inner_entity_kill", dim);
    }

    public static float getPottingFlower(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.active.potting_flower", dim);
    }

    public static int getPottingFlowerCooldown(ResourceLocation dim)
    {
        return ConfigManager.proxyd2i("sanity.active.potting_flower_cd", dim);
    }

    public static float getChangedDimension(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.active.changed_dimension", dim);
    }

    public static float getStruckByLightning(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.active.struck_by_lightning", dim);
    }

    public static List<ConfigItem> getItems(ResourceLocation dim)
    {
        return ConfigManager.proxy("sanity.active.items", dim);
    }

    public static List<ConfigItemCategory> getItemCats(ResourceLocation dim)
    {
        return ConfigManager.proxy("sanity.active.item_categories", dim);
    }

    public static Map<Integer, ConfigItemCategory> getIdToItemCat(ResourceLocation dim)
    {
        return ConfigManager.getIdToItemCat(dim);
    }

    public static List<ConfigBrokenBlock> getBrokenBlocks(ResourceLocation dim)
    {
        return ConfigManager.proxy("sanity.active.broken_blocks", dim);
    }

    public static List<ConfigBrokenBlockCategory> getBrokenBlockCats(ResourceLocation dim)
    {
        return ConfigManager.proxy("sanity.active.broken_block_categories", dim);
    }

    public static Map<Integer, ConfigBrokenBlockCategory> getIdToBrokenBlockCat(ResourceLocation dim)
    {
        return ConfigManager.getIdToBrokenBlockCat(dim);
    }

    public static float getSanePlayerCompany(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.multiplayer.sane_player_company", dim);
    }

    public static float getInsanePlayerCompany(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.multiplayer.insane_player_company", dim);
    }

    public static boolean getSaneSeeInnerEntities(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.entity.sane_see_inner_entities", dim);
    }

    public static boolean getRenderIndicator(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.client.indicator.render", dim);
    }

    public static boolean getTwitchIndicator(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.client.indicator.twitch", dim);
    }

    public static float getIndicatorScale(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.client.indicator.scale", dim);
    }

    public static SanityIndicatorLocation getIndicatorLocation(ResourceLocation dim)
    {
        return ConfigManager.proxy("sanity.client.indicator.location", dim);
    }

    public static boolean getRenderHint(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.client.hints.render", dim);
    }

    public static boolean getTwitchHint(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.client.hints.twitch", dim);
    }

    public static boolean getRenderBtOverlay(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.client.blood_tendrils.render", dim);
    }

    public static boolean getFlashBtOnShortBurst(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.client.blood_tendrils.short_burst_flash", dim);
    }

    public static boolean getRenderBtPassive(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.client.blood_tendrils.render_passive", dim);
    }

    public static boolean getRenderPost(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.client.render_post", dim);
    }

    public static boolean getPlaySounds(ResourceLocation dim)
    {
        return ConfigManager.proxyb("sanity.client.play_sounds", dim);
    }

    public static float getInsanityVolume(ResourceLocation dim)
    {
        return ConfigManager.proxyd2f("sanity.client.insanity_volume", dim);
    }
}