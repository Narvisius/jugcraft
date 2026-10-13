package io.github.jimbozoomer.jugcraft.concordance.ember;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
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
 * A piece of a fire set ({@link EmberGear}: the Pyromaniac's or the Pyromancer's): half a point of fire Spell Power, worn
 * as the owner's GeckoLib model (assets/jugcraft/geckolib/models/armor/{@link #model()}.geo.json) with its set's texture
 * (textures/armor/{@link #set()}.png), both imported as supplied, which the client's renderer draws through
 * {@link EmberHooks}. It has no animations: GeckoLib poses its bones to the wearer as vanilla armour moves.
 */
public class EmberArmorItem extends Item implements GeoItem {
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final String set;
	private final String model;

	public EmberArmorItem(Properties properties, String set, String model) {
		super(properties);
		this.set = set;
		this.model = model;
	}

	/** The set it belongs to: its equipment asset and its worn texture's name. */
	public String set() {
		return set;
	}

	/** The GeckoLib model it is worn as. */
	public String model() {
		return model;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
	}

	/** The client's renderer for the set ({@link EmberHooks}); GeckoLib casts it to its GeoRenderProvider. */
	@Override
	public Object getRenderProvider() {
		return EmberHooks.armor.apply(this);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		Identifier id = BuiltInRegistries.ITEM.getKey(this);
		tooltip.accept(Component.translatable("tooltip." + id.getNamespace() + "." + id.getPath()).withStyle(ChatFormatting.GRAY));
	}
}
