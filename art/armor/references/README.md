# The owner's armor renders

The owner designs armor in Blockbench and sends renders of it. These are those renders, kept so the 3D armor sets can
be compared with them and rebuilt from them (`tools/armor_reference.py`). They are the owner's own work, given for
this project; like the rest of the repository they fall under its [license](../../../LICENSE_POLICY.md).

Each set has a folder. A view `<view>.png` is the render as sent; `<view>.fit.json` beside it is what was fitted to it:
the camera, the figure's pose (shared by the set's views) and polygons round anything that hides the armor (a weapon, a
shield, sparks), which the fitting and the texel lifting leave out. Where the owner's figure stands differently from ours, a region of a render
has a camera of its own, `<view>@<region>.fit.json` (the Frost Knight's `front@head` and `front@legs`); each fit says
in its `note` how it was fitted and what it was used for.

| Set | Views | Sent |
| --- | --- | --- |
| [Sentinel](sentinel/) | `front_right`, `front_left`, `back` | 8 October 2026, with "Just made these ones aswell want them done weapons too please" (the sword and the star shield in them are the set's arms, not its armor) |
| [Frost Knight](frost_knight/) | `front`, `back_left` | 8 October 2026, in the same message (the ice sword and the sparks round it are the set's arm and the scene, not its armor) |
| [Paladin](paladin/) | `front_right`, `front_right_armed` | 8 October 2026, with the Berserker and the Templar: "I made these 3" (the sword and the shield in the second are not armor) |
| [Templar](templar/) | `front`, `front_left_armed` | the same day (the clawed axe in the second is not armor) |
| [Reaper](reaper/) | `front` | 8 October 2026, with the Wight King, the Banana and the Scarab (the two segmented crescents are the Reaper Scythes it holds, not its armor: "He is supposed to be holding 2 short scythe weapons they arent part of the armor") |

The armor built from them is a Blockbench project under [art/armor/](../), one per set: see
[docs/features/blockbench-armor.md](../../../docs/features/blockbench-armor.md).
