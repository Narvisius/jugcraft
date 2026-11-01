package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Rope/chain reels, cooperating shafts and sticky load resolution. Eight load blocks per reel. */
public final class OwnerPulleys {
    private OwnerPulleys(){}
    public static boolean winding(ItemStack stack){return stack.is(Blocks.IRON_CHAIN.asItem())||stack.is(OwnerSupplementaries.BLOCKS.get("rope").asItem());}
    static String windingName(ItemStack stack){return stack.is(Blocks.IRON_CHAIN.asItem())?"chain":stack.is(OwnerSupplementaries.BLOCKS.get("rope").asItem())?"rope":"none";}
    static boolean rope(BlockState state,Block block){return state.is(block)&&(block!=Blocks.IRON_CHAIN||HouseholdBlock.value(state,"axis").equals("y"));}
    static boolean sticky(BlockState state){return state.is(Blocks.SLIME_BLOCK)||state.is(Blocks.HONEY_BLOCK);}
    static boolean stickTogether(BlockState a,BlockState b){return !(a.is(Blocks.SLIME_BLOCK)&&b.is(Blocks.HONEY_BLOCK)||a.is(Blocks.HONEY_BLOCK)&&b.is(Blocks.SLIME_BLOCK));}
    public static InteractionResult use(OwnerSupplementData d,Player player,ItemStack stack){
        if(!(player instanceof ServerPlayer actor))return InteractionResult.SUCCESS;
        if(player.isShiftKeyDown()){boolean extending=!(stack.getItem() instanceof OwnerSupplementWrench);boolean ok=drive(actor.level(),actor,d.getBlockPos(),null,extending,4);if(!ok)actor.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Pulley blocked: check rope supply, clearance, claims and the 8-block load per cooperating reel. Keep each moving assembly in one chunk."));return ok?InteractionResult.SUCCESS:InteractionResult.FAIL;}
        actor.openMenu(d);return InteractionResult.SUCCESS;
    }
    public static boolean drive(ServerLevel level,ServerPlayer actor,BlockPos start,Direction axis,boolean extending,int ticks){
        if(!(level.getBlockEntity(start) instanceof OwnerSupplementData first)||!first.block().function.equals("pulley"))return false;
        String shaft=HouseholdBlock.value(first.getBlockState(),"axis");if(shaft.equals("y")||axis!=null&&!shaft.equals(axis.getAxis().getName()))return false;
        var direction=shaft.equals("x")?Direction.EAST:Direction.SOUTH;var reels=new ArrayList<OwnerSupplementData>();reels.add(first);
        for(var dir:List.of(direction,direction.getOpposite()))for(int i=1;i<=7&&reels.size()<8;i++){var p=start.relative(dir,i);if(!level.hasChunkAt(p)||!(level.getBlockEntity(p) instanceof OwnerSupplementData d)||!d.block().function.equals("pulley")||!HouseholdBlock.value(d.getBlockState(),"axis").equals(shaft))break;reels.add(d);}
        var sources=new LinkedHashSet<BlockPos>();var ropeCells=new HashSet<BlockPos>();var edits=new LinkedHashMap<BlockPos,BlockState>();var recovered=new HashMap<OwnerSupplementData,ItemStack>();var loads=new ArrayDeque<BlockPos>();
        for(var d:reels){
            if(!d.mayEdit(actor)||level.getGameTime()<d.pulseUntil)return false;var spool=d.getItem(0);var top=d.getBlockPos().below();var state=level.getBlockState(top);
            Block line=spool.isEmpty()?state.is(Blocks.IRON_CHAIN)?Blocks.IRON_CHAIN:OwnerSupplementaries.BLOCKS.get("rope"):spool.getItem() instanceof BlockItem b?b.getBlock():null;
            if(line==null||!spool.isEmpty()&&!winding(spool)||extending&&spool.isEmpty())return false;
            if(!extending&&(!rope(state,line)||!spool.isEmpty()&&!spool.is(line.asItem())||spool.getCount()>=d.getMaxStackSize()))return false;
            var cursor=top;int length=0;while(level.hasChunkAt(cursor)&&rope(level.getBlockState(cursor),line)&&length++<64){if(extending||!cursor.equals(top)){sources.add(cursor);ropeCells.add(cursor);}cursor=cursor.below();}
            if(length>64||!level.hasChunkAt(cursor))return false;
            if(!level.getBlockState(cursor).isAir())loads.add(cursor);
            if(extending){var next=line.defaultBlockState();next=HouseholdBlock.set(next,"axis","y");edits.put(top,next);}else{edits.put(top,Blocks.AIR.defaultBlockState());recovered.put(d,new ItemStack(line));}
        }
        Direction move=extending?Direction.DOWN:Direction.UP;int budget=reels.size()*8;var loadCells=new HashSet<BlockPos>();
        while(!loads.isEmpty()){
            var p=loads.removeFirst();if(sources.contains(p)||loadCells.contains(p)||edits.containsKey(p))continue;if(!OwnerAssemblyMotion.allowed(level,actor,p)||!OwnerAssemblyMotion.movable(level,p)||loadCells.size()>=budget)return false;
            var s=level.getBlockState(p);if(s.isAir())continue;loadCells.add(p);sources.add(p);
            var front=p.relative(move);if(!sources.contains(front)&&!edits.containsKey(front)&&!level.getBlockState(front).isAir())loads.add(front);
            if(sticky(s))for(var dir:Direction.values()){var neighbor=p.relative(dir);if(!level.hasChunkAt(neighbor))return false;var n=level.getBlockState(neighbor);if(!n.isAir()&&stickTogether(s,n)&&OwnerAssemblyMotion.movable(level,neighbor)&&!reels.stream().anyMatch(r->r.getBlockPos().equals(neighbor)))loads.add(neighbor);}
        }
        // All touched state and contents are captured together before any mutation.
        if(!OwnerAssemblyMotion.move(level,actor,sources,move,ticks,edits,null))return false;
        for(var d:reels){if(extending)d.getItem(0).shrink(1);else if(d.getItem(0).isEmpty())d.setItem(0,recovered.get(d));else d.getItem(0).grow(1);d.pulseUntil=level.getGameTime()+ticks+2;var s=HouseholdBlock.set(d.getBlockState(),"flipped",Boolean.toString(!HouseholdBlock.flag(d.getBlockState(),"flipped")));level.setBlockAndUpdate(d.getBlockPos(),s);d.changed();}
        level.playSound(null,start,extending?SoundEvents.PISTON_EXTEND:SoundEvents.PISTON_CONTRACT,SoundSource.BLOCKS,.6F,.8F);return true;
    }
}
