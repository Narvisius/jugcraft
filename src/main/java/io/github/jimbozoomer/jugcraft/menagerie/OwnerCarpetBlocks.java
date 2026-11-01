package io.github.jimbozoomer.jugcraft.menagerie;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Native stairs retain neighbor joins; the data stores the exact material beneath the carpet. */
public final class OwnerCarpetBlocks {
    public static final IntegerProperty LIGHT=BlockStateProperties.LEVEL;
    public static final EnumProperty<DyeColor> COLOR=EnumProperty.create("color",DyeColor.class);
    private OwnerCarpetBlocks(){}
    static InteractionResult interact(ItemStack held,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand){
        if(!Menagerie.mayChange(level,player,pos))return InteractionResult.FAIL;
        if(!(level.getBlockEntity(pos) instanceof OwnerAmendmentData data))return InteractionResult.PASS;
        if(held.is(Items.SHEARS)){
            if(!level.isClientSide()&&data.restore(player))held.hurtAndBreak(1,player,hand.asEquipmentSlot());return InteractionResult.SUCCESS;
        }
        if(held.getItem() instanceof DyeItem dye&&state.getValue(COLOR)!=dye.getDefaultInstance().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE)){
            if(!level.isClientSide()){
                var block=BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(dye.getDefaultInstance().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE).getName()+"_carpet"));
                data.decoration=block.defaultBlockState();level.setBlockAndUpdate(pos,state.setValue(COLOR,dye.getDefaultInstance().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE)));data.changed();held.consume(1,player);
            }return InteractionResult.SUCCESS;
        }return InteractionResult.PASS;
    }
    public static final class Slab extends SlabBlock implements EntityBlock {
        Slab(Properties props){super(props);registerDefaultState(defaultBlockState().setValue(COLOR,DyeColor.WHITE).setValue(LIGHT,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(COLOR,LIGHT);}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new OwnerAmendmentData(p,s);}
        @Override protected InteractionResult useItemOn(ItemStack held,BlockState s,Level l,BlockPos p,Player who,InteractionHand hand,BlockHitResult hit){return interact(held,s,l,p,who,hand);}
        @Override protected List<ItemStack> getDrops(BlockState s,LootParams.Builder b){return OwnerAmendmentData.drops(b);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(0,0,0,16,9,16);}
        @Override protected ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s,boolean data){return l.getBlockEntity(p) instanceof OwnerAmendmentData d?new ItemStack(d.base.getBlock()):ItemStack.EMPTY;}
    }
    public static final class Stairs extends StairBlock implements EntityBlock {
        Stairs(Properties props){super(Blocks.OAK_PLANKS.defaultBlockState(),props);registerDefaultState(defaultBlockState().setValue(COLOR,DyeColor.WHITE).setValue(LIGHT,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(COLOR,LIGHT);}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new OwnerAmendmentData(p,s);}
        @Override protected InteractionResult useItemOn(ItemStack held,BlockState s,Level l,BlockPos p,Player who,InteractionHand hand,BlockHitResult hit){return interact(held,s,l,p,who,hand);}
        @Override protected List<ItemStack> getDrops(BlockState s,LootParams.Builder b){return OwnerAmendmentData.drops(b);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){var base=super.getShape(s,l,p,c);return Shapes.or(base,base.move(0,1/16.0,0));}
        @Override protected ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s,boolean data){return l.getBlockEntity(p) instanceof OwnerAmendmentData d?new ItemStack(d.base.getBlock()):ItemStack.EMPTY;}
    }
}
