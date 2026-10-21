# Academy personnel and everyday life
The following characters/titles are **new Jugcraft proposals**, not existing registered entities. Preserve existing humanoid model/AI infrastructure and use robe textures/equipment/profession presentations before attempting bespoke models. Inspect available villager/trader identities and pathfinding first.

| NPC | Visible title | Station | Special identity and function |
|---|---|---|---|
| Magister Elowen Vey | Arch-Mage | AMG-02 | Head of College; optional grand research acknowledgements; deep midnight-blue/gold robe |
| Professor Arctus Vale | Master of Radiance | ELM-01 | Intro instructor and safe spell demonstration; ivory robe, blue hem |
| Mistress Serin Ashcroft | Ember Lecturer | WRK-01 | Hearthbinders, thermal experiments and controlled combustion; auburn robe |
| Scholar Nivra Frost | Rime Lecturer | WRK-03 | Stillness, preservation, frost studies; pale-blue embroidered robe |
| Tutor Orren Thale | Tempest Lecturer | WRK-04 | Storm observations and safe instrument use; cobalt/silver robe |
| Warden Beryn Stonehand | Strata and Circles | ELM-03 | Boundaries, structures, runic geometry; slate robe |
| Sister Valenne Moss | Verdance and Healing | WRK-05 | Living specimens, healing support; muted green robe |
| Keeper Meren Quill | Archivist of Echo | ARC-01 | Librarian, research ledgers, lore indexes; purple/bronze robe |
| Provost Corvin Dusk | Restricted Studies | ARC-03 | Ethical research and Hollow material warnings; black/violet robe |
| Instructor Lyssa Veil | Illusion and Perception | ATT-03 | Sensory puzzle tutorial using existing capabilities; lavender robe |
| Custodian Harrow | Midden Warden | MID-01 | Warnings, recovery route and dungeon rumors; dark practical robe |
| Apprentice Jori Fen | Student Representative | ATT-01 | Orientation, dorm guidance and teacher schedules; simple blue apprentice outfit |
| Quartermaster Heda | Supplies and Dining | SRV-01 | Trading/basic supplies using legitimate economy; russet apron over robe |

## Technical contract
- Titles as components or names above entities; outfit textures and equipment IDs must be stable and documented. Use proper source names only in user-facing UI where assets legally allow.
- Unique academy NPCs spawned/loaded once per structure instance; save persistent ID, profession, station, home anchor and schedule state; never duplicate after chunk reload.
- Restrict AI roaming to safe areas; no standing in stair bottlenecks, falling from cliff, wandering into hostile Midden.
- Display dialogue using existing acceptable GUI/text APIs with server validation; use simple and consistent interactive dialogue tree instead of custom RPG voice system.
- Time of day: morning lessons, afternoon research, evening commons, night rest. Suppress high-cost pathfinding when nobody is nearby; deterministic, graceful when station blocked.
- Safe on both integrated/dedicated servers; clear missing-owner states, no per-player permanent NPC duplication.
- Visually differentiated wardrobe **without** replacing every human model. Check asset catalog and license/provenance first.
- Research/reward actions must reuse the Concordance actual data contract and cap rewards server-side; no free high-tier research bypass.
