# Convenience transmutation and automated outputs

Owner-directed local change, 21 October 2026. Implementation: OpenAI Codex (GPT-6). Not published or deployed.

## Player behavior

The tablet remains useful for ordinary building supplies and convenience items. The server's explicit `jugcraft:tablet_outputs` item tag permits common wood, stone, glass, wool, concrete, terracotta, torches, lanterns, ladders and scaffolding. Unlisted items default to denied, including modded items, ores, metals, gems, storage blocks of valuable materials, machines, equipment, currencies and rare loot. A low essence price or being a placeable block is never enough to permit an item.

Dissolving still accepts the existing safe default items and credits their established essence value. It requires confirmation and protects stored contents/modified stacks. **Dissolving a valuable does not allow withdrawing it later.** The confirmation tooltip explains this. Existing knowledge, favorites and essence balances remain saved. Only permitted learned patterns appear in the browser; creating them still spends their value with no surcharge. Wallet/star payment, batch selection, favorites and search remain available.

Both the block table and portable tablet share this policy, as do the philosopher stone's menu, legacy transmutation menu, handheld forming action and the Mercurial Eye's building selection. Server checks run before payment and output, including for an old selection. Data packs can deliberately customize the item tag.

Condensers and density gems use the same server allowlist. Existing valuable targets remain saved but pause before consuming inputs, star charge or essence. Existing products remain recoverable. Owners can replace their targets with approved supplies to resume. Collector/relay transfers also stop at a blocked condenser target. New density gems default to oak logs; their picker cycles oak logs, glass, torches, stone bricks and ladders, skipping entries removed by a data pack.

This is **not a complete economy rebalance**. Recipe conversion, collector fuel upgrades, ore processing and NPC buying/selling retain their existing behavior and still need an economy audit. Ordinary allowed wood and stone can feed legitimate crafting and trade chains.

## Interface

Uses the owner's Exchange `transmute.png` unchanged: grey panel, purple alchemical circles and recessed inventory. Twelve learned-item slots surround the output circle; search, confirm-dissolve, withdraw, batch size, favorites and page controls sit alongside it. Unused controls baked into the source atlas are covered by the screen renderer; they are not presented as nonfunctional buttons. Transaction details appear in button tooltips. The interface retains keyboard-accessible controls and the existing reduced-motion preference.

Provenance: [Exchange tablet import](../../art/owner-library/ports/exchange-tablet/README.md).

## Validation

Local validation, October 21:

- The final gameplay implementation ran all **2,256 required server GameTests**. The only two failures were in `AlchemyPlayabilityGameTests`: its old Gem-armor manufacturing expectation and a new assertion that assumed output would not reuse an emptied feed slot. Both test assertions were corrected, with no production-code changes. The affected **23-test group then passed**; the other **2,233 tests had passed** in the combined run.
- The rendered `ConvenienceTabletClientGameTests` completed in the combined run: real menu packets, paging, confirmation/dissolution, paid torch output, valuable-pattern exclusion, two GUI scales and keyboard focus. The final screenshots confirm the supplied circles, unobstructed labels, inventory slots and visible output. A separate final client-only run passed; its captures are retained in the local `tablet-review/screenshots` folder.
- Repository/documentation checks passed. The data/recipe/art audit passed: **6,240 material IDs**, 13,187 block/item models, 3,729 quad parts and 176 rendered model checks. All 394 output-tag entries are unique; required supplies are included and tested valuables excluded. The supplied and runtime tablet atlases have identical SHA-256 hashes.
- Regression coverage includes permitted paid output, already-learned valuables, legacy/handheld bypasses, saved knowledge, ghost slots, confirmation, wallet/star billing, inventory capacity, paused saved condenser targets, preserved samples/products/buffers, rejected relay intake and density target selection.

These are local automated tests. Owner survival balancing and the coordinated hosted/client release remain pending. No server files or public update manifest were changed.
