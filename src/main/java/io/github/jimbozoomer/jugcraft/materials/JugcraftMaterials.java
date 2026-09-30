package io.github.jimbozoomer.jugcraft.materials;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;

/** Jugcraft's canonical metals. IDs here are permanent once released. */
public final class JugcraftMaterials {
	public static MetalFamily TIN;
	public static MetalFamily BRONZE;

	private JugcraftMaterials() {
	}

	public static void register() {
		TIN = MetalFamily.builder("tin").mined().build();
		BRONZE = MetalFamily.builder("bronze").extraItem("bronze_blend").build();

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output -> {
			output.accept(TIN.ore);
			output.accept(TIN.deepslateOre);
			output.accept(TIN.rawBlock);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.accept(TIN.raw);
			output.accept(TIN.ingot);
			output.accept(TIN.nugget);
			for (var extra : BRONZE.extras) {
				output.accept(extra);
			}
			output.accept(BRONZE.ingot);
			output.accept(BRONZE.nugget);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> {
			output.accept(TIN.storageBlock);
			output.accept(BRONZE.storageBlock);
		});
	}
}
