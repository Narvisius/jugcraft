package io.github.jimbozoomer.jugcraft.concordance.dreaming;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

/**
 * One dream expedition (roadmap step 22): the single transaction that holds a dreamer's possessions while they dream.
 * It lives on the player ({@link Dreaming#EXPEDITION}), saved with them in the same record as their inventory, so at
 * every save either the dreamer holds their own things and no expedition exists, or the expedition holds them
 * ({@code escrow}, slot by slot) and the dreamer holds only what they picked up in the dream. Also kept: when it began
 * and must end, where the body lies, the dreamer's game mode and experience, how many wisps have gathered and how many
 * they caught, and the censer.
 */
public record DreamExpedition(UUID id, long started, long until, String dimension, BlockPos body, float yRot, float xRot, String gameMode,
		int xpLevel, float xpProgress, int xpTotal, int gathered, int caught, List<Held> escrow, BlockPos censer) {
	/** One escrowed stack and the inventory slot it came from. */
	public record Held(int slot, ItemStack stack) {
		public static final Codec<Held> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.INT.fieldOf("slot").forGetter(Held::slot),
				ItemStack.CODEC.fieldOf("stack").forGetter(Held::stack)).apply(i, Held::new));
	}

	public static final Codec<DreamExpedition> CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("id").forGetter(DreamExpedition::id), Codec.LONG.fieldOf("started").forGetter(DreamExpedition::started),
			Codec.LONG.fieldOf("until").forGetter(DreamExpedition::until), Codec.STRING.fieldOf("dimension").forGetter(DreamExpedition::dimension),
			BlockPos.CODEC.fieldOf("body").forGetter(DreamExpedition::body), Codec.FLOAT.fieldOf("y_rot").forGetter(DreamExpedition::yRot),
			Codec.FLOAT.fieldOf("x_rot").forGetter(DreamExpedition::xRot), Codec.STRING.fieldOf("game_mode").forGetter(DreamExpedition::gameMode),
			Codec.INT.fieldOf("xp_level").forGetter(DreamExpedition::xpLevel), Codec.FLOAT.fieldOf("xp_progress").forGetter(DreamExpedition::xpProgress),
			Codec.INT.fieldOf("xp_total").forGetter(DreamExpedition::xpTotal), Codec.INT.fieldOf("gathered").forGetter(DreamExpedition::gathered),
			Codec.INT.fieldOf("caught").forGetter(DreamExpedition::caught),
			Held.CODEC.listOf().fieldOf("escrow").forGetter(DreamExpedition::escrow), BlockPos.CODEC.fieldOf("censer").forGetter(DreamExpedition::censer))
			.apply(i, DreamExpedition::new));

	public DreamExpedition withCaught(int count) {
		return new DreamExpedition(id, started, until, dimension, body, yRot, xRot, gameMode, xpLevel, xpProgress, xpTotal, gathered, count, escrow,
				censer);
	}

	public DreamExpedition withGathered(int count) {
		return new DreamExpedition(id, started, until, dimension, body, yRot, xRot, gameMode, xpLevel, xpProgress, xpTotal, count, caught, escrow,
				censer);
	}

	/** How many items the escrow holds, counted (for tests and the record). */
	public int items() {
		return escrow.stream().mapToInt(held -> held.stack().getCount()).sum();
	}
}
