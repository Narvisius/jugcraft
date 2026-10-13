package io.github.jimbozoomer.jugcraft.client.trinket;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.trinkets.api.client.renderer.element.TrinketRenderElement;
import eu.pb4.trinkets.api.client.renderer.element.TrinketRenderElements;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.trinket.WornDisplay;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/**
 * A Trinkets render element, {@code jugcraft:unless_covered}, that draws the elements it holds ({@code then}) only while
 * the wearer has not hidden their worn trinkets ({@link WornDisplay#hidden}: their own "Show my worn trinkets" setting,
 * which the server sends to everyone who sees them) and none of the armour slots it names ({@code armour}) holds armour
 * drawn on the body ({@link #covered}). Wayfaring's render definitions wrap its belt (hidden under a chestplate or
 * leggings) and boots (under boots) in it (tools/concordance_trinkets.py WORN_COVERED_BY). Both are asked every time
 * Trinkets draws the wearer, from the wearer itself (Trinkets calls an element while the wearer's render state is taken,
 * before the armour is copied into it), so armour put on or off, or a wearer's new choice arriving, shows on the next
 * frame.
 */
public record UnlessCoveredTrinketElement(List<EquipmentSlot> armour, List<TrinketRenderElement> then) implements TrinketRenderElement {
	public static final Identifier TYPE = Jugcraft.id("unless_covered");
	public static final MapCodec<UnlessCoveredTrinketElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ExtraCodecs.compactListCodec(EquipmentSlot.CODEC).fieldOf("armour").forGetter(UnlessCoveredTrinketElement::armour),
			ExtraCodecs.compactListCodec(TrinketRenderElements.CODEC).fieldOf("then").forGetter(UnlessCoveredTrinketElement::then)
	).apply(instance, UnlessCoveredTrinketElement::new));

	@Override
	public MapCodec<? extends TrinketRenderElement> type() {
		return CODEC;
	}

	@Override
	public Baked bake(BakingContext context) {
		List<Baked> inner = then.stream().map(element -> element.bake(context)).toList();
		return (owner, item, access, level, renderContext, state) -> {
			if (WornDisplay.hidden(owner) || covered(owner, armour)) {
				return;
			}
			for (Baked element : inner) {
				element.apply(owner, item, access, level, renderContext, state);
			}
		};
	}

	/** The held elements' models must be found and baked too (Trinkets only asks the top-level element). */
	@Override
	public void resolveDependencies(Resolver resolver) {
		for (TrinketRenderElement element : then) {
			element.resolveDependencies(resolver);
		}
	}

	/**
	 * Whether any of {@code slots} on {@code wearer} holds armour drawn on the body there: an equippable item for that slot
	 * with an equipment asset (as vanilla's armour layer asks; Jugcraft's 3D and GeckoLib sets name one too), other than a
	 * glider (an elytra is drawn as wings, not on the body).
	 */
	public static boolean covered(LivingEntity wearer, List<EquipmentSlot> slots) {
		for (EquipmentSlot slot : slots) {
			ItemStack worn = wearer.getItemBySlot(slot);
			Equippable equippable = worn.get(DataComponents.EQUIPPABLE);
			if (equippable != null && equippable.slot() == slot && equippable.assetId().isPresent() && !worn.has(DataComponents.GLIDER)) {
				return true;
			}
		}
		return false;
	}
}
