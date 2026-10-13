package local.peepo;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;

public final class GeneratorWheel {
    public static WheelBlock BLOCK;
    public static BlockEntityType<WheelBlockEntity> ENTITY;
    public static net.minecraft.world.inventory.MenuType<WheelMenu> MENU;
    public static void initialize() {
        var id=PeepoMod.id("generator_wheel");
        BLOCK=Registry.register(BuiltInRegistries.BLOCK,id,new WheelBlock(Block.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK,id)).strength(2.5F).noOcclusion()));
        var item=Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(BLOCK,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));
        ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id,FabricBlockEntityTypeBuilder.create(WheelBlockEntity::new,BLOCK).build());
        MENU=Registry.register(BuiltInRegistries.MENU,id,new net.minecraft.world.inventory.MenuType<>(WheelMenu::new,net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e->e.accept(item));
        EnergyStorage.SIDED.registerForBlocks((level,pos,state,be,side)-> {
            int part=state.getValue(WheelBlock.PART);
            Direction right=state.getValue(WheelBlock.FACING).getClockWise();
            if(side==null || !(part==0 && side==right.getOpposite() || part==1 && side==right))return null;
            return level.getBlockEntity(WheelBlock.master(pos,state)) instanceof WheelBlockEntity wheel?wheel.output:null;
        },BLOCK);
    }
}
