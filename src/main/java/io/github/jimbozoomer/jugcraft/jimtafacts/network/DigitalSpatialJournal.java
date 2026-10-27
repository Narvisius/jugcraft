package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

/** Write-ahead exchange journal. Cells carry a revision-fenced key, never a duplicable structure snapshot. */
public final class DigitalSpatialJournal {
    private static final Codec<List<BlockState>> STATES = BlockState.CODEC.listOf(0,4096);
    private static final long MAX_FILE = 8 * 1024 * 1024;
    private DigitalSpatialJournal() {}
    private record Box(BlockPos min,int x,int y,int z) {
        int volume(){return x*y*z;}
        List<BlockPos> positions(){return BlockPos.betweenClosedStream(min,min.offset(x-1,y-1,z-1)).map(BlockPos::immutable).toList();}
        AABB bounds(){return new AABB(min.getX(),min.getY(),min.getZ(),min.getX()+x,min.getY()+y,min.getZ()+z);}
    }
    private static Path root(MinecraftServer server){return server.getWorldPath(LevelResource.ROOT).resolve("data/jugcraft-spatial");}
    private static Path plot(MinecraftServer server,UUID id){return root(server).resolve(id+".nbt");}
    private static <T> Tag encode(Codec<T> codec,T value,ServerLevel level){return codec.encodeStart(RegistryOps.create(NbtOps.INSTANCE,level.registryAccess()),value).getOrThrow();}
    private static <T> T decode(Codec<T> codec,Tag value,ServerLevel level){if(value==null)throw new IllegalArgumentException("Missing journal data");return codec.parse(RegistryOps.create(NbtOps.INSTANCE,level.registryAccess()),value).getOrThrow();}
    static CompoundTag read(Path path)throws IOException{if(Files.size(path)>MAX_FILE)throw new IOException("Spatial file exceeds limit");return NbtIo.readCompressed(path,NbtAccounter.create(MAX_FILE));}
    static void write(Path target,CompoundTag data)throws IOException{
        // Check the same decoded budget used by recovery BEFORE changing any files/world blocks.
        var bytes=new java.io.ByteArrayOutputStream();
        var bounded=new java.io.FilterOutputStream(bytes){
            long count;
            @Override public void write(int b)throws IOException{if(++count>MAX_FILE)throw new IOException("Spatial data exceeds recovery budget");out.write(b);}
            @Override public void write(byte[] b,int off,int len)throws IOException{if((count+=len)>MAX_FILE)throw new IOException("Spatial data exceeds recovery budget");out.write(b,off,len);}
        };
        NbtIo.write(data,(java.io.DataOutput)new java.io.DataOutputStream(bounded));
        NbtIo.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())),NbtAccounter.create(MAX_FILE));
        Files.createDirectories(target.getParent());Path temporary=target.resolveSibling(target.getFileName()+".tmp");
        NbtIo.writeCompressed(data,temporary);try(var channel=FileChannel.open(temporary,StandardOpenOption.WRITE)){channel.force(true);}
        // Refuse an unsupported atomic move instead of exposing a half-written ledger.
        Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
    }
    private static CompoundTag boxTag(Box b){var tag=new CompoundTag();tag.putLong("min",b.min.asLong());tag.putInt("x",b.x);tag.putInt("y",b.y);tag.putInt("z",b.z);return tag;}
    private static Box box(CompoundTag tag){var b=new Box(BlockPos.of(tag.getLongOr("min",0)),tag.getIntOr("x",0),tag.getIntOr("y",0),tag.getIntOr("z",0));if(b.x<1||b.x>16||b.y<1||b.y>16||b.z<1||b.z>16)throw new IllegalArgumentException("Invalid spatial dimensions");return b;}
    public static boolean supported(BlockState state){
        if(state.isAir())return true;
        if(SpatialInventories.supported(state))return true;
        if(state.hasBlockEntity()||!state.getFluidState().isEmpty()||state.isRandomlyTicking()||!state.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,BlockPos.ZERO))return false;
        var id=BuiltInRegistries.BLOCK.getKey(state.getBlock());if(!id.getNamespace().equals("minecraft"))return false;
        return state.is(BlockTags.PLANKS)||state.is(BlockTags.LOGS)||state.is(BlockTags.WOOL)||state.is(BlockTags.BASE_STONE_OVERWORLD)||state.is(BlockTags.BASE_STONE_NETHER)
                || Set.of("stone","cobblestone","mossy_cobblestone","smooth_stone","bricks","stone_bricks","cracked_stone_bricks","chiseled_stone_bricks","mossy_stone_bricks","quartz_block","smooth_quartz","quartz_bricks","quartz_pillar","glass","terracotta","obsidian","crying_obsidian","end_stone","end_stone_bricks","purpur_block","purpur_pillar","glowstone","sea_lantern","deepslate_tiles","deepslate_bricks","polished_deepslate","cobbled_deepslate","tuff_bricks","polished_tuff","polished_andesite","polished_diorite","polished_granite").contains(id.getPath())
                || id.getPath().endsWith("_concrete")||id.getPath().endsWith("_terracotta")||id.getPath().endsWith("_stained_glass");
    }
    public static String exchange(DigitalSpatial.Port port,ServerPlayer actor){
        if(!port.permitted(actor))return "Spatial port access denied";
        if(!(port.getItem(0).getItem() instanceof DigitalSpatial.Cell cell)||!port.getItem(1).isEmpty())return "Insert one spatial cell; clear the output slot";
        if(!RateGate.allow(actor,"jimta_spatial_exchange",100))return "Wait five seconds between spatial exchanges";
        var level=actor.level();var server=level.getServer();var selected=port.getItem(0).getOrDefault(DigitalSpatial.CORNERS,List.<net.minecraft.core.GlobalPos>of());
        if(selected.size()!=2||selected.stream().anyMatch(p->!p.dimension().equals(level.dimension())))return "Select two opposite corner pylons in this dimension";
        var a=selected.getFirst().pos();var b=selected.getLast().pos();
        int sx=Math.abs(a.getX()-b.getX())-1,sy=Math.abs(a.getY()-b.getY())-1,sz=Math.abs(a.getZ()-b.getZ())-1;
        if(sx<1||sy<1||sz<1||sx>cell.size||sy>cell.size||sz>cell.size)return "The interior must fit this cell's "+cell.size+"-block dimensions";
        var min=new BlockPos(Math.min(a.getX(),b.getX())+1,Math.min(a.getY(),b.getY())+1,Math.min(a.getZ(),b.getZ())+1);var region=new Box(min,sx,sy,sz);
        if(min.distSqr(port.getBlockPos())>32*32||region.bounds().contains(net.minecraft.world.phys.Vec3.atCenterOf(port.getBlockPos())))return "Keep the port outside the region and within 32 blocks";
        var pylons=new ArrayList<BlockPos>();
        for(int x:new int[]{-1,sx})for(int y:new int[]{-1,sy})for(int z:new int[]{-1,sz}){
            var at=min.offset(x,y,z);if(!level.isLoaded(at)||!level.getBlockState(at).is(DigitalSpatial.PYLON)||!Authority.mayChange(level,actor,at))return "Eight loaded, permitted corner pylons are required";pylons.add(at);
        }
        var positions=region.positions();var before=new ArrayList<BlockState>();
        for(var at:positions){if(!level.isLoaded(at)||!level.isInWorldBounds(at)||!Authority.mayChange(level,actor,at))return "Every block must be loaded and permitted";var state=level.getBlockState(at);if(!supported(state))return "Unsupported live/unstable block: "+state.getBlock().getName().getString();before.add(state);}
        List<SpatialResidents.Entry> residents;
        for(int cx=min.getX()>>4;cx<=(min.getX()+sx-1)>>4;cx++)for(int cz=min.getZ()>>4;cz<=(min.getZ()+sz-1)>>4;cz++)if(!level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(cx,cz).pack()))return "Wait for resident entity data to finish loading before exchanging";
        try{residents=SpatialResidents.capture(level,actor,region.bounds(),min);}catch(IllegalArgumentException invalid){return invalid.getMessage();}
        long cost=1000+region.volume()*50L;if(port.energy.getAmount()<cost)return "Spatial exchange needs "+cost+" JE";
        Path journal=root(server).resolve("exchange.nbt");
        if(Files.exists(journal))return "A previous spatial journal needs recovery; restart the local world before exchanging";
        boolean began=false;
        try{
            for(var at:positions)if(level.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialLathe.Node lathe&&!lathe.permitted(actor))return "Lathe ownership prevents spatial capture";
            var inventories=SpatialInventories.capture(level,positions);
            SpatialInventories.validate(before,inventories);
            var key=port.getItem(0).get(DigitalSpatial.KEY);UUID id=key==null?UUID.randomUUID():key.id();Path ledger=plot(server,id);CompoundTag old=null;List<BlockState> stored;
            List<SpatialInventories.Entry> storedInventories=List.of();
            List<SpatialResidents.Entry> storedResidents=List.of();
            if(key==null)stored=Collections.nCopies(region.volume(),Blocks.AIR.defaultBlockState());
            else{if(!Files.isRegularFile(ledger))return "Stored plot is missing; no world blocks were changed";old=read(ledger);var shape=box(old.getCompoundOrEmpty("box"));
                if(old.getLongOr("revision",-1)!=key.revision())return "This cell is stale or copied; its stored plot has already moved";
                if(shape.x!=sx||shape.y!=sy||shape.z!=sz)return "Use a region with the stored plot's exact dimensions";
                stored=decode(STATES,old.get("states"),level);if(stored.size()!=region.volume()||stored.stream().anyMatch(s->!supported(s)))return "Stored plot contains unsupported data";
                if(old.contains("inventories"))storedInventories=decode(SpatialInventories.CODEC,old.get("inventories"),level);
                if(old.contains("residents"))storedResidents=decode(SpatialResidents.CODEC,old.get("residents"),level);
            }
            SpatialInventories.validate(stored,storedInventories);
            var outgoing=SpatialResidents.prepare(level,residents,min,region.bounds());
            var incoming=SpatialResidents.prepare(level,storedResidents,min,region.bounds());SpatialResidents.checkAbsent(level,incoming);
            var record=new CompoundTag();record.putString("dimension",level.dimension().identifier().toString());record.putString("plot",id.toString());record.put("box",boxTag(region));record.put("before",encode(STATES,before,level));if(old!=null)record.put("oldPlot",old);
            record.put("inventories",encode(SpatialInventories.CODEC,inventories,level));
            record.put("residents",encode(SpatialResidents.CODEC,residents,level));record.put("incomingResidents",encode(SpatialResidents.CODEC,storedResidents,level));
            record.putLong("port",port.getBlockPos().asLong());record.putString("owner",port.owner.toString());record.putLong("energy",port.energy.getAmount());record.put("input",encode(ItemStack.CODEC,port.getItem(0),level));
            write(journal,record);began=true;
            SpatialResidents.removeRecorded(level,outgoing,region.bounds());
            apply(level,positions,stored,storedInventories);
            SpatialResidents.install(level,incoming);
            long revision=key==null?1:key.revision()+1;var output=port.getItem(0).copyWithCount(1);output.set(DigitalSpatial.KEY,new DigitalSpatial.PlotKey(id,revision));port.setItem(0,ItemStack.EMPTY);port.setItem(1,output);port.energy.setAmount(port.energy.getAmount()-cost);port.setChanged();
            level.save(null,true,false);
            var next=new CompoundTag();next.putLong("revision",revision);next.put("box",boxTag(region));next.put("states",encode(STATES,before,level));next.put("inventories",encode(SpatialInventories.CODEC,inventories,level));next.put("residents",encode(SpatialResidents.CODEC,residents,level));write(ledger,next);Files.delete(journal);
            for(var p:pylons){level.setBlock(p,level.getBlockState(p).setValue(DigitalSpatial.Pylon.LIT,true),Block.UPDATE_CLIENTS);level.scheduleTick(p,DigitalSpatial.PYLON,20);}
            return "Exchanged "+region.volume()+" blocks for "+cost+" JE; take the output cell";
        }catch(Exception failure){
            if(began)try{recover(server);}catch(Exception recovery){failure.addSuppressed(recovery);}
            org.slf4j.LoggerFactory.getLogger("JugcraftSpatial").error("Spatial exchange failed; recovery journal retained until rollback completes",failure);
            return "Spatial exchange failed. Check the local log; no further spatial transfers until recovery completes";
        }
    }
    private static void apply(ServerLevel level,List<BlockPos> positions,List<BlockState> states,List<SpatialInventories.Entry> inventories)throws IOException{
        var prepared=SpatialInventories.prepare(level,positions,states,inventories);
        SpatialInventories.detach(level,positions);
        // Cancel outgoing block callbacks, including open-barrel and failed-exchange dispenser callbacks.
        // Valid captured dispensers/droppers are idle; incoming machinery is notified only after cargo restoration.
        if(!positions.isEmpty())level.getBlockTicks().clearArea(net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(positions.getFirst(),positions.getLast()));
        for(int i=0;i<positions.size();i++){
            var desired=states.get(i);if(desired.is(Blocks.BARREL))desired=desired.setValue(BarrelBlock.OPEN,false);
            if(!level.getBlockState(positions.get(i)).equals(desired)&&!level.setBlock(positions.get(i),desired,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE))throw new IOException("Cannot write spatial block "+positions.get(i));
        }
        SpatialInventories.install(level,prepared);
        for(var pos:positions)level.updateNeighborsAt(pos,level.getBlockState(pos).getBlock());
    }
    public static void recover(MinecraftServer server){
        Path journal=root(server).resolve("exchange.nbt");if(!Files.exists(journal))return;
        try{
            var record=read(journal);var level=server.getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(record.getStringOr("dimension",""))));if(level==null)throw new IOException("Journal dimension unavailable");
            var region=box(record.getCompoundOrEmpty("box"));var positions=region.positions();var states=decode(STATES,record.get("before"),level);if(states.size()!=positions.size()||states.stream().anyMatch(s->!supported(s)))throw new IOException("Invalid recovery palette");
            var portPos=BlockPos.of(record.getLongOr("port",0));if(portPos.distSqr(region.min)>32*32||!level.isInWorldBounds(region.min)||!level.isInWorldBounds(region.min.offset(region.x-1,region.y-1,region.z-1)))throw new IOException("Invalid recovery region");
            // Recovery loads only this already-authored, at-most-four-chunk region plus its port; normal use never loads chunks.
            for(int cx=region.min.getX()>>4;cx<=(region.min.getX()+region.x-1)>>4;cx++)for(int cz=region.min.getZ()>>4;cz<=(region.min.getZ()+region.z-1)>>4;cz++)level.getChunk(cx,cz);
            // Entity storage is asynchronous; drain each known region chunk before resolving UUIDs.
            // Otherwise an older on-disk resident could arrive after rollback has installed its replacement.
            for(int cx=region.min.getX()>>4;cx<=(region.min.getX()+region.x-1)>>4;cx++)for(int cz=region.min.getZ()>>4;cz<=(region.min.getZ()+region.z-1)>>4;cz++)level.waitForEntities(new net.minecraft.world.level.ChunkPos(cx,cz),0);
            var inventories=record.contains("inventories")?decode(SpatialInventories.CODEC,record.get("inventories"),level):List.<SpatialInventories.Entry>of();
            SpatialInventories.validate(states,inventories);
            var residents=record.contains("residents")?decode(SpatialResidents.CODEC,record.get("residents"),level):List.<SpatialResidents.Entry>of();
            var incomingResidents=record.contains("incomingResidents")?decode(SpatialResidents.CODEC,record.get("incomingResidents"),level):List.<SpatialResidents.Entry>of();
            var restoredResidents=SpatialResidents.prepare(level,residents,region.min,region.bounds());
            var replacedResidents=SpatialResidents.prepare(level,incomingResidents,region.min,region.bounds());
            // Parse all recovery metadata before changing blocks. A damaged cell/UUID must not
            // leave a successfully restored region paired with an unrestorable port.
            var owner=UUID.fromString(record.getStringOr("owner",""));
            var plotId=UUID.fromString(record.getStringOr("plot",""));
            var input=decode(ItemStack.CODEC,record.get("input"),level);
            long energy=record.getLongOr("energy",-1);
            if(!(input.getItem() instanceof DigitalSpatial.Cell)||input.getCount()!=1||energy<0
                    ||region.bounds().contains(net.minecraft.world.phys.Vec3.atCenterOf(portPos)))throw new IOException("Invalid recovery port metadata");
            level.getChunk(portPos);SpatialResidents.removeRecorded(level,replacedResidents,region.bounds());SpatialResidents.removeRecorded(level,restoredResidents,region.bounds());apply(level,positions,states,inventories);SpatialResidents.install(level,restoredResidents);
            if(!level.getBlockState(portPos).is(DigitalSpatial.PORT))level.setBlockAndUpdate(portPos,DigitalSpatial.PORT.defaultBlockState());
            if(!(level.getBlockEntity(portPos) instanceof DigitalSpatial.Port port))throw new IOException("Cannot restore spatial port");
            port.owner=owner;port.setItem(0,input);port.setItem(1,ItemStack.EMPTY);port.energy.setAmount(energy);port.setChanged();level.save(null,true,false);
            Path ledger=plot(server,plotId);if(record.contains("oldPlot"))write(ledger,record.getCompoundOrEmpty("oldPlot"));else Files.deleteIfExists(ledger);Files.delete(journal);
        }catch(Exception error){throw new IllegalStateException("Spatial recovery failed; preserve data/jugcraft-spatial/exchange.nbt and investigate before using this world",error);}
    }
}
