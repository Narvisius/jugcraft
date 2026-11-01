package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Floating flower variants retain their normal workshop behavior without requiring supporting soil. */
public final class FloatingBotanicalBlock extends BotanicalBlock {
    public static final IntegerProperty ISLAND=IntegerProperty.create("island",0,9);
    public static final java.util.List<String> ISLANDS=java.util.List.of("grass","snow","podzol","mycel","dry","golden","vivid","scorched","infused","mutated");
    public FloatingBotanicalBlock(String kind,Properties p){super(kind,p);registerDefaultState(defaultBlockState().setValue(ISLAND,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(ISLAND);}
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        int island=stack.is(Items.SNOWBALL)?1:BotanicalLandscaping.island(stack);
        if(island>=0&&island!=state.getValue(ISLAND)){
            if(player instanceof ServerPlayer p){
                if(!Authority.mayChange(p.level(),p,pos))return InteractionResult.FAIL;
                level.setBlockAndUpdate(pos,state.setValue(ISLAND,island));if(!p.hasInfiniteMaterials())stack.shrink(1);
            }return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack,state,level,pos,player,hand,hit);
    }
}
