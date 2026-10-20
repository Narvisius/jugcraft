# Underground drilling and Chima integration — 20 October 2026

The owner selected Group A: underground deposits/deep drilling (#438) and Chima (#360),
with conflict repair, combined testing and merge to main. Base: `62c32ad3a5cfa05c298eab93e978951ab351aba8`.
Integration and repairs: OpenAI Codex (GPT-6), under direct owner authorization.
Original feature artwork, authors and attribution remain in their feature records.

## Included outcome

- Four finite underground deposit families, a two-block Deep-Drilling Assembly on the existing rig,
  shared reserve accounting, protected extraction and a depth/progress interface.
- Chima's savanna spawning, beef taming/healing, owner follow/sit behavior and five appearances.
- Existing main content, platform pins, machine interfaces and approved assets are preserved.
  Generation changes affect new chunks only; no owner worlds or launcher profiles are modified.

## Integration repairs

- Reconcile generated resource ownership and refresh the combined survival/registry audit rather
  than taking either branch's outdated report. Add explicit essence records for the six new items.
- Check tamed Chima attacks against its owner's shared claim/PvP rules at the damage boundary,
  including retaliation, and reject strikes against its owner. Regression covers claimed animals.
- Keep Chima follow teleports in loaded, dry positions within the world border, and never teleport
  a sitting pet. Preserve the contributor's supplied appearance.
- Use reproducible PNG encoding for Chima; all eleven images retain the contributor's exact pixels.
- Exercise a tamed shark-look Chima next to the deep rig in the existing real-world inspection.
  Save/reopen checks both systems: reserve, mast, depth and partial extraction, plus pet identity,
  owner, health, appearance and sitting state.

## Source commits

| PR | Integrated head |
| --- | --- |
| #438 | `108aa4c0de5de86c723323f94df6154816f14ec9` |
| #360 | `7f4c91d93058effe235b1c6ede00bacc1aa24bdc` |

## Validation

All 155 local Python checks passed, as did repository validation and the material/model audit
(6,170 material IDs, 11,747 block/item models, 3,713 quad parts and 174 combined models).
The actual registry export refreshed survival reports with zero invalid ingredients.
Main/client/test sources compiled; the combined server suite is running.
Exact final checks and run links will be recorded on the
integration PR before merge. Feature-specific historical evidence remains in
[underground deposits](underground-bulk-deposits.md) and [Chima](chima.md).
No human or two-account playtest is claimed.
