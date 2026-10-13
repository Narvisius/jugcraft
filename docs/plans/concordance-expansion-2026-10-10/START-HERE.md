# Jugcraft magic expansion — start here

**Planning checkpoint 1 · 10 October 2026 · prepared by OpenAI Codex**

The next step is to deepen and connect the systems already built. Main already contains a substantial Concordance foundation, Ember content, Thallite, two full boss encounters, industry, companions and the custom World Designer. Rebuilding that foundation would waste work and create conflicting systems.

This pack is based on main at `4b36d2254fd76e7db2a4cfa6ec488d746a999f1f`. The Yeti's hall was an open contribution at review; the boss itself remained planned. New designs below are proposals. This planning pass changed no gameplay code and did not run Minecraft.

## What I recommend first

**Chunk A: make the existing magic fully usable.** Add a visual spell composer, a clear first survival journey, the encyclopedia entry point, and fair encounter credit for healing, warding and objectives. Then build **Chunk B: Rime, Strata and the Yeti** against the existing Glacier Hall plan.

This gives you complete experiences to try before expanding into dozens more systems.

## The major expansions

- **Distinct magical traditions:** each gets exploration, workshop and combat uses. Shared Principles/resources remain intact.
- **Connected workshops:** plants → assays → preparations → rituals → equipment or worker services, with optional industrial automation.
- **Pacts:** a complete Stolas experience first, followed by a shared framework for 72 genuinely distinct spirits.
- **Bosses:** preserve Vesperine, Tatterlace and the Yeti; deepen the seven other existing trophy-boss ideas; add six detailed new encounters.
- **New encounter ideas:** Glass Abbot, Verdigris Warden, Astral Adjudicator, Gilded Assayer, Red Orchard Regent and Somnolent Cartographer.
- **Custom world authoring:** terrain/biomes, exact supported settlements, magical regions and lair entrances, with honest limits for each placement mode.
- **Better presentation:** concrete screens, animation ownership, useful Jade/JEI integration, approved asset reuse and optional-library fallbacks.

Core crafting, farming and electricity stay reachable without boss kills. Boss rewards provide distinctive choices and useful extras.

## Read only what you need

| For you | Read |
|---|---|
| What exists, what is open and what needs caution | [01 — Current state](01-current-state-and-integration-map.md) |
| Schools, progression and complex spells | [02 — Spellcraft](02-magic-progression-and-spellcraft.md) |
| Gardens, alchemy, rituals, spirits and transformations | [03 — Workshops and pacts](03-workshops-ecology-and-pacts.md) |
| Your existing bosses and Yeti plan | [04 — Existing encounters](04-existing-bosses-and-shared-encounters.md) |
| Detailed future boss ideas | [05 — Boss compendium](05-new-boss-compendium.md) |
| How magic fits factories and materials | [06 — Technology](06-technology-and-economy.md) |
| The custom editor and magical worlds | [07 — World Designer](07-world-designer-and-realms.md) |
| Interfaces, animation, textures and libraries | [08 — Presentation](08-interfaces-animation-and-assets.md) |
| What Claude should build, in what order | [09 — Delivery chunks](09-claude-delivery-chunks.md) |
| How each chunk should report back | [10 — Validation and check-ins](10-validation-and-checkins.md) |
| Exact first-chunk coding tickets and protocol | [12 — First-chunk tickets](12-detailed-first-chunk-tickets.md) |
| Researched mechanisms and primary source links | [11 — Research](11-research-notes-and-sources.md) |

## Give this to Claude

Use [CLAUDE-MASTER-PROMPT.txt](CLAUDE-MASTER-PROMPT.txt) with the whole folder, or attach [the combined document](Jugcraft-Magic-Expansion-Plan.txt). The prompt tells the agent to refresh current source, reuse approved libraries, finish one substantial chunk, verify it, show a concise checkpoint and wait for your input before the next chunk.

Each chunk contains several reviewable units. It is not a single enormous unreviewable PR, and it is not a sequence of tiny permission questions.

## Decisions at this checkpoint

1. **Starting priority:** I recommend usable foundations, then winter/stone and the Yeti. Would you prefer the living workshop, Stolas or the editor first?
2. **First wholly new boss:** I recommend the Glass Abbot because light-routing gives magic and support roles a distinct job. The Verdigris Warden is the stronger choice if you want gardening and a nonlethal resolution first.
3. **Progression rewards:** I recommend quests guide learning and offer modest one-time supplies/cosmetics, while existing research remains the real progression. A more reward-heavy quest structure needs a separate economy decision.

The industrial choices already recorded are preserved, including automatic gas pressure, no routine machine maintenance and pollution-driven raiders without crop or biome damage. No need to decide those again.

These questions are recommendations for the next step, not hidden approvals already recorded in the plan.
