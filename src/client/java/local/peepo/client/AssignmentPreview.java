package local.peepo.client;

import java.util.*;
import local.peepo.*;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.*;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import com.mojang.blaze3d.vertex.*;

/** Draw only the selected entity and eight explicit targets, never scan nearby world blocks. */
public final class AssignmentPreview {
    private record Frame(AABB box,int color){}
    public static void initialize(){
        LevelRenderEvents.COLLECT_SUBMITS.register(context->{
            var mc=Minecraft.getInstance();if(mc.player==null || mc.level==null)return;
            var stack=mc.player.getMainHandItem().is(AssignmentTool.ITEM)?mc.player.getMainHandItem():mc.player.getOffhandItem();
            var npc=AssignmentTool.selected(mc.level,stack);if(npc==null)return;
            var camera=context.levelState().cameraRenderState.pos;
            var frames=new ArrayList<Frame>(9);
            frames.add(new Frame(npc.getBoundingBox().inflate(.06).move(-camera.x,-camera.y,-camera.z),0xFF66FF88));
            var first=AssignmentTool.firstCorner(stack);
            if(AssignmentTool.zoneMode(stack) && first!=null){
                frames.add(new Frame(new AABB(first).inflate(.03).move(-camera.x,-camera.y,-camera.z),0xFFFFFF55));
                if(AssignmentTool.zoneKind(stack)==4){
                    var start=mc.player.getEyePosition();var endPoint=start.add(mc.player.getViewVector(1).scale(6));
                    var hit=mc.level.clip(new net.minecraft.world.level.ClipContext(start,endPoint,net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY,mc.player));
                    var end=hit.getBlockPos();boolean valid=CompanionFishing.validCast(first,end) && CompanionWater.source(mc.level,end) && CompanionFishing.safeSeat(npc,first,end);
                    frames.add(new Frame(new AABB(end).inflate(.025).move(-camera.x,-camera.y,-camera.z),valid?0xFF55FF88:0xFFFF5555));
                    var from=CompanionFishing.seatPosition(first,end).add(0,.35,0).subtract(camera);var to=net.minecraft.world.phys.Vec3.atCenterOf(end).add(0,.4,0).subtract(camera);
                    context.submitNodeCollector().submitCustomGeometry(context.poseStack(),RenderTypes.lines(),(pose,c)->line(pose,c,from.x,from.y,from.z,to.x,to.y,to.z,valid?0xFF55FF88:0xFFFF5555));
                }else if(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit){var end=hit.getBlockPos();boolean valid=AnimalCareZone.valid(first,end);var box=AssignmentTool.zoneKind(stack)==1?new AnimalCareZone(first,end,7).box():new GardenZone(first,end,AssignmentTool.zoneKind(stack)==3).box();frames.add(new Frame(box.move(-camera.x,-camera.y,-camera.z),valid?0xFF55FF88:0xFFFF5555));}
            }
            var assignments=npc.assignments.view();
            for(int row=0;row<assignments.size();row++){
                var target=assignments.get(row);
                if(target==null || !target.local(mc.level) || !mc.level.hasChunkAt(target.at().pos()) || target.at().pos().distToCenterSqr(mc.player.position())>128*128)continue;
                if(target.fishing() && target.fishingWater()!=null){
                    frames.add(new Frame(new AABB(target.at().pos()).inflate(.025).move(-camera.x,-camera.y,-camera.z),0xFF55FF66));
                    frames.add(new Frame(new AABB(target.fishingWater()).inflate(.025).move(-camera.x,-camera.y,-camera.z),CompanionWater.source(mc.level,target.fishingWater())?0xFF55FF66:0xFFFF8844));
                    var from=CompanionFishing.seatPosition(target.at().pos(),target.fishingWater()).add(0,.35,0).subtract(camera);var to=net.minecraft.world.phys.Vec3.atCenterOf(target.fishingWater()).add(0,.4,0).subtract(camera);
                    context.submitNodeCollector().submitCustomGeometry(context.poseStack(),RenderTypes.lines(),(pose,c)->line(pose,c,from.x,from.y,from.z,to.x,to.y,to.z,0xFF55FF88));continue;
                }
                if(target.care() && target.zone()!=null){frames.add(new Frame(target.zone().box().inflate(.015).move(-camera.x,-camera.y,-camera.z),0xFF55CC88));frames.add(new Frame(new AABB(target.at().pos()).inflate(.025).move(-camera.x,-camera.y,-camera.z),0xFF55FF66));continue;}
                if(target.livestock()){
                    var anchor=target.at().pos();
                    frames.add(new Frame(new AABB(anchor).inflate(.025).move(-camera.x,-camera.y,-camera.z),0xFF55FF66));
                    frames.add(new Frame(new AABB(anchor.getX()+.5-8,anchor.getY()+1.02,anchor.getZ()+.5-8,anchor.getX()+.5+8,anchor.getY()+1.06,anchor.getZ()+.5+8).move(-camera.x,-camera.y,-camera.z),0xFF55CC88));
                    continue;
                }
                if(target.gardenZone()!=null){frames.add(new Frame(target.gardenZone().box().inflate(.015).move(-camera.x,-camera.y,-camera.z),target.orchard()?0xFF99DD55:0xFF55FF66));frames.add(new Frame(new AABB(target.at().pos()).inflate(.025).move(-camera.x,-camera.y,-camera.z),0xFF55FF66));continue;}
                if(target.garden()){
                    for(var cell:target.plot())if(mc.level.hasChunkAt(cell)){
                        AABB box=CompanionGarden.hanging(target)?new AABB(cell.below()):new AABB(cell.getX(),cell.getY()+.89,cell.getZ(),cell.getX()+1,cell.getY()+1.02,cell.getZ()+1);
                        frames.add(new Frame(box.inflate(.015).move(-camera.x,-camera.y,-camera.z),npc.garden.validCell(mc.level,target,cell)?0xFF55FF66:0xFFFF8844));
                    }
                    continue;
                }
                BlockPos pos=target.at().pos();AABB box=new AABB(pos);boolean present=target.present(mc.level);
                if(present){
                    var state=mc.level.getBlockState(pos);
                    if(state.getBlock() instanceof WheelBlock)for(int i=1;i<4;i++)box=box.minmax(new AABB(WheelBlock.partPos(pos,state.getValue(WheelBlock.FACING),i)));
                    else if(state.getBlock() instanceof BedBlock)box=box.minmax(new AABB(pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite())));
                    else if(state.getBlock() instanceof MachineBlock machine){var footprint=machine.footprint(state);for(int i=1;i<Math.min(256,footprint.size());i++)box=box.minmax(new AABB(footprint.partPos(pos,state.getValue(MachineBlock.FACING),i)));}
                    else if(state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock && state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE)!=net.minecraft.world.level.block.state.properties.ChestType.SINGLE){
                        var other=pos.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(state));
                        if(mc.level.hasChunkAt(other))box=box.minmax(new AABB(other));
                    }
                }
                int color=CompanionAssignments.supplySlot(row)?CompanionAssignments.SUPPLY_COLOR:row>=CompanionAssignments.SUPPLY?CompanionAssignments.OUTPUT_COLOR:0xFF55FF66;
                frames.add(new Frame(box.inflate(.025).move(-camera.x,-camera.y,-camera.z),present?color:0xFFFF8844));
            }
            context.submitNodeCollector().submitCustomGeometry(context.poseStack(),RenderTypes.lines(),(pose,consumer)->{
                for(var frame:frames)draw(pose,consumer,frame.box,frame.color);
            });
        });
    }
    private static void line(PoseStack.Pose p,VertexConsumer c,double x,double y,double z,double a,double b,double d,int color){
        float nx=(float)(a-x),ny=(float)(b-y),nz=(float)(d-z);float length=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(length==0)return;
        nx/=length;ny/=length;nz/=length;
        c.addVertex(p,(float)x,(float)y,(float)z).setColor(color).setNormal(p,nx,ny,nz).setLineWidth(2.5F);
        c.addVertex(p,(float)a,(float)b,(float)d).setColor(color).setNormal(p,nx,ny,nz).setLineWidth(2.5F);
    }
    private static void draw(PoseStack.Pose p,VertexConsumer c,AABB b,int color){
        for(double y:new double[]{b.minY,b.maxY}){
            line(p,c,b.minX,y,b.minZ,b.maxX,y,b.minZ,color);line(p,c,b.maxX,y,b.minZ,b.maxX,y,b.maxZ,color);
            line(p,c,b.maxX,y,b.maxZ,b.minX,y,b.maxZ,color);line(p,c,b.minX,y,b.maxZ,b.minX,y,b.minZ,color);
        }
        for(double x:new double[]{b.minX,b.maxX})for(double z:new double[]{b.minZ,b.maxZ})line(p,c,x,b.minY,z,x,b.maxY,z,color);
    }
}
