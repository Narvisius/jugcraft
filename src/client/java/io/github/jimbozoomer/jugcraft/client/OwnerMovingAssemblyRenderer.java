package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerAssemblyMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.*;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
/** Moving block models plus a content-free native chest renderer; never transmits carried inventories. */
final class OwnerMovingAssemblyRenderer implements BlockEntityRenderer<OwnerAssemblyMotion.Node,OwnerMovingAssemblyRenderer.State> {
 private final PistonHeadRenderer blocks=new PistonHeadRenderer();private final java.util.Map<OwnerAssemblyMotion.Node,BlockEntity> visuals=new java.util.WeakHashMap<>();
 OwnerMovingAssemblyRenderer(BlockEntityRendererProvider.Context c){}
 static final class State extends PistonHeadRenderState {BlockEntityRenderState carried;}
 @Override public State createRenderState(){return new State();}
 @Override public boolean shouldRenderOffScreen(){return true;}
 @Override public void extractRenderState(OwnerAssemblyMotion.Node node,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){blocks.extractRenderState(node,state,partial,camera,overlay);state.carried=null;if(node.getLevel()==null||!(node.getMovedState().getBlock() instanceof net.minecraft.world.level.block.ChestBlock chest))return;
  var visual=visuals.get(node);if(visual==null||visual.getLevel()!=node.getLevel()||!visual.getBlockState().equals(node.getMovedState())){visual=chest.newBlockEntity(node.getBlockPos(),node.getMovedState());visual.setLevel(node.getLevel());visuals.put(node,visual);}state.carried=Minecraft.getInstance().getBlockEntityRenderDispatcher().tryExtractRenderState(visual,partial,null,false);
 }
 @Override public void submit(State state,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){blocks.submit(state,pose,out,camera);if(state.carried!=null){pose.pushPose();pose.translate(state.xOffset,state.yOffset,state.zOffset);Minecraft.getInstance().getBlockEntityRenderDispatcher().submit(state.carried,pose,out,camera);pose.popPose();}}
}
