# Diesel Walker (batch 47)

Status: implemented (pending CI and review)
Proposal issue: none; the owner asked for it in chat ("a mech can have a drill arm for sure", then "start the mech batch") and sent three reference pictures. They were used only as mood: a blocky rusty mech with an open cockpit and a visible pilot, big plated fists, barrel shoulders, heavy feet and a glowing chest core. Nothing was copied from them.
Owner: jimbozoomer-byte
Target milestone and tier: petrochemical tier (a diesel engine, a mining drill, steel)
Primary specialty and supported player role: mining and combat

## Player experience
A rideable mech about four blocks tall, in the look of the dieselpunk giants:
- chipped red plating over riveted rust;
- barrel shoulders and heavy feet;
- an open chest cockpit where the pilot sits in view;
- a glowing amber core in the chest;
- an engine with twin smokestacks on its back;
- a huge riveted fist on the left arm and a spinning drill on the right.

| Action | How |
| --- | --- |
| Place | Use the item on the ground (it needs room; it faces the way you face) |
| Climb in (1 seat) | Use it |
| Walk | Forward and back keys walk (back is half speed); left and right turn. It climbs one-block steps by itself |
| Jump | Jump key |
| Drill | Hold use: it drills the block you look at, up to 5 blocks away |
| Punch | Press attack: the fist hits everything just in front |
| Refuel | Use a diesel, premium diesel or RP-1 kerosene bucket on it (+1000 mB each, 4000 mB tank) |
| Pick up | Hit it until it drops (60 damage; it heals slowly). Players only; in creative it just goes |

While the pilot sits in it, their own hands do nothing: use and attack go to the drill and the fist instead.

- **Walking:** 0.2 blocks a tick (4 a second). It turns 4° a tick.
- **Drill:** it cuts one block at a time. Each block takes 5 ticks per point of hardness, at least 3 ticks: stone takes 8 ticks and obsidian 250. It will not cut bedrock or other unbreakable blocks. The block breaks as if the pilot broke it by hand: it drops its items, and claimed or protected land and other mods' break checks still apply. It does not dig areas, tunnels or chains of blocks.
- **Fist:** 12 damage with strong knockback, once every 16 ticks, counted as the pilot's attack.
- **Fuel:** walking, turning and drilling burn 4 mB a second, so a full tank lasts about 16 minutes of work. Standing still burns nothing. With an empty tank it only stands. The pilot sees the fuel left above the hotbar.

## Connections
- Input producers: the diesel engine, the mining drill (powered tools), riveted steel plate (giants), steel plates, an anvil and pistons. Fuel comes from the refinery.
- Output consumer: mining by hand at a faster pace, and fighting.
- Recipe: `RER / DPA / L L`, where R is riveted steel plate, E a diesel engine, D a mining drill, P a steel plate, A an anvil and L a piston.

## Balance and automation
- Fuel is only used up, never made.
- The drill breaks one targeted block at a time for a piloting player. It is a faster pickaxe, not a quarry.
- There is nothing to automate and no loop.

## Multiplayer and persistence
- **Server authority:** the server walks it, drills and punches. The client only sends the pilot's keys (`WalkerInputPayload`, five small numbers). The server clamps them, ignores anyone but the pilot, and stops answering if no keys arrive for 10 ticks.
- **Protection:** drilling checks `mayInteract` and `mayBuild` and fires Fabric's block-break events, as a player breaking the block would. While piloting, the player's own use, attack and block-breaking callbacks are cancelled, so their held item can't act through the cockpit.
- **Saving:** fuel is saved with the entity. Drill cracks are cleared when it stops or is removed.
- **Animation:** the leg swing and punch are worked out on each client from movement and one synced number, so there is no extra networking.

## Dependencies and assets
- No dependencies. All art is original. The sounds are vanilla's (iron golem steps and attack, block hit sounds).
- The model is in `tools/mech.py` and the item icon is a 16×16 map, `tools/item_icons/diesel_walker.txt` (redrawn from the 5 October 2026 32x32 icon on 7 October 2026, [ITEM_ICONS.md](../ITEM_ICONS.md)). The model reuses the giants' `dr_*` textures and the mining drill's bit texture. Since the 5 October 2026 art fixes the thighs stop a quarter pixel inside the pelvis's side planes and the chest walls, knees, toe bands and drill band stand a quarter pixel off their neighbours, so nothing flickers as it walks ([big-guns-art-fixes.md](big-guns-art-fixes.md)).
- The model is exported as six animated parts to `assets/jugcraft/walker_quads.json` (about 770 faces, 16-pixel tiles, hidden faces culled). `client/DieselWalkerRenderer` draws and animates it.

## Verification
- Planned in CI:
  - `dieselWalkerWalksOnFuel`: a fuelled walker walks forward for its pilot and burns fuel; an empty one stays still.
  - `dieselWalkerDrillsWhatThePilotLooksAt`: the block the pilot looks at breaks and drops, and drilling burns fuel.
  - `dieselWalkerPunches`: the fist hurts a pig in front.
  - `dieselWalkerKeepsFuel`: fuel survives a save and load.
  - The client screenshot `jugcraft_diesel_walker`.
- Done locally:
  - `check_mod_data.py` passes. It also checks that Java's walking, drill, fist and fuel numbers and the renderer's joints match the tool.
  - `check_repository.py` passes.
  - I reviewed an offline render of the model.
- Not done: piloting it in a real client, and a two-player dedicated server.

## World and event applicability
Not applicable: it is crafted and placed by players only.

## Rollout and open questions
- The hitbox is 2.5 wide and 4.25 tall. The fist and drill reach a little past it.
- Movement is server-driven, so with high ping the pilot's keys answer a little late.
- Ideas for later: arm swaps (a second fist, a claw or a cannon arm, damage only), a bigger two-seat walker and a mech bay to refuel and repair.
- **Fixed 5 October 2026 (shared render fixes):** the walker is drawn closed (no see-through gaps), and the quads inside each part no longer z-fight. Record: [see-through-and-flicker-fixes.md](see-through-and-flicker-fixes.md).
