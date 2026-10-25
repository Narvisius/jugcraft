package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.shapes.*;

/** Six independent real cable covers/anchors; cosmetic samples never copy block-entity contents or behavior. */
public final class DigitalFacades {
    public static BlockEntityType<Covers> TYPE;
    public static DataComponentType<BlockState> MATERIAL;
    private DigitalFacades() {}
    public static void register() {
        MATERIAL = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("jimta_facade_material"), DataComponentType.<BlockState>builder().persistent(BlockState.CODEC).networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(BlockState.CODEC)).build());
        for (String name : List.of("facade", "cable_anchor")) {
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_" + name));
            DigitalStorage.ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, new CoverItem(new Item.Properties().setId(key), name.equals("facade"))));
        }
        TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("jimta_cable_covers"), FabricBlockEntityTypeBuilder.create(Covers::new, DigitalStorage.BLOCKS.values().stream().filter(b -> b instanceof DigitalCableBlock).toArray(Block[]::new)).build());
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Jugcraft.id("jimta_facade"), DigitalFacadeRecipe.FACADE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Jugcraft.id("jimta_cable_anchors"), DigitalFacadeRecipe.ANCHORS);
    }
    public static boolean valid(BlockState state) { return !state.isAir() && !state.hasBlockEntity() && state.getFluidState().isEmpty() && state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) && state.getBlock().asItem() != Items.AIR; }
    public static ItemStack facade(BlockState state) { var item = new ItemStack(DigitalStorage.ITEMS.get("facade")); item.set(MATERIAL, state); return item; }
    public static final class CoverItem extends Item {
        final boolean cover;
        CoverItem(Properties p, boolean cover) { super(p); this.cover = cover; }
        @Override public InteractionResult useOn(UseOnContext c) {
            if (!(c.getPlayer() instanceof ServerPlayer p) || !(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof Covers be)) return InteractionResult.PASS;
            if (p.isSpectator() || !p.isAlive() || !Authority.mayChange(p.level(), p, be.getBlockPos())) return InteractionResult.FAIL;
            var side = c.getClickedFace(); var sample = c.getItemInHand().get(MATERIAL);
            if (cover) {
                if (sample == null || !valid(sample) || !be.material(side).isAir()||be.parts.get(side)!=null) { p.sendOverlayMessage(Component.literal("Craft a facade from four anchors around a solid block; use an uncovered cable face without an attachment.")); return InteractionResult.FAIL; }
                be.faces.set(side.ordinal(), sample);
            } else {
                if (be.anchored(side) || be.parts.get(side)!=null || be.getBlockState().getValue(DigitalCableBlock.PROPERTY_BY_DIRECTION.get(side))) return InteractionResult.FAIL;
                be.anchors |= 1 << side.ordinal();
            }
            c.getItemInHand().shrink(1); be.changed(); return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, net.minecraft.world.item.component.TooltipDisplay d, java.util.function.Consumer<Component> out, TooltipFlag f) {
            if (cover && s.has(MATERIAL)) out.accept(s.get(MATERIAL).getBlock().getName());
            out.accept(Component.literal(cover ? "Cable face cover · hold Network Tool to see through; sneak-wrench removes a face." : "Cable support · use on a cable face; sneak-wrench removes."));
        }
    }
    public static boolean remove(UseOnContext c) {
        if (!(c.getPlayer() instanceof ServerPlayer p) || !p.isShiftKeyDown() || !(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof Covers be) || !Authority.mayChange(p.level(), p, c.getClickedPos())) return false;
        var side = c.getClickedFace(); ItemStack drop;
        if (!be.material(side).isAir()) { drop = facade(be.material(side)); be.faces.set(side.ordinal(), Blocks.AIR.defaultBlockState()); }
        else if (be.anchored(side)) { drop = new ItemStack(DigitalStorage.ITEMS.get("cable_anchor")); be.anchors &= ~(1 << side.ordinal()); }
        else return be.parts.detach(c);
        be.changed(); p.getInventory().placeItemBackInInventory(drop, net.minecraft.util.Prediction.SERVER_ONLY); return true;
    }
    public static VoxelShape shape(VoxelShape cable, BlockGetter world, BlockPos pos, CollisionContext context, boolean collision) {
        if (!(world.getBlockEntity(pos) instanceof Covers be)) return cable;
        for (Direction side : Direction.values()) {
            if (!be.material(side).isAir() && (collision || !context.isHoldingItem(DigitalStorage.ITEMS.get("network_tool")))) cable = Shapes.or(cable, plate(side));
            if (be.anchored(side)) cable = Shapes.or(cable, switch (side) { case NORTH -> Block.box(7,7,0,9,9,6); case SOUTH -> Block.box(7,7,10,9,9,16); case WEST -> Block.box(0,7,7,6,9,9); case EAST -> Block.box(10,7,7,16,9,9); case DOWN -> Block.box(7,0,7,9,6,9); case UP -> Block.box(7,10,7,9,16,9); });
            if(be.parts.get(side)!=null)cable=Shapes.or(cable,attachment(be.parts.get(side),side));
        }
        return cable;
    }
    private static VoxelShape plate(Direction side) { return switch(side) { case NORTH -> Block.box(0,0,0,16,16,2); case SOUTH -> Block.box(0,0,14,16,16,16); case WEST -> Block.box(0,0,0,2,16,16); case EAST -> Block.box(14,0,0,16,16,16); case DOWN -> Block.box(0,0,0,16,2,16); case UP -> Block.box(0,14,0,16,16,16); }; }
    static VoxelShape attachment(DigitalNode node,Direction side){
        boolean narrowBus=node.kind().endsWith("import_bus")||node.kind().endsWith("export_bus");
        int inset=node.kind().endsWith("_plane")?1:DigitalControls.controls(node.kind())?7:narrowBus?4:2;
        int depth=node.kind().endsWith("_plane")?2:DigitalControls.controls(node.kind())||narrowBus?5:4;
        int max=16-inset;
        return switch(side){case NORTH->Block.box(inset,inset,0,max,max,depth);case SOUTH->Block.box(inset,inset,16-depth,max,max,16);case WEST->Block.box(0,inset,inset,depth,max,max);case EAST->Block.box(16-depth,inset,inset,16,max,max);case DOWN->Block.box(inset,0,inset,max,depth,max);case UP->Block.box(inset,16-depth,inset,max,16,max);};
    }
    public static final class Covers extends BlockEntity {
        public final DigitalParts parts=new DigitalParts(this);
        private final List<BlockState> faces = new ArrayList<>(Collections.nCopies(6, Blocks.AIR.defaultBlockState()));
        private int anchors;
        public Covers(BlockPos pos, BlockState state) { super(TYPE, pos, state); }
        public BlockState material(Direction side) { return faces.get(side.ordinal()); }
        public boolean anchored(Direction side) { return (anchors & 1 << side.ordinal()) != 0; }
        void changed() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS); }
        @Override public void setLevel(Level level){super.setLevel(level);parts.levelChanged();}
        @Override public void setRemoved(){parts.removed();super.setRemoved();}
        @Override public void clearRemoved(){super.clearRemoved();parts.revived();}
        @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); out.store("covers", BlockState.CODEC.listOf(0,6), faces); out.putInt("anchors", anchors);parts.save(out,false); }
        @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); var saved = in.read("covers", BlockState.CODEC.listOf(0,6)).orElse(List.of()); for (int i = 0; i < 6; i++) faces.set(i, i < saved.size() && valid(saved.get(i)) ? saved.get(i) : Blocks.AIR.defaultBlockState()); anchors = in.getIntOr("anchors", 0) & 63;parts.load(in); }
        @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) { if (level != null && !level.isClientSide()) { parts.dropAll();for (Direction side : Direction.values()) { if (!material(side).isAir()) Block.popResource(level, pos, facade(material(side))); if (anchored(side)) Block.popResource(level, pos, new ItemStack(DigitalStorage.ITEMS.get("cable_anchor"))); } Collections.fill(faces, Blocks.AIR.defaultBlockState()); anchors = 0; } }
        @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
        @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
            var out=TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,lookup);out.putBoolean("render_only",true);
            out.store("covers",BlockState.CODEC.listOf(0,6),faces);out.putInt("anchors",anchors);parts.save(out,true);return out.buildResult();
        }
    }
}
