# Jugcraft design: factories, magical workshops, and worlds worth exploring

Status: owner-directed design scope. Minecraft Java Edition 26.3 + Fabric is selected. The systems below are planned, not implemented; example names and recipes remain proposals.

## The player promise

Build a home you love returning to. Become an engineer, workshop mage, farmer, explorer, hunter, beast keeper, spacefarer, or a mixture. Develop a specialty deeply, exchange useful goods and services with other players, and undertake shared projects that connect those specialties.

Factories and magical workshops are the center of the experience. They are branching, independently useful paths with selected collaboration milestones. Technology brings repeatability, precision, production scale, and logistics. Magic brings transformation, attunement, movement, combat, and access to other realms. Neither simply replaces the other.

The Overworld should feel coherent, comfortable and inviting. Dangerous caves, dungeons, hostile frontiers and bosses provide adventure with readable boundaries and preparation. Cozy settlements and friendly creatures remain valuable at every stage.

## Branches, not a mandatory checklist

- Let players start technology, magic or agriculture with obtainable local resources. Do not require advanced output from another branch to begin a specialty.
- Offer several specialties inside each branch; no player needs to master every school or profession.
- Make cooperation valuable through catalysts, refined materials, food, transport, tools, discoveries and services. Trading can satisfy cross-branch material needs; avoid unrelated personal-research gates on every recipe.
- Use selected milestones for strong collaboration, such as an advanced workshop, observatory, rocket expedition or stable realm gateway. Do not force both systems into every small recipe.
- Provide documented slower solo routes or staged self-production for necessary cross-branch supplies. Do not require simultaneous online players to unlock essential progression.
- Specialties must have continued demand and useful choices. Later automation should reduce repetitive work while keeping earlier producers useful.
- Shared milestones should open multiple destinations or specializations, not funnel every player into one final machine or spell.

## Progression framework

Tiers describe complexity and access, not a requirement to finish every branch. Each feature proposal specifies its own dependency path.

| Stage | Player opportunities | Shared connections |
| --- | --- | --- |
| Discovery | Cozy home, crops, livestock, basic mining, hand tools, first spells | Obtainable crops and materials support several entry paths |
| Workshops | Early factories, rituals, specialty farming, tame companions, cave expeditions | Refined parts, botanical reagents and simple catalysts circulate between professions |
| Specialization | Logistics, advanced agriculture, magic schools, dungeon expeditions, rare equipment | Specialist goods, enchanting/crafting services and discoveries improve other branches |
| Expeditions | Rocketry, moons and planets; portals into magical realms | Agriculture provisions journeys, industry builds equipment, magic supplies specific enhancements |
| Shared wonders | Larger factories, realm networks, observatories and community builds | Sustained demand from multiple specialties; several worthwhile projects, no universal endgame ladder |

## Content pillars

| Pillar | Planned scope | Connections |
| --- | --- | --- |
| Industry | Processing, power, machines, storage, transport, automation, precision fabrication | Shared ores, farming equipment, ritual hardware, rocketry |
| Magical workshops | Reagents, alchemy, inscriptions, enchanting, spellcraft and ritual automation | Prepared substrates, agricultural inputs, tool upgrades, travel and combat |
| Agriculture | Crops, soils, orchards, livestock, food and breeding; magical cultivation and technological farming | Reagents, fibers, fuels, food buffs, expedition provisions and greenhouse habitats |
| Cozy biomes | Believable terrain/climate transitions, warm palettes, inviting flora, building materials and ambient wildlife | Settlement identity, regional farming, gathering and exploration |
| Dangerous underground | Distinct cave biomes, environmental hazards, dungeons, enemy ecology and bosses | Shared resources, discoveries, instruments, relics and progression options |
| Space | Rocket construction, launch preparation, moons, planets and eventual galactic exploration | Propulsion, navigation, materials, agriculture, habitats and selected magical enhancements |
| Magical realms | Distinct destinations reached by portals, spells and teleportation infrastructure | Workshop research, exploration, reagents, ritual components and settlement links |
| Creatures | Farming, hunting and exploration depth; friendly wildlife, tamable companions and pets; enemies and bosses | Ecology, husbandry, loot, crafting, defense and discovery |
| Rare equipment | Distinctive appearances, special abilities, curated loot pools, meaningful build options | Exploration rewards plus technological refinement or magical attunement |
| Seasonal content | Halloween themes, then Christmas-esque December themes | Permanent systems gain seasonal recipes, encounters, crops, decor and workshop projects |

