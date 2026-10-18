package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Informational client preview only. Real actions recheck claims, collisions, payment and drops on the server. */
public final class AlchemyFeedbackClient {
    public static AlchemyTelemetry.State power=new AlchemyTelemetry.State(0,0,0,false,0);
    private record Box(BlockPos pos,int color){}
    private static List<Box> boxes=List.of();
    private static String preview="";
    private static final int GOOD=0x4076DCCB,BLOCKED=0x65E96760;
    private AlchemyFeedbackClient(){}
    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(AlchemyTelemetry.State.TYPE,(s,c)->power=s);
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{power=new AlchemyTelemetry.State(0,0,0,false,0);boxes=List.of();preview="";});
        ClientTickEvents.END_CLIENT_TICK.register(c->{if(c.player!=null&&c.player.tickCount%5==0)preview(c);});
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,Jugcraft.id("alchemy_power"),(g,d)->{
            var c=Minecraft.getInstance();if(c.player==null||!ConcordanceClientOptions.hud()||c.gui.screen()!=null)return;
            var held=c.player.getMainHandItem();boolean relevant=relevant(held)||relevant(c.player.getOffhandItem())||c.player.getInventory().contains(s->s.has(AlchemicalGear.ACTIVE));
            for(var slot:new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET})relevant|=c.player.getItemBySlot(slot).has(AlchemicalGear.ACTIVE);
            if(!relevant&&preview.isEmpty())return;
            int x=8,y=g.guiHeight()-83;
            String total=Component.translatable("hud.jugcraft.alchemy.power",power.wallet()+power.stars()).getString();
            g.fill(x-3,y-3,x+Math.min(preview.isEmpty()?275:340,g.guiWidth()-20),y+(preview.isEmpty()?33:45),0xB018202A);
            g.text(c.font,total,x,y,power.low()?0xFFFF897C:0xFFABDCD6,false);
            g.text(c.font,Component.translatable("hud.jugcraft.alchemy.sources",power.wallet(),power.stars()),x,y+11,0xFFBBCBD7,false);
            String state=power.low()?Component.translatable("hud.jugcraft.alchemy.low").getString():Component.translatable("hud.jugcraft.alchemy.spent",power.spent()).getString();
            if(held.has(AlchemicalGear.ACTIVE))state+=" | "+Component.translatable("screen.jugcraft.alchemy.on").getString();
            g.text(c.font,state,x,y+22,0xFFE4C786,false);
            if(!preview.isEmpty())g.text(c.font,BookArt.fit(c.font,preview,Math.min(340,g.guiWidth()-20)),x,y+33,0xFFAEDBCA,false);
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx->{
            var c=Minecraft.getInstance();if(c.player==null||c.gui.screen()!=null||boxes.isEmpty())return;
            var captured=boxes;var camera=ctx.levelState().cameraRenderState.pos;var pose=ctx.poseStack();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
            ctx.submitNodeCollector().submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(Jugcraft.id("textures/misc/blueprint_ghost.png")),(p,v)->{
                for(var b:captured){var at=b.pos;LinePreview.cube(p,v,at.getX()+.03f,at.getY()+.03f,at.getZ()+.03f,at.getX()+.97f,at.getY()+.97f,at.getZ()+.97f,b.color);}
            });pose.popPose();
        });
    }
    private static boolean relevant(ItemStack s){return s.getItem() instanceof DarkMatterTool||s.getItem() instanceof ExtendedMatterTool||AlchemyArtifacts.ITEMS.containsValue(s.getItem())||AlchemyStorage.ITEMS.containsValue(s.getItem())||s.is(AlchemicalGear.STONE)||KleinStars.capacity(s)>0;}
    private static BlockPos plane(BlockPos o,Direction face,int a,int b){return switch(face.getAxis()){case X->o.offset(0,a,b);case Y->o.offset(a,0,b);case Z->o.offset(a,b,0);};}
    private static void preview(Minecraft c){
        boxes=List.of();preview="";var p=c.player;var level=c.level;
        if(p==null||level==null||!(c.hitResult instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK||p.distanceToSqr(hit.getLocation())>64)return;
        var at=hit.getBlockPos();var held=p.getMainHandItem();var result=new ArrayList<Box>();long cost=0;int usable=0;
        if(level.getBlockEntity(at) instanceof AlchemyMachinesBlockEntity be&&be.kind()==AlchemyMachines.Kind.PEDESTAL){
            int range=be.range();String effect=BuiltInRegistries.ITEM.getKey(be.displayArtifact().getItem()).getPath();
            if(effect.equals("watch_of_flowing_time")){for(var d:Direction.values())result.add(new Box(at.relative(d),GOOD));}
            else for(int x=-range;x<=range;x++)for(int z=-range;z<=range;z++)if(x*x+z*z<=range*range&&(Math.abs(x)==range||Math.abs(z)==range||x*x+z*z>=(range-1)*(range-1)))result.add(new Box(at.offset(x,0,z),GOOD));
            preview=Component.translatable("hud.jugcraft.alchemy.range",effect.equals("watch_of_flowing_time")?1:range).getString();
        }else if(held.is(AlchemyArtifacts.ITEMS.get("mercurial_eye"))&&held.has(AlchemyPatterns.TARGET)){
            var id=Identifier.tryParse(held.getOrDefault(AlchemyPatterns.TARGET,""));var m=id==null?null:Assaying.catalog().material(id.toString());if(m==null)return;
            int radius=Math.clamp(held.getOrDefault(AlchemyPatterns.AREA,3),1,5)/2;long each=Assay.form(m,1)+8;
            for(int a=-radius;a<=radius;a++)for(int b=-radius;b<=radius;b++){
                var next=plane(at.relative(hit.getDirection()),hit.getDirection(),a,b);boolean good=level.hasChunkAt(next)&&level.getBlockState(next).isAir()&&p.distanceToSqr(Vec3.atCenterOf(next))<=64;
                result.add(new Box(next,good?GOOD:BLOCKED));if(good){cost+=each;usable++;}
            }
        }else{
            String id=BuiltInRegistries.ITEM.getKey(held.getItem()).getPath();var candidates=new ArrayList<BlockPos>();String mode="single";int each=0,limit=8,depth=4;boolean old=false;
            if(held.getItem() instanceof ExtendedMatterTool t){mode=t.selected(held);each=t.cost();limit=mode.equals("fell")?24:t.red?16:8;}
            else if(held.getItem() instanceof DarkMatterTool t){old=true;mode=t.mode(held)==0?"single":t.kind==DarkMatterTool.Kind.AXE?"fell":t.kind==DarkMatterTool.Kind.HAMMER?"face":t.mode(held)==2?"tunnel":"vein";each=t.kind.cost;limit=mode.equals("fell")?16:8;depth=2;}
            boolean lens=Set.of("destruction_catalyst","catalytic_lens","hyperkinetic_lens").contains(id);
            if(!lens&&(mode.equals("single")||p.isShiftKeyDown()||p.getAbilities().instabuild))return;
            var direction=Math.abs(p.getXRot())>45?(p.getXRot()>0?Direction.DOWN:Direction.UP):p.getDirection();
            if(lens){int r=id.equals("hyperkinetic_lens")?2:1,d=id.equals("catalytic_lens")?3:1;each=id.equals("destruction_catalyst")?16:32;
                for(int k=0;k<d;k++)for(int a=-r;a<=r;a++)for(int b=-r;b<=r;b++)candidates.add(plane(at.relative(hit.getDirection().getOpposite(),k),hit.getDirection(),a,b));
            }else if(mode.equals("face")){for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++)if(a!=0||b!=0)candidates.add(plane(at,direction,a,b));}
            else if(mode.equals("tunnel")){for(int i=1;i<=depth;i++)candidates.add(at.relative(direction,i));}
            else if(Set.of("vein","fell","shear").contains(mode)){
                var original=level.getBlockState(at);boolean connected=mode.equals("fell")?original.is(BlockTags.LOGS):mode.equals("shear")?original.is(BlockTags.LEAVES):original.is(TagKey.create(net.minecraft.core.registries.Registries.BLOCK,Identifier.parse("c:ores")));
                if(!connected)return;var queue=new ArrayDeque<BlockPos>();var seen=new HashSet<BlockPos>();queue.add(at);seen.add(at);
                while(!queue.isEmpty()&&seen.size()<192&&candidates.size()<limit){var from=queue.removeFirst();
                    for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
                        if(candidates.size()>=limit||seen.size()>=192)break;
                        if(old&&mode.equals("fell")?y<0||x==0&&y==0&&z==0:Math.abs(x)+Math.abs(y)+Math.abs(z)!=1)continue;
                        var next=from.offset(x,y,z);if(!seen.add(next)||Math.abs(next.getX()-at.getX())>4||Math.abs(next.getZ()-at.getZ())>4||Math.abs(next.getY()-at.getY())>(mode.equals("fell")?16:4)||mode.equals("fell")&&next.getY()<at.getY()||!level.hasChunkAt(next)||!level.getBlockState(next).is(original.getBlock()))continue;
                        candidates.add(next);queue.add(next);
                    }
                }
            }
            float hardness=level.getBlockState(at).getDestroySpeed(level,at);
            for(var next:candidates){var s=level.getBlockState(next);boolean good=level.hasChunkAt(next)&&!s.isAir()&&!s.hasBlockEntity()&&s.getFluidState().isEmpty()&&s.getDestroySpeed(level,next)>=0&&s.getDestroySpeed(level,next)<=(lens?20:hardness+.001f)&&held.isCorrectToolForDrops(s);
                if(held.getItem() instanceof ExtendedMatterTool t)good&=t.matches(s);
                result.add(new Box(next,good?GOOD:BLOCKED));if(good){cost+=each;usable++;}else if(mode.equals("tunnel"))break;
            }
        }
        if(preview.isEmpty()&&!result.isEmpty())preview=Component.translatable("hud.jugcraft.alchemy.preview",usable,cost).getString();
        boxes=List.copyOf(result);
    }
}
