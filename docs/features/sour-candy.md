# Sour candy and corn syrup

Status: implemented on the contributor's fork, pending review.

Proposal issue: none. TODO item: "Connect HCl-assisted corn-starch hydrolysis to finished glucose syrup; acetic acid/vinegar to acetate monomers and PVAc adhesives/appropriate fibre applications; citrus/citric acid to preservation, sour candy and drinks; food-grade phosphoric acid to cola." This change delivers the glucose syrup and the citric acid's sour candy; the rest of that item remains open.

**This change's choices:** the quantities, the sour flavour's effect and the syrup's sugar yield, listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, after the Chemical Reactor and the orchards
Primary specialty and supported player role: chemistry and cooking; a player with an orchard or a corn field and a Chemical Reactor

## Player experience

Two new ingredients come from the existing Chemical Reactor:

- **Citric acid.** Four lemons steeped in 250 mB of water crystallize into two citric acid; four oranges give one.
- **Glucose syrup.** Four corn and an empty glass bottle with 50 mB of hydrochloric acid give a bottle of glucose syrup. It stacks to 16.

Glucose syrup crafts into **three sugar** and gives back its bottle. Corn growers can then sweeten anything that takes sugar, the Candy Kettle included.

Citric acid is the Candy Kettle's new **Sour** flavour, added like the others (up to two flavours a batch). Every candy made with it is named "Sour" and gives **Jump Boost for 10 seconds** when eaten, along with any other flavour's effect. An undyed sour candy sets pale lemon yellow.

## Connections

- **Input producers:**
  - lemons and oranges (the orchards);
  - corn (the farm);
  - water;
  - hydrochloric acid (the Chemical Reactor, or the starter factory's Chemical Infuser).
- **Output consumers:**
  - the Candy Kettle (citric acid as the Sour flavour);
  - every sugar recipe (glucose syrup crafts into sugar).
- **Machines:** the existing Chemical Reactor and Candy Kettle, so no new machine.
- **Optional:** yes. Nothing in industrial progression needs either item.

## Balance

- 4 lemons give 2 citric acid, enough to flavour two kettle batches (one flavour item each).
- 4 corn, a bottle and 50 mB of acid give 3 sugar. That is a slower, costlier route than sugar cane, worthwhile for farms with spare corn.
- **No loops:** neither product turns back into its inputs.

## Multiplayer and persistence

- **New registrations,** stable from now on:
  - the items `jugcraft:citric_acid` and `jugcraft:glucose_syrup`;
  - the recipes `chemical_reaction/citric_acid_lemon`, `chemical_reaction/citric_acid_orange`, `chemical_reaction/glucose_syrup` and `sugar_from_glucose_syrup`;
  - the item tag `jugcraft:candy_flavours/sour`.
- The `SOUR` flavour is appended to the end of `CandyFlavour`. The flavours are saved by name, so existing kettles, trays and candies keep theirs.
- The server applies the candy's effects.

## Dependencies and assets

There are no new dependencies. Both icons are drawn by `tools/petro_textures.py`: a heap of white crystals with a faint lemon cast, and a stoppered glass bottle of pale-gold syrup. No texture is copied.

## Verification

**Local checks** (no Gradle or game): `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py` passed. The icons were inspected at 8x.

**Server game tests for CI** (`SourCandyGameTests`):

| Test | What it checks |
| --- | --- |
| `theReactorMakesCitricAcidAndGlucoseSyrup` | The three reactor recipes load and give two, one and one. |
| `glucoseSyrupCraftsIntoSugar` | The syrup crafts into three sugar and leaves its glass bottle. |
| `citricAcidMakesSourCandy` | Citric acid is the Sour flavour, and sour hard candy gives Jump Boost. |

### Results

Fork Build run [38824507444](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38824507444) on commit `c6c499a3` passed: the mod job (compilation and every server game test, these three included), the optional-integrations-absent job and all five rendered-client shards. No human gameplay or multiplayer test was run.

## Rollout and open questions

- The quantities, the Jump Boost effect and the syrup's three sugar await the owner's review.
- Preservation and drinks for citric acid, and the acetate/PVAc adhesives, are separate items. Phosphoric-acid cola: [cola-and-mmh.md](cola-and-mmh.md).
