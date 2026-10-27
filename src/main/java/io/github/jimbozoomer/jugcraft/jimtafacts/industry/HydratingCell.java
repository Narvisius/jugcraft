package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;

/** Original hydration-cell art on the shared transactional, refillable fluid-cell storage. */
public final class HydratingCell extends IndustrialProduction.Cell {
    public static final long DOSE=25L*81;
    public HydratingCell(Properties properties){super(properties);}
    @Override protected boolean acceptsFluid(FluidVariant fluid){return fluid.getFluid()==Fluids.WATER;}
    @Override public InteractionResult useOn(UseOnContext context){
        if(!(context.getLevel() instanceof ServerLevel level))return InteractionResult.SUCCESS;
        var player=context.getPlayer();if(player==null||player.isSpectator())return InteractionResult.FAIL;
        var clicked=context.getClickedPos();var soil=level.getBlockState(clicked).getBlock() instanceof FarmlandBlock?clicked:clicked.below();
        var state=level.getBlockState(soil);
        if(!(state.getBlock() instanceof FarmlandBlock))return super.useOn(context);
        if(!Authority.mayChange(level,player,clicked)||!Authority.mayChange(level,player,soil))return InteractionResult.FAIL;
        if(state.getValue(FarmlandBlock.MOISTURE)==7)return InteractionResult.PASS;
        try(var tx=Transaction.openOuter()){
            var storage=storage(ContainerItemContext.ofPlayerHand(player,context.getHand()));
            if(storage.extract(FluidVariant.of(Fluids.WATER),DOSE,tx)!=DOSE){player.sendOverlayMessage(Component.literal("Fill the hydrating cell with water at a tank first."));return InteractionResult.FAIL;}
            if(!level.setBlock(soil,state.setValue(FarmlandBlock.MOISTURE,7),Block.UPDATE_ALL))return InteractionResult.FAIL;
            tx.commit();
        }
        return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,java.util.function.Consumer<Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,context,display,lines,flag);
        lines.accept(Component.literal("Water only. Use on crops or farmland: 25 mB restores moisture; no instant growth."));
    }
}
