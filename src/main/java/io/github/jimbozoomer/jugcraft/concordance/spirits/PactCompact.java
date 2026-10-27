package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.concordance.sky.AstrolabeItem;
import io.github.jimbozoomer.jugcraft.concordance.sky.Sky;
import io.github.jimbozoomer.jugcraft.concordance.worker.PactDefinition;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D, D-04 (D-S13): the Open Hand Compact, what accepting a pact costs: ordinary materials and Astral
 * Resonance from an astrolabe the player carries. It is checked and spent only through the seal itself
 * (AgreementHolder.Sealing's affords, before anything is called; pay, after the Bound Will is sealed), so a refusal
 * spends nothing and an acceptance spends once. Resonance is only spent here, never made. A player with infinite
 * materials (creative) pays nothing, as vanilla's crafting does (proposed in unit 2); spending is shown by
 * TestPlayers.survivor.
 */
public final class PactCompact {
	public record Need(Item item, int count) {
	}

	private PactCompact() {
	}

	/** The definition's materials as items; null if one is not registered (the definition is then unusable). */
	public static @Nullable List<Need> needs(PactDefinition definition) {
		List<Need> needs = new ArrayList<>();
		for (PactDefinition.Cost cost : definition.compact()) {
			Identifier id = Identifier.tryParse(cost.item());
			Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
			if (item == null || item == Items.AIR) {
				return null;
			}
			needs.add(new Need(item, cost.count()));
		}
		return List.copyOf(needs);
	}

	public static boolean affords(ServerPlayer player, List<Need> needs, int resonance) {
		if (player.hasInfiniteMaterials()) {
			return true;
		}
		for (Need need : needs) {
			if (player.getInventory().countItem(need.item()) < need.count()) {
				return false;
			}
		}
		return resonance <= 0 || astrolabe(player, resonance) != null;
	}

	public static void pay(ServerPlayer player, List<Need> needs, int resonance) {
		if (player.hasInfiniteMaterials()) {
			return;
		}
		Inventory inventory = player.getInventory();
		for (Need need : needs) {
			int left = need.count();
			for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (stack.is(need.item())) {
					int take = Math.min(left, stack.getCount());
					stack.shrink(take);
					left -= take;
				}
			}
		}
		ItemStack astrolabe = resonance <= 0 ? null : astrolabe(player, resonance);
		if (astrolabe != null) {
			AstrolabeItem.setCharge(astrolabe, AstrolabeItem.charge(astrolabe) - resonance);
		}
		inventory.setChanged();
	}

	/**
	 * The first astrolabe the player carries holding at least {@code least} (0: any): main hand, off hand, then the inventory
	 * in slot order. Star-reckoning (D-05) picks its paying astrolabe the same way.
	 */
	static @Nullable ItemStack astrolabe(ServerPlayer player, int least) {
		List<ItemStack> order = new ArrayList<>(List.of(player.getMainHandItem(), player.getOffhandItem()));
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			order.add(inventory.getItem(slot));
		}
		for (ItemStack stack : order) {
			if (stack.is(Sky.ASTROLABE) && AstrolabeItem.charge(stack) >= least) {
				return stack;
			}
		}
		return null;
	}
}
