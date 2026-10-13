package local.peepo;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
public final class CompanionLunch {
    public static LunchBlock CRATE,COVER;
    public static BlockEntityType<LunchBlockEntity> ENTITY;
    private static LunchBlock block(String name,boolean cover){
        var id=PeepoMod.id(name);
        var block=Registry.register(BuiltInRegistries.BLOCK,id,new LunchBlock(Block.Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).strength(cover?.3F:1.5F).sound(SoundType.WOOD).noOcclusion(),cover));
        var item=Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(block,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e->e.accept(item));return block;
    }
    public static void initialize(){
        CRATE=block("lunch_crate",false);COVER=block("lunch_cover",true);
        ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,PeepoMod.id("lunch_source"),FabricBlockEntityTypeBuilder.create(LunchBlockEntity::new,CRATE,COVER).build());
        ItemStorage.SIDED.registerForBlocks((level,pos,state,be,side)->be instanceof LunchBlockEntity lunch && !lunch.cover()?lunch.inventory:null,CRATE);
    }
}
