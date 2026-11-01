package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerSupplementFlute;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerBubbles;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerFieldTools;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;

/** Shared arm ownership for actively used flute, blower and slingshot; ordinary equipment poses remain untouched. */
public final class OwnerFlutePose {
    private record Pose(boolean left,float time,int kind,float charge){}
    private static final RenderStateDataKey<Pose> DATA=RenderStateDataKey.create(()->"jugcraft:flute_pose");
    private OwnerFlutePose(){}
    public static void extract(LivingEntity user,ArmedEntityRenderState state,float partial){
        var item=user.getUseItem().getItem();int kind=item instanceof OwnerSupplementFlute?0:item instanceof OwnerBubbles.Blower?1:item instanceof OwnerFieldTools.Slingshot?2:-1;
        state.setData(DATA,user.isUsingItem()&&kind>=0&&!user.isSleeping()&&!user.isVisuallySwimming()&&!user.isFallFlying()
                ?new Pose((user.getMainArm()==HumanoidArm.LEFT)^(user.getUsedItemHand()==InteractionHand.OFF_HAND),user.tickCount+partial,kind,Math.clamp((user.getTicksUsingItem()+partial)/(kind==2?(float)OwnerFieldTools.Slingshot.chargeTicks(user.getUseItem(),user):20F),0,1)):null);
    }
    public static void apply(HumanoidModel<?> model,HumanoidRenderState state){
        var data=state.getData(DATA);if(data==null)return;
        var main=data.left()?model.leftArm:model.rightArm;var off=data.left()?model.rightArm:model.leftArm;
        if(data.kind()==1){float sign=data.left()?-1:1;main.xRot=off.xRot=model.head.xRot-1.45F;main.yRot=model.head.yRot-.3F*sign;off.yRot=model.head.yRot+.55F*sign;main.zRot=-.08F*sign;off.zRot=.08F*sign;return;}
        if(data.kind()==2){float sign=data.left()?-1:1;main.xRot=model.head.xRot-1.5708F;main.yRot=model.head.yRot-.12F*sign;main.zRot=0;off.xRot=model.head.xRot-1.5708F;off.yRot=model.head.yRot+(.45F+.3F*data.charge())*sign;off.zRot=.1F*sign;return;}
        float sign=data.left()?-1:1,pitch=Math.clamp(model.head.xRot,-1.2566371F,1.5707964F),down=Math.clamp(pitch,0,.8F);
        float angle=pitch-(state.isCrouching?1:0)-.3F+down*.5F;
        var vector=new net.minecraft.world.phys.Vec3(0,0,1).yRot(-.99F*sign).xRot(angle);
        main.yRot=(float)Math.atan2(-vector.x,vector.z)+model.head.yRot*1.4F-.1F*sign-.5F*down*sign;
        main.xRot=(float)Math.asin(vector.y/vector.length())-(float)Math.PI/2;
        off.yRot=Math.clamp((main.yRot-sign)*.2F,-.15F,.15F)+1.1F*sign;
        off.xRot=main.xRot-.06F;
        float bob=Mth.sin(data.time()*1.3F)*.005F;main.zRot=bob;off.zRot=-bob;
    }
    public static void firstPerson(PoseStack pose,HumanoidArm arm){
        var player=Minecraft.getInstance().player;if(player==null||!player.isUsingItem())return;var item=player.getUseItem().getItem();if(!(item instanceof OwnerSupplementFlute)&&!(item instanceof OwnerBubbles.Blower)&&!(item instanceof OwnerFieldTools.Slingshot))return;
        var used=player.getUsedItemHand()==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();if(used!=arm)return;
        int mirror=arm==HumanoidArm.RIGHT?1:-1;
        if(item instanceof OwnerBubbles.Blower){pose.translate(-.32*mirror,.18,-.1);pose.rotateDegrees(Axis.YP,-15*mirror);return;}
        if(item instanceof OwnerFieldTools.Slingshot){float charge=Math.clamp(player.getTicksUsingItem()/(float)OwnerFieldTools.Slingshot.chargeTicks(player.getUseItem(),player),0,1);pose.translate(-.18*mirror,.05,-.12*charge);pose.rotateDegrees(Axis.YP,-8*mirror);return;}
        pose.translate(-.4*mirror,.2+Math.sin(player.tickCount*1.3)*.0038,0);
        pose.rotateDegrees(Axis.ZP,-90);pose.scale(mirror,-mirror,-1);
    }
}
