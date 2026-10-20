# Designing a Jugcraft blueprint

This guide is for anyone writing a Jugcraft blueprint by hand or with an AI assistant. Give it to the assistant along with
`blocks.txt` and `tags.txt` from this kit, describe the building you want, and ask for a `.jugbp.json` file in this format.
Paste the result into the Blueprint Table's IMPORT sheet (+ NEW IMPORT, PASTE, IMPORT). The server checks it and says
in plain words what is wrong if it refuses it.

## The file

A blueprint is one JSON object:

```json
{
  "format": 1,
  "name": "Garden Shed",
  "kind": "individual",
  "palette": {
    "S": "minecraft:stone_bricks",
    "P": "#minecraft:planks",
    "D": "minecraft:oak_door[facing=south,half=lower]",
    "d": "minecraft:oak_door[facing=south,half=upper]",
    "G": "minecraft:glass_pane"
  },
  "layers": [
    ["SSSSS", "SSSSS", "SSSSS", "SSSSS"],
    ["PPPPP", "P...P", "P...P", "PPDPP"],
    ["PPGPP", "G...G", "P...P", "PPdPP"],
    ["PPPPP", "PPPPP", "PPPPP", "PPPPP"]
  ]
}
```

- **`format`**: 1. (Format 2 is the same with run-length rows: `"5S"` means five `S`.)
- **`name`**: what the table shows, at most 48 characters.
- **`kind`** (optional): `"individual"` (one structure, the default), `"set"` (several parts making a whole) or
  `"part"` (one part of a set, such as a single cooling tower).
- **`palette`**: one character for each block. Never `.` or a space, which mean "nothing here".
  - A block id, with its state in brackets where it matters: `"minecraft:oak_stairs[facing=east,half=bottom]"`.
  - Or an **"any material" slot**, a block tag starting with `#`: `"#minecraft:planks"` lets any planks count there, so
    the builder can use whatever wood they have. The tag's first block is the one shown.
- **`layers`**: the building from the **bottom layer up**. Each layer is a list of rows; each row is a string with one
  character per block.
  - Within a layer, the **last row is the front**, nearest the Survey Stake and the player placing it.
  - Each row runs left to right as the player standing at the stake sees it.
  - A block whose state has a facing turns with the blueprint. Draw it as if the front faces south: a door facing the
    player is `facing=south`.
- **`anchor`** (optional): `[x, y, z]`, where the stake stands, counted in characters, layers and rows from the first
  row's first character on the bottom layer. It must be outside the building. Left out, the stake stands one row in
  front of the middle of the front row.
- **`size`** (optional): `[width, height, depth]`, to keep empty space at the edges that the rows leave out.

## Limits

- At most **48 blocks** each way, **8,192 blocks** in all and **262,144 characters** of text.
- These blocks are refused, alone or inside a tag: command blocks, structure blocks and voids, jigsaws, barriers, light
  blocks, bedrock, spawners and trial spawners, end portals and their frames, end gateways, nether portals, reinforced
  deepslate and test blocks. `blocks.txt` lists every block that is allowed, and `tags.txt` every tag a slot can name.
- Water and lava in the palette are not placed by drones; the stake pours them in when the rest is finished.

## Tips for a good design

- Draw the floor first and build upward; drones place each column bottom first, so overhangs need something under them
  while they are built or a layer below that is finished first.
- Use "any material" slots for walls, floors and roofs where the exact wood or wool does not matter.
- Keep stairs and slabs in matching sets, and give stairs their `facing`, `half` and `shape`.
- A Survey Stake beside the build can feed a comparator: it reads how much of the build is in place.

## The kit

The Blueprint Table writes this kit to `.minecraft/jugcraft/blueprint_kit/` when you press KIT on the IMPORT sheet:

- `ai_guide.md`: this guide;
- `blocks.txt`: every allowed block id, with its state properties and their values;
- `tags.txt`: every block tag, with how many blocks it holds;
- `example.jugbp.json`: the garden shed above, ready to import.
