package io.github.jimbozoomer.jugcraft.styx;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class JugcraftStyx {
	public static final String[] NAMES = {"stolas_starflower", "witchglass_orchid", "ravenquill_lupine", "amethyst_mourningbell",
			"eclipse_camellia", "astral_verbena", "inkvein_helleborine", "violet_lanternbloom"};
	public static final Map<String, Block> FLOWERS = new LinkedHashMap<>();
	public static EntityType<Styxhexenhammer> WIZARD;
	private JugcraftStyx() { }
	public static void register() {
		for (String name : NAMES) {
			ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(name));
			boolean tall = name.equals("ravenquill_lupine");
			BlockBehaviour.Properties props = BlockBehaviour.Properties.ofFullCopy(tall ? Blocks.LILAC : Blocks.DANDELION).randomTicks().setId(key);
			Block block = Registry.register(BuiltInRegistries.BLOCK, key, tall ? new TallFlowerBlock(props) : new StyxFlowerBlock(props));
			FLOWERS.put(name, block);
			ResourceKey<Item> item = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
			Registry.register(BuiltInRegistries.ITEM, item, new BlockItem(block, new Item.Properties().setId(item).useBlockDescriptionPrefix()));
			if (!tall) {
				ResourceKey<Block> pot = ResourceKey.create(Registries.BLOCK, Jugcraft.id("potted_" + name));
				Registry.register(BuiltInRegistries.BLOCK, pot, new FlowerPotBlock(block, BlockBehaviour.Properties.ofFullCopy(Blocks.FLOWER_POT).setId(pot)));
			}
		}
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("styxhexenhammer"));
		WIZARD = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.<Styxhexenhammer>of(Styxhexenhammer::new, MobCategory.MISC)
				.sized(0.65F, 1.95F).eyeHeight(1.62F).fireImmune().clientTrackingRange(10).build(key));
		FabricDefaultAttributeRegistry.register(WIZARD, Styxhexenhammer.createAttributes());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output -> FLOWERS.values().forEach(output::accept));
		StyxConservatory.register();
	}
}
