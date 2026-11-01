package io.github.jimbozoomer.jugcraft.botany;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauFilters;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;

/** Hopperhock uses the same Fabric inventory transactions as the rest of Jugcraft automation. */
final class BotanicalCollection {
    private record Destination(BlockPos pos,Direction side,List<ItemStack> filter){}
    static boolean collect(BotanicalWorkshop flower,ServerLevel level,ServerPlayer actor,int mode){
        boolean mini=flower.kind().endsWith("chibi"),powered=flower.mana()>0;int range=mini?(powered?2:1):(powered?10:6);
        var destinations=new ArrayList<Destination>();var origin=flower.effectivePos();
        for(Direction side:Direction.values()){
            var at=origin.relative(side);if(!ClaimTransfers.allowed(level,origin,at)||!Authority.mayChange(level,actor,at))continue;
            if(level.getBlockEntity(at) instanceof RandomizableContainerBlockEntity c&&c.getLootTable()!=null)continue;
            if(ItemStorage.SIDED.find(level,at,side.getOpposite())==null)continue;
            destinations.add(new Destination(at,side.getOpposite(),frames(level,at)));
        }
        destinations.sort(Comparator.comparing(d->d.filter().isEmpty()));
        int inspected=0;boolean moved=false;
        for(var drop:level.getEntitiesOfClass(ItemEntity.class,new AABB(origin).inflate(range))){
            if(++inspected>32)break;if(!drop.isAlive()||drop.getAge()<20||drop.hasPickUpDelay()||!BotanicalDropCooldown.ready(drop)||drop.getItem().isEmpty()||!ClaimTransfers.allowed(level,origin,drop.blockPosition())||!Authority.mayChange(level,actor,drop.blockPosition()))continue;
            for(var target:destinations){
                var stack=drop.getItem();boolean matches=target.filter().stream().anyMatch(f->NouveauFilters.allows(f,stack));
                if(!target.filter().isEmpty()&&mode!=2&&(mode==0?!matches:matches))continue;
                var storage=ItemStorage.SIDED.find(level,target.pos(),target.side());if(storage==null)continue;
                try(var transaction=Transaction.openOuter()){
                    int accepted=(int)storage.insert(ItemVariant.of(stack),stack.getCount(),transaction);if(accepted<=0)continue;
                    var source=new Drop(drop);source.take(accepted,transaction);transaction.commit();moved=true;break;
                }
            }
        }return moved;
    }
    static List<ItemStack> frames(ServerLevel level,BlockPos at){
        var positions=new ArrayList<BlockPos>();positions.add(at);var state=level.getBlockState(at);
        if(state.getBlock() instanceof ChestBlock&&state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE){var other=at.relative(ChestBlock.getConnectedDirection(state));if(ClaimTransfers.allowed(level,at,other))positions.add(other);}
        var result=new ArrayList<ItemStack>();for(var pos:positions)for(var frame:level.getEntitiesOfClass(ItemFrame.class,new AABB(pos).inflate(1)))if(!frame.getItem().isEmpty()&&frame.getPos().relative(frame.getDirection().getOpposite()).equals(pos)){result.add(frame.getItem().copy());if(result.size()>=12)return result;}return result;
    }
    private static final class Drop extends SnapshotParticipant<ItemStack> {
        private final ItemEntity entity;
        Drop(ItemEntity entity){this.entity=entity;}
        void take(int amount,TransactionContext transaction){if(amount<1||amount>entity.getItem().getCount())throw new IllegalArgumentException("Invalid flower extraction");updateSnapshots(transaction);entity.getItem().shrink(amount);}
        @Override protected ItemStack createSnapshot(){return entity.getItem().copy();}
        @Override protected void readSnapshot(ItemStack stack){entity.setItem(stack);}
        @Override protected void onFinalCommit(){if(entity.getItem().isEmpty())entity.discard();else entity.setItem(entity.getItem().copy());}
    }
}
