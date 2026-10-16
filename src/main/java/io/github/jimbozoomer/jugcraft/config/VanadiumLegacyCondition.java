package io.github.jimbozoomer.jugcraft.config;

import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.resources.RegistryOps;
import org.jspecify.annotations.Nullable;

/** Explicit ordinary-iron fallback for existing worlds, also used when new acquisition is disabled. */
public record VanadiumLegacyCondition() implements ResourceCondition {
	public static final MapCodec<VanadiumLegacyCondition> CODEC = MapCodec.unit(new VanadiumLegacyCondition());
	public static final ResourceConditionType<VanadiumLegacyCondition> TYPE =
			ResourceConditionType.create(Jugcraft.id("vanadium_legacy_iron"), CODEC);

	@Override public ResourceConditionType<?> getType() { return TYPE; }

	@Override public boolean test(RegistryOps.@Nullable RegistryInfoLookup registryInfo) {
		return !JugcraftConfig.isFeatureEnabled("vanadium") || JugcraftConfig.option("vanadium.legacy_iron_recipes");
	}
}
