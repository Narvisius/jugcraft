# The Cinder Kiln, and the Kiln Seal

Status: implemented in source; CI passes (below), and its client test's pictures are in the record. Part 1 of boss 4, the Cinder Tyrant, in the [bosses plan](../branches/BOSSES.md#the-cinder-tyrant-the-plan-being-built), built by [the boss playbook](../branches/BOSS_PLAYBOOK.md): the Cinder Kiln, a fourth lair on the shared framework ([hollow-acre.md](hollow-acre.md)), and the summoning that opens it. The Cinder Tyrant and his loot are part 2, in their own pull request ([cinder-tyrant.md](cinder-tyrant.md)). It has not been played by hand, and the two-client dedicated-server playtest is still to do.
Proposal issue: none. On 10 October 2026 the owner asked to "do the next boss same way" as the Yeti King; the Cinder Tyrant is the next of the first eight in the bosses brainstorm, and the planning pack's "Kiln Beneath the Mountain" ([chapter 05](../plans/concordance-expansion-2026-10-10/05-new-boss-compendium.md)).

Target milestone and tier: Specialization tier (dungeon expeditions), as the other lairs are. The seal takes what a player has once they reach the Nether: obsidian, blaze powder, gold and a magma block.
Primary specialty and supported player role: adventuring, in the Nether and the volcanic lands.

## Player experience

### The Kiln Seal

1. Craft a **Kiln Seal**: four obsidian round a magma block, marked with two blaze powder above and rimmed with two gold ingots below (blaze powder, obsidian, blaze powder over obsidian, magma block, obsidian over gold, obsidian, gold).
2. Press it into a **magma block** (`#jugcraft:kiln_seal_ground`), in the Nether or in a volcanic land of the Overworld: the Volcano or the Cinder Barrens (`#jugcraft:kiln_seal_lands`). It works at any hour.
3. The magma cracks open and a **vent** of sparks and smoke rises from it. You sink through the stone and land on the ledge in the Cinder Kiln. The seal is used up.
4. For 60 seconds (`lairs.gate_seconds`) the vent stays open, crackling. Anyone who uses it sinks in after you, while the party has room (`lairs.party_size`). Nobody is taken in by standing near it.
5. When something is missing, the seal only says what, and nothing is used up:
   - pressed into anything but magma: "Press the seal into a magma block";
   - outside the Nether and the volcanic lands: "The seal takes only in the Nether, or in a volcanic land";
   - where a vent is already open, within 3 blocks: use the vent to follow;
   - when every instance of the kiln is taken.
6. Every press, answered or not, rests the seal for two seconds.

The Cinder Tyrant is no Witching Season boss: the seal works all year, whatever `lairs.off_season` says.

When the instance closes, the vent closes at once.

### The Cinder Kiln

A kiln hollowed out of a volcano's roots, 72 blocks across, 46 high and 80 long. Its wall is basalt veined with glowing magma up to a band of blackstone bricks; above the band it is a dome of buff **kiln brick**, braced by eight ribs of blackstone bricks that meet round a **vent** at the crown, open to a smoky red sky. Ash drifts in the air, and the slag lights it red.

