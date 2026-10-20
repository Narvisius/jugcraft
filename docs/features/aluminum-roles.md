# Aluminum's bulk electrical, structural and vehicle roles

Status: implemented on the contributor's fork, pending review.

Proposal issue: none. TODO item: "Add aluminum's affordable bulk electrical/lightweight structural/panel/vehicle roles; …" This change delivers three aluminum roles. Aluminum cladding (panels) and the drone rotors and nacelle (vehicles) already exist.

**This change's choices:** which recipes take aluminum, and the cable's yield of four. The owner can review both.

Owner: jimbozoomer-byte
Target milestone and tier: after aluminum (the Bayer process and the electrolytic cell)
Primary specialty and supported player role: metals and electricity; players with an aluminum line who want to spend it instead of steel

## Player experience

Aluminum plates now stand in for steel plates in three crafting recipes. The steel recipes stay.

| Recipe | Steel version | Aluminum version |
| --- | --- | --- |
| **Aluminum cable** (bulk electrical) | 2 aluminum wire + 1 steel plate + 3 silver cable → 3 | 2 aluminum wire + **1 aluminum plate** + 3 silver cable → **4** |
| **Zeppelin** (vehicle) | 5 hydrogen lift cells, a diesel engine, 2 steel plates and a chest | the same with **2 aluminum plates**, the duralumin frame of the real airships |
| **Heliostat** (lightweight structure) | 3 glass panes, an electric motor and a steel plate → 2 | the same with **1 aluminum plate** |

The aluminum-sheathed cable is the cheaper way to run a lot of top-tier power line once aluminum flows. The extra cable comes from the lighter sheath.

## Connections

- **Input producer:** aluminum plates (aluminum ingots in the Metal Press).
- **Output consumers:** unchanged: the same cable, zeppelin and heliostat items.
- **Optional:** every steel recipe stays.

## Balance

- One aluminum plate in the cable recipe gives one more cable (4 against 3). It still takes the same silver cable and aluminum wire, so the tier ladder (copper, then silver, then aluminum) is unchanged.
- No metal is created: nothing here is taken apart again.

## Multiplayer and persistence

- **New recipe IDs:** `jugcraft:aluminum_cable_aluminum_sheathed`, `zeppelin_aluminum_frame` and `heliostat_aluminum_backing`. They load with the `machines` and `aluminum` switches.
- No new items, blocks or saved data.

## Verification

**Local checks** (no Gradle or game): `python tools/generate_material_data.py`, `python tools/check_mod_data.py` and `python scripts/check_repository.py` passed.

**Server game tests for CI** (`AluminumRolesGameTests`):

| Test | What it checks |
| --- | --- |
| `aluminumSheathedCableMakesFour` | Aluminum wire, an aluminum plate and silver cable craft four aluminum cables. |
| `aluminumFramesAZeppelinAndBacksHeliostats` | Aluminum plates craft the zeppelin and two heliostats. |

**Results:** the fork's Build workflow passed on commit c167e9f3 ([run 39172996865](https://github.com/mmme123-spec/jugcraftfork/actions/runs/39172996865)): compilation, the headless server game tests and the rendered-client shards.

**Not run:** crafting in a running game client.

## AI attribution

Implemented with Claude Opus 5.5 (Claude Code) for the contributor; the contributor is responsible for the output.
