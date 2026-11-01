package io.github.jimbozoomer.jugcraft.botany;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.phys.*;

/** Per-dimension saved force links; moving a relay carries its link rather than leaking it to a later replacement. */
public final class BotanicalForceRelay extends Block {
    private static final ThreadLocal<Integer> DEPTH=ThreadLocal.withInitial(()->0);
    public record Link(BlockPos from,BlockPos to,UUID owner){
        static final Codec<Link> CODEC=RecordCodecBuilder.create(i->i.group(BlockPos.CODEC.fieldOf("from").forGetter(Link::from),BlockPos.CODEC.fieldOf("to").forGetter(Link::to),UUIDUtil.CODEC.fieldOf("owner").forGetter(Link::owner)).apply(i,Link::new));
    }
    public static final class Links extends SavedData {
        static final Codec<Links> CODEC=Link.CODEC.listOf().xmap(Links::new,l->new ArrayList<>(l.links.values()));
        static final SavedDataType<Links> TYPE=new SavedDataType<>(Jugcraft.id("botanical_force_links"),Links::new,CODEC,null);
        final Map<BlockPos,Link> links=new HashMap<>();Links(){}Links(List<Link> list){list.stream().limit(65536).filter(l->l.from.distSqr(l.to)<=256*256).forEach(l->links.put(l.from,l));}
        static Links get(ServerLevel level){return level.getDataStorage().computeIfAbsent(TYPE);}
    }
    BotanicalForceRelay(Properties p){super(p.pushReaction(PushReaction.PUSH));}
    static boolean bind(ServerPlayer p,BlockPos source,BlockPos to){if(source.equals(to)||source.distSqr(to)>256*256||!safe(p,source,to)||!safe(p,source,source)||!p.level().getBlockState(source).is(Jugtania.block("piston_relay")))return false;var data=Links.get(p.level());if(data.links.size()>=65536&&!data.links.containsKey(source))return false;data.links.put(source,new Link(source.immutable(),to.immutable(),p.getUUID()));data.setDirty();return true;}
    static BlockPos destination(ServerLevel level,ServerPlayer p,BlockPos source){var link=Links.get(level).links.get(source);var owner=link==null?null:Authority.answering(level,link.owner);return owner!=null&&safe(p,source,link.to)&&safe(owner,source,link.to)&&Authority.mayChange(level,p,source)?link.to:null;}
    private static boolean safe(ServerPlayer p,BlockPos from,BlockPos to){return p.level().hasChunkAt(from)&&p.level().hasChunkAt(to)&&p.level().getWorldBorder().isWithinBounds(to)&&Authority.mayChange(p.level(),p,from)&&Authority.mayChange(p.level(),p,to)&&ClaimTransfers.allowed(p.level(),from,to);}
    /** Snapshot relay associations before vanilla moves blocks; no map traversal after the source has become moving-piston air. */
    public static List<Link> capture(Level level,BlockPos piston,Direction facing,boolean extend){
        if(!(level instanceof ServerLevel server)||Links.get(server).links.isEmpty())return List.of();var resolver=new PistonStructureResolver(level,piston,facing,extend);if(!resolver.resolve())return List.of();var result=new ArrayList<Link>();var links=Links.get(server).links;
        for(var at:resolver.getToPush()){var link=links.get(at);if(link!=null&&level.getBlockState(at).is(Jugtania.block("piston_relay")))result.add(link);}return result;
    }
    public static void moved(ServerLevel level,List<Link> captured,Direction direction){
        if(captured.isEmpty())return;var data=Links.get(level);for(var link:captured)data.links.remove(link.from);
        for(var link:captured){var newSource=link.from.relative(direction);var actor=Authority.answering(level,link.owner);var target=link.to;
            if(actor!=null&&DEPTH.get()<8&&safe(actor,newSource,target)&&push(actor,target,direction,newSource))target=target.relative(direction);
            data.links.put(newSource,new Link(newSource,target,link.owner));
        }data.setDirty();
    }
    static boolean push(ServerPlayer actor,BlockPos first,Direction direction,BlockPos source){
        if(DEPTH.get()>=8)return false;var level=actor.level();var piston=first.relative(direction.getOpposite());
        // Resolver may explore sticky branches: preflight its complete maximum reach before calling it.
        for(var at:BlockPos.betweenClosed(first.offset(-13,-13,-13),first.offset(13,13,13)))if(!level.hasChunkAt(at))return false;
        var resolver=new PistonStructureResolver(level,piston,direction,true);if(!resolver.resolve())return false;var moving=resolver.getToPush();var breaking=resolver.getToDestroy();if(moving.isEmpty()||moving.size()>12||breaking.size()>12)return false;
        var snapshot=new LinkedHashMap<BlockPos,BlockState>();
        for(var at:moving){if(!safe(actor,source,at)||!safe(actor,source,at.relative(direction))||level.getBlockState(at).hasBlockEntity())return false;snapshot.put(at,level.getBlockState(at));snapshot.putIfAbsent(at.relative(direction),level.getBlockState(at.relative(direction)));}
        for(var at:breaking){if(!safe(actor,source,at)||level.getBlockState(at).hasBlockEntity())return false;snapshot.put(at,level.getBlockState(at));}
        for(var at:moving){var destination=at.relative(direction);if(!level.getEntitiesOfClass(LivingEntity.class,new AABB(destination)).isEmpty())return false;}
        var relayLinks=new ArrayList<Link>();for(var at:moving){var link=Links.get(level).links.get(at);if(link!=null&&snapshot.get(at).is(Jugtania.block("piston_relay")))relayLinks.add(link);}
        DEPTH.set(DEPTH.get()+1);try{
            // No neighbor propagation until all destinations are installed. Roll back the exact snapshot on refusal.
            var desired=new LinkedHashMap<BlockPos,BlockState>();for(var at:breaking)desired.put(at,Blocks.AIR.defaultBlockState());for(var at:moving)desired.put(at,Blocks.AIR.defaultBlockState());for(var at:moving)desired.put(at.relative(direction),snapshot.get(at));
            for(var entry:desired.entrySet())if(!level.getBlockState(entry.getKey()).equals(entry.getValue())&&!level.setBlock(entry.getKey(),entry.getValue(),Block.UPDATE_CLIENTS|Block.UPDATE_MOVE_BY_PISTON)){
                for(var old:snapshot.entrySet())level.setBlock(old.getKey(),old.getValue(),Block.UPDATE_CLIENTS|Block.UPDATE_MOVE_BY_PISTON);return false;
            }
            moved(level,relayLinks,direction);for(var at:breaking)Block.dropResources(snapshot.get(at),level,at,null,actor,ItemStack.EMPTY);
            for(var at:desired.keySet())level.updateNeighborsAt(at,level.getBlockState(at).getBlock());return true;
        }finally{DEPTH.set(DEPTH.get()-1);}
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel level,BlockPos at,boolean moving){super.affectNeighborsAfterRemoval(state,level,at,moving);if(!moving){var data=Links.get(level);if(data.links.remove(at)!=null)data.setDirty();}}
}
