package local.peepo;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class CraftingTables {
    public static BlockEntityType<CraftingTableCompanionBlockEntity> ENTITY;
    public static void initialize(){
        ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,PeepoMod.id("crafting_table_workspace"),FabricBlockEntityTypeBuilder.create(CraftingTableCompanionBlockEntity::new,Blocks.CRAFTING_TABLE).build());
        UseBlockCallback.EVENT.register((player,level,hand,hit)->{
            if(hand!=InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty() || player.isSpectator() || !level.hasChunkAt(hit.getBlockPos()))return InteractionResult.PASS;
            if(!(level.getBlockEntity(hit.getBlockPos()) instanceof CraftingTableCompanionBlockEntity table) || !table.recoverable()
                || !level.mayInteract(player,hit.getBlockPos()) || !io.github.jimbozoomer.jugcraft.finance.LandRegistry.mayBuild(player,level,hit.getBlockPos())
                || io.github.jimbozoomer.jugcraft.town.TownProtection.denies(player,level,hit.getBlockPos()))return InteractionResult.PASS;
            if(!level.isClientSide())table.recover(player);return InteractionResult.SUCCESS;
        });
    }
}
