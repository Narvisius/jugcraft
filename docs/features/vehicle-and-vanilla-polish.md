# Vehicle movement and vanilla-style art corrections

Owner request, 11 October 2026. Implementation: OpenAI Codex (GPT-6 family).

## Changes

- Diesel Walker, Armoured Walker, Landship, Self-Propelled Howitzer, Zeppelin and remote broomsticks use Minecraft's existing linear network interpolation. Minecraft 26.3 `Entity.commonTick()` saves the old pose and advances interpolation; entities must not advance it a second time. Removed the extra advancement from the three balloon classes.
- Walker strides and tank/howitzer tracks interpolate between render frames. Walkers step when turning and ease back to a standing pose at rest. Teleports do not create huge animation jumps. Raider Walker melee animation now uses the local receipt time rather than comparing unrelated server/client tick counts.
- All 15 custom log families, including cinnamon, reference vanilla bark, end-grain and stripped-log textures with species-specific colours. World and inventory models use the same generated tint table. Bark-all-sides wood blocks are included. The installed Minecraft/resource pack supplies the pixels; no Minecraft image is distributed. Planks and leaves retain their current art.
- Bronze, Steel, Gilded and Needle Rapiers, Bronze/Steel/Runebound Moonblades and Soulreaver use authored 16-pixel silhouettes and vanilla's thin item extrusion in hand. Compact hilts, clear blade edges and simpler colour areas replace fine geometry that became noisy or disappeared at gameplay scale. The Runebound Moonblade's previous smooth mesh is retired for this owner-requested style; other Runebound meshes remain.
- Bronze/Steel/Runebound Nodachis and Bogfang have their cutting edges turned over about the blade axis, keeping the hand on the grip and the tip pointing away. The Runebound Nodachi's mesh and box fallback both receive the correction.

No item/block/entity IDs, recipes, combat values, vehicle control protocols or saved data change.

## Art authority and regeneration

The owner's request to match vanilla wood and equipment supersedes the older requirement to distinguish every metal palette from vanilla and the previous ornate Moonblade direction. This is a scoped correction, not permission to repaint unrelated assets. The Gilded Rapier retains the existing pale steel and gold palette; its similarity to iron is intentional under this direction.

Sources: `tools/vanilla_logs.py`, `tools/vanilla_arms.py`, `tools/arms_icons/`, and the existing Arms geometry generators. The log generator runs after the other block generators so normal data generation preserves the references/tints. `WoodColors` reads the matching packaged tint table. Weapon maps regenerate through `arms.draw_all` and `arms_variants.draw_all` in the existing texture generator.

## Validation

`VehicleMotionClientGameTests` drives six real server entities along controlled trajectories while sampling their networked client positions and animation values. It checks forward movement, reversal, pivoting through the rotation seam, frame interpolation and idle settling. It isolates rendering/network behaviour; it is not a substitute for terrain/steering tests.

`VisualPolishClientGameTests` captures all 15 log families (bark, cut ends, stripped sides and ends) and the affected weapons in first/third person with both dominant hands. Those are visual evidence, not automatic aesthetic approval. `BigGunsClientGameTests` retains its balloon rise regression; `BroomstickClientGameTests` covers the locally piloted broom.

Local validation: all 1,379 required server tests passed, both new client classes passed, and the balloon-rise and piloted-broom regressions passed. Inspected final weapon screenshots in both dominant hands. Offline data/model/icon checks, the repository check, all 14 packaging/selection unit tests and all 18 dependency hash checks passed. A subsequent vanilla-model comparison corrected horizontal-log UVs; the final gallery passed and its sideways beams were inspected. The broader CI run caught an outdated Runebound assertion: three arms retain meshes, while the Moonblade now intentionally uses vanilla item extrusion. Its test still renders all four arms and checks that no remaining mesh falls back to boxes. Final GitHub validation is tracked in PR #321.

Human multiplayer and launcher import have not been performed for this build. Subjective owner acceptance remains a playtest check.

The final server rerun exposed a pre-existing isolation problem in `LairGameTests`: its no-grave probe was 20 blocks outside its fixture and could find a neighbouring test's headstone. The range probes now stay in a padded 16-block fixture and check both sides of the reach boundary. Five successful attempts are required; the full local 1,379-test server run passed with this correction. No Last Rites gameplay code changed.
