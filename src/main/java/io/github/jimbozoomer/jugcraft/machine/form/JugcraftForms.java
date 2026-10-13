package io.github.jimbozoomer.jugcraft.machine.form;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

/**
 * The shared industrial machine foundation (docs/features/industrial-machine-foundation.md): the formed-machine
 * screen, the registry of forms, and the controller type and port lookups each set of form blocks gets. Content
 * packages register their own form blocks and items, then hand the blocks to {@link #registerBlocks}.
 */
public final class JugcraftForms {
	public static ExtendedMenuType<FormMachineMenu, Identifier> MENU;
	private static final Map<Identifier, MachineForm> FORMS = new LinkedHashMap<>();

	private JugcraftForms() {
	}

	public static void register() {
		MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("machine_form"), new ExtendedMenuType<>(
				(containerId, inventory, id) -> new FormMachineMenu(containerId, inventory, formOrThrow(id)),
				Identifier.STREAM_CODEC.cast()));
	}

	/** The form with this id, or null. */
	public static @Nullable MachineForm form(Identifier id) {
		return FORMS.get(id);
	}

	private static MachineForm formOrThrow(Identifier id) {
		MachineForm form = FORMS.get(id);
		if (form == null) {
			throw new IllegalStateException("Unknown machine form " + id);
		}
		return form;
	}

	public static Collection<MachineForm> forms() {
		return Collections.unmodifiableCollection(FORMS.values());
	}

	/**
	 * Registers the controller block entity type {@code typeId} for already registered form blocks, and the lookups
	 * through which pipes, cables, conveyors and hoppers reach them: only at their forms' ports, and never a tool socket.
	 */
	public static BlockEntityType<FormMachineBlockEntity> registerBlocks(Identifier typeId, FormMachineBlock... blocks) {
		for (FormMachineBlock block : blocks) {
			if (FORMS.putIfAbsent(block.form().id(), block.form()) != null) {
				throw new IllegalStateException("Two blocks for machine form " + block.form().id());
			}
		}
		BlockEntityType<FormMachineBlockEntity> type = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, typeId,
				FabricBlockEntityTypeBuilder.create(FormMachineBlockEntity::new, blocks).build());
		for (FormMachineBlock block : blocks) {
			block.setEntityType(type);
		}
		FluidStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> {
			FormPort port = ((FormMachineBlock) state.getBlock()).portAt(state, side);
			if (port == null || !port.kind().fluid()) {
				return null;
			}
			FormMachineBlockEntity machine = FormMachineBlock.controllerAt(level, pos, state);
			return machine == null ? null : machine.fluidPort(port);
		}, blocks);
		ItemStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> {
			FormPort port = ((FormMachineBlock) state.getBlock()).portAt(state, side);
			if (port == null || !port.kind().item()) {
				return null;
			}
			FormMachineBlockEntity machine = FormMachineBlock.controllerAt(level, pos, state);
			return machine == null ? null : machine.itemPort(port);
		}, blocks);
		EnergyStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> {
			if (!((FormMachineBlock) state.getBlock()).acceptsPower(state, side)) {
				return null;
			}
			FormMachineBlockEntity machine = FormMachineBlock.controllerAt(level, pos, state);
			return machine == null ? null : machine.energy();
		}, blocks);
		return type;
	}
}
