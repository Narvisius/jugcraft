from jimtafacts_crafting import distinct
"""Native spellbook content; supplied art is copied, never redrawn or executed.

Only entries with an implemented player outcome belong in this runtime catalog.
The inventory includes unported source entries explicitly, so a new registry entry
cannot accidentally masquerade as a completed source-system port.
"""
from pathlib import Path
import hashlib
import json
import re
import shutil
import jimtafacts_spellbook_combat as combat
import jimtafacts_spellbook_fields as fields
import jimtafacts_spellbook_channels as channels
import jimtafacts_spellbook_strikes as strikes
import jimtafacts_spellbook_attacks as attacks
import jimtafacts_spellbook_personal as personal
import jimtafacts_spellbook_wards as wards
import jimtafacts_spellbook_areas as areas
import jimtafacts_spellbook_mages as mages
import jimtafacts_spellbook_gear as gear
import jimtafacts_spellbook_encounters as encounters
import jimtafacts_spellbook_workshops as workshops
import jimtafacts_spellbook_lore as lore
import jimtafacts_spellbook_exploration as exploration
import jimtafacts_spellbook_loot as source_loot
import jimtafacts_spellbook_structures as structures
import jimtafacts_spellbook_curios as curios
import jimtafacts_spellbook_upgrades as upgrades
import jimtafacts_spellbook_recipes as source_recipes

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'art/owner-library/originals/Oct23/Jimtafacts'
ART = SOURCE / 'assets/irons_spellbooks'
RES = ROOT / 'src/main/resources'
PREFIX = 'jimta_spellbooks_'
STATIONS = ('inscription_table','scroll_forge','arcane_anvil')

# Level-one native workings. Focus is the existing Concordance resource; no mana clone.
# source name: school, Focus, cooldown seconds, cast seconds, description
SPELLS = {
    'spectral_hammer': ('evocation',3,2,0,'Swing a spectral hammer through an eighteen-block section of natural terrain. Original hammer animation; normal drops and claims respected.'),
    'ice_block': ('ice',8,15,1.25,'Form a tracking ice block overhead, then drop it for seven contact damage and up to fourteen impact damage with freezing.'),
    'sculk_tentacles': ('eldritch',30,30,1,'Raise six animated sculk tentacles for fifteen seconds. Their eight-damage strikes blind nearby enemies.'),
    'heal': ('holy', 6, 30, 0, 'Restore five health to yourself.'),
    'greater_heal': ('holy', 30, 120, 3, 'Restore all of your missing health.'),
    'cleanse': ('holy', 10, 30, 1, 'Remove harmful effects from yourself, party members and your pets within three blocks.'),
    'fortify': ('holy', 12, 60, 1, 'Give yourself and nearby allies four temporary absorption hearts for two minutes.'),
    'haste': ('holy', 8, 30, .5, 'Increase spell casting speed by ten percent for thirty seconds. Aim at an ally to help them.'),
    'slow': ('evocation', 8, 20, .5, 'Reduce the target’s spell casting speed by ten percent for twenty seconds.'),
    'teleport': ('ender', 6, 10, .5, 'Blink up to sixteen blocks along your view to a clear, permitted position.'),
    'summon_ender_chest': ('ender', 5, 5, .5, 'Open your own ender chest wherever you are.'),
    'touch_dig': ('nature', 3, 1, 0, 'Break one mineable block within eight blocks, with normal drops and claim protection.'),
    'wololo': ('evocation', 2, 3, .5, 'Change a permitted sheep to a different wool colour.'),
    'angel_wings': ('holy', 20, 120, 1, 'Fly for thirty seconds using Jugcraft’s shared temporary flight system.'),
    'charge': ('lightning', 10, 45, 1, 'Gain five percent movement, melee and lightning power for thirty seconds.'),
    'oakskin': ('nature', 8, 45, 1, 'Reduce incoming damage by twenty percent for thirty seconds; move twenty-five percent slower.'),
    'blight': ('nature', 8, 20, .5, 'Reduce the target’s damage by ten percent and healing by twenty percent for twenty seconds.'),
    'gluttony': ('nature', 8, 60, .5, 'Eating restores shared Focus for one minute. Reusable food is excluded.'),
    'root': ('nature', 9, 35, 2.5, 'Trap a permitted target in breakable forty-health roots for five seconds. Destroying the roots releases the target.'),
    'wisp': ('holy', 3, 3, 1, 'Send an animated pursuing wisp toward a permitted target for five damage. Blocks and attacks can stop it.'),
    'blessing_of_life': ('holy', 2, 15, 1.5, 'Restore six health to the ally or owned pet you are aiming at, up to sixty-four blocks away.'),
    'counterspell': ('ender', 10, 10, 0, 'Interrupt a permitted target’s active spell, dispel spellbook statuses, or destroy a Wisp or Root.'),
    'spider_aspect': ('nature', 7, 90, 0, 'Deal five percent additional damage to poisoned, withered or blighted targets for twenty seconds.'),
    'summon_vex': ('evocation', 12, 150, 1.5, 'Summon three protective vexes for ten minutes. Shared summon limit: eight. Sneak-use the book to dismiss.'),
    'summon_polar_bear': ('ice', 10, 150, 1.5, 'Summon a twenty-four-health polar bear defender for ten minutes. Sneak-use the book to dismiss.'),
    'summon_horse': ('evocation', 8, 60, 1.5, 'Summon a saddled spectral steed for ten minutes. Only its summoner can ride it; sneak-use the book to dismiss.'),
    'summon_swords': ('ender', 16, 150, 1, 'Summon a sword, a quick rapier and a durable claymore for ten minutes. Each uses its supplied animated model and distinct combat stats. Sneak-use the book to dismiss.'),
    'raise_dead': ('blood', 10, 150, 1.5, 'Raise three protective zombies or skeletons for ten minutes. They drop no equipment. Sneak-use the book to dismiss.'),
}
SPELLS.update(combat.definitions())
SPELLS.update(fields.definitions())
SPELLS.update(channels.definitions())
SPELLS.update(strikes.definitions())
SPELLS.update(attacks.definitions())
SPELLS.update(personal.definitions())
SPELLS.update(wards.definitions())
SPELLS.update(areas.definitions())
BOOKS = {'copper_spell_book': 5, 'iron_spell_book': 6, 'gold_spell_book': 8,
         'diamond_spell_book': 10, 'netherite_spell_book': 12, 'rotten_spell_book': 8,
         'blaze_spell_book': 10, 'dragonskin_spell_book': 12, 'druidic_spell_book': 10,
         'villager_spell_book': 10, 'ice_spell_book': 12, 'cursed_doll_spell_book':10,
         'evoker_spell_book':10, 'legendary_spell_book':12, 'necronomicon_spell_book':10,
         'wimpy_spell_book':1}
