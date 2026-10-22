# Refreshing already generated candy caverns

The corrected [Candy Cavity generator](features/candy-cavity-generation.md)
applies to newly generated chunks. Installing it does not repair old geometry.
Updating the server JAR preserves the world.

## First survey an offline backup

Use a stopped-server copy or completed consistent backup, not a changing live
world. The read-only tool produces candidate coordinates and flags:

```text
python scripts/audit_candy_chunks.py --world PATH/TO/BACKUP/world --output candy-review.json
```

It accepts the 26.3 world layout (`dimensions/minecraft/overworld/region`) as well as a direct dimension folder. It examines Overworld Anvil biome palettes and reports Candy Cavity chunks,
inhabited time, block entities, native structure starts and chunk hashes. It
never deletes or edits world files, never follows external `.mcc` chunks, and
reports unsupported/corrupt records rather than treating them as empty. It cannot
identify whether a particular candy chunk used the old generator; the coordinates
and current geometry must be reviewed against the release history.

**No reported chunk is automatically safe to reset.** Low inhabited time, no chest,
or no claim does not prove the absence of player work. Surface builds, tunnels,
inventories, pets, claims, adjacent structure pieces and underground machines all
need review. Claim metadata and external entity/POI regions are not interpreted by
this survey and must be checked separately.

## Choose the least destructive route for each location

1. Prefer unexplored Candy Cavity areas if players have already built in or above
   an old cave. Use `/locate biome jugcraft:cavern_candy_cavity` from an unexplored
   area in a **copy first**; already generated nearby candidates may still be old.
2. For genuinely unused areas, select exact chunks (plus coherent structure
   boundaries), not whole 512-block regions. Stop the server, retain a verified
   full backup, then use an Anvil editor supporting this world's data version to
   reset the reviewed terrain chunks together with their matching entity and POI
   data. Rebuilding a chunk replaces its **entire vertical column**, including the
   surface. Do not delete only `region/` records while leaving entity/POI data.
3. Load the edited copy with the exact forthcoming server build and seed. Inspect
   plateau transitions, soda, templates, cave/surface joins and structures. Verify
   player builds outside the selection, claims, entity counts and all inventories.
4. Only after owner approval of the exact coordinates and a successful copy test,
   repeat the reviewed operation during a stopped-server maintenance window.
   Keep the pre-change full backup for rollback; never mix unrelated region files
   from different backups.

There is deliberately no blanket live-world regeneration command. The next input
needed for actual repair is the affected cave coordinates and a consistent server
world backup. No hosted world has been scanned or rewritten by this change.

Prepared with OpenAI Codex (GPT-6), 22 October 2026.
