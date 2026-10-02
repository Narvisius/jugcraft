# Engineer's Handbook

Status: implemented in source (PR #20); **not yet played**. CI renders it in a real client (screenshots).
Proposal issue: none; the owner selected it directly on 30 September 2026.
Owner: @jimbozoomer-byte
Target milestone and tier: available from the start
Primary specialty and supported player role: every player; onboarding

## Player experience
Craft a book with a copper ingot and right-click it to read how every machine works: what it does, its power use, its crafting recipe and example recipes. No wiki or recipe viewer is needed. This matters because no recipe viewer supports Minecraft 26.3 yet.

### Batch 23: reorganised, with a progression guide
The owner asked for this on 1 October 2026: "we need to redo the UI on the engineering guide it goes off screen and it needs to be more organized and maybe have a page that shows a good idea of how you would want to progress each item".
- **It fits the screen.** The book sizes itself to the window, between 300×170 and 440×260. The old fixed 330×190 book stacked 15 chapter buttons 16 pixels apart, so they ran off the bottom.
- **Contents list.** All chapters are listed on the left. The open chapter unfolds to list its pages, so any page is one click away. The list scrolls (mouse wheel, with a scroll bar) when it is longer than the book.
- **Long pages scroll.** The page title stays put and the body scrolls under it: mouse wheel, Up/Down, Page Up/Down. Left/Right and the arrow buttons turn pages. Recipes move under the crafting grid when the page is narrow.
- **Progression chapter, first in the book.** "The Road Ahead" shows the nine stages:
  1. Bronze Age;
  2. Workshop;
  3. Rotation and Logistics;
  4. Steel;
  5. Oil;
  6. Chemistry;
  7. Electronics;
  8. Late Game (big power);
  9. Special Materials (rubber, glass, grenades).

  Each stage then has its own page: what it is for, a plan in a few lines, and a numbered chain of the items to make in order. Each step is shown as a card with the item's icon (hover it for the name) and a short label.
- The steps are written in `PROGRESSION` in `tools/handbook.py`. `check_mod_data.py` checks that every step names a real item.

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
- Batch 23: the client game test screenshots the first page, the first progression stage and a machine page. A person has not yet tried the new screen at small window sizes.
- `tools/check_mod_data.py` passes (166 IDs) and checks that every item the book shows exists.
- CI job `client` runs `JugcraftClientGameTests`. It opens the handbook and saves the screenshots `jugcraft_handbook` and `jugcraft_handbook_machine_page`.
- Not run: human reading and play-testing.

## World and event applicability
Not applicable.

## Rollout and open questions
- Translations: the file is `en_us.json`; other languages would add files, with a lookup by the selected language.
- A recipe viewer plugin is still wanted once one supports 26.3.
