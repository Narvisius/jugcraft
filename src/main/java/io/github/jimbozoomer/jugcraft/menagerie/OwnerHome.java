package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.building.OwnerBuildingCollection;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

/** Native storage and synchronized upholstery for the supplied Home furniture. */
public final class OwnerHome {
    public static BlockEntityType<Data> DATA;
    public static BlockEntityType<OwnerHomeStorage> STORAGE;
    private static final java.util.Map<String,net.minecraft.sounds.SoundEvent> SOUNDS=new java.util.HashMap<>();
    private OwnerHome() {}
    public static void finishRegistration() {
        for(String name:java.util.List.of("lamp_toggle","fireplace_loop","grandfather_clock_tick","grandfather_clock_midnight","light_water_flow","splash","sink_toggle","running_water")){
            var id=Jugcraft.id("homeworks_"+name);SOUNDS.put(name,Registry.register(BuiltInRegistries.SOUND_EVENT,id,net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id)));
        }
        var blocks = OwnerBuildingCollection.BLOCKS.values().stream().filter(b -> b instanceof OwnerHomeBlock).toList();
        DATA = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("owner_home"),
            FabricBlockEntityTypeBuilder.create(Data::new, blocks.stream().filter(b -> !((OwnerHomeBlock)b).storage()).toArray(Block[]::new)).build());
        STORAGE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("owner_home_storage"),
            FabricBlockEntityTypeBuilder.create(OwnerHomeStorage::new, blocks.stream().filter(b -> ((OwnerHomeBlock)b).storage()).toArray(Block[]::new)).build());
        OwnerHomeMenu.register();
    }
    public static Item item(String name) { return OwnerBuildingCollection.ITEMS.get("homeworks_" + name); }
    public static net.minecraft.sounds.SoundEvent sound(String name){return java.util.Objects.requireNonNull(SOUNDS.get(name));}
    public static final class Data extends BlockEntity {
        public int color = 0xFFFFFFFF;
        public ItemStack cushion = ItemStack.EMPTY;
        public float yaw, pitch;
        public int fluidUnits;
        public java.util.UUID owner;
        public long lastClockTime=-1;
        public Data(BlockPos pos, BlockState state) { super(DATA, pos, state); }
        public void changed() {
            setChanged(); if(level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        @Override protected void saveAdditional(ValueOutput out) {
            super.saveAdditional(out); out.putInt("Color", color); out.store("Cushion", ItemStack.OPTIONAL_CODEC, cushion);
            out.putFloat("Yaw", yaw); out.putFloat("Pitch", pitch);
            out.putInt("FluidUnits",fluidUnits);
            if(owner!=null)out.store("Owner",net.minecraft.core.UUIDUtil.CODEC,owner);
        }
        @Override protected void loadAdditional(ValueInput in) {
            super.loadAdditional(in); color = in.getIntOr("Color", 0xFFFFFFFF);
            cushion = in.read("Cushion", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
            yaw = in.getFloatOr("Yaw", 0); pitch = in.getFloatOr("Pitch", 0);
            fluidUnits = Math.clamp(in.getIntOr("FluidUnits",0),0,6);
            owner=in.read("Owner",net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        }
        @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider lookup) { return saveWithoutMetadata(lookup); }
        @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
        @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            if(level != null && !level.isClientSide() && !cushion.isEmpty()) { Block.popResource(level,pos,cushion); cushion=ItemStack.EMPTY; }
            super.preRemoveSideEffects(pos,state);
        }
    }
}
