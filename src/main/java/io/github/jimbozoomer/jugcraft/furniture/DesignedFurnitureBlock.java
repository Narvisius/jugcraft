package io.github.jimbozoomer.jugcraft.furniture;

import java.util.*;
import io.github.jimbozoomer.jugcraft.menagerie.Menagerie;
import io.github.jimbozoomer.jugcraft.tower.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Designs occupy one block, retain their data when broken and share existing seating/container rules. */
public final class DesignedFurnitureBlock extends BaseEntityBlock implements Sittable {
    public static final EnumProperty<Direction> FACING=HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty LIGHT=IntegerProperty.create("light",0,15);
    public static final BooleanProperty LIT=BlockStateProperties.LIT;
    public DesignedFurnitureBlock(Properties p){super(p.dynamicShape().lightLevel(s->s.getValue(LIT)?s.getValue(LIGHT):0));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(LIGHT,0).setValue(LIT,true));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LIGHT,LIT);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new DesignedFurnitureEntity(p,s);}
    @Override public double seatHeight(){return .625;}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite()).setValue(LIGHT,FurnitureDesign.safe(c.getItemInHand().getOrDefault(ArtisanFurniture.DESIGN,"")).light());}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity who,ItemStack stack){if(l.getBlockEntity(p) instanceof DesignedFurnitureEntity e)e.design(FurnitureDesign.safe(stack.getOrDefault(ArtisanFurniture.DESIGN,"")));}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return l.getBlockEntity(p) instanceof DesignedFurnitureEntity e?e.design().shape(s.getValue(FACING)):Shapes.block();}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return getShape(s,l,p,c);}
    @Override protected ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s,boolean includeData){return ArtisanFurniture.stack(l.getBlockEntity(p) instanceof DesignedFurnitureEntity e?e.design():FurnitureDesign.stool());}
    @Override protected List<ItemStack> getDrops(BlockState s,LootParams.Builder b){var e=b.getOptionalParameter(LootContextParams.BLOCK_ENTITY);return List.of(ArtisanFurniture.stack(e instanceof DesignedFurnitureEntity f?f.design():FurnitureDesign.stool()));}
    @Override protected void affectNeighborsAfterRemoval(BlockState s,ServerLevel l,BlockPos p,boolean moved){Containers.updateNeighboursAfterDestroy(s,l,p);}
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return InteractionResult.TRY_WITH_EMPTY_HAND;}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(!ArtisanFurniture.mayUse(l,player,p))return InteractionResult.FAIL;
        if(l.getBlockEntity(p) instanceof DesignedFurnitureEntity e&&!l.isClientSide()){
            var d=e.design();
            if(player.isShiftKeyDown()&&d.light()>0)l.setBlockAndUpdate(p,s.cycle(LIT));
            else if(d.storage())player.openMenu(e);
            else if(d.seat()>0)SeatEntity.sit(l,p,player,s.getValue(FACING),d.seat()/16D);
            else if(d.light()>0)l.setBlockAndUpdate(p,s.cycle(LIT));
            if(d.sound()>0)l.playSound(null,p,switch(d.sound()){case 1->SoundEvents.NOTE_BLOCK_CHIME.value();case 2->SoundEvents.NOTE_BLOCK_BELL.value();default->SoundEvents.NOTE_BLOCK_HARP.value();},SoundSource.BLOCKS,.3F,1);
        }return InteractionResult.SUCCESS;
    }
    @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource random){
        if(!s.getValue(LIT)||random.nextInt(8)!=0||!(l.getBlockEntity(p) instanceof DesignedFurnitureEntity e))return;
        var d=e.design();if(d.effect()>0)l.addParticle(switch(d.effect()){case 1->ParticleTypes.SMALL_FLAME;case 2->ParticleTypes.ENCHANT;default->ParticleTypes.END_ROD;},p.getX()+.5,p.getY()+.9,p.getZ()+.5,0,.01,0);
    }
}
