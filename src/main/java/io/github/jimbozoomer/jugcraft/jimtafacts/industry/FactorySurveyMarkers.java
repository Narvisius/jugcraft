package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;

/** Physical anchors; the notebook owns route order, never a global nearest-marker scan. */
public final class FactorySurveyMarkers {
    public static Block VOLUME, PATH;
    private FactorySurveyMarkers() {}
    public static void register() {
        VOLUME=put("jimta_volume_beacon");PATH=put("jimta_path_beacon");
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->{out.accept(VOLUME);out.accept(PATH);});
    }
    private static Block put(String name) {
        var id=Jugcraft.id(name);var key=ResourceKey.create(Registries.BLOCK,id);
        var block=Registry.register(BuiltInRegistries.BLOCK,key,new Marker(BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_TORCH).noCollision().lightLevel(s->7).setId(key)));
        var item=ResourceKey.create(Registries.ITEM,id);
        Registry.register(BuiltInRegistries.ITEM,item,new BlockItem(block,new Item.Properties().setId(item).useBlockDescriptionPrefix()));
        return block;
    }
    private static final class Marker extends Block {
        private static final VoxelShape SHAPE=Block.box(7,0,7,9,9,9);
        Marker(Properties properties){super(properties);}
        @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    }
}
