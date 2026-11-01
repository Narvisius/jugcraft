package io.github.jimbozoomer.jugcraft.menagerie;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

/** Supplied side-by-side block/entity matcher: none=0, same kind=7, exact=15. */
final class OwnerSupplementGrouper {
    private OwnerSupplementGrouper(){}
    static void tick(ServerLevel level,BlockPos pos,BlockState state){
        var facing=HouseholdBlock.facing(state);var left=pos.relative(facing.getCounterClockWise());var right=pos.relative(facing.getClockWise());
        if(!level.hasChunkAt(left)||!level.hasChunkAt(right))return;
        var a=level.getBlockState(left);var b=level.getBlockState(right);
        boolean open=!a.isFaceSturdy(level,left,facing.getClockWise())&&!b.isFaceSturdy(level,right,facing.getCounterClockWise());
        String match="none";
        if(open){
            var lhs=entities(level,left);var rhs=lhs.isEmpty()?List.<Entity>of():entities(level,right);
            outer:for(var x:lhs)for(var y:rhs)if(x.getType()==y.getType()){
                if(exact(x,y)){match="full";break outer;}match="partial";
            }
            level.scheduleTick(pos,state.getBlock(),8);
        }else if(!a.isAir()&&a.is(b.getBlock()))match=a==b?"full":"partial";
        var next=HouseholdBlock.set(HouseholdBlock.set(state,"open_sides",Boolean.toString(open)),"match",match);
        if(next!=state){level.setBlockAndUpdate(pos,next);level.updateNeighborsAt(pos.relative(facing.getOpposite()),state.getBlock());}
    }
    private static List<Entity> entities(ServerLevel level,BlockPos pos){
        var result=new ArrayList<Entity>(16);
        level.getEntities(EntityTypeTest.forClass(Entity.class),new AABB(pos),e->e.isAlive()&&!e.isSpectator(),result,16);
        return result;
    }
    static boolean exact(Entity a,Entity b){
        if(a instanceof ItemEntity x&&b instanceof ItemEntity y)return ItemStack.isSameItemSameComponents(x.getItem(),y.getItem());
        if(a instanceof ItemFrame x&&b instanceof ItemFrame y)return ItemStack.isSameItemSameComponents(x.getItem(),y.getItem());
        if(a instanceof FallingBlockEntity x&&b instanceof FallingBlockEntity y)return x.getBlockState()==y.getBlockState();
        if(a instanceof LivingEntity x&&b instanceof LivingEntity y&&x.isBaby()!=y.isBaby())return false;
        if(a.hasCustomName()||b.hasCustomName())return Objects.equals(a.getCustomName(),b.getCustomName());
        if(a instanceof Sheep x&&b instanceof Sheep y)return x.getColor()==y.getColor();
        if(a instanceof net.minecraft.world.entity.npc.villager.VillagerDataHolder x&&b instanceof net.minecraft.world.entity.npc.villager.VillagerDataHolder y)return x.getVillagerData().profession().equals(y.getVillagerData().profession());
        for(var component:List.of(DataComponents.WOLF_VARIANT,DataComponents.CAT_VARIANT,DataComponents.FROG_VARIANT,DataComponents.FOX_VARIANT,DataComponents.SALMON_SIZE,DataComponents.PARROT_VARIANT,DataComponents.MOOSHROOM_VARIANT,DataComponents.RABBIT_VARIANT,DataComponents.PIG_VARIANT,DataComponents.COW_VARIANT,DataComponents.CHICKEN_VARIANT,DataComponents.HORSE_VARIANT,DataComponents.LLAMA_VARIANT,DataComponents.AXOLOTL_VARIANT,DataComponents.PAINTING_VARIANT)){
            var first=a.get(component);if(first!=null)return Objects.equals(first,b.get(component));
        }
        return false;
    }
    static int signal(BlockState state){return switch(HouseholdBlock.value(state,"match")){case "full"->15;case "partial"->7;default->0;};}
}
