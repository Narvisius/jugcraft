package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

/** Paged large building exchange. Every page has durable undo data before the first world write. */
public final class LargeSpatialExchange {
    private static final int PAGE=512,MAX=128;
    private static final Codec<List<BlockState>> STATES=BlockState.CODEC.listOf(1,PAGE);
    private static Job active;
    private static Job notifying;
    private static int noticeCursor;
    private LargeSpatialExchange(){}
    private static Path root(MinecraftServer server){return server.getWorldPath(LevelResource.ROOT).resolve("data/jugcraft-spatial-large");}
    static boolean busy(DigitalSpatial.Port port){return active!=null&&active.level==port.getLevel()&&active.port.equals(port.getBlockPos());}
    static boolean any(){return active!=null;}
    static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(LargeSpatialExchange::recover);
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{active=null;notifying=null;SpatialReservation.release();});
        ServerTickEvents.END_SERVER_TICK.register(server->{if(active!=null&&active.level.getServer()==server)tick();else if(notifying!=null&&notifying.level.getServer()==server)notifyBoundary();});
        PlayerBlockBreakEvents.BEFORE.register((world,player,pos,state,node)->!SpatialReservation.blocked(world,pos));
        UseBlockCallback.EVENT.register((player,world,hand,hit)->SpatialReservation.interior(world,hit.getBlockPos())?InteractionResult.FAIL:InteractionResult.PASS);
    }
    static String start(DigitalSpatial.Port port,ServerPlayer player){
        if(busy(port)){active.cancel=true;active.stalled=false;return "Cancellation requested; recorded pages will be restored before unlocking";}
        if(active!=null||notifying!=null)return "Another large spatial operation is finishing";
        if(!port.permitted(player)||!(port.getItem(0).getItem() instanceof DigitalSpatial.Cell cell)||cell.size!=128||!port.getItem(1).isEmpty())return "Insert a 128-block cell and clear the output slot";
        var points=port.getItem(0).getOrDefault(DigitalSpatial.CORNERS,List.<GlobalPos>of());
        var level=player.level();if(points.size()!=2||points.stream().anyMatch(p->!p.dimension().equals(level.dimension())))return "Select opposite pylons in this dimension";
        var a=points.getFirst().pos();var b=points.getLast().pos();int x=Math.abs(a.getX()-b.getX())-1,y=Math.abs(a.getY()-b.getY())-1,z=Math.abs(a.getZ()-b.getZ())-1;
        if(x<1||y<1||z<1||x>MAX||y>MAX||z>MAX)return "Interior dimensions must be 1..128";
        var min=new BlockPos(Math.min(a.getX(),b.getX())+1,Math.min(a.getY(),b.getY())+1,Math.min(a.getZ(),b.getZ())+1);
        var job=new Job(level,min,x,y,z,port.getBlockPos(),port.owner,port.getItem(0).copy(),port.energy.getAmount());
        if(!level.isInWorldBounds(min)||!level.isInWorldBounds(min.offset(x-1,y-1,z-1))||job.bounds().inflate(1).contains(net.minecraft.world.phys.Vec3.atCenterOf(job.port))||min.distSqr(job.port)>128*128)return "Place the port outside the pylon frame, within 128 blocks of its minimum corner";
        if(job.energy<job.cost())return "Large exchange needs "+job.cost()+" JE";
        if(!level.getBlockState(job.port.above()).isAir()||!level.getBlockState(job.port.above(2)).isAir())return "Keep two clear blocks above the port for safe player arrivals";
        if(!level.getEntities((net.minecraft.world.entity.Entity)null,job.bounds().inflate(1.01),e->!job.bounds().contains(e.position())).isEmpty())return "Clear players and entities from the one-block safety margin";
        for(int cx=(min.getX()-1)>>4;cx<=(min.getX()+x)>>4;cx++)for(int cz=(min.getZ()-1)>>4;cz<=(min.getZ()+z)>>4;cz++)if(!level.hasChunk(cx,cz))return "Load the whole region and its safety margin first";
        for(int cx=min.getX()>>4;cx<=(min.getX()+x-1)>>4;cx++)for(int cz=min.getZ()>>4;cz<=(min.getZ()+z-1)>>4;cz++)if(!level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(cx,cz).pack()))return "Wait for resident entity chunks to finish loading";
        for(int dx:new int[]{-1,x})for(int dy:new int[]{-1,y})for(int dz:new int[]{-1,z}){var at=min.offset(dx,dy,dz);if(!level.getBlockState(at).is(DigitalSpatial.PYLON)||!Authority.mayChange(level,player,at))return "Eight permitted corner pylons are required";}
        try{
            if(Files.exists(root(level.getServer()).resolve("operation.nbt")))return "A previous large exchange needs recovery";
            job.residents=SpatialResidents.capture(level,player,job.bounds(),min);
            job.initialize();SpatialReservation.reserve(level,job.bounds(),job.port);SpatialReservation.freeze();active=job;
            for(var viewer:level.players())if(viewer.containerMenu.slots.stream().anyMatch(slot->SpatialReservation.containerReserved(slot.container)))viewer.closeContainer();
            return "Region reserved. Capturing 512 blocks per step; sneak-use again to cancel safely";
        }catch(Exception failure){return "Cannot prepare spatial journal: "+failure.getMessage();}
    }
    private static void tick(){
        var job=active;
        SpatialReservation.internal(()->{
            try{job.step();}
            catch(Exception failure){
                org.slf4j.LoggerFactory.getLogger("JugcraftSpatial").error("Large spatial operation stopped; undo pages retained",failure);
                job.failure=failure.getMessage();job.cancel=true;
                // A failed rollback stays reserved. Retry only on an explicit request or world restart.
                if(job.phase.equals("rollback"))job.stalled=true;
                job.status("Spatial recovery retained: "+job.failure+". Sneak-use to retry rollback");
            }
            return null;
        });
    }
    private static void recover(MinecraftServer server){
        Path path=root(server).resolve("operation.nbt");if(!Files.exists(path))return;
        try{
            var data=DigitalSpatialJournal.read(path);var world=server.getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(data.getStringOr("dimension",""))));
            if(world==null)throw new IOException("Recovery dimension is unavailable");
            var input=decode(ItemStack.CODEC,data.get("input"),world);
            var job=new Job(world,BlockPos.of(data.getLongOr("min",0)),data.getIntOr("x",0),data.getIntOr("y",0),data.getIntOr("z",0),BlockPos.of(data.getLongOr("port",0)),UUID.fromString(data.getStringOr("owner","")),input,data.getLongOr("energy",-1));
            job.operation=UUID.fromString(data.getStringOr("operation",""));job.plot=UUID.fromString(data.getStringOr("plot",""));job.revision=data.getLongOr("revision",-1);
            job.savedPages=data.getIntOr("savedPages",0);job.phase=data.getStringOr("phase","");
            job.residents=data.contains("residents")?decode(SpatialResidents.CODEC,data.get("residents"),world):List.of();job.incomingResidents=data.contains("incomingResidents")?decode(SpatialResidents.CODEC,data.get("incomingResidents"),world):List.of();
            if(job.x<1||job.y<1||job.z<1||job.x>MAX||job.y>MAX||job.z>MAX||job.savedPages<0||job.savedPages>job.pages()||job.energy<job.cost()||job.energy>128_000_000||job.revision<0
                    ||!(input.getItem() instanceof DigitalSpatial.Cell cell)||cell.size!=128||input.getCount()!=1||job.min.distSqr(job.port)>128*128||!world.isInWorldBounds(job.min)||!world.isInWorldBounds(job.min.offset(job.x-1,job.y-1,job.z-1))||job.bounds().inflate(1).contains(net.minecraft.world.phys.Vec3.atCenterOf(job.port)))throw new IOException("Invalid spatial recovery metadata");
            SpatialResidents.prepare(world,job.residents,job.min,job.bounds());SpatialResidents.prepare(world,job.incomingResidents,job.min,job.bounds());
            if(job.phase.equals("complete")){Files.delete(path);return;}
            if(!Set.of("capture","apply","stage","rollback","commit").contains(job.phase)||!job.phase.equals("capture")&&job.savedPages!=job.pages())throw new IOException("Invalid recovery phase or missing undo pages");
            job.phase=job.phase.equals("capture")?"capture":"rollback";job.cancel=true;job.recovery=true;job.cursor=0;
            SpatialReservation.reserve(world,job.bounds(),job.port);if(!job.phase.equals("capture"))SpatialReservation.freeze();active=job;
        }catch(Exception failure){throw new IllegalStateException("Preserve data/jugcraft-spatial-large: cannot safely recover large exchange",failure);}
    }
    private static <T> Tag encode(Codec<T> codec,T value,ServerLevel level){return codec.encodeStart(RegistryOps.create(NbtOps.INSTANCE,level.registryAccess()),value).getOrThrow();}
    private static <T> T decode(Codec<T> codec,Tag value,ServerLevel level){if(value==null)throw new IllegalArgumentException("Missing spatial data");return codec.parse(RegistryOps.create(NbtOps.INSTANCE,level.registryAccess()),value).getOrThrow();}
    private static void notifyBoundary(){
        var job=notifying;int xy=job.x*job.y,xz=job.x*job.z,yz=job.y*job.z,total=2*(xy+xz+yz);
        for(int count=0;count<512&&noticeCursor<total;count++,noticeCursor++){
            int i=noticeCursor;BlockPos pos;
            if(i<2*xy)pos=job.min.offset(i%job.x,(i%xy)/job.x,i<xy?0:job.z-1);
            else if((i-=2*xy)<2*xz)pos=job.min.offset(i%job.x,i<xz?0:job.y-1,(i%xz)/job.x);
            else {i-=2*xz;pos=job.min.offset(i<yz?0:job.x-1,i%job.y,(i%yz)/job.y);}
            if(job.level.hasChunkAt(pos))job.level.updateNeighborsAt(pos,job.level.getBlockState(pos).getBlock());
        }
        if(noticeCursor>=total)notifying=null;
    }
    private static final class Job {
        final ServerLevel level;final BlockPos min,port;final int x,y,z;final UUID owner;final ItemStack input;final long energy;
        UUID operation=UUID.randomUUID(),plot;long revision;int cursor,savedPages,capturedContainers,incomingContainers;String phase="capture",failure="";boolean cancel,recovery,stalled;
        List<SpatialResidents.Entry> residents=List.of(),incomingResidents=List.of();
        Job(ServerLevel level,BlockPos min,int x,int y,int z,BlockPos port,UUID owner,ItemStack input,long energy){this.level=level;this.min=min;this.x=x;this.y=y;this.z=z;this.port=port;this.owner=owner;this.input=input;this.energy=energy;}
        AABB bounds(){return new AABB(min.getX(),min.getY(),min.getZ(),min.getX()+x,min.getY()+y,min.getZ()+z);}
        int volume(){return x*y*z;}int pages(){return (volume()+PAGE-1)/PAGE;}long cost(){return 1000+volume()*50L;}
        BlockPos at(int index){return min.offset(index%x,index/(x*z),index/x%z);}
        Path base(){return root(level.getServer());}Path operationRoot(){return base().resolve("undo").resolve(operation.toString());}
        Path head(){return base().resolve("plots").resolve(plot.toString()).resolve("head.nbt");}
        Path page(long version,int index){return base().resolve("plots").resolve(plot.toString()).resolve(Long.toString(version)).resolve(index+".nbt");}
        Path undo(int index){return operationRoot().resolve(index+".nbt");}
        void initialize()throws IOException{
            var key=input.get(DigitalSpatial.KEY);plot=key==null?UUID.randomUUID():key.id();revision=key==null?0:key.revision();
            if(key!=null){var data=DigitalSpatialJournal.read(head());if(data.getLongOr("revision",-1)!=revision||data.getIntOr("x",0)!=x||data.getIntOr("y",0)!=y||data.getIntOr("z",0)!=z)throw new IOException("Stale cell or mismatched stored dimensions");if(data.contains("residents"))incomingResidents=decode(SpatialResidents.CODEC,data.get("residents"),level);}
            SpatialResidents.prepare(level,residents,min,bounds());SpatialResidents.checkAbsent(level,SpatialResidents.prepare(level,incomingResidents,min,bounds()));
            save();
        }
        void save()throws IOException{
            var tag=new CompoundTag();tag.putString("dimension",level.dimension().identifier().toString());tag.putLong("min",min.asLong());tag.putInt("x",x);tag.putInt("y",y);tag.putInt("z",z);tag.putLong("port",port.asLong());tag.putString("owner",owner.toString());tag.putString("operation",operation.toString());tag.putString("plot",plot.toString());tag.putLong("revision",revision);tag.putLong("energy",energy);tag.putInt("savedPages",savedPages);tag.putString("phase",phase);tag.put("input",encode(ItemStack.CODEC,input,level));
            tag.put("residents",encode(SpatialResidents.CODEC,residents,level));tag.put("incomingResidents",encode(SpatialResidents.CODEC,incomingResidents,level));
            DigitalSpatialJournal.write(base().resolve("operation.nbt"),tag);
        }
        List<BlockState> readPage(Path path,int index)throws IOException{
            var data=DigitalSpatialJournal.read(path);var states=decode(STATES,data.get("states"),level);
            if(states.size()!=Math.min(PAGE,volume()-index*PAGE)||states.stream().anyMatch(s->!DigitalSpatialJournal.supported(s)))throw new IOException("Invalid large spatial page");
            SpatialInventories.validate(states,inventories(data));return states;
        }
        List<SpatialInventories.Entry> inventories(CompoundTag data){return data.contains("inventories")?decode(SpatialInventories.CODEC,data.get("inventories"),level):List.of();}
        List<BlockPos> positions(int page){var result=new ArrayList<BlockPos>();for(int i=page*PAGE;i<Math.min(volume(),(page+1)*PAGE);i++)result.add(at(i));return result;}
        void step()throws IOException{
            if(stalled)return;
            if(cancel&&!phase.equals("rollback")){if(phase.equals("capture")){finish(false);return;}phase="rollback";cursor=0;save();}
            var actor=Authority.answering(level,owner);
            if(!phase.equals("rollback")&&(actor==null||!Authority.mayChange(level,actor,port))){cancel=true;return;}
            if(cursor==pages()){
                if(phase.equals("capture")){phase="apply";cursor=0;save();SpatialReservation.freeze();SpatialResidents.removeRecorded(level,SpatialResidents.prepare(level,residents,min,bounds()),bounds());return;}
                if(phase.equals("apply")){phase="stage";cursor=0;save();return;}
                if(phase.equals("stage")){phase="commit";save();finish(true);return;}
                if(phase.equals("rollback")){finish(false);return;}
            }
            int begin=cursor*PAGE,end=Math.min(volume(),begin+PAGE);
            for(int i=begin;i<end;i++){
                var pos=at(i);
                if(!level.hasChunkAt(pos)){if(recovery)level.getChunk(pos);else {status("Waiting for loaded chunks; region remains reserved");return;}}
                if(!phase.equals("rollback")&&!Authority.mayChange(level,actor,pos)){cancel=true;return;}
            }
            if(phase.equals("stage")){
                // Copy one immutable undo page per step; keep the originals for crash rollback.
                DigitalSpatialJournal.write(page(revision+1,cursor),DigitalSpatialJournal.read(undo(cursor)));
            }else if(phase.equals("capture")){
                var states=new ArrayList<BlockState>(end-begin);
                for(int i=begin;i<end;i++){var state=level.getBlockState(at(i));if(!DigitalSpatialJournal.supported(state))throw new IOException("Unsupported spatial block: "+state.getBlock().getName().getString());if(level.getBlockEntity(at(i)) instanceof io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialLathe.Node lathe&&!lathe.permitted(actor))throw new IOException("Lathe owner does not permit capture");states.add(state);}
                var cargo=SpatialInventories.capture(level,positions(cursor));
                if(capturedContainers+cargo.size()>64)throw new IOException("A large plot supports at most 64 container/machine inventories");
                // Validate incoming pages before any outgoing world mutation.
                if(revision>0){readPage(page(revision,cursor),cursor);incomingContainers+=inventories(DigitalSpatialJournal.read(page(revision,cursor))).size();if(incomingContainers>64)throw new IOException("Incoming plot exceeds 64 inventories");}
                var data=new CompoundTag();data.put("states",encode(STATES,states,level));data.put("inventories",encode(SpatialInventories.CODEC,cargo,level));DigitalSpatialJournal.write(undo(cursor),data);capturedContainers+=cargo.size();savedPages=cursor+1;save();
            }else{
                var states=phase.equals("rollback")?readPage(undo(cursor),cursor):revision==0?Collections.nCopies(end-begin,Blocks.AIR.defaultBlockState()):readPage(page(revision,cursor),cursor);
                var cargo=phase.equals("rollback")?inventories(DigitalSpatialJournal.read(undo(cursor))):revision==0?List.<SpatialInventories.Entry>of():inventories(DigitalSpatialJournal.read(page(revision,cursor)));
                var locations=positions(cursor);var prepared=SpatialInventories.prepare(level,locations,states,cargo);SpatialInventories.detach(level,locations);
                for(int i=begin;i<end;i++){
                    var pos=at(i);var desired=states.get(i-begin);
                    if(!level.getBlockState(pos).equals(desired)&&!level.setBlock(pos,desired,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE))throw new IOException("Cannot write reserved spatial block");
                }
                SpatialInventories.install(level,prepared);
                // Undo pages remain immutable until the complete world and cell revision have been saved.
            }
            cursor++;status(phase+" "+cursor+" / "+pages()+" pages");
        }
        void status(String message){if(level.getBlockEntity(port) instanceof DigitalSpatial.Port node)node.status=message;}
        void finish(boolean commit)throws IOException{
            if(recovery)level.getChunk(port);
            if(!(level.getBlockEntity(port) instanceof DigitalSpatial.Port node))throw new IOException("Reserved spatial port is missing");
            if(commit){
                if(!ItemStack.isSameItemSameComponents(node.getItem(0),input)||node.getItem(0).getCount()!=1||!node.getItem(1).isEmpty()||node.energy.getAmount()<cost())throw new IOException("Reserved port changed during exchange");
                // All next-revision pages were staged incrementally; undo pages remain until completion.
                SpatialResidents.install(level,SpatialResidents.prepare(level,incomingResidents,min,bounds()));
                var head=new CompoundTag();head.putLong("revision",revision+1);head.putInt("x",x);head.putInt("y",y);head.putInt("z",z);head.put("residents",encode(SpatialResidents.CODEC,residents,level));DigitalSpatialJournal.write(head(),head);
                var output=input.copy();output.set(DigitalSpatial.KEY,new DigitalSpatial.PlotKey(plot,revision+1));node.setItem(0,ItemStack.EMPTY);node.setItem(1,output);node.energy.setAmount(node.energy.getAmount()-cost());
            }else if(!phase.equals("capture")||recovery){
                if(recovery)for(int cx=min.getX()>>4;cx<=(min.getX()+x-1)>>4;cx++)for(int cz=min.getZ()>>4;cz<=(min.getZ()+z-1)>>4;cz++){level.getChunk(cx,cz);level.waitForEntities(new net.minecraft.world.level.ChunkPos(cx,cz),0);}
                var restore=SpatialResidents.prepare(level,residents,min,bounds());var remove=SpatialResidents.prepare(level,incomingResidents,min,bounds());
                SpatialResidents.removeRecorded(level,remove,bounds());SpatialResidents.removeRecorded(level,restore,bounds());SpatialResidents.install(level,restore);
                var delivered=node.getItem(1).get(DigitalSpatial.KEY);
                boolean charged=delivered!=null&&delivered.id().equals(plot)&&delivered.revision()==revision+1;
                long restored=charged?Math.min(node.energy.getCapacity(),node.energy.getAmount()+cost()):node.energy.getAmount();
                node.setItem(0,input.copy());node.setItem(1,ItemStack.EMPTY);node.energy.setAmount(restored);
                if(revision==0)Files.deleteIfExists(head());
                else {var oldHead=new CompoundTag();oldHead.putLong("revision",revision);oldHead.putInt("x",x);oldHead.putInt("y",y);oldHead.putInt("z",z);oldHead.put("residents",encode(SpatialResidents.CODEC,incomingResidents,level));DigitalSpatialJournal.write(head(),oldHead);}
            }
            node.setChanged();level.save(null,true,false);
            phase="complete";save();Files.delete(base().resolve("operation.nbt"));
            node.status=commit?"Large spatial exchange complete":failure.isEmpty()?"Spatial exchange cancelled safely":"Spatial exchange rolled back: "+failure;
            notifying=this;noticeCursor=0;
            active=null;SpatialReservation.release();
        }
    }
}
