"""Original IndustrialCraft upgrade art on existing native machine upgrade slots."""
from pathlib import Path
import hashlib
import json
import shutil
ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
SOURCE = ROOT / 'art/owner-library/originals/Oct23/Jimtafacts/industrialcraft reborn by jimbo/assets/ic2/textures/items/upgrade'
UPGRADES = {
 'overclocker': ('overclockerUpgrade', 'Each card multiplies process time by 0.7 and JE per tick by 1.6. Maximum four; shares existing processor upgrade slots.'),
 'energy_storage': ('energyStorageUpgrade', 'Adds 10,000 JE of real processor buffer capacity per card. Removing it preserves stored charge until used.'),
 'transformer': ('transformerUpgrade', 'Quadruples the processor JE input limit per card. Does not generate power or bypass cable capacity.'),
 'ejector': ('ejectorUpgrade', 'Push up to four items per card every eight ticks through configured output faces. Respects claims and sided inventories.'),
 'pulling': ('pullingUpgrade', 'Pull up to four items per card every eight ticks through configured input faces. Respects claims and sided inventories.'),
 'fluid_ejector': ('fluidEjectorUpgrade', 'Adds a 250 mB output transfer every eight ticks. Never drains input-only tanks; respects claims.'),
 'redstone_inverter': ('redstoneinvUpgrade', 'Inverts the processor high/low redstone condition. Redstone-ignored mode stays ignored.'),
}
def items(): return {'jimta_' + name + '_upgrade' for name in UPGRADES}
def dump(path, data):
 path.parent.mkdir(parents=True, exist_ok=True)
 path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
def generate(shared=True):
 manifest=[]; lang={}
 def model(name, image, suffix=''):
  ident='jimta_'+name+'_upgrade'+suffix
  source=SOURCE/(image+'.png'); destination=RES/f'assets/jugcraft/textures/item/jimtafacts/upgrades/{ident}.png'
  destination.parent.mkdir(parents=True,exist_ok=True); shutil.copyfile(source,destination)
  manifest.append({'source':source.relative_to(ROOT).as_posix(),'runtime':destination.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest()})
  dump(RES/f'assets/jugcraft/models/item/{ident}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jugcraft:item/jimtafacts/upgrades/'+ident}})
  return {'type':'minecraft:model','model':'jugcraft:item/'+ident}
 for name,(image,description) in UPGRADES.items():
  ident='jimta_'+name+'_upgrade'; base=model(name,image)
  if name in ('ejector','pulling','fluid_ejector'):
   entries=[{'threshold':0,'model':base}]+[{'threshold':side+1,'model':model(name,image+'.'+str(side),'_'+str(side))} for side in range(6)]
   base={'type':'minecraft:range_dispatch','property':'minecraft:custom_model_data','index':0,'entries':entries,'fallback':base}
  dump(RES/f'assets/jugcraft/items/{ident}.json',{'model':base})
  lang['item.jugcraft.'+ident]=name.replace('_',' ').title()+' Upgrade'
  lang['tooltip.jugcraft.'+ident]=description
  dump(RES/f'data/jugcraft/spell_assignments/{ident}.json',{'access':'NONE','access_param':''})
 ingredients={
  'overclocker':['minecraft:water_bucket','jugcraft:tin_plate','jugcraft:basic_circuit','minecraft:glass_pane'],
  'energy_storage':['jugcraft:lithium_cell','jugcraft:basic_circuit','jugcraft:copper_wire','minecraft:glass_pane'],
  'transformer':['jugcraft:copper_wire']*3+['jugcraft:basic_circuit','jugcraft:steel_plate','minecraft:glass_pane'],
  'ejector':['minecraft:piston','jugcraft:basic_circuit','minecraft:glass_pane'],
  'pulling':['minecraft:sticky_piston','jugcraft:basic_circuit','minecraft:glass_pane'],
  'fluid_ejector':['minecraft:bucket','minecraft:piston','jugcraft:basic_circuit','minecraft:glass_pane'],
  'redstone_inverter':['minecraft:redstone_torch','jugcraft:basic_circuit','minecraft:glass_pane'],
 }
 for name,parts in ingredients.items():
  dump(RES/f'data/jugcraft/recipe/jimtafacts_upgrades/{name}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':parts,'result':{'id':'jugcraft:jimta_'+name+'_upgrade','count':1}})
 if shared:
  path=RES/'assets/jugcraft/lang/en_us.json'; data=json.loads(path.read_text(encoding='utf-8'));data.update(lang);dump(path,data)
 dump(ROOT/'art/owner-library/catalog/edited-2026-10-23-jimtafacts/upgrades-runtime-copies.json',manifest)
 return items()
if __name__=='__main__':
 import sys
 print(f'Generated {len(generate("--no-shared" not in sys.argv))} industrial upgrade items.')
