# Four-ingot ore and bioethanol

Status: implemented on `feature/chemistry-26` (batch 26), stacked on `feature/tools-25`, awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 2 October 2026, shared Mekanism jars and picked "4x chemical ore" and "Bioethanol fuel" from the list Claude offered.
Owner: jimbozoomer-byte
Target milestone and tier: industrial chemistry (sulfuric acid) and farming
Primary specialty and supported player role: chemistry and farming
Design inspiration: Mekanism (aidancbrady and team, MIT): its chemical ore processing and its bio-generator. Only the ideas are taken; no code, data or art. Both features run in the existing **chemical reactor**, with no new machine, following the owner's wish to keep the machine count down.

## Player experience
- **Acid leaching:** put an ore in the chemical reactor with sulfuric acid.
  - 250 mB of acid gives **four washed ores**, so four ingots once pulverized and smelted.
  - This is the best ore route: crusher 2, washer 3, acid 4.
  - It covers every ore that has a washed form: copper, iron, gold, tin, zinc, lead, silver, nickel, tungsten, uranium.
- **Bioethanol:** eight crops in a bucket of water ferment into 250 mB of **bioethanol**, a new fuel with a bucket.
  - Crops: wheat, sugar cane, potatoes, carrots, beetroot, sweet berries, melon slices or apples (`#jugcraft:fermentable`).
  - It burns in the **gas turbine** (192 JE/mB, like refinery gas) and the **advanced engine** (256 KE/mB, a little below diesel).
  - This gives farming (the crop harvester and sprinkler) a power use.
- An advancement (Moonshine), handbook text on the chemical reactor, ore processing and fuel value pages.

## Connections
- Input producers: sulfuric acid (chemical reactor), ores; crops (crop harvester, any farm), water.
- Output consumers:
  - washed ores go to the pulverizer;
  - bioethanol goes to the gas turbine and advanced engine, through pipes and tanks.
- Technology connection: industrial chemistry, farming, fluid fuels. Magic connection: none.
- Required vs optional: optional; both are better routes, not required ones.

## Balance and automation
- **Leaching** uses half a sulfur dust per ore (2 sulfur dust make a bucket of acid, four ores' worth). The audit allows ore recipes up to `ORE_LEACHING_MULTIPLIER` = 4, and only for ore inputs.
- **Bioethanol:** 8 crops + 9,600 JE give 250 mB (48,000 JE in the turbine), about 4,800 JE a crop after fermenting.
  - One harvester field averages roughly 10–20 JE/t: a renewable trickle, a few solar panels' worth.
  - The diesel generator and diesel engine do not take it.
- No fluid from nothing: fermenting turns 1,000 mB of water into 250 mB of fuel.

## Multiplayer and persistence
Server-side recipes and one new fluid (`bioethanol`, with block and bucket). Nothing new is saved.

## Dependencies and assets
No new dependencies. The bioethanol fluid and bucket textures are drawn by `tools/petro_textures.py` like the other fluids.

## Verification
- `tools/check_mod_data.py`: fluid recipes, the fluid list against Java, fuel values against `FluidFuels`, the ore bonus audit.
- Game tests: `reactorLeachesOreFourTimes` (an iron ore and 250 mB of acid → 4 washed iron ore) and `reactorFermentsBioethanol` (8 sugar cane and water → 250 mB; fuel values in the turbine and engine; refused by the diesel generator).
- Not run: client play, two players.

## World and event applicability
Not applicable.

## Rollout and open questions
- A dedicated bio-generator was left out on purpose: the gas turbine and engine already burn liquid fuels.
