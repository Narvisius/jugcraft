package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The security gatehouse's search train and alarm station (Justin: "rework the security post next, the whole building
 * everything inside of it up to new standards"; tools/sec_gatehouse.py draws them).
 * <ul>
 * <li>The walk-through metal detector looks at what each player walking through it carries: anything of metal (by its
 * name: iron, gold, steel, copper, netherite, chain; any weapon or tool) and it alarms for three seconds (its zone
 * lights red, it beeps, the player is told what it found), else its PASS light shows green.</li>
 * <li>The X-ray baggage scanner takes what a player holds out to it (a right-click), shows it on its operator's screen
 * CLEAR or SEARCH and keeps the last five; it counts the bags.</li>
 * <li>The alarm station console links the security post in reach (its access log, who is inside, its duress alarm)
 * and once a second watches the perimeter round it: six zones, each in alarm while a hostile mob is in it.</li>
 * </ul>
 */
public final class SecurityGatehouse {
	public static final String DETECTOR = "sec_metal_detector", XRAY = "sec_xray", CONSOLE = "sec_alarm_console";
	public static final String[] SAS_PAGES = {"ALARM ZONES", "ACCESS LOG", "PERSONNEL INSIDE"};
	public static final String[] ZONES = {"Z1 NORTH FENCE", "Z2 NE FENCE", "Z3 SE FENCE", "Z4 SOUTH GATE", "Z5 SW FENCE", "Z6 NW FENCE"};
	/** How far the perimeter's zones reach round the console, and how far it looks for its post (blocks). */
	public static final int ZONE_REACH = 48, POST_REACH = 16, XRAY_KEEP = 5;
	static final int ALARM_TICKS = 60, PASS_TICKS = 30, SCAN_TICKS = 40;
	private static final Set<String> METAL = Set.of("iron", "gold", "steel", "copper", "netherite", "chain", "anvil", "bucket", "shears",
			"compass", "clock", "minecart", "rail", "sword", "axe", "pickaxe", "shovel", "hoe", "spear", "mace", "trident", "crossbow", "gun",
			"rifle", "pistol", "ammo", "bullet", "cartridge", "knife", "blade", "wrench", "spyglass", "lantern");

	private SecurityGatehouse() {
	}

	public static boolean isDetector(BlockState state) {
		return DcRoom.is(state, DETECTOR);
	}

	public static boolean isXray(BlockState state) {
		return DcRoom.is(state, XRAY);
	}

	public static boolean isConsole(BlockState state) {
		return DcRoom.is(state, CONSOLE);
	}

	/** An item of metal, or a weapon or tool (what the detector alarms on, what the X-ray sends to be searched). */
	public static boolean metal(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
		for (String w : METAL) {
			if (path.contains(w)) {
				return true;
			}
		}
		return false;
	}

