# October 13 community PR integration

This branch combines the 31 source PRs captured on October 13 against main `b73f51c8`.
The integration PR's GitHub merge state determines whether the batch is on main. Source histories and original
attribution are preserved. Conflict resolution and additional fixes use OpenAI Codex (GPT-6), authorized by the owner.

## Implemented in this integration

- Peepo companion jobs, garden zones, animal care, deliveries and workshop automation (#323).
- Chemical Infuser, Oxidizer, methane power, Industrial Fermenter, Bulk Chemical Reactor, Anaerobic Digester,
  coal gasification and cleanup, contact sulfuric acid, PVC/HCl recovery, Bio Generator, vent/hold choice,
  useful gasification residue and ammonium nitrate (#324, #326–327, #329, #333–335, #339–342, #345–346).
- Exact gas-tank handling tests and the formed-machine power-label correction (#325, #328).
- Brighter Cinder Kiln/Tyrant presentation; Lantern Fen, Mire Hag and rewards; the Archive of Last Names and
  Writ of Exhumation and the Crypt Lich encounter with seals, phylactery, minions and rewards (#330–331, #336, #350, #353).
- Nine more guns: End, copper and lone-gun sets (#332, #338, #344).
- Concordance Chunk A: spell composition UI, encounter support credit, first-loadout routes and the live
  encyclopedia; community technology/magic guide cards are available inside that shared encyclopedia (#322, #337).
- Linked fluorite and spodumene deposits/outcrops (#343, #351).
- Long-distance power lines, pipes/cables inside walls, and the nuclear power/plant/storage/placement package
  (#347–349). Read their feature records for progression and alpha limitations.

## Integration corrections

- Preserve both encyclopedia contributions behind one B key and one inventory entry point. **Technology & Magic
  Guides** opens the community directory; Back returns to the live progression/inspection encyclopedia. Renamed its
  data generator and audit to avoid duplicate Python function names silently replacing validation.
- Combine registrations, test entrypoints, language keys, generators and feature records without discarding either
  side of shared-file conflicts.
- Remove unused recipe key W from the NRC desk and video wall recipes: these prevented registry/world loading.
  The recipe audit now rejects unused and missing pattern keys for every shaped recipe.
- Draw the nuclear package's 34 flat inventory icons on the native 16px grid. Equipment/world/worn models retain
  their authored textures. The source is `tools/nuclear_item_icons.py`; no asset-size exemptions were added.
- Use explicit DejaVu font lookup for nuclear asset generation on Windows. Install the same DejaVu fonts used in
  Linux CI and set `JUGCRAFT_FONT_DIR` to their TTF directory; missing fonts report a clear error rather than
  substituting another font and silently changing authored panels.
- Check custom equipment and power-line footprints before placement/upgrading, and reject spectator/adventure,
  out-of-bounds, unloaded, protected-town or spawn-protected changes. Check both endpoints before wiring changes;
  wall-run packets/direct removal and reactor console packets enforce permissions too.
- Support the medium plant's annulus ladders, extended crane rails and outer airlock door. These previously broke
  when Minecraft updated their shapes. The plant runtime test now requires every non-equipment blueprint block to
  remain after placement. Scope station-battery circuit tests to station chargers, separate from diesel starters.
- Support the fuel-handling rail over its water canal; keep the canal open below its steel beam.
- Register the encyclopedia once: duplicate registration from the two source branches crashed client startup.
- Close exposed nuclear model backs/end caps through `model_writer.finish_closed`, retaining the art audit without
  new exemptions. Round ladder-tray bend coordinates so Windows and Linux generate identical geometry JSON.
- The operator-only power-line showcase loads each tower's complete footprint before checking its footings.
  Normal player placement still rejects unloaded/protected cells. Nuclear frame naming uses the pinned Jade
  version's common `blockOperations().pick` API instead of a removed reflective client method.

- Update the Peepo machine-registration test for the combined 38 supported processor kinds, and register the Crypt
  Lich's implemented lair in the shared trophy acquisition guide. Rename the cafeteria plate base texture so Iris
  does not mistake its `_s` suffix for a specular map.

## Verification

Final results are recorded in [integration PR #352](https://github.com/jimbozoomer-byte/jugcraft/pull/352); do not treat
the source PRs' individual CI as the result for this integration. The combined build at `c3a7538b` passed all 1,564
server game tests locally. The later client registration, geometry and plant-support fixes require a fresh CI run.
Repository structure/link checks, all 14 Python packaging/selection tests and 11 world-designer tests passed. The client
planner includes all registered client test classes, distributed across five jobs (about 23 minutes estimated
per job). Full server, optional-absent, data, and client results will be recorded in that PR before merge.

Human two-client play, nuclear plant load profiling, survival balance and owner visual acceptance are not claimed.
No live-server deployment or public modpack release is part of this merge.

## Separate local work

The owner's local machine consolidation/recipe-picker/chemistry playtest branch `feature/starter-chemistry-playtest`
is preserved separately, including its saved test world and `.mrpack`. It is not an open source PR in this batch.
Some machines overlap these community implementations; reconcile layouts, IDs and recipes with that local branch
before replacing the owner's test profile. The Howitzer-Walker remains planned in the local TODO, not implemented here.

## Source heads

| PR | Source head | Title |
| --- | --- | --- |
| [#322](https://github.com/jimbozoomer-byte/jugcraft/pull/322) | `e49e8337d106ef35e417d31b876a5eb28414bb3b` | [Draft] Jugcraft Encyclopedia, first slice: K key, inventory button and route home |
| [#323](https://github.com/jimbozoomer-byte/jugcraft/pull/323) | `9a2f3ae792b5f435c1b9f8f9b1ba57d6700b3a32` | Expand Peepo companion jobs, garden zones and livestock automation |
| [#324](https://github.com/jimbozoomer-byte/jugcraft/pull/324) | `0b7e2a0bebdd7f2b940ca99359dd6202077f0716` | Industrial package 2: the Chemical Infuser (hydrogen chloride) |
| [#325](https://github.com/jimbozoomer-byte/jugcraft/pull/325) | `c61710c2b371bfb1fe9d6515a6d55742d115fc23` | Tests: gas tanks keep exact contents through breaking, placing and pipes |
| [#326](https://github.com/jimbozoomer-byte/jugcraft/pull/326) | `e08c4937fd717ce4b475a0075a1287b9e9fec9d3` | Industrial package 2: the Chemical Oxidizer (sulfur dioxide) |
| [#327](https://github.com/jimbozoomer-byte/jugcraft/pull/327) | `8bcb091ff90626075fa5defde10651eef9a0eac8` | Industrial package 2: methane power (stacked on the Infuser and Oxidizer PRs) |
| [#328](https://github.com/jimbozoomer-byte/jugcraft/pull/328) | `cffe97bf265287e6a995001336644a3649f15a8c` | Formed-machine screens keep the POWER label on large stores |
| [#329](https://github.com/jimbozoomer-byte/jugcraft/pull/329) | `c403cccc36acc443561d8aefaec230f4c76d6cf4` | Add the Industrial Fermenter (bioethanol with collected CO2) |
| [#330](https://github.com/jimbozoomer-byte/jugcraft/pull/330) | `c1b54f6f81096059c36219a954040f5c713702c5` | Light the Cinder Kiln's bowl and widen the Cinder Tyrant's seams |
| [#331](https://github.com/jimbozoomer-byte/jugcraft/pull/331) | `c60f7e034bfaa606c7bdd094892ae4df89463a12` | Add the Lantern Fen and the Hag Stone (the Mire Hag, part 1) |
| [#332](https://github.com/jimbozoomer-byte/jugcraft/pull/332) | `ae61def8fab237a111274da5b449ed9327777d8a` | Guns slice 12A: the End guns (Chitin SMG, Hermit Rifle, Borer) |
| [#333](https://github.com/jimbozoomer-byte/jugcraft/pull/333) | `1c9ebd556386797a84024b4d3dce5a2dae97b1b2` | Add the Bulk Chemical Reactor (hydrochloric acid and chloride leaching) |
| [#334](https://github.com/jimbozoomer-byte/jugcraft/pull/334) | `a3cce4885e8f6a717fdc656c3ff1f6d203a1764a` | Add the Anaerobic Digester (biogas from organic waste) |
| [#335](https://github.com/jimbozoomer-byte/jugcraft/pull/335) | `1826131e41db7194b234b11758f8a1dfba0ff2de` | Add coal gasification (Gasifier, gas cleanup rack, CO methanation) |
| [#336](https://github.com/jimbozoomer-byte/jugcraft/pull/336) | `a6af32968eb6a3427a04391c65b6efa1501152b3` | Add the Mire Hag, her Likenesses and her loot (the Mire Hag, part 2) |
| [#337](https://github.com/jimbozoomer-byte/jugcraft/pull/337) | `d4c07bf56f80eb0601f8b28d9381d758d69adf09` | Arcane Concordance, Chunk A: compose without commands, support earns a boss's reward, the encyclopedia's first route |
| [#338](https://github.com/jimbozoomer-byte/jugcraft/pull/338) | `aa9d7c45913b9dec6cb3b18daff53c41e88ce262` | Guns slice 12B: the copper guns (Penny Pistol, Conductor Carbine, Magpie SMG) |
| [#339](https://github.com/jimbozoomer-byte/jugcraft/pull/339) | `dbd9b260624ae69ef9763690437fd7057cef516d` | Add contact sulfuric acid (vanadium bed, SO3, absorption) |
| [#340](https://github.com/jimbozoomer-byte/jugcraft/pull/340) | `b7fc4c322ef59480e7b867d42cb22a523e56a84d` | Add the detailed PVC route with HCl recovery |
| [#341](https://github.com/jimbozoomer-byte/jugcraft/pull/341) | `33894acc76d0ae6186bde91bb3d4fc363435bd04` | Burn biogas in the Bio Generator |
| [#342](https://github.com/jimbozoomer-byte/jugcraft/pull/342) | `abe1801893325ffc3587a8bb718af694ac2857cb` | Let players hold vented byproduct gas instead of venting it |
| [#343](https://github.com/jimbozoomer-byte/jugcraft/pull/343) | `6256dd86442a98436782524cb9892b5a1b04bd59` | Add snowy-region fluorite deposits and linked surface outcrops |
| [#344](https://github.com/jimbozoomer-byte/jugcraft/pull/344) | `42bd073f4f83a2939f63511f4ef935e5efc87fcf` | Guns slice 12C: the lone guns (Tuppence Pistol, Goshawk Revolver, Lament Rifle) |
| [#345](https://github.com/jimbozoomer-byte/jugcraft/pull/345) | `35507a50a138420193ed721a3939c3c4dc8d32f9` | Give the Coal Gasifier a residue, bound into concrete or asphalt |
| [#346](https://github.com/jimbozoomer-byte/jugcraft/pull/346) | `2d7768f7de6d552b071db89eef1c50b5bf82e98d` | Make ammonium nitrate from nitric acid and ammonia |
| [#347](https://github.com/jimbozoomer-byte/jugcraft/pull/347) | `f823e3d2a1068750b80cdbaed777a9dc500a00a8` | Power lines, stage 1: supports, grid boxes and hanging wires |
| [#348](https://github.com/jimbozoomer-byte/jugcraft/pull/348) | `64ab5ef9748823cb2d097d4b117acb46c1357462` | Pipes and cables inside made walls |
| [#349](https://github.com/jimbozoomer-byte/jugcraft/pull/349) | `d05685d11e3338751aa618ab62037b99999644b2` | Nuclear power: reactor, pre-made plants, storage tanks, MBS placement |
| [#350](https://github.com/jimbozoomer-byte/jugcraft/pull/350) | `86456ab5cad5346c4184907b8cd4d7d2015df3ca` | Add the Archive of Last Names and the Writ of Exhumation (the Crypt Lich, part 1) |
| [#351](https://github.com/jimbozoomer-byte/jugcraft/pull/351) | `b7f0ec1005c47648776f26fd4e6d3d87917a294c` | Add rocky-upland spodumene deposits with stone outcrops |
| [#353](https://github.com/jimbozoomer-byte/jugcraft/pull/353) | `7ed1804a97b1ec0f824649467a2ff97861754359` | Add the Crypt Lich, his ward and phylactery, his dead and his loot (part 2) |
