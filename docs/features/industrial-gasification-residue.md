# Gasification residue: the Coal Gasifier's ash, bound into concrete and asphalt

Status: implemented (CI green on the contributor's fork; pending review).
Stacked on [vent or hold](industrial-vent-or-hold.md) and the branches below it, down to [coal gasification](industrial-coal-gasification.md).

Proposal issue: none. TODO item: the Coal Gasifier package's remaining "cleaning residue".

**From the owner's plan** ([starter gas and acid factory](industrial-starter-gas-and-acid-factory.md), decisions 7 and 8):
- one mixed residue at first;
- one modest stabilized road/filler product, made with a paid binder;
- raw residue is not itself the stabilized product;
- more valuable recovery comes later.

**This change's choices,** made at the contributor's request ("just pick reasonable numbers") and listed below for review:
- where the residue leaves;
- how much residue each fuel leaves;
- the binders and yields.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, the coal line of the starter gas and acid factory
Primary specialty and supported player role: fuel processing into construction

## Player experience

- **The residue.** The Coal Gasifier's ash chamber now leaves one **Gasification Residue** for each coal or coal coke it gasifies.
  - The residue comes out of a new chute, low on the right side of the ash chamber.
  - Charcoal, which is nearly ash-free, leaves none.
  - The syngas amounts are unchanged.
- **Concrete.** Four residues, four cement and a water bucket (`CRC / RWR / CRC`) make **8 concrete**, the same yield as the gravel recipe.
- **Asphalt.** Eight residues around one asphalt binder make **8 asphalt road**, the same yield as gravel.
- **Guides.** The Engineer's Handbook page for the Gasifier mentions the residue, and JEI lists both recipes.

## Connections

- **Existing input producers:** the Coal Gasifier; cement (calcite, clay and sand); asphalt binder (vacuum distillation).
- **Existing output consumers:** concrete and asphalt blocks, both already in the game.
- **Technology connection:**
  - the shared form foundation, which gives the Gasifier an item output and an item-out port;
  - the existing construction chemistry and asphalt recipes.
- **Magic connection:** none.
- **Reachable entry path:** cement needs no oil, so a coal-only base can use its residue. Asphalt is for bases that also refine oil.
- **Required vs optional:** optional. Gravel still works, and the residue can simply be stored.

## Balance and automation

- **The binder is still paid.** The residue stands in for gravel, which is a cheap material, so this saves digging rather than adding value. It is the plan's "modest" product.
- **Single use.** Each residue is used once, in one recipe. There is no loop, and nothing turns concrete or asphalt back into residue.
- **No output-full stall.** The chute is the Gasifier's only item output. Like any output slot, a full stack stops the Gasifier until it is emptied, so pull from the chute with a conveyor.

## Choices made here for review

- **The source:** the Gasifier's ash chamber, rather than the Gas Cleanup Rack. The rack keeps its gas-only design and needs no item output.
- **The amount:**
  - one residue for each coal or coke;
  - none for charcoal.
- **The products:** concrete through cement, and asphalt through asphalt binder, each in place of gravel at the gravel recipe's yield. No new block is added.
- **Not built:**
  - numeric pollution;
  - the plan's later "more valuable recovery" from the same residue;
  - a machine-based stabilization step. Crafting is used for now.

## Multiplayer and persistence

- The server decides everything. The chute only gives items out.
- New registrations, stable from now on:
  - the item `jugcraft:gasification_residue`;
  - the recipes `jugcraft:concrete_from_gasification_residue` and `jugcraft:asphalt_from_gasification_residue`.
- **Saved Gasifiers.** The form gains one item output slot. Older saves load with it empty.
- Not done: two-client and dedicated-server play.

## Dependencies and assets

None new.
- **The chute:** drawn in `tools/industrial_forms.py` from the shared textures. It is an olive housing with a hazard band and the hopper-mouth texture.
- **The icon:** drawn at 16×16 in `tools/petro_textures.py` in the item icon manner. It shows dark grey clinker lumps with a few rust grains, each lump outlined in its own darkest tone and lit from the top left.

## Verification

Locally, without Gradle or a game:
- `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py`: passed.
- An offline render of the Gasifier was inspected from six directions.

For CI, server game tests in `CoalGasificationGameTests`:
- `theGasifierStandsWholeAndOnlyItsPortsAnswer`: nothing can be put in at the residue chute.
- `coalGasifiesIntoRawSyngas`: one residue leaves at the chute.
- `charcoalGivesLessSyngas`: charcoal leaves no residue.
- `theGasifierRunsItsOwnRecipesAndIsBuiltFromItsBill`: both binding recipes give 8 blocks.

### Results

- **Run [38462095868](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38462095868) on the contributor's fork (commit 24b2886e): green.** Everything compiled; the data checks passed; every server game test passed, the residue checks in `CoalGasificationGameTests` among them; the build without optional integrations and all five client shards passed.
- The client screenshots have not yet been looked at by a person.

No survival playtest, two-client test or performance measurement has been done.

## Rollout and open questions

- The residue amounts, its source and the products await the owner's review.
