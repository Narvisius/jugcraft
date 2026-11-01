"""Adapt supplied Icarus resources and declarative model methods; never execute source bytecode."""
from pathlib import Path
import re,json,hashlib
from newest_building import write,copy,map_value
from newest_twigs import recipe
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'art/owner-library/originals/Edited-2026-10-29/icarus'
RES=ROOT/'src/main/resources'
REF=ROOT/'build/owner-oct29-reference/dev/cammiescorner/icarus/client/models'
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def main():
 names=sorted(p.stem for p in (SRC/'assets/icarus/models/item').glob('*.json'))
 for folder in ('textures','models'):
  for p in (SRC/'assets/icarus'/folder).rglob('*'):
   if not p.is_file():continue
   target=RES/'assets/jugcraft'/folder/'newest/skyweave'/p.relative_to(SRC/'assets/icarus'/folder)
   if p.suffix=='.json':write(target,map_value(read(p),'icarus','skyweave',True))
   else:copy(p,target)
 for effect in ('energized','flightless'):
  copy(SRC/f'assets/icarus/textures/mob_effect/{effect}.png',RES/f'assets/jugcraft/textures/mob_effect/skyweave_{effect}.png')
 copy(SRC/'assets/icarus/textures/mob_effect/energized.png',RES/'assets/jugcraft/textures/mob_effect/skyweave_second_wind.png')
 for name in names:write(RES/f'assets/jugcraft/items/skyweave_{name}.json',{'model':{'type':'minecraft:model','model':f'jugcraft:newest/skyweave/item/{name}'}})
 for folder in ('recipe','tags'):
  for p in (SRC/'data/icarus'/folder).rglob('*.json'):
   relative=p.relative_to(SRC/'data/icarus'/folder)
   target=RES/'data/jugcraft'/folder/'newest/skyweave'/relative if folder=='recipe' else RES/'data/jugcraft'/folder/relative.parent/('skyweave_'+relative.name)
   data=map_value(read(p),'icarus','skyweave')
   write(target,recipe(data) if folder=='recipe' else data)
 # The source's common elytra tag is not supplied. Preserve its actual vanilla base.
 write(RES/'data/jugcraft/tags/item/skyweave_wings_base.json',{'values':['minecraft:elytra']})
 write(RES/'data/trinkets/slots/chest/wings.json',{'amount':1,'validator_predicates':['trinkets:default']})
 write(RES/'data/trinkets/entities/jugcraft_skyweave.json',{'entities':['minecraft:player'],'slots':['chest/wings']})
 write(RES/'data/trinkets/tags/item/chest/wings.json',{'values':['jugcraft:skyweave_'+n for n in names]})
 langfile=RES/'assets/jugcraft/lang/en_us.json';lang=read(langfile)
 for key,value in read(SRC/'assets/icarus/lang/en_us.json').items():
  if key.startswith('item.icarus.'):lang[key.replace('item.icarus.','item.jugcraft.skyweave_',1)]=value
 lang['trinkets.slot.chest.wings']='Wings'
 for effect in ('flightless','energized','second_wind'):lang['effect.jugcraft.skyweave_'+effect]=effect.replace('_',' ').title()
 for name in ('feathers','leathers'):
  tag=RES/f'data/jugcraft/tags/item/skyweave_{name}.json';data=read(tag);data['values'].append('minecraft:'+('feather' if name=='feathers' else 'leather'));write(tag,data)
 write(langfile,lang);write(RES/'jugcraft/newest_wings.json',{'items':names})
 # Original pivot, mesh and UV instructions, mechanically mapped to the native API.
 methods=[];records=[]
 mapping={'class_5605':'CubeDeformation','method_32096':'mirror','method_32098':'addBox','class_5607':'LayerDefinition','class_5609':'MeshDefinition','class_5610':'PartDefinition','class_5606':'CubeListBuilder','class_5603':'PartPose','method_32111':'getRoot','method_32116':'getChild','method_32117':'addOrReplaceChild','method_32108':'create','method_32101':'texOffs','method_32097':'addBox','method_32100':'addBox','method_32090':'offset','method_32091':'offsetAndRotation','method_32110':'create'}
 for kind in ('Feathered','Leather','Light','Flandres','Discords','Zanzas'):
  original=SRC/f'dev/cammiescorner/icarus/client/models/{kind}WingsModel.class'
  text=(REF/f'{kind}WingsModel.java').read_text(encoding='utf-8')
  body=text.split('public static class_5607 getLayerDefinition() {',1)[1].split('\n    }',1)[0]
  body=re.sub(r'(?:\w+WingsModel|WingEntityModel).getModelData\(\)','base()',body)
  for old,new in mapping.items():body=body.replace(old,new)
  if 'method_' in body or 'class_' in body:raise ValueError('Unmapped model statement: '+kind)
  methods.append('    public static LayerDefinition '+kind.lower()+'(){'+body+'\n    }')
  records.append({'class':original.relative_to(SRC).as_posix(),'sha256':hashlib.sha256(original.read_bytes()).hexdigest()})
 target=ROOT/'src/client/java/io/github/jimbozoomer/jugcraft/client/OwnerWingMeshes.java'
 target.write_text('package io.github.jimbozoomer.jugcraft.client;\nimport net.minecraft.client.model.geom.PartPose;\nimport net.minecraft.client.model.geom.builders.*;\n/** Original supplied geometry and UVs, mechanically remapped by tools/newest_wings.py. */\nfinal class OwnerWingMeshes {\n private static MeshDefinition base(){var mesh=new MeshDefinition();for(String side:new String[]{"leftWing","rightWing"})mesh.getRoot().addOrReplaceChild(side,CubeListBuilder.create(),PartPose.offset(0,5,0));return mesh;}\n'+'\n'.join(methods)+'\n}\n',encoding='utf-8')
 write(ROOT/'art/owner-library/catalog/edited-2026-10-29/wings-integration.json',{'source':'icarus','items':names,'models':records,'sourceExecuted':False,'validation':'Not compiled or tested; native gameplay and source settings require combined validation.'})
 print('Generated',len(names),'wing presentations and',len(methods),'original model definitions')
if __name__=='__main__':main()
