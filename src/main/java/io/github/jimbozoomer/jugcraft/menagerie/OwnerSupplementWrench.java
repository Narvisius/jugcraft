package io.github.jimbozoomer.jugcraft.menagerie;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

/** Copper-repairable source wrench using the same safe rotation rules as turntables. */
public final class OwnerSupplementWrench extends Item {
    public OwnerSupplementWrench(Properties properties){super(properties.durability(200).repairable(Items.COPPER_INGOT));}
    @Override public InteractionResult useOn(UseOnContext context){
        var user=context.getPlayer();if(user==null||!Menagerie.mayChange(context.getLevel(),user,context.getClickedPos()))return InteractionResult.FAIL;
        if(!(user instanceof ServerPlayer player))return InteractionResult.SUCCESS;
        var level=player.level();var pos=context.getClickedPos();
        if(level.getBlockEntity(pos) instanceof OwnerSupplementData sign&&OwnerWaySigns.sign(sign))return OwnerWaySigns.use(sign,player,context.getItemInHand(),context.getHand(),new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(),context.getClickedFace(),pos,false));
        if(level.getBlockEntity(pos) instanceof OwnerSupplementData cannon&&cannon.block().function.equals("cannon"))return OwnerCannon.use(cannon,player,context.getItemInHand(),context.getHand());
        if(level.getBlockEntity(pos) instanceof OwnerSupplementData pulley&&pulley.block().function.equals("pulley")&&user.isShiftKeyDown())return OwnerPulleys.use(pulley,player,context.getItemInHand());
        boolean inverse=user.isShiftKeyDown();
        boolean changed=OwnerSupplementRotation.rotate(level,player,pos,context.getClickedFace(),inverse);
        if(!changed&&context.getClickedFace().getAxis()!=Direction.Axis.Y)changed=OwnerSupplementRotation.rotate(level,player,pos,Direction.UP,inverse);
        if(changed){
            context.getItemInHand().hurtAndBreak(1,player,context.getHand().asEquipmentSlot());
            level.playSound(null,pos,OwnerSupplementaries.SOUNDS.get("wrench_rotate"),SoundSource.PLAYERS,1,1.4F);
            net.minecraft.advancements.triggers.CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player,pos,context.getItemInHand());return InteractionResult.SUCCESS;
        }
        level.playSound(null,pos,OwnerSupplementaries.SOUNDS.get("wrench_fail"),SoundSource.PLAYERS,1.4F,.8F);return InteractionResult.FAIL;
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){
        if(!(target instanceof ArmorStand))return InteractionResult.PASS;
        if(!Menagerie.mayChange(player.level(),player,target.blockPosition()))return InteractionResult.FAIL;
        if(!player.level().isClientSide()){
            float yaw=target.getYRot()+(player.isShiftKeyDown()?-22.5F:22.5F);target.setYRot(yaw);target.setYBodyRot(yaw);target.setYHeadRot(yaw);
            stack.hurtAndBreak(1,player,hand.asEquipmentSlot());player.level().playSound(null,target.blockPosition(),OwnerSupplementaries.SOUNDS.get("wrench_rotate"),SoundSource.PLAYERS,1,1.4F);
        }return InteractionResult.SUCCESS;
    }
    @Override public void hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){
        super.hurtEnemy(stack,target,attacker);
        if(!attacker.level().isClientSide()){stack.hurtAndBreak(1,attacker,EquipmentSlot.MAINHAND);attacker.level().playSound(null,target.blockPosition(),OwnerSupplementaries.SOUNDS.get("wrench_hit"),SoundSource.PLAYERS,1,.9F+attacker.getRandom().nextFloat()*.2F);}
    }
}
