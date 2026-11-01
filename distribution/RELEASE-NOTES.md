JUGCRAFT NEWEST collection and progression update — 20261031.1 candidate

- Building and decoration collection: supplied stone finishes, paths, doors and garage gates, fences, bridges, windows, stairs, lights, furniture, seasonal decorations and paintings, with native interactions and survival recipes.
- Household tools and storage: seating, cabinets, sinks, cooking and utility blocks, rope workshops, movable attachments, backpacks and portable tools.
- Botanical and arcane systems: mana production and transport, functional flowers, crafting and control devices, equipment, brews, rituals, relics and encounters; supplied models, textures and animation data integrated into Jugcraft.
- Creatures and exploration: aquatic content, Ribbits, Moobloom variants, Naga, Wroughtnaut, Frostmaw, Bluff, Elokosa, Suntribe, Sculptor and monastery/galleon content. Missing originals remain explicitly documented.
- Equipment: accessory slots accept supported equipment without research. The tested client equipped and removed items in 14 slot groups.
- Getting started: unified Encyclopedia sections for technology, magic and transmutation, current-inventory ingredient lists, recipe previews and free NPC mentor lessons.
- Crafting: choose between matching crafting outputs using the small result selector. Six server tests covered all 76 outputs across 38 conflicts; table and inventory client flows were exercised.
- Repairs: supplied missing models/textures, aircraft banking direction and raid approach/pathing fixes from the accumulated local batch.

Known priority fixes (docs/TODO.md):
- 708 static geometry diagnostics still require source-aware visual review; these are findings, not 708 confirmed broken models. The static art check is NOT passing.
- Missing originals: two monastery dead-end templates, Home saddle/mangrove-zaisu geometry, two trophy skins, four cherry-waffle door variants, and the empty Fast Paintings source folder.
- Owner/player survival and visual acceptance remains outstanding.
- Full-suite CI memory reliability: the default 4 GB test run exhausted memory; the local 6 GB sweep completed.

Validation: the full local server sweep passed 3,258/3,263 tests; all five old test expectations were corrected and a 58-test follow-up passed without optional integrations, including all 19 NEWEST regressions. Rendered client checks passed, including 837,276 block states and representative content. Data/recipe and native resource-reference checks passed. This is evidence across runs, not one completely green unfiltered run or a human multiplayer playtest.

Preserve worlds, player data, settings and owner configuration. Do not regenerate the world for this update. New world-generation content appears in newly generated areas. Matching server/client versions are required. Keep the previous mod and a matching world backup for rollback after new content is saved.

Existing players: only after the server is updated and the channel promoted, use JUGCRAFT Updates > Download > Apply update & quit, wait for completion, then relaunch the same Modrinth profile. No new profile or public Modrinth pack listing is needed.

Implementation and release preparation: OpenAI Codex (GPT-6), directed by the owner. Original supplied assets and notices retained. This candidate is not a claim of completed deployment or clean CI.

Additional pre-publication priority: reconcile new essence values and conversion-cost fixtures, JEI station coverage, duplicate shapeless bills and survival-audit registry coverage. The broader local Python suite did not pass; packaging-specific tests did. These findings are tracked in docs/TODO.md and are not hidden by the passing gameplay tests.
