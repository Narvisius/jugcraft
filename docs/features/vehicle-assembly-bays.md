# Vehicle assembly bays

Owner request: 19 October 2026. Local implementation by OpenAI Codex (GPT-6); not pushed or merged.

## Play

Craft/place the **Mecha Assembly Workbench** (6 wide x 6 deep x 6 high) or **Tank Assembly Workbench** (6 wide x 6 deep x 3 high). The controller is the front-left corner when viewed from the front. The structure extends to the viewer's right and back. Placement requires the entire envelope to be clear and permitted. Deck plates, four pillars and side rails are real structural parts; the interior stays open. Breaking any part dismantles the bay once and returns its inputs.

Connect an existing Jugcraft cable/battery to the cyan socket on the controller's outside left face. This is the only power inlet. Open any structural part. Choose a vehicle, armor and gearing, put the previewed components into the nine input slots, then Build / refit. No research, typing or blueprint consumable is required. Ordinary vehicle crafting recipes remain available.

The mecha bay builds Diesel, Armoured and Howitzer Walkers; the tank bay builds the existing Landship. Material bills match the existing vehicle recipes, accepting shared steel tags. The armored/howitzer conversions use a Diesel Walker as their chassis. Outputs are deployment kits; take the finished kit and use it on clear ground. The bay does not spawn or automatically capture live vehicles, passengers or someone else's vehicle.

For refitting, insert an existing kit in the separate REFIT socket. It selects that kit's chassis. Four iron nuggets cover refitting; only newly installed upgrades cost extra components. Replacing/removing upgrades does not refund previous materials. Kit custom data, name and stored fuel survive a refit. Inputs stay locked during work. Cancel preserves them, but does not refund energy already used. No hopper access is exposed; this is a supervised workstation, with shared cable power.

## Chassis options

| Option | Actual effect | Extra components |
| --- | --- | --- |
| Standard armor | Existing vehicle damage threshold and speed | None |
| Reinforced armor | 1.5x existing damage threshold, 0.9x movement speed | 8 steel plates |
| Standard gearing | Existing driving speed and fuel use | None |
| Overdrive | 1.2x movement speed, 2x driving fuel use | 2 steel gears, 4 redstone |
| Economy | 0.85x movement speed, half driving fuel use | 2 steel gears, 4 redstone |

Armor and gearing speed multipliers combine. The existing weapons, aiming, seats, fuel types and movement animations remain authoritative. These upgrades do not add arbitrary weapon slots or recolor the owner's vehicle models. Vehicle fuel and fitout survive saves and survival recovery back into a kit. Older kits/entities default to standard settings.

Construction draws 64 JE/t for 200 ticks (12,800 JE); refitting uses 100 ticks (6,400 JE). Power loss pauses paid progress. The 64,000 JE buffer receives at most 512 JE per transaction. Unloaded/missing structural parts or obstructed work space pause production. Machines disabled in config cannot start or advance work. Stable item/block/entity IDs remain registered.

The console previews the actual vehicle item and exact component counts, exposes click-only armor/gearing choices and their tradeoffs in tooltips, and shows power, status and progress. There is one output slot and explicit start/cancel, so no hidden continuous crafting loop.

## Visuals and provenance

The owner's five attached images are design references, not copied textures. Original native cuboid geometry in `tools/vehicle_bays.py` uses existing Jugcraft silver steel, gunmetal, rubber, amber and cyan-screen materials. The owner library and catalog were consulted; originals remain unchanged. Runtime geometry is split through the existing `model_writer` to preserve UV bounds and separate coplanar faces. Animated geometry uses the existing QuadModel renderer.

The mecha bay is a tall four-post gantry; the tank bay is a low repair platform. Their carriage spans two supported rails and carries a telescoping clamp. A separate rear-post robot arm sweeps through a bounded angle. Both ease into and out of powered motion. Control cabinets, tool drawers and a flush power socket are attached to the deck. No dangling decorative pipes or floating arms.

## Validation

The combined local build and validation run passed all **1,999 required server tests**, including nine new bay tests. These cover all four vehicle bills and paid upgrades, power pause/cancel/save behavior, refit identity and fuel, every rotation and the real energy inlet, obstruction/config rejection, one-drop dismantling, real item placement, vehicle deployment/save/recovery, and both real crafting-recipe lookups.

The disposable rendered-client test passed real menu open/start/cancel actions and captured seven views of the bays, console and moving/stopped machinery. The final screenshots were inspected at 854x480: the complete console fits, selected settings are marked, and a full buffer correctly reads 64k JE. No owner profile was launched or changed. Local evidence: `../vehicle-bays-verified.log` and `build/run/clientGameTest/screenshots/`.

The updated acquisition graph covers **5,759 registered Jugcraft items**, including both new workbenches, with zero ingredient diagnostics. The 78 previously deferred equipment acquisitions remain explicitly deferred. No manual owner playtest, launcher import or two-human-client test is claimed.

The full offline data/recipe audit passes for **5,971 material IDs**. Art checks pass for **11,686 block/item models**, **3,713 animated quad parts** and **173 complete assemblies**. The two bay loot tables use the 26.3 singular `condition` field; both bays are included in the registry, feature-switch, mining-tag and assembled-machine conservation checks. Evidence: `../vehicle-bays-data-final.log` and `../vehicle-bays-acquisition.log`.

The final runtime world loaded and **all nine bay tests passed again** (`../vehicle-bays-final-runtime.log`). Loot predicates use `condition: {type: ...}`; the full loot registry also passes a new discriminator check in `check_mod_data.py`. The packaged JAR was verified byte-for-byte against the final build, including both corrected loot tables and bay blockstates.

Local playtest package: `../jugcraft-vehicle-bays-oct19/jugcraft-complete-0.1.0-vehicle-bays-oct19.mrpack`, with a short `PLAYTEST.txt` and two inspected screenshots alongside it. Import creates a separate launcher instance; launcher import and owner gameplay remain untested here.

## Follow-up

- Owner playtest: silhouette, scale, console readability and upgrade balance.
- Two human clients: simultaneous access and reconnect with pending work.
- Future optional extensions: a claim-aware live-vehicle docking system, independently authored cosmetic palettes, and chassis-specific weapon modules. Not silently included in this implementation.
