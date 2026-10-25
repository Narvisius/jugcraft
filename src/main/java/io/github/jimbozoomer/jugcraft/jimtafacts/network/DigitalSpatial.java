package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Bounded, journaled building exchange. Unsupported live machines/entities are refused before any change. */
public final class DigitalSpatial {
    public static Block PORT, PYLON;
    public static BlockEntityType<Port> TYPE;
    public static DataComponentType<PlotKey> KEY;
    public static DataComponentType<List<GlobalPos>> CORNERS;
    private DigitalSpatial() {}
    public record PlotKey(UUID id, long revision) {
        public static final Codec<PlotKey> CODEC = RecordCodecBuilder.create(i -> i.group(UUIDUtil.CODEC.fieldOf("id").forGetter(PlotKey::id), io.github.jimbozoomer.jugcraft.jimtafacts.JimtaCodecs.longRange(0, Long.MAX_VALUE - 1).fieldOf("revision").forGetter(PlotKey::revision)).apply(i, PlotKey::new));
    }
    public static void register() {
        KEY = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("jimta_spatial_plot"), DataComponentType.<PlotKey>builder().persistent(PlotKey.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(PlotKey.CODEC)).build());
        CORNERS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("jimta_spatial_corners"), DataComponentType.<List<GlobalPos>>builder().persistent(GlobalPos.CODEC.listOf(0,2)).networkSynchronized(ByteBufCodecs.fromCodec(GlobalPos.CODEC.listOf(0,2))).build());
        for (String name : List.of("spatial_io_port", "spatial_pylon")) {
            var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("jimta_" + name)); var p = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().setId(key);
            Block block = name.equals("spatial_io_port") ? new PortBlock(p) : new Pylon(p);
            Registry.register(BuiltInRegistries.BLOCK, key, block); DigitalStorage.BLOCKS.put(name, block);
            var ik = ResourceKey.create(Registries.ITEM, key.identifier()); Registry.register(BuiltInRegistries.ITEM, ik, new BlockItem(block, new Item.Properties().setId(ik).useBlockDescriptionPrefix()));
            if (name.equals("spatial_io_port")) PORT = block; else PYLON = block;
        }
        for (int size : new int[]{2,16}) for (boolean cell : new boolean[]{false,true}) {
            String name = "spatial_" + (cell ? "storage_cell_" : "cell_component_") + size; var key = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_" + name));
            DigitalStorage.ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, cell ? new Cell(new Item.Properties().setId(key).stacksTo(1), size) : new Item(new Item.Properties().setId(key))));
        }
        TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("jimta_spatial_port"), FabricBlockEntityTypeBuilder.create(Port::new, PORT).build());
        EnergyStorage.SIDED.registerForBlockEntity((port, side) -> port.energy, TYPE);
        ServerLifecycleEvents.SERVER_STARTED.register(DigitalSpatialJournal::recover);
    }
    public static final class Cell extends Item {
        public final int size;
        Cell(Properties p, int size) { super(p); this.size = size; }
        @Override public boolean canFitInsideContainerItems() { return false; }
        @Override public InteractionResult useOn(UseOnContext c) {
            if (!(c.getPlayer() instanceof ServerPlayer p) || !c.getLevel().getBlockState(c.getClickedPos()).is(PYLON)) return InteractionResult.PASS;
            if (!Authority.mayChange(p.level(), p, c.getClickedPos())) return InteractionResult.FAIL;
            var held = c.getItemInHand(); var old = held.getOrDefault(CORNERS,List.<GlobalPos>of()); var point = GlobalPos.of(p.level().dimension(), c.getClickedPos());
            var next = p.isShiftKeyDown() || old.size() != 1 || !old.getFirst().dimension().equals(point.dimension()) ? List.of(point) : List.of(old.getFirst(),point);
            held.set(CORNERS,next); p.sendOverlayMessage(Component.literal(next.size()==1 ? "First pylon selected. Select the opposite corner, with eight pylons around the box." : "Region selected. Insert this cell into the Spatial I/O Port, then sneak-use the port to exchange.")); return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, net.minecraft.world.item.component.TooltipDisplay d, java.util.function.Consumer<Component> out, TooltipFlag f) {
            out.accept(Component.literal("Up to " + size + " × " + size + " × " + size + " static building blocks"));
            out.accept(Component.literal("Select opposite corner pylons. No entities, containers, fluids or active blocks."));
            if (s.has(KEY)) out.accept(Component.literal("Stored plot revision " + s.get(KEY).revision()));
        }
    }
    public static final class Pylon extends Block {
        public static final BooleanProperty LIT = BlockStateProperties.LIT;
        public Pylon(Properties p) { super(p); registerDefaultState(defaultBlockState().setValue(LIT,false)); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(LIT); }
        @Override protected void tick(BlockState s, ServerLevel l, BlockPos p, net.minecraft.util.RandomSource random) { l.setBlock(p,s.setValue(LIT,false),Block.UPDATE_CLIENTS); }
    }
    private static final class PortBlock extends Block implements EntityBlock, EnergyConnectable {
        static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
        PortBlock(Properties p) { super(p); registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH)); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(FACING); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite()); }
        @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new Port(p,s); }
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity entity,ItemStack stack) { if (entity instanceof ServerPlayer player && l.getBlockEntity(p) instanceof Port port) { port.owner=player.getUUID(); port.setChanged(); } }
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
            if (!(stack.getItem() instanceof Cell)) return InteractionResult.PASS;
            if (player instanceof ServerPlayer p && level.getBlockEntity(pos) instanceof Port port && port.permitted(p) && port.getItem(0).isEmpty()) { port.setItem(0,stack.split(1)); return InteractionResult.SUCCESS; }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
            if (player instanceof ServerPlayer p && level.getBlockEntity(pos) instanceof Port port && port.permitted(p)) {
                if (p.isShiftKeyDown()) port.exchange(p);
                else p.openMenu(new SimpleMenuProvider((id,inv,who)->new Menu(id,inv,port),Component.literal("Spatial I/O · Input / Output")));
                p.sendOverlayMessage(Component.literal(port.status + " · " + port.energy.getAmount() + " JE · sneak-use to exchange"));
            }
            return InteractionResult.SUCCESS;
        }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
            return !level.isClientSide() && type==TYPE ? (world,pos,s,entity)->((Port)entity).tick() : null;
        }
    }
    public static final class Port extends BlockEntity implements Container {
        UUID owner; boolean signal; String status="Eight corner pylons required";
        final NonNullList<ItemStack> inventory=NonNullList.withSize(2,ItemStack.EMPTY);
        public final SimpleEnergyStorage energy=new SimpleEnergyStorage(1_000_000,4096,0,this::setChanged);
        Port(BlockPos p,BlockState s) { super(TYPE,p,s); }
        boolean permitted(ServerPlayer p) { return p.isAlive() && !p.isSpectator() && p.level()==level && owner!=null && (owner.equals(p.getUUID()) || io.github.jimbozoomer.jugcraft.party.JugcraftParties.sameParty(owner,p.getUUID())) && Authority.mayChange(level,p,worldPosition); }
        void tick() { if (!(level instanceof ServerLevel server)) return; boolean powered=level.hasNeighborSignal(worldPosition); if (powered && !signal) { var p=Authority.answering(server,owner); if (p!=null && permitted(p)) exchange(p); } signal=powered; }
        void exchange(ServerPlayer p) { if (!permitted(p)) return; status=DigitalSpatialJournal.exchange(this,p); p.sendOverlayMessage(Component.literal(status)); }
        @Override public int getContainerSize(){return 2;}
        @Override public boolean isEmpty(){return inventory.stream().allMatch(ItemStack::isEmpty);}
        @Override public ItemStack getItem(int slot){return inventory.get(slot);}
        @Override public ItemStack removeItem(int slot,int amount){var item=ContainerHelper.removeItem(inventory,slot,amount);setChanged();return item;}
        @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(inventory,slot);}
        @Override public void setItem(int slot,ItemStack stack){inventory.set(slot,stack);setChanged();}
        @Override public boolean stillValid(Player player){return player instanceof ServerPlayer p && permitted(p) && Container.stillValidBlockEntity(this,p);}
        @Override public void clearContent(){inventory.clear();setChanged();}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0 && stack.getItem() instanceof Cell;}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);ContainerHelper.saveAllItems(out,inventory);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.putLong("energy",energy.getAmount());out.putBoolean("signal",signal);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);ContainerHelper.loadAllItems(in,inventory);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);energy.setAmount(in.getLongOr("energy",0));signal=in.getBooleanOr("signal",false);}
        @Override public void preRemoveSideEffects(BlockPos p,BlockState s){if(level!=null&&!level.isClientSide()){Containers.dropContents(level,p,this);inventory.clear();}}
    }
    private static final class Menu extends AbstractContainerMenu {
        final Port port;
        Menu(int id,Inventory inv,Port port){super(MenuType.GENERIC_9x1,id);this.port=port;var blanks=new SimpleContainer(7);
            for(int i=0;i<9;i++){final int slot=i;addSlot(new Slot(i<2?port:blanks,i<2?i:i-2,8+i*18,18){@Override public boolean mayPlace(ItemStack s){return slot==0&&s.getItem() instanceof Cell;}@Override public boolean mayPickup(Player p){return slot<2;}});}
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+col*18,49+row*18));for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,107));
        }
        @Override public boolean stillValid(Player p){return port.stillValid(p);}
        @Override public void clicked(int slot,int button,ContainerInput type,Player p){if(stillValid(p))super.clicked(slot,button,type,p);}
        @Override public ItemStack quickMoveStack(Player p,int index){if(!stillValid(p)||index<0||index>=slots.size()||index>=2&&index<9)return ItemStack.EMPTY;var slot=slots.get(index);var s=slot.getItem();var copy=s.copy();if(index<2?!moveItemStackTo(s,9,slots.size(),true):!(s.getItem() instanceof Cell)||!moveItemStackTo(s,0,1,false))return ItemStack.EMPTY;slot.setByPlayer(s.isEmpty()?ItemStack.EMPTY:s);slot.setChanged();return copy;}
    }
}
