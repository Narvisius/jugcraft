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
    private static void select(Player player,ItemStack stack,PeepoEntity npc){
        var tag=new CompoundTag();tag.putString("Companion",npc.getUUID().toString());tag.putString("Dimension",npc.level().dimension().identifier().toString());tag.putInt("EntityId",npc.getId());tag.putString("Name",npc.getDisplayName().getString());
        CustomData.set(DataComponents.CUSTOM_DATA,stack,tag);
        player.sendOverlayMessage(Component.literal("Selected "+npc.getDisplayName().getString()+". Click a sheep/cow for animal work. Containers cycle Supply (blue) / Output (yellow). Left-click removes."));
    }
    @Override public Component getName(ItemStack stack){String name=data(stack).getStringOr("Name","");return name.isEmpty()?super.getName(stack):Component.literal("Companion Planner: "+name);}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(!level.isClientSide() && player.isShiftKeyDown()){player.getItemInHand(hand).remove(DataComponents.CUSTOM_DATA);player.sendOverlayMessage(Component.literal("Companion selection cleared."));}
        return InteractionResult.SUCCESS;
    }
    @Override public void inventoryTick(ItemStack stack,ServerLevel level,Entity entity,EquipmentSlot slot){
        if(!(entity instanceof ServerPlayer player) || slot!=EquipmentSlot.MAINHAND && slot!=EquipmentSlot.OFFHAND || level.getGameTime()%20!=Math.floorMod(player.getId(),20))return;
        var npc=selected(level,stack);if(npc==null)return;
        if(!npc.orders.allowed(player)){stack.remove(DataComponents.CUSTOM_DATA);return;}
        var tag=data(stack);String name=npc.getDisplayName().getString();
        if(tag.getIntOr("EntityId",-1)!=npc.getId() || !tag.getStringOr("Name","").equals(name)){tag.putInt("EntityId",npc.getId());tag.putString("Name",name);CustomData.set(DataComponents.CUSTOM_DATA,stack,tag);}
    }
    private static InteractionResult edit(Player player,Level level,InteractionHand hand,BlockPos pos,Direction face,boolean remove){
        ItemStack stack=player.getItemInHand(hand);if(!stack.is(ITEM))return InteractionResult.PASS;
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
        boolean container=!CompanionAssignments.bed(level,root) && !CompanionAssignments.work(level,root) && !(level.getBlockEntity(root) instanceof LunchBlockEntity);
        player.sendOverlayMessage(Component.literal(remove?npc.assignments.remove(level,pos):container?npc.assignments.cycleContainer(level,root,face):npc.assignments.assign(level,pos)));
        return InteractionResult.SUCCESS;
    }
    public static void initialize(){
        var id=PeepoMod.id("companion_planner");ITEM=Registry.register(BuiltInRegistries.ITEM,id,new AssignmentTool(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).stacksTo(1)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->e.accept(ITEM));
        UseEntityCallback.EVENT.register((player,level,hand,entity,hit)->{
            var stack=player.getItemInHand(hand);if(!stack.is(ITEM))return InteractionResult.PASS;
            if(entity instanceof net.minecraft.world.entity.animal.Animal animal && CompanionLivestock.assignmentType(animal)!=null){
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
        UseBlockCallback.EVENT.register((p,l,h,hit)->edit(p,l,h,hit.getBlockPos(),hit.getDirection(),false));
        AttackBlockCallback.EVENT.register((p,l,h,pos,dir)->edit(p,l,h,pos,dir,true));
    }
}
