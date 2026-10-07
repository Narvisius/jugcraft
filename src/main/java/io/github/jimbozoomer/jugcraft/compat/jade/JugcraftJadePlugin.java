package io.github.jimbozoomer.jugcraft.compat.jade;

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
