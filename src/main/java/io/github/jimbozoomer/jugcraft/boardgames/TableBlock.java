package io.github.jimbozoomer.jugcraft.boardgames;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
public final class TableBlock extends BaseEntityBlock {
 public final BoardPosition.Kind kind;
 public TableBlock(Properties p,BoardPosition.Kind k){super(p);kind=k;}
 @Override protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel level,BlockPos pos,boolean moved){super.affectNeighborsAfterRemoval(state,level,pos,moved);if(level.getBlockState(pos).is(this))return;var ledger=io.github.jimbozoomer.jugcraft.finance.Ledger.get(level.getServer());if(ledger.boardWagers.cancelLocation(ledger.book(),TableEntity.locationPrefix(level,pos)))ledger.setDirty();}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new TableEntity(p,s);}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(0,0,0,16,14,16);}
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player user,BlockHitResult hit){if(user instanceof ServerPlayer player&&l.getBlockEntity(p) instanceof TableEntity table)table.open(player);return InteractionResult.SUCCESS;}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide()?null:createTickerHelper(type,BoardGames.ENTITY,(world,pos,state,table)->table.tick());}
}
