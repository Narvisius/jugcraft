package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.resource.Conversion;
import io.github.jimbozoomer.jugcraft.concordance.resource.Overflow;
import io.github.jimbozoomer.jugcraft.concordance.resource.Reservoir;
import io.github.jimbozoomer.jugcraft.concordance.resource.ResourceType;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchEngine;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.UUID;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Lampwright's Bench: a specimen slot and a work slot, and the Concordance's first station.
 * <ul>
 * <li><b>Study.</b> A player who has the specimen in the dish starts a study ({@value #STUDY_TICKS} ticks). When it
 * completes the bench uses up the specimen and gives that player their notes: an {@link Evidence.Studied}, which is
 * how First Light is understood at a bench. If they are away when it completes, the notes wait in the bench until they
 * next open it. Taking the specimen out, or swapping it, cancels the study and nothing is used.</li>
 * <li><b>Workings</b> (data: {@code data/<ns>/concordance/working}): kindling a plain lantern with a specimen into a
 * Kindled Lantern, infusing a specimen's Radiance into one, and channelling the player's own Focus into one. Each
 * needs the player to know it.</li>
 * </ul>
 * Everything runs on the server. The screen only presses buttons; each press is checked again here (feature switch,
 * research, contents, Focus) before anything changes, and the menu's own checks keep the player within reach. Keep
 * STUDY_TICKS equal to tools/concordance.py.
 */
public class LampwrightBenchBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ExtendedMenuProvider<BlockPos> {
	public static final String STATION = Jugcraft.MOD_ID + ":lampwright_bench";
	public static final int STUDY_TICKS = 100;
	public static final int SPECIMEN = 0;
	public static final int WORK = 1;
	public static final int SLOTS = 2;

	public static final int BUTTON_STUDY = 0;
	public static final int BUTTON_CANCEL = 1;
	public static final int BUTTON_KINDLE = 2;
	public static final int BUTTON_INFUSE = 3;
	public static final int BUTTON_CHANNEL = 4;
	public static final int BUTTONS = 5;

	/** Menu data: study progress, then the viewer's status for each button, then the work's Radiance and so on. */
	public static final int DATA_PROGRESS = 0;
	public static final int DATA_STUDYING = 1;
	public static final int DATA_STATUS = 2; // BUTTONS entries from here
	public static final int DATA_CHARGE = DATA_STATUS + BUTTONS;
	public static final int DATA_CHANNEL_FOCUS = DATA_CHARGE + 1;
	public static final int DATA_COUNT = DATA_CHANNEL_FOCUS + 1;

	private static final int[] NO_SLOTS = {};

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private @Nullable UUID student;
	private @Nullable String studied;
	private int progress;
	private @Nullable UUID notesFor;
	private @Nullable String notesItem;

	public LampwrightBenchBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftConcordance.BENCH_ENTITY, pos, state);
	}

	// ---------------------------------------------------------------- study

	public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		if (student != null) {
			ItemStack specimen = items.get(SPECIMEN);
			if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE) || specimen.isEmpty()
					|| !ConcordanceProgress.itemId(specimen).equals(studied)) {
				cancel(level);
			} else if (++progress >= STUDY_TICKS) {
				complete(level, pos);
			}
			setChanged();
		}
		boolean working = student != null;
		if (state.getValue(LampwrightBenchBlock.WORKING) != working) {
			level.setBlock(pos, state.setValue(LampwrightBenchBlock.WORKING, working), Block.UPDATE_ALL);
		}
	}

	public boolean studying() {
		return student != null;
	}

	public int progress() {
		return progress;
	}

	public boolean notesWaiting() {
		return notesFor != null;
	}

	public @Nullable UUID student() {
		return student;
	}

	private void cancel(ServerLevel level) {
		ServerPlayer owner = student == null ? null : level.getServer().getPlayerList().getPlayer(student);
		if (owner != null) {
			owner.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.bench.cancelled"));
		}
		student = null;
		studied = null;
		progress = 0;
	}

	private void complete(ServerLevel level, BlockPos pos) {
		UUID owner = student;
		String item = studied;
		student = null;
		studied = null;
		progress = 0;
		if (owner == null || item == null) {
			return;
		}
		items.get(SPECIMEN).shrink(1);
		level.playSound(null, pos, JugcraftConcordance.STUDY_COMPLETE_SOUND, SoundSource.BLOCKS, 0.8F, 1.0F);
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
		if (player != null) {
			giveNotes(player, item, "message.jugcraft.concordance.bench.study_done");
		} else {
			notesFor = owner;
			notesItem = item;
		}
	}

	/** Hands a player the notes waiting for them here, if any (when they open the bench). */
	public void deliverNotes(ServerPlayer player) {
		if (notesFor != null && notesFor.equals(player.getUUID()) && notesItem != null) {
			String item = notesItem;
			notesFor = null;
			notesItem = null;
			setChanged();
			giveNotes(player, item, "message.jugcraft.concordance.bench.notes_waiting");
		}
	}

	private static void giveNotes(ServerPlayer player, String item, String message) {
		ResearchEngine.Result result = ConcordanceProgress.record(player, new Evidence.Studied(item, STATION));
		if (result.transitions().isEmpty()) {
			// A research step announces itself; otherwise say what the study was of.
			player.sendOverlayMessage(Component.translatable(message, itemName(item)));
		}
	}

	private static Component itemName(String item) {
		Identifier id = Identifier.tryParse(item);
		Item found = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
		return new ItemStack(found).getHoverName();
	}

	// ---------------------------------------------------------------- what a player may do

	/** Whether {@code player} may press {@code button} now, and if not why. Pure: changes nothing. */
	public BenchStatus status(ServerPlayer player, int button) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			return BenchStatus.DISABLED;
		}
		return switch (button) {
			case BUTTON_STUDY -> studyStatus(player);
			case BUTTON_CANCEL -> student != null && student.equals(player.getUUID()) ? BenchStatus.READY : BenchStatus.BUSY;
			case BUTTON_KINDLE -> workingStatus(player, Definitions.WorkingType.CRAFT);
			case BUTTON_INFUSE -> workingStatus(player, Definitions.WorkingType.INFUSE);
			case BUTTON_CHANNEL -> workingStatus(player, Definitions.WorkingType.CHANNEL);
			default -> BenchStatus.DISABLED;
		};
	}

	private BenchStatus studyStatus(ServerPlayer player) {
		if (student != null || notesFor != null && !notesFor.equals(player.getUUID())) {
			return BenchStatus.BUSY;
		}
		ItemStack specimen = items.get(SPECIMEN);
		if (specimen.isEmpty()) {
			return BenchStatus.NO_SPECIMEN;
		}
		if (!specimen.is(JugcraftConcordance.SPECIMENS)) {
			return BenchStatus.NOT_SPECIMEN;
		}
		// Would the notes teach anything? Worked out on a copy; nothing is recorded until the study completes.
		ResearchEngine.Result preview = ResearchEngine.apply(ConcordanceProgress.knowledge(player), ConcordanceData.rules(),
				new Evidence.Studied(ConcordanceProgress.itemId(specimen), STATION), ConcordanceProgress.TAG_LOOKUP);
		return preview.recorded() ? BenchStatus.READY : BenchStatus.NOTHING_TO_LEARN;
	}

	/** The working of {@code type} at this bench that {@code player} could run on the current contents, or a reason. */
	private BenchStatus workingStatus(ServerPlayer player, Definitions.WorkingType type) {
		Definitions.Working working = workingFor(player, type);
		if (working == null) {
			return BenchStatus.UNKNOWN_RESEARCH;
		}
		ItemStack work = items.get(WORK);
		if (!ConcordanceProgress.itemId(work).equals(working.work()) || work.isEmpty()) {
			return type == Definitions.WorkingType.CRAFT ? BenchStatus.NO_LANTERN : BenchStatus.NO_KINDLED_LANTERN;
		}
		ItemStack specimen = items.get(SPECIMEN);
		switch (type) {
			case CRAFT -> {
				if (student != null) {
					return BenchStatus.BUSY;
				}
				if (specimen.isEmpty() || !ConcordanceProgress.itemId(specimen).equals(working.specimen())) {
					return BenchStatus.NO_AMETHYST;
				}
			}
			case INFUSE -> {
				if (student != null) {
					return BenchStatus.BUSY;
				}
				if (specimen.isEmpty()) {
					return BenchStatus.NO_SPECIMEN;
				}
				Integer value = working.specimens().get(ConcordanceProgress.itemId(specimen));
				if (value == null) {
					return BenchStatus.NOT_SPECIMEN;
				}
				if (lantern(work).insert(ResourceType.RADIANCE, value, Overflow.REJECT).outcome() != Reservoir.Outcome.DONE) {
					return BenchStatus.OVERFULL;
				}
			}
			case CHANNEL -> {
				Conversion conversion = conversion(working);
				if (conversion == null) {
					return BenchStatus.UNKNOWN_RESEARCH;
				}
				if (lantern(work).space() < conversion.toAmount()) {
					return BenchStatus.OVERFULL;
				}
				if (conversion.batches(ConcordanceProgress.currentFocus(player), lantern(work).space(), 1) < 1) {
					return BenchStatus.NO_FOCUS;
				}
			}
		}
		return BenchStatus.READY;
	}

	/**
	 * The working of this type at this bench for the current work item: one the player knows if there is one, else any
	 * (so the reason given is about the contents rather than research), else null when the player knows none of this
	 * type at all.
	 */
	private Definitions.@Nullable Working workingFor(ServerPlayer player, Definitions.WorkingType type) {
		ConcordanceRules rules = ConcordanceData.rules();
		String work = ConcordanceProgress.itemId(items.get(WORK));
		Definitions.Working known = null;
		for (Definitions.Working working : rules.workings().values()) {
			if (working.type() != type || !STATION.equals(working.station())
					|| !ResearchEngine.knowsWorking(ConcordanceProgress.knowledge(player), working)) {
				continue;
			}
			if (working.work().equals(work)) {
				return working;
			}
			if (known == null) {
				known = working;
			}
		}
		return known;
	}

	private long now() {
		return level == null ? 0L : ConcordanceProgress.now(level);
	}

	private int charge(ItemStack work) {
		return work.is(JugcraftConcordance.KINDLED_LANTERN) ? KindledLanternItem.remaining(work, now()) : 0;
	}

	// ---------------------------------------------------------------- pressing a button (server)

	/** A button pressed on the bench's screen. Checked again from the start; returns whether anything happened. */
	public boolean press(ServerPlayer player, int button) {
		BenchStatus status = status(player, button);
		if (!status.ready()) {
			player.sendOverlayMessage(status.message(channelFocus(player)));
			return false;
		}
		ServerLevel server = (ServerLevel) level;
		if (server == null) {
			return false;
		}
		switch (button) {
			case BUTTON_STUDY -> {
				student = player.getUUID();
				studied = ConcordanceProgress.itemId(items.get(SPECIMEN));
				progress = 0;
			}
			case BUTTON_CANCEL -> cancel(server);
			case BUTTON_KINDLE -> {
				Definitions.Working working = workingFor(player, Definitions.WorkingType.CRAFT);
				Identifier result = working == null || working.result() == null ? null : Identifier.tryParse(working.result());
				if (result == null) {
					return false;
				}
				ItemStack made = new ItemStack(BuiltInRegistries.ITEM.getValue(result));
				if (made.is(JugcraftConcordance.KINDLED_LANTERN)) {
					KindledLanternItem.set(made, working.radiance(), now(), false);
				}
				items.get(SPECIMEN).shrink(1);
				ItemStack work = items.get(WORK);
				if (work.getCount() > 1) {
					// Only one lantern fits the work slot, but never lose any that got there some other way.
					work.shrink(1);
					player.getInventory().placeItemBackInInventory(made, Prediction.SERVER_ONLY);
				} else {
					items.set(WORK, made);
				}
				server.playSound(null, worldPosition, JugcraftConcordance.LANTERN_IGNITE_SOUND, SoundSource.BLOCKS, 0.8F, 1.1F);
			}
			case BUTTON_INFUSE -> {
				Definitions.Working working = workingFor(player, Definitions.WorkingType.INFUSE);
				Integer value = working == null ? null : working.specimens().get(ConcordanceProgress.itemId(items.get(SPECIMEN)));
				if (value == null || !addRadiance(value)) {
					return false;
				}
				items.get(SPECIMEN).shrink(1);
				server.playSound(null, worldPosition, JugcraftConcordance.LANTERN_IGNITE_SOUND, SoundSource.BLOCKS, 0.6F, 1.3F);
			}
			case BUTTON_CHANNEL -> {
				// One batch of the working's conversion recipe (data: concordance/conversion): Focus spent, Radiance made.
				Definitions.Working working = workingFor(player, Definitions.WorkingType.CHANNEL);
				Conversion conversion = working == null ? null : conversion(working);
				if (conversion == null || conversion.batches(ConcordanceProgress.currentFocus(player), lantern(items.get(WORK)).space(), 1) < 1
						|| !ConcordanceProgress.spendFocus(player, (int) conversion.fromAmount())) {
					return false;
				}
				addRadiance((int) conversion.toAmount());
				server.playSound(null, worldPosition, JugcraftConcordance.KINDLE_SOUND, SoundSource.BLOCKS, 0.6F, 1.2F);
			}
			default -> {
				return false;
			}
		}
		setChanged();
		return true;
	}

	/** The lantern in the work slot as a typed container: its Radiance now, out of its capacity. */
	private Reservoir lantern(ItemStack work) {
		return new Reservoir(ResourceType.RADIANCE, charge(work), KindledLanternItem.CAPACITY);
	}

	/**
	 * Adds Radiance to the lantern in the work slot, all or nothing (it never overfills), settling its burn first and
	 * keeping it lit or unlit. Returns whether it was added.
	 */
	private boolean addRadiance(int amount) {
		ItemStack work = items.get(WORK);
		Reservoir.Insertion insertion = lantern(work).insert(ResourceType.RADIANCE, amount, Overflow.REJECT);
		if (insertion.outcome() != Reservoir.Outcome.DONE) {
			return false;
		}
		KindledLanternItem.set(work, (int) insertion.reservoir().amount(), now(), KindledLanternItem.lit(work));
		return true;
	}

	private static @Nullable Conversion conversion(Definitions.Working working) {
		return working.conversion() == null ? null : ConcordanceData.rules().conversions().get(working.conversion());
	}

	private int channelFocus(ServerPlayer player) {
		Definitions.Working working = workingFor(player, Definitions.WorkingType.CHANNEL);
		Conversion conversion = working == null ? null : conversion(working);
		return conversion == null ? 0 : (int) conversion.fromAmount();
	}

	/** The menu data for one viewer: their own statuses, worked out at most once a tick. */
	private ContainerData dataFor(Player viewer) {
		return new ContainerData() {
			private final int[] values = new int[DATA_COUNT];
			private long computed = Long.MIN_VALUE;

			@Override
			public int get(int index) {
				if (index < 0 || index >= DATA_COUNT) {
					return 0;
				}
				long tick = now();
				if (tick != computed && viewer instanceof ServerPlayer player) {
					computed = tick;
					values[DATA_PROGRESS] = progress;
					values[DATA_STUDYING] = student == null ? 0 : student.equals(player.getUUID()) ? 2 : 1;
					for (int button = 0; button < BUTTONS; button++) {
						values[DATA_STATUS + button] = status(player, button).ordinal();
					}
					ItemStack work = items.get(WORK);
					values[DATA_CHARGE] = work.is(JugcraftConcordance.KINDLED_LANTERN) ? charge(work) : -1;
					values[DATA_CHANNEL_FOCUS] = channelFocus(player);
				}
				return values[index];
			}

			@Override
			public void set(int index, int value) {
			}

			@Override
			public int getCount() {
				return DATA_COUNT;
			}
		};
	}

	// ---------------------------------------------------------------- inventory

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.jugcraft.lampwright_bench");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	public int getContainerSize() {
		return SLOTS;
	}

	/** The dish takes luminous specimens; the work slot a lantern or a Kindled Lantern. */
	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == SPECIMEN ? stack.is(JugcraftConcordance.SPECIMENS)
				: slot == WORK && (stack.is(Items.LANTERN) || stack.is(JugcraftConcordance.KINDLED_LANTERN));
	}

	/** A hands-on station: no hoppers or pipes. */
	@Override
	public int[] getSlotsForFace(Direction side) {
		return NO_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	// ---------------------------------------------------------------- menu

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new LampwrightBenchMenu(containerId, inventory, this, this, dataFor(inventory.player));
	}

	@Override
	public BlockPos getScreenOpeningData(ServerPlayer player) {
		return worldPosition;
	}

	// ---------------------------------------------------------------- saving

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		student = input.read("student", UUIDUtil.CODEC).orElse(null);
		studied = blankToNull(input.getStringOr("studied", ""));
		progress = student == null || studied == null ? 0 : Math.clamp(input.getIntOr("progress", 0), 0, STUDY_TICKS);
		if (studied == null) {
			student = null;
		}
		notesFor = input.read("notes_for", UUIDUtil.CODEC).orElse(null);
		notesItem = blankToNull(input.getStringOr("notes_item", ""));
		if (notesItem == null) {
			notesFor = null;
		}
	}

	private static @Nullable String blankToNull(String text) {
		return text.isEmpty() ? null : text;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		Saved.stamp(output, 1);
		ContainerHelper.saveAllItems(output, items);
		if (student != null && studied != null) {
			output.store("student", UUIDUtil.CODEC, student);
			output.putString("studied", studied);
			output.putInt("progress", progress);
		}
		if (notesFor != null && notesItem != null) {
			output.store("notes_for", UUIDUtil.CODEC, notesFor);
			output.putString("notes_item", notesItem);
		}
	}
}
