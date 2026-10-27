# Jimtafacts follow-up work after the first combined candidate

Historical owner direction, 23 October 2026: pause further expansion, record incomplete
systems, test the current implementation together, fix failures and prepare a
coordinated GitHub/client/server release. This supersedes the earlier instruction
to defer all testing until the entire source collection was implemented.

Prepared by OpenAI Codex (user-selected Astra High). This is a remaining-work
record, not a claim that the candidate passed validation or was deployed.

## October 25 follow-up

The owner selected groups B–H for local implementation. The [current batch ledger](jimtafacts-b-h-local.md)
records exactly what has been added and what is still missing. The list below
is the earlier baseline, not an up-to-date claim that the new guides and factory
tools have no implementation. The October 26 B–H implementation has now passed
the complete 2,559-test server suite and six selected client groups. Final focused
checks for the spatial identity regression and small-window terminal repair are
recorded in the current batch ledger. Nothing has been published or deployed.

## Current native scope and deliberate limits — October 26

The historical missing systems below now have native implementations: guides,
factory extras, industrial equipment, original encounter/statue presentation,
128-cubed spatial transfers with supported cargo/residents and recovery, and an
isolated Lua runtime using the supplied ROM and native peripheral adapters.
Chompa is implemented through the existing costume renderer with its face cutout.

This is not a claim of unrestricted compatibility with the original mods:

- Spatial transfers explicitly whitelist cargo and residents (64 inventories,
  24 residents); players, bosses, arbitrary third-party machines and riding/leash
  graphs cannot be stored. Cross-page double chests are rejected.
- Computers intentionally have bounded virtual storage/workers and no HTTP,
  host directory mounts or arbitrary Java upgrades. ROM programs requiring those
  facilities are unavailable. Floppy mounts hold one document.
- Native JE and shared recipes replace duplicate legacy energy/material systems.
  Deprecated/debug/unused declarations and the unfinished source accelerator
  prototype are classified rather than exposed as misleading survival equipment.
- Spatial journal/revision files are retained for recovery; automatic garbage
  collection is future work. Human survival balance and outfit fit while moving
  on normal/slim skins still need owner playtesting.

See the batch ledger for the exact source inventory, adaptation details and test
evidence. Do not use the historical list below as a current implementation queue.

## Original follow-up scope

- **Chronicle and legacy guide-book integration.** The supplied Chronicle recipe
  and Patchouli guide recipe require book systems absent from the native port.
  Their source files are retained; invalid runtime recipes are withheld until
  their reading/progression interfaces are implemented. Players can use the
  existing Jugcraft Encyclopedia in the meantime.

1. **Large spatial storage and entity migration.** The current 2³/16³ cells move
   supported static blocks only. Add incremental 128³ exchanges, durable entity
   identity and supported block-entity contents, interruption recovery and
   protected region ownership before allowing larger or populated captures.
2. **Full computer compatibility.** The current computer/turtle system runs a
   bounded native workshop instruction language. Lua/CraftOS, source ROM programs
   and arbitrary external computer-upgrade APIs are not implemented. A complete
   runtime needs instruction/memory limits, isolated persistence and explicitly
   restricted host/network access; do not advertise existing programs as Lua.
3. **Remaining factory systems.** Original multi-part heat exchanger; remaining
   blueprint-library/zone-planner/list/map/goggle behavior; transport attachment
   variants and their source art; source-specific pump tube scanning and refinery
   behavior where it differs from the reused native machines. Preserve the
   implemented engines, robots, gates, chutes and pipes while extending them.
4. **Remaining industrial families.** Resolve the unimplemented source registry
   entries individually: distinct fluids/processes, tools and equipment,
   explosives/remote controls, cell/material metadata and secondary machine
   variants. Consolidate genuine duplicates into existing materials/processes.
   Debug/deprecated declarations should be explicitly classified rather than
   introduced as unusable survival items.
5. **Encounter fidelity.** Finish the documented Keeper per-bone ghost/rise
   presentation and spider multipart/feeding behavior. Supplied structure files
   that reference missing third-party statue models still need suitable actual
   models; placeholder removal is not a completed visual port.
6. **Source-specific presentation/API differences.** Review the feature records
   for remaining original animation, emissive, fluid/render and compatibility
   differences. Native instantaneous pipe routing and a single JE economy are
   deliberate adaptations, not legacy traveling-item/MJ/RF compatibility.

The static [industrial source inventory](../../art/owner-library/catalog/edited-2026-10-23-jimtafacts/industry-source-inventory.json)
currently records 612 families/aliases: 375 native adaptations, 46 consolidated
native entries, 189 unresolved entries and two unused declarations. **189 is not
a count of distinct missing machines.** Metadata families can contain several
variants, and duplicate or obsolete entries can describe no additional gameplay.
The inventory also retains 552 source recipe statements; mapped ingredients do
not certify that every source recipe exists in the native game.

## Required before this candidate ships

### Explicit acquisition follow-up

The nine previously deferred survival routes have been implemented in the local
October 25 batch: Arcane Debris generation; both Archevoker logbooks and
translation; Ancient Furled Map, Legendary Spell Book, Lurker Ring, Tarnished
Helmet, Whispers of Ice disc and Unchained Book rewards. See the
[route and validation record](jimtafacts-survival-aircraft-oct25.md). Group A validation passed: 157 server checks, eight Python checks and the
seven-aircraft client run. This does not mean these changes are deployed. Boss and
exploration rewards retain their respective acquisition paths rather than
generic crafting recipes.

The loose Paint block is a presentation state: use the Color Applicator and
paint balls to create splotches in the world. Creative engines, creative storage,
command computers and the developer crown are intentionally not survival items.

### Release gate

- Compile common, client and test sources; fix integration failures.
- Validate source asset copies, models, recipes, IDs and survival acquisition.
- Run combined server tests and relevant rendered client tests, including
  inventories, crafting conservation, claims/party access and persistence.
- Exercise the packaged client/server dependency set and updater packaging.
- Review the exact changes against current GitHub main and ready predecessor
  changes; resolve overlap without including unrelated local deletions/artifacts.
- Record actual results, outstanding limitations and the exact release revision.
- Prepare the matching server patch and client update manifest. Obtain explicit
  owner restart confirmation, warn players for a full 30 seconds, back up and
  deploy. Verify server startup before promoting the player update channel.

See [collection status](jimtafacts-integration.md),
[industry](jimtafacts-industry.md), [network](jimtafacts-network.md),
[computers](jimtafacts-computers.md) and [encounters](jimtafacts-encounters.md).
