package io.github.jimbozoomer.jugcraft.arcane;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class ArcaneMixture extends Item {
    ArcaneMixture(Properties p){super(p);}
    @Override public InteractionResult useOn(UseOnContext c){
        if(!c.getLevel().getBlockState(c.getClickedPos()).is(ForbiddenArcane.block("darkstone_pedestal")))return InteractionResult.PASS;
        if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(!Authority.mayChange(p.level(),p,c.getClickedPos())||!(p.level().getBlockEntity(c.getClickedPos()) instanceof ArcaneWorkshop w))return InteractionResult.FAIL;
        if(w.magnetize(p)){c.getItemInHand().consume(1,p);return InteractionResult.SUCCESS;}return InteractionResult.FAIL;
    }
}
