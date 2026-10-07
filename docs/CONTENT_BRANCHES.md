# Specialties and contribution briefs

This is a contribution map for the owner's vision, not a claim of shipped content or guaranteed acceptance. You may start one focused prototype in your fork and submit a draft PR without an approved issue. Reuse shared interfaces and build one usable slice per PR. Coordinate major shared-API changes first. Example specialties may be refined during review.

## Factory engineering

Possible specialties: metallurgy and processing; power generation/storage; precision fabrication; item/fluid logistics; agricultural machinery; rocketry and habitats. Each machine has an understandable role, operating cost and throughput. Start with shared resource/recipe interfaces so contributors do not create incompatible cables, inventories and power systems.

Idle machines should do negligible work. Cache networks and invalidate on topology changes. Cap scans, transfers, active jobs and work per tick. Document behavior on unload/reload, full outputs, interruption and simultaneous use. Performance claims require realistic factory benchmarks, not just an empty test world.

## Magical workshops and schools

The [Styxhexenhammer and Nightglass Observatory](features/styxhexenhammer.md) prototype adds a reference-led dark wizard/herbalist, the owner's brick observatory and timber greenhouse reconstructed from screenshots, and eight decorative flower cultivars. Placement is operator-controlled; natural discovery, flower uses and deeper magic integration remain future work.

Magic supports transport, crafting and combat. Every school needs a distinct identity, an accessible starting spell or craft, deeper specialties, and useful cross-system interactions. Effects below are proposal seeds; exact mechanics and numbers need review.

| School | Workshop, farming or exploration possibilities | Combat identity and boundaries |
| --- | --- | --- |
| Fire | Controlled heating, smelting assistance, kiln work | Heat, ignition and area denial; fire spread respects claims/configuration |
| Ice | Preservation, cooling, frost cultivation, temporary paths | Slowing, barriers and control; bounded terrain changes |
| Storm | Charging, weather instruments, movement tools | Lightning, chaining and mobility; capped targets and weather effects |
| Earth | Soil care, shaping, mineral sensing, construction tools | Armor, barriers and terrain control; no unrestricted claim bypass or excavation |
| Necromancy | Bound servants, limited labor, dungeon knowledge | Minions and attrition; strict entity/work caps and ownership |
| Blood | Risk/reward rituals, vitality-based catalysts | Health costs, lifesteal and sacrifice mechanics; server validation prevents healing loops |
| Vampirism | A transformation/specialization path with thematic senses and traversal | Feeding and survival tradeoffs; opt-in player transformation and clear counterplay |
| Cursing | Hexed instruments, bounded bindings, controlled debuffs | Weakening and conditional effects; duration, removal and PvP consent/permission rules |

Do not implement these as eight copies of the same projectile. Define overlapping effects once in common systems. Blood is ritual/spellcraft; Vampirism is a distinct transformation specialty, even when they share themes. Dark schools are optional and should not force unwilling players into transformations, resource extraction or permanent character penalties.

Transport magic includes local travel tools and crafted teleport/portal infrastructure. Crafting magic includes transformations, inscriptions, alchemy and apparatus. Define costs and destination access centrally so a later spell does not accidentally trivialize all logistics or space progression.

## Agriculture and husbandry

Start from useful ordinary crops, food, livestock and soils. Branch into magical botany/attunement and technological irrigation, harvesting, processing and controlled environments. Let both approaches stand alone at entry and offer worthwhile combined greenhouse/workshop projects later.

Proposals may cover orchards, culinary specialties, animal products, breeding, magical herbs, climate cultivation, fiber/fuel crops and off-world farming. Explain planting conditions, growth limits, harvesting automation, outputs, regional role and interactions with both industry and magic. Prevent infinite growth/yield feedback loops. Avoid excessive breeding/entity counts as the optimal farming strategy.

The branch's first three slices (the Fall Harvest: tall corn, wild plants and sickles; the Kitchen Garden: trellis crops, vegetables, grains and the Cooking Pot; the Festival Crops: gourds, turnips and Turnip Lanterns, cranberry bogs and the chestnut tree; and pumpkin carving) and its planned crop roster and farm equipment are in [branches/AGRICULTURE.md](branches/AGRICULTURE.md).

The owner-endorsed [industrial agriculture plan](features/industrial-agriculture-plan.md) gives farming and forestry finished-product industries: textiles, coated fabrics, paper, packaging, panels, linoleum, rubber goods and later fuels/chemical materials. It records machine routes, sidegrade choices, bootstrap boundaries and a proposed delivery sequence; it is planning documentation rather than a claim of shipped content.

