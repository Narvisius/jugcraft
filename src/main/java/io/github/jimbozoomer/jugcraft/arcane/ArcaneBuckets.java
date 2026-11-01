package io.github.jimbozoomer.jugcraft.arcane;
import net.minecraft.world.level.ClipContext;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.HitResult;

/** Source capacities, with transactional container exchanges and no mixed-fluid conversion. */
public final class ArcaneBuckets extends Item {
    public static DataComponentType<Integer> AMOUNT;
    public final String kind;
    public final int capacity;
    ArcaneBuckets(String name,Properties p){super(p.stacksTo(name.equals("edelwood_bucket")?16:1));kind=name.replace("edelwood_","").replace("_bucket","");capacity=kind.equals("lava")||kind.equals("powder_snow")?3:4;}
    public static void registerComponent(){AMOUNT=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("arcane_bucket_amount"),DataComponentType.<Integer>builder().persistent(Codec.intRange(1,4)).networkSynchronized(ByteBufCodecs.VAR_INT).build());}
    public static int amount(ItemStack stack){return stack.getItem() instanceof ArcaneBuckets b&&!b.kind.equals("bucket")?Math.clamp(stack.getOrDefault(AMOUNT,1),1,b.capacity):0;}
    public static ItemStack craftingRemainder(ItemStack stack){return stack.getItem() instanceof ArcaneBuckets bucket&&amount(stack)>0?filled(stack,bucket.kind,amount(stack)-1):ItemStack.EMPTY;}
    @Override public void inventoryTick(ItemStack stack,net.minecraft.server.level.ServerLevel level,Entity holder,EquipmentSlot slot){
        if(!kind.equals("lava")||!(holder instanceof ServerPlayer player)||player.hasInfiniteMaterials()||level.getRandom().nextDouble()>=.005)return;
        var at=player.blockPosition();if(!level.isEmptyBlock(at))return;
        // Lava spreads beyond its source. Reserve the possible horizontal spread before creating it.
        for(var p:BlockPos.betweenClosed(at.offset(-4,-1,-4),at.offset(4,0,4)))if(!level.hasChunkAt(p)||!Authority.mayChange(level,player,p))return;
        for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i)==stack){if(level.setBlockAndUpdate(at,Blocks.LAVA.defaultBlockState())){player.getInventory().setItem(i,new ItemStack(Items.CHARCOAL));player.getInventory().setChanged();level.playSound(null,at,SoundEvents.FIRE_EXTINGUISH,SoundSource.PLAYERS,.6F,.6F);}return;}
    }
    private static ItemStack filled(ItemStack before,String kind,int amount){if(amount<=0)return before.transmuteCopy(ForbiddenArcane.item("edelwood_bucket"),1);var next=before.transmuteCopy(ForbiddenArcane.item("edelwood_"+kind+"_bucket"),1);next.set(AMOUNT,amount);return next;}
    private static Fluid fluid(String kind){return kind.equals("water")?Fluids.WATER:kind.equals("lava")?Fluids.LAVA:Fluids.EMPTY;}
    public static void registerStorage(){
        FluidStorage.ITEM.registerForItems((stack,context)->new SingleSlotStorage<FluidVariant>(){
            private ItemStack stack(){return context.getItemVariant().toStack();}
            private String kind(){return stack().getItem() instanceof ArcaneBuckets b?b.kind:"bucket";}
            public boolean isResourceBlank(){return getAmount()==0;}
            public FluidVariant getResource(){return isResourceBlank()?FluidVariant.blank():FluidVariant.of(fluid(kind()));}
            public long getAmount(){return fluid(kind())==Fluids.EMPTY?0:amount(stack())*FluidConstants.BUCKET;}
            public long getCapacity(){return (kind().equals("lava")?3:4)*FluidConstants.BUCKET;}
            public long insert(FluidVariant variant,long limit,TransactionContext tx){if(limit<FluidConstants.BUCKET||variant.getFluid()!=Fluids.WATER&&variant.getFluid()!=Fluids.LAVA||!variant.equals(FluidVariant.of(variant.getFluid())))return 0;String next=variant.getFluid()==Fluids.LAVA?"lava":"water";if(!kind().equals("bucket")&&!kind().equals(next))return 0;int current=amount(stack()),cap=next.equals("lava")?3:4;int units=(int)Math.min(cap-current,limit/FluidConstants.BUCKET);return units>0&&context.exchange(ItemVariant.of(filled(stack(),next,current+units)),1,tx)==1?units*FluidConstants.BUCKET:0;}
            public long extract(FluidVariant variant,long limit,TransactionContext tx){if(limit<FluidConstants.BUCKET||!getResource().equals(variant)||variant.isBlank())return 0;int current=amount(stack()),units=(int)Math.min(current,limit/FluidConstants.BUCKET);return units>0&&context.exchange(ItemVariant.of(filled(stack(),kind(),current-units)),1,tx)==1?units*FluidConstants.BUCKET:0;}
        },ForbiddenArcane.item("edelwood_bucket"),ForbiddenArcane.item("edelwood_water_bucket"),ForbiddenArcane.item("edelwood_lava_bucket"));
    }
    @Override public InteractionResult useOn(UseOnContext c){
        if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;var at=c.getClickedPos();if(!Authority.mayChange(p.level(),p,at))return InteractionResult.FAIL;
        var tank=FluidStorage.SIDED.find(p.level(),at,c.getClickedFace());if(tank==null)return use(c.getLevel(),p,c.getHand());
        var ours=ContainerItemContext.ofPlayerHand(p,c.getHand()).find(FluidStorage.ITEM);if(ours==null)return InteractionResult.PASS;
        try(var tx=Transaction.openOuter()){
            long moved=0;if(kind.equals("bucket")||p.isShiftKeyDown()){for(var view:tank){if(view.isResourceBlank())continue;var f=view.getResource();try(var nested=tx.openNested()){long extracted=view.extract(f,FluidConstants.BUCKET,nested);if(extracted==FluidConstants.BUCKET&&ours.insert(f,extracted,nested)==extracted){nested.commit();moved=extracted;break;}}}}
            else {var f=FluidVariant.of(fluid(kind));if(f.getFluid()!=Fluids.EMPTY&&ours.extract(f,FluidConstants.BUCKET,tx)==FluidConstants.BUCKET&&tank.insert(f,FluidConstants.BUCKET,tx)==FluidConstants.BUCKET)moved=FluidConstants.BUCKET;}
            if(moved==0)return InteractionResult.FAIL;tx.commit();return InteractionResult.SUCCESS;
        }
    }
    @Override public InteractionResult use(Level world,Player user,InteractionHand hand){
        if(kind.equals("milk")){user.startUsingItem(hand);return InteractionResult.CONSUME;}
        var stack=user.getItemInHand(hand);var hit=getPlayerPOVHitResult(world,user,amount(stack)<capacity?ClipContext.Fluid.SOURCE_ONLY:ClipContext.Fluid.NONE);if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResult.PASS;
        if(!(user instanceof ServerPlayer p))return InteractionResult.SUCCESS;var at=hit.getBlockPos();if(!world.hasChunkAt(at)||!Authority.mayChange(p.level(),p,at))return InteractionResult.FAIL;
        var state=world.getBlockState(at);String found=state.is(Blocks.POWDER_SNOW)?"powder_snow":state.getFluidState().isSource()&&state.getFluidState().is(Fluids.WATER)?"water":state.getFluidState().isSource()&&state.getFluidState().is(Fluids.LAVA)?"lava":"";
        if(!found.isEmpty()&&(kind.equals("bucket")||kind.equals(found)&&amount(stack)<capacity)&&state.getBlock() instanceof BucketPickup pickup){
            var taken=pickup.pickupBlock(p,world,at,state);if(taken.isEmpty())return InteractionResult.FAIL;var next=filled(stack,found,amount(stack)+1);p.setItemInHand(hand,ItemUtils.createFilledResult(stack,p,next));pickup.getPickupSound().ifPresent(sound->world.playSound(null,at,sound,SoundSource.PLAYERS,1,1));return InteractionResult.SUCCESS;
        }
        if(kind.equals("bucket"))return InteractionResult.FAIL;
        boolean waterlog=kind.equals("water")&&state.getBlock() instanceof LiquidBlockContainer container&&container.canPlaceLiquid(p,world,at,state,Fluids.WATER);var target=waterlog?at:at.relative(hit.getDirection());
        if(!world.hasChunkAt(target)||!Authority.mayChange(p.level(),p,target)||!world.getWorldBorder().isWithinBounds(target))return InteractionResult.FAIL;
        boolean placed;if(kind.equals("powder_snow")){placed=world.getBlockState(target).canBeReplaced()&&world.setBlockAndUpdate(target,Blocks.POWDER_SNOW.defaultBlockState());if(placed)world.playSound(null,target,SoundEvents.BUCKET_EMPTY_POWDER_SNOW,SoundSource.PLAYERS,1,1);}else placed=((BucketItem)(kind.equals("lava")?Items.LAVA_BUCKET:Items.WATER_BUCKET)).emptyContents(p,world,target,null);
        if(!placed)return InteractionResult.FAIL;if(!p.hasInfiniteMaterials())p.setItemInHand(hand,filled(stack,kind,amount(stack)-1));return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){
        if(!kind.equals("bucket")&&!kind.equals("milk")||amount(stack)>=capacity||target.isBaby()||!(target instanceof net.minecraft.world.entity.animal.cow.Cow||target instanceof net.minecraft.world.entity.animal.goat.Goat))return InteractionResult.PASS;
        if(player instanceof ServerPlayer p){if(!Authority.mayChange(p.level(),p,target.blockPosition())||p.distanceToSqr(target)>36)return InteractionResult.FAIL;p.setItemInHand(hand,ItemUtils.createFilledResult(stack,p,filled(stack,"milk",amount(stack)+1)));p.level().playSound(null,target.blockPosition(),SoundEvents.COW_MILK,SoundSource.PLAYERS,1,1);}return InteractionResult.SUCCESS;
    }
    @Override public int getUseDuration(ItemStack stack,LivingEntity e){return kind.equals("milk")?32:0;}
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack){return kind.equals("milk")?ItemUseAnimation.DRINK:ItemUseAnimation.NONE;}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity e){if(kind.equals("milk")&&!level.isClientSide()){e.removeAllEffects();if(!(e instanceof Player p&&p.hasInfiniteMaterials()))return filled(stack,kind,amount(stack)-1);}return stack;}
}
