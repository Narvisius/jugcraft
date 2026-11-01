package io.github.jimbozoomer.jugcraft.client;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.wilds.WildCold;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

/** Original first-person arm tracks, sampled as data and applied around their source camera pivots. */
public final class WildFirstPerson {
    private record Clip(float length,boolean loop,float[][] right,float[][] left){}
    public record Frame(float[] values,boolean mirror,boolean hide,boolean forceArm,HumanoidArm arm){
        public void apply(PoseStack pose,float pitch){
            float[] v=values;float sign=mirror?-1:1;
            pose.rotateDegrees(Axis.XP,pitch*Math.clamp(v[9],0,1));
            pose.translate(-sign*v[3]/16,v[4]/16,v[5]/16);
            // Source first-person rig pivot after its (0,-2,-1) camera offset.
            double x=(arm==HumanoidArm.RIGHT?1:-1)*.546875,y=-.765625,z=-.625;
            pose.translate(x,y,z);pose.rotate(new Quaternionf().rotationZYX((float)Math.toRadians(sign*v[2]),(float)Math.toRadians(-sign*v[1]),(float)Math.toRadians(-v[0])));
            pose.scale(v[6],v[7],v[8]);pose.translate(-x,-y,-z);
        }
    }
    private static Map<String,Clip> clips;
    private static float[][] rows(JsonArray rows){float[][] out=new float[rows.size()][10];for(int i=0;i<out.length;i++)for(int j=0;j<10;j++)out[i][j]=rows.get(i).getAsJsonArray().get(j).getAsFloat();return out;}
    private static Map<String,Clip> clips(){
        if(clips!=null)return clips;var parsed=new HashMap<String,Clip>();
        try(var reader=Minecraft.getInstance().getResourceManager().getResourceOrThrow(Jugcraft.id("wilds_first_person.json")).openAsReader()){
            JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(entry->{var json=entry.getValue().getAsJsonObject();var arms=json.getAsJsonObject("arms");parsed.put(entry.getKey(),new Clip(json.get("length").getAsFloat(),json.get("loop").getAsBoolean(),rows(arms.getAsJsonArray("right")),rows(arms.getAsJsonArray("left"))));});
        }catch(java.io.IOException e){throw new IllegalStateException("Missing original wilds first-person tracks",e);}clips=Map.copyOf(parsed);return clips;
    }
    public static Frame frame(Player player,InteractionHand hand,ItemStack stack,float partial){
        if(player==null||!player.isAlive()||WildCold.frozen(player))return null;
        String name=WildPlayerAnimations.clip(player);if(name.isEmpty())return null;var clip=clips().get(name);if(clip==null)return null;
        boolean ice=name.startsWith("ice_"),axe=name.startsWith("axe_");
        var arm=hand==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();boolean mirror=player.getMainArm()==HumanoidArm.LEFT;
        if(axe&&hand!=InteractionHand.MAIN_HAND)return null;
        float age=WildPlayerAnimations.age(player,partial);if(clip.loop)age%=clip.length;
        var samples=(arm==HumanoidArm.RIGHT)^mirror?clip.right:clip.left;
        float at=Math.clamp(age*4,0,samples.length-1);int i=(int)at,j=Math.min(samples.length-1,i+1);float mix=at-i;float[] value=new float[10];
        for(int k=0;k<10;k++)value[k]=samples[i][k]+(samples[j][k]-samples[i][k])*mix;
        return new Frame(value,mirror,ice&&hand!=player.getUsedItemHand(),!ice&&!axe&&!name.equals("sunstrike")&&stack.isEmpty(),arm);
    }
}
