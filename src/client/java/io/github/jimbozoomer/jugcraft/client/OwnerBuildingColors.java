package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.building.OwnerBuildingCollection;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSources;

/** Supplied hedge/grass pixels already carry their colors; don't tint them twice. */
final class OwnerBuildingColors {
    private OwnerBuildingColors() { }
    static void register() {
        net.minecraft.client.gui.screens.MenuScreens.register(io.github.jimbozoomer.jugcraft.aquatic.OwnerAquaticGear.MENU,io.github.jimbozoomer.jugcraft.client.aquatic.OwnerTackleScreen::new);
        OwnerEnchantmentClient.register();
        OwnerBackpackScreen.registerPreviews();
        OwnerTooltipStyles.register();
        OwnerLootRenderer.register();
        net.minecraft.client.gui.screens.MenuScreens.register(io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks.MENU,OwnerBackpackScreen::new);
        io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks.BLOCKS.values().forEach(block->BlockColorRegistry.register(List.of(
            new net.minecraft.client.color.block.BlockTintSource(){
                public int color(net.minecraft.world.level.block.state.BlockState s){return 0xFF806347;}
                public int colorInWorld(net.minecraft.world.level.block.state.BlockState s,net.minecraft.client.renderer.block.BlockAndTintGetter l,net.minecraft.core.BlockPos p){return l.getBlockEntity(p) instanceof io.github.jimbozoomer.jugcraft.backpack.OwnerBackpackEntity bag?net.minecraft.world.item.component.DyedItemColor.getOrDefault(bag.carrier(),0xFF806347):0xFF806347;}
            },BlockTintSources.constant(0xFF49382D)),block));
        OwnerHomeRenderer.register();
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(net.minecraft.world.level.block.entity.BlockEntityTypes.ENCHANTING_TABLE,OwnerEnchantingRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.building.OwnerTwigs.PEBBLE, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        OwnerBuildingCollection.BLOCKS.forEach((name, block) -> {
            if(block instanceof io.github.jimbozoomer.jugcraft.menagerie.OwnerHomeBlock)
                BlockColorRegistry.register(List.of(new net.minecraft.client.color.block.BlockTintSource(){
                    public int color(net.minecraft.world.level.block.state.BlockState s){return -1;}
                    public int colorInWorld(net.minecraft.world.level.block.state.BlockState s,net.minecraft.client.renderer.block.BlockAndTintGetter level,net.minecraft.core.BlockPos p){
                        if(level.getBlockEntity(p) instanceof io.github.jimbozoomer.jugcraft.menagerie.OwnerHome.Data data&&data.color!=-1)return data.color;
                        return io.github.jimbozoomer.jugcraft.menagerie.HouseholdBlock.value(s,"contains").equals("water")?0xFF3F76E4:-1;
                    }
                }),block);
            if (name.startsWith("fenceworks_") && (name.endsWith("_hedge") || name.endsWith("_grass_topped_wall")))
                BlockColorRegistry.register(List.of(BlockTintSources.constant(0xFFFFFFFF)), block);
        });
    }
}
