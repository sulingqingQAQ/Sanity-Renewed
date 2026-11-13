package guivnf.sanity_renewed.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;

import java.util.ArrayList;
import java.util.List;

public class ConfigDefault
{
    public final DoubleValue m_posMul;
    public final DoubleValue m_negMul;

    public final DoubleValue m_baseRecovery;
    public final IntValue m_baseRecoveryMinLight;
    public final DoubleValue m_campfireRecovery;
    public final DoubleValue m_campfireRadius;

    public final BooleanValue m_linkEnabled;
    public final BooleanValue m_moodRecoveryEnabled;
    public final DoubleValue m_moodRecoveryE1;
    public final DoubleValue m_moodRecoveryE2;
    public final DoubleValue m_moodRecoveryE3;
    public final DoubleValue m_moodRecoveryM0;
    public final DoubleValue m_moodRecoveryM1;
    public final DoubleValue m_moodRecoveryM2;
    public final DoubleValue m_moodRecoveryM3;
    public final BooleanValue m_sanityMoodEnabled;
    public final DoubleValue m_sanityS1;
    public final DoubleValue m_sanityS2;
    public final DoubleValue m_sanityS3;
    public final DoubleValue m_stabilizeM0;
    public final DoubleValue m_stabilizeM1;
    public final DoubleValue m_stabilizeM2;
    public final DoubleValue m_stabilizeM3;
    public final DoubleValue m_damageM0;
    public final DoubleValue m_damageM1;
    public final DoubleValue m_damageM2;
    public final DoubleValue m_damageM3;
    public final BooleanValue m_ptsdEnabled;
    public final DoubleValue m_ptsdPenalty1;
    public final DoubleValue m_ptsdPenalty2;
    public final DoubleValue m_ptsdPenalty3;
    public final DoubleValue m_ptsdPenalty4;
    public final IntValue m_ptsdRearmSeconds;
    public final BooleanValue m_ptsdLog;

    public final BooleanValue m_medicineEffectEnabled;
    public final DoubleValue m_medicineSanityTier1;
    public final DoubleValue m_medicineSanityTier2;
    public final DoubleValue m_medicineSanityTier3;
    public final BooleanValue m_medicineCapByIllness;
    public final BooleanValue m_medicineApplyTraitMultiplier;
    public final BooleanValue m_overdosePenaltyEnabled;
    public final DoubleValue m_overdosePenalty1;
    public final DoubleValue m_overdosePenalty2;
    public final DoubleValue m_overdosePenalty3;
    public final IntValue m_overdosePauseMedicineTicks;
    public final BooleanValue m_sleepRestoreEnabled;
    public final DoubleValue m_sleepRestoreAmount;
    public final BooleanValue m_sleepManiaBlocksRestore;

    public final BooleanValue m_phase3InnerEntitySpawning;
    public final IntValue m_phase3SpawnFailureCooldown;
    public final IntValue m_phase3SpawnRadius;
    public final IntValue m_phase3DetectionRadius;

    public final DoubleValue m_passive;
    public final DoubleValue m_raining;
    public final BooleanValue m_affectInWater;
    public final BooleanValue m_affectThirsty;
    public final IntValue m_thirstyThreshold;
    public final DoubleValue m_thirsty;
    public final BooleanValue m_affectHot;
    public final DoubleValue m_hot;
    public final BooleanValue m_affectCold;
    public final DoubleValue m_cold;
    public final IntValue m_hungerThreshold;
    public final DoubleValue m_hungry;
    public final DoubleValue m_enderManAnger;
    public final DoubleValue m_pet;
    public final DoubleValue m_monster;
    public final DoubleValue m_darkness;
    public final IntValue m_darknessThreshold;
    public final DoubleValue m_lightness;
    public final IntValue m_lightnessThreshold;
    public final DoubleValue m_blockStuck;
    public final DoubleValue m_dirtPath;
    public final DoubleValue m_jukeboxPleasant;
    public final DoubleValue m_jukeboxUnsettling;
    public final ConfigValue<List<? extends String>> m_passiveBlocks;

    public final DoubleValue m_sleeping;
    public final DoubleValue m_hurtRatio;
    public final DoubleValue m_babyChickenSpawning;
    public final DoubleValue m_babyChickenSpawningCd;
    public final DoubleValue m_advancement;
    public final DoubleValue m_animalBreeding;
    public final DoubleValue m_animalBreedingCd;
    public final DoubleValue m_animalHurtRatio;
    public final DoubleValue m_petDeath;
    public final DoubleValue m_villagerTrade;
    public final DoubleValue m_villagerTradeCd;
    public final DoubleValue m_shearing;
    public final DoubleValue m_shearingCd;
    public final DoubleValue m_eating;
    public final DoubleValue m_eatingCd;
    public final DoubleValue m_fishing;
    public final DoubleValue m_fishingCd;
    public final DoubleValue m_farmlandTrample;
    public final DoubleValue m_innerEntityKill;
    public final DoubleValue m_pottingFlower;
    public final DoubleValue m_pottingFlowerCd;
    public final DoubleValue m_changedDimension;
    public final DoubleValue m_struckByLightning;
    public final ConfigValue<List<? extends String>> m_items;
    public final ConfigValue<List<? extends String>> m_itemCats;
    public final ConfigValue<List<? extends String>> m_brokenBlocks;
    public final ConfigValue<List<? extends String>> m_brokenBlockCats;

