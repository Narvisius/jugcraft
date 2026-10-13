package io.github.jimbozoomer.jugcraft.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;

/**
 * A one-input machine recipe (crusher, arc furnace, metal press, wire drawer). Data-driven:
 * data/<namespace>/recipe/<type>/<name>.json, for example
 * {@code {"type": "jugcraft:crushing", "ingredient": "jugcraft:tin_ore", "result": {"id": "jugcraft:raw_tin", "count": 2}, "time": 160}}.
 * The ingredient may be an item, a list or a tag ({@code "#c:ores/tin"}); time is in ticks.
 *
 * <p>Optional {@code "byproducts"}: extra results rolled once per operation, for machines with
 * byproduct slots (pulverizer, sieve, sawmill), for example
 * {@code [{"result": {"id": "jugcraft:gold_dust"}, "chance": 0.1, "feature": "tin"}]}. A byproduct whose
 * {@code feature} switch is off is never made; the machine waits while a possible byproduct has no room.
 */
public class MachineRecipe extends SingleItemRecipe {
	private final MachineKind machine;
	private final int time;
	private final int companionEffort;
	private final List<Byproduct> byproducts;

	public MachineRecipe(MachineKind machine, Recipe.CommonInfo commonInfo, Ingredient ingredient, ItemStackTemplate result, int time,
			List<Byproduct> byproducts) {
		this(machine, commonInfo, ingredient, result, time, byproducts, 0);
	}

	public MachineRecipe(MachineKind machine, Recipe.CommonInfo commonInfo, Ingredient ingredient, ItemStackTemplate result, int time,
			List<Byproduct> byproducts, int companionEffort) {
		super(commonInfo, ingredient, result);
		this.machine = machine;
		this.time = time;
		this.companionEffort = Math.clamp(companionEffort, 0, MachineCompanionEffort.MAX_PER_QUARTER);
		this.byproducts = List.copyOf(byproducts);
	}

	/** One possible extra result: made with probability {@code chance}, if its feature switch is on. */
	public record Byproduct(ItemStackTemplate result, float chance, Optional<String> feature) {
		public static final Codec<Byproduct> CODEC = RecordCodecBuilder.create(i -> i.group(
				ItemStackTemplate.CODEC.fieldOf("result").forGetter(Byproduct::result),
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(Byproduct::chance),
				Codec.STRING.optionalFieldOf("feature").forGetter(Byproduct::feature)
		).apply(i, Byproduct::new));
		public static final StreamCodec<RegistryFriendlyByteBuf, Byproduct> STREAM_CODEC = StreamCodec.composite(
				ItemStackTemplate.STREAM_CODEC, Byproduct::result,
				ByteBufCodecs.FLOAT, Byproduct::chance,
				ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), Byproduct::feature,
				Byproduct::new);

		public boolean enabled() {
			return feature.map(JugcraftConfig::isFeatureEnabled).orElse(true);
		}
	}

	public MachineKind machine() {
		return machine;
	}

	/** Ticks one operation takes. */
	public int time() {
		return time;
	}

	public int companionEffort() { return companionEffort; }

	/** Extra results rolled once per operation. */
	public List<Byproduct> byproducts() {
		return byproducts;
	}

	/** What one operation makes. */
	public ItemStackTemplate output() {
		return result();
	}

	@Override
	public RecipeSerializer<MachineRecipe> getSerializer() {
		return MachineRecipeTypes.singleSerializer(machine);
	}

	@Override
	public RecipeType<MachineRecipe> getType() {
		return MachineRecipeTypes.single(machine);
	}

	@Override
	public String group() {
		return "";
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return RecipeBookCategories.CRAFTING_MISC;
	}

	static RecipeSerializer<MachineRecipe> serializer(MachineKind machine) {
		MapCodec<MachineRecipe> codec = RecordCodecBuilder.mapCodec(i -> i.group(
				Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
				Ingredient.CODEC.fieldOf("ingredient").forGetter(SingleItemRecipe::input),
				ItemStackTemplate.CODEC.fieldOf("result").forGetter(MachineRecipe::output),
				ExtraCodecs.POSITIVE_INT.optionalFieldOf("time", 200).forGetter(MachineRecipe::time),
				Byproduct.CODEC.listOf().optionalFieldOf("byproducts", List.of()).forGetter(MachineRecipe::byproducts),
				MachineCompanionEffort.CODEC.optionalFieldOf("companion_effort_per_quarter", 0).forGetter(MachineRecipe::companionEffort)
		).apply(i, (info, ingredient, result, time, byproducts, effort) -> new MachineRecipe(machine, info, ingredient, result, time, byproducts, effort)));
		StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> stream = StreamCodec.composite(
				Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
				Ingredient.CONTENTS_STREAM_CODEC, SingleItemRecipe::input,
				ItemStackTemplate.STREAM_CODEC, MachineRecipe::output,
				ByteBufCodecs.VAR_INT, MachineRecipe::time,
				Byproduct.STREAM_CODEC.apply(ByteBufCodecs.list()), MachineRecipe::byproducts,
				ByteBufCodecs.VAR_INT, MachineRecipe::companionEffort,
				(info, ingredient, result, time, byproducts, effort) -> new MachineRecipe(machine, info, ingredient, result, time, byproducts, effort));
		return new RecipeSerializer<>(codec, stream);
	}
}
