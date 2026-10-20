package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/** Owner household designs on Jugcraft's existing storage, cutting, seating and authority services. */
public final class Household {
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static BlockEntityType<HouseholdAppliance> APPLIANCE;
    public static BlockEntityType<HouseholdFan> FAN;
    private Household(){}
    public static void register(){
        JsonObject catalog;
        try(var in=Household.class.getResourceAsStream("/jugcraft/menagerie/household.json")){
            catalog=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(IOException e){throw new IllegalStateException(e);}
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("household_door_mat_copy"),DoorMatCopyRecipe.SERIALIZER);HouseholdConstruction.register();HouseholdCooking.register();HouseholdMenu.register();
        var appliances=new ArrayList<Block>();var fans=new ArrayList<Block>();
        for(var entry:catalog.getAsJsonObject("blocks").entrySet()){
            String id=entry.getKey();var definition=entry.getValue().getAsJsonObject();
            String kind=definition.get("kind").getAsString();
            var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id(id));
            var props=BlockBehaviour.Properties.of().setId(key).strength(2).noOcclusion().sound(SoundType.WOOD)
                .lightLevel(s->(kind.equals("switch")||HouseholdCooking.KINDS.contains(kind))&&(HouseholdBlock.flag(s,"lit")||HouseholdBlock.flag(s,"powered"))?12:0);
            if(kind.equals("trampoline"))props.bounceRestitution(1.1F);
            Block block;
            if(kind.equals("generator"))block=new io.github.jimbozoomer.jugcraft.machine.MachineBlock(props,io.github.jimbozoomer.jugcraft.machine.MachineKind.COAL_GENERATOR){
                @Override public io.github.jimbozoomer.jugcraft.machine.Footprint footprint(BlockState state){return io.github.jimbozoomer.jugcraft.machine.Footprint.cuboid(1,1,1);}
            };
            else block=HouseholdBlock.create(props,definition);
            Registry.register(BuiltInRegistries.BLOCK,key,block);BLOCKS.put(id,block);Menagerie.BLOCKS.put(id,block);
            Menagerie.item(id,p->new BlockItem(block,p.useBlockDescriptionPrefix()));
            if(id.equals("household_door_mat"))((FabricBlockEntityType)JugcraftAgriculture.GRAVESTONE_ENTITY).addValidBlock(block);
            if(kind.equals("storage"))((FabricBlockEntityType)JugcraftAgriculture.KITCHEN_CABINET_ENTITY).addValidBlock(block);
            if(kind.equals("generator")){
                ((FabricBlockEntityType)io.github.jimbozoomer.jugcraft.machine.JugcraftMachines.MACHINE_ENTITY).addValidBlock(block);
                io.github.jimbozoomer.jugcraft.machine.JugcraftMachines.registerAutomation(block);
            }
            if(HouseholdCooking.KINDS.contains(kind))appliances.add(block);
            if(id.endsWith("ceiling_fan"))fans.add(block);
        }
        APPLIANCE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("household_appliance"),FabricBlockEntityTypeBuilder.create(HouseholdAppliance::new,appliances.toArray(Block[]::new)).build());
        FAN=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("household_fan"),FabricBlockEntityTypeBuilder.create(HouseholdFan::new,fans.toArray(Block[]::new)).build());
        for(var entry:catalog.getAsJsonObject("items").entrySet()){
            String id=entry.getKey(),name=entry.getValue().getAsJsonObject().get("source").getAsString();
            Menagerie.item(id,p->{
                if(name.equals("knife"))return new Item(p.sword(ToolMaterial.IRON,1,-2));
                if(name.equals("package"))return new MenagerieItems.Pouch(p.stacksTo(1).component(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.EMPTY));
                if(name.equals("wrench")||name.equals("television_remote")||name.equals("spatula"))return new Tool(name,p.stacksTo(1));
                return new Item(p.food(new FoodProperties.Builder().nutrition(name.contains("pizza")?6:name.contains("sandwich")||name.contains("toastie")?5:2).saturationModifier(.5F).build()));
            });
        }
        for(var value:catalog.getAsJsonArray("sounds")){
            String id=value.getAsString();Menagerie.SOUNDS.put(id,Registry.register(BuiltInRegistries.SOUND_EVENT,Jugcraft.id(id),SoundEvent.createVariableRangeEvent(Jugcraft.id(id))));
        }
    }
    private static final class Tool extends Item {
        private final String tool;Tool(String name,Properties p){super(p);tool=name;}
        @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext c){
            var player=c.getPlayer();var level=c.getLevel();var pos=c.getClickedPos();var state=level.getBlockState(pos);
            if(player==null||!(state.getBlock() instanceof HouseholdBlock b))return InteractionResult.PASS;
            if(!Menagerie.mayChange(level,player,pos))return InteractionResult.FAIL;
            if(!level.isClientSide()){
                if(tool.equals("wrench")&&!b.rotatePlaced(level,pos,state,player))return InteractionResult.FAIL;
                else if(tool.equals("television_remote")&&b.kind.equals("display"))b.toggle(level,pos,state);
                else if(tool.equals("spatula")&&level.getBlockEntity(pos) instanceof HouseholdAppliance a){var result=a.removeItem(2,64);if(!result.isEmpty()&&!player.getInventory().add(result))Block.popResource(level,pos.above(),result);}
            }
            return InteractionResult.SUCCESS;
        }
    }
}
