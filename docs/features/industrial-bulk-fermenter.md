# Bulk Fermenter: the Industrial Fermenter's 5×5×6 variant

Status: implemented on the contributor's fork; CI green; pending review.
Stacked on [copper entry](industrial-copper-entry-aluminum.md) and the branches below it, down to the [Industrial Fermenter](industrial-fermenter.md).

Proposal issue: none. TODO items:
- the Fermenter item's remaining "5×5×6 variant";
- "Larger chemical machines gain higher throughput and lower energy per unit; upgraded small machines remain useful".

**From the owner's plan:**
- the size and look come from the art brief ([Industrial Fermenter](industrial-machine-models-and-textures.md#industrial-fermenter)): "larger variant 5×5×6 … The bulk version enlarges this same vessel and its readable fittings, without a second unrelated brewery model";
- the bulk profile is the foundation's selected one.

**This change's choice:** the bill, listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, the bio line, after the Industrial Fermenter
Primary specialty and supported player role: farming into fuel at scale

## Player experience

The **Bulk Fermenter** is a 150-block landmark: the Industrial Fermenter's stepped stainless vessel, enlarged, on four stout legs.

It has the same features as the smaller one:
- the cream jacket with its amber level strip;
- the ringed manway;
- the domed crown with its turning agitator drive;
- the feed hatch at the top on the left;
- the broth outlet with its red-wheeled valve, low at the front;
- the gas flange, high at the back;
- the water inlet, low at the back left;
- the services box with the power socket, at the back right;
- the instrument box, which is the controller, at the front left.

What differs is capacity:
- **Same recipe.** It ferments exactly like the Industrial Fermenter: eight crops and a bucket of water give **375 mB of bioethanol and 250 mB of carbon dioxide**.
- **Four batches at once**, from 16,000 mB tanks, at **80% of the draw a batch**: 77 JE/t instead of 96.
- **Venting and holding.** The carbon dioxide tank vents what is not collected, and the screen's vent/hold switch works as on the smaller machine.
- **Guides.** Its Engineer's Handbook page describes it (listed in the handbook with the [Large Bio Generator](industrial-large-bio-generator.md) change).

## Connections

- **Existing input producers:** farm crops (`#jugcraft:fermentable`); water.
- **Existing output consumers:**
  - bioethanol for the Bio Generator, premium gasoline and the reactor's medical recipes;
  - carbon dioxide for the Infuser's methanation, and through it the Combined Cycle Power Plant.
- **Technology connection:**
  - the shared form foundation's bulk profile and vent option;
  - the existing `jugcraft:controlled_fermentation` capability and its recipe, so no new recipe was needed.
- **Magic connection:** none.
- **Reachable entry path:** its bill contains an Industrial Fermenter, so the smaller machine comes first. Nothing it makes is needed to build it.
- **Required vs optional:** optional. The Industrial Fermenter stays the entry machine, and upgrade cards keep it useful.

## Balance and automation

- **Throughput.** Four lanes give four times the Industrial Fermenter's rate.
- **Energy.** A batch costs 7,700 JE instead of 9,600, which is 20% less energy per litre of alcohol.
- **Same amounts.** It makes no more alcohol or gas per crop than the Industrial Fermenter.
- **Automation:**
  - conveyors reach the hatch;
  - pipes reach the inlet and both outlets;
  - the 16,000 mB tanks buffer a long run.

## Choices made here for review

- **The bill:** an Industrial Fermenter, two Steel Tanks, four steel plates, an electric motor and a basic circuit (`TFT / PMP / PCP`).
- **JEI.** It has its own recipe page with the Fermenter's recipes; main's recipe view gives every form one.

## Multiplayer and persistence

- The server decides everything. The ports check what goes in, and nothing can be put in at the outlets.
- New registrations, stable from now on: the block and item `jugcraft:bulk_fermenter`.
- Not done: two-client and dedicated-server play.

## Dependencies and assets

None new. The model is authored in `tools/industrial_forms.py` (`bulk_fermenter()`) from the same shared textures as the Industrial Fermenter:
- dull stainless steel, the cream jacket, gunmetal bands, the amber strip, the green and amber lamps;
- the bioethanol, carbon dioxide and water stills on their pipes;
- the turning pulley (`bulk_fermenter_drive` in `machine_rotor_quads.json`).

Every port sits at the middle of its block's face.

## Verification

Locally, without Gradle or a game:
- `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py`: passed.
- An offline render of the part models was inspected from six directions.

For CI, server game tests in `BulkFermenterGameTests`:
- `itStandsWholeAndOnlyItsPortsAnswer`: 150 blocks in every orientation, and only its five ports answer.
- `wheatFermentsIntoBioethanolAndCarbonDioxide`: 375 mB and 250 mB for exactly 7,700 JE.
- `fourBatchesRunSideBySide`.
- `everyPartLightsWhileItWorks`.
- `itRunsTheFermentersRecipeAndIsBuiltFromItsBill`.

Client game test: `BulkFermenterClientGameTests` takes four views and two screens.

### Results

- An earlier run ([38472612599](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38472612599)) failed only on the Combined Cycle Power Plant's client test underneath, fixed in [factory payoffs](industrial-factory-payoffs.md).
- **Run [38494571370](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38494571370) (commit cc075f97) passed:** the build, the data checks, every server game test (`BulkFermenterGameTests` included) with and without optional integrations, and all five client test shards (`BulkFermenterClientGameTests` included).

No survival playtest, two-client test or performance measurement has been done.

## Rollout and open questions

- The bill awaits the owner's review.
- Still to come: milling/mashing, distillation and dehydration.
