# Original digital-storage rendering

Local work, 23 October 2026. AI assistance: OpenAI Codex (GPT-6). No tests or
builds have been run for this addition; include it in the final combined batch.

`DigitalStorageRenderer` uses all ten supplied item/fluid cell chassis models in
drive bays, including their original texture coordinates and tier markings. The
source `DriveModel.getSlotOrigin` places the five rows and two columns at
`((9-col*8)/16, (13-row*3)/16, 1/16)`. The ME chest uses the source cell offset
`(5/16, 4/16, 0)`. Empty bays have no inserted chassis. Original five-face LED
geometry and green/cyan/amber/red status colors show empty, occupied, type-full
and capacity-full cells; unpowered cells use black LEDs. Actual content-count
changes produce the supplied cubic 200 ms pale-red activity blink.

The inscriber's moving inside faces reproduce `InscriberRenderer`'s six quads,
UV formulas and compression/decompression easing. A committed native workshop
operation starts one 16-tick (800 ms at normal tick rate) cycle. The displayed
press and middle input/result are the actual item identities. The charger shows
the inserted or finished crystal in the supplied location with the original
`sin(time) * 0.02` vertical motion at height `0.35`. The motion uses game time so
pausing freezes it. Reduced-motion mode settles the press and removes the bob
and status blink.

Only public appearance is synchronized: cell tier/type and status numbers, blink
timing, bare displayed item identities and operation timing. No cell inventory,
cell filter, program, custom item components, queued pattern or owner identity
is sent through rendering packets. Saved authoritative machine inventories are
unchanged. Render updates are checked at most every five ticks, with packets only
when appearance changes; the successful work animation is sent immediately.

The source remains at
`art/owner-library/originals/Oct23/Jimtafacts/applied energentics reborn by jimbo/`
with its retained notices. Trusted CFR/javap were used for static inspection only:
`DriveModel`, `DriveRenderer`, `CellLedRenderer`, `CellState`,
`ChestOrDriveRenderState`, `MEChestRenderer`, `InscriberRenderer`,
`ChargerRenderer`, and their render state classes. No supplied class was executed.
The helper `tools/jimtafacts_network_rendering.py` requests original meshes through
the existing resource adapter and copies the inside texture byte-for-byte. Its
texture digest is in `network-render-copies.json`; the main adapter records the
drive models in `network-runtime-copies.json`.

Prepared, unrun validation: `JimtafactsNetworkRenderingGameTests` checks public
packets cannot reveal stored items/filters/owners or populate inventories, and
strips custom data from displayed workshop items. The focused Python tests check
all ten mesh/UV definitions and the exact inside texture. The combined client
playtest must inspect all four facings, cell insertion/removal, empty/occupied/full
LEDs, moving inscriber faces and charger item alignment. This rendering work does
not claim broader AE API or storage-format compatibility.
