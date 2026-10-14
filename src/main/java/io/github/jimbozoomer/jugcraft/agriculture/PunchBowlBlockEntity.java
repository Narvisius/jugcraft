package io.github.jimbozoomer.jugcraft.agriculture;

import local.peepo.ServingCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** Only completed, paid-for bottles; the punch itself remains in the native block state. No ticker. */
public final class PunchBowlBlockEntity extends BlockEntity {
    public ItemStack bottled=ItemStack.EMPTY;
    public final ServingCompanion companion=new ServingCompanion(this);
    public PunchBowlBlockEntity(BlockPos pos,BlockState state){super(JugcraftAgriculture.PUNCH_BOWL_ENTITY,pos,state);}
    protected void loadAdditional(ValueInput in){super.loadAdditional(in);bottled=in.read("CompanionBottles",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);companion.load(in);}
    protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.store("CompanionBottles",ItemStack.OPTIONAL_CODEC,bottled);companion.save(out);}
    public void preRemoveSideEffects(BlockPos pos,BlockState state){if(level!=null && !bottled.isEmpty()){Containers.dropItemStack(level,pos.getX(),pos.getY(),pos.getZ(),bottled);bottled=ItemStack.EMPTY;}}
}
