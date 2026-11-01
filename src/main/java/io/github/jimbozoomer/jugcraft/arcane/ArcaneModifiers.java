package io.github.jimbozoomer.jugcraft.arcane;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.state.BlockState;

/** Source equipment modifiers without research gates; all effects use the owning player's authority. */
public final class ArcaneModifiers {
    public static DataComponentType<String> MODIFIER;
    private static AttachmentType<List<ItemStack>> HELD;
    private static final Set<UUID> BREAKING=new HashSet<>();
    public static final Map<String,String> ADDITIONS=Map.of("eternal","eternal_stella","aquatic","sea_prism","demolishing","terrastomp_prism","fiery","smelter_prism","magnetized","ferrognetic_mixture","soulbound","soul_binding_crystal");
    private ArcaneModifiers(){}
    static void register(){
        MODIFIER=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("arcane_item_modifier"),DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
        HELD=AttachmentRegistry.<List<ItemStack>>builder().persistent(ItemStack.CODEC.listOf()).copyOnDeath().buildAndRegister(Jugcraft.id("arcane_soulbound_kept"));
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("arcane_modifier"),ArcaneModifierRecipe.SERIALIZER);
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers()){
            if(!p.isAlive()||p.isSpectator())continue;
            var held=p.getAttached(HELD);if(held!=null&&!held.isEmpty()){
                var remaining=new ArrayList<ItemStack>();for(var saved:held){var stack=saved.copy();p.getInventory().add(stack);if(!stack.isEmpty())remaining.add(stack);}
                if(remaining.isEmpty())p.removeAttached(HELD);else p.setAttached(HELD,List.copyOf(remaining));
            }
            if(p.tickCount%100==0&&has(p.getItemBySlot(EquipmentSlot.HEAD),"aquatic")&&p.isEyeInFluid(FluidTags.WATER)&&p.getHealth()<p.getMaxHealth())p.heal(2);
            if(p.tickCount%4==0&&!p.isPassenger()&&has(p.getItemBySlot(EquipmentSlot.FEET),"magnetized")){
                int count=0;for(var item:p.level().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(2.5,1.5,2.5))){if(++count>32)break;if(item.isRemoved()||item.hasPickUpDelay()||!Authority.mayChange(p.level(),p,item.blockPosition())||!ClaimTransfers.allowed(p.level(),item.blockPosition(),p.blockPosition()))continue;item.playerTouch(p);}
            }
        }});
        PlayerBlockBreakEvents.AFTER.register((raw,player,pos,state,be)->{
            if(!(player instanceof ServerPlayer p)||!has(p.getMainHandItem(),"demolishing")||p.isShiftKeyDown()||!BREAKING.add(p.getUUID()))return;
            try{
                var tool=p.getMainHandItem();Direction.Axis axis=Math.abs(p.getXRot())>45?Direction.Axis.Y:p.getDirection().getAxis();
                for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++){
                    if(a==0&&b==0)continue;if(p.getMainHandItem()!=tool||tool.isEmpty())return;
                    var at=axis==Direction.Axis.Y?pos.offset(a,0,b):axis==Direction.Axis.X?pos.offset(0,a,b):pos.offset(a,b,0);
                    if(!p.level().hasChunkAt(at)||!p.level().getBlockState(at).is(state.getBlock())||p.level().getBlockEntity(at)!=null||!Authority.mayChange(p.level(),p,at))continue;
                    p.gameMode.destroyBlock(at);
                }
            }finally{BREAKING.remove(p.getUUID());}
        });
    }
    public static boolean has(ItemStack stack,String name){return MODIFIER!=null&&stack.getOrDefault(MODIFIER,"").equals(name);}
    public static boolean applicable(ItemStack stack,String name){
        if(stack.isEmpty()||!ADDITIONS.containsKey(name)||stack.has(MODIFIER)||stack.is(TagKey.create(Registries.ITEM,Jugcraft.id("forbidden_modifier/"+name+"_incompatible"))))return false;
        for(var enchantment:stack.getEnchantments().entrySet())if(enchantment.getKey().is(TagKey.create(Registries.ENCHANTMENT,Jugcraft.id("forbidden_modifier/"+name+"_incompatible"))))return false;
        return switch(name){
            case "eternal"->stack.has(DataComponents.MAX_DAMAGE)&&stack.has(DataComponents.DAMAGE);
            case "aquatic"->stack.is(ItemTags.HEAD_ARMOR);case "magnetized"->stack.is(ItemTags.FOOT_ARMOR);
            case "demolishing","fiery"->stack.is(ItemTags.PICKAXES)||stack.is(ItemTags.AXES)||stack.is(ItemTags.SHOVELS)||stack.is(ItemTags.HOES);
            case "soulbound"->stack.is(TagKey.create(Registries.ITEM,Jugcraft.id("forbidden_modifier/soulbound_applicable")));
            default->false;
        };
    }
    static ItemStack apply(ItemStack source,String name){if(!applicable(source,name))return ItemStack.EMPTY;var out=source.copyWithCount(1);out.set(MODIFIER,name);if(name.equals("eternal")){out.remove(DataComponents.DAMAGE);out.remove(DataComponents.MAX_DAMAGE);}return out;}
    public static boolean capture(ServerPlayer p,ItemStack stack){if(!has(stack,"soulbound")||!io.github.jimbozoomer.jugcraft.grave.DeathGraves.eligible(p))return false;var saved=new ArrayList<>(p.getAttachedOrElse(HELD,List.of()));saved.add(stack.copy());p.setAttached(HELD,List.copyOf(saved));return true;}
    public static List<ItemStack> drops(ServerLevel l,Entity actor,ItemInstance instance,List<ItemStack> drops){
        if(!(instance instanceof ItemStack tool)||!has(tool,"fiery")||!(actor instanceof ServerPlayer))return drops;
        var out=new ArrayList<ItemStack>();for(var drop:drops){if(drop.isEmpty())continue;var in=new SingleRecipeInput(drop.copyWithCount(1));var smelt=l.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING,in,l).map(r->r.value().assemble(in)).orElse(ItemStack.EMPTY);if(smelt.isEmpty())out.add(drop);else{int count=Math.min(4096,smelt.getCount()*drop.getCount());while(count>0){int n=Math.min(count,smelt.getMaxStackSize());out.add(smelt.copyWithCount(n));count-=n;}}}return out;
    }
}
