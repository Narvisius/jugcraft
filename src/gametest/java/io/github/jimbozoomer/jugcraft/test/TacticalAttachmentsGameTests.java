package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * In-game tests for the Tactical Grip and the Laser Sight (slice 9E, docs/features/guns.md): which guns take them, and
 * what they change. The Laser Sight's dot is drawn on the client only (GunsClientGameTests).
 */
public class TacticalAttachmentsGameTests {
	/**
	 * The guns the owner made tactical grip parts for (slice 10A's launchers, slice 10B's rifles, slice 10D's Reverb and
	 * slices 11A's and 11B's Nether guns among them).
	 */
	static final List<String> GRIPPED = List.of("drover_rifle", "coach_gun", "garrison_rifle", "breacher", "picket_rifle",
			"ranger_rifle", "kestrel_rifle", "squall_rifle", "sledge", "highwayman", "earthmover", "skylark_rifle", "bullfrog",
			"solenoid_rifle", "votive_rifle", "reverb", "tusker", "cinder_repeater", "bastion_rifle", "goldbristle_carbine",
			"crackling");
	/** The guns that take the scopes but not the Laser Sight (tools/guns.py ATTACHMENTS laser_sight "not_on"). */
	static final List<String> NO_LASER = List.of("breacher", "trench_lobber", "earthmover");

	/**
	 * Both recipes load. The Tactical Grip fits the twenty-one guns the owner made tactical grip parts for and no other.
	 * The Laser Sight fits every gun that takes the scopes but the Breacher, the Trench Lobber and the Earthmover.
	 */
	@GameTest
	public void tacticalAttachmentsFitTheirGuns(GameTestHelper helper) {
		for (String name : List.of("tactical_grip", "laser_sight")) {
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
		}
		for (String gun : JugcraftGuns.SPECS.keySet()) {
			List<String> takes = JugcraftGuns.ACCEPTS.getOrDefault(gun, List.of());
			helper.assertTrue(takes.contains("tactical_grip") == GRIPPED.contains(gun),
					"The " + gun + (GRIPPED.contains(gun) ? " does not take" : " takes") + " the Tactical Grip");
			boolean laser = takes.contains("reflex_sight") && !NO_LASER.contains(gun);
			helper.assertTrue(takes.contains(JugcraftGuns.LASER_SIGHT) == laser,
					"The " + gun + (laser ? " does not take" : " takes") + " the Laser Sight");
		}
		helper.succeed();
	}

	/**
	 * On a Kestrel Rifle the Tactical Grip leaves 90% of the spread from the hip and 80% of the kick; the Laser Sight,
	 * beside it in the scope's slot, 70% of that spread again, the aimed spread as it was. A Long Scope takes the Laser
	 * Sight's place, leaving it in the grid. The Breacher takes no Laser Sight, nor the Patchwork Carbine a Tactical Grip.
	 */
	@GameTest
	public void tacticalAttachmentsSteadyTheGun(GameTestHelper helper) {
		GunSpec kestrel = JugcraftGuns.SPECS.get("kestrel_rifle");
		Crafted gripped = craft(helper, new ItemStack(JugcraftGuns.GUNS.get("kestrel_rifle")), attachment("tactical_grip"));
		GunSpec spec = GunItem.spec(gripped.result());
		helper.assertTrue(GunItem.attachments(gripped.result()).equals(List.of("tactical_grip")), "The Tactical Grip did not fit the Kestrel Rifle");
		helper.assertTrue(Math.abs(spec.hipSpread() - kestrel.hipSpread() * 0.9F) < 1.0E-4F && Math.abs(spec.aimSpread() - kestrel.aimSpread()) < 1.0E-4F
				&& Math.abs(GunItem.kick(gripped.result()) - 0.8F) < 1.0E-4F,
				"With the Tactical Grip: spread " + spec.hipSpread() + " from the hip, " + spec.aimSpread() + " aimed, kick " + GunItem.kick(gripped.result()));

		Crafted lasered = craft(helper, gripped.result(), attachment(JugcraftGuns.LASER_SIGHT));
		spec = GunItem.spec(lasered.result());
		helper.assertTrue(GunItem.attachments(lasered.result()).equals(List.of("tactical_grip", JugcraftGuns.LASER_SIGHT)),
				"The Laser Sight did not fit beside the Tactical Grip: " + GunItem.attachments(lasered.result()));
		helper.assertTrue(Math.abs(spec.hipSpread() - kestrel.hipSpread() * 0.9F * 0.7F) < 1.0E-4F
				&& Math.abs(spec.aimSpread() - kestrel.aimSpread()) < 1.0E-4F,
				"With the Tactical Grip and the Laser Sight: spread " + spec.hipSpread() + " from the hip, " + spec.aimSpread() + " aimed");

		Crafted scoped = craft(helper, lasered.result(), attachment("long_scope"));
		helper.assertTrue(GunItem.attachments(scoped.result()).equals(List.of("tactical_grip", "long_scope"))
				&& scoped.left().get(1).is(JugcraftGuns.ATTACHMENT_ITEMS.get(JugcraftGuns.LASER_SIGHT)),
				"The Long Scope did not take the Laser Sight's place, leaving it in the grid: " + GunItem.attachments(scoped.result()));

		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		helper.assertFalse(crafting.getRecipeFor(grid(new ItemStack(JugcraftGuns.GUNS.get("breacher")), attachment(JugcraftGuns.LASER_SIGHT)),
				helper.getLevel()).isPresent(), "The Breacher took a Laser Sight");
		helper.assertFalse(crafting.getRecipeFor(grid(new ItemStack(JugcraftGuns.GUNS.get("patchwork_carbine")), attachment("tactical_grip")),
				helper.getLevel()).isPresent(), "The Patchwork Carbine took a Tactical Grip");
		helper.succeed();
	}

	/** What a crafting grid gave, and what stayed in it. */
	private record Crafted(ItemStack result, NonNullList<ItemStack> left) {
	}

	/** Crafts a gun and another item side by side in a 2 x 1 grid (the test fails if no recipe takes them). */
	private static Crafted craft(GameTestHelper helper, ItemStack gun, ItemStack other) {
		CraftingInput input = grid(gun, other);
		Optional<RecipeHolder<CraftingRecipe>> recipe = RecipeManager.createCheck(RecipeType.CRAFTING).getRecipeFor(input, helper.getLevel());
		helper.assertTrue(recipe.isPresent(), "No recipe takes " + gun + " and " + other);
		return new Crafted(recipe.get().value().assemble(input), recipe.get().value().getRemainingItems(input));
	}

	private static CraftingInput grid(ItemStack first, ItemStack second) {
		return CraftingInput.of(2, 1, List.of(first.copy(), second.copy()));
	}

	private static ItemStack attachment(String name) {
		return new ItemStack(JugcraftGuns.ATTACHMENT_ITEMS.get(name));
	}
}
