package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.botany.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import java.util.*;

/** Non-invasive redstone readouts, frame contents and visible spreader aiming. */
final class BotanicalMonocleClient {
    private static java.util.List<net.minecraft.world.item.ItemStack> preview(net.minecraft.world.item.ItemStack stack){
        var contents=stack.get(DataComponents.CONTAINER);if(contents!=null)return contents.itemCopies().filter(s->!s.isEmpty()).limit(54).toList();
        java.util.List<String> ids=stack.is(io.github.jimbozoomer.jugcraft.jimtafacts.industry.FactoryFieldTools.LIST)?io.github.jimbozoomer.jugcraft.jimtafacts.industry.FactoryFieldTools.entries(stack):stack.getOrDefault(io.github.jimbozoomer.jugcraft.nouveau.NouveauUtilities.FILTER,List.of());
        var out=new ArrayList<net.minecraft.world.item.ItemStack>();for(String id:ids.stream().limit(54).toList()){var key=net.minecraft.resources.Identifier.tryParse(id);if(key==null)continue;var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(key);if(item!=null&&item!=net.minecraft.world.item.Items.AIR)out.add(new net.minecraft.world.item.ItemStack(item));}return out;
    }
    private static String filterName(net.minecraft.world.item.ItemStack stack){return stack.is(io.github.jimbozoomer.jugcraft.nouveau.NouveauCatalog.item("deny_scroll"))?"Excluded items":"Frame filter contents";}
    static void register(){
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack,context,flags,lines)->{
            String attached=BotanicalCosmetics.attached(stack);
            if(!attached.isEmpty())lines.add(net.minecraft.network.chat.Component.literal("Appearance: ").append(new net.minecraft.world.item.ItemStack(Jugtania.item(attached)).getHoverName()).withStyle(net.minecraft.ChatFormatting.GRAY));
            var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());if(id.getNamespace().equals("jugcraft")&&id.getPath().startsWith("jugtania_")&&BotanicalCosmetics.cosmetic(id.getPath().substring(9)))lines.add(net.minecraft.network.chat.Component.literal("Wear in an accessory slot, or craft with a botanical accessory to attach.").withStyle(net.minecraft.ChatFormatting.GRAY));
        });
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,Jugcraft.id("botanical_monocle"),(g,delta)->{
            var c=Minecraft.getInstance();if(c.player==null||c.level==null||c.gui.hud.isHidden()||!BotanicalCosmetics.monocle(c.player))return;
            String text="";int x=g.guiWidth()/2+16,y=g.guiHeight()/2+12;
            if(c.hitResult instanceof BlockHitResult hit&&hit.getType()==HitResult.Type.BLOCK){
                var state=c.level.getBlockState(hit.getBlockPos());
                if(state.hasProperty(BlockStateProperties.POWER))text="Signal: "+state.getValue(BlockStateProperties.POWER)+" / 15";
                else if(state.is(Blocks.REPEATER))text="Delay: "+String.format(Locale.ROOT,"%.1f",state.getValue(RepeaterBlock.DELAY)*.1)+" seconds";
                else if(state.is(Blocks.COMPARATOR))text="Comparator: "+state.getValue(ComparatorBlock.MODE).getSerializedName();
                else if(c.level.getBlockEntity(hit.getBlockPos()) instanceof BotanicalWorkshop workshop)text="Mana: "+workshop.mana()+" / "+workshop.capacity();
            }else if(c.hitResult instanceof EntityHitResult hit&&hit.getEntity() instanceof ItemFrame frame){
                var stack=frame.getItem();var entries=preview(stack);
                if(!entries.isEmpty()){
                    int cols=Math.min(9,entries.size()),rows=(Math.min(27,entries.size())+8)/9;x=Math.min(x,g.guiWidth()-cols*18-8);
                    g.fill(x-4,y-4,x+cols*18+4,y+rows*18+18,0xE0202B38);
                    for(int i=0;i<Math.min(27,entries.size());i++)g.item(entries.get(i),x+i%9*18,y+i/9*18);
                    g.text(c.font,entries.size()>27?"+ "+(entries.size()-27)+" more":filterName(stack),x,y+rows*18+2,0xFFFFFFFF,false);return;
                }
                if(!stack.isEmpty())text=stack.getHoverName().getString();
            }
            if(text.isEmpty())return;x=Math.min(x,Math.max(4,g.guiWidth()-c.font.width(text)-8));g.fill(x-4,y-4,x+c.font.width(text)+4,y+13,0xE0202B38);g.text(c.font,text,x,y,0xFFFFFFFF,false);
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx->{
            var c=Minecraft.getInstance();if(c.player==null||c.level==null||!BotanicalCosmetics.monocle(c.player)||!(c.hitResult instanceof BlockHitResult hit)||!(c.level.getBlockEntity(hit.getBlockPos()) instanceof BotanicalWorkshop workshop) )return;
            if(BotanicalLuminizer.TYPES.contains(workshop.kind())){relayRadius(ctx,c,workshop);return;}
            if(!workshop.kind().contains("spreader"))return;
            var start=Vec3.atCenterOf(hit.getBlockPos());var direction=workshop.aim();var points=new ArrayList<Vec3>();
            for(int i=1;i<=64;i++){var at=start.add(direction.scale(i*.25));if(!c.level.hasChunkAt(net.minecraft.core.BlockPos.containing(at)))break;points.add(at);var collision=c.level.clip(new ClipContext(at.subtract(direction.scale(.25)),at,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.player));if(i>3&&collision.getType()!=HitResult.Type.MISS)break;}
            var camera=ctx.levelState().cameraRenderState.pos;var pose=ctx.poseStack();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
            ctx.submitNodeCollector().submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(Jugcraft.id("textures/misc/blueprint_ghost.png")),(p,v)->{for(int i=0;i<points.size();i++){var at=points.get(i);float x=(float)at.x,y=(float)at.y,z=(float)at.z;LinePreview.cube(p,v,x-.025F,y-.025F,z-.025F,x+.025F,y+.025F,z+.025F,i==points.size()-1?0xD0FFC857:0xA063E7CB);}});pose.popPose();
        });
    }

    private static void relayRadius(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext ctx,Minecraft mc,BotanicalWorkshop host){
        if(!mc.player.getMainHandItem().is(Jugtania.item("twig_wand"))&&!mc.player.getMainHandItem().is(Jugtania.item("dreamwood_wand"))&&!mc.player.getOffhandItem().is(Jugtania.item("twig_wand"))&&!mc.player.getOffhandItem().is(Jugtania.item("dreamwood_wand")))return;
        var origin=Vec3.atCenterOf(host.getBlockPos());var points=new ArrayList<Vec3>();
        for(int i=0;i<128;i++){double a=i*Math.PI/64;var point=origin.add(Math.cos(a)*20,0,Math.sin(a)*20);if(mc.level.hasChunkAt(net.minecraft.core.BlockPos.containing(point)))points.add(point);}
        var link=host.luminizer.visualLink();if(link!=null){var end=Vec3.atCenterOf(link);for(int i=1;i<40;i++)points.add(origin.lerp(end,i/40D));}
        var camera=ctx.levelState().cameraRenderState.pos;var pose=ctx.poseStack();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        ctx.submitNodeCollector().submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(Jugcraft.id("textures/misc/blueprint_ghost.png")),(m,v)->{for(var at:points){float x=(float)at.x,y=(float)at.y,z=(float)at.z;LinePreview.cube(m,v,x-.045F,y-.045F,z-.045F,x+.045F,y+.045F,z+.045F,0xAA8899FF);}});pose.popPose();
    }
}
