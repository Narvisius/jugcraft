package io.github.jimbozoomer.jugcraft.aircraft;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

public final class JugcraftAircraft {
    public static final EntityType<AircraftBullet> BULLET=Registry.register(BuiltInRegistries.ENTITY_TYPE,Jugcraft.id("aircraft_cannon_round"),EntityType.Builder.<AircraftBullet>of(AircraftBullet::new,MobCategory.MISC).sized(.15F,.15F).clientTrackingRange(8).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("aircraft_cannon_round"))));
    public static final Map<String,EntityType<Aircraft>> TYPES=new LinkedHashMap<>();
    public static final Map<String,MenuType<AircraftMenu>> MENUS=new LinkedHashMap<>();
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    public static final Map<String,net.minecraft.sounds.SoundEvent> SOUNDS=new LinkedHashMap<>();
    private JugcraftAircraft(){}
    public static void register(){
        AircraftSpec.load();
        for(String sound:List.of("engine_start","engine_start_warship","engine_start_bamboo_hopper","warship","propeller","propeller_bamboo_hopper","propeller_small","propeller_tiny","woosh","cannon","repair")){
            var id=Jugcraft.id("aircraft_"+sound);SOUNDS.put(sound,Registry.register(BuiltInRegistries.SOUND_EVENT,id,net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id)));
        }
        for(var spec:AircraftSpec.ALL.values()) {
            var id=Jugcraft.id("aircraft_"+spec.name()); var key=ResourceKey.create(Registries.ENTITY_TYPE,id);
            float w=switch(spec.name()){case "warship"->5;case "bamboo_hopper"->3;case "biplane","cargo_airship"->1.75F;default->1.5F;};
            float h=spec.name().equals("warship")?6.5F:spec.name().contains("airship")?3:1;
            TYPES.put(spec.name(),Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<Aircraft>of((t,l)->new Aircraft(t,l,spec),MobCategory.MISC).sized(w,h).noLootTable().clientTrackingRange(12).updateInterval(1).build(key)));
            MENUS.put(spec.name(),Registry.register(BuiltInRegistries.MENU,id,new MenuType<>((i,p)->new AircraftMenu(i,p,spec),FeatureFlags.DEFAULT_FLAGS)));
            item(spec.name(),true);
        }
        for(String name:List.of("hull","engine","sail","propeller","boiler","rotary_cannon","heavy_crossbow","telescope","bomb_bay"))item(name,false);
        for(String name:AircraftSpec.UPGRADES.keySet())item(name,false);
        PayloadTypeRegistry.serverboundPlay().register(AircraftInput.TYPE,AircraftInput.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AircraftInput.TYPE,(p,c)->{if(c.player().getVehicle() instanceof Aircraft craft)craft.steer(c.player(),p);});
        UseEntityCallback.EVENT.register((p,l,h,e,hit)->e instanceof Aircraft a?a.use(p,h):InteractionResult.PASS);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->ITEMS.values().forEach(out::accept));
    }
    private static void item(String name,boolean vehicle){
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("aircraft_"+name));var p=new Item.Properties().setId(key);
        ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,key,vehicle?new CraftItem(p.stacksTo(1),name):new Item(p)));
    }
    private static final class CraftItem extends Item {
        private final String kind;
        CraftItem(Properties p,String kind){super(p);this.kind=kind;}
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getLevel() instanceof ServerLevel level))return InteractionResult.SUCCESS;
            var at=c.getClickedPos().relative(c.getClickedFace());var player=c.getPlayer();
            if(player==null||!Authority.mayChange(level,player,at))return InteractionResult.FAIL;
            var craft=new Aircraft(TYPES.get(kind),level,AircraftSpec.ALL.get(kind));var point=Vec3.atBottomCenterOf(at);
            craft.snapTo(point.x,point.y,point.z,player.getYRot(),0);craft.setOwner(player.getUUID());
            var dye=c.getItemInHand().get(net.minecraft.core.component.DataComponents.DYED_COLOR);if(dye!=null)craft.paint(dye.rgb());
            if(!level.noCollision(craft,craft.getBoundingBox())||!level.getWorldBorder().isWithinBounds(craft.getBoundingBox()))return InteractionResult.FAIL;
            if(!level.addFreshEntity(craft))return InteractionResult.FAIL;
            if(!player.getAbilities().instabuild)c.getItemInHand().shrink(1);
            return InteractionResult.SUCCESS;
        }
    }
}
