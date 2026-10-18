# Connected alchemy — local integration, 17 October 2026

> Publication update, 18 October 2026: the owner accepted the combined alchemy presentation and authorized publishing the alchemy additions and UI fixes. Historical local-only checkpoints below are superseded. Current scope and validation are recorded in [Alchemy interfaces and access](alchemy-interface-and-access.md).

**Current behavior:** the [alchemy playability pass](alchemy-playability.md) supersedes retained-sample learning, the narrow catalogue, fuel fees, online factory requirements and the old menu flow below. This record preserves the earlier delivery evidence.

Owner request: implement the remaining systems identified after the 108-entry asset audit, use the remaining suitable assets, and keep the result local for an owner-controlled playtest. Implemented by **OpenAI Codex (GPT-6)**. This extends [Complete Alchemy](complete-alchemy.md); it adds behavior to those entries rather than registering duplicate items or another currency.

## Player controls

| System | Implemented behavior |
|---|---|
| Learned transmutation | Table, tablet and Stone open **Learned patterns**. Learn a retained sample, browse icons, optionally search, favorite, and form without carrying the sample again. Dissolving also learns eligible materials. Knowledge is personal and saved through death. |
| Philosopher's Stone | Use terrain to transform stone/cobble, dirt/sand/grass and gravel/sandstone. Sneak-use terrain cycles 1×1, 3×3 and 5×5. Sneak-use air opens ordinary crafting. Each successful terrain change costs at least 8 essence. |
| Density Gem / Void Ring | Use Density Gem, or sneak-use Void Ring, to configure nine material filters. Click a filter with a carried stack to copy its identity; an empty cursor removes it. Explicitly start automation. Density produces iron, gold or diamond; Void deposits essence. Empty filters do nothing. |
| Bag and chest artifacts | Activated converters consolidate filtered material; Repair Talisman repairs one stored tool; Black Hole Band collects drops matching an existing stored item. Open storage pauses its background processing. Redstone pauses chest automation. |
| Pedestal | Retains the original six artifacts and adds Black Hole Band, Archangel, Ignition, Zero, Watch and Mind Stone. Harvest Goddess now harvests/replants mature crops. Pickup inserts into adjacent permitted storage; defenses affect hostile mobs; Mind Stone collects XP; Watch accelerates adjacent Matter furnaces/condensers. |
| Collector fuels | Choose Charge or a stopping tier: Alchemical Coal, Mobius Fuel or Aeternalis Fuel. Input fuels advance through the existing three fuels; intermediate outputs continue until that tier. Charge mode retains neighbor/star export. |
| Klein Star equipment power | Equipment pays from the wallet first, then physical carried stars in inventory order. Nested stars are never searched. Ordinary transmutation and workshop ledger transfers retain their existing explicit controls. |
| Gem armor | **Gem abilities** in the tablet and Manual, or an optional keybind in Controls: helmet night vision, chest repulsion, leggings forward dash, boots paid flight. Wear an enabled corresponding piece. Boots share Swiftwolf's flight lease; armor removal stops its grant. |
| Mercurial Eye | Use in air to choose a learned building block. Use a surface to place a 3×3 face, paying formation price plus 8 essence per success. The older offhand-block route remains when no pattern is selected. |

## Limits, saved data and integration

