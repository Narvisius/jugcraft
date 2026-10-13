# Arcane Concordance: Reading the Signs (presentation of actual state)

Status: implemented on branch `claude/concordance-steps-26-32`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 27) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 19, every stage (Initiate to Architect). Roadmap step 27.
Primary specialty and supported player role: every player of the Concordance; no tradition of its own.

Builds on the features whose events it shows: [rituals](arcane-concordance-rituals.md), [alchemy](arcane-concordance-alchemy.md),
[workers](arcane-concordance-workers.md), [the Concord Spire](arcane-concordance-spire.md), the [shared effect
boundary](arcane-concordance-composition.md) and the [lantern and pylons](arcane-concordance-first-light.md). Contract and
checklist: [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## What already existed

Most presentation already followed synced server state (the inventory taken for this step): a pylon's crystal glowed
only while charged, the bench's lens only while a study ran, GeckoLib bodies played the clip for their synced status,
and the circle's motes ran only along channels the server had validated. The gaps were:

- Shortage and danger could not be told apart. Every ritual interruption played the same break, whether a pylon ran dry
  or the containment failed and lashed the participants.
- The crucible showed neither how full it was nor what it held, and a searing mixture (which a stir damages) looked
  exactly like a simmering one.
- A spire that was damaged, unsupplied or unattended looked the same as one never founded. Its upkeep, its phases and
  its growing field showed nothing.
- A porter waiting for goods, or held up on its path, looked idle or walked on the spot. A finished circle whose result
  waited looked like a circle at rest.
- The server's own particles (effect marks, a circle's completion, a caught wisp) ignored the player's settings. No
  setting limited how much a large installation drew.

## Player experience

**Signs.** When something happens, the Concordance shows it where it happened, and only when it happened:

| Kind | Looks and sounds | Shown when |
|---|---|---|
| Preparation | Enchanting glyphs drawn in to the place | A ritual begins to gather or channel |
| Execution | A few sparks of light rising | A crucible step is taken (by hand or by formula); a spire's field grows a crop |
| Execution, a transfer | Light travelling from the source to where it arrives | Each ritual step's Ley Charge, from each channel to the anchor; a spire's daily Ley Charge, from each pylon it was drawn from; Radiance poured from a lantern into a pylon; Focus a Star Spire gives, from the heart to the player |
| Success | A burst of light | A ritual completes; a bottle is filled; a spire's phase is finished or its field works again; a wisp is caught; a worker finishes |
| Shortage | Grey smoke and sinking ash; a hollow falling tone ("Something is lacking") | The channel that fell short and the anchor; a ritual stopped by a missing part, a participant gone, its conditions, an unloaded chunk or a lapse; a formula without its water, ingredient, container or room (once, as it starts waiting); a lantern too weak to pour; a spire's missed upkeep or lapsed attendance; a worker that comes to lack resources, access, a way, repair, energy, room or a loaded destination |
| Danger | Flame and sparks thrown outwards; a sharp rising warning ("Danger") | A ritual's containment failing or its offerings tampered with; a searing stir; a spire damaged |

A deliberate stop (a ritual cancelled, its anchor removed, the Concordance switched off, a ritual whose rules are gone)
plays only the circle's break, never a warning. Where the shared effect boundary applies an effect, its mark shows as
it always did (sparks for harm, green for mending, a puff for a push, light for illumination and detection, swirls for
a status, glyphs for a touch, smoke for an alteration). Harm is shown with the warnings' priority but without their
sound: being hurt has its own.

**Things as they are.**

- **The crucible.** Its liquid stands at the mixture's volume, one pixel a part of six. It is coloured by what a
  Sampling Spoon would find: plain water teal, then a colour for each strongest property (Radiance gold, Verdance green,
  Ember orange, Rime ice-blue, Tide deep blue, Hollow violet), muddied when the mixture is murky. Hot, it steams; searing,
  it smokes and spits at any intensity.
- **The Spire Heart.** Being raised, glyphs are drawn in. Working, light rises. Lapsed (unsupplied or unattended), ash
  sinks. Damaged, sparks and smoke fly, at any intensity.
