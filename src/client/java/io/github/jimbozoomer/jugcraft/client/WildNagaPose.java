package io.github.jimbozoomer.jugcraft.client;
import net.minecraft.client.model.geom.ModelPart;
/** Owner-authored Naga fold/webbing/jaw corrections, statically translated by tools/newest_wilds_pose.py. */
final class WildNagaPose {
    private final java.util.function.Function<String,ModelPart> lookup;
    WildNagaPose(ModelPart root){lookup=RecoveredWildlifeModel.recoveredLookup(root);}
    private ModelPart b(String name){return lookup.apply(name);}
    private static void scale(ModelPart p,float x,float y,float z){p.xScale=x;p.yScale=y;p.zScale=z;}
    void modelCorrections(){
        b("backWing_R").x -= 2.0f;
        b("backWing_L").x += 2.0f;
        scale(b("teethLower"),1.01f, 1.01f, 1.01f);
        
        scale(b("spike2joint"),0.99f, 1.0f, 1.0f);
        b("spike2joint").y = (float)((double)b("spike2joint").y + 0.28);
        b("spike2joint").z = (float)((double)b("spike2joint").z - 0.15);
        
        scale(b("spike3joint"),0.99f, 1.0f, 1.0f);
        b("spike3joint").y = (float)((double)b("spike3joint").y + 0.28);
        b("spike3joint").z = (float)((double)b("spike3joint").z - 0.15);
        scale(b("spike3Bottom"),1.0f, 1.8f, 1.0f);
        
        scale(b("spike4joint"),0.993f, 1.0f, 1.0f);
        b("spike4joint").y = (float)((double)b("spike4joint").y + 0.35);
        b("spike4joint").z = (float)((double)b("spike4joint").z + 0.7);
        scale(b("spike4"),1.0f, 1.0f, 1.0f);
        
        scale(b("spike5joint"),1.02f, 1.0f, 1.0f);
        b("spike5joint").y = (float)((double)b("spike5joint").y + 0.15);
        b("spike5joint").z = (float)((double)b("spike5joint").z - 0.42);
        scale(b("tail1"),1.03f, 1.0f, 1.0f);
        b("root").y += 22.0f;
        b("backFin1").x = (float)((double)b("backFin1").x + 0.001);
        b("backFin1Reversed").x = (float)((double)b("backFin1Reversed").x - 0.002);
        b("backFin2").x = (float)((double)b("backFin2").x + 5.0E-4);
        b("backFin2Reversed").x = (float)((double)b("backFin2Reversed").x - 0.001);
        b("handJoint_L").y = (float)((double)b("handJoint_L").y + 0.01);
        b("handJoint_R").y = (float)((double)b("handJoint_R").y + 0.01);
        b("backWing_LReversed").xRot=(float)Math.PI;b("backWing_LReversed").yRot=(float)Math.PI;
b("backWing_RReversed").xRot=(float)Math.PI;b("backWing_RReversed").yRot=(float)Math.PI;
b("tailFinReversed").xRot=(float)Math.PI;b("tailFinReversed").yRot=(float)Math.PI;
b("wingWebbing6_LReversed").xRot=(float)Math.PI;b("wingWebbing6_LReversed").yRot=(float)Math.PI;
b("wingWebbing5_LReversed").xRot=(float)Math.PI;b("wingWebbing5_LReversed").yRot=(float)Math.PI;
b("wingWebbing1_LReversed").xRot=(float)Math.PI;b("wingWebbing1_LReversed").yRot=(float)Math.PI;
b("wingWebbing2_LReversed").xRot=(float)Math.PI;b("wingWebbing2_LReversed").yRot=(float)Math.PI;
b("wingWebbing3_LReversed").xRot=(float)Math.PI;b("wingWebbing3_LReversed").yRot=(float)Math.PI;
b("wingWebbing4_LReversed").xRot=(float)Math.PI;b("wingWebbing4_LReversed").yRot=(float)Math.PI;
b("wingWebbing6_RReversed").xRot=(float)Math.PI;b("wingWebbing6_RReversed").yRot=(float)Math.PI;
b("wingWebbing5_RReversed").xRot=(float)Math.PI;b("wingWebbing5_RReversed").yRot=(float)Math.PI;
b("wingWebbing1_RReversed").xRot=(float)Math.PI;b("wingWebbing1_RReversed").yRot=(float)Math.PI;
b("wingWebbing2_RReversed").xRot=(float)Math.PI;b("wingWebbing2_RReversed").yRot=(float)Math.PI;
b("wingWebbing3_RReversed").xRot=(float)Math.PI;b("wingWebbing3_RReversed").yRot=(float)Math.PI;
b("wingWebbing4_RReversed").xRot=(float)Math.PI;b("wingWebbing4_RReversed").yRot=(float)Math.PI;
        b("backWing_LReversed").x = 30.0f;
        b("backWing_RReversed").x = -30.0f;
        b("tailFinReversed").x = 20.0f;
        b("wingWebbing1_LReversed").x = 53.0f;
        b("wingWebbing1_RReversed").x = -53.0f;
        b("wingWebbing2_LReversed").x = 50.0f;
        b("wingWebbing2_RReversed").x = -50.0f;
        b("wingWebbing3_LReversed").x = 43.0f;
        b("wingWebbing3_RReversed").x = -43.0f;
        b("wingWebbing4_LReversed").x = 31.0f;
        b("wingWebbing4_RReversed").x = -31.0f;
        b("wingWebbing5_LReversed").x = -4.0f;
        b("wingWebbing5_RReversed").x = 4.0f;
        b("wingWebbing6_LReversed").x = 4.0f;
        b("wingWebbing6_RReversed").x = -4.0f;
        b("backFin1Reversed").yRot = (float)Math.PI;
        b("backFin1Reversed").z = 20.0f;
    
    }
    void jawControls(){
        scale(b("teethUpper"),1.0f, 1.0f, 0.5f + 0.5f * Math.min(b("jaw").xRot, 1.0f));
        scale(b("underHead"),1.0f, 1.0f, 1.0f - 0.2f * Math.min(b("jaw").xRot, 1.0f));
        b("underHead").z = (float)((double)b("underHead").z + 1.2 * (double)Math.min(b("jaw").xRot, 1.0f));
    
    }
    void wingFoldControls(){
        b("shoulder1_R").yRot = (float)((double)b("shoulder1_R").yRot + 0.8 * (double)b("wingFolder").x);
        b("upperArm_R").yRot = (float)((double)b("upperArm_R").yRot + 0.3 * (double)b("wingFolder").x);
        b("lowerArm_R").yRot = (float)((double)b("lowerArm_R").yRot - 2.7 * (double)b("wingFolder").x);
        b("hand_R").yRot = (float)((double)b("hand_R").yRot + 1.8 * (double)b("wingFolder").x);
        b("wingFrame1_R").yRot = (float)((double)b("wingFrame1_R").yRot + 0.8 * (double)b("wingFolder").x);
        b("wingFrame2_R").yRot = (float)((double)b("wingFrame2_R").yRot + 0.3 * (double)b("wingFolder").x);
        b("wingFrame3_R").yRot = (float)((double)b("wingFrame3_R").yRot - 0.2 * (double)b("wingFolder").x);
        b("wingFrame4_R").yRot = (float)((double)b("wingFrame4_R").yRot - 0.7 * (double)b("wingFolder").x);
        b("shoulder1_L").yRot = (float)((double)b("shoulder1_L").yRot - 0.8 * (double)b("wingFolder").y);
        b("upperArm_L").yRot = (float)((double)b("upperArm_L").yRot - 0.3 * (double)b("wingFolder").y);
        b("lowerArm_L").yRot = (float)((double)b("lowerArm_L").yRot + 2.7 * (double)b("wingFolder").y);
        b("hand_L").yRot = (float)((double)b("hand_L").yRot - 1.8 * (double)b("wingFolder").y);
        b("wingFrame1_L").yRot = (float)((double)b("wingFrame1_L").yRot - 0.8 * (double)b("wingFolder").y);
        b("wingFrame2_L").yRot = (float)((double)b("wingFrame2_L").yRot - 0.3 * (double)b("wingFolder").y);
        b("wingFrame3_L").yRot = (float)((double)b("wingFrame3_L").yRot + 0.2 * (double)b("wingFolder").y);
        b("wingFrame4_L").yRot = (float)((double)b("wingFrame4_L").yRot + 0.7 * (double)b("wingFolder").y);
    
    }
    void computeWingWebbing(){
        
        
        
        
        
        
        
        
        
        
        
        
        float webbing1LScale = (float)((double)(b("wingFrame1_L").yRot - b("wingFrame2_L").yRot) / Math.toRadians(35.0));
        webbing1LScale = 0.8f * webbing1LScale + 0.2f;
        float webbing1LRot = (float)(-((double)b("wingFrame1_L").yRot - ((double)b("wingFrame2_L").yRot + Math.toRadians(35.0))) / 2.0);
        b("wingWebbing1_L").yRot += webbing1LRot;
        scale(b("wingWebbing1_L"),1.0f - webbing1LScale * 0.07f, 1.0f, webbing1LScale);
        float webbing2LScale = (float)((double)(b("wingFrame2_L").yRot - b("wingFrame3_L").yRot) / Math.toRadians(35.0));
        webbing2LScale = 0.8f * webbing2LScale + 0.2f;
        float webbing2LRot = (float)(-((double)b("wingFrame2_L").yRot - ((double)b("wingFrame3_L").yRot + Math.toRadians(35.0))) / 2.0);
        b("wingWebbing2_L").yRot += webbing2LRot;
        scale(b("wingWebbing2_L"),1.0f - webbing2LScale * 0.07f, 1.0f, webbing2LScale);
        float webbing3LScale = (float)((double)(b("wingFrame3_L").yRot - b("wingFrame4_L").yRot) / Math.toRadians(35.0));
        webbing3LScale = 0.8f * webbing3LScale + 0.2f;
        float webbing3LRot = (float)(-((double)b("wingFrame3_L").yRot - ((double)b("wingFrame4_L").yRot + Math.toRadians(35.0))) / 2.0);
        b("wingWebbing3_L").yRot += webbing3LRot;
        scale(b("wingWebbing3_L"),1.0f - webbing3LScale * 0.07f, 1.0f, webbing3LScale);
        float webbing4LScale = 1.0f - (float)(-0.5 * (double)b("hand_L").yRot);
        float webbing4LRot = (float)(-((double)(b("wingFrame4_L").yRot + b("hand_L").yRot) + Math.toRadians(105.0)) / 2.0);
        b("wingWebbing4_L").yRot += webbing4LRot;
        scale(b("wingWebbing4_L"),1.0f - webbing4LScale * 0.02f, 1.0f, webbing4LScale);
        b("wingWebbing5_L").x += 12.0f * (float)((double)b("upperArm_L").yRot / Math.toRadians(35.0));
        scale(b("wingWebbing5_L"),(float)(2.0 - (double)(-b("shoulder1_L").yRot + b("shoulder1_L").getInitialPose().yRot()) / 1.5), 1.0f, (float)(0.915 - (double)(-b("shoulder1_L").yRot + b("shoulder1_L").getInitialPose().yRot()) / 1.2));
        scale(b("wingWebbing6_L"),1.0f + (-b("shoulder1_L").yRot + b("shoulder1_L").getInitialPose().yRot()) / 4.0f, 1.0f, (float)(1.0 - (double)(-b("shoulder1_L").yRot + b("shoulder1_L").getInitialPose().yRot()) / 1.6));
        b("wingWebbing6_L").x += 14.0f * (-b("shoulder1_L").yRot + b("shoulder1_L").getInitialPose().yRot());
        float webbing1RScale = (float)((double)(-(b("wingFrame1_R").yRot - b("wingFrame2_R").yRot)) / Math.toRadians(35.0));
        webbing1RScale = 0.8f * webbing1RScale + 0.2f;
        float webbing1RRot = (float)(-((double)b("wingFrame1_R").yRot - ((double)b("wingFrame2_R").yRot - Math.toRadians(35.0))) / 2.0);
        b("wingWebbing1_R").yRot += webbing1RRot;
        scale(b("wingWebbing1_R"),1.0f - webbing1RScale * 0.07f, 1.0f, webbing1RScale);
        float webbing2RScale = (float)((double)(-(b("wingFrame2_R").yRot - b("wingFrame3_R").yRot)) / Math.toRadians(35.0));
        webbing2RScale = 0.8f * webbing2RScale + 0.2f;
        float webbing2RRot = (float)(-((double)b("wingFrame2_R").yRot - ((double)b("wingFrame3_R").yRot - Math.toRadians(35.0))) / 2.0);
        b("wingWebbing2_R").yRot += webbing2RRot;
        scale(b("wingWebbing2_R"),1.0f - webbing2RScale * 0.07f, 1.0f, webbing2RScale);
        float webbing3RScale = (float)((double)(-(b("wingFrame3_R").yRot - b("wingFrame4_R").yRot)) / Math.toRadians(35.0));
        webbing3RScale = 0.8f * webbing3RScale + 0.2f;
        float webbing3RRot = (float)(-((double)b("wingFrame3_R").yRot - ((double)b("wingFrame4_R").yRot - Math.toRadians(35.0))) / 2.0);
        b("wingWebbing3_R").yRot += webbing3RRot;
        scale(b("wingWebbing3_R"),1.0f - webbing3RScale * 0.07f, 1.0f, webbing3RScale);
        float webbing4RScale = 1.0f - (float)(0.5 * (double)b("hand_R").yRot);
        float webbing4RRot = (float)(-((double)(b("wingFrame4_R").yRot + b("hand_R").yRot) - Math.toRadians(105.0)) / 2.0);
        b("wingWebbing4_R").yRot += webbing4RRot;
        scale(b("wingWebbing4_R"),1.0f - webbing4RScale * 0.02f, 1.0f, webbing4RScale);
        b("wingWebbing5_R").x -= 12.0f * (float)((double)(-b("upperArm_R").yRot) / Math.toRadians(35.0));
        scale(b("wingWebbing5_R"),(float)(2.0 - (double)(b("shoulder1_R").yRot - b("shoulder1_R").getInitialPose().yRot()) / 1.5), 1.0f, (float)(0.915 - (double)(b("shoulder1_R").yRot - b("shoulder1_R").getInitialPose().yRot()) / 1.2));
        scale(b("wingWebbing6_R"),1.0f + (b("shoulder1_R").yRot - b("shoulder1_R").getInitialPose().yRot()) / 4.0f, 1.0f, (float)(1.0 - (double)(b("shoulder1_R").yRot - b("shoulder1_R").getInitialPose().yRot()) / 1.6));
        b("wingWebbing6_R").x += 14.0f * (b("shoulder1_L").yRot - b("shoulder1_L").getInitialPose().yRot());
        b("wingWebbing4_R").y -= 0.01f;
        b("wingWebbing4_L").y -= 0.01f;
        b("wingWebbing5_R").y += 0.01f;
        b("wingWebbing5_L").y += 0.01f;
        b("wingWebbing3_R").y += 0.01f;
        b("wingWebbing3_L").y += 0.01f;
        b("tailFin").y -= 0.1f;
        if (b("tail1") != null) {
            scale(b("backFin1"),1.0f, 1.0f, 1.0f - 0.5f * b("tail1").xRot);
            
            b("backFin1").x = (float)((double)b("backFin1").x + 0.002);
            b("backFin1Reversed").x = (float)((double)b("backFin1Reversed").x - 0.004);
            scale(b("backFin2"),1.0f, 1.0f, 1.0f - 0.2f * b("tail2").xRot);
            
            b("backFin2").x = (float)((double)b("backFin2").x + 0.001);
            b("backFin2Reversed").x = (float)((double)b("backFin2Reversed").x - 0.002);
        }
    
    }
}
