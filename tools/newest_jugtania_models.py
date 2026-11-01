"""Recover original botanical pylon geometry from static decompiler output only."""
import re,struct,hashlib
from pathlib import Path
from recover_wildlife_models import geometry
from newest_building import SOURCE,RES,write

ROOT=Path(__file__).resolve().parents[1]
def main():
 records=[]
 for model,texture in [('AvatarModel','avatar'),('CloakModel','cloak_holy'),('CorporeaIndexBlockEntityRenderer','corporea_index'),('PixieModel','pixie'),('GaiaPylonModel','pylon_gaia'),('NaturaPylonModel','pylon_natura'),('ManaPylonModel','pylon_mana'),('BellowsModel','bellows'),('HourglassModel','hourglass'),('BotanicalBreweryModel','brewery'),('TeruTeruBozuModel','teru_teru_bozu')]:
  folder='render/block_entity' if model=='CorporeaIndexBlockEntityRenderer' else 'model'
  source=ROOT/'build/owner-oct29-reference/vazkii/botania/client'/folder/(model+'.java')
  text=source.read_text(encoding='utf-8')
  text=re.sub(r'CubeDeformation deformation = new CubeDeformation\(0.01f\);','',text)
  text=re.sub(r'\bdeformation\b','new CubeDeformation(0.01f)',text)
  text=text.replace('PartPose.m_171430_(','PartPose.offsetAndRotation(0,0,0,')
  for old,new in {'m_171576_':'getRoot','m_171599_':'addOrReplaceChild','m_171558_':'create','m_171514_':'texOffs','m_171488_':'addBox','m_171480_':'mirror','m_171481_':'addBox','m_171419_':'offset','m_171423_':'offsetAndRotation'}.items():text=text.replace(old,new)
  text=text.replace('public static MeshDefinition createMesh()','public static LayerDefinition createBodyLayer()')
  text=text.replace('PartPose.f_171404_','PartPose.offset(0,0,0)')
  text=text.replace('((float)Math.PI)','3.1415927f')
  png=SOURCE/'Jugtania/assets/botania/textures/model'/(texture+'.png');width,height=struct.unpack('>II',png.read_bytes()[16:24])
  text=text.replace('return mesh;',f'return LayerDefinition.create((MeshDefinition)mesh, (int){width}, (int){height});')
  text=re.sub(r'(?m)^(\s*)root\.addOrReplaceChild\("([^"\n]+)"',lambda m:m[1]+'PartDefinition recovered_'+m[2]+' = root.addOrReplaceChild("'+m[2]+'"',text)
  data=geometry(text);write(RES/'assets/jugcraft/newest_jugtania_models'/(model+'.json'),data)
  original=SOURCE/'Jugtania/vazkii/botania/client'/folder/(model+'.class')
  records.append({'source':str(original.relative_to(SOURCE)),'sha256':hashlib.sha256(original.read_bytes()).hexdigest(),'model':model,'parts':len(data['parts']),'sourceExecuted':False})
 write(ROOT/'art/owner-library/catalog/edited-2026-10-29/jugtania-model-recovery.json',records)
 print('Recovered original botanical geometry; no runtime validation:',len(records))
if __name__=='__main__':main()
