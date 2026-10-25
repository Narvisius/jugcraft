"""Source weapon stats and assets adapted to native equipment and shared Focus."""
# name: damage, attack speed, durability, school, permanent spell
GEAR={
 'graybeard_staff':(2,-3,0,'arcane',''), 'artificer_cane':(3,-3,0,'arcane',''),
 'ice_staff':(4,-3,0,'ice',''), 'lightning_rod':(4,-3,0,'lightning',''),
 'blood_staff':(7,-3,0,'blood',''), 'pyrium_staff':(8,-2.5,0,'fire',''),
 'magehunter':(6,-2.4,1561,'',''), 'spellbreaker':(9,-2.2,2031,'','counterspell'),
 'claymore':(9,-2.7,1000,'',''), 'amethyst_rapier':(7,-1.7,2031,'','echoing_strikes'),
 'misery':(7,-2.1,1061,'','wither_skull'), 'hellrazor':(12,-2.6,2031,'','raise_hell'),
 'decrepit_scythe':(10,-2.6,1000,'',''), 'boreal_blade':(15,-3.1,2031,'','frostbite'),
 'twilight_gale':(12,-2.6,2031,'','volt_strike'), 'hither_thither_wand':(0,-3,0,'','portal'),
 'staff_of_the_nines':(2,-3,0,'',''), 'autoloader_crossbow':(0,0,465,'','')}
def items():return ['jimta_spellbooks_'+n for n in GEAR]
def generate(definition,model,texture,dump,RES,ART,lang):
 import json
 source_lang=json.loads((ART/'lang/en_us.json').read_text(encoding='utf-8'))
 records={}
 for name,(damage,speed,durability,school,spell) in GEAR.items():
  ident='jimta_spellbooks_'+name
  records[name]={'damage':damage,'speed':speed,'durability':durability,'school':school,'spell':spell}
  if name=='pyrium_staff':
   # The source item has a custom renderer: retain its authored haft and recreate
   # its numeric head/orb model in the native special renderer, including motion.
   base=model('irons_spellbooks:item/pyrium_staff_haft')
   for part in ('head','orb'): texture('irons_spellbooks:item/pyrium_staff_'+part)
   data={'model':{'type':'minecraft:composite','models':[{'type':'minecraft:model','model':base},{'type':'minecraft:special','base':base,'model':{'type':'jugcraft:jimta_pyrium'}}]}}
  elif name=='autoloader_crossbow':
   standby=model('irons_spellbooks:item/'+name)
   arrow=model('irons_spellbooks:item/autoloader_crossbow_arrow')
   firework=model('irons_spellbooks:item/autoloader_crossbow_firework')
   data={'model':{'type':'minecraft:select','property':'minecraft:charge_type','cases':[{'when':'arrow','model':{'type':'minecraft:model','model':arrow}},{'when':'rocket','model':{'type':'minecraft:model','model':firework}}],'fallback':{'type':'minecraft:model','model':standby}}}
  else:data=definition('irons_spellbooks:item/'+name,ident)
  dump(RES/f'assets/jugcraft/items/{ident}.json',data)
  dump(RES/f'data/jugcraft/spell_assignments/{ident}.json',{'access':'NONE','access_param':''})
  lang['item.jugcraft.'+ident]=source_lang.get('item.irons_spellbooks.'+name,name.replace('_',' ').title())
  # Boss weapons are awarded by their actual encounters, not cheaply forged.
  if name in ('hellrazor','decrepit_scythe'):continue
  reagent={'graybeard_staff':'oak_log','artificer_cane':'copper_ingot','ice_staff':'blue_ice',
           'lightning_rod':'lightning_rod','blood_staff':'nether_wart','pyrium_staff':'netherite_ingot',
           'magehunter':'diamond','spellbreaker':'netherite_ingot','claymore':'iron_block',
           'amethyst_rapier':'amethyst_block','misery':'wither_skeleton_skull','boreal_blade':'blue_ice',
           'twilight_gale':'lightning_rod','hither_thither_wand':'ender_pearl',
           'staff_of_the_nines':'gunpowder','autoloader_crossbow':'crossbow'}[name]
  ingredients=['minecraft:'+reagent,'jugcraft:jimta_spellbooks_arcane_ingot','jugcraft:jimta_spellbooks_rare_ink','minecraft:stick']
  if spell:ingredients.append('jugcraft:jimta_spellbooks_scroll_'+spell)
  dump(RES/f'data/jugcraft/recipe/{ident}.json',{'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':ingredients,'result':{'id':'jugcraft:'+ident,'count':1}})
 dump(RES/'jugcraft/jimtafacts_spellbooks/gear.json',records)
