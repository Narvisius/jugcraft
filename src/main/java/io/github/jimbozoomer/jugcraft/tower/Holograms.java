package io.github.jimbozoomer.jugcraft.tower;

import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/**
 * The Drone Tower's holograms (Justin, 28 October 2026: "make sure people can't craft any of the holograms. I want those
 * to be exclusive to the tower ... once they're down ... They stay there unless destroyed in creative mode"; "Those four
 * + Holo Panel Emitter", "Nothing: they come free", "Only creative can place"): the Hologram Table, the Holo Command
 * Table, the Holo Projector, the Big Map Table and the Holo Panel Emitter.
 *
 * <ul>
 * <li>None has a recipe (tools/advanced_crafting.py TOWER_ONLY); the tower puts them in as it builds, free
 * (TowerCosts.CHARGED leaves them out).</li>
 * <li>Placed, only a creative player breaks one: as bedrock, no tool mines it, no explosion, piston, wither or dragon
 * moves it (the minecraft:wither_immune and dragon_immune tags). Code that sets the world's blocks (the tower's own
 * steps and conversions) still can.</li>
 * <li>Only a creative player places one from its item; in survival the item says so and places nothing.</li>
 * </ul>
 */
public final class Holograms {
	/** The holograms' blocks, with the parts of the ones that are several blocks. */
	public static final Set<String> BLOCKS = Set.of("holo_table", "holo_command_table", "holo_projector", "holo_projector_part",
			"big_map_table", "big_map_table_part", "holo_emitter");
	/** The ones with an item. */
	public static final Set<String> ITEMS = Set.of("holo_table", "holo_command_table", "holo_projector", "big_map_table", "holo_emitter");

	private Holograms() {
	}

	/** A hologram's block: unbreakable but by a creative player, and not moved by explosions or pistons. */
	public static BlockBehaviour.Properties protect(BlockBehaviour.Properties properties) {
		return properties.strength(-1.0F, 3_600_000.0F).pushReaction(PushReaction.IMMOVEABLE);
	}

	/** Whether {@code player} may put a hologram down from its item: only in creative (the game mode, as a test's mock player has it too). */
	public static boolean mayPlace(Player player) {
		return player != null && player.isCreative();
	}

	/** A hologram's item: it places only for a creative player. */
	public static BlockItem item(Block block, Item.Properties properties) {
		return new TowerOnlyItem(block, properties);
	}

	public static final class TowerOnlyItem extends BlockItem {
		TowerOnlyItem(Block block, Item.Properties properties) {
			super(block, properties);
		}

		@Override
		protected boolean canPlace(BlockPlaceContext context, BlockState state) {
			Player player = context.getPlayer();
			if (!mayPlace(player)) {
				if (player != null && !context.getLevel().isClientSide()) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.hologram.tower_only"));
				}
				return false;
			}
			return super.canPlace(context, state);
		}
	}
}
