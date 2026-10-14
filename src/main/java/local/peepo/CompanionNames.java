package local.peepo;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.properties.*;

/** Bounded display-only labels. Names never change saved assignment/transport identity. */
public final class CompanionNames {
    public static final int LIMIT=48;
    public static String clean(String text){var out=new StringBuilder();text.codePoints().filter(c->!Character.isISOControl(c) && Character.getType(c)!=Character.FORMAT && c!=0xA7).takeWhile(c->out.length()+Character.charCount(c)<=LIMIT).forEach(out::appendCodePoint);return out.toString().strip();}
    static String container(Level level,CompanionAssignments.Target target){
        if(!target.present(level))return target.name();
        var positions=new ArrayList<BlockPos>(2);var anchor=target.at().pos();positions.add(anchor);var state=level.getBlockState(anchor);
        if(state.getBlock() instanceof ChestBlock && state.getValue(BlockStateProperties.CHEST_TYPE)!=ChestType.SINGLE)positions.add(anchor.relative(ChestBlock.getConnectedDirection(state)));
        for(var pos:positions){if(!level.hasChunkAt(pos))continue;
            for(var side:Direction.values()){
                var signPos=pos.relative(side);if(!level.hasChunkAt(signPos) || !(level.getBlockEntity(signPos) instanceof SignBlockEntity sign))continue;
                var block=level.getBlockState(signPos);
                boolean attached=block.getBlock() instanceof WallSignBlock?signPos.relative(block.getValue(WallSignBlock.FACING).getOpposite()).equals(pos)
                    :block.getBlock() instanceof StandingSignBlock?side==Direction.UP:block.getBlock() instanceof CeilingHangingSignBlock && side==Direction.DOWN;
                if(!attached)continue;
                for(var face:List.of(sign.getText(net.minecraft.world.level.block.entity.SignTextSlot.FRONT),sign.getText(net.minecraft.world.level.block.entity.SignTextSlot.BACK))){
                    var lines=new ArrayList<String>(4);for(int i=0;i<4;i++){var line=clean(face.getMessages(false).get(i).getString());if(!line.isBlank())lines.add(line);}
                    var name=clean(String.join(" ",lines));if(!name.isEmpty())return name;
                }
            }
        }
        for(var pos:positions)if(level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof Nameable named && named.hasCustomName())return clean(named.getName().getString());
        return target.name();
    }
}
