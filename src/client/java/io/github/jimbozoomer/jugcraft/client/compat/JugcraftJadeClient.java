package io.github.jimbozoomer.jugcraft.client.compat;

import io.github.jimbozoomer.jugcraft.compat.jade.ConcordanceDataProvider;
import io.github.jimbozoomer.jugcraft.compat.jade.JugcraftJadePlugin;
import io.github.jimbozoomer.jugcraft.compat.jade.MachineDataProvider;
import io.github.jimbozoomer.jugcraft.compat.jade.PactDataProvider;
import io.github.jimbozoomer.jugcraft.concordance.KindledLanternItem;
import io.github.jimbozoomer.jugcraft.concordance.CircleAnchorBlock;
import io.github.jimbozoomer.jugcraft.concordance.ComposeText;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlock;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleStandBlock;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Mixture;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Code;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Factor;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Habitat;
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
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactPerchBlock;
import io.github.jimbozoomer.jugcraft.concordance.spirits.SpiritAnchorBlock;
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
import net.minecraft.core.registries.BuiltInRegistries;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
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
		registration.registerBlockComponent(StorageCrateTooltip.INSTANCE, io.github.jimbozoomer.jugcraft.storage.StorageCrateBlock.class);
		registration.registerBlockComponent(ConcordanceTooltip.BENCH, LampwrightBenchBlock.class);
		registration.registerBlockComponent(ConcordanceTooltip.SCONCE, LumenSconceBlock.class);
		registration.registerBlockComponent(ConcordanceTooltip.ANCHOR, CircleAnchorBlock.class);
		registration.registerBlockComponent(ConcordanceTooltip.PYLON, LeyPylonBlock.class);
		registration.registerBlockComponent(ConcordanceTooltip.CRUCIBLE, CrucibleBlock.class);
		registration.registerBlockComponent(StandTooltip.INSTANCE, CrucibleStandBlock.class);
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
		// Concordance Chunk D, D-06 (G11): the pact perch and the Spirit Anchor.
		registration.registerBlockComponent(AgreementTooltip.PERCH, PactPerchBlock.class);
		registration.registerBlockComponent(AgreementTooltip.ANCHOR, SpiritAnchorBlock.class);
	}

	/** Occupied slots only, supplied after the server checks crate access. */
	private enum StorageCrateTooltip implements IBlockComponentProvider {
		INSTANCE;
		@Override public Identifier getUid() { return io.github.jimbozoomer.jugcraft.compat.jade.StorageCrateDataProvider.ID; }
		@Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			var data = accessor.getServerData();
			if (data.getIntOr("crate_capacity", 0) > 0) tooltip.add(Component.translatable("screen.jugcraft.storage_crate.used",
				data.getIntOr("crate_used", 0), data.getIntOr("crate_capacity", 0)));
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
				// Concordance Chunk D, D-06: Stolas's pact: its service, state, next offering and grace (PactReading).
				data.getCompound("pact").ifPresent(pact -> pactLines(tooltip, pact));
			});
		}
	}

	/**
	 * Concordance Chunk D, D-06: a pact's or agreement's reading (spirits/PactReading, read on the server), in words: whether
	 * one is kept and with whom, its service, its state, the next offering or the grace, why its presence waits; a visit or a
	 * departure. It names no player and shows nothing of the escrow, because the reading carries neither.
	 */
	private static void pactLines(ITooltip tooltip, CompoundTag data) {
		if (data.getBooleanOr("departing", false)) {
			tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.pact.departing"));
		}
		switch (data.getStringOr("standing", "")) {
			case "disabled" -> tooltip.add(Component.translatable("message.jugcraft.concordance.disabled"));
			case "none" -> tooltip.add(Component.translatable(data.getBooleanOr("visiting", false) ? "tooltip.jugcraft.concordance.jade.pact.visiting"
					: "tooltip.jugcraft.concordance.jade.pact.none"));
			// The diagnostic's own words, named, not worked out here (this client works out no diagnostic: check_signs, C-01).
			case "copy" -> tooltip.add(ComposeText.show(Text.of(Diagnostic.PREFIX + Code.CONFLICTING_AGREEMENT.id)));
			case "unreadable" -> tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.pact.unreadable"));
			case "kept" -> {
				// Stolas's own reading names no counterpart (he is it): an empty id would parse as "minecraft:" and show a raw key.
				String named = data.getStringOr("counterpart", "");
				Identifier counterpart = named.isEmpty() ? null : Identifier.tryParse(named);
				if (counterpart != null) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.pact.keeps",
							Component.translatable("entity." + counterpart.getNamespace() + "." + counterpart.getPath())));
				}
				String service = data.getStringOr("service", "");
				if (!service.isEmpty()) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.pact.service",
							Component.translatable("tooltip.jugcraft.concordance.jade.pact.service." + service)));
				}
				tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.pact.state." + data.getStringOr("state", "active")));
				int due = data.getIntOr("due_days", -1);
				if (due >= 0) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.pact.next", due));
				}
				int grace = data.getIntOr("grace_days", -1);
				if (grace >= 0) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.pact.grace", grace));
				}
				String waiting = data.getStringOr("waiting", "");
				if (waiting.equals("lost")) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.pact.lost"));
				} else if (!waiting.isEmpty()) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.pact.waiting",
							Component.translatable("compose.jugcraft.worker.status." + waiting)));
				}
			}
			default -> {
			}
		}
	}

	/** Concordance Chunk D, D-06 (G11): a pact perch or a Spirit Anchor: the agreement it keeps and how it stands; no player, no escrow. */
	private enum AgreementTooltip implements IBlockComponentProvider {
		PERCH(PactDataProvider.PERCH),
		ANCHOR(PactDataProvider.ANCHOR);

		private final PactDataProvider provider;

		AgreementTooltip(PactDataProvider provider) {
			this.provider = provider;
		}

		@Override
		public Identifier getUid() {
			return provider.id;
		}

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			accessor.getServerData().getCompound(provider.dataKey).ifPresent(data -> pactLines(tooltip, data));
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

	/**
	 * One of the simulation's reasons, sent as "key|factor|value|a|b" (VerdantBedBlockEntity.note): a field 1 that names a
	 * factor is shown by its name. Since Concordance Chunk C (C-08) a reason may carry no factor ("ecology.mixed|25"): its
	 * fields are shown as sent.
	 */
	private static Component reasonText(String encoded) {
		String[] parts = encoded.split("\\|");
		boolean factor = parts.length > 1 && Factor.fromId(parts[1]) != null;
		Object[] args = new Object[parts.length - 1];
		for (int i = 1; i < parts.length; i++) {
			args[i - 1] = i == 1 && factor ? Component.translatable("compose.jugcraft.factor." + parts[1]) : parts[i];
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

	/** Concordance Chunk C (C-08): how far its harvests have strained a bed, as the server counts it. */
	private static void strain(ITooltip tooltip, int strain) {
		if (strain > 0) {
			tooltip.add(Component.translatable("compose.jugcraft.ecology.strain", strain, Habitat.MAX_STRAIN));
		}
	}

	/**
	 * A Greenwarden crop: its step, and its bed's last verdict for it (ripe, as it grew: Concordance Chunk C, C-07, C-P55),
	 * which the server sends with the bed (a crop has no block entity of its own to ask), its bed's strain (C-08), and (C-09)
	 * its bed's graft when the crop is its host (the server sends the organism the graft is for).
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
			boolean ripe = crop.isMaxAge(state);
			tooltip.add(Component.translatable("message.jugcraft.concordance.garden.crop", crop.getName(), crop.getAge(state),
					OrganismCropBlock.MAX_AGE, ripe ? Component.translatable("compose.jugcraft.ecology.mature") : Component.empty()));
			if (accessor.getLevel().getBlockEntity(accessor.getPosition().below()) instanceof VerdantBedBlockEntity bed) {
				String growth = bed.growth();
				// Concordance Chunk C (C-07): ripe, it still shows its bed's last verdict, as it grew: what its regrowth will meet.
				if (ripe && !growth.isEmpty() && !growth.equals("dormant") && !growth.equals("waiting")) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.as_it_grew", growthText(growth)));
					growth = "";
				}
				verdict(tooltip, growth, String.join("\n", bed.reasons()));
				strain(tooltip, bed.strainNow());
				if (!bed.graft().isEmpty() && bed.graftFor().equals(crop.organismId())) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.graft", ComposeText.name(new Text.Ref("graft", bed.graft()))));
				}
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
			case "gathering" -> Component.translatable(key, data.getIntOr("joined", 0), data.getIntOr("needed", 0));
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
					int held = data.getIntOr("held", 0);
					if (held > 0) {
						tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.held", held));
					}
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.volume", data.getIntOr("parts", 0), Mixture.MAX_PARTS));
					// Concordance Chunk C (C-01): the tank, and why it waits as the crucible says it (worded here, worked out on the server).
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.tank", data.getIntOr("tank", 0), CrucibleBlockEntity.WATER_PER_BUCKET));
					int steps = data.getIntOr("steps", 0);
					if (steps > 0) {
						tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.step", Math.min(steps, data.getIntOr("step", 0) + 1), steps));
						String next = data.getStringOr("next", "");
						if (!next.isEmpty()) {
							tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.waiting", next));
						}
					}
					Diagnostic reason = Diagnostic.decode(data.getStringOr("reason", ""));
					if (reason != null) {
						tooltip.add(ComposeText.show(reason.text()));
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
					strain(tooltip, data.getIntOr("strain", 0));
					// Concordance Chunk C (C-09): the graft it carries, when the rules know it.
					String graft = data.getStringOr("graft", "");
					if (!graft.isEmpty()) {
						tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.graft", ComposeText.name(new Text.Ref("graft", graft))));
					}
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
					// Concordance Chunk C (C-03): who is channelling, what lies in the anchor, and while a rite holds it what it
					// has spent apart from what it still holds, as the server worked them out.
					if (phase.equals("channeling") || phase.equals("complete")) {
						tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.participants", data.getIntOr("joined", 0)));
					}
					int offerings = data.getIntOr("offerings", 0);
					for (int i = 0; i < Math.min(offerings, ConcordanceDataProvider.SHOWN_OFFERINGS); i++) {
						String[] offering = data.getStringOr("offering" + i, "").split("\\|");
						Identifier item = offering.length == 2 ? Identifier.tryParse(offering[0]) : null;
						if (item != null) {
							tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.offering",
									new ItemStack(BuiltInRegistries.ITEM.getValue(item)).getHoverName(), offering[1]));
						}
					}
					if (offerings > ConcordanceDataProvider.SHOWN_OFFERINGS) {
						tooltip.add(Component.literal("+" + (offerings - ConcordanceDataProvider.SHOWN_OFFERINGS)));
					}
					int spentFocus = data.getIntOr("spent_focus", -1);
					if (spentFocus >= 0) {
						tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.spent", spentFocus, data.getIntOr("spent_ley", 0)));
						tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.reserved", data.getIntOr("reserved", 0)));
					}
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

	/** A Crucible Stand (Concordance Chunk C, C-05): its JE, what decides its heat and at what, what it pays, why it gives none. */
	private enum StandTooltip implements IBlockComponentProvider {
		INSTANCE;

		@Override
		public Identifier getUid() {
			return ConcordanceDataProvider.STAND.id;
		}

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			accessor.getServerData().getCompound(ConcordanceDataProvider.STAND.dataKey).ifPresent(data -> {
				if (data.getBooleanOr("disabled", false)) {
					tooltip.add(Component.translatable("message.jugcraft.concordance.disabled"));
					return;
				}
				tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.stand_energy", data.getLongOr("energy", 0L), data.getLongOr("capacity", 0L)));
				String aim = data.getStringOr("aim", "");
				Component heat = aim.isEmpty() ? Component.translatable("tooltip.jugcraft.concordance.jade.stand_off")
						: Component.translatable("tooltip.jugcraft.concordance.jade.stand_heat", Component.translatable("compose.jugcraft.band." + aim),
								data.getIntOr("target", 0));
				tooltip.add(Component.translatable(data.getBooleanOr("following", false) ? "tooltip.jugcraft.concordance.jade.stand_following"
						: "tooltip.jugcraft.concordance.jade.stand_setting", heat));
				int draw = data.getIntOr("draw", 0);
				if (draw > 0) {
					tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.stand_draw", draw));
				}
				Diagnostic reason = Diagnostic.decode(data.getStringOr("reason", ""));
				if (reason != null) {
					if (data.getBooleanOr("waits", false)) {
						tooltip.add(Component.translatable("tooltip.jugcraft.concordance.jade.stand_waits"));
					}
					tooltip.add(ComposeText.show(reason.text()));
				}
			});
		}
	}
}
