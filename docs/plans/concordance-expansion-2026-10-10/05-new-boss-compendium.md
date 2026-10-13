# Expanded boss compendium

**Everything added here is a proposal.** The existing backlog is distinguished from newly proposed encounters. Numerical values are solo prototype targets, not measured balance. Use the shared lair contract and contribution rules in [chapter 04](04-existing-bosses-and-shared-encounters.md).

This is a menu and a delivery roadmap. It is not permission for an agent to build every boss before the first owner checkpoint. Complete the Yeti and one coherent new encounter before scaling the roster.

## 1. Preserve the existing trophy roster

Existing weapons are content to connect, not names to replace with a new loot system.

| Existing planned boss | Existing trophy direction | Main magical connection | Distinct encounter action |
|---|---|---|---|
| Yeti King | Glacier Maul / Rimeclaw | Rime, Strata | Bait a charge into a column and manage footing |
| Cinder Tyrant | Cinderbrand / Magmaw | Ember, Tide | Cool armor at controlled vents |
| Mire Hag | Hagthorn / Bogfang | Verdance, Hollow, Tether | Distinguish the true target and cleanse paths |
| Crypt Lich | Soulreaver / Gravewarden | Hollow, Echo, Circlewrights | Resolve phylactery links in a bounded sequence |
| Iron Dreadnought | Dynamo Halberd / Piston Hammer | Tempest, Strata, Clockhearts | Route a discharge and expose vents |
| Alpha Werewolf | Moonfang / Howler | Verdance, Echo; optional transformations | Separate a pack and read the hunt |
| Storm Roc | Stormcaller / Galefeather | Tempest, Radiance | Ground a storm, bait a dive |
| Abyssal Leviathan | Tidebreaker / Leviathan Hook | Tide, Rime | Operate moorings and cross safe currents |

The first eight trophy families already exist in source/data. Confirm their registered IDs, recipe/drop state and exact current effects before editing. A trophy being present does not prove its boss is implemented.

Other previously brainstormed encounters remain a later backlog: Pumpkin King, a Headless Horseman boss upgrade, Marrow Colossus, Chorus Sovereign, Hollow Monarch, Kaiser's Zeppelin, Frost Wyrm and Hive Matriarch. Keep the existing seasonal Horseman separate from a future full boss. An armor set named after a mythological figure does not establish that a boss of that name exists.

## 2. Shared numerical envelope for prototypes

For a first pass, use roughly 320–480 solo health for an ordinary major encounter and existing 50%-per-extra-player scaling capped at 2.5×. Armor 6–12 is a starting range, not a reason to normalize existing bosses. Prefer clear vulnerability windows to extremely high permanent mitigation.

Ordinary attacks should generally telegraph for 0.6–1.2 seconds, major arena actions 1.5–3 seconds. Avoid two overlapping mechanics that require contradictory movement with no safe region. Damage is in health points; twenty points is an ordinary full player health bar before equipment.

Track hazards by encounter ownership and cap each family. Provide a minimum recovery interval after unavoidable transitions. Server timing controls hit windows; animation and audio communicate that timing. Scaling should change health first, not silently shorten every telegraph for a large party.

All essential mechanisms have a manual or ordinary-weapon solution. Specialty interactions improve timing, efficiency or recovery. Every boss has at least one valuable noncombat or cosmetic reward and a mundane route for any essential utility.

## 3. Develop the existing backlog

### Cinder Tyrant — the Kiln Beneath the Mountain

**Inherited direction:** a volcanic salamander with armor that can be cooled; Cinderbrand and Magmaw rewards.

**Proposed arena:** a 44-block basalt kiln bowl, three cooling sluices, four permanent safe shelves and a central heat channel. The entrance is an expedition marker and a crafted invitation; do not require a rare active volcanic event.

**Loop:** cracked glowing armor shows the current heat state. At high heat, damage is reduced; opening a sluice and baiting a body slam into its marked channel cools one plate. A cooled plate provides an eight-second opportunity. The boss can reheat after the opening, not immediately erase the player's action.

**Attacks:** a 0.8-second tail sweep; a 1.2-second ember spit marking three spots; a 1.5-second kiln breath through one sector; a two-second mantle shed producing at most four small threats. At half health, alternate active sluices and add a clearly announced channel surge. Do not convert the whole floor into unavoidable damage.

