package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.gear.JugcraftGear;
import io.github.jimbozoomer.jugcraft.nuclear.HazmatItem;
import io.github.jimbozoomer.jugcraft.tools.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.*;
import net.minecraft.util.Unit;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.Level;

/** Source industrial equipment uses the single JE component, armor pipeline and existing flight input. */
public final class IndustrialGear {
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    private static final List<EquipmentSlot> ARMOR=List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
    private static final Identifier SPEED=Jugcraft.id("jimta_quantum_stride");
    private IndustrialGear(){}
    public static void register(){
        try(var in=IndustrialGear.class.getResourceAsStream("/jugcraft/jimtafacts/gear.json")){
            if(in==null)throw new IllegalStateException("Missing industrial gear catalog");
            var rows=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var entry:rows.entrySet()){
                var row=entry.getValue().getAsJsonObject();String behavior=row.get("behavior").getAsString();long capacity=row.has("capacity")?row.get("capacity").getAsLong():0;
                var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(entry.getKey()));var p=new Item.Properties().setId(key).stacksTo(1);
                if(row.has("slot"))p.humanoidArmor(armor(row),ArmorType.valueOf(row.get("slot").getAsString()));
                if(capacity>0)powered(p);
                Item item=switch(behavior){
                    case "bronze_tool"->bronze(p,row.get("kind").getAsString());
                    case "armor","solar","static"->new GearItem(p,behavior,0);
                    case "hazmat"->new SourceHazmat(p);
                    case "powered_armor"->new PoweredArmor(p,capacity,row.get("family").getAsString(),ArmorType.valueOf(row.get("slot").getAsString()));
                    case "jetpack"->new IndustrialJetpack(p,capacity);
                    case "fuel_jetpack"->new IndustrialBackpacks.FuelJetpack(p);
                    case "foam_pack"->new IndustrialBackpacks.FoamPack(p);
                    case "battery","auto_battery","battery_pack","nightvision","single_battery"->new PowerItem(p,behavior,capacity);
                    case "saber"->new NanoSaber(p.sword(ToolMaterial.DIAMOND,6,-2.4F),capacity);
                    case "plasma"->new IndustrialPlasmaLauncher(p);
                    case "drill"->new MiningDrillItem(p.tool(ToolMaterial.DIAMOND,JugcraftTools.MINEABLE_WITH_DRILL,3,-2.8F,0).component(JugcraftTools.DRILL_MODE,MiningDrillItem.SINGLE),capacity,60);
                    case "chainsaw"->new ChainsawItem(p.tool(ToolMaterial.DIAMOND,JugcraftTools.MINEABLE_WITH_CHAINSAW,6,-3,5),capacity,40);
                    default->IndustrialHandTools.create(p,behavior,capacity);
                };
                ITEMS.put(entry.getKey(),Registry.register(BuiltInRegistries.ITEM,key,item));
            }
        }catch(IOException e){throw new IllegalStateException("Cannot load industrial gear",e);}
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->ITEMS.values().forEach(out::accept));
        IndustrialBackpacks.registerStorage();
        ServerTickEvents.END_SERVER_TICK.register(server->server.getPlayerList().getPlayers().forEach(IndustrialGear::tick));
    }
    static Item.Properties powered(Item.Properties p){return p.component(JugcraftTools.ENERGY,0L).component(DataComponents.UNBREAKABLE,Unit.INSTANCE)
            .component(DataComponents.TOOLTIP_DISPLAY,TooltipDisplay.DEFAULT.withHidden(DataComponents.UNBREAKABLE,true));}
    private static ArmorMaterial armor(JsonObject row){
        String asset=row.get("asset").getAsString();int strength=asset.equals("quantum")?3:asset.equals("nano")?2:asset.equals("bronze")?1:0;
        return new ArmorMaterial(20,Map.of(ArmorType.HELMET,1+strength,ArmorType.CHESTPLATE,3+strength,ArmorType.LEGGINGS,2+strength,ArmorType.BOOTS,1+strength,ArmorType.BODY,3+strength),
                10,SoundEvents.ARMOR_EQUIP_IRON,strength,0,TagKey.create(Registries.ITEM,Jugcraft.id("repairs_bronze_gear")),ResourceKey.create(EquipmentAssets.ROOT_ID,Jugcraft.id("jimtafacts/"+asset)));
    }
    private static Item bronze(Item.Properties p,String type){return switch(type){
        case "pickaxe"->new Item(p.pickaxe(JugcraftGear.BRONZE,1,-2.8F));
        case "axe"->new Item(p.axe(JugcraftGear.BRONZE,6,-3.1F));
        case "shovel"->new Item(p.shovel(JugcraftGear.BRONZE,1.5F,-3));
        case "hoe"->new Item(p.hoe(JugcraftGear.BRONZE,-2,-1));
        default->new Item(p.sword(JugcraftGear.BRONZE,3,-2.4F));};}
    public static boolean active(ItemStack stack){return !stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("JimtaGearOff",false);}
    public static class GearItem extends Item {
        public final String behavior;public final long capacity;
        GearItem(Properties p,String behavior,long capacity){super(p);this.behavior=behavior;this.capacity=capacity;}
        @Override public InteractionResult use(Level level,Player p,InteractionHand hand){
            if(p.isShiftKeyDown()){if(!level.isClientSide()){var s=p.getItemInHand(hand);boolean off=active(s);CustomData.update(DataComponents.CUSTOM_DATA,s,t->t.putBoolean("JimtaGearOff",off));p.sendOverlayMessage(Component.literal(off?"Equipment abilities disabled.":"Equipment abilities enabled."));}return InteractionResult.SUCCESS;}
            return super.use(level,p,hand);
        }
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> text,TooltipFlag flag){
            if(capacity>0)text.accept(PoweredToolItem.energyLine(stack));
            text.accept(Component.literal("Shift-use to toggle equipment abilities.").withStyle(net.minecraft.ChatFormatting.GRAY));
            if(behavior.equals("battery_pack"))text.accept(Component.literal("Worn on chest: charges carried tools using its own stored JE."));
            if(behavior.equals("battery"))text.accept(Component.literal("Use to charge carried tools. Use on a power port to supply it; shift-use withdraws JE."));
        }
    }
    public static class PowerItem extends GearItem implements Chargeable {
        PowerItem(Properties p,String behavior,long capacity){super(p,behavior,capacity);}
        @Override public long baseCapacity(){return capacity;}
        @Override public boolean isBarVisible(ItemStack s){return capacity>0;}
        @Override public int getBarWidth(ItemStack s){return PoweredToolItem.barWidth(s);}
        @Override public int getBarColor(ItemStack s){return PoweredToolItem.BAR_COLOR;}
        @Override public InteractionResult use(Level level,Player p,InteractionHand hand){
            if(behavior.equals("battery")||behavior.equals("single_battery")){
                if(!level.isClientSide()){
                    ItemStack supply=p.getItemInHand(hand);long budget=behavior.equals("single_battery")?1200:Math.min(10000,Chargeable.energy(supply));
                    long sent=chargeInventory(p,supply,budget);if(sent>0){if(behavior.equals("single_battery"))supply.consume(1,p);else Chargeable.drain(supply,sent);}
                    p.sendOverlayMessage(Component.literal(sent>0?"Charged tools with "+sent+" JE.":"No carried tool needs charge."));
                }return InteractionResult.SUCCESS;
            }return super.use(level,p,hand);
        }
        @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext ctx){
            return behavior.equals("battery")?IndustrialHandTools.exchangePower(ctx):super.useOn(ctx);
        }
    }
    public static final class SourceHazmat extends HazmatItem {SourceHazmat(Properties p){super(p);}}
    static final class IndustrialJetpack extends PowerItem implements Jetpack {IndustrialJetpack(Properties p,long capacity){super(p,"jetpack",capacity);}@Override public boolean isJetpack(ItemStack s){return active(s);}}
    static final class PoweredArmor extends PowerItem implements Jetpack {
        final String family;final ArmorType slot;
        PoweredArmor(Properties p,long capacity,String family,ArmorType slot){super(p,"powered_armor",capacity);this.family=family;this.slot=slot;}
        @Override public boolean isJetpack(ItemStack s){return family.equals("quantum")&&slot==ArmorType.CHESTPLATE&&active(s);}
    }
    static final class NanoSaber extends PowerItem {
        NanoSaber(Properties p,long capacity){super(p,"saber",capacity);}
        @Override public float getAttackDamageBonus(Entity target,float damage,DamageSource source){var weapon=source.getWeaponItem();return weapon!=null&&weapon.getItem()==this&&(!active(weapon)||Chargeable.energy(weapon)<1000)?Math.min(0,1-damage):0;}
        @Override public void postHurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){if(active(stack))Chargeable.drain(stack,1000);}
    }
    static long chargeInventory(Player p,ItemStack supply,long budget){
        long remaining=budget;
        for(int i=0;i<36&&remaining>0;i++){var target=p.getInventory().getItem(i);
            if(target==supply||!(target.getItem() instanceof Chargeable)||target.getItem() instanceof PowerItem item&&(item.behavior.equals("battery")||item.behavior.equals("auto_battery")||item.behavior.equals("battery_pack")))continue;
            remaining-=Chargeable.charge(target,remaining);
        }return budget-remaining;
    }
    public static void tick(ServerPlayer p){
        boolean speed=false;
        for(var slot:ARMOR){var stack=p.getItemBySlot(slot);if(!(stack.getItem() instanceof GearItem item)||!active(stack))continue;
            if(item.behavior.equals("battery_pack")&&p.tickCount%20==0){long n=chargeInventory(p,stack,Math.min(256,Chargeable.energy(stack)));Chargeable.drain(stack,n);}
            if(item.behavior.equals("solar")&&slot==EquipmentSlot.HEAD&&p.level().isBrightOutside()&&p.level().canSeeSky(p.blockPosition())&&!p.level().isRaining())Chargeable.charge(p.getMainHandItem(),1);
            if(item.behavior.equals("static")&&slot==EquipmentSlot.FEET&&p.tickCount%10==0&&p.onGround()&&p.getDeltaMovement().horizontalDistanceSqr()>.0004)Chargeable.charge(p.getMainHandItem(),1);
            if(slot==EquipmentSlot.HEAD&&(item.behavior.equals("nightvision")||item instanceof PoweredArmor)){
                if(p.level().getMaxLocalRawBrightness(BlockPos.containing(p.getEyePosition()))<8){var effect=p.getEffect(MobEffects.NIGHT_VISION);
                    if((effect==null||effect.getDuration()<40)&&Chargeable.drain(stack,200))p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,200,0,true,false,true));}
                if(item instanceof PoweredArmor a&&a.family.equals("quantum")&&p.tickCount%20==0){
                    if(p.getAirSupply()<p.getMaxAirSupply()-20&&Chargeable.drain(stack,100))p.setAirSupply(Math.min(p.getMaxAirSupply(),p.getAirSupply()+100));
                    if(p.hasEffect(MobEffects.POISON)&&Chargeable.drain(stack,10000))p.removeEffect(MobEffects.POISON);
                    if(p.hasEffect(MobEffects.WITHER)&&Chargeable.drain(stack,25000))p.removeEffect(MobEffects.WITHER);
                }
            }
            if(item instanceof PoweredArmor a&&a.family.equals("quantum")&&slot==EquipmentSlot.LEGS&&p.isSprinting()&&Chargeable.drain(stack,10))speed=true;
        }
        AttributeInstance attribute=p.getAttribute(Attributes.MOVEMENT_SPEED);if(attribute!=null){if(!speed)attribute.removeModifier(SPEED);else if(!attribute.hasModifier(SPEED))attribute.addTransientModifier(new AttributeModifier(SPEED,.3,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));}
        if(p.tickCount%20==0)for(int i=0;i<36;i++){var stack=p.getInventory().getItem(i);if(stack.getItem() instanceof PowerItem b){
            if(b.behavior.equals("auto_battery")&&i<9&&active(stack)){long n=chargeInventory(p,stack,Math.min(Math.max(128,b.capacity/300),Chargeable.energy(stack)));Chargeable.drain(stack,n);}
            if(b.behavior.equals("battery")||b.behavior.equals("auto_battery")||b.behavior.equals("saber")){
                float frame=b.behavior.equals("saber")?(active(stack)&&Chargeable.energy(stack)>=1000?1:0):(float)Math.min(4,Chargeable.energy(stack)*5/Math.max(1,Chargeable.capacity(stack)));
                var model=new CustomModelData(List.of(frame),List.of(),List.of(),List.of());if(!model.equals(stack.get(DataComponents.CUSTOM_MODEL_DATA)))stack.set(DataComponents.CUSTOM_MODEL_DATA,model);}
        }}
    }
    /** Applied after ordinary armor, only on the authoritative server. Every absorbed point consumes stored charge. */
    public static float absorb(LivingEntity entity,DamageSource source,float damage){
        if(!(entity instanceof ServerPlayer p)||damage<=0||source.is(DamageTypes.FELL_OUT_OF_WORLD)||source.is(DamageTypes.GENERIC_KILL))return damage;
        if(source.is(DamageTypes.FALL)){var boots=p.getItemBySlot(EquipmentSlot.FEET);
            if(boots.getItem() instanceof PoweredArmor a&&active(boots)&&(a.family.equals("quantum")||damage<8)&&Chargeable.drain(boots,(long)Math.ceil(damage*1000)))return 0;}
        if(source.is(DamageTypeTags.BYPASSES_ARMOR))return damage;
        float original=damage;
        for(var slot:ARMOR){var stack=p.getItemBySlot(slot);if(!(stack.getItem() instanceof PoweredArmor a)||!active(stack))continue;
            float cover=Math.min(damage,Math.min(original*(a.family.equals("quantum")?.24F:.2F),Chargeable.energy(stack)/1000F));
            if(cover>0&&Chargeable.drain(stack,(long)Math.ceil(cover*1000)))damage-=cover;
        }return Math.max(0,damage);
    }
}