## Cozy surface biomes

Biomes should belong geographically: temperature, moisture, elevation, water, neighboring terrain, vegetation and animal ecology need a coherent explanation. Give players attractive building palettes, usable regional resources, quiet scenic places and ambient life. Example proposals: orchard valleys, misty woodland workshops, warm meadows or sheltered snowy groves.

Specify transition rules and whether resources are unique or obtainable elsewhere. Avoid forcing essential early items into an extremely rare distant biome. Review exploration distance, seed behavior and existing-chunk compatibility. Keep ambient sounds and effects adjustable and readable.

The biomes branch's roster (a remake of the Biomes O' Plenty catalog's biomes in nine batches, placed in Jugcraft regions) is in [branches/BIOMES.md](branches/BIOMES.md).

## Caves, dungeons and bosses

Underground danger can include hostile cave ecology, environmental hazards, structures, puzzles and dungeon inhabitants. Telegraph difficulty through biome/structure design and allow preparation or retreat. Define spawn limits, lighting behavior, structure frequency, mining/progression implications and existing-world generation.

Bosses can inhabit dungeons or be summoned with crafted/ritual requirements. Each needs encounter boundaries, readable attacks, recovery rules, loot eligibility, multiplayer scaling, reset/despawn rules, interruption behavior and anti-farming analysis. Summonable bosses must not become griefing tools in cozy settlements. Loot can support several specialties through alternative uses rather than forcing all players into repeated boss combat.

## Wildlife, hunting and companions

Add animals that deepen habitats, tracking, food, crafting or exploration. Include friendly and tamable creatures, cozy pets and useful husbandry animals as well as enemies. Define ecological role before adding another cosmetic spawn.

Pet proposals specify taming, ownership/transfer, name/state persistence, following, teleporting safely, resting/staying, combat participation, friendly fire and recovery. Decorative pets are valid through experience and companionship value; do not force them to become factory components. All creature features need pathfinding and population budgets.

## Space and magical realms

Space progression: a buildable launch system → one meaningful moon destination → planets and varied expeditions → broader galactic exploration over later milestones. Each destination needs reasons to visit, distinct terrain/ecology/hazards, resources with downstream uses, landing/return travel and persistent base behavior. Do not generate a large collection of empty worlds as the first implementation.

Rocketry covers parts, propulsion, fuel, navigation and expedition logistics. Habitats connect engineering, agriculture and optional magical protection. Magical exploration instead emphasizes attunement, portal construction, realm identity and spells. Cross-links should enrich both approaches while preserving each one's purpose. Explicitly decide when portals can reach off-world settlements, at what cost, and after which discovery.

Add one destination per reviewed increment. Dimension IDs and travel data must remain stable. No seasonal destination may disappear with players or bases inside it.

## Rare loot and special equipment

Offer visually distinctive technological tools, magical instruments, weapons, armor and curios with special abilities. Proposals include concept/visual evidence, source pools, rarity weights, eligible recipients, duplicate behavior, progression stage and upgrade/attunement paths.

Keep abilities readable and bounded. Define stacking, cooldowns, costs, death/trade behavior, PvP interactions and interactions with machines/spells. Rare does not justify unbounded power. Distribute desirable loot across farming, crafting, exploration, dungeons and bosses so combat is not the sole rewarding specialty.

## Seasonal content

Halloween is the first seasonal priority; Christmas-esque content follows in December. These are themes and sequencing goals, not promises that large features will ship by a date. Deliver small complete features built on permanent systems.

Halloween proposal seeds: autumn crops and foods, candlelit workshop decorations, spectral wildlife, a bounded haunted dungeon, curse research and a summonable harvest encounter. December seeds: snowy/cozy building sets, festive cooking, gift crafting, friendly winter creatures, workshop toy-making, ice rituals and a winter adventure.

The server operator explicitly configures event start/end times and timezone, with a manual override for testing and off-season worlds. Clients' clocks do not control rewards. Record timing and entitlement server-side. Ending an event changes availability/spawns/recipes safely; earned items, pets, structures and dimensions persist. Avoid mandatory progression depending on a short seasonal window; provide an off-season alternative or keep those rewards optional.

Every seasonal PR tests activation, deactivation, restart across the boundary, duplicate reward prevention and preservation of earned content.
