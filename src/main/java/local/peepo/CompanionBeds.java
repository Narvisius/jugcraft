package local.peepo;

import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class CompanionBeds {
    public static final List<CompanionBedBlock> BLOCKS = new ArrayList<>();
    public static BlockEntityType<CompanionBedEntity> ENTITY;
    public static void initialize() {
        for (DyeColor color : DyeColor.values()) {
            var id = PeepoMod.id(color.getName() + "_companion_bed");
            var block = Registry.register(BuiltInRegistries.BLOCK, id, new CompanionBedBlock(
                Block.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id)).strength(0.8F).sound(SoundType.WOOD).noOcclusion()));
            BLOCKS.add(block);
            var item = Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block,
                new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix()));
            CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e -> e.accept(item));
        }
        ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, PeepoMod.id("companion_bed"),
            FabricBlockEntityTypeBuilder.create(CompanionBedEntity::new, BLOCKS.toArray(Block[]::new)).build());
    }
}
