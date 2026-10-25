# Jimtafacts workshop computers

Local implementation, 23 October 2026. AI assistance: OpenAI Codex (GPT-6).
The supplied computers collection now has native Jugcraft hardware, original
rendering assets and a bounded workshop instruction language. **This is not a
Lua/CraftOS implementation.** The supplied ROM is retained in the owner library;
it is not executed, bundled as working firmware or advertised as compatible.

## Content and integration

All 23 named entries are registered: normal, advanced and command computers;
normal and advanced turtles and pocket computers; normal and advanced monitors;
printer, speaker, disk drive, redstone relay, cable, wired modem, full wired modem,
normal and advanced wireless modems; disk, treasure disk, printed page, printed
pages and printed book. Command computers are creative-only; their present operator
can run level-two Minecraft commands at a bounded rate. Pocket computers edit and
carry programs, transfer them to disks and link to permitted nearby computers for
remote operation.

Hardware IDs are `jugcraft:jimta_computer_<source-name>`; standalone items are
`jugcraft:jimta_<source-name>`. Existing JE, data cable, sensor, channel switch,
inventory transfer and owner/party/claim interfaces are reused. Hardware recipes
start from existing logic controllers, data cables, relays and vanilla resources.
Printed items are printer outputs, rather than crafting recipes.

The native terminal has an editable program panel, live output, inventory, energy
readout, Save/Run/Stop, disk transfer, Refuel and Help. Live snapshots do not erase
unsaved edits. Programs, cargo, fitted upgrades, energy, signal values and open
modem channels persist. Only public display text and visible turtle upgrade and
animation state go in ordinary world rendering packets. Open-menu snapshots are
authorized by the bound menu and owner/party permission.

## Playing with it

Place a computer and a monitor, printer, speaker or disk drive next to it, or link
them with Jugcraft data cable / the supplied computer cable. Place devices as the
same owner or party. Supply JE using an existing energy cable or insert coal or
charcoal and select Refuel (one item = 2,000 JE, buffer 16,000 JE). Each instruction
costs one JE. Save a program and press Run; Help contains the full reference.

Example:

```text
print Greenhouse online
display Greenhouse online
read green
channel green on
wait 100
channel green off
stop
```

Commands: `print`, `stop`, `wait`, `goto`, `if`, `signal`, `read`, `channel`,
`relay`, `display`, `beep`, `printpage`, `printpages`, `printbook`, `save`, `load`,
`send`, `listen`, `unlisten`, `transmit`, `receive`, `reply`, `ifmail`, `select`,
`equip`, `unequip`, `craft`, `attack`, `turn`, `move`, `dig`, `place`, `pull`, `push`.
Directions are `front/back/left/right/up/down`; channel colors are vanilla dye
names. Modem channel and reply-channel numbers are 0–65535. `receive` waits for a
message. `transmit 42 7 chemistry ready` sends text on channel 42 with reply channel
7; the receiver must have used `listen 42`. Messages are data, never executed code.

A turtle has 16 cargo slots plus left and right upgrade slots. Fit a diamond
pickaxe/axe/shovel/sword/hoe, crafting table, speaker or wireless modem directly
in slots 17/18, or select a cargo slot and use `equip left/right`. Movement and
crafting cost 20 extra JE; mining and attacks cost 30. Crafting uses cargo slots
1–9 as a 3×3 recipe grid and reserves space for output and remainders first.
Mining cannot break block entities, unbreakable blocks or blocks harder than 50.
Placement accepts simple blocks, excluding block entities, doors and double
plants. Turtle attacks use a fitted diamond sword, respect protection and line
of sight and never target players. Inventory transfers use Fabric transactions.

Monitor walls form owner-matched upright rectangles up to 8 wide × 6 tall. Their
original edge/interior models are selected by connected sides. Nonrectangular or
oversized layouts stay as individual displays. Normal wireless links reach 64
loaded blocks. Two advanced endpoints reach 256 in one dimension or communicate
across already-loaded dimensions on the same server. Remote-modem access still
checks the sender's identity against ownership, party and claims; it loads no chunks.

