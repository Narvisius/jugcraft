JUGCRAFT harvest polish — release candidate

Changes prepared since 0.1.0-alpha.20261102.2:
- Visible raider pilots riding walker hatches and blimp gondolas, persistent crews, bailout handling and bounded stuck-path recovery. No extra raiders or increased attack power.
- Restyled pet stable with portraits, status indicators, health and bond information, recovery timers, behavior controls and compact-window navigation.
- Sculpted models for Cornbread Stuffing, Vegetable Noodles, Squid Ink Pasta, Bacon and Eggs, Steak and Potatoes, Roasted Mutton Chops, Pasta with Meatballs and Tomato Sauce.
- Nineteen craftable harvest decorations: produce baskets, gourd stacks, place settings, cutlery, bread baskets, gravy boats, covered butter dishes, pie stands, leaf garlands, amber lanterns, candle centerpieces, market signs, copper pan racks, turkey/squirrel ornaments, acorn mobiles and tabletop cornucopias.
- An autumn leaf runner for connected Feast Tables.

- Recoverable industrial machines and Chemical Reactors retaining stored fluid, plus bounded Lua startup (PR 597 integrated into the release branch).
- Imported model cap/handle repairs and source-verified multipart seam checks.
- Third-person aiming preserves external yaw and pitch corrections.

Validation: 52 focused server tests, 29 Python checks and both rendered pet/holiday client suites passed on the holiday batch. See docs/features/holiday-batch-nov3.md. Fresh GitHub checks are pending. Full data and geometry audits now pass; combined runtime and full CI validation remain pending. Stationary crew visuals were checked; a full populated-city raid soak was not performed.

No automatic replacement of owner city builds, no world reset, no personal-profile replacement. Release version and exact source must be finalized after merge. Deploy the server only after separate owner restart confirmation and a full 30-second player warning; promote client updates only after server verification.

Implementation: Claude Opus 5.5 with OpenAI Codex integration, fixes, review and validation.
