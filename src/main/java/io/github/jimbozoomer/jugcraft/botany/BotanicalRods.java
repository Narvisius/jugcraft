package io.github.jimbozoomer.jugcraft.botany;
import net.minecraft.world.level.ClipContext;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;

/** Physical construction, native water placement and sustained mana-powered tools. */
public final class BotanicalRods {
    static final Set<String> ITEMS=Set.of("dirt_rod","cobble_rod","skydirt_rod","water_rod","tornado_rod","smelt_rod");
    private record Flight(ItemStack rod,int elapsed){}
    private record Smelt(ItemStack rod,BlockPos pos,BlockState original,int progress){}
    private static final Map<ServerPlayer,Flight> FLIGHTS=new WeakHashMap<>();
    private static final Map<ServerPlayer,Smelt> SMELTS=new WeakHashMap<>();
    static Item create(String name,Item.Properties p){return new Rod(name,p.stacksTo(1));}
    static void register(){
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{FLIGHTS.clear();SMELTS.clear();});
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(var p:List.copyOf(FLIGHTS.keySet())){
                var f=FLIGHTS.get(p);if(!p.isAlive()||p.isSpectator()||p.hasDisconnected()||f.elapsed>=80){active(f.rod,false);FLIGHTS.remove(p);continue;}
                boolean held=f.rod.isEmpty()||p.getMainHandItem()==f.rod||p.getOffhandItem()==f.rod;
                if(f.elapsed<20&&held){double boost=BotanicalArmor.fullSet(p).equals("manaweave")?1.6:1.25;var motion=p.getDeltaMovement();
                    if(p.isFallFlying()){var look=p.getLookAngle();motion=motion.add(look.scale(.1)).add(look.scale(boost*1.2).subtract(motion).scale(.5));}
                    else motion=new Vec3(motion.x,boost,motion.z);
                    p.setDeltaMovement(motion);p.syncVelocity=true;p.level().sendParticles(ParticleTypes.CLOUD,p.getX(),p.getY(),p.getZ(),2,.15,.05,.15,.01);
                }
                p.fallDistance=0;if(f.elapsed==19||!held)active(f.rod,false);FLIGHTS.put(p,new Flight(f.rod,held?f.elapsed+1:Math.max(20,f.elapsed+1)));
            }
            SMELTS.keySet().removeIf(p->!p.isAlive()||!p.isUsingItem()||p.getUseItem()!=SMELTS.get(p).rod);
        });
    }
    static void avatarBoost(ServerPlayer p){FLIGHTS.put(p,new Flight(ItemStack.EMPTY,0));}
    private static void active(ItemStack s,boolean value){if(s.isEmpty())return;s.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(List.of(),List.of(value),List.of(),List.of()));}
    private static boolean affordable(ServerPlayer p,int cost){return p.hasInfiniteMaterials()||BotanicalEquipment.available(p)>=BotanicalArmor.price(p,cost);}
    private static boolean pay(ServerPlayer p,int cost){return p.hasInfiniteMaterials()||BotanicalEquipment.pay(p,cost);}
    private static boolean allowed(ServerPlayer p,BlockPos at){return p.level().hasChunkAt(at)&&p.level().getWorldBorder().isWithinBounds(at)&&Authority.mayChange(p.level(),p,at);}
    private static InteractionResult place(UseOnContext c,Block block,int cost){
        if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(!affordable(p,cost)||!allowed(p,c.getClickedPos())||!allowed(p,c.getClickedPos().relative(c.getClickedFace())))return InteractionResult.FAIL;
        var material=new ItemStack(block);if(!(material.getItem() instanceof BlockItem item))return InteractionResult.FAIL;
        var context=new BlockPlaceContext(new UseOnContext(p.level(),p,c.getHand(),material,new BlockHitResult(c.getClickLocation(),c.getClickedFace(),c.getClickedPos(),c.isInside())));
        var at=context.getClickedPos();if(p.level().getBlockEntity(at)!=null)return InteractionResult.FAIL;
        var before=p.level().getBlockState(at);var result=item.place(context);
        if(result.consumesAction()&&!pay(p,cost)){p.level().setBlockAndUpdate(at,before);return InteractionResult.FAIL;}return result;
    }
    private static InteractionResult water(UseOnContext c){
        if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        var pos=c.getClickedPos();var state=p.level().getBlockState(pos);
        boolean cauldron=state.is(Blocks.CAULDRON)||state.is(Blocks.WATER_CAULDRON)&&state.getValue(LayeredCauldronBlock.LEVEL)<3;
        var at=state.getBlock() instanceof LiquidBlockContainer?pos:pos.relative(c.getClickedFace());
        if(!allowed(p,pos)||!allowed(p,at)||!affordable(p,75))return InteractionResult.FAIL;
        if(cauldron){if(!pay(p,75))return InteractionResult.FAIL;p.level().setBlockAndUpdate(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3));return InteractionResult.SUCCESS;}
        if(p.level().getFluidState(at).isSource()&&p.level().getFluidState(at).is(net.minecraft.tags.FluidTags.WATER))return InteractionResult.PASS;
        boolean result=((BucketItem)Items.WATER_BUCKET).emptyContents(p,p.level(),at,new BlockHitResult(c.getClickLocation(),c.getClickedFace(),pos,c.isInside()));
        if(result){pay(p,75);return InteractionResult.SUCCESS;}return InteractionResult.FAIL;
    }
    private static void smelt(ServerPlayer p,ItemStack rod){
        Vec3 eye=p.getEyePosition(),end=eye.add(p.getLookAngle().scale(32));for(int i=0;i<=32;i++)if(!p.level().hasChunkAt(BlockPos.containing(eye.lerp(end,i/32D)))){SMELTS.remove(p);return;}
        var hit=p.level().clip(new ClipContext(eye,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
        if(hit.getType()!=HitResult.Type.BLOCK||!allowed(p,hit.getBlockPos())||p.level().getBlockEntity(hit.getBlockPos())!=null){SMELTS.remove(p);return;}
        var pos=hit.getBlockPos();var state=p.level().getBlockState(pos);if(state.getDestroySpeed(p.level(),pos)<0){SMELTS.remove(p);return;}
        var input=new SingleRecipeInput(new ItemStack(state.getBlock()));var recipe=p.level().getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,p.level());
        if(recipe.isEmpty()){SMELTS.remove(p);return;}var output=recipe.get().value().assemble(input);
        if(output.getCount()!=1||!(output.getItem() instanceof BlockItem block)||block.getBlock() instanceof EntityBlock){SMELTS.remove(p);return;}
        var old=SMELTS.get(p);int progress=old!=null&&old.rod==rod&&old.pos.equals(pos)&&old.original.equals(state)?old.progress:0;
        if(!pay(p,30)){SMELTS.remove(p);return;}progress++;
        p.level().sendParticles(ParticleTypes.SMALL_FLAME,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,2,.3,.1,.3,0);
        if(progress>=10){p.level().setBlockAndUpdate(pos,block.getBlock().defaultBlockState());SMELTS.remove(p);p.level().playSound(null,pos,BotanicalSounds.get("smelt_rod"),net.minecraft.sounds.SoundSource.PLAYERS,1,1);}
        else SMELTS.put(p,new Smelt(rod,pos.immutable(),state,progress));
    }
    private static final class Rod extends Item {
        final String name;Rod(String name,Properties p){super(p);this.name=name;}
        @Override public InteractionResult useOn(UseOnContext c){return switch(name){case "dirt_rod","skydirt_rod"->place(c,Blocks.DIRT,75);case "cobble_rod"->place(c,Blocks.COBBLESTONE,150);case "water_rod"->water(c);default->super.useOn(c);};}
        @Override public InteractionResult use(Level level,Player user,InteractionHand hand){
            if(!(user instanceof ServerPlayer p))return InteractionResult.SUCCESS;var stack=p.getItemInHand(hand);
            if(name.equals("smelt_rod")){p.startUsingItem(hand);return InteractionResult.CONSUME;}
            if(name.equals("tornado_rod")){if(FLIGHTS.containsKey(p)||p.getCooldowns().isOnCooldown(stack)||!pay(p,350))return InteractionResult.FAIL;FLIGHTS.put(p,new Flight(stack,0));active(stack,true);p.getCooldowns().addCooldown(stack,80);return InteractionResult.SUCCESS;}
            if(name.equals("skydirt_rod")){var at=BlockPos.containing(p.getBoundingBox().getCenter().add(p.getLookAngle().scale(3)));if(!level.getBlockState(at).isAir())return InteractionResult.FAIL;return place(new UseOnContext(level,p,hand,stack,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false)),Blocks.DIRT,150);}
            if(name.equals("water_rod")){var hit=getPlayerPOVHitResult(level,p,ClipContext.Fluid.NONE);if(hit.getType()==HitResult.Type.BLOCK)return water(new UseOnContext(level,p,hand,stack,hit));}
            return InteractionResult.PASS;
        }
        @Override public int getUseDuration(ItemStack stack,LivingEntity user){return 72000;}
        @Override public ItemUseAnimation getUseAnimation(ItemStack stack){return ItemUseAnimation.BOW;}
        @Override public void onUseTick(Level level,LivingEntity user,ItemStack stack,int remaining){if(name.equals("smelt_rod")&&user instanceof ServerPlayer p)smelt(p,stack);}
    }
}