    public final DoubleValue m_sanePlayerCompany;
    public final DoubleValue m_insanePlayerCompany;

    public final BooleanValue m_saneSeeInnerEntities;

    public final BooleanValue m_renderIndicator;
    public final BooleanValue m_twitchIndicator;
    public final DoubleValue m_indicatorScale;
    public final EnumValue<SanityIndicatorLocation> m_indicatorLocation;

    public final BooleanValue m_renderHint;
    public final BooleanValue m_twitchHint;

    public final BooleanValue m_renderBloodTendrilsOverlay;
    public final BooleanValue m_flashBtOnShortBurst;
    public final BooleanValue m_renderBtPassive;

    public final BooleanValue m_renderPost;
    public final BooleanValue m_playSounds;
    public final DoubleValue m_insanityVolume;
    public final BooleanValue m_heartbeatYieldToDepression;

    public ConfigDefault(ModConfigSpec.Builder builder)
    {
        builder.comment(
                "Sanity configuration",
                "NOTE: all sanity values are measured in percentages (i.e. 40.0 is equal to 40% of sanity bar)",
                "NOTE: each subsequent usage of an active source or item has its effectiveness multiplied by (timeSinceLastUsage / cooldown) (capped at 1.0)",
                "NOTE (Depression addon, phase 1): the keys under <passive> and <active> (and multiplayer / inner entity kill) are INACTIVE.",
                "Sanity only recovers through the <recovery> section below, and through /sanity set|add (debug).",
                "NOTE (Depression addon, 2A): the <link> section below ties mood, sanity and PTSD together; PTSD is the only thing there that takes sanity away.")
                .push("sanity");

        m_posMul = builder
                .comment("For balancing purposes: the effectiveness of all positive sanity sources will be multiplied by this number")
                .defineInRange("positive_multiplier", 1.0, Float.MIN_VALUE, Float.MAX_VALUE);
        m_negMul = builder
                .comment("For balancing purposes: the effectiveness of all negative sanity sources will be multiplied by this number")
                .defineInRange("negative_multiplier", 1.0, Float.MIN_VALUE, Float.MAX_VALUE);

        builder.comment(
                "Depression addon, phase 1: the only sources that change sanity by themselves",
                "Values are sanity points per second (100 = clear-headed). Numbers are phase-1 defaults and still need tuning in real play").push("recovery");

        m_baseRecovery = builder
                .comment(
                        "Base recovery per second while it is safe: player alive and not creative/spectator, Depression state initialised,",
                        "not in combat and not in a Depression player event, no PTSD contact countdown running, and bright enough (see base_min_light)",
                        "Not applied when the Depression state is missing. 0 disables it")
                .defineInRange("base_per_second", .05, 0.0, 100.0);
        m_baseRecoveryMinLight = builder
                .comment("Minimum local light level (inclusive, measured at the player's eyes) for the base recovery. TO BE TUNED")
                .defineInRange("base_min_light", 10, 0, 15);
        m_campfireRecovery = builder
                .comment(
                        "Recovery per second near a lit campfire (within <campfire_radius> blocks, with line of sight)",
                        "Several campfires do not stack. Independent from the base recovery conditions. 0 disables it")
                .defineInRange("campfire_per_second", .10, 0.0, 100.0);
        m_campfireRadius = builder
                .comment("Campfire reach in blocks")
                .defineInRange("campfire_radius", 4.0, 0.5, 8.0);

        builder.pop();
        builder.comment(
                "Depression addon, 2A: how mood, sanity and PTSD affect each other. All numbers are first drafts and still need tuning in real play.",
                "Mood e runs from -20 (worst) to +20. Sanity S runs from 0 to 100 (100 = clear-headed).",
                "Per-dimension overrides of these keys are checked when they are loaded: a wrong type, an unknown key or a value outside its range stops the start-up",
                "(or makes /sanity config reload fail and keep the old overrides). There, write decimal numbers with a dot (1.0, not 1).",
                "In this file (default.toml) NeoForge itself puts a value that is out of range or of the wrong type back to its default and writes a warning to the log, so check the log after editing.",
                "Rules across several keys (the three mood thresholds and the three sanity thresholds must strictly descend, the four PTSD totals must not decrease) are checked for this file",
                "and for every dimension; breaking one stops the start-up (or fails the reload).").push("link");

        m_linkEnabled = builder
                .comment(
                        "Master switch of everything in this section. Off = mood does not slow the base recovery, sanity does not touch mood,",
                        "PTSD does not cost sanity (Depression then behaves exactly as without this addon)")
                .define("link_enabled", true);

        m_moodRecoveryEnabled = builder
                .comment("Whether a bad mood slows the BASE recovery down. The campfire recovery is never affected. Off = base recovery always runs at full speed")
                .define("mood_recovery_enabled", true);
        m_moodRecoveryE1 = builder
                .comment("Mood at or above this value: base recovery runs at full speed (mood_recovery_m0). Must be larger than e2")
                .defineInRange("mood_recovery_e1", 0.0, -20.0, 20.0);
        m_moodRecoveryE2 = builder
                .comment("Mood above this value (and below e1): speed mood_recovery_m1. Must be larger than e3")
                .defineInRange("mood_recovery_e2", -5.0, -20.0, 20.0);
        m_moodRecoveryE3 = builder
                .comment("Mood above this value (and not above e2): speed mood_recovery_m2. Mood at or below it: speed mood_recovery_m3")
                .defineInRange("mood_recovery_e3", -10.0, -20.0, 20.0);
        m_moodRecoveryM0 = builder
                .comment("Base recovery speed factor when the mood is good (1.0 = unchanged, 0 = stopped)")
                .defineInRange("mood_recovery_m0", 1.0, 0.0, 1.0);
        m_moodRecoveryM1 = builder
                .comment("Base recovery speed factor when the mood is slightly bad")
                .defineInRange("mood_recovery_m1", 0.75, 0.0, 1.0);
        m_moodRecoveryM2 = builder
                .comment("Base recovery speed factor when the mood is bad")
                .defineInRange("mood_recovery_m2", 0.5, 0.0, 1.0);
        m_moodRecoveryM3 = builder
                .comment(
                        "Base recovery speed factor when the mood is very bad. 0 = base recovery stops completely (only a campfire helps);",
                        "raise it to 0.1 if players get stuck")
                .defineInRange("mood_recovery_m3", 0.0, 0.0, 1.0);

        m_sanityMoodEnabled = builder
                .comment(
                        "Whether low sanity makes a negative mood fade away more slowly and makes psychological damage hurt more.",
                        "Off = Depression is not touched by sanity at all")
                .define("sanity_mood_enabled", true);
        m_sanityS1 = builder
                .comment("Sanity at or above this value is the best tier (stabilize_m0 / damage_m0). Must be larger than s2")
                .defineInRange("sanity_s1", 70.0, 0.0, 100.0);
        m_sanityS2 = builder
                .comment("Sanity at or above this value (and below s1) is the second tier. Must be larger than s3")
                .defineInRange("sanity_s2", 40.0, 0.0, 100.0);
        m_sanityS3 = builder
                .comment("Sanity at or above this value (and below s2) is the third tier, anything lower is the fourth tier")
                .defineInRange("sanity_s3", 20.0, 0.0, 100.0);

        m_stabilizeM0 = builder
                .comment("How fast a negative mood returns towards 0, best sanity tier (1.0 = Depression's normal speed, smaller = slower)")
                .defineInRange("stabilize_m0", 1.0, 0.0, 1.0);
        m_stabilizeM1 = builder
                .comment("Same, second sanity tier")
                .defineInRange("stabilize_m1", 0.75, 0.0, 1.0);
        m_stabilizeM2 = builder
                .comment("Same, third sanity tier")
                .defineInRange("stabilize_m2", 0.5, 0.0, 1.0);
        m_stabilizeM3 = builder
                .comment("Same, fourth (lowest) sanity tier. 0 would freeze a negative mood completely")
                .defineInRange("stabilize_m3", 0.25, 0.0, 1.0);

        m_damageM0 = builder
                .comment(
                        "Factor on every psychological damage the player takes, best sanity tier (1.0 = unchanged, larger = hurts more).",
                        "It also scales Depression's smaller hits (PTSD episodes, death, pet death, insomnia) and hits sent through Depression's API by other mods")
                .defineInRange("damage_m0", 1.0, 1.0, 3.0);
        m_damageM1 = builder
                .comment("Same, second sanity tier")
                .defineInRange("damage_m1", 1.0, 1.0, 3.0);
        m_damageM2 = builder
                .comment("Same, third sanity tier")
                .defineInRange("damage_m2", 1.15, 1.0, 3.0);
        m_damageM3 = builder
                .comment("Same, fourth (lowest) sanity tier")
                .defineInRange("damage_m3", 1.30, 1.0, 3.0);

        m_ptsdEnabled = builder
                .comment("Whether a PTSD level rising (1-4, as in Depression) costs sanity, once per episode")
                .define("ptsd_enabled", true);
        m_ptsdPenalty1 = builder
                .comment(
                        "Sanity points (S, before negative_multiplier) paid IN TOTAL once an episode reaches PTSD level 1.",
                        "Rising further only pays the difference to the next total, so 0 -> 4 directly and 0 -> 1 -> 2 -> 3 -> 4 cost the same.",
                        "The four totals must not decrease: 0 <= level 1 <= level 2 <= level 3 <= level 4")
                .defineInRange("ptsd_penalty_1", 2.0, 0.0, 100.0);
        m_ptsdPenalty2 = builder
                .comment("Total paid once an episode reaches PTSD level 2 (not smaller than level 1)")
                .defineInRange("ptsd_penalty_2", 4.0, 0.0, 100.0);
        m_ptsdPenalty3 = builder
                .comment("Total paid once an episode reaches PTSD level 3 (not smaller than level 2)")
                .defineInRange("ptsd_penalty_3", 6.0, 0.0, 100.0);
        m_ptsdPenalty4 = builder
                .comment("Total paid once an episode reaches PTSD level 4 (not smaller than level 3)")
                .defineInRange("ptsd_penalty_4", 8.0, 0.0, 100.0);
        m_ptsdRearmSeconds = builder
                .comment(
                        "An episode only ends after the PTSD level stayed at 0 for this many CONSECUTIVE seconds; only then the next rise costs sanity again.",
                        "Dropping to a lower, non-zero level does not end it. Seconds are Depression's 20-tick steps, so a laggy server counts slower. 0 = ends as soon as the level is 0")
                .defineInRange("ptsd_rearm_seconds", 10, 0, 300);
        m_ptsdLog = builder
                .comment("Write one line to the server log every time PTSD costs sanity (rare, handy to check numbers in real play)")
                .define("ptsd_log", true);

        m_medicineEffectEnabled = builder
                .comment("2B: whether on-indication Depression medicine restores sanity every 120 ticks")
                .define("medicine_effect_enabled", true);
        m_medicineSanityTier1 = builder
                .comment("2B: sanity points (S) restored per 120 ticks for tier 1 (mild depression or bipolar combined therapy)")
                .defineInRange("medicine_sanity_tier_1", 0.05, 0.0, 10.0);
        m_medicineSanityTier2 = builder
                .comment("2B: sanity points (S) restored per 120 ticks for tier 2 (moderate depression)")
                .defineInRange("medicine_sanity_tier_2", 0.10, 0.0, 10.0);
        m_medicineSanityTier3 = builder
                .comment("2B: sanity points (S) restored per 120 ticks for tier 3 (major depression)")
                .defineInRange("medicine_sanity_tier_3", 0.15, 0.0, 10.0);
        m_medicineCapByIllness = builder
                .comment("2B: cap anti-depression medicine tier by the player's current mentalHealthId (1..3) at tick time")
                .define("medicine_cap_by_illness", true);
        m_medicineApplyTraitMultiplier = builder
                .comment("2B: whether to multiply medicine sanity recovery by MentalTrait.medicineEffectMultiplier (default false)")
                .define("medicine_apply_trait_multiplier", false);

        m_overdosePenaltyEnabled = builder
                .comment("2B: whether each Depression medicine overdose event deducts sanity")
                .define("overdose_penalty_enabled", true);
        m_overdosePenalty1 = builder
                .comment("2B: sanity points (S, before negative_multiplier) deducted when odCount == 1")
                .defineInRange("overdose_penalty_1", 5.0, 0.0, 100.0);
        m_overdosePenalty2 = builder
                .comment("2B: sanity points (S, before negative_multiplier) deducted when odCount == 2")
                .defineInRange("overdose_penalty_2", 10.0, 0.0, 100.0);
        m_overdosePenalty3 = builder
                .comment("2B: sanity points (S, before negative_multiplier) deducted when odCount >= 3 (including saturated 3 -> 3)")
                .defineInRange("overdose_penalty_3", 15.0, 0.0, 100.0);
        m_overdosePauseMedicineTicks = builder
                .comment("2B: ticks after the latest overdose event during which medicine-sourced sanity recovery is paused")
                .defineInRange("overdose_pause_medicine_ticks", 200, 0, 12000);

        m_sleepRestoreEnabled = builder
                .comment("2B: whether sleeping through the night restores sanity (independent of legacy active sources)")
                .define("sleep_restore_enabled", true);
        m_sleepRestoreAmount = builder
                .comment("2B: sanity points (S) restored upon a valid night-skipping sleep")
                .defineInRange("sleep_restore_amount", 25.0, 0.0, 100.0);
        m_sleepManiaBlocksRestore = builder
                .comment("2B: whether bipolar mania blocks sleep sanity restoration (matching Depression's stopSleepInBed)")
                .define("sleep_mania_blocks_restore", true);

        builder.pop();

        builder.comment(
                "Depression addon, phase 3: inner entities (shadow creatures).",
                "Everything here ships OFF: leaving the defaults keeps the phase-1 behaviour where no new",
                "inner entities ever spawn. Operators must opt in explicitly per dimension.")
                .push("phase3");

        m_phase3InnerEntitySpawning = builder
                .comment(
                        "Master switch for spawning NEW inner entities (Rotting Stalker / Sneaking Terror).",
                        "Default false = no new spawns, exactly as in phase 1. Existing entities already in a world",
                        "are not removed and keep their visibility/attack gating. Set to true to allow spawning for",
                        "players whose insanity reaches the 0.75 spawn threshold (sanity S <= 25).",
                        "The compile-time phase gate must also be opened by a phase-3 build; this config alone is not enough.")
                .define("inner_entity_spawning", false);
        m_phase3SpawnFailureCooldown = builder
                .comment(
                        "Ticks to wait before retrying a player whose last spawn attempt failed (bad spot, collision).",
                        "Prevents an eligible player in an unsuitable location from retrying the radius-40 entity",
                        "query and block scan every tick. 20 ticks = 1 second.")
                .defineInRange("spawn_failure_cooldown_ticks", 20, 1, 600);
        m_phase3SpawnRadius = builder
                .comment("Horizontal radius (blocks) around the player in which a spawn column is sampled.")
                .defineInRange("spawn_radius", 20, 4, 64);
        m_phase3DetectionRadius = builder
                .comment("Radius (blocks) of the nearby-inner-entity count used to enforce the per-player cap.")
                .defineInRange("detection_radius", 40, 8, 128);

        builder.pop();
        builder.comment("Configuration for passive sanity sources").push("passive");

        m_passive = builder
                .comment("This value will be added to sanity each second regardless of any other factors")
                .defineInRange("passive", .0, -100.0, 100.0);
        m_raining = builder
                .comment("Sanity gain per second during rainy weather or in water")
                .defineInRange("raining", -.2, -100.0, 100.0);
        m_affectInWater = builder
                .comment("Whether 'raining' sanity also applies when the player is standing in water (rain still applies regardless)")
                .define("affect_in_water", true);
        m_affectThirsty = builder
                .comment(
                        "Whether low thirst should drain sanity. Requires Tough As Nails or Legendary Survival Overhaul.",
                        "No effect if neither mod is installed.")
                .define("affect_thirsty", true);
        m_thirstyThreshold = builder
                .comment("Players' sanity will start getting affected with thirst levels at and below this threshold (in half-droplets)")
                .defineInRange("thirsty_threshold", 8, 0, 20);
        m_thirsty = builder
                .comment("Players with thirst levels at and below <thirsty_threshold> gain this amount of sanity per second")
                .defineInRange("thirsty", -.2, -100.0, 100.0);
        m_affectHot = builder
                .comment(
                        "Whether being in hot environments should drain sanity. Requires Tough As Nails or Legendary Survival Overhaul.",
                        "No effect if neither mod is installed.")
                .define("affect_hot", true);
        m_hot = builder
                .comment("Players in hot environments gain this amount of sanity per second")
                .defineInRange("hot", -.15, -100.0, 100.0);
        m_affectCold = builder
                .comment(
                        "Whether being in cold environments should drain sanity. Requires Tough As Nails or Legendary Survival Overhaul.",
                        "No effect if neither mod is installed.")
                .define("affect_cold", true);
        m_cold = builder
                .comment("Players in cold environments gain this amount of sanity per second")
                .defineInRange("cold", -.15, -100.0, 100.0);
        m_hungerThreshold = builder
                .comment("Players' sanity will start getting affected with food levels at and below this threshold (in half-drumsticks)")
                .defineInRange("hunger_threshold", 8, 0, 20);
        m_hungry = builder
                .comment("Players with food levels at and below <hunger_threshold> gain this amount of sanity per second")
                .defineInRange("hungry", -.2, -100.0, 100.0);
        m_enderManAnger = builder
                .comment("Players will gain this amount of sanity per second for 5 seconds after looking at an enderman")
                .defineInRange("ender_man_anger", -5.0, -100.0, 100.0);
        m_pet = builder
                .comment("Players will gain this amount of sanity per second while being near their pets")
                .defineInRange("pet", .15, -100.0, 100.0);
        m_monster = builder
                .comment("Players will gain this amount of sanity per second while being near any monsters")
                .comment("This value is doubled if the monster is aggressive towards the player")
                .defineInRange("monster", -.1, -100.0, 100.0);
        m_darkness = builder
                .comment("Players will gain this amount of sanity per second while being in the dark")
                .defineInRange("darkness", -.15, -100.0, 100.0);
        m_darknessThreshold = builder
                .comment("Maximum light level considered to be darkness (inclusive)")
                .defineInRange("darkness_threshold", 4, 0, 15);
        m_lightness = builder
                .comment("Players will gain this amount of sanity per second while being in the light")
                .defineInRange("lightness", 0.0, -100.0, 100.0);
        m_lightnessThreshold = builder
                .comment("Minimum light level considered to be lightness (inclusive)")
                .defineInRange("lightness_threshold", 4, 0, 15);
        m_blockStuck = builder
                .comment("Players who are stuck in blocks (such as cobweb) and have their movement restricted gain this amount of sanity per second")
                .defineInRange("block_stuck", -.09, -100.0, 100.0);
        m_dirtPath = builder
                .comment("Players moving on a dirt path or a carpet receive this amount of sanity per second")
                .defineInRange("dirt_path", .09, -100.0, 100.0);
        m_jukeboxPleasant = builder
                .comment("Nearby jukebox playing a pleasant melody gives this amount of sanity per second")
                .defineInRange("jukebox_pleasant", .08, -100.0, 100.0);
        m_jukeboxUnsettling = builder
                .comment("Nearby jukebox playing an unsettling melody gives this amount of sanity per second (this takes priority over pleasant melodies)")
                .defineInRange("jukebox_unsettling", -.11, -100.0, 100.0);

        List<String> path = new ArrayList<>();
        path.add("blocks");

        m_passiveBlocks = builder.comment(
                "Define a list of blocks that affect sanity of players standing near them",
                "A block should be included as follows: block_registry_name[property1=value1,property2=value2];A;B;C",
                "Where A is how much sanity is gained per second, B is a radius in blocks,",
                "C is whether a block needs to be naturally generated (not placed by player) (true/false)",
                "Supports boolean block state properties (can be omitted together with brackets)",
                "Prefix with TAG_ and follow with a tag registry name to define all blocks with the tag",
                "NOTE: not everything may work correctly with any configuration, e.g. multiblocks like tall flowers and beds; needs testing")
                .defineListAllowEmpty(path, ConfigDefault::passiveBlocksDefault, ConfigManager::stringEntryIsValid);

        builder.pop();
        builder.comment("Configuration for active sanity sources").push("active");

        m_sleeping = builder
                .comment("Sleeping through the night restores this amount of sanity")
                .defineInRange("sleeping", 50.0, -100.0, 100.0);
        m_hurtRatio = builder
                .comment("Players will gain sanity based on the damage they take from any sources with the ratio of 1 to this number")
                .defineInRange("hurt_ratio", -1.0, -100.0, 100.0);
        m_babyChickenSpawning = builder
                .comment("Spawning a baby chicken by throwing an egg restores this amount of sanity")
                .defineInRange("baby_chicken_spawn", 5.0, -100.0, 100.0);
        m_babyChickenSpawningCd = builder
                .comment("Spawning a baby chicken cooldown (see notes above), real time in seconds")
                .defineInRange("baby_chicken_spawn_cd", 300.0, 0.0, Float.MAX_VALUE);
        m_advancement = builder
                .comment("Earning an advancement gives this amount of sanity")
                .defineInRange("advancement", 20.0, -100.0, 100.0);
        m_animalBreeding = builder
                .comment("Breeding two animals together gives this amount of sanity")
                .defineInRange("animal_breeding", 9.0, -100.0, 100.0);
        m_animalBreedingCd = builder
                .comment("Breeding animals cooldown (see notes above), real time in seconds")
                .defineInRange("animal_breeding_cd", 600.0, 0.0, Float.MAX_VALUE);
        m_animalHurtRatio = builder
                .comment(
                        "Players gain this amount of sanity for every point of damage dealt to peaceful animals (incl. neutral ones)",
                        "This value is doubled for baby animals")
                .defineInRange("animal_hurt_ratio", -.5, -100.0, 100.0);
        m_petDeath = builder
                .comment("Players gain this amount of sanity upon their pets' death")
                .defineInRange("pet_death", -60.0, -100.0, 100.0);
        m_villagerTrade = builder
                .comment("Players gain this amount of sanity upon successfully trading with a villager")
                .defineInRange("villager_trade", 20.0, -100.0, 100.0);
        m_villagerTradeCd = builder
                .comment("Villager trade cooldown (see notes above), real time in seconds")
                .defineInRange("villager_trade_cd", 600.0, 0.0, Float.MAX_VALUE);
        m_shearing = builder
                .comment("Shearing an animal gives this amount of sanity")
                .defineInRange("shearing", 5.0, -100.0, 100.0);
        m_shearingCd = builder
                .comment("Shearing animals cooldown (see notes above), real time in seconds")
                .defineInRange("shearing_cd", 300.0, 0.0, Float.MAX_VALUE);
        m_eating = builder
                .comment("Consuming food gives this amount of sanity for every half-drumstick the meal restores")
                .defineInRange("eating", .8, -100.0, 100.0);
        m_eatingCd = builder
                .comment("Eating cooldown (see notes above), real time in seconds")
                .defineInRange("eating_cd", 300.0, 0.0, Float.MAX_VALUE);
        m_fishing = builder
                .comment("Fishing an item gives this amount of sanity")
                .defineInRange("fishing", 1.3, -100.0, 100.0);
        m_fishingCd = builder
                .comment("Fishing cooldown (see notes above), real time in seconds")
                .defineInRange("fishing_cd", 0.0, 0.0, Float.MAX_VALUE);
        m_farmlandTrample = builder
                .comment("Trampling a farmland gives this amount of sanity")
                .defineInRange("farmland_trample", -2.0, -100.0, 100.0);
        m_innerEntityKill = builder
                .comment("Killing an inner entity gives this amount of sanity")
                .defineInRange("inner_entity_kill", 3.0, -100.0, 100.0);
        m_pottingFlower = builder
                .comment("Potting a flower gives this amount of sanity")
                .defineInRange("potting_flower", 4.0, -100.0, 100.0);
        m_pottingFlowerCd = builder
                .comment("Potting a flower cooldown (see notes above), real time in seconds")
                .defineInRange("potting_flower_cd", 300.0, 0.0, Float.MAX_VALUE);
        m_changedDimension = builder
                .comment("Teleporting to another dimension gives this amount of sanity")
                .defineInRange("changed_dimension", -10.0, -100.0, 100.0);
        m_struckByLightning = builder
                .comment("Players gain this amount of sanity upon being struck by lightning")
                .defineInRange("struck_by_lightning", -30.0, -100.0, 100.0);

        path.clear();
        path.add("items");

        m_items = builder.comment(
                "Define a list of items that will affect sanity upon their usage",
                "An item should be included as follows: item_registry_name;A;B",
                "Where A is how much sanity is gained upon usage and B is a custom category",
                "Items with the same categories share the same cooldown",
                "The sanity gained will be multiplied by (timeSinceLastUsage / categoryCooldown) capping at 1.0")
                .defineListAllowEmpty(path, ConfigDefault::itemsDefault, ConfigManager::stringEntryIsValid);

        path.clear();
        path.add("item_categories");

        m_itemCats = builder.comment(
                "Define a list of custom categories for items specified in <items>",
                "A category should be included as follows: A;B",
                "Where A is a category id (integer) and B is a cooldown (in seconds) all items in this category share")
                .defineListAllowEmpty(path, ConfigDefault::itemCatsDefault, ConfigManager::stringEntryIsValid);

        path.clear();
        path.add("broken_blocks");

        m_brokenBlocks = builder.comment(
                "Define a list of blocks that will affect sanity of players breaking them",
                "A block should be included as follows: block_registry_name;A;B;C;D",
                "Where A is how much sanity is gained upon breakage, B is a custom category,",
                "C is whether a block has to be naturally generated (not placed by players) (true/false),",
                "D is whether a block has to be mined with a correct tool (aka resources should drop) (true/false)",
                "Prefix with TAG_ and follow with a tag registry name to define all blocks with the tag",
                "Blocks with the same categories share the same cooldown",
                "The sanity gained will be multiplied by (timeSinceLastUsage / categoryCooldown) capping at 1.0",
                "NOTE: not everything may work correctly with any configuration, e.g. multiblocks like tall flowers and beds need testing")
                .defineListAllowEmpty(path, ConfigDefault::brokenBlocksDefault, ConfigManager::stringEntryIsValid);

        path.clear();
        path.add("broken_block_categories");

        m_brokenBlockCats = builder.comment(
                "Define a list of custom categories for blocks specified in <broken_blocks>",
                "A category should be included as follows: A;B",
                "Where A is a category id (integer) and B is a cooldown (in seconds) all blocks in this category share")
                .defineListAllowEmpty(path, ConfigDefault::brokenBlockCatsDefault, ConfigManager::stringEntryIsValid);

        builder.pop();
        builder.comment("Multiplayer configuration").push("multiplayer");

        m_sanePlayerCompany = builder
                .comment("Being around players with sanity higher than 50% gives this amount of sanity per second")
                .defineInRange("sane_player_company", .05, -100.0, 100.0);
        m_insanePlayerCompany = builder
                .comment("Being around players with sanity lower than 50% gives this amount of sanity per second")
                .defineInRange("insane_player_company", -.12, -100.0, 100.0);

        builder.pop();

        builder.comment("Entities configuration").push("entity");

        m_saneSeeInnerEntities = builder
                .comment(
                        "Whether sane players should be able to see and battle inner entities",
                        "Mobs will still be there server-side and will count towards passive sanity",
                        "Players who are targeted by inner entities see them regardless")
                .define("sane_see_inner_entities", false);

        builder.pop();

        builder.comment("Client configuration").push("client");
        builder.comment("Sanity indicator configuration").push("indicator");

        m_renderIndicator = builder
                .comment("Whether to render sanity indicator")
                .define("render", true);
        m_twitchIndicator = builder
                .comment("Whether to twitch sanity indicator at low sanity levels")
                .define("twitch", true);
        m_indicatorScale = builder
                .comment("Sanity indicator scale")
                .defineInRange("scale", 1.0, 0.0, Float.MAX_VALUE);
        m_indicatorLocation = builder
                .comment("Sanity indicator location")
                .defineEnum("location", SanityIndicatorLocation.HOTBAR_LEFT);

        builder.pop();
        builder.comment("Inner monologue configuration").push("hints");

        m_renderHint = builder
                .comment("Whether to render inner monologue/random thoughts")
                .define("render", true);
        m_twitchHint = builder
                .comment("Whether to twitch inner monologue/random thoughts at low sanity levels")
                .define("twitch", true);

        builder.pop();
        builder.comment("Blood tendrils overlay configuration").push("blood_tendrils");

        m_renderBloodTendrilsOverlay = builder
                .comment("Whether to render blood tendrils overlay")
                .define("render", true);
        m_flashBtOnShortBurst = builder
                .comment("Whether to flash blood tendrils overlay upon losing sanity in a short burst")
                .define("short_burst_flash", true);
        m_renderBtPassive = builder
                .comment("Whether to render blood tendrils overlay when passively losing sanity")
                .define("render_passive", true);

        builder.pop();

        m_renderPost = builder
                .comment("Whether to render sanity postprocessing effects")
                .define("render_post", true);
        m_playSounds = builder
                .comment("Whether to enable sanity sound effects")
                .define("play_sounds", true);
        m_insanityVolume = builder
                .comment("Insanity ambience max volume")
                .defineInRange("insanity_volume", .6, .0, 1.0);
        m_heartbeatYieldToDepression = builder
                .comment(
                        "Phase 3 audio boundary with Depression: when true, this addon's heartbeat loop goes silent",
                        "during a Depression PTSD onset (Depression plays its own heartbeat/breathing then), so the",
                        "two mods never play two heartbeats at once. The insanity drone and fake footsteps are not",
                        "affected. Only read on the client.")
                .define("heartbeat_yield_to_depression", true);

        builder.pop();
    }

