package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** One-way sourcestone frames. Creation is transactional; only entering players request destination chunks. */
public final class NouveauPortals {
    public static BlockEntityType<Portal> TYPE;
    private NouveauPortals(){}
    public static void register(){
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("nouveau_portal"),FabricBlockEntityTypeBuilder.create(Portal::new,NouveauCatalog.block("portal")).build());
        ServerTickEvents.END_SERVER_TICK.register(server->{if(!JugNouveau.enabled()||server.getTickCount()%20!=0)return;for(var p:server.getPlayerList().getPlayers()){
            int budget=4;for(var item:p.level().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(16),e->e.isAlive()&&e.getItem().has(NouveauUtilities.DESTINATION))){
                if(budget--<=0)break;if(item.getOwner()!=p||!item.getItem().is(NouveauCatalog.item("warp_scroll")))continue;
                if(create(p,item.blockPosition(),item.getItem().get(NouveauUtilities.DESTINATION))){item.getItem().shrink(1);if(item.getItem().isEmpty())item.discard();}
            }
        }});
    }
    private static boolean frame(BlockState s){var key=BuiltInRegistries.BLOCK.getKey(s.getBlock());return s.getBlock() instanceof NouveauWeaves.WeaveBlock||key.getNamespace().equals("jugcraft")&&key.getPath().contains("sourcestone");}
    public static boolean create(ServerPlayer p,BlockPos seed,GlobalPos destination){
        var l=p.level();if(destination==null||!destination.dimension().equals(l.dimension())||!l.getBlockState(seed).isAir())return false;
        for(var normal:Direction.Axis.values()){
            var directions=Arrays.stream(Direction.values()).filter(d->d.getAxis()!=normal).toList();var cells=new LinkedHashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();queue.add(seed);boolean valid=true;
            while(!queue.isEmpty()&&cells.size()<=441){var at=queue.removeFirst();if(cells.contains(at))continue;
                if(!l.hasChunkAt(at)||!Authority.mayChange(l,p,at)){valid=false;break;}var state=l.getBlockState(at);if(frame(state))continue;
                if(!state.isAir()||Math.abs(at.getX()-seed.getX())>20||Math.abs(at.getY()-seed.getY())>20||Math.abs(at.getZ()-seed.getZ())>20){valid=false;break;}
                cells.add(at.immutable());for(var d:directions)queue.add(at.relative(d));
            }
            if(!valid||cells.isEmpty()||cells.size()>441)continue;
            int minX=cells.stream().mapToInt(BlockPos::getX).min().orElse(0),maxX=cells.stream().mapToInt(BlockPos::getX).max().orElse(0),minY=cells.stream().mapToInt(BlockPos::getY).min().orElse(0),maxY=cells.stream().mapToInt(BlockPos::getY).max().orElse(0),minZ=cells.stream().mapToInt(BlockPos::getZ).min().orElse(0),maxZ=cells.stream().mapToInt(BlockPos::getZ).max().orElse(0);
            if((maxX-minX+1)*(maxY-minY+1)*(maxZ-minZ+1)!=cells.size()||maxX-minX>20||maxY-minY>20||maxZ-minZ>20)continue;
            NouveauWorkshop jar=null;for(var at:BlockPos.betweenClosed(seed.offset(-5,-3,-5),seed.offset(5,3,5)))if(l.hasChunkAt(at)&&l.getBlockEntity(at) instanceof NouveauWorkshop b&&b.kind().endsWith("source_jar")&&b.permitted(p)&&b.ley()>=1000){jar=b;break;}
            if(jar==null)return false;jar.draw(1000);var state=NouveauCatalog.block("portal").defaultBlockState().setValue(PortalBlock.AXIS,normal);
            for(var at:cells){l.setBlock(at,state,Block.UPDATE_CLIENTS);if(l.getBlockEntity(at) instanceof Portal portal){portal.destination=destination;portal.owner=p.getUUID();portal.setChanged();}}
            for(var at:cells)l.updateNeighborsAt(at,state.getBlock());NouveauAdvancements.award(p,"create_portal");return true;
        }return false;
    }
    public static final class PortalBlock extends BaseEntityBlock {
        public static final EnumProperty<Direction.Axis> AXIS=BlockStateProperties.AXIS;
        PortalBlock(Properties p){super(p.lightLevel(s->10));registerDefaultState(defaultBlockState().setValue(AXIS,Direction.Axis.Z));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(AXIS);}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Portal(p,s);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return switch(s.getValue(AXIS)){case X->Block.box(6,0,0,10,16,16);case Y->Block.box(0,6,0,16,10,16);case Z->Block.box(0,0,6,16,16,10);};}
        @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide()?null:createTickerHelper(type,TYPE,Portal::tick);}
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos at,Block source,net.minecraft.world.level.redstone.Orientation orientation,boolean moving){if(l instanceof ServerLevel server)for(var d:Direction.values())if(d.getAxis()!=s.getValue(AXIS)){var neighbor=l.getBlockState(at.relative(d));if(!neighbor.is(this)&&!frame(neighbor)){server.removeBlock(at,false);break;}}}
    }
    public static final class Portal extends BlockEntity {
        private GlobalPos destination;private UUID owner;
        Portal(BlockPos p,BlockState s){super(TYPE,p,s);}
        static void tick(Level level,BlockPos pos,BlockState s,Portal b){
            if(!(level instanceof ServerLevel l)||!JugNouveau.enabled()||b.destination==null||l.getGameTime()%10!=0)return;
            for(var p:l.getEntitiesOfClass(ServerPlayer.class,new AABB(pos),q->q.isAlive()&&!q.isSpectator()&&!q.isPassenger()).stream().limit(4).toList())if(RateGate.allow(p,"nouveau_portal",60))NouveauUtilities.teleport(p,b.destination);
        }
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(destination!=null)out.store("destination",GlobalPos.CODEC,destination);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);destination=in.read("destination",GlobalPos.CODEC).orElse(null);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);}
    }
}
