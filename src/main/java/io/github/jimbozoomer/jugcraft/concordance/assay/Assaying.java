package io.github.jimbozoomer.jugcraft.concordance.assay;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.CycleAudit;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Eligibility;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.EquivalenceCatalog;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Exact;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Material;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Transmutation;
import io.github.jimbozoomer.jugcraft.concordance.resource.Overflow;
import io.github.jimbozoomer.jugcraft.concordance.resource.Reservoir;
import io.github.jimbozoomer.jugcraft.concordance.resource.ResourceKind;
import io.github.jimbozoomer.jugcraft.concordance.resource.ResourceType;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sky.SkyItem;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.jspecify.annotations.Nullable;

/**
 * Bounded material equivalence on the server (roadmap step 21, docs/features/arcane-concordance-equivalence.md). An
 * Assayer's Scale weighs a held stack against the catalogue: if it is eligible ({@link Eligibility}, judged from what
 * the stack really carries) it says its worth, and a second use within {@value #CONFIRM_TICKS} ticks dissolves it into
 * its user's Prima Materia ledger, the exact value rounded down; with an empty main hand, the material held in the other
 * hand is the scale's pattern (a catalyst, kept) and one more of it is formed for its price, rounded up. The ledger is a
 * {@link Reservoir} of Prima Materia moved by the shared resource rules: all or nothing, never past
 * {@value Assay#MAX_BALANCE} grains, never below empty.
 * <p>
 * The scale fails closed: while the catalogue has problems (a recipe or cycle that gains value, an unvalued item, an
 * item whose very nature carries an inventory, a creature or magic, an excluded item catalogued, or a recipe this
 * server really has that makes a catalogued material worth more than went in: {@link #probe}), it weighs nothing.
 */
public final class Assaying {
	public static final String RESEARCH = "jugcraft:assay";
	/** The practice dissolving records, with the material as its detail: five different materials master Assay. */
	public static final String ACTIVITY = "jugcraft:assay";
	public static final int CONFIRM_TICKS = 200;
	public static final ResourceType PRIMA = ResourceType.of(ResourceKind.PRIMA_MATERIA);
	public static final TagKey<Item> SPECIMENS = TagKey.create(Registries.ITEM, Jugcraft.id("assay_specimens"));
	public static final TagKey<Item> EXCLUDED = TagKey.create(Registries.ITEM, Jugcraft.id("equivalence/excluded"));

	/** A player's Prima Materia, in grains (saved with them, kept through death). */
	public static AttachmentType<Long> LEDGER;
	public static Block SCALE;

	/** A weighing waiting for its confirming second use: the item and count weighed, and when it lapses. */
	private record Pending(String item, int count, long until) {
	}

	private static final Map<UUID, Pending> PENDING = new HashMap<>();
	/** The problems of the catalogue last checked, and which catalogue that was (it changes on reload). */
	private static @Nullable EquivalenceCatalog checked;
	private static List<String> problems = List.of();

	private Assaying() {
	}

