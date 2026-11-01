"""Translate owner Naga scalar pose controls into native model-part operations.

Static text conversion only; never load or execute the supplied classes.
"""
from pathlib import Path
import re
from recover_creature_models import body
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'build/owner-oct29-reference/com/bobmowzie/mowziesmobs/client/model/entity/ModelNaga.java'

def main():
 src=SOURCE.read_text()
 planes=re.search(r'this.reversePlanes = new AdvancedModelRenderer\[\]\{([^}]+)\}',src)[1]
 planes=re.findall(r'this\.(\w+)',planes)
 methods=[]
 for name in ['modelCorrections','jawControls','wingFoldControls','computeWingWebbing']:
  text=body(src,r'private void '+name+r'\(')
  text=re.sub(r'for \(AdvancedModelRenderer m : this.reversePlanes\) \{.*?\}', '\n'.join('this.'+p+'.rotateAngleX=(float)Math.PI;this.'+p+'.rotateAngleY=(float)Math.PI;' for p in planes),text,flags=re.S)
  text=re.sub(r'this\.\w+\.scaleChildren = true;', '',text)
  text=re.sub(r'this.tailDynamic\[(\d+)\]',lambda m:'this.tail'+str(int(m[1])+1),text)
  text=re.sub(r'this\.(\w+)\.setScale\(([^;]+)\);',lambda m:'scale(b("'+m[1]+'"),'+m[2]+');',text)
  text=re.sub(r'this\.(\w+)',lambda m:'b("'+m[1]+'")',text)
  for old,new in [('rotateAngleX','xRot'),('rotateAngleY','yRot'),('rotateAngleZ','zRot'),('rotationPointX','x'),('rotationPointY','y'),('rotationPointZ','z'),('defaultRotationY','getInitialPose().yRot()')]:text=text.replace('.'+old,'.'+new)
  methods.append('    void '+name+'(){'+text+'\n    }')
 out='''package io.github.jimbozoomer.jugcraft.client;
import net.minecraft.client.model.geom.ModelPart;
/** Owner-authored Naga fold/webbing/jaw corrections, statically translated by tools/newest_wilds_pose.py. */
final class WildNagaPose {
    private final java.util.function.Function<String,ModelPart> lookup;
    WildNagaPose(ModelPart root){lookup=RecoveredWildlifeModel.recoveredLookup(root);}
    private ModelPart b(String name){return lookup.apply(name);}
    private static void scale(ModelPart p,float x,float y,float z){p.xScale=x;p.yScale=y;p.zScale=z;}
'''+ '\n'.join(methods)+'\n}\n'
 (ROOT/'src/client/java/io/github/jimbozoomer/jugcraft/client/WildNagaPose.java').write_text(out)
def wrought():
 src=(SOURCE.parent/'ModelWroughtnaut.java').read_text()
 text=body(src,r'public void setDefaultAngles\(')
 text=text.replace('this.resetToDefaultPose();','')
 text=text.replace('entity.isActive()','active').replace('entity.getAnimation()','action')
 for old,new in [('ACTIVATE_ANIMATION','6'),('DEACTIVATE_ANIMATION','7'),('DIE_ANIMATION','9')]:text=text.replace('EntityWroughtnaut.'+old,new)
 text=text.replace('(float)entity.frame + delta','frame')
 text=re.sub(r'float frame = frame;','',text)
 text=re.sub(r'float walkTimerInterp = [^;]+;','float walkTimerInterp=walkAmount*10;',text)
 text=text.replace('Mth.m_14177_','Mth.wrapDegrees').replace('Mth.m_14036_','Mth.clamp').replace('Mth.m_14031_','Mth.sin')
 for method in ['walk','swing','flap','bob']:text=text.replace('this.'+method+'(',method+'(')
 text=re.sub(r'this\.(\w+)\.setScale\(([^;]+)\);',lambda m:'scale(b("'+m[1]+'"),'+m[2]+');',text)
 text=re.sub(r'this\.(\w+)',lambda m:'b("'+m[1]+'")',text)
 for old,new in [('rotateAngleX','xRot'),('rotateAngleY','yRot'),('rotateAngleZ','zRot'),('rotationPointX','x'),('rotationPointY','y'),('rotationPointZ','z'),('showModel','visible')]:text=text.replace('.'+old,'.'+new)
 out='''package io.github.jimbozoomer.jugcraft.client;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
/** Original Wroughtnaut resting/active pose and authored gait, statically adapted from supplied data. */
final class WildWroughtPose {
 private final java.util.function.Function<String,ModelPart> lookup;
 WildWroughtPose(ModelPart root){lookup=RecoveredWildlifeModel.recoveredLookup(root);}
 private ModelPart b(String name){return lookup.apply(name);}
 private void scale(ModelPart p,float n){p.xScale=p.yScale=p.zScale=n;}
 private float wave(float speed,float degree,boolean invert,float offset,float weight,float time,float amount){return (float)(Math.cos(time*speed+offset)*degree*amount+weight*amount)*(invert?-1:1);}
 private void walk(ModelPart p,float s,float d,boolean i,float o,float w,float t,float a){p.xRot+=wave(s,d,i,o,w,t,a);}
 private void swing(ModelPart p,float s,float d,boolean i,float o,float w,float t,float a){p.yRot+=wave(s,d,i,o,w,t,a);}
 private void flap(ModelPart p,float s,float d,boolean i,float o,float w,float t,float a){p.zRot+=wave(s,d,i,o,w,t,a);}
 private void bob(ModelPart p,float s,float d,boolean bounce,float t,float a){float n=(float)Math.sin(t*s)*a*d;p.y+=bounce?-Math.abs(n):n-a*d;}
 void apply(boolean active,int action,float frame,float walkAmount,float headYaw,float headPitch){float limbSwing=0,limbSwingAmount=0;'''+text+'\n }\n}\n'
 (ROOT/'src/client/java/io/github/jimbozoomer/jugcraft/client/WildWroughtPose.java').write_text(out)
