package io.github.jimbozoomer.jugcraft.botany;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.SpatialReservation;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;

/** Conserved, bounded terrain lifting. Source and destination always share the same saved chunk. */
public final class BotanicalLaputa extends Item {
    public static DataComponentType<Integer> TIER;
    private static AttachmentType<List<Job>> JOBS;
    private static final TagKey<Block> IMMOBILE=TagKey.create(Registries.BLOCK,Jugcraft.id("jugtania_laputa_immobile"));
    private record Job(UUID owner,BlockPos origin,int tier,boolean pointy,double scale,int cursor,int moved){
        static final Codec<Job> CODEC=RecordCodecBuilder.create(i->i.group(UUIDUtil.CODEC.fieldOf("owner").forGetter(Job::owner),BlockPos.CODEC.fieldOf("origin").forGetter(Job::origin),Codec.intRange(0,19).fieldOf("tier").forGetter(Job::tier),Codec.BOOL.fieldOf("pointy").forGetter(Job::pointy),Codec.doubleRange(.1,2).fieldOf("scale").forGetter(Job::scale),Codec.intRange(0,300000).fieldOf("cursor").forGetter(Job::cursor),Codec.intRange(0,300000).fieldOf("moved").forGetter(Job::moved)).apply(i,Job::new));
        int radius(){return 14+tier;}int width(){return radius()*2+1;}int size(){return width()*width()*64;}
        BlockPos at(int index){int plane=width()*width();int y=21-index/plane;int n=index%plane;return origin.offset(n%width()-radius(),y,n/width()-radius());}
        boolean contains(BlockPos at){double x=at.getX()-origin.getX(),z=at.getZ()-origin.getZ(),y=at.getY()-origin.getY();if(y>=0)return x*x+z*z<radius()*radius();return pointy?Math.sqrt(x*x+z*z)<radius()+y/scale:x*x+z*z+y*y/(scale*scale)<radius()*radius();}
    }
    BotanicalLaputa(Properties p){super(p.stacksTo(1));}
    static void register(){
        TIER=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_laputa_tier"),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,19)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        JOBS=AttachmentRegistry.<List<Job>>builder().persistent(Job.CODEC.listOf(0,4)).buildAndRegister(Jugcraft.id("botanical_laputa_jobs"));
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("botanical_laputa_upgrade"),Upgrade.SERIALIZER);
        ServerTickEvents.END_LEVEL_TICK.register(BotanicalLaputa::tick);
    }
    @Override public InteractionResult useOn(UseOnContext c){
        if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        var level=p.level();var jobs=new ArrayList<>(level.getAttachedOrElse(JOBS,List.of()));
        if(p.isShiftKeyDown()&&jobs.removeIf(j->j.owner.equals(p.getUUID()))){level.setAttached(JOBS,List.copyOf(jobs));p.sendSystemMessage(Component.literal("Island lift stopped. Already moved terrain stays; this shard was not consumed."));return InteractionResult.SUCCESS;}
        if(jobs.size()>=4||jobs.stream().anyMatch(j->j.owner.equals(p.getUUID()))){p.sendOverlayMessage(Component.literal("A terrain lift is already running. Stay nearby until it finishes."));return InteractionResult.FAIL;}
        int tier=Math.clamp(c.getItemInHand().getOrDefault(TIER,0),0,19),r=14+tier;var center=c.getClickedPos();
        var job=new Job(p.getUUID(),center,tier,p.getRandom().nextFloat()<.25,(p.getRandom().nextDouble()+.5)*14/r,0,0);
        if(center.getY()+63>=level.getMaxY()||center.getY()-42<level.getMinY()){p.sendOverlayMessage(Component.literal("The island needs room below and 63 blocks of clear height above its center."));return InteractionResult.FAIL;}
        // Preflight chunk availability and column boundaries cheaply. Every actual write
        // independently rechecks authority at both positions in the bounded tick worker.
        for(int x=-r;x<=r;x+=8)for(int z=-r;z<=r;z+=8){
            if(!allowed(level,p,center.offset(x,0,z))||!allowed(level,p,center.offset(x,42,z))){p.sendOverlayMessage(Component.literal("Keep the source and destination loaded and accessible."));return InteractionResult.FAIL;}
        }
        if(jobs.stream().anyMatch(j->Math.abs(j.origin.getX()-center.getX())<=j.radius()+r&&Math.abs(j.origin.getZ()-center.getZ())<=j.radius()+r)){p.sendOverlayMessage(Component.literal("Another island lift overlaps this area."));return InteractionResult.FAIL;}
        jobs.add(job);level.setAttached(JOBS,List.copyOf(jobs));c.getItemInHand().shrink(1);
        level.playSound(null,center,BotanicalSounds.get("laputa_start"),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        p.sendSystemMessage(Component.literal("Lifting radius "+r+" by 42 blocks. Keep this area loaded. Containers, machines and occupied destinations stay in place."));return InteractionResult.SUCCESS;
    }
    private static boolean allowed(ServerLevel l,ServerPlayer p,BlockPos at){return l.hasChunkAt(at)&&l.isInWorldBounds(at)&&l.getWorldBorder().isWithinBounds(at)&&!SpatialReservation.blocked(l,at)&&Authority.mayChange(l,p,at);}
    private static boolean movable(BlockState s,ServerLevel l,BlockPos at){
        return !s.isAir()&&s.getFluidState().isEmpty()&&!s.is(IMMOBILE)&&s.getDestroySpeed(l,at)>=0&&!s.hasBlockEntity()&&!(s.getBlock() instanceof FallingBlock)&&!(s.getBlock() instanceof BedBlock)&&!s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)&&!s.hasProperty(BlockStateProperties.WATERLOGGED)&&s.isCollisionShapeFullBlock(l,at);
    }
    private static void tick(ServerLevel level){
        var saved=level.getAttachedOrElse(JOBS,List.of());if(saved.isEmpty())return;var next=new ArrayList<Job>();int scanBudget=2048,moveBudget=32;
        for(var job:saved){var p=level.getServer().getPlayerList().getPlayer(job.owner);int cursor=job.cursor,moved=job.moved;
            if(p==null||!p.isAlive()||p.isSpectator()||p.level()!=level||p.distanceToSqr(Vec3.atCenterOf(job.origin))>256*256){next.add(job);continue;}
            while(cursor<job.size()&&scanBudget>0&&moveBudget>0){
                var from=job.at(cursor);var to=from.above(42);scanBudget--;
                if(!job.contains(from)){cursor++;continue;}
                // Permission revocation or unloaded space pauses, preserving the exact scan cursor.
                if(!allowed(level,p,from)||!allowed(level,p,to))break;
                cursor++;var state=level.getBlockState(from);if(!movable(state,level,from)||!level.getBlockState(to).isAir()||!level.getEntities((net.minecraft.world.entity.Entity)null,new AABB(from).minmax(new AABB(to)),e->e.isAlive()).isEmpty())continue;
                // Top-down traversal prevents a destination being lifted a second time. No drops,
                // block entity cargo, or intermediate floating-block custody can duplicate contents.
                if(!level.setBlock(from,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS))continue;
                if(!level.setBlock(to,state,Block.UPDATE_CLIENTS)){level.setBlock(from,state,Block.UPDATE_CLIENTS);continue;}
                level.updateNeighborsAt(from,state.getBlock());level.updateNeighborsAt(to,state.getBlock());
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,state),from.getX()+.5,from.getY()+.5,from.getZ()+.5,6,.2,.2,.2,.06);
                level.sendParticles(ParticleTypes.PORTAL,to.getX()+.5,to.getY()+.5,to.getZ()+.5,4,.3,.3,.3,0);moved++;moveBudget--;
            }
            if(cursor<job.size())next.add(new Job(job.owner,job.origin,job.tier,job.pointy,job.scale,cursor,moved));else p.sendSystemMessage(Component.literal("Island lift finished: "+moved+" blocks moved. Obstructed or unsupported blocks stayed where they were."));
        }
        if(!next.equals(saved)){if(next.isEmpty())level.removeAttached(JOBS);else level.setAttached(JOBS,List.copyOf(next));}
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){out.accept(Component.literal("Level "+(s.getOrDefault(TIER,0)+1)+" · radius "+(14+s.getOrDefault(TIER,0))+" · lifts 42 blocks"));out.accept(Component.literal("Consumes this shard. Stable solid terrain only; containers, machines and fluids remain."));out.accept(Component.literal("Upgrade with a Gaia Spirit. Stay nearby while the island moves; sneak-use another shard to stop."));}
    public static final class Upgrade extends CustomRecipe {
        private static final Upgrade INSTANCE=new Upgrade();public static final RecipeSerializer<Upgrade> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,Upgrade>unit(INSTANCE));
        private ItemStack target(CraftingInput in){ItemStack shard=ItemStack.EMPTY;boolean spirit=false;for(int i=0;i<in.size();i++){var s=in.getItem(i);if(s.isEmpty())continue;if(s.is(Jugtania.item("laputa_shard"))&&shard.isEmpty()&&s.getOrDefault(TIER,0)<19)shard=s;else if(s.is(Jugtania.item("life_essence"))&&!spirit)spirit=true;else return ItemStack.EMPTY;}return spirit?shard:ItemStack.EMPTY;}
        @Override public boolean matches(CraftingInput in,Level l){return !target(in).isEmpty();}
        @Override public ItemStack assemble(CraftingInput in){var source=target(in);if(source.isEmpty())return source;var out=source.copyWithCount(1);out.set(TIER,source.getOrDefault(TIER,0)+1);return out;}
        @Override public RecipeSerializer<Upgrade> getSerializer(){return SERIALIZER;}
    }
}
