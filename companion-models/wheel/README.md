# Companion Generator Wheel

Open `Companion Generator Wheel.bbmodel` in Blockbench. The embedded atlas is also supplied as `wheel_atlas.png`. This is a generic mesh model; preserve the mesh format when editing.

Dimensions: 32 x 32 x 16 model units (2 blocks wide x 2 tall x 1 deep). Both sides are open, with no central axle or side frame. The 32 tread boards meet edge-to-edge; seams are texture details. Two wooden rims and narrow iron bindings rotate with the tread. The two side generator housings, support rollers, front/rear bearings, copper buses and under-wheel mechanisms remain stationary. The four front/rear generator pods have been removed.

In Animate, select `animation.companion_wheel.running` and press Play. The linear, seamless loop turns the wheel once every 2 seconds around Z at [16, 17.725, 8]. At rest use the default pose; a future renderer can scale playback speed with output. The supplied GIF is a preview of the same geometry and rotation.

`wheel_layout.json` records a 2x2x1 multiblock footprint with outputs only on the outward sides: west on the left bottom block and east on the right bottom block in the default orientation. There are no front, rear, top, bottom or internal outputs. Visible side contacts align with the outer block-face centers. Runtime block registration, collision, NPC mounting/running, cables, power transfer and animation playback still need integration. This delivery is the model and animation, not a working generator JAR.

Texture inspiration: Jugcraft main's `tools/steampunk_textures.py` and `tools/steampunk_models.py`, read 2026-10-05. Uses the warm copper and dark iron palette, restrained grain, panel edges and rivets; the wood is lighter for a readable wheel. Generated assets are original.

The checked-in Blockbench file is the editable source. From the repository root, run `python tools/export_companion_wheel.py` to update the runtime quads, or add `--check` to verify they agree. The earlier external workspace generator predates the geometry repairs below and should not overwrite this model.

On 9 October 2026, OpenAI Codex (GPT-6) repaired the base's overlapping faces uncovered by PR validation. The upper vents stop below the copper outputs; whole conduit/elbow and reinforcement boxes are separated using the shared `model_writer.separate_boxes` helper; the rubber roller cores are recessed beneath their existing metal collars. Atlas UVs, closed meshes, the rotor, animation pivot, side output contacts and 32 × 32 × 16 bounds are preserved. The shared owner asset catalog and art rules were reviewed; this repair uses the existing original wheel assets and no new textures. The runtime export is generated from the repaired Blockbench geometry. The earlier PNG/GIF previews above predate these small base adjustments.

Validation: the exporter consistency check and full `tools/check_mod_data.py` audit passed, including 4,537 block/item models, 321 quad parts and 154 models drawn together (`build/pr282-data-fixed.log`). A before/after render of the runtime geometry was inspected locally. This repair does not claim a new in-game visual playtest.


