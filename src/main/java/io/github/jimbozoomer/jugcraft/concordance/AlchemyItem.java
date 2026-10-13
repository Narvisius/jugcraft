package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.concordance.alchemy.Formula;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Alchemy's simpler items (roadmap step 13): the Stirring Rod, Sampling Spoon and Assay Glass (the crucible acts on
 * them), the Formula (its tooltip shows the process it records) and the Reagent (named for what it was ground from).
 */
public class AlchemyItem extends Item {
	public AlchemyItem(Properties properties) {
		super(properties);
	}

	/** A reagent is "Ground Sugar"; anything else is its own name. */
	@Override
	public Component getName(ItemStack stack) {
		Reagent reagent = stack.get(JugcraftConcordance.REAGENT);
		if (reagent == null) {
			return super.getName(stack);
		}
		Identifier id = Identifier.tryParse(reagent.item());
		Component ingredient = id == null ? Component.literal(reagent.item()) : BuiltInRegistries.ITEM.getValue(id).getName(new ItemStack(BuiltInRegistries.ITEM.getValue(id)));
		return Component.translatable("tooltip.jugcraft.concordance.reagent", ingredient);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		Identifier id = BuiltInRegistries.ITEM.getKey(this);
		String formula = stack.get(JugcraftConcordance.FORMULA);
		if (stack.is(JugcraftConcordance.FORMULA_ITEM)) {
			Formula read = formula == null ? null : Formula.parse(formula);
			tooltip.accept((read == null ? Component.translatable("tooltip.jugcraft.concordance.formula.blank")
					: Component.translatable("tooltip.jugcraft.concordance.formula.steps", read.operations().size(), read.text()))
					.withStyle(ChatFormatting.DARK_AQUA));
		}
		tooltip.accept(Component.translatable("tooltip." + id.getNamespace() + "." + id.getPath()).withStyle(ChatFormatting.GRAY));
	}
}
