"""Statically translate owner-authored Elokosa gait equations; never execute supplied code."""
from pathlib import Path
import re
from recover_creature_models import body
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'build/owner-oct29-reference/com/bobmowzie/mowziesmobs/client/model/entity/ModelElokosa.java'
def main():
 src=SOURCE.read_text()
 methods=[]
 for name in ['runAnim','walkForwardAnim','walkBackwardAnim','walkLeftAnim','walkRightAnim','walkAnimDay','runAnimDay']:
  text=body(src,r'private void '+name+r'\(').replace('MowzieGeoBone','Bone')
  methods.append('    private void '+name+'(double blend,float limbSwing,float limbSwingAmount,float speed){'+text+'\n    }')
 wrapper='''package io.github.jimbozoomer.jugcraft.client;
import com.geckolib.renderer.base.BoneSnapshots;
/** Owner gait coefficients translated to immutable-per-render Gecko snapshots. */
final class WildElokosaPose {
    private final BoneSnapshots snapshots;
    WildElokosaPose(BoneSnapshots snapshots){this.snapshots=snapshots;}
    private Bone getMowzieBone(String name){return new Bone(name);}
    private double poweredWave(double x,double speed,double offset,double power){return Math.pow((Math.cos(x*speed+offset)+1)/2,power);}
    void apply(float swing,float amount,double forward,double backward,double left,double right,double run,double blend){
        amount=Math.min(.7F,amount*2);float speed=.65F;
        walkForwardAnim(forward*blend*(1-run),swing,amount,speed);walkBackwardAnim(backward*blend*(1-run),swing,amount,speed);
        walkLeftAnim(left*blend*(1-run),swing,amount,speed);walkRightAnim(right*blend*(1-run),swing,amount,speed);
        runAnim(blend*run,swing,amount,speed);walkAnimDay(blend*(1-run),swing,amount,speed);runAnimDay(blend*run,swing,amount,speed);
    }
    private final class Bone {
        final String name;Bone(String name){this.name=name;}
        void setHidden(boolean value){snapshots.ifPresent(name,b->b.skipRender(value));}
'''
 for kind,field in [('Rot','Rot'),('Pos','Translate')]:
  for axis in 'XYZ':wrapper+=f'        void add{kind}{axis}(double v){{snapshots.ifPresent(name,b->b.set{field}{axis}(b.get{field}{axis}()+(float)v));}}\n'
 wrapper+='    }\n'+'\n'.join(methods)+'\n}\n'
 (ROOT/'src/client/java/io/github/jimbozoomer/jugcraft/client/WildElokosaPose.java').write_text(wrapper)
if __name__=='__main__':main()
