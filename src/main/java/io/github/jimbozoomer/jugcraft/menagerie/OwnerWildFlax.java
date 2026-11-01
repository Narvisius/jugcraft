package io.github.jimbozoomer.jugcraft.menagerie;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Source sand-bank habitat and bounded bonemeal spreading. */
final class OwnerWildFlax extends VegetationBlock implements BonemealableBlock {
    OwnerWildFlax(Properties p){super(p);}
    @Override protected boolean mayPlaceOn(BlockState s,BlockGetter l,BlockPos p){return s.is(BlockTags.SAND);}
    @Override public boolean isValidBonemealTarget(LevelReader l,BlockPos p,BlockState s,BonemealSource source){return true;}
    @Override public boolean isBonemealSuccess(Level l,RandomSource r,BlockPos p,BlockState s,BonemealSource source){return r.nextFloat()<.8F;}
    @Override public void performBonemeal(ServerLevel l,RandomSource r,BlockPos p,BlockState s,BonemealSource source){
        int count=0;for(var q:BlockPos.betweenClosed(p.offset(-4,-1,-4),p.offset(4,1,4)))if(l.hasChunkAt(q)&&l.getBlockState(q).is(this)&&++count>=10)return;
        var origin=p;for(int i=0;i<5;i++){
            var q=origin.offset(r.nextInt(3)-1,r.nextInt(2)-r.nextInt(2),r.nextInt(3)-1);
            // Never spread a decorative crop into a protected parcel from outside it.
            if(l.hasChunkAt(q)&&l.isEmptyBlock(q)&&s.canSurvive(l,q)&&!io.github.jimbozoomer.jugcraft.town.TownProtection.shieldsBlock(l,q)&&!io.github.jimbozoomer.jugcraft.finance.LandRegistry.crossesBoundary(l,p,q)){if(i==4)l.setBlockAndUpdate(q,s);else origin=q;}
        }
    }
}
