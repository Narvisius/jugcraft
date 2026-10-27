package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;

/** Explicit inventory migration whitelist. No generic mod-machine NBT or entity identity copying. */
final class SpatialInventories {
    static final int LIMIT=64;
    record Entry(int index,CompoundTag data){
        static final Codec<Entry> CODEC=RecordCodecBuilder.create(i->i.group(
                Codec.intRange(0,4095).fieldOf("index").forGetter(Entry::index),
                CompoundTag.CODEC.fieldOf("data").forGetter(Entry::data)).apply(i,Entry::new));
    }
    static final Codec<List<Entry>> CODEC=Entry.CODEC.listOf(0,LIMIT);
    private SpatialInventories(){}
    private static final String KIND="jugcraft_spatial_container";
    private static boolean chest(BlockState state){return state.is(Blocks.CHEST)||state.is(Blocks.TRAPPED_CHEST);}
    private static boolean containerBlock(BlockState state){return chest(state)|| state.is(Blocks.BARREL)||state.is(Blocks.HOPPER)||state.is(Blocks.DISPENSER)||state.is(Blocks.DROPPER)||processing(state)||shulker(state);}
    private static boolean shulker(BlockState state){return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("minecraft")&&state.getBlock() instanceof ShulkerBoxBlock;}
    private static boolean furnace(BlockState state){
        return state.is(Blocks.FURNACE)||BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals("jugcraft:jimta_iron_furnace");
    }
    private static boolean processing(BlockState state){return furnace(state)||state.is(Blocks.SMOKER)||state.is(Blocks.BLAST_FURNACE)||state.is(Blocks.BREWING_STAND);}
    private static boolean lathe(BlockState state){return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals("jugcraft:jimta_lathe");}
    static boolean supported(BlockState state){
        if(lathe(state)||chest(state)||state.is(Blocks.BARREL)||state.is(Blocks.HOPPER)||processing(state)||shulker(state))return true;
        return (state.is(Blocks.DISPENSER)||state.is(Blocks.DROPPER))&&!state.getValue(DispenserBlock.TRIGGERED);
    }
    private static boolean correctType(BlockState state,BlockEntity node){
        return lathe(state)?node.getType()==io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialLathe.TYPE:state.is(Blocks.CHEST)?node.getType()==BlockEntityTypes.CHEST:state.is(Blocks.TRAPPED_CHEST)?node.getType()==BlockEntityTypes.TRAPPED_CHEST:state.is(Blocks.BARREL)?node.getType()==BlockEntityTypes.BARREL:
                state.is(Blocks.HOPPER)?node.getType()==BlockEntityTypes.HOPPER:
                state.is(Blocks.DISPENSER)?node.getType()==BlockEntityTypes.DISPENSER:
                state.is(Blocks.DROPPER)?node.getType()==BlockEntityTypes.DROPPER:
                furnace(state)?node.getType()==BlockEntityTypes.FURNACE:
                state.is(Blocks.SMOKER)?node.getType()==BlockEntityTypes.SMOKER:
                state.is(Blocks.BLAST_FURNACE)?node.getType()==BlockEntityTypes.BLAST_FURNACE:
                state.is(Blocks.BREWING_STAND)?node.getType()==BlockEntityTypes.BREWING_STAND:
                shulker(state)&&node.getType()==BlockEntityTypes.SHULKER_BOX;
    }
    static List<Entry> capture(ServerLevel level,List<BlockPos> positions){
        validateChestConnections(level,positions,positions.stream().map(level::getBlockState).toList());
        var result=new ArrayList<Entry>();
        for(int i=0;i<positions.size();i++)if(supported(level.getBlockState(positions.get(i)))){
            if(result.size()==LIMIT)throw new IllegalArgumentException("At most 64 supported containers per spatial exchange");
            var pos=positions.get(i);var state=level.getBlockState(pos);
            if(!(level.getBlockEntity(pos) instanceof BaseContainerBlockEntity container)||!correctType(state,container))throw new IllegalArgumentException("Missing or mismatched spatial container data");
            if((state.is(Blocks.DROPPER)||state.is(Blocks.DISPENSER))&&level.getBlockTicks().hasScheduledTick(pos,state.getBlock()))throw new IllegalArgumentException("Wait for dispenser/dropper pulses to finish before exchanging");
            if(container instanceof ShulkerBoxBlockEntity shulker&&!shulker.isClosed())throw new IllegalArgumentException("Close shulker boxes and let their lids finish closing before exchanging");
            // saveWithoutMetadata preserves contents, name, lock and ungenerated loot without opening it.
            var data=container.saveWithoutMetadata(level.registryAccess());data.putString(KIND,BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            result.add(new Entry(i,data));
        }
        return List.copyOf(result);
    }
    static void validate(List<BlockState> states,List<Entry> entries){
        if(entries.size()>LIMIT)throw new IllegalArgumentException("Spatial inventory limit exceeded");
        var seen=new HashSet<Integer>();
        for(var e:entries){
            if(e.index<0||e.index>=states.size()||!supported(states.get(e.index))||!seen.add(e.index))throw new IllegalArgumentException("Invalid or duplicate spatial inventory position");
            var state=states.get(e.index);String kind=e.data.getStringOr(KIND,"");
            // Pre-extension journals contained barrels only. Never interpret an old payload as another inventory size.
            if(kind.isEmpty()?!state.is(Blocks.BARREL):!kind.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()))throw new IllegalArgumentException("Spatial container payload type does not match its block");
        }
        for(int i=0;i<states.size();i++)if(supported(states.get(i))&&!seen.contains(i))throw new IllegalArgumentException("Stored container inventory is missing");
    }
    static void detach(ServerLevel level,List<BlockPos> positions){
        var containers=Collections.newSetFromMap(new IdentityHashMap<Container,Boolean>());
        for(var pos:positions)if(supported(level.getBlockState(pos))&&level.getBlockEntity(pos) instanceof BaseContainerBlockEntity container)containers.add(container);
        // Close menus before clearing contents, so queued client clicks cannot use the outgoing inventory.
        for(var player:level.players())if(player.containerMenu.slots.stream().anyMatch(slot->containers.contains(slot.container)||slot.container instanceof net.minecraft.world.CompoundContainer pair&&containers.stream().anyMatch(pair::contains)))player.closeContainer();
        for(var pos:positions)if(supported(level.getBlockState(pos))&&level.getBlockEntity(pos) instanceof BaseContainerBlockEntity container){
            // Old menus/cached storage cannot retain a second usable copy. Never run destruction drops.
            // Remove loot metadata first: clearContent must not unpack an ungenerated loot table.
            if(container instanceof RandomizableContainerBlockEntity random)random.setLootTable(null);container.clearContent();
            if(container instanceof io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialLathe.Node lathe)lathe.kinetic=0;
            level.removeBlockEntity(pos);
        }
    }
    /** Construct and decode every incoming inventory before detaching any outgoing world state. */
    static List<BaseContainerBlockEntity> prepare(ServerLevel level,List<BlockPos> positions,List<BlockState> states,List<Entry> entries){
        if(positions.size()!=states.size())throw new IllegalArgumentException("Spatial position/state count mismatch");
        validate(states,entries);validateChestConnections(level,positions,states);
        var prepared=new ArrayList<BaseContainerBlockEntity>();
        for(var e:entries){
            var pos=positions.get(e.index);var state=states.get(e.index);
            if(!supported(state)||!(state.getBlock() instanceof EntityBlock block))throw new IllegalArgumentException("Unsupported restored container");
            var created=block.newBlockEntity(pos,state);
            if(!(created instanceof BaseContainerBlockEntity container)||!correctType(state,container))throw new IllegalArgumentException("Restored container has the wrong type");
            container.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),e.data));
            prepared.add(container);
        }
        return List.copyOf(prepared);
    }
    private static void validateChestConnections(ServerLevel level,List<BlockPos> positions,List<BlockState> states){
        var indices=new HashMap<BlockPos,Integer>();for(int i=0;i<positions.size();i++)indices.put(positions.get(i),i);
        for(int i=0;i<states.size();i++){
            var state=states.get(i);if(!chest(state))continue;var pos=positions.get(i);
            var type=state.getValue(ChestBlock.TYPE);
            if(type!=net.minecraft.world.level.block.state.properties.ChestType.SINGLE){
                var partner=pos.relative(ChestBlock.getConnectedDirection(state));var index=indices.get(partner);
                if(index==null)throw new IllegalArgumentException("Both halves of a double chest must be inside the spatial region");
                var other=states.get(index);
                if(!other.is(state.getBlock())||other.getValue(ChestBlock.TYPE)!=type.getOpposite()||other.getValue(ChestBlock.FACING)!=state.getValue(ChestBlock.FACING)||!partner.relative(ChestBlock.getConnectedDirection(other)).equals(pos))throw new IllegalArgumentException("Invalid double chest pairing");
            }
            for(var side:net.minecraft.core.Direction.Plane.HORIZONTAL){var outside=pos.relative(side);if(indices.containsKey(outside))continue;
                if(!level.hasChunkAt(outside))throw new IllegalArgumentException("Chest boundary must be loaded before exchange");
                if(level.getBlockState(outside).is(state.getBlock()))throw new IllegalArgumentException("Separate chests outside the spatial boundary before exchange");
            }
        }
    }
    static void install(ServerLevel level,List<BaseContainerBlockEntity> prepared){
        for(var container:prepared){level.setBlockEntity(container);container.setChanged();}
    }
    static void restore(ServerLevel level,List<BlockPos> positions,List<Entry> entries){
        install(level,prepare(level,positions,positions.stream().map(level::getBlockState).toList(),entries));
    }
}