- Existing Assay research, audited catalogue, shared ledger, claims, party access, item placement and Concordance combat rules remain authoritative. Only plain, currently eligible material can be learned or dissolved. Catalogue reloads invalidate unavailable patterns without deleting the saved knowledge.
- Learned inventory icons are server-synchronized ghost slots. Every click mode, shift-transfer and close path protects them from becoming actual items. Real sample/output slots return their real contents when closed. Formation checks current price, access, balance and output space before spending.
- Automatic converters consume at most 16 matching items each second. Loose converters skip hotbar/offhand feedstock. One carried bag is visited each second on a rotating schedule. Density needs output room before consuming and makes at most one target per pulse. Stored equipment keeps its components, names and enchantments.
- Pickup scans at most 24 drops and inserts at most four stacks per pulse. It honors pickup reservations, ownership, protection, visibility and actual insertion counts. Pedestals check both source and destination permissions. Chests keep their original party gate and no unrestricted hopper access.
- Mind Stone collects at most 100 XP per pulse from up to 16 orbs. Merged orb counts and partial large orbs conserve exact XP; the remainder stays in the world. Each affected orb costs one pedestal essence.
- Watch bonuses are limited to one per machine per second across handheld and pedestal watches. Each bonus costs 128 essence plus the target machine's usual processing cost. It never accelerates arbitrary ticks or loads chunks.
- Fuel upgrades take one step per second and cost 1,024 / 4,096 / 16,384 essence. Selected final fuel is never upgraded further. Collectors expand from one to eleven physical slots while retaining old slot zero; mode and contents are saved. Paused, protected, disabled and absent-owner workshops retain their normal stop rules.
- Building uses native placement, collision, loaded-position and claims checks. No stored container blocks or fluids are created by the Eye. Failed paid placements refund their exact cost. Stone changes only matching block types within its bounded face.

## Owner assets

`tools/concordance_ee_expansion.py` is the canonical source map and help-data definition, included by `concordance.py` and `owner_art.py`. Eleven remaining supplied GUI sheets are used by the real inventory/ability interfaces: chest, book, three collectors, two condensers, density, Eye, NEI and portal-device sheets. The legacy NEI arrow and portal-device symbols are supporting UI art; they do not claim to add those legacy mods or a new portal system.

The density, Void Ring and Mind Stone active icons now reflect their real state. Chest and condenser lids animate using Minecraft's chest model with the supplied textures. The reduced-motion option is honored. All imported PNG/OGG files remain byte-identical to the owner library; originals and notices remain untouched.

## Validation and delivery

Executed locally on 17 October 2026, with Minecraft 26.3, Fabric Loader 0.19.5, Java 25 and the existing 18-library lock:

- `./gradlew build --offline --console=plain`: **all 1,909 required server game tests passed**, including 23 new cases. `ConnectedAlchemyGameTests` covers conversion, filtering, ghost-slot custody, payments/refunds, collector progression/persistence, claims, building, XP conservation, pickup, harvest, armor and concurrent lid viewers.
- `./gradlew runClientGameTest -PclientTests=CompleteAlchemyClientGameTests --offline --console=plain`: real rendered client checks passed. `ConnectedAlchemyClientChecks` exercises learned-pattern/search/favorite/form packets, ghost/filter clicks, Eye selection, collector controls, lid synchronization and armor effects. Screenshots cover the interfaces and supplied chest/condenser models, alongside the existing equipment showroom.
- `python tools/check_mod_data.py`: full audit passed for 3,021 material IDs, 8,522 block/item models and supporting geometry/data/recipes. `python tools/owner_art.py --check`: every imported owner file matches its source.
- `python -m unittest discover -s scripts/tests -v`: **67 tests passed**. Locked dependencies and repository structure/documentation links also passed their checks.
- Validation caught and fixed portable crafting losing ingredients on close. Updated the old Density-menu expectation and used a real Survival player for flight tests. Collector cost tests now isolate exact debits with a full buffer, avoiding fresh-roof lighting propagation affecting their expected balance.

The separate **Jugcraft Connected Alchemy** save includes all 108 alchemy entries, labeled workshops and practice supplies, six learned sample materials, 500,000 wallet essence and a star holding another 500,000. The local `.mrpack` includes that save and the controls guide. Its one-time ownership datapack binds the showroom to the actual first player; no automated test controls the player or camera in the packaged world. Older test profiles and saves are preserved.

Owner playtest, two independent clients and multiplayer load testing remain pending. Nothing is pushed or published by this local integration.
