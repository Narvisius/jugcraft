# Scuba gear, free runners, power weapons and plastic blocks

Status: implemented on `feature/gear-27` (batch 27), stacked on `feature/chemistry-26`, awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 2 October 2026, shared Mekanism jars ("lets integrate a bunch of this") and picked "Gear & weapons" from the list Claude offered.
Owner: jimbozoomer-byte
Target milestone and tier: oil and electronics (steel tier and later)
Primary specialty and supported player role: exploring and fighting; plastic blocks for building.
Design inspiration: Mekanism and Mekanism: Additions by aidancbrady and team (MIT): scuba gear, free runners, the Meka-Tana and Meka-Bow, plastic blocks. Mekanism: Weapons by oMeranha (MIT) was also shared. Only the ideas are taken; no code, data or art was copied.

## Player experience
- **Scuba mask and scuba tank.**
  - Worn together (head and chest), they let the player breathe under water.
  - The tank holds 8,000 mB of oxygen. Use it on anything holding oxygen to fill it: a gas holder, or a machine's oxygen tank (the air separation unit, the electrolytic cell).
  - Under water, with the mask on, it keeps the wearer's air full for 1 mB a tick: 400 seconds a tank.
  - Both are light armor (leather-like), mended with rubber.
- **Free runners:** rubber-soled boots.
  - No fall damage at all.
  - Step up a full block without jumping (+0.5 step height).
  - Iron boots' protection; mended with rubber.
- **Power katana:** a sword that runs on JE instead of wearing out.
  - 11 attack damage at 1.8 attacks a second; a netherite sword does 8 at 1.6.
  - 1,000 JE a hit; 200,000 JE full (200 hits). Empty, it hits for 1.
- **Power bow:** a bow that runs on JE.
  - Charged, it fires arrows of energy for 500 JE a shot (100,000 JE full, 200 shots), with no arrows needed.
  - Energy arrows fly 25% faster (3.75 against 3.0) and start from 3 damage against 2, so a full draw hits hard. They cannot be picked up.
  - Empty, it is an ordinary bow that uses the player's arrows.
  - Takes bow enchantments.
- Both weapons charge at the charging station, show an energy bar, and take capacity modules.
- **Plastic blocks in all sixteen dye colours:** eight plastic sheets around a dye make eight. Smooth and bright, as hard as concrete; mined with a pickaxe.
- Four advancements:
  - Deep Breath (scuba gear);
  - Light on Your Feet (free runners);
  - Charged Up (a power weapon);
  - Lego My Ego (plastic blocks).
- Handbook pages: scuba gear, free runners and the power weapons in the Steel chapter; plastic blocks beside the chlorine page.

## Connections
- Existing input producers:
  - rubber (polymerization);
  - steel and tungsten plates;
  - the fluid tank;
  - the lithium cell and advanced circuit (electronics);
  - plastic sheets (oil line);
  - oxygen (air separation unit, electrolytic cell, gas holder).
- Output consumer: the player. The scuba tank is a new consumer of oxygen.
- Technology connection: the charging station and capacity modules (shared `Chargeable` interface); Fabric fluid transfer for filling the tank.
- Magic connection: none.
- Required vs optional: all optional.

## Balance and automation
| Item | Cost to use | Full | Compare |
| --- | --- | --- | --- |
| Scuba tank | 1 mB oxygen a tick under water, only when air is short | 8,000 mB: 400 s | The air separation unit makes 2 mB/t at 64 JE/t: a tank is 200 s and 256,000 JE of it. Water breathing potions last 3 or 8 minutes. |
| Power katana | 1,000 JE a hit | 200,000 JE | Netherite sword: 8 damage, 1.6/s, 2,031 uses |
| Power bow | 500 JE a shot | 100,000 JE | Bow: 384 uses, needs arrows |

