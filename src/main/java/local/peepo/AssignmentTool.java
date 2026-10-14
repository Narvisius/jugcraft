package local.peepo;

import java.util.UUID;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import io.github.jimbozoomer.jugcraft.town.TownProtection;

/** Selection belongs to the held stack. UUID, ownership, reach and loaded state are checked on every edit. */
public final class AssignmentTool extends Item {
    public static Item ITEM;
    public static java.util.function.Consumer<InteractionHand> openPlanner=hand->{};
    public static int zoneKind(ItemStack stack){return stack.is(ITEM)?Math.clamp(data(stack).getIntOr("ZoneKind",data(stack).getBooleanOr("ZoneMode",false)?1:0),0,3):0;}
    public static boolean zoneMode(ItemStack stack){return zoneKind(stack)!=0;}
    public static BlockPos firstCorner(ItemStack stack){var tag=data(stack);return tag.getBooleanOr("HasCorner",false)?BlockPos.of(tag.getLongOr("Corner",0)):null;}
    public static void plannerAction(ServerPlayer player,int hand,int action){
        if(hand<0 || hand>1 || action<0 || action>5 || player.isSpectator())return;var stack=player.getItemInHand(InteractionHand.values()[hand]);if(!stack.is(ITEM) || player.getCooldowns().isOnCooldown(stack))return;
        var npc=selected(player.level(),stack);if(action!=3 && (npc==null || !npc.orders.allowed(player) || npc.distanceToSqr(player)>128*128))return;
        player.getCooldowns().addCooldown(stack,5);
        if(action==3){stack.remove(DataComponents.CUSTOM_DATA);player.sendOverlayMessage(Component.literal("Companion selection cleared."));return;}
        var tag=data(stack);tag.putInt("ZoneKind",action==2?zoneKind(stack):action==1?1:action==4?2:action==5?3:0);tag.remove("ZoneMode");tag.remove("HasCorner");tag.remove("Corner");CustomData.set(DataComponents.CUSTOM_DATA,stack,tag);
        player.sendOverlayMessage(Component.literal(action==4?"Farmland: click two ground corners (maximum 16 x 16).":action==5?"Orchard: click two ground corners (maximum 16 x 16).":action==1?"Livestock Management: click two ground corners (maximum 16 x 16).":action==2?"Corner selection cancelled.":"Normal assignments selected."));
    }
    private static void chooseCorner(Player player,ItemStack stack,PeepoEntity npc,BlockPos pos){
        var first=firstCorner(stack);var tag=data(stack);
        if(first==null){if(!CompanionJobs.permitted(npc,pos) || pos.distToCenterSqr(npc.position())>4096 || npc.level().getBlockState(pos).isAir())return;tag.putBoolean("HasCorner",true);tag.putLong("Corner",pos.asLong());CustomData.set(DataComponents.CUSTOM_DATA,stack,tag);player.sendOverlayMessage(Component.literal("First corner set. Click the opposite ground corner (maximum 16 x 16)."));return;}
        if(!AnimalCareZone.valid(first,pos)){player.sendOverlayMessage(Component.literal("Zone must be at most 16 x 16, with at most 3 blocks of ground-height difference. Choose a closer second corner."));return;}
        if(!player.level().hasChunkAt(first) || !player.level().mayInteract(player,first) || TownProtection.denies(player,player.level(),first))return;
        int before=npc.assignments.loadedStations().size();String result=zoneKind(stack)==1?npc.assignments.assignZone(first,pos):npc.assignments.assignGardenZone(first,pos,zoneKind(stack)==3);player.sendOverlayMessage(Component.literal(result));
        if(npc.assignments.loadedStations().size()>before){tag.putInt("ZoneKind",0);tag.remove("ZoneMode");tag.remove("HasCorner");tag.remove("Corner");CustomData.set(DataComponents.CUSTOM_DATA,stack,tag);}
    }
    public AssignmentTool(Properties p){super(p);}
    private static CompoundTag data(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static PeepoEntity selected(Level level,ItemStack stack){
        if(!stack.is(ITEM))return null;
        var tag=data(stack);if(!level.dimension().identifier().toString().equals(tag.getStringOr("Dimension","")))return null;
        try{
            UUID id=UUID.fromString(tag.getStringOr("Companion",""));
            Entity entity=level instanceof ServerLevel server?server.getEntity(id):level.getEntity(tag.getIntOr("EntityId",-1));
            return entity instanceof PeepoEntity npc && npc.isAlive() && npc.getUUID().equals(id)?npc:null;
        }catch(IllegalArgumentException ignored){return null;}
    }
    static void select(Player player,ItemStack stack,PeepoEntity npc){
        var tag=new CompoundTag();tag.putString("Companion",npc.getUUID().toString());tag.putString("Dimension",npc.level().dimension().identifier().toString());tag.putInt("EntityId",npc.getId());tag.putString("Name",npc.getDisplayName().getString());
        CustomData.set(DataComponents.CUSTOM_DATA,stack,tag);
        player.sendOverlayMessage(Component.literal("Selected "+npc.getDisplayName().getString()+". Shift-right-click to choose a livestock, farmland or orchard zone. Click source water for Water collection. Containers cycle Supply (blue) / Output (yellow). Left-click removes."));
    }
    @Override public Component getName(ItemStack stack){String name=data(stack).getStringOr("Name","");return name.isEmpty()?super.getName(stack):Component.literal("Companion Planner: "+name);}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(level.isClientSide() && player.isShiftKeyDown())openPlanner.accept(hand);
        if(!player.isShiftKeyDown()){
            var water=waterHit(player,level,hand);
            if(water!=null)return edit(player,level,hand,water.getBlockPos(),water.getDirection(),false);
        }
        return InteractionResult.SUCCESS;
    }
    private static net.minecraft.world.phys.BlockHitResult waterHit(Player player,Level level,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(!stack.is(ITEM) || zoneMode(stack) || player.isShiftKeyDown())return null;
        var hit=getPlayerPOVHitResult(level,player,net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY);
        return hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK && CompanionWater.source(level,hit.getBlockPos())?hit:null;
    }
    @Override public void inventoryTick(ItemStack stack,ServerLevel level,Entity entity,EquipmentSlot slot){
        if(!(entity instanceof ServerPlayer player) || slot!=EquipmentSlot.MAINHAND && slot!=EquipmentSlot.OFFHAND || level.getGameTime()%20!=Math.floorMod(player.getId(),20))return;
        var npc=selected(level,stack);if(npc==null)return;
        if(!npc.orders.allowed(player)){stack.remove(DataComponents.CUSTOM_DATA);return;}
        var tag=data(stack);String name=npc.getDisplayName().getString();
        if(tag.getIntOr("EntityId",-1)!=npc.getId() || !tag.getStringOr("Name","").equals(name)){tag.putInt("EntityId",npc.getId());tag.putString("Name",name);CustomData.set(DataComponents.CUSTOM_DATA,stack,tag);}
    }
    static InteractionResult edit(Player player,Level level,InteractionHand hand,BlockPos pos,Direction face,boolean remove){
        ItemStack stack=player.getItemInHand(hand);if(!stack.is(ITEM))return InteractionResult.PASS;
        if(!remove && player.isShiftKeyDown()){if(level.isClientSide())openPlanner.accept(hand);return InteractionResult.SUCCESS;}
        // Consume the attack on both sides so left-click can never start mining.
        if(level.isClientSide() || player.isSpectator())return InteractionResult.SUCCESS;
        if(player.getCooldowns().isOnCooldown(stack))return InteractionResult.SUCCESS;
        player.getCooldowns().addCooldown(stack,5);
        if(!level.hasChunkAt(pos)||player.distanceToSqr(Vec3.atCenterOf(pos))>49 || !level.mayInteract(player,pos)||TownProtection.denies(player,level,pos))return InteractionResult.FAIL;
        var root=CompanionAssignments.canonical(level,pos);
        if(!level.hasChunkAt(root) || !level.mayInteract(player,root) || TownProtection.denies(player,level,root))return InteractionResult.FAIL;
        var npc=selected(level,stack);
        if(npc==null){player.sendOverlayMessage(Component.literal("Select a loaded companion in this dimension first."));return InteractionResult.SUCCESS;}
        if(!npc.orders.allowed(player)){player.sendOverlayMessage(Component.literal("Only the owner or an allowed party member can assign this companion."));return InteractionResult.SUCCESS;}
        if(npc.distanceToSqr(player)>128*128){player.sendOverlayMessage(Component.literal("Move closer to the selected companion."));return InteractionResult.SUCCESS;}
        if(!remove && zoneMode(stack)){chooseCorner(player,stack,npc,pos);return InteractionResult.SUCCESS;}
        boolean container=!CompanionAssignments.bed(level,root) && !CompanionAssignments.work(level,root) && !(level.getBlockEntity(root) instanceof LunchBlockEntity);
        player.sendOverlayMessage(Component.literal(remove?npc.assignments.remove(level,pos):container?npc.assignments.cycleContainer(level,root,face):npc.assignments.assign(level,pos)));
        return InteractionResult.SUCCESS;
    }
    public static void initialize(){
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(PlannerActionPayload.TYPE,PlannerActionPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(PlannerActionPayload.TYPE,(payload,context)->plannerAction(context.player(),payload.hand(),payload.action()));
        var id=PeepoMod.id("companion_planner");ITEM=Registry.register(BuiltInRegistries.ITEM,id,new AssignmentTool(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).stacksTo(1)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->e.accept(ITEM));
        UseEntityCallback.EVENT.register((player,level,hand,entity,hit)->{
            var stack=player.getItemInHand(hand);if(!stack.is(ITEM))return InteractionResult.PASS;
            if(player.isShiftKeyDown()){if(level.isClientSide())openPlanner.accept(hand);return InteractionResult.SUCCESS;}
            if(entity instanceof net.minecraft.world.entity.animal.Animal animal && CompanionLivestock.assignmentType(animal)!=null){
                if(zoneMode(stack)){if(!level.isClientSide())player.sendOverlayMessage(Component.literal("Click two ground blocks to define the selected zone."));return InteractionResult.SUCCESS;}
                if(level.isClientSide() || player.isSpectator())return InteractionResult.SUCCESS;
                if(player.getCooldowns().isOnCooldown(stack))return InteractionResult.SUCCESS;
                player.getCooldowns().addCooldown(stack,5);
                if(player.distanceToSqr(animal)>36 || !level.mayInteract(player,animal.blockPosition()) || TownProtection.denies(player,level,animal.blockPosition()))return InteractionResult.FAIL;
                var selected=selected(level,stack);
                player.sendOverlayMessage(Component.literal(selected==null?"Select a loaded companion first.":!selected.orders.allowed(player)?"You cannot assign this companion.":selected.distanceToSqr(player)>128*128?"Move closer to the companion.":selected.assignments.assignAnimal(animal)));
                return InteractionResult.SUCCESS;
            }
            if(!(entity instanceof PeepoEntity npc))return InteractionResult.PASS;
            if(!level.isClientSide() && !player.isSpectator() && player.distanceToSqr(npc)<=36){
                if(npc.orders.allowed(player))select(player,stack,npc);else player.sendOverlayMessage(Component.literal(npc.orders.tamed()?"This companion belongs to another player.":"Feed this companion once to tame it first."));
            }
            return InteractionResult.SUCCESS;
        });
        UseBlockCallback.EVENT.register((p,l,h,hit)->{var water=waterHit(p,l,h);var at=water==null?hit:water;return edit(p,l,h,at.getBlockPos(),at.getDirection(),false);});
        AttackBlockCallback.EVENT.register((p,l,h,pos,dir)->{var water=waterHit(p,l,h);return edit(p,l,h,water==null?pos:water.getBlockPos(),water==null?dir:water.getDirection(),true);});
    }
}
