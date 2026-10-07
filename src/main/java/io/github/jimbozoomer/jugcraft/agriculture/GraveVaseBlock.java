package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The grave vase (graveyard pack 4): a bronze vase to bring flowers to a grave. Any small flower put in it (item tag
 * {@code jugcraft:grave_flowers}) makes a bouquet (its colour from the item tags {@code jugcraft:grave_flowers/<colour>},
 * mixed for any other); fresh flowers
 * wilt in time ({@value #WILT_CHANCE} of its random ticks, about a day of play), and new ones replace the old. While
 * fresh, they calm the graves within {@value #CALM_REACH} blocks: those stir restless spirits only {@value #CALM} as
 * often ({@link #calms}). Shears clear it. Changing it needs build rights.
 */
public class GraveVaseBlock extends Block {
	public static final float WILT_CHANCE = 0.05F;
	public static final int CALM_REACH = 3;
	public static final float CALM = 0.5F;
	public static final EnumProperty<Bouquet> FLOWERS = EnumProperty.create("flowers", Bouquet.class);
	/** Every flower the vase takes: each colour's, and every other small flower. */
	public static final TagKey<Item> FLOWERS_IT_TAKES = TagKey.create(Registries.ITEM, Jugcraft.id("grave_flowers"));
	public static final BooleanProperty WILTED = BooleanProperty.create("wilted");
	private static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 9.0, 11.0);
	private static final VoxelShape WITH_FLOWERS = Block.box(5.0, 0.0, 5.0, 11.0, 15.0, 11.0);

	/** What is in the vase. */
	public enum Bouquet implements StringRepresentable {
		NONE, WHITE, RED, YELLOW, PURPLE, MIXED;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}

		/** The bouquet {@code flower} makes: the colour whose tag lists it, or mixed. */
		static Bouquet of(ItemStack flower) {
			for (Bouquet bouquet : new Bouquet[] {WHITE, RED, YELLOW, PURPLE}) {
				if (flower.is(TagKey.create(Registries.ITEM, Jugcraft.id("grave_flowers/" + bouquet.getSerializedName())))) {
					return bouquet;
				}
			}
			return MIXED;
		}
	}

	public GraveVaseBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FLOWERS, Bouquet.NONE).setValue(WILTED, false));
	}

	/** Whether there are fresh flowers in a grave vase within reach of the grave at {@code grave}. */
	public static boolean calms(Level level, BlockPos grave) {
		Block vase = JugcraftAgriculture.block(JugcraftAgriculture.GRAVE_VASE);
		for (BlockPos pos : BlockPos.betweenClosed(grave.offset(-CALM_REACH, -1, -CALM_REACH), grave.offset(CALM_REACH, 1, CALM_REACH))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(vase) && state.getValue(FLOWERS) != Bouquet.NONE && !state.getValue(WILTED)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FLOWERS) == Bouquet.NONE ? SHAPE : WITH_FLOWERS;
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return state.getValue(FLOWERS) != Bouquet.NONE && !state.getValue(WILTED);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (random.nextFloat() < WILT_CHANCE) {
			level.setBlock(pos, state.setValue(WILTED, true), Block.UPDATE_ALL);
		}
	}

	/** A small flower makes a fresh bouquet; shears clear it. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		boolean flower = stack.is(FLOWERS_IT_TAKES);
		boolean shears = stack.is(Items.SHEARS) && state.getValue(FLOWERS) != Bouquet.NONE;
		if (!flower && !shears) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!player.mayBuild()) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		if (shears) {
			level.setBlock(pos, state.setValue(FLOWERS, Bouquet.NONE).setValue(WILTED, false), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
		} else {
			level.setBlock(pos, state.setValue(FLOWERS, Bouquet.of(stack)).setValue(WILTED, false), Block.UPDATE_ALL);
			stack.consume(1, player);
			level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
			server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 4, 0.2, 0.15, 0.2, 0.0);
			if (player instanceof ServerPlayer giver) {
				TrickOrTreat.award(giver, "flowers_for_the_dead");
			}
		}
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		return InteractionResult.SUCCESS;
	}

	/** Used with anything else, it says what goes in it. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && state.getValue(FLOWERS) == Bouquet.NONE && player instanceof ServerPlayer visitor) {
			visitor.sendOverlayMessage(Component.translatable("message.jugcraft.grave_vase.not_a_flower"));
		}
		return InteractionResult.PASS;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FLOWERS, WILTED);
	}
}
