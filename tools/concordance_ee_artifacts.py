"""Remaining owner alchemical artifacts: original Jugcraft behavior over the existing essence ledger."""
MOD='jugcraft'
def rid(n): return f'{MOD}:{n}'
SPECS={
 'swiftwolf_rending_gale':("Swiftwolf's Rending Gale",'rings/swrg_off','rings/swrg_on1','Use: toggle; sneak-use: Flight / Repulsion / Both. Hold or wear. Flight costs 16 essence/s; repulsion 8 per hostile, up to four/s. Double-jump to fly; losing power gives 5s slow falling.'),
 'harvest_goddess_band':('Harvest Goddess Band','rings/harvest_god_off','rings/harvest_god_on','Use: toggle growth. Hold/wear to grow up to four nearby crops each second, 16 essence per step. Use on a ripe crop: harvest and replant, 16 essence. Special crops retain nutrient requirements.'),
 'ignition_ring':('Ignition Ring','rings/ignition_off','rings/ignition_on','Use: toggle. Fire protection while burning costs 8 essence/s. Scorches up to four visible nearby hostiles for 16 each/s. Use on an unlit campfire/candle: light it for 8. Does not ignite terrain.'),
 'zero_ring':('Zero Ring','rings/zero_off','rings/zero_on','Use: toggle. Chills and slows up to four visible hostiles within five blocks, 8 essence each/s. Use on source water: temporary frosted ice, 32 essence.'),
 'arcana_ring':('Arcana Ring','rings/arcana_0','rings/arcana_0_on','Use: toggle; sneak-use: Ice / Fire / Harvest / Wind. Combines the corresponding ring behavior and costs; wind combines flight and repulsion. Hold or wear. One mode at a time.'),
 'archangel_smite':("Archangel's Smite",'rings/archangel_smite',None,'Use: aimed spirit shot, 8 damage for 64 essence, up to 24 blocks. Sneak-use: toggle automatic shots at visible hostiles within 12 blocks, at most one/s. Hold or wear. No recoverable arrows.'),
 'body_stone':('Body Stone','rings/body_stone_off','rings/body_stone_on','Use: toggle. Hold or wear to restore one missing food point each second for 64 essence. Duplicate Body/Life stones do not stack.'),
 'soul_stone':('Soul Stone','rings/soul_stone_off','rings/soul_stone_on','Use: toggle. Hold or wear to heal one missing health point each second for 64 essence. Duplicate Soul/Life stones do not stack.'),
 'life_stone':('Life Stone','rings/life_stone_off','rings/life_stone_on','Use: toggle. Combines Body and Soul stone support; each missing food/health point restored costs 64 essence. Hold or wear.'),
 'watch_of_flowing_time':('Watch of Flowing Time','rings/time_watch_off','rings/time_watch_on','Use: toggle. Slows up to four nearby visible hostiles for 16 essence each/s. Look at your condenser or Matter furnace within six blocks for one paid extra processing step/s, 128 essence. No global tick acceleration.'),
 'divining_rod_1':('Divining Rod I','divining_rod_1',None,'Aim at a block and use: survey its surrounding 3x3x3 loaded volume for ores, up to eight blocks away; 4 essence. Reports up to six ore types. Protected terrain is excluded.'),
 'divining_rod_2':('Divining Rod II','divining_rod_2',None,'Aim at a block and use: survey 5x5x5 for ores, up to 16 blocks away; 16 essence. Loaded, permitted terrain only.'),
 'divining_rod_3':('Divining Rod III','divining_rod_3',None,'Aim at a block and use: survey 7x7x7 for ores, up to 24 blocks away; 64 essence. Loaded, permitted terrain only.'),
 'destruction_catalyst':('Destruction Catalyst','destruction_catalyst',None,'Main hand, use on a block: mine a 3x3 face for 16 essence per successful block. Ordinary tool drops/durability and claims apply; containers, fluids and very hard blocks are skipped.'),
 'catalytic_lens':('Catalytic Lens','catalytic_lens',None,'Main hand, use on a block: mine a 3x3x3 volume for 32 essence per successful block. Ordinary tool drops/durability and claims apply; no explosions or entity damage.'),
 'hyperkinetic_lens':('Hyperkinetic Lens','hyperkinetic_lens',None,'Main hand, use on a block: mine a broad 5x5 face for 32 essence per successful block. Ordinary tool drops/durability and claims apply.'),
 'mercurial_eye':('Mercurial Eye','mercurial_eye',None,'Main hand, use on a block face: place up to nine blocks from your offhand in a 3x3 plane, costing 8 essence per placed block. Plain catalogued blocks only; ordinary placement and collision rules apply.'),
}
ITEMS={n:{'name':s[0]} for n,s in SPECS.items()}
TOOLTIPS={n:s[3]+' No research required.' for n,s in SPECS.items()}
TOOLS={'destruction_catalyst','catalytic_lens','hyperkinetic_lens'}
CHARMS={'body_stone','soul_stone','life_stone','watch_of_flowing_time'}
RINGS=set(SPECS)-TOOLS-CHARMS-{'divining_rod_1','divining_rod_2','divining_rod_3','mercurial_eye'}
OWNER_FILES={}
for n,(_,off,on,_) in SPECS.items():
 OWNER_FILES[f'textures/item/{n}.png']=f'textures/item/{off}.png'
 if on:OWNER_FILES[f'textures/item/{n}_active.png']=f'textures/item/{on}.png'