- **The arrival:** a ledge high in the south-south-east wall, seven blocks above the bowl, under an alcove at the mouth of a lava tube long since cooled. You land there facing the crucible.
- **The way home:** behind the arrival, Grey Mist hangs in an arch of kiln brick across the lava tube. Use it at any time, in or out of a fight, to go back to exactly where you stood.
- **The stair:** polished blackstone steps curving down the south-east wall from the ledge to the bowl, half a block lower every few degrees, so no step is more than half a block: you walk down it (and back up it) without jumping.
- **The bowl**, the arena: 44 blocks across, floored with **cracked basalt**, its cracks glowing faintly. Round it, to the wall, a ring of rough basalt.
- **The forge mouth:** an arched recess in the north wall. A **lip** of kiln brick runs across its front, three blocks above the bowl, and behind it a pool of **molten slag** fills the forge. In the middle the slag runs over the lip and falls into the **heat channel**, a trench of slag across the floor to the **crucible**: a round pool of slag in the bowl's north third, three blocks deep and rimmed with kiln brick, but where the channel runs in. The Tyrant will wait sunk in it (part 2).
- **The slag** glows, and whoever stands in it burns, sneaking or not: a point of damage, as a magma block deals, and burning for three seconds. Fire Resistance stops it, and so does being unhurtable (creative). Feet sink into it a little.
- **The sluices:** three great iron **sluice gates** in the west, east and south walls, each in a recess, with its red **wheel** on its right as you face it. A paved **quench trough** of trough stone, three blocks wide, runs from under each gate toward the bowl's middle, a little below the floor.
  - Use any part of a gate to turn it. A ready gate opens with a clank and a rush of water: its panels rise, water gushes from under them, and its trough floods for 10 seconds.
  - Then the gate closes and the trough drains, and the gate fills again for 20 seconds. Turned meanwhile, it only says "The sluice is already open" or "The sluice is filling again".
  - A ready gate seeps water at its foot.
  - Standing in a flooded trough puts out the burning, with a hiss of steam.
  - The flooded troughs are where the Tyrant's Body Slam quenches him ([cinder-tyrant.md](cinder-tyrant.md)).
  - In his Eruption one gate at a time is **choked** with slag: a glowing crust over its panels, embers at its foot, and turned, it says "Slag chokes the sluice: it will not turn". The choke moves from gate to gate, and every gate is clear again when the fight ends. Part 2 added this fourth state of the gate.
- **The shelves:** four raised shelves of smooth basalt, rimmed with polished basalt, round the bowl (north-west, north-east, south-west and south-east), a step up. The slag never reaches them.
- **The light:** hidden light blocks, and the slag's own glow.

