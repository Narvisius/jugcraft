# Diesels make real power (27 October 2026: built in r147; see docs/features/nuclear.md, "Losing power")

Picked by Justin at the next-task picker. Already built earlier (don't redo): fuel burn from the day tank
(`nuclear/DieselFuel.java`, round 14: 110 L/h idle + 0.232 L/kWh, stops when dry), the load sequencer's kW
(`DieselFuel.kw`, STEP_KW 300/1500/1100/900/1000/900/700/400 every 5 s from the breaker's close), panels, modes, 86G,
E-stop, starting air, CO2 (diesel room rebuild parts 1-3), SAT-1 carrying the house load while the generator is off (yard).

## Justin's answers (27 October 2026)
- What depends on the diesels: "Safety loads need real power": lose offsite power and the coolant pumps coast down
  (reactor trips), the diesels start themselves and carry the safety pumps (SI, aux feed, cooling water); if the diesels
  don't run, those pumps stop and the core heats on decay heat.
- What cuts offsite power: "Storms and a test switch": offsite power is normally there; a lightning strike on the
  switchyard in a thunderstorm can drop it for a few minutes; a test switch in the control room. A plant with no grid can
  still start up.

## What the code has today
- `BoardPlant` signal `st.offsite` (and st.reserve, swyd.rat_a/b) is always true: no loss of offsite power anywhere.
- Plant loads draw no real power (`LoadCentre`: feeder amps worked out from the plant; only cables' JE is real).
- `ReactorCore.tick`: a stopped coolant pump's loop coasts to 0 flow and carries no heat: no natural circulation, so with
  all four RCPs stopped decay heat (6.5%) heats the core about 0.5 C/s to fuel damage in about half an hour. Heat removal
  is `min(heat, cooling * waterShare)` (the condenser's water); no automatic atmospheric relief (ARVs only by hand).
- AFW: `afwRunning` bits (motor A for SGs 1-2, motor B for 3-4, turbine-driven bit 2 on steam while hot); started on
  low-low level, both MFPs lost, or the boards. Condensate pumps `cpRunning`; MFPs turbine-driven.
- Safety injection (`ControlBoard`) starts both diesels (`EquipmentPlacement.dieselSwitch(..., REMOTE)`, needs AUTO).
- Water works: service water pumps; `lossOfServiceWaterTripsTheDiesel` game test.
- DC rooms (`DcRoom`): battery charge; chargers on the 480 V buses.

## Design outline (to propose to Justin, not shown yet)
- Offsite power state on the switchyard's Yard Controller: lost for a few minutes by a lightning strike on the
  switchyard in a thunderstorm (a real bolt on a gantry), or by the control room's test (probably the electrical board's
  existing startup transformer breaker switches made live: TRIP = loss of offsite power, CLOSE = back).
- Buses: non-safety live = generator online (UATs) or offsite (SAT-1); safety bus A/B live = non-safety live or its EDG
  running with its breaker closed.
- LOOP at power: generator breaker opens, turbine trip, reactor trip (P-9), UATs dead; RCPs coast down; condensate and
  circ water pumps stop.
- Diesels auto-start on bus undervoltage in AUTO; breaker in 5 s; the sequencer's steps start the real loads (SW pump,
  CCW, motor AFW, SI if actuated).
- ReactorCore needs: natural circulation (a few % flow a loop with its SG tubes covered and the core hot) and automatic
  ARV / safety valve relief to atmosphere for decay heat (about 12% of rated) when the condenser can't take it; the SGs'
  water then needs AFW (motor AFW on the diesel buses, TDAFW on steam, maybe needing DC).
- Offsite back: buses re-energize from SAT-1; diesel breakers open; diesels run unloaded until stopped; RCPs restarted by
  the player.
- Boards: st.offsite real; LOSS OF OFFSITE POWER and bus undervoltage annunciators; the overview already shows LOST.

## Built (r147, 27 October 2026)
Justin left the four design questions on "no preference", so the recommended answers were built: about half the
storms lose the grid (one strike in eight trips the lines), for 2 to 5 minutes; the RES XFMR A/B breaker switches are
the test switch; the safety buses stay on their diesels until RES XFMR A/B is closed. Corrections to the notes above,
found in the code: decay heat (exp, 5 minutes) couldn't damage the fuel at all, so it now falls as a real core's
does; SAT-1 carried no real power (it now feeds the load centres with the generator off); safety injection doesn't start
by itself (still manual); the sequencer had no aux feed step (the motor-driven pump loads at step 6, the CONT SPRAY slot).
Not done: SI on its own signal, containment spray, the batteries' own run-down time against the TDAFW (they run down
as the DC rooms already model).
