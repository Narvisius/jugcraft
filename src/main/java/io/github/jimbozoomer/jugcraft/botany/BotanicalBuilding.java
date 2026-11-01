package io.github.jimbozoomer.jugcraft.botany;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Bounded material-backed building and a saved, non-destructive survey guide. */
public final class BotanicalBuilding {
    public static DataComponentType<ItemStack> MATERIAL;
    public static DataComponentType<Integer> SIZE,RADIUS;
    public static DataComponentType<Boolean> SPHERE;
    public static DataComponentType<GlobalPos> CENTER;
    static void register(){
        MATERIAL=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_builder_material"),DataComponentType.<ItemStack>builder().persistent(ItemStack.CODEC).networkSynchronized(ItemStack.STREAM_CODEC).build());
        SIZE=integer("botanical_builder_size",3,11);RADIUS=integer("botanical_survey_radius",1,256);
        SPHERE=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_survey_sphere"),DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
        CENTER=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_survey_center"),DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());
    }
    private static DataComponentType<Integer> integer(String id,int min,int max){return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id(id),DataComponentType.<Integer>builder().persistent(Codec.intRange(min,max)).networkSynchronized(ByteBufCodecs.VAR_INT).build());}
    private static boolean ordinary(ItemStack s){return s.getItem() instanceof BlockItem b&&!(b.getBlock() instanceof EntityBlock)&&!(b.getBlock() instanceof DoorBlock)&&!(b.getBlock() instanceof BedBlock)&&!(b.getBlock() instanceof DoublePlantBlock)&&ItemStack.isSameItemSameComponents(s,new ItemStack(s.getItem()));}
    private record Supply(ItemStack stack,ItemStack talisman,Runnable dirty){
        void consumed(ItemStack material){if(talisman.isEmpty())stack.shrink(1);else BotanicalBlackHole.take(talisman,material);dirty.run();}
    }
    private static Supply supply(ServerPlayer p,ItemStack material){
        for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(!s.isEmpty()&&ItemStack.isSameItemSameComponents(s,material))return new Supply(s,ItemStack.EMPTY,p.getInventory()::setChanged);}
        boolean ender=false;
        for(int i=0;i<36;i++){var s=p.getInventory().getItem(i);if(BotanicalBlackHole.supplies(s,material))return new Supply(ItemStack.EMPTY,s,p.getInventory()::setChanged);if(s.is(Jugtania.item("ender_hand")))ender=true;}
        if(ender)for(int i=0;i<p.getEnderChestInventory().getContainerSize();i++){var s=p.getEnderChestInventory().getItem(i);if(!s.isEmpty()&&ItemStack.isSameItemSameComponents(s,material))return new Supply(s,ItemStack.EMPTY,p.getEnderChestInventory()::setChanged);}
        return null;
    }
    public static final class Astrolabe extends Item {
        Astrolabe(Properties p){super(p.stacksTo(1));}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(!player.isShiftKeyDown())return InteractionResult.PASS;
            if(player instanceof ServerPlayer p){var s=p.getItemInHand(hand);int size=s.getOrDefault(SIZE,3);size=size>=11?3:size+2;s.set(SIZE,size);p.sendOverlayMessage(Component.literal("Building area: "+size+" × "+size));}return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;if(p.isSpectator())return InteractionResult.FAIL;
            var tool=c.getItemInHand();var level=p.level();var clicked=c.getClickedPos();
            if(!level.hasChunkAt(clicked)||!Authority.mayChange(level,p,clicked))return InteractionResult.FAIL;
            if(p.isShiftKeyDown()){var sample=new ItemStack(level.getBlockState(clicked).getBlock());if(!ordinary(sample))return InteractionResult.FAIL;tool.set(MATERIAL,sample);p.sendOverlayMessage(Component.literal("Selected ").append(sample.getHoverName()));return InteractionResult.SUCCESS;}
            var material=tool.getOrDefault(MATERIAL,ItemStack.EMPTY);if(!ordinary(material)||p.getCooldowns().isOnCooldown(tool))return InteractionResult.FAIL;
            int size=Math.clamp(tool.getOrDefault(SIZE,3),3,11),cost=size*320;
            if(!p.hasInfiniteMaterials()&&BotanicalManaItems.available(p)<cost){p.sendOverlayMessage(Component.literal("Needs "+cost+" mana."));return InteractionResult.FAIL;}
            var startContext=new BlockPlaceContext(new UseOnContext(level,p,c.getHand(),material.copy(),new BlockHitResult(c.getClickLocation(),c.getClickedFace(),clicked,c.isInside())));
            var start=startContext.getClickedPos();var axis=c.getClickedFace().getAxis();int made=0;boolean paid=p.hasInfiniteMaterials();
            for(int a=-size/2;a<=size/2;a++)for(int b=axis==Direction.Axis.Y?-size/2:0;b<=(axis==Direction.Axis.Y?size/2:size-1);b++){
                var at=axis==Direction.Axis.Y?start.offset(a,0,b):axis==Direction.Axis.X?start.offset(0,b,a):start.offset(a,b,0);
                if(!level.hasChunkAt(at)||!level.getWorldBorder().isWithinBounds(at)||level.isOutsideBuildHeight(at)||!Authority.mayChange(level,p,at)||level.getBlockEntity(at)!=null)continue;
                var source=supply(p,material);if(source==null&&!p.hasInfiniteMaterials())continue;
                var placed=material.copyWithCount(1);var hit=new BlockHitResult(Vec3.atCenterOf(at),c.getClickedFace(),at,false);
                var context=new BlockPlaceContext(new UseOnContext(level,p,c.getHand(),placed,hit)){@Override public BlockPos getClickedPos(){return at;}};
                if(!level.getBlockState(at).canBeReplaced(context))continue;
                // Native placement handles collision, support, waterlogging and platform placement hooks.
                if(!paid){if(!BotanicalManaItems.pay(p,cost))break;paid=true;}
                var before=level.getBlockState(at);var result=((BlockItem)placed.getItem()).place(context);
                if(!result.consumesAction()||level.getBlockState(at).equals(before))continue;
                if(!p.hasInfiniteMaterials()){source.consumed(material);}made++;
            }
            p.getCooldowns().addCooldown(tool,8);p.sendOverlayMessage(Component.literal("Placed "+made+" blocks. Uses inventory, Black Hole Talisman and your Ender Chest while carrying an Ender Hand."));
            return made>0?InteractionResult.SUCCESS:InteractionResult.FAIL;
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<Component> text,TooltipFlag f){text.accept(Component.literal("Sneak-click a block to select it. Sneak-use in air changes area."));var m=s.get(MATERIAL);if(m!=null)text.accept(Component.literal(s.getOrDefault(SIZE,3)+" × "+s.getOrDefault(SIZE,3)+" · ").append(m.getHoverName()));}
    }
    public static final class Sextant extends Item {
        Sextant(Properties p){super(p.stacksTo(1));}
        @Override public InteractionResult use(Level level,Player p,InteractionHand hand){
            var s=p.getItemInHand(hand);
            if(p.isShiftKeyDown()){if(!level.isClientSide()){if(s.has(CENTER)){s.remove(CENTER);s.remove(RADIUS);}else s.set(SPHERE,!s.getOrDefault(SPHERE,false));p.sendOverlayMessage(Component.literal(s.getOrDefault(SPHERE,false)?"Sphere guide":"Circle guide"));}return InteractionResult.SUCCESS;}
            var hit=p.pick(128,0,false);if(!(hit instanceof BlockHitResult b)||hit.getType()!=HitResult.Type.BLOCK||!level.hasChunkAt(b.getBlockPos()))return InteractionResult.PASS;
            if(!level.isClientSide()){s.set(CENTER,GlobalPos.of(level.dimension(),b.getBlockPos().above()));s.remove(RADIUS);}p.startUsingItem(hand);return InteractionResult.CONSUME;
        }
        @Override public int getUseDuration(ItemStack s,LivingEntity p){return 72000;}
        @Override public ItemUseAnimation getUseAnimation(ItemStack s){return ItemUseAnimation.BOW;}
        public static int radius(ItemStack s,LivingEntity p){
            var center=s.get(CENTER);if(center==null||!center.dimension().equals(p.level().dimension()))return 0;
            var eye=p.getEyePosition();var look=p.getLookAngle();if(Math.abs(look.y)<.0001)return 0;
            double t=(center.pos().getY()-eye.y)/look.y;if(t<=0)return 0;
            var point=eye.add(look.scale(t));double dx=point.x-center.pos().getX()-.5,dz=point.z-center.pos().getZ()-.5;double r=Math.sqrt(dx*dx+dz*dz);return r>256?0:Math.max(1,(int)Math.round(r));
        }
        @Override public boolean releaseUsing(ItemStack s,Level level,LivingEntity user,int remaining){
            if(!level.isClientSide()){int r=radius(s,user);if(r>0)s.set(RADIUS,r);else{s.remove(CENTER);s.remove(RADIUS);}if(user instanceof Player p)p.sendOverlayMessage(Component.literal(r>0?"Guide radius: "+r+" blocks. Sneak-use clears it; sneak-use again changes shape.":"Aim below the horizon within a radius of 256 blocks."));}return true;
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<Component> text,TooltipFlag f){text.accept(Component.literal("Hold use on a block, aim outward, then release to fix the guide."));text.accept(Component.literal((s.getOrDefault(SPHERE,false)?"Sphere":"Circle")+" · sneak-use clears / changes shape"));}
    }
}
