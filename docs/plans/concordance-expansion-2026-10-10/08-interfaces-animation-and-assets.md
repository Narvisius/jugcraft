# Interfaces, animation, textures and owner assets

**Goal:** make the existing systems understandable and enjoyable to use. The installed frameworks are tools, not automatically completed features. [The audit](01-current-state-and-integration-map.md) lists the approved versions and optionality.

## 1. One encyclopedia, several useful workspaces

The owner requested an inventory button and configurable key, without needing a book item. Both entry points should open the same encyclopedia. Technology and magic share navigation, search, pins and path explanations; their authoritative progression remains in the existing systems.

Keep the current J journal and Modonomicon registrations working while the new entry point is built. Decide keyboard conflicts through existing configuration. Do not silently remove an existing guide or create a second progression database.

Proposed top-level pages:
- **Discover:** current clues and meaningful next actions.
- **Paths:** technology/magic dependencies with required, optional and trade-supported edges.
- **Workshop:** recipes, known formulas, ritual patterns and station diagnostics.
- **Spellcraft:** instruments, composition and loadouts.
- **Pacts:** agreements, obligations and worker jobs.
- **Expeditions:** discovered lairs, preparation and boss studies.
- **Projects:** Conclave/civic work and wonders.

Build only tabs with useful content in the current chunk. A polished empty menu is not a completed system.

## 2. A concrete visual spell composer

### Screen layout

Left: learned parts, filtered by delivery/selection/operation/modifier. Center: readable ordered spell strip and optional branch view. Right: instrument capacity, Focus estimate, target/work bounds, range, cooldown and clear validation messages. Bottom: Preview, Inscribe, Undo and return.

At small GUI scales, use stacked panels and scroll instead of shrinking text beyond readability. A keyboard user can select a slot, choose a part, move it, remove it and confirm without drag-and-drop. A text view remains available for experienced players and diagnostics.

### Interaction sequence

1. Open with the currently held eligible instrument and server-provided learned catalogue.
2. Build locally using a model that matches the server compiler.
3. Preview locally for responsiveness, clearly treated as provisional.
4. Send a bounded expression and instrument reference for authoritative preview.
5. Display the server result with rule revision and rejection reasons.
6. Inscribe by sending intent and expected revision, not calculated costs or NBT.
7. Server rechecks item, ownership, knowledge and current rules, then changes the instrument once.
8. Close/reopen and confirm the composition persists and casts correctly.

Client predictions never grant a spell, spend resources or award practice. A stale response must not overwrite a newer edit.

### Useful validation language

Prefer “This wand supports one branch; the spell has two” to “Invalid graph.” Prefer “Mend needs an allied living target” to a red icon with no text. Indicate which part causes the failure. Do not reveal unknown parts merely because they exist in the global server catalogue.

## 3. Five additional workspaces

### Ritual planner

Show an isometric preview and a top-down accessibility view of the current pattern. List missing blocks and exact relative positions, blocked spaces, channel storage, reserved ingredients and participants. Selecting a missing part highlights it in the world when nearby.

Start is an explicit server request. The player must be in range and authorized. A plan view can be opened remotely as documentation, but remote viewing does not permit executing a ritual.

### Alchemy notebook

Show measured components, preparation, heat timeline, contamination, vessel capacity and expected containers. Separate known exact outcomes from unknown experiments. Pin a formula and expose its actual ingredient variants.

An automated replay panel displays running, blocked, cooling and complete. Do not allow client sliders to assign arbitrary temperature or free power. Requests choose a permitted schedule or recipe.

### Observatory chart

Display current occurrence, next forecast, claim eligibility, stored attunements and a practical alternative when a sky event is unavailable. Show time in understandable game units. Do not imply changing the clock grants another reward.

### Pact and worker roster

Each row: portrait, spirit/worker, owner, service, location status, current task, next obligation and reason for waiting. An agreement page explains terms before acceptance and offers suspend/end.

Show blocked cargo clearly. Never put a second editable inventory on top of authoritative courier cargo. A recall button cannot teleport items out of unloaded or inaccessible containers.

### Expedition dossier

Before discovery, show only clues allowed by the server. After study, show invitation, preparation, arena sketch and readable mechanic notes. After victory, show rewards and elective mastery opportunities.

During a fight, a small optional objective panel displays ward/column/mirror state. Reward qualification can say “participating” and list useful actions, without encouraging damage-meter competition. Spoiler settings hide attack details until observed unless the player chooses otherwise.

## 4. Framework responsibilities

| Framework | What to implement with it | What remains Jugcraft's responsibility |
|---|---|---|
| GuiLib | Rich layout, controls, transitions and reusable client widgets | Models, validation, server protocol, keyboard/accessibility |
| Native screens | Functional fallback for optional UI absence | Same authoritative requests and features |
| Modonomicon | Durable explanations, branching reference and recipe pages | Real knowledge, milestones and reward state |
| JEI | Ingredients, uses, catalysts and process views | Correct server recipes, visibility policy, refresh behavior |
| Jade | Short state at the looked-at station/entity | Safe server snapshots and access control |
| Cloth Config / Mod Menu | User display/input preferences | Server limits and gameplay authority |
| GeckoLib | Authored entity/item/armor/block animation | Server phase timing and physical collision |
| Player Animation Library | Third-person gestures and suitable player poses | Pose ownership and synchronization |
| Fusion | Connected/continuous/scrolling surfaces | Usable ordinary textures and block models |
| LambDynamicLights | Visual held/moving illumination | Actual gameplay light or discovery conditions |
| Iris/Sodium | Supported renderer/shader environment | Readability and fallback without custom shaders |

