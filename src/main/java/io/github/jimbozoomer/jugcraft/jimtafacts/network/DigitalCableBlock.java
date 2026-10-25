package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Connections are real mesh arms; cable runs stop at unloaded chunks. */
public class DigitalCableBlock extends PipeBlock implements net.minecraft.world.level.block.EntityBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LAMP=net.minecraft.world.level.block.state.properties.BooleanProperty.create("lamp");
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty CHANNELS = net.minecraft.world.level.block.state.properties.IntegerProperty.create("channels", 0, 8);
    public final String color;
    public final int capacity;
    public DigitalCableBlock(Properties p) { this(p, "fluix"); }
    public DigitalCableBlock(Properties p, String color) { this(p, color, 4); }
    public DigitalCableBlock(Properties p, String color, float width) {
        super(width, p.lightLevel(s->s.getValue(LAMP)?15:0));
        this.color = color;
        this.capacity = width >= 10 ? 32 : 8;
        var state = stateDefinition.any().setValue(LAMP,false);
        if (state.hasProperty(CHANNELS)) state = state.setValue(CHANNELS, 0);
        for (var property : PROPERTY_BY_DIRECTION.values()) state = state.setValue(property, false);
        registerDefaultState(state);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(NORTH, EAST, SOUTH, WEST, UP, DOWN,LAMP); }
    public static final class Smart extends DigitalCableBlock {
        public Smart(Properties p, String color, float width) { super(p, color, width); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { super.createBlockStateDefinition(b); b.add(CHANNELS); }
    }
    public static boolean compatible(BlockState first, BlockState second) {
        return !(first.getBlock() instanceof DigitalCableBlock a) || !(second.getBlock() instanceof DigitalCableBlock b)
                || a.color.equals("fluix") || b.color.equals("fluix") || a.color.equals(b.color);
    }
    private boolean connects(BlockState state) { return (state.getBlock() instanceof DigitalCableBlock || state.getBlock() instanceof DigitalNodeBlock) && compatible(defaultBlockState(), state); }
    static boolean toggleOpen(net.minecraft.world.level.BlockGetter level,BlockPos pos,String kind){
        if(!(level instanceof net.minecraft.world.level.Level world))return false;
        return kind.equals("toggle_bus")?world.hasNeighborSignal(pos):kind.equals("inverted_toggle_bus")&&!world.hasNeighborSignal(pos);
    }
    static boolean openFace(net.minecraft.world.level.BlockGetter level,BlockPos pos,Direction face){
        if(level instanceof net.minecraft.world.level.Level world&&!world.isLoaded(pos))return false;
        if(!(level.getBlockEntity(pos) instanceof DigitalFacades.Covers covers))return true;
        var part=covers.parts.get(face);return part==null||toggleOpen(level,pos,part.kind());
    }
    static void refreshLamp(DigitalFacades.Covers covers){
        var level=covers.getLevel();if(level==null||level.isClientSide())return;
        boolean on=covers.parts.nodes().stream().anyMatch(n->n.kind().equals("light_p2p_tunnel")&&n.getBlockState().getValue(DigitalLinks.OUTPUT)>0);
        var state=covers.getBlockState();if(state.getValue(LAMP)!=on)level.setBlock(covers.getBlockPos(),state.setValue(LAMP,on),Block.UPDATE_ALL);
    }
    static void refresh(DigitalFacades.Covers covers){
        var level=covers.getLevel();if(level==null||!(covers.getBlockState().getBlock() instanceof DigitalCableBlock cable))return;
        var state=covers.getBlockState();var pos=covers.getBlockPos();
        for(var face:Direction.values())state=state.setValue(PROPERTY_BY_DIRECTION.get(face),covers.parts.get(face)!=null||openFace(level,pos,face)&&openFace(level,pos.relative(face),face.getOpposite())&&cable.connects(level.getBlockState(pos.relative(face))));
        if(!state.equals(covers.getBlockState()))level.setBlock(pos,state,Block.UPDATE_ALL);
        refreshLamp(covers);
    }
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DigitalFacades.Covers(pos, state); }
    @Override protected boolean isSignalSource(BlockState state){return true;}
    @Override protected int getSignal(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,Direction side){
        if(!(level.getBlockEntity(pos) instanceof DigitalFacades.Covers covers))return 0;
        var part=covers.parts.get(side.getOpposite());if(part==null)return 0;var signal=part.getBlockState();
        if(signal.hasProperty(DigitalControls.SIGNAL))return signal.getValue(DigitalControls.SIGNAL)?15:0;
        return part.kind().equals("redstone_p2p_tunnel")?signal.getValue(DigitalLinks.OUTPUT):0;
    }
    @Override protected int getDirectSignal(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,Direction side){return getSignal(state,level,pos,side);}
    @Override public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){
        return !level.isClientSide()&&type==DigitalFacades.TYPE?(world,pos,s,be)->((DigitalFacades.Covers)be).parts.tick():null;
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
        return level.getBlockEntity(pos) instanceof DigitalFacades.Covers covers&&covers.parts.interact(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit)?net.minecraft.world.InteractionResult.SUCCESS:net.minecraft.world.InteractionResult.PASS;
    }
    @Override protected net.minecraft.world.InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        if(stack.getItem() instanceof DigitalParts.PartItem||stack.getItem() instanceof DigitalEquipment.Wrench||stack.getItem() instanceof DigitalEquipment.Memory||stack.getItem() instanceof DigitalFacades.CoverItem||stack.getItem() instanceof WirelessTerminal||stack.getItem() instanceof DigitalCellCards.Card)return net.minecraft.world.InteractionResult.PASS;
        return level.getBlockEntity(pos) instanceof DigitalFacades.Covers covers&&covers.parts.interact(player,hand,hit)?net.minecraft.world.InteractionResult.SUCCESS:net.minecraft.world.InteractionResult.PASS;
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) { return DigitalFacades.shape(super.getShape(state, level, pos, context), level, pos, context, false); }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) { return DigitalFacades.shape(super.getShape(state, level, pos, context), level, pos, context, true); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) {
        var state = defaultBlockState();
        for (var d : Direction.values()) state = state.setValue(PROPERTY_BY_DIRECTION.get(d), openFace(c.getLevel(),c.getClickedPos().relative(d),d.getOpposite())&&connects(c.getLevel().getBlockState(c.getClickedPos().relative(d))));
        return state;
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos next, BlockState neighbor, RandomSource random) {
        boolean attached=level.getBlockEntity(pos) instanceof DigitalFacades.Covers covers&&covers.parts.get(direction)!=null;
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction),attached||openFace(level,pos,direction)&&openFace(level,next,direction.getOpposite())&&connects(neighbor));
    }
}
