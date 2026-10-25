"""Reproducible runtime copies of owner-supplied Jimtafacts assets; no source code execution."""
from pathlib import Path
import json
import shutil
import hashlib

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'art/owner-library/originals/Oct23/Jimtafacts'
RES = ROOT / 'src/main/resources'

# Native implementations only. Unimplemented collection entries stay in the intake catalog.
ACCESSORIES = {
    'crystal_heart': ('legs/charm', 'Adds five hearts while worn.', {'max_health': 10}),
    'power_glove': ('offhand/glove', 'Adds 3 melee attack damage.', {'attack_damage': 3}),
    'feral_claws': ('offhand/glove', 'Adds 1 attack speed.', {'attack_speed': 1}),
    'pocket_piston': ('offhand/glove', 'Adds 1 attack knockback.', {'attack_knockback': 1}),
    'digging_claws': ('offhand/glove', 'Doubles block-breaking speed and harvests stone-tier pickaxe blocks with your claws.', {'block_break_speed': 1}),
    'charm_of_shrinking': ('legs/charm', 'Shrinks the wearer to half size.', {'scale': -.5}),
    'bunny_hoppers': ('feet/boots', 'Higher jumps, five extra safe-fall blocks and half fall damage.', {'jump_strength': .2, 'safe_fall_distance': 5, 'fall_damage_multiplier': -.5}),
    'steadfast_spikes': ('feet/boots', 'Strong knockback resistance and firm footing on slippery ice.', {'knockback_resistance': .8}),
    'flippers': ('feet/boots', 'Removes the underwater movement penalty.', {'water_movement_efficiency': 1}),
    'snorkel': ('head/hat', 'Breathe underwater while worn.', {}),
    'night_vision_goggles': ('head/hat', 'Night vision while worn; fades within twelve seconds after removal.', {}),
    'scarf_of_invisibility': ('chest/necklace', 'Invisibility while worn; armor remains visible.', {}),
    'panic_necklace': ('chest/necklace', 'Taking damage grants Speed II for five seconds. Ten-second cooldown.', {}),
    'cross_necklace': ('chest/necklace', 'Extends the brief invulnerability after taking a hit.', {}),
    'obsidian_skull': ('legs/charm', 'After fire hurts you, grants ten seconds of fire resistance. Thirty-second cooldown.', {}),
    'antidote_vessel': ('legs/charm', 'Clears poison and wither once every ten seconds.', {}),
    'universal_attractor': ('legs/charm', 'Draws nearby loose items toward you. Respects claims and pickup ownership.', {}),
    'fire_gauntlet': ('offhand/glove', 'Melee hits ignite permitted targets for four seconds.', {}),
    'vampiric_glove': ('offhand/glove', 'Melee hits heal 20% of damage, at most one heart per second.', {}),
    'withered_bracelet': ('hand/bracelet', 'Melee hits inflict Wither for three seconds. Five-second cooldown.', {}),
    'thorn_pendant': ('chest/necklace', 'Returns 2 damage to permitted melee attackers. Three-second cooldown.', {}),
    'flame_pendant': ('chest/necklace', 'Ignites permitted melee attackers for four seconds. Five-second cooldown.', {}),
    'shock_pendant': ('chest/necklace', 'Resists lightning and shocks permitted melee attackers for 3 damage. Five-second cooldown.', {}),
    'rooted_boots': ('feet/boots', 'Standing still on grass restores food; eating encourages nearby plants to grow.', {}),
    'running_shoes': ('feet/boots', 'Twenty-five percent faster sprinting and one-block step height.', {}),
    'charm_of_sinking': ('legs/charm', 'Sink toward the bottom underwater; adds oxygen reserve.', {'oxygen_bonus': 2}),
}
ACCESSORIES.update({
    'anglers_hat': ('head/hat', 'Adds one level of Lure and Luck of the Sea while fishing.', {}),
    'aqua_dashers': ('feet/boots', 'Sprint across water; stop sprinting or sneak to dive.', {}),
    'chorus_totem': ('legs/charm', 'Consumed to rescue you from a lethal hit and teleport to nearby safe ground.', {}),
    'cloud_in_a_bottle': ('legs/charm', 'Press jump again while airborne for one extra jump.', {'safe_fall_distance': 3}),
    'cowboy_hat': ('head/hat', 'Your living mount moves 25% faster while you ride it.', {}),
    'golden_hook': ('offhand/glove', 'Mobs you defeat drop 50% more experience.', {}),
    'helium_flamingo': ('legs/charm', 'Hold sprint in midair to swim through the air for up to eight seconds. Recharges on the ground.', {}),
    'kitty_slippers': ('feet/boots', 'Nearby creepers and phantoms avoid you unless you attack them.', {}),
    'lucky_scarf': ('chest/necklace', 'Adds one level of Fortune to block drops.', {}),
    'novelty_drinking_hat': ('head/hat', 'Eat and drink twice as quickly.', {}),
    'plastic_drinking_hat': ('head/hat', 'Eat and drink twice as quickly.', {}),
    'onion_ring': ('hand/bracelet', 'Eating while wearing this grants Haste for ten seconds per food point.', {}),
    'pickaxe_heater': ('legs/charm', 'Automatically smelts mined drops using their furnace recipes.', {}),
    'snowshoes': ('feet/boots', 'Walk over powder snow, resist freezing and move faster on snow.', {}),
    'strider_shoes': ('feet/boots', 'Sneak to walk on lava; protects against hot floors.', {}),
    'superstitious_hat': ('head/hat', 'Adds one level of Looting to mobs you defeat.', {}),
    'villager_hat': ('head/hat', 'Villagers offer their friendly reputation discount while worn.', {}),
    'warp_drive': ('legs/charm', 'Throw ender pearls for two food points instead of consuming them; prevents pearl damage.', {}),
    'whoopee_cushion': ('legs/charm', 'Sneak to surprise everyone nearby with a familiar sound.', {}),
})
SPECIAL = {'umbrella', 'mimic_spawn_egg'}
FOOD = {'everlasting_beef': (3, .3), 'eternal_steak': (8, .8)}

