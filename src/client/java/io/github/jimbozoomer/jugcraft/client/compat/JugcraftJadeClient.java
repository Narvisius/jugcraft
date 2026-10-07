package io.github.jimbozoomer.jugcraft.client.compat;

import io.github.jimbozoomer.jugcraft.compat.jade.JugcraftJadePlugin;
import io.github.jimbozoomer.jugcraft.compat.jade.MachineDataProvider;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.config.IPluginConfig;

/** Resolved only from Jade's client-registration callback. */
public final class JugcraftJadeClient implements JugcraftJadePlugin.ClientRegistration {
	@Override
	public void register(IWailaClientRegistration registration) {
		registration.registerBlockComponent(MachineTooltip.INSTANCE, MachineBlock.class);
	}

	private enum MachineTooltip implements IBlockComponentProvider {
		INSTANCE;

		@Override
		public Identifier getUid() {
			return MachineDataProvider.ID;
		}

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			accessor.getServerData().getCompound(MachineDataProvider.DATA_KEY).ifPresent(data -> {
				long capacity = data.getLongOr("capacity", 0);
				if (capacity > 0) {
					tooltip.add(Component.translatableWithFallback("tooltip.jugcraft.machine_energy",
							"Energy: %s / %s JE", data.getLongOr("energy", 0), capacity));
				}
				int duration = data.getIntOr("duration", 0);
				if (duration > 0) {
					int percent = (int) Math.clamp(100L * data.getIntOr("progress", 0) / duration, 0, 100);
					tooltip.add(Component.translatableWithFallback("tooltip.jugcraft.machine_progress",
							"Processing: %s%%", percent));
				}
			});
		}
	}
}
