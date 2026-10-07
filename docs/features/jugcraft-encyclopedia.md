# Jugcraft Encyclopedia

Status: owner-approved feature direction, recorded 6 October 2026; UI and quest implementation not started by this contribution.
Proposal issue: direct owner request during progression planning; this feature is independently documented and tracked in the [TODO list](../TODO.md#jugcraft-encyclopedia).
Owner: jimbozoomer-byte.
Target milestone and tier: guidance available from the beginning of a world and useful throughout technology, magic and their specialties. No delivery date is assigned.
Primary supported roles: every Jugcraft player, including engineers, magical specialists, farmers, explorers, builders and players learning a particular pathway.

## Explicit owner requirements

- Build an **entire Jugcraft Encyclopedia UI**, rather than treating the request as a few additions to the current industrial plan.
- Explain the overall **machinery / technology route** and the overall **magic route**.
- Provide **in-depth guidance for each pathway within those routes**.
- Include **quests** as part of the same experience.
- The Encyclopedia **must not be an item**. Make it openable from the inventory and through a configurable keybind, without acquiring or carrying a book.
- Keep this feature independently documented and on the project TODO list, with all ten owner-supplied UI reference images.

The owner also chose an approximately **2–4 active-hour target for first electricity** from a fresh world when following known recipes, excluding optional building detours. That is a progression-balancing/playtest target, not a timer, unlock condition or measured result. The encyclopedia should make that essential route understandable while presenting the workshop's wider products and specialties as worthwhile choices.

Choosing guidance option A remains the baseline: an optional guide with capabilities, useful products and recipe hints. The owner's clarification expands it into this full UI with quests. Reward quantities, team completion and spoiler rules have not been selected; do not infer mandatory quest gates or a reward economy from the presence of quests.

## Player experience

Open the Encyclopedia using a clearly labeled inventory button or its remappable keybind. Neither entry requires crafting, a carried item, first electricity, a completed quest or a magical ritual. Both open the same feature. A returning player can resume their last chapter or page; that convenience is proposed rather than a finalized interaction rule.

Start with technology and magic route overviews, then enter a specialty to read its practical guide. Players should be able to answer: What does this pathway let me make or do? What do I need to start? Which steps are essential? What are my alternatives? Which products or upgrades are optional? Where does this output become useful?

The Encyclopedia combines explanations, visual dependency routes and quests. A route graph provides orientation; a detailed page explains the actual process. The UI should remain useful to players who ignore quests or specialize in one branch. Quest completion displays progress and suggested goals; it does not introduce an unrelated personal-research gate to every machine, spell or recipe.

## Proposed screen structure

These layouts develop the approved feature; exact colors, node frames, panels and interactions remain to design.

| Area or view | Purpose | Proposed behavior |
| --- | --- | --- |
| Home / route overview | Choose technology or magic and understand the wider mod | Prominent route summaries with links into specialties, shared topics and quests |
| Chapter navigation | Keep large amounts of content navigable | Collapsible categories and named pathway chapters with recognizable icons |
| Pathway canvas | Explain dependencies and sideways choices | Pan/zoom graph, grouped related nodes, prominent milestones and a clear legend |
| Entry detail view | Teach one machine, material, spell, ritual or process | Readable explanation, actual recipe/process data, operating instructions and useful next links |
| Quest view | Follow optional goals and see progress | Objective details, progress, completion state and links back to the relevant entry or pathway |
| Search and navigation tools | Find an answer without completing the whole route | Search, breadcrumbs and back/forward navigation; bookmarks/pins are proposed conveniences |

Keep essential material/capability prerequisites distinct from recommendations, trade alternatives and quest-order suggestions. A visible connection does not always mean a mandatory requirement. Use labels and shapes as well as color for available, in-progress and completed states. Avoid an unreadable universal graph containing every item at once.

Inventory-button placement and default key must be chosen during UI design. The keybind should appear in Minecraft's controls settings, handle conflicts and avoid opening while the player types in another text field. Opening/closing should return players predictably to their previous inventory/game view; exact screen-history behavior needs UI testing.

## Technology content

The technology overview should separate the minimum capability route from the wider manufacturing possibilities. Earlier workshop equipment remains useful after electrification; motors can drive existing machinery, and larger equipment is an optional throughput/efficiency choice unless a genuinely new process needs a different capability. Steel and electricity are parallel capabilities under the existing planning direction.

Candidate chapters, aligned with current source and separately published plans:

- Homestead materials, ceramics, firing, construction palettes and refractory/insulating parts.
- Mechanical power, shafts and workshop tools; metal preparation, alloys, plates, wire and other basic components.
- First electricity, reachable silicon/basic circuits, generation, storage and motor conversion.
- Steel, larger industry, material handling and factory logistics.
- Industrial agriculture and finished goods: fibers, textiles, oils, coatings, paper, wood products, rubber and suitable food/feed processing.
- Regional mineral sands, extraction, physical separation and abrasives/ceramic ingredients.
- Shared chemical refining, appropriate recovered compounds/metals and expanding rare-earth applications.
- Waste recovery, equipment disassembly, pollution monitoring and treatment choices.
- Precision manufacturing, control, advanced power and expeditions where the corresponding feature is available.

For each machine or production pathway, explain its inputs and useful outputs, reachable entry path, construction and assembly, ports/feeds, power or heat, operating steps, recipe variants, byproducts, compatible upgrades and common failure states such as full outputs or missing feeds. Numbers and recipes should follow canonical mod data rather than a second manually maintained recipe database.

Clearly distinguish implemented content from owner-approved plans and examples. The Encyclopedia must not advertise a future machine or changed recipe as available in the installed version merely because it appears in a planning document.

## Magic content

Give magic its own overall route explanation and detailed specialty chapters. It is not a small miscellaneous subsection of machinery. Begin with reachable starter magical crafts and branch into distinct schools, rituals, prepared reagents, apparatus, spells, inscriptions, travel and exploration where those features exist.

The [specialty map](../CONTENT_BRANCHES.md#magical-workshops-and-schools) currently names Fire, Ice, Storm, Earth, Necromancy, Blood, Vampirism and Cursing as school/specialization directions. Preserve their distinct identities and clearly label proposed rather than implemented content. Blood ritual craft and Vampirism's opt-in transformation should not collapse into one interchangeable route. Future schools or pathways can extend the content without redesigning the entire UI.

For each pathway, explain its identity, accessible starting point, prepared materials/apparatus, costs, spell or ritual sequence, useful applications, deeper branches and relevant risks/counterplay. Include cultivation, crafting and exploration uses alongside combat. Explain selected industry/agriculture links and solo/trade alternatives, without requiring every mage to complete every industrial specialty.

## Integrated quests

Quests are an explicit owner requirement. Candidate quest families teach first steps, demonstrate a useful process, suggest specialty projects and celebrate major milestones. A mechanical workshop goal can lead into first electricity; an optional textile or construction project can show sideways depth. Magic quests should teach their own pathways rather than reuse the machinery route with different icons.

Detailed quest design still needs objective types, prerequisite semantics, discovery rules, notification behavior and any rewards. If a quest-order prerequisite is used, it must be distinguished from the actual crafting/process requirement. Do not turn optional guidance into a compulsory quest-completion ladder without a separate owner decision.

Proposed progress/reward requirements for implementation:

- Stable quest/entry identifiers and explicit versioned data so chapter reorganization does not reset earned progress.
- Server authority for authoritative completion and any rewards; validate objectives and prevent duplicate claims, including simultaneous clicks and reconnects.
- An explicit per-player versus shared-party policy, reusing the existing party system where appropriate rather than silently inventing teams.
- A defined treatment for crafting, obtaining through trade, using machines/rituals, previously owned items and consuming/detecting inventory objectives.
- Clear full-inventory, interruption, disabled-feature and migrated-save behavior if rewards or objectives involve those states.

No reward economy, repeatable material farm or new currency is approved by this record. Rewards can be decided later; completing this UI task is not permission to add them automatically.

## Existing source and compatibility

The inspected main source already has an item-based Engineer's Handbook, generated by [tools/handbook.py](../../tools/handbook.py), rendered by [HandbookScreen](../../src/client/java/io/github/jimbozoomer/jugcraft/client/HandbookScreen.java), and opened by [EngineersHandbookItem](../../src/main/java/io/github/jimbozoomer/jugcraft/guide/EngineersHandbookItem.java). It contains useful prose, recipe data and progression steps. Those observations describe source, not new gameplay test results.

The approved Encyclopedia is accessed without an item. Reuse useful handbook content/data generation where appropriate, but do not mistake the current book screen for completion of the requested feature. Existing saved book registrations need a compatibility decision: for example, an old book could remain a legacy shortcut to the same UI. Do not remove saved identifiers as part of documentation or assume a new craftable Encyclopedia item is wanted.

[PartyClient](../../src/client/java/io/github/jimbozoomer/jugcraft/client/PartyClient.java) provides an existing keymapping example. Existing advancements and party data are integration starting points, not proof that a complete quest backend already exists. Detailed architecture and dependency choices require review at implementation time.

Keep rendering/input in client code and authoritative progression/rewards on the server. Opening a guide should not require world scans, chunk loading or continuous server polling. Long pages and large chapter graphs need bounded rendering, sensible loading and readable behavior at ordinary resolutions and GUI scales.

## Visual references and provenance

All ten original screenshot files are preserved unchanged in [the reference folder](../images/jugcraft-encyclopedia/README.md), with dimensions, byte counts and SHA-256 hashes in [reference-manifest.json](../images/jugcraft-encyclopedia/reference-manifest.json). The [TODO list includes their embedded gallery](../TODO.md#reference-gallery), so this feature and its references remain visible outside the industrial planning records.

| Reference | Design lesson to consider |
| --- | --- |
| [01: sidebar and bounty board](../images/jugcraft-encyclopedia/01-chapter-sidebar-bounty-board.png) | Collapsible chapter navigation and a grouped quest overview |
| [02: completed magic route](../images/jugcraft-encyclopedia/02-completed-magic-route-graph.png) | Branching paths, completion checks and completion notices |
| [03: resource/process graph](../images/jugcraft-encyclopedia/03-branching-resource-process-graph.png) | Dependencies and distinct processing branches |
| [04: tiers and specialties](../images/jugcraft-encyclopedia/04-tier-and-specialty-navigation.png) | Route and specialty navigation beside a readable progress graph |
| [05: main and side pathways](../images/jugcraft-encyclopedia/05-main-and-side-pathways.png) | An overall route alongside deeper workshop and magical chapters |
| [06: themed route columns](../images/jugcraft-encyclopedia/06-thematic-route-columns.png) | Visually separated paths, large milestones and smaller supporting nodes |
| [07: focused advanced route](../images/jugcraft-encyclopedia/07-focused-endgame-route.png) | A compact major-milestone view with grouped prerequisites |
| [08: technology, magic and encyclopedia](../images/jugcraft-encyclopedia/08-tech-magic-encyclopedia-sections.png) | Dedicated technology/magic sections plus wider reference categories |
| [09: illustrated overview](../images/jugcraft-encyclopedia/09-illustrated-chapter-overview.png) | Chapter illustration, visual grouping and prominent guidance markers |
| [10: logistics/resource groups](../images/jugcraft-encyclopedia/10-grouped-logistics-resource-paths.png) | Hierarchical navigation and related production nodes grouped together |

The owner supplied these screenshots for UI planning. Upstream source URLs, authors and license grants were not supplied. They retain their respective creators' rights and are excluded from the repository's default MIT grant; this record does not grant reuse of depicted artwork, logos, portraits, code or textures as Jugcraft runtime assets. Implement an original Jugcraft presentation using approved/original assets. No external quest mod or copied UI code is selected by these references.

## Implementation TODO and verification

The canonical checklist is in [TODO.md](../TODO.md#jugcraft-encyclopedia). Build this in focused slices: inventory/keybind access and navigation, readable technology/magic content, route views, then authoritative quest tracking and the chosen reward/team behavior. The sequence is proposed; each slice needs an independently usable result and truthful evidence.

Future verification should cover both entry points without a carried item, key remapping/conflicts, inventory return behavior, search/history/navigation, zoom/pan and GUI scales, accessibility, translated content, recipe-data consistency, disabled-feature handling, and large chapter performance. Quest behavior needs two-client, restart/reconnect, concurrent/duplicate-completion and save-upgrade checks appropriate to the final design. An empty quest screen or graph mockup does not complete the entire Encyclopedia.

This contribution adds documentation, TODO navigation and reference images only. Repository/link checks, PNG integrity and byte-for-byte reference-copy checks apply. It changes no runtime UI, recipes, items, registries, quests, dependencies or saved data, and provides no new build or game-test evidence.

## Remaining design choices

Default key and inventory-button placement; visual style and screen layout; graph/entry data schema; spoiler/discovery behavior; quest objective and reward policies; individual/shared progress; old-handbook behavior; exact content coverage for each implementation slice. These details remain open without reopening the explicit owner requirements.

AI-assisted planning documentation: OpenAI Codex, GPT-6 family. No gameplay feature implementation is included.
