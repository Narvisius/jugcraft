# Remaining contribution reconciliation, 21 October 2026

Owner scope: finish ready contributions, repair conflicts, test together, merge
and prepare hosting/player files without another approval. Integration by OpenAI
Codex (GPT-6). Existing contributor branches remain intact.

## Direct integrations

- **#442**, `5c28cc2287a7ce21df002bbeb178349f918e4e31`: Workshop Bench,
  Small Ceramic Kiln, two-block Small Crucible Furnace, tools and early
  plates/wire/circuits, bronze/brass/solder and zinc plate. Preserve the approved
  cable yields, manual work, sounds, inventories and older recipes. Registries
  are merged as unions; the alchemy catalog is regenerated from combined recipes.
- **#441**, `06d211d0e86aa2947028fad83b42f268f25884e0`: Frostward College
  **M0**, current interiors, instructor, read-only journal consultation, personal
  courtyard memory and bounded winter effects. Manually placeable, not worldgen.
  Academy and Exchange client tests both remain registered. Larger college plans
  and other room furnishing/stair-edge review are still future work.

Detailed records: [workshop](starter-workshop-batch-1.md),
[college](frostward-academy.md).

## Older technology drafts

The drafts predate the seven-package integration. Closing them as superseded by
canonical systems does **not** claim every original commit or proposed variant
was merged verbatim. The original PRs preserve the designs and discussion.

| PR | Canonical implementation / reconciliation | Residual difference |
|---|---|---|
| [#363](https://github.com/jimbozoomer-byte/jugcraft/pull/363) copper entry | `tools/machines.py`: copper-conductor Electrolytic Cell; `check_self_output_gates` audits circular costs. | No duplicate cell. |
| [#387](https://github.com/jimbozoomer-byte/jugcraft/pull/387) purity | `tools/advanced_electronics.py`: concentrated to electronic-grade acid in the shared Conditioner, measured spent mixture, wafer/chip uses. | Current yields/waste accounting; no extra purification station. |
| [#394](https://github.com/jimbozoomer-byte/jugcraft/pull/394) phosphates | `tools/phosphate_processing.py`: slurry/filter/industrial acid, superphosphate, gypsum and farming. | Cola and a food-grade acid chain: [cola-and-mmh.md](cola-and-mmh.md) (pending review). |
| [#401](https://github.com/jimbozoomer-byte/jugcraft/pull/401) lead-acid | `tools/battery_expansion.py`: electrodes/separators/cells and `lead_acid_battery_bank`, 2,000,000 JE / 1,024 JE/t. | No second incompatible 1.6M battery. |
| [#402](https://github.com/jimbozoomer-byte/jugcraft/pull/402) porcelain | `tools/ceramics.py`: powder/press/oven/finisher and canonical insulator, alternate transformer recipe. | No parallel short furnace chain. |
| [#405](https://github.com/jimbozoomer-byte/jugcraft/pull/405) alumina | Finished `alumina_plate` feeds the alternate shared processor assembly. | No duplicate substrate/blank. |
| [#409](https://github.com/jimbozoomer-byte/jugcraft/pull/409) magnalium | Existing `aluminum_magnesium` alloy, frames/panels; now adds six cladding or two propeller sets from four existing alloy plates. | Keep canonical 7:1 alloy instead of duplicate 3:1 metal. No vehicle stat changes. |
| [#414](https://github.com/jimbozoomer-byte/jugcraft/pull/414) stacked fuels | `tools/cryogenic_fuels.py`: methane/hydrogen cooling, filled tanks/motors, hydrazine/N2O4 and paid warm-back in the Distillation Station. | Retained modules/current yields stay; MMH (from hydrazine and methyl chloride): [cola-and-mmh.md](cola-and-mmh.md) (pending review); methylamine chemistry remains TODO. |
| [#406](https://github.com/jimbozoomer-byte/jugcraft/pull/406) flow proposal | Paired tank/stack upgrades, vanadyl sulfate/electrolyte, saved reserve/mode/priority/rate controls. | One shared electrolyte/energy ledger. |
| [#407](https://github.com/jimbozoomer-byte/jugcraft/pull/407) fertilizer proposal | Nitrogen/phosphate tags, mixed blend, real growth applications, sprinkler/hydroponic integration. | Eight separate crop groups and weak-legume tuning remain future balance work. |
| [#422](https://github.com/jimbozoomer-byte/jugcraft/pull/422) synthetic fuel | `tools/synthetic_fuels.py`: regional cobalt, reusable ceramic catalyst, synthesis/refining, paid water/waste recovery. | Keep current common recipes and outputs. |

## Release preparation and verification

The internal pack name is JUGCRAFT. Offline packaging produces a Windows import
helper, direct pack, server-only upload ZIP, managed-file inventory and integrity
manifest. The manual GitHub workflow requires successful Build/Foundation on its
exact main commit, reuses the tested JAR and creates a draft prerelease. It never
deploys a server. See [server releases](../SERVER_RELEASES.md) for native Modrinth
linked-server updates and owner account/address steps.

Combined validation is pending at authoring. The integration PR records final
build/server/client/packaging results; main must receive the same tested tree.
No human playtest, live deployment, public Modrinth listing or launcher update is
claimed. Existing IDs/world data remain supported. No test worlds ship in the pack.