for i in range(1,4):
 for active in ('','_on'):OWNER_FILES[f'textures/item/arcana_ring_mode{i}{active}.png']=f'textures/item/rings/arcana_{i}{active}.png'
OWNER_FILES['textures/item/swiftwolf_rending_gale_repel.png']='textures/item/rings/swrg_on3.png'
OWNER_FILES['textures/item/swiftwolf_rending_gale_both.png']='textures/item/rings/swrg_on2.png'
SOUND_NAMES=('chargetick','gust','kinesis','launch','nova','pecharge','pedestruct','peheal','pepower','petransmute','peuncharge','pewatermagic','pewindmagic','philball','tock')
for name in SOUND_NAMES:OWNER_FILES[f'sounds/alchemy/{name}.ogg']=f'sounds/item/{name}.ogg'
def sounds():return {f'alchemy_{n}':{'sounds':[rid(f'alchemy/{n}')],'subtitle':f'subtitles.jugcraft.alchemy_{n}'} for n in SOUND_NAMES}

INGREDIENTS={
 'swiftwolf_rending_gale':['jugcraft:iron_band','jugcraft:dark_matter','minecraft:feather','minecraft:ghast_tear'],
 'harvest_goddess_band':['jugcraft:iron_band','jugcraft:dark_matter','minecraft:wheat','minecraft:carrot','minecraft:potato','minecraft:bone_meal'],
 'ignition_ring':['jugcraft:iron_band','jugcraft:dark_matter','minecraft:blaze_rod','minecraft:magma_cream'],
 'zero_ring':['jugcraft:iron_band','jugcraft:dark_matter','minecraft:packed_ice','minecraft:snowball'],
 'arcana_ring':['jugcraft:zero_ring','jugcraft:ignition_ring','jugcraft:harvest_goddess_band','jugcraft:swiftwolf_rending_gale','jugcraft:red_matter'],
 'archangel_smite':['jugcraft:iron_band','jugcraft:dark_matter','minecraft:bow','minecraft:feather','minecraft:ender_pearl'],
 'body_stone':['jugcraft:dark_matter','minecraft:golden_carrot','minecraft:gold_ingot'],
 'soul_stone':['jugcraft:dark_matter','minecraft:ghast_tear','minecraft:gold_ingot'],
 'life_stone':['jugcraft:body_stone','jugcraft:soul_stone','jugcraft:red_matter'],
 'watch_of_flowing_time':['minecraft:clock','jugcraft:red_matter','minecraft:gold_ingot','minecraft:lapis_lazuli'],
 'divining_rod_1':['minecraft:stick','minecraft:stick','jugcraft:low_covalence_dust'],
 'divining_rod_2':['jugcraft:divining_rod_1','jugcraft:medium_covalence_dust','minecraft:iron_ingot'],
 'divining_rod_3':['jugcraft:divining_rod_2','jugcraft:high_covalence_dust','minecraft:diamond'],
 'destruction_catalyst':['jugcraft:dark_matter','minecraft:flint_and_steel','minecraft:obsidian','minecraft:gunpowder'],
 'catalytic_lens':['jugcraft:destruction_catalyst','jugcraft:red_matter','minecraft:glass'],
 'hyperkinetic_lens':['jugcraft:red_matter','minecraft:ender_pearl','minecraft:glass','minecraft:gunpowder'],
 'mercurial_eye':['jugcraft:dark_matter','minecraft:ender_eye','minecraft:glass','minecraft:stone_bricks'],
}
def model(n):return {'type':'minecraft:model','model':rid(f'item/{n}')}
def active(off,on):return {'type':'minecraft:condition','property':'minecraft:has_component','component':rid('alchemy_active'),'on_false':model(off),'on_true':model(on)}
def write_all(write,assets,data,lang,condition,self_drop):
 lang.update({'message.jugcraft.artifact.survey':'Ore survey: %s','message.jugcraft.artifact.state':'%s: %s | mode %s'})
 for n in SOUND_NAMES:lang[f'subtitles.jugcraft.alchemy_{n}']='Alchemical '+{'pecharge':'charging','peuncharge':'discharging','peheal':'healing','pedestruct':'excavation','petransmute':'transmutation','pewatermagic':'water','pewindmagic':'wind','philball':'projectile','chargetick':'charge tick'}.get(n,n)
 for dest in OWNER_FILES:
  if not dest.startswith('textures/item/'):continue
  name=dest[len('textures/item/'):-4]
  write(assets/f'models/item/{name}.json',{'parent':'minecraft:item/handheld' if name in TOOLS else 'minecraft:item/generated','textures':{'layer0':rid(f'item/{name}')}})
 for n,s in SPECS.items():
  lang[f'item.jugcraft.{n}']=s[0];lang[f'tooltip.jugcraft.{n}']=TOOLTIPS[n]
  visual=active(n,n+'_active') if s[2] else model(n)
  if n=='arcana_ring':
   for i in range(1,4):visual={'type':'minecraft:condition','property':'minecraft:has_component','component':rid(f'artifact_mode_{i}'),'on_true':active(f'arcana_ring_mode{i}',f'arcana_ring_mode{i}_on'),'on_false':visual}
  if n=='swiftwolf_rending_gale':
   for i,suffix in [(1,'repel'),(2,'both')]:visual={'type':'minecraft:condition','property':'minecraft:has_component','component':rid(f'artifact_mode_{i}'),'on_true':active(n,n+'_'+suffix),'on_false':visual}
  write(assets/f'items/{n}.json',{'model':visual})
  write(data/f'recipe/{n}.json',{'fabric:load_conditions':condition('concordance'),'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':INGREDIENTS[n],'result':{'id':rid(n),'count':1}})
