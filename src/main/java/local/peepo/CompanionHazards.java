package local.peepo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** Shared foot/body checks for paths and precise job positions, with no world scans or chunk loads. */
final class CompanionHazards {
    private static final TagKey<Block> ALWAYS=TagKey.create(Registries.BLOCK,PeepoMod.id("harmful_blocks"));
    private static final TagKey<Block> WHEN_LIT=TagKey.create(Registries.BLOCK,PeepoMod.id("harmful_when_lit"));
    private CompanionHazards(){}
    static boolean harmful(PeepoEntity npc,BlockState state){
        return npc.getType().isBlockDangerous(state) || state.getFluidState().is(FluidTags.LAVA) || state.is(ALWAYS)
            || state.is(WHEN_LIT) && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
    }
    static boolean safeAt(PeepoEntity npc,Vec3 point){
        double radius=npc.getBbWidth()/2+.01;
        var min=BlockPos.containing(point.x-radius,point.y-.01,point.z-radius);
        var max=BlockPos.containing(point.x+radius,point.y+npc.getBbHeight()-.001,point.z+radius);
        for(var pos:BlockPos.betweenClosed(min,max))
            if(!npc.level().hasChunkAt(pos) || harmful(npc,npc.level().getBlockState(pos)))return false;
        return true;
    }
}
