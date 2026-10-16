# Trees batch 3: yew and the Gloomweald

Status: implemented and tested, awaiting owner inspection, 15 October 2026. Public branch `speedygroyper:codex/tree-generation-batch-3-yew`, based on the tested batch-2 checkpoint. Actual tool: OpenAI Codex (GPT-6). The owner subsequently instructed us to finish batch 3 and not start batch 4; no later tree implementation is included.

The owner requested the remaining [tree roster](../branches/TREES.md) after batch 2. Its palettes were approved on 6 October; yew uses the approved painted wood color `#654135`. Runtime logs follow the subsequent 11 October vanilla-texture reference policy. Pine's separately approved original textures remain unchanged.

## Intended player outcome

Yew supplies an evergreen, low, dark crown and a full timber set for building, with naturally ripening red arils. Single saplings grow ordinary yew; four grow a gnarled ancient yew. Arils are poisonous food, a source of red dye and an optional cauldron floater. Existing wood processing and replanting apply without a new progression gate.

Gloomweald replaces the overlapping vanilla dark-forest vegetation with one canopy list: about sixteen attempts per chunk, dark oak 45%, yew 20%, dead tree 10%, dead snag 10%, huge red mushroom 8%, huge brown mushroom 7%. Tree attempts still require compatible ground and space. New chunks only.

The yew also introduces optional leaf drawing controls for gaps, blossom edge greens/coverage and frond midribs. Existing texture defaults and seeds stay identical. Later roster trees reuse these controls.

This is Discovery-stage exploration/building content: natural trees and planted saplings produce timber; players, crafting, sawmill and tree farm consume it. No required cross-specialty dependency is added. Failed giant growth retains the four saplings. Evergreen fruiting leaves tick only while unripe or while unsupported and decaying; player-placed leaves do not fruit.

## Evidence

Yew uses a straight trunk 4 (+2) under blob foliage radius 3 / height 4, targeting a 5-7-block dome; the plan's trunk 2 (+2) would produce a shorter 3-5-block shrub under the actual placer. Ancient yew uses the native dark-oak trunk, foliage and three-layer clearance shape, plus the shared podzol decorator. Both grow actual yew wood.

Arils ripen one step per 16 random ticks on average, give 1-2 per ripe exposed leaf, poison for ten seconds when eaten, craft one red dye, and fit the existing cauldron floater tag. They are not compostable; ripe-leaf drops do not add arils when using shears or Silk Touch. Persistent leaves cannot ripen. Supported ripe leaves stop random ticking; unsupported ripe leaves still decay.

The leaf generator now accepts per-entry gap thresholds, edge-green palettes, under-gap colours, flower coverage and frond midrib count/length/angle. Yew uses a 0.15 threshold (three transparent pixels in its 256-pixel generated tile). Fruit clusters use a tree-specific seed with wrapping minimum spacing. All 104 pre-existing generated texture outputs were checked byte-for-byte at the pixel level before and after the options change and stayed identical. New textures are authored with the established Canvas/wood generator; yew's approved owner-bank palette supplies its planks and sapling bark, while its runtime logs reference vanilla oak/pale-oak with fixed tints under the current log policy.

The final focused build/server run passed all five yew tests, and `YewClientGameTests` passed in Minecraft 26.3 / Fabric Loader 0.19.5 / Fabric API 0.161.0+26.3 on Java 25.0.4.1. Twelve separated seeds, each placed twice, produced normal yews 5-7 blocks high and 9 wide, and ancient yews 7-11 high and 8-10 wide. Identical seeds reproduced exactly; different seeds changed the crowns. Invalid stone ground was rejected; blocked four-sapling growth preserved all original sapling states. Fruit eligibility, harvest reset/no duplicate pickup, ripe-leaf decay, poison, dye, hand crafting, sawmill acceptance and fuel components passed. Shears and Silk Touch preserve one leaf block without duplicating fruit or saplings. Natural drops supply yew saplings; the tree-farm recipe yields six yew logs and returns one sapling.

The normal world uses seed `jugcraft`. Its authored garden contains three examples of each shape and a seven-wood comparison wall. The client saved/reopened all six trees unchanged. Its natural Gloomweald stop is `/tp @s 448 87 -2200 135 25`: the surveyed area contained 97 yew logs, 1,993 dark-oak logs and 193 dead logs. These are block counts, not tree percentages: different shapes have different log counts and placement success. The generated selector has the exact 20% yew attempt share, independently checked by the Python test.

All 40 Python tests and the repository structure/link check passed. The complete offline data/recipe audit passed 2,671 material IDs, with 8,376 block/item models, 3,707 quad parts and 171 combined models audited. Initial broad checks caught missing poison/fruit metadata and a fixed fence-count assertion; the metadata now describes the implemented behavior and the test checks every registered fence/bar/wall instead of a count that becomes obsolete with each wood.

The last full server run completed 1,653 tests: all but the existing `CrowGameTests.aCrowFliesDownToPeck` passed. That AI test timed out waiting for the crow to peck; no crow or crop behavior was changed. Its isolated rerun passed without changing crow code (`build/tree3-crow-recheck.log`), consistent with an intermittent AI timing failure. Another earlier run exposed the existing spodumene survey test assuming its whole structure fits inside one chunk; that test and its identical fluorite counterpart now use the actual prospector's surrounding-chunk radius. They passed in the subsequent full run. The full suite is not represented as entirely green.

Commands/evidence: `./build-local.ps1 -Tasks @('build','runGameTest')` for the full runs; the final focused build added `-I build/pine-focused.gradle -PgameTestFilter=jugcraft-test:yew_game_tests_* runClientGameTest -PclientTests=YewClientGameTests`. Python: `python -m unittest discover -s scripts/tests -v`, `python scripts/check_repository.py`, `python tools/check_mod_data.py`. Ignored local logs: `build/tree3-verified.log` (full suite), `build/tree3-yew-final.log` (five tests/build/client), `build/tree3-data-final.log`, `build/tree3-python-tests.log`. Screenshot evidence is copied with the world export so later builds cannot erase it.

The delivery contains a matching JAR, standalone world ZIP, Modrinth inspection pack and tour. World cleanup scanned 4,765 saved region records and all other files: it removed exactly two development datapack names and five unused registry entries, changing only `level.dat`, `level.dat_old` and Fabric registry metadata. All other saved bytes are preserved. The source world remains untouched.

The cleaned world also opened in the ordinary client, with the full locked integrations and without the test mod. The player joined at the ancient-yew garden `(1040.5, 180, -987.5)`; evidence is `build/tree3-ordinary-client.log`. The supplied pack/world archives are separate copies, so this inspection session cannot modify the delivered originals.

No fruit compost is registered. At the largest observed ordinary crown (217 leaves), even Fortune's 10% sapling chance at 30% compost gives 217 × 0.10 × 0.30 = 6.51 expected composter levels, below the seven needed for one bone meal; natural growth remains renewable. Leaves themselves are not compostable. This is a sampled crown bound, not a mathematical proof over every seed.

Screenshots: [ordinary yew](../images/trees-batch-3-yew.png), [ancient yew](../images/trees-batch-3-ancient.png), [natural Gloomweald](../images/trees-batch-3-gloomweald.png), [wood comparison](../images/trees-batch-3-wood.png), [three fruit stages](../images/trees-batch-3-arils.png).

Not claimed: owner approval of the new in-game yew, external launcher import, two-client multiplayer, a long-duration performance run or all world seeds. No merge, release or server deployment is included.
