package io.github.jimbozoomer.jugcraft.arcane;
import net.minecraft.world.level.ClipContext;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Four separate source essences. Containers never manufacture their contents. */
public final class ArcaneEssences {
    public static DataComponentType<Integer> BLOOD;
    public static AttachmentType<Integer> AUREAL;
    private ArcaneEssences(){}
    public static void register(){
        BLOOD=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("arcane_blood"),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,3000)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        AUREAL=AttachmentRegistry.create(Jugcraft.id("arcane_aureal"),b->b.initializer(()->0).persistent(Codec.intRange(0,10000)).copyOnDeath());
        ServerLivingEntityEvents.AFTER_DAMAGE.register((victim,source,base,dealt,blocked)->{
            if(blocked||dealt<=0||!source.is(DamageTypes.PLAYER_ATTACK)||!(source.getEntity() instanceof ServerPlayer p)||!Authority.mayStrike(p,victim))return;
            if(victim.getType().builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE,Jugcraft.id("forbidden_test_tube_blacklisted"))))return;
            var tube=p.getOffhandItem();if(tube.is(ForbiddenArcane.item("test_tube"))){tube=new ItemStack(ForbiddenArcane.item("blood_test_tube"));p.setItemInHand(InteractionHand.OFF_HAND,tube);}
            if(tube.is(ForbiddenArcane.item("blood_test_tube")))tube.set(BLOOD,Math.clamp(tube.getOrDefault(BLOOD,0)+(int)(20*Math.min(150,dealt)),0,3000));
        });
    }
    public static int kind(ItemStack s){
        if(ArcaneJar.portable(s))return ArcaneJar.kind(s);
        if(s.is(ForbiddenArcane.item("aureal_bottle")))return 0;
        if(s.is(ForbiddenArcane.item("soul"))||s.is(ForbiddenArcane.item("corrupt_soul"))||s.is(ForbiddenArcane.item("enchanted_soul")))return 1;
        if(s.is(ForbiddenArcane.item("blood_test_tube")))return 2;
        if(s.is(ForbiddenArcane.item("xpetrified_orb")))return 3;
        return -1;
    }
    public static int amount(ItemStack s){if(ArcaneJar.portable(s))return ArcaneJar.amount(s);return switch(kind(s)){case 0->35;case 1->s.is(ForbiddenArcane.item("enchanted_soul"))?10:1;case 2->Math.clamp(s.getOrDefault(BLOOD,0),0,3000);case 3->91;default->0;};}
    public static boolean divisible(ItemStack s){return ArcaneJar.portable(s)||s.is(ForbiddenArcane.item("blood_test_tube"));}
    public static ItemStack finishInput(ItemStack s,int amount){if(ArcaneJar.portable(s))return ArcaneJar.drain(s,amount);if(kind(s)==2){int left=Math.max(0,s.getOrDefault(BLOOD,0)-amount);if(left==0)return new ItemStack(ForbiddenArcane.item("test_tube"));s.set(BLOOD,left);}else s.shrink(1);return s;}
    /** Consumes one whole fixed-value item, or only the requested portion of a blood container. */
    public static void consume(ServerPlayer p,InteractionHand hand,int amount){
        p.setItemInHand(hand,finishInput(p.getItemInHand(hand),amount));
    }
    public static final class Orb extends Item {
        Orb(Properties p){super(p.stacksTo(16));}
        @Override public InteractionResult use(Level l,Player p,InteractionHand hand){if(p instanceof ServerPlayer sp){sp.giveExperiencePoints(91);p.getItemInHand(hand).consume(1,p);}return InteractionResult.SUCCESS;}
    }
    public static final class Bottle extends Item {
        Bottle(Properties p){super(p.stacksTo(16));}
        @Override public InteractionResult use(Level l,Player p,InteractionHand hand){p.startUsingItem(hand);return InteractionResult.CONSUME;}
        @Override public int getUseDuration(ItemStack s,LivingEntity e){return 32;}
        @Override public ItemUseAnimation getUseAnimation(ItemStack s){return ItemUseAnimation.DRINK;}
        @Override public ItemStack finishUsingItem(ItemStack s,Level l,LivingEntity e){if(e instanceof ServerPlayer p){p.setAttached(AUREAL,Math.min(10000,p.getAttachedOrCreate(AUREAL)+35));s.consume(1,p);}return s;}
    }
    public static final class Extractor extends Item {
        Extractor(Properties p){super(p.durability(128));}
        @Override public InteractionResult useOn(UseOnContext c){if(c.getPlayer()==null||c.getPlayer().isShiftKeyDown()||!c.getLevel().getBlockState(c.getClickedPos()).is(Blocks.SOUL_SAND))return InteractionResult.PASS;if(c.getPlayer() instanceof ServerPlayer p&&!Authority.mayChange(p.level(),p,c.getClickedPos()))return InteractionResult.FAIL;c.getPlayer().startUsingItem(c.getHand());return InteractionResult.CONSUME;}
        @Override public int getUseDuration(ItemStack s,LivingEntity e){return 35;}
        @Override public ItemUseAnimation getUseAnimation(ItemStack s){return ItemUseAnimation.BOW;}
        @Override public ItemStack finishUsingItem(ItemStack s,Level raw,LivingEntity actor){
            if(!(actor instanceof ServerPlayer p)||p.isShiftKeyDown())return s;var l=p.level();var hit=getPlayerPOVHitResult(l,p,ClipContext.Fluid.SOURCE_ONLY);var at=hit.getBlockPos();
            if(!l.hasChunkAt(at)||!l.getBlockState(at).is(Blocks.SOUL_SAND)||!Authority.mayChange(l,p,at))return s;
            l.setBlockAndUpdate(at,ForbiddenArcane.block("soulless_sand").defaultBlockState());Block.popResource(l,at.above(),new ItemStack(ForbiddenArcane.item("soul")));s.hurtAndBreak(1,p,p.getUsedItemHand().asEquipmentSlot());l.sendParticles(ParticleTypes.SOUL,at.getX()+.5,at.getY()+1,at.getZ()+.5,8,.3,.1,.3,.02);return s;
        }
    }
}
