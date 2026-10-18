# Alchemical gear — local first slice

> Publication update, 18 October 2026: the owner accepted the combined alchemy presentation and authorized publishing the alchemy additions and UI fixes. Historical local-only checkpoints below are superseded. Current scope and validation are recorded in [Alchemy interfaces and access](alchemy-interface-and-access.md).

Owner request, 16 October 2026: build a non-spellcasting magical equipment branch inspired by Equivalent Exchange 1/2, using the owner's supplied assets. Implementation: OpenAI Codex (GPT-6), under the owner's session authorization. This branch remains local for review.

## Feature contract

Players turn spare mundane materials into useful supplies, maintain their tools, and gather nearby drops. The Philosopher's Stone opens a portable stone transmutation tablet. Its input is deliberately dissolved with a confirmation; its physical sample is retained when forming one matching output. Closing the interface returns all three slots. Dissolving produces **essence**. The scale and stone share the existing personal Prima Materia ledger and audited material catalogue, including its 25% forming markup and exclusion of named, enchanted, stored or otherwise modified matter.

Repair Talisman: toggle with use, then hold or equip in a Charm slot. Once a second it repairs up to four durability on one held tool or worn armor piece, for one essence per durability. It requires a repairable damageable item, preserves its other components, and never repairs a second duplicate talisman's worth. It does not repair the backpack's contents or recharge machines.

Black Hole Band: toggle with use, then hold or equip in a Ring slot. Once a second it draws up to eight eligible item entities within six blocks toward the wearer, costing one essence per moved entity. It respects pickup delay, loot ownership, line of sight and the shared claim boundary. Ordinary Minecraft pickup delivers the stack; full inventories suspend attraction. It never opens containers, loads chunks or pulls mobs. Wearing duplicate bands does not multiply its work.

All three require understood Assay. The existing route is First Light observation and bench study, then examination and study of raw matter: no wand, spell inscription or spell cast is required. Crafted gear is tradeable. Prima Materia stays personal. Both passive items default off; use toggles and reports the balance. Feature disablement, missing research and exhausted balance stop their effects.

## Assets and inspiration

Icons are byte-for-byte imports from `art/owner-library/originals/Magic/textures/item/`: `philosophers_stone.png`, `repair_talisman.png`, and `rings/black_hole_off.png` / `black_hole_on.png`. Originals and their notices remain intact. `tools/concordance_alchemical_gear.py` records the mapping; `tools/owner_art.py --check` verifies it. The tablet also uses the supplied `textures/gui/transmute.png`, imported unchanged as `textures/gui/alchemy/transmutation.png`. The client samples its two sigils, with a custom stone/silver frame, recessed item sockets and native keyboard-accessible action buttons. Runtime item models use Jugcraft identifiers. No library executable is run or external implementation copied.

Design inspiration: the original Equivalent Exchange 1/2 transmutation and utility-equipment idea, also described by the [ProjectE maintainers](https://www.curseforge.com/minecraft/mc-mods/projecte). ProjectE is not a dependency. Jugcraft uses its own code and existing bounded ledger, not a second EMC economy.

## Validation plan

Exercise menu authority, sample retention, explicit dissolution confirmation, edited-input cancellation, full output, close-return, ledger bounds and modified-item rejection. Exercise paid repair, preserved components, disabled/unknown/empty-ledger behavior, duplicate gear, and ring ownership/claims/occlusion/pickup/full-inventory behavior. Render and operate the interface in a disposable client world; package that world for the owner to play independently.

## Later slices

The [Dark Matter workshop](dark-matter-workshop.md) follow-up implements fuel tiers, covalence repair and three Dark Matter tools locally. Remaining Dark Matter equipment, red matter tools and armor, charged storage stars, elemental utility amulets and mobility rings, and bounded collectors/condensers and automation are still planned. Each needs its own cost, progression and exploit review.

## Results

- Full offline Gradle build passed: **1,771 required server game tests**, including **13 new equipment/menu cases**.
- Full data/recipe/art audit passed: **2,916 material IDs**, 8,516 block/item models, 3,709 quad parts and 171 combined models.
- Owner import audit passed; the four new sprites match their supplied originals byte for byte.
- Nine packaging unit checks passed. A scoped generator replay matched all 11 new/reused JSON outputs and 23 interface language entries.
- The rendered singleplayer client test passed, then passed again after visual polish. It used real menu buttons to dissolve eight copper for 512 essence and form one iron for 320, kept the sample, returned both sample and output on close, checked keyboard focus and the GUI scale 4 setting, and activated both passive items by normal right-click. Real server ticks repaired the held pick and attracted an emerald until ordinary pickup collected it.
- Final screenshot review moved the sample-retention note off the cover border and dimmed the background. The independent **Jugcraft Alchemy Workshop** save is included in the local Modrinth pack.
- Regeneration also reconciles one existing label with the electronics generator: Automation Upgrade displays as **Automation Card**, as its existing guide already calls it. Its behavior is unchanged. All other pre-existing language values are unchanged.
- No owner playtest, two-person manual multiplayer test or publication is claimed. Main and remote branches remain unchanged.

## Tablet follow-up — 16 October 2026

Owner feedback replaces the book presentation with a stone-and-silver tablet, violet alchemical circles, animated essence conduits, a persistent balance, explicit dissolve confirmation and a retained sample/output pair. The three real item slots and inventory remain vanilla container slots. All visible Assay/gear currency language (tablet, tooltips, chat, journal and codex) now calls the value **essence**. The original book-based encyclopedia and constellation Spell Composer are unchanged.

Compatibility: this is a presentation change. The existing `prima_ledger` player attachment, `prima_materia` resource ID, `grains` material-value JSON field, catalogue, costs, limits and arithmetic remain unchanged. Saved balances are reused directly; no conversion or migration is needed. Alchemical essence is the name for this personal Prima Materia balance; Principle-specific essence in vials is still a separate existing resource.

Follow-up validation:
- Common/client/test compilation and the rendered equipment client test passed. The tablet performed a confirmed 512-essence deposit and a 320-essence withdrawal, retained the sample and returned both slots on close. Passive equipment still repaired and collected through real server ticks.
- Screenshots reviewed at GUI scales 1 and 2, with an assertion that their actual GUI widths differ; keyboard focus passed. The confirmation button uses a distinct warm accent.
- Canonical generator replay updated 30 localized entries and found no generated JSON changes. Assay catalogue/conversion-cycle checks, journal checks and owner-import audit passed. The imported sigil sheet is byte-identical to the source.
- The preceding implementation's full 1,771-test server run was not repeated for this presentation-only follow-up. No saved-data IDs, material values or transaction arithmetic changed.
- A separate **Jugcraft Alchemy Tablet** save is provided for manual play, preserving the prior Alchemy Workshop save. Nothing has been pushed to main.
