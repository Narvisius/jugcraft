# Aspirin

Status: implemented on the contributor's fork, pending CI and review.

Proposal issue: none. TODO item: "Plan acetylsalicylic-acid medicine with independently reachable precursors and defined game effects; keep pharmacy optional to industrial progression."

**This change's choices:** the precursors, quantities and the tablet's effect, listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, after the Chemical Reactor and the cider press
Primary specialty and supported player role: chemistry and farming; any player who wants a cheap healing tablet

## Player experience

Two new items join the [Field Chemistry](field-chemistry.md) medicines:

- **Salicylic acid.** The Chemical Reactor steeps four willow logs in 500 mB of water into two salicylic acid, or four birch logs into one.
- **Aspirin.** The Chemical Reactor acetylates two salicylic acid with one cider vinegar over 50 mB of sulfuric acid into four aspirin tablets.

Eating a tablet takes under a second. It gives **Regeneration I for 10 seconds** and **clears nausea**, and leaves every other effect alone (the antidote clears poison). Then the next tablet waits 30 seconds. Tablets stack to 16.

## Connections

- **Input producers:**
  - willow logs (Jugcraft's biomes);
  - birch logs (vanilla), so salicylic acid stays reachable when the biomes feature is off;
  - cider vinegar (the agriculture pantry's cider line);
  - sulfuric acid (the Chemical Reactor or the contact process).
- **Output consumer:** the player. Nothing in industrial progression needs aspirin.
- **Machines:** the existing Chemical Reactor, so no new machine.
- **Optional:** yes. Pharmacy is optional to every other branch.

## Balance

- 4 willow logs, 500 mB of water, 1 cider vinegar and 50 mB of acid give 4 tablets: 40 seconds of Regeneration I in all, and each tablet can be taken only every 30 seconds.
- It sits between golden apples (stronger, no cooldown) and the First Aid Kit (four hearts at once, 10-second wait).
- **No loops:** nothing it makes turns back into its inputs.

## Multiplayer and persistence

- **New registrations,** stable from now on:
  - the items `jugcraft:salicylic_acid` and `jugcraft:aspirin`;
  - the recipes `chemical_reaction/salicylic_acid_willow`, `chemical_reaction/salicylic_acid_birch` and `chemical_reaction/aspirin`.
- The server applies the effects and the cooldown.

## Dependencies and assets

There are no new dependencies. Both icons are drawn by `tools/field_chemistry.py`: a heap of white needle crystals and two scored white tablets. No texture is copied.

## Verification

**Local checks** (no Gradle or game): `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py` passed. The icons were inspected at 8x.

**Server game tests for CI** (`AspirinGameTests`):

| Test | What it checks |
| --- | --- |
| `theReactorMakesSalicylicAcidAndAspirin` | The three recipes load and give two, one and four. |
| `aspirinRegeneratesAndClearsNausea` | A tablet gives Regeneration I, clears nausea, leaves poison, and has a cooldown. |

### Results

Fork Build run [38805626684](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38805626684) on commit `d651da4a` passed: the mod job (compilation and every server game test, these two included), the optional-integrations-absent job and all five rendered-client shards. No human gameplay or multiplayer test was run.

## Rollout and open questions

- The quantities, the effect and the 30-second wait await the owner's review.
- Other medicines (the TODO's preservatives, adhesives and drinks) are separate items.
