package io.github.jimbozoomer.jugcraft.powerline;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Moves energy along power lines. A line network is every structure joined by wires (discovered on demand, cached
 * per level and thrown away whenever a wire or structure changes). Each tick, every box's waiting energy goes out
 * over the line to the other boxes, at most the network's rate (its weakest support's capacity) in total, shared
 * evenly. A network with a superconducting pylon only carries power while a Unit Substation on it has nitrogen for
 * the cryogenic cable. How full the line ran is kept on every structure (wire sheen, hum, and how hard a live wire
 * shocks), and anything touching a live wire is electrocuted.
 */
public final class LineNetworks {
	/** Cap on structures in one network, so a huge grid cannot stall a tick. */
	public static final int MAX_STRUCTURES = 4096;
	/** Nitrogen a superconducting network boils off per tick (the Air Separation Unit makes 8 mB/t). */
	public static final int NITROGEN_PER_TICK = 2;
	/** How close to a live wire counts as touching it. */
	public static final double SHOCK_RANGE = 0.6;
	public static final ResourceKey<DamageType> ELECTROCUTION = ResourceKey.create(Registries.DAMAGE_TYPE,
			io.github.jimbozoomer.jugcraft.Jugcraft.id("electrocution"));

	private static final Map<ServerLevel, List<Network>> CACHE = new WeakHashMap<>();

	private LineNetworks() {
	}

	public static void invalidate(net.minecraft.world.level.Level level) {
		if (level instanceof ServerLevel server) {
			CACHE.remove(server);
		}
	}

	/** One tick of every line network in the level (called from the end of the level tick). */
	public static void tick(ServerLevel level) {
		List<Network> networks = CACHE.computeIfAbsent(level, LineNetworks::discoverAll);
		for (Network network : networks) {
			network.tick(level);
		}
	}

	/** The network through this anchor, for tests and the tooltip readout (rate, structures). */
	public static Network networkOf(ServerLevel level, BlockPos anchor) {
		for (Network network : CACHE.computeIfAbsent(level, LineNetworks::discoverAll)) {
			if (network.members.contains(anchor)) {
				return network;
			}
		}
		return null;
	}

	private static List<Network> discoverAll(ServerLevel level) {
		List<Network> out = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		for (BlockPos anchor : LineRegistry.of(level).anchors().keySet()) {
			if (seen.contains(anchor) || !(level.getBlockEntity(anchor) instanceof LineAnchorBlockEntity)) {
				continue;
			}
			Network network = Network.discover(level, anchor);
			seen.addAll(network.members);
			if (network.members.size() > 1) {
				out.add(network);
			}
		}
		return out;
	}

	public static final class Network {
		final Set<BlockPos> members = new HashSet<>();
		final List<BlockPos> boxes = new ArrayList<>();
		final List<BlockPos> substations = new ArrayList<>();
		/** Every wire pair once: {a, b}. */
		final List<BlockPos[]> links = new ArrayList<>();
		long rate = Long.MAX_VALUE;
		boolean superconducting;
		/** JE moved on the last tick. */
		public long lastMoved;

		public long rate() {
			return rate == Long.MAX_VALUE ? 0 : rate;
		}

		public int size() {
			return members.size();
		}

		static Network discover(ServerLevel level, BlockPos start) {
			Network n = new Network();
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			queue.add(start);
			n.members.add(start);
			while (!queue.isEmpty() && n.members.size() < MAX_STRUCTURES) {
				BlockPos pos = queue.poll();
				if (!(level.getBlockEntity(pos) instanceof LineAnchorBlockEntity anchor)) {
					continue;
				}
				LineKind kind = anchor.kind();
				n.rate = Math.min(n.rate, kind.capacity);
				if (kind.isBox()) {
					n.boxes.add(pos);
				}
				if (kind == LineKind.UNIT_SUBSTATION) {
					n.substations.add(pos);
				}
				if (kind == LineKind.SUPERCONDUCTING_PYLON) {
					n.superconducting = true;
				}
				for (BlockPos other : anchor.connections()) {
					if (!level.isLoaded(other) || !(level.getBlockEntity(other) instanceof LineAnchorBlockEntity partner)
							|| !partner.connections().contains(pos)) {
						continue;
					}
					if (pos.asLong() < other.asLong()) {
						n.links.add(new BlockPos[] {pos, other});
					}
					if (n.members.add(other)) {
						queue.add(other);
					}
				}
			}
			return n;
		}

