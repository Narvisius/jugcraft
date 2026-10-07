package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Wax Melting Pot, where Aura Candles are made ({@link WaxPotBlockEntity} holds the wax). Set it over a heat source
 * and use on it:
 * <ul>
 * <li>honeycomb (beeswax) or rotten flesh (tallow) to put wax in; it melts over the heat;</li>
 * <li>once some is molten, dyes to colour it, scents to scent it, glowstone dust to brighten it and redstone to make
 * it burn longer;</li>
 * <li>string, to dip a wick and start a candle (a measure of wax);</li>
 * <li>an Aura Candle, to dip it again and add a layer, once its last layer has cooled (the candle's cooldown, shown on
 * the hotbar); dipped while still warm, the new layer slides off and its wax is lost;</li>
 * <li>an empty hand, to see what is in it; sneaking, to pour it all away.</li>
 * </ul>
 */
public class WaxPotBlock extends BaseEntityBlock {
	private static final String MESSAGES = "message.jugcraft.wax_melting_pot.";
	private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 11.0, 14.0);

	public WaxPotBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WaxPotBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.WAX_POT_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((WaxPotBlockEntity) entity).serverTick((ServerLevel) tickLevel);
	}

	/** Whether the pot takes {@code stack} for something (the server decides whether it can). */
	private static boolean handles(ItemStack stack) {
		return CandleWax.of(stack) != null || ScarecrowBlock.dyeColor(stack) != null || CandleScent.of(stack) != null
				|| stack.is(WaxPotBlockEntity.BRIGHTENERS) || stack.is(WaxPotBlockEntity.EXTENDERS) || stack.is(Items.STRING)
				|| stack.has(JugcraftAgriculture.CANDLE_MIX);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!handles(stack)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof WaxPotBlockEntity pot)) {
			return InteractionResult.SUCCESS;
		}
		CandleWax wax = CandleWax.of(stack);
		if (wax != null) {
			if (pot.addWax(wax)) {
				stack.consume(1, player);
				level.playSound(null, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 0.8F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			} else {
				player.sendOverlayMessage(Component.translatable(MESSAGES + (pot.wax() != null && pot.wax() != wax ? "other_wax" : "full")));
			}
			return InteractionResult.SUCCESS;
		}
		if (pot.molten() <= 0) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + "not_molten"));
			return InteractionResult.SUCCESS;
		}
		DyeColor dye = ScarecrowBlock.dyeColor(stack);
		CandleScent scent = CandleScent.of(stack);
		if (dye != null) {
			pot.addDye(dye);
			stir(level, pos, player, stack, SoundEvents.DYE_USE);
		} else if (scent != null) {
			if (pot.canScent(scent)) {
				pot.addScent(scent);
				stir(level, pos, player, stack, SoundEvents.BREWING_STAND_BREW);
			} else {
				player.sendOverlayMessage(Component.translatable(MESSAGES + (pot.scents().contains(scent) ? "already" : "two_scents")));
			}
		} else if (stack.is(WaxPotBlockEntity.BRIGHTENERS) || stack.is(WaxPotBlockEntity.EXTENDERS)) {
			boolean brightener = stack.is(WaxPotBlockEntity.BRIGHTENERS);
			if (brightener ? pot.bright() : pot.lasting()) {
				player.sendOverlayMessage(Component.translatable(MESSAGES + "already"));
			} else {
				if (brightener) {
					pot.brighten();
				} else {
					pot.extend();
				}
				stir(level, pos, player, stack, SoundEvents.BREWING_STAND_BREW);
			}
		} else if (stack.is(Items.STRING)) {
			CandleMix mix = CandleMix.first(pot.wax(), pot.color(), pot.scents(), pot.bright(), pot.lasting());
			stack.consume(1, player);
			pot.useMeasure();
			give(level, pos, player, hand, AuraCandleItem.make(mix));
			player.sendOverlayMessage(Component.translatable(MESSAGES + "dipped", 1, AuraCandleBlock.MAX_DIPS));
		} else {
			dip(stack, level, pos, player, hand, pot);
		}
		return InteractionResult.SUCCESS;
	}

	/** Dips a candle again: a layer once it has cooled, lost if it is still warm. */
	private static void dip(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand, WaxPotBlockEntity pot) {
		CandleMix mix = stack.get(JugcraftAgriculture.CANDLE_MIX);
		if (mix.dips() >= AuraCandleBlock.MAX_DIPS) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + "full_size"));
			return;
		}
		if (player.getCooldowns().isOnCooldown(stack)) {
			pot.useMeasure();
			level.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 1.0F, 1.0F);
			player.sendOverlayMessage(Component.translatable(MESSAGES + "too_warm"));
			return;
		}
		CandleMix dipped = mix.dip(pot.wax(), pot.color(), pot.scents(), pot.bright(), pot.lasting());
		ItemStack candle = AuraCandleItem.make(dipped);
		stack.consume(1, player);
		pot.useMeasure();
		give(level, pos, player, hand, candle);
		player.sendOverlayMessage(Component.translatable(MESSAGES + (dipped.muddled() ? "muddled" : "dipped"), dipped.dips(), AuraCandleBlock.MAX_DIPS));
	}

	/** Hands a freshly dipped candle to the player (still warm: its cooldown) with a plop. */
	private static void give(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack candle) {
		player.getCooldowns().addCooldown(candle, WaxPotBlockEntity.COOL_TICKS);
		level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 0.7F);
		if (player.getItemInHand(hand).isEmpty()) {
			player.setItemInHand(hand, candle);
		} else if (!player.getInventory().add(candle)) {
			Block.popResource(level, pos.above(), candle);
		}
	}

	/** Stirs one of {@code stack} in, handing back what it leaves (a bottle of ectoplasm leaves its bottle). */
	private static void stir(Level level, BlockPos pos, Player player, ItemStack stack, net.minecraft.sounds.SoundEvent sound) {
		ItemStackTemplate remainder = stack.getItem().getCraftingRemainder();
		stack.consume(1, player);
		if (remainder != null && !player.getAbilities().instabuild) {
			ItemStack left = remainder.create();
			if (!player.getInventory().add(left)) {
				Block.popResource(level, pos.above(), left);
			}
		}
		level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
	}

	/** An empty hand shows what is in the pot; sneaking, it pours the pot away. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof WaxPotBlockEntity pot)) {
			return InteractionResult.SUCCESS;
		}
		if (pot.wax() == null) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + "empty"));
		} else if (player.isSecondaryUseActive()) {
			pot.empty();
			level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
			player.sendOverlayMessage(Component.translatable(MESSAGES + "poured"));
		} else {
			MutableComponent status = Component.translatable(MESSAGES + "status", pot.wax().displayName(), pot.molten(), pot.total());
			for (CandleScent scent : pot.scents()) {
				status.append(", ").append(scent.displayName());
			}
			if (pot.bright()) {
				status.append(", ").append(Component.translatable(MESSAGES + "bright"));
			}
			if (pot.lasting()) {
				status.append(", ").append(Component.translatable(MESSAGES + "lasting"));
			}
			player.sendOverlayMessage(status);
		}
		return InteractionResult.SUCCESS;
	}

	/** Molten wax bubbles, and its scents drift up from it (client only). */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!(level.getBlockEntity(pos) instanceof WaxPotBlockEntity pot) || pot.molten() <= 0) {
			return;
		}
		double surface = pos.getY() + (1.0 + 8.0 * pot.total() / WaxPotBlockEntity.CAPACITY) / 16.0;
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.3 + random.nextDouble() * 0.4, surface, pos.getZ() + 0.3 + random.nextDouble() * 0.4,
					0.0, 0.0, 0.0);
		}
		for (CandleScent scent : pot.scents()) {
			if (random.nextInt(4) == 0) {
				level.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | scent.color), pos.getX() + 0.3 + random.nextDouble() * 0.4,
						surface + 0.1, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.02, 0.0);
			}
		}
	}
}
