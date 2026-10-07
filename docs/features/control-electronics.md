# Control electronics: data cables, sensors, relays, the logic controller and the control room

Status: batch 36 is implemented on `feature/control-electronics-36`, stacked on `feature/gas-storage-35`. Batch 37 (the control room) is on `feature/control-room-37`, stacked on batch 36. Both await review. They compile and test in CI only; **not yet played**.
Proposal issue: the owner, 3 October 2026, chose control electronics first, then rocketry ("lets begin doing all these but start with the order you recommend").
Owner: jimbozoomer-byte
Target milestone and tier: electronics tier (processors, optical fibre, the network terminal).
Primary specialty and supported player role: base automation and factory management.

## Why
Until now, machines only reacted to redstone wired right next to them. This batch lets a player run a factory by rules: "when the hydrogen holder is above 90%, stop the electrolytic cells; start them again below 50%". It is the first of two batches. The second adds the control-room monitor (gauges and history graphs), an alarm and a handheld remote.

## Player experience
- **Channels** are the sixteen dye colours. Use a dye on a sensor or relay to set its channel; the dye is used up. Each shows its channel as a coloured lamp.
- **Data Cable** (12 from 6 plastic sheets and 3 optical fibre): joins sensors, relays and logic controllers. It carries no power or items.
- **Sensor** (2 from a glass pane, 2 copper wire, a microchip and 3 plastic sheets): mounted on a tank, battery, machine or chest.
  - It reads how full that block is: energy if it stores any, else fluids, else items.
  - It gives a redstone signal like a comparator's (0 when empty, 1-15 by fill).
  - It reports the exact percentage on its channel, re-reading every second.
  - Right-click it to see the reading.
- **Relay** (2 from a repeater, 2 copper wire, a microchip and 3 plastic sheets): while a logic controller has its channel on, it gives a full redstone signal on every side. Put it beside a machine set to a redstone mode and the controller runs the machine.
- **Logic Controller** (4 steel plates, a network terminal, 2 microchips, a processor and a data cable): eight rules, each "IF *channel* below/above *N*% THEN *channel* ON/OFF".
  - Every second it reads each channel; several sensors on one channel are averaged.
  - It applies its rules in order, a later rule overriding an earlier one.
  - A channel no rule switches keeps its state, so a pair of rules makes a dead band.
  - It sets every relay on a channel its rules target, and leaves other channels' relays alone.
  - Every part of a rule is a button on its screen. A strip along the bottom shows each channel's reading and whether the controller has it on.
- Advancement **In Control** (build a logic controller). Handbook: "Control Networks" and "Logic Controller" pages in the Electronics chapter.

### The control room (batch 37)
- **Control Monitor** (6 from 2 plastic sheets, 4 glass panes, a microchip and a data cable): a thin panel hung on a wall.
  - Six panels, three wide and two tall, all facing the same way, form one screen.
  - Run a data cable from any panel to a logic controller to link it; it re-checks the link every two seconds.
  - The screen lists every channel that has a sensor or is on: its colour and name, reading, a bar, a two-minute graph (a sample every 5 seconds) and ON/OFF.
  - Loose panels show a standby pattern.
- **Alarm Klaxon** (orange stained glass, 2 note blocks, a microchip, 2 plastic sheets and a data cable): switched by a controller like a relay. While on, its dome lights and it sounds every 1.5 seconds, loud enough to hear across a base.
- **Control Remote** (copper wire, a processor, 2 plastic sheets and a stone button):
  - Use it on a logic controller to bind it.
  - Sneak and use it in the air to pick the next channel.
  - Use it in the air to flip that channel on the controller, which switches its relays and alarms at once. The controller's rules may switch it back the next time their condition holds.
  - It works in the same dimension, within 256 blocks, with the controller loaded.
- Advancement **Mission Control** (build control monitor panels). Handbook page "The Control Room".

## Connections
- Input producer: plastic, optical fibre (glass chemistry), microchips and processors (electronics), the network terminal.
- Output consumer: every machine with a redstone mode, and anything else redstone drives.
- Technology connection: Fabric energy, fluid and item storage lookups; the redstone modes from machine control. Magic connection: none.
- Required vs optional: optional.

## Balance and automation
Control only switches machines that already exist; it makes and stores nothing. It cannot run anything faster than its own machines allow.

## Multiplayer and persistence
- Everything runs on the server.
- Rules and channel states are saved on the controller. Channels and relay states are block states.
- The controller sends its readings, states and history to nearby clients when they change (block entity update packets), at most once a second. The monitor's text is drawn on the client from that data.
- The remote stores its controller as a position and dimension on the item.
- Only players who may build at the controller can change its rules.

## Performance
- Sensors re-read every 20 ticks.
- A controller walks its network every 20 ticks, staggered by position, along at most 1,024 cables. It stops at unloaded chunks.
- Cables have no block entity and no tick.
- A monitor's anchor walks the cables from its six panels every 40 ticks. Other panels do nothing.

## Dependencies and assets
No new dependencies. All textures (cable, sixteen channel lamps, relay indicator, key strip) are drawn in `tools/control_electronics.py`. Models reuse the existing electric casing textures. All original.

## Verification
- `tools/check_mod_data.py`: the intervals, rule count, threshold step and cable limit match `tools/control_electronics.py`; the screen's channel colours match the lamps.
- Game test `logicControllerSwitchesRelays` (CI): a sensor on a battery box, through a data cable to a controller with two rules and on to a relay:
  - a full battery switches the relay on, and the relay gives a redstone signal;
  - the sensor gives 15;
  - at 50% the relay stays on (the dead band);
  - at 10% it goes off and the signal stops.
- Game test `controlRoomWorks` (CI):
  - six panels form one screen (parts 1 to 6) and link over the cable to the controller;
  - the remote switches a relay and the controller's channel on;
  - the remote sounds the alarm on another channel, then silences it.
- Not run: the screens and the alarm sound in game, client play.

## World and event applicability
Not applicable.

## Rollout and open questions
- Possible later: timers and counters as rule conditions, higher-tier controllers with more rules.
