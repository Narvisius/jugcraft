# Companion delivery and task animations

Owner-requested on 12 October 2026. Implemented with **OpenAI Codex (GPT-6)**.

## Player behavior

- Supply/Output and general porter trips show one representative item from the actual carried stack, with two-handed carrying and a short pickup/placement reach. Bulk cargo, custom item components and ordinary walking remain intact. Existing Hearth Oven pie/cake poses retain priority.
- Cider Press helpers sit on top of one end of the moving capstan, with their legs dangling and hands resting beside them. The entire visible rider follows the block's interpolated quarter-turn angle. Both grinding and pressing advance the handle, using the existing eight-tick work cadence.
- Ground planting reaches down with the actual seed, then pats the soil. Hand harvesting reaches and pulls; mushroom colonies show the equipped cutter. Existing hoe strikes, overhead orchard and hanging-herb poses are retained.
- Water collection holds the actual empty bucket, bottle or canteen in the right hand, bends the whole torso forward to scoop, then lifts it upright. The free arm balances behind him; both arms retain their normal length throughout the motion. A successful water-bucket delivery to a Sprinkler tilts the bucket to pour.
- Skillet and Kitchen Stove deliveries reach toward their nearest surface, then briefly use a wooden-handled metal spatula. Collection and carrying show the real output item. Cooking remains autonomous: no resident helper, added delay or speed bonus.

Transfer, sprinkler and planting/picking motions aim at the nearest block surface and limit both arms to three model pixels of reach (normal length: 2.7). The item and both grips move together, so distant or elevated targets cannot produce long elastic arms. A target outside that reach receives a short nearby gesture rather than an exaggerated stretch.

All poses support Peepo, Jughead and their pumpkin costume. Eating, resting, wheel running, settings editing and death suppress transfer poses. Work poses retain priority. These are runtime animations; editable Blockbench exports are unchanged.

## Implementation and server cost

`CompanionGesture` is a short visual overlay, separate from work scheduling. Successful committed transfers publish an action, timestamp, target and a one-item display copy. The copy never owns or consumes cargo. Carrying refreshes a server-side five-tick lease; unchanged item/gesture data is not resent. Short reaches expire after twelve ticks and can finish while walking. No extra trips, wait states, scans, particles or prop entities are created.

`CompanionActionPose` evaluates motion client-side and shares the existing arm solver and prop layer. Water collection uses `WaterScoopPose` with fixed-length shoulder/hand transforms instead of stretching arms toward the source center. Visible targets are read only from loaded chunks. New work actions are appended to preserve existing ordinal IDs. The work-item snapshot comparison ignores stack count to avoid repeated updates from multi-item seed/container stacks.

The press saves a bounded visual turn and its start time in its existing block update/save payload. Existing transfer, apple, pulp, juice, pomace, energy and cooldown logic remain unchanged. Client and server should use the same updated build.

The seated press rider is a client render offset, cleared every extraction when work ends or is interrupted. The server keeps its existing checked approach position and job lease; no moving passenger entity, per-frame network update or new server scan is introduced.

## Assets and validation

The shared owner-library README and complete catalog were inspected before authoring. These procedural clips extend the existing Peepo rig. The small spatula uses the existing original cider-press wood and iron textures; no library files, new raster textures or dependencies are added.

Focused validation uses `PeepoActionClientTests` for shared grips, real planting/water work, synchronized props and visual checkpoints, and `PeepoDeliveryClientTests` for physical bulk deliveries, rollback and interrupted cargo. Full unrelated gameplay suites are outside this change's validation scope.

On 12 October 2026, both selected suites passed: **741 action assertions** and **60 delivery assertions**. The action suite also checks press access in all four horizontal orientations, stroke costs, handle interpolation and cooldown protection. The final visual-check run passed the same 741 assertions with the corrected camera and representative farm/water fixtures. Carrying, stovetop work, planting, picking, water filling and both companions at the press were inspected in captured client images. Compilation and assembly succeeded; repository/documentation-link checks passed. This does not constitute a large multiplayer load test or owner visual approval.

Evidence: `build/companion-action-tests.log`, `build/companion-action-visuals.log`, and the preserved `build/companion-action-evidence/` screenshots. Early test-only failures were corrected to select garden work before claiming it and advance work on real server ticks, respecting the existing per-tick reserve extraction limit.

The one-handed water-scoop revision passed **861 focused action assertions**, including fixed arm scale on both appearances/costumes, container-to-hand attachment, upright endpoints and unchanged water collection timing/cost. In-game images of Peepo holding, dipping and lifting a bucket and Jughead dipping a bottle were inspected. Assembly and repository checks passed; evidence is in `build/companion-water-scoop.log` and `build/companion-water-scoop-evidence/`. No unrelated gameplay suite was run for this revision.

The seated press and short-reach revision passed **2,411 focused action assertions** on 12 October 2026. Coverage includes bounded arm length with attached prop grips, both appearances/costumes, seated legs, the seat radius/height through a complete turn in all four press facings, live render offsets, and clearing the press overlay outside work. Captured client images of both press riders, the stove and sprinkler were inspected. Assembly and repository checks passed. Evidence: `build/companion-short-reach.log` and `build/companion-short-reach-evidence/`. Existing water and delivery assertions remain in the focused action suite; unrelated gameplay tests were not run.
