package io.github.jimbozoomer.jugcraft.machine;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.CableBlock;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.materials.JugcraftRegistry;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Registers the electricity system: cables, machine blocks, their block entity, menus and energy lookup. */
public final class JugcraftMachines {
	public static Block COPPER_CABLE;
	public static Block SILVER_CABLE;
	public static Block ALUMINUM_CABLE;
	public static Block MACHINE_CASING;
	public static Block ARC_FURNACE_CASING;
	public static final Map<MachineKind, MachineBlock> MACHINES = new EnumMap<>(MachineKind.class);
	public static BlockEntityType<MachineBlockEntity> MACHINE_ENTITY;
	private static final Map<MachineKind, ExtendedMenuType<MachineMenu, BlockPos>> MENUS = new EnumMap<>(MachineKind.class);

	private JugcraftMachines() {
	}

	public static ExtendedMenuType<MachineMenu, BlockPos> menuType(MachineKind kind) {
		return MENUS.get(kind);
	}

	public static void register() {
		MachineRecipeTypes.register();
		MachineUpgrades.register();
		MachineRecipes.register();
		COPPER_CABLE = cable("copper_cable", MapColor.COLOR_ORANGE, CableBlock.COPPER_RATE);
		SILVER_CABLE = cable("silver_cable", MapColor.METAL, CableBlock.SILVER_RATE);
		ALUMINUM_CABLE = cable("aluminum_cable", MapColor.COLOR_LIGHT_BLUE, CableBlock.ALUMINUM_RATE);
		MACHINE_CASING = JugcraftRegistry.block("machine_casing", Blocks.IRON_BLOCK);
		ARC_FURNACE_CASING = JugcraftRegistry.block("arc_furnace_casing", Blocks.BRICKS);

		for (MachineKind kind : MachineKind.values()) {
			// Furnace properties include light emission while LIT, which machines share. Machine models are
			// detailed rather than full cubes (in either style), so they must not hide their neighbours' faces.
			BlockBehaviour.Properties props = properties(kind.id, BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE)).noOcclusion();
			// Pistons already refuse to move multi-block machines: every part is an entity block (even
			// dummies without a block entity), which pistons never push.
			MachineBlock machine = kind.isLarge() ? new LargeMachineBlock(props, kind) : new MachineBlock(props, kind);
			MACHINES.put(kind, (MachineBlock) block(kind.id, machine));

			ExtendedMenuType<MachineMenu, BlockPos> menu = new ExtendedMenuType<>(
					(containerId, inventory, pos) -> new MachineMenu(menuType(kind), kind, containerId, inventory),
					BlockPos.STREAM_CODEC.cast());
			MENUS.put(kind, Registry.register(BuiltInRegistries.MENU, Jugcraft.id(kind.id), menu));
		}

		MACHINE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("machine"),
				FabricBlockEntityTypeBuilder.create(MachineBlockEntity::new, MACHINES.values().toArray(Block[]::new)).build());
		// Registered per block, not per block entity, so the dummy parts of multi-block machines answer too.
		EnergyStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> {
			MachineBlockEntity machine = MachineBlock.machineAt(level, pos, state);
			// Machines with a power port only answer on that face, so cables only connect there.
			if (machine == null || !((MachineBlock) state.getBlock()).acceptsPower(state, side)) {
				return null;
			}
			return machine.energyFor(side);
		}, MACHINES.values().toArray(Block[]::new));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.accept(COPPER_CABLE);
			output.accept(SILVER_CABLE);
			output.accept(ALUMINUM_CABLE);
			output.accept(MACHINE_CASING);
			MACHINES.values().forEach(output::accept);
			output.accept(ARC_FURNACE_CASING);
		});
	}

	private static Block cable(String path, MapColor color, long rate) {
		return block(path, new CableBlock(properties(path,
				BlockBehaviour.Properties.of().mapColor(color).strength(0.5F).sound(SoundType.COPPER).noOcclusion()), rate));
	}

	private static BlockBehaviour.Properties properties(String path, BlockBehaviour.Properties properties) {
		return properties.setId(ResourceKey.create(Registries.BLOCK, Jugcraft.id(path)));
	}

	private static Block block(String path, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id(path), block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(path));
		Registry.register(BuiltInRegistries.ITEM, itemKey,
				new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}
}
