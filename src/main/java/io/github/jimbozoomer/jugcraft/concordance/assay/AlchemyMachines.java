package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Additional owner-art workshops, paid from the existing essence economy. */
public final class AlchemyMachines {
    public enum Kind { RELAY, FURNACE, PEDESTAL, TORCH, NOVA }
    public static final Map<String,AlchemyMachinesBlock> BLOCKS = new LinkedHashMap<>();
    public static BlockEntityType<AlchemyMachinesBlockEntity> ENTITY;
    public static MenuType<AlchemyMachinesMenu> MENU;
    private AlchemyMachines() { }
    public static void register() {
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("alchemy_machine"),new MenuType<>(AlchemyMachinesMenu::new,FeatureFlags.DEFAULT_FLAGS));
        for(int t=1;t<=3;t++)block("relay_mk"+t,Kind.RELAY,t);
        block("dark_matter_furnace",Kind.FURNACE,1);block("red_matter_furnace",Kind.FURNACE,2);
        block("dark_matter_pedestal",Kind.PEDESTAL,1);block("interdiction_torch",Kind.TORCH,1);
        block("nova_catalyst",Kind.NOVA,1);block("nova_cataclysm",Kind.NOVA,2);
        ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("alchemy_machine"),FabricBlockEntityTypeBuilder.create(AlchemyMachinesBlockEntity::new,BLOCKS.values().toArray(Block[]::new)).build());
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity((be,side)->side==null
            ?net.fabricmc.fabric.api.transfer.v1.storage.Storage.empty():net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(be,side),ENTITY);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e->BLOCKS.values().forEach(e::accept));
    }
    private static void block(String name,Kind kind,int tier) {
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id(name));
        var props=BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.COLOR_BLACK).strength(3.5F,8).sound(SoundType.STONE).noOcclusion()
            .lightLevel(s->kind==Kind.TORCH?12:s.getValue(AlchemyMachinesBlock.ACTIVE)?7:0);
        if(kind==Kind.TORCH)props.noCollision().instabreak();
        var block=new AlchemyMachinesBlock(props,kind,tier);Registry.register(BuiltInRegistries.BLOCK,key,block);BLOCKS.put(name,block);
        var itemKey=ResourceKey.create(Registries.ITEM,Jugcraft.id(name));
        Registry.register(BuiltInRegistries.ITEM,itemKey,new BlockItem(block,new Item.Properties().setId(itemKey).useBlockDescriptionPrefix().stacksTo(1).component(AlchemicalWorkshops.ESSENCE,0)){
            @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> text,TooltipFlag flag){
                text.accept(Component.translatable("tooltip.jugcraft."+name).withStyle(ChatFormatting.GRAY));
                if(kind!=Kind.NOVA)text.accept(Component.translatable("tooltip.jugcraft.workshop.stored",stack.getOrDefault(AlchemicalWorkshops.ESSENCE,0)).withStyle(ChatFormatting.AQUA));
            }
        });
    }
    /** Watch acceleration is exactly one normal furnace pulse; consumers still pay their normal recipe charge. */
    public static boolean bonusWork(ServerPlayer player,BlockPos pos) {
        if(!player.level().hasChunkAt(pos)||!(player.level().getBlockEntity(pos) instanceof AlchemyMachinesBlockEntity be)
                ||be.kind()!=Kind.FURNACE||!be.mayUse(player)||!be.readyOwner())return false;
        return be.bonusWork();
    }
}
