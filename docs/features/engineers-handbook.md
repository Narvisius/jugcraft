# Engineer's Handbook

Status: implemented in source (PR #20); **not yet played**. CI renders it in a real client (screenshots).
Proposal issue: none; the owner selected it directly on 30 September 2026.
Owner: @jimbozoomer-byte
Target milestone and tier: available from the start
Primary specialty and supported player role: every player; onboarding

## Player experience
Craft a book with a copper ingot and right-click it to read how every machine works: what it does, its power use, its crafting recipe and example recipes. No wiki or recipe viewer is needed. This matters because no recipe viewer supports Minecraft 26.3 yet.

## Connections
- Input producer: a vanilla book and copper.
- Output consumer: none; this is infrastructure for learning.
- Reachable entry path: craftable before anything else.
- For infrastructure: it documents every system; content is generated from `tools/machines.py` and `tools/materials.py`.

## Balance and automation
None. It is a reading-only item and sends nothing to the server.

## Multiplayer and persistence
Client-side screen only. The item is an ordinary stack-of-1 item with nothing saved.

## Dependencies and assets
Fabric API only. The texture is original (MIT).

## Verification
- `tools/check_mod_data.py` passes (166 IDs) and checks that every item the book shows exists.
- CI job `client` runs `JugcraftClientGameTests`. It opens the handbook and saves the screenshots `jugcraft_handbook` and `jugcraft_handbook_machine_page`.
- Not run: human reading and play-testing.

## World and event applicability
Not applicable.

## Rollout and open questions
- Translations: the file is `en_us.json`; other languages would add files, with a lookup by the selected language.
- A recipe viewer plugin is still wanted once one supports 26.3.
