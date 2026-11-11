# Tower master console

The current industrial command room has one three-metre master console and two wall computers. The master is a single workstation, with a continuous desktop, open knee bay, equipment pedestals, a rear service spine, linked monitor bridge, keyboard, touch instruments, rotary controls and guarded isolator. Its front fascia and live centre instrument both identify **TOWER CONTROL**.

The six occupied blocks are one multi-block structure, placed with one item and dismantled together with one item drop. The front-centre block is the controller. `facing` points toward the operator; a north-facing console extends one block behind its controller to the south. The source footprint, in part order, is:

| Part | Offset from controller |
| --- | --- |
| 0 | 0, 0, 0 |
| 1 | −1, 0, 0 |
| 2 | 1, 0, 0 |
| 3 | 0, 0, 1 |
| 4 | −1, 0, 1 |
| 5 | 1, 0, 1 |

The tower places the controller at core-relative `(0, 1, 4)`, facing south. The operator aisle is south of it at z5–6; the hologram table remains across the cross aisle. The monitor bank rises to 25 model pixels above the floor. Placement and its real-model preview share one validation helper that checks the six occupied cells and clear space above the three rear cells, including loaded chunks, world height/border, protection, replacement and collision rules. A low solid, glass or leaf ceiling is rejected and marked by the preview; those clearance cells remain air rather than extra MBS parts. The crosshair outlines the complete workstation while each part supplies its own collision shape. Structure placement reach recognizes the three-block width. The deletion stick requires all six physical cells; a partial selection fails before any block is removed.

Right-clicking any complete part opens the linked Drone Control screen, including its real mode controls and Tower Control/upgrade screen. The server checks the clicked part is loaded and within eight metres, the complete six-part console still exists, its controller is within 24 metres of the target terminal, and its pinned terminal is exactly that target. A stale, broken or differently linked console cannot authorize an old screen. The existing terminal and core ownership rules still apply. Like direct terminal use and the wall computers, the first operator claims an imported unowned terminal. Tower upgrades also verify the terminal belongs to the exact requested core.

Only the controller has a passive `DepotDisplayBlockEntity`. Tower construction pins it to that tower’s own terminal. There is no console ticker, new inventory, duplicate fleet, or independent status stream. The client draws live fleet, active/airborne counts, tier, mode, pads, battery, draw, load and supply/job status from the terminal’s existing synchronized snapshot. The three-metre readout uses a 48-metre render limit and permits its anchor’s monitor bank to extend beyond the single-block frustum box.

The one-item recipe is four steel plates, three console desks, one advanced circuit and one processor. Its whole-kit value is 30.7 steel-ingot equivalents; tower module accounting spreads that value across six placed cells. New industrial foundations use layout 4; persisted older layouts retain their own saved geometry.

## Art and reproducibility

Original code, geometry, control textures and documentation: OpenAI Codex (GPT-6), October 2026, MIT. `tools/tower_master_console.py` is the editable original model source. It exports 103 closed authored solids into six native block-part JSON models, plus the whole-model item. Instrument/keyboard/touch/label textures are opaque, deterministic, power-of-two originals retained in `art/tower-master-console/originals`; checksums and exact screen planes are recorded in `art/tower-master-console/provenance.json`.

The native body references the approved tower’s existing `tower_detail_*` material textures without rewriting them. Their original image-generation source remains in `art/tower-v3-sample`, with its existing provenance. The owner asset library catalog and `LICENSE_POLICY.md` were reviewed; this console does not copy owner-library pixels, third-party models, or Minecraft artwork.

Generate this console alone with `python tools/tower_master_console.py --write --check --preview`. The main tower generator imports its metadata and calls `write_all(root=...)` after the shared runtime-material pass. The narrow writer does not modify language, recipes, tower layouts, or other fixtures. Native preview `build/drone_tower/master_console_native.png` is an offline render of the actual exported JSON and PNG assets with a z-buffer and back-face culling. Its screens show their static instrument surface; the live text is supplied by Minecraft.

## Validation

The narrow asset check verifies native model bounds, positive-volume geometry, closed exposed faces, UV bounds, near-coplanar surfaces, cross-part seams, and opaque power-of-two control textures. `MasterConsoleGameTests` covers all four orientations, one controller, whole-console selection, one-kit dismantling, first-user claim through a dummy part, owner and stale-link restrictions, exact-core upgrade authority, complete versus partial no-drop deletion, low monitor ceilings, and the top build-height boundary. `TowerRenderChecks` inspects the actual linked client readout alongside the two wall computers and formed wall screens, and verifies the real MBS preview matches occupied cells and monitor headroom failures. `TowerClientGameTests` uses a normal world right-click, toggles and restores the owner's mode, opens Tower Control, starts the actual next tier and records the master's front and wall-computer closeups.

The isolated combined build passed on 8 October 2026 in 2m 51s, including all five master-console server tests within 79 passing server tests and both actual client test classes. The 41 real Minecraft captures include front-facing live instruments, normal console controls, the upgrade screen and the finished command room at night. Evidence is retained in `build/drone_tower/validation-v5.json` and `build/drone_tower/gameplay-v5-validated`; the [industrial tower record](drone-tower-industrial.md) lists the exact candidate hash and delivery/restart behavior.
