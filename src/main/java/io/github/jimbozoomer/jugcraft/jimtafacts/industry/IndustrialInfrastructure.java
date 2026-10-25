package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.artillery.AutomatedDefense;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.*;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Native powered pads and defensive Tesla; no automatic player teleport or friendly fire. */
public final class IndustrialInfrastructure {
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static Item FREQUENCY_TRANSMITTER;
    public static BlockEntityType<Node> ENTITY;
    public static final List<String> KINDS=List.of("teleporter","tesla","chargepad_batbox","chargepad_cesu","chargepad_mfe","chargepad_mfsu","magnetizer");
    private IndustrialInfrastructure(){}
    public static void register(){
        for(String kind:KINDS){var id=Jugcraft.id("jimta_"+kind);var key=ResourceKey.create(Registries.BLOCK,id);var block=new Device(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key),kind);Registry.register(BuiltInRegistries.BLOCK,key,block);BLOCKS.put(kind,block);var ik=ResourceKey.create(Registries.ITEM,id);Registry.register(BuiltInRegistries.ITEM,ik,new BlockItem(block,new Item.Properties().setId(ik).useBlockDescriptionPrefix()));}
        var ik=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_frequency_transmitter"));FREQUENCY_TRANSMITTER=Registry.register(BuiltInRegistries.ITEM,ik,new Item(new Item.Properties().setId(ik).stacksTo(1)){
            @Override public InteractionResult useOn(UseOnContext c){if(!(c.getLevel() instanceof ServerLevel l)||!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;var at=c.getClickedPos();if(!(l.getBlockEntity(at) instanceof Node n)||!n.kind().equals("teleporter")||!Authority.mayChange(l,p,at))return InteractionResult.FAIL;var stack=c.getItemInHand();var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();String dim=l.dimension().identifier().toString();
                if(!p.isShiftKeyDown()&&dim.equals(data.getStringOr("Dimension",""))&&data.contains("Pad")){var other=BlockPos.of(data.getLongOr("Pad",0));if(!other.equals(at)&&l.hasChunkAt(other)&&l.getBlockEntity(other) instanceof Node partner&&partner.kind().equals("teleporter")&&Authority.mayChange(l,p,other)){n.target=other;partner.target=at;n.setChanged();partner.setChanged();p.sendOverlayMessage(Component.literal("Teleporters paired. Right-click a pad with an empty hand to travel."));return InteractionResult.SUCCESS;}}
                CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{t.putLong("Pad",at.asLong());t.putString("Dimension",dim);});p.sendOverlayMessage(Component.literal("First teleporter selected. Use on the other pad; sneak-use selects a new first pad."));return InteractionResult.SUCCESS;
            }
        });
        ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_industrial_infrastructure"),FabricBlockEntityTypeBuilder.create(Node::new,BLOCKS.values().toArray(Block[]::new)).build());EnergyStorage.SIDED.registerForBlockEntity((n,side)->n.energy,ENTITY);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->{BLOCKS.values().forEach(out::accept);out.accept(FREQUENCY_TRANSMITTER);});
    }
    private static final class Device extends Block implements EntityBlock,EnergyConnectable {
        static final EnumProperty<Direction> FACING=HorizontalDirectionalBlock.FACING;static final BooleanProperty LIT=BlockStateProperties.LIT;final String kind;
        Device(Properties p,String kind){super(p);this.kind=kind;registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(LIT,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LIT);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return !l.isClientSide()&&type==ENTITY?(world,p,state,e)->{if(world.getGameTime()%5==0)((Node)e).step((ServerLevel)world);}:null;}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity entity,ItemStack stack){super.setPlacedBy(l,p,s,entity,stack);if(entity!=null&&l.getBlockEntity(p) instanceof Node n){n.owner=entity.getUUID();n.setChanged();}}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player player,BlockHitResult hit){if(l instanceof ServerLevel server&&player instanceof ServerPlayer p){if(!Authority.mayChange(server,p,pos))return InteractionResult.FAIL;if(server.getBlockEntity(pos) instanceof Node n){if(n.kind().equals("teleporter"))return n.teleport(server,p)?InteractionResult.SUCCESS:InteractionResult.FAIL;p.sendOverlayMessage(Component.literal(n.kind().equals("magnetizer")?"Magnetizer: "+n.energy.getAmount()+" JE. Metal boots lift you beside the front iron fence; crouch descends. Redstone pauses.":n.kind().equals("tesla")?"Tesla: "+n.energy.getAmount()+" JE. Hostiles only; redstone pauses it.":"Charge pad: "+n.energy.getAmount()+" JE. Stand on top to charge equipped gear."));}}return InteractionResult.SUCCESS;}
    }
    public static final class Node extends BlockEntity {
        public UUID owner;public BlockPos target;public final SimpleEnergyStorage energy;
        public Node(BlockPos p,BlockState s){super(ENTITY,p,s);int tier=Math.max(0,KINDS.indexOf(kind())-2);energy=new SimpleEnergyStorage(kind().startsWith("chargepad")?new long[]{40000,300000,4_000_000,40_000_000}[tier]:kind().equals("magnetizer")?100:200000,kind().startsWith("chargepad")?new int[]{32,128,512,2048}[tier]:kind().equals("magnetizer")?32:512,0,this::setChanged);}
        public String kind(){return ((Device)getBlockState().getBlock()).kind;}
        public void step(ServerLevel l){boolean active=false;if(!l.hasNeighborSignal(worldPosition)&&energy.getAmount()>0){if(kind().startsWith("chargepad"))active=charge(l);else if(kind().equals("tesla")&&l.getGameTime()%20==0)active=shock(l);else if(kind().equals("teleporter"))active=target!=null;else if(kind().equals("magnetizer"))active=MagnetizerLift.step(l,this);}
            if(getBlockState().getValue(Device.LIT)!=active)l.setBlock(worldPosition,getBlockState().setValue(Device.LIT,active),Block.UPDATE_CLIENTS);
        }
        private boolean charge(ServerLevel l){var players=new ArrayList<ServerPlayer>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ServerPlayer.class),new AABB(worldPosition).move(0,1,0).inflate(.15,.05,.15),p->p.isAlive()&&!p.isSpectator(),players,8);long before=energy.getAmount();int tier=Math.max(0,KINDS.indexOf(kind())-2);long budget=Math.min(before,new int[]{160,640,2560,10240}[tier]);
            for(var p:players){if(!Authority.mayChange(l,p,worldPosition))continue;for(EquipmentSlot slot:EquipmentSlot.values()){if(budget<=0)break;var stack=p.getItemBySlot(slot);long amount=Chargeable.charge(stack,budget);budget-=amount;energy.setAmount(energy.getAmount()-amount);}}
            if(before!=energy.getAmount()){setChanged();return true;}return false;
        }
        private boolean shock(ServerLevel l){if(energy.getAmount()<2000)return false;var actor=Authority.answering(l,owner);if(actor==null||!Authority.mayChange(l,actor,worldPosition))return false;var targets=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),new AABB(worldPosition).inflate(6),AutomatedDefense::hostile,targets,16);var origin=Vec3.atCenterOf(worldPosition).add(0,.7,0);
            for(var target:targets){if(target.distanceToSqr(origin)>36||!ClaimTransfers.allowed(l,worldPosition,target.blockPosition())||!Authority.mayChange(l,actor,target.blockPosition())||!Authority.mayStrike(l,actor,owner,target)||l.clip(new ClipContext(origin,target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,actor)).getType()!=HitResult.Type.MISS)continue;
                if(!target.hurtServer(l,l.damageSources().indirectMagic(null,actor),6))continue;energy.setAmount(energy.getAmount()-2000);setChanged();for(int i=0;i<=8;i++){var point=origin.lerp(target.getEyePosition(),i/8.0);l.sendParticles(ParticleTypes.ELECTRIC_SPARK,point.x,point.y,point.z,2,.03,.03,.03,.01);}l.playSound(null,worldPosition,SoundEvents.LIGHTNING_BOLT_IMPACT,SoundSource.BLOCKS,.15F,1.8F);return true;
            }return false;
        }
        public long teleportCost(){return target==null?0:Math.min(200000,10000+(long)Math.sqrt(worldPosition.distSqr(target))*20);}
        public boolean teleport(ServerLevel l,ServerPlayer player){if(target==null||!l.hasChunkAt(target)||!(l.getBlockEntity(target) instanceof Node pad)||!pad.kind().equals("teleporter")||!worldPosition.equals(pad.target)){player.sendOverlayMessage(Component.literal("Pair two loaded pads with a Frequency Transmitter."));return false;}if(l.hasNeighborSignal(worldPosition)||l.hasNeighborSignal(target)||!Authority.mayChange(l,player,worldPosition)||!Authority.mayChange(l,player,target)||player.isPassenger()){player.sendOverlayMessage(Component.literal("Pad is paused, protected or you are riding a vehicle."));return false;}
            var point=Vec3.atBottomCenterOf(target.above());if(!l.getWorldBorder().isWithinBounds(target.above())||!l.noCollision(player,player.getBoundingBox().move(point.subtract(player.position())))||!l.getBlockState(target.above()).getFluidState().isEmpty()||!l.getBlockState(target.above(2)).getFluidState().isEmpty()){player.sendOverlayMessage(Component.literal("Destination is obstructed."));return false;}long cost=teleportCost();if(energy.getAmount()<cost){player.sendOverlayMessage(Component.literal("Needs "+cost+" JE for this journey."));return false;}
            energy.setAmount(energy.getAmount()-cost);setChanged();player.teleportTo(point.x,point.y,point.z);player.resetFallDistance();l.sendParticles(ParticleTypes.PORTAL,point.x,point.y+1,point.z,30,.3,.6,.3,.05);l.playSound(null,target,SoundEvents.ENDERMAN_TELEPORT,SoundSource.BLOCKS,.7F,1);return true;
        }
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putLong("Energy",energy.getAmount());if(owner!=null)out.store("Owner",UUIDUtil.CODEC,owner);if(target!=null)out.store("Target",BlockPos.CODEC,target);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);energy.setAmount(in.getLongOr("Energy",0));owner=in.read("Owner",UUIDUtil.CODEC).orElse(null);target=in.read("Target",BlockPos.CODEC).orElse(null);}
    }
}
