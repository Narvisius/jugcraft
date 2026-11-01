package io.github.jimbozoomer.jugcraft.menagerie;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
/** Pushable stone puzzle blocks use the same conserved moving-block carrier as elevators. */
public final class OwnerSlidingBlocks {
    private OwnerSlidingBlocks(){}
    public interface NativeCarrier {void jugcraft$slidingOwner(UUID owner);}
    public static boolean pistonAllowed(net.minecraft.world.level.Level level,BlockPos pos,Direction dir){
        if(!(level instanceof ServerLevel server)||!(server.getBlockEntity(pos) instanceof OwnerSupplementData d)||!d.block().function.equals("slidy"))return false;
        var actor=Authority.present(server,d.owner);return actor!=null&&OwnerAssemblyMotion.allowed(server,actor,pos)&&OwnerAssemblyMotion.allowed(server,actor,pos.relative(dir))&&net.minecraft.world.level.ChunkPos.containing(pos).equals(net.minecraft.world.level.ChunkPos.containing(pos.relative(dir)));
    }
    public static void pistonFinished(net.minecraft.world.level.block.piston.PistonMovingBlockEntity piston,UUID owner){
        if(owner==null||!(piston.getLevel() instanceof ServerLevel level)||level.getBlockEntity(piston.getBlockPos())==piston)return;
        if(level.getBlockEntity(piston.getBlockPos()) instanceof OwnerSupplementData d&&d.block().function.equals("slidy")){
            d.owner=owner;continueSlide(level,piston.getBlockPos(),piston.getMovementDirection(),owner);
        }
    }
    public static InteractionResult use(OwnerSupplementData data,Player who,BlockHitResult hit){if(hit.getDirection().getAxis().isVertical())return InteractionResult.PASS;if(!(who instanceof ServerPlayer p))return InteractionResult.SUCCESS;return slide(p.level(),data.getBlockPos(),hit.getDirection().getOpposite(),p)?InteractionResult.SUCCESS:InteractionResult.FAIL;}
    static boolean slide(ServerLevel level,BlockPos pos,Direction dir,ServerPlayer actor){
        if(!(level.getBlockEntity(pos) instanceof OwnerSupplementData d)||!d.block().function.equals("slidy")||!d.mayEdit(actor)||!level.hasChunkAt(pos.relative(dir))||!level.getBlockState(pos.relative(dir)).isAir())return false;
        d.slideDirection=dir;d.slideActor=actor.getUUID();d.changed();return OwnerAssemblyMotion.move(level,actor,List.of(pos),dir,4,Map.of(),actor.getUUID());
    }
    public static void continueSlide(ServerLevel level,BlockPos pos,Direction dir,UUID actor){if(level.getBlockEntity(pos) instanceof OwnerSupplementData d){d.slideDirection=dir;d.slideActor=actor;d.pulseUntil=level.getGameTime()+1;d.changed();}}
    public static void tick(ServerLevel level,OwnerSupplementData d){
        if(level.getGameTime()<d.pulseUntil)return;d.pulseUntil=level.getGameTime()+4;var pos=d.getBlockPos();var actor=Authority.present(level,d.slideActor==null?d.owner:d.slideActor);if(actor==null)return;
        boolean held=OwnerSupplementRopes.rope(level.getBlockState(pos.above()));for(var side:Direction.Plane.HORIZONTAL)held|=level.getBlockState(pos.relative(side)).is(Blocks.HONEY_BLOCK);
        if(!held&&level.hasChunkAt(pos.below())&&level.getBlockState(pos.below()).isAir()){slide(level,pos,Direction.DOWN,actor);return;}
        if(d.slideDirection!=null&&d.slideDirection.getAxis().isHorizontal()&&level.getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.ICE)){if(slide(level,pos,d.slideDirection,actor))return;}
        d.slideDirection=null;d.slideActor=null;d.setChanged();
    }
}
