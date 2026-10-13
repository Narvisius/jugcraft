package io.github.jimbozoomer.jugcraft.concordance.garden;

import io.github.jimbozoomer.jugcraft.concordance.ComposeText;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Organism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Habitat Gauge's block: a comparator reads its signal. An empty hand chooses what it reads (its keeper's party
 * only); a crop (or a Verdant Heart) used on it makes it judge the habitat for that organism. {@link #OPEN} shows its
 * bulb open while it gives a signal. Everything it does is {@link HabitatGaugeBlockEntity}.
 */
public class HabitatGaugeBlock extends LivingDeviceBlock {
	public static final BooleanProperty OPEN = BooleanProperty.create("open");

	public HabitatGaugeBlock(Properties properties) {
		super(properties, Block.box(4.0, 0.0, 4.0, 12.0, 15.0, 12.0), false);
		registerDefaultState(stateDefinition.any().setValue(OPEN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(OPEN);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HabitatGaugeBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, Garden.GAUGE_ENTITY, (tickLevel, pos, tickState, gauge) -> gauge.serverTick((ServerLevel) tickLevel));
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof HabitatGaugeBlockEntity gauge ? gauge.signal() : 0;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		Organism organism = stack.isEmpty() ? null : Garden.catalog().byItem(item);
		if (organism == null && stack.is(Garden.VERDANT_HEART.asItem())) {
			organism = Garden.catalog().byBlock(BuiltInRegistries.BLOCK.getKey(Garden.VERDANT_HEART).toString());
		}
		if (organism == null) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel
				&& level.getBlockEntity(pos) instanceof HabitatGaugeBlockEntity gauge) {
			if (!RateGate.allow(server, "garden", 4)) {
				return InteractionResult.FAIL;
			}
			if (!gauge.mayChange(server)) {
				Garden.tell(server, "garden.not_owner");
				return InteractionResult.FAIL;
			}
			gauge.attune(serverLevel, organism.id());
			Garden.tell(server, "garden.gauge_attuned", stack.getHoverName());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void useEmpty(LivingDeviceBlockEntity device, ServerPlayer player, ServerLevel level) {
		if (!(device instanceof HabitatGaugeBlockEntity gauge)) {
			return;
		}
		if (player.isShiftKeyDown() || !gauge.mayChange(player)) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.garden.gauge", mode(gauge.mode()), gauge.signal()));
			return;
		}
		gauge.cycle(level);
		Garden.tell(player, "garden.gauge_mode", mode(gauge.mode()));
	}

	static Component mode(HabitatGaugeBlockEntity.Mode mode) {
		return mode.factor == null ? ComposeText.show(Text.of("ecology.gauge.suitability"))
				: ComposeText.name(new Text.Ref("factor", mode.factor.id));
	}
}