The current JEI integration's generated view does not automatically reflect arbitrary server datapack changes. A new recipe UI must either fix that through the supported API or clearly label the limitation. Do not use JEI's presence as proof live recipes are synchronized.

## 5. Proposed UI protocol contract

Reuse Fabric networking and existing payload conventions. Introduce only the messages needed for the active screen.

A request contains bounded IDs/options, a request sequence, relevant object identity and expected rule revision. It does not contain trusted resource totals, output stacks, a whole arbitrary research graph or executable commands.

The server:
1. Checks player state, object existence, loaded chunk, dimension, range and permission.
2. Resolves current rules and knowledge.
3. Bounds collection sizes, text lengths and request frequency.
4. Returns a small snapshot or rejection reason.
5. Applies a requested mutation only through the authoritative system.
6. Sends a revision change when the previous preview becomes invalid.

The current composition command uses a 20-tick rate gate: one server operation per second. Retain that limit initially and share it between commands and the new screen. Local feedback remains immediate while authoritative preview is debounced. Raise the rate only after measured justification. Final commit always revalidates; it does not rely on a preceding preview.

Client-only classes cannot appear in common saved-state types or initialization. Kotlin remains in the optional client path unless its common runtime is deliberately made a declared dependency. Test the absence path.

## 6. Animation ownership

Jugcraft already has ArmsMotion for legacy weapons and GeckoLib-controlled first-person gun arms. New spell gestures must explicitly decide which system owns:
- First-person hands.
- Third-person torso and arms.
- Off-hand object.
- Riding, swimming, sneaking and handedness.
- Interruptions by weapon use, damage, death and screen opening.
- Remote-player replay.

A cast does not get to override a gun reload because a library was installed later. Use a small action-priority policy: an active server-recognized weapon action owns its required bones; compatible casting gestures use free channels; incompatible gestures are refused or use a restrained fallback.

For a boss, define idle, awareness, wake, tell, release, recovery, transition, stagger, defeat and reset. Separate cosmetic idle layers from the attack controller. Animation events can play presentation, but hit timing and state changes remain server-controlled.

A late-joining client receives phase and elapsed state, not a replay of every past particle. Restart does not replay a defeat reward because an animation completes again.

## 7. Visual language that carries gameplay

Give each Principle a shape as well as color:
- Radiance: rays and open circles.
- Ember: rising chevrons and hot seams.
- Rime: facets and still horizontal bands.
- Tempest: broken arcs and directional marks.
- Strata: nested planes and load-bearing corners.
- Verdance: branching veins and buds.
- Tide: contours and flowing ribbons.
- Tether: knots and linked rings.
- Echo: offset repeats and fading inscriptions.
- Hollow: gaps and inward-falling fragments.

Use these sparingly in effects, UI and equipment. Do not recolor approved owner assets unnecessarily; apply the vocabulary to new effect layers and signage.

Every major attack should have at least two channels: posture plus ground mark, or sound plus icon, for example. High contrast and reduced motion modes retain the dangerous footprint and timing. Do not hide the only warning in bloom, transparent fog or a shader-specific effect.

### Proposed presentation budgets

Start with capped effect events per actor and distance-aware particle reduction. A major telegraph should prioritize its outline over decorative sparks. Sound events need concurrency limits so sixteen players do not produce sixteen overlapping full-volume ritual loops.

Record particle count, draw cost and frame-time impact under an actual fight. Fixed “looks fine on my machine” claims are insufficient. The precise budget belongs in the feature brief and can vary by hardware target.

## 8. Owner asset workflow

The approved magic asset library contains thousands of models/textures and a smaller set of audio, scene and animation sources. It is already authorized for direct use. Do not ask the owner to approve each file again, or redraw good material solely to make it different.

For a feature:
1. Search `art/owner-library/MAGIC_ASSETS.md` and its catalog for a matching design.
2. Inspect the actual model, texture, animation and companion files together.
3. Preserve original source files.
4. Record provenance/source hash and intended runtime mapping.
5. Adapt namespace, schema, dimensions and animation format only as necessary.
6. Put generated runtime output under its established generator's ownership.
7. Verify inventory icon, in-world scale, equipment fit, animation and remote rendering.
8. Capture real-client evidence under supported presentation configurations.

Do not execute arbitrary programs found in the asset archive. Do not copy a complete unrelated asset tree into resources to make one item work. Preserve applicable notices; owner authorization does not make upstream dependency licenses disappear.

## 9. Evidence required for a UI/presentation chunk

A useful review bundle contains:
- One screenshot of each complete workspace at ordinary scale.
- One small-window/high-scale screenshot.
- Keyboard-only walkthrough result.
- Optional GuiLib/JEI/Jade absence result where relevant.
- A short clip or timed screenshot sequence for important tells.
- Both first- and third-person equipment views.
- A remote player and dedicated-server synchronization result when possible.
- Known gaps stated plainly.

Screenshots of a browser mockup are design evidence, not Minecraft implementation evidence. Automated client showrooms are rendering evidence; they are not a two-person combat playtest.

[Next: Claude implementation chunks](09-claude-delivery-chunks.md)
