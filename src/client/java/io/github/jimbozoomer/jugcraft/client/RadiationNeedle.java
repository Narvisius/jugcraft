package io.github.jimbozoomer.jugcraft.client;

import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;

/**
 * The item model property "jugcraft:radiation_needle": where the Geiger counter's needle stands (0 = left stop, 1 =
 * right stop), from {@link RadiationMeter}. The counter's item definition picks one of its needle models with it
 * (tools/nuclear.py geiger_item). A counter in someone else's hand shows what this player's meter reads, which is
 * near enough the same place.
 */
public record RadiationNeedle() implements RangeSelectItemModelProperty {
	public static final MapCodec<RadiationNeedle> MAP_CODEC = MapCodec.unit(new RadiationNeedle());

	public static void register() {
		RangeSelectItemModelProperties.ID_MAPPER.put(Jugcraft.id("radiation_needle"), MAP_CODEC);
	}

	@Override
	public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
		return (float) RadiationMeter.needle();
	}

	@Override
	public MapCodec<RadiationNeedle> type() {
		return MAP_CODEC;
	}
}
