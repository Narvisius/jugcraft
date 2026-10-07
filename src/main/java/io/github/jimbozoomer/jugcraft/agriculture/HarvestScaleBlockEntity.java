package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Harvest Scale's memory: its board of the {@link #BOARD} heaviest giant pumpkins it has weighed (each
 * pumpkin once, at its latest weight, under the name of the player who last weighed it), the pumpkins it
 * has already given a ribbon to (the last {@link #REMEMBERED}), and the last weight, for comparators.
 *
 * <p>Ribbons are trophies and nothing more: the first time a pumpkin places on this scale's board, the
 * player weighing it gets the ribbon for that place. Weighing it again, heavier or not, gives no second
 * ribbon. The weight is read from the pumpkin on the server; nothing the client sends counts.
 */
public class HarvestScaleBlockEntity extends BlockEntity {
	public static final int BOARD = 3;
	public static final int REMEMBERED = 64;
	public static final List<String> RIBBONS = List.of("first_prize_ribbon", "second_prize_ribbon", "third_prize_ribbon");

	/** One place on the board. */
	public record Entry(UUID pumpkin, String grower, int weight) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.CODEC.fieldOf("pumpkin").forGetter(Entry::pumpkin),
				Codec.STRING.fieldOf("grower").forGetter(Entry::grower),
				Codec.INT.fieldOf("weight").forGetter(Entry::weight)).apply(i, Entry::new));
	}

	private final List<Entry> board = new ArrayList<>();
	private final Deque<UUID> awarded = new ArrayDeque<>();
	private int lastWeight;

	public HarvestScaleBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.HARVEST_SCALE_ENTITY, pos, state);
	}

	public List<Entry> board() {
		return List.copyOf(board);
	}

	public int lastWeight() {
		return lastWeight;
	}

	/** Comparator output: the last weight on a scale of 15, at least 1 once anything was weighed. */
	public int signal() {
		return lastWeight <= 0 ? 0 : Math.max(1, lastWeight * 15 / GiantPumpkinBlockEntity.MAX_WEIGHT);
	}

	/**
	 * Weighs a full-grown giant pumpkin for {@code player}: records it on the board, tells the player its
	 * weight and the board, and hands out a ribbon the first time it places. Returns its place (0 for first)
	 * or -1 if it is not on the board.
	 */
	public int weigh(ServerPlayer player, GiantPumpkinBlockEntity pumpkin) {
		int weight = pumpkin.weight();
		UUID id = pumpkin.id();
		board.removeIf(entry -> entry.pumpkin().equals(id));
		board.add(new Entry(id, player.getName().getString(), weight));
		board.sort(Comparator.comparingInt(Entry::weight).reversed());
		while (board.size() > BOARD) {
			board.removeLast();
		}
		int place = -1;
		for (int i = 0; i < board.size(); i++) {
			if (board.get(i).pumpkin().equals(id)) {
				place = i;
			}
		}
		lastWeight = weight;
		player.sendSystemMessage(Component.translatable("message.jugcraft.harvest_scale.weight", weight));
		if (place >= 0 && !awarded.contains(id)) {
			awarded.addLast(id);
			while (awarded.size() > REMEMBERED) {
				awarded.removeFirst();
			}
			ItemStack ribbon = new ItemStack(JugcraftAgriculture.item(RIBBONS.get(place)));
			player.sendSystemMessage(Component.translatable("message.jugcraft.harvest_scale.ribbon", ribbon.getHoverName()));
			if (!player.getInventory().add(ribbon)) {
				Block.popResource(player.level(), worldPosition.above(), ribbon);
			}
		}
		for (int i = 0; i < board.size(); i++) {
			Entry entry = board.get(i);
			player.sendSystemMessage(Component.translatable("message.jugcraft.harvest_scale.board", i + 1, entry.grower(), entry.weight()));
		}
		setChanged(); // also tells comparators
		return place;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		board.clear();
		input.read("board", Entry.CODEC.listOf()).ifPresent(entries -> entries.stream().limit(BOARD).forEach(board::add));
		awarded.clear();
		input.read("awarded", UUIDUtil.CODEC.listOf()).ifPresent(ids -> ids.stream().limit(REMEMBERED).forEach(awarded::addLast));
		lastWeight = input.getIntOr("last_weight", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("board", Entry.CODEC.listOf(), board);
		output.store("awarded", UUIDUtil.CODEC.listOf(), List.copyOf(awarded));
		output.putInt("last_weight", lastWeight);
	}
}
