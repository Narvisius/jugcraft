# PVAc wood glue

Status: implemented on the contributor's fork, pending review.

Proposal issue: none. TODO item: "Connect HCl-assisted corn-starch hydrolysis to finished glucose syrup; acetic acid/vinegar to acetate monomers and PVAc adhesives/appropriate fibre applications; …" This change delivers the vinegar → vinyl acetate → PVAc adhesive route and its first uses.

**This change's choices:** the quantities and the glue's two uses, listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, after the Chemical Reactor, ethylene and the Polymerization Reactor
Primary specialty and supported player role: chemistry and polymers; a player without slimes or cattle who needs sticky pistons or books

## Player experience

- **Vinyl acetate** (a gas, kept in tanks and pipes). In the Chemical Reactor, two cider vinegar (its acetic acid) with **500 mB of ethylene** (first tank) and **250 mB of oxygen** (second tank) give **500 mB of vinyl acetate**. The vinegar's bottles are used up.
- **PVAc Wood Glue.** In the Polymerization Reactor, 500 mB of vinyl acetate gives **four pots** of white wood glue.
- **Uses:**
  - three paper and one glue craft into a **book**, without leather;
  - a piston and one glue craft into a **sticky piston**, without a slime ball.

## Connections

- **Input producers:**
  - cider vinegar (the agriculture pantry's cider line);
  - ethylene (the refinery's ethylene separation, or dehydrated bioethanol);
  - oxygen (the air separation unit or water electrolysis).
- **Output consumers:**
  - books (enchanting, bookshelves, lecterns);
  - sticky pistons (redstone).
- **Machines:** the existing Chemical Reactor and Polymerization Reactor, so no new machine.
- **Optional:** yes. Leather books and slime-ball sticky pistons stay.

## Balance

- Two cider vinegar, 500 mB of ethylene and 250 mB of oxygen make four glue: four books or four sticky pistons.
- That costs far more than a slime ball once slimes are farmed. It is a reliable industrial route for players without slime chunks or swamps, and a leather-free one for books.
- **No loops:** nothing it makes turns back into vinegar, ethylene or oxygen. Fluid out (500 mB) never exceeds fluid in (750 mB).

## Multiplayer and persistence

- **New registrations,** stable from now on:
  - the gas `jugcraft:vinyl_acetate`;
  - the item `jugcraft:pvac_glue`;
  - the recipes `chemical_reaction/vinyl_acetate`, `polymerization/vinyl_acetate`, `book_from_pvac_glue` and `sticky_piston_from_pvac_glue`.
- The gas is registered after every existing gas, so the textures drawn for the others are unchanged.

## Dependencies and assets

There are no new dependencies. The art is drawn by `tools/petro_textures.py`:

- the gas's swirl, from its own colours;
- the glue: a tall white squeeze bottle with a blue label band and a tapered orange nozzle.

No texture is copied.

## Verification

**Local checks** (no Gradle or game): `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py` passed. The icon was inspected at 8x.

**Server game tests for CI** (`PvacGlueGameTests`):

| Test | What it checks |
| --- | --- |
| `vinylAcetatePolymerizesIntoGlue` | The reactor recipe gives 500 mB of vinyl acetate; the polymerization recipe gives four glue. |
| `glueBindsBooksAndStickyPistons` | Three paper and glue craft a book; a piston and glue craft a sticky piston. |

### Results

Fork Build run [38870250823](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38870250823) on commit `e1092c00` passed on its second attempt; the first attempt's optional-integrations-absent job failed only on the unrelated NuclearPlantGameTests.reactorBuildingsKeepRadiationIn (a steel door missing from the plant blueprint): the mod job (compilation and every server game test, this change's included), the optional-integrations-absent job and all five rendered-client shards. No human gameplay or multiplayer test was run.

## Rollout and open questions

- The quantities and the two uses await the owner's review.
- Fibre applications (glued boards, fibreboard) and other adhesives remain open.