BUILTIN_SPELLS={'evoker_spell_book':['fang_strike','fang_ward','summon_vex'],
                'necronomicon_spell_book':['blood_slash','blood_step','ray_of_siphoning','blaze_storm']}
ARMOR = {
    'wizard': ('wizard_armor', 'wizard', 'arcane'),
    'wandering_magician': ('wandering_magician_armor', 'wandering_magician', 'arcane'),
    'pyromancer': ('pyromancer_armor', 'pyromancer', 'fire'),
    'electromancer': ('electromancer_armor', 'electromancer', 'lightning'),
    'cultist': ('cultist_armor', 'cultist', 'blood'),
    'archevoker': ('archevoker_armor', 'archevoker', 'evocation'),
    'cryomancer': ('cryomancer_armor', 'cryomancer', 'ice'),
    'shadowwalker': ('shadowwalker_armor', 'shadowwalker', 'ender'),
    'priest': ('priest_armor', 'priest', 'holy'),
    'plagued': ('plagued_armor', 'plagued', 'nature'),
    'pumpkin': ('pumpkin_armor', 'pumpkin', 'evocation'),
    'netherite_mage': ('netherite_armor', 'netherite', 'arcane'),
}
MATERIALS = ['arcane_essence', 'magic_cloth', 'arcane_ingot', 'common_ink', 'uncommon_ink',
             'rare_ink', 'epic_ink', 'legendary_ink', 'blank_rune', 'holy_rune', 'nature_rune',
             'ender_rune', 'evocation_rune', 'lightning_rune', 'fire_rune', 'ice_rune', 'blood_rune', 'lesser_spell_slot_upgrade', 'shriving_stone']
SCHOOL = {'holy': 'healing', 'nature': 'arcane', 'ender': 'arcane', 'evocation': 'arcane',
          'lightning': 'lightning', 'fire': 'fire', 'ice': 'frost', 'blood': 'soul', 'arcane': 'arcane', 'eldritch': 'arcane'}

def dump(path, obj):
    if 'recipe' in path.parts and isinstance(obj,dict) and 'type' in obj:
        obj.setdefault('fabric:load_conditions',[{'condition':'jugcraft:feature_enabled','feature':'concordance'}])
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')

