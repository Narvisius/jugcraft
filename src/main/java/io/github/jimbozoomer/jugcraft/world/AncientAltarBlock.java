package io.github.jimbozoomer.jugcraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Ancient Altar on the top of a knoll of Adam-ondi-Ahman (docs/features/adam-ondi-ahman.md): a stone older than the
 * altars it lies on, carved with a red star. It cannot be broken. For now it is dormant, and says so when used; the
 * Altar Keystone will wake it (docs/branches/BOSSES.md, part 2).
 */
public class AncientAltarBlock extends Block {
	public static final String DORMANT = "message.jugcraft.adam_ondi_ahman.dormant";

	public AncientAltarBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer server) {
			server.sendOverlayMessage(Component.translatable(DORMANT));
			level.playSound(null, pos, SoundType.STONE.getHitSound(), SoundSource.BLOCKS, 0.8F, 0.6F);
		}
		return InteractionResult.SUCCESS;
	}
}
