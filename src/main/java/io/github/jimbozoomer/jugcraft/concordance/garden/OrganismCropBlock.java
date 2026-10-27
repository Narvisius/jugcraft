package io.github.jimbozoomer.jugcraft.concordance.garden;

import io.github.jimbozoomer.jugcraft.agriculture.CropGrowth;
import io.github.jimbozoomer.jugcraft.chemistry.FertilizerItem;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ComposeText;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.Illumination;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Code;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Graft;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Habitat;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Niche;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Organism;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Verdict;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * One of the Greenwardens' crops (roadmap step 14): a vanilla crop with {@value #MAX_AGE} steps that grows only in an
 * awake Verdant Bed, on random ticks (vanilla's, and a sprinkler's: both reach it as they reach any crop), as its
 * organism's niche allows ({@link #grow}). Its organism is data ({@code concordance/organism}); this block only knows
 * which one it is.
 * <p>
 * Bone meal and fertilizer feed its bed rather than forcing a step, so they are a nutrient source like any other (and
 * compost: Concordance Chunk C, C-08, C-P61); used on a growing crop they are their own use, as on a sweet berry bush,
 * bone meal by the bed's own rule (rate-limited, for someone who may change the bed). Ripe, an empty hand harvests it (its
 * produce and chaff, and it falls back to its replanting step); otherwise an empty hand tends its bed as on the bed itself
 * (C-08, C-P61) and says how it grows.
 * <p>
 * Concordance Chunk C (C-08, C-P58, C-P59): a growth attempt first counts a harvest its bed saw
 * ({@link VerdantBedBlockEntity#settle}); a step taken to ripe tells the bed; a mixed bed's bonus is in the step's chance
 * only ({@link #speed}), never in what it costs.
 * <p>
 * Concordance Chunk C (C-09, C-P62 to C-P65): a Greenwarden's plain use of a donor's item grafts its trait onto the bed
 * (graftBed); its organism grows by the niche its bed gives it ({@link VerdantBedBlockEntity#niche}), and a crop of
 * another organism growing there clears the graft.
 */
public class OrganismCropBlock extends CropBlock {
	public static final int MAX_AGE = Organism.STAGES;
	public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
	/** The least a step forced by a Verdant Spire costs its bed, whatever the crop (roadmap step 29). */
	public static final int HASTENED_COST = 1;

	private final Identifier organism;

	public OrganismCropBlock(Properties properties, Identifier organism) {
		super(properties);
		this.organism = organism;
	}

	/** The organism this crop is, as data names it (and the item that plants it). */
	public String organismId() {
		return organism.toString();
	}

	public @Nullable Organism organism() {
		return Garden.catalog().organism(organismId());
	}

	@Override
	protected IntegerProperty getAgeProperty() {
		return AGE;
	}

	@Override
	public int getMaxAge() {
		return MAX_AGE;
	}

	@Override
	protected ItemLike getBaseSeedId() {
		return BuiltInRegistries.ITEM.getValue(organism);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(Garden.VERDANT_BED);
	}

	/** Only the bed matters: light is the niche's business, so a shade crop survives the dark. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockPos below = pos.below();
		return mayPlaceOn(level.getBlockState(below), level, below);
	}

	/**
	 * A crop newly set on a bed makes the bed forget the last plant's verdict, so Jade never shows another plant's reasons
	 * until this one's first growth attempt notes its own (Concordance Chunk C, C-07, C-P55; the unit review). Only on the
	 * server (mc263src LevelChunk.setBlockState); a harvest, a growth step or a bee changes this same block, so none forgets.
	 */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!oldState.is(this) && level.getBlockEntity(pos.below()) instanceof VerdantBedBlockEntity bed) {
			bed.forgetVerdict();
		}
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return !isMaxAge(state);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		grow(level, pos, state, random, false);
	}

	/** What a growth attempt came to. */
	public enum Growth {
		GREW, RIPE, DISABLED, NO_BED, DORMANT, WAITING, STALLED, NOT_THIS_TICK
	}

	/**
	 * One growth attempt, as a random tick makes it: in an awake bed and within the niche, a step with the chance a crop
	 * of this growth time and pace has ({@link CropGrowth}: the soil round it, a legume beside it, the Harvest Moon),
	 * or for certain with {@code certain} (tests). A step costs the organism's nutrients from the bed (or, for a fixer,
	 * gives its nutrients to the poorest bed round it) and dries the bed by one.
	 */
	public Growth grow(ServerLevel level, BlockPos pos, BlockState state, RandomSource random, boolean certain) {
		return grow(level, pos, state, random, certain, false);
	}

	/**
	 * One step forced by a Verdant Spire's field (roadmap step 29): by the crop's own rules (its awake bed, its niche),
	 * for certain, but costing at least {@value #HASTENED_COST} nutrient a step and fixing none. Time grows a fixer for
	 * nothing and it fixes as it grows; a hastened step skips that time, so it must neither be free nor make nutrients,
	 * or a spire's field would mint them (and through a Verdant Heart, Ley Charge) without end.
	 */
	public Growth hasten(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
		return grow(level, pos, state, random, true, true);
	}

	private Growth grow(ServerLevel level, BlockPos pos, BlockState state, RandomSource random, boolean certain, boolean hastened) {
		if (isMaxAge(state)) {
			return Growth.RIPE;
		}
		Organism definition = organism();
		if (!Garden.enabled() || definition == null || !definition.crop()) {
			return Growth.DISABLED;
		}
		if (!(level.getBlockEntity(pos.below()) instanceof VerdantBedBlockEntity bed)) {
			return Growth.NO_BED;
		}
		// Concordance Chunk C (C-09, C-P64): a crop of another organism growing here clears the bed's graft (reading never does).
		bed.clearGraftUnlessFor(organismId());
		// Concordance Chunk C (C-08, C-P59): growing below ripe, it is a harvest the bed saw it ripen for, whatever took it.
		bed.settle();
		if (!bed.awake()) {
			bed.note("dormant", List.of());
			return Growth.DORMANT;
		}
		Habitat habitat = bed.habitat(level);
		if (habitat == null) {
			bed.note("waiting", List.of());
			return Growth.WAITING;
		}
		Verdict verdict = Verdict.of(bed.niche(definition), habitat);
		bed.note(verdict, habitat, definition);
		if (verdict.growth() == Verdict.Growth.STALLED) {
			return Growth.STALLED;
		}
		float speed = speed(level, pos, definition, verdict, bed);
		if (!certain && random.nextInt(CropGrowth.chanceDivisor(speed, (float) definition.growth())) != 0) {
			return Growth.NOT_THIS_TICK;
		}
		// The niche's nutrient floor is at least the cost, so a bed that passed it can pay (a hastened step may still not).
		// A mixed bed's bonus is in the speed only: a step costs the same, so it mints nothing (C-P58).
		if (!bed.take(hastened ? Math.max(HASTENED_COST, definition.cost()) : definition.cost())) {
			return Growth.STALLED;
		}
		int age = getAge(state) + 1;
		level.setBlock(pos, getStateForAge(age), Block.UPDATE_CLIENTS);
		bed.dry(level);
		if (age >= MAX_AGE) {
			// Ripe: the next time it stands below ripe, it was harvested (C-08).
			bed.ripen();
		}
		if (!hastened && definition.fix() > 0) {
			Garden.nourish(Garden.bedAndNeighbours(level, pos.below()), definition.fix());
		}
		return Growth.GREW;
	}

	/**
	 * The speed a growth attempt steps at: CropGrowth's (the soil round it, a legume beside it, the Harvest Moon) times its
	 * pace (half while a factor is only tolerable), and in a mixed bed its neighbours' bonus (Concordance Chunk C, C-08,
	 * C-P58). It sets only the chance of a step; what a step costs never depends on it.
	 */
	public float speed(ServerLevel level, BlockPos pos, Organism definition, Verdict verdict, VerdantBedBlockEntity bed) {
		float speed = CropGrowth.speed(level, pos, definition.fix() > 0) * verdict.growth().pace / 2.0F;
		return bed.mixed(definition) ? speed * (100 + definition.neighbours().percent()) / 100.0F : speed;
	}

	/** What harvesting it ripe yields: its produce and its chaff. Empty while it is not ripe. */
	public List<ItemStack> harvestYield(BlockState state) {
		Organism definition = organism();
		if (!isMaxAge(state) || definition == null) {
			return List.of();
		}
		Item produce = BuiltInRegistries.ITEM.getValue(organism);
		return definition.chaff() > 0 ? List.of(new ItemStack(produce, definition.produce()), new ItemStack(Garden.VERDANT_CHAFF, definition.chaff()))
				: List.of(new ItemStack(produce, definition.produce()));
	}

	/** Harvests it if ripe: it falls back to its replanting step; returns what it yielded (empty if it was not ripe). */
	public List<ItemStack> harvest(ServerLevel level, BlockPos pos, BlockState state) {
		List<ItemStack> out = harvestYield(state);
		Organism definition = organism();
		if (out.isEmpty() || definition == null) {
			return List.of();
		}
		level.setBlock(pos, getStateForAge(definition.replant()), Block.UPDATE_CLIENTS);
		level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 0.8F, 1.0F);
		return out;
	}

	/**
	 * Concordance Chunk C (C-08, C-P61): fertilizer on a growing crop is its own use (its own area rule, as on any crop), and
	 * bone meal on one feeds the bed below as bone meal on the bed does (rate-limited, for someone who may change the bed),
	 * which is compost; as on vanilla's sweet berry bush, a ripe crop still harvests. C-09 (C-P62): a Greenwarden's plain use
	 * of a graft's donor on its host grafts the bed beneath (graftBed), at any age, and every outcome consumes the use
	 * (a refusal neither harvests, reports nor plants; sneaking with an item never reaches a block, mc263src
	 * ServerPlayerGameMode.useItemOn). Anything else, and anyone else's donor, goes on as before.
	 */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (!isMaxAge(state) && stack.getItem() instanceof FertilizerItem) {
			return InteractionResult.PASS; // its own area rule, as on any crop
		}
		if (!isMaxAge(state) && stack.is(Items.BONE_MEAL)) {
			// As bone meal on the bed: rate-limited, for someone who may change the bed; compost.
			if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel
					&& level.getBlockEntity(pos.below()) instanceof VerdantBedBlockEntity bed) {
				if (!RateGate.allow(server, "garden", 4) || !Illumination.mayChange(serverLevel, player, pos.below())) {
					return InteractionResult.CONSUME; // refused: the bone meal's own use must not feed it either
				}
				if (bed.give(Garden.BONE_MEAL_NUTRIENTS) == 0) {
					Garden.tell(server, "garden.bed_full");
					return InteractionResult.CONSUME;
				}
				bed.compost();
				stack.consume(1, player);
				level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 5);
				Garden.tell(server, "garden.fed", bed.nutrients(), VerdantBedBlockEntity.CAPACITY);
			}
			return InteractionResult.SUCCESS;
		}
		// Concordance Chunk C (C-09, C-P62): a Greenwarden's donor grafts; anyone else's is just an item.
		if (stack.isEmpty() || !(player instanceof ServerPlayer server) || !(level instanceof ServerLevel serverLevel)
				|| !Garden.enabled() || !Garden.knows(server)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		Graft graft = Garden.catalog().graft(organismId(), ConcordanceProgress.itemId(stack));
		if (graft == null) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		return graftBed(serverLevel, pos, server, stack, graft);
	}

	/**
	 * Grafts {@code graft} onto the bed beneath for {@code player}, who holds its donor (C-P62): at most once every four ticks
	 * (RateGate), only where they could change both this crop and its bed by hand (Authority), and only on a bed with no
	 * graft the rules know (one trait a bed; one they no longer know is replaced). Spends one donor; changes nothing else.
	 */
	private InteractionResult graftBed(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack donor, Graft graft) {
		if (!RateGate.allow(player, "garden", 4)) {
			return InteractionResult.CONSUME;
		}
		if (!(level.getBlockEntity(pos.below()) instanceof VerdantBedBlockEntity bed)) {
			Garden.tell(player, "garden.no_bed");
			return InteractionResult.CONSUME;
		}
		if (!Authority.mayChange(level, player, pos) || !Authority.mayChange(level, player, pos.below())) {
			player.sendOverlayMessage(ComposeText.show(Diagnostic.of(Code.MISSING_PERMISSION).text()));
			return InteractionResult.CONSUME;
		}
		if (!bed.graft().isEmpty() && Garden.catalog().graft(bed.graft()) != null) {
			Garden.tell(player, "garden.graft_held", ComposeText.name(new Text.Ref("graft", bed.graft())));
			return InteractionResult.CONSUME;
		}
		bed.setGraft(graft.id());
		// The niche changed: the last verdict spoke for the ungrafted plant (the unit review).
		bed.forgetVerdict();
		donor.consume(1, player);
		level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 5);
		Garden.tell(player, "garden.grafted", getName(), ComposeText.name(new Text.Ref("graft", graft.id())));
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}
		if (!RateGate.allow(server, "garden", 4)) {
			return InteractionResult.FAIL;
		}
		if (isMaxAge(state) && Garden.enabled()) {
			if (!Illumination.mayChange(serverLevel, player, pos)) {
				return InteractionResult.FAIL;
			}
			List<ItemStack> crop = harvest(serverLevel, pos, state);
			for (ItemStack stack : crop) {
				Block.popResource(level, pos, stack);
			}
			if (!crop.isEmpty()) {
				// Mastery is practice: each crop harvested by one's own hand counts once.
				ConcordanceProgress.record(server, new Evidence.Practiced(Garden.ACTIVITY, organismId()));
			}
			return InteractionResult.SUCCESS;
		}
		// Concordance Chunk C (C-08, C-P61; the unit review): a crop covers its bed from above, so a Greenwarden's empty hand
		// on it tends the bed beneath by the bed's own rule (Authority at the bed), then reports.
		if (player.getMainHandItem().isEmpty() && level.getBlockEntity(pos.below()) instanceof VerdantBedBlockEntity bed) {
			VerdantBedBlock.tendBy(serverLevel, pos.below(), server, bed);
		}
		report(serverLevel, pos, state, server);
		return InteractionResult.SUCCESS;
	}

	/** Tells a player how it grows: its step and pace, and each factor holding it back. */
	public void report(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
		Organism definition = organism();
		Component name = getName();
		if (definition == null || !(level.getBlockEntity(pos.below()) instanceof VerdantBedBlockEntity bed)) {
			Garden.tell(player, "garden.no_bed");
			return;
		}
		if (!bed.awake()) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.garden.dormant"));
			return;
		}
		Habitat habitat = bed.habitat(level);
		if (habitat == null) {
			player.sendSystemMessage(Garden.status("waiting"));
			return;
		}
		Niche niche = bed.niche(definition);
		Verdict verdict = Verdict.of(niche, habitat);
		bed.note(verdict, habitat, definition);
		Component pace = isMaxAge(state) ? ComposeText.show(Text.of("ecology.mature"))
				: ComposeText.show(Text.of("ecology.growth." + verdict.growth().id));
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.garden.crop", name, getAge(state), MAX_AGE, pace));
		// Concordance Chunk C (C-09): its graft, when the bed carries one for it.
		Graft graft = Garden.catalog().graft(bed.graft());
		if (graft != null && graft.host().equals(organismId())) {
			List<Text> grafted = Garden.catalog().graftLines(graft.id());
			if (!grafted.isEmpty()) {
				player.sendSystemMessage(ComposeText.show(grafted.getFirst()));
			}
		}
		player.sendSystemMessage(ComposeText.show(Text.of("ecology.habitat", habitat.moisture(), habitat.light(), habitat.nutrients(),
				habitat.diversity(), habitat.disturbance())));
		for (Text line : verdict.reasons(niche, habitat)) {
			player.sendSystemMessage(ComposeText.show(line));
		}
		// Concordance Chunk C (C-08): a mixed bed's bonus and the strain its harvests left.
		for (Text line : bed.notes(definition)) {
			player.sendSystemMessage(ComposeText.show(line));
		}
	}

	// ---------------------------------------------------------------- bone meal feeds the bed

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return level.getBlockEntity(pos.below()) instanceof VerdantBedBlockEntity bed && bed.nutrients() < VerdantBedBlockEntity.CAPACITY;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		if (level.getBlockEntity(pos.below()) instanceof VerdantBedBlockEntity bed && bed.give(Garden.BONE_MEAL_NUTRIENTS) > 0) {
			// Concordance Chunk C (C-08, C-P61): bone meal and fertilizer are compost; each dose that feeds the bed eases a strain.
			bed.compost();
		}
	}
}
