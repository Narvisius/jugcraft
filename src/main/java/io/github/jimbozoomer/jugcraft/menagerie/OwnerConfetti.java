package io.github.jimbozoomer.jugcraft.menagerie;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Supplied party poppers: server-authoritative use with one bounded visual packet. */
public final class OwnerConfetti extends BlockItem {
    public interface PartyCreeper {boolean jugcraft$isParty();void jugcraft$setParty(boolean value);}
    public static final SimpleParticleType CONFETTI=net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple();
    public static final ParticleType<ColorParticleOption> DYED=new ParticleType<>(false){
        public com.mojang.serialization.MapCodec<ColorParticleOption> codec(){return ColorParticleOption.codec(this);}
        public StreamCodec<? super RegistryFriendlyByteBuf,ColorParticleOption> streamCodec(){return ColorParticleOption.streamCodec(this);}
    };
    public record Burst(Vec3 pos,Vec3 velocity,List<Integer> colors,int count,boolean radial) implements CustomPacketPayload{
        public static final Type<Burst> TYPE=new Type<>(Jugcraft.id("supps_confetti"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Burst> CODEC=StreamCodec.of((b,p)->{
            b.writeDouble(p.pos.x);b.writeDouble(p.pos.y);b.writeDouble(p.pos.z);b.writeFloat((float)p.velocity.x);b.writeFloat((float)p.velocity.y);b.writeFloat((float)p.velocity.z);
            b.writeVarInt(p.colors.size());for(int color:p.colors)b.writeInt(color);b.writeVarInt(p.count);b.writeBoolean(p.radial);
        },b->{var pos=new Vec3(b.readDouble(),b.readDouble(),b.readDouble());var v=new Vec3(b.readFloat(),b.readFloat(),b.readFloat());int n=b.readVarInt();if(n<0||n>8)throw new IllegalArgumentException("Confetti palette too large");var colors=new ArrayList<Integer>();for(int i=0;i<n;i++)colors.add(b.readInt());return new Burst(pos,v,List.copyOf(colors),Math.clamp(b.readVarInt(),0,128),b.readBoolean());});
        public Type<Burst> type(){return TYPE;}
    }
    public OwnerConfetti(Block block,Properties p){super(block,p);}
    public static void register(){
        Registry.register(BuiltInRegistries.PARTICLE_TYPE,Jugcraft.id("supps_confetti"),CONFETTI);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE,Jugcraft.id("supps_confetti_dyed"),DYED);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("supps_confetti_dye"),OwnerConfettiDyeRecipe.SERIALIZER);
        PayloadTypeRegistry.clientboundPlay().register(Burst.TYPE,Burst.CODEC);
    }
    public static List<Integer> colors(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().read("SuppsConfettiColors",Codec.INT.listOf()).orElse(List.of()).stream().limit(8).map(c->0xFF000000|c).toList();}
    @Override protected boolean canPlace(BlockPlaceContext context,BlockState state){return (context.isSecondaryUseActive()||context.getLevel().getBlockState(context.getClickedPos()).is(getBlock()))&&super.canPlace(context,state);}
    @Override public InteractionResult use(Level level,Player user,InteractionHand hand){
        if(user.isSpectator())return InteractionResult.PASS;
        if(user instanceof ServerPlayer player){
            var stack=player.getItemInHand(hand);float pitch=player.getXRot()-(float)(20*Math.cos(Math.toRadians(player.getXRot())));
            Vec3 direction=Vec3.directionFromRotation(pitch,player.getYRot());
            burst(player.level(),new Burst(player.getEyePosition().add(player.getLookAngle().scale(.2)).add(0,-.25,0),direction,colors(stack),48,false));
            player.getCooldowns().addCooldown(stack,10);stack.consume(1,player);
        }return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){
        if(!(target instanceof Creeper creeper)||!(target instanceof PartyCreeper party)||party.jugcraft$isParty()||creeper.isIgnited())return InteractionResult.PASS;
        if(!Menagerie.mayChange(player.level(),player,target.blockPosition()))return InteractionResult.FAIL;
        if(!player.level().isClientSide()){party.jugcraft$setParty(true);stack.consume(1,player);}
        return InteractionResult.SUCCESS;
    }
    public static void burst(ServerLevel level,Burst packet){
        for(var player:level.players())if(player.distanceToSqr(packet.pos())<=64*64)ServerPlayNetworking.send(player,packet);
        level.playSound(null,packet.pos.x,packet.pos.y,packet.pos.z,OwnerSupplementaries.SOUNDS.get("confetti_popper"),SoundSource.PLAYERS,1,1);
    }
}
