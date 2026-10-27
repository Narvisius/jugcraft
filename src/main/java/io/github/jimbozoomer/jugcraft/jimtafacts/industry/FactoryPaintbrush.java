package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Supplied 17 brush sprites; finite dye loads and live, saved factory-pipe color boundaries. */
public final class FactoryPaintbrush extends Item {
    public static Item BRUSH,MARKER;
    public static final int LOAD=64;
    private static final List<String> FAMILIES=List.of("wool","carpet","concrete","concrete_powder","terracotta","glazed_terracotta","stained_glass","stained_glass_pane");
    private FactoryPaintbrush(Properties properties){super(properties);}
    public static void register(){
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_paintbrush"));
        BRUSH=Registry.register(BuiltInRegistries.ITEM,key,new FactoryPaintbrush(new Properties().setId(key).stacksTo(1)));
        var marker=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_pipe_color_marker"));
        // Render-only model carrier. No recipe, creative entry or survival drop.
        MARKER=Registry.register(BuiltInRegistries.ITEM,marker,new Item(new Properties().setId(marker)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(o->{
            o.accept(BRUSH);for(var color:DyeColor.values()){var s=new ItemStack(BRUSH);set(s,color.getId(),LOAD);o.accept(s);}
        });
    }
    public static int color(ItemStack stack){var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();return uses(stack)==0?-1:Math.clamp(tag.getIntOr("paint_color",-1),-1,15);}
    public static int uses(ItemStack stack){return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("paint_uses",0),0,LOAD);}
    public static void set(ItemStack stack,int color,int uses){
        int count=color<0?0:Math.clamp(uses,0,LOAD),shade=count==0?-1:Math.clamp(color,0,15);
        CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{t.putInt("paint_color",shade);t.putInt("paint_uses",count);});
        stack.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(List.of((float)(shade+1)),List.of(),List.of(),List.of()));
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(p.isSpectator()||!p.isAlive())return InteractionResult.FAIL;
        var stack=p.getItemInHand(hand);
        if(p.isShiftKeyDown()){set(stack,-1,0);p.sendOverlayMessage(Component.literal("Brush cleaned. Use it on a pipe or insulated cable to remove its paint."));return InteractionResult.SUCCESS;}
        var dye=p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
        if(dye.getItem() instanceof DyeItem){
            int shade=dye.getOrDefault(DataComponents.DYE,DyeColor.WHITE).getId();
            if(color(stack)!=shade||uses(stack)!=LOAD){set(stack,shade,LOAD);dye.consume(1,p);}
            p.sendOverlayMessage(Component.literal("Loaded "+DyeColor.byId(shade).getName().replace('_',' ')+": 64 applications."));
        }else p.sendOverlayMessage(Component.literal("Hold dye in the other hand and use to load 64 applications. Sneak-use air cleans the brush."));
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult useOn(UseOnContext c){
        if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        var at=c.getClickedPos();var level=p.level();var stack=c.getItemInHand();
        if(!p.isAlive()||p.isSpectator()||!level.hasChunkAt(at)||!Authority.mayChange(level,p,at))return InteractionResult.FAIL;
        int shade=color(stack);boolean changed=false;var before=level.getBlockState(at);var fitting=TransportAttachments.at(level,at);
        if(fitting!=null){if(fitting.paint!=shade){fitting.paint=shade;fitting.changed();changed=true;}}
        else if(before.getBlock() instanceof IndustrialCables.Painted){
            if(before.getValue(IndustrialCables.COLOR)!=shade+1)changed=level.setBlock(at,before.setValue(IndustrialCables.COLOR,shade+1),Block.UPDATE_CLIENTS);
        }else if(shade>=0&&!before.hasBlockEntity()){
            var after=recolor(before,DyeColor.byId(shade));
            if(after!=null&&!after.equals(before))changed=level.setBlock(at,after,Block.UPDATE_ALL);
        }
        // Consume this block interaction even on a no-op; falling through to air-use could refill and waste another dye.
        if(!changed)return InteractionResult.SUCCESS;
        if(shade>=0&&!p.getAbilities().instabuild)set(stack,shade,uses(stack)-1);
        return InteractionResult.SUCCESS;
    }
    private static BlockState recolor(BlockState before,DyeColor dye){
        var id=BuiltInRegistries.BLOCK.getKey(before.getBlock());if(!id.getNamespace().equals("minecraft"))return null;
        String family=switch(id.getPath()){case "glass"->"stained_glass";case "glass_pane"->"stained_glass_pane";case "terracotta"->"terracotta";default->null;};
        if(family==null)for(var old:DyeColor.values())for(String f:FAMILIES)if(id.getPath().equals(old.getName()+"_"+f))family=f;
        if(family==null)return null;
        var target=Identifier.withDefaultNamespace(dye.getName()+"_"+family);
        if(!BuiltInRegistries.BLOCK.containsKey(target))return null;
        return BuiltInRegistries.BLOCK.getValue(target).withPropertiesOf(before);
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){
        int shade=color(s);out.accept(Component.literal(shade<0?"Clean brush":DyeColor.byId(shade).getName().replace('_',' ')+" · "+uses(s)+" / 64 uses"));
        out.accept(Component.literal("Dye in other hand + use: refill. Sneak-use air: discard paint."));
        out.accept(Component.literal("Different painted factory pipes disconnect; unpainted pipes bridge colors."));
        out.accept(Component.literal("Insulated JE cable colors are labels only. Also paints vanilla decorative color families."));
    }
}
