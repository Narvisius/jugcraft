package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

/** November additions remain craftable and wearable year-round. */
public final class HarvestFestival {
    public static final java.util.Map<String,Item> FOODS=new java.util.LinkedHashMap<>();
    public static Item PILGRIM_COAT, PILGRIM_DRESS;
    public static GiantTurkeyFeastBlock GIANT_TURKEY;
    private static volatile boolean clientCostumes;
    private HarvestFestival(){}
    public static boolean costumesOn(java.time.LocalDate date){return date.getMonthValue()==11;}
    public static boolean clientCostumes(){return clientCostumes;}
    public static void setClientCostumes(boolean active){clientCostumes=active;}
    private static Item item(String name,java.util.function.Function<Item.Properties,Item> factory,Item.Properties properties){
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(name));
        return Registry.register(BuiltInRegistries.ITEM,key,factory.apply(properties.setId(key)));
    }
    public static void register(){
        for(String name:java.util.List.of("mashed_potatoes_and_gravy","harvest_pea_casserole","candied_sweet_potatoes","dinner_roll")){
            var props=new Item.Properties().food(new FoodProperties.Builder().nutrition(name.equals("dinner_roll")?2:8).saturationModifier(.6F).build());
            if(!name.equals("dinner_roll"))props.usingConvertsTo(Items.BOWL).stacksTo(16);
            FOODS.put(name,item(name,Item::new,props));
        }
        var worn=net.minecraft.world.item.equipment.Equippable.builder(net.minecraft.world.entity.EquipmentSlot.HEAD)
            .setEquipSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER).setAsset(ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,Jugcraft.id("harvest_costume"))).build();
        PILGRIM_COAT=item("pilgrim_coat_costume",Item::new,new Item.Properties().stacksTo(1).component(net.minecraft.core.component.DataComponents.EQUIPPABLE,worn));
        PILGRIM_DRESS=item("pilgrim_dress_costume",Item::new,new Item.Properties().stacksTo(1).component(net.minecraft.core.component.DataComponents.EQUIPPABLE,worn));
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("giant_turkey_feast"));
        GIANT_TURKEY=Registry.register(BuiltInRegistries.BLOCK,key,new GiantTurkeyFeastBlock(BlockBehaviour.Properties.of().setId(key).strength(.6F).sound(SoundType.WOOD).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)));
        var feast=item("giant_turkey_feast",p->new BlockItem(GIANT_TURKEY,p){
            @Override public void appendHoverText(ItemStack stack,TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip,TooltipFlag flag){
                super.appendHoverText(stack,context,display,tooltip,flag);
                var saved=stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
                int left=saved==null?GiantTurkeyFeastBlock.PORTIONS:saved.apply(GIANT_TURKEY.defaultBlockState()).getValue(GiantTurkeyFeastBlock.REMAINING);
                tooltip.accept(net.minecraft.network.chat.Component.translatable("tooltip.jugcraft.giant_turkey_feast",left));
                tooltip.accept(net.minecraft.network.chat.Component.translatable("tooltip.jugcraft.giant_turkey_feast.controls"));
            }
        },new Item.Properties().stacksTo(1).useBlockDescriptionPrefix());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(o->{FOODS.values().forEach(o::accept);o.accept(feast);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(o->{o.accept(PILGRIM_COAT);o.accept(PILGRIM_DRESS);});
    }
}
