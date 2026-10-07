package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.gear.TraitTooltips;
import io.github.jimbozoomer.jugcraft.weapons.ArmItem;
import io.github.jimbozoomer.jugcraft.weapons.ArmVariants;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftArms;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * In-game tests for the trait details on Shift (docs/features/trait-details.md): the wrapping of descriptions; every
 * arm's tooltip folded (its traits' names and a "hold Shift" line) and expanded (each name with its description under
 * it, indented, and no "hold Shift" line); a variant's five traits; a boss trophy's line, a name with no description;
 * and the bows, crossbows and shields through their own tooltips. The texts are compared as the server's language
 * renders them, so the test holds whether or not it has the mod's English.
 */
public class TraitDetailsGameTests {
	private static final String INDENT = "  ";

	/** Descriptions wrap at spaces to the width; a word longer than the width is cut; spaces collapse. */
	@GameTest
	public void descriptionsWrapAtSpaces(GameTestHelper helper) {
		check(helper, TraitTooltips.wrap("The quick brown fox jumps over the lazy dog", 10),
				List.of("The quick", "brown fox", "jumps over", "the lazy", "dog"));
		check(helper, TraitTooltips.wrap("abcdefghijkl mn", 5), List.of("abcde", "fghij", "kl mn"));
		check(helper, TraitTooltips.wrap("  a   b ", 10), List.of("a b"));
		check(helper, TraitTooltips.wrap("", 10), List.of());
		check(helper, TraitTooltips.wrap("exactly ten", 11), List.of("exactly ten"));
		helper.succeed();
	}

	/**
	 * Every arm, base and variant: folded, its names then the "hold Shift" line; expanded, the same names, each followed
	 * by its description's words, wrapped no wider than the width, and no "hold Shift" line.
	 */
	@GameTest
	public void everyArmFoldsAndExpandsItsTraits(GameTestHelper helper) {
		String hint = hint();
		List<Item> arms = new ArrayList<>(JugcraftArms.ITEMS.values());
		arms.addAll(ArmVariants.ITEMS.values());
		for (Item item : arms) {
			String id = item.toString();
			helper.assertTrue(item instanceof ArmItem, id + " is not an arm");
			ArmItem arm = (ArmItem) item;
			List<String> folded = lines(arm, false);
			List<String> expanded = lines(arm, true);
			helper.assertTrue(folded.size() >= 2 && folded.get(folded.size() - 1).equals(hint),
					id + ": folded, its tooltip does not end with the hold-Shift line: " + folded);
			List<String> names = folded.subList(0, folded.size() - 1);
			helper.assertTrue(names.stream().noneMatch(line -> line.startsWith(INDENT) || line.equals(hint)),
					id + ": folded, its tooltip shows a description: " + folded);
			helper.assertTrue(names.get(0).equals(Component.translatable("tooltip.jugcraft.arms." + arm.kind() + ".trait").getString()),
					id + ": its first trait is not its kind's: " + names.get(0));
			helper.assertTrue(!expanded.contains(hint), id + ": expanded, it still says to hold Shift: " + expanded);
			List<String> shownNames = expanded.stream().filter(line -> !line.startsWith(INDENT)).toList();
			helper.assertTrue(shownNames.equals(names), id + ": expanded names " + shownNames + " are not the folded " + names);
			helper.assertTrue(expanded.size() > names.size(), id + ": expanded, no description shows: " + expanded);
			for (String line : expanded) {
				helper.assertTrue(line.length() <= INDENT.length() + TraitTooltips.WIDTH,
						id + ": a description line is wider than " + TraitTooltips.WIDTH + ": \"" + line + "\"");
			}
			// The kind's description, word for word, under its name.
			String kind = Component.translatable("tooltip.jugcraft.arms." + arm.kind()).getString();
			helper.assertTrue(words(expanded, 1).equals(String.join(" ", kind.trim().split("\\s+"))),
					id + ": the kind's description does not follow its name: " + expanded);
		}
		helper.succeed();
	}

