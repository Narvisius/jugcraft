package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintItem;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A storage tank kit (Justin, Oct 7: "place down just a multi-block structure, and then it's just that silo"): used on
 * the ground, it places the whole tank at once, level with the ground and reaching away from the player: the concrete
 * foundation, the shell and roof, outlet and inlet with the fill line, manway, caged ladder, roof railing and gate,
 * hatch and the fittings of its rating (tools/tank_kits.py writes the structures, data/jugcraft/tank_kit/*.jugbp.json).
 * The tank forms at once. It needs clear room: nothing but grass, flowers or air where it goes above the ground.
 */
public class TankKitItem extends Item {
	private static final Map<String, Blueprint> CACHE = new HashMap<>();
	public final String kit;
	public final TankRating rating;

	public TankKitItem(Properties properties, String kit, TankRating rating) {
		super(properties);
		this.kit = kit;
		this.rating = rating;
	}

	/** The kit's structure (read from the jar once), or null. */
	public static synchronized @Nullable Blueprint blueprint(String kit) {
		if (CACHE.containsKey(kit)) {
			return CACHE.get(kit);
		}
		Blueprint found = null;
		String path = "/data/jugcraft/tank_kit/" + kit + ".jugbp.json";
		try (InputStream in = TankKitItem.class.getResourceAsStream(path)) {
			if (in == null) {
				Jugcraft.LOGGER.error("Missing tank kit {}", path);
			} else {
				found = Blueprint.parse("tank_kit/" + kit, new String(in.readAllBytes(), StandardCharsets.UTF_8), "built in");
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Tank kit {} is invalid: {}", kit, e.getMessage());
		}
		CACHE.put(kit, found);
		return found;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		Level level = context.getLevel();
		if (player == null) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		// The stake stands on the ground clicked: the foundation takes the ground's top layer, the tank stands on it.
		BlockPos stake = level.getBlockState(context.getClickedPos()).canBeReplaced() ? context.getClickedPos() : context.getClickedPos().above();
		Component refused = place(server, kit, stake, BlueprintItem.rotationFor(player.getDirection()), player);
		if (refused != null) {
			player.sendOverlayMessage(refused);
			return InteractionResult.FAIL;
		}
		if (!player.hasInfiniteMaterials()) {
			context.getItemInHand().shrink(1);
		}
		level.playSound(null, stake, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.6F, 0.8F);
		return InteractionResult.SUCCESS;
	}

	/**
	 * Places kit {@code kit} with its stake at {@code stake} (the cell above the ground), turned by {@code rotation}.
	 * Returns why it can't go there, or null once it is placed (and formed).
	 */
	public static @Nullable Component place(ServerLevel level, String kit, BlockPos stake, Rotation rotation, @Nullable Player player) {
		Blueprint blueprint = blueprint(kit);
		if (blueprint == null) {
			return Component.translatable("message.jugcraft.tank_kit.missing");
		}
		int ground = blueprint.anchorY();
		for (int i = 0; i < blueprint.size(); i++) {
			BlockPos offset = blueprint.offset(i, rotation);
			BlockPos pos = stake.offset(offset);
			if (level.isOutsideBuildHeight(pos) || !level.isLoaded(pos)) {
				return Component.translatable("message.jugcraft.tank_kit.no_room");
			}
			BlockState there = level.getBlockState(pos);
			if (offset.getY() < 0) {
				// The foundation's layer: any ground goes, but nothing built (a block entity) and nothing unbreakable.
				if (level.getBlockEntity(pos) != null || there.getDestroySpeed(level, pos) < 0) {
					return Component.translatable("message.jugcraft.tank_kit.blocked", there.getBlock().getName(), pos.getX(), pos.getY(), pos.getZ());
				}
			} else if (!there.canBeReplaced()) {
				return Component.translatable("message.jugcraft.tank_kit.blocked", there.getBlock().getName(), pos.getX(), pos.getY(), pos.getZ());
			}
		}
		if (ground != 1) {
			Jugcraft.LOGGER.warn("Tank kit {} has its stake {} layers up (expected 1)", kit, ground);
		}
		PlantBuilder.build(level, blueprint, stake, rotation, player == null ? null : player.getUUID(), false);
		return null;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		Blueprint blueprint = blueprint(kit);
		tooltip.accept(Component.translatable("tooltip.jugcraft.tank_kit." + rating.getSerializedName()).withStyle(ChatFormatting.GRAY));
		if (blueprint != null) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.tank_kit.size", blueprint.sizeX, blueprint.sizeZ, blueprint.sizeY)
					.withStyle(ChatFormatting.DARK_GRAY));
		}
		tooltip.accept(Component.translatable("tooltip.jugcraft.tank_kit.use").withStyle(ChatFormatting.DARK_GRAY));
	}
}
