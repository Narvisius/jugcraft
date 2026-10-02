package io.github.jimbozoomer.jugcraft.blueprint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

/**
 * A printed Blueprint. It carries only the blueprint's id (and an imported blueprint's name). While it is held,
 * the structure is previewed where you look, up to five chunks away; right-click sets a Survey Stake there and
 * the structure is staked out in front of it, facing you, as a hologram.
 */
public class BlueprintItem extends Item {
	public BlueprintItem(Properties properties) {
		super(properties);
	}

	/** A blueprint for a built-in id (its name comes from the language file). */
	public static ItemStack stack(String blueprintId) {
		return stack(blueprintId, "");
	}

	public static ItemStack stack(Blueprint blueprint) {
		return stack(blueprint.id, blueprint.name);
	}

	public static ItemStack stack(String blueprintId, String name) {
		ItemStack stack = new ItemStack(JugcraftBlueprints.BLUEPRINT);
		CompoundTag tag = new CompoundTag();
		tag.putString("blueprint", blueprintId);
		if (!name.isEmpty() && !Blueprint.BUILT_IN.contains(blueprintId)) {
			tag.putString("name", name);
		}
		CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
		// The item's colour follows the blueprint's kind (blue complete, green part, red imported).
		Blueprint blueprint = Blueprint.get(blueprintId, false);
		Blueprint.Kind kind = blueprint != null ? blueprint.kind : Blueprint.Kind.COMPLETE;
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(
				java.util.List.of(), java.util.List.of(), java.util.List.of(kind.id), java.util.List.of()));
		return stack;
	}

	public static String idOf(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr("blueprint", "");
	}

	@Override
	public Component getName(ItemStack stack) {
		String id = idOf(stack);
		if (id.isEmpty()) {
			return super.getName(stack);
		}
		String name = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr("name", "");
		return Component.translatable("item.jugcraft.blueprint.named",
				name.isEmpty() ? Component.translatable("blueprint.jugcraft." + id) : Component.literal(name));
	}

	/** How far away a blueprint can be placed: five chunks, so large builds can be staked out from a distance. */
	public static final double PLACE_RANGE = 80.0;

	/** Where a blueprint goes: the stake's position and the turn. */
	public record Target(BlockPos stake, Rotation rotation) {
	}

	/** The spot the player is looking at, up to {@link #PLACE_RANGE} blocks away (the same on client and server), or null. */
	public static @org.jspecify.annotations.Nullable Target target(Player player, float partialTick) {
		net.minecraft.world.phys.HitResult hit = player.pick(PLACE_RANGE, partialTick, false);
		if (!(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit) || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
			return null;
		}
		Level level = player.level();
		BlockPos pos = blockHit.getBlockPos();
		if (!level.getBlockState(pos).canBeReplaced()) {
			pos = pos.relative(blockHit.getDirection());
		}
		return new Target(pos, rotationFor(player.getDirection()));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		return context.getPlayer() == null ? InteractionResult.PASS : place(context.getLevel(), context.getPlayer(), context.getItemInHand());
	}

	@Override
	public InteractionResult use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
		return place(level, player, player.getItemInHand(hand));
	}

	private InteractionResult place(Level level, Player player, ItemStack stack) {
		String id = idOf(stack);
		Blueprint blueprint = Blueprint.get(id, level.isClientSide());
		Target target = target(player, 1.0F);
		if (target == null) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (blueprint == null || blueprint.size() == 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.blueprint.unknown"));
			return InteractionResult.FAIL;
		}
		BlockPos pos = target.stake();
		if (!level.isLoaded(pos) || !level.getBlockState(pos).canBeReplaced() || !level.mayInteract(player, pos)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.blueprint.no_room"));
			return InteractionResult.FAIL;
		}
		level.setBlockAndUpdate(pos, JugcraftBlueprints.STAKE.defaultBlockState());
		if (level.getBlockEntity(pos) instanceof SurveyStakeBlockEntity stake) {
			stake.setup(id, target.rotation(), player.getUUID());
		}
		if (!player.isCreative()) {
			stack.shrink(1);
		}
		player.sendSystemMessage(Component.translatable("message.jugcraft.blueprint.placed", blueprint.name, "PERSONAL"));
		int top = pos.getY();
		for (Blueprint.Cell cell : blueprint.cells(target.rotation())) {
			top = Math.max(top, pos.getY() + cell.offset().getY());
		}
		if (top > level.getMaxY()) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.blueprint.clipped", top - level.getMaxY(), level.getMaxY() + 1)
					.withStyle(net.minecraft.ChatFormatting.RED));
		}
		return InteractionResult.SUCCESS;
	}

	/** Blueprints are drawn extending north from the stake; turn them to extend the way the player looks. */
	static Rotation rotationFor(Direction facing) {
		return switch (facing) {
			case EAST -> Rotation.CLOCKWISE_90;
			case SOUTH -> Rotation.CLOCKWISE_180;
			case WEST -> Rotation.COUNTERCLOCKWISE_90;
			default -> Rotation.NONE;
		};
	}
}
