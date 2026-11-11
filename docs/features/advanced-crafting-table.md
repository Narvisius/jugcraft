# Advanced Workbench (block `advanced_crafting_table`)

Status: implemented (take 2 of the mockups approved by Justin, 26 October 2026; its 21 recipes approved the same day;
every tower block's recipe moved onto it and the name changed from "Advanced Crafting Table", 28 October 2026)
Proposal issue:
Owner: Justin (design), Claude (build)
Target milestone and tier: mid game, before the drones
Primary specialty and supported player role: crafting; every player who builds drones or the tower's advanced kit

## Player experience
A heavy steel workstation, 3 blocks wide, 2 high and 1 deep, placed whole from one item facing the player. Its bench has
two pedestals of drawers, a black resin top with a lit 6 x 6 glass grid let into it, a bench vise, a drill and a soldering
station; behind it a slotted tool wall (wrenches, screwdrivers, the pinned plan of a drone, two power rails, a shelf of
parts bins), over it a light gantry with an LED bar and a holo emitter over the grid; beside it a parts cabinet with four
drawers, six labelled bins and a touchscreen.

Using it opens a 6 x 6 crafting grid with its result and the player's inventory. The items stay on the table when the
screen closes and lie on the grid's cells, cell for cell. When the grid holds a recipe the result turns slowly over the
grid (a drone as a see-through cyan hologram of its own model, anything else as itself) in a faint beam from the emitter,
the status lamp on the bench's front lights green and the touchscreen shows the result's name and READY; otherwise it
shows NO MATCH (or GRID EMPTY) and a map of the filled cells. Shift-clicking the result makes as many as the grid and the
inventory allow.

## Connections
- Recipes: the data-driven type `jugcraft:advanced_crafting` (crafting/AdvancedCraftingRecipe), a shaped pattern of up to
  6 x 6 that is always bigger than 3 x 3 (wider or taller than three), so it can never be the same as a crafting table's
  recipe or another mod's; it matches anywhere on the grid and mirrored, and a space is an empty cell. Example:
  `{"type": "jugcraft:advanced_crafting", "pattern": ["R    R", " B  B ", "  FC  ", "  PP  ", " B  B ", "R    R"],
  "key": {"R": "jugcraft:rotor_assembly", "B": "#c:plates/bronze", "F": "jugcraft:flight_controller",
  "C": "jugcraft:basic_circuit", "P": "jugcraft:lead_acid_pack"}, "result": {"id": "jugcraft:drone_t1"}}`.
- What is made on it (tools/advanced_crafting.py; the recipe sheet approved 26 October 2026): 21 recipes that replaced
  their 3 x 3s. The nine drones keep the parts they had, laid out bigger (the T1 adds a basic circuit and a second
  lead-acid pack). The holo kit was made fairly expensive; since 28 October 2026 it has no recipe at all (the
  holograms are the tower's own: docs/features/drone-tower-tripillar.md, "The holograms"), and the core and consoles
  take the emitter's glass pane, glowstone and circuits instead. The Drone Tower Core, the depot
  terminal, the tower master and control consoles, the tower info totem, the cargo and energy exchanges. Everything
  else stayed on the crafting table then: the structural blocks, the drone parts, furniture, the simple consoles.
- **Every tower block (28 October 2026).** Justin: "start making recipes for the blocks for the drone tower ... use the
  advanced crafting bench ... so that it doesn't interfere with any kind of vanilla recipes", "Same cost, bigger shape",
  "All tower blocks". `advanced_crafting.convert()` turns every crafting-table recipe that makes a block of the tower or
  the drone depot (tower.tower_blocks, drones.drone_blocks, and any other block whose recipe is switched on by the drones
  feature: 255 recipes, the building materials and their stairs and slabs, the room kits, lights, signs, fittings, and the
  other routes to them such as the porcelain insulators and the Jimtafacts composite panels) into a workbench recipe with
  the same ingredients in the same shape, each cell doubled across (or down, for a pattern one wide), making twice as
  many. Each block costs what it did, so the tower's tier costs are unchanged; a recipe bigger than 3 x 3 can't be the
  same as any crafting-table recipe, vanilla's or another mod's, and `check_all()` checks that no two workbench recipes
  are the same grid (276 in all). The generators keep writing the 3 x 3s: generate_material_data converts them last of
  all, and the tower's own tools (tower_control_console._recipe and the pieces that write their own) as they write.
  Not converted: the drone parts and the Field Manual (items), the cryogenic tank kit (a storage tank), and the
  workbench itself.
- **Its name (28 October 2026):** "Advanced Workbench", because upstream's Jimtafacts industry has its own "Advanced
  Crafting Table" (an Auto-Crafter housing). Only the name changed: the block id, recipes and placed tables are the same.
- **In JEI:** the workbench has its own page (client/compat/JugcraftJeiAdvanced): every recipe laid out on a 6 x 6 grid
  with its result, the workbench as the station. Recipes are server data in 26.x, so the page reads
  recipe_view.json "advanced_crafting" (tools/advanced_crafting.view, written by tools/recipe_view.py).
- The generators that wrote those 3 x 3s (generate_material_data for drones.DRONE_CRAFTING and tower.CRAFTING,
  holo_projector.py, big_map_table.py, holo_command_table.py, tower_control_console.py) call advanced_crafting.apply()
  after writing, which writes the 6 x 6 over each, gated by the switches the file had plus those its new ingredients are
  registered under (a processor's silicon).
- The table itself is a room kit piece (tower/RoomKit, display "crafting"; tools/adv_crafting_table.py writes its models,
  the "workshop" room) with an ordinary 3 x 3 recipe: steel plates round a crafting table, a glass pane, two basic
  circuits and redstone (tools/room_kit_recipes.py). Checked against every vanilla recipe and every recipe in the mod:
  nothing else has that grid (28 October 2026, kept: Justin, "Keep it").

## Balance and automation
One of every item on the grid is used per result (a recipe uses every filled cell); a container (a bucket) stays in its
slot, or goes to the player if the slot still holds more. The grid is a container at the table's anchor, so hoppers can
fill or empty it; nothing crafts by itself.

## Multiplayer and persistence
The server finds the result whenever the grid changes and tells every screen open on the table at once, so two players
see the same result and taking it never makes something the grid no longer holds. The grid is saved with the table and
drops when it is broken. Server load: nothing ticks; the table sends what it holds to the clients only when it changes,
and the clients draw the items, the hologram, the lamp and the screen.

## Dependencies and assets
None beyond the mod. Models and textures: tools/adv_crafting_table.py (act_ materials 64 square, the pictures in
act_atlas 512 square, each inside one block); the screen's background textures/gui/advanced_crafting_table.png.

## Verification
- TowerGameTests.advancedCraftingTableCraftsSixBySix: the anchor places the whole table (six blocks, the grid at the
  anchor only); the test pack's recipe (three sticks and a log in a row: three ladders) matches anywhere and mirrored,
  not with an extra item or upright; making it takes one of each item; the T1 drone's 6 x 6 recipe makes it;
  Reinforced Concrete's doubled recipe makes 16 on it, and its old 3 x 3 makes nothing on a crafting table; the crafting
  table still makes the workbench; the grid's items drop when the table is broken.
- recipe_conflict_cases.json (upstream's): its Copper Busbar and Steel Armor Plate cases are gone, as those are no
  longer crafting-table recipes.
- check_mod_data checks every advanced recipe's pattern (one row length, at most 6 x 6, bigger than 3 x 3, its key) and
  its items and tags; tower_costs reads the type (values, and no two recipes of a type with the same shape).
- The mockup sheets (take 1 and 2) were rendered from the same geometry.

## World and event applicability
A crafting station: no world generation, ecology or dimension rules apply.

## Rollout and open questions
- The tower's step costs (structural blocks plus the holo parts) are reworked next, from these values.
- A drone result shows as a hologram of its model; other results show as themselves, turning.
