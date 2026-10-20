# Progression tier fixes

Status: implemented on the contributor's fork, pending review.

Proposal issue: none. The owner asked for a check that no upgrade needs its own product to build, and that no later machine is worse than the earlier one it follows.

Owner: jimbozoomer-byte
Target milestone and tier: existing industrial polymer and bio-power lines, and the dark matter tools
Primary specialty and supported player role: factories and alchemy; players moving up a tier

## What was checked

- **Dead ends and self-gates:** every machine, tool and material was traced back from recipes to mined, farmed or vanilla things. Nothing needs its own output to be built, and nothing is unobtainable.
- **Later tiers against earlier ones:** each later machine was compared with the earlier one that makes the same product, by output per input and power per output.
- **Gear tiers:** damage, durability, armor and special properties were compared tier by tier.

Three clear problems turned up and are fixed here.

## Fixes

| Problem | Before | After |
| --- | --- | --- |
| The Polymer Extruder made fewer sheets per stock than the older Metal Press, and less insulation and hose per rubber than the older Molding Press, at more power | 4 stock -> 3 sheets, 3 insulation or 1 hose (4 rubber); 48 JE/t | 4 stock -> 8 sheets or 6 insulation; 3 rubber -> 1 hose; 24 JE/t. Films and profiles stay at 3, as only the extruder makes them |
| Cleaned biogas gave less power in the Bio-Generator than ordinary biogas, so the cleaning step lost power | 192 JE/mB | 268 JE/mB, the same as ordinary biogas. Bioethanol stays at 192 |
| Dark matter pickaxe, axe and hammer burned in lava and fire, while the earlier netherite tools survive it | burns | fire resistant |

Each extruder batch still trims one clean offcut. Four offcuts reclaim to one stock, so extruding and reclaiming never gains stock. Sheets cost 480 JE each from the extruder against 3,840 JE per batch of eight.

With cleaned biogas at 268 JE/mB a full digester batch leaves 252,400 JE per 16 farm items after processing, instead of 176,400. Upgrading cleaned biogas to methane with electrolytic hydrogen still costs more power than it returns, so there is no free-power loop.

The new numbers are provisional and flagged for the owner.

## Left as they are

These are deliberate trade-offs recorded in their own feature docs, not mistakes:

- the Electrolytic Cell against the Electrolytic Separator;
- the Fuel Cell against the Gas Burning Generator;
- contact-process sulfuric acid;
- Flow Battery transfer.

Smaller items the owner may want to look at:

- the carbon monoxide methanation variant runs at low priority;
- bulk hydrochloric acid uses a lot of water;
- the Polymerization Kettle against the Polymerization Reactor;
- the Pulverizer against the Crusher;
- the diamond bayonet equals the steel bayonet;
- the exosuit doc's knockback figure does not match the code.

## Connections

- **Input producer / output consumer:** unchanged. Sheets, insulation and hoses keep all their recipes; cleaned biogas still comes from the Biogas Cleaner.
- **Unlocks and failure behavior:** unchanged.

## Multiplayer and persistence

No new IDs. Recipe yields and a fuel value change; saved machines and items load unchanged.

## Verification

**Updated tests:**
- `PolymerGameTests`: eight sheets and 3,840 JE per batch;
- `AnaerobicGameTests`: 268 JE/mB with fuel credit across a restart, and the plant energy totals.

**Local checks:**
- `python tools/generate_material_data.py` regenerated the six extruder recipes, the handbook and the recipe viewer data;
- `python tools/check_mod_data.py` passed (2,885 material IDs, the data files and the recipe audit);
- `python scripts/check_repository.py` passed.

**Results:** fork [run 39191616535](https://github.com/mmme123-spec/jugcraftfork/actions/runs/39191616535) on 10aace3b2: green. The mod built, the generated-data and material checks passed, and the server game tests passed, including the updated polymer and anaerobic tests. All eight client shards passed. On the first attempt, client shard 7 failed before any test ran: the Fabric Loom build plugin could not be downloaded. Re-running that one job passed.

Not run: a human playing through the extruder or Bio-Generator.

## AI attribution

Implemented with Claude Opus 5.5 (Claude Code) for the contributor; the contributor is responsible for the output.


## Combined integration correction (20 October 2026)

Full-yield sheet, insulation and hose extrusion returns no offcut. Film/profile
batches retain their one offcut; reclaiming remains four offcuts to one stock.
This replaces the earlier claim that every improved-yield batch could also return
scrap. Tests account for each product in stock-equivalent units and verify that
a full sheet drawer blocks spending and eight sheets survive batch save/reload.
Integration validation is recorded in [the combined feature record](trees-chemistry-integration-oct20.md).
