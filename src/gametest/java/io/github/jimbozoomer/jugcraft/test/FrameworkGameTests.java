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
    public void researchPacketKeepsSendTimeSnapshot(GameTestHelper helper) {
        var source = new com.klikli_dev.modonomicon.research.state.PlayerResearchState();
        var fact = io.github.jimbozoomer.jugcraft.Jugcraft.id("snapshot_fact");
        var node = io.github.jimbozoomer.jugcraft.Jugcraft.id("snapshot_node");
        source.grantFact(fact);
        source.unlockNode(node);
        source.setValue(fact, 7);
        source.setNodeStageIndex(node, 3);
        var packet = new com.klikli_dev.modonomicon.networking.SyncResearchStateMessage(source);
        source.reset();
        // Mutations after queueing must not reach Netty's later serialization.
        for (int i = 0; i < 256; i++) source.grantFact(io.github.jimbozoomer.jugcraft.Jugcraft.id("later_" + i));
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            com.klikli_dev.modonomicon.networking.SyncResearchStateMessage.STREAM_CODEC.encode(buffer, packet);
            var decoded = com.klikli_dev.modonomicon.networking.SyncResearchStateMessage.STREAM_CODEC.decode(buffer).state;
            helper.assertTrue(decoded.factIds().equals(java.util.Set.of(fact)), "Queued facts are independent of later mutations");
            helper.assertTrue(decoded.unlockedNodeIds().equals(java.util.Set.of(node)), "Queued unlocks retain send-time state");
            helper.assertTrue(decoded.getValue(fact) == 7 && decoded.getNodeStageIndex(node) == 3, "Values and stages survive encoding unchanged");
            helper.assertTrue(source.factIds().size() == 256 && !source.hasFact(fact), "Serialization must not change live research");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

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
