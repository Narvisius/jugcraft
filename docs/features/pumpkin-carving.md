# Pumpkin carving: the Carving Knife and hand-carved pumpkins

Status: implemented in source. **First played by the owner on 1 October 2026:** the screen worked, but clicking the grid did nothing. That is fixed (see Verification); the fixed version has not been played yet. The Build workflow compiles it, and CI's game tests pass (details below).
Proposal issue: none; requested directly by the owner on 1 October 2026 ("start with the carving knife, 16 per block is fine"), the first of the Halloween and harvest additions discussed with them.
Owner: @jimbozoomer-byte
Target milestone and tier: Milestone 3 (first homestead); Discovery tier (an iron ingot and a stick).
Primary specialty and supported player role: decoration and building; supports farmers (pumpkin seeds, a roasted snack) and anyone decorating for Halloween

## Player experience
Carve any face you like into a pumpkin, pixel by pixel:

- **The Carving Knife** (an iron ingot over a stick). Use it on the side of a pumpkin to open the carving screen for that side.
- **The carving screen** shows the side as a 16x16 grid, one cell per texel of the block, so carvings match Minecraft's pixel size. Left-drag carves with the chosen tool:
  - **Cut** goes right through (a hole the candle shines out of);
  - **Shave** peels the skin, so light glows through it more softly;
  - **Erase** undoes this session's strokes (right-drag erases too).
  Brush sizes 1-3, **Mirror** (copies every stroke to the other half), four **starter faces** (Classic, Cat, Ghost, Spooky), **Undo** (Ctrl+Z), **Reset**, a **Candle** preview and an actual-size preview. **Done** carves it.
