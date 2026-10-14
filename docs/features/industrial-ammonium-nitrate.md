# Ammonium nitrate: fertilizer and explosive filling from nitric acid and ammonia

Status: implemented (CI green on the contributor's fork; pending review).
Stacked on [gasification residue](industrial-gasification-residue.md) and the branches below it, down to the [Bulk Chemical Reactor](industrial-bulk-chemical-reactor.md).

Proposal issue: none. TODO item: "Extend nitric/ammonia chemistry for ammonium-nitrate fertilizer and compatible finished game ammunition/rocketry products".

**This change's choices,** made at the contributor's request ("just pick reasonable numbers") and listed below for review:
- the machine;
- the quantities;
- the fertilizer strength;
- the explosive uses.

The owner's separate decision on named fertilizers and crop groups (TODO: "Define selected named nitrogen/phosphate fertilizers…") is not pre-empted. Ammonium nitrate is an ordinary fertilizer item here, with no crop-group rules.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, nitrogen chemistry
Primary specialty and supported player role: chemical processing for farming and munitions

## Player experience

**Making it.** In the Bulk Chemical Reactor:
- pipe nitric acid into the side inlet and ammonia into the lid inlet;
- 250 mB of acid and 125 mB of ammonia give **four ammonium nitrate** at the chute in 100 ticks.

**Ammonium nitrate is three things:**
- **A fertilizer.** Used on the ground or a crop, it works like superphosphate over the same 5×5 area, but gives each crop three doses of bone meal instead of two.
- **Grenade filling.** In the grenade recipe it takes guncotton's place: an iron nugget, three steel plates and one ammonium nitrate make 4 grenades.
- **Rocket filling.** In the Rocket Workshop, 2 solid propellant, 2 ammonium nitrate and a rocket casing make 4 HE rockets, in place of guncotton.

**Where to find it.** The Bulk Chemical Reactor's handbook page describes it, and JEI lists all three recipes.

## Connections

- **Existing input producers:**
  - nitric acid (the Synthesis Converter's Ostwald recipe);
  - ammonia (its Haber-Bosch recipe).
- **Existing output consumers:**
  - crops (the fertilizer behaviour);
  - grenades;
  - HE rockets.
- **Technology connection:**
  - the Bulk Chemical Reactor gains the capability `jugcraft:nitrate_neutralization`;
  - the item reuses `FertilizerItem`'s shared growth rule.
- **Magic connection:** none.
- **Reachable entry path:** the Bulk Chemical Reactor and both reagents come before it. Nothing it makes is needed to build anything that makes it.
- **Required vs optional:** optional. Superphosphate, ammonium phosphate and guncotton are unchanged.

## Balance and automation

- **Units.** HNO3 + NH3 → NH4NO3. 250 mB of liquid acid is 125 reaction amounts, matched by 125 mB of ammonia gas.
- **Cost against ammonium phosphate.** Counting the ammonia that goes into the nitric acid, one batch takes about 250 mB of ammonia in all.
  - It gives 4 items × 3 doses = 12 doses.
  - Ammonium phosphate also gives 12 doses (6 × 2) from 250 mB of ammonia plus 2 phosphate.
  - Ammonium nitrate saves the phosphate but costs oxygen and a second machine. Neither recipe is strictly better.
- **Explosives.** One ammonium nitrate replaces one guncotton at the same yield.
  - Guncotton costs cotton and nitric acid.
  - Ammonium nitrate costs nitric acid and ammonia, so a base without cotton fields can still make grenades.
- **Energy.** 8,700 JE a batch in the expanded profile.

## Choices made here for review

- **The machine:** the Bulk Chemical Reactor. It already takes a liquid and a gas and has an item chute.
- **The quantities:** 250 mB of acid + 125 mB of ammonia → 4 items, in 100 ticks.
- **The fertilizer:** three doses over the 5×5 area.
- **The explosive uses:** guncotton's place in the grenade and the HE rocket.
- **Not built:** ANFO with fuel oil, prill-tower models, and storage hazards.

## Multiplayer and persistence

- The server decides everything.
- New registrations, stable from now on:
  - the item `jugcraft:ammonium_nitrate`;
  - the capability `jugcraft:nitrate_neutralization`;
  - the recipes `chemical_reaction/bulk_ammonium_nitrate`, `grenade_from_ammonium_nitrate` and `rocket_assembly/he_rocket_ammonium_nitrate`.
- Not done: two-client and dedicated-server play.

## Dependencies and assets

None new.
- **The icon:** drawn at 16×16 in `tools/petro_textures.py` in the item icon manner. It shows a heap of small round white prills, outlined in their own darkest tone and lit from the top left.

## Verification

Locally, without Gradle or a game:
- `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py`: passed.

For CI, server game test `BulkChemicalReactorGameTests.nitricAcidAndAmmoniaGiveAmmoniumNitrate`:
- 250 mB of nitric acid and 125 mB of ammonia are used up;
- the batch costs exactly 8,700 JE;
- four ammonium nitrate leave at the chute;
- the item is a fertilizer;
- it crafts 4 grenades.

The existing `itRunsItsOwnRecipesAndIsBuiltFromItsBill` still checks that every recipe the reactor runs is one of its own.

### Results

- **Run [38463142878](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38463142878) on the contributor's fork (commit f365ca00): green.** Everything compiled; the data checks passed; every server game test passed, `nitricAcidAndAmmoniaGiveAmmoniumNitrate` among them; the build without optional integrations and all five client shards passed. Later commits on the branch change only documentation.

No survival playtest, two-client test or performance measurement has been done.

## Rollout and open questions

- The quantities, fertilizer strength and explosive uses await the owner's review.
- How it fits the owner's later named-fertilizer and crop-group design is still open.
