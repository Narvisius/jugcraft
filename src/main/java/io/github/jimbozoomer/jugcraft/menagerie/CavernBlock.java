package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.JsonObject;
import java.util.Set;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import org.jspecify.annotations.Nullable;

/** Working cave stations, incubators and ecosystem props using the owner models. */
class CavernBlock extends HouseholdBlock {
    static final class WithEntity extends CavernBlock implements EntityBlock { WithEntity(Properties p,JsonObject d){super(p,d);} }
    CavernBlock(Properties p,JsonObject d){super(p,d);}
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos p,BlockState s){return CavernControl.CAVE.contains(name)?new CavernControl(p,s):CavernDevice.KINDS.contains(name)?new CavernDevice(p,s):super.newBlockEntity(p,s);}
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return !l.isClientSide()&&type==CavernControl.TYPE?(level,p,state,be)->CavernControl.tick((ServerLevel)level,p,state,(CavernControl)be):!l.isClientSide()&&type==CavernDevice.TYPE?(level,p,state,be)->CavernDevice.tick((ServerLevel)level,p,state,(CavernDevice)be):super.getTicker(l,s,type);}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity who,ItemStack stack){super.setPlacedBy(l,p,s,who,stack);if(who instanceof Player player&&l.getBlockEntity(p) instanceof CavernDevice b)b.owner(player);if(who instanceof Player player&&l.getBlockEntity(p) instanceof CavernControl control)control.owner(player);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player who,BlockHitResult hit){
        if(!Menagerie.mayChange(l,who,p))return InteractionResult.FAIL;
        if(who instanceof ServerPlayer sp&&l.getBlockEntity(p) instanceof CavernControl control){if(!control.permitted(sp))return InteractionResult.FAIL;if(name.equals("nuclear_bomb")){if(sp.isShiftKeyDown())control.disarm(sp);else sp.sendOverlayMessage(Component.literal("Use flint and steel or a linked detonator to arm. Crouch-use to disarm."));return InteractionResult.SUCCESS;}if(name.equals("copper_valve")){toggle(l,p,s);return InteractionResult.SUCCESS;}}

        if(!l.isClientSide()&&l.getBlockEntity(p) instanceof CavernDevice device)device.claimIfUnowned(who);
        if(name.equals("beholder")&&who instanceof ServerPlayer sp){CaveObservation.open(sp,p);return InteractionResult.SUCCESS;}
        if((name.equals("dinosaur_chop")||name.equals("cooked_dinosaur_chop"))&&who.canEat(false)){
            if(!l.isClientSide()){who.getFoodData().eat(name.startsWith("cooked")?6:2,.5F);int bites=Integer.parseInt(value(s,"bites"));var prop=stateDefinition.getProperty("bites");if(prop.getValue(Integer.toString(bites+1)).isPresent())l.setBlock(p,set(s,"bites",Integer.toString(bites+1)),UPDATE_ALL);else l.removeBlock(p,false);}return InteractionResult.SUCCESS;
        }
        if(name.equals("giant_sweetberry")){if(!l.isClientSide()){Block.popResource(l,p,new ItemStack(Items.SWEET_BERRIES,6));l.removeBlock(p,false);}return InteractionResult.SUCCESS;}
        if(name.equals("mussel")&&l.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER)){
            int count=Integer.parseInt(value(s,"mussels"));if(count>1){if(!l.isClientSide()){Block.popResource(l,p,new ItemStack(Menagerie.item("cavern_mussel")));l.setBlock(p,set(s,"mussels",Integer.toString(count-1)),UPDATE_ALL);}return InteractionResult.SUCCESS;}
        }
        if(name.equals("spelunkery_table")){if(who instanceof ServerPlayer player)CaveWorkshop.open(player,p);return InteractionResult.SUCCESS;}
        if(name.equals("nuclear_furnace_component")&&l instanceof ServerLevel server){var controller=CavernDevice.controller(server,p);if(controller!=null){if(!Menagerie.mayChange(server,who,controller.getBlockPos()))return InteractionResult.FAIL;controller.claimIfUnowned(who);who.openMenu(controller);return InteractionResult.SUCCESS;}}
        return super.useWithoutItem(s,l,p,who,hit);
    }
    @Override protected InteractionResult useItemOn(ItemStack held,BlockState s,Level l,BlockPos p,Player who,InteractionHand hand,BlockHitResult hit){
        if(!Menagerie.mayChange(l,who,p))return InteractionResult.FAIL;
        if(name.equals("nuclear_bomb")&&held.is(Items.FLINT_AND_STEEL)){if(who instanceof ServerPlayer sp&&l.getBlockEntity(p) instanceof CavernControl control&&control.arm(sp))held.hurtAndBreak(1,sp,hand);return InteractionResult.SUCCESS;}
        if(name.equals("drain")&&held.is(Items.BUCKET)){
            var at=p.relative(facing(s).getOpposite());if(!Menagerie.mayChange(l,who,at))return InteractionResult.FAIL;var source=l.getBlockState(at);
            if(!l.isClientSide()&&source.getBlock() instanceof BucketPickup pickup){var bucket=pickup.pickupBlock(who,l,at,source);if(!bucket.isEmpty())who.setItemInHand(hand,ItemUtils.createFilledResult(held,who,bucket));}return InteractionResult.SUCCESS;
        }
        return super.useItemOn(held,s,l,p,who,hand,hit);
    }
    @Override protected boolean isSignalSource(BlockState s){return name.equals("copper_valve")||super.isSignalSource(s);}
    @Override public void stepOn(Level l,BlockPos p,BlockState s,Entity e){
        super.stepOn(l,p,s,e);if(l instanceof ServerLevel server&&e instanceof LivingEntity living&&e.tickCount%20==0&&Set.of("primal_magma","fissure_primal_magma","geothermal_vent","flood_basalt").contains(name)&&!e.fireImmune())living.hurtServer(server,l.damageSources().hotFloor(),2);
    }
    @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource random){
        if(name.equals("mussel")&&l.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER)&&random.nextInt(12)==0){int count=Integer.parseInt(value(s,"mussels"));var prop=stateDefinition.getProperty("mussels");if(prop.getValue(Integer.toString(count+1)).isPresent())l.setBlock(p,set(s,"mussels",Integer.toString(count+1)),UPDATE_ALL);}
        if(name.endsWith("_egg")&&random.nextInt(12)==0){
            String source=name.substring(0,name.length()-4);var type=Menagerie.TYPES.get("cavern_"+source);if(type==null||l.getEntitiesOfClass(MenagerieAnimal.class,new AABB(p).inflate(16)).size()>=12)return;
            var mob=type.create(l,EntitySpawnReason.BREEDING);if(mob==null)return;mob.setBaby(true);mob.snapTo(p.getX()+.5,p.getY()+1,p.getZ()+.5,0,0);
            if(l.noCollision(mob)&&l.addFreshEntity(mob))l.removeBlock(p,false);return;
        }
        if(name.equals("gobthumper"))for(var worm:l.getEntitiesOfClass(MenagerieAnimal.class,new AABB(p).inflate(24),a->a.species().source().equals("gum_worm")&&!a.sitting()).stream().limit(4).toList())worm.getNavigation().moveTo(p.getX()+.5,p.getY()+1,p.getZ()+.5,1);
        if(name.equals("amber_monolith")&&random.nextInt(80)==0){
            var nearby=l.getEntitiesOfClass(MenagerieAnimal.class,new AABB(p).inflate(24),a->a.owner()==null&&!a.species().mood().equals("hostile")&&a.species().health()<150);
            if(nearby.isEmpty()||nearby.size()>=8)return;var parent=nearby.get(random.nextInt(nearby.size()));var baby=Menagerie.TYPES.get(parent.species().id()).create(l,EntitySpawnReason.BREEDING);if(baby==null)return;
            baby.setBaby(true);baby.snapTo(parent.getX()+2,parent.getY(),parent.getZ(),0,0);if(l.noCollision(baby))l.addFreshEntity(baby);
        }
    }
}
