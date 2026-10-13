package io.github.jimbozoomer.jugcraft.compat.jade;

import io.github.jimbozoomer.jugcraft.concordance.CircleAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.LampwrightBenchBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.LumenSconceBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.GleanerBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.HabitatGaugeBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.MulchMawBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantHeartBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.reliquary.ReliquaryShrineBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spire.SpireHeartBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.sky.ObservatoryBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import net.fabricmc.loader.api.FabricLoader;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;

/** Loaded exclusively by Jade's optional entrypoint, never by Jugcraft's core initializer. */
public final class JugcraftJadePlugin implements IWailaPlugin {
	@Override
	public void register(IWailaCommonRegistration registration) {
		registration.registerBlockDataProvider(MachineDataProvider.INSTANCE, MachineBlockEntity.class);
		registration.registerBlockDataProvider(ConcordanceDataProvider.BENCH, LampwrightBenchBlockEntity.class);
		registration.registerBlockDataProvider(ConcordanceDataProvider.SCONCE, LumenSconceBlockEntity.class);
		registration.registerBlockDataProvider(ConcordanceDataProvider.ANCHOR, CircleAnchorBlockEntity.class);
		registration.registerBlockDataProvider(ConcordanceDataProvider.PYLON, LeyPylonBlockEntity.class);
		registration.registerBlockDataProvider(ConcordanceDataProvider.CRUCIBLE, CrucibleBlockEntity.class);
		registration.registerBlockDataProvider(ConcordanceDataProvider.BED, VerdantBedBlockEntity.class);
		registration.registerBlockDataProvider(ConcordanceDataProvider.HEART, VerdantHeartBlockEntity.class);
		registration.registerBlockDataProvider(ConcordanceDataProvider.MAW, MulchMawBlockEntity.class);
		registration.registerBlockDataProvider(ConcordanceDataProvider.GAUGE, HabitatGaugeBlockEntity.class);
		registration.registerBlockDataProvider(ConcordanceDataProvider.GLEANER, GleanerBlockEntity.class);
		registration.registerBlockDataProvider(ConcordanceDataProvider.OBSERVATORY, ObservatoryBlockEntity.class);
		registration.registerEntityDataProvider(WorkerDataProvider.INSTANCE, WorkerEntity.class);
		registration.registerBlockDataProvider(CourierDataProvider.INSTANCE, CourierPostBlockEntity.class);
		registration.registerBlockDataProvider(ShrineDataProvider.INSTANCE, ReliquaryShrineBlockEntity.class);
		registration.registerBlockDataProvider(SpireDataProvider.INSTANCE, SpireHeartBlockEntity.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		// Jade invokes this only on clients. Keep the actual renderer in the client source set.
		for (ClientRegistration plugin : FabricLoader.getInstance()
				.getEntrypoints("jugcraft:jade_client", ClientRegistration.class)) {
			plugin.register(registration);
		}
	}

	public interface ClientRegistration {
		void register(IWailaClientRegistration registration);
	}
}
