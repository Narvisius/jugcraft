package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.boardgames.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
/** Small solid carved pieces reflect the actual board, including captures and promotions. */
public final class BoardTableRenderer implements BlockEntityRenderer<TableEntity,BoardTableRenderer.State> {
 public static final class State extends BlockEntityRenderState {int[] squares=new int[64];boolean checkers;int turn;}
 public BoardTableRenderer(BlockEntityRendererProvider.Context c){}
 public State createRenderState(){return new State();}
 public void extractRenderState(TableEntity b,State s,float dt,Vec3 camera,ModelFeatureRenderer.@Nullable CrumblingOverlay overlay){BlockEntityRenderState.extractBase(b,s,overlay);s.squares=b.game.squares.clone();s.checkers=b.game.kind==BoardPosition.Kind.CHECKERS;s.turn=b.game.turn;}
 public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){collector.submitCustomGeometry(pose,RenderTypes.entityCutout(Identifier.withDefaultNamespace("textures/block/white_concrete.png")),(matrix,v)->{
  for(int q=0;q<64;q++){int p=s.squares[q];if(p==0)continue;float x=2.75F+1.5F*(q%8),z=2.75F+1.5F*(q/8),y=13.16F;int color=p>0?0xFFF0D9AB:0xFF353B43;
   TintedBoxes.box(v,matrix,x-.57F,y,z-.57F,x+.57F,y+.22F,z+.57F,color,s.lightCoords);
   if(s.checkers){TintedBoxes.box(v,matrix,x-.43F,y+.22F,z-.43F,x+.43F,y+.4F,z+.43F,color,s.lightCoords);if(Math.abs(p)==2)TintedBoxes.box(v,matrix,x-.32F,y+.4F,z-.32F,x+.32F,y+.65F,z+.32F,0xFFCA9A40,s.lightCoords);continue;}
   int type=Math.abs(p);float height=type==1?1.05F:type==6?1.9F:type==5?1.75F:1.5F;
   TintedBoxes.box(v,matrix,x-.25F,y+.22F,z-.25F,x+.25F,y+height-.3F,z+.25F,color,s.lightCoords);
   TintedBoxes.box(v,matrix,x-.4F,y+height-.3F,z-.4F,x+.4F,y+height,z+.4F,color,s.lightCoords);
   if(type==2)TintedBoxes.box(v,matrix,x-.3F,y+height-.3F,z-.65F,x+.3F,y+height+.15F,z+.2F,color,s.lightCoords);
   if(type==3)TintedBoxes.box(v,matrix,x-.2F,y+height,z-.2F,x+.2F,y+height+.35F,z+.2F,color,s.lightCoords);
   if(type==4||type==5)for(int c=-1;c<=1;c+=2)TintedBoxes.box(v,matrix,x+c*.28F-.1F,y+height,z-.35F,x+c*.28F+.1F,y+height+.3F,z+.35F,color,s.lightCoords);
   if(type==5)TintedBoxes.box(v,matrix,x-.13F,y+height,z-.13F,x+.13F,y+height+.45F,z+.13F,color,s.lightCoords);
   if(type==6){TintedBoxes.box(v,matrix,x-.12F,y+height,z-.12F,x+.12F,y+height+.65F,z+.12F,color,s.lightCoords);TintedBoxes.box(v,matrix,x-.35F,y+height+.28F,z-.12F,x+.35F,y+height+.45F,z+.12F,color,s.lightCoords);}
  }
 });}
}