def items():
    return ['jimta_' + name for name in sorted(set(ACCESSORIES) | set(FOOD) | SPECIAL)]

def slot_links(slot):
    return {'#jugcraft:jimtafacts/' + slot} if any(row[0] == slot for row in ACCESSORIES.values()) else set()

def dump(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')

def generate():
    lang_source = json.loads((SOURCE / 'assets/artifacts/lang/en_us.json').read_text(encoding='utf-8'))
    lang = {}
    copies = []
    catalog = {}
    for name in sorted(set(ACCESSORIES) | set(FOOD) | SPECIAL):
        ident = 'jimta_' + name
        if name != 'mimic_spawn_egg':
            src = SOURCE / f'assets/artifacts/textures/item/{name}.png'
            dest = RES / f'assets/jugcraft/textures/item/jimtafacts/{name}.png'
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(src, dest)
            if src.with_suffix('.png.mcmeta').exists():
                shutil.copyfile(src.with_suffix('.png.mcmeta'), dest.with_suffix('.png.mcmeta'))
            copies.append({'source': src.relative_to(ROOT).as_posix(), 'runtime': dest.relative_to(ROOT).as_posix(),
                           'sha256': hashlib.sha256(src.read_bytes()).hexdigest(), 'changes': 'None; byte-identical texture.'})
        if name != 'mimic_spawn_egg': dump(RES / f'assets/jugcraft/models/item/{ident}.json', {'parent':'minecraft:item/generated', 'textures':{'layer0':f'jugcraft:item/jimtafacts/{name}'}})
        dump(RES / f'assets/jugcraft/items/{ident}.json', {'model':{'type':'minecraft:model','model':f'jugcraft:item/{ident}'}})
        lang[f'item.jugcraft.{ident}'] = lang_source[f'item.artifacts.{name}']
        if name in ACCESSORIES:
            slot, description, attributes = ACCESSORIES[name]
            catalog[name] = {'slot': slot, 'description': description, 'attributes': attributes}
        elif name in SPECIAL:
            catalog[name] = {'slot': '', 'special': name, 'description': 'A gliding umbrella that also blocks attacks.' if name == 'umbrella' else 'Spawns a dormant treasure mimic.', 'attributes': {}}
        else:
            nutrition, saturation = FOOD[name]
            catalog[name] = {'slot': '', 'description': 'Reusable food. One-minute cooldown after eating.', 'nutrition': nutrition, 'saturation': saturation, 'attributes': {}}
        lang[f'tooltip.jugcraft.{ident}'] = catalog[name]['description']
    # Retain supplied 3-D handheld models and display transforms. Repair-only predicates are disabled
    # because the supplied default configuration disables artifact durability.
    def remap(value):
        if isinstance(value, str):
            return value.replace('artifacts:item/', 'jugcraft:item/jimtafacts/').replace('artifacts:entity/', 'jugcraft:entity/jimtafacts/')
        if isinstance(value, list): return [remap(x) for x in value]
        if isinstance(value, dict):
            if value.get('property') == 'artifacts:needs_repair': return remap(value['on_false'])
            return {k:remap(v) for k,v in value.items()}
        return value
    for source in (SOURCE / 'assets/artifacts/models/item').glob('*.json'):
        from jimtafacts_model_repairs import repair
        dump(RES / 'assets/jugcraft/models/item/jimtafacts' / source.name, repair('item/jimtafacts/'+source.stem,remap(json.loads(source.read_text(encoding='utf-8')))))
    for source in (SOURCE / 'assets/artifacts/textures/item').glob('*'):
        if source.is_file():
            dest = RES / 'assets/jugcraft/textures/item/jimtafacts' / source.name
            shutil.copyfile(source, dest)
    for name in items():
        source = SOURCE / f'assets/artifacts/items/{name.removeprefix("jimta_")}.json'
        if source.exists():
            definition = remap(json.loads(source.read_text(encoding='utf-8')))
            # Source damage predicates are rendering-only; no foreign custom codecs remain at runtime.
            dump(RES / f'assets/jugcraft/items/{name}.json', definition)
    # Resource packs permit additive files for entity slots/tags. Use a nested tag to avoid replacing existing tags.
    slots = sorted({x[0] for x in ACCESSORIES.values()})
    dump(RES / 'data/trinkets/entities/jugcraft_jimtafacts.json', {'entities':['player'], 'slots':slots})
    for slot in slots:
        values = ['jugcraft:jimta_' + name for name, row in ACCESSORIES.items() if row[0] == slot]
        dump(RES / f'data/jugcraft/tags/item/jimtafacts/{slot}.json', {'replace': False, 'values': values})
        path = RES / f'data/trinkets/tags/item/{slot}.json'
        old = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'replace':False, 'values':[]}
        entry = '#jugcraft:jimtafacts/' + slot
        if entry not in old['values']: old['values'].append(entry)
        dump(path, old)
    dump(RES / 'jugcraft/jimtafacts/accessories.json', catalog)
    # Native language files cannot be split. This import is repeated by the canonical generator.
    lang_path = RES / 'assets/jugcraft/lang/en_us.json'
    existing = json.loads(lang_path.read_text(encoding='utf-8')); existing.update(lang); dump(lang_path, existing)
    for kind, time in [('smelting',200),('smoking',100),('campfire_cooking',600)]:
        dump(RES / f'data/jugcraft/recipe/jimta_eternal_steak_{kind}.json', {
            'fabric:load_conditions':[{'condition':'jugcraft:feature_enabled','feature':'concordance'}],
            'type':'minecraft:'+kind, 'category':'food', 'ingredient':'jugcraft:jimta_everlasting_beef',
            'result': {'id':'jugcraft:jimta_eternal_steak'}, 'experience':0, 'cookingtime':time})
    # Supplied item weights, campsite provisions, mimic rewards and sounds are preserved.
    def loot_remap(value):
        if isinstance(value, str) and value.startswith('artifacts:'):
            rest=value.split(':',1)[1]
            return 'jugcraft:jimta_'+rest if rest in set(ACCESSORIES)|set(FOOD)|SPECIAL else 'jugcraft:jimtafacts/'+rest
        if isinstance(value,list): return [loot_remap(x) for x in value if not (isinstance(x,dict) and x.get('condition')=='artifacts:config_value')]
        if isinstance(value,dict):
            if value.get('condition')=='artifacts:artifact_rarity_adjusted_chance':return {'type':'minecraft:random_chance','chance':value['default_probability']}
            if value.get('condition')=='artifacts:config_value_chance':return {'type':'minecraft:random_chance','chance':0.01}
            result={}
            for key,entry in value.items():
                if key=='conditions':
                    terms=loot_remap(entry)
                    if terms:result['condition']=terms[0] if len(terms)==1 else {'type':'minecraft:all_of','terms':terms}
                elif key=='functions':result['modifier']=loot_remap(entry)
                elif key=='function' or key=='condition' and isinstance(entry,str):result['type']=loot_remap(entry)
                else:result[key]=loot_remap(entry)
            return result
        return value
    loot_root=SOURCE/'data/artifacts/loot_table'
    for source in loot_root.rglob('*.json'):
        rel=source.relative_to(loot_root)
        if rel.parts[0] not in ('items','chests','entities') and rel.as_posix()!='artifact.json': continue
        dest=RES/'data/jugcraft/loot_table/jimtafacts'/rel
        if rel.as_posix()=='entities/mimic.json':dest=RES/'data/jugcraft/loot_table/entities/jimta_mimic.json'
        dump(dest,loot_remap(json.loads(source.read_text(encoding='utf-8'))))
    palette_source=json.loads((SOURCE/'data/artifacts/worldgen/configured_feature/campsite.json').read_text(encoding='utf-8'))['config']
    palette={}
    for name,provider in palette_source.items():
        if 'state' in provider:palette[name]=[provider['state']]
        elif 'entries' in provider:palette[name]=[entry['data'] for entry in provider['entries'] for _ in range(entry['weight'])]
    # 26.3 block states use id/properties, not the older NBT-shaped Name/Properties.
    palette={name:[{'id':state['Name'], **({'properties':state['Properties']} if state.get('Properties') else {})}
                   for state in states] for name,states in palette.items() if states}
    dump(RES/'data/jugcraft/worldgen/feature/jimtafacts/campsite.json',{'type':'jugcraft:jimta_campsite','palette':palette})
    dump(RES/'data/jugcraft/worldgen/placed_feature/jimtafacts/campsite.json',{'feature':'jugcraft:jimtafacts/campsite','placement':[
        {'type':'minecraft:rarity_filter','chance':10},{'type':'minecraft:in_square'},
        {'type':'minecraft:height_range','height':{'type':'minecraft:uniform','min_inclusive':{'absolute':-48},'max_inclusive':{'absolute':32}}},{'type':'minecraft:biome'}]})
    soundfile=RES/'assets/jugcraft/sounds.json';sounddata=json.loads(soundfile.read_text(encoding='utf-8'))
    for name,files in {'mimic_hurt':['mimic/hurt1','mimic/hurt2','mimic/hurt3'],'mimic_death':['mimic/death1','mimic/death2'],'fart':['fart1','fart2']}.items():
        sounddata['jimtafacts/'+name]={'sounds':['jugcraft:jimtafacts/'+f for f in files]}
        for file in files:
            src=SOURCE/f'assets/artifacts/sounds/{file}.ogg';dest=RES/f'assets/jugcraft/sounds/jimtafacts/{file}.ogg';dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dest)
    dump(soundfile,sounddata)
    dest=RES/'assets/jugcraft/textures/entity/jimtafacts/mimic.png';dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(SOURCE/'assets/artifacts/textures/entity/mimic.png',dest)
    copies.append({'source':(SOURCE/'assets/artifacts/textures/entity/mimic.png').relative_to(ROOT).as_posix(),
                   'runtime':dest.relative_to(ROOT).as_posix(),
                   'sha256':hashlib.sha256((SOURCE/'assets/artifacts/textures/entity/mimic.png').read_bytes()).hexdigest(),
                   'changes':'None; original entity atlas retained.'})
    lang_path=RES/'assets/jugcraft/lang/en_us.json';existing=json.loads(lang_path.read_text(encoding='utf-8'));existing['entity.jugcraft.jimta_mimic']='Chest Mimic';existing['item.jugcraft.jimta_mimic_spawn_egg']='Chest Mimic Spawn Egg';dump(lang_path,existing)
    dump(ROOT / 'art/owner-library/catalog/edited-2026-10-23-jimtafacts/runtime-copies.json', copies)
    return catalog

if __name__ == '__main__':
    print(f'Generated {len(generate())} native accessory/food resources from supplied textures.')
