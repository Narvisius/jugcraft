# Arcane Concordance: persistence, migration and performance

Status: implemented on branch `claude/concordance-steps-26-32`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 30) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 22, every stage. Roadmap step 30.
Primary specialty and supported player role: every player, and every server keeping a world across versions.

The brief's acceptance criterion: the system meets a stated performance budget and does not multiply work unexpectedly
after loading a saved world. Contract and checklist: [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## What already existed

- **Loading.** Every block entity and creature already read each field with a default, so a missing field never
  failed a load.
- **Removed definitions.** Most stored identifiers whose definition is gone were already kept or handled safely:
  - research entries are kept;
  - a ritual with no definition stops as forgotten;
  - a worker with no definition says it is disabled;
  - a relic or spire with no definition shows "unknown";
  - a curse with no definition lifts.
- **Unloaded regions.** No Concordance code loads a chunk, and work that reaches an unloaded place pauses.
- **The gaps:**
  - Only two formats (a player's knowledge and Focus) wrote a version, and nothing read it.
  - The world records failed whole on a single field they could not read, and vanilla then replaced them with empty
    ones on the next save.
  - A Conclave project whose definition was removed blocked its owner's projects forever.
  - Every block broken anywhere looked at every loaded circle anchor in the dimension.

## What changed

**One way to keep saves readable** (`concordance/Saved`):

- **`Saved.versioned`.**
  - Every record the Concordance keeps in a world is now written as `{"version": N, "data": ...}`, and so is every
    attachment it keeps on a player or creature.
  - A save from before versions (the bare data) reads as version 0.
  - Each older version is brought forward through its upgrade steps, in order, before it is read. That is the
    migration path for a renamed identifier or a changed shape: add a step, raise the version.
  - A save from a newer version is read as far as this one understands it, with a warning, rather than refused.
- **`Saved.keeping`.**
  - Records whose entries are independent read each entry on its own: the spires, the Bound Wills, the worker roster,
    the sky's claims and the Conclave's projects.
  - An entry that cannot be read (from a newer version, or damaged) is kept exactly as it was, logged, and written back
    unchanged. One bad entry no longer empties a record, and reading never destroys anything.
  - The courier ledger is read whole, versioned: its requests, reservations, cargo and samples refer to one another.
- **`Saved.stamp`.** Every Concordance block entity's and creature's save now carries its version under `version`,
  for a later format to read with `Saved.version`.

**A removed Conclave project no longer blocks.** A project whose definition is gone does not stand in the way of a new
one. When one begins, the old one is set aside exactly as it was, with everyone's contributions, and kept in the record
(`set_aside`) for an operator to restore.

**The circle index is kept by chunk.** A block change looks only at the anchors in the chunks within reach (at most 2
by 2), however many circles the world holds. An anchor that is unloaded and loaded again is indexed once.

## Every persistent format

| What | Where | Format | Unreadable or unknown content |
|---|---|---|---|
| Spires | world (`jugcraft:spires`) | versioned, entry by entry | entry kept as written; a spire whose configuration is gone stays recorded and shows "unknown" |
| Worker roster | world (`jugcraft:worker_roster`) | versioned, player by player | kept as written |
| Bound Wills | world (`jugcraft:bound_wills`) | versioned, will by will | kept as written |
| Sky's claims | world (`jugcraft:astral_claims`) | versioned, player by player | kept as written; a pattern no longer defined is skipped when read |
| Conclave projects | world (`jugcraft:conclave`) | versioned, project by project | kept as written; a project no longer defined is set aside when its owner begins another |
| Courier ledger | world (`jugcraft:courier_ledger`) | versioned, whole | read as written or not at all (its parts refer to one another) |
| Knowledge, Focus, stage, milestones, standing, attunement, offerings, dream, dreams, curses, wards, Prima ledger | player or creature attachments | versioned | knowledge keeps unknown entries; an unknown stage is recomputed; an unknown curse lifts |
| Block entities (pylon, sconce, crucible, anchor, heart, shrine, observatory, garden devices, bench, lectern, post, spirit anchor) | chunk | stamped `version`, every field defaulted | a ritual run that cannot be read is reset to idle |
| Workers (porter, shade, Hearthling) | chunk | stamped `version`, every field defaulted | a worker whose definition is gone says it is disabled |
| Item components (Radiance, notes, inscription, tunings, brew, relic, artifice, reagent, link, route, and so on) | item stacks | unchanged | most fields optional. A changed shape gets a new component id, since an envelope would change every stack, command and recipe that names one |

A dream wisp is never saved.

## While a region is unloaded

Simulation pauses; nothing models what an unloaded place would have done:
- a spire partly unloaded changes nothing, and days spent unloaded are not charged as upkeep;
- workers stop and say their destination is unloaded;
- a lingering spell pulse whose place is unloaded is skipped;
- a ritual whose circle is partly unloaded stops as unloaded;
- a cursed creature that unloads leaves the curse index and returns when it loads;
- a dream's wisps gather only where creatures are live.

No Concordance code forces, tickets or synchronously loads a chunk.

## Performance budget

Every recurring piece of server work has a stated bound. None of it grows with time spent unloaded, and loading a saved
world schedules nothing extra: block entities pulse on their own period, staggered by position, and the indexes add each
thing once.

| Work | Bound |
|---|---|
| A circle anchor | nothing while idle; a ritual checks its circle (at most 64 positions) every step (40 ticks) |
| A block broken or a pylon changed | the anchors in the chunks within 8 blocks (at most 2 by 2 chunks) |
| Garden beds and crops | an area sample of at most 100 positions; at most 16 samples a level a tick |
| Verdant Heart, Gleaner, Mulch Maw, Habitat Gauge | one pulse every 200, 40, 40 and 40 ticks, staggered |
| Spire Heart | one look every 20 ticks; a field pulse every 100 ticks of at most its count (4 or 6) |
| Reliquary Shrine, Observatory | one pulse every 20 and 100 ticks, staggered |
| Workers | one decision every 20 ticks; a porter searches at most the loaded block entities within 8 blocks of its post |
| Relics carried, the sky, dreams | each online player every 20, 100 and 10 ticks |
| Curses | each loaded cursed creature every 20 ticks |
| Lingering spell pulses | at most 64, 2 a player |
| Signs | at most 48 a level a tick, plus 16 warnings |
| Courier ledger | at most 1,024 open requests, 32 history events a post |

The budget is stated in work, not in milliseconds. It has not been profiled under load. Four pieces of it (a block
change's circle look-up, a spell compile, the graph's audit walk, a garden sample) are timed on a CI runner by
`ConcordanceWorkloadGameTests`; the figures are in the [delivery record](arcane-concordance-delivery.md). A large
installation's frame cost (GeckoLib, particles, GUI, shaders) has not been measured on a client.

## Connections

- Input producer and output consumer: every Concordance system that saves.
- Reachable entry path: nothing to unlock.

## Balance

None.

## Multiplayer and persistence

- Saves made before this step read as version 0 and are written at version 1 on the next save. The data does not
  change; only its envelope does.
- Downgrading: a save written by this version is not read by the version before it, which does not know the envelope.
  The world records are rewritten on save, so keep a backup of the world before moving between versions.

## Dependencies and assets

No new dependency or asset.

## Verification

- Run locally before pushing:
  - `python3 tools/check_mod_data.py` on a copy of the tree, with the new `check_persistence`;
  - `python3 scripts/check_repository.py`.
  Their results are in the pull request. None of the Java of this step can compile here; CI is its first compile.
- `check_persistence` confirms:
  - every Concordance SavedData's codec and every persistent attachment goes through `Saved.versioned`;
  - every save stamps its version, directly or through a base its save calls;
  - the records with independent entries use `Saved.keeping`.
- Server game tests added:
  - `ConcordancePersistenceGameTests` (five): a format at version 2 reads saves from before versions, version 1,
    version 2 and a newer version; a spire record keeps a damaged spire exactly as written; every world record reads
    its save from before versions back to itself; block entities stamp their version; the circle index looks only in
    reach and indexes an anchor once;
  - `ConcordanceConclaveGameTests.aProjectWhoseDefinitionIsGoneIsSetAside`.
- CI: Build run 37683103100 on 441cd24, the first compile of `Saved` and of this step's tests: `mod` and `optional
  integrations absent` passed, "All 1075 required tests passed" in each.

Not yet run:
- a real world saved by an earlier build and opened with this one (none is kept: world saves are not committed);
- a server restart;
- profiling under load.

## World and event applicability

Works everywhere.

## Rollout and open questions

- New identifiers: the record field `set_aside` in `jugcraft:conclave`. Every Concordance world record and attachment
  is now written in the versioned envelope.
- Known limits:
  - item components are not versioned (see the table);
  - the courier ledger is read whole;
  - a downgrade needs a backup.
