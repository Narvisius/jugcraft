"""Explicit adapters for acquisition that is implemented outside vanilla recipes.

These describe existing systems, not new crafting shortcuts. Keep evidence tied
to the implementation and do not treat every item mentioned by Java as a source.
"""
import json


def extend(root, edge, evidence, items):
    res = root / 'src/main/resources'
    data = res / 'data/jugcraft'
    java = 'src/main/java/io/github/jimbozoomer/jugcraft/'
    def read(p): return json.loads(p.read_text(encoding='utf-8'))
    def j(n):
        if isinstance(n, list): return [j(v) for v in n]
        return n if ':' in n else 'jugcraft:' + n
    def route(source, ins, out, kind='native_interaction'):
        edge(source, kind, [j(i) for i in ins], [j(o) for o in out])
    def found(names, source, note):
        for n in names:
            if j(n) in items:
                evidence[j(n)].append({'kind': 'native_acquisition', 'source': source, 'note': note})

    # Only wood families actually registered as strippable logs; not arbitrary
    # item names containing "stripped" (candy cane geometry is different).
    import agriculture as ag
    for wood in ag.WOOD_SETS:
        for suffix in ('log',):
            out = 'stripped_' + wood + '_' + suffix
            if j(out) in items:
                route(java + 'agriculture/StrippableLogBlock.java', [wood + '_' + suffix, '#minecraft:axes'], [out])
    for group in ('landscape', 'cavern'):
        catalog = read(res / ('jugcraft/menagerie/' + group + '.json'))
        for name, definition in catalog['blocks'].items():
            if definition['kind'] == 'log' and definition['source'].startswith('stripped_'):
                original = group + '_' + definition['source'].removeprefix('stripped_')
                route(java + 'menagerie/NaturalCollections.java', [original, '#minecraft:axes'], [name])
        # These are placed world states, made by using a plant on a flower pot.
        for name, definition in catalog['blocks'].items():
            if definition['kind'] == 'potted':
                route(java + 'menagerie/NaturalCollections.java',
                      [group + '_' + definition['source'].removeprefix('potted_'), 'minecraft:flower_pot'], [name])

    found(['glowcap_block', 'blight_wart_block', 'bramble_bush_leaves'],
          java + 'agriculture/FungalGrowthFeature.java',
          'Generated huge fungi and bramble bushes in Glowcap Grotto/Fungal Thicket, Blighted Sands and Netherbrush; harvest caps/leaves with the tools required by their block loot (including Silk Touch).')
    route(java + 'agriculture/HollyLeavesBlock.java',
          ['holly_sapling', 'minecraft:shears'], ['holly_sprig'], 'grow_holly_and_clip_supported_regrowing_leaves')
    route(java + 'agriculture/TreeTapBlock.java',
          ['rubber_tree_sapling', 'tree_tap', 'minecraft:glass_bottle'], ['latex_bottle'],
          'grow_rooted_rubber_tree_with_live_canopy_and_wait_four_tap_fill_steps')
    found(['walnut_log', 'walnut_leaves'], java + 'agriculture/WalnutTree.java',
          'Walnut and large walnut trees generate in Woodland and Dense Forest; harvest logs and supported leaves with the tools required by their block loot.')
    route(java + 'agriculture/WalnutLeavesBlock.java',
          ['walnut_leaves'], ['ecology_walnut', 'walnut_husk'],
          'harvest_ripe_supported_walnut_leaves_in_season')

    for name, tree in ag.TREES.items():
        if tree.get('fruit'):
            route('tools/agriculture.py:TREES/' + name, [tree['leaves']], [tree['fruit']['item']], 'fruit_harvest')
    for name, d in ag.CROPS.items():
        route('tools/agriculture.py:CROPS/' + name, [d['seed']], [d['produce']], 'crop_growth')
    for name, d in ag.PIES['fillings'].items():
        pie = ag.pie_name(name)
        if j('raw_' + pie) in items:
            route('tools/agriculture.py:PIES/' + name, ['raw_' + pie, ag.PIES['oven'], '#minecraft:coals'], [pie], 'hearth_baking')
        if j(pie + '_slice') in items:
            route('tools/agriculture.py:PIES/' + name, [pie, '#jugcraft:knives'], [pie + '_slice'], 'slicing')
    route('tools/agriculture.py:CIDER', ['cider_press', 'minecraft:apple', 'minecraft:glass_bottle'], ['sweet_cider', 'apple_pomace'], 'cider_press')
    route('tools/agriculture.py:CIDER', ['cider_barrel', 'sweet_cider'], ['sparkling_cider', 'aged_cider'], 'fermentation')
    for base, products in ag.CANDY['makes'].items():
        for out in set(products) - {None}:
            route('tools/agriculture.py:CANDY/' + base,
                  ['candy_kettle', 'candy_tray', 'minecraft:sugar', 'minecraft:water_bucket', 'minecraft:campfire'] +
                  (['minecraft:milk_bucket'] if base == 'cream' else []), [out], 'candy_temperature')
    route('tools/agriculture.py:CANDY/lollipop', ['hard_candy', 'minecraft:stick', 'candy_tray'], ['lollipop'])
    route('tools/agriculture.py:CHANDLERY', ['wax_melting_pot', 'minecraft:campfire', 'minecraft:honeycomb', 'minecraft:string'], ['aura_candle'], 'candle_dipping')

    # Native workshop conversions remain workshop recipes, including their fuel.
    for row in read(res / 'jugcraft/menagerie/beast.json')['capsid']:
        route(java + 'menagerie/BeastCapsid.java', row['ingredients'] + ['beast_capsid', 'minecraft:amethyst_shard'], [row['result']['id']], 'capsid')
    for p in (data / 'concordance/working').glob('*.json'):
        d = read(p)
        if d.get('type') == 'craft':
            route(p.relative_to(root).as_posix(), [d['station'], d['work'], d['specimen']], [d['result']], 'bench_working')
    for p in (data / 'concordance/ritual').glob('*.json'):
        d = read(p); transform = d.get('result', {}).get('transform')
        if transform:
            structure = read(data / ('concordance/structure/' + d['structure'].split(':')[1] + '.json'))
            ins = [v['item'] for v in d['offerings']] + [structure['anchor']]
            for v in structure['parts']:
                if not v.get('block'): continue
                ref = v['block']
                if ref.startswith('#jugcraft:'):
                    tag = read(data / ('tags/block/' + ref.split(':')[1] + '.json'))
                    ins.append(tag['values'])
                else: ins.append(ref)
            route(p.relative_to(root).as_posix(), ins, [transform['into']], 'ritual')

    # Actual native animal/plant interactions. Encounter and timing conditions
    # remain in the notes; a static graph does not establish their difficulty.
    found(['beast_moose_antler', 'beast_shed_snake_skin', 'beast_banana_slug_slime', 'beast_emu_egg',
           'beast_gongylidia', 'beast_ambergris'], java + 'menagerie/Beasts.java',
          'Owned moose, anaconda, banana slug, emu, platypus and cachalot periodically produce their respective resources.')
    found(['beast_stink_bottle', 'beast_komodo_spit_bottle'], java + 'menagerie/Beasts.java',
          'Use a glass bottle on an owned, harvest-ready skunk or komodo dragon.')
    found(['companion_pearl', 'companion_snail_slime_bottle', 'companion_sea_bunny_slime_bottle'],
          java + 'menagerie/CompanionCreatures.java', 'Species-specific pearl/slime harvesting with regrowth cooldown.')
    found(['ecology_coconut', 'ecology_prickly_pear'], java + 'menagerie/Ecology.java', 'Harvest ripe supported fruit plants.')
    found(['ecology_coconut_leaves', 'ecology_coconut_log', 'ecology_coconut_seedling',
           'ecology_maple_log', 'ecology_sappy_maple_log', 'ecology_maple_sapling',
           'ecology_azalea_log', 'ecology_azalea_flower'], java + 'menagerie/EcologyTree.java',
          'Native generated/grown trees and their harvests; verify biome availability in a survival world.')
    route(java + 'menagerie/HouseholdBlock.java', ['ecology_spile', 'ecology_sappy_maple_log', 'minecraft:bucket'], ['ecology_maple_sap_bucket'], 'sap_collection')
    route(java + 'menagerie/HouseholdBlock.java', ['ecology_maple_sap_bucket', 'minecraft:cauldron', 'minecraft:campfire', 'minecraft:glass_bottle'], ['ecology_maple_syrup_bottle'], 'sap_boiling')
    route(java + 'menagerie/CaveWorkshop.java', ['cavern_spelunkery_table', 'cavern_cave_tablet', 'minecraft:paper'], ['cavern_cave_map'], 'cave_survey')
    for color in ('red', 'blue', 'green', 'purple'):
        found(['nouveau_' + color + '_archwood_log', 'nouveau_' + color + '_archwood_leaves'],
              java + 'nouveau/NouveauArchwoodFeature.java', 'Archwood generation in temperate Overworld biomes; saplings also grow the trees.')
    found(['nouveau_bombegranate_pod', 'nouveau_frostaya_pod', 'nouveau_mendosteen_pod', 'nouveau_bastion_pod'],
          java + 'nouveau/NouveauArchwoodFeature.java', 'Pods generated on archwood trunks; harvest ripe fruit.')
    route(java + 'agriculture/SpinningWheelBlockEntity.java', ['spinning_wheel', 'minecraft:white_wool'], ['yarn'], 'spinning')
    route('tools/petro.py:air_separation_unit', ['air_separation_unit'],
          ['fluid:jugcraft:nitrogen', 'fluid:jugcraft:oxygen', 'fluid:jugcraft:argon'], 'powered_air_separation')
    route(java + 'machine/form/CoastalSeawaterIntake.java', ['coastal_seawater_intake'],
          ['fluid:jugcraft:seawater'], 'powered_ocean_intake')
    found(['nouveau_starbuncle_shards'], java + 'nouveau/Starbuncle.java',
          'Offer a gold nugget to a wild Starbuncle; it leaves a shard.')
    found(['nouveau_drygmy_shard', 'nouveau_alakarkinos_token', 'nouveau_whirlisprig_shards'],
          java + 'nouveau/NouveauHelper.java', 'Drygmy accepts a wilden horn; Alakarkinos accepts a pottery sherd; Whirlisprig watches a sapling grow.')
    found(['nouveau_experience_gem', 'nouveau_greater_experience_gem'], java + 'nouveau/NouveauResources.java',
          'Native loot injections; ordinary gems also come from Drygmy work and the experience ritual.')
    found(['nouveau_amethyst_golem_charm'], java + 'nouveau/NouveauEncounters.java',
          'Awakening at budding amethyst produces a charm; the budding block becomes ordinary amethyst.')
    found(['grove_pink_minipad_flower', 'grove_light_pink_minipad_flower', 'grove_yellow_minipad_flower'],
          java + 'menagerie/GroveCreatures.java', 'Shear the matching Lilytad variant; flowers regrow.')
    found(['ectoplasm'], java + 'agriculture/RestlessSpirit.java', 'Bottle a restless spirit through its native interaction.')
    found(['dreamglass'], java + 'concordance/dreaming/Dreaming.java', 'Catch dream wisps and return from a dream.')
    found(['acorn'], java + 'agriculture/JugcraftAgriculture.java', 'Oak-leaf harvesting and squirrel gathering.')
    route(java + 'agriculture/CinnamonLogBlock.java', ['cinnamon_log', '#minecraft:axes'], ['cinnamon'], 'bark_stripping')
    # Capture is a live-animal interaction, not a recipe for an already-filled
    # bucket. Only species/variant combinations accepted by MenagerieItems count.
    captured = {'ant', 'butterfly', 'caterpillar', 'dragonfly', 'firefly', 'hedgehog', 'rat', 'scorpion', 'snail', 'crab'}
    for catalog_name in ('catalog', 'grove', 'companion', 'beast', 'cavern', 'ecology'):
        for s in read(res / ('jugcraft/menagerie/' + catalog_name + '.json'))['species']:
            group = s['id'].split('_')[0]; source = s['source']
            if group in ('wildlife', 'beast', 'cavern', 'companion'):
                names = [source]
                if group == 'beast' and source == 'catfish': names = ['small_catfish']
                if group == 'cavern' and source == 'sweetish_fish': names = [v['name'] for v in s['variants']]
                found([group + '_' + n + '_bucket' for n in names], java + 'menagerie/MenagerieItems.java',
                      'Capture the living species in its supported water/lava bucket; ducks must be babies.')
            if group == 'wildlife' and source in captured:
                found(['wildlife_' + source], java + 'menagerie/MenagerieItems.java', 'Use the capture net on a small living animal.')
            if group == 'fauna' and source in {'butterfly', 'dragonfly', 'pixie'}:
                found(['fauna_bug_habitat_' + v['name'] + '_' + source for v in s['variants']],
                      java + 'menagerie/MenagerieItems.java', 'Bug net plus an empty habitat; preserves the captured animal and variant.')
    found(['wildlife_alligator_egg', 'wildlife_ostrich_egg', 'wildlife_tortoise_egg', 'wildlife_snail_eggs', 'grove_lizard_egg_block'],
          java + 'menagerie/MenagerieAnimal.java', 'Breed the corresponding adults; eggs are laid on suitable ground.')

    # Every liquid has a registered bucket, obtainable by filling it. Gases have
    # no bucket item; never infer a container route from a gas's display name.
    import petro
    for fluid in petro.FLUIDS:
        if j(fluid + '_bucket') in items:
            route(java + 'chemistry/PetroFluids.java', ['fluid:' + j(fluid), 'minecraft:bucket'], [fluid + '_bucket'], 'fluid_filling')
    for group in ('landscape', 'cavern'):
        for name in read(res / ('jugcraft/menagerie/' + group + '.json'))['fluids']:
            fluid = group + '_' + name
            route(java + 'chemistry/PetroFluids.java', ['fluid:' + j(fluid), 'minecraft:bucket'], [fluid + '_bucket'], 'fluid_filling')

    import gear
    exclusions = {j(tier + '_' + part): 'Deferred acquisition design: owner requested preview armor only on October 6; tools/gear.py ARMOR_TIERS records the still-unassigned recipe/drop choice.'
                  for tier in gear.ARMOR_TIERS for part in ('helmet', 'chestplate', 'leggings', 'boots')}
    # Only the retired basic forms are compatibility-only. Recovered furniture,
    # boats and the edible walnut remain active survival items.
    from walnut_data import ALIASES
    for old, canonical in (ALIASES | {
            j('ecology_walnut_leaves'): j('walnut_leaves'),
            j('ecology_walnut_sapling'): j('walnut_sapling')}).items():
        exclusions[old] = ('Saved-world compatibility ID; tools/walnut_data.py directs new recipes and drops to '
                           + canonical + '. No fresh survival source for the retired form.')
    for name in ('creative_drone', 'creative_energy_cell', 'creative_supply_crate', 'creative_tower_guide',
                 'nouveau_creative_source_jar', 'nouveau_creative_spell_book', 'nouveau_debug', 'fauna_ff_icon',
                 'landscape_bop_icon', 'check_source'):
        exclusions[j(name)] = 'Creative, debug or presentation-only entry; no survival recipe.'
    from survival_native_followup import extend as followup
    exclusions.update(followup(root, edge, evidence, items))
    import deposits
    for name in deposits.DEPOSITS:
        for variant in (name, "deepslate_" + name):
            exclusions[j(variant)] = 'Natural finite reserve, not a survival block item: picks drop nothing; the deposit drill extracts coal or raw metal.'
    found(['rotten_spear'], java + 'menagerie/RottenCreatures.java',
          'Glacial Hunter equips this spear with setItemSlotAndDropWhenKilled; defeat the encounter for the equipment drop.')
    exclusions['jugcraft:rotten_treasure_chest'] = 'Preview encounter storage; Deadbeard treasure placement is still on the creature-recovery TODO. No survival acquisition is implemented yet.'
    # Full-yield extrusion no longer invents recyclable waste. Preserve saved
    # HDPE/rubber offcuts and their existing reclaim recipes as compatibility items.
    for name in ('hdpe_offcut', 'rubber_offcut'):
        exclusions[j(name)] = 'Saved-world compatibility offcut; full-yield extrusion no longer creates scrap. Existing stock still reclaims through the Polymer Reclaimer.'
    found(['market_chest'], java + 'finance/ExchangeBlocks.java',
          'A company controller purchases a Market Chest from the Exchange Equities tab for the configured Jugs price; buyChest checks ownership and transfers the fee to the reserve.')
    found(['exchange_guide'], java + 'finance/ExchangeGuide.java',
          'Granted once on joining; additional guides from /jugcraft exchange guide.')
    import exchange_casino
    found(list(exchange_casino.SCRATCH_CARDS),
          java + 'finance/CasinoBlockEntity.java', 'Purchased as server-issued, single-use paid tickets at a lottery kiosk; alchemy blanks have no prize entitlement.')
    if 'jugcraft:artisan_furniture' in items:
        route(java + 'furniture/ArtisanFurniture.java', ['artisans_workstation', 'furniture_material'],
              ['artisan_furniture'], 'design_at_workstation_and_pay_server_computed_material_cost')
    from survival_jimtafacts_routes import extend as collection_routes
    exclusions.update(collection_routes(root, edge, evidence, items))
    return exclusions