- **A finished circle** glows softly until its result is taken.
- **Workers.** The Gathering Shade folds its arms and looks about, and the Clockwork Porter taps a foot and looks round,
  while they wait for goods, access, a way or room. A worker on its way or following but not moving stands still rather
  than walking on the spot.

**Visual intensity** (Concordance settings): **Full**, **Reduced** or **Minimal (warnings only)**. Reduced draws half
of each sign and a third of the blocks' sparkle. Minimal draws only the warnings, and only a little of them; warning
states (a searing crucible, a damaged spire) still show. **Reduced motion** keeps everything calmer, as before. The
codex's Foundations gains **Reading the Signs**.

**Nothing by colour alone.** Each kind differs in shape and movement. The two warnings have their own sounds with
subtitles. The crucible's colour repeats what a spoon, Jade and the journal say in words.

## How it works

**On the server** (`concordance/sign`).

- `Sign` is the vocabulary: six signs of five kinds, and the seven effect marks.
- `Signs.show` is called by a feature where, and only when, the event happened: a draw that succeeded, a stir that was
  applied, a status that changed.
- It sends a `SignPayload` (the sign, where, and for a transfer where from) to the players tracking that chunk within
  48 blocks.
- Each level sends at most 48 signs a tick, then up to 16 more warnings (`Signs.Budget`), and the same sign at the same
  block once a tick.
- The three places that sent particles from the server (effect marks, a circle's completion, a caught wisp) now send
  signs. `check_signs` keeps it that way: no `sendParticles` in the Concordance.

The events are wired as follows:

- `CircleAnchorBlockEntity`: preparation at the start, a flow from each channel at each draw, success at commit.
  `warning(Interruption)` gives shortage, danger or nothing. A stop for power, structure or containment also shows its
  warning at each part the circle's last check found at fault (the dry channel, the missing part, the lost boundary
  stone), and a draw that finds a channel short shows it there.
- `CrucibleBlockEntity`: work or danger per step, success per bottle, and a shortage once when its formula starts
  waiting.
- `LeyPylonBlock`: a flow from the lantern when Radiance is poured; a shortage when the lantern holds too little.
- `ConcordSpire` and `SpireHeartBlockEntity`: the upkeep's flows or its shortage, success at a phase, work per crop
  grown, flows of Focus, and a sign at each change of state.
- `WorkerEntity.setStatus`: a sign at a change into a lacking or finished status, at most one every 200 ticks per
  worker.

**What a client is told.** Only stable states and events, never animation frames:

- the crucible's update now carries `taste` and `murky` (the spoon's reading, which anyone may take) beside the heat and
  volume, and nothing of the mixture's makeup (`check_signs` checks the update's keys);
- the spire's status, a worker's status and a circle's phase are sent as before;
- a sign is one small packet per event.

**On the client.**

- `SignClient` draws each sign from its own settings and plays the warnings' sounds.
- `Presentation` holds the visual intensity, reduced motion and one particle budget a tick shared by every Concordance
  effect: 160 at full, 64 reduced, 16 minimal. Signs spend it first. A block's ambient sparkle may use half, so a large
  installation draws no more than the budget however many blocks it has. A warning always draws its floor of two.
