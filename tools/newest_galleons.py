"""Adapt the original galleon structures, connector graph, crew markers and treasure data."""
from newest_building import SOURCE,RES,read,write,map_value
from newest_wilds_monastery import source_json,modernize,text_tag
from menagerie_data import loot
import nouveau_nbt
D=SOURCE/'SUPPS/data/supplementaries'
def remap(v):
 if v=='moonlight:spawn_box':return 'minecraft:air'
 if v=='supplementaries:remove_disabled_blocks':return 'jugcraft:supps_galleon_data'
 return map_value(v,'supplementaries','supps')
def clean(v):
 if isinstance(v,str):return remap(v)
 if isinstance(v,list):return [clean(x) for x in v]
 if not isinstance(v,dict):return v
 out={k:clean(x) for k,x in v.items() if k not in ('fabric:load_conditions','neoforge:conditions')}
 if 'conditions' in out:out['conditions']=[x for x in out['conditions'] if x.get('condition')!='moonlight:load_conditions']
 if out.get('type') in ['empty','item','loot_table']:out['type']='minecraft:'+out['type']
 if 'Name' in out:out['id']=out.pop('Name');out['properties']=out.pop('Properties',{})
 return out

def main():
 for source in (D/'structure/galleon').glob('*.nbt'):
  kind,name,root=nouveau_nbt.read(nouveau_nbt.adapt(source.read_bytes(),remap));root=modernize((kind,root))[1]
  for b in root['blocks'][1][1]:
   if 'nbt' not in b:continue
   data=b['nbt'][1];ident=data.get('id',(8,''))[1]
   if ident=='minecraft:air':b.pop('nbt');continue
   if ident.startswith('jugcraft:supps_'):
    data['id']=(8,'jugcraft:supps_household')
    for old,new in [('yaw','CannonYaw'),('pitch','CannonPitch'),('fire_power','CannonPower'),('Password','KeyPassword'),('patterns','FlagPatterns')]:
     if old in data:data[new]=data.pop(old)
    if 'Mimic' in data:data['WayBase']=data.pop('Mimic')
    if 'BucketHolder' in data:
     captive=data.pop('BucketHolder')[1].get('EntityData')
     if captive:
      c=captive[1];c.pop('UUID',None);c.pop('attributes',None);data['Captive']=captive
   if 'components' in data:
    components=data['components'][1]
    for key in ['minecraft:item_name','minecraft:custom_name']:
     if key in components:components[key]=text_tag(components[key])
    components.pop('minecraft:hide_additional_tooltip',None)
  dest=RES/'data/jugcraft/structure/supps_galleon'/source.name;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(nouveau_nbt.write((kind,name,root)))
 for source in (D/'worldgen/template_pool/galleon').glob('*.json'):
  data=clean(source_json(source))
  for entry in data['elements']:
   e=entry['element']
   if e['element_type']!='minecraft:feature_pool_element':continue
   f=e['feature']['feature']
   if isinstance(f,str):f={'entity':'urn','spread':4,'attempts':9}
   else:
    c=f['config'];entity=c['entity']
    if entity=='minecraft:boat':entity='minecraft:'+c['data']['boat_type']+'_boat'
    elif entity=='jugcraft:supps_cannon_boat':entity='jugcraft:supps_cannon_boat_'+c['data']['boat_type']
    f={'entity':entity,'passengers':c.get('passengers',[]),'spread':c.get('spread',0),'attempts':c.get('attempts',1)}
   e['feature']={'feature':{'type':'jugcraft:supps_galleon_crew',**f},'placement':[]};e.pop('processors',None)
  write(RES/'data/jugcraft/worldgen/template_pool/supps_galleon'/source.name,data)
 write(RES/'data/jugcraft/worldgen/processor_list/supps_galleon_data.json',{'processors':[{'processor_type':'jugcraft:supps_galleon_data'}]})
 write(RES/'data/jugcraft/worldgen/structure/supps_galleon.json',{'type':'jugcraft:supps_galleon','biomes':'#minecraft:is_ocean','step':'surface_structures','terrain_adaptation':'none','spawn_overrides':{}})
 write(RES/'data/jugcraft/worldgen/structure_set/supps_galleons.json',{'structures':[{'structure':'jugcraft:supps_galleon','weight':1}],'placement':{'type':'minecraft:random_spread','salt':121047156,'spacing':31,'separation':19,'exclusion_zone':{'other_set':'minecraft:ocean_monuments','chunk_count':5}}})
 for folder in ['galleon','urn_loot']:
  for source in (D/'loot_table/loot'/folder).glob('*.json'):
   data=loot(clean(source_json(source)));data.setdefault('type','minecraft:chest');write(RES/'data/jugcraft/loot_table/supps_loot'/folder/source.name,data)
 source=D/'tags/enchantment/galleon_treasure.json'
 if source.exists():write(RES/'data/jugcraft/tags/enchantment/supps_galleon_treasure.json',clean(read(source)))
if __name__=='__main__':main()
