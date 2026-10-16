# KV-1/W walker

The owner's KV-1/W miniature (a KV-1 hull and turret on four jointed spider legs) as a Blockbench project, built by `tools/kv_walker.py` (`--bbmodel` regenerates it, `--preview out.png` renders views and a frame sheet). Model and clips only; no entity, item or recipe yet.

- `kv_walker.bbmodel`: bones `hull > turret > gun > flash, puff` and, under the hull, four legs each as `leg_<corner>` (yaw at the corner hinge) `> femur_<corner> > tibia_<corner> > foot_<corner>`, corners `front_left`, `front_right`, `rear_left`, `rear_right`. The hull is 40 x 18 x 40 pixels with its underside 7 pixels up; the feet stand about 40 pixels out from each corner; the barrel's underside is at y 31.4 and nothing on the legs reaches above y 29, so the turret traverses clear. `flash` and `puff` sit at the muzzle and are scaled to nothing except during `fire`.
- Clips: `idle` 3.0 s and `walk` 1.2 s (trot: diagonal pairs alternate), `turn_left` and `turn_right` 1.6 s (the feet sweep round the hull), `climb` 2.0 s (nose up, the front legs reaching high in turn): looping; `jump` 2.0 s (crouch, spring, tuck, heavy landing) and `fire` 1.2 s (recoil, kick, burst and smoke) play once. Keyed every tick relative to the rest pose, so the entity's own yaw and movement add to them.
- Textures `kv_*` (12, 16x16) drawn by the script in the project's flat clean manner.

Previews: `docs/images/kv_walker_preview.png` (five views) and `docs/images/kv_walker_frames.png` (four frames each of walk, turn_left, jump, climb, fire).