Detailed contribution opportunities: [specialties and content briefs](CONTENT_BRANCHES.md).

## Integration examples, not final recipes

1. A farmer grows oilseed and alchemical herbs. An engineer turns oilseed into machine lubricant; a workshop mage prepares a catalyst from herbs and a refined mineral. Both improve specialized agricultural tools.
2. An ore has a useful mechanical alloy path and a useful magical instrument path. Precision-made fittings support a wand or ritual apparatus; an inscription adds a distinct machine behavior rather than a universal free speed multiplier.
3. A cave dungeon yields a bounded relic and a research discovery. One supports a tool ability, another enables a workshop recipe; loot does not make all manufactured equipment obsolete.
4. Rocket expeditions need fabricated parts, fuel, food and habitat supplies. A magical specialist can provide a navigation or protection option at a selected milestone. Not every engine component needs a spell.
5. Realm explorers bring back a reagent that improves a branch of farming; industrial glass and fittings help build the corresponding cultivation chamber.
6. Halloween harvests and cursed encounters reuse crop, loot and workshop systems. December projects reuse husbandry, fabrication, food, winter magic and decorative crafting.

For every mandatory dependency, prove a reachable entry path. No first ritual needs a catalyst that can only be made by that ritual; no first machine depends on its own output.

## Shared materials, resources and rewards

Use one canonical material identity and common tags rather than parallel sets of nearly identical ores. Each new material needs a location/ecological reason, acquisition tier, useful technology application and magical application (or an explicit narrower-role rationale). Reuse current materials before adding another ore.

Energy and magical resources remain conceptually distinct. Conversions require defined units, direction, throughput, loss, upkeep and limits; audit entire cycles for duplication or positive gain. Do not add one independent currency per contributor or school.

Rare loot must look recognizable, state its ability clearly, and provide tradeoffs, cooldowns or bounded use. Define drop sources, weights, duplicate handling and whether an alternate acquisition route exists. Avoid making a very low-probability drop the only gateway to necessary progression. Review crafted and found gear together.

## World, safety and persistence

Cozy does not mean consequence-free, but danger must be intentional and legible. Describe biome transitions, cave hazard signals, dungeon containment and boss boundaries. Player summons, hostile automation and dark magic must respect server permissions and protected settlements. Pets need ownership, friendly-fire policy and safe persistence.

Space and magical realms should have distinct identities and access rules. Teleportation must not bypass progression, destination safety, claims or expedition logistics without an explicit design decision. Limit persistent dimensions, forced chunks, entities, network scans and per-tick work. Test return travel and stranded-player recovery.

New worldgen must explain existing-world behavior: old chunks are not silently rewritten. Seasonal activation must not remove registered blocks/items, invalidate inventories or strand players when the season ends.

## Acceptance contract

Every gameplay addition identifies its primary specialty, player purpose, stage, reachable entry path, input producer, output consumer, connections to other specialties, balance costs, multiplayer rules, persistence, performance budget and test plan. Say which links are required, optional or obtainable through trade. Infrastructure, accessibility, cosmetic and pet additions can explain experiential/support value instead of inventing arbitrary resource gates.

Ask: What choice does this add? Who benefits from producing or using it? How does it respect cozy homes, adventure boundaries and existing progression? If it disappears, what useful connection or experience is lost?