	/** The first item of metal a player carries (worn, held or packed), or empty. */
	public static ItemStack found(Player player) {
		var inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (metal(s)) {
				return s;
			}
		}
		return ItemStack.EMPTY;
	}

	// ------------------------------------------------------------------------------------------- the metal detector

	/**
	 * Each tick: the players inside the detector's portal; each one newly walking in is looked at (the ones still in it
	 * from the tick before are not again).
	 */
	static void detectorTick(LivePanelBlockEntity d, ServerLevel level) {
		BlockPos pos = d.getBlockPos();
		List<Player> in = level.getEntitiesOfClass(Player.class, new AABB(pos).expandTowards(0, 1, 0).deflate(0.1, 0, 0.1), p -> !p.isSpectator());
		List<java.util.UUID> now = new ArrayList<>();
		for (Player p : in) {
			now.add(p.getUUID());
			if (!d.wdInside.contains(p.getUUID())) {
				walkedThrough(d, level, p);
			}
		}
		d.wdInside.clear();
		d.wdInside.addAll(now);
		long t = level.getGameTime();
		if (d.wdAlarm && t < d.wdUntil && (d.wdUntil - t) % 10 == 0) {
			level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.8F, 1.4F);
		}
	}

	/** Someone walked through: their pockets looked at, the lights and the sound, what it found said to them. */
	public static boolean walkedThrough(LivePanelBlockEntity d, ServerLevel level, Player p) {
		ItemStack hit = found(p);
		d.wdAlarm = !hit.isEmpty();
		d.wdUntil = level.getGameTime() + (d.wdAlarm ? ALARM_TICKS : PASS_TICKS);
		d.wdCount++;
		d.changedAndSync(level);
		if (d.wdAlarm) {
			level.playSound(null, d.getBlockPos(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.8F, 1.4F);
			p.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.wd_alarm", hit.getHoverName()).withStyle(ChatFormatting.RED));
		} else {
			level.playSound(null, d.getBlockPos(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.4F, 1.8F);
			p.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.wd_pass").withStyle(ChatFormatting.GREEN));
		}
		return d.wdAlarm;
	}

	// ------------------------------------------------------------------------------------------- the X-ray

	/** A bag put through: its name and verdict kept ("name|1" to be searched, "name|0" clear), the tunnel lit. */
	public static boolean scan(LivePanelBlockEntity x, ServerLevel level, ItemStack stack) {
		boolean search = metal(stack);
		x.xrLog.add(stack.getHoverName().getString().toUpperCase(Locale.ROOT) + "|" + (search ? 1 : 0));
		while (x.xrLog.size() > XRAY_KEEP) {
			x.xrLog.remove(0);
		}
		x.xrCount++;
		x.xrUntil = level.getGameTime() + SCAN_TICKS;
		x.changedAndSync(level);
		level.playSound(null, x.getBlockPos(), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.3F, 0.6F);
		level.playSound(null, x.getBlockPos(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.5F, search ? 0.8F : 1.8F);
		return search;
	}

	/** A logged bag: its name, to be searched. */
	public record Bag(String name, boolean search) {
		static Bag of(String s) {
			int bar = s.lastIndexOf('|');
			return bar < 0 ? new Bag(s, false) : new Bag(s.substring(0, bar), s.endsWith("1"));
		}
	}

	public static List<Bag> bags(LivePanelBlockEntity x) {
		List<Bag> out = new ArrayList<>();
		for (String s : x.xrLog) {
			out.add(Bag.of(s));
		}
		return out;
	}

	// ------------------------------------------------------------------------------------------- the alarm station

	/** Once a second: the console's post (the nearest security post in reach), the perimeter's zones. */
	static void consoleTick(LivePanelBlockEntity c, ServerLevel level) {
		BlockPos here = c.getBlockPos();
		List<LivePanelBlockEntity> posts = ControlBoard.panels(level, here, PlantInterior.BLOCKS.get(AccessControl.STATION), POST_REACH);
		posts.sort(java.util.Comparator.comparingDouble(p -> p.getBlockPos().distSqr(here)));
		BlockPos post = posts.isEmpty() ? null : posts.get(0).getBlockPos().immutable();
		int zones = zones(level, here);
		boolean changed = post == null ? c.panel != null : !post.equals(c.panel);
		if (changed || zones != c.secZones) {
			c.panel = post;
			c.secZones = zones;
			c.changedAndSync(level);
		}
	}

	/** The zones in alarm (a bit each): a hostile mob in the zone's sixth of the circle round here, within reach. */
	public static int zones(ServerLevel level, BlockPos here) {
		int mask = 0;
		AABB box = new AABB(here).inflate(ZONE_REACH, 16, ZONE_REACH);
		for (Mob m : level.getEntitiesOfClass(Mob.class, box, m -> m instanceof Enemy && m.isAlive())) {
			double dx = m.getX() - (here.getX() + 0.5), dz = m.getZ() - (here.getZ() + 0.5);
			if (dx * dx + dz * dz > (double) ZONE_REACH * ZONE_REACH) {
				continue;
			}
			mask |= 1 << zone(dx, dz);
		}
		return mask;
	}

	/** The zone a point is in, from here: 0 north (330 to 30 degrees), on clockwise by sixths. */
	public static int zone(double dx, double dz) {
		double a = Math.toDegrees(Math.atan2(dx, -dz));
		return (int) Math.floor(((a + 30 + 360) % 360) / 60) % 6;
	}

	/** What a click on the console off its screens says: its post, the zones. */
	static Component status(LivePanelBlockEntity c) {
		int n = Integer.bitCount(c.secZones);
		return Component.translatable("message.jugcraft.nuclear.sas_status", n == 0 ? "all zones secure" : n + " in alarm",
				c.panel == null ? "no security post in reach" : "linked to the security post");
	}
}
