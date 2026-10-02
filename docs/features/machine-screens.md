# Machine screens redesigned

Status: merged in #78 (batch 22). Compiles and tests in CI only; screenshots come from the client game test.
Proposal issue: the owner, 1 October 2026: "redo all the userinterfaces for all the machines to make them more interesting and obvious what they are for and thematically correct and still readable". The owner attached five reference images:
- an orange-on-brown terminal with a pixel keyboard;
- a grey device with a screen;
- a slate-teal inventory panel;
- a teal mech-GUI kit;
- a dark green-text monitor.

Owner: jimbozoomer-byte
Target milestone and tier: all machines
Primary specialty and supported player role: every player who uses a machine

## Player experience
Every machine screen now has two parts.

**The machine bay and inventory** (the same slots, in the same places):
- It sits on a themed panel, with the title in a header band.
- Slots, the energy bar, tanks and the progress arrow are drawn in the theme's colours:
  - the energy bar is a column of lit segments;
  - tanks are glass tubes with a highlight and quarter marks;
  - progress is an arrow that fills;
  - fuel is a flame that burns down.

**A control terminal on the right**, a CRT with a keyboard:
- **What the machine is for**, in a line. For example: "Crushes one ore into two raw ores", "Splits air into nitrogen, oxygen, argon". Every machine has one.
- **What it is doing:** RUNNING, IDLE or NO POWER (in a warning colour). Generators show GENERATING; batteries show CHARGING, DISCHARGING or STORING.
- **Progress** as a ten-cell meter and a percentage.
- **Power** stored, and its **rate** in JE/t over the last second.
- **The machine's own condition**, where it has one: arc furnace formed or incomplete, wind turbine clear or blocked, pumpjack oil or dry, fracking rig shale or none, water wheel turning or still.
- Below the screen, the **side controls** (faces, redstone, eject), moved out of the crowded bay.

**Three themes**, following each machine's model:
- **Dieselpunk** (most machines): riveted dark iron, brass trim, an amber-on-brown terminal and an orange keyboard (reference 1).
- **Electric** (the power gear in the electric look): graphite, mint-green glow and a green-text monitor (reference 5, with the grey device of reference 2).
- **Lab** (chemistry and electronics): slate-teal panels with cyan accents (references 3 and 4).

Hovering the energy bar or a tank still shows exact numbers.

## Connections
Every machine. No gameplay change.

## Balance and automation
None: the screen only shows what the server already syncs. The rate is measured on the client from the synced energy.

## Multiplayer and persistence
- Client-only drawing.
- The menu, slots, synced data and buttons are unchanged, so servers and saves are unaffected.

## Dependencies and assets
- No new dependencies.
- The backgrounds are drawn by `tools/gui_textures.py`, which holds the themes, their colours, which machine uses which, and the taglines. It writes:
  - `textures/gui/machine_<theme>.png`;
  - `gui/machine_themes.json`;
  - the lang keys.
- All original. The references were used for mood and colour only.

## Verification
- `tools/check_mod_data.py` passes. Every machine has a tagline and a theme.
- The client game test now screenshots three screens, one per theme: the crusher (dieselpunk), the battery box (electric) and the circuit assembler (lab). CI prints small previews.
- Not run:
  - a human looking at every machine's screen;
  - other GUI scales;
  - recipe viewers' (JEI/EMI) placement next to the wider screen.

## World and event applicability
Not applicable.

## Rollout and open questions
- The screen is 268 pixels wide instead of 176. At the smallest window sizes the GUI scale steps down, as vanilla's wide screens do.
- Further polish could add per-machine art in the bay (a furnace mouth, a press, a column), and themed buttons in place of vanilla ones.
