package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.compat.jade.MachineDataProvider;
import io.github.jimbozoomer.jugcraft.biome.BiomeBootstrapScope;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;

/** Exercises the optional overlay against a real machine without changing its stored state. */
public class FrameworkGameTests {
	@GameTest
	public void viewerBootstrapKeepsNormalWorldGenerationEnabled(GameTestHelper helper) {
		helper.assertFalse(BiomeBootstrapScope.isVanillaOnly(), "Normal world generation starts enabled");
		var fallback = BiomeBootstrapScope.vanillaOnly(() -> net.minecraft.data.registries.VanillaRegistries.createReloadableLookup(
				net.minecraft.data.registries.VanillaRegistries.createWorldLookup()));
		helper.assertTrue(fallback.lookupOrThrow(net.minecraft.core.registries.Registries.BIOME)
				.get(net.minecraft.world.level.biome.Biomes.PLAINS).isPresent(), "Vanilla fallback must build successfully");
		try {
			BiomeBootstrapScope.vanillaOnly(() -> {
				BiomeBootstrapScope.vanillaOnly(() -> true);
				helper.assertTrue(BiomeBootstrapScope.isVanillaOnly(), "Nested bootstrap preserves its enclosing scope");
				throw new IllegalStateException("test bootstrap failure");
			});
		} catch (IllegalStateException expected) {
			helper.assertTrue("test bootstrap failure".equals(expected.getMessage()), "Unexpected bootstrap error");
		}
		helper.assertFalse(BiomeBootstrapScope.isVanillaOnly(), "Failure must restore normal world generation");
		helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME)
				.get(io.github.jimbozoomer.jugcraft.world.AlpineSpawn.BIOME).isPresent(), "The real world retains custom biomes");
		helper.assertTrue(net.fabricmc.fabric.api.biome.v1.NetherBiomes.canGenerateInNether(
				net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
						io.github.jimbozoomer.jugcraft.Jugcraft.id("ashfall_wastes"))), "Custom Nether placement stays enabled");
		helper.succeed();
	}

	@GameTest
	public void jadeSnapshotPreservesMachineState(GameTestHelper helper) {
		if (!FabricLoader.getInstance().isModLoaded("jade")) {
			helper.succeed(); // Also proves this test class can load on the optional-absent path.
			return;
		}
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, JugcraftMachines.MACHINES.get(MachineKind.BATTERY_BOX).defaultBlockState());
		MachineBlockEntity machine = helper.getBlockEntity(pos, MachineBlockEntity.class);
		SimpleEnergyStorage energy = (SimpleEnergyStorage) machine.energyFor(null);
		energy.setAmount(350_000);
		int progress = machine.processingProgress();
		int duration = machine.processingDuration();
		CompoundTag first = MachineDataProvider.snapshot(machine);
		CompoundTag second = MachineDataProvider.snapshot(machine);
		helper.assertTrue(first.getLongOr("energy", -1) == 350_000,
				"Inspection must preserve energy values larger than a short");
		helper.assertTrue(first.getLongOr("capacity", -1) == 400_000, "Inspection must report the real capacity");
		helper.assertTrue(first.equals(second), "Repeated inspections must be stable");
		helper.assertTrue(energy.getAmount() == 350_000 && machine.processingProgress() == progress
				&& machine.processingDuration() == duration, "Inspection must not change machine state");
		helper.succeed();
	}
}
