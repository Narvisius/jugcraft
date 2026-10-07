# Jugcraft World Designer

Draw the landscape for a **new Minecraft 26.3 world**, paint biomes from your installed mods, choose spawn, place Jugcraft's walled city, and pin village or structure start chunks. The editor runs locally in a browser. Single-player is enough; a running multiplayer server is not required.

This feature needs a Jugcraft build containing World Designer. The original framework-only build does not include its world-generation codecs.

## Open the editor with your mods

1. Start any local world with the intended Jugcraft mod collection. Enable commands, or have operator permission on a test server.
2. Run `/jugcraft design export`.
3. Open the `index.html` path printed in chat. Its folder is under `config/jugcraft/world-designer/<timestamp>/` inside that Minecraft instance. Keep the files together.
4. The editor automatically loads the accompanying catalog: installed biome IDs, structure IDs and their permitted biomes, town size, and the current Overworld generation settings. It also writes `catalog.json`, which another copy of the editor can load with **Load mod catalog**.

The export command on a dedicated server writes to **that server's filesystem**, not to a player's computer. Copy the whole exported folder to the computer where you want to draw. Export again after changing Minecraft, the mod collection, or world-generation datapacks.

Developers can also open [the editor source](../tools/world-designer/index.html) directly. Without a catalog it works in planning mode: drawing and saving work, but playable-pack export requires a real catalog. It needs no Node installation, hosted service, account, or internet connection.

## Draw the world

- **Raise / Lower:** sculpt a soft circular patch. Increase strength for faster changes.
- **Level:** bring terrain towards the selected height. **Smooth** averages nearby height samples.
- **River:** lower a channel towards height 59 and paint the river biome. Sea level is 63; this is a terrain brush, not a simulation of flowing water or drainage.
- **Paint:** select a biome and brush it onto the land. **Clear paint** restores the seed's normal biome selection in that area. The whole design still uses its authored height field.
- **Spawn:** choose the starting X/Z location on dry ground. Minecraft finds its actual surface height. The world's normal spawn-radius rules still control a player's precise arrival position.
- **Walled city:** place the existing Jugcraft town, including its buildings, shops and NPC systems. Its footprint snaps to chunk boundaries and its builder levels the ground. There is at most one city, and `town.enabled=false` disables construction. A design without a city suppresses the usual automatic town.
- **Village / structure:** select an installed structure and place its starting chunk. Paint a suitable biome around it, using the requirements shown in the details panel. The marker snaps to the chunk center.

Click a landmark in the list to center the map on it. Remove a city or structure with its × button. **Place by coordinates** provides exact numeric placement. Scroll to zoom; use **Move map**, middle-drag or Space + drag to pan. Undo/redo retains 40 steps and also supports Ctrl/Cmd+Z and Ctrl/Cmd+Shift+Z. Use **Save design** regularly: there is no automatic persistent browser storage. **Open design** can reopen an unfinished design even if its placements need repair before export.

The initial canvas covers **2,048 × 2,048 blocks**. It has a 65 × 65 height grid sampled every 32 blocks; the game interpolates between samples. Heights are 40–256. The dotted border marks the fully authored core. Two outer grid cells blend terrain back into normal seed generation. The preview shows the planned surface, not the final vegetation, structures, caves or water simulation.

## Create a playable world

1. Clear the issues listed under **Before you export**. A missing biome or structure means the catalog and design do not agree. Re-export the catalog or change the painted content.
2. Save your editable design JSON, then click **Export world pack**. The downloaded ZIP is a datapack, not a mod or a `.mrpack`.
3. In Minecraft, select **Singleplayer → Create New World → More → Data Packs**. Add the ZIP, move it to the enabled side and apply the selection.
4. In the World tab, select **Jugcraft Designed** as the World Type. Keep **Generate Structures** enabled. Choose a seed and create the world. Accept Minecraft's custom-worldgen notice if you want to use this preset.
5. Run `/jugcraft design info` to confirm that the world uses the design. Check its spawn, terrain, city and structure sites before committing to a long-running save.

You can choose another seed with the same design. The painted height field and marker coordinates stay fixed; procedural surroundings, caves and native structure layouts can differ.

