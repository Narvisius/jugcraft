"""Recover the supplied armor cuboids/UVs; never load or execute library classes."""
import re, struct, hashlib
from pathlib import Path
from recover_wildlife_models import geometry
from newest_building import SOURCE, RES, write

ROOT = Path(__file__).resolve().parents[1]

def main():
    records = []
    for family, model in [('manasteel','ManasteelArmorModel'),('elementium','ElementiumArmorModel'),('terrasteel','TerrasteelArmorModel'),('manaweave','ManaweaveRobeModel')]:
        text = (ROOT/'build/owner-oct29-reference/vazkii/botania/client/model/armor'/f'{model}.java').read_text(encoding='utf-8')
        png = SOURCE/'Jugtania/assets/botania/textures/model'/f'armor_{family}.png'
        width,height = struct.unpack('>II',png.read_bytes()[16:24])
        for side in ['Inside','Outside']:
            start = text.index('{',text.index(f'create{side}Mesh()'))+1
            end = text.index('return mesh;',start)+len('return mesh;')
            body = text[start:end]
            body = re.sub(r'CubeDeformation deformation = new CubeDeformation\(0.01f\);','',body)
            body = re.sub(r'\bdeformation\b','new CubeDeformation(0.01f)',body)
            for old,new in {'m_171576_':'getRoot','m_171599_':'addOrReplaceChild','m_171558_':'create','m_171514_':'texOffs','m_171488_':'addBox','m_171481_':'addBox','m_171480_':'mirror','m_171419_':'offset','m_171423_':'offsetAndRotation'}.items(): body=body.replace(old,new)
            body=body.replace('PartPose.f_171404_','PartPose.offset(0,0,0)').replace('PartPose.m_171430_(','PartPose.offsetAndRotation(0,0,0,')
            body=re.sub(r'(?m)^(\s*)(\w+)\.addOrReplaceChild\("([^"\n]+)"',lambda m:m[1]+'PartDefinition recovered_'+m[3]+' = '+m[2]+'.addOrReplaceChild("'+m[3]+'"',body)
            body=body.replace('return mesh;',f'return LayerDefinition.create((MeshDefinition)mesh, (int){width}, (int){height});')
            data=geometry('public static LayerDefinition createBodyLayer() {'+body+'}')
            write(RES/'assets/jugcraft/newest_jugtania_models'/f'{family}_{side.lower()}.json',data)
        original=SOURCE/'Jugtania/vazkii/botania/client/model/armor'/f'{model}.class'
        records.append({'source':str(original.relative_to(SOURCE)),'sha256':hashlib.sha256(original.read_bytes()).hexdigest(),'family':family,'layers':['inside','outside'],'sourceExecuted':False})
    write(ROOT/'art/owner-library/catalog/edited-2026-10-29/jugtania-armor-model-recovery.json',records)
    print('Recovered eight original armor layers; no runtime validation.')

if __name__=='__main__':main()
