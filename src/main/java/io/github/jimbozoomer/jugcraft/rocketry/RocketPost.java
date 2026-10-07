package io.github.jimbozoomer.jugcraft.rocketry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The rocket post (batch 39, docs/features/rocket-post.md): deliveries in flight between rocket pads, saved with the
 * world. A delivery is due {@link #flightTicks} after launch. Once due, it lands as soon as its target pad's area is
 * loaded: if nobody is near the target, it waits (nothing is force-loaded) and lands when the area loads again. It goes
 * into the target pad's cargo slots; what does not fit waits for room. If the target pad is gone, the cargo is dropped
 * where it stood. Checked once a second.
 */
public final class RocketPost {
	public static final int RANGE = 4_096;
	public static final int MIN_FLIGHT = 60;
	public static final int BLOCKS_PER_TICK = 4;
	public static final int CHECK_INTERVAL = 20;

	private RocketPost() {
	}

	/** Ticks a rocket takes to fly {@code distance} blocks. */
	public static int flightTicks(double distance) {
		return MIN_FLIGHT + (int) (distance / BLOCKS_PER_TICK);
	}

	public record Delivery(ResourceKey<Level> dimension, BlockPos target, List<ItemStack> cargo, long due) {
		static final Codec<Delivery> CODEC = RecordCodecBuilder.create(i -> i.group(
				ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Delivery::dimension),
				BlockPos.CODEC.fieldOf("target").forGetter(Delivery::target),
				ItemStack.CODEC.listOf().fieldOf("cargo").forGetter(Delivery::cargo),
				Codec.LONG.fieldOf("due").forGetter(Delivery::due)).apply(i, Delivery::new));
	}

	public static Data data(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Data.TYPE);
	}

	/** Books a delivery of {@code cargo} to the pad at {@code target}, due after its flight. */
	public static void send(MinecraftServer server, ResourceKey<Level> dimension, BlockPos target, List<ItemStack> cargo, double distance) {
		Data data = data(server);
		data.deliveries.add(new Delivery(dimension, target.immutable(), List.copyOf(cargo),
				server.overworld().getGameTime() + flightTicks(distance)));
		data.setDirty();
	}

	/** Deliveries still in flight or waiting to land. */
	public static List<Delivery> pending(MinecraftServer server) {
		return List.copyOf(data(server).deliveries);
	}

	static void tick(MinecraftServer server) {
		if (server.overworld().getGameTime() % CHECK_INTERVAL == 0) {
			deliver(server);
		}
	}

	/** Lands every due delivery whose target area is loaded; the rest keep waiting. */
	public static void deliver(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		Data data = data(server);
		if (data.deliveries.isEmpty()) {
			return;
		}
		List<Delivery> kept = new ArrayList<>();
		boolean changed = false;
		for (Delivery delivery : data.deliveries) {
			ServerLevel level = server.getLevel(delivery.dimension());
			if (delivery.due() > now || level == null || !level.isLoaded(delivery.target())) {
				kept.add(delivery);
				continue;
			}
			changed = true;
			List<ItemStack> left = land(level, delivery);
			if (!left.isEmpty()) {
				kept.add(new Delivery(delivery.dimension(), delivery.target(), left, delivery.due()));
			}
		}
		if (changed) {
			data.deliveries.clear();
			data.deliveries.addAll(kept);
			data.setDirty();
		}
	}

	/** Lands a delivery whose target is loaded; returns the cargo that did not fit (to try again). */
	private static List<ItemStack> land(ServerLevel level, Delivery delivery) {
		BlockPos target = delivery.target();
		List<ItemStack> left = new ArrayList<>();
		if (level.getBlockEntity(target) instanceof RocketPadBlockEntity pad) {
			for (ItemStack stack : delivery.cargo()) {
				ItemStack rest = pad.receive(stack.copy());
				if (!rest.isEmpty()) {
					left.add(rest);
				}
			}
		} else {
			// The pad is gone: the cargo comes down where it stood.
			for (ItemStack stack : delivery.cargo()) {
				level.addFreshEntity(new ItemEntity(level, target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5, stack.copy()));
			}
		}
		if (left.size() < delivery.cargo().size() || left.isEmpty()) {
			ItemStack firework = new ItemStack(Items.FIREWORK_ROCKET);
			firework.set(DataComponents.FIREWORKS, new Fireworks(1, List.of()));
			level.addFreshEntity(new FireworkRocketEntity(level, target.getX() + 0.5, target.getY() + 1.2, target.getZ() + 0.5, firework));
			level.playSound(null, target, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.BLOCKS, 2.0F, 0.6F);
		}
		return left;
	}

	public static final class Data extends SavedData {
		static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
				Delivery.CODEC.listOf().fieldOf("deliveries").forGetter(d -> d.deliveries)).apply(i, Data::new));
		static final SavedDataType<Data> TYPE = new SavedDataType<>(Jugcraft.id("rocket_post"), Data::new, CODEC, null);

		private final List<Delivery> deliveries = new ArrayList<>();

		Data() {
		}

		Data(List<Delivery> saved) {
			deliveries.addAll(saved);
		}
	}
}