## Original models, textures and animation

Source: `art/owner-library/originals/Oct23/Jimtafacts/computers/`. The source archive,
notices and names are retained there. `tools/jimtafacts_computers.py` copies every
supplied computer texture and texture sidecar byte-for-byte, including the
original terminal surround. It remaps model references into Jugcraft and removes
the source loader's `render_type` property; cube geometry, UVs, display transforms
and texture pixels are unchanged. The copy manifest records source paths and
SHA-256 digests in
`art/owner-library/catalog/edited-2026-10-23-jimtafacts/computer-copies.json`.

Original static class inspection (trusted CFR/javap, no source classes executed)
covered `TurtleBrain`, `TurtleMoveCommand`, `TurtleBlockEntityRenderer`,
`ItemUpgradeModel`, `MonitorBlockEntity`, `PrinterBlockEntity` and `ModemPeripheral`.
The native turtle renderer uses the original body and left/right crafting-table,
speaker and modem models, with native diamond tool models in the original mount
orientation. Movement and quarter-turns interpolate over eight ticks. Tool swings
use the original 45-degree sine arc over eight ticks. The reduced-motion setting
settles those poses immediately. Printer/disk drive full states use their source
models, and monitors use all 16 original connection variants. Source-rendered
visual fidelity still needs the combined client playtest.

## Bounds and authority

- 128 program lines / 8,192 characters, one instruction per five ticks per device;
  at most 64 instructions per server tick across these computers.
- No chunk tickets or unloaded-chunk reads; bounded network traversal of at most
  128 examined positions, rechecking owner/party/claim permissions at endpoints.
- Up to 16 open channels and 16 queued messages per computer; messages up to 512
  characters, at most eight sends per server tick, 512 examined loaded wireless
  endpoints and 32 wireless receivers per send.
- Physical work requires a present owner, or the existing explicitly enabled
  absent-owner authority option. No general permission or protection bypass.
- Failed movement preserves original inventory; rollback suppresses destination
  drops. This is an in-tick operation, not a claimed crash-atomic world transaction.
- No host filesystem, operating-system process, JVM loading, HTTP or source Lua
  execution. Unknown instructions stop with an explicit native-language error.

## October 23 continuation — peripherals, documents and monitors

Implemented locally with OpenAI Codex, user-selected Astra High; no compilation or tests run:

- `ports` resolves up to 32 loaded, permitted item/fluid ports beside the computer and connected wired modems. Local names use absolute directions; remote names use stable modem coordinates and direction, so adding another device does not renumber existing ports. `items <port>` and `fluids <port>` show at most 16 entries after examining at most 128 slots. `moveitems <from> <to> <count>` and `movefluid <from> <to> <mB>` transfer up to 64 items or 1,000 mB and pay 10 JE in the same transaction. Source, destination and connecting claims are rechecked; unavailable ports fail explicitly.
- Printer pagination now uses 25 columns and 21 lines, up to 16 pages. Each actual page consumes one paper and one ink; binding consumes string, and a book also leather. `printpage` refuses oversized text before consuming ingredients. The document viewer shows the page across its two panes with Previous/Next controls. Existing saved text is preserved.
- `colour <dye>` changes connected advanced-monitor text color. Advanced monitor front-face clicks enqueue bounded touch events for permitted wired computers; `waittouch <line>` waits, sets `touch_x`/`touch_y` and branches. Plain text uses a native 16-unit grid per monitor block; cell mode uses 1-based 48×24 coordinates matching the drawn area and ignores unused margins. At most 16 events wait per computer. Sneak-use opens the monitor inventory. Render packets contain public display state only.
- Sneak-use a pocket terminal on a permitted computer to link it. Ordinary use opens that computer within 64 blocks, in the same loaded dimension; sneak-use in air retains the offline program editor. Distance, actual held stack, ownership/party and claims are checked for every menu action. This is native remote-terminal access, not portable execution of the source ROM.

