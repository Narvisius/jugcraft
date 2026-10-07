# Arms motion

Status: merged in #156 (batch 43). Arms II (batch 45, [arms-ii.md](arms-ii.md)) gives its eight kinds guards, combos and strokes made the same way. Compiles and tests in CI only; **not yet played**. The first-person strokes were tuned against preview renders, not in the game.
Proposal issue: the owner, 3 October 2026, with an archive of combat animation mods and resource packs attached: "I want you to make NEW ANIMATIONS for all the weapons and LEARN how to make new animations that are very high quality while keeping it optimized. I want them to be super fluid and look good use these as an example of how to do it".
Owner: jimbozoomer-byte
Target milestone and tier: the arms of batch 42 ([arms.md](arms.md)); no tier of its own.
Primary specialty and supported player role: fighting (looks only).

## Player experience
Every kind of arm from batch 42 now moves as a weapon of its weight, seen by everyone around the player and in first person.

- **A guard for each kind.** The torso turns side-on, the head turns back to the front, and the feet set. The guard eases in over 5 ticks when an arm is taken in hand, and breathes slowly while held: the torso, arms and blade rise and settle, each player at their own phase.
  - Longsword: the blade up before the face.
  - Greatsword, war hammer, glaive and halberd: held across the body, with **both hands** on the grip or haft.
  - Rapier: en garde, point forward and the off hand raised behind.
  - Mace: held ready at the side.
- **A combo of attacks for each kind.** Each swing goes on to the kind's next attack, and the combo starts again after 1.5 s without a swing.

  | Arm | Attacks, in turn | Use |
  |---|---|---|
  | Longsword | forehand cut, backhand cut, thrust | parry: the blade across the body |
  | Greatsword | wide sweep, overhead cleave | |
  | Rapier | lunge, quick cut | parry |
  | Flanged Mace | overhead smash, side swing | |
  | War Hammer | overhead slam, side swing | |
  | Glaive | wide sweep, rising cut | |
  | Halberd | thrust, overhead chop | |
  | Spear | jab: the body lunges | the lean into a charge |
  | Lance | jab: the body lunges | couched and braced for the charge |

  - Every attack has anticipation (a short wind-up), the blow, follow-through and a settle back into the guard.
  - The whole body joins in: the torso turns up to about 60° and bends forward at the waist, the head counters to keep the eyes on the target, and the feet step through.
  - The blow lands at the moment of the click, so the animation reaches it a quarter to a third of the way in. The rest of the swing is follow-through. A two-handed kind's blow lands later, at the animation's blow ([arms-iii.md](arms-iii.md)), and its swing is longer.
  - The spear and lance keep vanilla's arm and thrust animations (26.3's own spear work) and add the body.
- **First person.** The held arm takes its own guard on screen and its own stroke for each attack, crossing the crosshair as the hit lands:
  - a cut sweeps from one side and out low to the other;
  - an overhead blow comes down from above the screen;
  - a sweep goes flat and wide;
  - a thrust drives point first at the crosshair.

  A parry uses vanilla's sword-blocking pose; the guard eases out under it.

  Through a stroke, the arm is held up against vanilla's cooldown dip (the drop of the held arm after a blow, which is largest for the slow arms); the dip comes back as the stroke settles, so the cooldown still shows.
- **Left-handed players** get everything mirrored.
- **Kept to vanilla's poses:**
  - swimming, crawling, gliding, sleeping and riptide spinning;
  - the off arm when the off hand holds something (a shield, a torch);
  - the legs when riding.

## What was learned from the archive
The archive held five mods and three resource packs. Each was studied for its approach only; none of their code, animation files or art is used, and nothing was copied.