	public static void register() {
		LEDGER = AttachmentRegistry.<Long>builder().persistent(Saved.versioned("prima_ledger", Codec.LONG)).copyOnDeath().buildAndRegister(Jugcraft.id("prima_ledger"));
		ResourceKey<Block> scaleKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("assayers_scale"));
		SCALE = Registry.register(BuiltInRegistries.BLOCK, scaleKey, new AssayersScaleBlock(BlockBehaviour.Properties.of()
				.mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD).noOcclusion().setId(scaleKey)));
		ResourceKey<Item> scaleItemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("assayers_scale"));
		Item scale = Registry.register(BuiltInRegistries.ITEM, scaleItemKey,
				new SkyItem(SCALE, new Item.Properties().useBlockDescriptionPrefix().setId(scaleItemKey)));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PENDING.remove(handler.getPlayer().getUUID()));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> command(dispatcher));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(scale));
	}

	public static EquivalenceCatalog catalog() {
		return ConcordanceData.rules().equivalence();
	}

	public static boolean enabled() {
		return JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
	}

	public static boolean knows(Player player) {
		return ConcordanceProgress.knowledge(player).state(RESEARCH).atLeast(ResearchState.UNDERSTOOD);
	}

	public static long balance(Player player) {
		Long grains = player.getAttached(LEDGER);
		return grains == null ? 0L : Math.clamp(grains, 0L, Assay.MAX_BALANCE);
	}

	/** Operators and tests: sets a player's ledger. */
	public static void setBalance(Player player, long grains) {
		player.setAttached(LEDGER, Math.clamp(grains, 0L, Assay.MAX_BALANCE));
	}

	// ---------------------------------------------------------------- what a stack is

	/**
	 * The components {@code stack} adds to, removes from or changes on its item's own, by id: every registered component
	 * whose value on the stack differs from the item's default.
	 */
	public static Set<String> patched(ItemStack stack) {
		Set<String> ids = new TreeSet<>();
		DataComponentMap defaults = stack.getItem().components();
		for (DataComponentType<?> type : BuiltInRegistries.DATA_COMPONENT_TYPE) {
			if (!Objects.equals(stack.get(type), defaults.get(type))) {
				ids.add(componentId(type));
			}
		}
		return ids;
	}

	/**
	 * The components an item has by its nature: those whose default value differs from a plain stick's. Every item carries
	 * the common defaults (vanilla gives each an empty enchantment list, for one), and those say nothing about it.
	 */
	public static Set<String> distinctive(Item item) {
		DataComponentMap own = item.components();
		DataComponentMap plain = Items.STICK.components();
		Set<String> ids = new TreeSet<>();
		for (DataComponentType<?> type : BuiltInRegistries.DATA_COMPONENT_TYPE) {
			if (!Objects.equals(own.get(type), plain.get(type))) {
				ids.add(componentId(type));
			}
		}
		return ids;
	}

	private static String componentId(DataComponentType<?> type) {
		Identifier id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
		return id == null ? "unknown:unknown" : id.toString();
	}

	public static Eligibility.Specimen specimen(ItemStack stack) {
		return new Eligibility.Specimen(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), patched(stack),
				stack.is(EXCLUDED) ? Set.of(Eligibility.EXCLUDED_TAG) : Set.of());
	}

	/** Why {@code stack} may not be weighed, or the empty string. */
	public static String eligibility(ItemStack stack) {
		return Eligibility.check(specimen(stack), catalog());
	}

	/**
	 * Why the scale is out of balance, if it is: the catalogue's own problems; any catalogued item that is excluded or
	 * whose every stack carries an inventory, a creature or magic (an adapter cannot sneak one in); and the live
	 * recipes' gains ({@link #probe}). Checked again whenever the data reloads. Empty: balanced.
	 */
	public static synchronized List<String> problems(ServerLevel level) {
		EquivalenceCatalog catalog = catalog();
		if (catalog != checked) {
			List<String> found = new ArrayList<>(catalog.problems());
			found.addAll(probe(level, catalog));
			for (Material material : catalog.materials().values()) {
				Optional<Item> item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(material.item()));
				if (item.isEmpty()) {
					found.add("material " + material.item() + ": no such item");
					continue;
				}
				ItemStack plain = new ItemStack(item.get());
				String nature = Eligibility.nature(distinctive(item.get()));
				if (nature != null || plain.is(EXCLUDED)) {
					found.add("material " + material.item() + ": it may never be catalogued (" + (nature == null ? "excluded" : nature) + ")");
				}
			}
			problems = List.copyOf(found);
			checked = catalog;
		}
		return problems;
	}

	// ---------------------------------------------------------------- the scale

	/** One use of the scale (tests call this directly). */
	public static void use(ServerPlayer player, ServerLevel level, ItemStack main, ItemStack other) {
		if (!enabled()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.disabled"));
			return;
		}
		if (!knows(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.assay.unknown"));
			return;
		}
		if (!problems(level).isEmpty()) {
			player.sendOverlayMessage(Component.translatable("compose.jugcraft.assay.reason.unbalanced"));
			return;
		}
		if (!main.isEmpty()) {
			weigh(player, level, main);
		} else if (!other.isEmpty()) {
			form(player, other);
		} else {
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.assay.balance", balance(player), Assay.MAX_BALANCE));
		}
	}

	/** Weighs the held stack; the same stack weighed again within {@link #CONFIRM_TICKS} is dissolved. */
	private static void weigh(ServerPlayer player, ServerLevel level, ItemStack stack) {
		String reason = eligibility(stack);
		if (!reason.isEmpty()) {
			refuse(player, stack, reason);
			return;
		}
		String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		Material material = catalog().material(item);
		long now = level.getGameTime();
		Pending pending = PENDING.get(player.getUUID());
		if (pending != null && pending.item().equals(item) && pending.count() == stack.getCount() && now <= pending.until()
				&& material.dissolvable()) {
			PENDING.remove(player.getUUID());
			dissolve(player, stack, material);
			return;
		}
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.assay.value", stack.getHoverName(), material.exact().toString(),
				stack.getCount(), material.dissolvable() ? Assay.dissolve(material, stack.getCount()) : 0,
				material.formable() ? Assay.form(material, 1) : "-"));
		if (!material.dissolvable()) {
			refuse(player, stack, "not_dissolvable");
			return;
		}
		PENDING.put(player.getUUID(), new Pending(item, stack.getCount(), now + CONFIRM_TICKS));
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.assay.confirm"));
	}

	/**
	 * Dissolves as much of {@code stack} as the ledger has room for: the exact value of that many, rounded down once,
	 * into the ledger by the shared resource rules (all of it or none); then the items are taken. Returns the grains.
	 */
	public static long dissolve(ServerPlayer player, ItemStack stack, Material material) {
		long room = Assay.MAX_BALANCE - balance(player);
		int count = Math.min(stack.getCount(), Assay.MAX_BATCH);
		while (count > 0 && Assay.dissolve(material, count) > room) {
			count--;
		}
		if (count == 0) {
			refuse(player, stack, "full");
			return 0;
		}
		long grains = Assay.dissolve(material, count);
		Reservoir.Insertion insertion = new Reservoir(PRIMA, balance(player), Assay.MAX_BALANCE).insert(PRIMA, grains, Overflow.REJECT);
		if (insertion.outcome() != Reservoir.Outcome.DONE) {
			refuse(player, stack, "full");
			return 0;
		}
		Component name = stack.getHoverName();
		stack.shrink(count);
		setBalance(player, insertion.reservoir().amount());
		ConcordanceProgress.record(player, new Evidence.Practiced(ACTIVITY, material.item()));
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.assay.dissolved", count, name, grains, balance(player)));
		return grains;
	}

	/** Forms one more of the pattern held in the other hand (the pattern is kept): its price, rounded up, from the ledger. */
	public static boolean form(ServerPlayer player, ItemStack pattern) {
		String reason = eligibility(pattern);
		if (!reason.isEmpty()) {
			refuse(player, pattern, reason);
			return false;
		}
		Material material = catalog().material(BuiltInRegistries.ITEM.getKey(pattern.getItem()).toString());
		if (!material.formable()) {
			refuse(player, pattern, "not_formable");
			return false;
		}
		long cost = Assay.form(material, 1);
		Reservoir.Extraction extraction = new Reservoir(PRIMA, balance(player), Assay.MAX_BALANCE).extract(PRIMA, cost, true);
		if (extraction.outcome() != Reservoir.Outcome.DONE) {
			refuse(player, pattern, "too_poor");
			return false;
		}
		setBalance(player, extraction.reservoir().amount());
		ItemStack formed = new ItemStack(pattern.getItem());
		Component name = formed.getHoverName();
		player.getInventory().placeItemBackInInventory(formed, Prediction.SERVER_ONLY);
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.assay.formed", name, cost, balance(player)));
		return true;
	}

	private static void refuse(ServerPlayer player, ItemStack stack, String reason) {
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.assay.refused", stack.getHoverName(),
				Component.translatable("compose.jugcraft.assay.reason." + reason)));
	}

	// ---------------------------------------------------------------- the diagnostic

	/** The shapes a probe tries in a crafting grid: one, a column of two, two by two, three by three. */
	private static final int[][] SHAPES = {{1, 1}, {1, 2}, {2, 2}, {3, 3}};

	/**
	 * Probes this server's real recipes (vanilla's, other mods' and datapacks') with each catalogued material: one, a
	 * column of two, a 2x2 and a 3x3 of it in a crafting grid, and one in a furnace (allowed an eighth of a coal's worth
	 * for fuel). Any that makes catalogued materials (with what the grid gives back) worth more than went in is a
	 * problem. The declared graph is the catalogue's promise; this is the check that the game keeps it.
	 */
	public static List<String> probe(ServerLevel level, EquivalenceCatalog catalog) {
		List<String> found = new ArrayList<>();
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		Material coal = catalog.material("minecraft:coal");
		Exact fuel = coal == null ? Exact.ZERO : coal.exact().divide(Exact.of(8));
		for (Material material : catalog.materials().values()) {
			Optional<Item> item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(material.item()));
			if (item.isEmpty()) {
				continue;
			}
			for (int[] shape : SHAPES) {
				int count = shape[0] * shape[1];
				List<ItemStack> grid = new ArrayList<>();
				for (int i = 0; i < count; i++) {
					grid.add(new ItemStack(item.get()));
				}
				CraftingInput input = CraftingInput.of(shape[0], shape[1], grid);
				crafting.getRecipeFor(input, level).ifPresent(holder -> {
					List<ItemStack> made = new ArrayList<>(holder.value().getRemainingItems(input));
					made.add(holder.value().assemble(input));
					gain(catalog, material, count, Exact.ZERO, made, "crafting " + count, found);
				});
			}
			SingleRecipeInput single = new SingleRecipeInput(new ItemStack(item.get()));
			level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING, single, level)
					.ifPresent(holder -> gain(catalog, material, 1, fuel, List.of(holder.value().assemble(single)), "smelting", found));
		}
		return found;
	}

	private static void gain(EquivalenceCatalog catalog, Material input, int count, Exact extra, List<ItemStack> made, String how,
			List<String> found) {
		Exact out = Exact.ZERO;
		for (ItemStack stack : made) {
			if (!stack.isEmpty()) {
				Exact value = catalog.value(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
				if (value != null) {
					out = out.plus(value.times(stack.getCount()));
				}
			}
		}
		Exact in = input.exact().times(count).plus(extra);
		if (out.compareTo(in) > 0) {
			found.add("live recipe (" + how + " " + input.item() + "): gives " + out + " grains of matter for " + in);
		}
	}

	/** The audit's findings: the catalogue's problems and the search over its graph and the scale's conversions. */
	public static List<String> audit(ServerLevel level) {
		List<String> lines = new ArrayList<>(problems(level));
		for (CycleAudit.Finding finding : CycleAudit.audit(catalog())) {
			String line = finding.toString();
			if (!lines.contains("equivalence: " + line)) {
				lines.add(line);
			}
		}
		return lines;
	}

	private static void command(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jugcraft").then(Commands.literal("concordance").then(Commands.literal("equivalence")
				.then(Commands.literal("audit").requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions())).executes(context -> {
					List<String> findings = audit(context.getSource().getLevel());
					CommandSourceStack source = context.getSource();
					if (findings.isEmpty()) {
						EquivalenceCatalog catalog = catalog();
						List<Transmutation> all = new ArrayList<>(catalog.transmutations().values());
						all.addAll(Assay.conversions(catalog));
						List<Transmutation> recipes = all.stream().filter(t -> t.kind() == Transmutation.Kind.RECIPE).toList();
						int cycles = CycleAudit.cycles(catalog, recipes, CycleAudit.MAX_LENGTH, CycleAudit.MAX_CYCLES).size();
						source.sendSuccess(() -> Component.translatable("message.jugcraft.concordance.assay.audit_clean", all.size(), cycles), false);
						return 1;
					}
					for (String finding : findings) {
						source.sendSuccess(() -> Component.translatable("message.jugcraft.concordance.assay.audit_finding", finding), false);
					}
					return 0;
				}))
				.then(Commands.literal("balance").executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					context.getSource().sendSuccess(() -> Component.translatable("message.jugcraft.concordance.assay.balance", balance(player),
							Assay.MAX_BALANCE), false);
					return 1;
				}))
				.then(Commands.literal("value").executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					ItemStack held = player.getMainHandItem();
					String reason = eligibility(held);
					if (held.isEmpty() || !reason.isEmpty()) {
						context.getSource().sendFailure(Component.translatable("message.jugcraft.concordance.assay.refused", held.getHoverName(),
								Component.translatable("compose.jugcraft.assay.reason." + (held.isEmpty() ? "uncatalogued" : reason))));
						return 0;
					}
					Material material = catalog().material(BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
					context.getSource().sendSuccess(() -> Component.translatable("message.jugcraft.concordance.assay.value", held.getHoverName(),
							material.exact().toString(), held.getCount(), material.dissolvable() ? Assay.dissolve(material, held.getCount()) : 0,
							material.formable() ? Assay.form(material, 1) : "-"), false);
					return 1;
				})))));
	}
}
