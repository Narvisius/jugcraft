# Bio Generator (factory package 2, biological power)

Status: implemented (CI green on the contributor's fork; pending review). Built on the [shared machine foundation](industrial-machine-foundation.md) and its generator forms, beside the [Gas Burning Generator](industrial-gas-burning-generator.md).
Proposal issue: none. The TODO list's "separate lower-output Bio-Generator for cleaned biogas and bioethanol" (the bioethanol half). Its size and look are the [art brief](industrial-machine-models-and-textures.md#bio-generator); its bill is the [construction and operation](industrial-machine-construction-and-operation.md) proposal; its 96 JE/t on bioethanol is the [implementation plan](industrial-factory-implementation-plan.md)'s proposed value for testing.
Owner: jimbozoomer-byte
Target milestone and tier: steel-era power from farming
Primary specialty and supported player role: power generation for farms and factories

## Player experience

The **Bio Generator** burns bioethanol for power. It is three wide, four deep and three tall: a compact engine-generator on a thick skid, slower than the Gas Burning Generator and fed from fields instead of electrolysis.

**Building and placing.** It is crafted from a Steel Tank, an electric motor, a steel gear, a basic circuit and a Machine Casing. The plan's bill is eight steel plates, a Tinplate Tank and the rest; eight plates round a Tinplate Tank are a Steel Tank, so the grid takes one. Placed from the front, it fills 30 of the 36 positions in its envelope, with its instrument panel at the front left. Above the train, only the exhaust's run along the middle and the radiator at the back take blocks; the other top positions stay open. If any block it needs is taken, nothing is placed and the player is told what is in the way.

**What it looks like** (the art brief):
- a thick gunmetal skid with saddles under the train;
- at the front, the ringed olive combustion body with a round bolted inspection cover, thick mounting bolts and pale ceramic heat shields on its flanks;
- at the front left, the sheltered instrument panel with two gauges and the warm amber lamp;
- the guarded coupling, inside hazard-banded hoops, turning while it runs;
- the ribbed olive generator housing;
- at the back, the radiator with grilles front and back and its fan behind a chrome guard, turning while it runs;
- on the right, the low fuel-handling box: the liquid inlet low on its side, banded in bioethanol colour, and the capped gas inlet above it for biogas;
- on the left, the terminal box with the power socket, a mint lamp and copper terminals on ceramic insulators;
- a short black exhaust rising from the combustion body and folding back along the top to a rain cap.

**Ports.**

| Port | Where (seen from the front) | What |
| --- | --- | --- |
| Fuel in | right side of the bottom layer, third row from the front | bioethanol; nothing else goes in |
| Power out | left side of the bottom layer, third row from the front | its power, up to 1,024 JE a tick |

**Working.** It burns bioethanol at the gas turbine's 192 JE per mB, making 96 JE/t into its 60,000 JE store: a bucket lasts 100 seconds and gives 192,000 JE. Like the Gas Burning Generator, it makes only what its store has room for and keeps fuel in hand through saves. Its screen is the shared generator screen.

## Connections

- Existing input producer: bioethanol from the Chemical Reactor (eight fermentable crops and a bucket of water make 250 mB).
- Existing output consumer: every Jugcraft cable network.
- Technology connection: the diesel generator's family (`MachineKind.DIESEL_GENERATOR`) for its energy figures; its fuel is listed on the form, so the Diesel Generator itself still burns no bioethanol.
- Magic connection: none.
- Reachable entry path: a Steel Tank, a motor, a steel gear, a basic circuit and a casing; bioethanol needs only the Chemical Reactor and crops.
- Which connections are required vs optional: optional power; the gas turbine and advanced engine already burn bioethanol at higher outputs and cost.
- How the specialty stays useful alone: a farm can power itself from its own crops without petroleum or electrolysis.

## Balance and automation

- **Output**: 96 JE/t, below the Gas Burning Generator's 128 (hydrogen) and the gas turbine's 512. **Burn value**: 192 JE/mB, the gas turbine's existing value, so a bucket is worth the same in either and only the rate differs.
- **Primary fuel, not a loop.** Bioethanol is made by using up crops. `tools/check_mod_data.py` now treats a fuel whose every producing recipe uses up items as a primary fuel, like coal, and skips the electricity-loop audit for it. Fuels made only from fluids and electricity, such as hydrogen, are still audited.
- **Biogas**: not yet in the game. When the Anaerobic Digester adds cleaned biogas, the form gains it as a second fuel at the plan's proposed 64 JE/t, and the capped gas inlet becomes its port.

## Multiplayer and persistence

- The server decides placement, burning and the ports; the client draws the synced state and turns the coupling and fan.
- Saving uses the foundation's role-named tank (`fuel`), the energy and the JE in hand, and its form version (1).
- New registration: the block and item `jugcraft:bio_generator`.
- Not done: two-client and dedicated-server play.

## Dependencies and assets

None new. The model is built from the mod's own textures: the dieselpunk olive, stencil, gunmetal, chrome, rubber, hazard, grille, exhaust, gauge and lamp; the cream ceramic panel; ribbed steel; the mint lamp; copper and ceramic insulators; the flange plate; the power socket; and the bioethanol still for the inlet band. `tools/industrial_forms.py` authors it and slices it into 30 part models; the coupling and fan are quads in `machine_rotor_quads.json`.

## Verification

Locally, without Gradle or a game:
- `python tools/generate_material_data.py`: regenerated the blockstate, 60 part models, item model, loot table, crafting recipe, turning parts, name and handbook page.
- `python tools/check_mod_data.py` (with the primary-fuel rule) and `python scripts/check_repository.py`: passed.
- `javac` reported no syntax errors in the changed Java files; it could not compile them without Minecraft's libraries.
- The sliced part models were drawn together outside the game from six sides with a small z-buffer script.

For CI:
- **Server game tests** (`BioGeneratorGameTests`), 4 tests: it stands whole in all four orientations, its open positions stay open and only its two ports answer, taking bioethanol but not water, hydrogen or diesel; bioethanol burns at exactly 96 JE a tick with every millibucket accounted for; every part lights while it runs and only the controller gives off light; its fuel and bill.
- **Client game test** (`BioGeneratorClientGameTests`): `jugcraft_bio_generator` (front left, running), `_front`, `_back`, `_side`, `_dark`, `_screen` and `_store_full`.

### Results

- **Run [38333756008](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38333756008) on the contributor's fork (commit b015cc4e): green.** It compiled, the data checks passed, every server game test passed (the new Bio Generator tests among them), the build without optional integrations passed, and all five client shards passed, `BioGeneratorClientGameTests` included.
- The client screenshots were taken but have not yet been looked at by a person.

No survival playtest, two-client test or performance measurement has been done.

## World and event applicability

Not applicable: a placed machine.

## Rollout and open questions

- Cleaned biogas at 64 JE/t with the Anaerobic Digester; the 5×6×5 larger variant; exhaust animation; pollution once a pollution record exists.
- Open for review: the Steel Tank standing in for the plan's eight plates and Tinplate Tank in the grid.
- Reversible: removing it would need its blocks broken first. Its saves use only the foundation's form data.