def items():
    return [PREFIX + x for x in list(BOOKS) + MATERIALS + list(STATIONS)
            + [f'{family}_{part}' for family in ARMOR for part in ('helmet', 'chestplate', 'leggings', 'boots')]
            + ['scroll_' + x for x in SPELLS] + ['portal_frame']] + mages.items() + gear.items() + encounters.items() + workshops.items() + lore.items() + exploration.items() + curios.items() + upgrades.items()

def entities():
    return ['jimtafacts_frozen_humanoid','jimtafacts_catacombs_zombie','jimtafacts_magehunter_vindicator','jimtafacts_root', 'jimtafacts_wisp', 'jimtafacts_spectral_steed', 'jimtafacts_shot','jimtafacts_blood_projectile','jimtafacts_spectral_hammer','jimtafacts_ice_block','jimtafacts_void_tentacle'] + ['jimtafacts_'+kind for kind in ('black_hole','gravity_fissure','firefly_swarm','fang_swirl','sunbeam','wall_of_fire','ice_tomb','ice_spike','devour_jaw','frozen_copy','echoing_sword','scapegoat','shield','portal','pocket_portal','arcane_shackle')] + ['jimtafacts_summoned_'+kind for kind in ('zombie','skeleton','sword','rapier','claymore')] + mages.entities() + encounters.entities()

def effects():
    return ['jimtafacts_' + name for name in ('immolate','instant_mana','hastened','slowed','charge','oakskin','blight','gluttony','rend','guided','chilled','spider_aspect','vigor','echoing_strikes','frostbitten_strikes','abyssal_shroud','heartstop','planar_sight','evasion','true_invisibility')]

def blocks():
    return [PREFIX+name for name in (*STATIONS,'voidstone','pocket_dimension_portal_frame','portal_frame')] + encounters.blocks() + workshops.blocks()

