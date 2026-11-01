"""Adapt recovered owner model and keyframe constants; never execute source-library code."""
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[1]
REF=ROOT/'build/owner-oct29-reference/com/stal111/forbidden_arcanus/client'
OUT=ROOT/'src/client/java/io/github/jimbozoomer/jugcraft/client/ArcaneJarSoulModel.java'
def main():
 model=(REF/'model/UtremJarSoulsModel.java').read_text()
 body=model.split('public static LayerDefinition createBodyLayer() {',1)[1].split('\n    public void setupAnim',1)[0]
 animation=(REF/'animation/UtremJarSoulAnimation.java').read_text().split('public static final AnimationDefinition ROTATE = ',1)[1].rsplit('}',1)[0].strip()
 # Geometry, UVs, named hierarchy and every keyframe remain the supplied data.
 source='''package io.github.jimbozoomer.jugcraft.client;
import net.minecraft.client.animation.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
/** Owner UtremJarSoulsModel geometry and UtremJarSoulAnimation keyframes, adapted to native rendering. */
final class ArcaneJarSoulModel {
    static final AnimationDefinition ROTATE = '''+animation+'''
    static LayerDefinition createBodyLayer() {'''+body+'\n}\n'
 source=re.sub(r'\((?:float|double|int|MeshDefinition)\)', '', source)
 OUT.write_text(source,encoding='utf-8')
 model=(REF/'model/QuantumInjectorModel.java').read_text()
 body=model.split('public static LayerDefinition createBodyLayer() {',1)[1].split('\n    }',1)[0]
 animation=(REF/'animation/QuantumInjectorAnimation.java').read_text()
 definitions='\n'.join('    '+line.strip().replace('public static final','static final') for line in animation.splitlines() if 'public static final AnimationDefinition ' in line)
 source='''package io.github.jimbozoomer.jugcraft.client;
import net.minecraft.client.animation.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
/** Original QuantumInjector geometry, UVs and complete transform/rotation keyframes. */
final class ArcaneInjectorModel {
'''+definitions+'\n    static LayerDefinition createBodyLayer() {'+body+'\n    }\n}\n'
 source=re.sub(r'\((?:float|double|int|MeshDefinition)\)', '', source)
 (OUT.parent/'ArcaneInjectorModel.java').write_text(source,encoding='utf-8')
 model=(REF/'model/LostSoulModel.java').read_text()
 body=model.split('public static LayerDefinition createBodyLayer() {',1)[1].split('\n    }',1)[0]
 animation=(REF/'animation/LostSoulAnimation.java').read_text()
 definitions='\n'.join('    '+line.strip().replace('public static final','static final') for line in animation.splitlines() if 'public static final AnimationDefinition ' in line)
 source='''package io.github.jimbozoomer.jugcraft.client;
import net.minecraft.client.animation.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
/** Original LostSoulModel geometry and complete idle/fear keyframes. */
final class ArcaneLostSoulModel {
'''+definitions+'\n    static LayerDefinition createBodyLayer() {'+body+'\n    }\n}\n'
 source=re.sub(r'\((?:float|double|int|MeshDefinition)\)', '', source)
 (OUT.parent/'ArcaneLostSoulModel.java').write_text(source,encoding='utf-8')
def trader_models():
 for source_name,target,method,animation_name in [('DarkTraderModel','ArcaneDarkTraderModel','createBodyLayer','DarkTraderAnimation'),('QuantumLightDoorModel','ArcaneLightDoorModel','createLayer','QuantumLightDoorAnimation')]:
  model=(REF/f'model/{source_name}.java').read_text()
  body=model.split(f'public static LayerDefinition {method}() {{',1)[1].split('\n    }',1)[0]
  animation=(REF/f'animation/{animation_name}.java').read_text()
  definitions='\n'.join('    '+line.strip().replace('public static final','static final') for line in animation.splitlines() if 'public static final AnimationDefinition ' in line)
  source='package io.github.jimbozoomer.jugcraft.client;\nimport net.minecraft.client.animation.*;\nimport net.minecraft.client.model.geom.*;\nimport net.minecraft.client.model.geom.builders.*;\n/** Original supplied '+source_name+' geometry, UVs and complete animation keyframes. */\nfinal class '+target+' {\n'+definitions+'\n    static LayerDefinition createBodyLayer() {'+body+'\n    }\n}\n'
  source=re.sub(r'\((?:float|double|int|MeshDefinition)\)', '', source)
  (OUT.parent/f'{target}.java').write_text(source,encoding='utf-8')
if __name__=='__main__':
 main()
 trader_models()
