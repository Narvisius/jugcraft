package io.github.jimbozoomer.jugcraft.concordance.smithy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Artifice;
import io.github.jimbozoomer.jugcraft.concordance.artifice.ArtificeCatalog;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Forge;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Gem;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Quality;
import io.github.jimbozoomer.jugcraft.concordance.artifice.RolledAffix;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Rune;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Stat;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Substrate;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sky.SkyItem;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.jspecify.annotations.Nullable;

/**
 * Runesmithing on the server (roadmap step 19, docs/features/arcane-concordance-artifice.md): the Resonant Ring and its
 * {@code jugcraft:artifice} component, the Artificer's Bench, and every process the bench performs. Each process works
 * out the ring's next state with {@link Forge}, writes it to the ring, takes what the process costs, and only then
 * tells the player: what a player sees has already been decided and saved. Rolls happen once, when a ring is forged
 * (the seed comes from the server's random) and when it is reforged (from that seed and its count), so nothing a
 * player does short of the process itself can roll again.
 */
public final class Artificery {
	public static final String RESEARCH = "jugcraft:runesmithing";
	/** The practice the enhancements record (reforge, inscribe, socket, bond, repair: three different master it). */
	public static final String ACTIVITY = "jugcraft:artifice";
	public static final int FORGE_FOCUS = 8;
	public static final int REFORGE_FOCUS = 6;
	public static final int INSCRIBE_FOCUS = 4;
	public static final int BOND_FOCUS = 10;
	/** A salvage must be confirmed by a second use within this many game ticks. */
	public static final int CONFIRM_TICKS = 200;
	/** A worn ring wears one point this often (game ticks); at its last point it is dull until repaired. */
	public static final int WEAR_TICKS = 600;
	public static final Item REFORGE_CATALYST = Items.REDSTONE;
	public static final Item UNSOCKET_TOOL = Items.SHEARS;
	public static final Item SALVAGE_TOOL = Items.FLINT;
	/** Held in the other hand, bonds a ring to its user, or unbonds it (not used up). */
	public static final Item BOND_TOOL = Items.LEAD;
	public static final TagKey<Item> SPECIMENS = TagKey.create(Registries.ITEM, Jugcraft.id("artifice_specimens"));