**Magic:** Tide can prime a sluice, Rime extends one cooling window by up to two seconds, Ember stabilizes a friendly heat shield, Strata braces the slam bait. None creates infinite water, permanently freezes the boss or makes fire damage required.

**Rewards:** connect the existing weapons; propose a tempered scale for one heat-resistant rune or a decorative kiln mantle. Ordinary refractory ceramics remain sufficient for industrial furnaces. Test whether ranged weapons can bypass cooling entirely, and adjust exposure rather than silently nullify every gun.

### Mire Hag — the Lantern Fen

**Inherited direction:** fog, shadow decoys, roots and poison.

**Proposed arena:** a shallow marsh with firm boardwalks and four lamps. Fog is confined encounter presentation with an accessible clear-visibility mode; it is not the industrial pollution system.

**Loop:** the real Hag leaves a different ripple and responds to light. Relighting a lamp reveals her briefly. Decoys have a consistent visual/audio tell, and ordinary thrown items can probe them. A decoy never drops loot or grants repeatable research.

**Attacks:** root lanes with 1.2-second ground tells, a lobbed pool that lasts six seconds, a two-second decoy exchange and a short close swipe. At 55% health, extinguish two lamps and send a bounded root knot toward one. Destroying or severing it preserves that lamp.

**Magic:** Radiance reveals sooner; Verdance cleanses a temporary path rather than deleting all hazards; Tether severs knots; Tide dilutes one pool. An ordinary antidote and accessible supplies remain available without this boss.

**Rewards:** existing venom trophies; optional mire-lantern lens, botanical sample and cottage decoration. No mandatory ingredient for the first medical item. Verify particles-off readability and that poison plus unavoidable damage does not form a guaranteed death sequence.

### Crypt Lich — the Archive of Last Names

**Inherited direction:** undead and phylactery objectives.

**Proposed arena:** three sarcophagus alcoves, a central lectern and four clear lanes. The player reads a short pattern from grave inscriptions before the fight; accessibility permits symbols/text instead of color alone.

**Loop:** three links protect the Lich. Each vulnerable link has a visible rune matching a currently lit inscription. Interact or strike in the indicated sequence to expose the boss. Wrong input produces a short recoverable add, capped at four, rather than resetting a ten-minute puzzle.

**Attacks:** named bone lanes, a slow reflectable soul projectile, a grave pull that leaves a marked escape sector and a modest melee sweep. At half health, the Lich erases one inscription temporarily; a memory at the lectern restores it.

**Magic:** Echo reads the missing inscription sooner; Hollow can redirect one owned soul hazard; Circlewrights stabilize an alcove; Radiance reveals the active link. Damage types do not replace the sequence.

**Rewards:** existing drain/Wither trophies; a memorial rune and archive decoration. Soulreaver healing must be included in the Vitae anti-loop audit. No farming village NPC deaths for the invitation.

### Iron Dreadnought — the Foundry Trench

**Inherited direction:** trench machinery, coil charge and vent exposure.

**Proposed arena:** a ruined industrial test pit with three grounded pylons, two service gantries and permanent cover. Use the approved dieselpunk art language and actual Jugcraft materials. It is an encounter set piece, not a source of free dismantled tier-five machinery.

**Loop:** an announced coil charge selects a lane. Players turn a fixture to route the discharge into a ground terminal. A correct route overloads armor and opens vents for six seconds. Fixtures use encounter state, not an unrestricted cable network that can drain a player's base.

**Attacks:** piston punch, rivet fan, a telegraphed mortar arc and a slow trench sweep. At 50%, one terminal is obstructed by an encounter-owned component that can be removed through a short interaction. Keep at least two valid routes.

**Magic:** Tempest primes a terminal, Strata braces against a piston, Clockheart support operates a permitted fixture, and Tide cools a vent briefly. Normal interaction performs the same essential routing.

**Rewards:** existing shock trophies, an optional precision actuator variant and an industrial banner. First electricity, steel, standard actuators and chemical production remain independent. Test shields, piercing ammunition, explosive launchers and support-worker credit against current main.

### Alpha Werewolf — the Moonlit Pursuit

**Inherited direction:** full moon and pack tactics. Use a stored invitation or a configured nonseasonal route so the encounter is not calendar-locked.

**Proposed arena:** a moonlit woodland ring with clear paths and three bell clearings. The pack marks a target before a hunt; ringing the correct bell interrupts coordination, creating a recovery opening.

