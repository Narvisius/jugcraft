package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Native burst routing conserves unaccepted mana; voids discard overflow and signal devices explicitly consume their triggering burst. */
final class BotanicalRouting {
    private BotanicalRouting(){}
    static boolean receiver(String kind){return kind.startsWith("drum_")||kind.endsWith("pool")||Set.of("mana_fluxfield","avatar","animated_torch","spawner_claw","runic_altar","terra_plate","mana_distributor","mana_void","hourglass","brewery","enchanter").contains(kind);}
    static boolean overflow(BotanicalWorkshop pool,ServerPlayer actor){
        var level=actor.level();var pos=pool.getBlockPos().below();return pool.kind().endsWith("pool")&&level.hasChunkAt(pos)&&level.getBlockState(pos).is(Jugtania.block("mana_void"))&&Authority.mayChange(level,actor,pos)&&ClaimTransfers.allowed(level,pool.getBlockPos(),pos);
    }
    static int room(BotanicalWorkshop target,ServerPlayer actor){if(target.kind().equals("mana_fluxfield"))return (int)((target.flux.getCapacity()-target.flux.getAmount())/3);return target.kind().equals("animated_torch")||target.kind().startsWith("drum_")||target.kind().equals("hourglass")||target.kind().equals("mana_void")||overflow(target,actor)?1000000:target.freeMana();}
    static int receive(BotanicalWorkshop target,ServerPlayer actor,int amount){
        if(amount<=0||!target.permitted(actor))return 0;
        if(target.kind().equals("mana_fluxfield")){int accepted=Math.min(amount,room(target,actor));target.flux.setAmount(target.flux.getAmount()+accepted*3L);if(accepted>0)target.changed();return accepted;}
        if(target.kind().equals("animated_torch")){target.devices.burst(actor);return amount;}
        if(target.kind().startsWith("drum_")){target.triggerDrum(actor);return amount;}
        if(target.kind().equals("hourglass")){target.mechanisms.burst(actor);return amount;}
        int stored=target.kind().equals("mana_void")?0:target.fill(amount);
        if(stored<amount&&(target.kind().equals("mana_void")||overflow(target,actor))){
            var pos=target.kind().equals("mana_void")?target.getBlockPos():target.getBlockPos().below();
            actor.level().sendParticles(ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+.6,pos.getZ()+.5,3,.2,.1,.2,0);return amount;
        }return stored;
    }
    static void split(BotanicalWorkshop splitter,ServerLevel level,ServerPlayer actor){
        if(splitter.mana()<=0)return;var receivers=new ArrayList<BotanicalWorkshop>();
        for(var dir:Direction.Plane.HORIZONTAL){var pos=splitter.getBlockPos().relative(dir);
            if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof BotanicalWorkshop pool&&pool.kind().endsWith("pool")&&pool.permitted(actor)&&ClaimTransfers.allowed(level,splitter.getBlockPos(),pos)&&room(pool,actor)>0)receivers.add(pool);
        }
        int count=receivers.size();for(var pool:receivers){int share=(splitter.mana()+count-1)/count--;if(share<=0)break;
            if(splitter.draw(share)){int moved=receive(pool,actor,share);if(moved<share)splitter.fill(share-moved);}
        }
    }
    static void trigger(ServerLevel level,ServerPlayer actor,BlockPos pos){
        var state=level.getBlockState(pos);
        if(state.getBlock() instanceof Detector&&!state.getValue(BlockStateProperties.POWERED)&&Authority.mayChange(level,actor,pos)){
            level.setBlockAndUpdate(pos,state.setValue(BlockStateProperties.POWERED,true));level.scheduleTick(pos,state.getBlock(),4);
        }
    }
    static final class Detector extends Block {
        Detector(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.POWERED,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.POWERED);}
        @Override protected boolean isSignalSource(BlockState state){return true;}
        @Override protected int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction direction){return state.getValue(BlockStateProperties.POWERED)?15:0;}
        @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){if(state.getValue(BlockStateProperties.POWERED))level.setBlockAndUpdate(pos,state.setValue(BlockStateProperties.POWERED,false));}
    }
}
