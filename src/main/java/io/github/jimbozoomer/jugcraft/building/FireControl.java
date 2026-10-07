package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.artillery.CrewedGun;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;

/**
 * Fire control (batch 56, docs/features/fire-control.md): the Fire Control Table ({@link FireControlTableBlock}) and the
 * Fire Control Wire that links guns to it. Keep the numbers in sync with tools/fire_control.py; tools/check_mod_data.py
 * checks them.
 *
 * <p>Linking: use the wire on a table (which this remembers, on the server, per player), then on a gun within
 * {@value #LINK_RANGE} blocks of it. Using it on a gun already linked to that table unlinks it.
 */
public final class FireControl {
	/** How many guns a table directs, and how far from it (in blocks) a linked gun may stand. */
	public static final int MAX_GUNS = 8;
	public static final int LINK_RANGE = 64;
	/** A parallel sheaf's spacing across the line of fire, in blocks. */
	public static final int SHEAF_SPACING = 6;
	/**
	 * Sentry: how far round each gun it looks for hostile mobs, how close to the gun it will not fire, how near a player
	 * must not be to its mark, and how often (ticks) it picks a target.
	 */
	public static final int SENTRY_RANGE = 96;
	public static final int SENTRY_MIN_RANGE = 12;
	public static final int CHECK_FIRE = 8;
	public static final int SENTRY_SCAN = 10;
	/** Ticks between the table's own updates (its comparator reading). */
	public static final int TABLE_INTERVAL = 10;
	/** A creeping barrage: how far (blocks) the target steps down range after each salvo, and how many steps it takes. */
	public static final int CREEP_STEP = 5;
	public static final int CREEP_STEPS = 6;
	/** The sector widths, in degrees, that sneak-using a table cycles through. */
	public static final int[] SECTORS = {90, 180, 270, 360};

	public static Block TABLE;
	public static Item WIRE;
	public static BlockEntityType<FireControlTableBlock.Entity> TABLE_ENTITY;

	private record Pending(ResourceKey<Level> dimension, BlockPos table) {
	}

	/** Server side: the table each player last used wire on. Not saved: a link in progress is not part of the world. */
	private static final Map<UUID, Pending> PENDING = new HashMap<>();

	private FireControl() {
	}

	public static void register() {
		ResourceKey<Block> tableKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("fire_control_table"));
		TABLE = Registry.register(BuiltInRegistries.BLOCK, tableKey, new FireControlTableBlock(BlockBehaviour.Properties.of()
				.mapColor(MapColor.METAL).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(3.0F, 6.0F).noOcclusion()
				.setId(tableKey)));
		ResourceKey<Item> tableItem = ResourceKey.create(Registries.ITEM, Jugcraft.id("fire_control_table"));
		Registry.register(BuiltInRegistries.ITEM, tableItem, new BlockItem(TABLE, new Item.Properties().setId(tableItem)
				.useBlockDescriptionPrefix()) {
			@Override
			public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
					Consumer<Component> tooltip, TooltipFlag flag) {
				tooltip.accept(Component.translatable("tooltip.jugcraft.fire_control_table").withStyle(ChatFormatting.GRAY));
			}
		});
		ResourceKey<Item> wireKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("fire_control_wire"));
		WIRE = Registry.register(BuiltInRegistries.ITEM, wireKey, new Item(new Item.Properties().setId(wireKey).stacksTo(1)) {
			@Override
			public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
					Consumer<Component> tooltip, TooltipFlag flag) {
				tooltip.accept(Component.translatable("tooltip.jugcraft.fire_control_wire").withStyle(ChatFormatting.GRAY));
			}
		});
		TABLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("fire_control_table"),
				FabricBlockEntityTypeBuilder.create(FireControlTableBlock.Entity::new, TABLE).build());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.accept(TABLE);
			output.accept(WIRE);
		});
	}

	/** Remembers the table a player has started a link from. */
	public static void startLink(Player player, ServerLevel level, BlockPos table) {
		PENDING.put(player.getUUID(), new Pending(level.dimension(), table.immutable()));
	}

	/** Using the wire on a gun: links it to the player's table, or unlinks it if it is linked there already. */
	public static InteractionResult link(Player player, CrewedGun gun) {
		if (!(gun.level() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		Pending pending = PENDING.get(player.getUUID());
		if (pending == null || pending.dimension() != level.dimension()
				|| !(level.getBlockEntity(pending.table()) instanceof FireControlTableBlock.Entity table)) {
			PENDING.remove(player.getUUID());
			player.sendOverlayMessage(Component.translatable("message.jugcraft.fire_control.no_table"));
			return InteractionResult.SUCCESS;
		}
		if (table.linked(gun.getUUID())) {
			table.unlink(gun.getUUID());
			gun.unlinkDirector();
			player.sendOverlayMessage(Component.translatable("message.jugcraft.fire_control.unlinked"));
			return InteractionResult.SUCCESS;
		}
		if (gun.position().distanceTo(Vec3.atCenterOf(pending.table())) > LINK_RANGE) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.fire_control.too_far", LINK_RANGE));
			return InteractionResult.SUCCESS;
		}
		if (!table.link(gun.getUUID())) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.fire_control.full", MAX_GUNS));
			return InteractionResult.SUCCESS;
		}
		gun.linkDirector(level, pending.table(), table);
		level.playSound(null, gun.getX(), gun.getY(), gun.getZ(), SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 1.0F, 1.0F);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.fire_control.linked", table.links().size(), MAX_GUNS));
		return InteractionResult.SUCCESS;
	}
}