Use a 0.7-second claw combination, a 1.2-second pounce shadow, a marked howl and at most three pack allies. At low health, the boss changes pursuit route rather than simply doubling unavoidable speed. Ground trails remain visible without particles.

Echo reads the hunt target; Verdance creates an allowed short refuge; Tether interrupts one pack signal. No spell permanently roots the boss. Rewards connect Moonfang/Howler and optional tracking equipment. Transformation is a separate offered story with explicit acceptance and cure, never an automatic on-hit punishment.

### Storm Roc — the Crown of Thunder

**Inherited direction:** high peaks, lightning rods and dives.

**Proposed arena:** a sheltered summit ring with four rods, ledges and recovery routes. Falling invokes lair recovery, not inventory loss. A marked rod becomes charged before a dive; align the dive with that rod to ground the Roc briefly.

Attacks include a one-second wing cone, a two-second dive lane, wind strips with persistent ground arrows and widely spaced lightning marks. Airborne periods must remain short enough that melee players regularly receive ground openings. Do not require flight equipment.

Tempest handles charge more conveniently, Radiance exposes the intended rod, Strata reduces ordinary displacement. Existing Gale trophies and a weather-vane lens reward the fight; normal lightning protection and weather information remain craftable.

### Abyssal Leviathan — the Drowned Ring

**Inherited direction:** trench, whirlpools and tide weapons.

**Proposed arena:** a partially flooded ring with dry refuges, two moorings and walkable submerged shelves. Give players a readable oxygen/preparation route, including ordinary supplies. Do not create new manual gas-pressure maintenance.

Operate the moorings to redirect a current and expose the creature's flank. Telegraph a tail lane, suction cone and rising-water event separately. A two-second current warning includes arrows and a safe anchoring point. Never combine all refuges flooding with unavoidable suction.

Tide assists a mooring, Rime stabilizes a short marked footing, Strata braces, Verdance supports recovery. Tidebreaker/Leviathan Hook remain the trophy direction. A decorative tidal engine or optional current-control rune is a suitable extra; essential water pumps and transport cannot require the boss.

## 4. Six new encounters that broaden the magical systems

### N1. The Glass Abbot — revelation and reflection

**Role:** first new original encounter after the Yeti; Radiance/Echo and readable support objectives. **Prototype:** 340 health, armor 6.

**Invitation and place:** assemble a Glass Accord from ordinary glass, a prepared lens and Radiance research. Enter an abandoned optical chapel with a 36-block nave, three rotating mirrors and four permanent pillars. Reflections are stylized authored beams; the fight does not require expensive real-time ray-traced reflections.

**Core loop:** the Abbot's glass mantle refracts direct attacks. A mirror can be rotated through four fixed angles. A three-second announced beam must strike two marked mirrors and return to the mantle. Correct routing opens an eight-second vulnerability. Ordinary interaction rotates mirrors; Radiance can preview the next segment. All beam collision is server-calculated and bounded.

**Attacks:** a 0.8-second fan of three shards; a one-second marked prism line; a 1.5-second false-image cast where only the real figure casts a patterned shadow. At half health one mirror is veiled for four seconds, but the shadow and floor diagram preserve a solvable route. Reflection does not require recognizing color alone.

**Optional magic:** Echo retains the previous correct arrangement briefly; Tether holds one mirror against a scheduled turn; Strata braces a shard push. These consume resources and cannot route all mirrors automatically.

**Rewards:** first-win choice of a Lantern Lens or Mirror Inscription, plus glass ornaments. Lens reveals one approved concealed interaction from closer range; inscription changes a ward into a single weak-projectile reflection with a meaningful cooldown. Ordinary wards and research remain accessible.

**Build:** first implement deterministic beam-to-fixture geometry, then the mirror state machine, then phase attacks, then art. Test rapid clicks, two players rotating one mirror, duplicated projectiles, phase reset and reflection attribution. A mirror never reflects an unrelated player's spell outside the instance.

### N2. The Verdigris Warden — repair a hostile garden

**Role:** Verdance/Strata, gardening and constructs. **Prototype:** 380 health, armor 10; optional nonlethal resolution.

**Invitation and place:** locate a neglected conservatory or craft its invitation through ordinary garden mastery. The arena has four irrigation beds, a central copper-and-root construct and safe stone paths. This is a contained damaged garden, not pollution destroying crops across the world.

