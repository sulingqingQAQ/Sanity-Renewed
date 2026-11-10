<p align="center"><img src="https://github.com/user-attachments/assets/1de15a37-0aa8-4b50-a2c8-615a18202af4" style="width:800px;"/></p>

<p align="center"><img src="https://github.com/Antikythera-Studios/EpicFight-Files/raw/Wiki-Images/Icons/new_icons/line_breaker.png"  style="width:1000px;"/></p>

# Sanity: Renewed 🧠

Sanity: Renewed introduces a sanity meter inspired by Don't Starve, adding a new mechanic that forces players to manage their mental state alongside hunger and health. Darkness, isolation, hostile creatures, and dangerous situations slowly chip away at your sanity creating scary new situations & events around you, making the game harder.

<p align="center">
  <img src="readme/sanity_animation.gif" style="width:292px; height:128px; object-fit:cover;"/>
  <img src="readme/garland_craft.png" style="width:242px; height:128px; object-fit:cover;"/>
</p>

As your sanity decreases, reality begins to distort around you. Strange sounds echo through the world, your vision becomes blurry and unstable, shadows move in the distance, and hallucinations begin to appear. Ignore your mental state for too long, and your mind will eventually manifest deadly creatures known as Inner Entities.

<p align="center">
  <a href="https://www.youtube.com/watch?v=ReNKOjPWhyo">
    <img 
      src="https://github.com/user-attachments/assets/411fea12-6b5e-4267-9a06-9cf8eb428aad"
      alt="Sanity Trailer"
      width="640"
    />
  </a>
  <br>
  <sub>Check out PIX ROXY's Showcase of Sanity: Descend into Madness</sub>
</p>

<p align="center"><img src="https://github.com/Antikythera-Studios/EpicFight-Files/raw/Wiki-Images/Icons/new_icons/line_breaker2.png"  style="width:1000px;"/></p>

## Configuration 👁

Sanity: Renewed itself is a very flexible mod, with multiple config options for you to tweak your gameplay and make sure the entire playthrough is well tuned to your likings. If you have any suggestions on how to improve them or want to suggest new config options/new features let me know by making a new issue tracker on my repository.

The mod also includes dimension-specific configuration support through ``config/sanity_renewed/dimension/``, check ``example.toml`` to see & understand how to properly configure the mod per dimension. You're free to include this mod in your modpacks.

<div align="center">
<table>
<tr>
<td valign="top" width="50%">

**Actions that DEPLETES your sanity (configurable):**

* Consuming bad food (like Rotten Flesh)
* Getting wet (rain & ocean)
* Receiving damage
* Being in the dark
* Difficult movement (Cobweb tangling)
* Listening to bad music (like Disc 11)
* Breaking infested blocks
* Being near monsters/enemies
* Hurting peaceful animals
* Having your dog/pets die
* Traversing between dimensions
* Provoking Enderman
* Being hungry
* Being thirsty
* Being too hot/cold

</td>

<td valign="top" width="50%">

**Actions that RESTORE your sanity (configurable):**

* Staying fed
* Being around your dog/pets
* Listening to good music
* Walking on dirt paths/carpets
* Being near campfires
* Sleeping through the night
* Catching fish
* Hatching chickens from eggs
* Earning advancements
* Husbandry (animal breeding/shearing)
* Trading with villagers
* Potting flowers
* Wearing a Garland

</td>
</tr>
</table>
</div>

<p align="center"><img src="https://github.com/Antikythera-Studios/EpicFight-Files/raw/Wiki-Images/Icons/new_icons/line_breaker2.png"  style="width:1000px;"/></p>

## Depression addon: mood, sanity and PTSD (2A)

When it runs next to the Depression mod, the three values talk to each other through multipliers and one-off events only. Nothing is converted, and nothing is written into Depression's mood or mental health. All numbers are in the `[sanity.link]` config section (28 keys, each with a plain-language hint) and can be overridden per dimension; a wrong dimension override (type, range, unknown key), or a combination that breaks the ordering rules (strictly descending thresholds, non-decreasing PTSD totals) in `default.toml` or in any dimension, stops the game at start-up (or makes `/sanity config reload` fail and keep the old overrides). In `default.toml` itself NeoForge puts a single out-of-range value back to its default and writes a warning to the log.

* **Bad mood slows the base recovery.** The calm, bright-place recovery (+0.05 S/s) is multiplied by x1 / x0.75 / x0.5 / x0 for mood >= 0 / above -5 / above -10 / at or below -10. Campfire recovery is never affected.
* **Low sanity makes a bad mood last longer and hurts more.** Negative mood fades x1 / x0.75 / x0.5 / x0.25 as fast, and every psychological damage is x1 / x1 / x1.15 / x1.30, for sanity >= 70 / >= 40 / >= 20 / below 20. This needs a Depression build that has the multiplier hook (`MentalModifiers`); with an older build the addon logs `modifiers unavailable` and only the other two links work. The damage factor also scales Depression's smaller hits (PTSD episodes, death, pet death, insomnia) and hits that other mods send through Depression's API.
* **PTSD costs sanity once per episode.** When the PTSD level (1 to 4, as Depression shows it) rises, you pay the difference to the next total (default totals 2 / 4 / 6 / 8 S, multiplied by `negative_multiplier`). Dropping to a lower, non-zero level does not end the episode; only the level staying at 0 for `ptsd_rearm_seconds` (default 10) in a row does, and only then the next rise costs again.
* `/sanity link [targets]` (permission 2, read-only) prints mood, sanity, the three multipliers, what stops the base recovery and the PTSD ledger of the targets. Asking never changes anything.

