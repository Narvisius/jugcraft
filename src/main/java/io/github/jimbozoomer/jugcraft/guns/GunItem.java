package io.github.jimbozoomer.jugcraft.guns;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A gun ({@link JugcraftGuns}). Held right click aims down the sights (using the item, so the server and the other
 * players know); the trigger and reload come as payloads ({@link GunShots}). Slice 10G: with a one-handed gun in each
 * hand ({@link #dual}), right click is the other gun's trigger instead. GeckoLib draws it from
 * assets/jugcraft/geckolib (models and the owner's animations, tools/guns.py) through the client's renderer.
 * <p>
 * Its attachments ({@link JugcraftGuns#FITTED}, fitted in a crafting grid by {@link GunAttachmentRecipe}) change its
 * numbers: everything that fires, loads or shows the gun reads them through {@link #spec(ItemStack)}.
 */
public class GunItem extends Item implements GeoItem {
	/** The ammunition gauge's colour: brass. */
	private static final int BAR_COLOR = 0xFFD8A848;
	private final String name;
	private final GunSpec spec;
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public GunItem(String name, GunSpec spec, Properties properties) {
		super(properties);
		this.name = name;
		this.spec = spec;
	}

	public String name() {
		return name;
	}

	/** The gun's own numbers, without attachments. */
	public GunSpec spec() {
		return spec;
	}

	/** This gun's numbers with its attachments' effects (the stack must hold a gun). */
	public static GunSpec spec(ItemStack stack) {
		GunSpec base = ((GunItem) stack.getItem()).spec;
		List<String> fitted = attachments(stack);
		if (fitted.isEmpty()) {
			return base;
		}
		float damage = 1.0F;
		float range = 1.0F;
		float hipSpread = 1.0F;
		float aimSpread = 1.0F;
		float capacity = 1.0F;
		float reload = 1.0F;
		for (String name : fitted) {
			GunAttachment attachment = JugcraftGuns.ATTACHMENTS.get(name);
			damage *= attachment.damage();
			range *= attachment.range();
			hipSpread *= attachment.hipSpread();
			aimSpread *= attachment.aimSpread();
			capacity *= attachment.capacity();
			reload *= attachment.reload();
		}
		// A gun loaded a shell at a time keeps its reload of 0 (and its shell times).
		return new GunSpec(base.damage() * damage, base.pellets(), base.interval(), base.auto(),
				Math.max(1, Math.round(base.capacity() * capacity)), base.byShell() ? 0 : Math.max(1, Math.round(base.reload() * reload)),
				base.shellStart(), base.shellEach(), base.shellFinish(), base.hipSpread() * hipSpread, base.aimSpread() * aimSpread,
				Math.max(1, Math.round(base.range() * range)), base.ammo());
	}

	/** How far the view jumps with each shot, against the gun alone. */
	public static float kick(ItemStack stack) {
		float kick = 1.0F;
		for (String name : attachments(stack)) {
			kick *= JugcraftGuns.ATTACHMENTS.get(name).kick();
		}
		return kick;
	}

	/** How loud the shot is, against the gun alone. */
	public static float volume(ItemStack stack) {
		float volume = 1.0F;
		for (String name : attachments(stack)) {
			volume *= JugcraftGuns.ATTACHMENTS.get(name).volume();
		}
		return volume;
	}

	/** The attachments fitted to this gun, oldest first (ids no longer known are left out). */
	public static List<String> attachments(ItemStack stack) {
		return stack.getOrDefault(JugcraftGuns.FITTED, List.<String>of()).stream().filter(JugcraftGuns.ATTACHMENTS::containsKey).toList();
	}

	/** The bayonet fitted to this gun (slice 7), or null. */
	public static @Nullable GunAttachment bayonet(ItemStack stack) {
		if (!(stack.getItem() instanceof GunItem)) {
			return null;
		}
		for (String name : attachments(stack)) {
			GunAttachment attachment = JugcraftGuns.ATTACHMENTS.get(name);
			if (attachment.bayonet()) {
				return attachment;
			}
		}
		return null;
	}

	/** Whether this gun takes this attachment. */
	public static boolean takes(ItemStack stack, String attachment) {
		return stack.getItem() instanceof GunItem gun && JugcraftGuns.ACCEPTS.getOrDefault(gun.name, List.of()).contains(attachment);
	}

	/** The attachment fitted in this slot, or null. */
	public static @Nullable String inSlot(ItemStack stack, String slot) {
		for (String name : attachments(stack)) {
			if (JugcraftGuns.ATTACHMENTS.get(name).slot().equals(slot)) {
				return name;
			}
		}
		return null;
	}

	/** Fits this attachment, taking off any the slot held. */
	public static void fit(ItemStack stack, String attachment) {
		String slot = JugcraftGuns.ATTACHMENTS.get(attachment).slot();
		List<String> fitted = new ArrayList<>(attachments(stack));
		fitted.removeIf(name -> JugcraftGuns.ATTACHMENTS.get(name).slot().equals(slot));
		fitted.add(attachment);
		stack.set(JugcraftGuns.FITTED, List.copyOf(fitted));
	}

	/** Takes this attachment off. */
	public static void remove(ItemStack stack, String attachment) {
		List<String> fitted = new ArrayList<>(attachments(stack));
		fitted.remove(attachment);
		if (fitted.isEmpty()) {
			stack.remove(JugcraftGuns.FITTED);
		} else {
			stack.set(JugcraftGuns.FITTED, List.copyOf(fitted));
		}
	}

	public static int loaded(ItemStack stack) {
		return stack.getOrDefault(JugcraftGuns.LOADED, 0);
	}

	/**
	 * Sets the rounds loaded, up to the magazine's capacity. A gun whose larger magazine came off keeps the rounds it
	 * held beyond that until they are fired.
	 */
	public static void setLoaded(ItemStack stack, int rounds) {
		int capacity = stack.getItem() instanceof GunItem ? Math.max(spec(stack).capacity(), loaded(stack)) : 0;
		stack.set(JugcraftGuns.LOADED, Math.max(0, Math.min(capacity, rounds)));
	}

	/**
	 * Aims down the sights while held (main hand only: the trigger is the attack button). Slice 10G: with a one-handed gun
	 * in each hand ({@link #dual}) nothing is aimed, and right click pulls the other gun's trigger: the client pulls it as
	 * vanilla hands it the click for that hand (GunsClient, through {@link GunHooks#offTrigger}), so a click that opens a
	 * door or talks to a villager fires nothing. Neither hand counts as used, so neither gun dips out of view and back.
	 */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (dual(player)) {
			if (hand == InteractionHand.OFF_HAND && level.isClientSide()) {
				GunHooks.offTrigger.run();
			}
			return InteractionResult.PASS;
		}
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	/** Whether this is a gun held in one hand (slice 10G, {@link JugcraftGuns#ONE_HANDED}). */
	public static boolean oneHanded(ItemStack stack) {
		return stack.getItem() instanceof GunItem gun && JugcraftGuns.ONE_HANDED.contains(gun.name);
	}

	/**
	 * Whether this creature holds a one-handed gun in each hand, so fires both (slice 10G, two guns at once): left click
	 * the main hand's, right click the other's.
	 */
	public static boolean dual(LivingEntity holder) {
		return oneHanded(holder.getMainHandItem()) && oneHanded(holder.getOffhandItem());
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.NONE;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 72000;
	}

	/** Whether this player is aiming down the sights of this gun. */
	public static boolean aiming(LivingEntity entity, ItemStack stack) {
		return entity.isUsingItem() && entity.getUseItem() == stack;
	}

	/** Gives the stack its GeckoLib animation id (on the server, once), so its animations survive the stack's updates. */
	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
		GeoItem.getOrAssignId(stack, level);
	}

	/** Firing and loading change the stack; the gun should not dip out of view and back each time. */
	@Override
	public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
		return !(oldStack.is(this) && newStack.is(this));
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.min(13, Math.round(13.0F * loaded(stack) / spec(stack).capacity()));
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return BAR_COLOR;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		GunSpec fitted = spec(stack);
		tooltip.accept(Component.translatable("tooltip.jugcraft.guns.ammo", loaded(stack), fitted.capacity())
				.withStyle(ChatFormatting.GOLD));
		if (JugcraftGuns.takesGrenades(fitted) && loaded(stack) > 0) {
			// Slice 9G: which grenade a grenade gun holds.
			tooltip.accept(Component.translatable("tooltip.jugcraft.guns.loaded_grenade",
					Component.translatable(JugcraftGuns.loadedAmmo(stack, fitted).getDescriptionId())).withStyle(ChatFormatting.GOLD));
		}
		tooltip.accept(Component.translatable("tooltip.jugcraft.guns.stats", String.format(Locale.ROOT, "%.1f", fitted.damage()),
				fitted.pellets(), String.format(Locale.ROOT, "%.1f", 20.0F / fitted.interval()), fitted.range())
				.withStyle(ChatFormatting.BLUE));
		if (JugcraftGuns.ONE_HANDED.contains(name)) {
			// Slice 10G: it pairs with another in the other hand.
			tooltip.accept(Component.translatable("tooltip.jugcraft.guns.one_handed").withStyle(ChatFormatting.DARK_GREEN));
		}
		int charge = JugcraftGuns.charge(this);
		if (charge > 0) {
			// Slice 8D: an energy weapon's rounds are charge from Energy Cells.
			tooltip.accept(Component.translatable("tooltip.jugcraft.guns.charge", String.format(Locale.ROOT, "%,d", charge))
					.withStyle(ChatFormatting.AQUA));
		}
		List<String> attachments = attachments(stack);
		if (!attachments.isEmpty()) {
			MutableComponent names = Component.empty();
			for (String attachment : attachments) {
				if (!names.getSiblings().isEmpty()) {
					names.append(", ");
				}
				names.append(Component.translatable(JugcraftGuns.ATTACHMENT_ITEMS.get(attachment).getDescriptionId()));
			}
			tooltip.accept(Component.translatable("tooltip.jugcraft.guns.fitted", names).withStyle(ChatFormatting.DARK_AQUA));
		}
		tooltip.accept(Component.translatable("tooltip.jugcraft.guns." + name).withStyle(ChatFormatting.GRAY));
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		GunHooks.controllers.accept(this, controllers);
	}

	/** The client's renderer for this gun ({@link GunHooks}); GeckoLib casts it to its GeoRenderProvider. */
	@Override
	public Object getRenderProvider() {
		return GunHooks.renderer.apply(this);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
