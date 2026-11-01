package io.github.jimbozoomer.jugcraft.botany;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;

/** Structure-attuned encounters use supplied configurations, equipment tables and single carried rewards. */
public final class BotanicalLoonium extends SimpleJsonResourceReloadListener<JsonElement> {
    public static final BotanicalLoonium INSTANCE=new BotanicalLoonium();
    public static final String MARKER="jugcraft:loonium_encounter";
    private static volatile Map<String,JsonObject> configs=Map.of();
    private BotanicalLoonium(){super(ExtraCodecs.JSON,FileToIdConverter.json("jugtania_loonium_config"));}
    public interface Carrier {void jugcraft$looniumReward(ItemStack stack);}
    public interface Charge {void jugcraft$looniumCharged();}
    @Override protected void apply(Map<Identifier,JsonElement> values,ResourceManager manager,ProfilerFiller profiler){
        var raw=new HashMap<String,JsonObject>();values.forEach((key,value)->{String path=key.getPath();int split=path.indexOf('/');if(split>0)raw.put(path.substring(0,split)+":"+path.substring(split+1),value.getAsJsonObject());});
        var resolved=new HashMap<String,JsonObject>();
        for(var key:raw.keySet())try{resolved.put(key,resolve(key,raw,new HashSet<>()));}catch(RuntimeException e){Jugcraft.LOGGER.error("Rejected botanical encounter {}: {}",key,e.getMessage());}
        configs=Map.copyOf(resolved);
    }
    private static JsonObject resolve(String key,Map<String,JsonObject> raw,Set<String> trail){
        if(trail.size()>=16||!trail.add(key))throw new IllegalArgumentException("Configuration inheritance cycle");
        var own=raw.get(key);if(own==null)throw new IllegalArgumentException("Missing parent "+key);
        var result=own.has("parent")?resolve(own.get("parent").getAsString(),raw,trail):new JsonObject();
        own.entrySet().forEach(e->{if(!e.getKey().equals("parent"))result.add(e.getKey(),e.getValue().deepCopy());});return result;
    }
    static void tick(BotanicalWorkshop flower,ServerLevel level,ServerPlayer actor){
        if(level.getGameTime()%100!=0||level.getDifficulty()==Difficulty.PEACEFUL||flower.mana()<1)return;
        var origin=flower.effectivePos();var base=configs.get("botania:default");if(base==null)return;
        var choices=new ArrayList<Map.Entry<String,JsonObject>>();var structures=level.structureManager();
        for(var structure:structures.getAllStructuresAt(origin).keySet()){
            String id=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(structure).toString();var config=configs.getOrDefault(id,base);var start=structures.getStructureAt(origin,structure);
            if(start.isValid()&&(config.has("boundingBoxType")&&config.get("boundingBoxType").getAsString().equals("full")||structures.structureHasPieceAt(origin,start)))choices.add(Map.entry(id,config));
        }
        var choice=choices.isEmpty()?Map.entry("botania:default",base):choices.get(level.getRandom().nextInt(choices.size()));var config=choice.getValue();
        int cost=number(config,"manaCost",35000,1,35000),limit=number(config,"maxNearbyMobs",10,1,32);
        if(flower.mana()<cost||level.getEntitiesOfClass(Mob.class,new AABB(origin).inflate(9),m->m.entityTags().contains(MARKER)).size()>=limit)return;
        String table=choice.getKey().equals("botania:default")?"jugcraft:jugtania_loonium/default":"jugcraft:jugtania_loonium/"+choice.getKey().replace(':','/');
        var rewards=loot(level,table,Vec3.atCenterOf(origin),null);if(rewards.isEmpty())rewards=loot(level,"jugcraft:jugtania_loonium/default",Vec3.atCenterOf(origin),null);
        var blacklist=TagKey.create(Registries.ITEM,Jugcraft.id("jugtania_loonium_blacklist"));rewards.removeIf(s->s.isEmpty()||s.is(blacklist));if(rewards.isEmpty())return;
        var mobs=config.getAsJsonArray("spawnedMobs");if(mobs==null||mobs.isEmpty())return;int total=0;for(var value:mobs)total+=number(value.getAsJsonObject(),"weight",1,1,100000);
        int roll=level.getRandom().nextInt(total);JsonObject selected=null;
        for(var value:mobs){var mob=value.getAsJsonObject();roll-=number(mob,"weight",1,1,100000);if(roll<0){selected=mob;break;}}if(selected==null)return;
        var type=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(selected.get("type").getAsString()));if(type==null)return;
        if(!(type.create(level,EntitySpawnReason.MOB_SUMMONED) instanceof Mob mob))return;
        boolean found=false;
        for(int attempt=0;attempt<16;attempt++){
            var at=origin.offset(level.getRandom().nextInt(11)-5,level.getRandom().nextInt(5)-1,level.getRandom().nextInt(11)-5);
            if(!level.hasChunkAt(at)||!level.getWorldBorder().isWithinBounds(at)||!level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP))continue;
            mob.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,level.getRandom().nextFloat()*360,0);
            boolean permitted=true;for(var p:BlockPos.betweenClosed(BlockPos.containing(mob.getBoundingBox().minX,mob.getBoundingBox().minY,mob.getBoundingBox().minZ),BlockPos.containing(mob.getBoundingBox().maxX,mob.getBoundingBox().maxY,mob.getBoundingBox().maxZ)))if(!level.hasChunkAt(p)||!Authority.mayChange(level,actor,p))permitted=false;
            if(permitted&&level.noCollision(mob)&&level.containsAnyLiquid(mob.getBoundingBox())==false){found=true;break;}
        }
        if(!found)return;
        mob.finalizeSpawn(level,level.getCurrentDifficultyAt(mob.blockPosition()),EntitySpawnReason.MOB_SUMMONED,null);
        if(selected.has("spawnAsBaby"))mob.setBaby(selected.get("spawnAsBaby").getAsBoolean());
        modifiers(mob,selected.has("attributeModifiers")?selected.getAsJsonArray("attributeModifiers"):config.getAsJsonArray("attributeModifiers"));
        effects(mob,selected.has("effectsToApply")?selected.getAsJsonArray("effectsToApply"):config.getAsJsonArray("effectsToApply"));
        if(selected.has("nbt")){
            var nbt=selected.getAsJsonObject("nbt");if(nbt.has("powered")&&nbt.get("powered").getAsInt()!=0&&mob instanceof Creeper)((Charge)mob).jugcraft$looniumCharged();
            if(mob instanceof AbstractPiglin piglin&&nbt.has("IsImmuneToZombification"))piglin.setImmuneToZombification(nbt.get("IsImmuneToZombification").getAsInt()!=0);
            if(nbt.has("Brain")){mob.getBrain().setMemory(MemoryModuleType.ADMIRING_DISABLED,true);mob.getBrain().setMemory(MemoryModuleType.UNIVERSAL_ANGER,true);}
        }
        if(selected.has("equipmentTable"))for(var stack:loot(level,remap(selected.get("equipmentTable").getAsString()),mob.position(),mob)){
            var slot=stack.is(TagKey.create(Registries.ITEM,Jugcraft.id("jugtania_loonium_offhand_equipment")))?EquipmentSlot.OFFHAND:mob.getEquipmentSlotForItem(stack);
            if(mob.getItemBySlot(slot).isEmpty()){mob.setItemSlot(slot,stack);mob.setDropChance(slot,0);}
        }
        mob.setCanPickUpLoot(false);mob.addTag(MARKER);((Carrier)mob).jugcraft$looniumReward(rewards.get(level.getRandom().nextInt(rewards.size())).copy());
        if(!flower.draw(cost))return;if(!level.addFreshEntity(mob)){flower.fill(cost);return;}level.levelEvent(2004,origin,0);
    }
    private static String remap(String id){return id.startsWith("botania:")?"jugcraft:jugtania_"+id.substring(8):id;}
    private static ArrayList<ItemStack> loot(ServerLevel level,String id,Vec3 origin,Mob mob){
        var key=ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse(id));var table=level.getServer().reloadableRegistries().getLootTable(key);if(table==LootTable.EMPTY)return new ArrayList<>();
        var builder=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,origin);if(mob!=null)builder.withParameter(LootContextParams.THIS_ENTITY,mob);
        return new ArrayList<>(table.getRandomItems(builder.create(mob==null?LootContextParamSets.CHEST:LootContextParamSets.SELECTOR)));
    }
    private static void modifiers(Mob mob,JsonArray values){if(values==null)return;int i=0;for(var value:values){
        var data=value.getAsJsonObject();String id=data.get("attribute").getAsString().replace("minecraft:generic.","minecraft:");
        var attribute=BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(id));if(attribute.isEmpty())continue;var instance=mob.getAttribute(attribute.get());if(instance==null)continue;
        var operation=switch(data.get("operation").getAsString()){case "multiply_base"->AttributeModifier.Operation.ADD_MULTIPLIED_BASE;case "multiply_total"->AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;default->AttributeModifier.Operation.ADD_VALUE;};
        instance.addPermanentModifier(new AttributeModifier(Jugcraft.id("loonium_"+i++),data.get("amount").getAsDouble(),operation));
    }mob.setHealth(mob.getMaxHealth());}
    private static void effects(Mob mob,JsonArray values){if(values==null)return;for(var value:values){var data=value.getAsJsonObject();var effect=BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(data.get("effect").getAsString()));if(effect.isPresent())mob.addEffect(new MobEffectInstance(effect.get(),number(data,"duration",-1,-1,72000),number(data,"amplifier",0,0,10)));}}
    private static int number(JsonObject data,String key,int fallback,int min,int max){return data.has(key)?Math.clamp(data.get(key).getAsInt(),min,max):fallback;}
}