<p align="center"><img src="https://github.com/Antikythera-Studios/EpicFight-Files/raw/Wiki-Images/Icons/new_icons/line_breaker2.png"  style="width:1000px;"/></p>

## Important Notice
This is the 1.20+ unofficial continuation of Sanity: Descent Into Madness, originally created by croissantnova for Forge 1.16-1.20.1. The license is MIT, and i've decided to pick up the project since it hasn't been updated since 2023. Croissantnova [recently said he's working on a new project](https://github.com/croissantnova/SanityDescentIntoMadness/pull/93#issuecomment-2746349988) and he plans to get back to working on Sanity: Descent Into Madness when he deems it polished enough for publish. 

This version is a 1:1 rewritte of Sanity: Descend Into Madness, with support for newer versions, and all major modloader (forge/neoforge/fabric). I've also implemented some of the ideas that i saw on the old repo's issue tracker. I also want to thank [Desynq](https://github.com/Desynq) for his PR on the old repository that inspired me to rewritte the mod and implement the ideas he filtered out.

Credit goes to [croissantnova](https://www.curseforge.com/members/croissantnova/projects) for the original concept and implementation. If you enjoy this mod, consider supporting his future projects and of course, Sanity: Descend Into Madness, this project would never be here if it wasn't for his awesome work ❤️.

Check the original mod's page [[here]](https://www.curseforge.com/minecraft/mc-mods/sanity-descent-into-madness), for older versions and to give the original authors support

#### Credits
* [croissantnova](https://github.com/croissantnova) - programmer and artist,<br/>
* [toujourspareil](https://twitter.com/toujourspareil_) - artist.
* [Desynq](https://github.com/Desynq) - major changes & inspirations ([PR #93](https://github.com/croissantnova/SanityDescentIntoMadness/pull/93))
* Sound effects provided by [Zapsplat](https://www.zapsplat.com/)

#### License
See [license](https://raw.githubusercontent.com/croissantnova/SanityDescentIntoMadness/main/LICENSE).

<p align="center"><img src="https://github.com/Antikythera-Studios/EpicFight-Files/raw/Wiki-Images/Icons/new_icons/line_breaker2.png"  style="width:1000px;"/></p>

## Requirements:
This Depression addon fork currently targets **NeoForge 1.21.1 only**. The Fabric section below is retained as upstream reference and is not a supported addon target.

NeoForge:
* [**Architectury API**](https://www.curseforge.com/minecraft/mc-mods/architectury-api)
* [**GeckoLib**](https://www.curseforge.com/minecraft/mc-mods/geckolib)
* **Depression** (0.2.3+, this is a Depression addon; DepressionTianTian is optional)

Fabric (upstream reference only):
* [**Architectury API**](https://www.curseforge.com/minecraft/mc-mods/architectury-api)
* [**GeckoLib**](https://www.curseforge.com/minecraft/mc-mods/geckolib)
* [**Forge Config API Port**](https://www.curseforge.com/minecraft/mc-mods/forge-config-api-port)

<p align="center"><img src="https://github.com/Antikythera-Studios/EpicFight-Files/raw/Wiki-Images/Icons/new_icons/line_breaker2.png"  style="width:1000px;"/></p>

<div align="center">
  
  <a href="https://www.patreon.com/bePatron?u=53051224"><img src="https://github.com/Antikythera-Studios/EpicFight-Files/raw/Wiki-Images/Icons/new_icons/patreon.png" alt="Patreon" width="80"/></a>
<a href="https://discord.com/invite/NbAJwj8RHg"><img src="https://github.com/Antikythera-Studios/EpicFight-Files/raw/Wiki-Images/Icons/new_icons/discord.png" alt="Discord" width="80"/></a>
<a href="https://www.youtube.com/@yesman4100"><img src="https://github.com/Antikythera-Studios/EpicFight-Files/raw/Wiki-Images/Icons/new_icons/youtube.png" alt="YouTube" width="80"/></a>
<a href="https://github.com/guivnf/Sanity-Renewed"><img src="https://github.com/Antikythera-Studios/EpicFight-Files/raw/Wiki-Images/Icons/new_icons/github.png" alt="GitHub" width="80"/></a>
<a href=""><img src="https://github.com/Antikythera-Studios/EpicFight-Files/raw/Wiki-Images/Icons/new_icons/curseforge.png" alt="CurseForge" width="80"/></a>
<a href=""><img src="https://github.com/Antikythera-Studios/EpicFight-Files/raw/Wiki-Images/Icons/new_icons/modrinth.png" alt="Modrinth" width="80"/></a>

</div>