- `CrucibleRenderer` (GeckoLib 5.5.7's renderer, pinned) hides the model's liquid bone in the pot's pass. It draws the
  bone again on its own, scaled to the volume and tinted. The liquid's texture is drawn pale for that
  (`tools/concordance_alchemy_art.py`).
- `CircleClient` shows a held result and spends the budget on its channel motes. It now leaves the gesture off players
  holding Jugcraft arms, as the Vigil's gesture does, so ArmsMotion keeps one owner of the arms.
- `WorkerEntity.shown` keeps a stalled traveller's body still.
- The Spire Heart's and the crucible's `animateTick` read the synced state.

### Rendering, light and limits

| Effect | Layer and transparency | Count and rate | Distance | Cleanup |
|---|---|---|---|---|
| Signs | Vanilla particles | A sign's count (4 to 24) scaled by intensity, halved beyond 24 blocks; at most 48 signs a level a tick sent, 16 more for warnings | Sent to players tracking the chunk within 48 blocks; not drawn beyond 48 | Particles expire as vanilla's do; nothing is kept |
| Block sparkles (pylon, sconce, bench, mote, crucible, spire heart) | Vanilla particles | 1 in N display ticks per block (N from 2 to 12, times 3 reduced, never at minimal except warning states); at most half the budget a tick | Vanilla's display-tick radius | None kept |
| Circle channel motes | Vanilla particles | One per linked channel every 4 ticks (12 with reduced motion), none at minimal, each from the budget | 48 blocks | None kept |
| Crucible liquid | GeckoLib's own render type for the crucible (entity cutout) with the model's texture; a second draw of one bone, tinted | Every frame, like the rest of the model | Vanilla's block-entity view distance | The pot's renderer; no texture is generated |
| Worker, spire and circle bodies | GeckoLib, as before | Animation clips chosen from synced status; no extra network traffic | As before | As before |

Real light, emissive surfaces and bloom stay apart:

- **Real light** is the server's: the lit sconce, the charged pylon, the working bench, the active spire, Kindled motes
  and a lit lantern's trail.
- **Emissive surfaces** are model elements that are bright but give no light: the pylon's crystal, the sconce's lens,
  the bench's lens.
- **LambDynamicLights**, when installed, lights only the hand of whoever holds a lantern, an Astrolabe or a Wardlight;
  it is unchanged by this step.
- **Bloom**: none. No effect needs a shader. Nothing has been tried with Iris and a shader pack, or with Sodium beyond
  CI's client runs.

Spell Engine's own cast and release presentation (gestures, sounds, the spells' particle bursts) is unchanged. Its
impacts come back through the shared effect boundary, whose marks are now signs.

## Connections

- Input producer: rituals, alchemy, pylons, the Concord Spire, workers, dreams and the shared effect boundary (their real
  events and synced states).
- Output consumer: the player's eyes and ears; nothing in the game reads a sign.
- Technology and magic connections: Ley Charge transfers and the crucible's automation (formulas) show what they do and
  what they lack.
- Reachable entry path: every sign appears with the feature it belongs to; the settings and the codex page are there
  from the start.
- Mastery: none.

## Balance

No balance effect. Signs are sent after the server has decided; nothing reads them back. The display settings are read
only by client code and blocks' `animateTick`, which `check_signs` checks. A game test casts Kindle and Dawn Aegis at
every intensity with reduced motion off and on, and gets the same light, shell and Focus.

## Multiplayer and persistence

- Server authority: the server decides every sign; a client only chooses how much of it to draw.
- Hidden information: a sign goes only to players who can see its place. The crucible's colour is the spoon's reading,
  which anyone near it can take; its makeup stays on the server.
- Rate: per level, at most 48 signs a tick plus 16 warnings; a worker's sign at most once every 200 ticks; a
  crucible's shortage once per wait.
- Persistence: nothing new is saved in worlds. The spire's status and a worker's status were already saved; a sign is
  never saved. The intensity is this computer's setting (`concordance.visual_intensity` in
  `config/jugcraft-client.properties`).
- Disable behaviour: with `concordance.enabled=false` features refuse their actions as before, so no sign follows.

## Dependencies and assets

No new dependency. GeckoLib (required) draws the crucible's liquid and the new clips. Cloth Config (required) holds the
new setting. Nothing new is optional: LambDynamicLights and Fusion keep their uses, and no shader is needed.

Assets, all original and generated:

- Two warning sounds, `sign_shortage` and `sign_danger`, synthesised by `tools/concordance_sounds.py` (no recorded
  audio). Only these two files were written; the others are untouched.
- The waiting clips of the Gathering Shade and the Clockwork Porter (`tools/concordance_worker_models.py`).
- The crucible's liquid redrawn pale in `tools/concordance_alchemy_art.py`, so it can be tinted. The pot is unchanged.

The owner library ([catalog](../../art/owner-library/catalog/README.md)) was searched before drawing these.

- **Sounds.** Its 275 sounds include `industrial_alarm.ogg`, `siren_1.ogg`, `hiss.ogg`, `sparks.ogg` and
  `enchanted_fire.ogg` (in its Guns, Big Cannons and tile collections), which were considered for the two warnings. They
  were not used: they are machine and battlefield sounds, and the Concordance's cues share one synthesised voice and key
  (`tools/concordance_sounds.py`), which a warning should keep so it reads as the magic's own.
