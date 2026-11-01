package io.github.jimbozoomer.jugcraft.botany;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Full local ore scan spread across ticks; private markers share the existing spectral renderer. */
public final class BotanicalDiviner extends Item {
    private static final TagKey<Block> ORES=TagKey.create(Registries.BLOCK,Identifier.fromNamespaceAndPath("c","ores"));
    private static final Map<ServerPlayer,Scan> SCANS=new WeakHashMap<>();
    private static final class Scan {
        final ServerLevel level;final BlockPos center;final Iterator<BlockPos> cells;final List<BlockPos> hits=new ArrayList<>();int ticks;
        Scan(ServerPlayer p,BlockPos origin,int radius){level=p.level();center=origin.immutable();cells=BlockPos.betweenClosed(center.offset(-radius,-radius,-radius),center.offset(radius,radius,radius)).iterator();}
    }
    BotanicalDiviner(Properties p){super(p.stacksTo(1));}
    static void register(){
        PayloadTypeRegistry.clientboundPlay().register(Marks.TYPE,Marks.CODEC);
        ServerLifecycleEvents.SERVER_STOPPED.register(s->SCANS.clear());
        ServerTickEvents.END_SERVER_TICK.register(s->{for(var p:List.copyOf(SCANS.keySet())){
            var scan=SCANS.get(p);if(!p.isAlive()||p.isSpectator()||p.hasDisconnected()||p.level()!=scan.level||p.blockPosition().distSqr(scan.center)>4096||++scan.ticks>100){SCANS.remove(p);continue;}
            for(int n=0;n<1024&&scan.cells.hasNext();n++){var at=scan.cells.next();if(scan.level.hasChunkAt(at)&&scan.level.getBlockState(at).is(ORES)&&Authority.mayChange(scan.level,p,at))scan.hits.add(at.immutable());}
            if(!scan.cells.hasNext()){
                scan.hits.sort(Comparator.comparingDouble(scan.center::distSqr));ServerPlayNetworking.send(p,new Marks(scan.level.dimension().identifier(),List.copyOf(scan.hits.subList(0,Math.min(128,scan.hits.size())))));
                SCANS.remove(p);
            }
        }});
    }
    static boolean begin(ServerPlayer p,BlockPos center,int radius){if(!p.isAlive()||p.isSpectator()||SCANS.containsKey(p))return false;SCANS.put(p,new Scan(p,center,radius));return true;}
    @Override public InteractionResult use(Level level,Player user,InteractionHand hand){
        if(!(user instanceof ServerPlayer p))return InteractionResult.SUCCESS;var stack=p.getItemInHand(hand);
        if(SCANS.containsKey(p)||p.getCooldowns().isOnCooldown(stack)||!p.hasInfiniteMaterials()&&!BotanicalEquipment.pay(p,3000))return InteractionResult.FAIL;
        SCANS.put(p,new Scan(p,p.blockPosition(),BotanicalArmor.fullSet(p).equals("manaweave")?20:15));p.getCooldowns().addCooldown(stack,200);return InteractionResult.SUCCESS;
    }
    public record Marks(Identifier dimension,List<BlockPos> blocks) implements CustomPacketPayload {
        public static final Type<Marks> TYPE=new Type<>(Jugcraft.id("botanical_divining_marks"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Marks> CODEC=StreamCodec.composite(Identifier.STREAM_CODEC,Marks::dimension,BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(128)),Marks::blocks,Marks::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
