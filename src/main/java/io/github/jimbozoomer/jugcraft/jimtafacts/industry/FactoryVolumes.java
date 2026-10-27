package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.*;

/** Persistent, non-block survey volumes. Items hold only a reference, never authority or another copy. */
public final class FactoryVolumes extends SavedData {
    public record Volume(UUID id,UUID owner,BlockPos a,BlockPos b,int pattern,boolean inverted,Optional<BlockPos> machine){
        static final Codec<Volume> CODEC=RecordCodecBuilder.create(i->i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Volume::id),UUIDUtil.CODEC.fieldOf("owner").forGetter(Volume::owner),
            BlockPos.CODEC.fieldOf("a").forGetter(Volume::a),BlockPos.CODEC.fieldOf("b").forGetter(Volume::b),Codec.intRange(0,8).optionalFieldOf("pattern",0).forGetter(Volume::pattern),Codec.BOOL.optionalFieldOf("inverted",false).forGetter(Volume::inverted),BlockPos.CODEC.optionalFieldOf("machine").forGetter(Volume::machine)).apply(i,Volume::new));
    }
    private static final Codec<FactoryVolumes> CODEC=Volume.CODEC.listOf(0,1024).xmap(FactoryVolumes::new,s->List.copyOf(s.volumes.values()));
    private static final SavedDataType<FactoryVolumes> TYPE=new SavedDataType<>(Jugcraft.id("jimta_factory_volumes"),()->new FactoryVolumes(List.of()),CODEC,null);
    private final Map<UUID,Volume> volumes=new LinkedHashMap<>();
    public static Item TOOL,PLANNER,CONNECTOR;
    private record Drag(Volume original,boolean first,BlockPos point){}
    private final Map<UUID,Drag> dragging=new HashMap<>();
    private Volume preview(UUID player,Volume saved){var d=dragging.get(player);return d!=null&&d.original.id.equals(saved.id)?new Volume(saved.id,saved.owner,d.first?d.point:saved.a,d.first?saved.b:d.point,saved.pattern,saved.inverted,saved.machine):saved;}
    private void dragTick(ServerPlayer p){
        var d=dragging.get(p.getUUID());if(d==null)return;
        var current=volumes.get(d.original.id);
        if(!p.isAlive()||p.isSpectator()||!p.getMainHandItem().is(CONNECTOR)&&!p.getOffhandItem().is(CONNECTOR)||!d.original.equals(current)){dragging.remove(p.getUUID());return;}
        var hit=p.pick(32,1,false);var at=BlockPos.containing(hit.getLocation());
        if(!p.level().hasChunkAt(at)||!Authority.mayChange(p.level(),p,at))return;
        if(FactoryPlanning.valid(d.first?at:d.original.a,d.first?d.original.b:at))dragging.put(p.getUUID(),new Drag(d.original,d.first,at));
    }
    private FactoryVolumes(List<Volume> saved){for(var v:saved)if(FactoryPlanning.valid(v.a,v.b))volumes.put(v.id,v);}
    public static FactoryVolumes get(ServerLevel level){return level.getDataStorage().computeIfAbsent(TYPE);}
    public Volume create(UUID owner,BlockPos at){
        if(volumes.size()>=1024||volumes.values().stream().filter(v->v.owner.equals(owner)).count()>=16)return null;
        var v=new Volume(UUID.randomUUID(),owner,at.immutable(),at.immutable(),0,false,Optional.empty());volumes.put(v.id,v);setDirty();return v;
    }
    public boolean resize(UUID id,UUID owner,BlockPos at,boolean first){
        var v=volumes.get(id);if(v==null||!v.owner.equals(owner)||v.machine.isPresent())return false;
        var next=new Volume(v.id,v.owner,first?at.immutable():v.a,first?v.b:at.immutable(),v.pattern,v.inverted,v.machine);
        if(!FactoryPlanning.valid(next.a,next.b))return false;volumes.put(id,next);setDirty();return true;
    }
    public boolean plan(UUID id,UUID owner,int pattern,boolean inverted){
        var v=volumes.get(id);if(v==null||!v.owner.equals(owner)||v.machine.isPresent()||pattern<0||pattern>=FillerPatterns.NAMES.size())return false;
        volumes.put(id,new Volume(v.id,v.owner,v.a,v.b,pattern,inverted,v.machine));setDirty();return true;
    }
    public boolean remove(UUID id,UUID owner){var v=volumes.get(id);if(v==null||!v.owner.equals(owner)||v.machine.isPresent())return false;volumes.remove(id);setDirty();return true;}
    public boolean bind(UUID id,ServerPlayer player,CivilWorkshops.Node machine){
        var v=volumes.get(id);if(v==null||!v.owner.equals(player.getUUID())||!machine.permitted(player)||machine.running||machine.retracting)return false;
        if(v.machine.isPresent()&&!v.machine.get().equals(machine.getBlockPos()))return false;
        if(!machine.setArea(v.a,v.b))return false;
        machine.surveyVolume=id;machine.setChanged();
        volumes.put(id,new Volume(v.id,v.owner,v.a,v.b,v.pattern,v.inverted,Optional.of(machine.getBlockPos().immutable())));setDirty();return true;
    }
    /** Machine-owned cleanup, including removing a block or replacing its survey through another tool. */
    public void releaseMachine(UUID id,BlockPos machine){
        var v=volumes.get(id);if(v==null||v.machine.isEmpty()||!v.machine.get().equals(machine))return;
        volumes.put(id,new Volume(v.id,v.owner,v.a,v.b,v.pattern,v.inverted,Optional.empty()));setDirty();
    }
    private boolean disconnect(Volume v,ServerPlayer p){
        if(!v.owner.equals(p.getUUID()))return false;if(v.machine.isEmpty())return true;
        var at=v.machine.get();if(!p.level().hasChunkAt(at))return false;
        if(p.level().getBlockEntity(at) instanceof CivilWorkshops.Node machine&&v.id.equals(machine.surveyVolume)){
            if(!machine.permitted(p)||machine.running||machine.retracting)return false;
            machine.surveyVolume=null;machine.setChanged();
        }
        releaseMachine(v.id,at);return true;
    }
    public static void register(){
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_volume_box"));TOOL=Registry.register(BuiltInRegistries.ITEM,key,new Tool(new Item.Properties().setId(key).stacksTo(1)));
        var plannerKey=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_filler_planner"));PLANNER=Registry.register(BuiltInRegistries.ITEM,plannerKey,new PlannerTool(new Item.Properties().setId(plannerKey).stacksTo(1)));
        var connectorKey=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_marker_connector"));CONNECTOR=Registry.register(BuiltInRegistries.ITEM,connectorKey,new Connector(new Item.Properties().setId(connectorKey).stacksTo(1)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(o->{o.accept(TOOL);o.accept(PLANNER);o.accept(CONNECTOR);});
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%5!=0)return;for(var level:server.getAllLevels()){
            var store=get(level);store.dragging.keySet().removeIf(id->{var player=server.getPlayerList().getPlayer(id);return player==null||player.level()!=level;});
            for(var player:level.players())store.dragTick(player);
        }});
        ServerTickEvents.END_SERVER_TICK.register(s->{if(s.getTickCount()%20==0)for(var p:s.getPlayerList().getPlayers()){
            var stack=p.getMainHandItem().is(TOOL)?p.getMainHandItem():p.getOffhandItem();if(!stack.is(TOOL)||p.isSpectator())continue;
            var v=selected(p,stack);if(v==null||!v.owner.equals(p.getUUID()))continue;v=get(p.level()).preview(p.getUUID(),v);
            // Eight corners and twelve edge midpoints per second, only to the holder; no chunk tickets.
            double[] low={Math.min(v.a.getX(),v.b.getX()),Math.min(v.a.getY(),v.b.getY()),Math.min(v.a.getZ(),v.b.getZ())};
            double[] high={Math.max(v.a.getX(),v.b.getX())+1,Math.max(v.a.getY(),v.b.getY())+1,Math.max(v.a.getZ(),v.b.getZ())+1};
            for(int x=0;x<2;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++)spark(p,x==0?low[0]:high[0],y==0?low[1]:high[1],z==0?low[2]:high[2]);
            for(int axis=0;axis<3;axis++)for(int a=0;a<2;a++)for(int b=0;b<2;b++){double[] point=low.clone();point[axis]=(low[axis]+high[axis])/2;point[(axis+1)%3]=a==0?low[(axis+1)%3]:high[(axis+1)%3];point[(axis+2)%3]=b==0?low[(axis+2)%3]:high[(axis+2)%3];spark(p,point[0],point[1],point[2]);}
        }});
    }
    private static void spark(ServerPlayer p,double x,double y,double z){if(p.distanceToSqr(x,y,z)<=64*64&&p.level().hasChunkAt(BlockPos.containing(x,y,z)))p.level().sendParticles(p,ParticleTypes.END_ROD,false,false,x,y,z,1,0,0,0,0);}
    private static Volume selected(ServerPlayer p,ItemStack stack){
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(!p.level().dimension().identifier().toString().equals(tag.getStringOr("VolumeDimension","")))return null;
        try{return get(p.level()).volumes.get(UUID.fromString(tag.getStringOr("VolumeId","")));}catch(IllegalArgumentException ignored){return null;}
    }
    private static void select(ServerPlayer p,ItemStack stack,Volume v){CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{t.putString("VolumeId",v.id.toString());t.putString("VolumeDimension",p.level().dimension().identifier().toString());t.putBoolean("FirstCorner",false);});}
    /** Source-style movable corner: transient preview, explicit commit/cancel, never a write every aim tick. */
    private static final class Connector extends Item {
        Connector(Properties p){super(p);}
        @Override public InteractionResult useOn(UseOnContext c){return c.getPlayer()==null?InteractionResult.FAIL:use(c.getLevel(),c.getPlayer(),c.getHand());}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;if(p.isSpectator())return InteractionResult.FAIL;
            var store=get(p.level());var drag=store.dragging.get(p.getUUID());
            if(drag!=null){
                if(p.isShiftKeyDown()){store.dragging.remove(p.getUUID());p.sendOverlayMessage(Component.literal("Corner edit cancelled."));return InteractionResult.SUCCESS;}
                var live=store.volumes.get(drag.original.id);
                boolean valid=drag.original.equals(live)&&p.level().hasChunkAt(drag.point)&&Authority.mayChange(p.level(),p,drag.point);
                if(valid)valid=store.resize(live.id,p.getUUID(),drag.point,drag.first);
                store.dragging.remove(p.getUUID());p.sendOverlayMessage(Component.literal(valid?"Corner position saved.":"Volume changed or access was revoked; edit cancelled."));return valid?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
            var other=p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
            if(!other.is(TOOL))return InteractionResult.PASS;
            var v=selected(p,other);if(v==null||!v.owner.equals(p.getUUID())||v.machine.isPresent())return InteractionResult.FAIL;
            var hit=p.pick(32,1,false).getLocation();boolean first=net.minecraft.world.phys.Vec3.atCenterOf(v.a).distanceToSqr(hit)<=net.minecraft.world.phys.Vec3.atCenterOf(v.b).distanceToSqr(hit);
            if(store.dragging.size()>=64)return InteractionResult.FAIL;
            store.dragging.put(p.getUUID(),new Drag(v,first,first?v.a:v.b));
            p.sendOverlayMessage(Component.literal("Moving corner "+(first?"A":"B")+". Aim, then use to save; sneak-use cancels."));return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){
            out.accept(Component.literal("Hold a selected Volume Box in your other hand. Aim near a corner and use to move it."));
            out.accept(Component.literal("Aim up to 32 blocks; use to confirm, sneak-use to cancel. Linked volumes must be disconnected."));
        }
    }
    private static final class PlannerTool extends Item {
        PlannerTool(Properties p){super(p);}
        private int pattern(ItemStack s){return Math.clamp(s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("Pattern",0),0,8);}
        private boolean inverted(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("Inverted",false);}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;if(p.isSpectator())return InteractionResult.FAIL;
            var stack=p.getItemInHand(hand);var other=p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
            if(other.is(TOOL)){
                var volume=selected(p,other);if(volume==null||!get(p.level()).plan(volume.id,p.getUUID(),pattern(stack),inverted(stack)))return InteractionResult.FAIL;
                p.sendOverlayMessage(Component.literal("Saved volume plan: "+FillerPatterns.NAMES.get(pattern(stack))+(inverted(stack)?" (inverted)":"")));return InteractionResult.SUCCESS;
            }
            int next=(pattern(stack)+1)%9;boolean invert=!inverted(stack);
            CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{if(p.isShiftKeyDown())t.putBoolean("Inverted",invert);else t.putInt("Pattern",next);});
            p.sendOverlayMessage(Component.literal(FillerPatterns.NAMES.get(pattern(stack))+(inverted(stack)?" (inverted)":"")));return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!(p.level().getBlockEntity(c.getClickedPos()) instanceof CivilWorkshops.Node n)||!n.kind().equals("filler"))return InteractionResult.PASS;
            if(!n.permitted(p)||n.running)return InteractionResult.FAIL;
            n.fillerPattern=pattern(c.getItemInHand());n.invertPattern=inverted(c.getItemInHand());n.cursor=0;n.setChanged();
            p.sendOverlayMessage(Component.literal("Filler plan applied. Start the workshop when ready."));return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){
            out.accept(Component.literal(FillerPatterns.NAMES.get(pattern(s))+(inverted(s)?" (inverted)":"")));
            out.accept(Component.literal("Use to choose shape; sneak-use toggles inversion. Use on a paused filler."));
            out.accept(Component.literal("Hold a selected Volume Box in the other hand and use to save its plan."));
        }
    }
    private static final class Tool extends Item {
        Tool(Properties p){super(p);}
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            var at=c.getClickedPos();if(!p.isAlive()||p.isSpectator()||!Authority.mayChange(p.level(),p,at))return InteractionResult.FAIL;
            var stack=c.getItemInHand();var store=get(p.level());var v=selected(p,stack);
            if(v!=null&&!v.owner.equals(p.getUUID()))return InteractionResult.FAIL;
            if(v!=null&&p.level().getBlockEntity(at) instanceof CivilWorkshops.Node machine){
                if(!store.bind(v.id,p,machine)){p.sendOverlayMessage(Component.literal("Pause the workshop and disconnect any previous volume binding first."));return InteractionResult.FAIL;}
                if(machine.kind().equals("filler")){machine.fillerPattern=v.pattern;machine.invertPattern=v.inverted;machine.setChanged();}
                p.sendOverlayMessage(Component.literal("Saved volume applied. Open the workshop to start."));return InteractionResult.SUCCESS;
            }
            if(v==null){
                // Select an existing owned corner instead of creating overlapping references.
                for(var old:store.volumes.values())if(old.owner.equals(p.getUUID())&&(old.a.equals(at)||old.b.equals(at))){select(p,stack,old);p.sendOverlayMessage(Component.literal("Saved volume selected."));return InteractionResult.SUCCESS;}
                v=store.create(p.getUUID(),at);if(v==null){p.sendOverlayMessage(Component.literal("Volume limit reached: 16 per player, 1024 per dimension."));return InteractionResult.FAIL;}select(p,stack,v);
            }else{
                boolean first=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("FirstCorner",false);
                if(!store.resize(v.id,p.getUUID(),at,first)){p.sendOverlayMessage(Component.literal("Volume must be unlinked and fit 32 × 32 × 32 blocks."));return InteractionResult.FAIL;}
            }
            p.sendOverlayMessage(Component.literal("Corner saved. Use air to switch corners; sneak-use air to deselect, or sneak with empty other hand to delete."));return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;if(p.isSpectator())return InteractionResult.FAIL;
            var stack=p.getItemInHand(hand);var v=selected(p,stack);if(v==null||!v.owner.equals(p.getUUID()))return InteractionResult.PASS;
            if(p.isShiftKeyDown()){
                boolean delete=p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND).isEmpty();
                var other=p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
                if(other.is(CivilWorkshops.AREA_MARKER)){
                    boolean released=get(p.level()).disconnect(v,p);p.sendOverlayMessage(Component.literal(released?"Volume disconnected; editing unlocked.":"Load and approach the linked workshop, then pause it before disconnecting."));return released?InteractionResult.SUCCESS:InteractionResult.FAIL;
                }
                if(delete&&!get(p.level()).remove(v.id,p.getUUID())){p.sendOverlayMessage(Component.literal("Volume is linked. Sneak-use with a Volume Marker in the other hand to disconnect first."));return InteractionResult.FAIL;}
                CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{t.remove("VolumeId");t.remove("VolumeDimension");});
                p.sendOverlayMessage(Component.literal(delete?"Volume deleted; no world blocks changed.":"Volume deselected; saved in this dimension."));
            }else{
                boolean first=!stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("FirstCorner",false);
                CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putBoolean("FirstCorner",first));p.sendOverlayMessage(Component.literal("Next block click moves corner "+(first?"A":"B")));
            }return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){
            out.accept(Component.literal("Persistent 32³ survey. Click blocks to create/resize; use air to switch corners."));
            out.accept(Component.literal("Use on a civil workshop to apply. Sneak-use with occupied other hand deselects; empty deletes."));
            out.accept(Component.literal("Linked volumes are locked. Pause workshop; sneak-use with Volume Marker in other hand to disconnect."));
            out.accept(Component.literal("Unselected volumes can be selected again by clicking either saved corner."));
        }
    }
}
