package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

/** Original field-tool art, bounded item filters and survey marks on the native workshop systems. */
public final class FactoryFieldTools {
    public static Item LIST,MAP,GOGGLES,WATERPROOFING;
    public static final Map<String,Item> GEARS=new LinkedHashMap<>();
    private FactoryFieldTools(){}
    public static void register(){
        FactoryPaintbrush.register();
        FactoryVolumes.register();
        FactoryDiagnostics.register();
        FactoryGuide.register();
        FactoryDecor.register();
        FactorySurveyMarkers.register();
        var sealKey=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_pipe_waterproofing"));
        WATERPROOFING=Registry.register(BuiltInRegistries.ITEM,sealKey,new Item(new Item.Properties().setId(sealKey)));
        for(String kind:List.of("wood","stone","gold","diamond")){
            var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_"+kind+"_gear"));GEARS.put(kind,Registry.register(BuiltInRegistries.ITEM,key,new Item(new Item.Properties().setId(key))));
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(o->{GEARS.values().forEach(o::accept);o.accept(WATERPROOFING);});
        LIST=put("factory_list",0);MAP=put("map_location",1);GOGGLES=put("factory_goggles",2);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(o->{o.accept(LIST);o.accept(MAP);o.accept(GOGGLES);});
        ServerTickEvents.END_SERVER_TICK.register(s->{if(s.getTickCount()%10==0)for(var p:s.getPlayerList().getPlayers())inspect(p);});
    }
    private static Item put(String id,int kind){var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_"+id));var properties=new Item.Properties().setId(key).stacksTo(1);if(kind==2)properties.component(DataComponents.EQUIPPABLE,Equippable.builder(EquipmentSlot.HEAD).setDamageOnHurt(false).build());return Registry.register(BuiltInRegistries.ITEM,key,new Tool(properties,kind));}
    public static List<String> entries(ItemStack list){var tag=list.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var out=new ArrayList<String>();for(int i=0;i<9;i++){String id=tag.getStringOr("filter"+i,"");if(Identifier.tryParse(id)!=null)out.add(id);}return out;}
    public static boolean matches(ItemStack sample,Item item){return sample.is(LIST)?entries(sample).contains(BuiltInRegistries.ITEM.getKey(item).toString()):sample.is(item);}
    private static void frame(ItemStack stack,int n){stack.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(List.of((float)n),List.of(),List.of(),List.of()));}
    private static final class Tool extends Item {
        private final int kind;
        Tool(Properties p,int kind){super(p);this.kind=kind;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(kind==2)return super.use(level,player,hand);
            if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            var stack=p.getItemInHand(hand);if(p.isSpectator())return InteractionResult.FAIL;
            if(kind==0){
                var entries=new ArrayList<>(entries(stack));var sample=p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
                if(p.isShiftKeyDown())entries.clear();
                else if(!sample.isEmpty()&&!sample.is(LIST)){
                    String id=BuiltInRegistries.ITEM.getKey(sample.getItem()).toString();if(!entries.remove(id)){if(entries.size()==9){p.sendOverlayMessage(Component.literal("List full: remove a sample or sneak-use to clear."));return InteractionResult.FAIL;}entries.add(id);}
                }
                CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{for(int i=0;i<9;i++)t.remove("filter"+i);for(int i=0;i<entries.size();i++)t.putString("filter"+i,entries.get(i));});frame(stack,entries.isEmpty()?0:1);
                p.sendOverlayMessage(Component.literal(entries.size()+" / 9 filters. Other hand selects a sample; sneak-use clears. Samples are never consumed."));
            }else if(p.isShiftKeyDown()){
                var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();int mode=(data.getIntOr("mode",0)+1)%3;
                CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{t.putInt("mode",mode);t.remove("CornerA");t.remove("CornerB");t.putInt("points",0);});frame(stack,0);
                p.sendOverlayMessage(Component.literal("Survey: "+List.of("single location","area corners","path (up to 16 points)").get(mode)));
            }else p.sendOverlayMessage(Component.literal("Use blocks to survey. Apply a path to your carrier robot; sneak-use it on the robot clears its route. Sneak-use air changes mode."));
            return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult useOn(UseOnContext c){
            if(kind!=1)return InteractionResult.PASS;
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            var pos=c.getClickedPos();var stack=c.getItemInHand();var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();String dim=p.level().dimension().identifier().toString();
            if(p.isSpectator()||!Authority.mayChange(p.level(),p,pos))return InteractionResult.FAIL;
            if(data.contains("CornerA")&&data.contains("CornerB")&&dim.equals(data.getStringOr("Dimension",""))&&p.level().getBlockEntity(pos) instanceof CivilWorkshops.Node machine){
                if(!machine.permitted(p))return InteractionResult.FAIL;
                if(!machine.setArea(BlockPos.of(data.getLongOr("CornerA",0)),BlockPos.of(data.getLongOr("CornerB",0))))return InteractionResult.FAIL;
                p.sendOverlayMessage(Component.literal("Surveyed area applied. Start the workshop when ready."));return InteractionResult.SUCCESS;
            }
            var target=p.level().getBlockState(pos);
            int mode=target.is(FactorySurveyMarkers.VOLUME)?1:target.is(FactorySurveyMarkers.PATH)?2:Math.clamp(data.getIntOr("mode",0),0,2);
            boolean same=dim.equals(data.getStringOr("Dimension",""))&&mode==data.getIntOr("mode",0);int count=same?Math.clamp(data.getIntOr("points",0),0,16):0;
            if(mode==2&&same&&count>0&&data.getLongOr("point"+(count-1),Long.MIN_VALUE)==pos.asLong()){p.sendOverlayMessage(Component.literal("That is already the last path point."));return InteractionResult.SUCCESS;}
            if(mode==2&&count==16){p.sendOverlayMessage(Component.literal("Path full. Change mode to clear the survey."));return InteractionResult.FAIL;}
            final int n=count;CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{
                if(!same){t.remove("CornerA");t.remove("CornerB");t.putInt("points",0);for(int i=0;i<16;i++)t.remove("point"+i);}
                t.putString("Dimension",dim);
                t.putInt("mode",mode);
                if(mode==0){t.putLong("point0",pos.asLong());t.putInt("points",1);}
                else if(mode==1){if(!same||!t.contains("CornerA")||t.contains("CornerB")){t.putLong("CornerA",pos.asLong());t.remove("CornerB");}else t.putLong("CornerB",pos.asLong());}
                else {t.putLong("point"+n,pos.asLong());t.putInt("points",n+1);}
            });frame(stack,mode+1);p.sendOverlayMessage(Component.literal("Recorded "+pos.toShortString()+(mode==2?" · point "+(count+1):"")));return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,java.util.function.Consumer<Component> out,TooltipFlag flag){
            if(kind==0){out.accept(Component.literal("Place in a filter sample slot. Other hand + use adds/removes an item."));for(String id:entries(stack))out.accept(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id))).getHoverName());}
            else if(kind==1){var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();out.accept(Component.literal(data.getStringOr("Dimension","No survey yet")));if(data.contains("CornerA"))out.accept(Component.literal("A: "+BlockPos.of(data.getLongOr("CornerA",0)).toShortString()));if(data.contains("CornerB"))out.accept(Component.literal("B: "+BlockPos.of(data.getLongOr("CornerB",0)).toShortString()));for(int i=0;i<Math.clamp(data.getIntOr("points",0),0,16);i++)out.accept(Component.literal((i+1)+": "+BlockPos.of(data.getLongOr("point"+i,0)).toShortString()));}
            else out.accept(Component.literal("Wear to inspect nearby workshop energy and fluid ports. No armor value."));
        }
    }
    private static void inspect(ServerPlayer p){
        if(!p.getItemBySlot(EquipmentSlot.HEAD).is(GOGGLES)||p.isSpectator())return;
        var hit=p.level().clip(new ClipContext(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(8)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
        if(hit.getType()!=HitResult.Type.BLOCK||!p.level().hasChunkAt(hit.getBlockPos())||!Authority.mayChange(p.level(),p,hit.getBlockPos()))return;
        var energy=io.github.jimbozoomer.jugcraft.energy.EnergyStorage.SIDED.find(p.level(),hit.getBlockPos(),hit.getDirection());
        if(energy!=null){p.sendOverlayMessage(Component.literal("Energy: "+energy.getAmount()+" / "+energy.getCapacity()+" JE"));return;}
        var fluid=net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(p.level(),hit.getBlockPos(),hit.getDirection());
        if(fluid!=null){int seen=0;for(var view:fluid){if(++seen>8)break;if(!view.isResourceBlank()){p.sendOverlayMessage(Component.literal(BuiltInRegistries.FLUID.getKey(view.getResource().getFluid())+": "+view.getAmount()/81+" mB"));return;}}}
    }
}
