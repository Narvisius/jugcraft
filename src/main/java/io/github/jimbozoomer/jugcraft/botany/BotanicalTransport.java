package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.energy.*;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Rail-mounted mana transport and a one-way bridge into the existing JE cable system. */
public final class BotanicalTransport {
    public static EntityType<PoolCart> CART;
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("botanical_pool_cart"));CART=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<PoolCart>of(PoolCart::new,MobCategory.MISC).sized(.98F,.7F).clientTrackingRange(8).build(key));}
    static void registerDispenser(){
        DispenserBlock.registerBehavior(Jugtania.item("pool_minecart"),new net.minecraft.core.dispenser.DefaultDispenseItemBehavior(){
            @Override protected ItemStack execute(net.minecraft.core.dispenser.BlockSource source,ItemStack stack){
                var component=io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialDynamite.DISPENSER_OWNER;
                var p=Authority.present(source.level(),stack.get(component));if(p==null||!Authority.mayChange(source.level(),p,source.pos()))return stack;
                var facing=source.state().getValue(DispenserBlock.FACING);var at=source.pos().relative(facing);
                if(!source.level().hasChunkAt(at)||!Authority.mayChange(source.level(),p,at))return stack;
                if(!source.level().getBlockState(at).is(BlockTags.RAILS)){if(!source.level().isEmptyBlock(at)||!source.level().getBlockState(at.below()).is(BlockTags.RAILS))return super.execute(source,stack);at=at.below();}
                if(!Authority.mayChange(source.level(),p,at)||!ClaimTransfers.allowed(source.level(),source.pos(),at))return stack;
                var state=source.level().getBlockState(at);double rise=state.getBlock() instanceof BaseRailBlock rail&&state.getValue(rail.getShapeProperty()).isSlope()?.5:0;
                var cart=AbstractMinecart.createMinecart(source.level(),at.getX()+.5,at.getY()+.0625+rise,at.getZ()+.5,CART,EntitySpawnReason.DISPENSER,stack,p);
                if(cart!=null){cart.owner=p.getUUID();if(source.level().noCollision(cart)&&source.level().addFreshEntity(cart))stack.shrink(1);}return stack;
            }
        });
    }
    static void registerEnergy(){EnergyStorage.SIDED.registerForBlockEntity((host,side)->host.kind().equals("mana_fluxfield")?host.flux:null,Jugtania.WORKSHOP);}
    static void flux(BotanicalWorkshop host,ServerLevel level,ServerPlayer actor){
        // Supplied Fabric conversion: 1 mana -> 3 energy, 1280 mana buffer, 160 mana/tick.
        long budget=480;
        for(var side:Direction.values()){
            var at=host.getBlockPos().relative(side);if(budget<=0||!level.hasChunkAt(at)||!Authority.mayChange(level,actor,at)||!ClaimTransfers.allowed(level,host.getBlockPos(),at))continue;
            var output=EnergyStorage.SIDED.find(level,at,side.getOpposite());if(output==null||output.transferIdentity()==host.flux.transferIdentity())continue;
            try(var tx=Transaction.openOuter()){long accepted=output.insert(Math.min(budget,host.flux.getAmount()),tx);if(accepted>0&&host.flux.extract(accepted,tx)==accepted){tx.commit();budget-=accepted;}}
        }
    }
    static void pump(BotanicalWorkshop host,ServerLevel level,ServerPlayer actor){
        var pos=host.getBlockPos();var face=host.getBlockState().getValue(Pump.FACING);boolean moved=false;int signal=0;
        if(!level.hasNeighborSignal(pos))for(var side:Direction.Plane.HORIZONTAL){
            if(side!=face&&side!=face.getOpposite())continue;var rail=pos.relative(side);var poolPos=pos.relative(side.getOpposite());
            if(!level.hasChunkAt(rail)||!level.hasChunkAt(poolPos)||!Authority.mayChange(level,actor,rail)||!ClaimTransfers.allowed(level,rail,poolPos)||!(level.getBlockEntity(poolPos) instanceof BotanicalWorkshop pool)||!pool.kind().endsWith("pool")||!pool.permitted(actor))continue;
            for(var cart:level.getEntitiesOfClass(PoolCart.class,new AABB(rail).inflate(.1),Entity::isAlive)){
                if(!cart.permitted(actor))continue;
                int transfer=side==face?Math.min(10000,Math.min(cart.mana(),pool.freeMana())):Math.min(10000,Math.min(pool.mana(),1000000-cart.mana()));
                if(transfer>0){if(side==face){int stored=pool.fill(transfer);cart.mana(cart.mana()-stored);moved|=stored>0;}else if(pool.draw(transfer)){cart.mana(cart.mana()+transfer);moved=true;}}
                signal=Math.max(signal,cart.mana()==0?0:1+(int)(14L*cart.mana()/1000000));break;
            }
        }
        if(host.pumpActive!=moved||host.pumpSignal!=signal){host.pumpActive=moved;host.pumpSignal=signal;host.changed();}
    }
    public static final class FluxBlock extends BotanicalBlock implements EnergyConnectable {public FluxBlock(Properties p){super("mana_fluxfield",p);}
        @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction d){return l.getBlockEntity(p) instanceof BotanicalWorkshop h?(int)(15*h.flux.getAmount()/3840):0;}
    }
    public static final class Pump extends BotanicalBlock {
        public static final EnumProperty<Direction> FACING=HorizontalDirectionalBlock.FACING;
        public Pump(Properties p){super("pump",p);registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(FACING);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
        @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction d){return l.getBlockEntity(p) instanceof BotanicalWorkshop host?host.pumpSignal:0;}
        @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return s.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis()==Direction.Axis.X?Block.box(0,0,3,16,8,13):Block.box(3,0,0,13,8,16);}
    }
    public static final class CartItem extends MinecartItem {
        CartItem(Properties p){super(CART,p.stacksTo(1));}
        @Override public InteractionResult use(Level world,Player user,InteractionHand hand){if(user instanceof ServerPlayer p){p.getItemInHand(hand).set(io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialDynamite.DISPENSER_OWNER,p.getUUID());p.sendOverlayMessage(Component.literal("Mana cart assigned to you for dispenser placement."));}return InteractionResult.SUCCESS;}
        @Override public void appendHoverText(ItemStack stack,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<Component> out,TooltipFlag flag){out.accept(Component.literal("Use in air before loading into a dispenser to authorize placement."));}
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer p))return c.getLevel().getBlockState(c.getClickedPos()).is(BlockTags.RAILS)?InteractionResult.SUCCESS:InteractionResult.PASS;
            var at=c.getClickedPos();var state=p.level().getBlockState(at);if(!state.is(BlockTags.RAILS)||!Authority.mayChange(p.level(),p,at))return InteractionResult.FAIL;
            double rise=state.getBlock() instanceof BaseRailBlock rail&&state.getValue(rail.getShapeProperty()).isSlope()?.5:0;
            var cart=AbstractMinecart.createMinecart(p.level(),at.getX()+.5,at.getY()+.0625+rise,at.getZ()+.5,CART,EntitySpawnReason.SPAWN_ITEM_USE,c.getItemInHand(),p);if(cart==null)return InteractionResult.FAIL;cart.owner=p.getUUID();
            if(!p.level().noCollision(cart)||!p.level().addFreshEntity(cart))return InteractionResult.FAIL;if(!p.hasInfiniteMaterials())c.getItemInHand().shrink(1);return InteractionResult.SUCCESS;
        }
    }
    public static final class PoolCart extends Minecart {
        private static final EntityDataAccessor<Integer> MANA=SynchedEntityData.defineId(PoolCart.class,EntityDataSerializers.INT);
        private UUID owner;
        public PoolCart(EntityType<?> type,Level l){super(type,l);}
        @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(MANA,0);}
        public int mana(){return entityData.get(MANA);}void mana(int value){entityData.set(MANA,Math.clamp(value,0,1000000));}
        boolean permitted(ServerPlayer p){return (owner==null||owner.equals(p.getUUID()))&&Authority.mayChange(p.level(),p,blockPosition());}
        @Override public boolean isRideable(){return false;}
        @Override protected boolean canAddPassenger(Entity e){return false;}
        @Override public InteractionResult interact(Player user,InteractionHand hand,Vec3 at){
            if(user instanceof ServerPlayer p){if(!permitted(p))return InteractionResult.FAIL;if(owner==null)owner=p.getUUID();p.sendOverlayMessage(Component.literal("Mana cart: "+mana()+" / 1000000. Rail → pump → pool. Wand reverses the pump; redstone pauses it."));}return InteractionResult.SUCCESS;
        }
        @Override public BlockState getDefaultDisplayBlockState(){return Jugtania.block("mana_pool").defaultBlockState().setValue(BotanicalBlock.FILLED,mana()>0);}
        @Override public int getDefaultDisplayOffset(){return 8;}
        @Override protected Item getDropItem(){return Jugtania.item("pool_minecart");}
        @Override public ItemStack getPickResult(){return new ItemStack(getDropItem());}
        @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("mana",mana());if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);}
        @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);mana(in.getIntOr("mana",0));owner=in.read("owner",UUIDUtil.CODEC).orElse(null);}
    }
}
