package io.github.jimbozoomer.jugcraft.client.compat;

import io.github.jimbozoomer.jugcraft.compat.jade.ConcordanceDataProvider;
import io.github.jimbozoomer.jugcraft.compat.jade.JugcraftJadePlugin;
import io.github.jimbozoomer.jugcraft.compat.jade.MachineDataProvider;
import io.github.jimbozoomer.jugcraft.concordance.KindledLanternItem;
import io.github.jimbozoomer.jugcraft.concordance.CircleAnchorBlock;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlock;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Mixture;
import io.github.jimbozoomer.jugcraft.concordance.LampwrightBenchBlock;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlock;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.LumenSconceBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.GleanerBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.HabitatGaugeBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.MulchMawBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.OrganismCropBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantHeartBlock;
import io.github.jimbozoomer.jugcraft.concordance.sky.ObservatoryBlock;
import io.github.jimbozoomer.jugcraft.concordance.sky.Sky;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerEntity;
import io.github.jimbozoomer.jugcraft.compat.jade.WorkerDataProvider;
import io.github.jimbozoomer.jugcraft.compat.jade.CourierDataProvider;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlock;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Event;
import io.github.jimbozoomer.jugcraft.compat.jade.ShrineDataProvider;
import io.github.jimbozoomer.jugcraft.concordance.relic.Context;
import io.github.jimbozoomer.jugcraft.concordance.reliquary.Reliquary;
import io.github.jimbozoomer.jugcraft.concordance.reliquary.ReliquaryShrineBlock;
import io.github.jimbozoomer.jugcraft.compat.jade.SpireDataProvider;
import io.github.jimbozoomer.jugcraft.concordance.spire.SpireHeartBlock;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.world.level.block.state.BlockState;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.config.IPluginConfig;

/** Resolved only from Jade's client-registration callback. */
public final class JugcraftJadeClient implements JugcraftJadePlugin.ClientRegistration {
	@Override
	public void register(IWailaClientRegistration registration) {
		registration.registerBlockComponent(MachineTooltip.INSTANCE, MachineBlock.class);
		registration.registerBlockComponent(ConcordanceTooltip.BENCH, LampwrightBenchBlock.class);
		registration.registerBlockComponent(ConcordanceTooltip.SCONCE, LumenSconceBlock.class);
		registration.registerBlockComponent(ConcordanceTooltip.ANCHOR, CircleAnchorBlock.class);
		registration.registerBlockComponent(ConcordanceTooltip.PYLON, LeyPylonBlock.class);
		registration.registerBlockComponent(ConcordanceTooltip.CRUCIBLE, CrucibleBlock.class);
		registration.registerBlockComponent(ConcordanceTooltip.BED, VerdantBedBlock.class);
		registration.registerBlockComponent(ConcordanceTooltip.HEART, VerdantHeartBlock.class);
		registration.registerBlockComponent(ConcordanceTooltip.MAW, MulchMawBlock.class);
		registration.registerBlockComponent(ConcordanceTooltip.GAUGE, HabitatGaugeBlock.class);
		registration.registerBlockComponent(ConcordanceTooltip.GLEANER, GleanerBlock.class);
		registration.registerBlockComponent(ConcordanceTooltip.OBSERVATORY, ObservatoryBlock.class);
		registration.registerEntityComponent(WorkerTooltip.INSTANCE, WorkerEntity.class);
		registration.registerBlockComponent(CourierTooltip.INSTANCE, CourierPostBlock.class);
		registration.registerBlockComponent(ShrineTooltip.INSTANCE, ReliquaryShrineBlock.class);
		registration.registerBlockComponent(SpireTooltip.INSTANCE, SpireHeartBlock.class);
		registration.registerBlockComponent(CropTooltip.INSTANCE, OrganismCropBlock.class);
		// Nuclear equipment's invisible frame blocks (round 25b; Justin: the switchgear was "just labeled nuclear
		// equipment"): named and shown by what pick-block gives, the piece's own item, not as the frame block. Looked up
		// by name, so a Jade without it is only logged.
		try {
			IWailaClientRegistration.class.getMethod("usePickedResult", net.minecraft.world.level.block.Block.class)
					.invoke(registration, io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear.FRAME);
		} catch (ReflectiveOperationException | RuntimeException e) {
			io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.warn("Jade cannot name nuclear equipment by its picked item: {}", e.toString());
		}
	}

