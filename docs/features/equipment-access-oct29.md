# Equipment ownership, not research — October 29

Owner direction: no research requirement to equip or use owned equipment. Work by
OpenAI Codex (GPT-6), local only. Existing collection, encyclopedia, crafting,
aircraft and town edits are preserved.

Removed Relic Lore from Wayfaring belt/charm/boot insertion and effects; removed
research requirements from Fire/Water Bangle effects, Bonding Charm, folded
Clockwork Porter deployment, Crimson Chalice, Taglock and Astrolabe use/recall.
Astrolabe recall still needs an observed pattern and spends resonance with the
same once-per-occurrence limit. No research is silently granted to bypass a gate.
Slot compatibility, costs, cooldowns, feature switches, claims/PvP protection,
non-stacking effects and the belt's occupied-extra-slot removal protection remain.

Regression coverage added for every resolved tagged accessory/slot pair on a
fresh survival player and actual survival-inventory equip/remove packets. Existing
effect tests now require no research for previously blocked gear. Guide/tooltip
source text and generated English strings are being kept consistent.

Validation so far: compilation passed; all 119 focused server game tests passed
(`build/equipment-review/combined.log`). The fresh-survival-player audit accepted
153 item/slot pairs across 11 slot types with no research granted. The rendered
inventory packet test passed for all 11 slot types: ordinary inventory packets
equipped and removed one representative item per type, waited for the server
acknowledgement, and verified no loss or duplication. Evidence:
`build/equipment-review/client.log` (successful run, 3m20s). This is not a release or a live-server fix yet.

The combined run also corrected a pre-existing familiar test fixture: its mock
player must stand inside its loaded test structure, not at the unloaded origin.
A failed familiar spawn no longer records a ghost worker or reports success.
