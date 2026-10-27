"""Source workshop art, finite original cauldron recipes, native materials and reward keys."""
import json
BLOCKS=('alchemist_cauldron','blood_cauldron','pedestal','armor_pile','brazier','brazier_soul','bone_vault','cinderous_vault','cinderous_soul_rune','book_stack','firefly_jar','nether_brick_pillar','grimy_tiles','arcane_debris','mithril_ore','deepslate_mithril_ore','wisewood_planks','wisewood_bookshelf','wisewood_chiseled_bookshelf','tyros_statue')
MATERIALS=('blood_vial','bloody_vellum','hogskin','bone_key','decrepit_key','raw_mithril','mithril_ingot','pyrium_ingot','ancient_knowledge_fragment','weapon_parts','weapon_schematics','ice_venom_vial','timeless_slurry','lightning_bottle','mithril_scrap')
ELIXIRS=('oakskin_elixir','greater_oakskin_elixir','greater_healing_potion','invisibility_elixir','greater_invisibility_elixir','evasion_elixir','greater_evasion_elixir','fire_ale')
def items():return ['jimta_spellbooks_'+name for name in (*BLOCKS,*MATERIALS,*ELIXIRS)]
def blocks():return ['jimta_spellbooks_'+name for name in BLOCKS]
def j(name):return 'jugcraft:jimta_spellbooks_'+name
def generate(copy,definition,model,texture,dump,RES,ART,lang):
 from jimtafacts_survival_rewards import generate_ore
 generate_ore(dump, RES)
 source=ART.parents[1]/'data/irons_spellbooks'
 source_lang=json.loads((ART/'lang/en_us.json').read_text(encoding='utf-8'));shapes={}
 for name in BLOCKS:
  data=json.loads((ART/f'blockstates/{name}.json').read_text(encoding='utf-8'))
  def remap(v):
   if isinstance(v,dict):return {k:model(x) if k=='model' else remap(x) for k,x in v.items()}
   if isinstance(v,list):return [remap(x) for x in v]
   return v
  dump(RES/f'assets/jugcraft/blockstates/jimta_spellbooks_{name}.json',remap(data))
  if name=='tyros_statue':
   copy(ART/'geo/statue/tyros_statue.geo.json',RES/'assets/jugcraft/geckolib/models/block/jimtafacts/tyros_statue.geo.json')
   texture('irons_spellbooks:entity/statue/tyros')
   dump(RES/'assets/jugcraft/geckolib/animations/block/jimtafacts/tyros_statue.animation.json',{'format_version':'1.8.0','animations':{'idle':{'loop':True,'bones':{}}}})
   # Original mesh is rendered by the block and item special renderer; the
   # absent external library's particle is replaced only with vanilla stone.
   dump(RES/'assets/jugcraft/models/jimtafacts/spellbooks/block/tyros_statue.json',{'textures':{'particle':'minecraft:block/stone'}})
   dump(RES/'assets/jugcraft/items/jimta_spellbooks_tyros_statue.json',{'model':{'type':'minecraft:special','base':model('irons_spellbooks:item/tyros_statue'),'model':{'type':'geckolib:geckolib'}}})
  elif name=='blood_cauldron':dump(RES/f'assets/jugcraft/items/jimta_spellbooks_{name}.json',{'model':{'type':'minecraft:model','model':model('irons_spellbooks:block/blood_cauldron_level1')}})
  elif not (ART/f'models/item/{name}.json').exists():dump(RES/f'assets/jugcraft/items/jimta_spellbooks_{name}.json',{'model':{'type':'minecraft:model','model':model('irons_spellbooks:block/'+('grimy_tiles_01' if name=='grimy_tiles' else name))}})
  else:dump(RES/f'assets/jugcraft/items/jimta_spellbooks_{name}.json',definition('irons_spellbooks:item/'+name,'jimta_spellbooks_'+name))
  lang['block.jugcraft.jimta_spellbooks_'+name]=source_lang.get('block.irons_spellbooks.'+name,name.replace('_',' ').title())
  if name in ('bone_vault','cinderous_vault'):
   dump(RES/f'data/jugcraft/loot_table/blocks/jimta_spellbooks_{name}.json',{'type':'minecraft:block','pools':[]})
   continue
  drop='raw_mithril' if 'mithril_ore' in name else 'pyrium_ingot' if name=='arcane_debris' else name
  dump(RES/f'data/jugcraft/loot_table/blocks/jimta_spellbooks_{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':j(drop)}],'condition':{'type':'minecraft:survives_explosion'}}]})
 for name in (*MATERIALS,*ELIXIRS):
  dump(RES/f'assets/jugcraft/items/jimta_spellbooks_{name}.json',definition('irons_spellbooks:item/'+name,'jimta_spellbooks_'+name))
  lang['item.jugcraft.jimta_spellbooks_'+name]=source_lang.get('item.irons_spellbooks.'+name,name.replace('_',' ').title())
 from jimtafacts_survival_rewards import debris_loot
 dump(RES/"data/jugcraft/loot_table/blocks/jimta_spellbooks_arcane_debris.json", debris_loot())
 # Keep four independent fluid identities and the source's combined 1000mB cap.
 # The actual original recipes are data input, with only registry names migrated.
 recipes=[]
 for path in sorted((source/'recipe/alchemist_cauldron').glob('*.json')):
  data=json.loads(path.read_text(encoding='utf-8'));data['source_recipe']=path.stem
  # Source loader's old common tags do not exist on the pinned Fabric runtime.
  aliases={'c:ingots/gold':'minecraft:gold_ingot','c:ingots/iron':'minecraft:iron_ingot',
           'c:ingots/copper':'minecraft:copper_ingot','c:gems/amethyst':'minecraft:amethyst_shard'}
  if data.get('input',{}).get('tag') in aliases:
   data['input']={'item':aliases[data['input']['tag']]}
  recipes.append(data)
 dump(RES/'jugcraft/jimtafacts_spellbooks/cauldron_recipes.json',{'recipes':recipes})
 dump(RES/'jugcraft/jimtafacts_spellbooks/workshops.json',{'blocks':list(BLOCKS),'materials':list(MATERIALS),'elixirs':list(ELIXIRS)})
 def recipe(name,ingredients,count=1):dump(RES/f'data/jugcraft/recipe/jimta_spellbooks_{name}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':ingredients,'result':{'id':j(name),'count':count}})
 for name,base in {'alchemist_cauldron':'cauldron','blood_cauldron':'cauldron','pedestal':'chiseled_stone_bricks','armor_pile':'iron_ingot','brazier':'campfire','brazier_soul':'soul_campfire','cinderous_soul_rune':'magma_block','book_stack':'bookshelf','firefly_jar':'lantern','nether_brick_pillar':'nether_bricks','grimy_tiles':'deepslate_tiles','wisewood_planks':'oak_planks','wisewood_bookshelf':'bookshelf','wisewood_chiseled_bookshelf':'chiseled_bookshelf','tyros_statue':'chiseled_stone_bricks'}.items():
  recipe(name,['minecraft:'+base,j('arcane_essence')],4 if name in ('wisewood_planks','grimy_tiles','nether_brick_pillar') else 1)
 recipe('hogskin',['minecraft:leather','minecraft:rotten_flesh']);recipe('weapon_parts',[j('mithril_ingot'),'minecraft:iron_ingot'],2)
 recipe('weapon_schematics',[j('ancient_knowledge_fragment'),'minecraft:paper'])
 dump(RES/'data/jugcraft/recipe/jimta_spellbooks_lesser_spell_slot_upgrade_schematics.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[j('weapon_schematics'),j('mithril_ingot'),j('rare_ink')],'result':{'id':j('lesser_spell_slot_upgrade')}})
 for tag,names in {'enchantment_power_provider':['wisewood_bookshelf'], 'mineable/pickaxe':[n for n in BLOCKS if not n.startswith('wisewood') and n not in ('book_stack',)],'mineable/axe':['wisewood_planks','wisewood_bookshelf','wisewood_chiseled_bookshelf','book_stack']}.items():
  path=RES/f'data/minecraft/tags/block/{tag}.json';data=json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'replace':False,'values':[]}
  data['values']=sorted(set(data.get('values',[]))|{j(n) for n in names});dump(path,data)
