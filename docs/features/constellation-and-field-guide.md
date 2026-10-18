# Constellation composer and illustrated field guide

> Publication update, 18 October 2026: the owner accepted the combined alchemy presentation and authorized publishing the alchemy additions and UI fixes. Historical local-only checkpoints below are superseded. Current scope and validation are recorded in [Alchemy interfaces and access](alchemy-interface-and-access.md).

Owner-directed local redesign, 16 October 2026. Implemented with OpenAI Codex (GPT-6).
Based on local `364a622129300a46643e812fe6d2aa4053db5bb5`; no main push is part of this slice.
Open PRs were refreshed before implementation; none of the 27 open PR titles described overlapping composer/encyclopedia UI work.

## Player experience

The Spell Composer now opens the same narrated constellation screen in the complete pack and without optional UI libraries. There is no text field. Choose Reach, Target and Effect stars, then optionally Refine. Learned parts come only from the server. Effects toggle, up to the compiler's existing three-effect limit. Refine explicitly selects the part to enhance; compatible modifiers toggle independently. Repeat and branches retain the existing grammar and instrument limits. Undo, Reset, Load and Erase remain available. Closing retains the draft; leaving the world clears it.

A live side page describes the draft, capacity, authoritative Focus/cooldown, validation and inscription. Detailed compiler explanations are optional; errors remain visible. Star traces and gentle twinkling honor reduced motion. Tab/Enter activates narrated widgets; left/right changes composing stages; Ctrl+Z undoes. Server checks, saved inscriptions, Spell Engine casting, research, item revisions and payload bounds remain unchanged. No new spells or unlocks are introduced.

The encyclopedia opens as a two-page illustrated book, with six clear subjects: Technology, Magic, First steps, Spellcraft, Bestiary & expeditions and Controls. An undiscovered subject gives an empty-state explanation, rather than fabricated lore or unlocks. Looking at an inspectable magic device retains the direct field-inspection route.

Live pages split headings and progression steps into a topic directory. Only the selected topic's facts, recipe grids, item illustrations and links are shown. Details adds exact figures. Topic descriptions and recipes come from the same server snapshots. Recipe ingredient illustrations wrap within a page, and recipe station labels sit below their grids. The spellcraft page has a direct Composer button. Contents and Esc provide navigation; inventory return still closes/reopens the inventory safely.

Technology and Magic have their own book directories, with Topics and Milestones separated. Selecting a topic previews it; Read chapter opens its existing in-depth handbook/codex entry. No recipes or duplicated content databases were invented. The handbook returns to the directory; Modonomicon and Journal retain their own back behavior.

## Art and accessibility

`art/owner-library/originals/Magic/gui/spell_book_template.png` is copied unchanged into `assets/jugcraft/textures/gui/encyclopedia/book.png`. The source remains intact; `tools/encyclopedia.py` registers it with the shared owner-art importer. Existing item sprites illustrate subjects. UI constellation lines, stars and the home compass ornament are runtime geometry, not copied external artwork. Owner collection provenance/notices remain applicable.

The native screen and widgets retain keyboard traversal and narration on both installations; GuiLib remains available for the other existing screens, but its former text-entry composer delegates to this shared screen. This is custom presentation, not a new UI framework or dependency. Paper text is drawn without shadows for legibility. Decoration is bounded and performs no networking/world scans. Reduced motion freezes decorative animations.

## Validation

On 16 October 2026, Minecraft 26.3 / Fabric Loader 0.19.5 / Java 25, with the unchanged framework lock:

- All **1,758 server game tests passed** during `gradlew build runClientGameTest` (offline). That combined run subsequently failed a client-test lookup of the numbered Path button; the test was corrected to resolve its translated label with the path number.
- `gradlew assemble runClientGameTest -PclientTests=ConcordanceComposerClientGameTests,EncyclopediaClientGameTests,EncyclopediaRoutesClientGameTests --offline --no-daemon -Pkotlin.compiler.execution.strategy=in-process`: **PASS**, 3m 13s, with optional libraries. The same command with `-PjugcraftOptionalIntegrations=false`: **PASS**, 3m 8s, including the final compact button labels.
- Those clients exercise no-typing selection, invalid-enhancement rejection, undo, server inscription, actual Spell Engine casting (one 2-Focus charge), a second visual branch, focus restoration, inventory/recipe-book safety, progression snapshots, inspection, guide links and handbook return. The route test also verifies Enter activates a focused Contents button rather than reopening the selected topic. Screenshots inspected at the normal size, GUI scale 4, 640×360 and 1280×800.
- Full `tools/check_mod_data.py`: **PASS**, 2,913 material IDs, recipes/data and art audit (8,516 block/item models, 3,709 quad parts, 171 combined scenes). Final focused composer/encyclopedia audits and generated UI translation consistency: **PASS**. No server/compiler checks were removed.
- `python -m unittest discover -s scripts/tests -p test_encyclopedia.py -v`: **4 passed**. `test_packaging.py`: **9 passed**. `scripts/check_repository.py`, `scripts/package_modrinth.py check` (18 pinned libraries), and `git diff --check`: **PASS**.

The local `.mrpack` contains the built Jugcraft JAR and an independent fresh client-test world named **Jugcraft Field Guide**, with an Adept Wand and First Light mastered for UI review. The saved singleplayer identity and player inventory/research were inspected. The output folder records the final source commit, JAR/pack hashes, screenshots and logs. Earlier manual worlds and profiles remain intact. No GitHub push, merge, or deployment.

Human gameplay/visual approval, Modrinth launcher import, two independent multiplayer clients, spoken narrator output and a complete survival progression run remain unverified. Existing deep Modonomicon and Journal screens keep their own presentation. The bestiary still contains the existing encountered-lair dossiers, not a newly written entry for every mob.

## Follow-up after owner feedback

- Broader bestiary content is still limited to the existing encountered-lair dossiers; the visual redesign does not add dossiers for every mob.
- Optional encyclopedia search, favorites and a persistent reading history can be added after the owner approves the chapter/topic organization.
- Existing deep Modonomicon and Journal screens retain their own presentation and back behavior. This slice replaces the encyclopedia directory and live pages, and the composer.
