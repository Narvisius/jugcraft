package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Seeds of a climbing crop, such as tomato seeds. Used on a {@link TrellisBlock} that stands on
 * farmland, they plant the crop in it; anywhere else they do nothing. The server checks that the
 * player may build there (spawn protection, claims).
 */
public class TrellisSeedItem extends Item {
	private final TallCrop crop;

	public TrellisSeedItem(Properties properties, TallCrop crop) {
		super(properties);
		this.crop = crop;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Player player = context.getPlayer();
		TallCropBlock block = JugcraftAgriculture.TALL_CROPS.get(crop);
		BlockState planted = block.defaultBlockState();
		if (!(level.getBlockState(pos).getBlock() instanceof TrellisBlock) || !planted.canSurvive(level, pos)
				|| (player != null && (!player.mayBuild() || !level.mayInteract(player, pos)))) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, planted, Block.UPDATE_ALL);
			level.playSound(null, pos, SoundType.CROP.getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
			ItemStack stack = context.getItemInHand();
			if (player == null || !player.getAbilities().instabuild) {
				stack.shrink(1);
			}
		}
		return InteractionResult.SUCCESS;
	}
}
