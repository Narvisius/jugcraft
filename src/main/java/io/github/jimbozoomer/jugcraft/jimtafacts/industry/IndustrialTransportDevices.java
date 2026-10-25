package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.fluid.*;
import io.github.jimbozoomer.jugcraft.logistics.*;
import io.github.jimbozoomer.jugcraft.mixin.AlchemicalItemAccess;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import io.github.jimbozoomer.jugcraft.tools.PoweredToolItem;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Saved filters/cargo and owner-aware world endpoints; native instantaneous routing. */
public final class IndustrialTransportDevices {
    public static BlockEntityType<Node> ENTITY;
    private static final int[] CARGO={9,10,11,12,13,14,15,16,17};
    private static final Item[] DYES={Items.DYE.pick(net.minecraft.world.item.DyeColor.WHITE),Items.DYE.pick(net.minecraft.world.item.DyeColor.ORANGE),Items.DYE.pick(net.minecraft.world.item.DyeColor.MAGENTA),Items.DYE.pick(net.minecraft.world.item.DyeColor.LIGHT_BLUE),Items.DYE.pick(net.minecraft.world.item.DyeColor.YELLOW),Items.DYE.pick(net.minecraft.world.item.DyeColor.LIME),Items.DYE.pick(net.minecraft.world.item.DyeColor.PINK),Items.DYE.pick(net.minecraft.world.item.DyeColor.GRAY),Items.DYE.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY),Items.DYE.pick(net.minecraft.world.item.DyeColor.CYAN),Items.DYE.pick(net.minecraft.world.item.DyeColor.PURPLE),Items.DYE.pick(net.minecraft.world.item.DyeColor.BLUE),Items.DYE.pick(net.minecraft.world.item.DyeColor.BROWN),Items.DYE.pick(net.minecraft.world.item.DyeColor.GREEN),Items.DYE.pick(net.minecraft.world.item.DyeColor.RED),Items.DYE.pick(net.minecraft.world.item.DyeColor.BLACK)};
    private IndustrialTransportDevices() {}
    public static void register(){
        List<Block> entities=new ArrayList<>();
        for(String kind:List.of("obsidian_item","stripes_item","diamond_wood_item","diamond_wood_fluid")){
            var block=new Device(IndustrialTransport.properties(kind),kind);IndustrialTransport.put(kind,block);entities.add(block);
        }
        for(String kind:List.of("lapis_item","daizuli_item","emzuli_item")){
            var block=new ColorPipe(IndustrialTransport.properties(kind),kind);IndustrialTransport.put(kind,block);entities.add(block);
        }
        ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_transport_device"),
                FabricBlockEntityTypeBuilder.create(Node::new,entities.toArray(Block[]::new)).build());
        ItemStorage.SIDED.registerForBlockEntity((n,side)->n.getBlockState().getBlock() instanceof ColorPipe||n.kind().endsWith("fluid")?null:ContainerStorage.of(n,side==null?Direction.DOWN:side),ENTITY);
    }
    public static final class ColorPipe extends ItemPipeBlock implements EntityBlock {
        public static final IntegerProperty COLOR=IntegerProperty.create("color",0,15);
        final String kind;
        ColorPipe(Properties p,String kind){super(p);this.kind=kind;registerDefaultState(defaultBlockState().setValue(COLOR,11));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(COLOR);}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public boolean coloredRouting(){return true;}
        @Override public int routeColor(Level l,BlockPos p,BlockState s,ItemVariant item,int incoming){
            int chosen=s.getValue(COLOR);
            if(kind.equals("daizuli_item"))return incoming==chosen?incoming:-2;
            if(kind.equals("emzuli_item"))return l.getBlockEntity(p) instanceof Node n?n.paintedColor(item):-2;
            return chosen;
        }
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
            if(l instanceof ServerLevel server){
                if(player.isSpectator()||!Authority.mayChange(server,player,p))return InteractionResult.FAIL;
                if(kind.equals("emzuli_item")&&l.getBlockEntity(p) instanceof Node n)player.openMenu(n);
                else {int next=(s.getValue(COLOR)+1)%16;l.setBlock(p,s.setValue(COLOR,next),Block.UPDATE_ALL);ItemNetworks.invalidate(l);
                    player.sendOverlayMessage(Component.literal("Route color: "+DyeColor.byId(next).getName()));}
            }return InteractionResult.SUCCESS;
        }
    }
    public static final class Device extends Block implements EntityBlock,ItemConnectable,FluidConnectable {
        final String kind;
        Device(Properties p,String kind){super(p);this.kind=kind;registerDefaultState(defaultBlockState().setValue(DirectionalBlock.FACING,Direction.NORTH));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(DirectionalBlock.FACING);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(DirectionalBlock.FACING,c.getNearestLookingDirection().getOpposite());}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return !l.isClientSide()&&type==ENTITY?(world,p,state,e)->{if(world.getGameTime()%8==0)((Node)e).step((ServerLevel)world);}:null;}
        @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){
            return net.minecraft.world.phys.shapes.Shapes.or(Block.box(5,5,0,11,11,16),Block.box(0,5,5,16,11,11),Block.box(5,0,5,11,16,11));}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity placer,ItemStack stack){
            super.setPlacedBy(l,p,s,placer,stack);if(placer!=null&&l.getBlockEntity(p) instanceof Node n){n.owner=placer.getUUID();n.setChanged();}}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
            if(l instanceof ServerLevel server&&l.getBlockEntity(p) instanceof Node n){
                if(player.isSpectator()||!Authority.mayChange(server,player,p))return InteractionResult.FAIL;
                if(player.isShiftKeyDown()&&kind.equals("stripes_item")){n.mining=!n.mining;n.setChanged();player.sendOverlayMessage(Component.literal(n.mining?"Stripes: break front. Tool top-left; cargo below.":"Stripes: place front from top-row supplies."));}
                else if(player.isShiftKeyDown()){Direction next=Direction.values()[(s.getValue(DirectionalBlock.FACING).ordinal()+1)%6];
                    l.setBlock(p,s.setValue(DirectionalBlock.FACING,next),Block.UPDATE_ALL);ItemNetworks.invalidate(l);FluidNetworks.invalidate(l);player.sendOverlayMessage(Component.literal("Working face: "+next.getName()));}
                else {
                    player.openMenu(n);
                    if(kind.endsWith("fluid"))player.sendOverlayMessage(Component.literal("Top: filled-container filters. Buffer: "+n.fluid.amount/FluidNetworks.DROPLETS_PER_MB+" / 1000 mB. Intake: "+s.getValue(DirectionalBlock.FACING).getName()+"; other faces output."));
                    else player.sendOverlayMessage(Component.literal(kind.equals("obsidian_item")?"Collects nearby loose items into the bottom row. Redstone pauses collection.":"Top row: filters / supplies. Bottom row: cargo. Sneak-use changes mode or intake."));
                }
            }return InteractionResult.SUCCESS;
        }
    }
    public static final class Node extends BaseContainerBlockEntity implements WorldlyContainer {
        private NonNullList<ItemStack> inventory=NonNullList.withSize(18,ItemStack.EMPTY);
        private int[] bankColors={11,14,5,4};
        private UUID owner;private boolean mining;
        private final SingleFluidStorage fluid=new SingleFluidStorage(){protected long getCapacity(FluidVariant v){return FluidConstants.BUCKET;}protected void onFinalCommit(){setChanged();}};
        Node(BlockPos p,BlockState s){super(ENTITY,p,s);if(kind().equals("emzuli_item"))inventory=NonNullList.withSize(36,ItemStack.EMPTY);}
        String kind(){var b=getBlockState().getBlock();return b instanceof Device d?d.kind:((ColorPipe)b).kind;}
        int paintedColor(ItemVariant item){
            for(int bank=0;bank<4;bank++)for(int slot=bank*9;slot<bank*9+9;slot++)if(!inventory.get(slot).isEmpty()&&item.isOf(inventory.get(slot).getItem()))return bankColors[bank];
            return -2;
        }
        boolean matches(ItemVariant v){for(int i=0;i<9;i++)if(!inventory.get(i).isEmpty()&&v.isOf(inventory.get(i).getItem()))return true;return false;}
        private boolean allowed(ServerLevel l,BlockPos p){return l.hasChunkAt(p)&&Authority.mayChangeFor(l,owner,p)&&ClaimTransfers.allowed(l,worldPosition,p);}
        void step(ServerLevel l){
            if(l.hasNeighborSignal(worldPosition)||!allowed(l,worldPosition))return;
            Direction face=getBlockState().getValue(DirectionalBlock.FACING);BlockPos front=worldPosition.relative(face);
            if(kind().equals("obsidian_item"))collect(l);
            else if(kind().equals("stripes_item")){if(allowed(l,front)){if(mining)mine(l,front);else place(l,front);}}
            else if(allowed(l,front))extract(l,front,face);
            var cargo=ContainerStorage.of(this,Direction.DOWN);
            long left=16;for(Direction side:Direction.values())if(side!=face&&left>0)left-=ItemNetworks.push(l,worldPosition,side,cargo,left,Set.of(front));
            if(kind().endsWith("fluid"))FluidNetworks.pushToNeighbors(l,worldPosition,fluid,250*FluidNetworks.DROPLETS_PER_MB,Arrays.stream(Direction.values()).filter(d->d!=face).toList(),Set.of(front));
        }
        private void extract(ServerLevel l,BlockPos front,Direction face){
            if(kind().endsWith("fluid")){
                var source=FluidStorage.SIDED.find(l,front,face.getOpposite());if(source==null)return;Set<FluidVariant> filters=new HashSet<>();
                for(int i=0;i<9;i++){var stack=inventory.get(i);if(stack.isEmpty())continue;var held=FluidStorage.ITEM.find(stack,ContainerItemContext.withConstant(stack));if(held==null)continue;
                    int examined=0;for(var view:held){if(++examined>32)break;if(!view.isResourceBlank()&&view.getAmount()>0)filters.add(view.getResource());}}
                moveBounded(source,fluid,filters::contains,250*FluidNetworks.DROPLETS_PER_MB);
            }else {var source=ItemStorage.SIDED.find(l,front,face.getOpposite());if(source!=null)moveBounded(source,ContainerStorage.of(this,Direction.DOWN),this::matches,16);}
        }
        private static <T extends TransferVariant<?>> void moveBounded(Storage<T> source,Storage<T> target,java.util.function.Predicate<T> accepts,long limit){
            var offered=new LinkedHashMap<T,Long>();int views=0;
            // Inspect before moving anything; an oversized external inventory cannot create an unbounded pass.
            for(var view:source){if(++views>128)return;if(view.isResourceBlank()||view.getAmount()<=0||!accepts.test(view.getResource()))continue;
                offered.merge(view.getResource(),Math.min(limit,view.getAmount()),(a,b)->Math.min(limit,a+b));}
            long left=limit;
            for(var entry:offered.entrySet()){
                if(left<=0)break;
                try(var tx=Transaction.openOuter()){
                    long moved=target.insert(entry.getKey(),Math.min(left,entry.getValue()),tx);
                    if(moved>0&&source.extract(entry.getKey(),moved,tx)==moved){tx.commit();left-=moved;}
                }
            }
        }
        private void collect(ServerLevel l){
            List<ItemEntity> nearby=new ArrayList<>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ItemEntity.class),new AABB(worldPosition).inflate(2),e->e.isAlive()&&!e.hasPickUpDelay(),nearby,16);
            for(var entity:nearby){UUID reserved=((AlchemicalItemAccess)entity).jugcraft$pickupTarget();if(reserved!=null&&!reserved.equals(owner)||!allowed(l,entity.blockPosition()))continue;
                var end=entity.position().add(0,.1,0);var start=Vec3.atCenterOf(worldPosition);var ray=end.subtract(start);
                double edge=.501/Math.max(.001,Math.max(Math.abs(ray.normalize().x),Math.max(Math.abs(ray.normalize().y),Math.abs(ray.normalize().z))));
                if(ray.length()>edge)start=start.add(ray.normalize().scale(edge));else start=end;
                var hit=l.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,entity));
                if(hit.getType()!=HitResult.Type.MISS)continue;
                var stack=entity.getItem();try(var tx=Transaction.openOuter()){long accepted=ContainerStorage.of(this,Direction.DOWN).insert(ItemVariant.of(stack),Math.min(16,stack.getCount()),tx);
                    if(accepted<=0)continue;tx.commit();int left=stack.getCount()-(int)accepted;if(left==0)entity.discard();else entity.setItem(stack.copyWithCount(left));return;}
            }
        }
        private void place(ServerLevel l,BlockPos p){
            if(!l.getBlockState(p).isAir())return;
            for(int i=0;i<9;i++){var stack=inventory.get(i);if(!(stack.getItem() instanceof BlockItem item))continue;Block b=item.getBlock();
                if(b instanceof EntityBlock||b instanceof DoorBlock||b instanceof BedBlock||b instanceof DoublePlantBlock||!Blueprint.allowed(b))continue;
                var state=b.defaultBlockState();var shape=state.getCollisionShape(l,p);if(!state.canSurvive(l,p)||!shape.isEmpty()&&!l.noCollision(shape.move(p.getX(),p.getY(),p.getZ()).bounds()))continue;
                if(l.setBlock(p,state,Block.UPDATE_ALL)){stack.shrink(1);setChanged();}return;}
        }
        private void mine(ServerLevel l,BlockPos p){
            var state=l.getBlockState(p);var tool=inventory.get(0);var actor=Authority.answering(l,owner);
            if(actor==null||state.isAir()||l.getBlockEntity(p)!=null||!state.getFluidState().isEmpty()||state.getDestroySpeed(l,p)<0||state.getDestroySpeed(l,p)>50||tool.isEmpty()||!tool.has(net.minecraft.core.component.DataComponents.TOOL)||state.requiresCorrectToolForDrops()&&!tool.isCorrectToolForDrops(state))return;
            if(tool.getItem() instanceof PoweredToolItem powered&&Chargeable.energy(tool)<powered.energyPerBlock(tool))return;
            var storage=ContainerStorage.of(this,Direction.DOWN);
            try(var tx=Transaction.openOuter()){
                for(var drop:Block.getDrops(state,l,p,null,actor,tool))if(!drop.isEmpty()&&storage.insert(ItemVariant.of(drop),drop.getCount(),tx)!=drop.getCount())return;
                if(!l.destroyBlock(p,false,actor))return;tx.commit();
            }
            if(tool.getItem() instanceof PoweredToolItem powered)Chargeable.drain(tool,powered.energyPerBlock(tool));
            else if(tool.isDamageableItem())tool.hurtAndBreak(1,actor,EquipmentSlot.MAINHAND);setChanged();
        }
        @Override protected Component getDefaultName(){return Component.literal(kind().equals("emzuli_item")?"Emzuli: four color-filter rows":kind().equals("stripes_item")?"Stripes: supplies / cargo":"Pipe: samples / cargo");}
        @Override protected NonNullList<ItemStack> getItems(){return inventory;}
        @Override protected void setItems(NonNullList<ItemStack> items){inventory=items;}
        @Override public int getContainerSize(){return kind().equals("emzuli_item")?36:18;}
        @Override protected AbstractContainerMenu createMenu(int id,Inventory player){return kind().equals("emzuli_item")?new ColorMenu(id,player,this):new ChestMenu(MenuType.GENERIC_9x2,id,player,this,2);}
        @Override public boolean stillValid(Player player){return super.stillValid(player)&&!player.isSpectator()&&level instanceof ServerLevel l&&Authority.mayChange(l,player,worldPosition);}
        @Override public int[] getSlotsForFace(Direction side){return getBlockState().getBlock() instanceof ColorPipe?new int[0]:CARGO;}
        @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return !(getBlockState().getBlock() instanceof ColorPipe)&&slot>=9&&slot<18;}
        @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return canPlaceItemThroughFace(slot,stack,side);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);inventory=NonNullList.withSize(getContainerSize(),ItemStack.EMPTY);ContainerHelper.loadAllItems(in,inventory);
            int first=getBlockState().hasProperty(ColorPipe.COLOR)?getBlockState().getValue(ColorPipe.COLOR):11;
            bankColors=new int[]{first,14,5,4};for(int i=0;i<4;i++)bankColors[i]=Math.clamp(in.getIntOr("BankColor"+i,bankColors[i]),0,15);
            try{owner=UUID.fromString(in.getStringOr("Owner",""));}catch(IllegalArgumentException e){owner=null;}
            mining=in.getBooleanOr("Mining",false);fluid.variant=in.read("Fluid",FluidVariant.CODEC).orElse(FluidVariant.blank());fluid.amount=Math.clamp(in.getLongOr("FluidAmount",0),0,FluidConstants.BUCKET);if(fluid.variant.isBlank())fluid.amount=0;}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);ContainerHelper.saveAllItems(out,inventory);if(owner!=null)out.putString("Owner",owner.toString());out.putBoolean("Mining",mining);out.store("Fluid",FluidVariant.CODEC,fluid.variant);out.putLong("FluidAmount",fluid.amount);for(int i=0;i<4;i++)out.putInt("BankColor"+i,bankColors[i]);}
    }
    /** Four physical sample rows. The bottom icons are controls, never obtainable items. */
    private static final class ColorMenu extends AbstractContainerMenu {
        final Node node;final SimpleContainer controls=new SimpleContainer(9);
        ColorMenu(int id,Inventory inventory,Node node){
            super(MenuType.GENERIC_9x5,id);this.node=node;
            for(int i=0;i<36;i++)addSlot(new Slot(node,i,8+i%9*18,18+i/9*18));
            for(int i=0;i<9;i++)addSlot(new Slot(controls,i,8+i*18,90){@Override public boolean mayPlace(ItemStack stack){return false;}@Override public boolean mayPickup(Player p){return false;}});
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,121+row*18));
            for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,179));refresh();
        }
        private void refresh(){
            for(int i=0;i<9;i++){
                boolean button=i%2==0&&i<8;int bank=i/2;
                ItemStack icon=button?new ItemStack(DYES[node.bankColors[bank]]):new ItemStack(Items.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.GRAY));
                icon.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal(button?"Row "+(bank+1)+": "+DyeColor.byId(node.bankColors[bank]).getName()+" · left/right click changes color":"Samples stay here. First matching row wins."));controls.setItem(i,icon);
            }
        }
        @Override public boolean stillValid(Player p){return node.stillValid(p);}
        @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
        @Override public void clicked(int slot,int button,ContainerInput type,Player p){
            if(!stillValid(p))return;
            if(slot>=36&&slot<45){int control=slot-36;
                if(type==ContainerInput.PICKUP&&control%2==0&&control<8&&(button==0||button==1)){
                    int bank=control/2;node.bankColors[bank]=Math.floorMod(node.bankColors[bank]+(button==0?1:-1),16);node.setChanged();ItemNetworks.invalidate(node.getLevel());refresh();broadcastChanges();
                }return;
            }super.clicked(slot,button,type,p);
        }
        @Override public ItemStack quickMoveStack(Player p,int index){
            if(!stillValid(p)||index<0||index>=slots.size()||index>=36&&index<45)return ItemStack.EMPTY;
            var slot=slots.get(index);var stack=slot.getItem();if(stack.isEmpty())return ItemStack.EMPTY;var copy=stack.copy();
            if(index<36?!moveItemStackTo(stack,45,slots.size(),true):!moveItemStackTo(stack,0,36,false))return ItemStack.EMPTY;
            slot.setByPlayer(stack.isEmpty()?ItemStack.EMPTY:stack);slot.setChanged();return copy;
        }
    }
}
