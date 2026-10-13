package local.peepo.client;
import local.peepo.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.*;

public final class PeepoRenderer extends MobRenderer<PeepoEntity,PeepoState,PeepoModel> {
    private final ItemModelResolver items;
    public PeepoRenderer(EntityRendererProvider.Context context) { super(context, new PeepoModel(context.bakeLayer(PeepoClient.LAYER)), .18F);
        items=context.getItemModelResolver();
        addLayer(new FoodLayer(this));
        addLayer(new HeldLayer(this));
        addLayer(new KnifeLayer(this));
        addLayer(new HoeLayer(this));
        addLayer(new AnimalWorkLayer(this));
        addLayer(new PieLayer(this));
        addLayer(new SpoonLayer(this,new StirringSpoonModel(context.bakeLayer(PeepoClient.SPOON))));
        addLayer(new WorkPropsLayer(this,new WorkPropsModel(context.bakeLayer(PeepoClient.WORK_WOOD)),
            new WorkPropsModel(context.bakeLayer(PeepoClient.WORK_METAL))));
        addLayer(new EyesLayer<PeepoState,PeepoModel>(this) {
            @Override public RenderType renderType() { return RenderTypes.eyes(PeepoMod.id("textures/entity/peepo_pumpkin_glow.png")); }
        });
    }
    @Override public PeepoState createRenderState() { return new PeepoState(); }
    @Override protected void setupRotations(PeepoState state, PoseStack pose, float bodyRot, float scale) {
        super.setupRotations(state, pose, bodyRot, scale);
        if (state.sleeping && state.deathTime <= 0) {
            pose.translate(0, state.pumpkin ? .26 : .17, state.jughead && !state.pumpkin ? -.30 : -.25);
            pose.rotateDegrees(com.mojang.math.Axis.XP, 90);
        }
    }
    @Override public Identifier getTextureLocation(PeepoState state) { if(state.jughead) return PeepoMod.id(state.blushing ? "textures/entity/jughead_blush.png" : "textures/entity/jughead.png"); return PeepoMod.id(state.blushing ? "textures/entity/peepo_blush.png" : "textures/entity/peepo.png"); }
    @Override protected RenderType getRenderType(PeepoState state, boolean visible, boolean forceTransparent, boolean glowing) {
        if(state.jughead && visible && !forceTransparent) return RenderTypes.entityTranslucent(getTextureLocation(state));
        return super.getRenderType(state,visible,forceTransparent,glowing);
    }
    @Override public void extractRenderState(PeepoEntity entity, PeepoState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.blushing = entity.isBlushing();
        state.pumpkin = entity.isWearingPumpkin();
        state.jughead = entity.isJughead();
        state.eating=entity.isEating();
        state.holdingLight=TikiTorch.isHeldLight(entity.getMainHandItem());
        state.heldLightScale=entity.getMainHandItem().getItem() instanceof net.minecraft.world.item.BlockItem item
            && item.getBlock() instanceof TikiTorch ? .5F : .35F;
        state.wheelRunning=entity.isWheelRunning();
        state.sleeping=entity.getRestMode()==CompanionEnergy.Rest.SLEEPING;
        state.sitting=entity.getRestMode()==CompanionEnergy.Rest.SITTING;
        state.work=state.eating || state.sleeping || state.sitting || state.wheelRunning || !entity.isAlive()?WorkAnimation.NONE:entity.workAnimation();
        state.social=state.eating || state.sleeping || state.sitting || state.wheelRunning || state.work!=WorkAnimation.NONE || !entity.isAlive()?CompanionSocial.NONE:entity.socialPose();
        state.socialTime=(float)Math.clamp(entity.level().getGameTime()-entity.socialStarted()+partialTick,0,120);
        state.workPhase=(float)WorkAnimation.stirPhase(entity.level().getGameTime(),partialTick);
        var shownPie=net.minecraft.world.item.ItemStack.EMPTY;
        state.rawBake=null;state.cake=false;
        if(state.work.isPie()){
            shownPie=entity.workPie();
            state.rawBake=io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlockEntity.rawFilling(shownPie);
            var filling=io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlockEntity.selectedFilling(shownPie);
            state.cake=filling!=null && filling.cake || shownPie.is(io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture.item("burnt_cake"));
            state.pieX=0;state.pieY=20.0F;state.pieZ=state.pumpkin?-6.0F:-4.8F;state.pieReach=0;
            if(state.work!=WorkAnimation.PIE_CARRY){
                state.bodyRot=entity.getYRot();state.yRot=0;state.xRot=0;
                var target=entity.workTarget();
                if(state.work==WorkAnimation.PIE_LOAD || state.work==WorkAnimation.PIE_TAKE){
                    float t=(float)Math.clamp((entity.level().getGameTime()-entity.workStarted()+partialTick)/20.0,0,1);
                    float slide=t*t*(3-2*t);if(state.work==WorkAnimation.PIE_TAKE)slide=1-slide;
                    state.pieReach=net.minecraft.util.Mth.sin(t*net.minecraft.util.Mth.PI);
                    if(entity.level().hasChunkAt(target)){
                        var block=entity.level().getBlockState(target);
                        if(block.getBlock() instanceof io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlock){
                            var facing=block.getValue(io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlock.FACING);
                            // Same center and shelf height as HearthOvenRenderer's resting pie.
                            var center=net.minecraft.world.phys.Vec3.atBottomCenterOf(target).add(-facing.getStepX()/16.0,state.cake?3.375/16.0:.18,-facing.getStepZ()/16.0);
                            var d=center.subtract(entity.getPosition(partialTick));double a=Math.toRadians(entity.getYRot());
                            state.pieX=net.minecraft.util.Mth.lerp(slide,0,(float)((d.x*Math.cos(a)+d.z*Math.sin(a))*16));
                            state.pieY=net.minecraft.util.Mth.lerp(slide,state.pieY,(float)(24-d.y*16));
                            state.pieZ=net.minecraft.util.Mth.lerp(slide,state.pieZ,(float)((d.x*Math.sin(a)-d.z*Math.cos(a))*16));
                        }
                    }
                }
            }
        }
        items.updateForLiving(state.pie,shownPie,ItemDisplayContext.NONE,entity);
        if(state.work==WorkAnimation.CRANK){
            var target=entity.workTarget();var level=entity.level();
            if(!level.hasChunkAt(target) || !(level.getBlockState(target).getBlock() instanceof io.github.jimbozoomer.jugcraft.kinetic.HandCrankBlock))state.work=WorkAnimation.NONE;
            else{
                // Match kinetic_rotors.json exactly: six degrees per tick, radius 4.5 pixels.
                // No UUID phase offset: both hands must follow the actual block's wooden handle.
                double angle=(Math.floorMod(level.getGameTime(),60)+partialTick)*Math.PI*2/60;
                state.workPhase=(float)angle;
                double x=-Math.sin(angle)*4.5/16,y=Math.cos(angle)*4.5/16,z=3.5/16;
                var facing=level.getBlockState(target).getValue(io.github.jimbozoomer.jugcraft.kinetic.HandCrankBlock.FACING);
                var offset=switch(facing){
                    case NORTH -> new net.minecraft.world.phys.Vec3(x,y,z);
                    case SOUTH -> new net.minecraft.world.phys.Vec3(-x,y,-z);
                    case EAST -> new net.minecraft.world.phys.Vec3(-z,y,x);
                    case WEST -> new net.minecraft.world.phys.Vec3(z,y,-x);
                    case UP -> new net.minecraft.world.phys.Vec3(x,-z,y);
                    case DOWN -> new net.minecraft.world.phys.Vec3(x,z,-y);
                };
                var delta=net.minecraft.world.phys.Vec3.atCenterOf(target).add(offset).subtract(entity.getPosition(partialTick));
                double yaw=Math.toRadians(entity.getYRot());
                state.crankX=(float)((delta.x*Math.cos(yaw)+delta.z*Math.sin(yaw))*16);
                state.crankZ=(float)((delta.x*Math.sin(yaw)-delta.z*Math.cos(yaw))*16);
                state.crankY=(float)(24-delta.y*16);
                state.bodyRot=entity.getYRot();state.yRot=0;state.xRot=0;
            }
        }
        if(state.work==WorkAnimation.STIR){
            var target=entity.workTarget();
            // The server plants the companion on the rim; only the hands/spoon animate.
            if(!entity.level().hasChunkAt(target) || !(entity.level().getBlockState(target).getBlock() instanceof io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlock))state.work=WorkAnimation.NONE;
            else{
                state.bodyRot=entity.getYRot();state.yRot=0;state.xRot=0;state.shadowRadius=0;
            }
        }
        if(state.work.hasTool()){
            var target=entity.workTarget();
            if(!entity.level().hasChunkAt(target) || !(entity.level().getBlockState(target).getBlock() instanceof io.github.jimbozoomer.jugcraft.machine.MachineBlock))state.work=WorkAnimation.NONE;
            else{
                state.bodyRot=entity.getYRot();state.yRot=0;state.xRot=0;
                state.workPhase=state.work.phase(entity.level().getGameTime(),partialTick,entity.getUUID().hashCode());
                state.toolPose.evaluate(state.work,state.workPhase,state.pumpkin);
            }
        }
        if(state.work==WorkAnimation.CHOP){
            var target=entity.workTarget();
            if(!entity.level().hasChunkAt(target)
                || !(entity.level().getBlockState(target).getBlock() instanceof io.github.jimbozoomer.jugcraft.agriculture.CuttingBoardBlock)
                || !entity.getMainHandItem().is(io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture.KNIVES))state.work=WorkAnimation.NONE;
            else{
                state.bodyRot=entity.getYRot();state.yRot=0;state.xRot=0;
                state.workPhase=WorkAnimation.CHOP.phase(entity.level().getGameTime()-entity.workStarted(),partialTick,0);
                var delta=net.minecraft.world.phys.Vec3.atBottomCenterOf(target).add(0,1.05/16,0).subtract(entity.getPosition(partialTick));
                double a=Math.toRadians(entity.getYRot());
                state.toolPose.chop(state.workPhase,(float)(24-delta.y*16),(float)((delta.x*Math.sin(a)-delta.z*Math.cos(a))*16));
            }
        }
        if(state.work==WorkAnimation.HARVEST){
            var target=entity.workTarget();
            if(!entity.level().hasChunkAt(target)
                || !(entity.level().getBlockState(target).getBlock() instanceof net.minecraft.world.level.block.FarmlandBlock)
                || !entity.getMainHandItem().is(net.minecraft.tags.ItemTags.HOES))state.work=WorkAnimation.NONE;
            else{
                state.bodyRot=entity.getYRot();state.yRot=0;state.xRot=0;
                // One server-timed harvesting action, rather than a randomly phased looping prop.
                float elapsed=net.minecraft.util.Mth.clamp(entity.level().getGameTime()-entity.workStarted()+partialTick,0,39.99F);
                state.workPhase=elapsed*net.minecraft.util.Mth.TWO_PI/CompanionGarden.HOE_TICKS;
                var delta=net.minecraft.world.phys.Vec3.atBottomCenterOf(target).add(0,15.0/16,0).subtract(entity.getPosition(partialTick));
                double a=Math.toRadians(entity.getYRot());
                state.toolPose.harvest(state.workPhase,(float)(24-delta.y*16),(float)((delta.x*Math.sin(a)-delta.z*Math.cos(a))*16));
            }
        }
        if(state.work==WorkAnimation.SHEAR || state.work==WorkAnimation.MILK){
            var animal=entity.level().getEntity(entity.workAnimal());
            if(!(animal instanceof net.minecraft.world.entity.animal.Animal) || !animal.isAlive())state.work=WorkAnimation.NONE;
            else{
                state.bodyRot=entity.getYRot();state.yRot=0;state.xRot=0;
                float elapsed=(float)Math.max(0,entity.level().getGameTime()-entity.workStarted()+partialTick);
                state.workPhase=elapsed*net.minecraft.util.Mth.TWO_PI/CompanionLivestock.WORK_TICKS;
                var delta=animal.getPosition(partialTick).subtract(entity.getPosition(partialTick));double yaw=Math.toRadians(entity.getYRot());
                state.bucketX=(float)((delta.x*Math.cos(yaw)+delta.z*Math.sin(yaw))*16);
                state.bucketZ=(float)((delta.x*Math.sin(yaw)-delta.z*Math.cos(yaw))*16);
                state.bucketY=(float)(24-(delta.y+.16)*16);
                if(state.work==WorkAnimation.SHEAR)state.toolPose.shear(state.workPhase,(float)(24-(delta.y+animal.getBbHeight()*.46)*16),state.bucketZ+animal.getBbWidth()*8);
            }
        }
        items.updateForLiving(state.animalTool,state.work==WorkAnimation.SHEAR?entity.getMainHandItem():state.work==WorkAnimation.MILK?new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET):net.minecraft.world.item.ItemStack.EMPTY,ItemDisplayContext.NONE,entity);
        // NONE supplies the original item model; the harvesting layer applies its own hoe scale.
        items.updateForLiving(state.knife,state.work==WorkAnimation.CHOP?entity.getMainHandItem():net.minecraft.world.item.ItemStack.EMPTY,ItemDisplayContext.NONE,entity);
        items.updateForLiving(state.hoe,state.work==WorkAnimation.HARVEST?entity.getMainHandItem():net.minecraft.world.item.ItemStack.EMPTY,ItemDisplayContext.NONE,entity);
        state.eatingTime=PeepoEntity.EAT_DURATION-entity.getEatingTicks()+partialTick;
        items.updateForLiving(state.held,!state.eating && !state.sleeping && state.work==WorkAnimation.NONE ? entity.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY,state.holdingLight ? ItemDisplayContext.NONE : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,entity);
        items.updateForLiving(state.food,state.eating ? entity.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY,ItemDisplayContext.FIXED,entity);
    }
    private static final class KnifeLayer extends RenderLayer<PeepoState,PeepoModel>{
        KnifeLayer(PeepoRenderer renderer){super(renderer);}
        @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,PeepoState state,float yaw,float pitch){
            if(state.isInvisible || state.work!=WorkAnimation.CHOP || state.knife.isEmpty())return;
            pose.pushPose();knifePose(pose,state.toolPose);
            state.knife.submit(pose,collector,light,OverlayTexture.NO_OVERLAY,state.outlineColor);pose.popPose();
        }
    }
    private static final class HoeLayer extends RenderLayer<PeepoState,PeepoModel>{
        HoeLayer(PeepoRenderer renderer){super(renderer);}
        @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,PeepoState state,float yaw,float pitch){
            if(state.isInvisible || state.work!=WorkAnimation.HARVEST || state.hoe.isEmpty())return;
            pose.pushPose();hoePose(pose,state.toolPose);
            state.hoe.submit(pose,collector,light,OverlayTexture.NO_OVERLAY,state.outlineColor);pose.popPose();
        }
    }
    private static final class AnimalWorkLayer extends RenderLayer<PeepoState,PeepoModel>{
        AnimalWorkLayer(PeepoRenderer renderer){super(renderer);}
        @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,PeepoState state,float yaw,float pitch){
            if(state.isInvisible || state.animalTool.isEmpty())return;
            pose.pushPose();
            if(state.work==WorkAnimation.SHEAR)workItemPose(pose,state.toolPose,.45F,90);
            else {pose.translate(state.bucketX/16,state.bucketY/16,state.bucketZ/16);pose.rotateDegrees(com.mojang.math.Axis.XP,180);pose.scale(.4F,.4F,.4F);}
            state.animalTool.submit(pose,collector,light,OverlayTexture.NO_OVERLAY,state.outlineColor);pose.popPose();
        }
    }
    static void knifePose(PoseStack pose,MachineWorkClip p){
        workItemPose(pose,p,1,90);
    }
    static void hoePose(PoseStack pose,MachineWorkClip p){
        // Reverse the blade around the handle: the working edge leads the downstroke.
        workItemPose(pose,p,MachineWorkClip.HOE_SCALE,-90);
    }
    private static void workItemPose(PoseStack pose,MachineWorkClip p,float scale,float facing){
        pose.translate(p.x/16,p.y/16,p.z/16);
        pose.rotateDegrees(com.mojang.math.Axis.XP,p.xRot*net.minecraft.util.Mth.RAD_TO_DEG);
        pose.scale(scale,scale,scale);
        pose.rotateDegrees(com.mojang.math.Axis.YP,facing);
        pose.rotateDegrees(com.mojang.math.Axis.ZP,-45);
        pose.rotateDegrees(com.mojang.math.Axis.XP,180);
        // Diagonal knife/hoe sprites have their handle near pixel (4,12).
        // Scale around this grip, keeping the item anchored to the shared hand pivot.
        pose.translate(.25,.25,0);
    }
    private static final class WorkPropsLayer extends RenderLayer<PeepoState,PeepoModel>{
        private static final Identifier WOOD=io.github.jimbozoomer.jugcraft.Jugcraft.id("textures/block/cider_press_wood.png");
        private static final Identifier METAL=io.github.jimbozoomer.jugcraft.Jugcraft.id("textures/block/cider_press_iron.png");
        private final WorkPropsModel wood,metal;
        WorkPropsLayer(PeepoRenderer parent,WorkPropsModel wood,WorkPropsModel metal){super(parent);this.wood=wood;this.metal=metal;}
        @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,PeepoState state,float yaw,float pitch){
            if(state.isInvisible || !state.work.hasTool())return;
            if(state.work==WorkAnimation.LEVER || state.work==WorkAnimation.MALLET)
                collector.order(1).submitModel(wood,state,pose,RenderTypes.entitySolid(WOOD),light,OverlayTexture.NO_OVERLAY,state.outlineColor);
            collector.order(1).submitModel(metal,state,pose,RenderTypes.entitySolid(METAL),light,OverlayTexture.NO_OVERLAY,state.outlineColor);
        }
    }
    private static final class SpoonLayer extends RenderLayer<PeepoState,PeepoModel>{
        private final StirringSpoonModel spoon;
        SpoonLayer(PeepoRenderer parent,StirringSpoonModel spoon){super(parent);this.spoon=spoon;}
        @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,PeepoState state,float yaw,float pitch){
            if(state.isInvisible || state.work!=WorkAnimation.STIR)return;
            collector.order(1).submitModel(spoon,state,pose,RenderTypes.entitySolid(io.github.jimbozoomer.jugcraft.Jugcraft.id("textures/block/cider_press_wood.png")),light,OverlayTexture.NO_OVERLAY,state.outlineColor);
        }
    }
    private static final class HeldLayer extends RenderLayer<PeepoState,PeepoModel> {
        HeldLayer(PeepoRenderer renderer){super(renderer);}
        @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,PeepoState state,float yaw,float pitch){
            if(state.isInvisible || state.held.isEmpty())return;
            pose.pushPose();
            if(state.holdingLight){
                getParentModel().translateToUprightHand(pose);
                // Raw item model: remove both arm tilt and third-person item rotation.
                pose.rotateDegrees(com.mojang.math.Axis.XP,180);
                pose.scale(state.heldLightScale,state.heldLightScale,state.heldLightScale);
                pose.translate(0,.25F,0);
            }else{
                getParentModel().translateToHand(pose);
                pose.rotateDegrees(com.mojang.math.Axis.XP,-90);
                pose.rotateDegrees(com.mojang.math.Axis.YP,180);
                pose.scale(.35F,.35F,.35F);
            }
            state.held.submit(pose,collector,light,OverlayTexture.NO_OVERLAY,state.outlineColor);
            pose.popPose();
        }
    }
    private static final class PieLayer extends RenderLayer<PeepoState,PeepoModel>{
        PieLayer(PeepoRenderer renderer){super(renderer);}
        @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,PeepoState state,float yaw,float pitch){
            if(state.isInvisible || !state.work.isPie() || state.rawBake==null && state.pie.isEmpty())return;
            pose.pushPose();pose.translate(state.pieX/16,state.pieY/16,state.pieZ/16);
            pose.rotateDegrees(com.mojang.math.Axis.XP,180);
            if(state.rawBake!=null){
                // Match the oven's raw dough/tin, at its actual size and centered between the hands.
                pose.translate(-.5,state.cake?-3.375/16.0:-.18,-9.0/16);
                io.github.jimbozoomer.jugcraft.client.HearthOvenRenderer.submitBake(state.rawBake,0,pose,collector,light);
            }else{
                pose.scale(.42F,.42F,.42F);
                pose.translate(0,state.cake?.21875F:.375F,0);
                state.pie.submit(pose,collector,light,OverlayTexture.NO_OVERLAY,state.outlineColor);
            }
            pose.popPose();
        }
    }
    private static final class FoodLayer extends RenderLayer<PeepoState,PeepoModel> {
        FoodLayer(PeepoRenderer renderer) { super(renderer); }
        @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,PeepoState state,float yaw,float pitch) {
            if(!state.eating || state.isInvisible || state.food.isEmpty())return;
            pose.pushPose();
            pose.translate(0,1.10-Math.sin(state.eatingTime*.8)*.012,state.pumpkin ? -.28 : -.235);
            // Entity layers use a Y-down pose; turn the item upright in both hands.
            pose.rotateDegrees(com.mojang.math.Axis.XP,180);
            pose.scale(.17F,.17F,.17F);
            state.food.submit(pose,collector,light,OverlayTexture.NO_OVERLAY,state.outlineColor);
            pose.popPose();
        }
    }

}

