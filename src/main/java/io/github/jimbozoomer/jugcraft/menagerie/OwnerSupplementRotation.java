package io.github.jimbozoomer.jugcraft.menagerie;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;

/** Six-axis rotation, including atomic vertical pairs; unsupported assemblies stay intact. */
final class OwnerSupplementRotation {
    private OwnerSupplementRotation(){}
    static void tick(ServerLevel level,OwnerSupplementData data){
        long now=level.getGameTime();int signal=level.getBestNeighborSignal(data.getBlockPos());
        if(signal>0&&now>=data.pulseUntil){
            data.rotate(level);data.pulseUntil=now+64-signal*4;data.setChanged();
        }
        var state=data.getBlockState();var next=HouseholdBlock.set(HouseholdBlock.set(state,"power",Integer.toString(signal)),"rotating",Boolean.toString(signal>0||now<data.pulseUntil));
        if(next!=state)level.setBlockAndUpdate(data.getBlockPos(),next);
    }
    static boolean rotate(ServerLevel level,ServerPlayer actor,BlockPos pos,Direction axis,boolean inverse){
        if(!level.hasChunkAt(pos)||!Authority.mayChange(level,actor,pos))return false;
        BlockState state=level.getBlockState(pos);
        if(state.is(OwnerSupplementaries.BLOCKS.get("pulley_block")))return OwnerPulleys.drive(level,actor,pos,axis,inverse,4);
        if(state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF))return rotateTall(level,actor,pos,state,axis,inverse);
        if(state.hasProperty(BlockStateProperties.BED_PART)
                ||state.hasProperty(ChestBlock.TYPE)&&state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE
                ||state.hasProperty(BlockStateProperties.EXTENDED)&&state.getValue(BlockStateProperties.EXTENDED)
                ||state.getBlock() instanceof PistonHeadBlock||state.getBlock() instanceof MovingPistonBlock)return false;
        // Rotation of linked structures belongs to their placement controller, not a single cell.
        if(state.getBlock() instanceof io.github.jimbozoomer.jugcraft.machine.MachineBlock machine
                &&machine.footprint(state).offsets().size()>1)return false;
        for(var property:state.getProperties()){
            String name=property.getName();
            if(name.equals("part")||name.equals("segment")||name.equals("section"))return false;
        }
        var next=rotated(state,axis,inverse);
        if(next==state||!next.canSurvive(level,pos))return false;
        next=Block.updateFromNeighbourShapes(next,level,pos);
        if(next.getBlock()!=state.getBlock()||next==state)return false;
        // Same-block state update preserves inventory and block-entity identity.
        if(!level.setBlockAndUpdate(pos,next))return false;
        if(axis==Direction.UP&&next.getBlock() instanceof BaseRailBlock rail)rotateCarts(level,actor,pos,next,rail,inverse);
        level.gameEvent(actor,GameEvent.BLOCK_CHANGE,pos);
        level.playSound(null,pos,OwnerSupplementaries.SOUNDS.get("block_rotate"),SoundSource.BLOCKS,1,1);
        return true;
    }
    static void step(ServerLevel level,BlockPos pos,BlockState state,Entity entity){
        if(!entity.onGround()||entity.isPassenger()||entity.isVehicle()||entity.isSpectator()||entity.isShiftKeyDown()
                ||HouseholdBlock.facing(state)!=Direction.UP||!(level.getBlockEntity(pos) instanceof OwnerSupplementData data))return;
        int power=level.getBestNeighborSignal(pos);if(power==0)return;
        var actor=Authority.present(level,data.owner);
        if(actor==null||!Authority.mayChange(level,actor,pos)||!mayMove(level,actor,entity))return;
        if(entity instanceof LivingEntity living&&io.github.jimbozoomer.jugcraft.wilds.WildCold.frozen(living))return;
        float increment=(HouseholdBlock.flag(state,"inverted")?1:-1)*90F/(65-power*4);
        Vec3 center=Vec3.atBottomCenterOf(pos),offset=entity.position().subtract(center);
        Vec3 delta=offset.yRot(increment*(float)Math.PI/180).subtract(offset);
        if(!clear(level,actor,entity,delta))return;
        entity.move(MoverType.SHULKER_BOX,delta);entity.syncVelocity=true;
        float yaw=entity.getYRot()-increment;entity.setYRot(yaw);
        if(entity instanceof LivingEntity living){living.setYBodyRot(yaw);living.setYHeadRot(living.getYHeadRot()-increment);}
        if(entity instanceof ServerPlayer player)player.connection.send(new ClientboundPlayerRotationPacket(-increment,true,0,true));
    }
    private static boolean mayMove(ServerLevel level,ServerPlayer actor,Entity entity){
        if(entity.isRemoved()||entity.isSpectator()||!Authority.mayChange(level,actor,entity.blockPosition()))return false;
        if(entity==actor)return true;
        if(entity instanceof LivingEntity living)return io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(actor,level,living);
        return net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.invoker().interact(actor,level,net.minecraft.world.InteractionHand.MAIN_HAND,entity,null)!=net.minecraft.world.InteractionResult.FAIL;
    }
    private static boolean clear(ServerLevel level,ServerPlayer actor,Entity entity,Vec3 delta){
        var box=entity.getBoundingBox().move(delta);
        if(!level.getWorldBorder().isWithinBounds(box))return false;
        if(box.getSize()>8)return false;
        for(var cell:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))
            if(!level.hasChunkAt(cell)||!Authority.mayChange(level,actor,cell))return false;
        return level.noCollision(entity,box);
    }
    private static void rotateCarts(ServerLevel level,ServerPlayer actor,BlockPos pos,BlockState state,BaseRailBlock rail,boolean inverse){
        RailShape shape=state.getValue(rail.getShapeProperty());
        if(shape!=RailShape.NORTH_SOUTH&&shape!=RailShape.EAST_WEST)return;
        int count=0;
        for(var cart:level.getEntitiesOfClass(AbstractMinecart.class,new AABB(pos))){
            if(++count>16)break;
            // Occupied carts stay put: rotating the rail is safe, forcing passengers across claims is not.
            if(cart.isVehicle()||!mayMove(level,actor,cart))continue;
            Vec3 delta=new Vec3(pos.getX()+.5-cart.getX(),0,pos.getZ()+.5-cart.getZ());
            if(!clear(level,actor,cart,delta))continue;
            cart.setPos(cart.position().add(delta));cart.setYRot(cart.getYRot()+(inverse?90:-90));cart.yRotO=cart.getYRot();
            cart.setDeltaMovement(cart.getDeltaMovement().yRot((inverse?1:-1)*(float)Math.PI/2));cart.syncVelocity=true;
        }
    }
    private static boolean rotateTall(ServerLevel level,ServerPlayer actor,BlockPos pos,BlockState state,Direction axis,boolean inverse){
        if(axis.getAxis()!=Direction.Axis.Y||state.hasBlockEntity())return false;
        var other=state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==DoubleBlockHalf.LOWER?pos.above():pos.below();
        if(!level.hasChunkAt(other)||!Authority.mayChange(level,actor,other))return false;var partner=level.getBlockState(other);
        if(partner.getBlock()!=state.getBlock()||partner.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF))return false;
        var next=rotated(state,axis,inverse);var nextPartner=rotated(partner,axis,inverse);
        if(next==state||nextPartner==partner||!next.canSurvive(level,pos)||!nextPartner.canSurvive(level,other))return false;
        // No block-entity move, drops or neighbor callbacks until both halves agree.
        int flags=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE;
        if(!level.setBlock(pos,next,flags))return false;
        if(!level.setBlock(other,nextPartner,flags)){level.setBlock(pos,state,flags);return false;}
        level.updateNeighborsAt(pos,next.getBlock());level.updateNeighborsAt(other,nextPartner.getBlock());
        level.gameEvent(actor,GameEvent.BLOCK_CHANGE,pos);level.playSound(null,pos,OwnerSupplementaries.SOUNDS.get("block_rotate"),SoundSource.BLOCKS,1,1);return true;
    }
    static BlockState rotated(BlockState state,Direction axis,boolean inverse){
        boolean reverse=inverse^(axis.getAxisDirection()==Direction.AxisDirection.NEGATIVE);
        if(axis.getAxis()==Direction.Axis.Y)return state.rotate(reverse?Rotation.COUNTERCLOCKWISE_90:Rotation.CLOCKWISE_90);
        // Supplied data-backed blocks use named properties rather than native EnumProperty<net.minecraft.core.Direction>.
        var facing=state.getBlock().getStateDefinition().getProperty("facing");
        if(facing!=null&&facing.getValue("up").isPresent()&&facing.getValue("down").isPresent()){
            Direction old=HouseholdBlock.facing(state);if(old.getAxis()==axis.getAxis())return state;
            return HouseholdBlock.set(state,"facing",(reverse?old.getCounterClockWise(axis.getAxis()):old.getClockWise(axis.getAxis())).getName());
        }
        var pillar=state.getBlock().getStateDefinition().getProperty("axis");
        if(pillar!=null){
            String current=HouseholdBlock.value(state,"axis");Direction.Axis old=Direction.Axis.valueOf(current.toUpperCase(java.util.Locale.ROOT));
            if(old==axis.getAxis())return state;
            Direction rotated=Direction.fromAxisAndDirection(old,Direction.AxisDirection.POSITIVE).getClockWise(axis.getAxis());
            return HouseholdBlock.set(state,"axis",rotated.getAxis().getName());
        }
        if(state.getBlock() instanceof StairBlock){
            Direction old=state.getValue(StairBlock.FACING);if(old.getAxis()==axis.getAxis())return state;
            boolean top=state.getValue(StairBlock.HALF)==Half.TOP,positive=old.getAxisDirection()==Direction.AxisDirection.POSITIVE;
            boolean flipped=(axis.getAxisDirection()==Direction.AxisDirection.POSITIVE)^inverse;
            return top^positive^flipped?state.cycle(StairBlock.HALF):state.setValue(StairBlock.FACING,old.getOpposite());
        }
        if(state.hasProperty(SlabBlock.TYPE)&&state.getValue(SlabBlock.TYPE)!=SlabType.DOUBLE)
            return state.setValue(SlabBlock.TYPE,state.getValue(SlabBlock.TYPE)==SlabType.BOTTOM?SlabType.TOP:SlabType.BOTTOM);
        if(state.getBlock() instanceof TrapDoorBlock)return state.cycle(TrapDoorBlock.HALF);
        if(state.hasProperty(BlockStateProperties.ATTACH_FACE)&&state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)){
            Direction old=state.getValue(BlockStateProperties.HORIZONTAL_FACING);if(old.getAxis()==axis.getAxis())return state;
            return switch(state.getValue(BlockStateProperties.ATTACH_FACE)){
                case FLOOR->state.setValue(BlockStateProperties.ATTACH_FACE,AttachFace.WALL).setValue(BlockStateProperties.HORIZONTAL_FACING,inverse?axis.getClockWise():axis.getCounterClockWise());
                case CEILING->state.setValue(BlockStateProperties.ATTACH_FACE,AttachFace.WALL).setValue(BlockStateProperties.HORIZONTAL_FACING,inverse?axis.getCounterClockWise():axis.getClockWise());
                case WALL->state.setValue(BlockStateProperties.ATTACH_FACE,(old.getAxisDirection()==Direction.AxisDirection.POSITIVE)^reverse?AttachFace.CEILING:AttachFace.FLOOR);
            };
        }
        return state;
    }
}
