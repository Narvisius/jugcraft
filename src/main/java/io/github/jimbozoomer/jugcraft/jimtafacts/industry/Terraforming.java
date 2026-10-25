package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tools.*;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/** Explicit selected-area terraforming; source plan art, actual supplies, bounded protected changes. */
public final class Terraforming {
    public static final List<String> MODES=List.of("blank","chilling","cultivation","desertification","flatification","irrigation","mushroom");
    public static final Map<String,Item> PLANS=new LinkedHashMap<>();
    public enum Result { SKIP, WAIT, CHANGED }
    private Terraforming() {}
    public static void register(){
        for(String mode:MODES){var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_terraform_"+mode));
            PLANS.put(mode,Registry.register(BuiltInRegistries.ITEM,key,new Item(new Item.Properties().setId(key).stacksTo(1)){
                @Override public void appendHoverText(ItemStack s,TooltipContext c,TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){out.accept(Component.literal(help(mode)));}
            }));}
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->PLANS.values().forEach(out::accept));
    }
    public static String mode(ItemStack stack){for(var e:PLANS.entrySet())if(stack.is(e.getValue()))return e.getKey();return "blank";}
    public static boolean plan(ItemStack stack){return PLANS.containsValue(stack.getItem());}
    public static String help(String mode){return switch(mode){
        case "chilling"->"Freeze source water. Supply water for snow on supported surfaces (125 mB per layer).";
        case "cultivation"->"Prepare farmland (250 mB water), plant supplied crop seeds, and grow existing crops with bone meal.";
        case "desertification"->"Dry exposed soil into sand and clear snow. No biome-ID rewriting or spreading fire.";
        case "flatification"->"Clear natural terrain above the selection's bottom plane. Tool in slot 2; real drops go to output slots.";
        case "irrigation"->"Prepare and moisten exposed farmland using 250 mB water per soil block.";
        case "mushroom"->"Make mycelium with brown mushrooms, then plant supplied red/brown mushrooms in suitable shade.";
        default->"Blank terraforming plan. Combine with a themed ingredient to program it.";
    };}
    public static boolean material(ItemStack stack){return stack.getItem() instanceof BlockItem||stack.has(DataComponents.TOOL)||stack.is(Items.BONE_MEAL)||stack.is(Items.BROWN_MUSHROOM)||stack.is(Items.RED_MUSHROOM);}
    private static boolean soil(BlockState state){return state.is(Blocks.DIRT)||state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.COARSE_DIRT)||state.is(Blocks.ROOTED_DIRT)||state.is(Blocks.MYCELIUM)||state.is(Blocks.PODZOL)||state.is(Blocks.FARMLAND);}
    private static int supplied(CivilWorkshops.Node node,Item item){for(int slot=1;slot<14;slot++)if(node.getItem(slot).is(item))return slot;return -1;}
    private static Result change(CivilWorkshops.Node node,ServerLevel level,BlockPos at,BlockState wanted,int slot,int water){
        if(water>0&&!node.fluid.has(Fluids.WATER,water)){node.status=7;return Result.WAIT;}
        if(!wanted.canSurvive(level,at)||!level.isUnobstructed(wanted,at,net.minecraft.world.phys.shapes.CollisionContext.empty())){node.status=6;return Result.WAIT;}
        if(!level.setBlock(at,wanted,Block.UPDATE_ALL)){node.status=6;return Result.WAIT;}
        if(slot>=0)node.getItem(slot).shrink(1);if(water>0)node.fluid.drain(water);return Result.CHANGED;
    }
    public static Result work(CivilWorkshops.Node node,ServerLevel level,ServerPlayer actor,BlockPos at,BlockState before){
        String mode=mode(node.getItem(0));if(mode.equals("blank")){node.status=5;return Result.WAIT;}
        if(level.getBlockEntity(at)!=null||before.getDestroySpeed(level,at)<0)return Result.SKIP;
        if(!node.allowed(level,actor,at.above())||!node.allowed(level,actor,at.below())){node.status=2;return Result.WAIT;}
        boolean exposed=level.getBlockState(at.above()).getCollisionShape(level,at.above()).isEmpty();
        if(mode.equals("flatification")){
            if(at.getY()<=node.minimum.getY()||!(soil(before)||before.is(Blocks.STONE)||before.is(Blocks.SAND)||before.is(Blocks.RED_SAND)||before.is(Blocks.GRAVEL)||before.is(Blocks.SNOW)||before.is(Blocks.SNOW_BLOCK)))return Result.SKIP;
            var tool=node.getItem(1);
            if(tool.isEmpty()||!tool.has(DataComponents.TOOL)||before.requiresCorrectToolForDrops()&&!tool.isCorrectToolForDrops(before)
                    ||tool.getItem() instanceof PoweredToolItem powered&&Chargeable.energy(tool)<powered.energyPerBlock(tool)){node.status=6;return Result.WAIT;}
            var saved=node.reserve(Block.getDrops(before,level,at,null,actor,tool));if(saved==null){node.status=8;return Result.WAIT;}
            if(!level.destroyBlock(at,false,actor)){node.status=6;return Result.WAIT;}
            node.apply(saved);tool=node.getItem(1);
            if(tool.getItem() instanceof PoweredToolItem powered)Chargeable.drain(tool,powered.energyPerBlock(tool));else if(tool.isDamageableItem())tool.hurtAndBreak(1,actor,EquipmentSlot.MAINHAND);
            return Result.CHANGED;
        }
        if(mode.equals("chilling")){
            if(before.is(Blocks.WATER)&&before.getFluidState().isSource())return change(node,level,at,Blocks.ICE.defaultBlockState(),-1,0);
            if(before.is(Blocks.SNOW)&&before.getValue(SnowLayerBlock.LAYERS)<8)return change(node,level,at,before.setValue(SnowLayerBlock.LAYERS,before.getValue(SnowLayerBlock.LAYERS)+1),-1,125);
            if(before.isAir()&&Blocks.SNOW.defaultBlockState().canSurvive(level,at))return change(node,level,at,Blocks.SNOW.defaultBlockState(),-1,125);
            return Result.SKIP;
        }
        if(mode.equals("desertification")){
            if(soil(before)&&exposed&&level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP))return change(node,level,at,Blocks.SAND.defaultBlockState(),-1,0);
            if(before.is(Blocks.SNOW)||before.is(Blocks.SNOW_BLOCK))return change(node,level,at,Blocks.AIR.defaultBlockState(),-1,0);
            return Result.SKIP;
        }
        if(mode.equals("irrigation")||mode.equals("cultivation")){
            if(soil(before)&&exposed){
                if(before.is(Blocks.FARMLAND)&&before.getValue(FarmlandBlock.MOISTURE)==7)return Result.SKIP;
                return change(node,level,at,Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7),-1,250);
            }
            if(mode.equals("irrigation"))return Result.SKIP;
            if(before.getBlock() instanceof CropBlock crop&&!crop.isMaxAge(before)){
                int fertilizer=supplied(node,Items.BONE_MEAL);if(fertilizer<0){node.status=5;return Result.WAIT;}
                return change(node,level,at,crop.getStateForAge(Math.min(crop.getMaxAge(),crop.getAge(before)+1)),fertilizer,0);
            }
            if(before.isAir()&&level.getBlockState(at.below()).is(Blocks.FARMLAND)){
                for(int slot=1;slot<14;slot++)if(node.getItem(slot).getItem() instanceof BlockItem seed&&seed.getBlock() instanceof CropBlock crop)
                    return change(node,level,at,crop.defaultBlockState(),slot,0);
                node.status=5;return Result.WAIT;
            }
        }
        if(mode.equals("mushroom")){
            if(soil(before)&&!before.is(Blocks.MYCELIUM)&&exposed){int sample=supplied(node,Items.BROWN_MUSHROOM);if(sample<0){node.status=5;return Result.WAIT;}return change(node,level,at,Blocks.MYCELIUM.defaultBlockState(),sample,0);}
            if(before.isAir()&&level.getBlockState(at.below()).is(Blocks.MYCELIUM)){
                for(Item item:List.of(Items.BROWN_MUSHROOM,Items.RED_MUSHROOM)){int slot=supplied(node,item);if(slot>=0)return change(node,level,at,((BlockItem)item).getBlock().defaultBlockState(),slot,0);}
                node.status=5;return Result.WAIT;
            }
        }
        return Result.SKIP;
    }
}