**Core loop:** the Warden protects four overgrown valves. Clear a marked root knot and deliver a prepared water dose to restore a valve. Each restored bed removes one armor layer. All four restored opens a choice: fight through the remaining health, or maintain the beds through a final twenty-second test to calm it. Both resolve the encounter and give equivalent primary rewards.

**Attacks:** 0.9-second root sweep, 1.5-second seed barrage with clear safe gaps, two-second uproot circles and a slow vine grab. At two restored beds it alternates dry and wet pressure; no permanent loss of the player's supplied rare plants.

**Optional magic:** Verdance reduces one tending interaction, Strata stabilizes a safe path and a construct can carry one reserved water dose. Ordinary buckets and manual interaction work. A helper cannot instantly restore all four valves.

**Rewards:** a grafting frame variant, a decorative Warden seed and a limited garden-inspection rune. The core grafting workstation is craftable normally. The calm ending changes dialogue and trophy appearance, not statistical superiority.

**Build:** write valve/water reservation logic before AI. Distinguish restore, calming success, combat death and reset so only one reward path fires. Test all endings, a full inventory, broken invitation setup and multiple simultaneous water deliveries.

### N3. The Astral Adjudicator — read a moving sky

**Role:** Starwatchers/Radiance/Echo; mastery of forecasts without waiting for a real date. **Prototype:** 360 health, armor 8.

**Invitation and place:** spend stored attunement to enter a pocket orrery. Three rings contain fixed symbols and four safe platforms. Actual progression uses existing calendar occurrence records; the encounter's scripted sky is local presentation, not a change to world time.

**Core loop:** the Adjudicator announces a three-symbol sequence in sound, text and light. Rotate two rings to align the sequence while avoiding an attack. Each correct alignment lowers a shield and creates a six-second opening. Sequences use a small authored set in the pilot, with enough repeatable logic to learn them.

**Attacks:** a 1.2-second meridian sweep, a two-second marked star fall and a slow orbiting hazard capped at three. At half health, the next sequence contains one remembered symbol from the previous alignment. An accessible journal panel retains the visible sequence; this is spatial planning, not a test of working memory accessibility.

**Optional magic:** Echo provides a brief ghost of the former ring, Radiance highlights a valid connector, Tempest moves one ring through a paid fixture action. None forges extra occurrence claims.

**Rewards:** observatory forecast display variant, an orbit ornament and a choice of two mutually exclusive attunement conveniences. No permanent flat bonus to every school and no calendar-speed control.

**Build:** use deterministic ring state and server-chosen sequences; replay on reconnect without rerolling. Test time-command changes, duplicate invitation uses and completing two instances during one sky occurrence.

### N4. The Gilded Assayer — conservation made physical

**Role:** Balancewrights/Tide/Strata and alchemical diagnostics. **Prototype:** 400 health, armor 10.

**Invitation and place:** enter an abandoned assay court through an advanced but ordinary material audit. Two weighing platforms, three furnaces and a central adjudicating construct form the arena.

**Core loop:** the arena supplies marked temporary ingots of three weights. Distribute them to balance the displayed target while dodging. Correct balance exposes the Assayer for seven seconds. The temporary pieces exist only as encounter tokens, cannot enter normal inventories or equivalence and disappear on exit.

**Attacks:** a one-second falling-weight lane, a 1.5-second molten arc and a two-second floor tilt with a marked stable strip. At half health a piece is coated, and an assay fixture reveals its true weight. Wrong placement gives clear feedback; it does not steal the player's permanent inventory.

**Optional magic:** Tide clears a coating, Strata braces during tilt, Radiance reads an assay label earlier. Manual fixtures provide every necessary result.

**Rewards:** an assay lens, decorative scale and a salvage convenience that remains net-loss after all inputs. Never reward unrestricted transmutation, a higher universal EMC equivalent or permanent zero-loss crafting.

**Build:** the puzzle model is pure integer arithmetic with bounded tokens. Validate solvability of every authored state. Test token leakage, hopper extraction, dimension transfer, reward conversion and a player disconnecting while holding a piece.

### N5. The Red Orchard Regent — life shared under pressure

**Role:** Crimson Vigil/Verdance and meaningful support play. **Prototype:** 360 health, armor 8.

**Invitation and place:** a voluntary garden rite using prepared fruit and an ordinary Vitae budget. No villager sacrifice and no unwilling player extraction. The arena contains four heart trees and a central Regent.

