# Companion commands

## Inventory and equipment

Shift-right-click an owned Peepo or Jughead to open the command screen. The left panel contains eight general storage slots in two rows of four, plus separate Costume and Hand slots. The lower inventory is the player's inventory.

- Put one Jack o'Lantern in Costume to equip the pumpkin outfit; removing it restores the default appearance. Existing equipped outfits migrate into this slot on loading.
- Hand accepts one item, including tools and torches, and displays it attached to the right hand. This equips the item visually; tool work is not yet automated. Held lighting uses the optional integration described below.
- Eating temporarily displays the meal; the equipped item stays safely in its slot and returns afterward. You may change equipment while eating.
- Shift-click moves items between companion storage and player inventory; a Jack o'Lantern first fills an empty costume slot. Place held items directly in Hand.
- Storage and equipment retain exact slots and item components across saves. Contents drop once on normal death, under the existing mob-loot rules. Access uses the existing owner/party command permissions.

Manual checks: transfer full and partial stacks, equip/remove a costume, hold a torch/tool while walking and sitting, feed while equipped (including changing the hand item during a meal), reload during eating, and check normal death drops for duplication. No automated tests run.

## Held lights and tiki torches

Companions raise their right arm when holding a light, including while sitting or running. Eating and sleeping keep their existing poses. The held-light tag `peepo_companion:held_lights` covers torches/lanterns and the tiki torch; other block items with an emissive default state also qualify. Held lights stay upright independently of the animated arm. Optional LambDynamicLights support supplies moving illumination when that client mod is installed and enabled.

Craft one Tiki Torch with coal above a stick above another stick in a crafting table. It occupies two vertical blocks on a sturdy floor and has a wooden shaft, tapered open supports, woven reed basket, black cap and wick. Its upper half emits light level 14 and client-side flame/smoke particles. Breaking it removes both halves and drops one torch in survival. It can also be equipped in the companion Hand slot.

### Dynamic-light integration

Checked upstream `origin/main` at `8ca8aee5`: `distribution/frameworks.lock.json` pins LambDynamicLights **4.13.0+26.3** as optional/client-side (`lambdynlights`). This local branch adds the data-only definition `assets/peepo_companion/dynamiclights/item/tiki_torch.json` with luminance 14 and water sensitivity. An explicit definition is needed because the torch's lower/default block half emits zero light.

LambDynamicLights already reads held equipment on living entities, including Peepo/Jughead's synchronized main hand, so no entity scan, server ticking light blocks, or mandatory Java dependency is added. Vanilla torch lighting is supplied by LambDynamicLights. Install/enable the pinned client mod to see moving illumination; this jar does not bundle it. Gameplay block lighting continues to come from placed torches. No remote branch changes were merged into this branch.

References: https://github.com/speedygroyper/jugcraft/blob/8ca8aee5/distribution/frameworks.lock.json and https://lambdaurora.dev/projects/lambdynamiclights/docs/v4/item.html and https://lambdaurora.dev/projects/lambdynamiclights/docs/v4/entity.html .

## Explicit home/work assignments

The local branch is now `peepo-companion`. Use the Companion Planner to select a tamed companion and then assign one home and up to four workstations. Left-click with the tool unassigns without mining; Shift-right-click air clears the selection. The GUI now lists assignments and coordinates in place of Set home/Set work. See [the assignment feature record](docs/features/companion-assignments.md) for supported jobs, limits and suggested follow-ups.

The planner also binds a separate lunch crate/cover. The companion GUI's Routine tab controls Auto/Day/Night shifts, break/resume energy, carried meals, food preference and optional problem alerts. Operator command `/peepobudget` shows server-wide search/path admissions and deferrals. See [the job-system record](docs/features/companion-jobs.md).

Opening the companion GUI pauses its walking and work until all players editing it close their menus. It then resumes the selected command automatically; no Stay command is needed.

## Cooking Pot assistance

Assign a Cooking Pot with the planner, then shift-right-click the companion. Left-click the ghost slot beside that workstation with its finished food item; the real item remains on the cursor. Right-click the ghost slot to clear the recipe back to automatic cooking. Hover the ghost item for ingredient counts. The saved workstation plan filters input items. One helper adds 50% cooking speed while spending up to 16 JE/t, with heat and ingredients still required. Leave a reachable approach beside the pot and headroom above its rim for the standing companion and angled spoon; Jughead needs extra jug clearance. Schedules, recovery and workstation priorities still apply. Assigned Supply and Output containers now use that plan for Cooking Pot transport. See [Cooking Pot assistance](docs/features/companion-cooking.md).

Companion-only tests: `.\build-local.ps1 -Tasks @('runClientGameTest','-PclientTests=PeepoCompanionClientTests')`. See [results and coverage](docs/features/companion-tests.md).

## Processor assistance and two-helper teams