		void tick(ServerLevel level) {
			long budget = rate();
			if (superconducting && !coolSuperconductor(level)) {
				budget = 0;
			}
			// What is waiting at each box, and the room each box has to take.
			long moved = 0;
			if (budget > 0 && boxes.size() > 1) {
				List<LineAnchorBlockEntity> list = new ArrayList<>();
				for (BlockPos pos : boxes) {
					if (level.getBlockEntity(pos) instanceof LineAnchorBlockEntity box) {
						list.add(box);
					}
				}
				for (LineAnchorBlockEntity sender : list) {
					long waiting = Math.min(sender.waitingToSend(), budget - moved);
					if (waiting <= 0) {
						continue;
					}
					List<LineAnchorBlockEntity> receivers = new ArrayList<>();
					for (LineAnchorBlockEntity r : list) {
						if (r != sender && r.roomToReceive() > 0) {
							receivers.add(r);
						}
					}
					for (int pass = 0; pass < 2 && waiting > 0 && !receivers.isEmpty(); pass++) {
						long share = Math.max(1, waiting / receivers.size());
						for (LineAnchorBlockEntity r : receivers) {
							if (waiting <= 0) {
								break;
							}
							long accepted = r.receive(Math.min(share, waiting));
							sender.takeToSend(accepted);
							waiting -= accepted;
							moved += accepted;
						}
					}
				}
			}
			lastMoved = moved;
			int load = rate() == 0 ? 0 : (int) Math.min(255, Math.ceil(moved * 255.0 / rate()));
			if (level.getGameTime() % 10 == 0) {
				for (BlockPos pos : members) {
					if (level.getBlockEntity(pos) instanceof LineAnchorBlockEntity anchor) {
						anchor.setLoad(load);
					}
				}
			}
			if (moved > 0 && level.getGameTime() % 5 == 0) {
				shock(level, load);
			}
		}

		private boolean coolSuperconductor(ServerLevel level) {
			for (BlockPos pos : substations) {
				if (level.getBlockEntity(pos) instanceof LineAnchorBlockEntity sub && sub.nitrogen != null
						&& io.github.jimbozoomer.jugcraft.chemistry.PetroFluids.NITROGEN != null
						&& sub.nitrogen.has(io.github.jimbozoomer.jugcraft.chemistry.PetroFluids.NITROGEN.fluid(), NITROGEN_PER_TICK)) {
					sub.nitrogen.drain(NITROGEN_PER_TICK);
					sub.setChanged();
					return true;
				}
			}
			return false;
		}

		/** Players and mobs touching a live wire take a shock that grows with the load, harder on high voltage. */
		private void shock(ServerLevel level, int load) {
			DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ELECTROCUTION));
			for (BlockPos[] link : links) {
				if (!(level.getBlockEntity(link[0]) instanceof LineAnchorBlockEntity a) || !(level.getBlockEntity(link[1]) instanceof LineAnchorBlockEntity b)) {
					continue;
				}
				List<LineWires.Wire> wires = LineWires.between(link[0], a.kind(), a.connections(), link[1], b.kind(), b.connections());
				if (wires.isEmpty()) {
					continue;
				}
				AABB box = null;
				for (LineWires.Wire wire : wires) {
					AABB w = new AABB(wire.a(), wire.b()).expandTowards(0, -wire.sag() - 1, 0).inflate(SHOCK_RANGE + 1);
					box = box == null ? w : box.minmax(w);
				}
				long capacity = Math.min(a.kind().capacity, b.kind().capacity);
				float voltage = (float) (Math.log10(Math.max(10, capacity)) - 2);   // about 1.6 for wood to 5.2 for superconducting
				for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.isSpectator())) {
					if (entity instanceof Player player && player.isCreative()) {
						continue;
					}
					Vec3 centre = entity.getBoundingBox().getCenter();
					double reach = SHOCK_RANGE + entity.getBbWidth() / 2;
					for (LineWires.Wire wire : wires) {
						if (LineWires.distance(wire, centre) <= reach + entity.getBbHeight() / 2) {
							float damage = 1.0F + voltage * 2.0F * (load / 255.0F) * 3.0F;
							if (entity.hurtServer(level, source, damage)) {
								Vec3 push = centre.subtract(wire.at(0.5)).multiply(1, 0, 1).normalize().scale(0.6);
								entity.push(push.x, 0.3, push.z);
								if (load > 200) {
									entity.igniteForSeconds(3);
								}
								level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y, centre.z, 12,
										0.3, 0.3, 0.3, 0.1);
							}
							break;
						}
					}
				}
			}
		}
	}
}
