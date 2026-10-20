"""Acquisition adapters verified against native implementations, October 19 follow-up.

These add evidence/edges, never runtime rewards. A named implementation is
required; merely appearing in a model or catalog is not acquisition evidence.
"""
import re
import json


def extend(root, edge, evidence, items):
    base = 'src/main/java/io/github/jimbozoomer/jugcraft/'
    def source(path):
        path = base + path + '.java'
        assert (root / path).exists(), path
        return path
    def j(n):
        if isinstance(n, list): return [j(v) for v in n]
        return n if ':' in n else 'jugcraft:' + n
    def route(path, ins, outs, kind='native_interaction'):
        edge(source(path), kind, [j(v) for v in ins], [j(v) for v in outs])
    def found(path, names, note):
        path = source(path)
        for name in names:
            assert j(name) in items, name
            evidence[j(name)].append({'kind': 'native_acquisition', 'source': path, 'note': note})

    import agriculture as ag
    import feasts
    # PieFilling is the complete runtime list, including the later cakes/tarts.
    text = (root / source('agriculture/PieFilling')).read_text(encoding='utf-8')
    for name in re.findall(r'^\s*[A-Z_]+\("([^"]+)"', text, re.M):
        candidates = [name, ag.pie_name(name) if name in ag.PIES['fillings'] else name + '_pie']
        baked = next((n for n in candidates if j('raw_' + n) in items), None)
        if not baked: continue
        route('agriculture/HearthOvenBlockEntity', ['raw_' + baked, 'hearth_oven', '#minecraft:coals'], [baked])
        route('agriculture/HearthOvenBlockEntity', ['raw_' + baked, 'hearth_oven', '#minecraft:coals'],
              ['burnt_cake' if 'cake' in baked else 'burnt_pie'], 'overbaking')
        route('agriculture/PieBlock', [baked, '#jugcraft:knives'], [baked + '_slice'], 'slicing')
    for name, d in feasts.FEASTS.items():
        route('agriculture/FeastBlock', [name, 'minecraft:bowl'], [d['serving']], 'serving')
    route('agriculture/PlacedPieBlock', ['minecraft:pumpkin_pie', '#jugcraft:knives'], ['pumpkin_pie_slice'])
    for name in ag.KNITTING['garments']:
        route('agriculture/KnittingNeedlesItem', [ag.KNITTING['needles'], 'yarn'], [name], 'knitting')
    for name, d in ag.HEX['brews'].items():
        route('agriculture/BubblingCauldronBlock', ['bubbling_cauldron', 'minecraft:water_bucket',
              'minecraft:campfire', 'minecraft:glass_bottle', '#jugcraft:brew/' + d['brew'], '#jugcraft:hex/' + name], [d['item']], 'hex_brewing')
    route('agriculture/SpiceGrinding', ['mortar', 'dried_chili'], ['paprika'], 'grinding')
    route('agriculture/CinnamonLogBlock', ['cinnamon_log', '#minecraft:axes'], ['stripped_cinnamon_log'])
    route('chemistry/FoamSprayerItem', ['foam_sprayer', 'foam_canister'], ['construction_foam'])
    route('agriculture/OrganicCompostBlock', ['organic_compost'], ['rich_soil'], 'compost_maturation')
    route('agriculture/BlankStencilItem', ['blank_stencil'], ['pumpkin_stencil'], 'stencil_selection')
    route('agriculture/IronBoundCoffinBlock', ['iron_bound_coffin','key_blank'], ['skeleton_key'], 'lock_key_binding')
    route('agriculture/RoastTurkeyBlock', ['roast_turkey', '#jugcraft:knives'], ['turkey_slice'], 'serving')
    route('agriculture/PunchBowlBlock', ['witchs_brew_punch_bowl', 'minecraft:glass_bottle', 'minecraft:sweet_berries'], ['witchs_brew_punch'])
    route('agriculture/BarmbrackBlock', ['barmbrack'], ['barmbrack_ring'], 'hidden_prize')
    route('agriculture/MarshmallowStickItem', ['marshmallow_on_a_stick', 'minecraft:campfire'], ['toasted_marshmallow', 'burnt_marshmallow'])
    route('machine/MachineBlockEntity', ['fracking_rig', 'fluid:jugcraft:fracking_fluid'], ['fluid:jugcraft:flowback_water'], 'shale_fracking')
    found('nuclear/EquipmentBlockEntity', ['spent_fuel_assembly'], 'Recover depleted assemblies from a functioning fueled reactor; not fresh fuel.')
    found('agriculture/BatHouseBlockEntity', ['bat_guano'], 'Occupied bat houses accumulate collectable guano.')
    found('agriculture/TomatoVineBlock', ['rotten_tomato'], 'Tomato vine overripening.')
    found('agriculture/GiantPumpkinVineBlock', ['giant_pumpkin'], 'Grow and maintain a giant pumpkin vine.')
    found('agriculture/HarvestScaleBlockEntity', ['first_prize_ribbon', 'second_prize_ribbon', 'third_prize_ribbon'], 'Judged harvest entries at the harvest scale; score determines ribbon.')
    found('agriculture/JudgesTableBlockEntity', ['best_costume_ribbon'], 'Costume judging reward.')
    found('agriculture/WillOWisp', ['wisp_in_a_jar'], 'Catch a living wisp with the supported container.')
    found('agriculture/PumpkinBoat', ['pumpkin_barge', 'pumpkin_racer'], 'Carve a giant pumpkin into a boat; choose the boat variant through its native interaction.')
    found('styx/Styxhexenhammer', list(('amethyst_mourningbell','astral_verbena','eclipse_camellia','inkvein_helleborine','ravenquill_lupine','stolas_starflower','violet_lanternbloom','witchglass_orchid')), 'Styx greenhouse exchange supplies all eight flowers; conservatory plants can also be harvested.')
    found('menagerie/EcologyTree', ['ecology_' + c + '_maple_leaves' for c in ('green','orange','red','yellow')], 'Maple generation chooses one of these four leaf colors; use shears or Silk Touch.')
    found('menagerie/MenagerieAnimal', ['wildlife_duck_egg'], 'Adult ducks periodically lay eggs.')
    found('menagerie/MenagerieEcology', ['wildlife_' + n for n in ('bee','silverfish','endermite')], 'Capture the vanilla living critter with the wildlife capture net.')
    found('menagerie/MenagerieItems', ['wildlife_scorpion', 'grove_cindershell_bucket'] + ['grove_lizard_item_' + v for v in ('desert','desert_2','jungle','jungle_2','mushroom')], 'Capture a scorpion with the wildlife net, a lizard variant with the grove net, or a baby Cindershell with a lava bucket.')
    for species, home, block in [('drygmy','drygmy_stone','minecraft:mossy_cobblestone'),('wixie','wixie_cauldron','minecraft:cauldron'),('whirlisprig','whirlisprig_flower','#minecraft:flowers')]:
        route('nouveau/NouveauHelpers', ['nouveau_' + species + '_charm', block], ['nouveau_' + home], 'helper_home')
    route('nouveau/NouveauUtilities', ['nouveau_blank_parchment'], ['nouveau_scryer_scroll'], 'destination_binding')
    found('nouveau/NouveauFamiliars', ['nouveau_familiar_' + n for n in ('amethyst_golem','bookwyrm','drygmy','starbuncle','whirlisprig','wixie')], 'Familiar-binding ritual at the ritual brazier with an eligible nearby helper; ownership and source requirements remain enforced.')
    route('agriculture/HarvestEffigyBlockEntity', ['harvest_effigy', 'minecraft:flint_and_steel'], ['effigy_ashes'], 'effigy_burning')
    route('agriculture/EffigyAshesBlock', ['effigy_ashes'], ['hearth_ash'], 'ash_collection')
    found('agriculture/PumpkinCarvings', ['hand_carved_' + p for p in ('pumpkin','cinderella_pumpkin','jarrahdale_pumpkin','kabocha_pumpkin','red_kuri_pumpkin','white_pumpkin')], 'Use the carving knife/stencil system on the corresponding pumpkin; preserves authored carving data.')
    found('blueprint/SurveyStakeBlockEntity', ['blueprint'], 'Record a bounded building selection through survey stakes; blueprint carries the recorded plan.')
    found('concordance/Alchemy', ['draught','salve'], 'Brew an actual ingredient mixture in the crucible and choose its form; effect data and solvent are required.')
    found('concordance/MortarItem', ['reagent'], 'Grind an ingredient from the alchemy catalog using the mortar; the output preserves ingredient/preparation data.')
    found('concordance/smithy/Artificery', ['resonant_ring'], 'Artificer bench forging consumes the selected substrate and focus, and writes rolled artifice data.')
    found('concordance/garden/Garden', ['verdant_chaff'], 'Garden harvesting byproduct.')

    # The crop is reachable from its seed; its normal block loot, including
    # secondary stalks, is then an edge. Item-less crop blocks need this bridge.
    for name, d in ag.TALL_CROPS.items():
        route('agriculture/TallCropBlock', [d['seed']], [d['pick']['item']], 'crop_growth')
        if name in ag.STALKS['crops']:
            route('agriculture/TallCropBlock', [d['seed']], ['corn_stalks'], 'mature_crop_harvest')
    route('agriculture/JugcraftAgriculture', ['roasted_corn'], ['corncob'], 'food_remainder')
    found('menagerie/CavernColorLoot', ['cavern_sweetish_fish_' + c for c in ('blue','green','pink','yellow')], 'Existing gummy-color loot modifier preserves the killed fish or gummy bear color; furnace modifier follows afterward.')
    found('menagerie/MenagerieHabitats', ['wildlife_ant_hill'], 'Native Overworld habitat generation on soil; requires new terrain.')
    found('menagerie/MenagerieItems', ['wildlife_' + c + '_starfish' for c in ('blue','orange','purple','red')], 'Wildlife net captures the actual living starfish variant; releasing consumes the stored capture once.')
    found('menagerie/MenagerieAnimal', ['mount_firebird_feather'+n for n in ('','2','3','4')], 'Adult Firebirds shed their matching feathers on the existing bounded production timer.')
    found('menagerie/MenagerieAnimal', [n+'_egg' for n in ('beast_caiman','beast_platypus','beast_terrapin','cavern_atlatitan','cavern_grottoceratops','cavern_relicheirus','cavern_subterranodon','cavern_tremorsaurus','cavern_vallumraptor')]+['beast_triops_eggs'], 'Breed the living species using its accepted food; one egg per breeding, normal parent cooldown and ownership checks.')

    # Read only palettes actually used by templates referenced by worldgen.
    # Unused templates are not evidence; entity lists are ignored just as the
    # native template placer ignores entities. No supplied programs are run.
    from nouveau_nbt import read as read_nbt
    data = root / 'src/main/resources/data/jugcraft'
    referenced = set()
    def strings(v):
        if isinstance(v,str): yield v
        elif isinstance(v,list):
            for c in v: yield from strings(c)
        elif isinstance(v,dict):
            for c in v.values(): yield from strings(c)
    for p in (data/'worldgen').rglob('*.json'):
        referenced.update(strings(json.loads(p.read_text(encoding='utf-8'))))
    for p in (data/'structure').rglob('*.nbt'):
        id = 'jugcraft:' + p.relative_to(data/'structure').as_posix()[:-4]
        if id not in referenced: continue
        nbt = read_nbt(p.read_bytes())[2]
        palettes = [nbt['palette'][1][1]] if 'palette' in nbt else [v[1] for v in nbt.get('palettes',(9,(9,[])))[1][1]]
        indices = {b['state'][1] for b in nbt.get('blocks',(9,(10,[])))[1][1]}
        for palette in palettes:
            for i in indices:
                d = palette[i]; name = d.get('id',d.get('Name',(8,'')))[1]
                if name.startswith('jugcraft:'):
                    evidence[name].append({'kind':'structure_block','source':p.relative_to(root).as_posix(), 'note':'Used block in a referenced worldgen template; harvest/tool requirements still apply.'})
    # Block and fluid registries use the same ID for these liquid sources.
    for group in ('cavern','landscape'):
        catalog = json.loads((root/('src/main/resources/jugcraft/menagerie/'+group+'.json')).read_text(encoding='utf-8'))
        for name in catalog['fluids']:
            key=j(group+'_'+name)
            if evidence[key]: evidence['fluid:'+key].extend(evidence[key])
    # A blockitem for the crop itself is a placement/debug convenience; seeds
    # are the survival inventory item. Mage blocks are temporary spell output.
    exclusions={j('herbal_pumpkling_crop'): 'Internal crop block item; plant herbal_pumpkling_seeds in survival.',
                j('nouveau_mage_block'): 'Temporary phantom-block spell output; not a permanent survival building material.'}
    for name in ('frost_knight_greatsword','hades_scythe','reaper_scythe','sentinel_longsword','sentinel_shield','wight_king_zweihander'):
        exclusions[j(name)]='Deferred acquisition design: matching owner-preview armor set; tools/arms_variants.py SETS and ArmsVIIGameTests intentionally specify no survival recipe or trophy table yet.'
    for size in ('medium','large'):
        exclusions[j('beast_'+size+'_catfish_bucket')]='Compatibility variant: current native catfish has one small profile; actual captures use beast_small_catfish_bucket. Legacy containers remain releasable.'
    return exclusions
