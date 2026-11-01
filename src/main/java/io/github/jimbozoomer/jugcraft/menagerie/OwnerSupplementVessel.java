package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.fluid.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Portable jars/cages retain entity identity; failed release never consumes the saved occupant. */
public final class OwnerSupplementVessel extends BlockItem {
    static final String MOB="JugcraftVesselMob";
    public OwnerSupplementVessel(Block block,Properties p){super(block,p.stacksTo(1));}
    @Override public boolean canFitInsideContainerItems(){return false;}
    public static CompoundTag occupant(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(MOB).orElse(null);}
    static void occupant(ItemStack stack,CompoundTag mob){var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(mob==null)data.remove(MOB);else data.put(MOB,mob.copy());stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));}
    @Override public InteractionResult interactLivingEntity(ItemStack held,Player actor,LivingEntity target,InteractionHand hand){
        if(!(actor instanceof ServerPlayer player))return InteractionResult.SUCCESS;
        if(!(target instanceof Mob mob)||!mob.isAlive()||mob.isPassenger()||mob.isVehicle()||mob.isLeashed()||occupant(held)!=null||held.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("SoftPortions",0)>0||held.has(JugcraftFluids.STORED_FLUID)||held.getOrDefault(DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.EMPTY).nonEmptyItemCopyStream().findAny().isPresent())return InteractionResult.PASS;
        boolean cage=((OwnerSupplementBlock)getBlock()).function.equals("cage");
        if(mob.getBbWidth()>(cage?.875:.625)||mob.getBbHeight()>(cage?1:.875)||!cage&&mob instanceof Enemy||mob instanceof TamableAnimal pet&&pet.isTame()&&!pet.isOwnedBy(player))return InteractionResult.FAIL;
        if(player.distanceToSqr(mob)>36||!Authority.mayChange(player.level(),player,mob.blockPosition())||!Authority.mayStrike(player,mob))return InteractionResult.FAIL;
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,player.registryAccess());if(!mob.save(out))return InteractionResult.FAIL;var saved=out.buildResult();if(saved.toString().length()>65536)return InteractionResult.FAIL;
        occupant(held,saved);mob.discard();player.getInventory().setChanged();return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult useOn(UseOnContext c){
        var saved=occupant(c.getItemInHand());if(saved!=null&&c.getPlayer()!=null&&c.getPlayer().isShiftKeyDown()){
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(release(p,saved,c.getClickedPos().relative(c.getClickedFace()))){occupant(c.getItemInHand(),null);return InteractionResult.SUCCESS;}return InteractionResult.FAIL;
        }return super.useOn(c);
    }
    static boolean release(ServerPlayer p,CompoundTag saved,BlockPos at){
        if(!p.level().hasChunkAt(at)||!Authority.mayChange(p.level(),p,at)||p.distanceToSqr(Vec3.atCenterOf(at))>64)return false;
        var entity=EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),saved),p.level(),EntitySpawnReason.MOB_SUMMONED,EntityProcessor.NOP);
        if(!(entity instanceof Mob mob)||mob.isVehicle()||mob.isPassenger())return false;
        for(var level:p.level().getServer().getAllLevels())if(level.getEntity(mob.getUUID())!=null)return false;
        mob.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),0);
        if(!p.level().noCollision(mob)||!p.level().getWorldBorder().isWithinBounds(mob.getBoundingBox()))return false;
        return p.level().addFreshEntity(mob);
    }
}