def frost():
 src=(SOURCE.parent/'ModelFrostmaw.java').read_text()
 method=body(src,r'protected void animate\(')
 text=method[method.index('float globalSpeed = 0.5f;'):]
 text=re.sub(r'FrozenCapability\.IFrozenCapability frozenCapability = [^;]+;','',text)
 text=re.sub(r'boolean bl = frozen = [^;]+;', 'frozen = false;',text)
 text=text.replace('entity.getActive()','active').replace('entity.active','active').replace('entity.getHasCrystal()','hasCrystal').replace('entity.getAnimationTick()','actionTicks').replace('entity.getAnimation()','action')
 for old,new in [('ROAR_ANIMATION',3),('ACTIVATE_ANIMATION',6),('ACTIVATE_NO_CRYSTAL_ANIMATION',7),('DEACTIVATE_ANIMATION',8),('SLAM_ANIMATION',10),('DIE_ANIMATION',12)]:text=text.replace('EntityFrostmaw.'+old,str(new))
 text=text.replace('this.chest.scaleChildren = false;','')
 for method_name in ['walk','swing','flap','bob','faceTarget']:text=text.replace('this.'+method_name+'(',method_name+'(')
 text=re.sub(r'this\.(\w+)\.setScale\(([^;]+)\);',lambda m:'scale(b("'+m[1]+'"),'+m[2]+');',text)
 text=re.sub(r'this\.(\w+)',lambda m:'b("'+m[1]+'")',text)
 for old,new in [('rotateAngleX','xRot'),('rotateAngleY','yRot'),('rotateAngleZ','zRot'),('rotationPointX','x'),('rotationPointY','y'),('rotationPointZ','z'),('defaultRotationX','getInitialPose().xRot()'),('showModel','visible')]:text=text.replace('.'+old,'.'+new)
 # Native ModelPart always propagates scale; counter the source's scaleChildren=false.
 import json
 data=json.loads((ROOT/'src/main/resources/assets/jugcraft/recovered_creatures/wilds_modelfrostmaw.json').read_text())
 compensation=''.join('b("'+part['name']+'").xScale/=b("chest").xScale;b("'+part['name']+'").yScale/=b("chest").yScale;b("'+part['name']+'").zScale/=b("chest").zScale;' for part in data['parts'] if part['parent']=='chest')
 base=(ROOT/'src/client/java/io/github/jimbozoomer/jugcraft/client/WildWroughtPose.java').read_text().split(' void apply(')[0].replace('WildWroughtPose','WildFrostPose').replace('Original Wroughtnaut resting/active pose and authored gait','Original Frostmaw sleeping pose, quadruped/biped gait and hand/crystal controls')
 base+=' void scale(ModelPart p,float x,float y,float z){p.xScale=x;p.yScale=y;p.zScale=z;}\n void faceTarget(float yaw,float pitch,float divisor,ModelPart p){p.yRot+=yaw/(57.295776F*divisor);p.xRot+=pitch/(57.295776F*divisor);}'
 out=base+'\n void apply(boolean active,boolean hasCrystal,int action,float actionTicks,float frame,float limbSwing,float limbSwingAmount,float headYaw,float headPitch){'+text+compensation+'\n }\n}\n'
 (ROOT/'src/client/java/io/github/jimbozoomer/jugcraft/client/WildFrostPose.java').write_text(out)
if __name__=='__main__':main();wrought();frost()
