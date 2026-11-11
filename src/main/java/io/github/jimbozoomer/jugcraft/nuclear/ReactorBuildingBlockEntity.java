package io.github.jimbozoomer.jugcraft.nuclear;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The medium plant's reactor building inside (22 October 2026; claude/jugcraft-reactor-building-proposal.md, machines 1
 * to 10 approved on 19 October): the anchor of its compiled sites (tools/reactor_site.py), under the loop floor.
 *
 * <p>The reactor is the plant's own (its vessel, steam generators, coolant pumps and pressurizer stand hidden under the
 * floor: EquipmentAnchorBlock.HIDDEN); once a second this reads it (BoardPlant, as the control room's boards do) and
 * shows the building as it is: at power (reactor_building) or refuelling (reactor_building_refuel: the head package
 * on its stand, the internals in the bay, the polar crane over the cavity, the refuelling machine over the core),
 * opened and closed from the console. Its live parts: the head package's CRDM fans (1A and 1B run while the head is
 * hot, 1C stands by), the four coolant pumps' couplings turning while each loop has flow, the steam generators' and
 * the pressurizer's gauges, the pumps' upper oil pots (warm while they run), the head's exhaust dial, the area
 * radiation monitor's mR/hr and the frisker's counts.
 *
 * <p>Clicking the head package (or the open vessel while refuelling) works the vessel as clicking it did: its status,
 * or fuel in and out. Its radiation comes from where the building shows the core.
 */
public class ReactorBuildingBlockEntity extends BlockEntity implements SiteAnchor {
	public static final String POWER = "reactor_building", REFUEL = "reactor_building_refuel";
	/** How far the vessel may be from the controller. */
	public static final int RANGE = 48;

	private boolean refuel;
	/** Coolant pumps turning (bit k: loop k + 1's), head package fans running (bit i: fan 1A, 1B, 1C). */
	private int pumps, fans;
	private final float[] sgP = {900, 900, 900, 900}, oilT = {140, 140, 140, 140};
	private float pzrP = 2235, exhT = 145, armMr = 2.2F, cpm = 52;
	private @Nullable BlockPos vessel;
	private int shown;

