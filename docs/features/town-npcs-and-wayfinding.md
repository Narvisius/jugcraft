# Town NPCs and wayfinding

Owner-directed local development, 16 October 2026. Prepared with OpenAI Codex
(GPT-6). Built on the local city refinement and seven technology packages; no
remote publication is authorized by this slice.

## Player outcome

The 32 town residents retain their named roles and 18 classic player skins, with
an owner-requested Nordic-inspired palette: fair complexions and blond, brown,
auburn and grey hair. Faces retain vanilla-like single-pixel-height eyes, clean
flat shading and distinct work clothing. This changes the town roster, not player
skins, monsters or the separately approved Styx model.

Ten plaques identify the four real shops, bakery, two taverns, church, bank and
town hall. Bakery/tavern plaques honestly identify conversation-only services.
Three directory signs frame a north-up vanilla filled map south-west of the market
square. It shows the generated roofs, walls and roads with numbered landmarks:
1 church, 2 bank, 3 hall, 4 General Store. Market stalls surround the square.

Crouch and use any town guard to obtain a free portable copy. Ordinary guard
conversation explains this. All copies use one saved map ID. A player already
carrying it gets directions instead of another copy; a full inventory is told to
make space, and no map is dropped. This is a fixed architectural plan, not a live
survey of player alterations or a map of the separate Styx district. It tracks the
carrier using vanilla maps. North is always the top.

Residents stop navigating and look at their conversational partner for up to four
seconds. Combat takes precedence for guards, and walking away ends the pause.
Full daily routines and work animations remain TODO items.

## Implementation and persistence

- Existing `town_skins.py` generates the recolored skins; the owner catalog was
  checked, and its villager profession overlays do not fit this player-shaped cast.
  Existing seeds, model silhouettes and texture IDs remain stable.
- `town_wayfinding.py` places supported vanilla spruce sign plaques without
  consuming layout randomness or occupying seasonal decoration slots. Metadata is
  exported with the town blueprint. Existing spot/site order remains stable.
- `TownWayfinding` fills and waxes signs after the owning chunk is built and places
  the protected item frame. No command-bearing sign text or new client protocol.
- The vanilla map's public codec sets its exact centre instead of the normal
  snapped grid. Its colors are calculated once from the embedded town blueprint,
  without loading terrain chunks, and locked. `TownState.town_map` defaults to -1
  for old saves and persists the shared map ID. Moving/forgetting the town clears
  the reference; existing map items retain their old maps normally.
- Existing generated town chunks are not rewritten. Skins and conversation behavior
  update existing NPCs, and guards can provide a map in older towns. New physical
  signs and the board require a newly generated town. An old town's portable map
  depicts the current authored plan, so its earlier buildings may differ.

## Validation

- Nine Python town regressions pass, including supported signs, clear reading
  space, seasonal slots, original architectural access and exact generated blocks.
- Targeted town data audit, repository documentation checks and `git diff --check`
  pass. Resident identities, site ordering, ATMs, gates, masks and themes were
  compared against the preceding local commit and remain unchanged.
- Full `build` passed all **1,758 required server game tests**, including the new
  map ID persistence, correct map centre, locked map, duplicate-copy prevention
  and labelled/waxed sign assertions. This preceded the final board rotation and
  shortened sign wording; gameplay Java did not change afterward.
- Final `assemble runClientGameTest -PclientTests=TownClientGameTests` passed:
  144/144 town chunks, no unknown block states, working shop and ATM, all 13 signs
  retained after Halloween/December changes, framed and carried maps, and every
  sign line within the actual game's 90-pixel text limit.
- Rendered inspection found an initial directory facing the neighbouring inn and
  overly long lines. Both were corrected; the final screenshot shows the directory
  facing the open square over its existing low bench. Shop signs and NPC skin
  presentation were also inspected in the rendered client.
- A separate `Jugcraft Town and NPCs` world and local `.mrpack` include this slice,
  the prior architecture refinement and all seven local technology packages.
  The world includes its saved vanilla map data. Existing manual saves were kept.

No two-client session or owner-controlled walkthrough has been performed for this slice.
Modrinth import has not been performed. No source or artifact was pushed to main.
