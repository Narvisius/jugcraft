# Public Spawn City interactions

Owner request, October 22: visitors can use doors, trapdoors and non-container
amenities in Spawn City. AI assistance: OpenAI Codex (GPT-6).

The server now executes the block's ordinary interaction directly within town
protection. Doors, trapdoors, gates, buttons, levers, note blocks, crafting and
other inventory-free workstations work even while a player holds an item.
The tag explicitly includes bells and enchanting tables; native furniture allows
seating, display/light switches, bells and its construction bench. The bank ender
chest remains public because it exposes the visitor's personal inventory.

Stored inventories remain private: vanilla containers, native cabinets/appliances,
sided or proxy item storage and fluid tanks are rejected. Unknown block entities,
lecterns, jukeboxes and pots holding removable town decorations remain protected.
Iron doors/trapdoors retain vanilla redstone requirements. Shops and ATMs retain
their existing behavior. Player-owned land claims still apply.

Held-item placement, bucket use, dyeing and tool modification do not follow the
block interaction. Breaking and all other town protection remain in place. This
is an interaction rule update and applies to the existing town without rebuilding
it, moving NPCs or regenerating chunks.

All **nine town server tests passed** on October 22 (`town-interactions-tests-final3.log`).
The public-amenity scenario uses a real connected survival player, rather than
Minecraft's mode-hardcoded mock. It exercises door/trapdoor operation, controls,
workstation/ender-chest/ATM menus, native chair seating, protected inventories,
blocked bucket/placement/breaking actions and spectator restrictions. The first
spectator test exposed the unsuitable mock fixture; replacing it verified the
actual mode-dependent behavior. Repository checks passed.
The same local build also passed all 79 updater assertions after incorporating
the merged Windows handoff fix. This does not constitute a launcher playtest.

This local gameplay batch has not been deployed or promoted to the player updater.
