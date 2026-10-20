package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoBlockRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.TagValueInput;

/** Captures display-only copies. Rendering never ticks entities or reads another dimension. */
public final class NouveauWorkshopRenderer extends GeoBlockRenderer<NouveauWorkshop,BlockEntityRenderState> {
    private record ItemView(ItemStackRenderState item,double x,double y,double z,float scale){}
    private record Display(List<ItemView> items,EntityRenderState mob,float mobScale,float angle){}
    private record Cached(net.minecraft.world.item.component.CustomData data,Entity entity){}
    private static final DataTicket<Display> DISPLAY=DataTicket.create("jugcraft_nouveau_display",Display.class);
    private final Map<NouveauWorkshop,Cached> creatures=new WeakHashMap<>();
    private final EntityRenderDispatcher entities;
    private static int displayId=-2000000;
    public NouveauWorkshopRenderer(BlockEntityRendererProvider.Context c){super(c,new NouveauVisuals.Model<>("block","empty",b->b.entry().geo().isEmpty()?"empty":b.entry().geo()));entities=c.entityRenderer();}
    @Override public void captureDefaultRenderState(NouveauWorkshop b,Void unused,BlockEntityRenderState s,float partial){
        s.addGeckolibData(NouveauVisuals.MODEL,b.entry().geo().isEmpty()?"empty":b.entry().geo());s.addGeckolibData(NouveauVisuals.TEXTURE,"block/"+(b.entry().geo().isEmpty()?"arcane_core":NouveauPresentation.texture(b.entry().geo())));super.captureDefaultRenderState(b,unused,s,partial);
    }
    @Override public void addRenderData(NouveauWorkshop b,Void unused,BlockEntityRenderState s,float partial){
        var level=b.getLevel();List<ItemView> items=new ArrayList<>();EntityRenderState mob=null;float scale=1;
        float angle=ConcordanceClientOptions.reducedMotion()?0:(float)((level==null?0:level.getGameTime()+partial)%360)*1.5F;
        if(Set.of("arcane_pedestal","arcane_platform","enchanting_apparatus","imbuement_chamber","scribes_table","ritual_brazier","mob_jar").contains(b.kind())){
            ItemStack shown=b.getItem(0);if(shown.isEmpty())shown=b.getItem(NouveauWorkshop.OUTPUT);if(!shown.isEmpty()){
                var facing=b.getBlockState().getValue(NouveauWorkshopBlock.FACING);
                if(b.pedestal()){double distance=b.kind().equals("arcane_platform")?-.1:.55;items.add(item(b,shown,facing.getStepX()*distance,.5+facing.getStepY()*distance,facing.getStepZ()*distance,.4F));}
                else items.add(item(b,shown,0,.95,0,.4F));
            }
        }
        if(b.kind().equals("planarium"))for(var block:b.preview())items.add(item(b,new ItemStack(block.state().getBlock()),(block.offset().getX()-7.5)/32,.15+block.offset().getY()/32.0,(block.offset().getZ()-7.5)/32,1/32F));
        if(b.kind().equals("mob_jar")&&b.captured()!=null&&level!=null){
            var cached=creatures.get(b);if(cached==null||!cached.data.equals(b.captured())||cached.entity.level()!=level){
                var input=TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),b.captured().copyTag());
                var entity=EntityType.loadEntityRecursive(input,level,EntitySpawnReason.MOB_SUMMONED,EntityProcessor.NOP);
                if(entity!=null){entity.setId(displayId--);entity.setPos(b.getBlockPos().getX()+.5,b.getBlockPos().getY()+.1,b.getBlockPos().getZ()+.5);cached=new Cached(b.captured(),entity);creatures.put(b,cached);}
            }
            if(cached!=null){var e=cached.entity;e.setCustomNameVisible(false);scale=Math.min(.6F,.6F/Math.max(.1F,Math.max(e.getBbWidth(),e.getBbHeight())));mob=entities.extractEntity(e,partial);}
        }else creatures.remove(b);
        s.addGeckolibData(DISPLAY,new Display(List.copyOf(items),mob,scale,angle));
    }
    private static ItemView item(NouveauWorkshop b,ItemStack stack,double x,double y,double z,float scale){var state=new ItemStackRenderState();Minecraft.getInstance().getItemModelResolver().updateForTopItem(state,stack,ItemDisplayContext.FIXED,b.getLevel(),null,0);return new ItemView(state,x,y,z,scale);}
    @Override public void submit(BlockEntityRenderState s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        super.submit(s,pose,collector,camera);var display=s.getGeckolibData(DISPLAY);if(display==null)return;
        for(var item:display.items){pose.pushPose();pose.translate(.5+item.x,item.y,.5+item.z);if(item.scale>.1)pose.rotateDegrees(Axis.YP,display.angle);pose.scale(item.scale,item.scale,item.scale);item.item.submit(pose,collector,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
        if(display.mob!=null){pose.pushPose();pose.translate(.5,.1,.5);pose.scale(display.mobScale,display.mobScale,display.mobScale);entities.getRenderer(display.mob).submit(display.mob,pose,collector,camera);pose.popPose();}
    }
}
