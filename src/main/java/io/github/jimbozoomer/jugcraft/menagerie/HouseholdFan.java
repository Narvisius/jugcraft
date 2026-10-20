package io.github.jimbozoomer.jugcraft.menagerie;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A visual-only rotor; rotation is derived on the client, with no per-tick packets or server searches. */
public final class HouseholdFan extends BlockEntity {
    public HouseholdFan(BlockPos p,BlockState s){super(Household.FAN,p,s);}
    public String design(){return ((HouseholdBlock)getBlockState().getBlock()).name;}
}
