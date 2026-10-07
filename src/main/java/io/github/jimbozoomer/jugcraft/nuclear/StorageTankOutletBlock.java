package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/**
 * The storage tank's outlet, which is also its controller. Its service says what the tank is for in a pre-made plant
 * (a placed outlet is for nothing in particular): what is stencilled on the shell and what a fuelled plant build fills
 * it with.
 */
public class StorageTankOutletBlock extends StorageTankPortBlock {
	public enum Service implements StringRepresentable {
		NONE("none", null), CONDENSATE("condensate", "minecraft:water"), REFUELLING_WATER("refuelling_water", "minecraft:water"),
		DIESEL_FUEL("diesel_fuel", "jugcraft:diesel"), DIESEL_DAY_A("diesel_day_a", "jugcraft:diesel"), DIESEL_DAY_B("diesel_day_b", "jugcraft:diesel");

		private final String name;
		/** What a fuelled build fills the tank with (most of the way), or null. */
		public final @Nullable String fill;

		Service(String name, @Nullable String fill) {
			this.name = name;
			this.fill = fill;
		}

		@Override
		public String getSerializedName() {
			return name;
		}

		/** A fuel tank: stencilled with the no-smoking warnings and the fire diamond. */
		public boolean diesel() {
			return "jugcraft:diesel".equals(fill);
		}
	}

	public static final EnumProperty<Service> SERVICE = EnumProperty.create("service", Service.class);

	public StorageTankOutletBlock(Properties properties) {
		super(properties, true);
		registerDefaultState(defaultBlockState().setValue(SERVICE, Service.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(SERVICE);
	}
}
