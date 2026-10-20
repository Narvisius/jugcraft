# Copper entry for aluminum, and the self-output gate check

Status: implemented on the contributor's fork; CI green; pending review.
Stacked on the [ruthenium bed](industrial-ruthenium-bed.md) and the branches below it.

Proposal issue: none. TODO item: "Use earlier copper conductors in entry aluminum-processing equipment, with aluminum improvements later; verify construction and reagent producers without self-output gates."

**From the owner's plan:**
- the tenth-batch selection, "Earlier copper conductors build entry aluminum-processing equipment; aluminum improves later versions";
- the [starter factory table](industrial-starter-gas-and-acid-factory.md): "Aluminum cable cannot gate the cell intended to begin the later aluminum route; provide copper entry".

Owner: jimbozoomer-byte
Target milestone and tier: the steel-era electrical chemistry entry
Primary specialty and supported player role: metals and electricity

## Player experience

- **The Electrolytic Cell.** It smelts alumina into aluminum, and it is now wired with **copper cable** instead of aluminum cable. Its bill is 4 steel plates, 2 Steel Tanks, copper cable, an advanced circuit and a Machine Casing. A player can build it before owning any aluminum.
- **The other aluminum steps already used copper:**
  - the Arc Furnace (bauxite → aluminum ingot);
  - the Chemical Reactor's Bayer step (bauxite + lye → alumina).
- **Aluminum still improves later equipment.** Aluminum cable stays in the Fuel Cell, the magnet motor and dynamo, and the advanced solar panel.

## Connections

- **Existing input producers:** copper cable (copper wire, before steel).
- **Existing output consumers:** unchanged. The cell makes the same aluminum, chlorine, hydrogen, lye and oxygen.
- **Technology connection:** the shared crafting data; the new check in `tools/check_mod_data.py`.
- **Magic connection:** none.
- **Reachable entry path:** the copper cell needs nothing it makes. Aluminum was already reachable through the Arc Furnace, so this removes a soft gate, not a hard one.
- **Required vs optional:** the cell itself is unchanged in role.

## The self-output gate check

The new check, `check_self_output_gates`, runs in `tools/check_mod_data.py`. It covers every machine that runs recipes: the item machines, the fluid machines and the industrial forms.
- **What it checks:** each ingredient of the machine's crafting recipe must be reachable without that machine.
- **What counts as reachable:**
  - vanilla items, and items no recipe makes (ores, crops, drops), which count as found in the world;
  - items some recipe makes, where that recipe can run in another machine or by crafting and its own inputs are reachable;
  - tags, which are resolved to their items.
- **What it reports:** a machine whose recipe needs something only it can make. It fails the data check with the item named.
- **Proven to catch a gate:** a probe recipe was tried. It made the Bulk Chemical Reactor from vanadium pentoxide, which only that reactor makes. The check reported it, and the probe was removed.
- **Today it finds no gates.**

**Not added:** a mod-wide "every recipe's inputs are reachable" check. Crops grown in the world are also unpacked from crates by recipes, so that wider check would report hundreds of false gates.

## Balance and automation

- **Cost.** Copper cable is cheaper and earlier than aluminum cable, which needed aluminum wire and silver cable.
- **Throughput.** The cell's own recipes, times and energy are unchanged.

## Choices made here for review

- **The cable:** copper replaces aluminum in the cell. The aluminum "improved version" is left to the later machines that already use aluminum cable. No second cell tier is added.

## Multiplayer and persistence

- Placed Electrolytic Cells are unaffected. Only the recipe that builds new ones changes.
- No new registrations.

## Dependencies and assets

None.

## Verification

Locally, without Gradle or a game:
- `python tools/generate_material_data.py`, `python tools/check_mod_data.py` (with the new check) and `python scripts/check_repository.py`: passed.
- The probe described above showed that the check catches a gate.

For CI, server game test `ElectrolyticSeparatorGameTests.theAluminumCellIsWiredWithCopper`: copper cable crafts the cell, and aluminum cable no longer does.

### Results

- An earlier run ([38471431369](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38471431369)) failed only on the Combined Cycle Power Plant's client test underneath, fixed in [factory payoffs](industrial-factory-payoffs.md).
- **Run [38494568219](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38494568219) (commit e63f1475) passed:** the build, the data checks (the self-output gate audit included), every server game test (`theAluminumCellIsWiredWithCopper` included) with and without optional integrations, and all five client test shards.

## Rollout and open questions

- An aluminum-wired cell tier, if the owner wants a visible "improved version", awaits review.
