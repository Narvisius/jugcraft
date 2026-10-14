# Vent or hold: the stop-instead setting for vented byproduct tanks

Status: implemented (CI green on the contributor's fork; pending review).
Stacked on [biogas power](industrial-biogas-power.md) and the branches below it, down to the [Industrial Fermenter](industrial-fermenter.md), which added vented tanks.

Proposal issue: none. TODO item: "Full CO2 capture storage defaults to continued production/excess venting, with optional stop-instead". The owner's plan ([chemistry and fuels plan](industrial-chemistry-and-fuels-plan.md), decision 8) settled the default and asked for an optional stop-instead setting, leaving the control's details to design. The control below is this change's choice, for review.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, every form with an optional byproduct gas
Primary specialty and supported player role: factory control

## Player experience

- Any machine whose form vents a byproduct tank (today the Industrial Fermenter's carbon dioxide) shows a full-width **Excess: vent** button on its screen, above Pause and Cancel. Its tooltip explains both choices.
- **Vent** (the default): a full byproduct tank keeps what fits and the rest is released, so the work goes on. This is unchanged.
- **Hold**: the tank keeps its gas, and when it is full the machine stops like any full tank, with "output full" naming that tank. Pipe the gas away, or switch back to vent, and it goes on.
- The choice belongs to the machine and is saved with it.
- Machines without a vented tank show no switch, and the button does nothing for them.

## Connections

- **Existing input producers / output consumers:** unchanged. It only decides what a full vented tank does.
- **Technology connection:** the shared form foundation. The switch is a property of `FormMachineBlockEntity`, so every later form declared with `vent(...)` gets it with no further code.
- **Magic connection:** none.
- **Reachable entry path / circular unlocks:** none; it needs nothing.
- **Required vs optional:** optional; the default is the plan's.

## Balance and automation

- Holding never makes or saves anything: a held batch simply does not start until its byproduct fits. Venting releases only what does not fit, counted once, as before.
- Switching while a batch runs takes effect when that batch finishes; gas already released does not come back.
- Comparators and pipes work as before.

## Choices made here for review

- **The control:** a button on the machine's own screen, cycling between "Excess: vent" and "Excess: hold". It is one switch for all of a machine's vented tanks, since none has more than one yet.
- **Where it is:** above Pause and Cancel, so the terminal's text ends one row sooner on such machines.

## Multiplayer and persistence

- The server decides everything. The screen sends only a menu-button number; the server toggles only for a machine whose form vents, and only through the open menu.
- Saved data: one new boolean, `hold_vented`, absent (vent) in older saves. No new registrations.
- Not done: two-client and dedicated-server play.

## Dependencies and assets

None. Three new lang strings: `container.jugcraft.form.excess_vent`, `excess_hold` and `excess_tooltip`.

## Verification

Locally, without Gradle or a game:
- `python tools/generate_material_data.py` regenerated the lang file.
- `python tools/check_mod_data.py` and `python scripts/check_repository.py`: passed.

For CI, server game test `IndustrialFermenterGameTests.holdingTheGasStopsInsteadOfVenting`: the Fermenter vents by default; its screen's button switches it to hold and the choice is saved; holding, a full carbon dioxide tank stops it with "output full" on that tank and nothing starts; switched back to vent, it goes on. The existing `aFullGasTankVentsButAFullAlcoholTankWaits` covers the default. `IndustrialFermenterClientGameTests` screenshots the screen with the new button.

### Results

- **Run [38452018763](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38452018763) on the contributor's fork (commit a7969d1d): green.** Everything compiled; the data checks passed; every server game test passed, `holdingTheGasStopsInsteadOfVenting` among them; the build without optional integrations and all five client shards passed, `IndustrialFermenterClientGameTests` included.
- The client screenshots have not yet been looked at by a person.

No survival playtest, two-client test or performance measurement has been done.

## Rollout and open questions

- The control's form awaits the owner's review.
- Still to come: per-tank switches if a form ever vents more than one tank, and a redstone or logistics control if the owner wants one.
