# Powered exosuit (Vanguard and Ronin)

Status: implemented on `feature/exosuit-28` (batch 28), stacked on `feature/gear-27`, awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 2 October 2026: "let's do the powered exosuit next use this as a reference (image 1) then make an alternate / variant style that looks like image 2". Claude had proposed the exosuit as a next step.
Owner: jimbozoomer-byte
Target milestone and tier: electronics and late game (titanium, processors, rocket pack)
Primary specialty and supported player role: exploring and fighting
Design inspiration:
- Mekanism's MekaSuit (MIT) inspired the idea of JE-powered armor with powers. No code or art was taken.
- The looks follow two images the owner shared:
  - Vanguard: heavy gunmetal plating with layered shoulder and skirt plates and teal lights.
  - Ronin: a crimson, silver and black mech samurai with a conical hat and a red energy blade.
- Only their themes and colours are followed. Every pixel and box is our own, drawn procedurally.

## Player experience
- **Four pieces** (helmet, chestplate, leggings, boots), as strong as netherite (3/6/8/3, toughness 3) and unbreakable.
  - Each holds 400,000 JE and charges at the charging station; capacity modules fit.
  - Each piece has its own power, and only while it is worn and charged. A flat piece is plain armor.

| Piece | Power | Cost |
| --- | --- | --- |
| Helmet | Night vision while it is dark (light below 8) | 2 JE/t while dark |
| Chestplate | Energy shield: up to 8 absorption points (4 hearts), one regrown every half second. Also a jetpack: hold jump in the air, as with the rocket pack | 4,000 JE a point; 50 JE/t of thrust |
| Leggings | +30% walking speed | 1 JE/t |
| Boots | No fall damage, and a full-block step | 1 JE/t |

- **Two liveries:**
  - **Vanguard** (as crafted): heavy gunmetal plate.
    - Helmet: a T-visor with a mouth grille and teal ear modules.
    - Chestplate: a segmented chest and abdomen, teal waist slits and an X-buckle belt, with a power pack on the back. Stacked 3D shoulder plates and slate-teal forearms with bindings.
    - Leggings: 3D skirt plates and knee guards.
    - Boots: armored, with toe caps.
  - **Ronin**: crimson, silver and black.
    - Helmet: a silver faceplate with glowing red eyes, under a wide 3D conical hat with hanging cords.
    - Chestplate: crimson pectorals over black mechanical ribs. Big crimson 3D shoulder plates with silver ports.
    - Leggings: a silver belt, and long 3D crimson skirt strips with a black tabard bearing a red sigil.
    - Boots: silver greaves with clawed feet.
  - **Ronin Katana**: the power katana with a crimson energy blade (same stats). Its outline and crimson glow fringe are opaque since 6 October 2026 (part-transparent, they showed as a see-through rim in the hand).
- **Repainting:** a Ronin Livery, the piece (or the power katana) and red dye at a smithing table make the Ronin version. A Vanguard Livery and cyan dye paint it back. Smithing keeps the charge, modules and enchantments.
- Art:
  - 32x32 icons;
  - 128x64 worn layers (double resolution);
  - 3D parts drawn by a render layer that follows each body part, so they swing with arms and legs.
- Advancements: Steel Samurai (an exosuit piece) and Masterless (a Ronin piece or katana). Handbook pages: Powered Exosuit and Liveries (Steel chapter).

## Connections
- Inputs: titanium plates, a processor, tinted glass, lithium cells, advanced circuits and neodymium magnets.
  - The chestplate is built around a **rocket pack**.
  - The boots are built around **free runners**.
- Output consumer: the player.
- Technology connection: the shared `Chargeable` energy (charging station, capacity modules) and a new `Jetpack` interface (the rocket pack and the exosuit chestplate).
- Magic connection: none.
- Required vs optional: optional end-game gear.

## Balance and automation
- A full charge lasts:
  - leggings or boots: 400,000 ticks (about 5.5 hours) of wear;
  - helmet: 200,000 dark ticks;
  - chestplate: 100 shield points (50 hearts absorbed), or 8,000 ticks (400 s) of flight, from the one charge.
- The shield only spends when it regrows a point, so standing around costs nothing.
- Protection equals netherite; the edge is the powers and no durability loss, paid for in JE.
- No loop: nothing turns the suit, its charge or its liveries back into anything. Liveries cost two dyes and a steel plate for two, and smithing consumes the template and dye.
- These numbers are first guesses for the owner to tune (tools/exosuit.py; the checker keeps Java in sync).

## Multiplayer and persistence
- Powers run on the server (`ServerTickEvents.END_SERVER_TICK`, every player).
  - Bonuses are transient attribute modifiers, added while a piece works and removed the tick it stops. Nothing is saved on the player.
  - The suit's night vision is ambient and particle-free; it is removed when the helmet stops providing it. A potion's night vision is left alone.
- Energy is the existing `jugcraft:energy` item component. The jetpack uses the existing rocket-pack thrust packet, rate-limited per player.
- Recipes follow the `machines` feature switch.

## Dependencies and assets
No new dependencies. Art is drawn by `tools/exosuit_art.py`, and the 3D part boxes are listed in `tools/exosuit.py` (exported to `worn_models.json`). The Ronin katana reuses `tools/hitech.py`'s katana with a crimson palette. Nothing is traced or recoloured from the reference images, vanilla or Mekanism.

## Verification
- `tools/check_mod_data.py`:
  - `JugcraftExosuit` liveries and pieces and `Exosuit` numbers match `tools/exosuit.py`;
  - every icon, worn layer and 3D part texture exists, and every part is in `worn_models.json`;
  - recipes (including the smithing ones) resolve.
- Game tests (CI):
  - `exosuitPowersRunOnCharge`: speed, step, fall and shield with charge; costs; all removed when flat.
  - `exosuitShieldRegrows`: absorption returns, for 4,000 JE a point.
  - `exosuitChestplateIsAJetpack`: thrust from the chestplate's charge; a non-chest piece does not fly.
  - `liveryRepaintsAndKeepsCharge`: the smithing recipe turns a charged Vanguard helmet Ronin with its charge.
- Client screenshots: `jugcraft_exosuit_vanguard` (front), `jugcraft_exosuit_vanguard_back`, `jugcraft_exosuit_ronin`.
- Not run:
  - the helmet's night vision in real darkness (not covered by a test);
  - client play, flying, two players.

## World and event applicability
Not applicable.

## Rollout and open questions
- A module system (choosing which powers each piece has) is not included; each piece has one fixed power.
- The night vision cannot be switched off except by taking the helmet off or standing in light.
- The 3D steel Pickelhaube spike could reuse this render layer if the owner wants it.
