package io.github.jimbozoomer.jugcraft.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class AcademyRuneBlock extends Block {
	public static final IntegerProperty RUNE = IntegerProperty.create("rune", 0, 3);
	public AcademyRuneBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(RUNE, 0));
	}
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(RUNE); }
	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer server) {
			// Four fixed candidates, never a world scan or forced chunk load. The ward verifies the transformed position.
			for (int x : new int[]{-4, 4}) for (int z : new int[]{-4, 4}) {
				BlockPos candidate = pos.offset(x, 2, z);
				if (level.hasChunkAt(candidate) && level.getBlockEntity(candidate) instanceof AcademyWardBlockEntity ward
						&& ward.runePosition(state.getValue(RUNE)).equals(pos)) {
					ward.press(server, state.getValue(RUNE), pos);
					return InteractionResult.SUCCESS;
				}
			}
			server.sendSystemMessage(net.minecraft.network.chat.Component.translatable("academy.jugcraft.dormant"));
		}
		return InteractionResult.SUCCESS;
	}
}