def tags(tags):
 for n in ITEMS:tags.add('item',rid('equivalence/excluded'),rid(n))
 for n in sorted(RINGS):tags.add('item','trinkets:hand/ring',rid(n))
 for n in sorted(CHARMS):tags.add('item','trinkets:legs/charm',rid(n))
 for n in sorted(TOOLS):tags.add('item','c:tools',rid(n))
def item_tags():return {tag:[rid(n) for n in sorted(TOOLS)] for tag in ('pickaxes','enchantable/durability','enchantable/mining','enchantable/mining_loot','enchantable/vanishing')}
def codex():
 entries={}
 groups=[('artifact_rings','Rings and Living Stones',list(SPECS)[:10],10,2),('artifact_tools','Divining and Shaping',list(SPECS)[10:],12,2)]
 for key,title,names,x,y in groups:
  entries['equivalence',key]={'name':title,'x':x,'y':y,'icon':rid(names[0]),'condition':None,'description':'Paid alchemical equipment, no spell composer required','pages':
   [('text','Shared Controls','Passive artifacts work only while held or worn in the matching Ring/Charm slot, with enough personal **essence**. Merely carrying one does nothing. Use toggles activation; Arcana/Swiftwolf sneak-use changes mode. All areas and entity counts are bounded, claims and friendly-fire checks apply. Equipment uses the wallet first, then carried Klein Stars. Active artifacts can also have bag/chest or pedestal behavior described in Connected Alchemy.')] +
   [('crafting_recipe',SPECS[n][0],TOOLTIPS[n],rid(n)) for n in names]}
 return entries

# Owner-requested connected behavior supersedes the original manual-only descriptions.
from concordance_ee_expansion import TOOLTIPS as _connected_tooltips
TOOLTIPS.update({k:v for k,v in _connected_tooltips.items() if k in TOOLTIPS})