Assign the same processor to two tamed companions using the Companion Planner. Full multiblocks accept two helpers at +25% each (8 JE/t of reserve each); single-block and compact legacy copies accept one at +50% (16 JE/t). All 36 item/fluid/mining/farming processor types are enabled, including pumps and air separation. Each bonus step still uses normal machine electricity and production resources. Two separate reachable standing spaces are required; the third helper sees Occupied. Priorities, schedules, meals and recovery still apply. Generators and storage have no assistance job. Cooking Pot ghost recipes and its 16 JE/t drain are unchanged; the wheel remains up to 64 JE/t. See [the full per-machine energy and animation table](docs/features/companion-jobs.md#companion-reserve-drain-and-animation-suggestions). No tests were run for this expansion.

Processor workers now automatically use reusable valve, lever, mallet and wrench animations, with visible props and complementary roles for two helpers. No tool needs to be equipped. Both Peepo and Jughead use the same clips, including in pumpkin costumes; their held equipment returns after work. Cooking and wheel animations are unchanged. See [clip timing and per-machine roles](docs/features/companion-jobs.md#reusable-work-clips).

## Hand crank, Cider Press, Supply and Output

Assign a Hand Crank with the planner to have a companion jump/hang from its actual turning handle. It supplies up to 16 KE/t, spending only the energy accepted. Receiving flywheels at 99% pause it until below 90%; no accepting consumer also stops it. Keep a reachable standing place and headroom beside the crank.

Assign a Cider Press, Supply and Output to automate the full cider cycle. Put apples and empty glass bottles in Supply and keep one companion cargo slot free. The companion loads apples, grinds/presses using the general interaction clip, bottles juice and carries Sweet Cider and pomace to Output. No recipe selection is needed. Missing bottles or blocked Output stop further batches; products stay in a saved tray. Shift-right-click the press with an empty hand to collect buffered products manually. Existing manual pressing and bottling still work.

With a selected companion, right-click containers to assign/cycle **Supply (blue)** and **Output (yellow)**. The first two new containers fill those roles; click either again to switch, swapping a valid existing pair. No Shift modifier is needed. The two links appear below Lunch in the companion GUI, and left-click or x removes them. Use distinct containers and keep each travel leg within 64 blocks. For Cooking Pot supply, select the finished dish in that workstation's ghost slot. The companion fetches missing recipe ingredients and carries completed food/returned containers to Output, using one free cargo slot per trip. Food reserved for a delivery is not eaten. Full destinations keep the cargo for retry, and recipe changes return surplus to Supply. Hearth Oven, Canning Kettle and standard processor item transport are also supported. See [job details and validation](docs/features/companion-jobs.md#hand-crank-cider-press-and-transport).

Choose **Porter** in the companion GUI to move any extractable items directly from Supply to Output, up to 32 per walking trip, without a workstation. Keep one cargo slot free and the pair within 64 blocks. Empty Supply/full Output waits; meals, rest, schedules and the editing pause still apply. **Work** remains machine assistance and recipe logistics. Filters and ground-item cleanup are future additions. See [general porter behavior and limits](docs/features/companion-general-porter.md).

Each job row in the companion GUI has separate **Supply / Output** buttons cycling **Auto → On → Off**. Auto yields to connected hoppers/extractors, configured direct Eject outputs, and recent successful machine pipe/conveyor transfers. Idle pipes alone do not count. On forces companion participation; Off stops that direction while preserving speed assistance. For recipe-based processors, choose a finished item in the job's ghost slot to supply its ingredients and lock processing to that recipe; clear it to restore automatic processing. Special machine inputs and fluids still need their normal setup. See [behavior, supported scope and detection limits](docs/features/companion-transport-controls.md).

## Canning Kettle automation

Assign a heated Canning Kettle plus Supply and Output containers. Stock Supply with a water bucket (only needed while dry) and fresh, full, unsealed preserve jars from the Cooking Pot. Keep one cargo slot free. The companion fills water, sends the empty bucket to Output, loads up to four jars and collects sealed jars. Water remains between batches; no recipe selection or constant helper is needed. Spoiled jars in the kettle go to Output unchanged and unsealed. Finished/rejected products must clear before loading more. See [setup and behavior](docs/features/companion-canning.md).

## Hearth Oven automation

Assign the oven, Supply and Output. In the companion GUI, click the oven ghost slot with a whole raw or baked pie to choose what to make; right-click clears new pie supply. Stock Supply with matching prepared raw pies and logs/charcoal/coal/coke (not coal blocks). Keep one cargo slot free and storage close. Peepo supplies pies and fuel, stays beside a hot oven with the general interaction clip, collects the baked pie into cargo and delivers it. Full Output keeps the finished pie in cargo. Unattended pies can still burn if work is interrupted. See [Hearth Oven setup and behavior](docs/features/companion-hearth.md).
