package io.github.jimbozoomer.jugcraft.concordance;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.AlchemyCatalog;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Assay;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Axis;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Band;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Formula;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Heat;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Mixture;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Operation;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Outcome;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Preparation;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Vector;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.sign.Signs;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * An Alembic Crucible (roadmap step 13): a {@link Mixture}, its temperature, and a formula to follow.
 * <ul>
 * <li><b>Heat</b> comes from the block beneath ({@link Heat#SOURCES}; a campfire only while lit) and moves one degree
 * a tick, worked out on the server every tick from what is there now.</li>
 * <li><b>By hand</b> (the Alembic Arts understood): water (a bucket is {@value #WATER_PER_BUCKET} parts, a water bottle
 * one), ingredients as they come or prepared, a stir (Stirring Rod or stick, at most one every {@value #STIR_TICKS}
 * ticks) at the band it is in, and bottling (a glass bottle makes a draught, a bowl a salve). Bottling records the
 * outcome as practice.</li>
 * <li><b>Sampling</b>: a spoon by anyone, an Assay Glass (more with the Alembic Arts understood, and the prediction once
 * mastered): {@link Assay}.</li>
 * <li><b>Formulas</b>: a blank formula records a fresh mixture that does something; a written one sets the crucible to
 * repeat it. Following a formula, every {@value #AUTOMATION_TICKS} ticks the crucible takes its next step when it can
 * (water from its tank, an ingredient from its {@value #BUFFER_SLOTS} buffer slots, a stir once the heat is right),
 * then bottles into its output while it has bottles or bowls. It never substitutes or guesses: it waits.</li>
 * </ul>
 * Hoppers and pipes: ingredients and reagents into the buffer, bottles and bowls into the bottle slot, from above or the
 * sides; brews out from the sides or below (the heat source usually sits below); water into the tank. The mixture is
 * saved with the crucible; broken, its slots drop and the mixture spills. Keep the numbers equal to
 * tools/concordance_alchemy.py.
 */
public class CrucibleBlockEntity extends BlockEntity implements GeoBlockEntity, WorldlyContainer {
	public static final int BUFFER_SLOTS = 5;
	public static final int BOTTLE_SLOT = 5;
	public static final int OUTPUT_SLOT = 6;
	public static final int SLOTS = 7;
	public static final int STIR_TICKS = 10;
	public static final int AUTOMATION_TICKS = 20;
	public static final int WATER_PER_BUCKET = 3;
	/** From above, only what goes in; from the sides, that and the output; from below, only the output. */
	private static final int[] ABOVE = {0, 1, 2, 3, 4, BOTTLE_SLOT};
	private static final int[] SIDES = {0, 1, 2, 3, 4, BOTTLE_SLOT, OUTPUT_SLOT};
	private static final int[] BELOW = {OUTPUT_SLOT};
	private static final RawAnimation EMPTY = RawAnimation.begin().thenLoop("animation.crucible.empty");
	private static final RawAnimation STILL = RawAnimation.begin().thenLoop("animation.crucible.still");
	private static final RawAnimation SIMMER = RawAnimation.begin().thenLoop("animation.crucible.simmer");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private Mixture mixture = Mixture.EMPTY;
	private int temperature = Heat.AMBIENT;
	private @Nullable Formula program;
	private int step;
	private long lastStir = Long.MIN_VALUE / 2;
	/** Whether its formula is waiting for something it lacks (so the shortage shows once, not every try). Not saved. */
	private boolean waiting;
	/** On a client: what the liquid looks like, as the server sent it (the spoon's reading, never the makeup). */
	private Assay.Look shown = Assay.Look.PLAIN;

	/** Water waiting for the formula's water steps: up to a bucket, water only, never drawn back out. */
	public final SingleFluidStorage water = new SingleFluidStorage() {
		@Override
		protected long getCapacity(FluidVariant variant) {
			return FluidConstants.BUCKET;
		}

		@Override
		protected boolean canInsert(FluidVariant variant) {
			return variant.isOf(Fluids.WATER);
		}

		@Override
		protected boolean canExtract(FluidVariant variant) {
			return false;
		}

		@Override
		protected void onFinalCommit() {
			setChanged();
		}
	};

	public CrucibleBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftConcordance.CRUCIBLE_ENTITY, pos, state);
	}

	public Mixture mixture() {
		return mixture;
	}

	public int temperature() {
		return temperature;
	}

	public Band band() {
		return Band.of(temperature);
	}

	/** What the liquid looks like (roadmap step 27): on the server, the spoon's reading of the mixture; on a client, what was sent. */
	public Assay.Look look() {
		return level != null && level.isClientSide() ? shown : Assay.Look.of(mixture, Alchemy.catalog());
	}

	public @Nullable Formula program() {
		return program;
	}

	public int step() {
		return step;
	}

	/** Tests and operators: sets the temperature at once (it then moves towards its source as usual). */
	public void setTemperature(int temperature) {
		this.temperature = temperature;
		setChanged();
	}

	// ---------------------------------------------------------------- heat and automation

	void serverTick(ServerLevel level) {
		int target = heatBelow(level);
		int next = Heat.toward(temperature, target);
		if (next != temperature) {
			boolean newBand = Band.of(next) != Band.of(temperature);
			temperature = next;
			setChanged();
			if (newBand) {
				sync(level);
			}
		}
		if (program != null && level.getGameTime() % AUTOMATION_TICKS == 0 && JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			automate(level);
		}
	}

	/** The temperature the block beneath gives (a campfire only while lit). */
	private int heatBelow(ServerLevel level) {
		BlockState below = level.getBlockState(worldPosition.below());
		Integer heat = Heat.SOURCES.get(BuiltInRegistries.BLOCK.getKey(below.getBlock()).toString());
		if (heat == null || below.hasProperty(BlockStateProperties.LIT) && !below.getValue(BlockStateProperties.LIT)) {
			return Heat.AMBIENT;
		}
		return heat;
	}

	/** One step of the formula, if it can be taken now; or, the formula done, one bottle into the output. */
	private void automate(ServerLevel level) {
		List<Operation> operations = program.operations();
		if (step >= operations.size()) {
			if (mixture.isEmpty()) {
				step = 0;
				setChanged();
			} else {
				bottleIntoOutput(level);
			}
			return;
		}
		if (step == 0 && !mixture.isEmpty()) {
			return; // a repeat starts from an empty crucible
		}
		Operation operation = operations.get(step);
		int slot = -1;
		long water = 0;
		switch (operation) {
			case Operation.Water pour -> {
				water = pour.parts() * FluidConstants.BOTTLE;
				if (this.water.amount < water) {
					want(level);
					return;
				}
			}
			case Operation.Add add -> {
				slot = bufferSlotFor(add);
				if (slot < 0) {
					want(level);
					return;
				}
			}
			case Operation.Stir stir -> {
				if (band() != stir.band()) {
					return;
				}
			}
		}
		Mixture.Step result = mixture.apply(operation, Alchemy.catalog());
		if (!(result instanceof Mixture.Applied applied)) {
			// The rules changed under it (a data pack): it stops rather than guess.
			program = null;
			step = 0;
			setChanged();
			sound(level, JugcraftConcordance.CIRCLE_BREAK_SOUND);
			return;
		}
		if (water > 0) {
			this.water.amount -= water;
			if (this.water.amount == 0) {
				this.water.variant = FluidVariant.blank();
			}
		}
		if (slot >= 0) {
			items.get(slot).shrink(1);
		}
		mixture = applied.mixture();
		step++;
		waiting = false;
		setChanged();
		sync(level);
		sound(level, operation instanceof Operation.Stir ? JugcraftConcordance.CRUCIBLE_STIR_SOUND : JugcraftConcordance.CRUCIBLE_ADD_SOUND);
		signStep(level, operation);
	}

	/** Roadmap step 27: a step taken shows as work; a searing stir, which damages the mixture, as danger. */
	private void signStep(ServerLevel level, Operation operation) {
		Signs.show(level, worldPosition, operation instanceof Operation.Stir stir && stir.band() == Band.SEARING ? Sign.PERIL : Sign.WORK);
	}

	/** Its formula cannot go on for want of water, an ingredient, a container or room: shown once, when it starts waiting. */
	private void want(ServerLevel level) {
		if (!waiting) {
			waiting = true;
			Signs.show(level, worldPosition, Sign.WANT);
		}
	}

	/** The buffer slot holding what an Add step needs: the item itself as it comes, or a reagent of that preparation. */
	private int bufferSlotFor(Operation.Add add) {
		Preparation plain = Alchemy.catalog().asItComes();
		for (int i = 0; i < BUFFER_SLOTS; i++) {
			ItemStack stack = items.get(i);
			if (stack.isEmpty()) {
				continue;
			}
			Reagent reagent = stack.get(JugcraftConcordance.REAGENT);
			if (reagent != null ? reagent.item().equals(add.item()) && reagent.preparation().equals(add.preparation())
					: plain != null && plain.id().equals(add.preparation()) && ConcordanceProgress.itemId(stack).equals(add.item())) {
				return i;
			}
		}
		return -1;
	}

	private void bottleIntoOutput(ServerLevel level) {
		ItemStack container = items.get(BOTTLE_SLOT);
		Item form = formFor(container);
		if (form == null) {
			want(level);
			return;
		}
		Mixture.Bottled bottled = mixture.bottle();
		Outcome outcome = Outcome.of(bottled.dose(), bottled.contaminant(), Alchemy.catalog(), mixture.history());
		ItemStack made = Alchemy.brew(form, outcome);
		ItemStack output = items.get(OUTPUT_SLOT);
		if (!output.isEmpty() && !(ItemStack.isSameItemSameComponents(output, made) && output.getCount() < output.getMaxStackSize())) {
			want(level);
			return;
		}
		container.shrink(1);
		if (output.isEmpty()) {
			items.set(OUTPUT_SLOT, made);
		} else {
			output.grow(1);
		}
		mixture = bottled.rest();
		if (mixture.isEmpty()) {
			step = 0;
		}
		waiting = false;
		setChanged();
		sync(level);
		sound(level, JugcraftConcordance.CRUCIBLE_BOTTLE_SOUND);
		Signs.show(level, worldPosition, Sign.DONE);
	}

	private static @Nullable Item formFor(ItemStack container) {
		if (container.is(Items.GLASS_BOTTLE)) {
			return JugcraftConcordance.DRAUGHT;
		}
		return container.is(Items.BOWL) ? JugcraftConcordance.SALVE : null;
	}

	// ---------------------------------------------------------------- players

	/** A player uses an item on the crucible. TRY_WITH_EMPTY_HAND for an item it does nothing with. */
	public InteractionResult use(ServerPlayer player, ServerLevel level, ItemStack held, InteractionHand hand) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.disabled"));
			return InteractionResult.FAIL;
		}
		AlchemyCatalog catalog = Alchemy.catalog();
		if (held.is(JugcraftConcordance.SAMPLING_SPOON)) {
			Alchemy.tell(player, Assay.read(mixture, temperature, Assay.SPOON, catalog));
			return InteractionResult.SUCCESS;
		}
		if (held.is(JugcraftConcordance.ASSAY_GLASS)) {
			ResearchState state = Alchemy.state(player);
			int level0 = state.atLeast(ResearchState.MASTERED) ? Assay.MASTERED : state.atLeast(ResearchState.UNDERSTOOD) ? Assay.GLASS : Assay.SPOON;
			Alchemy.tell(player, Assay.read(mixture, temperature, level0, catalog));
			return InteractionResult.SUCCESS;
		}
		boolean water = held.is(Items.WATER_BUCKET) || held.is(Items.POTION)
				&& held.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER);
		boolean stirrer = held.is(JugcraftConcordance.STIRRING_ROD) || held.is(Items.STICK);
		Reagent reagent = held.get(JugcraftConcordance.REAGENT);
		boolean ingredient = reagent != null || catalog.ingredient(ConcordanceProgress.itemId(held)) != null;
		Item form = formFor(held);
		boolean formula = held.is(JugcraftConcordance.FORMULA_ITEM);
		if (!water && !stirrer && !ingredient && form == null && !formula) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!Alchemy.knows(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.alchemy.unknown"));
			return InteractionResult.FAIL;
		}
		if (formula) {
			return formula(player, held);
		}
		if (form != null) {
			return bottle(player, level, held, form);
		}
		if (program != null && step < program.operations().size()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.alchemy.automation_busy"));
			return InteractionResult.FAIL;
		}
		if (stirrer) {
			long now = ConcordanceProgress.now(level);
			if (now - lastStir < STIR_TICKS) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.alchemy.settling"));
				return InteractionResult.FAIL;
			}
			if (apply(player, level, new Operation.Stir(band()))) {
				lastStir = now;
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.alchemy.stirred",
						ComposeText.name(new Text.Ref("band", band().id))));
				sound(level, JugcraftConcordance.CRUCIBLE_STIR_SOUND);
			}
			return InteractionResult.SUCCESS;
		}
		if (water) {
			boolean bucket = held.is(Items.WATER_BUCKET);
			if (apply(player, level, new Operation.Water(bucket ? WATER_PER_BUCKET : 1))) {
				player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(bucket ? Items.BUCKET : Items.GLASS_BOTTLE)));
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.alchemy.water", mixture.parts()));
			}
			return InteractionResult.SUCCESS;
		}
		String item = reagent != null ? reagent.item() : ConcordanceProgress.itemId(held);
		Preparation preparation = reagent != null ? catalog.preparation(reagent.preparation()) : catalog.asItComes();
		if (preparation == null) {
			return InteractionResult.FAIL;
		}
		if (apply(player, level, new Operation.Add(item, preparation.id()))) {
			Component name = held.getHoverName();
			held.shrink(1);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.alchemy.added", name));
			sound(level, JugcraftConcordance.CRUCIBLE_ADD_SOUND);
		}
		return InteractionResult.SUCCESS;
	}

	/** Applies an operation by hand, or tells the player why it cannot be done. */
	private boolean apply(ServerPlayer player, ServerLevel level, Operation operation) {
		Mixture.Step result = mixture.apply(operation, Alchemy.catalog());
		if (result instanceof Mixture.Refused refused) {
			player.sendOverlayMessage(ComposeText.show(refused.reason()));
			return false;
		}
		mixture = ((Mixture.Applied) result).mixture();
		setChanged();
		sync(level);
		signStep(level, operation);
		return true;
	}

	/** Bottles one dose by hand into a glass bottle (a draught) or a bowl (a salve), and records the outcome as practice. */
	private InteractionResult bottle(ServerPlayer player, ServerLevel level, ItemStack held, Item form) {
		Mixture.Bottled bottled = mixture.bottle();
		if (bottled == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.alchemy.nothing_to_bottle"));
			return InteractionResult.FAIL;
		}
		Outcome outcome = Outcome.of(bottled.dose(), bottled.contaminant(), Alchemy.catalog(), mixture.history());
		ItemStack made = Alchemy.brew(form, outcome);
		mixture = bottled.rest();
		if (program != null && mixture.isEmpty()) {
			step = 0;
		}
		held.shrink(1);
		player.getInventory().placeItemBackInInventory(made, Prediction.SERVER_ONLY);
		if (!outcome.effects().isEmpty()) {
			ConcordanceProgress.record(player, new Evidence.Practiced(Alchemy.ACTIVITY, outcome.key()));
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.alchemy.bottled", made.getHoverName()));
		setChanged();
		sync(level);
		sound(level, JugcraftConcordance.CRUCIBLE_BOTTLE_SOUND);
		Signs.show(level, worldPosition, Sign.DONE);
		return InteractionResult.SUCCESS;
	}

	/** A blank formula records this mixture's process; a written one sets the crucible to repeat it. */
	private InteractionResult formula(ServerPlayer player, ItemStack held) {
		String written = held.get(JugcraftConcordance.FORMULA);
		if (written != null) {
			Formula formula = Formula.parse(written);
			if (formula == null || !(formula.replay(Alchemy.catalog()) instanceof Mixture.Applied)) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.alchemy.formula_invalid"));
				return InteractionResult.FAIL;
			}
			program = formula;
			step = 0;
			waiting = false;
			setChanged();
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.alchemy.formula_set", formula.operations().size()));
			return InteractionResult.SUCCESS;
		}
		Formula formula = Formula.of(mixture);
		if (formula == null || mixture.outcome(Alchemy.catalog()).effects().isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.alchemy.no_formula"));
			return InteractionResult.FAIL;
		}
		ItemStack record = held.copyWithCount(1);
		record.set(JugcraftConcordance.FORMULA, formula.text());
		held.shrink(1);
		player.getInventory().placeItemBackInInventory(record, Prediction.SERVER_ONLY);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.alchemy.recorded", formula.operations().size()));
		return InteractionResult.SUCCESS;
	}

	/** Empty-handed: the state of the pot; sneaking, stop repeating a formula. */
	public void useEmpty(ServerPlayer player, boolean sneaking) {
		if (sneaking && program != null) {
			// Only an alembist may stop a formula (roadmap step 28), as only one may set it.
			if (!Alchemy.knows(player)) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.alchemy.unknown"));
				return;
			}
			program = null;
			step = 0;
			waiting = false;
			setChanged();
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.alchemy.formula_cleared"));
			return;
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.crucible.status",
				ComposeText.name(new Text.Ref("band", band().id)), temperature, mixture.parts()));
	}

	private void sound(ServerLevel level, SoundEvent sound) {
		level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.7F, 1.0F);
	}

	// ---------------------------------------------------------------- hoppers and pipes

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? BELOW : side == Direction.UP ? ABOVE : SIDES;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side != Direction.DOWN && canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		// The heat source sits beneath, so an extractor at a side must be able to take the brews too.
		return slot == OUTPUT_SLOT && side != Direction.UP;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot < BUFFER_SLOTS) {
			return stack.has(JugcraftConcordance.REAGENT) || Alchemy.catalog().ingredient(ConcordanceProgress.itemId(stack)) != null;
		}
		return slot == BOTTLE_SLOT && formFor(stack) != null;
	}

	@Override
	public int getContainerSize() {
		return SLOTS;
	}

	@Override
	public boolean isEmpty() {
		return items.stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack taken = ContainerHelper.removeItem(items, slot, count);
		if (!taken.isEmpty()) {
			setChanged();
		}
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return false; // no screen: only hoppers, pipes and the crucible's own uses reach the slots
	}

	@Override
	public void clearContent() {
		items.clear();
	}

	// ---------------------------------------------------------------- saving and clients

	private void sync(ServerLevel level) {
		level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		water.readValue(input);
		temperature = input.getIntOr("temperature", Heat.AMBIENT);
		int parts = Math.clamp(input.getIntOr("parts", 0), 0, Mixture.MAX_PARTS);
		List<Operation> history = new ArrayList<>();
		boolean intact = true;
		for (String text : input.read("history", Codec.STRING.listOf()).orElse(List.of())) {
			Operation operation = Operation.parse(text);
			if (operation == null || !operation.text().equals(text) || history.size() >= Mixture.MAX_OPERATIONS) {
				intact = false;
				break;
			}
			history.add(operation);
		}
		mixture = parts == 0 ? Mixture.EMPTY : new Mixture(parts, Vector.fromList(input.read("pending", Codec.LONG.listOf()).orElse(List.of())),
				Vector.fromList(input.read("dissolved", Codec.LONG.listOf()).orElse(List.of())), Math.max(0L, input.getLongOr("contaminant", 0L)),
				// A history that does not read back can no longer be recorded as a formula.
				intact ? Math.max(0, input.getIntOr("drawn", 0)) : 1, intact ? history : List.of());
		// Only an update to a client carries the look (the server works it out from the mixture).
		Axis taste = Axis.fromId(input.getStringOr("taste", ""));
		shown = new Assay.Look(taste, input.getBooleanOr("murky", false));
		String saved = input.getStringOr("program", "");
		program = saved.isEmpty() ? null : Formula.parse(saved);
		step = program == null ? 0 : Math.clamp(input.getIntOr("step", 0), 0, program.operations().size());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		Saved.stamp(output, 1);
		ContainerHelper.saveAllItems(output, items);
		water.writeValue(output);
		output.putInt("temperature", temperature);
		output.putInt("parts", mixture.parts());
		if (!mixture.isEmpty()) {
			output.store("pending", Codec.LONG.listOf(), mixture.pending().toList());
			output.store("dissolved", Codec.LONG.listOf(), mixture.dissolved().toList());
			output.putLong("contaminant", mixture.contaminant());
			output.putInt("drawn", mixture.drawn());
			List<String> history = new ArrayList<>();
			for (Operation operation : mixture.history()) {
				history.add(operation.text());
			}
			output.store("history", Codec.STRING.listOf(), history);
		}
		if (program != null) {
			output.putString("program", program.text());
			output.putInt("step", step);
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/**
	 * Clients get the temperature and volume (the animation and Jade's lines) and what the liquid looks like (its
	 * strongest property and whether it is murky: what anyone's spoon would tell), not the slots or the mixture's makeup.
	 */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("temperature", temperature);
		tag.putInt("parts", mixture.parts());
		Assay.Look look = Assay.Look.of(mixture, Alchemy.catalog());
		tag.putString("taste", look.taste() == null ? "" : look.taste().id);
		tag.putBoolean("murky", look.murky());
		return tag;
	}

	/** What a client knows: the volume (the server's mixture is never sent). */
	public int parts() {
		return mixture.parts();
	}

	// ---------------------------------------------------------------- GeckoLib (drawn on the client)

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<CrucibleBlockEntity>("main", 5, test -> test.setAndContinue(
				test.animatable().parts() == 0 ? EMPTY : test.animatable().band().ordinal() >= Band.HOT.ordinal() ? SIMMER : STILL)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
