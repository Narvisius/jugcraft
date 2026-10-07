package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Bubbling Cauldron: a witch's iron pot on three legs. A water bucket fills it; a brew ingredient then turns the
 * water to a brew ({@link Brew}: green from spider eyes and slime, purple from nether wart and chorus, orange from
 * glowstone and blaze powder), which glows (light {@value #BREW_LIGHT}). An empty bucket pours the water out again
 * (pouring a brew gives the water back too). Over a fire, campfire, magma or lava (the Cooking Pot's heat sources) it
 * bubbles and steams, drawn by clients only.
 *
 * <p>Hex brewing (fall addition 21): a brew bubbling over a heat source turns into a hex brew when its hex ingredient
 * is stirred in (item tags {@code jugcraft:hex/<hex>}): the green brew and a brown mushroom make the Shrinking Draught,
 * the orange brew and beans the Giant's Draught, the purple brew and a phantom membrane Flying Ointment. A hex brew
 * holds {@value #DOSES} doses ({@link #DOSES_LEFT}); each glass bottle draws one ({@link Hexes}), and the pot is empty
 * when the last is drawn. Changing the pot needs build rights.
 */
public class BubblingCauldronBlock extends Block {
	public static final int BREW_LIGHT = 7;
	/** How many bottles a hex brew fills. */
	public static final int DOSES = 3;
	public static final EnumProperty<Brew> CONTENTS = EnumProperty.create("contents", Brew.class);
	public static final IntegerProperty DOSES_LEFT = IntegerProperty.create("doses", 1, DOSES);
	private static final VoxelShape SHAPE = Shapes.or(Block.box(2.0, 2.0, 2.0, 14.0, 13.0, 14.0), Block.box(3.0, 0.0, 3.0, 13.0, 2.0, 13.0));

	/** What is in the cauldron, and for a brew the item tag that makes it. */
	public enum Brew implements StringRepresentable {
		EMPTY, WATER, GREEN, PURPLE, ORANGE, SHRINKING, GIANT, FLYING;

		/** The items that turn water into this brew ({@code jugcraft:brew/<colour>}), or null for anything but a brew. */
		public TagKey<Item> ingredients() {
			return isBrew() ? TagKey.create(Registries.ITEM, Jugcraft.id("brew/" + getSerializedName())) : null;
		}

		/** A coloured brew, made from water. */
		public boolean isBrew() {
			return ordinal() >= GREEN.ordinal() && ordinal() <= ORANGE.ordinal();
		}

		/** A hex brew, made from a brew over the fire. */
		public boolean isHex() {
			return ordinal() >= SHRINKING.ordinal();
		}

		/** For a hex brew, the brew it is made from; null for anything else. */
		public Brew base() {
			return switch (this) {
				case SHRINKING -> GREEN;
				case GIANT -> ORANGE;
				case FLYING -> PURPLE;
				default -> null;
			};
		}

		/** For a hex brew, the items that turn its brew into it ({@code jugcraft:hex/<hex>}). */
		public TagKey<Item> hexIngredients() {
			return isHex() ? TagKey.create(Registries.ITEM, Jugcraft.id("hex/" + getSerializedName())) : null;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public BubblingCauldronBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(CONTENTS, Brew.EMPTY).setValue(DOSES_LEFT, DOSES));
	}

	public static int light(BlockState state) {
		Brew contents = state.getValue(CONTENTS);
		return contents.isBrew() || contents.isHex() ? BREW_LIGHT : 0;
	}

	/** The hex brew {@code stack} makes of the brew {@code contents}, or null. */
	public static Brew hexFor(Brew contents, ItemStack stack) {
		for (Brew hex : Brew.values()) {
			if (hex.isHex() && hex.base() == contents && stack.is(hex.hexIngredients())) {
				return hex;
			}
		}
		return null;
	}

	/** The brew {@code stack} turns water into, or null if it is no brew ingredient. */
	public static Brew brewFor(ItemStack stack) {
		for (Brew brew : Brew.values()) {
			if (brew.isBrew() && stack.is(brew.ingredients())) {
				return brew;
			}
		}
		return null;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		Brew contents = state.getValue(CONTENTS);
		if (!player.mayBuild() && (stack.is(Items.WATER_BUCKET) || stack.is(Items.BUCKET) || stack.is(Items.GLASS_BOTTLE)
				|| brewFor(stack) != null || hexFor(contents, stack) != null)) {
			return InteractionResult.PASS;
		}
		Brew hex = hexFor(contents, stack);
		if (hex != null) {
			if (!isHeated(level, pos)) {
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
			if (!level.isClientSide()) {
				stack.consume(1, player);
				level.setBlock(pos, state.setValue(CONTENTS, hex).setValue(DOSES_LEFT, DOSES), Block.UPDATE_ALL);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
				level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 0.5F);
				if (level instanceof ServerLevel server) {
					server.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 16, 0.3, 0.2, 0.3, 0.05);
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(Items.GLASS_BOTTLE) && contents.isHex()) {
			if (!level.isClientSide()) {
				int left = state.getValue(DOSES_LEFT);
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Hexes.draught(contents))));
				level.setBlock(pos, left > 1 ? state.setValue(DOSES_LEFT, left - 1) : state.setValue(CONTENTS, Brew.EMPTY).setValue(DOSES_LEFT, DOSES),
						Block.UPDATE_ALL);
				level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
				level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(Items.WATER_BUCKET) && contents == Brew.EMPTY) {
			if (!level.isClientSide()) {
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
				change(level, pos, state, Brew.WATER, player);
				level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(Items.BUCKET) && contents != Brew.EMPTY) {
			if (!level.isClientSide()) {
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.WATER_BUCKET)));
				change(level, pos, state, Brew.EMPTY, player);
				level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
			}
			return InteractionResult.SUCCESS;
		}
		Brew brew = brewFor(stack);
		if (brew != null && contents != Brew.EMPTY && !contents.isHex() && contents != brew) {
			if (!level.isClientSide()) {
				stack.consume(1, player);
				change(level, pos, state, brew, player);
				level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 0.8F);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	/** Whether the pot stands over one of the Cooking Pot's heat sources. */
	public static boolean isHeated(Level level, BlockPos pos) {
		return CookingPotBlockEntity.isHeated(level, pos);
	}

	private static void change(Level level, BlockPos pos, BlockState state, Brew contents, Player player) {
		level.setBlock(pos, state.setValue(CONTENTS, contents).setValue(DOSES_LEFT, DOSES), Block.UPDATE_ALL);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
	}

	/** Over a heat source, its brew or water bubbles; a brew gives off witchy sparkles too. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		Brew contents = state.getValue(CONTENTS);
		if (contents == Brew.EMPTY || !CookingPotBlockEntity.isHeated(level, pos)) {
			return;
		}
		double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
		double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
		double y = pos.getY() + 12.2 / 16.0;
		level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0.0, 0.02, 0.0);
		if (random.nextInt(3) == 0) {
			level.addParticle(contents.isBrew() || contents.isHex() ? ParticleTypes.WITCH : ParticleTypes.CLOUD, x, y + 0.1, z, 0.0, 0.03, 0.0);
		}
		if (contents.isHex() && random.nextInt(2) == 0) {
			level.addParticle(ParticleTypes.ENCHANT, x, y + 0.4, z, 0.0, 0.2, 0.0);
		}
		if (random.nextInt(12) == 0) {
			level.playLocalSound(x, y, z, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.6F, 0.8F + random.nextFloat() * 0.4F, false);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CONTENTS, DOSES_LEFT);
	}
}