    private static List<String> passiveBlocksDefault()
    {
        List<String> list = new ArrayList<>();

        list.add("minecraft:campfire[lit=true];0.1;4;false");

        return list;
    }

    private static List<String> itemsDefault()
    {
        List<String> list = new ArrayList<>();

        list.add("minecraft:pufferfish;-5;0");
        list.add("minecraft:poisonous_potato;-5;0");
        list.add("minecraft:spider_eye;-5;0");
        list.add("minecraft:rotten_flesh;-5;0");
        list.add("minecraft:chorus_fruit;-3;0");
        list.add("minecraft:ender_pearl;-1;0");
        list.add("minecraft:honey_bottle;6;1");
        list.add("minecraft:golden_carrot;7;1");
        list.add("minecraft:golden_apple;8;1");
        list.add("minecraft:enchanted_golden_apple;13;1");

        return list;
    }

    private static List<String> itemCatsDefault()
    {
        List<String> list = new ArrayList<>();

        list.add("0;0");
        list.add("1;800.0");

        return list;
    }

    private static List<String> brokenBlocksDefault()
    {
        List<String> list = new ArrayList<>();

        list.add("minecraft:infested_stone;-8;0;false;false");
        list.add("minecraft:infested_cobblestone;-8;0;false;false");
        list.add("minecraft:infested_stone_bricks;-8;0;false;false");
        list.add("minecraft:infested_cracked_stone_bricks;-8;0;false;false");
        list.add("minecraft:infested_mossy_stone_bricks;-8;0;false;false");
        list.add("minecraft:infested_chiseled_stone_bricks;-8;0;false;false");
        list.add("minecraft:infested_deepslate;-8;0;false;false");

        return list;
    }

    private static List<String> brokenBlockCatsDefault()
    {
        List<String> list = new ArrayList<>();

        list.add("0;0");

        return list;
    }
}