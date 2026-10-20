# Silicone rubber

Status: implemented on the contributor's fork, pending review.

Proposal issue: none. TODO item: "Expand process-specific reagents, useful rigid/flexible/protective/heat-resistant polymers and advanced ceramic linings/insulators/cutting/precision parts. …" This change adds the first heat-resistant polymer, silicone rubber.

**This change's choices:** the quantities and silicone's first use (gaskets), listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, after the Chemical Reactor, chlorine, methane and silicon
Primary specialty and supported player role: chemistry and polymers; a player making gaskets without the oil refinery's butadiene

## Player experience

- **Methyl Chloride** (a gas, kept in tanks and pipes). In the Chemical Reactor, **500 mB of methane** (first tank) and **500 mB of chlorine** (second tank) give **500 mB of methyl chloride** (100 ticks).
- **Silicone Rubber.** In the Chemical Reactor, one **silicon** with **500 mB of methyl chloride** (first tank) and **250 mB of water** (second tank) gives **two silicone rubber** and **250 mB of hydrochloric acid** (160 ticks). Some of the chlorine comes back as acid.
- **Use:** the Polymer Molding Press's existing gasket mold presses **two silicone into three gaskets** (120 ticks at 32 JE/t), next to its two-rubber-into-two-gaskets recipe.

## Connections

- **Input producers:**
  - methane (the cleaned biogas from the anaerobic digester, or methanation);
  - chlorine (brine electrolysis);
  - silicon (quartz in the blast furnace);
  - water.
- **Output consumers:** gaskets, which seal pipes, gas tanks, the grappling hook and other machines. The hydrochloric acid goes to the existing acid consumers (copper leaching, magnesium chloride).
- **Machines:** the existing Chemical Reactor and Polymer Molding Press, so no new machine.
- **Optional:** yes. Rubber gaskets stay. Silicone is a route for players without crude oil.

## Balance

- 500 mB methane, 500 mB chlorine, one silicon and 250 mB water make two silicone and 250 mB acid. Two silicone press into three gaskets.
- That is a better gasket yield than rubber (two into two), paid for by a three-input chemistry chain.
- **No loops:** nothing it makes turns back into methane, chlorine or silicon. The acid returned (250 mB) is less than the chlorine used (500 mB).

## Multiplayer and persistence

- **New registrations,** stable from now on:
  - the gas `jugcraft:methyl_chloride`;
  - the item `jugcraft:silicone`;
  - the recipes `chemical_reaction/methyl_chloride`, `chemical_reaction/silicone` and `chemical_reaction/mold_silicone_gasket`.
- The gas is registered after every existing gas, so the textures drawn for the others are unchanged.

## Dependencies and assets

There are no new dependencies. The art is drawn by `tools/petro_textures.py`:

- the gas's swirl, from its own colours;
- the silicone: a rounded bale of red silicone with a pressed seam.

No texture is copied.

## Verification

**Local checks** (no Gradle or game): `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py` passed. The icon was inspected at 10x.

**Server game tests for CI** (`SiliconeGameTests`):

| Test | What it checks |
| --- | --- |
| `methaneAndChlorineMakeMethylChloride` | 500 mB methane and 500 mB chlorine give 500 mB methyl chloride. |
| `siliconBecomesSiliconeAndAcid` | One silicon, 500 mB methyl chloride and 250 mB water give two silicone and 250 mB hydrochloric acid. |
| `gasketMoldPressesSilicone` | The molding press's gasket mold presses two silicone into three gaskets. |

`CoastalMetalsGameTests.everyLoadedRecipePaysItsLedgerAndRetainsItsTool` runs every molding-press recipe in a real press, so it now also runs the silicone gasket recipe. Its expected count rose from 21 to 22.

### Results

The first fork Build run, [38973357673](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38973357673), failed only on that recipe count (22 found, 21 expected). All eight rendered-client shards passed in it. After the fix, fork Build run [38978639713](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38978639713) on commit `516684dd` passed on its second attempt. That covers the mod job (compilation and every server game test, this change's included), the optional-integrations-absent job and all eight rendered-client shards. The first attempt's client shard 5 failed only on the unrelated `PeepoPopulationClientTests` companion check timing out. No human gameplay or multiplayer test was run.

## Rollout and open questions

- The quantities and the gasket use await the owner's review.
- Other heat-resistant uses (high-temperature seals, cable insulation for furnaces) remain open.