	/**
	 * A runebound nodachi has five traits (its kind, two-handed, its weapon art, the Mark boon and the Runebound line),
	 * each described; the Crypt Lich's soulreaver ends with its trophy line, a name with nothing under it.
	 */
	@GameTest
	public void variantsShowEveryTrait(GameTestHelper helper) {
		ArmItem nodachi = (ArmItem) ArmVariants.ITEMS.get("runebound_nodachi");
		List<String> folded = lines(nodachi, false);
		helper.assertTrue(folded.size() == 6, "The runebound nodachi should show 5 trait names and the hold-Shift line: " + folded);
		List<String> expected = List.of("tooltip.jugcraft.arms.nodachi.trait", "tooltip.jugcraft.arms.two_handed.trait",
				"tooltip.jugcraft.arms.art.iaido.trait", "tooltip.jugcraft.arms.boon.mark.trait",
				"tooltip.jugcraft.arms.line.runebound.trait");
		for (int i = 0; i < expected.size(); i++) {
			helper.assertTrue(folded.get(i).equals(Component.translatable(expected.get(i)).getString()),
					"The runebound nodachi's trait " + i + " is \"" + folded.get(i) + "\", not " + expected.get(i));
		}
		List<String> expanded = lines(nodachi, true);
		helper.assertTrue(expanded.get(expanded.size() - 1).startsWith(INDENT),
				"The Runebound line's description should end the expanded tooltip: " + expanded);

		ArmItem soulreaver = (ArmItem) ArmVariants.ITEMS.get("soulreaver");
		List<String> trophy = lines(soulreaver, true);
		String last = trophy.get(trophy.size() - 1);
		helper.assertTrue(last.equals(Component.translatable("tooltip.jugcraft.arms.line.crypt_lich.trait").getString()),
				"The soulreaver's expanded tooltip should end with its trophy line and nothing under it: " + trophy);
		helper.succeed();
	}

	/** The bows, crossbows and shields: their trait name and the hold-Shift line, and with Shift their description. */
	@GameTest
	public void theKitFoldsAndExpands(GameTestHelper helper) {
		String hint = hint();
		BooleanSupplier before = TraitTooltips.details;
		try {
			for (Item item : JugcraftArms.KIT.values()) {
				TraitTooltips.details = () -> false;
				List<String> folded = tooltip(item);
				TraitTooltips.details = () -> true;
				List<String> expanded = tooltip(item);
				helper.assertTrue(folded.contains(hint) && folded.stream().noneMatch(line -> line.startsWith(INDENT)),
						item + ": folded, it should show its name and the hold-Shift line only: " + folded);
				helper.assertTrue(!expanded.contains(hint) && expanded.stream().anyMatch(line -> line.startsWith(INDENT)),
						item + ": expanded, it should show its description: " + expanded);
			}
		} finally {
			TraitTooltips.details = before;
		}
		helper.succeed();
	}

	private static void check(GameTestHelper helper, List<String> got, List<String> expected) {
		helper.assertTrue(got.equals(expected), "wrap gave " + got + ", expected " + expected);
	}

	/** An arm's trait lines, folded or expanded, as the server's language renders them. */
	private static List<String> lines(ArmItem arm, boolean expanded) {
		List<Component> lines = new ArrayList<>();
		arm.traits(TraitTooltips.of(lines::add, expanded));
		return lines.stream().map(Component::getString).toList();
	}

	/** An item's own tooltip lines (appendHoverText), as the server's language renders them. */
	private static List<String> tooltip(Item item) {
		List<Component> lines = new ArrayList<>();
		item.appendHoverText(new ItemStack(item), Item.TooltipContext.EMPTY, TooltipDisplay.DEFAULT, lines::add, TooltipFlag.NORMAL);
		return lines.stream().map(Component::getString).toList();
	}

	private static String hint() {
		return Component.translatable("tooltip.jugcraft.hold_shift", Component.literal("Shift")).getString();
	}

	/** The words of the description lines that follow the name at {@code from}, joined by single spaces. */
	private static String words(List<String> lines, int from) {
		List<String> words = new ArrayList<>();
		for (int i = from; i < lines.size() && lines.get(i).startsWith(INDENT); i++) {
			words.add(lines.get(i).trim());
		}
		return String.join(" ", words);
	}
}
