# Field chemistry: chemical grenades, the gas mask and medicines

Status: implemented on `feature/chemical-31` (batch 31), stacked on `feature/grapple-30`, awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 3 October 2026, asked for more useful chemistry and to build the suggested batches in order, rethinking weak ideas. This is the third: chemical grenades, a gas mask to counter them, and a pharmacy.
Owner: jimbozoomer-byte
Target milestone and tier: industrial chemistry (chlorine, ammonia, bioethanol, lye, aluminum); after the frag grenade (batch 18).
Primary specialty and supported player role: combat, escape and survival.

## What was rethought
- **No pharmacy machine.** A "chemist's bench" would only repeat the chemical reactor, which already mixes items with a fluid. The medicines are reactor recipes instead, and the effort went into making each one do something vanilla brewing does not.
- **Grenades by role, not by gas.** Each one is a different tool: chlorine for living crowds, smoke to escape, thermite for one tough target, a flashbang to stop a rush. All use the existing grenade launcher.
- **Chlorine spares the undead.** It hurts what breathes (Minecraft's own "can breathe under water" list: fish, axolotls, the undead…). That makes thermite, frag and flashbangs the answer to zombies and skeletons, and chlorine the answer to creepers, spiders, illagers and players.
- **The scuba set doubles as a sealed respirator** while its tank has oxygen, so batch 27 gear gains a use.

## Player experience
- **Chlorine grenade:** a 3-block cloud for 10 s. Everything inside that breathes takes 2 damage a second, through armor. A gas mask or a sealed scuba set keeps it out. Made in the chemical reactor: a steel plate and an iron nugget with 500 mB of chlorine make two.
- **Smoke grenade:** a 4-block smoke screen for 15 s. Mobs inside forget their target, and so do mobs outside hunting someone inside, so you can break off a fight or cross a skeleton's line of fire. Players inside without a mask are blinded. Reactor: a steel plate and two sugar with 250 mB of ammonia make two.
- **Thermite grenade:** a white-hot pool on the floor for 6 s. Whatever stands in it takes 4 damage a second through armor and is set alight for 5 s. Fire-immune mobs and fire resistance are immune. It never places fire or lights a block. Thermite (an aluminum ingot and two iron dust make three) fills grenades in the frag grenade's shape.
- **Flashbang:** a flash and a bang, no damage. Within 10 blocks, players who can see it are blinded and dizzy for 4 s (a third of that if they look away; not at all in a gas mask); mobs that can see it lose their target and are slowed and weakened for 3 s. Walls shield. Crafted: two steel plates, an iron nugget, an aluminum nugget and glowstone dust make four.
- **Grenade launcher:** fires any grenade (the other hand first, then the inventory).
- **Gas mask:** a helmet (one point of armor). Keeps out chlorine and smoke; its tinted lenses keep out the flash. The filter wears one point a second in gas or smoke (220 s in all); repair it with charcoal on an anvil. Crafted from rubber, glass panes, a steel plate and charcoal.
- **Scuba mask + tank:** with oxygen in the tank, they keep out chlorine and smoke too, using 20 mB a second.
- **First aid kit:** heals four hearts (Instant Health II) after a 1.5 s use, then a 10 s cooldown. Reactor: two cotton and a soap in 250 mB of bioethanol make two. A stackable heal without brewing.
- **Antidote:** drink to clear poison, wither, nausea, blindness, hunger, weakness, slowness, mining fatigue, darkness, levitation, infested, oozing and weaving, keeping the good effects (milk clears both). Reactor: two charcoal and a glass bottle in 250 mB of lye. Gives the bottle back.
- **Stimulant:** Speed II and Haste II for a minute, with Hunger. Haste has no vanilla potion. Reactor: four cocoa beans and a glass bottle in 250 mB of bioethanol. Gives the bottle back.
- Advancements: **Filtered** (gas mask), **Chemical Arsenal** (any chemical grenade), **Field Medic** (first aid kit). Handbook pages: Chemical Grenades, Gas Mask, Medicines.

## Connections
- Input producer: chlorine and lye (electrolytic cell), ammonia (synthesis converter), bioethanol (fermentation), aluminum, iron dust, steel plates, rubber, cotton, soap.
- Output consumer: the player. New sinks for chlorine, ammonia, lye and bioethanol.
- Technology connection: the grenade launcher (batch 18), scuba gear (batch 27), the shared party API.
- Magic connection: none. Required vs optional: optional.

## Balance and automation
- Chlorine: up to 20 damage over 10 s for anything that stays, but walking out ends it; it is strongest against crowds that cannot leave.
- Thermite: 24 damage over 6 s plus burning, for one target held in place. Frag stays the burst weapon (16 at the centre).
- Smoke and flashbangs deal no damage; they buy time.
- Medicines: the first aid kit matches Instant Health II but has a cooldown, so it cannot be spammed. No positive-gain loops: every recipe consumes its inputs.

## Multiplayer and persistence
- Server-authoritative: only the server spawns clouds and applies damage and effects.
- **PvP and parties:** other players are affected only where the thrower could hurt them (the server's PvP setting) and are not in the thrower's party. Creative and spectator players are never affected. The thrower is affected by their own grenades.
- **Griefing:** none possible. No block is broken, moved or lit; clouds touch living things only (not armor stands, item frames or dropped items).
- Clouds are not saved with the world: one in an unloading chunk just ends.
- Recipes follow the `machines` feature switch (and `salt` where chlorine or lye is used).

## Dependencies and assets
No new dependencies. Items, the worn mask (doubled resolution) and recipes from `tools/field_chemistry.py`; reactor recipes in `tools/petro.py`. Two damage types, `jugcraft:chlorine` (bypasses armor) and `jugcraft:thermite` (fire, bypasses armor), with death messages. Medicines use vanilla's consumable components; the cloud is drawn with vanilla particles. All original.

## Verification
- `tools/check_mod_data.py`: numbers in `FieldChemistry` match `tools/field_chemistry.py`; items, textures, the cloud entity, damage types and the worn mask exist.
- Game tests (CI):
  - `chlorineHurtsWhatBreathesUnlessMasked`: a bare pig is hurt; a pig in a gas mask is not and its filter wears one point; a pig in a sealed scuba set is not and uses 20 mB; a zombie is not;
  - `smokeHidesFromMobs`: a zombie targeting a pig in smoke loses it;
  - `thermiteBurnsButLightsNothing`: a pig in the pool is hurt and set alight; no fire block appears and the planks beside it stand;
  - `flashbangStaggersWhatSeesIt`: a zombie in sight is slowed, one behind a wall is not, nothing is hurt;
  - `medicinesWork`: the antidote clears poison and keeps speed; the stimulant gives haste; the first aid kit heals.
- Not run: client play (throwing, the look of the clouds, blindness from the player's view), two players, PvP.

## World and event applicability
Not applicable: no worldgen, mobs or dimensions.

## Rollout and open questions
- Radii, durations and damage are first values for the owner to tune.
- Brain-driven mobs (piglins, wardens, villagers) keep their own memory of a target, so smoke and flashbangs may not shake them.
- ANFO blasting stays out until the owner decides, since it would break blocks.
