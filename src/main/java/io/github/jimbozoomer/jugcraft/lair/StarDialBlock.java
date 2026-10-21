package io.github.jimbozoomer.jugcraft.lair;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A star dial of the Cydonian Sanctum's Dust Gallery (docs/features/cydonian-sanctum.md): a disc of night sky in a bronze
 * ring, set into the gallery's wall, showing one constellation ({@link Stars}). A lair fixture: it cannot be broken, but
 * any player may use it, which turns it to the next constellation. Each of the Star Door's three figures burns while a
 * dial shows it; when all three burn, the door grinds open ({@link CydonianSanctum#dialsChanged}). {@link #FACING} is the
 * way its face looks, out of the wall.
 */
public class StarDialBlock extends HorizontalDirectionalBlock {
	public static final EnumProperty<Stars> STARS = EnumProperty.create("stars", Stars.class);

	/** The constellations a dial shows, in the order a turn takes them (tools/cydonian_sanctum.py CONSTELLATIONS). */
	public enum Stars implements StringRepresentable {
		PLOUGH, HUNTER, LYRE, SWAN, SCORPION, CROWN;

		/** The next constellation a turn shows. */
		public Stars next() {
			return values()[(ordinal() + 1) % values().length];
		}

		/** The constellation, as shown: "the Plough", "the Hunter" and so on. */
		public Component display() {
			return Component.translatable("lair.jugcraft.cydonian_sanctum.stars." + getSerializedName());
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public StarDialBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STARS, Stars.PLOUGH));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
			turn(server, pos, serverPlayer);
		}
		return InteractionResult.SUCCESS;
	}

	/** Turns the dial at {@code pos} to its next constellation, and lets the Star Door see it. Returns what it shows now. */
	public static Stars turn(ServerLevel level, BlockPos pos, ServerPlayer player) {
		BlockState state = level.getBlockState(pos);
		Stars next = state.getValue(STARS).next();
		level.setBlock(pos, state.setValue(STARS, next), Block.UPDATE_ALL);
		level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.5F, 1.4F);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.lair.dial.turned", next.display()));
		if (level.dimension() == Lair.CYDONIAN_SANCTUM.dimension) {
			CydonianSanctum.dialsChanged(level, CydonianSanctum.originAt(pos), player);
		}
		return next;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, STARS);
	}
}
