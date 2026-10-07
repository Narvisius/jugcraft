package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * The Skeleton Key and the Key Blank (Halloween decorations batch 18). A key's wards (data component
 * {@code jugcraft:key_wards}, {@value #WARD_DIGITS} digits of one to {@value #WARD_KINDS}) say which lock it opens. Sneak-use
 * an unlocked Iron-Bound Coffin with a key to lock it to that key, and again to unlock it; sneak-use one with a blank and
 * the blank is cut to new wards and locks it, becoming its key. A cut key copies onto blanks in a crafting grid
 * ({@link KeyCopyingRecipe}). Locking needs the right to use blocks there; everything is decided on the server.
 */
public class SkeletonKeyItem extends Item {
	public static final int WARD_DIGITS = 6;
	public static final int WARD_KINDS = 5;
	/** How many different wards there are: every key's wards are 1 to this, less one. */
	public static final int PATTERNS = 15625;

	private final boolean blank;

	public SkeletonKeyItem(Properties properties, boolean blank) {
		super(properties);
		this.blank = blank;
	}

	/** The wards of {@code stack}, or 0 for anything that is not a cut Skeleton Key. */
	public static int wards(ItemStack stack) {
		return stack.getItem() instanceof SkeletonKeyItem key && !key.blank ? stack.getOrDefault(JugcraftAgriculture.KEY_WARDS, 0) : 0;
	}

	/** Whether {@code player} holds, in either hand, a key with the wards {@code lock}. */
	public static boolean holdsKey(Player player, int lock) {
		return lock != 0 && (wards(player.getMainHandItem()) == lock || wards(player.getOffhandItem()) == lock);
	}

	/** Wards written as their digits, such as {@code 3-1-4-2-5-1}. */
	public static String describe(int wards) {
		StringBuilder out = new StringBuilder();
		int rest = wards;
		for (int i = 0; i < WARD_DIGITS; i++) {
			if (i > 0) {
				out.append('-');
			}
			out.append(rest % WARD_KINDS + 1);
			rest /= WARD_KINDS;
		}
		return out.toString();
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		int wards = wards(stack);
		if (wards != 0) {
			tooltip.accept(Component.translatable("item.jugcraft.skeleton_key.wards", describe(wards)).withStyle(ChatFormatting.GRAY));
		} else if (blank) {
			tooltip.accept(Component.translatable("item.jugcraft.key_blank.hint").withStyle(ChatFormatting.GRAY));
		}
	}

	/** Sneak-using a coffin skips the coffin, so locking is the key's to do. */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		Player player = context.getPlayer();
		if (!(state.getBlock() instanceof IronBoundCoffinBlock) || player == null) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		BlockPos head = CoffinBlock.head(state, pos);
		if (!(level.getBlockEntity(head) instanceof IronBoundCoffinBlockEntity coffin) || !level.mayInteract(player, head)) {
			return InteractionResult.FAIL;
		}
		ItemStack stack = context.getItemInHand();
		int lock = coffin.lock();
		if (blank) {
			if (lock != 0) {
				return refuse(player, level, head);
			}
			int wards = 1 + server.getRandom().nextInt(PATTERNS - 1);
			ItemStack key = new ItemStack(JugcraftAgriculture.item(JugcraftAgriculture.SKELETON_KEY));
			key.set(JugcraftAgriculture.KEY_WARDS, wards);
			stack.consume(1, player);
			if (stack.isEmpty()) {
				player.setItemInHand(context.getHand(), key);
			} else {
				player.getInventory().placeItemBackInInventory(key, Prediction.SERVER_ONLY);
			}
			IronBoundCoffinBlock.setLock(level, head, coffin, wards);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.coffin.key_cut"));
			level.playSound(null, head, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.5F, 1.6F);
		} else {
			int wards = wards(stack);
			if (wards == 0 || lock != 0 && lock != wards) {
				return refuse(player, level, head);
			}
			IronBoundCoffinBlock.setLock(level, head, coffin, lock == 0 ? wards : 0);
			player.sendOverlayMessage(Component.translatable(lock == 0 ? "message.jugcraft.coffin.locked_to" : "message.jugcraft.coffin.unlocked"));
		}
		level.playSound(null, head, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.6F, 1.8F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, head);
		return InteractionResult.SUCCESS;
	}

	private static InteractionResult refuse(Player player, Level level, BlockPos pos) {
		player.sendOverlayMessage(Component.translatable("message.jugcraft.coffin.wrong_key"));
		level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.8F, 1.0F);
		return InteractionResult.FAIL;
	}
}
