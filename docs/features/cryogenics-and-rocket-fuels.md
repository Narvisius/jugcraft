# Cryogenics and rocket fuels — package 7

Technology package 7, implemented locally with OpenAI Codex (GPT-6). On 16 October
2026 the owner authorized integration with main and local validation of all seven
packages. The earlier testing hold is superseded. **Keep this work local until the
owner has played it and approves publication.**

Current build, test evidence, draft reconciliation and remaining release checks:
[seven-package integration record](tech-seven-local-integration.md). The detailed
verification lists below are release checklists; only results explicitly recorded
there are claimed complete.

## Player route

Use existing air separation/electrolysis for oxygen and hydrogen, and cleaned biogas
or methanation for methane. Keep the original Cryogenic Liquefier for all three:

| Input | Output | Retained tool | Base time / energy |
|---|---|---|---|
| 1,000 mB oxygen | 250 mB LOX | None, original recipe | 80 ticks / 96 JE/t |
| 1,000 mB methane | 250 mB liquid methane | Methane or advanced hydrogen cooling module | 160 ticks / 96 JE/t |
| 1,000 mB hydrogen | 250 mB liquid hydrogen | Advanced hydrogen cooling module | 320 ticks / 96 JE/t |

Cryogenic Insulation uses existing PTFE liners, alumina and porcelain. The first
cooling module uses stainless plates, precision bearings, insulation and a valve;
the second consumes the first module plus ceramic bearings, insulation and an
advanced chip. Neither tool wears out. One appended inventory slot preserves the
existing liquefier's tank indexes, energy and IDs; it is excluded from sided item
transfer. Removing/replacing tooling cannot consume a batch's input fluids or
produce its output early. Missing tooling is reported as a missing machine tool.

The existing Chemical Distillation Station converts 250 mB of any of these three
liquids back to 1,000 mB of the corresponding gas, 160 ticks at base 32 JE/t. Each
liquefaction/vaporization round trip returns its starting gas quantity and spends
power. End-to-end upgraded balance still needs testing. No boil-off or pressure UI.

## Storage and visual design

Two optional one-block variants extend `FluidTankBlock` and `FluidTankBlockEntity`:
32-bucket Insulated Fluid Tank and 64-bucket Vacuum-insulated Fluid Tank. Both use
the existing storage component, pipes, pumps, tank groups, comparator and gauge.
The original 16-bucket capacity constant and old tanks remain unchanged. Mixed
groups sum their actual capacities. Both loot tables copy stored fluid; recipes
use raw parts, avoiding accidental destruction of a filled tank as an ingredient.

Models use existing silver steel, olive collars and dark bases, with fittings at
all six face centers supported by the vessel. Native 16×16 item maps supply twelve
new component/propellant/motor icons. Both built-in model styles have authored tank
geometry. Native resources have matching full-generator ownership.

The owner library was considered, including
`originals/Blocks/Big Cannons and Mounted Guns/textures/item/hydrogencanister.png`.
This slice uses editable native item maps and existing shared machine materials
to keep the family consistent. No external art was added.

## Storable propellants and waste

These are deliberately abstract game recipes, not real production procedures.
The Bulk Chemical Reactor supplies hydrazine and N2O4; both recipes consume redstone
and existing industrial reagents, require output space and leave a solid Propellant
Process Residue. The Conditioner washes two residue with 500 mB water into 500 mB
existing Process Recovery Concentrate. Package 6's clay stabilization/concrete
route handles that concentrate. No silent waste deletion or fertilizer shortcut.

## Usable rocket assemblies

The compact Chemical Reactor packages 1,000 mB of each new propellant with one
Rocket Casing into a sealed manufacturing ingredient. These are not refillable
portable fluid inventories. They and their fuel are consumed by assembly.

| Rocket Workshop assembly | Inputs | Loaded motor yield |
|---|---|---|
| Methalox | Methane tank + LOX tank + 2 nozzles | 3 |
| Hydrolox | Hydrogen tank + LOX tank + 2 nozzles | 4 |
| Hydrazine thruster | Hydrazine tank + nozzle + porcelain insulator | 1 |
| Storable bipropellant | Hydrazine tank + N2O4 tank + 2 nozzles | 3 |

The `atmospheric_rocket_motors` ingredient tag includes these four assemblies and
the existing rocket motor. Survey, Cloud-seeding, Clear-sky, Delivery and Line
Rocket recipes use this tag while retaining recipe IDs and all other inputs.
Solid and kerosene/LOX manufacturing remains available. Fuel-pair compatibility is
enforced by assembly ingredients. These items do not create new vehicles or alter
payload flight, range, damage or destination access. Future MMH and orbital/station
systems remain TODO items. No live-server change or PR is part of this work.

## Release verification checklist

When the owner authorizes it: compile and data/icon audit; cooling/tool gating and
old save loading; manual and shift-click tool removal; output backpressure; fluid
volume and complete production energy balance; tank break/place, save/reload,
mixed capacities and pipe/comparator operation; all five payload substitutions and
invalid fuel pairs; optional feature configuration; guide/viewer readability;
multiplayer permissions and an owner-controlled local visual playtest. Only the checks recorded in the integration record have been run.

AI assistance: OpenAI Codex (GPT-6). Resource authoring is not validation.
