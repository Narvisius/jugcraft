package io.github.jimbozoomer.jugcraft.machine.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * One running batch of a formed machine: durable input escrow plus the results it has reserved room for. Starting a
 * batch moves its inputs out of the machine's buffers into the lane exactly once and snapshots its results, so a
 * restart, an unload or a data-pack reload carries on with the same batch and never takes the inputs again or changes
 * what it makes. Paid ticks advance {@link #progress()}; finishing moves the results out of the lane in one step.
 * Cancelling gives back only the untransformed inputs, never the energy already spent.
 */
public final class WorkLane {
	/** A fluid amount and the tank it came from (input tank) or goes to (output tank). */
	public record FluidPart(int tank, FluidVariant fluid, int mb) {
		public static final Codec<FluidPart> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("tank").forGetter(FluidPart::tank),
				FluidVariant.CODEC.fieldOf("fluid").forGetter(FluidPart::fluid),
				Codec.INT.fieldOf("mb").forGetter(FluidPart::mb)
		).apply(i, FluidPart::new));
	}

	/** An item stack and the container slot it came from (input) or goes to (output). */
	public record ItemPart(int slot, ItemStack stack) {
		public static final Codec<ItemPart> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("slot").forGetter(ItemPart::slot),
				ItemStack.CODEC.fieldOf("stack").forGetter(ItemPart::stack)
		).apply(i, ItemPart::new));
	}

	public static final Codec<WorkLane> CODEC = RecordCodecBuilder.create(i -> i.group(
			Identifier.CODEC.optionalFieldOf("recipe").forGetter(lane -> Optional.ofNullable(lane.recipe)),
			ItemPart.CODEC.listOf().optionalFieldOf("inputs", List.of()).forGetter(WorkLane::inputs),
			FluidPart.CODEC.listOf().optionalFieldOf("fluids", List.of()).forGetter(WorkLane::fluids),
			ItemPart.CODEC.listOf().optionalFieldOf("item_results", List.of()).forGetter(WorkLane::itemResults),
			FluidPart.CODEC.listOf().optionalFieldOf("fluid_results", List.of()).forGetter(WorkLane::fluidResults),
			Codec.INT.fieldOf("duration").forGetter(WorkLane::duration),
			Codec.INT.optionalFieldOf("tool", -1).forGetter(WorkLane::tool),
			Codec.BOOL.optionalFieldOf("cold", false).forGetter(WorkLane::cold),
			Codec.INT.optionalFieldOf("progress", 0).forGetter(WorkLane::progress)
	).apply(i, (recipe, inputs, fluids, itemResults, fluidResults, duration, tool, cold, progress) ->
			new WorkLane(recipe.orElse(null), inputs, fluids, itemResults, fluidResults, duration, tool, cold, progress)));

	private final @Nullable Identifier recipe;
	private final List<ItemPart> inputs;
	private final List<FluidPart> fluids;
	private final List<ItemPart> itemResults;
	private final List<FluidPart> fluidResults;
	private final int duration;
	private final int tool;
	private final boolean cold;
	private int progress;

	public WorkLane(@Nullable Identifier recipe, List<ItemPart> inputs, List<FluidPart> fluids, List<ItemPart> itemResults,
			List<FluidPart> fluidResults, int duration, int tool, boolean cold, int progress) {
		this.recipe = recipe;
		this.inputs = List.copyOf(inputs);
		this.fluids = List.copyOf(fluids);
		this.itemResults = List.copyOf(itemResults);
		this.fluidResults = List.copyOf(fluidResults);
		this.duration = Math.max(1, duration);
		this.tool = tool;
		this.cold = cold;
		this.progress = Math.max(0, progress);
	}

	/** The recipe the batch was started from (for display); the batch itself no longer depends on it. */
	public @Nullable Identifier recipe() {
		return recipe;
	}

	/** The escrowed item inputs, untransformed, with the slots they came from. */
	public List<ItemPart> inputs() {
		return inputs;
	}

	/** The escrowed fluid inputs, with the input tanks they came from. */
	public List<FluidPart> fluids() {
		return fluids;
	}

	/** The item results and the output slots reserved for them. */
	public List<ItemPart> itemResults() {
		return itemResults;
	}

	/** The fluid results and the output tanks (counted among the outputs) reserved for them. */
	public List<FluidPart> fluidResults() {
		return fluidResults;
	}

	/** The recipe's unmodified work, in ticks. */
	public int duration() {
		return duration;
	}

	/** The tool socket the batch needs, or -1. That socket's tool cannot be taken out while the batch runs. */
	public int tool() {
		return tool;
	}

	/** Whether the batch began from cold, so its first paid ticks show as warming. */
	public boolean cold() {
		return cold;
	}

	/** Paid ticks so far. */
	public int progress() {
		return progress;
	}

	void advance(int ticks) {
		progress += ticks;
	}
}