For a **new dedicated-server world**, place the ZIP in `<level-name>/datapacks/` before first start, select `level-type=jugcraft:designed`, and include `file/<your-pack>.zip` in `initial-enabled-packs` while preserving the server's other intended packs. Keep structures enabled. Everyone connecting needs the matching Jugcraft build and required mods. This does not change an existing world's preset.

## What this first version does and does not control

| Area | Current behavior |
| --- | --- |
| Terrain | Authored heights above Y32; original density below Y8, transitioning between 8 and 32. Surfaces and biome features run through Minecraft's normal generator afterward. |
| Caves | Deep procedural caves remain. Higher caves and overhangs within the authored region are not preserved or individually editable. |
| Biomes | Registered biome IDs can be painted above Y0. Below that, and outside painted cells, the seeded source remains. Biome borders use Minecraft's biome sampling, not a soft paint blend. |
| Mod content | Registered biomes/structures from the exported catalog are selectable. Their existing implementations and generation constraints still apply. No promise of compatibility with every third-party biome feature order or custom chunk generator. |
| City | One existing Jugcraft walled town. The editor does not assemble arbitrary cities or let you rotate or edit individual buildings. |
| Structure pins | Fixed start chunks, not exact building corners or rotations. Native biome, terrain and height checks can prevent generation. Large structures can extend beyond the approximate 96-block preview radius. |
| Other structures | Normal random structures remain enabled and may overlap planned content. Pins do not replace vanilla distribution rules. `/locate` is not a reliable way to find custom pinned placements; use the displayed coordinates. |
| Dimensions | This preset defines the standard Overworld, Nether and End. The authored map affects the Overworld. It does not inherit an arbitrary other mod's world preset or extra dimensions. |
| Block palette | The catalog records installed blocks for future tools. This version has no block-by-block building or schematic painting tool. |
| Editing existing worlds | Not supported. Generated chunks are not rewritten. Replacing a design pack partway through play can create mismatched terrain in newly generated chunks. |
| Larger maps | The saved schema allows 5–129 grid points per side and power-of-two spacing 4–128, but the UI starts with a fixed 65 × 65 grid. There is no map-resize UI or performance guarantee for the largest schema sizes. |

Keep the design JSON, the exact world pack, and the matching mod collection with the world backup. Keep the same pack enabled when reopening the world. Removing Jugcraft or its world-generation pack can make the save unloadable; there is no automatic vanilla conversion.

## For contributors and AI agents

Read the [feature record](features/world-designer.md), [framework catalog](FRAMEWORKS.md), and [testing guide](TESTING.md). Extend this implementation instead of adding a second world planner or silently moving towns after generation.

- `tools/world-designer/model.js` is the shared browser/Node data model, validation and pack compiler. Its versioned JSON is the authoring format. Keep imported file values out of executable HTML.
- `editor.js`, `index.html` and `style.css` are the offline UI. No remote script dependencies or startup downloads. The editor is bundled into the mod by `processResources` and explicitly exported by the operator command.
- `world/design/DesignGrid`, `DesignDensity`, `DesignBiomeSource` and `DesignPlacement` are per-world, serialized generation components. No global active-map singleton: two different worlds must retain separate designs.
- `WorldDesigner` handles registration, catalog export and the existing spawn/town hooks. Reuse `TownPlanner`, `TownState` and `TownBuilder` for town construction and persistence.
- New generation features should have a versioned field, editor preview, bounded validation, server codec, real-world generation test and save/reopen evidence. Update both the Java and JavaScript contracts together.
- Existing frameworks remain available for future in-game screens, animated previews or guides. This browser editor does not need GeckoLib, a spell framework, GuiLib, or a second UI runtime to draw a map. Keep client-only APIs out of the common generation classes.

Run the model tests with `node --test tools/world-designer/model.test.cjs` (Node 18+). A headless export is available as `node tools/world-designer/compile.cjs design.jugcraft.json catalog.json output.zip`; it refuses to overwrite an existing output. Run the Minecraft checks documented in the feature record as well—JavaScript tests alone do not prove world generation works.
