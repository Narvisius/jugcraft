# Nitrogen chemistry: air separation, ammonia and nitric acid

Status: merged in #65 (batch 12). Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 1 October 2026, asked to save Claude's chemistry ideas and pursue them next ("I also loved your chemistry ideas can you save all those and we can pursue them next", then "lets begin your chemistry ideas"). This batch builds the first two from the backlog in [MACHINE_ROADMAP.md](../MACHINE_ROADMAP.md#idea-backlog-saved-by-the-owner-1-october-2026): the air separation unit and Haber–Bosch ammonia, with nitric acid (Ostwald) as the next step.
Owner: jimbozoomer-byte
Target milestone and tier: industrial chemistry, after the electrolytic cell (batch 5) and titanium (batch 6)
Primary specialty and supported player role: chemistry

## Player experience
- **Air Separation Unit** (2×2, six tall): a cold box with its distillation column.
  - It needs only power (64 JE/t). It liquefies air and splits it: 8 mB of **nitrogen** a tick out of its top row and 2 mB of **oxygen** out of its bottom row, four parts to one, like air.
  - It stops while either tank is full.
- **Synthesis Converter** (3 wide, 4 tall, 2 deep): a high-pressure catalytic loop at 128 JE/t, 2 seconds a batch. It has three input tanks and one output.
  - **Haber–Bosch:** 300 mB hydrogen + 100 mB nitrogen → 200 mB **ammonia**.
  - **Ostwald:** 100 mB ammonia + 200 mB oxygen + 100 mB water → 200 mB **nitric acid**.
- **Uses:**
  - **Fertilizer:** two phosphate in 250 mB of ammonia make **six** fertilizer in the chemical reactor (ammonium phosphate), against four with sulfuric acid.
  - **Microchips:** nitric acid etches them in the lithography station with **50 mB** where sulfuric acid takes 100.
- Nitrogen, oxygen and ammonia are gases: they live in tanks, pipes and gas holders. Nitric acid is a liquid with a bucket.

### Batch 13: oxygen-blown steel and argon
- The air separation unit also draws off **argon**, the scarce third part of air: 1 mB every 2 ticks from its middle row, into a third tank.
- **Boost gases:** pipe **oxygen** into the steel foundry and it blows the charge (basic oxygen steelmaking); pipe **argon** into the crystal grower and it shields the melt. Either runs **twice as fast** while it has gas, burning 2 mB of oxygen or 1 mB of argon a tick. Without gas they work as before.

## Connections
- Existing input producer: hydrogen from the electrolytic cell (brine); water; power.
- Existing output consumer: the chemical reactor (fertilizer, for the farming branch and the sprinkler), the lithography station (microchips), gas holders.
- Technology connection: fluid machines, pipes, gas holders; power.
- Magic connection: none.
- Reachable entry path:
  - Air separation unit: steel plates, steel pipes, two tinplate tanks, a machine casing, two electric motors and an advanced circuit.
  - Synthesis converter: steel plates, two titanium ingots, steel pipes, a machine casing and an advanced circuit.
  - All of these come from earlier tiers, so there is no circular unlock.
- Required vs optional: optional; both uses improve on existing routes rather than replacing them.
- Later ideas from the backlog build on this: explosives (nitric acid, gated by a hazard design), polymer tiers, flow batteries.

## Balance and automation
- **Fluid is never made from nothing.** Air separation draws from the air like the pumpjack draws from a reservoir, paid for in power (64 JE/t for 10 mB/t).
- **The converter's recipes shrink in volume:** 400 mB in → 200 mB of ammonia; 400 mB in → 200 mB of nitric acid. The fluid audit in `check_mod_data.py` checks this.
- **Hydrogen is the bottleneck.** 200 mB of ammonia needs 300 mB of hydrogen, which is 1.2 buckets of brine through the electrolytic cell. Ammonia is not cheap.
- **Fertilizer gain:** 6 instead of 4 from two phosphate; phosphate stays the limit.
- **Microchip gain:** half the acid per batch; wafers and copper stay the limit.
- Oxygen has one consumer for now (Ostwald). Argon is left for later.

## Multiplayer and persistence
Server-side machines using the shared fluid-machine framework. Their tanks, energy and settings save like every machine. Nothing new is stored elsewhere.

## Dependencies and assets
No new dependencies. Models (`tools/dieselpunk_models.py`), front textures and fluid textures (`tools/petro_textures.py`) are original.

## Verification
- `tools/check_mod_data.py` checks:
  - the fluids and gases against `PetroFluids.java`;
  - the fluid specs and recipe types against `MachineKind`;
  - that no recipe makes fluid from nothing.
- Game tests (`PetroGameTests`), batch 13: `oxygenSpeedsUpTheSteelFoundry`, `argonSpeedsUpTheCrystalGrower`, and the air separation test now checks argon.
- Game tests (`PetroGameTests`):
  - `airSeparationMakesNitrogenAndOxygen`
  - `converterMakesAmmoniaAndNitricAcid`
  - `reactorMakesAmmoniumPhosphate`
  - `lithographyEtchesWithNitricAcid`
- Not run: client play, two players, performance with many converters.

## World and event applicability
Not applicable: no worldgen, mobs or dimensions.

## Rollout and open questions
- Oxygen: further uses (oxygen-blown steel, a better fuel cell) and argon (shielding gas for titanium) are candidates for a later batch.
- Explosives from nitric acid need a hazard design and a server switch before anything is built.