def generate():
    copies, models = [], set()
    source_info=json.loads((ROOT/'tools/jimtafacts_spellbook_sources.json').read_text(encoding='utf-8'))
    lang_source = json.loads((ART / 'lang/en_us.json').read_text(encoding='utf-8'))
    lang, records = {}, {}

    def copy(src, dst):
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(src, dst)
        copies.append({'source': str(src.relative_to(ROOT)).replace('\\', '/'),
                       'runtime': str(dst.relative_to(ROOT)).replace('\\', '/'),
                       'sha256': hashlib.sha256(src.read_bytes()).hexdigest(), 'changes': 'None; byte-identical.'})

    def texture(ref):
        if not ref.startswith('irons_spellbooks:'):
            return ref if ':' in ref or ref.startswith('#') else 'minecraft:' + ref
        path = ref.split(':')[1]
        src = ART / ('textures/' + path + '.png')
        copy(src, RES / ('assets/jugcraft/textures/jimtafacts/spellbooks/' + path + '.png'))
        if src.with_suffix('.png.mcmeta').exists():
            copy(src.with_suffix('.png.mcmeta'), RES / ('assets/jugcraft/textures/jimtafacts/spellbooks/' + path + '.png.mcmeta'))
        return 'jugcraft:jimtafacts/spellbooks/' + path

    def model(ref):
        if ':' not in ref:
            return 'minecraft:' + ref
        if not ref.startswith('irons_spellbooks:'):
            return ref
        path = ref.split(':')[1]
        target = 'jugcraft:jimtafacts/spellbooks/' + path
        if path in models:
            return target
        models.add(path)
        data = json.loads((ART / ('models/' + path + '.json')).read_text(encoding='utf-8'))
        if data.get('loader') == 'neoforge:separate_transforms':
            data = data['base']
        if 'parent' in data:
            data['parent'] = model(data['parent'])
        if 'textures' in data:
            data['textures'] = {k: texture(v) for k, v in data['textures'].items()}
        # Predicate overrides are the obsolete 1.21 schema. Books here are fixed
        # closed inventory/held models; authored geometry/UV/display is preserved.
        data.pop('overrides', None)
        data.pop('loader', None)
        from jimtafacts_model_repairs import repair
        dump(RES / ('assets/jugcraft/models/jimtafacts/spellbooks/' + path + '.json'), repair('jimtafacts/spellbooks/'+path, data))
        return target

    def definition(source_model, ident):
        base = {'type': 'minecraft:model', 'model': model(source_model)}
        data = json.loads((ART / ('models/' + source_model.split(':')[1] + '.json')).read_text(encoding='utf-8'))
        if data.get('loader') == 'neoforge:separate_transforms':
            cases = []
            for perspective, value in data.get('perspectives', {}).items():
                value = dict(value)
                if 'parent' in value:
                    value['parent'] = model(value['parent'])
                if 'textures' in value:
                    value['textures'] = {k:texture(v) for k,v in value['textures'].items()}
                ref = f'jimtafacts/spellbooks/perspective/{ident}_{perspective}'
                dump(RES / f'assets/jugcraft/models/{ref}.json', value)
                cases.append({'when': perspective, 'model': {'type':'minecraft:model','model':'jugcraft:' + ref}})
            base = {'type':'minecraft:select', 'property':'minecraft:display_context', 'cases':cases, 'fallback':base}
        return {'model':base}

    for name in list(BOOKS) + MATERIALS + [f'{family}_{part}' for family in ARMOR for part in ('helmet', 'chestplate', 'leggings', 'boots')]:
        ident = PREFIX + name
        source_model = 'irons_spellbooks:item/' + name
        if not (ART / ('models/item/' + name + '.json')).exists():
            raise ValueError('No supplied item model for ' + name)
        dump(RES / f'assets/jugcraft/items/{ident}.json', definition(source_model, ident))
        dump(RES / f'data/jugcraft/spell_assignments/{ident}.json', {'access':'NONE', 'access_param':''})
        lang['item.jugcraft.' + ident] = lang_source.get('item.irons_spellbooks.' + name,name.replace('_',' ').title())
        kind = 'book' if name in BOOKS else 'material' if name in MATERIALS else 'armor'
        records[name] = {'kind': kind, 'capacity': BOOKS.get(name, 0)}
        if name in BUILTIN_SPELLS: records[name]['fixed_spells']=BUILTIN_SPELLS[name]
        if kind == 'armor':
            family = next(x for x in ARMOR if name.startswith(x + '_'))
            records[name].update(family=family, model=ARMOR[family][0], texture=ARMOR[family][1], school=ARMOR[family][2])
    for family, (geometry, tex, school) in ARMOR.items():
        copy(ART / f'geo/{geometry}.geo.json', RES / f'assets/jugcraft/geckolib/models/armor/jimtafacts/{geometry}.geo.json')
        copy(ART / f'textures/models/armor/{tex}.png', RES / f'assets/jugcraft/textures/jimtafacts/spellbooks/models/armor/{tex}.png')
    copy(ART / 'animations/wizard_armor_animation.json', RES / 'assets/jugcraft/geckolib/animations/armor/jimtafacts/wizard.animation.json')
    dump(RES / 'assets/jugcraft/geckolib/animations/armor/jimtafacts/empty.animation.json', {'format_version': '1.8.0', 'animations': {'idle': {'loop': True, 'bones': {}}}})
    for name,animation in {'root':'root_animations','wisp':'wisp.animation'}.items():
        copy(ART / f'geo/{name}.geo.json', RES / f'assets/jugcraft/geckolib/models/entity/jimtafacts/{name}.geo.json')
        copy(ART / f'animations/{animation}.json', RES / f'assets/jugcraft/geckolib/animations/entity/jimtafacts/{name}.animation.json')
        tex='entity/wisp/wisp' if name=='wisp' else 'entity/root'
        texture('irons_spellbooks:'+tex)
        lang['entity.jugcraft.jimtafacts_' + name]=name.title()
    for kind in ('sword','rapier','claymore'):
        copy(ART / f'geo/summoned_{kind}.geo.json',RES / f'assets/jugcraft/geckolib/models/entity/jimtafacts/summoned_{kind}.geo.json')
        texture('irons_spellbooks:entity/summoned_weapons/summoned_'+kind)
        lang['entity.jugcraft.jimtafacts_summoned_'+kind]='Summoned '+kind.title()
    copy(ART/'animations/summoned_weapon_animations.json',RES/'assets/jugcraft/geckolib/animations/entity/jimtafacts/summoned_weapons.animation.json')
    for kind in ('zombie','skeleton'):
        texture('irons_spellbooks:entity/summoned_'+kind)
        lang['entity.jugcraft.jimtafacts_summoned_'+kind]='Summoned '+kind.title()
    for kind,model_name,animation,tex in [('spectral_hammer','spectral_hammer','spectral_hammer.animation','entity/shield/shield_overlay'),('ice_block','ice_block_projectile','ice_block_animations','entity/ice_block'),('void_tentacle','void_tentacle','void_tentacle_animations','entity/void_tentacle/void_tentacle')]:
        copy(ART/f'geo/{model_name}.geo.json',RES/f'assets/jugcraft/geckolib/models/entity/jimtafacts/{kind}.geo.json')
        copy(ART/f'animations/{animation}.json',RES/f'assets/jugcraft/geckolib/animations/entity/jimtafacts/{kind}.animation.json')
        texture('irons_spellbooks:'+tex)
        lang['entity.jugcraft.jimtafacts_'+kind]=kind.replace('_',' ').title()
    copy(SOURCE/'data/irons_spellbooks/tags/block/spectral_hammer_mineable.json',RES/'data/jugcraft/tags/block/jimtafacts/spectral_hammer_mineable.json')
    attacks.generate(copy,texture,dump,RES,ART,lang)
    personal.generate(copy,texture,dump,RES,ART,lang)
    wards.generate(copy,texture,dump,RES,ART,lang)
    areas.generate(copy,texture,dump,RES,ART,lang)
    mages.generate(copy,texture,dump,RES,ART,lang)
    encounters.generate(copy,texture,dump,RES,ART,lang)
    workshops.generate(copy,definition,model,texture,dump,RES,ART,lang)
    lore.generate(copy,definition,texture,dump,RES,ART,lang)
    exploration.generate(definition,model,dump,RES,ART,lang)
    source_loot.generate(dump,RES,ART,SPELLS)
    structures.generate(copy,texture,dump,RES,ART,lang)
    gear.generate(definition,model,texture,dump,RES,ART,lang)
    curios.generate(definition,model,texture,dump,RES,ART,lang)
    upgrades.generate(copy,definition,texture,dump,RES,ART,lang)
    spells = {}
    for name, (school, focus, cooldown, cast, description) in SPELLS.items():
        ident = PREFIX + 'scroll_' + name
        records['scroll_' + name] = {'kind': 'scroll', 'spell': name}
        source_name = 'angel_wing' if name == 'angel_wings' else name
        lang['item.jugcraft.' + ident] = lang_source.get('spell.irons_spellbooks.' + source_name, name.replace('_', ' ').title()) + ' Scroll'
        lang['spell.jugcraft.jimtafacts/' + name + '.name'] = lang_source.get('spell.irons_spellbooks.' + source_name, name.replace('_', ' ').title())
        lang['spell.jugcraft.jimtafacts/' + name + '.description'] = description + f' Costs {focus} Focus.'
        dump(RES / f'assets/jugcraft/items/{ident}.json', {'model': {'type': 'minecraft:model', 'model': model('irons_spellbooks:item/scroll_' + school)}})
        dump(RES / f'data/jugcraft/spell_assignments/{ident}.json', {'access':'NONE', 'access_param':''})
        icon_name={'soulfire_ray':'firebolt','thunder_step':'volt_strike'}.get(source_name,source_name)
        icon = ART / f'textures/gui/spell_icons/{icon_name}.png'
        if not icon.exists():
            raise ValueError('Missing supplied spell icon ' + str(icon))
        copy(icon, RES / f'assets/jugcraft/textures/spell/jimtafacts/{name}.png')
        spells[name] = {'school': school, 'focus': focus, 'cooldown': int(cooldown * 20), 'description': description}
        dump(RES / f'data/jugcraft/spell/jimtafacts/{name}.json', {
            'school': 'spell_power:' + SCHOOL[school], 'range': 24, 'tier': 1, 'group': 'jimtafacts_spellbooks', 'type': 'ACTIVE',
            'active': {'cast': {'duration': cast, 'animation': {'id': 'jugcraft:kindle_cast'}}},
            'release': {'animation': {'id': 'jugcraft:kindle_release'}}, 'target': {'type': 'CASTER'}, 'deliver': {'type': 'DIRECT'},
            'impacts': [{'action': {'type': 'CUSTOM', 'custom': {'handler': 'jugcraft:jimtafacts_spellbook', 'intent': 'HELPFUL'}}}],
            'cost': {'exhaust': 0, 'durability': 0, 'cooldown': {'duration': cooldown, 'hosting_item': False}}})
        if name in combat.COMBAT:
            dump(RES / f'data/jugcraft/spell/jimtafacts/{name}.json', combat.spell(name, 'spell_power:' + SCHOOL[school], texture, dump, RES))
        # Player Animation Library natively reads this supplied Bedrock format.
        # Preserve authored keyframes; bind the actual source start/finish clips.
        entry=source_info.get(source_name,{})
        path=RES/f'data/jugcraft/spell/jimtafacts/{name}.json'
        spell_data=json.loads(path.read_text(encoding='utf-8'))
        if name in channels.CHANNELS:
            spell_data['active']['cast'].update({'type':'CHANNEL','haste_affected':False,'channel':{'ticks':5 if name=='blaze_storm' else 4 if name=='starfall' else 10,'release_fx':False}})
        if name in wards.CHANNEL_TICKS:
            spell_data['active']['cast'].update({'type':'CHANNEL','haste_affected':False,'channel':{'ticks':wards.CHANNEL_TICKS[name],'release_fx':False}})
        for section,key in [('active','start_animation'),('release','finish_animation')]:
            target=spell_data['active']['cast'] if section=='active' else spell_data['release']
            animation=entry.get(key)
            if animation:target['animation']={'id':'jugcraft:'+animation}
            else:target.pop('animation',None)
        if name in attacks.RECASTS|wards.RECASTS|areas.RECASTS:spell_data['cost']['cooldown']['duration']=0
        dump(path,spell_data)

    forge_recipes={}
    def recipe(name, ingredients, count=1, suffix=''):
        ingredients=distinct(PREFIX+name,ingredients)
        dump(RES / f'data/jugcraft/recipe/{PREFIX}{name}{suffix}.json', {
            'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': ingredients,
            'result': {'id': 'jugcraft:' + PREFIX + name, 'count': count},
            'fabric:load_conditions': [{'condition': 'jugcraft:feature_enabled', 'feature': 'concordance'}]})
        if name.startswith('scroll_'):
            forge_recipes[name.removeprefix('scroll_')]=ingredients
    j = lambda name: 'jugcraft:' + PREFIX + name
    recipe('arcane_essence', ['minecraft:amethyst_shard', 'minecraft:lapis_lazuli', 'minecraft:glowstone_dust'], 4)
    recipe('magic_cloth', ['minecraft:white_wool', j('arcane_essence')], 2)
    recipe('arcane_ingot', ['minecraft:iron_ingot', j('arcane_essence')])
    recipe('common_ink', ['minecraft:ink_sac', 'minecraft:glass_bottle', j('arcane_essence')], 2)
    for index, grade in enumerate(('uncommon', 'rare', 'epic', 'legendary')):
        previous = ('common', 'uncommon', 'rare', 'epic')[index]
        recipe(grade + '_ink', [j(previous + '_ink')] * 3 + [j('arcane_essence')])
    recipe('blank_rune', ['minecraft:stone', j('arcane_essence')], 2)
    reagents = {'holy': 'minecraft:glowstone_dust', 'ender': 'minecraft:ender_pearl', 'nature': 'minecraft:oak_sapling',
                'evocation': 'minecraft:emerald', 'lightning': 'minecraft:copper_ingot', 'fire':'minecraft:blaze_powder',
                'ice':'minecraft:packed_ice', 'blood':'minecraft:nether_wart'}
    for school, reagent in reagents.items():
        recipe(school + '_rune', [j('blank_rune'), reagent])
    recipe('lesser_spell_slot_upgrade', [j('arcane_ingot'), j('rare_ink'), 'minecraft:amethyst_shard'])
    recipe('shriving_stone', ['minecraft:grindstone', j('arcane_essence')])
    for name in STATIONS:
        model('irons_spellbooks:block/'+name)
        dump(RES / f'assets/jugcraft/items/{PREFIX}{name}.json', definition('irons_spellbooks:item/'+name,PREFIX+name))
        dump(RES / f'assets/jugcraft/blockstates/{PREFIX}{name}.json', {'variants':{f'facing={f}':{'model':model('irons_spellbooks:block/'+name),'y':r} for f,r in [('north',0),('east',90),('south',180),('west',270)]}})
        dump(RES / f'data/jugcraft/loot_table/blocks/{PREFIX}{name}.json', {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':j(name)}],'condition':{'type':'minecraft:survives_explosion'}}]})
        copy(ART / f'textures/gui/{name}.png',RES / f'assets/jugcraft/textures/gui/jimtafacts/{name}.png')
        lang['block.jugcraft.'+PREFIX+name]=lang_source['block.irons_spellbooks.'+name]
        recipe(name,['minecraft:crafting_table','minecraft:'+{'inscription_table':'lectern','scroll_forge':'amethyst_block','arcane_anvil':'anvil'}[name],j('arcane_ingot'),j('magic_cloth')])
    for name, (school, focus, cooldown, cast, description) in SPELLS.items():
        grade = 'rare' if focus >= 20 else 'uncommon' if focus >= 10 else 'common'
        reagent=areas.recipes()[name] if name in areas.AREAS else wards.recipes()[name] if name in wards.WARDS else personal.recipes()[name] if name in personal.PERSONAL else attacks.recipes()[name] if name in attacks.ATTACKS else strikes.STRIKES[name][4] if name in strikes.STRIKES else channels.CHANNELS[name][-1] if name in channels.CHANNELS else fields.FIELDS[name][-2] if name in fields.FIELDS else {'heal':'apple','greater_heal':'golden_apple','cleanse':'honey_bottle','fortify':'shield',
                                'haste':'sugar','slow':'cobweb','teleport':'chorus_fruit','summon_ender_chest':'ender_chest',
                                'touch_dig':'iron_pickaxe','wololo':'white_dye','angel_wings':'feather','charge':'redstone',
                                'oakskin':'oak_log','blight':'fermented_spider_eye','gluttony':'bread',
                                'firebolt':'blaze_powder','magic_missile':'amethyst_shard','icicle':'ice', 'root':'mangrove_roots','wisp':'glow_ink_sac',
                                'fireball':'fire_charge','magic_arrow':'arrow','fire_arrow':'arrow','poison_arrow':'arrow',
                                'lightning_lance':'lightning_rod','wither_skull':'wither_skeleton_skull','guiding_bolt':'spectral_arrow',
                                'acid_orb':'slime_ball','ray_of_frost':'blue_ice','sonic_boom':'sculk_shrieker',
                                'shockwave':'copper_block','frostwave':'snow_block','heat_surge':'magma_cream',
                                'blessing_of_life':'glistering_melon_slice','counterspell':'ender_eye','spider_aspect':'cobweb',
                                'summon_vex':'iron_sword','summon_polar_bear':'cod','summon_horse':'saddle','raise_dead':'rotten_flesh','summon_swords':'iron_sword',
                                'spectral_hammer':'iron_pickaxe','ice_block':'packed_ice','sculk_tentacles':'sculk_catalyst','blood_needles':'cactus','acupuncture':'pointed_dripstone','blood_slash':'iron_sword'}[name]
        recipe('scroll_' + name, ['minecraft:paper', j(grade + '_ink'), 'minecraft:echo_shard' if school=='eldritch' else j(school + '_rune'),'minecraft:'+reagent])
    for name in BOOKS:
        if name in ('legendary_spell_book','necronomicon_spell_book'): continue # Boss rewards.
        material = {'copper_spell_book':'minecraft:copper_ingot','iron_spell_book':'minecraft:iron_ingot',
                    'gold_spell_book':'minecraft:gold_ingot','diamond_spell_book':'minecraft:diamond',
                    'netherite_spell_book':'minecraft:netherite_ingot','rotten_spell_book':'minecraft:rotten_flesh',
                    'blaze_spell_book':'minecraft:blaze_rod','dragonskin_spell_book':'minecraft:dragon_breath',
                    'druidic_spell_book':'minecraft:moss_block','villager_spell_book':'minecraft:emerald',
                    'ice_spell_book':'minecraft:blue_ice','wimpy_spell_book':'minecraft:string',
                    'evoker_spell_book':'minecraft:totem_of_undying','cursed_doll_spell_book':'minecraft:rotten_flesh'}[name]
        recipe(name, ['minecraft:book', material, j('magic_cloth'), j('common_ink')])
    # Material construction stays reachable using existing resources. Higher armor
    # tiers pay ordinary material costs rather than creating another ore economy.
    family_reagents = {'wizard':'amethyst_shard', 'wandering_magician':'compass', 'pyromancer':'blaze_rod',
                       'electromancer':'copper_ingot', 'cultist':'rotten_flesh', 'archevoker':'emerald',
                       'cryomancer':'blue_ice', 'shadowwalker':'ender_pearl', 'priest':'glowstone_dust',
                       'plagued':'fermented_spider_eye', 'pumpkin':'carved_pumpkin', 'netherite_mage':'netherite_ingot'}
    for family in ARMOR:
        for part in ('helmet', 'chestplate', 'leggings', 'boots'):
            recipe(f'{family}_{part}', ['minecraft:leather_' + part, j('magic_cloth'),
                                       'minecraft:' + family_reagents[family], j('arcane_essence')])
    for status in ('hastened', 'slowed', 'charge', 'oakskin', 'blight', 'gluttony', 'rend', 'guided', 'chilled', 'spider_aspect', 'vigor', 'echoing_strikes', 'frostbitten_strikes'):
        source_status = {'charge':'charged','frostbitten_strikes':'frostbite'}.get(status,status)
        copy(ART / f'textures/mob_effect/{source_status}.png', RES / f'assets/jugcraft/textures/mob_effect/jimtafacts_{status}.png')
        lang['effect.jugcraft.jimtafacts_' + status] = lang_source.get('effect.irons_spellbooks.' + source_status, status.replace('_', ' ').title())
    dump(RES / 'data/jugcraft/tags/mob_effect/jimtafacts/cleanse_immune.json', {'values': []})
    dump(RES / 'data/jugcraft/tags/entity_type/jimtafacts/root_immune.json', {'values':['minecraft:ender_dragon','minecraft:wither','minecraft:warden']})
    dump(RES / 'data/jugcraft/tags/item/jimtafacts/spellbook_armor_repair.json', {'values':[j('magic_cloth'),j('arcane_ingot')]})
    dump(RES / 'jugcraft/jimtafacts_spellbooks/items.json', records)
    dump(RES / 'jugcraft/jimtafacts_spellbooks/spells.json', spells)
    dump(RES / 'jugcraft/jimtafacts_spellbooks/combat.json', {name:combat.profile(name) for name in combat.COMBAT})
    dump(RES / 'jugcraft/jimtafacts_spellbooks/forge_recipes.json',forge_recipes)
    dump(RES / 'jugcraft/jimtafacts_spellbooks/fields.json',fields.profiles())
    dump(RES / 'jugcraft/jimtafacts_spellbooks/channels.json',channels.profiles())
    copy(ART/'player_animations/casting_animations.json',RES/'assets/jugcraft/player_animations/jimtafacts_casting.json')
    texture('irons_spellbooks:entity/horse/spectral_steed')
    lang['entity.jugcraft.jimtafacts_spectral_steed']='Spectral Steed'
    lang['item.jugcraft.jimta_spellbooks_pumpkin_helmet']='Scarecrow Mage Hat'
    dump(RES / 'jugcraft/jimtafacts_spellbooks/lang.json', lang)
    source_spells=sorted(source_info)
    source_runtime={name:('angel_wings' if name=='angel_wing' else name) for name in source_spells}
    dump(RES / 'jugcraft/jimtafacts_spellbooks/source_status.json', {
        'native_workings': sorted(SPELLS), 'not_yet_ported': [name for name in source_spells if source_runtime[name] not in SPELLS],
        'source_inventory':{name:{**row,'native_id':source_runtime[name] if source_runtime[name] in SPELLS else None,'status':'native level-one adaptation; final combined tests pending' if source_runtime[name] in SPELLS else 'remaining'} for name,row in source_info.items()},
        'source_parity': 'Native level-one workings with shared Focus and economy balancing. Original player animation keyframes, armor geometry/textures and Root/Wisp clips retained. Bespoke encounters and structures remain separate work.',
        'armor': list(ARMOR), 'model_rule':'Supplied meshes and byte-identical textures; bounded face separation, closure and UV repairs are listed in tools/jimtafacts_model_repairs.py. No replacement meshes.'})
    dump(ROOT / 'art/owner-library/catalog/edited-2026-10-23-jimtafacts/spellbooks-runtime-copies.json', copies)
    source_recipes.generate(dump,RES,SOURCE)
    # Translate removed/loader-specific tags to equivalent registered 26.3 members.
    tag_replacements={
        '#minecraft:dripstone_replaceable_blocks':['#minecraft:base_stone_overworld'],
        '#minecraft:wart_blocks':['minecraft:nether_wart_block','minecraft:warped_wart_block'],
        '#c:barrels':['minecraft:barrel'],
        '#c:ingots/gold':['minecraft:gold_ingot'],
        '#curios:spellbook':[j(name) for name in BOOKS],
        'jugcraft:jimta_spellbooks_chronicle':[],
        'jugcraft:jimtafacts/spellbooks/portal_frame':['jugcraft:jimta_spellbooks_portal_frame'],
    }
    for tag_path in (RES/'data/jugcraft/tags').rglob('*.json'):
        if 'jimtafacts' not in tag_path.parts:continue
        tag_data=json.loads(tag_path.read_text(encoding='utf-8'))
        values=[]
        for value in tag_data.get('values',[]):
            if isinstance(value,dict) and value.get('id')=='jugcraft:jimta_spellbooks_arcane_salvage' and not value.get('required',True):continue
            values.extend(tag_replacements.get(value,[value]) if isinstance(value,str) else [value])
        tag_data['values']=values
        dump(tag_path,tag_data)
    dump(RES / "jugcraft/jimtafacts_spellbooks/power_scaling.json",{("angel_wings" if n=="angel_wing" else n):{"base":r.get("baseSpellPower",1),"per_level":r.get("spellPowerPerLevel",0)} for n,r in source_info.items()})
    return lang
if __name__ == '__main__':
    generate()