	/** A growth word as the server noted it: a pace, dormant, or waiting for a reading. */
	private static Component growthText(String growth) {
		return switch (growth) {
			case "dormant" -> Component.translatable("tooltip.jugcraft.concordance.jade.dormant");
			case "waiting" -> Component.translatable("compose.jugcraft.ecology.status.waiting");
			default -> Component.translatable("compose.jugcraft.ecology.growth." + growth);
		};
	}

	/** A familiar, spirit or construct (roadmap step 17): its status, then its own model's numbers. */
	private enum WorkerTooltip implements IEntityComponentProvider {
		INSTANCE;

		@Override
		public Identifier getUid() {
			return WorkerDataProvider.ID;
		}

		@Override
		public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
			accessor.getServerData().getCompound(WorkerDataProvider.KEY).ifPresent(data -> {
				tooltip.add(Component.translatable("compose.jugcraft.worker.status." + data.getStringOr("status", "idle")));
				int bondMax = data.getIntOr("bond_max", 0);
				if (bondMax > 0) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.bond", data.getIntOr("bond", 0), bondMax));
				}
				int integrityMax = data.getIntOr("integrity_max", 0);
				if (integrityMax > 0) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.body", data.getIntOr("integrity", 0), integrityMax,
							data.getLongOr("energy", 0L), data.getIntOr("energy_max", 0)));
				}
				int carried = data.getIntOr("carried", 0);
				if (carried > 0) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.carried", carried));
				}
			});
		}
	}

	/** A Courier Post (roadmap step 18): its open and in-transit requests and its latest step. */
	private enum CourierTooltip implements IBlockComponentProvider {
		INSTANCE;

		@Override
		public Identifier getUid() {
			return CourierDataProvider.ID;
		}

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			accessor.getServerData().getCompound(CourierDataProvider.KEY).ifPresent(data -> {
				tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.courier", data.getIntOr("open", 0), data.getIntOr("moving", 0)));
				String kind = data.getStringOr("kind", "");
				if (!kind.isEmpty()) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.courier_last", Couriers.event(new Event(0L,
							data.getLongOr("request", 0L), kind, data.getIntOr("amount", 0), data.getStringOr("note", "")))));
				}
			});
		}
	}

	/**
	 * A Reliquary Shrine (roadmap step 20): the relic it holds and its charge, and whether it works or exactly why not
	 * (the reasons that name where a relic works list its contexts).
	 */
	private enum ShrineTooltip implements IBlockComponentProvider {
		INSTANCE;

		@Override
		public Identifier getUid() {
			return ShrineDataProvider.ID;
		}

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			accessor.getServerData().getCompound(ShrineDataProvider.KEY).ifPresent(data -> {
				String relic = data.getStringOr("relic", "");
				if (relic.isEmpty()) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.shrine_empty"));
					return;
				}
				String status = data.getStringOr("status", "");
				Component state;
				if (status.equals("working")) {
					state = Component.translatable("tooltip.jugcraft.concordance.jade.shrine_working",
							Component.translatable("compose.jugcraft.relic.mode." + data.getStringOr("mode", "")));
				} else if (status.equals("wrong_context") || status.equals("cannot_install")) {
					java.util.List<Context> contexts = new java.util.ArrayList<>();
					for (String id : data.getStringOr("contexts", "").split(",")) {
						Context context = Context.fromId(id);
						if (context != null) {
							contexts.add(context);
						}
					}
					state = Component.translatable("message.jugcraft.concordance.relic.cannot",
							Component.translatable("compose.jugcraft.relic.reason." + status, Reliquary.contexts(contexts)));
				} else {
					state = Component.translatable("message.jugcraft.concordance.relic.cannot",
							Component.translatable("compose.jugcraft.relic.reason." + status));
				}
				tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.shrine", Component.translatable(relic), state));
				tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.shrine_charge", data.getIntOr("charge", 0),
						data.getIntOr("capacity", 0)));
			});
		}
	}

	/**
	 * A Spire Heart (roadmap step 25): its configuration, its phase or that it is raised, whether its field works or why it
	 * rests, and its store against a day's upkeep.
	 */
	private enum SpireTooltip implements IBlockComponentProvider {
		INSTANCE;

		@Override
		public Identifier getUid() {
			return SpireDataProvider.ID;
		}

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			accessor.getServerData().getCompound(SpireDataProvider.KEY).ifPresent(data -> {
				String configuration = data.getStringOr("configuration", "");
				if (configuration.isEmpty()) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.spire_unfounded"));
					return;
				}
				tooltip.add(Component.translatable("compose.jugcraft.spire.configuration." + configuration.substring(configuration.indexOf(':') + 1)));
				String phase = data.getStringOr("phase_id", "");
				tooltip.add(phase.isEmpty() ? Component.translatable("tooltip.jugcraft.concordance.jade.spire_raised")
						: Component.translatable("tooltip.jugcraft.concordance.jade.spire_phase", data.getIntOr("phase", 0) + 1, data.getIntOr("phases", 0),
								Component.translatable("compose.jugcraft.spire.phase." + phase)));
				String status = data.getStringOr("status", "");
				tooltip.add(status.equals("active") ? Component.translatable("tooltip.jugcraft.concordance.jade.spire_working")
						: Component.translatable("tooltip.jugcraft.concordance.jade.spire_resting",
								Component.translatable("compose.jugcraft.spire.dormant." + (status.equals("unfounded") ? "raising" : status))));
				tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.spire_store", data.getIntOr("stock", 0), data.getIntOr("daily", 0)));
			});
		}
	}

	/** One of the simulation's reasons, sent as "key|factor|value|a|b" (VerdantBedBlockEntity.note). */
	private static Component reasonText(String encoded) {
		String[] parts = encoded.split("\\|");
		Object[] args = new Object[parts.length - 1];
		for (int i = 1; i < parts.length; i++) {
			args[i - 1] = i == 1 ? Component.translatable("compose.jugcraft.factor." + parts[1]) : parts[i];
		}
		return Component.translatable("compose.jugcraft." + parts[0], args);
	}

	private static void verdict(ITooltip tooltip, String growth, String reasons) {
		if (!growth.isEmpty()) {
			tooltip.add(growthText(growth));
		}
		if (!reasons.isEmpty()) {
			for (String reason : reasons.split("\n")) {
				tooltip.add(reasonText(reason));
			}
		}
	}

	/**
	 * A Greenwarden crop: its step, and its bed's last verdict for it, which the server sends with the bed (a crop
	 * has no block entity of its own to ask).
	 */
	private enum CropTooltip implements IBlockComponentProvider {
		INSTANCE;

		@Override
		public Identifier getUid() {
			return Jugcraft.id("organism");
		}

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			BlockState state = accessor.getBlockState();
			if (!(state.getBlock() instanceof OrganismCropBlock crop)) {
				return;
			}
			tooltip.add(Component.translatable("message.jugcraft.concordance.garden.crop", crop.getName(), crop.getAge(state),
					OrganismCropBlock.MAX_AGE, crop.isMaxAge(state) ? Component.translatable("compose.jugcraft.ecology.mature") : Component.empty()));
			if (!crop.isMaxAge(state) && accessor.getLevel().getBlockEntity(accessor.getPosition().below()) instanceof VerdantBedBlockEntity bed) {
				verdict(tooltip, bed.growth(), String.join("\n", bed.reasons()));
			}
		}
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

	/** A Circle Anchor's phase line, as the anchor itself says it (CircleAnchorBlockEntity.phaseText). */
	private static Component anchorPhase(String phase, CompoundTag data) {
		String key = "message.jugcraft.concordance.circle.phase." + phase;
		return switch (phase) {
			case "gathering" -> Component.translatable(key, data.getIntOr("joined", 0), "?");
			case "channeling" -> Component.translatable(key, data.getIntOr("done", 0) + 1, data.getIntOr("steps", 0));
			default -> Component.translatable(key);
		};
	}

	/** One structural fault, as CircleAnchorBlockEntity.faultText says it. */
	private static Component faultText(String role, String problem, String where) {
		Component roleName = Component.translatable("message.jugcraft.concordance.circle.role." + role);
		String key = "message.jugcraft.concordance.circle.fault." + problem;
		return switch (problem) {
			case "missing", "incompatible" -> Component.translatable(key, roleName, where);
			case "unloaded" -> Component.translatable(key, roleName);
			default -> Component.translatable(key, where);
		};
	}

	/** The Concordance's blocks: a study, notes waiting and Radiance held (ConcordanceDataProvider). */
	private enum ConcordanceTooltip implements IBlockComponentProvider {
		BENCH(ConcordanceDataProvider.BENCH),
		SCONCE(ConcordanceDataProvider.SCONCE),
		ANCHOR(ConcordanceDataProvider.ANCHOR),
		PYLON(ConcordanceDataProvider.PYLON),
		CRUCIBLE(ConcordanceDataProvider.CRUCIBLE),
		BED(ConcordanceDataProvider.BED),
		HEART(ConcordanceDataProvider.HEART),
		MAW(ConcordanceDataProvider.MAW),
		GAUGE(ConcordanceDataProvider.GAUGE),
		GLEANER(ConcordanceDataProvider.GLEANER),
		OBSERVATORY(ConcordanceDataProvider.OBSERVATORY);

		private final ConcordanceDataProvider provider;

		ConcordanceTooltip(ConcordanceDataProvider provider) {
			this.provider = provider;
		}

		@Override
		public Identifier getUid() {
			return provider.id;
		}

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			accessor.getServerData().getCompound(provider.dataKey).ifPresent(data -> {
				int study = data.getIntOr("study", -1);
				if (study >= 0) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.study", study));
				}
				if (data.getIntOr("notes", 0) != 0) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.notes"));
				}
				int radiance = data.getIntOr("radiance", -1);
				if (radiance >= 0) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.lantern.charge", radiance, KindledLanternItem.CAPACITY));
				}
				int ley = data.getIntOr("ley", -1);
				if (ley >= 0) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.ley", ley, LeyPylonBlockEntity.CAPACITY));
				}
				String band = data.getStringOr("band", "");
				if (!band.isEmpty()) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.temperature", data.getIntOr("temperature", 0),
							Component.translatable("compose.jugcraft.band." + band)));
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.volume", data.getIntOr("parts", 0), Mixture.MAX_PARTS));
					int steps = data.getIntOr("steps", 0);
					if (steps > 0) {
						tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.step", Math.min(steps, data.getIntOr("step", 0) + 1), steps));
						String next = data.getStringOr("next", "");
						if (!next.isEmpty()) {
							tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.waiting", next));
						}
					}
				}
				int capacity = data.getIntOr("capacity", -1);
				if (capacity >= 0) {
					if (!data.getBooleanOr("awake", true)) {
						tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.dormant"));
					}
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.nutrients", data.getIntOr("nutrients", 0), capacity));
					int[] habitat = data.getIntArray("habitat").orElse(new int[0]);
					if (habitat.length == 5) {
						tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.habitat", habitat[0], habitat[1], habitat[2]));
						tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.area", habitat[3], habitat[4]));
					}
					verdict(tooltip, data.getStringOr("growth", ""), data.getStringOr("reasons", ""));
				}
				String device = data.getStringOr("device", "");
				if (!device.isEmpty()) {
					tooltip.add(Component.translatable("compose.jugcraft.ecology.status." + device));
					int verdanceCapacity = data.getIntOr("verdance_capacity", 0);
					if (verdanceCapacity > 0) {
						tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.verdance", data.getLongOr("verdance", 0L), verdanceCapacity));
					}
					int held = data.getIntOr("held", -1);
					if (held >= 0) {
						tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.maw", held));
					}
					String mode = data.getStringOr("mode", "");
					if (!mode.isEmpty()) {
						tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.gauge",
								Component.translatable(mode.equals("suitability") ? "compose.jugcraft.ecology.gauge.suitability" : "compose.jugcraft.factor." + mode),
								data.getIntOr("signal", 0)));
					}
				}
				String sky = data.getStringOr("sky", "");
				if (!sky.isEmpty()) {
					tooltip.add(Component.translatable("compose.jugcraft.sky.status." + sky));
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.resonance", data.getLongOr("resonance", 0L),
							data.getIntOr("resonance_capacity", 0)));
					for (String pattern : data.getStringOr("visible", "").split("\n")) {
						if (!pattern.isEmpty()) {
							tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.visible", Sky.patternName(pattern)));
						}
					}
				}
				String phase = data.getStringOr("phase", "");
				if (!phase.isEmpty()) {
					tooltip.add(anchorPhase(phase, data));
					int faults = data.getIntOr("faults", 0);
					for (int i = 0; i < Math.min(faults, ConcordanceDataProvider.SHOWN_FAULTS); i++) {
						String[] fault = data.getStringOr("fault" + i, "").split("\\|");
						if (fault.length == 3) {
							tooltip.add(faultText(fault[0], fault[1], fault[2]));
						}
					}
					if (faults > ConcordanceDataProvider.SHOWN_FAULTS) {
						tooltip.add(Component.literal("+" + (faults - ConcordanceDataProvider.SHOWN_FAULTS)));
					}
				}
			});
		}
	}
}
