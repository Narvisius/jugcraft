# Powered tools

Status: implemented in source (PR #40); **not yet played**. Compiles in CI; game tests cover it, and the client test screenshots the tools.
Proposal issue: none. The owner selected "Powered tools & armor" directly on 30 September 2026, with the direction that higher tiers look dieselpunk, very detailed, with things that are big in real life being big blocks.
Owner: @jimbozoomer-byte
Target milestone and tier: steel. Every tool needs steel plates, a steel gear or tungsten plates, and an advanced circuit.
Primary specialty and supported player role: engineering, mining, exploration

## Player experience
- **Mining Drill:** a JE-powered pickaxe and shovel, faster than netherite. Sneak and use it to switch modes:
  - one block;
  - a 3×3 square facing the way you mine;
  - a whole ore vein (up to 32 blocks).
- **Chainsaw:** a JE-powered axe that also cuts leaves. Cutting a log fells the whole tree above it (up to 128 logs). Sneak to cut one log.
- **Rocket Pack:** worn in the chest slot. Hold jump in the air to fly; landing is safe while it fires.
- **Charging Station:** two blocks tall. Hang a tool on its cradle and it fills the tool from cables. Take the tool back with an empty hand. Its lamp lights while it charges.
- The tools never wear out. Empty, the drill and chainsaw mine like a bare hand and get no drops, and the rocket pack does nothing.

## Connections
- Input producer: JE from any generator, through cables into the charging station.
- Output consumer: mining, logging and travel; the drill's ore drops feed ore processing.
- Technology connection: steel tier (steel plates and gears, tungsten plates, advanced circuits, lead, a battery box, fluid tanks).
- Magic connection: none yet.

## Balance and automation

| Item | Capacity | Cost | Notes |
| --- | --- | --- | --- |
| Mining Drill | 100,000 JE | 60 JE per block | speed 14 (diamond 8, netherite 9); diamond-tier drops; 3×3 costs 540 JE |
| Chainsaw | 100,000 JE | 40 JE per block | axe blocks and leaves |
| Rocket Pack | 200,000 JE | 50 JE per tick of thrust | about 200 seconds of flight |
| Charging Station | 50,000 JE buffer | 1,024 JE/t in | charges 512 JE/t |

- **Extra blocks:** the drill's and chainsaw's extra blocks are broken by the player's normal block breaking, so each costs its JE, drops normally, and respects spawn protection and other block-breaking rules.
- **No conversion loop:** JE goes into tools and is only spent.

## Multiplayer and persistence
- Energy and the drill's mode are item components, saved with the item and synced to clients.
- The charging station saves its buffer and the tool on it; breaking it drops both.
- **Rocket pack authority:** the client moves the player (as vanilla does), sends one thrust message per tick, and the server pays the JE and cancels fall damage. The server ignores more than one message per player per tick and does nothing without a charged pack in the chest slot.
- **Dedicated servers:** hovering a few seconds with `allow-flight=false` gets a player kicked for flying, as with other jetpacks. Set `allow-flight=true` to use the rocket pack there. Singleplayer allows flight.

## Dependencies and assets
Fabric networking and lookup APIs. Original 3D item models and dieselpunk textures (MIT); see [ART_DIRECTION.md](../ART_DIRECTION.md). The worn rocket pack is a flat armor-layer texture (tanks on the back, straps on the front), not a 3D model.

## Verification
- `tools/check_mod_data.py` passes.
- Game tests (CI):
  - `chargingStationChargesTool`
  - `emptyDrillIsSlow`
  - `drillMinesThreeByThree`
  - `chainsawFellsTree`
  - `rocketPackThrustUsesEnergy`
- Client screenshots:
  - `jugcraft_charging_stations`
  - `jugcraft_drill_in_hand`
  - `jugcraft_rocket_pack_worn`
- Not run: flying in a client, two players, a dedicated server, vein mining in real ore veins.

## World and event applicability
Not applicable.

## Rollout and open questions
- The worn rocket pack could get a 3D model (a render layer) instead of the armor texture.
- No speed upgrades or tiers for the tools yet.
- Existing steel-tier machines could move to the dieselpunk look.
