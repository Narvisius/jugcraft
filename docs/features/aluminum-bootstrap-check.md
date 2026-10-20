# Aluminum bootstrap check

Status: implemented on the contributor's fork, pending review.

Proposal issue: none. TODO item: "Use earlier copper conductors in entry aluminum-processing equipment, with aluminum improvements later; **verify construction and reagent producers without self-output gates**."

Owner: jimbozoomer-byte
Target milestone and tier: industrial chemistry, the first aluminum line
Primary specialty and supported player role: metals; players starting aluminum production

## Player experience

Nothing in the game changes. The first aluminum line already uses copper:

| Machine | What it does | Conductor |
| --- | --- | --- |
| Electrolytic Separator | brine to lye, hydrogen and chlorine | copper cable |
| Bauxite Digestion Vessel | bauxite and lye to aluminate slurry | copper cable |
| Aluminate Recovery Press | slurry to aluminum hydroxide, recovering lye | none |
| Alumina Calciner | hydroxide to alumina | copper cable |
| Aluminum Reduction Cell | alumina and coke to aluminum ingots | copper cable |
| Arc Furnace (controller and casing) | bauxite straight to aluminum | copper cable |

This change adds a permanent automatic check, so a later recipe change cannot quietly make aluminum a requirement for making aluminum.

## How the check works

`check_aluminum_bootstrap` in `tools/check_mod_data.py`:

1. Withholds all aluminum metal. That means every Jugcraft item named "aluminum", except the line's own aluminum hydroxide and the reduction cell itself.
2. Treats anything no recipe makes as available: mined and vanilla things.
3. Treats an item as available once some recipe makes it from available things. Tags count as available when any member is.
4. Fails if any machine above, or the lye and coke the line uses, is not available.

It follows ingredient trees only. It does not follow the machines that run each process step, apart from the ones listed above.

Run against a list that also named the Electrolytic Cell and the Advanced Solar Panel (both made with aluminum cable), the check reported both as needing aluminum, so it does catch a gate.

## Connections

- **Input producer / output consumer:** unchanged.
- **Unlocks:** unchanged. The "aluminum improvements later" half of the TODO item, such as an aluminum-cabled upgrade, is not part of this change.

## Multiplayer and persistence

No game change: no registrations, recipes or saved data.

## Verification

**Local checks:**
- the new check alone passes;
- with the two aluminum-cabled machines added to its list it fails on exactly those two;
- `python tools/check_mod_data.py` passed (2,885 material IDs, the data files and the recipe audit, including the new check);
- `python scripts/check_repository.py` passed.

**Results:** fork [run 39189348976](https://github.com/mmme123-spec/jugcraftfork/actions/runs/39189348976) on 348d101e5: green. The mod built, and the generated-data check passed. So did the material check, including this new check. The server game tests and all eight client shards passed. On the first attempt, client shard 5 failed when the companion-population screenshot test (`PeepoPopulationClientTests`) timed out waiting for creatures. This change touches no game code, and the same test passed on other branches that day. Re-running that one job passed. The only later commit records the local checks in this file.

## AI attribution

Implemented with Claude Opus 5.5 (Claude Code) for the contributor; the contributor is responsible for the output.
