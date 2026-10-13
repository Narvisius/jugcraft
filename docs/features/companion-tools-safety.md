# Companion tool stowing and hazardous surfaces

Owner-requested on 9 October 2026, implemented locally with OpenAI Codex (GPT-6), extending the existing companion inventory, work clips and ground navigation. No new assets or dependencies.

## Tools

The Hand slot remains the single authoritative storage location. Hoes, knives and shears are drawn only for harvesting, chopping and shearing respectively. Other tool-component items, brass wrenches, flint and steel, fishing rods and brushes are stowed outside their supported actions. This is an actual empty main-hand equipment state while stowed, not just a hidden client model. It needs no free cargo slot, never drops or duplicates a tool, and retains durability, components, tool-supply behavior and the hand-lock preference. Cutting-board recipe planning reads the stored knife, so a stowed knife does not prevent starting work or fetching ingredients.

Eating temporarily owns the displayed hand. Sleeping, sitting, wheel running, social gestures, settings editing and unrelated work clear the usual held item. Torches and other ordinary held items remain visible during normal idle/follow/travel activity. Hand state is reconciled with the existing server tick; it copies/synchronizes only when the displayed stack actually changes. Existing saves need no migration.

## Safer movement

Companion ground paths reject vanilla recognized damaging blocks and lava, including fire, magma, lit campfires, cactus, berry bushes, wither roses and powder snow. The following data-pack tags extend the same checks for project-specific and future blocks:

- `peepo_companion:harmful_blocks`: ember beds and barbed wire.
- `peepo_companion:harmful_when_lit`: kitchen stoves and Halloween bonfires; applies only when the standard `lit` state is true.

Unlit stoves and bonfires remain usable surfaces. A harmless machine is not classified as damaging just because it is lit. Pathfinding checks the prospective cell and its floor; damaging path types are also assigned an impassable cost. Every ten ticks, staggered by companion ID, navigation checks at most three upcoming path nodes for newly introduced hazards and stops an unsafe route so the existing goals can replan. It allows the current cell to be left if a hazard appears beneath the companion.

The same loaded-only foot/body check filters precise processor, cooking-pot, cutting-board, crank, press, livestock, garden, supply/output, lunch, bed-exit and seat positions. The shared routine rechecks the destination before occupation, including approaches already within working distance, so close-range work cannot bypass path safety. The checks preserve existing diagonal fences and companion gate handling, search/path budgets, distance limits, and protection checks. They do not load chunks, scan the world, or provide damage immunity. Unknown future hazards require a tag entry or vanilla danger classification; knockback, player placement and environmental damage are still possible.

## Validation

Common/client compilation and assembly succeeded with `build-local.ps1 -Tasks @('compileJava','compileClientJava','assemble')`. The local Modrinth pack was rebuilt with `scripts/package_modrinth.py build`, including its library-lock validation. No automated tests or gameplay checks were run, following the owner's continuing instruction. Manual checks still needed: all three equipped work tools across work/rest/eating/social/follow transitions; full cargo; tool breakage and reload; knife recipe planning while stowed; detours around lit/unlit stoves and vanilla hazards; lighting a stove during travel; safe cooking-pot rim access, bed exits, diagonal fences and gates.
