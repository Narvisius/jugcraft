package io.github.jimbozoomer.jugcraft.menagerie;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.BlockHitResult;

/** Two independently stored item faces; text remains intact underneath the display. */
public interface OwnerSignDisplay {
    ItemStack jugcraft$display(SignTextSlot side);
    void jugcraft$display(SignTextSlot side, ItemStack stack);

    static InteractionResult use(Player player,Level level,InteractionHand hand,BlockHitResult hit){
        if(!(level.getBlockEntity(hit.getBlockPos()) instanceof HangingSignBlockEntity sign)
                ||!(sign instanceof OwnerSignDisplay display))return InteractionResult.PASS;
        var held=player.getItemInHand(hand);var side=sign.getSlotPlayerIsFacing(player);var stored=display.jugcraft$display(side);
        // Keep normal dye, wax and text editing available. Crouching explicitly mounts an item.
        if(stored.isEmpty()&&(!player.isShiftKeyDown()||held.isEmpty()))return InteractionResult.PASS;
        if(held.getItem() instanceof SignApplicator&&!player.isShiftKeyDown())return InteractionResult.PASS;
        if(sign.isWaxed()||!Menagerie.mayChange(level,player,hit.getBlockPos()))return InteractionResult.FAIL;
        if(!stored.isEmpty()&&!held.isEmpty())return InteractionResult.FAIL;
        if(level.isClientSide())return InteractionResult.SUCCESS;
        boolean removing=!stored.isEmpty();
        if(removing){display.jugcraft$display(side,ItemStack.EMPTY);player.setItemInHand(hand,stored);}
        else{display.jugcraft$display(side,held.copyWithCount(1));held.consume(1,player);}
        sign.setChanged();level.sendBlockUpdated(sign.getBlockPos(),sign.getBlockState(),sign.getBlockState(),3);
        level.playSound(null,sign.getBlockPos(),removing?net.minecraft.sounds.SoundEvents.ITEM_FRAME_REMOVE_ITEM:net.minecraft.sounds.SoundEvents.ITEM_FRAME_ADD_ITEM,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        return InteractionResult.SUCCESS;
    }
}