- **A knife can't put skin back.** What was carved before the screen opened is fixed; you can only carve deeper. Within one session, Erase and Undo take back your own strokes.
- **Every side** of the pumpkin can carry its own face. The first cut opens the pumpkin: its 4 seeds fall out (vanilla's carving loot, as with shears) and it becomes a **Hand-Carved Pumpkin** facing the side you carved.
- **Light it:** use a torch on it. It glows more the more is carved out: 4, plus 1 for every 3 holes and every 12 shaved pixels, up to 15 (a face about the size of vanilla's jack-o'-lantern gives 15). Use it with an empty hand to take the torch back out.
- **Keep it:** broken, it drops as an item with its design and its torch (the icon shows whether it is lit), and placed again it faces you with the same carving.
- **Roasted Pumpkin Seeds:** roast pumpkin seeds in a furnace, smoker or campfire for a small snack.

Nothing is seasonal: the knife and carved pumpkins work all year and stay in the world.

## Connections
- Existing input producer: vanilla pumpkins (wild patches and pumpkin farms), iron and sticks, torches.
- Existing output consumer: decoration and light for builders; pumpkin seeds for farmers; roasted seeds (food, compost); the `c:foods` tag.
- Technology connection: none needed. A Jugcraft lamp or electric candle could light pumpkins later.
- Magic connection: none yet.
- Reachable entry path: an iron ingot, a stick and a pumpkin. Nothing else.
- Required vs optional: entirely optional; nothing in progression needs a carved pumpkin.
- How this stays useful without other branches: decoration and light on its own.

## Balance and automation
- The knife lasts 238 carvings (shears' durability); one finished carving (one Done) uses one.
- Carving a pumpkin gives the same 4 seeds as carving it with shears, once (the first cut). Hand-carved pumpkins are never turned back into plain ones, so there is no seed loop.
- Light: at most 15, like a jack-o'-lantern, and only with a torch inside (the torch is kept, not used up while lit).
- Roasted pumpkin seeds restore 2 hunger (like roasted sunflower seeds); roasting only turns seeds into food.
- No automation: carving is by hand, one side at a time.

## Multiplayer and persistence
- **Server authority.** The client only draws the screen. The finished face comes back as a fixed-size message (16 ints, so no length to forge), and the server checks every one before anything changes:
  - the player opened a carving session for exactly that block and side, in that dimension, within the last 5 minutes (one session per use of the knife, one carve per session);
  - they hold a Carving Knife in either hand, are within block reach, may use items there (`mayUseItemAt`: adventure mode can't) and may interact with the block (`mayInteract`: spawn protection);
  - the block is still a pumpkin and the side is not the top or bottom;
  - every pixel is skin, shaved or cut, and no pixel is shallower than before;
  - with `carving.free_draw=false` in `config/jugcraft.properties`, the face is one of the starter faces.
  Refusals send the player a short message and change nothing.
- **Moderation.** Free drawing means players can carve anything, as with signs and maps. Each carved pumpkin records who carved it last (UUID and name) in the saved world for server operators (`/data get block`); it is not sent to clients. Servers that want no free drawing set `carving.free_draw=false`, which allows only the starter faces to be carved. It does not reach creative mode or commands, which can give any item with any design, as they can with other block data.
- **Saved state.** The design (four sides, 2 bits a pixel, 256 bytes) and the carver live in the block entity; on the item, the design is the `jugcraft:carving` data component and the torch is the block state component (`lit`). Clients receive the design only.
- **Bounded work.** No ticking. A carve checks 256 pixels once. The client keeps one 64x16 texture per distinct design (and lit or not), at most 256 at a time, the least recently drawn released first, and all of them when leaving a world.
- New IDs only: `carving_knife`, `hand_carved_pumpkin`, `roasted_pumpkin_seeds`. Vanilla pumpkins, carved pumpkins and jack-o'-lanterns are unchanged. The `agriculture` switch turns off the knife's and the seeds' recipes; existing carved pumpkins stay.

## Dependencies and assets
No new dependencies. Uses Fabric API's networking (two play payloads), block entity and creative tab APIs, which are already required. The hand-carved pumpkin uses vanilla's pumpkin model and textures by reference (so resource packs apply); the carving colours, the knife, the item icons and the starter faces are Jugcraft's own (`tools/carving_textures.py`, `CarvingTemplates.java`).

## Verification
Actual results (1 October 2026, Minecraft 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Temurin JDK 25.0.4, GitHub Actions):

| Check | Result |
| --- | --- |
| `python3 scripts/check_repository.py` | Pass |
| `python3 tools/check_mod_data.py` (now also checks the carving numbers against Java (face size, glow formula, session length, knife durability), that every starter face is 16 rows of 16 pixels and named, that every refusal has a message, and the models of select item definitions) | Pass, 304 IDs |
| `./gradlew build`, compile, with the input fix, the Festival Crops, Kitchen Garden, Fall Harvest and `main` after #47 merged in (`1864fc9`) | Pass |
| Game tests on the headless server, same commit: 114 in total, 10 of them new here (all 107 passed at `734bc6e`, before #47's tests) | **All 114 pass** |
| Client game test (real client, Mesa software rendering, CI job `client`), including carving with the mouse and keyboard | **Passes** on `1864fc9`. Before the input fix it passed on `734bc6e` and `dce9952`, which only pressed buttons; the screenshots in the branch document are from the `734bc6e` run |

The 10 new game tests (`CarvingGameTests`):
1. the first carve turns a plain pumpkin into a hand-carved one facing the carved side, keeps the face, records the carver, sets the glow from the design, gives no light without a torch, costs the knife one use and lets out 4 seeds;
2. the knife does nothing on the top of a pumpkin, opens a session on its side, and one session allows one carve;
3. filling a hole back in is refused, the same face changes nothing, and carving deeper works (a second use);
4. every server check refuses with its reason: no session, a session for another side or block, a pixel value of 3, a 15-row face, the top side, no knife (shears), too far away, adventure mode, and a melon; a knife in the off hand carves;
5. two sides keep their own faces, numbered from the front clockwise;
6. a torch lights a carved pumpkin to its glow and is used up; an empty hand takes it back into the inventory; an uncarved hand-carved pumpkin can't be lit;
7. a lit pumpkin carved on two sides drops itself with its design and its torch, and placed again is lit with the same design and glow;
8. with `carving.free_draw` off, a free design is refused and a starter face carves;
9. glow values (blank 0, one hole 4, 48 holes 15, 96 shaved 12), a design saves and loads unchanged, a wrong-sized design doesn't load, and the starter faces are valid and distinct;
10. roasted pumpkin seeds restore 2, and their furnace, smoker and campfire recipes load.

The client game test goes through the screen twice. First with its buttons:
- the server opens the carving screen for a plain pumpkin, as using the knife does (`PumpkinCarvings.open`);
- the test presses Apply (the Classic face), Mirror and Candle, then Done;
- it then checks on the server that the pumpkin holds the Classic face. The log reads `[carving test] face carved over the network: true`.

Then by hand, as a player carves (added after the owner's first play, below): the use key with the knife opens the screen, and mouse clicks and drags, a right-click, Ctrl+Z and the Shave tool carve a face the server must then hold exactly. The log reads `[carving test] face carved by mouse and keyboard: true`.

The test also builds a row of carved pumpkins on hay bales and photographs it by day and lit at midnight.

The first CI run did not compile: the client test used a field that 26.3's `Minecraft` doesn't have; it now waits for the screen with the test context's `waitForScreen`. The main and client code compiled, and every server test passed, on the first run that built.

The client log shows no model or texture errors.

**Found in play, and fixed.** The owner played the first version: the screen looked right, but clicking the grid did nothing.
- Cause: Minecraft 26.3 numbers mouse buttons from 1 (left 1, middle 2, right 3), as its `MouseHandler` and vanilla's widgets read them. The screen used the older numbering (left 0, right 1), so a left click was taken as a right-click erase, which leaves uncarved skin as it is, and right clicks were ignored. Ctrl+Z checked the old key code for Z (90; 26.3 numbers keys differently, and Z is 29), so undo by keyboard did nothing either.
- Fix: the screen uses 26.3's button numbers, and matches Ctrl+Z as vanilla's edit shortcuts do: by the letter on the keyboard layout, with Cmd on macOS.
- Why the tests missed it: the client test only pressed the screen's buttons through the test harness and never clicked the grid.
- New test: the client test now also carves a pumpkin as a player does, through the game's own input handlers:
  - it uses the knife on a pumpkin with the use key, which opens the screen;
  - with the mouse (Fabric's test input, through Minecraft's `MouseHandler`) it left-clicks a cell, left-drags along a row, right-clicks a cut to erase it, presses Ctrl+Z, right-clicks another, picks Shave and clicks a cell, then presses Done;
  - Ctrl+Z goes through Minecraft's `KeyboardHandler` as the event a keyboard sends (the Z key, its key code and the platform's shortcut modifier). Fabric's `TestInput.pressKey` can't send it: it sends every key without modifiers, even while `holdControl` holds Ctrl;
  - the server must then hold exactly that face, pixel by pixel. The log reads `[carving test] face carved by mouse and keyboard: true`.
- Without the fix, this test fails as the owner saw it (`d2663ad`): the screen opened, but the pumpkin was left uncarved. With the fix (`26eccb7`), only the test's Ctrl+Z step still failed, because of the Fabric limitation above; the test now sends Ctrl+Z through the keyboard handler and passes (`1864fc9`).

**Not run:**
- a person playing the fixed version in a client (the test drives the real input path, but with simulated input);
- a dedicated server with two players, including two players carving the same side at once;
- save and restart with carved pumpkins in the world (the design's save format and the item round trip are tested);
- resource packs that retexture pumpkins;
- performance with many different designs in view.

These need a play session.

## World and event applicability
No worldgen. Nothing is seasonal; a later Halloween event could add a carving contest on top, but carving never depends on an event.
