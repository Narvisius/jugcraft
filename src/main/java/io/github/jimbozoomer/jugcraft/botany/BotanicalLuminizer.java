package io.github.jimbozoomer.jugcraft.botany;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Loaded, permission-checked 20-block light-rail segments; no forced chunk tickets. */
public final class BotanicalLuminizer {
    public static final Set<String> TYPES=Set.of("light_relay","detector_light_relay","fork_light_relay","toggle_light_relay");
    private final BotanicalWorkshop host;private BlockPos link,visualLink;private boolean hidden;
    BotanicalLuminizer(BotanicalWorkshop host){this.host=host;}
    public BlockPos link(){return link;}
    public BlockPos visualLink(){return visualLink;}
    public boolean hidden(){return hidden;}
    boolean bind(ServerPlayer p,BlockPos target){if(!host.permitted(p)||host.getBlockPos().equals(target)||host.getBlockPos().distSqr(target)>400||!valid(p,target))return false;link=target.immutable();host.changed();return true;}
    private boolean valid(ServerPlayer p,BlockPos at){return p.level().hasChunkAt(at)&&p.level().getBlockEntity(at) instanceof BotanicalWorkshop b&&TYPES.contains(b.kind())&&b.permitted(p)&&ClaimTransfers.allowed(p.level(),host.getBlockPos(),at);}
    BlockPos next(ServerPlayer p){
        if(!host.permitted(p)||host.kind().equals("toggle_light_relay")&&p.level().hasNeighborSignal(host.getBlockPos()))return null;
        if(host.kind().equals("fork_light_relay"))for(int y=-2;y<=2;y++){
            var at=host.getBlockPos().above(y);if(!p.level().hasChunkAt(at))continue;var state=p.level().getBlockState(at);if(!state.is(Jugtania.block("animated_torch")))continue;
            var direction=state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();for(int n=1;n<=20;n++){var candidate=host.getBlockPos().relative(direction,n);if(!p.level().hasChunkAt(candidate))break;if(valid(p,candidate))return candidate;}break;
        }
        return link!=null&&valid(p,link)?link:null;
    }
    InteractionResult use(ServerPlayer p,InteractionHand hand){var stack=p.getItemInHand(hand);
        if(stack.is(Jugtania.item("phantom_ink"))){if(!hidden){hidden=true;if(!p.hasInfiniteMaterials())stack.shrink(1);host.changed();}return InteractionResult.SUCCESS;}
        if(!stack.isEmpty())return InteractionResult.PASS;return ride(p,p)?InteractionResult.SUCCESS:InteractionResult.FAIL;
    }
    boolean ride(Entity rider,ServerPlayer actor){var dest=next(actor);if(dest==null||rider.isPassenger()||rider.isVehicle()||rider.isSpectator())return false;return BotanicalLightRide.begin(rider,actor,host.getBlockPos(),dest);}
    void tick(ServerLevel level,ServerPlayer actor){
        if(host.kind().equals("toggle_light_relay")){boolean power=level.hasNeighborSignal(host.getBlockPos());if(host.getBlockState().getValue(BlockStateProperties.POWERED)!=power)level.setBlockAndUpdate(host.getBlockPos(),host.getBlockState().setValue(BlockStateProperties.POWERED,power));}
        if(level.getGameTime()%5!=0)return;var target=next(actor);if(!Objects.equals(target,visualLink)){visualLink=target;host.changed();}if(target==null)return;var start=Vec3.atCenterOf(host.getBlockPos());var end=Vec3.atCenterOf(target);
        // The client renders the original bounded spiral from the synchronized visible route.
        int count=0;for(var item:level.getEntitiesOfClass(ItemEntity.class,new AABB(host.getBlockPos()).inflate(.25),e->e.isAlive()&&!e.isPassenger()&&!e.hasPickUpDelay())){if(++count>8)break;ride(item,actor);}
        count=0;for(var pearl:level.getEntitiesOfClass(ThrownEnderpearl.class,new AABB(host.getBlockPos()).inflate(.6))){
            if(++count>8)break;if(!(pearl.getOwner() instanceof ServerPlayer p)||!host.permitted(p))continue;var endpoint=endpoint(p);if(endpoint==null)continue;var position=Vec3.atCenterOf(endpoint).add(pearl.position().subtract(start));
            if(!level.noCollision(pearl,pearl.getBoundingBox().move(position.subtract(pearl.position()))))continue;pearl.setPos(position);pearl.needsSync=true;
        }
    }
    private BlockPos endpoint(ServerPlayer p){var seen=new HashSet<BlockPos>();var current=host;BlockPos last=null;
        for(int i=0;i<128;i++){if(!seen.add(current.getBlockPos()))return null;var dest=current.luminizer.next(p);if(dest==null)return last;if(!(p.level().getBlockEntity(dest) instanceof BotanicalWorkshop node))return null;current=node;last=dest;}return null;
    }
    void arrived(ServerPlayer p){if(host.kind().equals("detector_light_relay")&&host.permitted(p)){p.level().setBlockAndUpdate(host.getBlockPos(),host.getBlockState().setValue(BlockStateProperties.POWERED,true));p.level().scheduleTick(host.getBlockPos(),host.getBlockState().getBlock(),2);}}
    void save(ValueOutput out){if(visualLink!=null)out.store("luminizer_visual",BlockPos.CODEC,visualLink);if(link!=null)out.store("luminizer_link",BlockPos.CODEC,link);out.putBoolean("luminizer_hidden",hidden);}
    void load(ValueInput in){visualLink=in.read("luminizer_visual",BlockPos.CODEC).filter(p->p.distSqr(host.getBlockPos())<=400).orElse(null);link=in.read("luminizer_link",BlockPos.CODEC).filter(p->!p.equals(host.getBlockPos())&&p.distSqr(host.getBlockPos())<=400).orElse(null);hidden=in.getBooleanOr("luminizer_hidden",false);}
    public static final class Block extends BotanicalBlock {
        Block(String name,Properties p){super(name,p.noCollision());registerDefaultState(defaultBlockState().setValue(BlockStateProperties.POWERED,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.POWERED);}
        @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
        @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return net.minecraft.world.level.block.Block.box(4,4,4,12,12,12);}
        @Override protected boolean isSignalSource(BlockState s){return kind.equals("detector_light_relay");}
        @Override protected int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return kind.equals("detector_light_relay")&&s.getValue(BlockStateProperties.POWERED)?15:0;}
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,net.minecraft.util.RandomSource r){if(kind.equals("detector_light_relay")&&s.getValue(BlockStateProperties.POWERED))l.setBlockAndUpdate(p,s.setValue(BlockStateProperties.POWERED,false));}
    }
}