	private static final Codec<RolledAffix> AFFIX = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("affix").forGetter(RolledAffix::affix), Codec.DOUBLE.fieldOf("value").forGetter(RolledAffix::value))
			.apply(i, RolledAffix::new));
	private static final Codec<Quality> QUALITY = Codec.STRING.xmap(id -> {
		Quality quality = Quality.fromId(id);
		return quality == null ? Quality.SOUND : quality;
	}, quality -> quality.id);
	public static final Codec<Artifice> ARTIFICE_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("substrate").forGetter(Artifice::substrate), QUALITY.fieldOf("quality").forGetter(Artifice::quality),
			Codec.LONG.fieldOf("seed").forGetter(Artifice::seed), Codec.INT.fieldOf("reforges").forGetter(Artifice::reforges),
			AFFIX.listOf().fieldOf("affixes").forGetter(Artifice::affixes), Codec.STRING.listOf().fieldOf("runes").forGetter(Artifice::runes),
			Codec.STRING.listOf().fieldOf("gems").forGetter(Artifice::gems),
			UUIDUtil.CODEC.optionalFieldOf("bond").forGetter(artifice -> Optional.ofNullable(artifice.bond())))
			.apply(i, (substrate, quality, seed, reforges, affixes, runes, gems, bond) -> new Artifice(substrate, quality, seed, reforges, affixes,
					runes, gems, bond.orElse(null))));

	public static DataComponentType<Artifice> ARTIFICE;
	public static Block ARTIFICER_BENCH;
	public static Item RESONANT_RING;

	/** Salvages waiting for their confirming second use: the player, the ring's state and when it lapses. */
	private record Pending(Artifice ring, long until) {
	}

	private static final Map<UUID, Pending> PENDING = new HashMap<>();

	private Artificery() {
	}

	public static void register() {
		ARTIFICE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("artifice"),
				DataComponentType.<Artifice>builder().persistent(ARTIFICE_CODEC).networkSynchronized(ByteBufCodecs.fromCodec(ARTIFICE_CODEC)).build());
		ResourceKey<Block> benchKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("artificer_bench"));
		ARTIFICER_BENCH = Registry.register(BuiltInRegistries.BLOCK, benchKey, new ArtificerBenchBlock(BlockBehaviour.Properties.of()
				.mapColor(MapColor.STONE).strength(3.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().setId(benchKey)));
		ResourceKey<Item> benchItemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("artificer_bench"));
		Item bench = Registry.register(BuiltInRegistries.ITEM, benchItemKey,
				new SkyItem(ARTIFICER_BENCH, new Item.Properties().useBlockDescriptionPrefix().setId(benchItemKey)));
		ResourceKey<Item> ringKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("resonant_ring"));
		RESONANT_RING = Registry.register(BuiltInRegistries.ITEM, ringKey, new ResonantRingItem(new Item.Properties().durability(250).setId(ringKey)));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(bench));
	}

	public static ArtificeCatalog catalog() {
		return ConcordanceData.rules().artifice();
	}

	public static boolean enabled() {
		return JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
	}

	public static boolean knows(Player player) {
		return ConcordanceProgress.knowledge(player).state(RESEARCH).atLeast(ResearchState.UNDERSTOOD);
	}

	public static String id(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	/** Whether a worn ring is at its last point: it gives nothing until repaired, and never breaks. */
	public static boolean dull(ItemStack ring) {
		return ring.getMaxDamage() > 0 && ring.getDamageValue() >= ring.getMaxDamage() - 1;
	}

	// ---------------------------------------------------------------- processes at the bench

	/** Forges a ring from the substrate in the main hand: decided, saved on the ring and given before it is described. */
	public static void forge(ServerPlayer player, ServerLevel level, ItemStack stock, Substrate substrate) {
		if (stock.getCount() < substrate.cost()) {
			refuse(player, "not_enough");
			return;
		}
		if (!ConcordanceProgress.spendFocus(player, FORGE_FOCUS)) {
			refuse(player, "no_focus");
			return;
		}
		Artifice artifice = Forge.forge(substrate, catalog(), level.getRandom().nextLong());
		ItemStack ring = new ItemStack(RESONANT_RING);
		ring.set(ARTIFICE, artifice);
		ring.set(DataComponents.MAX_DAMAGE, substrate.durability());
		ring.set(DataComponents.DAMAGE, 0);
		stock.shrink(substrate.cost());
		player.getInventory().placeItemBackInInventory(ring, Prediction.SERVER_ONLY);
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.artifice.forged", describeLine(ring, artifice)));
	}

	/** One enhancement of the ring in {@code ring}, chosen by what the other hand holds. */
	public static void enhance(ServerPlayer player, ServerLevel level, ItemStack ring, ItemStack other) {
		Artifice artifice = ring.get(ARTIFICE);
		ArtificeCatalog catalog = catalog();
		Substrate substrate = artifice == null ? null : catalog.substrate(artifice.substrate());
		if (artifice == null || substrate == null) {
			refuse(player, "unknown");
			return;
		}
		if (other.isEmpty()) {
			inspect(player, ring, artifice);
			return;
		}
		if (other.is(BOND_TOOL)) {
			bond(player, ring, artifice, substrate, catalog);
			return;
		}
		String item = id(other.getItem());
		Gem gem = catalog.gemOf(item);
		Rune rune = catalog.runeOf(item);
		if (other.is(REFORGE_CATALYST)) {
			if (!ConcordanceProgress.spendFocus(player, REFORGE_FOCUS)) {
				refuse(player, "no_focus");
				return;
			}
			done(player, ring, Forge.reforge(artifice, substrate, catalog).artifice(), "reforge", other);
		} else if (rune != null) {
			Forge.Result result = Forge.inscribe(artifice, substrate, rune, catalog);
			if (!result.done()) {
				refuse(player, result.refusal());
			} else if (!ConcordanceProgress.spendFocus(player, INSCRIBE_FOCUS)) {
				refuse(player, "no_focus");
			} else {
				done(player, ring, result.artifice(), "inscribe", other);
			}
		} else if (gem != null) {
			Forge.Result result = Forge.socket(artifice, substrate, gem, catalog);
			if (!result.done()) {
				refuse(player, result.refusal());
			} else {
				done(player, ring, result.artifice(), "socket", other);
			}
		} else if (other.is(UNSOCKET_TOOL)) {
			Forge.Removal removal = Forge.unsocket(artifice);
			Gem removed = removal.gem() == null ? null : catalog.gem(removal.gem());
			if (removal.artifice() == null || removed == null) {
				refuse(player, removal.refusal().isEmpty() ? "no_gem" : removal.refusal());
				return;
			}
			ring.set(ARTIFICE, removal.artifice());
			ItemStack back = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(removed.item())));
			player.getInventory().placeItemBackInInventory(back, Prediction.SERVER_ONLY);
			other.hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.artifice.gem_back", back.getHoverName()));
		} else if (item.equals(substrate.item())) {
			repair(player, ring, substrate, other);
		} else if (other.is(SALVAGE_TOOL)) {
			salvage(player, level, ring, artifice, substrate);
		}
	}

	private static void bond(ServerPlayer player, ItemStack ring, Artifice artifice, Substrate substrate, ArtificeCatalog catalog) {
		if (artifice.bond() != null) {
			Forge.Result result = Forge.unbond(artifice, substrate, player.getUUID(), catalog);
			if (!result.done()) {
				refuse(player, result.refusal());
				return;
			}
			done(player, ring, result.artifice(), "unbond", ItemStack.EMPTY);
			return;
		}
		Forge.Result result = Forge.bond(artifice, player.getUUID());
		if (!result.done()) {
			refuse(player, result.refusal());
		} else if (!ConcordanceProgress.spendFocus(player, BOND_FOCUS)) {
			refuse(player, "no_focus");
		} else {
			done(player, ring, result.artifice(), "bond", ItemStack.EMPTY);
		}
	}

	private static void repair(ServerPlayer player, ItemStack ring, Substrate substrate, ItemStack stock) {
		int damage = ring.getDamageValue();
		if (damage <= 0) {
			refuse(player, "nothing_to_repair");
			return;
		}
		int used = Math.min(Forge.repairItems(damage, substrate), stock.getCount());
		ring.setDamageValue(Math.max(0, damage - used * substrate.repair()));
		stock.shrink(used);
		ConcordanceProgress.record(player, new Evidence.Practiced(ACTIVITY, "repair"));
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.artifice.repaired", ring.getDamageValue(), ring.getMaxDamage()));
	}

	/** Salvage: the first use says what comes back and what is destroyed; a second use within the confirmation window does it. */
	private static void salvage(ServerPlayer player, ServerLevel level, ItemStack ring, Artifice artifice, Substrate substrate) {
		Forge.Salvage salvage = Forge.salvage(artifice, substrate);
		Item stock = BuiltInRegistries.ITEM.getValue(Identifier.parse(salvage.substrate()));
		long now = level.getGameTime();
		Pending pending = PENDING.remove(player.getUUID());
		if (pending == null || pending.until() < now || !pending.ring().equals(artifice)) {
			PENDING.put(player.getUUID(), new Pending(artifice, now + CONFIRM_TICKS));
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.artifice.confirm", salvage.count(),
					new ItemStack(stock).getHoverName(), salvage.gems().size()));
			return;
		}
		ring.shrink(1);
		if (salvage.count() > 0) {
			player.getInventory().placeItemBackInInventory(new ItemStack(stock, salvage.count()), Prediction.SERVER_ONLY);
		}
		for (String id : salvage.gems()) {
			Gem gem = catalog().gem(id);
			if (gem != null) {
				player.getInventory().placeItemBackInInventory(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(gem.item()))),
						Prediction.SERVER_ONLY);
			}
		}
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.artifice.salvaged", salvage.count(),
				new ItemStack(stock).getHoverName(), salvage.gems().size()));
	}

	/** Writes the ring's next state, takes one of what the other hand holds (if anything), records the practice, then tells. */
	private static void done(ServerPlayer player, ItemStack ring, @Nullable Artifice next, String process, ItemStack spent) {
		if (next == null) {
			return;
		}
		ring.set(ARTIFICE, next);
		if (!spent.isEmpty()) {
			spent.shrink(1);
		}
		ConcordanceProgress.record(player, new Evidence.Practiced(ACTIVITY, process));
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.artifice.done",
				Component.translatable("compose.jugcraft.artifice.process." + process), describeLine(ring, next)));
	}

	private static void refuse(ServerPlayer player, String reason) {
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.artifice.refused",
				Component.translatable("compose.jugcraft.artifice.refusal." + reason)));
	}

	// ---------------------------------------------------------------- describing

	/** The ring's summary line: quality, substrate, capacity used and wear. */
	public static Component describeLine(ItemStack ring, Artifice artifice) {
		ArtificeCatalog catalog = catalog();
		Substrate substrate = catalog.substrate(artifice.substrate());
		int capacity = substrate == null ? 0 : Forge.capacity(artifice, substrate);
		return Component.translatable("message.jugcraft.concordance.artifice.ring",
				Component.translatable("compose.jugcraft.artifice.quality." + artifice.quality().id),
				Component.translatable("compose.jugcraft.artifice.substrate." + path(artifice.substrate())), Forge.used(artifice, catalog), capacity,
				ring.getDamageValue(), ring.getMaxDamage());
	}

	/** Everything about the ring, then what each process keeps and destroys. */
	public static void inspect(ServerPlayer player, ItemStack ring, Artifice artifice) {
		lines(ring, artifice, player::sendSystemMessage);
		for (Forge.Process process : Forge.Process.values()) {
			if (process == Forge.Process.FORGE) {
				continue;
			}
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.artifice.keeps",
					Component.translatable("compose.jugcraft.artifice.process." + process.id), parts(process.keeps), parts(process.destroys)));
		}
	}

	public static void lines(ItemStack ring, Artifice artifice, Consumer<Component> out) {
		out.accept(describeLine(ring, artifice));
		ArtificeCatalog catalog = catalog();
		for (RolledAffix rolled : artifice.affixes()) {
			var affix = catalog.affix(rolled.affix());
			out.accept(Component.translatable("message.jugcraft.concordance.artifice.affix",
					Component.translatable("compose.jugcraft.artifice.affix." + path(rolled.affix())),
					affix == null ? Component.literal(format(rolled.value())) : amount(affix.attribute(), affix.operation(), rolled.value())));
		}
		for (String rune : artifice.runes()) {
			out.accept(Component.translatable("message.jugcraft.concordance.artifice.rune", Component.translatable("compose.jugcraft.artifice.rune." + path(rune))));
		}
		for (String gem : artifice.gems()) {
			out.accept(Component.translatable("message.jugcraft.concordance.artifice.gem", Component.translatable("compose.jugcraft.artifice.gem." + path(gem))));
		}
		if (artifice.bond() != null) {
			out.accept(Component.translatable("message.jugcraft.concordance.artifice.bond", artifice.bond().toString().substring(0, 8)));
		}
		if (dull(ring)) {
			out.accept(Component.translatable("message.jugcraft.concordance.artifice.dull"));
		}
	}

	private static Component amount(String attribute, Stat.Operation operation, double value) {
		String shown = operation == Stat.Operation.ADD_VALUE ? "+" + format(value) : "+" + format(value * 100.0) + "%";
		return Component.translatable("compose.jugcraft.artifice.attribute." + attribute.replace(':', '.')).append(" " + shown);
	}

	private static Component parts(java.util.Set<Forge.Part> parts) {
		if (parts.isEmpty()) {
			return Component.translatable("compose.jugcraft.artifice.nothing");
		}
		Component out = Component.empty();
		boolean first = true;
		for (Forge.Part part : parts) {
			if (!first) {
				out = out.copy().append(", ");
			}
			out = out.copy().append(Component.translatable("compose.jugcraft.artifice.part." + part.name().toLowerCase(Locale.ROOT)));
			first = false;
		}
		return out;
	}

	static String format(double value) {
		return value == Math.rint(value) ? Long.toString((long) value) : String.format(Locale.ROOT, "%.2f", value);
	}

	static String path(String id) {
		int colon = id.indexOf(':');
		return colon < 0 ? id : id.substring(colon + 1);
	}

	/** Modifier operations by the data's names. */
	static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation(Stat.Operation operation) {
		return switch (operation) {
			case ADD_VALUE -> net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;
			case ADD_MULTIPLIED_BASE -> net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
			case ADD_MULTIPLIED_TOTAL -> net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
		};
	}

	/** Every stat a ring gives {@code wearer} now (none when dull or bonded to someone else). */
	public static List<Stat> stats(ItemStack ring, UUID wearer) {
		Artifice artifice = ring.get(ARTIFICE);
		if (artifice == null || dull(ring) || !Forge.serves(artifice, wearer)) {
			return List.of();
		}
		return Forge.stats(artifice, catalog());
	}
}
