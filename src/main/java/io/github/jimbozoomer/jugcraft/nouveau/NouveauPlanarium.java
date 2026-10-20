package io.github.jimbozoomer.jugcraft.nouveau;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.phys.*;

/** Named, persistent building cells. Generation occurs only on explicit first entry into a new jar. */
public final class NouveauPlanarium extends SavedData {
    public static final ResourceKey<Level> DIMENSION=ResourceKey.create(Registries.DIMENSION,Jugcraft.id("nouveau_planarium"));
    public static AttachmentType<Visit> VISIT;
    private static final int SIZE=18,BASE_Y=64,SPACING=256,MAX_CELLS=2048;
    public record Cell(UUID owner,String name,int index,BlockPos arrival){
        static final Codec<Cell> CODEC=RecordCodecBuilder.create(i->i.group(UUIDUtil.CODEC.fieldOf("owner").forGetter(Cell::owner),Codec.STRING.fieldOf("name").forGetter(Cell::name),Codec.intRange(0,MAX_CELLS-1).fieldOf("index").forGetter(Cell::index),BlockPos.CODEC.fieldOf("arrival").forGetter(Cell::arrival)).apply(i,Cell::new));
        public BlockPos corner(){return new BlockPos((index%64)*SPACING,BASE_Y,(index/64)*SPACING);}
        public boolean contains(BlockPos p){var c=corner();return p.getX()>c.getX()&&p.getX()<c.getX()+SIZE-1&&p.getY()>BASE_Y&&p.getY()<BASE_Y+SIZE-1&&p.getZ()>c.getZ()&&p.getZ()<c.getZ()+SIZE-1;}
        public boolean allows(UUID id){return owner.equals(id)||JugcraftParties.sameParty(owner,id);}
    }
    public record Visit(int cell,GlobalPos home,Vec3 position,float yaw,float pitch){
        static final Codec<Visit> CODEC=RecordCodecBuilder.create(i->i.group(Codec.INT.fieldOf("cell").forGetter(Visit::cell),GlobalPos.CODEC.fieldOf("home").forGetter(Visit::home),Vec3.CODEC.fieldOf("position").forGetter(Visit::position),Codec.FLOAT.fieldOf("yaw").forGetter(Visit::yaw),Codec.FLOAT.fieldOf("pitch").forGetter(Visit::pitch)).apply(i,Visit::new));
    }
    private final List<Cell> cells;
    private static final Codec<NouveauPlanarium> CODEC=Cell.CODEC.listOf().xmap(NouveauPlanarium::new,d->List.copyOf(d.cells));
    private static final SavedDataType<NouveauPlanarium> TYPE=new SavedDataType<>(Jugcraft.id("nouveau_planaria"),NouveauPlanarium::new,CODEC,null);
    private NouveauPlanarium(){this(List.of());}private NouveauPlanarium(List<Cell> cells){this.cells=new ArrayList<>(cells);}
    private static NouveauPlanarium data(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);}
    public static void register(){
        VISIT=AttachmentRegistry.<Visit>builder().persistent(Visit.CODEC).copyOnDeath().buildAndRegister(Jugcraft.id("nouveau_planarium_visit"));
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%10!=0)return;for(var p:server.getPlayerList().getPlayers()){
            var visit=p.getAttached(VISIT);if(!p.level().dimension().equals(DIMENSION)){if(visit!=null)p.removeAttached(VISIT);continue;}
            Cell cell=visit==null?null:cell(p.level(),visit.cell);if(!JugNouveau.enabled()||cell==null||!cell.allows(p.getUUID())||!cell.contains(p.blockPosition()))leave(p);
        }});
        AttackBlockCallback.EVENT.register((p,l,hand,pos,face)->{if(p instanceof ServerPlayer s&&l.dimension().equals(DIMENSION)&&l.getBlockState(pos).is(NouveauCatalog.block("dim_boundary"))){leave(s);return InteractionResult.FAIL;}return InteractionResult.PASS;});
        UseBlockCallback.EVENT.register((p,l,hand,hit)->{if(p instanceof ServerPlayer s&&l.dimension().equals(DIMENSION)&&l.getBlockState(hit.getBlockPos()).is(NouveauCatalog.block("dim_boundary"))&&!p.getItemInHand(hand).is(JugNouveau.WAND)){leave(s);return InteractionResult.SUCCESS;}return InteractionResult.PASS;});
        PlayerBlockBreakEvents.BEFORE.register((l,p,pos,state,be)->{if(l.dimension().equals(DIMENSION)&&state.is(NouveauCatalog.block("dim_boundary"))){if(p instanceof ServerPlayer s)leave(s);return false;}return true;});
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer,p,alive)->{if(p.getAttached(VISIT)!=null)leave(p);});
    }
    public record Mini(BlockPos offset,net.minecraft.world.level.block.state.BlockState state){
        public static final Codec<Mini> CODEC=RecordCodecBuilder.create(i->i.group(BlockPos.CODEC.fieldOf("at").forGetter(Mini::offset),net.minecraft.world.level.block.state.BlockState.CODEC.fieldOf("state").forGetter(Mini::state)).apply(i,Mini::new));
    }
    public static void refresh(NouveauWorkshop jar){
        if(!(jar.getLevel() instanceof ServerLevel l)||jar.owner()==null||jar.customName().isBlank())return;
        var cell=named(l,jar.owner(),jar.customName());var pocket=l.getServer().getLevel(DIMENSION);if(cell==null||pocket==null)return;
        List<Mini> sample=new ArrayList<>();var corner=cell.corner();
        for(int y=SIZE-2;y>0&&sample.size()<768;y--)for(int x=1;x<SIZE-1&&sample.size()<768;x++)for(int z=1;z<SIZE-1&&sample.size()<768;z++){
            var at=corner.offset(x,y,z);if(!pocket.hasChunkAt(at))continue;var state=pocket.getBlockState(at);if(state.isAir()||state.is(NouveauCatalog.block("dim_boundary")))continue;
            boolean visible=false;for(var face:Direction.values()){var neighbor=at.relative(face);if(pocket.hasChunkAt(neighbor)&&!pocket.getBlockState(neighbor).isSolidRender()){visible=true;break;}}
            if(visible)sample.add(new Mini(new BlockPos(x-1,y-1,z-1),state));
        }jar.preview(sample);
    }
    public static Cell cell(ServerLevel l,int index){return data(l).cells.stream().filter(c->c.index==index).findFirst().orElse(null);}
    public static Cell named(ServerLevel l,UUID owner,String name){return data(l).cells.stream().filter(c->c.owner.equals(owner)&&c.name.equals(name)).findFirst().orElse(null);}
    public static boolean enter(ServerPlayer p,NouveauWorkshop jar){
        if(!jar.mayUse(p)||p.isPassenger()||p.level().dimension().equals(DIMENSION)||!RateGate.allow(p,"nouveau_planarium",20))return false;
        String name=jar.customName().strip();if(name.isEmpty()){p.sendOverlayMessage(Component.literal("Name this Planarium at an anvil or with the Name glyph first."));return false;}
        ServerLevel pocket=p.level().getServer().getLevel(DIMENSION);if(pocket==null)return false;
        var data=data(p.level());Cell cell=named(p.level(),jar.owner(),name);
        if(cell==null){
            if(data.cells.size()>=MAX_CELLS||data.cells.stream().filter(c->c.owner.equals(jar.owner())).count()>=16){p.sendOverlayMessage(Component.literal("Planarium limit reached (16 per owner)."));return false;}
            int index=data.cells.stream().mapToInt(Cell::index).max().orElse(-1)+1;if(index>=MAX_CELLS)return false;
            var corner=new BlockPos((index%64)*SPACING,BASE_Y,(index/64)*SPACING);cell=new Cell(jar.owner(),name,index,corner.offset(SIZE/2,2,SIZE/2));
            // The flat dimension contains only air. This cell has never been allocated or visited.
            for(BlockPos at:BlockPos.betweenClosed(corner,corner.offset(SIZE-1,SIZE-1,SIZE-1))){int x=at.getX()-corner.getX(),y=at.getY()-BASE_Y,z=at.getZ()-corner.getZ();
                if(x==0||x==SIZE-1||y==0||y==SIZE-1||z==0||z==SIZE-1)pocket.setBlock(at,NouveauCatalog.block("dim_boundary").defaultBlockState(),Block.UPDATE_CLIENTS);
                else if(y==1)pocket.setBlock(at,Blocks.GRASS_BLOCK.defaultBlockState(),Block.UPDATE_CLIENTS);
            }data.cells.add(cell);data.setDirty();
        }
        if(!cell.allows(p.getUUID()))return false;
        var visit=new Visit(cell.index,GlobalPos.of(p.level().dimension(),p.blockPosition()),p.position(),p.getYRot(),p.getXRot());
        BlockPos at=cell.arrival; pocket.getChunk(at);Vec3 arrival=Vec3.atBottomCenterOf(at);
        if(!pocket.noCollision(p,p.getBoundingBox().move(arrival.subtract(p.position())))){p.sendOverlayMessage(Component.literal("The Planarium arrival point is obstructed."));return false;}
        p.setAttached(VISIT,visit);p.teleportTo(pocket,arrival.x,arrival.y,arrival.z,Set.of(),p.getYRot(),p.getXRot(),true);
        p.sendOverlayMessage(Component.literal(name+" · Touch or strike a boundary to return"));return true;
    }
    public static void leave(ServerPlayer p){
        var visit=p.getAttached(VISIT);ServerLevel target=visit==null?null:p.level().getServer().getLevel(visit.home.dimension());
        if(target==null||target.dimension().equals(DIMENSION))target=p.level().getServer().overworld();
        BlockPos fallback=target.getRespawnData().pos();Vec3 point=visit!=null&&target.dimension().equals(visit.home.dimension())?visit.position:Vec3.atBottomCenterOf(fallback);
        target.getChunk(BlockPos.containing(point));
        Vec3 safe=null;for(int y=0;y<8;y++){var option=point.add(0,y,0);if(target.getWorldBorder().isWithinBounds(BlockPos.containing(option))&&target.noCollision(p,p.getBoundingBox().move(option.subtract(p.position())))){safe=option;break;}}
        if(safe==null){target=p.level().getServer().overworld();safe=Vec3.atBottomCenterOf(target.getRespawnData().pos()).add(0,1,0);}
        p.teleportTo(target,safe.x,safe.y,safe.z,Set.of(),visit==null?p.getYRot():visit.yaw,visit==null?p.getXRot():visit.pitch,true);p.removeAttached(VISIT);
    }
    public static boolean setArrival(ServerPlayer p,BlockPos at){
        var visit=p.getAttached(VISIT);if(visit==null||!p.level().dimension().equals(DIMENSION))return false;Cell cell=cell(p.level(),visit.cell);
        if(cell==null||!cell.owner.equals(p.getUUID())||!cell.contains(at)||!p.level().getBlockState(at).canBeReplaced()||!p.level().getBlockState(at.above()).canBeReplaced()||!p.level().getBlockState(at.below()).isSolidRender())return false;
        var data=data(p.level());data.cells.set(data.cells.indexOf(cell),new Cell(cell.owner,cell.name,cell.index,at.immutable()));data.setDirty();p.sendOverlayMessage(Component.literal("Planarium arrival point set"));return true;
    }
    public static final class Boundary extends Block {
        public Boundary(Properties p){super(p.strength(-1,3600000));}
        @Override protected InteractionResult useWithoutItem(net.minecraft.world.level.block.state.BlockState s,Level l,BlockPos at,Player player,BlockHitResult hit){if(player instanceof ServerPlayer p)leave(p);return InteractionResult.SUCCESS;}
    }
}
