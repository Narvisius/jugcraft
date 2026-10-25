package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Native programs for the supplied robot art. The legacy distribution registers no robot implementations. */
public final class WorkshopRobots {
    public static final List<String> PROGRAMS=List.of("clean","picker","lumberjack","planter","leafcutter","carrier","fluid_carrier",
            "bomber","knight","miner","farmer","harvester","shovelman","butcher","builder","crafter","delivery","pump","stripes","breaker");
    public static final Map<String,Item> BOARDS=new LinkedHashMap<>();
    public static final Map<String,Item> SHELLS=new LinkedHashMap<>();
    public static Item ROBOT,LINKER;
    public static EntityType<WorkshopRobot> TYPE;
    private WorkshopRobots(){}
    public static void register(){
        TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,Jugcraft.id("jimta_workshop_robot"),EntityType.Builder.of(WorkshopRobot::new,MobCategory.MISC)
                .sized(.5F,.5F).eyeHeight(.3F).clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimta_workshop_robot"))));
        FabricDefaultAttributeRegistry.register(TYPE,Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,.3)
                .add(Attributes.FLYING_SPEED,.35).add(Attributes.FOLLOW_RANGE,32));
        ROBOT=register("jimta_workshop_robot",p->new RobotItem(p.stacksTo(1),"clean"));
        LINKER=register("jimta_robot_linker",p->new Linker(p.stacksTo(1)));
        for(String program:PROGRAMS){BOARDS.put(program,register("jimta_robot_board_"+program,p->new BoardItem(p,program)));
            SHELLS.put(program,register("jimta_robot_"+program,p->new RobotItem(p.stacksTo(1),program)));}
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->{out.accept(ROBOT);out.accept(LINKER);BOARDS.values().forEach(out::accept);SHELLS.values().forEach(out::accept);});
    }
    private static Item register(String name,java.util.function.Function<Item.Properties,Item> factory){
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(name));return Registry.register(BuiltInRegistries.ITEM,key,factory.apply(new Item.Properties().setId(key)));
    }
    public static String program(ItemStack stack){for(var e:BOARDS.entrySet())if(stack.is(e.getValue()))return e.getKey();return null;}
    static final class BoardItem extends Item {
        private final String program;
        BoardItem(Properties p,String program){super(p);this.program=program;}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<Component> out,TooltipFlag flag){
            out.accept(Component.translatable("tooltip.jugcraft.robot_program."+program).withStyle(net.minecraft.ChatFormatting.GRAY));
            out.accept(Component.literal("Use on your robot. Cards are reusable.").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
        }
    }
    static final class RobotItem extends Item {
        private final String program;
        RobotItem(Properties p,String program){super(p);this.program=program;}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<Component> out,TooltipFlag flag){
            out.accept(Component.translatable("tooltip.jugcraft.robot_program."+program).withStyle(net.minecraft.ChatFormatting.GRAY));
            out.accept(Component.literal("Place on a powered block. Use redstone for a starter charge.").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
            out.accept(Component.literal("Open for cargo; slot 1 holds a tool, filter or blueprint. Shift-use to pause.").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        @Override public InteractionResult useOn(UseOnContext ctx){
            if(!(ctx.getLevel() instanceof ServerLevel level))return InteractionResult.SUCCESS;
            var player=ctx.getPlayer();var home=ctx.getClickedPos();var at=home.relative(ctx.getClickedFace());
            if(player==null||!Authority.mayChange(level,player,home)||!Authority.mayChange(level,player,at)||!level.getBlockState(at).isAir())return InteractionResult.FAIL;
            if(level.getEntitiesOfClass(WorkshopRobot.class,new net.minecraft.world.phys.AABB(home).inflate(24)).size()>=16){player.sendOverlayMessage(Component.literal("This workshop already has 16 robots."));return InteractionResult.FAIL;}
            var robot=TYPE.create(level,EntitySpawnReason.SPAWN_ITEM_USE);if(robot==null)return InteractionResult.FAIL;
            robot.configure(player.getUUID(),home);robot.program(program);robot.snapTo(Vec3.atCenterOf(at));
            if(!level.noCollision(robot)){robot.discard();return InteractionResult.FAIL;}
            if(level.addFreshEntity(robot)){ctx.getItemInHand().consume(1,player);player.sendOverlayMessage(Component.literal("Robot ready. Fit a program board, add cargo, and power the block it was placed on."));return InteractionResult.SUCCESS;}
            return InteractionResult.FAIL;
        }
    }
    static final class Linker extends Item {
        Linker(Properties p){super(p);}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<Component> out,TooltipFlag flag){
            out.accept(Component.literal("Select your robot, then use on an inventory or work-area center.").withStyle(net.minecraft.ChatFormatting.GRAY));
            out.accept(Component.literal("Shift-use in the air: output → input → work area. Links stay within 32 blocks.").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
        }
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            var stack=player.getItemInHand(hand);if(player.isShiftKeyDown()&&!level.isClientSide()){
                int mode=(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getInt("Mode").orElse(0)+1)%3;
                CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putInt("Mode",mode));player.sendOverlayMessage(Component.literal("Robot link: "+List.of("output inventory","input inventory","work area center").get(mode)));
            }return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult useOn(UseOnContext ctx){
            if(!(ctx.getLevel() instanceof ServerLevel level))return InteractionResult.SUCCESS;
            var player=ctx.getPlayer();var data=ctx.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            UUID id;try{id=UUID.fromString(data.getString("Robot").orElse(""));}catch(IllegalArgumentException e){return InteractionResult.FAIL;}
            if(!(level.getEntity(id) instanceof WorkshopRobot robot)||player==null||!robot.ownedBy(player)||!Authority.mayChange(level,player,ctx.getClickedPos()))return InteractionResult.FAIL;
            if(!robot.link(Math.floorMod(data.getInt("Mode").orElse(0),3),ctx.getClickedPos(),ctx.getClickedFace())){player.sendOverlayMessage(Component.literal("Keep links within 32 blocks of the robot's charging point."));return InteractionResult.FAIL;}
            player.sendOverlayMessage(Component.literal("Robot link saved. Shift-use in the air changes link type."));return InteractionResult.SUCCESS;
        }
    }
}
