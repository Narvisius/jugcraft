package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

/** Supplied Supplementaries household mechanics; remaining families are tracked separately. */
public final class OwnerSupplementaries {
    public static final Map<String,net.minecraft.sounds.SoundEvent> SOUNDS=new LinkedHashMap<>();
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static BlockEntityType<OwnerSupplementData> DATA;
    public static JsonArray HOURGLASS;
    private OwnerSupplementaries(){}
    public static void register(){
        for(String name:List.of("pignata_hit","pignata_burst","globe_spin","bellows_blow","bellows_retract","soap_wash","awning_bounce","confetti_popper","block_rotate","wrench_rotate","wrench_fail","wrench_hit","cannon_fire","bubble_blow","bubble_pop","bubble_place","slingshot_charge","slingshot_shoot","bomb_explode","heave_ho","plunderer_idle","plunderer_hurt","plunderer_death","plunderer_celebrate")){var id=Jugcraft.id("supps_"+name);SOUNDS.put(name,Registry.register(BuiltInRegistries.SOUND_EVENT,id,net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id)));}
        for(var entry:OwnerBackpacks.source("newest_supps.json").getAsJsonObject("blocks").entrySet()){
            String name=entry.getKey();var d=entry.getValue().getAsJsonObject();var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("supps_"+name));
            boolean wood=d.get("wood").getAsBoolean();var props=BlockBehaviour.Properties.ofFullCopy(wood?Blocks.OAK_PLANKS:Blocks.STONE).setId(key);String kind=d.get("kind").getAsString(),function=d.has("function")?d.get("function").getAsString():"";if(Set.of("mob_head","spring_arm","way_sign","hidden_bricks","bubble","rope_bunting").contains(function))props.dynamicShape();
            if(!kind.equals("block"))props.noOcclusion();if(name.equals("gunpowder"))props.noCollision().instabreak().pushReaction(net.minecraft.world.level.material.PushReaction.POPPED);if(name.equals("bubble_block"))props.instabreak().noLootTable().replaceable();if(function.equals("mob_head"))props.instrument(NoteBlockInstrument.CUSTOM_HEAD);
            if(function.endsWith("lamp"))props.lightLevel(s->15);
            if(name.startsWith("sconce"))props.lightLevel(s->HouseholdBlock.flag(s,"lit")?(name.contains("soul")?10:14):0);
            if(name.equals("redstone_illuminator"))props.lightLevel(s->Integer.parseInt(HouseholdBlock.value(s,"power")));
            if(name.equals("statue"))props.lightLevel(s->HouseholdBlock.flag(s,"lit")?12:0);
            if(name.equals("fire_pit"))props.lightLevel(s->HouseholdBlock.flag(s,"lit")?15:0);
            if(name.equals("grouper"))props.lightLevel(s->HouseholdBlock.value(s,"match").equals("none")?0:5);
            if(name.equals("hourglass"))props.lightLevel(s->Integer.parseInt(HouseholdBlock.value(s,"light_level")));
            Block block=switch(kind){
                case "flax"->new OwnerSupplementFlax(props.noCollision().randomTicks());
                case "wild_flax"->new OwnerWildFlax(props.noCollision());
                case "supps_terrain"->HouseholdBlock.create(name.equals("ash")?props.randomTicks():name.equals("soap_block")?props.friction(.98F):props,d);
                case "barnacles"->new OwnerBarnacles(props.noCollision().strength(.4F).sound(SoundType.CORAL_BLOCK));
                case "cannonball"->new OwnerCannonPile(props.noOcclusion());
                case "potted_flax"->new FlowerPotBlock(BLOCKS.get("flax"),props.noCollision().instabreak());
                case "candle_holder"->new OwnerCandleHolder(props.lightLevel(CandleBlock.LIGHT_EMISSION),d);
                case "metal_gate"->new OwnerMetalGate(props.sound(SoundType.METAL),name.equals("gold_gate"));
                case "supps"->HouseholdBlock.create(props,d);
                case "bunting"->HouseholdBlock.create(props.noCollision().instabreak().sound(SoundType.WOOL),d);
                case "slab"->name.equals("fine_wood_slab")?new OwnerDecorVariants.AxisSlab(props):new SlabBlock(props);
                case "stairs"->new StairBlock((wood?Blocks.OAK_PLANKS:Blocks.STONE).defaultBlockState(),props);
                case "wall"->new WallBlock(props);
                case "crumbly"->new OwnerHiddenBricks.Crumbly(props);
                case "assembly_motion"->new OwnerAssemblyMotion.Moving(props);
                case "key_lock"->new OwnerKeyLocks.Lock(props);
                case "door"->name.equals("netherite_door")?new OwnerKeyLocks.Door(props):new DoorBlock(BlockSetType.IRON,props);
                case "trapdoor"->name.equals("netherite_trapdoor")?new OwnerKeyLocks.Trap(props):new TrapDoorBlock(BlockSetType.IRON,props);
                case "fence"->name.equals("wicker_fence")?new WallBlock(props):new FenceBlock(props);
                case "bars"->new IronBarsBlock(props);
                case "pillar"->new RotatedPillarBlock(props);
                default->name.equals("daub_brace")?new OwnerDecorVariants.Brace(props):name.equals("flax_block")?new OwnerDecorVariants.Facing(props):new Block(props);
            };
            Registry.register(BuiltInRegistries.BLOCK,key,block);BLOCKS.put(name,block);
        }
        for(var entry:BLOCKS.entrySet()){
            String name=entry.getKey();Block block=entry.getValue();if(name.equals("pignata_anchor")||name.equals("moving_pulley_block")||name.equals("gunpowder")||name.equals("bubble_block")||name.equals("enderman_wall_head")||name.equals("spider_wall_head")||name.equals("way_sign")||name.equals("way_sign_wall")||name.equals("rope_buntings")||name.startsWith("bunting_wall_")||name.equals("confetti_litter")||name.startsWith("sconce_wall")||name.equals("flax")||name.equals("potted_flax")||name.equals("spring_launcher_arm")||name.equals("spring_launcher_head")||name.startsWith("book_pile"))continue;
            var itemKey=ResourceKey.create(Registries.ITEM,Jugcraft.id("supps_"+name));var item=Registry.register(BuiltInRegistries.ITEM,itemKey,(name.equals("enderman_head")||name.equals("spider_head"))?new StandingAndWallBlockItem(block,BLOCKS.get(name.replace("_head","_wall_head")),Direction.DOWN,new Item.Properties().setId(itemKey).rarity(Rarity.UNCOMMON).component(net.minecraft.core.component.DataComponents.EQUIPPABLE,net.minecraft.world.item.equipment.Equippable.builder(net.minecraft.world.entity.EquipmentSlot.HEAD).setDamageOnHurt(false).build()).useBlockDescriptionPrefix()):name.startsWith("bunting_")?new OwnerBuntings.Pennant(block,BLOCKS.get(name.replace("bunting_","bunting_wall_")),DyeColor.byName(name.substring(8),DyeColor.WHITE),new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()):block instanceof OwnerSupplementBlock b&&(b.function.equals("jar")||b.function.equals("cage"))?new OwnerSupplementVessel(block,new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()):name.startsWith("sconce")&&!name.equals("sconce_lever")?new StandingAndWallBlockItem(block,BLOCKS.get(name.replace("sconce","sconce_wall")),Direction.DOWN,new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()):new BlockItem(block,new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()){ @Override public boolean canFitInsideContainerItems(){return !(block instanceof OwnerSupplementBlock b)||!(b.name.startsWith("sack")||b.name.contains("present")||b.name.equals("lunch_basket"));}});
            ITEMS.put(name,item);
            CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e->e.accept(item));
        }
        for(String name:List.of("flax","flax_seeds","soap","ash_brick","flute","altimeter","confetti_popper","wrench","rope_arrow","key","bubble_blower","quiver","slingshot","bomb","bomb_blue","slice_map","candy","antique_ink","hat_stand","pignata","dispenser_minecart")){var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("supps_"+name));var p=new Item.Properties().setId(key);var item=Registry.register(BuiltInRegistries.ITEM,key,name.equals("dispenser_minecart")?new OwnerDispenserCart.Place(p):name.equals("pignata")?new OwnerPignata.Place(p):name.equals("hat_stand")?new OwnerHatStand.Place(p):name.equals("antique_ink")?new OwnerAntiqueInk(p):name.equals("quiver")?new OwnerQuiver(p):name.equals("slingshot")?new OwnerFieldTools.Slingshot(p):name.startsWith("bomb")?new OwnerFieldTools.Bomb(p):name.equals("slice_map")?new OwnerSliceMaps(p):name.equals("candy")?new Item(p.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(1).saturationModifier(.1F).alwaysEdible().build())):name.equals("bubble_blower")?new OwnerBubbles.Blower(p):name.equals("rope_arrow")?new OwnerRopeArrow.Ammo(p):name.equals("confetti_popper")?new OwnerConfetti(BLOCKS.get("confetti_litter"),p.equippable(net.minecraft.world.entity.EquipmentSlot.HEAD)):name.equals("flax_seeds")?new BlockItem(BLOCKS.get("flax"),p):name.equals("soap")?new OwnerSupplementSoap(p):name.equals("flute")?new OwnerSupplementFlute(p):name.equals("wrench")?new OwnerSupplementWrench(p):name.equals("altimeter")?new Item(p.stacksTo(1)){@Override public net.minecraft.world.InteractionResult use(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){if(!level.isClientSide())player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Altitude: "+player.blockPosition().getY()+" blocks"));return net.minecraft.world.InteractionResult.SUCCESS;}}:new Item(p));ITEMS.put(name,item);CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(e->e.accept(item));}
        DATA=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("supps_household"),FabricBlockEntityTypeBuilder.create(OwnerSupplementData::new,BLOCKS.values().stream().filter(b->b instanceof OwnerSupplementBlock).toArray(Block[]::new)).build());
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlockEntity((data,side)->data.block().function.equals("jar")?data.fluid:null,DATA);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("supps_flag_patterns"),OwnerSupplementFlags.PatternRecipe.SERIALIZER);OwnerSupplementTreasures.register();OwnerLumisene.register();OwnerPlunderer.register();OwnerCannonBoat.register();OwnerGalleon.register();OwnerDispenserCart.register();OwnerPignata.register();OwnerHatStand.register();OwnerAntiqueInk.register();OwnerFieldProjectiles.register();OwnerHiddenBricks.register();OwnerAssemblyMotion.register();OwnerBubbles.register();OwnerKeyLocks.register();OwnerRopeArrow.register();OwnerCannon.register();OwnerMobHeads.register();OwnerGlobes.register();OwnerWaySigns.register();OwnerRoadSigns.register();OwnerBarnaclePatch.register();OwnerConfetti.register();OwnerSupplementFlora.register();OwnerBlackboards.register();OwnerSupplementBooks.register();OwnerSupplementText.register();
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player,level,hand,hit)->{
            if(player.isSpectator()||!(player.getItemInHand(hand).is(net.minecraft.tags.ItemTags.HOES)))return net.minecraft.world.InteractionResult.PASS;
            var pos=hit.getBlockPos();var s=level.getBlockState(pos);if(!s.is(Blocks.GRAVEL)&&!s.is(BLOCKS.get("raked_gravel")))return net.minecraft.world.InteractionResult.PASS;
            if(!Menagerie.mayChange(level,player,pos)||hit.getDirection()==Direction.DOWN||level.getBlockState(pos.above()).isSolid())return net.minecraft.world.InteractionResult.FAIL;
            if(!level.isClientSide()){level.setBlockAndUpdate(pos,OwnerSupplementTerrain.connected(BLOCKS.get("raked_gravel").defaultBlockState(),level,pos,player.getDirection()));player.getItemInHand(hand).hurtAndBreak(1,player,hand.asEquipmentSlot());level.playSound(null,pos,net.minecraft.sounds.SoundEvents.HOE_TILL.value(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);}
            return net.minecraft.world.InteractionResult.SUCCESS;
        });
        HOURGLASS=OwnerBackpacks.source("newest_supps.json").getAsJsonArray("hourglass");OwnerSupplementMenu.register();
    }
}
