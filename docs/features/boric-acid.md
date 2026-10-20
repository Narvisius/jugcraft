# Boric acid and reactor control rods

Status: implemented on the contributor's fork, pending CI and review.

Proposal issue: none. TODO item: "Add boric-acid/boron connections for suitable nuclear control and advanced solar manufacturing, plus later sulfamic/sulfamate terminal plating with reviewed nickel/zinc compatibility." This change does the first part, nuclear control.

**This change's choices:** the quantities, the control rod's bill and the change to the reactor pressure vessel's bill, listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: nuclear, before the first reactor
Primary specialty and supported player role: chemistry feeding nuclear power

## Player experience

- **Boric acid.** The Chemical Reactor frees two boric acid from one borax (mined from tincal in deserts and badlands) with 250 mB of sulfuric acid.
- **Control Rod Assembly.** Three boric acid between four tungsten plates, over two steel plates, make one control rod assembly: neutron-absorbing boron in tungsten-clad steel.
- **The reactor pressure vessel** now takes two control rod assemblies, where it took two steel blocks in its middle row. Its bill is now: two tungsten plates and a processor, two control rod assemblies around the LEU fuel assembly, and three steel blocks. That is one steel block fewer than before.

## Connections

- **Input producers:** borax (tincal, existing); sulfuric acid (the Chemical Reactor or the contact process); tungsten and steel plates.
- **Output consumers:** the reactor pressure vessel. Boric acid is also there for later boron uses (the TODO's advanced solar manufacturing).
- **Reachable entry path:** every input is made before the reactor; nothing needs the reactor.
- **Existing worlds:** a reactor already built is untouched. Only crafting a new vessel needs the rods.

## Balance

- One borax and 250 mB of acid give two boric acid; a vessel's two rods take six boric acid (three borax, 750 mB of acid) and eight tungsten plates more than before, against one steel block less.
- Building a reactor is rare, so this adds a step, not a running cost.

## Multiplayer and persistence

- **New registrations,** stable from now on: the items `jugcraft:boric_acid` and `jugcraft:control_rod_assembly`; the recipes `chemical_reaction/boric_acid` and `control_rod_assembly`.
- **Changed recipe:** `reactor_pressure_vessel` (its ID unchanged).

## Dependencies and assets

There are no new dependencies. Both icons are drawn by `tools/petro_textures.py`: a heap of pearly flakes, and four dark rods hanging from a steel spider. No texture is copied.

## Verification

**Local checks** (no Gradle or game): `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py` passed. The icons were inspected at 8x.

**Server game tests for CI** (`BoricAcidGameTests`):

| Test | What it checks |
| --- | --- |
| `boraxGivesBoricAcid` | The Chemical Reactor recipe loads and gives two boric acid for one borax and 250 mB of acid. |
| `controlRodsGoIntoTheReactorVessel` | The control rod recipe crafts; the vessel crafts with two rods and no longer without them. |

### Results

Fork Build run [38809622624](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38809622624) on commit `ca6458d1` passed: the mod job (compilation and every server game test, these two included), the optional-integrations-absent job and all five rendered-client shards. No human gameplay or multiplayer test was run.

## Rollout and open questions

- The quantities and the vessel's new bill await the owner's review.
- Boron for advanced solar manufacturing and sulfamate plating remain open.