**Core loop:** the Regent draws sap from two marked trees. Players interrupt the links or redirect a limited arena-provided restorative charge to a threatened tree. A saved tree opens a brief exposure. Do not make actual player self-harm the only way to progress.

**Attacks:** 0.8-second thorn arc, 1.5-second marked root grasp, a two-second draining cone with escape lanes and a limited fruit-add phase. At half health one tree becomes a decoy, identifiable through its pulse and a textual inspect cue. Saving trees and effective ally healing qualify for rewards.

**Optional magic:** Verdance stabilizes a tree, Tether severs one link, Crimson preparation allows a voluntary risky shortcut with a hard health floor and no exhaustion refund. Ordinary severing and supplied charges remain enough.

**Rewards:** a sanctuary decoration, a limited emergency-support rune and an elective Thornheart growth deed. No upgrade that pays back more health/Vitae/Focus than it costs. Different rescue and combat approaches give equivalent core rewards.

**Build:** attribute tree saves independently of boss damage. Audit heal-from-damage, lifesteal, death/revival, regeneration, potions and allied self-damage as one economic graph. Reduced particles must retain tree state.

### N6. The Somnolent Cartographer — rebuild a broken route

**Role:** Dreamwalkers/Echo/Tide and world design. **Prototype:** objective encounter with three reconstruction stages; a conventional health bar is optional, not mandatory.

**Invitation and place:** enter a short dream through the existing escrow system with a previously discovered map fragment. The arena consists of three islands and authored bridges. It never edits the player's actual world.

**Core loop:** the Cartographer erases one route and projects two false ones. Players inspect a memory marker, place a limited supplied route token at an anchor and cross to recover a fragment. Completing three distinct routes resolves the encounter. Combat attacks suppress hazards for a short interval; raw damage cannot skip every reconstruction.

**Hazards:** one-second ink lane, two-second island fade, a slow pursuing eraser and a marked current. Permanent staging platforms and a wake exit always remain. A fall resets only that local attempt with a bounded penalty; it does not duplicate escrow or consume a unique fragment repeatedly.

**Optional magic:** Echo previews a remembered bridge, Tide counters one current, Radiance marks the truthful anchor. The normal memory marker is enough.

**Rewards:** dream cartography decoration, a limited exploration note and an editor-preview cosmetic. No free teleport to unknown coordinates or writable access to server world-design files.

**Build:** test wake, defeat, objective victory, disconnect and restart at every stage. Store one completion receipt. Temporary dream items and blocks cannot escape. Do not expand into an endless procedural dream dimension until this short encounter is enjoyable.

## 5. Later backlog: give each a reason to exist

| Candidate | Distinct promise | Avoid |
|---|---|---|
| Pumpkin King | Cooperative harvest preparation and protecting a bounded field | Seasonal-only core ingredients or crop destruction outside the instance |
| Horseman upgrade | Mounted pursuit with route choices and dismount openings | Replacing the already existing seasonal encounter silently |
| Marrow Colossus | Large readable joints and bone bridges in a Nether arena | More health without navigable weak points |
| Chorus Sovereign | Sound sequences, safe teleport anchors and End geometry | Random unavoidable teleport deaths |
| Hollow Monarch | Choice-driven court of echoes and pact consequences | Making Stolas or all spirits hostile by default |
| Kaiser's Zeppelin | Deck objectives, engines and safe boarding | A moving overworld machine that destroys towns |
| Frost Wyrm | Vertical aerial fight and thermal shelter | Repeating the Yeti's column-and-floor loop |
| Hive Matriarch | Interruptible colony logistics and brood roles | Unbounded entity spawning |
| Tideglass Curator, optional later | Preserve/restore staged exhibits under time pressure | Another mirror boss with a different skin |

## 6. Build and review order

1. Finish Yeti part two against the actual merged/updated hall.
2. Add contribution eligibility and objective capability seams using the two existing bosses.
3. Build Glass Abbot as a small new encounter that exercises those seams.
4. Choose Cinder Tyrant or Mire Hag next according to the owner's preferred school emphasis.
5. Build Iron Dreadnought only after relevant industrial materials and current gun balance are settled.
6. Introduce Warden and Stolas near the gardening/pact chunk.
7. Add astronomical, conservation and dream encounters after their noncombat systems already work.
8. Revisit the rest at the end of each chunk. Do not turn this ordered menu into a requirement to implement everything at once.

[Next: technology and economy](06-technology-and-economy.md)
