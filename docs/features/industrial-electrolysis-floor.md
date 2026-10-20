# Electrolysis energy floor

Status: implemented on the contributor's fork (a data check, no gameplay change); CI green; pending review.
Stacked on the [Large Gas Burning Generator](industrial-large-gas-burning-generator.md) and the branches below it.

Proposal issue: none. TODO item: "Enforce the electrolysis energy floor before any expanded or bulk Separator."

**From the owner's plan:** the floor of 256 JE per mB of hydrogen (a proposal in the [Electrolytic Separator](industrial-electrolytic-separator.md#balance-and-automation) and the [starter gas factory](industrial-starter-gas-and-acid-factory.md)), to be enforced before a larger Separator.

**This change's choice:** how the floor is applied, below.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, the starter gas factory
Primary specialty and supported player role: none (a balance guard)

## Player experience

Nothing changes in play today. The check exists so a later bigger Separator cannot quietly make hydrogen cheaper than the plan allows.

Without it, a bulk Separator with four efficiency cards would make hydrogen for 134.4 JE/mB. Every generator burns hydrogen at 128 JE/mB, so hydrogen would become a nearly lossless battery, one small bonus away from a power loop.

## Connections

- **Input producer / output consumer:** none; it reads `tools/industrial_forms.py`, `tools/machines.py` and `OperatingProfile.java`.
- **Technology connection:** the shared form foundation's operating profiles and upgrade cards.
- **Magic connection:** none.

## Balance and automation

Two changes to `tools/check_mod_data.py`:

1. **The loop audit counts profile savings.** `cheapest_electrolysis` already counted four efficiency cards. It now also counts the share an expanded (90%) or bulk (80%) form's lanes pay, rounded up as `OperatingProfile.lanePerTick` does. So no generator can burn a fuel for as much as its cheapest electric making costs on any form, large ones included.
2. **The floor.** `check_electrolysis_floor` fails any expanded or bulk form whose recipe makes hydrogen from electricity alone for less than 256 JE/mB. That cost includes:
   - every card it takes;
   - its profile saving;
   - the electric cost of any non-water inputs.

   Recipes that use items (coal, crops) are not electricity routes and are not held to it.

**The entry Separator is not held to the floor.** It has no profile saving, and its doc already records 168 JE/mB with four cards (the Fuel Cell returns 128, at most 76%). Lifting it to 256 would change today's play and needs the owner's decision.

**Results today:**
- No form breaches the floor.
- The audit's cheapest hydrogen is still 168 JE/mB.

**Probe:** the same forms with the Separator set to bulk and then expanded are caught at 134.4 and 152.0 JE/mB.

## Multiplayer and persistence

Not applicable: nothing in game changes.

## Dependencies and assets

None.

## Verification

Locally:
- `python tools/check_mod_data.py`: passed.
- The probe above was run by setting the Separator's profile in memory: both larger profiles were caught.

### Results

**Run [38498552563](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38498552563) (commit 447f9797) passed:** the data checks with the new floor check, the build, every server game test with and without optional integrations, and all five client test shards.

## Rollout and open questions

- **Owner decision:** whether the entry Separator should also be held to 256 JE/mB. This would mean weaker efficiency cards on it.
- **Next step:** a 4×4×6 bulk Separator, the brief's larger variant, needs a recipe priced at or above the floor. Taking no efficiency cards would do it: its 800-tick water batch at 80% of 256 JE/t makes 500 mB of hydrogen for about 328 JE/mB. Allowing cards but capping them is another option. Since built without cards: the [Bulk Electrolytic Separator](industrial-bulk-electrolytic-separator.md), which also extends both checks to larger forms that run a smaller form's recipes.
