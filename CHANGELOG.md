# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## Unreleased - Depression addon, 2A (mood, sanity and PTSD)

Needs the Depression build that has `MentalModifiers` for the sanity-to-mood part (an older build only loses that part and logs `modifiers unavailable`).

### Added
  - Bad mood slows the base sanity recovery (x1 / x0.75 / x0.5 / x0 by mood tier). The campfire recovery is not affected.
  - Low sanity slows the fading of a negative mood (x1 / x0.75 / x0.5 / x0.25) and raises every psychological damage (x1 / x1 / x1.15 / x1.30). Because it hooks Depression's single damage primitive it also scales PTSD episode damage, death, pet death, insomnia wake-ups and hits sent through Depression's API by other mods (top factor x1.30 by default, the config allows up to x3).
  - PTSD costs sanity once per episode: a rise in the PTSD level pays the difference to the next total (default totals 2 / 4 / 6 / 8 S, times `negative_multiplier`). Dropping to a lower non-zero level does not end an episode, only `ptsd_rearm_seconds` (default 10) consecutive seconds at level 0 do. Joining, respawning and switching dimension never charge by themselves.
  - Config section `sanity.link` (28 keys, also overridable per dimension). Dimension overrides are checked when they are loaded (type, range, unknown keys), and the rules across keys (strictly descending thresholds, non-decreasing PTSD totals) are checked for the default config and for every dimension; a violation stops the start-up, or makes `/sanity config reload` fail and keep the previous overrides, with the key and value in the message. In `default.toml` a single out-of-range value is put back to its default by NeoForge's own config loader, with a warning in the log.
  - `/sanity link [targets]` (permission 2, read-only): mood, sanity, the three multipliers, what stops the base recovery, PTSD level and episode ledger.

### Changed
  - Depression's `MentalModifiers` hook is registered at start-up when present; a Depression build without it is tolerated (one log line), a Depression build whose PTSD level API does not match stops the start-up with a clear message.

---

## Unreleased - Depression addon, phase 1

### Changed
  - Now an addon that requires the Depression mod. Sanity is its own value, separate from Depression's mood and mental health (nothing is converted or written between them).
  - This fork targets NeoForge 1.21.1 only; the upstream Fabric subproject is excluded from the build and its sources are left untouched.
  - Sanity (0-100, 100 = clear-headed) is kept through death, respawn, dimension changes and relogging.
  - Sanity is sent to the client only when it really changes, at most once a second (it used to be sent every tick). `/sanity set|add` update the client right away.
  - Only two things raise sanity by themselves: a slow base recovery (+0.05/s) while it is calm and bright, and a lit campfire nearby (+0.10/s, several campfires do not stack).
  - All other original sanity sources (passive and one-shot ones, including sleeping and the garland effect) are switched off. `/sanity set|add` still work.
  - New shadow creatures no longer appear; the ones already in a world are left alone.

### Added
  - `/sanity get [targets]` prints the current sanity.
  - Config section `sanity.recovery` (base rate, minimum light, campfire rate and radius).

---

## 21.1.3 - 2026-08-24

### Added
  - Killing a shadow creature restores a small amount of sanity.
  - Shadow creatures show up more often the more of them you kill, easing off again after staying sane for a while.

### Fixed
  - Shadow creatures chasing forever after being hit, even once sanity was restored.
  - Shadow creatures draining sanity while completely invisible.
  - Looking at an enderman not draining sanity on Fabric.
  - Sleeping not restoring sanity alongside mods that change how sleeping works.

### Changed
  - Shadow creatures are much weaker.
  - Shadow creatures no longer push players, and can only be hit once they turn hostile.

### Removed
  - Sleeping cooldown; sleeping through the night always restores sanity.

---

## 21.1.2 - 2026-08-23

### Fixed
  - Players being disconnected from dedicated servers.
  - Garland being uncraftable, and its recipe not showing up in recipe viewers such as JEI.
  - Garland never losing durability, and wearing out too fast on servers.
  - Farmland trampling, fishing, lightning strikes and shearing not affecting sanity.
  - Cooldowns being ignored by sanity sources such as eating, breeding and trading.
  - Cooldowns for broken blocks not being saved.
  - Shadow creatures staying aggressive after sanity was restored.

### Changed
  - Garland is crafted from small flowers again, and can no longer be repaired.

---

## 21.1.1 - 2026-07-19

### Added
  - Mod icon.

### Fixed
  - Crash when loading the mod on dedicated servers.
  - Sanity draining while sitting or while standing on blocks with a lower hitbox, such as soul sand and mud.

---

## 21.1.0 - 2026-05-21

### Added
  - Support for multiloader(NeoForge/Fabric)
  - Tough as Nails & Legendary Survival Overhaul Compatibility
  - Ported to 1.21.1
