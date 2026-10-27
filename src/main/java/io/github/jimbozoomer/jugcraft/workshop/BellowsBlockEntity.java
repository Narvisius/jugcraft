package io.github.jimbozoomer.jugcraft.workshop;

import io.github.jimbozoomer.jugcraft.kinetic.KineticConsumer;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** At most one tick's paid airflow; no stored rotation, offline work or multiple-source speed bonus. */
public final class BellowsBlockEntity extends BlockEntity implements KineticConsumer {
    public static final int COST = 8;
    private int energy;
    private boolean manualStroke;
    private long received = Long.MIN_VALUE, active = Long.MIN_VALUE;
    public BellowsBlockEntity(BlockPos p, BlockState s) { super(JugcraftWorkshop.BELLOWS_ENTITY,p,s); }
    public @Nullable WorkshopBlockEntity furnace() {
        if (level==null) return null;
        BlockPos p=worldPosition.relative(getBlockState().getValue(BellowsBlock.FACING));
        return level.hasChunkAt(p) && level.getBlockEntity(p) instanceof WorkshopBlockEntity f && f.station()==2 ? f : null;
    }
    public boolean hasAir() { return level!=null && energy==COST && received>=level.getGameTime()-1; }
    @Override public long acceptKinetic(Direction side, long amount) {
        if (!(level instanceof ServerLevel) || side!=getBlockState().getValue(BellowsBlock.FACING).getOpposite() || amount<=0) return 0;
        var f=furnace(); long now=level.getGameTime();
        if (f==null || f.bellows()!=this || !f.wantsShaftAir()) return 0;
        if (received<now-1) energy=0;
        int take=(int)Math.min(amount,COST-energy); if(take==0)return 0;
        energy+=take; received=now; return take;
    }
    public void worked(boolean manual) {
        // A source ticking after the furnace may refill the one-tick buffer for its next step.
        // Progress still belongs exclusively to the furnace ticker, never to incoming power.
        if (!manual) energy=0;
        manualStroke=manual;
        active=level.getGameTime();
    }
    /** Presentation only: consuming this tick's airflow must not make an operating menu say "stopped". */
    public int currentStroke() { return level!=null && active==level.getGameTime() ? manualStroke ? 1 : 2 : 0; }
    public void tick(ServerLevel l) {
        int stage=active>=l.getGameTime()-1 ? (int)(l.getGameTime()/4%6) : 0;
        if(stage>3)stage=6-stage;
        if(getBlockState().getValue(BellowsBlock.PUMP)!=stage) l.setBlock(worldPosition,getBlockState().setValue(BellowsBlock.PUMP,stage),Block.UPDATE_CLIENTS);
    }
}
