# Boron in solar cells

Status: implemented on the contributor's fork, pending review. Builds on [boric acid](boric-acid.md) (`claude/boric-acid`).

Proposal issue: none. TODO item: "Add boric-acid/boron connections for suitable nuclear control and advanced solar manufacturing…". The nuclear half is the boric-acid change's control rod assembly; this change adds the solar half.

**This change's choices:** the doping recipe and the panel's new bill, listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: titanium era, the Advanced Solar Panel
Primary specialty and supported player role: electricity; a player building solar power

## Player experience

- **Boron-Doped Silicon.** Two silicon and one boric acid craft into two boron-doped silicon: the p-type layer of a solar cell.
- **Advanced Solar Panel.** The recipe now takes two boron-doped silicon in place of its two aluminum plates. The rest is unchanged: three solar panels, a processor, two aluminum cables and a titanium ingot. Panels already built are untouched.

## Connections

- **Input producers:**
  - silicon (quartz blasted into silicon);
  - boric acid (borax freed with sulfuric acid in the Chemical Reactor).
- **Output consumer:** the Advanced Solar Panel.
- **Reachability:** boric acid comes from sulfuric acid and borax, which tincal (a crust on dry desert sand) drops when mined. The doping recipe loads with the machines, silicon and sulfur features.

## Balance

- One boric acid dopes two silicon, enough for one panel. Its two aluminum plates are no longer needed; aluminum still goes into the panel's cables.
- **No loops:** nothing turns back into silicon or boric acid.

## Multiplayer and persistence

- **New registrations,** stable from now on: the item `jugcraft:boron_doped_silicon` and the recipe `boron_doped_silicon`.
- The `advanced_solar_panel` recipe changes its bill. Its ID and every placed panel stay.

## Dependencies and assets

There are no new dependencies. The icon is drawn by `tools/petro_textures.py`: a round dark blue-grey wafer, the standard 12-pixel pixel circle (symmetric on both axes), with a grid of cells. No texture is copied.

## Verification

**Local checks** (no Gradle or game): `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py` passed.

**Server game test for CI** (`BoricAcidGameTests`):

| Test | What it checks |
| --- | --- |
| `dopedSiliconGoesIntoTheAdvancedSolarPanel` | Two silicon and a boric acid craft two boron-doped silicon; two of those complete the Advanced Solar Panel. |

### Results

Fork Build run [38872869983](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38872869983) on commit `dafd4de3` passed on its second attempt; the first attempt's optional-integrations-absent job failed only on the unrelated NuclearPlantGameTests.reactorBuildingsKeepRadiationIn and client shard 2 hit the 60-minute limit: the mod job (compilation and every server game test, this change's included), the optional-integrations-absent job and all five rendered-client shards. No human gameplay or multiplayer test was run.

## Rollout and open questions

- The doping recipe and the panel's new bill await the owner's review.
- Sulfamate plating, the TODO item's other half, remains open.
