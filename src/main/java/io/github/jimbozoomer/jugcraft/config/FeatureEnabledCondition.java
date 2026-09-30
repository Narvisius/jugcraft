package io.github.jimbozoomer.jugcraft.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.RegistryOps;
import org.jspecify.annotations.Nullable;

/**
 * Resource condition {@code jugcraft:feature_enabled}: loads a recipe only when its
 * feature switch is on, so disabling a feature removes the recipes without touching
 * registered items.
 */
public record FeatureEnabledCondition(String feature) implements ResourceCondition {
	public static final MapCodec<FeatureEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.STRING.fieldOf("feature").forGetter(FeatureEnabledCondition::feature)
	).apply(instance, FeatureEnabledCondition::new));

	public static final ResourceConditionType<FeatureEnabledCondition> TYPE =
			ResourceConditionType.create(Jugcraft.id("feature_enabled"), CODEC);

	public static void register() {
		ResourceConditions.register(TYPE);
	}

	@Override
	public ResourceConditionType<?> getType() {
		return TYPE;
	}

	@Override
	public boolean test(RegistryOps.@Nullable RegistryInfoLookup registryInfo) {
		return JugcraftConfig.isFeatureEnabled(feature);
	}
}
