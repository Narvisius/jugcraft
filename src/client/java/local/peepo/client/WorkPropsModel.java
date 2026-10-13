package local.peepo.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Small closed-cube tools, with separate existing wood/iron textures and no item entities. */
final class WorkPropsModel extends EntityModel<PeepoState> {
    private final ModelPart valve,lever,mallet,wrench,valveMount,leverMount;
    WorkPropsModel(ModelPart root){
        super(root);
        valve=root.getChild("valve");lever=root.getChild("lever");
        mallet=root.getChild("mallet");wrench=root.getChild("wrench");
        valveMount=root.getChild("valve_mount");leverMount=root.getChild("lever_mount");
    }
    @Override public void setupAnim(PeepoState state){
        super.setupAnim(state);
        valve.visible=lever.visible=mallet.visible=wrench.visible=valveMount.visible=leverMount.visible=false;
        ModelPart active=switch(state.work){
            case VALVE -> valve;
            case LEVER -> lever;
            case MALLET -> mallet;
            case WRENCH -> wrench;
            default -> null;
        };
        if(active==null)return;
        var p=state.toolPose;
        active.visible=true;active.x=p.x;active.y=p.y;active.z=p.z;active.xRot=p.xRot;active.zRot=p.zRot;
        ModelPart mount=state.work==local.peepo.WorkAnimation.VALVE?valveMount:
            state.work==local.peepo.WorkAnimation.LEVER?leverMount:null;
        if(mount!=null){mount.visible=true;mount.x=p.x;mount.y=p.y;mount.z=p.z;}
    }
    static LayerDefinition create(boolean metal){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        var valve=root.addOrReplaceChild("valve",CubeListBuilder.create(),PartPose.ZERO);
        var lever=root.addOrReplaceChild("lever",CubeListBuilder.create(),PartPose.ZERO);
        var mallet=root.addOrReplaceChild("mallet",CubeListBuilder.create(),PartPose.ZERO);
        var wrench=root.addOrReplaceChild("wrench",CubeListBuilder.create(),PartPose.ZERO);
        var valveMount=root.addOrReplaceChild("valve_mount",CubeListBuilder.create(),PartPose.ZERO);
        var leverMount=root.addOrReplaceChild("lever_mount",CubeListBuilder.create(),PartPose.ZERO);
        if(metal){
            // Large octagonal rim; the fixed hub and downpipe belong to the stationary mount.
            for(int i=0;i<8;i++){
                float angle=i*Mth.PI/4;
                valve.addOrReplaceChild("rim_"+i,box(-.25F,-1.11F,-.25F,.5F,2.22F,.5F),
                    PartPose.offsetAndRotation(Mth.cos(angle)*MachineWorkClip.VALVE_RADIUS,
                        Mth.sin(angle)*MachineWorkClip.VALVE_RADIUS,0,0,0,angle));
            }
            valve.addOrReplaceChild("spokes",box(-2.5F,-.16F,-.18F,5,.32F,.36F)
                .texOffs(0,0).addBox(-.16F,-2.5F,-.18F,.32F,5,.36F),PartPose.ZERO);
            valve.addOrReplaceChild("hub",box(-.42F,-.42F,-.28F,.84F,.84F,.56F),PartPose.ZERO);
            valveMount.addOrReplaceChild("shaft",box(-.25F,-.25F,-.95F,.5F,.5F,1.05F),PartPose.ZERO);
            float ground=24-MachineWorkClip.VALVE_Y;
            valveMount.addOrReplaceChild("downpipe",box(-.30F,-.3F,-1.05F,.6F,ground+.08F,.6F),PartPose.ZERO);
            valveMount.addOrReplaceChild("collar",box(-.42F,.20F,-1.17F,.84F,.42F,.84F),PartPose.ZERO);
            valveMount.addOrReplaceChild("foot_flange",box(-.65F,ground-.22F,-1.4F,1.3F,.22F,1.3F),PartPose.ZERO);
            lever.addOrReplaceChild("shaft",box(-.28F,MachineWorkClip.LEVER_GRIP,-.28F,.56F,3.7F,.56F),PartPose.ZERO);
            leverMount.addOrReplaceChild("pivot",box(-.8F,-.4F,-.5F,1.6F,.8F,1),PartPose.ZERO);
            leverMount.addOrReplaceChild("bracket",box(-.6F,-.25F,-1.5F,1.2F,.5F,1.25F),PartPose.ZERO);
            // Match the wooden head's quarter-turn around the handle, including its metal strap.
            mallet.addOrReplaceChild("band",box(-.35F,-.69F,-.69F,.7F,1.38F,1.38F),
                PartPose.offsetAndRotation(0,-3.05F,0,0,Mth.PI/2,0));
            wrench.addOrReplaceChild("handle",box(-.36F,-.05F,-.27F,.72F,3.5F,.54F),PartPose.ZERO);
            wrench.addOrReplaceChild("jaw_base",box(-1.05F,-.5F,-.32F,2.1F,.7F,.64F),PartPose.ZERO);
            wrench.addOrReplaceChild("jaw_left",box(-1.05F,-1.5F,-.32F,.52F,1,.64F),PartPose.ZERO);
            wrench.addOrReplaceChild("jaw_right",box(.53F,-1.5F,-.32F,.52F,1,.64F),PartPose.ZERO);
        }else{
            lever.addOrReplaceChild("grip",box(-1.05F,MachineWorkClip.LEVER_GRIP-.32F,-.35F,2.1F,.64F,.7F),PartPose.ZERO);
            mallet.addOrReplaceChild("handle",box(-.30F,-3.1F,-.30F,.6F,3.5F,.6F),PartPose.ZERO);
            mallet.addOrReplaceChild("head",box(-1.65F,-.65F,-.65F,3.3F,1.3F,1.3F),
                PartPose.offsetAndRotation(0,-3.05F,0,0,Mth.PI/2,0));
        }
        return LayerDefinition.create(mesh,16,16);
    }
    private static CubeListBuilder box(float x,float y,float z,float dx,float dy,float dz){
        return CubeListBuilder.create().texOffs(0,0).addBox(x,y,z,dx,dy,dz);
    }
}
