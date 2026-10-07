package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.chemistry.GasFluid;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * What a storage tank's label says about the liquid in it (Justin, Oct 7: the painted side fills itself in from what
 * the tank holds): its kind (which tanks take it, TankRating), the NFPA 704 fire diamond (health, fire, instability and
 * the special mark: OX oxidizer, W reacts with water), the UN number and the warning lines. Ratings from NFPA 49 tables
 * and Wikipedia's NFPA 704 page; mixtures without one rating (fracking fluid, flowback water, nutrient solution,
 * lubricant, naphtha, vanadium electrolyte) are estimates (docs/features/nuclear.md). Gases (the gas holder and
 * cylinders hold those) and lava (the steel tank does) go in no storage tank; a liquid not listed here (another
 * mod's) is taken as a general liquid with no diamond.
 */
public final class TankProducts {
	public enum Kind {
		GENERAL, FLAMMABLE, CORROSIVE, CRYOGENIC, REFUSED
	}

	/** One liquid's label. Warnings are lang keys under tank.jugcraft.warning. */
	public record Product(Kind kind, int health, int fire, int instability, String special, String un, List<String> warnings) {
		/** Whether the label shows the fire diamond (not for 0-0-0 liquids such as water). */
		public boolean diamond() {
			return health + fire + instability > 0 || !special.isEmpty();
		}
	}

	private static final List<String> FLAMMABLE_LINES = List.of("flammable", "no_smoking", "no_naked_flames");

	private static Product general(int h, int f, int i) {
		return new Product(Kind.GENERAL, h, f, i, "", "", List.of());
	}

	private static Product flammable(int h, int f, int i, String un) {
		return new Product(Kind.FLAMMABLE, h, f, i, "", un, FLAMMABLE_LINES);
	}

	private static Product corrosive(int h, int f, int i, String special, String un, String second) {
		return new Product(Kind.CORROSIVE, h, f, i, special, un, List.of("corrosive", second));
	}

	public static final Product PLAIN = general(0, 0, 0);
	public static final Product GAS = new Product(Kind.REFUSED, 0, 0, 0, "", "", List.of());
	public static final Product LAVA = new Product(Kind.REFUSED, 0, 0, 0, "", "", List.of());

	static final Map<String, Product> PRODUCTS = Map.ofEntries(
			Map.entry("minecraft:water", PLAIN),
			Map.entry("jugcraft:brine", PLAIN),
			Map.entry("jugcraft:nutrient_solution", PLAIN),
			Map.entry("jugcraft:fracking_fluid", general(1, 0, 0)),
			Map.entry("jugcraft:flowback_water", general(1, 0, 0)),
			Map.entry("jugcraft:lubricant", general(0, 1, 0)),
			Map.entry("jugcraft:crude_oil", flammable(1, 3, 0, "1267")),
			Map.entry("jugcraft:naphtha", flammable(1, 4, 0, "1268")),
			Map.entry("jugcraft:gasoline", flammable(1, 3, 0, "1203")),
			Map.entry("jugcraft:premium_gasoline", flammable(1, 3, 0, "1203")),
			Map.entry("jugcraft:bioethanol", flammable(2, 3, 0, "1170")),
			Map.entry("jugcraft:kerosene", flammable(0, 2, 0, "1223")),
			Map.entry("jugcraft:diesel", flammable(1, 2, 0, "1202")),
			Map.entry("jugcraft:premium_diesel", flammable(1, 2, 0, "1202")),
			Map.entry("jugcraft:heavy_fuel_oil", flammable(0, 2, 0, "3082")),
			Map.entry("jugcraft:lye", corrosive(3, 0, 1, "", "1824", "face_shield")),
			Map.entry("jugcraft:sulfuric_acid", corrosive(3, 0, 2, "W", "1830", "no_water")),
			Map.entry("jugcraft:nitric_acid", corrosive(3, 0, 0, "OX", "2031", "oxidizer")),
			Map.entry("jugcraft:vanadium_electrolyte", corrosive(3, 0, 0, "", "3264", "face_shield")),
			Map.entry("jugcraft:liquid_oxygen", new Product(Kind.CRYOGENIC, 3, 0, 0, "OX", "1073", List.of("oxidizer", "cryogenic"))));

	private TankProducts() {
	}

	/** The label for a liquid, or null for none (blank). */
	public static @Nullable Product of(FluidVariant variant) {
		if (variant == null || variant.isBlank()) {
			return null;
		}
		return of(variant.getFluid());
	}

	public static Product of(Fluid fluid) {
		if (fluid instanceof GasFluid) {
			return GAS;
		}
		if (fluid.isSame(Fluids.LAVA)) {
			return LAVA;
		}
		return PRODUCTS.getOrDefault(BuiltInRegistries.FLUID.getKey(fluid).toString(), PLAIN);
	}

	public static Kind kind(FluidVariant variant) {
		Product product = of(variant);
		return product == null ? Kind.GENERAL : product.kind();
	}
}
