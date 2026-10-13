package local.peepo;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

/** Only explicitly opened path gates are tracked. A saved state bit and scheduled tick survive reloads. */
public final class CompanionGates {
    public static final BooleanProperty PASSAGE=BooleanProperty.create("jugcraft_companion_passage");
    private record Passage(UUID owner,long until){}
    private static final Map<Level,Map<BlockPos,Passage>> OPEN=new WeakHashMap<>();
    private CompanionGates(){}
    public static boolean guarded(BlockState state){return state.hasProperty(PASSAGE) && state.getValue(PASSAGE) && state.getValue(FenceGateBlock.OPEN);}
    public static boolean mayOpen(PeepoEntity npc,BlockPos pos,BlockState state){
        return npc.level() instanceof ServerLevel server && npc.orders.tamed() && state.getBlock() instanceof FenceGateBlock && !state.getValue(FenceGateBlock.POWERED)
            && CompanionJobs.permitted(npc,pos) && server.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING);
    }
    public static void beforeMove(PeepoEntity npc){
        if(!(npc.level() instanceof ServerLevel level) || !npc.isAlive() || !npc.orders.tamed() || level.getGameTime()%4!=Math.floorMod(npc.getId(),4))return;
        var path=npc.getNavigation().getPath();if(path==null || path.isDone())return;
        for(int i=path.getNextNodeIndex();i<Math.min(path.getNodeCount(),path.getNextNodeIndex()+3);i++){
            var pos=path.getNodePos(i);if(pos.distToCenterSqr(npc.position())>4 || !level.hasChunkAt(pos))continue;
            var state=level.getBlockState(pos);if(!mayOpen(npc,pos,state) || state.getValue(FenceGateBlock.OPEN))continue;
            var gates=OPEN.computeIfAbsent(level,l->new HashMap<>());
            if(gates.size()>=128)gates.entrySet().removeIf(e->e.getValue().until<=level.getGameTime());
            if(gates.size()>=128)return;
            gates.put(pos.immutable(),new Passage(npc.getUUID(),level.getGameTime()+40));
            level.setBlock(pos,state.setValue(FenceGateBlock.OPEN,true).setValue(PASSAGE,true),Block.UPDATE_ALL);
            level.playSound(null,pos,SoundEvents.FENCE_GATE_OPEN,SoundSource.BLOCKS,.5F,1);
            level.scheduleTick(pos,state.getBlock(),4);return;
        }
    }
    public static void scheduled(BlockState state,ServerLevel level,BlockPos pos){
        var gates=OPEN.get(level);var passage=gates==null?null:gates.get(pos);
        if(!guarded(state)){
            if(state.hasProperty(PASSAGE) && state.getValue(PASSAGE))level.setBlock(pos,state.setValue(PASSAGE,false),Block.UPDATE_ALL);
            if(gates!=null)gates.remove(pos);return;
        }
        if(state.getValue(FenceGateBlock.POWERED)){
            level.setBlock(pos,state.setValue(PASSAGE,false),Block.UPDATE_ALL);if(gates!=null)gates.remove(pos);return;
        }
        boolean keep=false;
        if(passage!=null && passage.until>level.getGameTime() && level.getEntity(passage.owner) instanceof PeepoEntity npc && npc.isAlive()){
            keep=level.hasEntities(EntityTypeTest.forClass(PeepoEntity.class),new AABB(pos).inflate(.3),p->p.isAlive())
                || !npc.getNavigation().isDone() && pos.distToCenterSqr(npc.position())<4;
        }
        if(keep){level.scheduleTick(pos,state.getBlock(),4);return;}
        level.setBlock(pos,state.setValue(FenceGateBlock.OPEN,false).setValue(PASSAGE,false),Block.UPDATE_ALL);
        level.playSound(null,pos,SoundEvents.FENCE_GATE_CLOSE,SoundSource.BLOCKS,.5F,1);
        if(gates!=null)gates.remove(pos);
    }
}
