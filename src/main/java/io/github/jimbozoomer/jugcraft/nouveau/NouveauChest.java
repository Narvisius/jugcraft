package io.github.jimbozoomer.jugcraft.nouveau;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
/** Native single/double chest behavior and lid animation, using the owner's original atlases. */
public final class NouveauChest extends ChestBlock {
    public static BlockEntityType<Chest> TYPE;
    private NouveauChest(Properties p){super(()->TYPE,SoundEvents.CHEST_OPEN,SoundEvents.CHEST_CLOSE,p);}
    public static void register(){var entry=NouveauCatalog.BLOCK_ENTRIES.get("archwood_chest");var block=NouveauCatalog.registerBlock(entry,BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD).noOcclusion(),NouveauChest::new);TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("nouveau_archwood_chest"),FabricBlockEntityTypeBuilder.create(Chest::new,block).build());NouveauCatalog.registerBlockItem(entry);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Chest(pos,state);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){if(player instanceof ServerPlayer p){if(!Authority.mayChange(p.level(),p,pos))return InteractionResult.FAIL;if(state.getValue(TYPE_PROPERTY)!=net.minecraft.world.level.block.state.properties.ChestType.SINGLE&&!Authority.mayChange(p.level(),p,ChestBlock.getConnectedBlockPos(pos,state)))return InteractionResult.FAIL;}return super.useWithoutItem(state,level,pos,player,hit);}
    private static final net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.world.level.block.state.properties.ChestType> TYPE_PROPERTY=ChestBlock.TYPE;
    public static final class Chest extends ChestBlockEntity {public Chest(BlockPos pos,BlockState state){super(TYPE,pos,state);}@Override protected net.minecraft.network.chat.Component getDefaultName(){return net.minecraft.network.chat.Component.literal("Archwood Chest");}}
}
