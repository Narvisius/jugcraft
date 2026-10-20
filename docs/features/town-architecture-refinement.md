# City architecture refinement — 16 October 2026

Owner request: refine the scuffed church and the city's buildings, then consider
improvements to the whole city concept. Implemented locally with OpenAI Codex
(GPT-6), on top of the seven technology packages. No remote publication.

## Building pass

- Church: join the nave roof to the front, form one exterior surface where the
  transept crosses it, close the rear aisles and the stepped apse, and close the
  end gables. Keep the existing footprint, priest and seasonal sites.
- Give the church quieter upper masonry, corner piers and string courses,
  recessed tower windows, blue/gold rose glass with stone tracery, a framed
  double-door entrance, and a broad carpeted central aisle.
- Shape the arcade arches, suspend the nave lights from the roof, support the
  aisle lamps from the walls (ceiling chains in the open transept), add lower pew
  sconces, and open tower entrances with ladders to the belfries.
- Houses and shops: match doors, shutters and floors to their structural timber;
  frame roof edges with that timber; place flues in exterior walls and measure
  chimney height from the local roof. Flues no longer intersect beds or ladders.
  Furnish third storeys as well as second storeys.
- Bank: framed side windows, a stone entrance apron and manually usable timber
  doors. The formerly open iron doors could close after a neighbour update with
  no accessible way to reopen them.
- Town hall: a supported entrance portico, a readable clock face, and a council
  desk in place of the generic residential centre table.

The source is `tools/town_buildings.py` with the finishing routines in
`tools/town_refinement.py`. `python tools/town.py` regenerates the shipped
`data/jugcraft/town/town.json.gz`. This changes block-built architecture, not
textures, custom models, shop prices or NPC logic. Existing gates, walls, market
stalls, Styx's district and its observatory/greenhouse retain their designs.

## Saves and integration

The 44 houses, 32 resident anchors, four ATMs, street layout, town dimensions,
protection mask and seasonal site identities/order are preserved. The new pass
consumes no extra shared random draws, so it does not reshuffle the settlement.
The town builder still works through its normal bounded chunk placement.

**Already-generated buildings are not overwritten.** Test the full architectural
pass in a fresh world or the supplied separate review world. A partially built
old town can contain both building revisions as new chunks load; there is no
automatic migration, repair command or safe whole-town replacement in this pass.
The separate `small_church` building blueprint is not this spawn-city cathedral.

## Validation

- Seven Python architectural regression tests cover roof coverage, entrance-to-
  priest/aisle/tower access, rear enclosure, suspended lights, house beds/ladders,
  usable bank doors, seasonal slots and exact generated/runtime block agreement.
- Targeted town data audit passed.
- `gradlew build`: all 1,758 required server game tests passed, including 171,148
  town blocks compared with the runtime data (zero differences) and zero blocks
  drifting after ticking. This full suite preceded the final visual polish below.
- In-game review exposed narrow rear roof gaps and lamps too high to light the
  pews. Closed the gaps, added sconces/ceiling chains and strengthened the geometry
  tests. Reran all seven Python checks and the targeted town audit successfully.
- Final `gradlew assemble runClientGameTest -PclientTests=TownClientGameTests`
  passed with the final data: zero unknown palette states, all 144 town chunks
  built, a shop screen opened and an ATM available. Visually inspected the final
  church exterior/interior/roof/rear, hall, bank and residential street screenshots.
  This final rendered-client run includes a real integrated server; it is not a
  repeat of the entire 1,758-test headless suite after decorative changes.
- Repository structure/docs links and `git diff --check` passed. A screenshot-only
  compile error from using the old HUD field was corrected to the existing 26.3
  HUD toggle API before the successful final run.
- Saved a separate **Jugcraft Refined City** world and launcher-managed local
  pack for manual review, including the seven locally integrated tech packages.
  No automation harness is shipped in the playable mod; no user world was changed.
- Owner visual approval and real multiplayer walkthrough: pending.

## Proposed next improvements (not implemented)

1. **Make the city teach the mod.** A small public workshop demonstrates a working
   low-tier factory beside a magical workbench. Signs and encyclopedia links explain
   inputs/outputs; players can inspect examples without gaining unlimited materials.
2. **Give districts purpose.** Keep the square for markets and meeting, group
   workshops near a service lane, connect Styx's gardens to an herbalist quarter,
   and give the church a quiet garden/crypt entrance. Preserve clear main routes.
3. **Make buildings recognizable from the street.** Shop signs, consistent door
   lighting, district paving, a town map and a few distinct roof silhouettes should
   guide players without needing waypoint spam.
4. **Add repeatable civic commissions.** NPCs request crops, machine parts or
   magical supplies through existing Jugs/trade systems. Reuse the validation and
   bounded contribution patterns in the [Conclave](arcane-concordance-conclave.md)
   instead of duplicating its project ledger. Civic starter tasks should not
   require taking a magical oath. Cap rewards, prevent buy/sell loops, and offer
   several specialties and a solo route for each project.
5. **Give residents routines.** Shop hours need fallback counters; walks, social
   stops and seasonal decorating should remain bounded and work after chunk reloads.
6. **Keep the protected centre, add an outer player district.** Build on existing
   protection hooks for player shops and workshops outside the NPC core. A town
   plot/ownership system does not exist yet; choose that design or a compatible
   claim integration before promising plots. Decide ownership and abandonment
   rules without weakening spawn protection.

Suggested next slice: wayfinding plus distinct shop entrances, followed by one
public tech/magic workshop. These make the city easier to understand immediately;
commissions and housing can then add longer-term reasons to return.
