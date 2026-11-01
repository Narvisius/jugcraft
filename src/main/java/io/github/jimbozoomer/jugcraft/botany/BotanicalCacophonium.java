package io.github.jimbozoomer.jugcraft.botany;
import net.minecraft.world.entity.monster.cubemob.Slime;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.*;

/** Records a creature's actual registered ambient sound; never stores or executes foreign entity code. */
public final class BotanicalCacophonium {
    public static DataComponentType<String> SOUND;
    private final BotanicalWorkshop host;private boolean powered;private long lastPlayed=Long.MIN_VALUE;
    BotanicalCacophonium(BotanicalWorkshop host){this.host=host;}
    static void register(){SOUND=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_recorded_sound"),DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());}
    static SoundEvent sound(ItemStack stack){
        if(stack.isEmpty())return null;
        if(stack.getHoverName().getString().equalsIgnoreCase("shia labeouf"))return BotanicalSounds.get("doit");
        var id=Identifier.tryParse(stack.getOrDefault(SOUND,""));return id==null?null:BuiltInRegistries.SOUND_EVENT.getOptional(id).orElse(null);
    }
    private static void play(ServerLevel level,BlockPos at,ItemStack stack){var sound=sound(stack);if(sound==null)return;level.playSound(null,at,sound,SoundSource.BLOCKS,.9F,1+(level.getRandom().nextFloat()-level.getRandom().nextFloat())*.2F);level.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE,at.getX()+.5,at.getY()+1.2,at.getZ()+.5,0,level.getRandom().nextInt(25)/24.0,0,0,1);}
    void tick(ServerLevel level){boolean now=level.hasNeighborSignal(host.getBlockPos());if(now&&!powered)play(level);if(now!=powered){powered=now;host.setChanged();}}
    void play(ServerLevel level){if(lastPlayed!=Long.MIN_VALUE&&level.getGameTime()>=lastPlayed&&level.getGameTime()-lastPlayed<6)return;lastPlayed=level.getGameTime();play(level,host.getBlockPos(),host.getItem(0));host.setChanged();}
    void save(ValueOutput out){out.putBoolean("recorded_sound_power",powered);out.putLong("recorded_sound_played",lastPlayed);}
    void load(ValueInput in){powered=in.getBooleanOr("recorded_sound_power",false);lastPlayed=in.getLongOr("recorded_sound_played",Long.MIN_VALUE);}
    static final class Instrument extends Item {
        Instrument(Properties p){super(p.stacksTo(1));}
        @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){
            if(!(target instanceof Mob mob))return InteractionResult.PASS;
            if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!Authority.mayChange(p.level(),p,target.blockPosition()))return InteractionResult.FAIL;
            SoundEvent sample=mob instanceof Creeper?SoundEvents.CREEPER_PRIMED:mob instanceof Slime slime?(slime.isTiny()?SoundEvents.SLIME_SQUISH_SMALL:SoundEvents.SLIME_SQUISH):((io.github.jimbozoomer.jugcraft.mixin.BotanicalMobSoundAccess)mob).jugcraft$ambientSound();
            if(sample==null)return InteractionResult.PASS;stack.set(SOUND,BuiltInRegistries.SOUND_EVENT.getKey(sample).toString());p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Recorded: ").append(target.getType().getDescription()));return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;var at=c.getClickedPos();var held=c.getItemInHand();
            if(!(p.level().getBlockState(at).getBlock() instanceof NoteBlock)||sound(held)==null)return InteractionResult.PASS;
            if(!Authority.mayChange(p.level(),p,at))return InteractionResult.FAIL;var old=p.level().getBlockState(at);
            if(!p.level().setBlockAndUpdate(at,Jugtania.block("cacophonium_block").defaultBlockState()))return InteractionResult.FAIL;
            if(!(p.level().getBlockEntity(at) instanceof BotanicalWorkshop w)){p.level().setBlockAndUpdate(at,old);return InteractionResult.FAIL;}
            w.owner(p.getUUID());w.setItem(0,held.copyWithCount(1));if(!p.hasInfiniteMaterials())held.shrink(1);return InteractionResult.SUCCESS;
        }
        @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return 72000;}
        @Override public ItemUseAnimation getUseAnimation(ItemStack stack){return ItemUseAnimation.TOOT_HORN;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){if(sound(player.getItemInHand(hand))==null)return InteractionResult.PASS;player.startUsingItem(hand);return InteractionResult.CONSUME;}
        @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){if(entity instanceof ServerPlayer p&&remaining%6==0)play(p.level(),p.blockPosition(),stack);}
    }
}