- Nothing turns oxygen, JE or plastic back into anything else, so there is no gain loop.
- Energy arrows cannot be picked up, so the bow cannot make arrows from JE.
- Plastic blocks: 8 sheets and a dye make 8 blocks. There is no recipe back to sheets.
- The katana's 11 damage is deliberately above netherite: it needs electronics (advanced circuit, lithium cell, tungsten) and costs JE every hit.

## Multiplayer and persistence
- Server-side: oxygen use, refilling, hits and shots all run on the server. The tank's oxygen is an item component (`jugcraft:oxygen`), and the weapons use the existing `jugcraft:energy` component.
- Free runners use vanilla attributes (`fall_damage_multiplier`, `step_height`), so the client and server agree on step height.
- Recipes follow the `machines` feature switch (scuba gear, free runners, weapons) and `crude_oil` (plastic blocks).

## Dependencies and assets
No new dependencies. All art is original; nothing is traced or recoloured from vanilla or Mekanism.
- **High-detail gear** (`tools/hitech.py`; the owner asked for "higher quality"), drawn procedurally:
  - **Power katana:** a 32x32 icon, animated, with a light running down the blade. Its shape follows an image the owner shared (wrapped grip, guard, collar, a long two-tone blade); its art is our own. It has a dark-spined energy blade with a white-hot edge, circuit nodes by the gold collar, a cyan-ringed guard, a wrapped grip with glowing cell windows, and a pommel status light.
  - **Scuba mask, scuba tank and free runners:** 32x32 icons and 128x64 worn layers (twice the vanilla armor resolution; the game scales the UVs to the texture). The worn gear is:
    - a dive mask with two tempered panes, a nose pocket, a strap and a head-up light;
    - twin yellow tanks on a manifold;
    - a webbing harness with a lit chest buckle and a pressure gauge;
    - a dive computer on each wrist;
    - white-shelled free runners with a cyan light strip, a sprung sole and a heel coil.
- The power bow keeps its 16x16 icon and draw frames from `tools/gear_textures.py`, and the plastic textures come from `tools/plastic.py`.
- Since 6 October 2026 the power katana's outline and the glow fringe along its edge (now the blade's own bright glow, opaque), and the scuba mask's, scuba tank's and free runners' outlines, are opaque (`tools/hitech.py`): drawn part-transparent, they showed as a see-through rim on the sprite in the hand.

## Verification
- `tools/check_mod_data.py` (340 IDs):
  - the gear extras registered in `JugcraftGear` match `tools/gear.py`;
  - the scuba tank's numbers match;
  - every icon, bow frame and worn layer exists;
  - the plastic blocks match `PetroBlocks.PLASTIC_COLORS`, with names, models, loot and recipes;
  - item definitions with nested models (the bow's draw) are checked all the way down.
- Game tests (CI):
  - `scubaTankKeepsAirUnderWater`: the air stays full under water and costs 1 mB; nothing happens without the mask.
  - `scubaTankFillsFromAGasHolder`: fills to 8,000 mB and leaves the rest in the holder.
  - `freeRunnersCancelFallDamage`: the attribute modifiers are present, alongside the armor.
  - `powerKatanaRunsOnCharge`: hits for 1 when empty; full damage and 1,000 JE when charged.
  - `powerBowFiresOnCharge`: one uncollectable arrow for 500 JE; an empty bow with no arrows fires nothing.
- Client screenshots: `jugcraft_scuba_gear_worn` (with the katana in hand) and `jugcraft_plastic_blocks`.
- Not run:
  - client play;
  - two players;
  - an actual fall with free runners (the test checks the modifiers, not a landing);
  - fighting a mob with the katana or bow.

## World and event applicability
Not applicable: no worldgen, mobs or dimensions.

## Rollout and open questions
- Mekanism's MekaSuit, jetpack and flamethrower are not included. The rocket pack already covers the jetpack.
- Mekanism: Weapons' guns were not added. The grenade launcher (batch 18) is Jugcraft's ranged explosive.
- The katana's damage (11) and the bow's energy cost are first guesses for the owner to tune.
