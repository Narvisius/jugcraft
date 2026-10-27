package io.github.jimbozoomer.jugcraft.control;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * A control network (batch 36): the data cables joined to a block (a logic controller, or a monitor looking for
 * one), and the sensors, switches (relays and alarms) and controllers touching them or that block. Found by a walk
 * along the cables, at most {@link #MAX_CABLES} long; devices do not pass the network on, so two separate cable runs stay separate. Unloaded chunks end the walk.
 */
public final class ControlNetwork {
	public static final int MAX_CABLES = 1_024;

	public record Devices(List<BlockPos> sensors, List<BlockPos> switches, List<BlockPos> controllers, int cables) {
	}

	private ControlNetwork() {
	}

	private record Step(BlockPos pos,int channels){}
    public static Devices find(Level level,BlockPos controller) {
        var sensors=new java.util.LinkedHashSet<BlockPos>();var switches=new java.util.LinkedHashSet<BlockPos>();var controllers=new java.util.LinkedHashSet<BlockPos>();
        var seen=new java.util.HashMap<BlockPos,Integer>();var queue=new ArrayDeque<Step>();var cables=new HashSet<BlockPos>();
        seen.put(controller,65535);queue.add(new Step(controller,65535));int visits=0;
        while(!queue.isEmpty()&&visits++<MAX_CABLES*16+1){
            var step=queue.removeFirst();
            for(var direction:Direction.values()){
                var next=step.pos.relative(direction);
                if(!level.isLoaded(next)||!io.github.jimbozoomer.jugcraft.finance.ClaimTransfers.allowed(level,step.pos,next))continue;
                var state=level.getBlockState(next);var block=state.getBlock();
                int wire=io.github.jimbozoomer.jugcraft.jimtafacts.industry.FactoryWires.mask(level,next);
                int channels=step.channels&(block instanceof DataCableBlock?65535:wire!=0?wire:state.hasProperty(Channels.CHANNEL)?1<<state.getValue(Channels.CHANNEL).ordinal():65535);
                channels&=~seen.getOrDefault(next,0);if(channels==0)continue;
                if(block instanceof DataCableBlock||wire!=0){
                    if(!cables.contains(next)&&cables.size()>=MAX_CABLES)continue;
                    cables.add(next);seen.merge(next,channels,(a,b)->a|b);queue.add(new Step(next,channels));
                }else{
                    seen.merge(next,channels,(a,b)->a|b);
                    if(block instanceof SensorBlock)sensors.add(next);
                    else if(block instanceof ChannelSwitch)switches.add(next);
                    else if(block instanceof LogicControllerBlock)controllers.add(next);
                }
            }
        }
        return new Devices(List.copyOf(sensors),List.copyOf(switches),List.copyOf(controllers),cables.size());
    }
}
