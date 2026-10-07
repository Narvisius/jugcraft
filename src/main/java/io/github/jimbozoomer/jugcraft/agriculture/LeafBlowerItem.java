package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import io.github.jimbozoomer.jugcraft.tools.PoweredToolItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Leaf Blower (fall addition 30): a dieselpunk electric leaf blower, charged at the Charging Station as the other
 * powered tools are ({@link Chargeable}). Held in use it blows, {@value #BLOW_JE} JE a tick: its stream is a cone
 * {@value #RANGE} blocks long, {@value #CONE} degrees either side of where its user looks. Items and experience in it are
 * pushed along it ({@value #PUSH_ITEMS} blocks a tick, less further off), mobs gently ({@value #PUSH_MOBS}, less as they
 * resist knockback), and other players likewise where they could be fought (PvP on); nothing is hurt. Every
 * {@value #PILE_EVERY} ticks each leaf pile within {@value #PILE_RANGE} blocks in the stream gives a layer to the block
 * beyond it, onto a pile of its colour there or a new pile on open ground, so leaves can be herded into a heap; lit
 * candles in the stream blow out.
 *
 * <p>Sneaking, it vacuums ({@value #VACUUM_JE} JE a tick): items within {@value #VACUUM_RANGE} blocks in its cone are
 * drawn in, and every {@value #PILE_EVERY} ticks a layer of each leaf pile (or of vanilla leaf litter) within reach comes
 * up into its user's inventory, for the composter. All of it is the server's, for blocks its user may change.
 */
public class LeafBlowerItem extends Item implements Chargeable {
	public static final String ID = "leaf_blower";
	public static final long CAPACITY = 40000;
	public static final long BLOW_JE = 4;
	public static final long VACUUM_JE = 6;
	public static final double RANGE = 8.0;
	public static final double CONE = 25.0;
	public static final double PUSH_ITEMS = 0.12;
	public static final double PUSH_MOBS = 0.05;
	public static final int PILE_RANGE = 6;
	public static final int PILE_EVERY = 4;
	public static final int VACUUM_RANGE = 4;
	/** The fastest the stream drives anything across, in blocks a tick. */
	public static final double MAX_DRIVE = 0.8;
	private static final int USE_TICKS = 72000;

	public LeafBlowerItem(Properties properties) {
		super(properties);
	}

	@Override
	public long baseCapacity() {
		return CAPACITY;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (Chargeable.energy(stack) < BLOW_JE) {
			if (player instanceof ServerPlayer user) {
				user.sendOverlayMessage(Component.translatable("message.jugcraft.leaf_blower.flat"));
			}
			return InteractionResult.FAIL;
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.NONE;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return USE_TICKS;
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
		if (!(entity instanceof Player player)) {
			return;
		}
		boolean vacuum = player.isShiftKeyDown();
		int used = getUseDuration(stack, entity) - remaining;
		if (!(level instanceof ServerLevel server)) {
			stream(level, player, vacuum);
			return;
		}
		if (!Chargeable.drain(stack, vacuum ? VACUUM_JE : BLOW_JE)) {
			player.stopUsingItem();
			if (player instanceof ServerPlayer user) {
				user.sendOverlayMessage(Component.translatable("message.jugcraft.leaf_blower.flat"));
			}
			return;
		}
		if (used % 20 == 0) {
			server.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), Midway.sound("entity.breeze.idle_air", SoundEvents.WOOL_HIT),
					SoundSource.PLAYERS, 0.6F, vacuum ? 0.8F : 1.4F);
		}
		if (vacuum) {
			vacuum(server, player, used % PILE_EVERY == 0);
		} else {
			blow(server, player, used % PILE_EVERY == 0);
		}
	}

	// ---------------------------------------------------------------- the stream

	/** Whether {@code point} is in the cone {@code range} blocks long and {@code cone} degrees either side of {@code look} from {@code eye}. */
	public static boolean inCone(Vec3 eye, Vec3 look, Vec3 point, double range, double cone) {
		Vec3 to = point.subtract(eye);
		double distance = to.length();
		if (distance > range) {
			return false;
		}
		return distance < 0.5 || to.dot(look) / distance >= Math.cos(Math.toRadians(cone));
	}

	/** The push the stream gives something {@code distance} blocks off: {@code push}, falling off towards its end. */
	public static double falloff(double push, double distance) {
		return push * Mth.clamp(1.0 - distance / (RANGE + 2.0), 0.0, 1.0);
	}

	/** Blows: pushes what is in the stream along it, and, on a leaf tick, the leaves and candles. */
	public static void blow(ServerLevel level, Player player, boolean leafTick) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		for (Entity entity : level.getEntities(player, new AABB(eye, eye).inflate(RANGE))) {
			Vec3 at = entity.getBoundingBox().getCenter();
			if (!inCone(eye, look, at, RANGE, CONE)) {
				continue;
			}
			double distance = at.distanceTo(eye);
			double push;
			if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
				push = falloff(PUSH_ITEMS, distance);
			} else if (entity instanceof Player other && !player.canHarmPlayer(other)) {
				// Other players only where they could be fought (PvP on, not a team-mate).
				continue;
			} else if (entity instanceof LivingEntity living) {
				push = falloff(PUSH_MOBS, distance) * (1.0 - Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0));
			} else {
				continue;
			}
			drive(entity, look.scale(push));
		}
		if (!leafTick) {
			return;
		}
		int moved = blowLeaves(level, player, eye, look);
		blowOutCandles(level, player, eye, look);
		if (moved > 0 && player instanceof ServerPlayer user) {
			TrickOrTreat.award(user, "gone_with_the_wind");
		}
	}

	/**
	 * Adds {@code push} to an entity's motion, no faster across than {@link #MAX_DRIVE}. A player moves themselves, so a
	 * pushed player's client is told.
	 */
	private static void drive(Entity entity, Vec3 push) {
		Vec3 motion = entity.getDeltaMovement().add(push);
		double across = motion.horizontalDistance();
		if (across > MAX_DRIVE) {
			motion = new Vec3(motion.x * MAX_DRIVE / across, motion.y, motion.z * MAX_DRIVE / across);
		}
		entity.setDeltaMovement(motion);
		if (entity instanceof ServerPlayer pushed) {
			pushed.connection.send(new ClientboundSetEntityMotionPacket(pushed));
		}
	}

	/** Whether the leaf blower's user may change the block at {@code pos} (build rights, spawn protection). */
	private static boolean mayChange(ServerLevel level, Player player, BlockPos pos) {
		return player.mayBuild() && level.mayInteract(player, pos);
	}

	/** The blocks round {@code player}, within {@code reach}, that are in the stream and satisfy {@code what}, the furthest first. */
	private static List<BlockPos> inStream(ServerLevel level, Player player, Vec3 eye, Vec3 look, int reach, java.util.function.Predicate<BlockState> what) {
		List<BlockPos> found = new ArrayList<>();
		BlockPos feet = player.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-reach, -2, -reach), feet.offset(reach, 2, reach))) {
			BlockState state = level.getBlockState(pos);
			// The ground is wide to a blower held low: a little wider cone for blocks.
			if (what.test(state) && inCone(eye, look, Vec3.atCenterOf(pos), reach + 0.5, CONE + 10.0) && mayChange(level, player, pos)) {
				found.add(pos.immutable());
			}
		}
		found.sort(Comparator.comparingDouble(pos -> -Vec3.atCenterOf(pos).distanceToSqr(eye)));
		return found;
	}

	/**
	 * Each leaf pile in the stream gives a layer to the block beyond it along the stream, the furthest first: onto a
	 * pile of its colour there (if not full), or a new pile on open ground there or a step down. Against anything else it
	 * stays. Returns how many layers moved.
	 */
	public static int blowLeaves(ServerLevel level, Player player, Vec3 eye, Vec3 look) {
		Vec3 flat = new Vec3(look.x, 0.0, look.z);
		if (flat.lengthSqr() < 0.01) {
			return 0;
		}
		flat = flat.normalize();
		int dx = (int) Math.round(flat.x);
		int dz = (int) Math.round(flat.z);
		int moved = 0;
		for (BlockPos pos : inStream(level, player, eye, look, PILE_RANGE, state -> state.getBlock() instanceof LeafPileBlock)) {
			BlockState pile = level.getBlockState(pos);
			if (!(pile.getBlock() instanceof LeafPileBlock)) {
				continue;
			}
			BlockPos beyond = pos.offset(dx, 0, dz);
			if (!mayChange(level, player, beyond) || !(give(level, pile.getBlock(), beyond) || give(level, pile.getBlock(), beyond.below()))) {
				continue;
			}
			int layers = pile.getValue(LeafPileBlock.LAYERS);
			level.setBlock(pos, layers > 1 ? pile.setValue(LeafPileBlock.LAYERS, layers - 1) : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, pile), pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 6,
					0.3, 0.1, 0.3, 0.05);
			moved++;
		}
		return moved;
	}

	/** Puts a layer of {@code pile} at {@code pos}: onto a pile of it there with room, or a new one on sturdy ground. */
	private static boolean give(ServerLevel level, Block pile, BlockPos pos) {
		BlockState there = level.getBlockState(pos);
		if (there.is(pile)) {
			int layers = there.getValue(LeafPileBlock.LAYERS);
			if (layers >= LeafPileBlock.MAX_LAYERS) {
				return false;
			}
			level.setBlock(pos, there.setValue(LeafPileBlock.LAYERS, layers + 1), Block.UPDATE_ALL);
			return true;
		}
		BlockState fresh = pile.defaultBlockState();
		if (there.isAir() && fresh.canSurvive(level, pos)) {
			level.setBlock(pos, fresh, Block.UPDATE_ALL);
			return true;
		}
		return false;
	}

	/** Blows out the lit candles (and candle cakes) in the stream. */
	public static int blowOutCandles(ServerLevel level, Player player, Vec3 eye, Vec3 look) {
		List<BlockPos> lit = inStream(level, player, eye, look, PILE_RANGE,
				state -> state.getBlock() instanceof AbstractCandleBlock && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT));
		for (BlockPos pos : lit) {
			AbstractCandleBlock.extinguish(player, level.getBlockState(pos), level, pos);
		}
		return lit.size();
	}

	/** Vacuums: draws in the items in reach, and, on a leaf tick, takes up a layer of each pile or leaf litter in reach. */
	public static void vacuum(ServerLevel level, Player player, boolean leafTick) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		for (Entity entity : level.getEntities(player, new AABB(eye, eye).inflate(VACUUM_RANGE))) {
			Vec3 at = entity.getBoundingBox().getCenter();
			if ((entity instanceof ItemEntity || entity instanceof ExperienceOrb) && inCone(eye, look, at, VACUUM_RANGE, CONE * 1.5)) {
				drive(entity, eye.subtract(at).normalize().scale(PUSH_ITEMS));
			}
		}
		if (!leafTick) {
			return;
		}
		Block litter = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("leaf_litter"));
		for (BlockPos pos : inStream(level, player, eye, look, VACUUM_RANGE,
				state -> state.getBlock() instanceof LeafPileBlock || state.is(litter) && litter != Blocks.AIR)) {
			BlockState state = level.getBlockState(pos);
			if (takeLayer(level, pos, state)) {
				// Into the inventory, or at its user's feet if it is full.
				player.getInventory().placeItemBackInInventory(new ItemStack(state.getBlock().asItem()), Prediction.SERVER_ONLY);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 4,
						0.2, 0.05, 0.2, 0.02);
			}
		}
	}

	/** Takes a layer (or a piece of leaf litter) off the block at {@code pos}; returns whether there was one. */
	private static boolean takeLayer(ServerLevel level, BlockPos pos, BlockState state) {
		Property<?> property = state.getBlock() instanceof LeafPileBlock ? LeafPileBlock.LAYERS : state.getBlock().getStateDefinition().getProperty("segment_amount");
		if (property instanceof IntegerProperty count && state.getValue(count) > 1) {
			level.setBlock(pos, state.setValue(count, state.getValue(count) - 1), Block.UPDATE_ALL);
			return true;
		}
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		return true;
	}

	/** The stream as its user's client draws it: puffs of air along it (or drawn in, vacuuming). */
	private static void stream(Level level, Player player, boolean vacuum) {
		Vec3 look = player.getLookAngle();
		Vec3 nozzle = player.getEyePosition().add(look.scale(1.2)).add(0.0, -0.35, 0.0);
		for (int i = 0; i < 2; i++) {
			double spread = 0.15;
			Vec3 jitter = new Vec3(level.getRandom().nextGaussian() * spread, level.getRandom().nextGaussian() * spread, level.getRandom().nextGaussian() * spread);
			if (vacuum) {
				Vec3 from = nozzle.add(look.scale(VACUUM_RANGE * level.getRandom().nextDouble()));
				Vec3 in = nozzle.subtract(from).scale(0.15).add(jitter.scale(0.1));
				level.addParticle(ParticleTypes.WHITE_ASH, from.x, from.y, from.z, in.x, in.y, in.z);
			} else {
				Vec3 out = look.scale(0.6).add(jitter);
				level.addParticle(ParticleTypes.CLOUD, nozzle.x, nozzle.y, nozzle.z, out.x * 0.5, out.y * 0.5, out.z * 0.5);
			}
		}
	}

	// ---------------------------------------------------------------- the charge bar and tooltip

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return PoweredToolItem.barWidth(stack);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return PoweredToolItem.BAR_COLOR;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(PoweredToolItem.energyLine(stack));
		tooltip.accept(Component.translatable("item.jugcraft.leaf_blower.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
