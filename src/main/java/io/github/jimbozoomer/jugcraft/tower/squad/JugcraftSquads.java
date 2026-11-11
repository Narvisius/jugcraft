package io.github.jimbozoomer.jugcraft.tower.squad;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;

/**
 * Drop pod squads (Justin's sheets 4 and 5, approved 9 November 2026; the plan: the scratchpad's
 * justin_notes_dropsquads.md): the LTD-7 laser designator ({@link LaserDesignatorItem}) marks a spot from its own sight;
 * one flare over it, then a pod for each soldier weaving down to land 10 to 20 blocks apart round it
 * ({@link SquadDrop}, {@link DropPod}); out of each a legionary ({@link Legionary}), who walks in to the squad's leader
 * and follows the player who called them, fighting what threatens them. Squads are requisitioned on the tower manager's
 * computer, paid from the tower's stores ({@link Requisitions}); each requisition is a charge on the player's account
 * ({@link SquadAccounts}) that any designator they hold can spend. One squad in the field at a time; never under a roof
 * of rock.
 */
public final class JugcraftSquads {
	/** The squads: how many soldiers, the leader's rank (Roman: a decanus leads three, a tesserarius five, an optio
	 * seven), and what the first of each costs (iron, copper, gold ingots and diamonds, from the tower's stores; tripled
	 * 11 November 2026, Justin: "triple the base cost of the call ins"). */
	public static final int[] SIZES = {3, 5, 7};
	public static final String[] RANKS = {"decanus", "tesserarius", "optio"};
	public static final Item[] PAY = {Items.IRON_INGOT, Items.COPPER_INGOT, Items.GOLD_INGOT, Items.DIAMOND};
	public static final int[][] COST = {{288, 192, 72, 18}, {480, 336, 120, 30}, {672, 480, 192, 48}};
	/** Each squad's price goes up this much every time a player requisitions one (their own prices, each size its own,
	 * compounding: Justin, 11 November 2026, "the price on each individual call in to go up 10% every time it's bought"). */
	public static final double PRICE_RISE = 1.10;
	/** How far the designator marks (blocks); how long from the mark till the pods land (ticks: the flare hangs, then they
	 * come down, fast from high in the sky: Justin, 11 November 2026, "they should look like they're coming from high up in
	 * the atmosphere and making a rapid entrance, only slowing at the last moment"); how far apart they land (blocks, at
	 * least and at most from the mark); how far its soldier is from a pod before it blows itself apart. */
	public static final double MARK_RANGE = 256;
	public static final int FLARE_TICKS = 60, FALL_TICKS = 120;
	public static final double SPREAD_MIN = 10, SPREAD_MAX = 20, CLEAR = 10;

	public static Item DESIGNATOR;
	public static EntityType<DropPod> DROP_POD;
	public static EntityType<Legionary> LEGIONARY;
	public static SoundEvent POD_ROAR_FAR, POD_ROAR_NEAR, POD_BOOM, POD_RETRO, POD_IMPACT, POD_HATCH, POD_DESTRUCT, MARK;

	private JugcraftSquads() {
	}

	/** Squad {@code index}'s price after {@code bought} of them: the first's, up {@link #PRICE_RISE} for each bought before,
	 * each payment rounded up. */
	public static int[] price(int index, int bought) {
		double rise = Math.pow(PRICE_RISE, Math.max(0, bought));
		int[] out = new int[PAY.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = (int) Math.min(Integer.MAX_VALUE, Math.ceil(COST[index][i] * rise - 1e-6));
		}
		return out;
	}

	/** The squad index (0 to 2) for a squad of {@code size}, or -1. */
	public static int index(int size) {
		for (int i = 0; i < SIZES.length; i++) {
			if (SIZES[i] == size) {
				return i;
			}
		}
		return -1;
	}

	public static void register() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id("laser_designator"));
		DESIGNATOR = Registry.register(BuiltInRegistries.ITEM, key, new LaserDesignatorItem(new Item.Properties().setId(key).stacksTo(1)
				.rarity(Rarity.EPIC)));
		net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.COMBAT)
				.register(output -> output.accept(DESIGNATOR));
		ResourceKey<EntityType<?>> podKey = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("drop_pod"));
		DROP_POD = Registry.register(BuiltInRegistries.ENTITY_TYPE, podKey, EntityType.Builder.<DropPod>of(DropPod::new, MobCategory.MISC)
				.sized(2.4F, 4.8F).noSummon().fireImmune().clientTrackingRange(16).updateInterval(20).build(podKey));
		ResourceKey<EntityType<?>> legKey = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("legionary"));
		LEGIONARY = Registry.register(BuiltInRegistries.ENTITY_TYPE, legKey, EntityType.Builder.<Legionary>of(Legionary::new, MobCategory.MISC)
				.sized(0.6F, 1.8F).eyeHeight(1.62F).vehicleAttachment(new Vec3(0, 0.6, 0)).clientTrackingRange(10).build(legKey));
		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(LEGIONARY, Legionary.attributes());
		POD_ROAR_FAR = sound("drop_pod.roar_far");             // (real recordings: tools/drop_pod_sounds.py)
		POD_ROAR_NEAR = sound("drop_pod.roar_near");
		POD_BOOM = sound("drop_pod.boom");
		POD_RETRO = sound("drop_pod.retro");
		POD_IMPACT = sound("drop_pod.impact");
		POD_HATCH = sound("drop_pod.hatch");
		POD_DESTRUCT = sound("drop_pod.selfdestruct");
		MARK = sound("drop_pod.mark");
		SquadPayloads.register();
		Requisitions.register();
	}

	private static SoundEvent sound(String path) {
		Identifier id = Jugcraft.id(path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}
}
