# The Jugcraft Encyclopedia's first slice: the First Light route

Status: implemented for Concordance Chunk A (unit 4: ticket A-04, the encyclopedia entry and first route); its server
and client tests pass in CI (run [38435014953](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38435014953) on `aa4d06c4e`; see Verification).
Proposal issue: none. The owner chose Chunk A on 10 October 2026; [the chunk's contract](concordance-chunk-a.md) holds
the scope, [the first-chunk tickets](../plans/concordance-expansion-2026-10-10/12-detailed-first-chunk-tickets.md) (A-04)
the ask, and [the encyclopedia's own record](jugcraft-encyclopedia.md) the owner's requirements for the whole feature.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: a fresh survival world, from the first glowing thing found to a spell of the player's own
(First Light understood, the Practitioner stage), and on to a ward for a friend and, if they like, a lair.
Primary specialty and supported player role: a new caster, and anyone helping one.

## Player experience

**Two ways in, no item.** The Jugcraft Encyclopedia opens:

- with its key (Controls, *Jugcraft Encyclopedia*; **B** unless remapped), only from the game itself, so it never fires
  while typing in chat, a sign, an anvil or a book. Inside the encyclopedia the same key closes it, as Esc does;
- with the small scroll button in the survival inventory, right of the recipe-book button (it moves with it when the
  recipe book opens). In the creative inventory it is on the Survival Inventory tab only. Pressing it closes the
  inventory as Esc would (the carried stack and the 2×2 grid go back), then opens the encyclopedia; Back opens a fresh
  inventory.

**One plain screen**, vanilla widgets only, every control on the keyboard and read by the narrator. A tab for each page
that has something to show this player:

- **Discover**: the First Light route below, then the journal's research and stage reports, and a button to the
  Concordance Journal (J). Every step says, in words beside a mark of its own shape and colour, where it stands (Done,
  Next, Can do now, Later, Not available here) and how much it matters (Required, Recommended, Optional: a boss fight).
  A step the player can act on lists its ways: Required, Recommended, Trade (bought or shared), Optional (after a boss).
  A recipe is shown as its crafting grid, read from the server's own recipe; a recipe the route needs but this server
  lacks says so instead, and one it merely recommends is left out. The numbers (how dark, how many, what a spell costs)
  are read from the loaded rules and the compiler, so a data pack that changes them changes the page; a bench study
  takes the bench's own 5 seconds. Where a step has several ways, the page says any one of them is enough.
- **Spellcraft** (once First Light is begun, or an instrument carried): the instrument in hand and its limits, Focus,
  what is inscribed and its cost, the invocations known with what each costs this player, the words learned for
  composing (and how many are still to learn), and the way into the Spell Composer, whose Back returns here.
- **Expeditions**: a dossier for each lair the player has found (below).
- **Inspect**: opened with its key while looking at a Circle Anchor or a Ley Pylon within 16 blocks (the client looks
  that far itself, past the crosshair's reach; the server checks the distance again). For an anchor, its own report
  (the same lines chat and `/jugcraft concordance circle` show, from the anchor's cached report; Jade shows its phase
  and first three faults its own way) and the rituals the player knows there; for a pylon, its charge, how it fills and
  whose rituals it lends to.
- **Keys**: every Jugcraft key with its binding now, what it does, and any other key bound the same; the vanilla keys the
  route uses; and where to change them.

**Codex links** open an Arcane Concordance entry the player's research has opened, without the codex in hand.

### The route

Steps in order, with what each waits for, the recipes it shows as grids (with the ingredients the recipe files
register) and those it only checks are present before naming a set, the sales it names when a town stands (each one a
shop really sells), the codex entries it links, and the game tests that exercise it. Written by `tools/encyclopedia.py`
(`python3 tools/encyclopedia.py`) and checked against the Java (`check_encyclopedia`: each grid with its weight inside
its own step).

<!-- first-route:start -->
| Step | Matters | Done when | After | Recipes shown (ingredients on disk) | Trade | Codex | Tests |
|---|---|---|---|---|---|---|---|
| find | Required | First Light encountered, or a luminous specimen carried | - | - | curios: `minecraft:glow_ink_sac` | `radiance/first_light` | `aNewcomerSeesOnlyTheFirstSteps`, `theRouteNamesOnlyRealSales` |
| examine | Required | First Light encountered | find | - | - | - | `theRouteFollowsTheResearch`, `examiningBySneakUse` |
| dark | Required | First Light observed | examine | - | - | - | `theRouteFollowsTheResearch`, `fieldEvidenceUnderstandsFirstLight` |
| understand | Required | First Light understood | dark | recommended: `jugcraft:lampwright_bench` (2 copper ingot, 1 #concordance/luminous matter, 3 #planks, 2 stick) | - | `radiance/lampwright_bench`<br>`foundations/research_notes` | `theRouteFollowsTheResearch`, `benchStudyUnderstandsFirstLight`, `anAbsentRecipeIsNeverAdvertised` |
| wand | Required | an instrument carried | - | required: `jugcraft:initiate_wand` (1 #concordance/luminous matter, 1 stick, 1 copper ingot) | - | `foundations/initiate_wand` | `everyRecipeShownIsOnTheServer`, `anAbsentRecipeIsNeverAdvertised` |
| cast | Required | the advancement Let There Be (Kindle cast) | understand, wand | - | - | `invocations/kindle` (first_light understood) | `theRouteFollowsTheResearch`, `kindleCastsThroughSpellEngine` |
| compose | Required | a spell inscribed on a carried instrument | understand, wand | - | - | `composition/composing` (first_light understood) | `theRouteFollowsTheResearch`, `spellcraftShowsWhatIsHeld` |
| support | Recommended | an inscription that wards someone other than its caster (ward, with or without modifiers, not delivered here) | compose | - | - | - | `theRouteFollowsTheResearch`, `aWardForAFriendIsComposedAndCast` |
| equip | Recommended | a Kindled Lantern, a fire-set piece, a fire focus or the Fire Bangle carried or worn | understand | recommended: `jugcraft:lesser_fire_focus` (2 string, 2 gold ingot, 2 #coals, 1 amethyst shard)<br>recommended: `jugcraft:fire_bangle` (4 gold ingot, 1 #coals, 1 amethyst shard)<br>the sets, named when each of these exists: `jugcraft:pyromancers_hat`, `jugcraft:pyromancers_robes`, `jugcraft:pyromancers_leggings`, `jugcraft:pyromancers_boots`, `jugcraft:pyromaniacs_hood`, `jugcraft:pyromaniacs_tunic`, `jugcraft:pyromaniacs_pants`, `jugcraft:pyromaniacs_shoes` | general: `minecraft:lantern` | - | `theFirstLoadoutIsMadeInSurvival` |
| lair | Optional: after a boss | The Last Harvest or Unravelled | compose | - | - | - | `aBossIsHiddenUntilDiscovered` |
<!-- first-route:end -->

How a step is placed: done when the player's own state says so; otherwise waiting (Later) until the steps it waits for
are done; otherwise Not available here when this server has no way to it (the wand without its recipe); otherwise the
first required step that can be taken is Next (when no required step can be, the first recommended one) and the rest
Can do now. A boss fight is never Next. Understanding has three ways, any one of them enough, so none is called
required: three different specimens examined in the dark, one study at a Lampwright's Bench, or Research Notes. The
bench is not a way round the first examination in the dark (the dark step comes first, as states are reached in order),
but after it one study replaces the other two examinations; a study made earlier is kept and counts then. The cast step
is done by casting Kindle: once First Light is understood it is called "Cast Kindle" and says which key casts it (the
spell bar offers the invocations in the order they are known, Dawn Aegis first, so Kindle is usually on 2).

What the player makes along the way, where everything the route touches comes from, and what stays creative-only:
[the first loadout's record](concordance-first-loadout.md).

### Nothing before it is met (proposed, A-D9)

- The route's own research, First Light, is named from the start; other research, and words, invocations and spells,
  are named only on steps that need First Light understood. The stage report counts a route through research the player
  has not met ("and 1 more, through research you have not met yet") without naming it. An item's own description is
  shown as its tooltip gives it.
- A lair, and its rite, show once its token is carried (a Death Knell or a Mourning Wreath for the Hollow Acre, a
  Cursed Spindle for the Spindle Loft); its keeper once met (inside her lair, or owed her reward) or beaten.
- A boss's dossier then adds how support earns a share of her reward ([lair encounter credit](lair-encounter-credit.md)),
  what she gives (the acquisition text JEI shows, "with the default data", exact chances included: A-D15), the cheaper
  token her loot opens and the pieces made of it, how to leave, and, inside her lair, the player's own part in the
  current fight. What counts towards a share is named for each lair: her own creatures (not Vesperine's Grave Thralls),
  healing and warding friends, and her objective.

## Connections

- Existing input producer: the Concordance's research, evidence, invocations and composer (`Journal.research`,
  `StageProgress.describe`, `Composer.state`, `ComposedSpells.compile`), the server's recipes, the town's shops, the
  lairs' tokens, visits, owed rewards and first-win advancements, and a Circle Anchor's or Ley Pylon's own report.
- Existing output consumer: none; the encyclopedia only shows. Its links lead to the codex, the Spell Composer and the
  Concordance Journal.
- Technology connection: none in this slice (the technology route is a later chapter).
- Magic connection: the first route of the Arcane Concordance, from the first examination to a ward for a friend.
- Reachable entry path: the route's first step needs nothing; every required step's recipe uses Overworld vanilla
  things, and no required recipe needs a boss's loot (`check_encyclopedia`).
- Required vs optional; trade and solo routes: every way is labelled. A town's sales show only where a town stands, for
  offers on sale in today's theme (A-D11); Research Notes from a player who understands First Light are a Trade way.
- How this specialty stays useful without the others: every page is optional reading; nothing is gated on it.

## Balance and automation

None: no recipe, item, trade, reward or saved state is added. A recipe a data pack removes is not advertised.

## Multiplayer and persistence

- Server authority: the server writes every page from what it knows of that player. A request names no page and carries
  no text, only a sequence number and, when opened from the game, the block looked at (checked on the server: loaded and
  within 16 blocks, the command's own reach). A player may ask at most once every 10 ticks; one who asks sooner is
  answered at the first allowed tick (only their newest request waits). An answer goes only to a game that can receive
  it. Building a page reads the rules, the player's own records and inventory, the server's recipes and at most one
  block entity near the player; it writes nothing (`check_encyclopedia` forbids the writes).
- Bounds: 6 pages, 96 rows a page (the last says there is more), 12 grids a page, 9 icons a row, 4 alternatives a slot.
- Persistence: none. The page being read is remembered in memory only and forgotten on leaving a world; resuming
  where one left off is a proposed convenience, so nothing is saved.
- Disable behaviour: with the Concordance off, its pages are left out and a notice says so; with the lairs off, no
  dossier; a server without Jugcraft's pages shows only the Keys page and a notice.
- New network payloads: `jugcraft:encyclopedia_request` (client to server) and `jugcraft:encyclopedia` (server to client).
- New key: `key.jugcraft.encyclopedia`.

## Dependencies and assets

No new dependency. Modonomicon (required, pinned) is called once, on the client, to open a codex entry
(`client/compat/CodexLinks`); a failure says so and is logged once. The screens are vanilla widgets (no GuiLib view in
this slice). The inventory button is added through Fabric's screen events, with no mixin.

The button's sprite is the owner's, copied byte for byte by `tools/owner_art.py`. It came in a set with waystone
sprites, which are not used; that set's language file carries the credit "Icons by JoeCreates (CC-BY-SA 3.0)"
(`originals/Magic/lang/en_us.json`, `waystones.iconCredits`), which is kept here with the asset as the library's guide
asks (art/owner-library/MAGIC_ASSETS.md: retain supplied credits). Whether this scroll is one of the credited icons is
not known here; the owner's 9 October declaration says the library was remade from its bases
([PROVENANCE.md](../../art/owner-library/PROVENANCE.md)).

| Runtime file | Library source | SHA-256 |
|---|---|---|
| `textures/gui/sprites/encyclopedia/inventory_button.png` | `originals/Magic/textures/gui/sprites/inventory/inventory_button.png` | `3dc52840afcc95022192e52bff317fe8e2cc6ce753db087f79a96c013cf8b4a4` |

## Verification

- `python3 tools/check_mod_data.py` (with the new `check_encyclopedia`) and `python3 scripts/check_repository.py` pass
  locally; `python3 tools/owner_art.py --check` matches.
- Game tests, passed in CI (run [38435014953](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38435014953)): `EncyclopediaGameTests` (a newcomer sees only the first steps; the route follows the
  research; every recipe shown is on the server; an absent recipe is never advertised; switched-off features have no
  pages; Spellcraft shows what is held; a boss is hidden until discovered; the route names only real sales; the pages
  survive the trip; requests are gated, answered at their tick and the Inspect page reads only a near, loaded block;
  the page model keeps its place), `ConcordanceComposeGameTests.aWardForAFriendIsComposedAndCast`, and the client test
  `EncyclopediaClientGameTests` (one client on a dedicated server, log prefix `[encyclopedia-check]`): no other loaded
  non-debug key shares the default; the key opens on Discover, the key or Esc closes, and typing b in chat does not
  open it; the inventory button stands right of the recipe-book button over no slot or widget and follows it when the
  recipe book opens, a real click puts the 2×2 grid's things back and opens the encyclopedia, and Back reopens the
  inventory; in creative it shows on the Survival Inventory tab only, over no slot; with the examination recorded on the
  server the dark step is done, understood with a wand in hand the cast step is next, and Spellcraft's composer link
  opens the Spell Composer, whose Esc comes back; looking at a Circle Anchor the Inspect page is its own report; the
  Expeditions and Keys pages; GUI scale 4 and a 640×360 window. Screenshots `jugcraft_encyclopedia_discover_new`,
  `_inventory_button`, `_recipe_book`, `_discover_route`, `_spellcraft`, `_inspect`, `_expedition`, `_keys`,
  `_creative_button`, `_scale4` and `_small`, and, for the first loadout's A-07 view, `jugcraft_first_loadout_kindled_lantern`
  and `jugcraft_first_loadout_adept_wand` (each held in hand).

CI's history: the first run (38424079157) found Iris 1.11.7's *Shader pack selection* on the default key, I, so the
default became B (A-D7); the test now also logs every loaded default. A later run (38433352364) failed once on the
route step after R, because the test waited for the Spellcraft page, which is already there once First Light is
begun, and could read the pages from before the refresh; the test now waits for the answer to the request it sent.

Settled in CI by the client test: B is shared with no other loaded default; the button covers no slot or widget,
beside the recipe book or on the creative Survival Inventory tab (below the slots Trinkets adds there).

Not run: the full route by keys from a fresh world to a lair win as one test (the chunk's handoff maps each A-08 step
to the separate tests that cover it: [Unit 5](concordance-chunk-a.md#unit-5-evidence-and-handoff-a-08)); a person
following the route from a fresh world without reading source (A-04's acceptance is a human judgement); readability
judged by a person at other GUI scales (the screenshots are there); the narrator heard; two clients. Still unverified:
`BookGuiManager.openEntry` in Modonomicon 2.16.0 (read in 2.15.0's source; no test opens a codex link); a placeable
specimen being placed when aimed at a block.

## World and event applicability

Seasonal: the dossiers say whether a rite works on any night or only in the Halloween season, from the server's own
setting. Boss containment, loot and dimension travel are unchanged; the dossier only describes them.

## Rollout and open questions

Owner decisions, recorded as proposed in [the chunk's contract](concordance-chunk-a.md): the default key (A-D7), the
button and its place (A-D8), the discovery rule (A-D9), trade edges only from existing shops (A-D11), codex links without
the codex (A-D12), the route's support spell (A-D13) and exact drop chances "with the default data" (A-D15). Also for the
owner: a fresh player following only the encyclopedia is not told which token opens a lair (the lair step says a token
is made, not which), while JEI names every item and its recipe; naming the tokens there, or pointing to the recipe book,
belongs to A-D9.

Known limits:

- Discovery is read, not saved: a player who entered a lair, left, and used up their token (a Cursed Spindle) reads as
  not having found it until they carry one again or win.
- The dossiers cover the Witching Season's two lairs. The Glacier Hall and the Cinder Kiln, on main since #320, have
  none yet, and the lair step's token check names only the Death Knell and the Cursed Spindle.
- The Inspect page is read when the encyclopedia opens; refreshing keeps the block it was opened on.
- The route's equip step is done by carrying or wearing a Kindled Lantern, a fire-set piece, a fire focus or the Fire
  Bangle (the wand and the bench are steps of their own).
- Back navigation is in this slice (Backspace, through the last 16 pages turned to).
- Later (outside Chunk A): the GuiLib route canvas, search, forward history and bookmarks, a last page remembered across
  sessions, the technology route, quests, other schools' chapters, invocation cooldowns, bench and machine inspection.
