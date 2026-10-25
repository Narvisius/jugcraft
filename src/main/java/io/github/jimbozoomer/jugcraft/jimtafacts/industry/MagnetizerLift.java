package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

/** Native magnetic lift along the source iron fence; contiguous loaded column, paid motion and no forced travel. */
public final class MagnetizerLift {
    public static final TagKey<Item> BOOTS=TagKey.create(Registries.ITEM,Jugcraft.id("magnetizer_boots"));
    private static final Map<ServerPlayer,Long> LAST_LIFT=new WeakHashMap<>();
    private MagnetizerLift(){}
    public static boolean step(ServerLevel level,IndustrialInfrastructure.Node n){
        var actor=Authority.answering(level,n.owner);if(actor==null||n.energy.getAmount()<5||!Authority.mayChange(level,actor,n.getBlockPos()))return false;
        BlockPos base=n.getBlockPos().relative(n.getBlockState().getValue(HorizontalDirectionalBlock.FACING));
        if(!fence(level,base,actor,n.getBlockPos()))return false;
        int low=0,high=0;for(int i=1;i<=20&&fence(level,base.below(i),actor,n.getBlockPos());i++)low=i;
        for(int i=1;i<=20&&fence(level,base.above(i),actor,n.getBlockPos());i++)high=i;
        var players=new ArrayList<ServerPlayer>();level.getEntities(EntityTypeTest.forClass(ServerPlayer.class),
            new AABB(base.getX()-.35,base.getY()-low,base.getZ()-.35,base.getX()+1.35,base.getY()+high+1,base.getZ()+1.35),
            p->p.isAlive()&&!p.isSpectator()&&!p.isPassenger()&&!p.getAbilities().flying&&p.getItemBySlot(EquipmentSlot.FEET).is(BOOTS),players,8);
        boolean moved=false;
        for(var p:players){if(n.energy.getAmount()<5)break;if(LAST_LIFT.getOrDefault(p,Long.MIN_VALUE)==level.getGameTime())continue;
            if(!Authority.mayChange(level,p,n.getBlockPos())||!Authority.mayChange(level,p,p.blockPosition())||!ClaimTransfers.allowed(level,n.getBlockPos(),p.blockPosition()))continue;
            // Walking away from the column releases the lift; crouch descends instead of being pulled upward.
            var velocity=p.getDeltaMovement();double y=p.isShiftKeyDown()?-.12:Math.max(.38,Math.min(.6,velocity.y+.2));
            p.setDeltaMovement(velocity.x,y,velocity.z);p.resetFallDistance();p.connection.send(new ClientboundSetEntityMotionPacket(p));
            n.energy.setAmount(n.energy.getAmount()-5);LAST_LIFT.put(p,level.getGameTime());moved=true;
        }
        if(moved)n.setChanged();return moved;
    }
    private static boolean fence(ServerLevel l,BlockPos p,ServerPlayer actor,BlockPos origin){return l.isInWorldBounds(p)&&l.hasChunkAt(p)&&l.getBlockState(p).is(IndustrialConstruction.IRON_FENCE)&&ClaimTransfers.allowed(l,origin,p)&&Authority.mayChange(l,actor,p);}
}