	public ReactorBuildingBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftNuclear.REACTOR_BUILDING_ENTITY, pos, state);
	}

	public boolean refuelling() {
		return refuel;
	}

	// ------------------------------------------------------------------ ticking

	public void serverTick(ServerLevel level) {
		checkSite(level);
		if (Math.floorMod(level.getGameTime() + getBlockPos().hashCode(), 20) == 0) {
			update(level);
		}
	}

	/** Once a second: reads the reactor, shows the building as it is, tells the clients. */
	public void update(ServerLevel level) {
		EquipmentBlockEntity v = vessel(level);
		boolean wasRefuel = refuel;
		int key = quantized();
		if (v == null) {
			refuel = false;
			pumps = fans = 0;
			for (int k = 0; k < 4; k++) {
				sgP[k] = 0;
				oilT[k] = approach(oilT[k], 70, 2);
			}
			pzrP = 0;
			exhT = 100;
		} else {
			BoardPlant p = BoardPlant.of(v);
			refuel = v.core.refuelOpen;
			pumps = 0;
			for (int k = 0; k < 4; k++) {
				boolean on = p.pumps && k < Math.max(1, p.loops) && p.loopFlow[k] > 0.05 && (p.rcpStopped & 1 << k) == 0;
				if (on) pumps |= 1 << k;
				sgP[k] = (float) p.sgPress(k);
				oilT[k] = approach(oilT[k], on ? 140 : 85, 1);
			}
			pzrP = (float) p.pzrPress();
			double tavg = p.tavg();
			fans = !refuel && tavg > 200 ? 0b011 : 0;
			exhT = refuel ? 100 : (float) (100 + 45 * Math.max(0, Math.min(1, (tavg - 100) / 457)));
			showCore(level, v);
		}
		// The area radiation monitor: a little background on the floor, more with the reactor open; the frisker's counts
		// wander round its background.
		double base = refuel ? 3.6 : 2.2;
		if (v != null && v.core.fuelDamaged) base += 40;
		armMr = (float) Math.max(0.1, base + level.getRandom().nextGaussian() * 0.12);
		cpm = (float) Math.max(30, Math.min(80, cpm + level.getRandom().nextGaussian() * 4 + (52 - cpm) * 0.2));
		if (refuel != wasRefuel) {
			checkSite(level);
		}
		if (quantized() != key || ++shown >= 5) {
			// (the displays' figures change every few seconds anyway)
			shown = 0;
			sync();
		}
	}

	public void clientTick(Level level) {
		checkSite(level);
	}

	private static float approach(float v, float to, float step) {
		return v < to ? Math.min(to, v + step) : Math.max(to, v - step);
	}

	/** What the clients draw, as a number that changes when they need telling. */
	private int quantized() {
		int h = (refuel ? 1 : 0) + 31 * pumps + 961 * fans + Math.round(pzrP / 100) * 7 + Math.round(exhT / 10) * 11;
		for (int k = 0; k < 4; k++) {
			h = h * 17 + Math.round(sgP[k] / 50) * 3 + Math.round(oilT[k] / 10);
		}
		return h;
	}

	/** The reactor this building holds: the nearest vessel within RANGE. */
	private @Nullable EquipmentBlockEntity vessel(ServerLevel level) {
		if (vessel != null && level.isLoaded(vessel) && level.getBlockEntity(vessel) instanceof EquipmentBlockEntity e && e.kind().reactor()) {
			return e;
		}
		vessel = null;
		double best = (double) RANGE * RANGE;
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : EquipmentRegistry.of(level).anchors().entrySet()) {
			if (e.getValue().kind() != EquipmentKind.REACTOR_VESSEL) continue;
			double d = e.getKey().distSqr(getBlockPos());
			if (d < best && level.isLoaded(e.getKey())) {
				best = d;
				vessel = e.getKey();
			}
		}
		return vessel != null && level.getBlockEntity(vessel) instanceof EquipmentBlockEntity v ? v : null;
	}

	/** The vessel's radiation from where the building shows its core and head (its live data's points). */
	private void showCore(ServerLevel level, EquipmentBlockEntity v) {
		SiteData d = data();
		if (d == null || !EquipmentAnchorBlock.hidden(v.getBlockState())) return;
		BlockPos core = point(d, "core"), top = point(d, "head");
		if (core != null && top != null) {
			v.showAt(level, core, top);
		}
	}

	private @Nullable BlockPos point(SiteData d, String name) {
		JsonElement pts = d.live.get("points");
		if (pts == null || !pts.isJsonObject() || !pts.getAsJsonObject().has(name)) return null;
		JsonArray c = pts.getAsJsonObject().getAsJsonArray(name);
		Vec3 w = Yards.world(getBlockPos(), siteFacing(), d, c.get(0).getAsDouble(), c.get(1).getAsDouble(), c.get(2).getAsDouble());
		return BlockPos.containing(w);
	}

	/** Its site in the registry is the one it shows now (at power or refuelling): swapped, and the chunks built again. */
	private void checkSite(Level level) {
		SiteRegistry.Placement p = SiteRegistry.at(level, getBlockPos());
		String want = siteKey();
		if (p != null && p.data().key.equals(want) || p == null && SiteData.get(want) == null) {
			return;
		}
		if (level.isClientSide()) {
			Sites.clientRefresh.accept(this);
		} else {
			Sites.unload(this, level);
			Sites.load(this, level);
		}
	}

	public Component status() {
		return Component.translatable("message.jugcraft.nuclear.reactor_building_status",
				Component.translatable("message.jugcraft.nuclear.reactor_building_" + (refuel ? "refuelling" : "power")),
				Integer.bitCount(pumps), Math.round(pzrP), Math.round(sgP[0]), String.format(Locale.ROOT, "%.1f", armMr));
	}

	// ------------------------------------------------------------------ the site

	private @Nullable SiteData data() {
		return SiteData.get(siteKey());
	}

	@Override
	public String siteKey() {
		return refuel ? REFUEL : POWER;
	}

	@Override
	public Direction siteFacing() {
		return getBlockState().getValue(ReactorBuildingBlock.FACING);
	}

	private static final Map<String, SiteData.Group[][]> TABLES = new java.util.concurrent.ConcurrentHashMap<>();

	@Override
	public SiteData.Group[][] groups() {
		SiteData d = data();
		if (d == null) return new SiteData.Group[0][];
		return TABLES.computeIfAbsent(d.key, k -> d.groupTable());
	}

	@Override
	public int value(int v) {
		SiteData d = data();
		if (d == null || level == null || v >= d.variables.size()) return 0;
		SiteData.Variable var = d.variables.get(v);
		String n = var.name();
		long now = level.getGameTime();
		int frames = var.values().length;
		if (n.startsWith("fan")) {
			int i = n.charAt(3) - '1';
			return (fans & 1 << i) != 0 ? (int) Math.floorMod(now + i * 3L, frames) : 0;
		}
		if (n.startsWith("rcp")) {
			int k = n.charAt(3) - '1';
			return (pumps & 1 << k) != 0 ? (int) Math.floorMod(now / 2 + k, frames) : 0;
		}
		if (n.startsWith("sg") && n.endsWith("_p")) return var.nearest(sgP[n.charAt(2) - '1']);
		if (n.equals("pzr_p")) return var.nearest(pzrP);
		if (n.startsWith("oil")) return var.nearest(oilT[n.charAt(3) - '1']);
		if (n.equals("exh_t")) return var.nearest(exhT);
		return 0;
	}

	@Override
	public double display(String key) {
		return switch (key) {
			case "arm_mr" -> armMr;
			case "frisker_cpm" -> cpm;
			default -> Double.NaN;
		};
	}

	/** A click on the head package (at power) or the open vessel (refuelling): the vessel's own use (its status, fuel). */
	@Override
	public void onZone(ServerLevel level, Player player, String zone) {
		if (!zone.equals("vessel")) return;
		EquipmentBlockEntity v = vessel(level);
		if (v == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.reactor_building_no_vessel"));
			return;
		}
		EquipmentPlacement.use(level, v.getBlockPos(), player, player.getMainHandItem(), InteractionHand.MAIN_HAND);
	}

	// ------------------------------------------------------------------ saving and syncing

	private void sync() {
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		saveAdditional(out);
		return out.buildResult();
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		refuel = in.getBooleanOr("refuel", false);
		pumps = in.getIntOr("pumps", 0);
		fans = in.getIntOr("fans", 0);
		for (int k = 0; k < 4; k++) {
			sgP[k] = in.getFloatOr("sg_p" + k, 0);
			oilT[k] = in.getFloatOr("oil_t" + k, 85);
		}
		pzrP = in.getFloatOr("pzr_p", 0);
		exhT = in.getFloatOr("exh_t", 100);
		armMr = in.getFloatOr("arm_mr", 2.2F);
		cpm = in.getFloatOr("cpm", 52);
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putBoolean("refuel", refuel);
		out.putInt("pumps", pumps);
		out.putInt("fans", fans);
		for (int k = 0; k < 4; k++) {
			out.putFloat("sg_p" + k, sgP[k]);
			out.putFloat("oil_t" + k, oilT[k]);
		}
		out.putFloat("pzr_p", pzrP);
		out.putFloat("exh_t", exhT);
		out.putFloat("arm_mr", armMr);
		out.putFloat("cpm", cpm);
	}
}
