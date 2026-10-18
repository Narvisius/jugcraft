package io.github.jimbozoomer.jugcraft.concordance.assay;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Sync only an open flag; clients interpolate locally. No inventory or owner data is exposed. */
public final class AlchemyLid {
    public float previous,value;
    public void tick(Level level,BlockPos pos,int viewers){
        var state=level.getBlockState(pos);if(!state.hasProperty(BlockStateProperties.OPEN))return;
        if(level.isClientSide()){
            previous=value;value=Math.clamp(value+(state.getValue(BlockStateProperties.OPEN)?.12F:-.12F),0,1);
        }else if(state.getValue(BlockStateProperties.OPEN)!=(viewers>0)){
            level.setBlock(pos,state.setValue(BlockStateProperties.OPEN,viewers>0),Block.UPDATE_CLIENTS);
            level.playSound(null,pos,viewers>0?SoundEvents.CHEST_OPEN:SoundEvents.CHEST_CLOSE,SoundSource.BLOCKS,.4F,1F);
        }
    }
    public float openness(float partial){return previous+(value-previous)*partial;}
}
