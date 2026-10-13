# Automatic companion routine

Applies to Peepo, pumpkin Peepo and Jughead.

- Eating/nearby food takes priority over station work. Automatic feeding still requires missing health or less than 95% energy. A healthy NPC with an active food regeneration buff lets that meal digest instead of consuming every dropped item; hand feeding remains available below 95%.
- Reaching zero energy starts a saved, synchronized recovery state. Work and running are blocked until the reserve reaches 80% (102,400 JE). A cancelled energy transaction cannot trigger exhaustion.
- Station search considers only loaded block entities within 16 blocks, at staggered intervals of 80–99 ticks, with shared loaded-chunk station indexing. It prefers an available wheel with buffer space when work is allowed; otherwise a bed at night, then a chair. With no usable station, it can wander and regenerate. Exhausted NPCs take a stationary break until their recovery threshold is reached.
- Beds are night-only. Dawn ends sleep. During recovery, furniture remains preferred until 80%. Chairs finish their current break after recovery, then look for work; an occupied bed continues sleep until 95% or dawn.
- A wheel receives at most the minimum of its remaining capacity, the NPC's reserve, and 64 JE/t. Wheel insertion and reserve extraction commit together. A full or nonaccepting wheel stops the running animation and is released without spending energy.
- A station is reserved for one NPC before pathing. Unreachable routes are skipped for ten seconds; a trip times out after ten seconds. Station removal, chunk unload, lost availability and eating release it. No chunks are force-loaded.
- Shift-right-click opens the authorized companion command GUI; see COMMANDS.md.

## What is usable now

Energy-based food seeking, digestion, passive recovery, exhaustion lockout and status reporting are available. The registered Companion Generator Wheel implements station discovery, exclusive occupancy, animated running and transactional JE generation. Colored, stackable companion beds provide sleeping stations; stools and shared player chairs provide seated rest, alongside supported bed and fence perches. See LOCAL_TESTING.md for the local build and manual test steps.

## Station integration contract

A loaded block entity implements `CompanionStation`. It supplies its kind, safe reachable approach position, permission/occupancy check, atomic claim, occupation/mount operation, and release. `availableTo` must remain true for its current occupant. `occupy` is idempotent and refreshes the claim; it should leave the NPC at the approach point or an equally close mounting point (within 0.8 blocks). `release` must dismount and restore a safe position. Claims must time out if not refreshed and must not survive a missing/dead/unloaded NPC. Implementations should revalidate ownership/party permissions on every operation.

Wheels expose actual free buffer capacity and transactional insertion; never discard overflow. The routine calls `CompanionWork.transfer` while occupied. Block code must not also extract energy separately. Map the resulting buffer to Jugcraft's sided energy lookup on the wheel's two outward sides. Chair/bed occupancy uses the existing rest hooks; no block is identified by guessing its registry name.

This integration was compiled with assemble only. Automated tests were intentionally not run for this change; physical pathing, rendering and power delivery need manual in-game verification.

## Suggested next features, based on GitHub main inspected 2026-10-06

1. **Assigned food bowl / lunch crate.** Retrieve one meal from a designated container, return bowls, and prefer better meals when exhausted. Start with existing cooking-pot dishes and ItemStorage rather than unrestricted chest access. [Kitchen Garden](https://github.com/jimbozoomer-byte/jugcraft/blob/main/docs/features/kitchen-garden.md), [Item logistics](https://github.com/jimbozoomer-byte/jugcraft/blob/main/docs/features/item-logistics.md).
2. **Home, follow, stay and work commands (implemented locally; see COMMANDS.md).** Give each companion a home/work radius and an owner; party members can help feed and assign jobs using the existing shared permission rules. [Parties](https://github.com/jimbozoomer-byte/jugcraft/blob/main/docs/features/parties.md).
3. **Small garden helper.** Harvest ripe crops, reserve seeds to replant, and deliver produce to an assigned crate. Keep its range and speed below the powered 9x9 harvester so the machine remains an upgrade. Tomatoes and peppers need their special pick-without-breaking handling. [Farming](https://github.com/jimbozoomer-byte/jugcraft/blob/main/docs/features/farming.md), [Kitchen Garden](https://github.com/jimbozoomer-byte/jugcraft/blob/main/docs/features/kitchen-garden.md).
4. **Short-range porter.** Carry one stack between two assigned crates or collect loose workshop drops. Keep routes local, leaving long-distance transport to pipes and the Drone Tower. [Storage](https://github.com/jimbozoomer-byte/jugcraft/blob/main/docs/features/storage.md), [Drone Depot](https://github.com/jimbozoomer-byte/jugcraft/blob/main/docs/features/drone-depot.md).
5. **Workshop alerts.** Wave or make a short sound when a designated wheel battery fills, its food supply runs out, or its assigned path is blocked. Show the reason in its status rather than silently standing still.
6. **Social rest and celebrations.** Sit together, look toward nearby players, react to petting, dance to music, and celebrate a finished work session. A future adapter could use the already implemented Haunted Dining Chair. Pumpkin-costume reactions can fit Halloween without automating the player-only trick-or-treat rewards. [Dining room](https://github.com/jimbozoomer-byte/jugcraft/blob/main/docs/features/laboratory-larder-dining.md), [Trick-or-treating](https://github.com/jimbozoomer-byte/jugcraft/blob/main/docs/features/pumpkin-regatta-and-trick-or-treat.md).

Commands/ownership are now implemented locally; the remaining ideas are proposals, not additional implemented jobs. Recommended order: commands/ownership, food bowl, wheel block hookup, then gardening or carrying.
