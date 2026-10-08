package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintItem;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * While held, a see-through copy of the formed tank shows where it would go (client/MbsPreview.java, from
 * {@link #formed} and {@link #blocked}); breaking any block of the tank takes it all down and gives the kit back,
 * empty (TankKitBreaking; Justin, Oct 8).
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
	 * Returns why it can't go there, or null once it is placed (and formed). The tank remembers it came from this kit
	 * (and the ground its foundation took), so it comes down whole when any part of it is broken
	 * ({@link TankKitBreaking}).
	 */
	public static @Nullable Component place(ServerLevel level, String kit, BlockPos stake, Rotation rotation, @Nullable Player player) {
		Blueprint blueprint = blueprint(kit);
		if (blueprint == null) {
			return Component.translatable("message.jugcraft.tank_kit.missing");
		}
		List<BlockPos> blocked = blocked(level, blueprint, stake, rotation);
		if (!blocked.isEmpty()) {
			BlockPos pos = blocked.get(0);
			if (level.isOutsideBuildHeight(pos) || !level.isLoaded(pos)) {
				return Component.translatable("message.jugcraft.tank_kit.no_room");
			}
			BlockState there = level.getBlockState(pos);
			return Component.translatable("message.jugcraft.tank_kit.blocked", there.getBlock().getName(), pos.getX(), pos.getY(), pos.getZ());
		}
		if (blueprint.anchorY() != 1) {
			Jugcraft.LOGGER.warn("Tank kit {} has its stake {} layers up (expected 1)", kit, blueprint.anchorY());
		}
		// What the kit takes: every block it lays, and the ground its foundation goes into (given back when it comes down).
		List<BlockPos> cells = new ArrayList<>();
		List<BlockPos> groundCells = new ArrayList<>();
		List<BlockState> ground = new ArrayList<>();
		BlockPos outlet = null;
		for (int i = 0; i < blueprint.size(); i++) {
			BlockState state = blueprint.state(i, rotation);
			if (state.isAir()) {
				continue;
			}
			BlockPos pos = stake.offset(blueprint.offset(i, rotation));
			cells.add(pos);
			if (blueprint.offsetY(i) < 0) {
				groundCells.add(pos);
				ground.add(level.getBlockState(pos));
			}
			if (state.getBlock() instanceof StorageTankOutletBlock) {
				outlet = pos;
			}
		}
		PlantBuilder.build(level, blueprint, stake, rotation, player == null ? null : player.getUUID(), false);
		if (outlet != null && level.getBlockEntity(outlet) instanceof StorageTankPortBlockEntity port) {
			port.setKit(level, kit, cells, groundCells, ground);
		}
		return null;
	}

	/**
	 * Where kit {@code blueprint} can't go with its stake at {@code stake}, turned by {@code rotation}: cells above the
	 * ground that aren't free (only grass, flowers or air will do), foundation cells holding something built (a block
	 * entity) or unbreakable, and cells outside the world or not loaded. Empty when it fits. The client's preview asks
	 * too (client/MbsPreview.java).
	 */
	public static List<BlockPos> blocked(Level level, Blueprint blueprint, BlockPos stake, Rotation rotation) {
		List<BlockPos> blocked = new ArrayList<>();
		for (int i = 0; i < blueprint.size(); i++) {
			BlockPos offset = blueprint.offset(i, rotation);
			BlockPos pos = stake.offset(offset);
			if (level.isOutsideBuildHeight(pos) || !level.isLoaded(pos)) {
				blocked.add(pos);
				continue;
			}
			BlockState there = level.getBlockState(pos);
			if (offset.getY() < 0) {
				// The foundation's layer: any ground goes, but nothing built (a block entity) and nothing unbreakable.
				if (level.getBlockEntity(pos) != null || there.getDestroySpeed(level, pos) < 0) {
					blocked.add(pos);
				}
			} else if (!there.canBeReplaced()) {
				blocked.add(pos);
			}
		}
		return blocked;
	}

	/** A kit as it stands once placed and formed (for the placement preview): its blocks and the space inside, from the stake. */
	public record Formed(List<Blueprint.Cell> cells, Set<BlockPos> inside) {
	}

	private static final Map<String, Formed> FORMED = new HashMap<>();

	/**
	 * Kit {@code kit} turned by {@code rotation} as it looks once formed: the plates painted in their courses (roof, rim,
	 * the band under the roof, the product stripe under that, wall) in the kit's rating, the nozzles and hatch joined
	 * up, as {@link StorageTankPortBlockEntity} paints a tank when it forms; and the space inside (the blueprint's air).
	 */
	public static synchronized @Nullable Formed formed(String kit, Rotation rotation) {
		String key = kit + "/" + rotation.ordinal();
		if (FORMED.containsKey(key)) {
			return FORMED.get(key);
		}
		Blueprint blueprint = blueprint(kit);
		Formed formed = null;
		if (blueprint != null) {
			Set<BlockPos> inside = new HashSet<>();
			int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
			TankRating rating = TankRating.GENERAL;
			for (Blueprint.Cell cell : blueprint.cells(rotation)) {
				if (cell.state().isAir()) {
					inside.add(cell.offset());
					minY = Math.min(minY, cell.offset().getY());
					maxY = Math.max(maxY, cell.offset().getY());
				} else if (cell.state().getBlock() instanceof StorageTankOutletBlock) {
					rating = cell.state().getValue(StorageTankOutletBlock.RATING);
				}
			}
			int roofY = maxY + 1;
			boolean stripe = roofY - 2 > minY;
			List<Blueprint.Cell> cells = new ArrayList<>();
			for (Blueprint.Cell cell : blueprint.cells(rotation)) {
				BlockState state = cell.state();
				if (state.isAir()) {
					continue;
				}
				BlockPos p = cell.offset();
				if (state.getBlock() instanceof StorageTankPlateBlock && !inside.isEmpty()) {
					StorageTankPlateBlock.Band band;
					if (p.getY() > roofY) {
						band = StorageTankPlateBlock.Band.RIM;
					} else if (p.getY() == roofY) {
						band = inside.contains(p.below()) ? StorageTankPlateBlock.Band.ROOF : StorageTankPlateBlock.Band.RIM;
					} else if (p.getY() == roofY - 1) {
						band = StorageTankPlateBlock.Band.BAND;
					} else if (stripe && p.getY() == roofY - 2) {
						band = StorageTankPlateBlock.Band.STRIPE;
					} else {
						band = StorageTankPlateBlock.Band.WALL;
					}
					state = state.setValue(StorageTankPlateBlock.FORMED, true).setValue(StorageTankPlateBlock.BAND, band)
							.setValue(StorageTankPlateBlock.RATING, rating);
				} else if (state.getBlock() instanceof StorageTankPortBlock) {
					state = state.setValue(StorageTankPortBlock.FORMED, true);
				}
				cells.add(new Blueprint.Cell(p, state));
			}
			formed = new Formed(List.copyOf(cells), Set.copyOf(inside));
		}
		FORMED.put(key, formed);
		return formed;
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