| Source (license) | What it does | What Jugcraft took from it |
|---|---|---|
| Better Combat 2.3.0 and 3.2.2 for 26.3 (All Rights Reserved) | Per-weapon data: a hold pose per category, a combo of attacks (for example the claymore's horizontal slash, stab and slam), each with an "upswing" (the part of the swing before the hit), a hitbox shape and angle, and a two-handed flag. It plays the attacks through Player Animation Library and moves the hit to the end of the upswing, with networking. | A hold pose per kind, combos, and timing the strike against the hit. The hit is **not** moved: vanilla deals it on the click, so the strike comes early and there is no new networking. |
| Malfu Combat Animation 3.1 (All Rights Reserved) | 102 attack animations in the emote format for Better Combat: 1 to 20 ticks, 1 to 14 key ticks each, quadratic in, out and in-out easing per key, over the head, torso, arms and legs. | Animations need few keys. Whole-body motion (torso twist, steps) is what sells a blow. Ease-in-out on every key makes each key a stop, which reads as stiff, so Jugcraft joins keys with splines instead (below). |
| Player Animation Library: 2.0.5 for 1.21.4, and the separate 1.2.6 for 26.1 (both MIT) | Layers keyframed animations over the vanilla player model. It bends the torso, mirrors for the left hand, and has a first-person mode. | A layer over vanilla's pose instead of a replacement, so walking and crouching still show; a mirrored left hand; first person as its own pass. Jugcraft writes its own small player rather than taking the library as a dependency (see Dependencies). |
| Fresh Animations 1.10.5, its extensions and Fresh Moves 3.1.1 (resource packs for Entity Model Features; their own terms) | Procedural animation from expressions of time: idle breathing and sways, varied per entity. | A slow procedural breath under the guard, at a phase that differs per player, so a crowd does not move in lockstep. |
| Mo' Bends 1.2.2 for Minecraft 1.12.2 | The early whole-body animation mod: torso bending and sword combos. | The same lesson as Malfu. It is a 1.12.2 jar, so nothing of it applies to 26.3 code. |
| NdRz's weapons 0.1.0 for NeoForge 26.1.2 (Apache-2.0) | Weapons with custom item renderers. | That batch 42's approach (plain item models, no custom renderers) is the cheaper one, so nothing was taken. |

## How the motion is made
- **Authoring** (`tools/arms_moves.py`, on `tools/arms_motion.py`):
  - Each kind has a hold pose, attacks as a few keys (time, pose, tension), a use pose, and a first-person track per attack.
  - A pose gives each of seven bones (head, body, both arms, both legs, the item in the hand) a turn in degrees and an offset in pixels.
  - `tools/generate_material_data.py` writes them to `assets/jugcraft/arms_motion/<kind>.json`.
- **Previews** (`tools/arms_motion_preview.py`, a development aid that ships nothing): it draws the posed model as boxes, from the front and the side, through each attack, and the first-person view with the arm's own sprite, placed as 26.3 places the hand. The keys were tuned against these renders.
- **Splines.** Keys are joined by cubic Hermite splines with Catmull-Rom tangents, so speed carries through a key instead of stopping at it. A key's tension (0 to 1) flattens its tangent where a crisp stop is wanted: the moment a blow lands, a thrust's full extension. Python and Java evaluate the same formula.
- **The body.** The torso turns about the waist, not the neck, so the shoulders swing round and the neck moves. The arms ride on the torso, their turns composed with it. The head is composed with the torso, then counter-turned. The legs add a step to vanilla's walk.
- **Two hands.** For a two-handed kind, the off arm is pointed every frame at a point on the weapon below the main hand (a two-bone aim), so the off hand stays on the grip through every swing.
- **The clock** is vanilla's own swing progress with the partial tick. It is smooth at any frame rate and needs no networking, since every client already sees every player's swings. Each new swing advances the combo.

## How it stays fast
- No per-tick or server code, and no networking: it all runs as a player is drawn.
- No allocation per frame:
  - the clips are parsed once at client start into flat float arrays;
  - each player holding an arm has one track and one pose (in a weak map, dropped with the player), reused every frame;
  - the maths uses fixed scratch arrays and one quaternion.
- The work per frame is small: one spline evaluation over 42 channels per player holding an arm, plus a few 3×3 matrix products.
- Players beyond 32 blocks get the arms and the item only: no torso, head or legs.
- Only players holding one of the arms are touched. Every other entity and item costs one map lookup at most.

## Mixins
Four client mixins, in `jugcraft.client.mixins.json`. Fabric API's rendering module offers layer registration, cape and outline events, but we found no hook after a humanoid model is posed, around the held item's transform in the hand, or in first-person hand drawing.

| Mixin | Target | Why |
|---|---|---|
| `ArmsRenderStateMixin` | the end of `ArmedEntityRenderState.extractArmedEntityRenderState` | The one call with the entity, its render state and the partial tick together. The pose is kept on the state with Fabric's render state data. |
| `ArmsHumanoidModelMixin` | the end of `HumanoidModel.setupAnim` | Lays the pose over vanilla's. Armor is posed from the same state, so it follows. |
| `ArmsItemInHandLayerMixin` | `ItemInHandLayer.submitArmWithItem`: before the item is drawn, and around `SpearAnimations.thirdPersonAttackItem` | Turns the weapon in the hand (the wrist). Skips vanilla's thrust of the item for kinds whose arms this poses, so the two do not add up. |
| `ArmsFirstPersonMixin` | `FirstPersonHandsAndItemsRenderer`: `submitArmWithItem` (start and end), the end of `applyItemArmTransform`, and around `swingArm` and `SpearAnimations.firstPersonAttack` | The guard and strokes on screen, in place of vanilla's swing for the main hand when it holds one of the arms (except the spear and lance). |

The flail's swinging head ([arms-restyle.md](arms-restyle.md#the-flails-head-swings-5-october-2026), `client/arms/FlailHeads.java`) rides on the same four targets, with no new one: `ArmsRenderStateMixin` also resolves its chain and ball parts for the frame; `ArmsHumanoidModelMixin`, after the arms pose, keeps the body as finally posed (head, torso, arms and legs), which the ball keeps clear of; `ArmsItemInHandLayerMixin` keeps the model's root at the start of `submitArmWithItem` (a new inject at its head) and draws the head where the item is drawn, after the wrist; `ArmsFirstPersonMixin` keeps the view's frame at the start, the item's after `applyItemArmTransform` and after `swingArm`, and draws the head on screen at the end. The flail's first-person guard is held higher (`FL_FP`), so its ball hangs in sight.

## Connections
- Input producer: the arms of batch 42 (`weapons/ArmItem.kind()`).
- Output consumer: the player's view. No item, recipe, unlock or progression changes.
- Failure behavior:
  - A missing or unreadable motion file is logged, and that kind keeps vanilla's animation.
  - A mixin target missing in a future version stops the game at start (`defaultRequire` 1), so it cannot fail silently.

## Balance and automation
None. The animations change nothing in play: damage, reach, swing time and the moment of the hit are vanilla's and batch 42's. A combo is looks only; it does not change what the next blow does.

## Multiplayer and persistence
- Client only, with no new packets. Each client animates the players it sees from the swings vanilla already sends.
- Each client counts combos itself, so if it misses a swing (out of range), two clients may show different attacks of the combo for a moment. This is cosmetic.
- Nothing is saved.

## Dependencies and assets
- No new dependencies. Player Animation Library (MIT) would have been a new required dependency for every player; under the project rules that needs maintainer approval. It also plays a general emote format, which is more than nine kinds of arms need.
- The motion data and previews are generated by Jugcraft's own tools. The key poses are original.

## Verification
- `python3 tools/check_mod_data.py`:
  - checks the motion files against the tools (kinds, bones, 42-number poses, keys rising from 0 to 1, tensions from 0 to 1, first-person tracks);
  - checks that `ArmsMotion.java` reads the same kinds and bones;
  - checks that the four mixins are registered.
- `ArmsMotionClientGameTests` (CI job `client`):
  - checks that every kind's motion loads (twenty-one, with Arms II and III);
  - screenshots the player from the front, holding each kind's guard and two ticks into a swing;
  - screenshots three longsword blows in a row (the combo);
  - screenshots guards and strokes in first person;
  - logs each swing's progress, combo and pose.
- **Not done:**
  - no play: how the motion looks and feels in motion is untested;
  - no left-handed or mounted check;
  - no second client watching;
  - no frame-time measurement.

## World and event applicability
None.

## Rollout and open questions
- Tuning after the owner plays it: key timing, how far the torso turns, the first-person strokes.
- Mobs keep vanilla's arms: zombies, skeletons and piglins pose their own arms after the shared humanoid pose. Animating them would need one mixin per mob model.
- A future batch could add hit-stop or trails; neither is here.
