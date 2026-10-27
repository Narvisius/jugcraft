package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import io.github.jimbozoomer.jugcraft.logistics.ItemNetworks;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalFacades;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** Saved face fittings composed with factory conduit inventories; routing and client arms use the same edge predicate. */
public final class TransportAttachments {
    public static BlockEntityType<Node> TYPE;
    public static Item BLOCKER,POWER_ADAPTOR;
    public static final List<Item> LENSES=new ArrayList<>();
    private TransportAttachments(){}
    public static void register(){
        TransportFacades.register();
        FactoryWires.register();
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_transport_attachments"),FabricBlockEntityTypeBuilder.create(Node::new,
                IndustrialTransport.BLOCKS.values().stream().filter(b->b instanceof IndustrialTransport.RoutePipe||b instanceof IndustrialTransport.RouteFluid||b instanceof IndustrialTransport.RoutePower||b instanceof IndustrialTransport.FluidExtractor||b instanceof IndustrialTransport.ItemExtractor||b instanceof IndustrialTransportSpecial.VoidBlock).toArray(Block[]::new)).build());
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_pipe_blocker"));
        BLOCKER=Registry.register(BuiltInRegistries.ITEM,key,new Plug(new Item.Properties().setId(key)));
        var powerKey=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_pipe_power_adaptor"));
        POWER_ADAPTOR=Registry.register(BuiltInRegistries.ITEM,powerKey,new PowerAdaptor(new Item.Properties().setId(powerKey)));
        io.github.jimbozoomer.jugcraft.energy.EnergyStorage.SIDED.registerForBlockEntity((n,side)->n.attachments().powerPort(side),TYPE);
        io.github.jimbozoomer.jugcraft.energy.EnergyStorage.SIDED.registerForBlockEntity((n,side)->n.attachments().powerPort(side),IndustrialTransportDevices.ENTITY);
        for(boolean filter:new boolean[]{false,true})for(int color=-1;color<16;color++){
            String name="jimta_"+(filter?"filter_lens_":"lens_")+(color<0?"clear":DyeColor.byId(color).getName());
            var lensKey=ResourceKey.create(Registries.ITEM,Jugcraft.id(name));int code=LENSES.size()+1;
            LENSES.add(Registry.register(BuiltInRegistries.ITEM,lensKey,new Lens(new Item.Properties().setId(lensKey),code)));
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(o->{o.accept(BLOCKER);o.accept(POWER_ADAPTOR);LENSES.forEach(o::accept);});
    }
    public interface Host { Fittings attachments(); }
    public static Fittings at(BlockGetter world,BlockPos pos){
        if(world instanceof Level l&&!l.hasChunkAt(pos))return null;
        var entity=world.getBlockEntity(pos);
        return entity instanceof Host host&&IndustrialTransport.BLOCKS.containsValue(entity.getBlockState().getBlock())?host.attachments():null;
    }
    public static boolean colored(BlockGetter world,BlockPos at){var n=at(world,at);return n!=null&&Arrays.stream(n.lenses).anyMatch(i->i!=0);}
    /** Apply exit then entry fittings. The color belongs to this route, never to the stored item. */
    public static int routeAcross(BlockGetter world,BlockPos from,Direction direction,int color){
        var a=at(world,from);var b=at(world,from.relative(direction));
        if(a!=null)color=a.route(direction,color);
        if(color>=-1&&b!=null)color=b.route(direction.getOpposite(),color);
        return color;
    }
    public static boolean openFace(BlockGetter world,BlockPos pos,Direction side){
        var state=at(world,pos);return state==null||side==null||!state.blocked(side)&&state.powerFace!=side.ordinal();
    }
    /** Unpainted junctions bridge colors; two different painted factory pipes stay disconnected. */
    public static boolean colorsOpen(BlockGetter world,BlockPos from,Direction side){
        var a=at(world,from);var b=at(world,from.relative(side));
        return a==null||b==null||a.paint<0||b.paint<0||a.paint==b.paint;
    }
    public static boolean openEnergyEdge(BlockGetter world,BlockPos from,Direction side){
        if(world instanceof Level level&&(!level.hasChunkAt(from)||!level.hasChunkAt(from.relative(side))))return false;
        var a=at(world,from);var b=at(world,from.relative(side));
        return colorsOpen(world,from,side)&&(a==null||!a.blocked(side))&&(b==null||!b.blocked(side.getOpposite()));
    }
    public static long boostedLimit(BlockGetter world,BlockPos pos,long base,long quantum){var n=at(world,pos);return n==null?base:n.boostLimit(base,quantum);}
    public static void payBoost(BlockGetter world,BlockPos pos,long moved,long base,long quantum){var n=at(world,pos);if(n!=null)n.payBoost(moved,base,quantum);}
    public static boolean openEdge(BlockGetter world,BlockPos from,Direction side){
        if(world instanceof Level level&&(!level.hasChunkAt(from)||!level.hasChunkAt(from.relative(side))))return false;
        return colorsOpen(world,from,side)&&openFace(world,from,side)&&openFace(world,from.relative(side),side.getOpposite());
    }
    public static net.minecraft.world.phys.shapes.VoxelShape shape(net.minecraft.world.phys.shapes.VoxelShape original,BlockGetter world,BlockPos pos){
        var n=at(world,pos);if(n==null)return original;
        for(Direction side:Direction.values()){
            net.minecraft.world.phys.shapes.VoxelShape part=null;
            if(!n.faces.get(side.ordinal()).isAir()){
                part=switch(side){case NORTH->Block.box(0,0,0,16,16,2);case SOUTH->Block.box(0,0,14,16,16,16);case WEST->Block.box(0,0,0,2,16,16);case EAST->Block.box(14,0,0,16,16,16);case DOWN->Block.box(0,0,0,16,2,16);case UP->Block.box(0,14,0,16,16,16);};
                if(n.facadeModes[side.ordinal()]==TransportFacades.OPEN){
                    var opening=switch(side.getAxis()){case X->Block.box(0,4,4,16,12,12);case Y->Block.box(4,0,4,12,16,12);case Z->Block.box(4,4,0,12,12,16);};
                    part=net.minecraft.world.phys.shapes.Shapes.join(part,opening,net.minecraft.world.phys.shapes.BooleanOp.ONLY_FIRST);
                }
            }
            else if(n.lenses[side.ordinal()]!=0||n.powerFace==side.ordinal())part=switch(side){case WEST->Block.box(0,3,3,2,13,13);case EAST->Block.box(14,3,3,16,13,13);case NORTH->Block.box(3,3,0,13,13,2);case SOUTH->Block.box(3,3,14,13,13,16);case DOWN->Block.box(3,0,3,13,2,13);case UP->Block.box(3,14,3,13,16,13);};
            else if(n.blocked(side))part=switch(side){case WEST->Block.box(2,4,4,4.01,12,12);case EAST->Block.box(11.99,4,4,14,12,12);case NORTH->Block.box(4,4,2,12,12,4.01);case SOUTH->Block.box(4,4,11.99,12,12,14);case DOWN->Block.box(4,2,4,12,4.01,12);case UP->Block.box(4,11.99,4,12,14,12);};
            if(part!=null)original=net.minecraft.world.phys.shapes.Shapes.or(original,part);
        }
        return original;
    }
    public static boolean cover(UseOnContext c){
        var n=at(c.getLevel(),c.getClickedPos());if(n==null)return false;
        if(!(c.getPlayer() instanceof ServerPlayer p))return true;
        if(!editable(n,p))return true;
        var sample=c.getItemInHand().get(DigitalFacades.MATERIAL);var side=c.getClickedFace();
        if(sample==null||!DigitalFacades.valid(sample)||!n.free(side))return true;
        n.phases.set(side.ordinal(),c.getItemInHand().getOrDefault(TransportFacades.PHASE,TransportFacades.Phase.NONE));n.faces.set(side.ordinal(),sample);n.facadeModes[side.ordinal()]=TransportFacades.mode(c.getItemInHand());c.getItemInHand().consume(1,p);n.changed();return true;
    }
    public static boolean remove(Level world,BlockPos pos,Player player,Direction side){
        var n=at(world,pos);if(!player.isShiftKeyDown()||n==null)return false;
        if(n.free(side))return false;
        if(!(player instanceof ServerPlayer p))return true;
        if(!editable(n,p))return true;
        ItemStack drop;
        if(n.powerFace==side.ordinal()){drop=n.adaptorStack();n.powerFace=-1;n.power.setAmount(0);}
        else if(n.plugged(side)){n.mask&=~(1<<side.ordinal());drop=new ItemStack(BLOCKER);}
        else if(n.lenses[side.ordinal()]!=0){drop=new ItemStack(LENSES.get(n.lenses[side.ordinal()]-1));n.lenses[side.ordinal()]=0;}
        else {drop=n.facadeStack(side);n.faces.set(side.ordinal(),Blocks.AIR.defaultBlockState());n.facadeModes[side.ordinal()]=0;n.phases.set(side.ordinal(),TransportFacades.Phase.NONE);}
        n.changed();p.getInventory().placeItemBackInInventory(drop,net.minecraft.util.Prediction.SERVER_ONLY);return true;
    }
    private static boolean editable(Fittings n,ServerPlayer p){
        return p.isAlive()&&!p.isSpectator()&&!n.isRemoved()&&p.level()==n.getLevel()&&p.level().getBlockEntity(n.getBlockPos())==n.owner&&Authority.mayChange(p.level(),p,n.getBlockPos());
    }
    private static final class Plug extends Item {
        Plug(Properties p){super(p);}
        @Override public InteractionResult useOn(UseOnContext c){
            var n=at(c.getLevel(),c.getClickedPos());if(n==null)return InteractionResult.PASS;
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!editable(n,p))return InteractionResult.FAIL;
            var side=c.getClickedFace();if(!n.free(side))return InteractionResult.FAIL;
            n.mask|=1<<side.ordinal();c.getItemInHand().consume(1,p);n.changed();return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<net.minecraft.network.chat.Component> out,TooltipFlag f){
            out.accept(net.minecraft.network.chat.Component.literal("Seals one factory pipe face. Sneak-use that face with an empty hand to remove."));
        }
    }
    private static final class PowerAdaptor extends Item implements io.github.jimbozoomer.jugcraft.tools.Chargeable {
        PowerAdaptor(Properties p){super(p.stacksTo(1));}
        @Override public long baseCapacity(){return 2048;}
        @Override public InteractionResult useOn(UseOnContext c){
            var n=at(c.getLevel(),c.getClickedPos());if(n==null||!n.allowsAdapter())return InteractionResult.PASS;
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!editable(n,p)||n.powerFace>=0||!n.free(c.getClickedFace()))return InteractionResult.FAIL;
            n.powerFace=c.getClickedFace().ordinal();n.power.setAmount(io.github.jimbozoomer.jugcraft.tools.Chargeable.energy(c.getItemInHand()));
            c.getItemInHand().consume(1,p);n.changed();return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack stack,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<net.minecraft.network.chat.Component> out,TooltipFlag f){
            out.accept(net.minecraft.network.chat.Component.literal("Fits wooden and diamond-wood item/fluid extraction pipes. One per pipe."));
            out.accept(net.minecraft.network.chat.Component.literal("Connect JE cable to this face. Blocks item/fluid connections on that face."));
            out.accept(net.minecraft.network.chat.Component.literal("Up to 4x extraction: 8 JE per extra item or 25 mB. No power spent on blocked transfers."));
            out.accept(io.github.jimbozoomer.jugcraft.tools.PoweredToolItem.energyLine(stack));
        }
    }
    private static final class Lens extends Item {
        final int code;
        Lens(Properties p,int code){super(p);this.code=code;}
        @Override public InteractionResult useOn(UseOnContext c){
            var n=at(c.getLevel(),c.getClickedPos());if(n==null||!n.allowsLenses())return InteractionResult.PASS;
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!editable(n,p)||!n.free(c.getClickedFace()))return InteractionResult.FAIL;
            n.lenses[c.getClickedFace().ordinal()]=code;c.getItemInHand().consume(1,p);n.changed();return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<net.minecraft.network.chat.Component> out,TooltipFlag f){
            out.accept(net.minecraft.network.chat.Component.literal(code>17?"Only matching route colors may cross this face.":"Sets the route color when an item crosses this face."));
            out.accept(net.minecraft.network.chat.Component.literal("Through-routing factory item pipes. Sneak-use empty-handed to remove. Does not dye the item itself."));
        }
    }
    public static final class Fittings {
        private final BlockEntity owner;
        public Fittings(BlockEntity owner){this.owner=owner;this.power=new io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage(2048,256,0,owner::setChanged);}
        public Level getLevel(){return owner.getLevel();}
        public BlockPos getBlockPos(){return owner.getBlockPos();}
        public BlockState getBlockState(){return owner.getBlockState();}
        public boolean isRemoved(){return owner.isRemoved();}
        public boolean allowsLenses(){return getBlockState().getBlock() instanceof io.github.jimbozoomer.jugcraft.logistics.ItemPipeBlock;}

        public int mask;
        public int wires;
        public int paint=-1;
        public int powerFace=-1;
        public final io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage power;
        public boolean allowsAdapter(){var block=getBlockState().getBlock();return block instanceof IndustrialTransport.ItemExtractor||block instanceof IndustrialTransport.FluidExtractor||block instanceof IndustrialTransportDevices.Device d&&d.kind.startsWith("diamond_wood_");}
        public boolean hasAdapter(){return powerFace>=0&&allowsAdapter();}
        public ItemStack adaptorStack(){var stack=new ItemStack(POWER_ADAPTOR);io.github.jimbozoomer.jugcraft.tools.Chargeable.setEnergy(stack,power.getAmount());return stack;}
        public long boostLimit(long base,long quantum){return !hasAdapter()?base:Math.min(base*4,base+(power.getAmount()/8)*quantum);}
        public void payBoost(long moved,long base,long quantum){if(!hasAdapter()||moved<=base)return;long cost=((moved-base+quantum-1)/quantum)*8;power.setAmount(power.getAmount()-cost);owner.setChanged();}
        private boolean powerLive(Direction side){return side!=null&&hasAdapter()&&powerFace==side.ordinal()&&TransportPortStorage.liveEnergy(owner,side);}
        public io.github.jimbozoomer.jugcraft.energy.EnergyStorage powerPort(Direction side){
            if(side==null||!hasAdapter()||powerFace!=side.ordinal())return null;
            return new io.github.jimbozoomer.jugcraft.energy.EnergyStorage(){
                public boolean supportsInsertion(){return powerLive(side);}
                public long insert(long amount,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx){if(amount<0)throw new IllegalArgumentException("Negative power");return powerLive(side)?power.insert(amount,tx):0;}
                public boolean supportsExtraction(){return false;}
                public long extract(long amount,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx){return 0;}
                public long getAmount(){return powerLive(side)?power.getAmount():0;}
                public long getCapacity(){return powerLive(side)?power.getCapacity():0;}
                public Object transferIdentity(){return power;}
            };
        }
        public final int[] lenses=new int[6];
        public final int[] facadeModes=new int[6];
        public final List<TransportFacades.Phase> phases=new ArrayList<>(Collections.nCopies(6,TransportFacades.Phase.NONE));
        public final List<BlockState> faces=new ArrayList<>(Collections.nCopies(6,Blocks.AIR.defaultBlockState()));
        public boolean plugged(Direction side){return (mask&(1<<side.ordinal()))!=0;}
        public boolean blocked(Direction side){
            if(side==null){for(var face:Direction.values())if(blocked(face))return true;return false;}
            return plugged(side)||!faces.get(side.ordinal()).isAir()&&facadeModes[side.ordinal()]==TransportFacades.SEALED;
        }
        public ItemStack facadeStack(Direction side){var stack=TransportFacades.stack(faces.get(side.ordinal()),facadeModes[side.ordinal()]);var phase=phases.get(side.ordinal());if(phase.valid())stack.set(TransportFacades.PHASE,phase);return stack;}
        public boolean free(Direction side){return !blocked(side)&&powerFace!=side.ordinal()&&lenses[side.ordinal()]==0&&faces.get(side.ordinal()).isAir();}
        public int route(Direction side,int color){int code=lenses[side.ordinal()];return code==0?color:code<=17?code-2:color==code-19?color:-2;}
        public void changed(){
            owner.setChanged();var level=getLevel();var worldPosition=getBlockPos();if(level==null)return;
            ItemNetworks.invalidate(level);FluidNetworks.invalidate(level);EnergyNetworks.invalidate(level);
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);
            if(!level.isClientSide()){
                level.scheduleTick(worldPosition,getBlockState().getBlock(),1);
                level.updateNeighborsAt(worldPosition,getBlockState().getBlock());
            }
        }
        public void save(ValueOutput out){out.putInt("signal_wires",wires);out.store("facade_phases",TransportFacades.Phase.CODEC.listOf(0,6),phases);out.putInt("pipe_paint",paint);out.putInt("power_face",powerFace);out.putLong("adapter_energy",power.getAmount());out.putInt("blocked_faces",mask);out.store("covers",BlockState.CODEC.listOf(0,6),faces);out.store("facade_modes",com.mojang.serialization.Codec.intRange(0,2).listOf(0,6),Arrays.stream(facadeModes).boxed().toList());out.store("lenses",com.mojang.serialization.Codec.intRange(0,34).listOf(0,6),Arrays.stream(lenses).boxed().toList());}
        public void load(ValueInput in){
            wires=in.getIntOr("signal_wires",0)&65535;
            paint=Math.clamp(in.getIntOr("pipe_paint",-1),-1,15);
            mask=in.getIntOr("blocked_faces",0)&63;var saved=in.read("covers",BlockState.CODEC.listOf(0,6)).orElse(List.of());
            for(int i=0;i<6;i++)faces.set(i,i<saved.size()&&DigitalFacades.valid(saved.get(i))&&(mask&(1<<i))==0?saved.get(i):Blocks.AIR.defaultBlockState());
            Collections.fill(phases,TransportFacades.Phase.NONE);var savedPhases=in.read("facade_phases",TransportFacades.Phase.CODEC.listOf(0,6)).orElse(List.of());
            for(int i=0;i<savedPhases.size();i++)if(!faces.get(i).isAir()&&savedPhases.get(i).valid())phases.set(i,savedPhases.get(i));
            Arrays.fill(facadeModes,0);var modes=in.read("facade_modes",com.mojang.serialization.Codec.intRange(0,2).listOf(0,6)).orElse(List.of());
            for(int i=0;i<modes.size();i++)if(!faces.get(i).isAir())facadeModes[i]=modes.get(i);
            Arrays.fill(lenses,0);var ls=in.read("lenses",com.mojang.serialization.Codec.intRange(0,34).listOf(0,6)).orElse(List.of());
            if(allowsLenses())for(int i=0;i<ls.size();i++)if((mask&(1<<i))==0&&faces.get(i).isAir())lenses[i]=ls.get(i);
            powerFace=-1;int face=in.getIntOr("power_face",-1);
            if(face>=0&&face<6&&allowsAdapter()&&free(Direction.values()[face]))powerFace=face;
            power.setAmount(hasAdapter()?in.getLongOr("adapter_energy",0):0);
        }
        public void drop(){
            var level=getLevel();var p=getBlockPos();
            if(level!=null&&!level.isClientSide())for(var colour:DyeColor.values())if((wires&(1<<colour.ordinal()))!=0)Block.popResource(level,p,new ItemStack(FactoryWires.ITEMS.get(colour)));
            wires=0;
            if(level!=null&&!level.isClientSide())for(Direction side:Direction.values()){
                if(powerFace==side.ordinal())Block.popResource(level,p,adaptorStack());
                if(plugged(side))Block.popResource(level,p,new ItemStack(BLOCKER));
                if(lenses[side.ordinal()]!=0)Block.popResource(level,p,new ItemStack(LENSES.get(lenses[side.ordinal()]-1)));
                if(!faces.get(side.ordinal()).isAir())Block.popResource(level,p,facadeStack(side));
            }
            Collections.fill(phases,TransportFacades.Phase.NONE);paint=-1;mask=0;powerFace=-1;power.setAmount(0);Arrays.fill(lenses,0);Arrays.fill(facadeModes,0);Collections.fill(faces,Blocks.AIR.defaultBlockState());
        }
        public net.minecraft.nbt.CompoundTag updateTag(HolderLookup.Provider lookup){
            var out=TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,lookup);out.putBoolean("fittings_only",true);save(out);return out.buildResult();
        }
    }
    public static final class Node extends BlockEntity implements Host {
        private final Fittings fittings=new Fittings(this);
        public Node(BlockPos p,BlockState s){super(TYPE,p,s);}
        @Override public Fittings attachments(){return fittings;}
        public boolean blocked(Direction side){return fittings.blocked(side);}
        public int route(Direction side,int color){return fittings.route(side,color);}
        public void changed(){fittings.changed();}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);fittings.save(out);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);fittings.load(in);}
        @Override public void preRemoveSideEffects(BlockPos p,BlockState s){fittings.drop();super.preRemoveSideEffects(p,s);}
        @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
        @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider lookup){return fittings.updateTag(lookup);}
    }
}
