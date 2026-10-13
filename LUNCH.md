# Companion lunch supplies

Lunch Crates and Lunch Covers supply food to tamed Peepo and Jughead. Both are in the Functional Blocks creative tab.

- **Lunch Crate:** nine slots, with the vanilla inventory screen. Craft from eight planks surrounding one bowl. Place it beside a walkable space and fill it with food.
- **Lunch Cover:** craft three wool above a bowl. Sneak-place against any exposed face of a chest, barrel, or compatible item container. The thin green cover marks the attached inventory as a lunch source. Open the underlying container from another face. Its collision is empty so it does not obstruct a chest lid. Breaking the cover leaves the underlying container and its contents intact.
- Sources feed companions owned by the player who placed them. Empty-hand sneak-right-click toggles optional party sharing; ordinary right-click opens a crate or reports cover status. Sharing covers feeding access, not protection of the original container or its automation ports.
- Place supplies within the companion's Home/Work radius and within 16 blocks of the companion. Leave an accessible standing space beside the container. Follow mode uses the followed player's nearby area; Stay mode does not let them wander off for meals.
- Companions collect one edible item when injured or below 95% energy. A healthy companion waits for its current food regeneration buff to expire before collecting another meal. Better food is preferred within the inspected inventory slots. Existing eating, healing, particles, sound and energy bonuses apply.
- Bowls/bottles are returned to the source if it is still nearby, accessible and has space; otherwise they drop beside the companion. Meals and their return location survive saves.

## Compatibility and efficiency

Covers use Fabric's sided item-storage API, including vanilla containers and modded containers exposing that API. They honor extraction/insertion restrictions on the covered face and reject vanilla locked containers (including adjacent locked chest halves). Containers without compatible exposed storage cannot accept a cover. Third-party permission/claim systems require their own automation integration; a cover is not a universal permission adapter.

There are no ticking lunch block entities and no forced chunk loads. The shared station index caches loaded-chunk positions for 80 ticks. Each hungry companion searches every 80–99 ticks, probes at most four candidate inventories (up to 128 storage views each), and attempts at most two paths per search. Candidates rotate across searches. Extremely large inventories should keep food in their first 128 exposed views. Newly placed sources can take several seconds to be discovered.

## Manual check

Tame a companion, set Home or Work near a supplied crate, and let its energy drop below 95% (or injure it). Confirm it walks over, takes one meal and eats. Repeat using a cover on a chest and a barrel; check a soup bowl returns, cover removal preserves chest contents, and crate removal drops its contents. Check owner/party access with a second player.

Built with `build-local.ps1 -Tasks assemble`; no automated tests requested or run. In-game verification is left to the user.
