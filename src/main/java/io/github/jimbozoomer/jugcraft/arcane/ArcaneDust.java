package io.github.jimbozoomer.jugcraft.arcane;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The source smithing-table ritual becomes a forge only over its complete authored base. */
public final class ArcaneDust extends Item {
    ArcaneDust(Properties p){super(p);}
    @Override public InteractionResult useOn(UseOnContext c){
        if(c.getLevel().getBlockState(c.getClickedPos()).is(ForbiddenArcane.block("clibano_core"))){if(c.getPlayer() instanceof ServerPlayer p){if(!ArcaneClibano.form(p,c.getClickedPos(),c.getClickedFace()))return InteractionResult.FAIL;c.getItemInHand().consume(1,p);}return InteractionResult.SUCCESS;}
        if(c.getLevel().getBlockState(c.getClickedPos()).is(ForbiddenArcane.block("arcane_crystal_block"))||c.getLevel().getBlockState(c.getClickedPos()).is(ForbiddenArcane.block("arcane_polished_darkstone"))){if(c.getPlayer() instanceof ServerPlayer p){if(!ArcaneObelisk.form(p,c.getClickedPos()))return InteractionResult.FAIL;c.getItemInHand().consume(1,p);}return InteractionResult.SUCCESS;}
        if(!c.getLevel().getBlockState(c.getClickedPos()).is(Blocks.SMITHING_TABLE))return InteractionResult.PASS;
        if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        var l=p.level();var at=c.getClickedPos();
        if(!Authority.mayChange(l,p,at)||!ArcaneWorkshop.floor(l,at,p,true))return InteractionResult.FAIL;
        if(l.setBlockAndUpdate(at,ForbiddenArcane.block("hephaestus_forge_tier_1").defaultBlockState())){if(l.getBlockEntity(at) instanceof ArcaneWorkshop w)w.owner(p.getUUID());c.getItemInHand().consume(1,p);return InteractionResult.SUCCESS;}
        return InteractionResult.FAIL;
    }
}
