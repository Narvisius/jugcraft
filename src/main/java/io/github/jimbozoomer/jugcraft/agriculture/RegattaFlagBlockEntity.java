package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Regatta Flag's memory: its course (the buoys it found, in number order) and its board of the
 * {@link #BOARD} best times (each racer once, at their best), plus the racers it has given a ribbon to.
 *
 * <p>The course is the {@link RegattaBuoyBlock}s within {@link #COURSE_RANGE} blocks across and
 * {@link #COURSE_HEIGHT} up or down, one per number (the nearest if two share one), in loaded chunks only.
 * Surveying a different course clears the board. Ribbons are trophies, as at the Harvest Scale: the first
 * time a racer places on this flag's board they get that place's ribbon, never again here (the last
 * {@link #REMEMBERED} racers are remembered). Times are measured by the boat on the server.
 */
public class RegattaFlagBlockEntity extends BlockEntity {
	public static final int BOARD = HarvestScaleBlockEntity.BOARD;
	public static final int REMEMBERED = 64;
	public static final int COURSE_RANGE = 64;
	public static final int COURSE_HEIGHT = 16;

	/** One place on the board: a racer's best time in ticks. */
	public record Entry(UUID racer, String name, int ticks) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.CODEC.fieldOf("racer").forGetter(Entry::racer),
				Codec.STRING.fieldOf("name").forGetter(Entry::name),
				Codec.INT.fieldOf("ticks").forGetter(Entry::ticks)).apply(i, Entry::new));
	}

	private final List<BlockPos> course = new ArrayList<>();
	private final List<Entry> board = new ArrayList<>();
	private final Deque<UUID> awarded = new ArrayDeque<>();

	public RegattaFlagBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.REGATTA_FLAG_ENTITY, pos, state);
	}

	public List<BlockPos> course() {
		return List.copyOf(course);
	}

	public List<Entry> board() {
		return List.copyOf(board);
	}

	/** A time in ticks as minutes, seconds and hundredths ({@code 1:02.35}). */
	public static String time(int ticks) {
		int hundredths = ticks * 5;
		return String.format(Locale.ROOT, "%d:%02d.%02d", hundredths / 6000, hundredths / 100 % 60, hundredths % 100);
	}

	/**
	 * Looks for the course's buoys in the loaded chunks around the flag and keeps them; a different course
	 * than before clears the board. Reads the block entities of at most 81 chunks. Returns the course.
	 */
	public List<BlockPos> survey(ServerLevel level) {
		BlockPos flag = worldPosition;
		Map<Integer, BlockPos> byNumber = new LinkedHashMap<>();
		int minX = (flag.getX() - COURSE_RANGE) >> 4;
		int maxX = (flag.getX() + COURSE_RANGE) >> 4;
		int minZ = (flag.getZ() - COURSE_RANGE) >> 4;
		int maxZ = (flag.getZ() + COURSE_RANGE) >> 4;
		for (int cx = minX; cx <= maxX; cx++) {
			for (int cz = minZ; cz <= maxZ; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					BlockPos pos = entity.getBlockPos();
					if (!(entity instanceof RegattaBuoyBlockEntity) || Math.abs(pos.getX() - flag.getX()) > COURSE_RANGE
							|| Math.abs(pos.getZ() - flag.getZ()) > COURSE_RANGE || Math.abs(pos.getY() - flag.getY()) > COURSE_HEIGHT) {
						continue;
					}
					BlockState state = entity.getBlockState();
					if (!state.hasProperty(RegattaBuoyBlock.NUMBER)) {
						continue;
					}
					int number = state.getValue(RegattaBuoyBlock.NUMBER);
					BlockPos other = byNumber.get(number);
					if (other == null || pos.distSqr(flag) < other.distSqr(flag)) {
						byNumber.put(number, pos.immutable());
					}
				}
			}
		}
		List<BlockPos> found = byNumber.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toList();
		if (!found.equals(course)) {
			course.clear();
			course.addAll(found);
			board.clear();
			setChanged();
		}
		return course();
	}

	/**
	 * Records a finished run for {@code player}: tells them their time, keeps it if it is their best, hands
	 * out a ribbon the first time they place, and shows the board. Returns their place (0 for first) or -1.
	 */
	public int finish(ServerPlayer player, int ticks) {
		UUID id = player.getUUID();
		player.sendSystemMessage(Component.translatable("message.jugcraft.regatta.finished", time(ticks)));
		Entry best = board.stream().filter(entry -> entry.racer().equals(id)).findFirst().orElse(null);
		if (best == null || ticks < best.ticks()) {
			board.removeIf(entry -> entry.racer().equals(id));
			board.add(new Entry(id, player.getName().getString(), ticks));
			board.sort(Comparator.comparingInt(Entry::ticks));
			while (board.size() > BOARD) {
				board.removeLast();
			}
		}
		int place = -1;
		for (int i = 0; i < board.size(); i++) {
			if (board.get(i).racer().equals(id)) {
				place = i;
			}
		}
		if (place >= 0 && !awarded.contains(id)) {
			awarded.addLast(id);
			while (awarded.size() > REMEMBERED) {
				awarded.removeFirst();
			}
			ItemStack ribbon = new ItemStack(JugcraftAgriculture.item(HarvestScaleBlockEntity.RIBBONS.get(place)));
			player.sendSystemMessage(Component.translatable("message.jugcraft.harvest_scale.ribbon", ribbon.getHoverName()));
			if (!player.getInventory().add(ribbon)) {
				Block.popResource(player.level(), worldPosition.above(), ribbon);
			}
		}
		showBoard(player);
		setChanged();
		return place;
	}

	/** Tells a player the course and the board. */
	public void showBoard(ServerPlayer player) {
		player.sendSystemMessage(Component.translatable("message.jugcraft.regatta.course", course.size()));
		for (int i = 0; i < board.size(); i++) {
			Entry entry = board.get(i);
			player.sendSystemMessage(Component.translatable("message.jugcraft.regatta.board", i + 1, entry.name(), time(entry.ticks())));
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		course.clear();
		input.read("course", BlockPos.CODEC.listOf()).ifPresent(marks -> marks.stream().limit(RegattaBuoyBlock.MAX_NUMBER).forEach(course::add));
		board.clear();
		input.read("board", Entry.CODEC.listOf()).ifPresent(entries -> entries.stream().limit(BOARD).forEach(board::add));
		awarded.clear();
		input.read("awarded", UUIDUtil.CODEC.listOf()).ifPresent(ids -> ids.stream().limit(REMEMBERED).forEach(awarded::addLast));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("course", BlockPos.CODEC.listOf(), List.copyOf(course));
		output.store("board", Entry.CODEC.listOf(), List.copyOf(board));
		output.store("awarded", UUIDUtil.CODEC.listOf(), List.copyOf(awarded));
	}
}
