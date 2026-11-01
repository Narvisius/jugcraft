package io.github.jimbozoomer.jugcraft.wilds;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
/** Owner-authored four-symbol rail recognizer, bounded to one vertex transition per tick. */
final class WildRailTune {
    private static final long[] MASK={1151706154984090261L,1149919144941647226L,1152921487426969583L,1152635631447310335L};
    private BlockPos vertex,edge;
    private int ordinal;
    private long bits=0xFFFFFFFFFFFFEL;
    void clear(){vertex=edge=null;ordinal=0;bits=0xFFFFFFFFFFFFEL;}
    boolean visit(Minecart cart){var level=cart.level();var at=cart.blockPosition();if(level.getBlockState(at.below()).is(BlockTags.RAILS))at=at.below();if(!level.getBlockState(at).is(BlockTags.RAILS)){clear();return false;}if(vertex==null){vertex=at;return false;}if(vertex.equals(at))return false;var next=at.subtract(vertex);vertex=at;if(edge==null){edge=next;return false;}
        int turn=1+next.getZ()*edge.getX()-next.getX()*edge.getZ()+((edge.getX()*next.getX()+edge.getZ()*next.getZ())&2);boolean found=false;if(turn>=0&&turn<4&&(turn!=1||turn!=ordinal)){bits=(bits|MASK[turn])<<1;found=(bits&0x10000000000000L)==0;}ordinal=turn;edge=next;return found;
    }
}