- **Animations.** Its animation JSON is for guns and cannons, not for these bodies.
- **Liquids.** It has no liquid surface for a crucible.

## Verification

- Run locally before pushing:
  - `python3 tools/check_mod_data.py`: PASS (1597 material IDs, data files and recipe audit), on a copy of the tree;
  - regenerating the data on a second copy changes nothing;
  - `python3 scripts/check_repository.py`: PASS;
  - six faults injected into the Java on a third copy (a server `sendParticles`, a changed limit, the mixture's makeup
    sent to clients, a sign's kind changed, a server class reading a display setting, an intensity's budget changed)
    were all caught by `check_signs` and `check_baselines`;
  - `Sign` and the alchemy `Assay.Look` compile with `javac` against their pure dependencies. Nothing else of the step
    can compile without Minecraft; CI is its first compile.
- `python3 tools/check_mod_data.py`: the new `check_signs` confirms:
  - the signs, kinds, intensities and limits are `tools/concordance_signs.py`'s;
  - the warning sounds are registered, drawn and subtitled;
  - there is no `sendParticles` in the Concordance;
  - the display settings are read only by client code and `animateTick`;
  - the crucible's update carries only heat, volume and the spoon's reading;
  - the setting's words exist.
- Server game tests added:
  - `ConcordancePresentationGameTests` (four): the per-level budget and the warning reserve, warnings never hidden at
    any intensity or distance, ambient sparkle by setting and share, a sign crossing to the client unchanged.
  - `ConcordanceRitualGameTests` (three): a clean working shows exactly one preparation, twenty flows (one per draw
    from each channel) and one success, and no warning; a dry pylon shows a shortage at that pylon and the anchor; a
    lost boundary shows danger.
  - `ConcordanceAlchemyGameTests` (one): the crucible's update holds only the heat, volume and spoon's reading; each
    step shows as work, a searing stir as danger, a bottle as success.
  - `ConcordanceWorkerGameTests` (one): a porter shows its lack once, and a stalled traveller is shown still.
- Server game tests strengthened: three `ConcordanceSpireGameTests` (an upkeep's shortage and its flows from exactly the
  pylons drawn from, danger when damaged and success when repaired, a lapse of attendance) and
  `ConcordanceBaselineGameTests.presentationOptionsChangeNoOutcome` (every intensity).
- Client game test added: `ConcordancePresentationClientGameTests` takes three screenshots:
  - `jugcraft_concordance_crucibles`: four crucibles of different volume and contents, one searing;
  - `jugcraft_concordance_signs_full` and `jugcraft_concordance_signs_minimal`: the same signs drawn at each
    intensity.
- CI: Build run [37670918838](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/37670918838) on 8d583c6 passed
  every job.
  - `mod` and `optional integrations absent`: "All 1056 required tests passed". The first run, on 4976e8a, failed one
    test (a dry pylon's shortage was looked for only at the anchor); 8d583c6 shows a stopped ritual's warning at its
    faulty parts as well.
  - Client shard 1 ran `ConcordancePresentationClientGameTests` and its job passed. It saved
    `jugcraft_concordance_crucibles`, `jugcraft_concordance_signs_full` and `jugcraft_concordance_signs_minimal`.

Not yet run: a person looking at the screenshots and at the signs in play; the sounds heard in game; Iris with a shader
pack; a large installation profiled for frame time; a two-client server (whether another player sees the same signs).

## World and event applicability

Works everywhere; no seasonal content.

## Rollout and open questions

New identifiers:

- the payload `jugcraft:concordance_sign`;
- the sounds `jugcraft:concordance.sign_shortage` and `jugcraft:concordance.sign_danger`;
- the codex entry `foundations/signs`;
- the client setting `concordance.visual_intensity`;
- the clips `animation.gathering_shade.waiting` and `animation.clockwork_porter.waiting`.

The crucible's update gained `taste` and `murky` (sent, never saved). Nothing is saved in worlds.

Open items:

- The pylon shows whether it is charged, not how much it holds; Jade and the circle report give the figure.
- The relics, the garden's living devices, the observatory and the logistics posts keep their own presentation, which
  already follows their synced status. They send no signs of their own yet; the effects they apply show their marks.
- Spell Engine's cast effects are its own and are not scaled by the visual intensity.