Everything else is the shared framework ([hollow-acre.md](hollow-acre.md#lairs-the-shared-rules)): an instance per opening, at most eight; at most four players; nothing can be built or broken (the sluice gates are the kiln's one fixture besides the Grey Mist); the mist's toll at the edges; Grave Goods on death; closing after 30 seconds empty.

## Changes from the plan

- **The arrival is in the south-south-east wall, and the stair curves.** The plan has the ledge in the south wall and the stair coming straight down. The south sluice stands due south, so the ledge sits a little east of it and the stair curves down the south-east wall instead. Its steps fall half a block every 3.5 degrees, which keeps every step between neighbouring blocks to half a block.
- **The stair is polished blackstone,** with half steps of its slabs; vanilla has no basalt slab.
- **The floor is its own block, cracked basalt,** a fifth lair-only block. The plan named four (kiln brick, molten slag, trough stone and the sluice gates). The bowl reads as an arena, and part 2 can put back exactly what the channel's surge covers.
- **A flooded trough puts out the burning.** The plan has the troughs quench only the Tyrant. Water puts out fire, so they do it for anyone, and it gives the sluices a use beyond baiting him.
- **The Grey Mist is behind the arrival,** across the lava tube's mouth, rather than beside it.
- **The crucible has a rim** of kiln brick, a block high, open where the channel runs in.
- **The vent is the Mist Gate.** The Last Rites' gate entity serves, as it did for the Frost Horn: on the magma it draws smoke, lava sparks and flames, and those who follow sink in.
- **The Kiln Seal stacks to 16.**
- **Tyrant Scale** in a cheaper seal comes with the Tyrant's loot in part 2.

## Connections

- **Inputs:**
  - obsidian, from water and lava, mined with a diamond pickaxe;
  - blaze powder, from the Nether's blazes;
  - gold;
  - a magma block, from the Nether or the volcanic lands, or crafted from magma cream;
  - a magma block to press the seal into, in the Nether (common there) or in the Volcano or the Cinder Barrens.
- **Outputs:** the way into the Cinder Kiln, where the Cinder Tyrant will wait (part 2).
- **Technology and magic:** none yet; the kiln is an adventuring destination. Part 2 brings Arms VII's two Cinder Tyrant trophies (the Cinderbrand and the Magmaw) into survival, and the plan's Ember link, Hearthguard's Fire Resistance, helps against the slag (as a potion does).
- **Reachable entry path:** every input is reachable without the kiln, by a player who has reached the Nether: no circular unlock.

## Balance and automation

- **The cost:** every opening uses up a Kiln Seal: four obsidian, two blaze powder, two gold and a magma block. There is no cooldown beyond the seal's two-second rest: the seal is the limit, as the other rituals' items are.
- **No loop:** nothing comes out of the kiln in part 1. Its lair-only blocks drop nothing and have no items.
- **No automation:** only a player's own press opens the kiln, only a player's own use of the vent takes them in, and only a player in the kiln turns a sluice.

## Multiplayer and persistence

- **Server authority:** the server decides everything. Clients only send the use: the press, the refusals, the vent and the entry, the sluices' turns, flooding and draining, and the slag's burn all happen on the server. The seal's rest is counted there.
- **What is saved:**
  - the vent is never saved, since a restart closes its instance;
  - a sluice's turn runs on block ticks the gate schedules, which are saved with the instance's chunks. Each instance is placed fresh, and a tick left over from an earlier instance finds a ready gate and does nothing;
  - a player's visit and Grave Goods are saved with the player, as for every lair.
- **Chunks:** no chunk is force-loaded. The vent shows itself only where its chunk is loaded (an entity, ticked by its chunk).
- **Griefing:** nobody is pulled in. A second seal where a vent is open is refused and kept. Nothing in the kiln can be changed but its sluices, which reset with the instance.
- **Still to do:** the two-client dedicated-server playtest.

## Dependencies and assets

No new dependency. Everything is drawn by code:
- the kiln is laid out by `tools/cinder_kiln.py`, on `tools/lair_layout.py`. Its template has 12,573 blocks: only the stone touching the open air is kept, but the crucible keeps its depth. Nothing in it burns;
- its five lair-only blocks' textures (and the troughs' water and the sluices' frame, panels, gushing water and wheel) are painted by `tools/cinder_kiln_textures.py`, in the manner of the vanilla blocks (`tools/block_style.py`);
- the Kiln Seal's icon is a 16×16 map, `tools/item_icons/kiln_seal.txt`, drawn by [ITEM_ICONS.md](../ITEM_ICONS.md), with two new materials, `obsidian` and `magma`, in `tools/icon_materials.py`, and checked by `tools/check_icon_maps.py`;
- the dimension files are written by `tools/lair_data.py` from the kiln's line in `tools/lairs.py`. Its music is vanilla's basalt deltas.

No Mojang texture is read, traced or copied.

New IDs (all under `jugcraft`):
- the dimension, dimension type and biome `cinder_kiln`;
- the structure `lair/cinder_kiln`;
- the lair-only blocks `kiln_brick`, `cracked_basalt`, `molten_slag`, `trough_stone` and `sluice_gate`;
- the item `kiln_seal`;
- the block tag `kiln_seal_ground` and the biome tag `kiln_seal_lands`.

Nothing is renamed.

## Verification

![A Kiln Seal pressed into a magma block on the test world's flat land, made a volcano's: the magma cracked open into a vent, sparks and smoke rising from it](../images/ingame_kiln_seal_vent.jpg)
![The view from the arrival ledge: the dome of kiln brick ribbed with blackstone, the bowl's cracked basalt, the shelves, and the crucible glowing under the forge mouth at the far end](../images/ingame_cinder_kiln_arrival.jpg)
![The bowl from near the stair's foot: a raised shelf in front, the cracked basalt glowing in its cracks, the forge mouth beyond](../images/ingame_cinder_kiln_bowl.jpg)
![The crucible under the forge mouth, its basalt rim round the slag, the heat channel running in from the lip](../images/ingame_cinder_kiln_crucible.jpg)
![The slag pouring over the forge's lip and down the heat channel](../images/ingame_cinder_kiln_forge.jpg)
![The west sluice open in the wall, its trough flooded across the bowl's floor](../images/ingame_cinder_kiln_sluice.jpg)
![A raised shelf of basalt in the bowl, the dome's glowing cracks beyond](../images/ingame_cinder_kiln_shelf.jpg)
![Looking up into the dome: the blackstone ribs meeting at the vent](../images/ingame_cinder_kiln_dome.jpg)
![The arch beside the arrival ledge, filled with Grey Mist](../images/ingame_cinder_kiln_exit.jpg)
![A second Kiln Seal pressed into a magma block in the Nether: its vent among the netherrack](../images/ingame_kiln_seal_vent_nether.jpg)

*The client game test's pictures (CI, commit `042b98d`): the vent where the seal was pressed, then in the kiln the view from the ledge, the bowl from the stair's foot, the crucible, the slag over the lip, the west sluice's flooded trough, a shelf, the dome and the Grey Mist's arch; last, the vent a second seal opened in the Nether. The test client renders at 480x270.*

CI (10 October 2026, GitHub Actions, the pins in [PLATFORM.md](../PLATFORM.md)):

| Commit | What ran | Result |
| --- | --- | --- |
| `8a7a4d4` | Build, data audit, game tests, client game tests | **Two failures.** `mod` passed all 1299 required server game tests, the kiln's six among them, and `optional integrations absent` passed. `repository` failed its check on how the client tests are shared out: with the kiln's client test class, the estimate for the busiest of the four client jobs came to 25 minutes 10 seconds, over the 25 it allows. The client test stopped at the seal's press: it judged the press by comparing the item's result with the game's one plain success value, and the result was something else |
| `042b98d` | The same, the client tests now shared between five jobs ([TESTING.md](../TESTING.md)), and the press judged by whether it took effect (the checks after it still look for the open kiln, the used seal and the vent) | **All pass:** all 1299 required game tests, and every client test class (a workflow change runs them all) in five jobs of 12 to 28 minutes, `CinderKilnClientGameTests` among them. The pictures above are from this commit |

Run locally:

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py`: now also checks `Lair.CINDER_KILN` against `tools/cinder_kiln.py` (size, arrival, centre, bounds, floor, no moon); the seal's rest and reach, the slag's damage and burning, the sluices' timings, walks and parts, and the trough's height against `tools/lairs.py` and `tools/lair_data.py`; the seal's ground and land tags; and the kiln itself (`check_cinder_kiln`, below). Thirteen deliberate changes to the Java and eight to the layout, one at a time, each failed it | Pass, 2165 IDs |
| `python3 tools/check_icon_maps.py tools/item_icons/kiln_seal.txt` | Pass |
| `python3 tools/generate_material_data.py` and `tools/generate_textures.py`, then `git status` | Write this part's data and textures only (the PNGs they re-encoded with identical pixels were put back) |
| `javac` on the new Java, for syntax only | Only the expected errors for the game's missing libraries |
| `./gradlew build`, game tests and client game tests | Not run locally (the Fabric Maven is out of reach here); to be run by CI |

`check_cinder_kiln` checks the kiln as `tools/cinder_kiln.py` builds it:
- the arrival stands on the ledge with room overhead, and the Grey Mist hangs in its arch;
- no two neighbouring blocks of the ledge and the stair differ by more than half a block, across a corner too;
- a walk from the arrival reaches the bowl's middle, and back, with no step more than half a block and without treading the slag;
- the bowl is cracked basalt but for its troughs, channel and crucible;
- the crucible is slag three deep on basalt, rimmed but where the channel runs in, and the slag runs over the lip and down the channel to it;
- the lip has room over it;
- each sluice's gate is whole and faces into the kiln; the walk `SluiceGateBlock` makes from any of its blocks finds all of it and nothing else, and the walk from under it finds its whole trough and no other; its wheel can be reached;
- each shelf is a raised step inside the bowl with room over it, more than three blocks from the slag;
- no magma lies where anyone could stand, and nothing burns.

What the breaking found:
- The audit's first run caught the audit itself, counting the void outside the kiln's shell as open air; it now walks only the kiln's own open space.
- A stair four times steeper slipped through at first: another way down could still be walked half a block at a time. The audit now checks every step of the stair itself.

The game tests (`CinderKilnGameTests`) show what a game-test server can:
1. the kiln's dimension type and biome load from their files;
2. its template loads in the running game, at the size `Lair.CINDER_KILN` gives, with all five of its own blocks and the Grey Mist, and the game's data version;
3. the seal's checks: not on basalt, only on magma; refused on magma in the test world's flat land; volcanic land is the whole Nether and the Overworld's Volcano and Cinder Barrens, not its plains or the End;
4. the seal pressed in full when no instance can open: the kiln is full, the seal is kept, nobody moves, no vent opens; on basalt it is refused and kept;
5. the slag burns a pig standing in it, not a pig under Fire Resistance, and harms a player in survival but not in creative; feet sink into it, and a trough's stone sits lower; a flooded trough puts out a burning player and a dry one does not; the five blocks cannot be broken and have no item;
6. a sluice gate floods its own trough and not a strip it does not touch, refuses a second turn while open, closes and drains after its flood, refuses a turn while filling, and is ready again after.

The client game test (`CinderKilnClientGameTests`, CI job `client`) runs the seal from end to end in a real world, with its one player in survival:
1. pressed into basalt, or into magma in the plains, the seal is refused and kept;
2. the land made a volcano's (`fillbiome`), the seal pressed into the magma opens an instance in the kiln's own dimension and places the kiln. It checks the ledge and a half step of the stair, the arch's mist, the bowl's cracked basalt, the channel's slag, the crucible's depth, the lip, the west sluice's ready wheel and dry trough, and a shelf. The seal is used up and rested, a vent opens on the magma, and the player lands on the ledge, unable to build;
3. the wheel can be used and the floor cannot be built on. Turned, the west sluice floods its trough and not the east one's. The slag sets the player burning, and the flooded trough puts it out. Falling out of the kiln throws the player back to the ledge for the mist's toll;
4. leaving takes the player back to where they stood. A second seal at the open vent is refused and kept, and using the vent takes the player back into the same kiln;
5. when the instance closes, the player goes home and the vent is gone.

Then it takes ten pictures: the vent on the magma, and in the kiln the view from the ledge, the bowl from near the stair's foot, the crucible and channel under the forge mouth, the slag falling over the lip, the west sluice open over its flooded trough, a shelf, the dome and its vent, and the Grey Mist's arch. Last, a seal pressed into a magma block in the Nether opens a second kiln, and the tenth picture is that vent.

Not run: the two-client dedicated-server playtest, and play by hand.

## World and event applicability

The Cinder Kiln is its own dimension, so nothing here changes the Overworld or the Nether but the vent, which lasts its minute. The seal works at any hour, all year, on magma in the Nether or the volcanic lands. No season gates it.

## Rollout and open questions

The shared framework's defaults hold: four players and eight instances, Grave Goods on death.

Part 2, the Cinder Tyrant, adds:
- the fight in the bowl: hot, he takes half damage until his Body Slam lands in a flooded trough and quenches him;
- his Cinderlings, and the channel's surges;
- his loot: Tyrant Scale, the Cinderbrand and the Magmaw, the Salamander Charm, the Tyrant's Crest and the advancement Tempered;
- Tyrant Scale in a cheaper seal.
