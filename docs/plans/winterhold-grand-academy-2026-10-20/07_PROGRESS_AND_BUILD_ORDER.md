# Milestone plan | save early, build vertical slices
Stage IDs match `data/rooms.csv`. Do not call milestones done until files are pushed and tests/evidence recorded.

## M0 | Foundation + first iconic slice
Audit existing code, open PRs and previous work. Branch, push early checkpoint. Implement geometry for cliff/site, bridge, gate, courtyard, Elements Hall, roof silhouette. Add one named working instructor, one existing Concordance interaction and a single courtyard secret; minimal localized snow/fog slice. Show plan/top-down screenshot and first-person bridge view. **Draft PR now**.

## M1 | Functioning college community
Dorm common room, player/student beds, staff reception, Arcanaeum/librarian, working orientation and readable wayfinding. Persistent identities, doors, accessibility stairs. Demonstrate join-rejoin, sleep/storage behavior where real APIs allow.

## M2 | Academic floors and reachable variety
Gallery, practical classroom, upper studies, faculty rooms, stacks, balconies. Teacher schedules, wardrobes, hands-on existing systems, credible classroom variety. Screenshot each visited room and test no impossible paths.

## M3 | Service and advanced research
Restricted stacks, Arch-Mage study/garden, laboratory, enchanting, Rime/Healing halls, dining/kitchen. Hook up actual research/data/trade when available. Build deeper trapdoor and fair access. Do not fake a finished spell school if it is not implemented.

## M4 | Grand rituals and first dungeon
Observatory, roof storm terrace, council, Midden old tunnels/forge/relic hand/abandoned lab. Puzzles S02/S06/S07, clues, reset and no duplicate rewards. Ensure isolated hostile mobs and escape route.

## M5 | Depth, storytelling, polish
Midden Dark, Augur sanctuary, memory vault, sea exit and hidden route; optional winter aurora and repeated events. Balance and accessibility, lighting polish, fog tuning, asset provenance review, world-gen stress tests, two-client human QA.

## Per-milestone check-in (mandatory)
- Branch/PR link and latest pushed SHA
- Newly completed room IDs (from CSV)
- Mechanics actually playable, proposed or skipped
- Explicit test commands and outcomes including screenshots
- Remaining blockers and next small milestone

## Prioritized dependency chain
Structure placement/geometry -> entrances and room paths -> client weather atmosphere -> NPC identity/outfit -> basic existing magic interaction -> persistence -> service wings -> deeper puzzles -> optional complex magic. Don't let a cinematic effect block a valid playable building, but include a basic local climate slice early.
