package io.github.jimbozoomer.jugcraft.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.RegistryOps;
import org.jspecify.annotations.Nullable;

/**
 * Resource condition {@code jugcraft:feature_enabled}: loads a recipe only when its
 * feature switch is on, so disabling a feature removes the recipes without touching
 * registered items. An optional {@code "or"} lists more switches, any one of which also
 * loads it: a wood whose trees several features grow (tools/agriculture.py WOOD_SWITCHES).
 */
public record FeatureEnabledCondition(String feature, List<String> or) implements ResourceCondition {
	public static final MapCodec<FeatureEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.STRING.fieldOf("feature").forGetter(FeatureEnabledCondition::feature),
			Codec.STRING.listOf().optionalFieldOf("or", List.of()).forGetter(FeatureEnabledCondition::or)
	).apply(instance, FeatureEnabledCondition::new));

	public static final ResourceConditionType<FeatureEnabledCondition> TYPE =
			ResourceConditionType.create(Jugcraft.id("feature_enabled"), CODEC);

	/** A condition on one switch alone (no alternatives), as written before the any-of switches. */
	public FeatureEnabledCondition(String feature) {
		this(feature, List.of());
	}

	public static void register() {
		ResourceConditions.register(TYPE);
	}

	@Override
	public ResourceConditionType<?> getType() {
		return TYPE;
	}

	@Override
	public boolean test(RegistryOps.@Nullable RegistryInfoLookup registryInfo) {
		return JugcraftConfig.isFeatureEnabled(feature) || or.stream().anyMatch(JugcraftConfig::isFeatureEnabled);
	}
}