## Deferred compatibility work

Full Lua/CraftOS, original ROM programs and games, arbitrary Lua peripheral APIs,
rednet compatibility, continuous streaming audio and arbitrary external turtle
upgrade mods are **not implemented**. Native cross-dimensional advanced modems,
cell-color monitors, finite DFPWM clips and operator-only commands are implemented
below, alongside remote terminals and document pagination. These do not establish Lua peripheral compatibility.
They are not silently substituted with claimed source compatibility. The bounded
native hardware is usable independently; the dependency and isolation review
below describes what a real Lua implementation requires.

## Lua runtime review

No approved Lua runtime exists in the current pinned dependencies, and the supplied
computer collection does not contain its Cobalt runtime classes. Upstream
[CC:Tweaked pins Cobalt 0.9.9](https://raw.githubusercontent.com/cc-tweaked/CC-Tweaked/mc-1.21.x/gradle/libs.versions.toml).
[Cobalt's build](https://raw.githubusercontent.com/cc-tweaked/Cobalt/master/build.gradle.kts)
identifies `cc.tweaked:cobalt`, a Java 17 toolchain and its publisher. Its
[README](https://raw.githubusercontent.com/cc-tweaked/Cobalt/master/README.md)
describes a mostly Lua 5.2 interpreter with selected Lua 5.1 compatibility and
interrupt/resume support, designed for CC:Tweaked rather than a stable general API.

A reviewed, pinned runtime could support a future adapter, but introducing it must
follow the repository dependency/distribution policy. The current approved mod
lock does not authorize adding an arbitrary nested library JAR. The necessary
work includes a virtual filesystem with quotas and validated paths, immutable ROM
resources, terminal/event/peripheral adapters, isolated globals, inherited coroutine
instruction limits and server-wide work accounting. Cobalt's
[debug hooks](https://raw.githubusercontent.com/cc-tweaked/Cobalt/master/src/main/java/org/squiddev/cobalt/debug/DebugHook.java)
and [interrupt API](https://raw.githubusercontent.com/cc-tweaked/Cobalt/master/src/main/java/org/squiddev/cobalt/LuaState.java)
offer instruction control; these alone do not establish a hard heap allocation
limit for strings/tables and native library functions.

[LuaJ's own sandbox example](https://raw.githubusercontent.com/luaj/luaj/master/examples/jse/SampleSandboxed.java)
omits host I/O, OS and Java access and calls out coroutine-thread and shared-state
hazards. Therefore no unbounded VM was added as a quick replacement. No source
ROM or supplied executable classes were run during this review.

## Local continuation — program storage, displays, audio and advanced hardware

Implemented with OpenAI Codex, user-selected Astra High; **not compiled or tested**.

- `programs`, `store <name>`, `runfile <name>` and `deletefile <name>` use a virtual shelf inside the computer's saved block data. It holds at most 16 programs and 65,536 characters total, with the existing 128-line/8,192-character limit per program. Names are flat letters/numbers/underscores/dashes; they cannot resolve host paths. `runfile` replaces the current program rather than creating an unbounded call stack.
- `set`, `add`, `subtract`, `multiply`, `divide` and `ifvar` provide actual integer control flow. There are at most 32 registers, each clamped to ±1 billion; division by zero stops with an error. `print`/`display`/`write` expand `$register` words. `readvar`, `energyvar`, `mailvar` and `countvar` expose current sensor, JE, message-count and permitted real-inventory values. Missing sensors return -1. No program/register content is sent in public block-render packets.
- Cell displays support `cursor`, `ink`, `write`, `clear`, `scroll` and advanced `blit` on a fixed 48×24 grid. Original `term_font.png` pixels and recovered 6×9 glyph UVs render both glyphs and solid background cells; this replaces a proportional-font approximation. Display data persists. Normal monitors remain monochrome. The plain `display` command returns to wrapped-text mode. Touch coordinates follow the same display margins and scale.
- `audio <hex>` plays an independently decoded DFPWM1a clip through connected speakers and speaker turtle upgrades. Maximum encoded size is 4,096 bytes (also subject to program length), at most four speakers per instruction and four such instructions per server tick. A computer pays 20 additional JE and waits at least a second between clips. Only players within 48 blocks receive data; clients cap active clips at eight and use Minecraft's record volume controls. There are no URLs, external files or background downloads. This is finite clip support, not continuous source audio streaming.
- The DFPWM predictor/decoder is adapted from the supplied `data/computercraft/lua/rom/modules/main/cc/audio/dfpwm.lua`; the MPL-2.0 SPDX notice and source attribution are retained in `WorkshopDfpwm.java`. The source Lua was read, not run. `DirectFixedWidthFontRenderer` was read statically to recover the original font atlas layout; the native renderer uses the existing Fabric/Minecraft rendering API, not source binary loading.
- Advanced modem pairs transmit across loaded dimensions, paying 10 extra JE per remote modem reached. The remote permission identity carries the sender's UUID and answers current owner/party/claim checks; transmitted text is never executed as a program.
- `command <Minecraft command>` works only on Command Computers with a present, non-stand-in operator. Its source is capped at permission level two, rate is once per second and cost is 100 additional JE. Regular computers/turtles cannot use it. Operator revocation pauses access; no permission level is saved as an enduring grant.

Example native monitor dashboard (connect a monitor and supply JE):

```text
clear
cursor 1 1
ink lime black
write WORKSHOP POWER
energyvar charge
cursor 1 3
write $charge JE available
wait 20
goto 5
```

This continuation does not add Lua syntax, execute original ROM programs, provide a host filesystem/HTTP API or claim full source VM compatibility. Those remain genuine additional work, not completed content hidden behind a translated label.

## Validation status

### Additional local turtle instructions

`selectedvar`, `slotvar`, `spacevar` and `comparevar` read real cargo/selection into integer registers. `transferslot <slot> <count>` moves existing stacks between the 16 cargo slots while preserving their components. `detectvar` and `compareblockvar` inspect one loaded, permitted position and write 1/0; `inspect <side>` prints its block identifier/properties without exposing block-entity data. `till <side>` uses a fitted diamond hoe, 20 extra JE and one durability point to prepare actual farmland (or coarse dirt into dirt), retaining the source arm swing. `refuel` consumes actual coal/charcoal only when 2,000 JE fits; it never eats fitted upgrades. Inventory transfers now also check the shared claim-boundary rule. Beds join doors, double plants, fluids and block entities in the unsupported turtle-placement set. Monitor touch coordinates update as a pair before consuming the queued event.

These changes remain uncompiled and untested. A deferred farmland/cargo/accounting scenario accompanies them; existing test players now have bounded cleanup even when a scenario fails.

Implementation and source inspection only; **no compilation, automated tests or
game launch performed for this subsystem yet**, as requested. Prepared tests:

- `JimtafactsComputerGameTests`: registered content, power accounting, disk/save
  persistence, redstone shutdown, cargo/upgrades across movement, protected mining,
  real crafting, printer consumption, monitor walls, channel/reply messages and
  rejection of foreign executable syntax.
- `scripts/tests/test_jimtafacts_computers.py`: source hashes, exact copied assets,
  permitted model remapping, original monitor variants and turtle upgrade models.
- `JimtafactsComputersClientGameTests`: all native block-state model variants and
  the twelve original turtle peripheral side models bake into renderable parts.

Combined validation must include ordinary and optional-mod server tests, client
model baking and manual inspection of original turtle body/side upgrades and all
eight-tick motions, monitor-wall text alignment, terminal editing/disk transfer,
reloading saved computers, and multiplayer ownership/claims. All changes remain
local until the owner has playtested them.
