package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Native cave encounters and equipment; shares the saved companion and claim systems. */
public final class Caverns {
    private Caverns(){}
    static MenagerieAnimal create(EntityType<? extends MenagerieAnimal> type,Level level,Menagerie.Species s){return s.source().equals("ferrouslime")?new RecoveredFerrouslime(type,level,s):s.mood().equals("hostile")?new Hostile(type,level,s):new Creature(type,level,s);}
    static void register(){
        CavernColorLoot.register();
        CaveWorkshop.register();CavernDevice.register();CavernSubmarine.register();CavernControl.register();CavernProjectile.register();OwnerArrow.register();
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%20==0)for(var p:server.getPlayerList().getPlayers())tickEquipment(p);});
    }
    static Item item(String name,JsonObject data,Item.Properties p){
        if(data.has("species"))return new MenagerieItems.Spawn(data.get("species").getAsString(),p);
        if(name.endsWith("_bucket"))return new MenagerieItems.Captured(name,p.stacksTo(1));
        if(data.has("food")&&data.get("food").getAsBoolean())p.food(new FoodProperties.Builder().nutrition(name.startsWith("cooked_")?7:name.contains("stew")||name.contains("soup")||name.contains("meal")?9:3).saturationModifier(.6F).build());
        if(data.has("armor")&&!data.get("armor").getAsString().isEmpty()){
            var material=new ArmorMaterial(25,Map.of(ArmorType.HELMET,3,ArmorType.CHESTPLATE,7,ArmorType.LEGGINGS,5,ArmorType.BOOTS,2,ArmorType.BODY,7),16,SoundEvents.ARMOR_EQUIP_LEATHER,1,0,TagKey.create(Registries.ITEM,Jugcraft.id("cavern_armor_repair")),ResourceKey.create(EquipmentAssets.ROOT_ID,Jugcraft.id("cavern/"+name)));
            p.humanoidArmor(material,ArmorType.valueOf(data.get("armor").getAsString().toUpperCase(Locale.ROOT)));
        }
        if(data.has("food")&&data.get("food").getAsBoolean()){if(name.endsWith("bottle"))p.usingConvertsTo(Items.GLASS_BOTTLE).stacksTo(16);else if(name.endsWith("stew")||name.endsWith("soup")||name.endsWith("salad"))p.usingConvertsTo(Items.BOWL).stacksTo(16);}
        if(Set.of("cave_book","cave_codex").contains(name))return new MenagerieItems.Guide(p.stacksTo(1));
        if(name.equals("submarine"))return new CavernSubmarine.Kit(p);
        if(name.equals("sack_of_sating"))return new SatingSack(p);
        if(name.equals("candy_cane_hook"))return new CompanionCreatures.SilkGrapple(p.durability(250));
        if(name.equals("dreadbow"))return new BowItem(p.durability(500));
        if(name.equals("resistor_shield"))return new ShieldItem(p.durability(600));
        if(name.endsWith("_arrow"))return OwnerArrow.item("cavern_"+name,p);
        if(Set.of("primitive_club","limestone_spear","extinction_spear","ortholance","desolate_dagger","sharpened_candy_cane","frostmint_spear","galena_gauntlet").contains(name))p.sword(ToolMaterial.DIAMOND,name.equals("primitive_club")?5:3,name.equals("primitive_club")?-3.2F:-2.4F);
        if(Set.of("cave_map","underground_cabin_explorer_map","raygun","shot_gum","sea_staff","sugar_staff","quarry_smasher","holocoder","game_controller","remote_detonator","totem_of_possession").contains(name))p.stacksTo(1);
        if(name.startsWith("music_disc_"))p.stacksTo(1).jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG,Jugcraft.id("cavern_"+name.substring(11))));
        return new Relic(name,p);
    }
    private static boolean worn(Player p,EquipmentSlot slot,String name){var item=Menagerie.ITEMS.get("cavern_"+name);return item!=null&&p.getItemBySlot(slot).is(item);}
    static boolean hazmat(Player p){return worn(p,EquipmentSlot.HEAD,"hazmat_mask")&&worn(p,EquipmentSlot.CHEST,"hazmat_chestplate")&&worn(p,EquipmentSlot.LEGS,"hazmat_leggings")&&worn(p,EquipmentSlot.FEET,"hazmat_boots");}
    private static void effect(LivingEntity p,Holder<MobEffect> effect,int duration){p.addEffect(new MobEffectInstance(effect,duration,0,true,false));}
    public static void tickEquipment(ServerPlayer p){
        // Equipment and environmental hazards must never generate chunks during login/teleport or at view borders.
        if(p.isRemoved()||!p.level().hasChunkAt(p.blockPosition()))return;
        if(worn(p,EquipmentSlot.HEAD,"diving_helmet")&&p.isInWater())effect(p,MobEffects.WATER_BREATHING,60);
        if(worn(p,EquipmentSlot.CHEST,"diving_chestplate")&&p.isInWater())effect(p,MobEffects.DOLPHINS_GRACE,40);
        if(worn(p,EquipmentSlot.FEET,"rainbounce_boots")&&!p.onGround())effect(p,MobEffects.SLOW_FALLING,40);
        if(worn(p,EquipmentSlot.HEAD,"hood_of_darkness")&&p.level().getMaxLocalRawBrightness(p.blockPosition())<8)effect(p,MobEffects.NIGHT_VISION,240);
        if(worn(p,EquipmentSlot.CHEST,"cloak_of_darkness")&&p.isShiftKeyDown()&&p.level().getMaxLocalRawBrightness(p.blockPosition())<5)effect(p,MobEffects.INVISIBILITY,40);
        if(worn(p,EquipmentSlot.HEAD,"primordial_helmet")&&worn(p,EquipmentSlot.CHEST,"primordial_tunic")&&worn(p,EquipmentSlot.LEGS,"primordial_pants"))effect(p,MobEffects.STRENGTH,40);
        var fluid=p.level().getFluidState(p.blockPosition()).getType();var acid=io.github.jimbozoomer.jugcraft.chemistry.PetroFluids.FLUIDS.get("cavern_acid");var soda=io.github.jimbozoomer.jugcraft.chemistry.PetroFluids.FLUIDS.get("cavern_purple_soda");
        if(acid!=null&&acid.source().isSame(fluid)&&!hazmat(p)&&!p.isCreative()&&!p.isSpectator()){p.hurtServer(p.level(),p.damageSources().magic(),2);effect(p,MobEffects.POISON,60);}
        if(soda!=null&&soda.source().isSame(fluid))effect(p,MobEffects.SLOW_FALLING,40);
        boolean radiation=false;for(var at:BlockPos.betweenClosed(p.blockPosition().offset(-2,-1,-2),p.blockPosition().offset(2,1,2))){if(!p.level().hasChunkAt(at))continue;var state=p.level().getBlockState(at);if(state.is(Menagerie.BLOCKS.get("cavern_unrefined_waste"))||state.is(Menagerie.BLOCKS.get("cavern_block_of_uranium"))){radiation=true;break;}}
        if(radiation&&!hazmat(p)&&!p.isCreative()&&!p.isSpectator())effect(p,MobEffects.POISON,80);
        if(hazmat(p)){p.removeEffect(MobEffects.POISON);p.removeEffect(MobEffects.NAUSEA);}
    }
    static final class Hostile extends Creature implements Enemy {Hostile(EntityType<? extends MenagerieAnimal> t,Level l,Menagerie.Species s){super(t,l,s);}}
    static class Creature extends Beasts.Creature {
        Creature(EntityType<? extends MenagerieAnimal> t,Level l,Menagerie.Species s){super(t,l,s);}
        @Override public boolean fireImmune(){return Set.of("luxtructosaurus","tremorzilla").contains(species().source())||super.fireImmune();}
        @Override protected void special(ServerLevel l){
            var target=getTarget();if(target==null||!allowed(l,target)||!hasLineOfSight(target)||distanceToSqr(target)>144)return;
            String n=species().source();
            if(Set.of("teletor","magnetron","notor").contains(n)){
                var pull=position().subtract(target.position()).normalize().scale(.6);target.push(pull.x,.15,pull.z);target.hurtServer(l,damageSources().mobAttack(this),5);
                l.sendParticles(NaturalCollections.particle("cavern_magnet_lightning",ParticleTypes.ELECTRIC_SPARK),target.getX(),target.getY()+1,target.getZ(),12,.3,.4,.3,.03);
            }else if(Set.of("nucleeper","mine_guardian","gumbeeper","forsaken","tremorzilla","luxtructosaurus").contains(n)){
                double radius=n.equals("tremorzilla")||n.equals("luxtructosaurus")?7:4;
                for(var victim:l.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(radius),a->a!=this&&a.isAlive()).stream().limit(24).toList()){
                    if(victim!=target&&owner()==null)continue;
                    if(!allowed(l,victim)||!hasLineOfSight(victim))continue;
                    victim.hurtServer(l,damageSources().mobAttack(this),species().health()>=200?12:7);victim.knockback(.7,getX()-victim.getX(),getZ()-victim.getZ(),damageSources().mobAttack(this),7);
                    if(n.equals("nucleeper")&&!(victim instanceof Player p&&hazmat(p)))effect(victim,MobEffects.POISON,80);
                }
                l.sendParticles(ParticleTypes.EXPLOSION,getX(),getY()+1,getZ(),5,1,.5,1,0);
            }else if(Set.of("deep_one_mage","licowitch","underzealot","watcher").contains(n)){
                target.hurtServer(l,damageSources().mobAttack(this),6);effect(target,n.equals("watcher")?MobEffects.BLINDNESS:MobEffects.SLOWNESS,60);
                l.sendParticles(NaturalCollections.particle(n.equals("deep_one_mage")?"cavern_deep_one_magic":"cavern_purple_witch_magic",ParticleTypes.WITCH),target.getX(),target.getEyeY(),target.getZ(),12,.4,.4,.4,.01);
            }else super.special(l);
        }
        @Override public boolean doHurtTarget(ServerLevel l,Entity target){
            boolean hit=super.doHurtTarget(l,target);if(hit&&target instanceof LivingEntity victim){
                if(Set.of("gammaroach","brainiac").contains(species().source())&&!(victim instanceof Player p&&hazmat(p)))effect(victim,MobEffects.POISON,80);
                if(Set.of("caramel_cube","gum_worm").contains(species().source()))effect(victim,MobEffects.SLOWNESS,60);
            }return hit;
        }
        @Override public boolean carriesCargo(){return Set.of("gingerbread_man","vallumraptor").contains(species().source())||super.carriesCargo();}
        @Override protected void customServerAiStep(ServerLevel l){
            super.customServerAiStep(l);if(species().source().equals("caramel_cube")&&onGround()&&!sitting()&&tickCount%20==0)getJumpControl().jump();if(tickCount%40!=0||owner()==null)return;
            var keeper=Authority.present(l,owner());if(keeper==null||distanceToSqr(keeper)>144)return;
            if(species().source().equals("gingerbread_man")&&chested()&&!sitting()){
                for(var item:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,getBoundingBox().inflate(3),i->i.isAlive()&&!i.hasPickUpDelay()&&(i.getOwner()==null||i.getOwner().getUUID().equals(owner()))).stream().limit(4).toList())if(Menagerie.mayChange(l,keeper,item.blockPosition())){var left=cargo.addItem(item.getItem().copy());if(left.isEmpty())item.discard();else item.setItem(left);}
                for(var at:BlockPos.betweenClosed(blockPosition().offset(-3,-1,-3),blockPosition().offset(3,1,3)))if(l.hasChunkAt(at)&&l.getBlockState(at).is(Menagerie.BLOCKS.get("cavern_gingerbarrel"))&&Menagerie.mayChange(l,keeper,at)&&l.getBlockEntity(at) instanceof net.minecraft.world.Container chest){
                    var staged=new SimpleContainer(chest.getContainerSize());for(int i=0;i<chest.getContainerSize();i++)staged.setItem(i,chest.getItem(i).copy());
                    for(int i=0;i<cargo.getContainerSize();i++)if(!cargo.getItem(i).isEmpty()){var original=cargo.getItem(i);var left=staged.addItem(original.copy());if(left.getCount()==original.getCount())continue;for(int j=0;j<chest.getContainerSize();j++)chest.setItem(j,staged.getItem(j));chest.setChanged();cargo.setItem(i,left);break;}break;
                }
            }
            if(species().source().equals("raycat")){keeper.removeEffect(MobEffects.POISON);keeper.removeEffect(MobEffects.NAUSEA);}
            if(species().source().equals("notor"))for(var mob:l.getEntitiesOfClass(Mob.class,getBoundingBox().inflate(10),m->m instanceof Enemy).stream().limit(8).toList())if(TwoHanded.allowed(keeper,l,mob))effect(mob,MobEffects.GLOWING,60);
        }
        @Override public InteractionResult mobInteract(Player p,InteractionHand hand){
            var held=p.getItemInHand(hand);String n=species().source();
            if(n.startsWith("deep_one")&&held.is(Menagerie.ITEMS.get("cavern_pearl"))&&readyToHarvest()&&Menagerie.mayChange(p.level(),p,blockPosition())){
                if(p instanceof ServerPlayer sp){held.consume(1,p);RecoveredCreatureActions.play(this,"trade","trading");spawnAtLocation(sp.level(),new ItemStack(Menagerie.item("cavern_"+(random.nextBoolean()?"bioluminesscence":"sea_glass_shards")),2));harvested();setTarget(null);}return InteractionResult.SUCCESS;
            }return super.mobInteract(p,hand);
        }
    }
    static final class SatingSack extends MenagerieItems.Pouch {
        SatingSack(Properties p){super(p);}
        @Override public void inventoryTick(ItemStack stack,ServerLevel l,Entity holder,EquipmentSlot slot){
            super.inventoryTick(stack,l,holder,slot);if(!(holder instanceof ServerPlayer p)||holder.tickCount%100!=0||!p.canEat(false))return;
            var contents=stack.getOrDefault(DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.EMPTY);var items=new ArrayList<ItemStack>();contents.itemCopies().forEach(s->items.add(s.copy()));
            for(var food:items){var nutrition=food.get(DataComponents.FOOD);if(nutrition==null||nutrition.nutrition()<=0)continue;
                var remainder=food.getItem().getCraftingRemainder();if(remainder!=null||food.has(DataComponents.USE_REMAINDER))continue; // Container meals are eaten normally, preserving their vessel.
                p.getFoodData().eat(nutrition);food.shrink(1);stack.set(DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(items));break;
            }
        }
    }
    static final class Relic extends Item {
        final String name;
        Relic(String n,Properties p){super(p);name=n;}
        @Override public void hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){super.hurtEnemy(stack,target,attacker);if(attacker instanceof ServerPlayer p&&TwoHanded.allowed(p,p.level(),target)){if(name.equals("primitive_club"))effect(target,MobEffects.SLOWNESS,40);if(name.equals("desolate_dagger"))effect(target,MobEffects.WEAKNESS,60);if(name.equals("galena_gauntlet"))target.knockback(.8,p.getX()-target.getX(),p.getZ()-target.getZ(),p.damageSources().playerAttack(p),3);if(name.equals("ortholance")&&target.isInWater())target.hurtServer(p.level(),p.damageSources().playerAttack(p),3);}}
        @Override public ItemStack finishUsingItem(ItemStack stack,Level l,LivingEntity eater){if(!l.isClientSide()){switch(name){case "darkened_apple"->effect(eater,MobEffects.NIGHT_VISION,2400);case "serene_salad"->effect(eater,MobEffects.REGENERATION,120);case "seething_stew"->effect(eater,MobEffects.FIRE_RESISTANCE,1200);case "vesper_stew"->effect(eater,MobEffects.SLOW_FALLING,600);case "purple_soda_bottle"->effect(eater,MobEffects.SPEED,400);default->{}}}return super.finishUsingItem(stack,l,eater);}
        @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){
            if(Set.of("game_controller","totem_of_possession").contains(name)){
                if(!(target instanceof MenagerieAnimal animal)||!Menagerie.mayChange(player.level(),player,target.blockPosition()))return InteractionResult.FAIL;
                if(player instanceof ServerPlayer p){if(!animal.ownedBy(p)&&name.equals("totem_of_possession")&&animal.owner()==null&&animal.species().health()<=40&&animal.getHealth()<=animal.getMaxHealth()/2){
                    int gem=-1;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(Menagerie.ITEMS.get("cavern_occult_gem"))){gem=i;break;}if(gem>=0){p.getInventory().getItem(gem).consume(1,p);animal.tame(p.getUUID());}
                }
                if(!animal.ownedBy(p)){p.sendOverlayMessage(Component.literal("Tame this creature first; other players' companions cannot be taken."));return InteractionResult.FAIL;}var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.putString("ControlledMob",animal.getUUID().toString());stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));stack.set(DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(List.of(Component.literal("Linked to "+animal.getName().getString()))));}return InteractionResult.SUCCESS;
            }
            if(!name.equals("holocoder"))return super.interactLivingEntity(stack,player,target,hand);
            if(!Menagerie.mayChange(player.level(),player,target.blockPosition()))return InteractionResult.FAIL;
            if(player instanceof ServerPlayer){var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                tag.putString("DisplayType",target instanceof Player?"minecraft:mannequin":net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());
                if(target instanceof MenagerieAnimal animal){tag.putInt("DisplayVariant",animal.variant());tag.putBoolean("DisplayBaby",animal.isBaby());}else{tag.remove("DisplayVariant");tag.remove("DisplayBaby");}
                if(target instanceof Player avatar)stack.set(DataComponents.PROFILE,net.minecraft.world.item.component.ResolvableProfile.createResolved(avatar.getGameProfile()));else stack.remove(DataComponents.PROFILE);
                stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));stack.set(DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(List.of(Component.literal("Projection: "+target.getName().getString()))));
            }return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(name.equals("remote_detonator")&&p.level().getBlockEntity(c.getClickedPos()) instanceof CavernControl control&&control.name().equals("nuclear_bomb")){
                if(!control.permitted(p))return InteractionResult.FAIL;var tag=c.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putLong("ChargePos",c.getClickedPos().asLong());tag.putString("ChargeDimension",p.level().dimension().identifier().toString());c.getItemInHand().set(DataComponents.CUSTOM_DATA,CustomData.of(tag));c.getItemInHand().set(DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(List.of(Component.literal("Charge: "+c.getClickedPos().toShortString()))));return InteractionResult.SUCCESS;
            }
            if(name.equals("biome_treat")&&p.isShiftKeyDown()){
                if(!Menagerie.mayChange(p.level(),p,c.getClickedPos()))return InteractionResult.FAIL;
                var biome=p.level().getBiome(c.getClickedPos()).unwrapKey();if(biome.isEmpty())return InteractionResult.FAIL;
                var tag=c.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putString("BoundBiome",biome.get().identifier().toString());c.getItemInHand().set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
                c.getItemInHand().set(DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(List.of(Component.literal("Bound to "+biome.get().identifier()))));return InteractionResult.SUCCESS;
            }
            if(name.equals("fertilizer")){if(!Menagerie.mayChange(p.level(),p,c.getClickedPos()))return InteractionResult.FAIL;return BoneMealItem.growCrop(c.getItemInHand(),p.level(),c.getClickedPos())?InteractionResult.SUCCESS:InteractionResult.PASS;}
            String summon=name.equals("ominous_catalyst")?"luxtructosaurus":name.equals("immortal_embryo")?"forsaken":name.equals("magic_conch")?"hullbreaker":"";
            if(!summon.isEmpty())return summon(p,c,summon);
            return super.useOn(c);
        }
        @Override public InteractionResult use(Level l,Player who,InteractionHand hand){
            if(!(who instanceof ServerPlayer p))return InteractionResult.SUCCESS;var held=p.getItemInHand(hand);if(p.getCooldowns().isOnCooldown(held))return InteractionResult.FAIL;
            if(name.equals("cave_map")||name.equals("underground_cabin_explorer_map")){CaveWorkshop.open(p,null);return InteractionResult.SUCCESS;}
            if(name.equals("remote_detonator")){
                var tag=held.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(!p.level().dimension().identifier().toString().equals(tag.getStringOr("ChargeDimension",""))||!tag.contains("ChargePos"))return InteractionResult.FAIL;var at=BlockPos.of(tag.getLongOr("ChargePos",0));if(p.blockPosition().distSqr(at)>256*256||!p.level().hasChunkAt(at))return InteractionResult.FAIL;
                if(p.level().getBlockEntity(at) instanceof CavernControl control)return (p.isShiftKeyDown()?control.disarm(p):control.arm(p))?InteractionResult.SUCCESS:InteractionResult.FAIL;return InteractionResult.FAIL;
            }
            if(Set.of("game_controller","totem_of_possession").contains(name)){
                var data=held.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();UUID id;try{id=UUID.fromString(data.getStringOr("ControlledMob",""));}catch(IllegalArgumentException e){return InteractionResult.FAIL;}
                if(!(p.level().getEntity(id) instanceof MenagerieAnimal animal)||!animal.ownedBy(p)||p.distanceToSqr(animal)>64*64||!Menagerie.mayChange(l,p,animal.blockPosition()))return InteractionResult.FAIL;
                if(p.isShiftKeyDown()){animal.sit(!animal.sitting());return InteractionResult.SUCCESS;}
                var target=aim(p,32);if(target!=null&&target!=animal&&TwoHanded.allowed(p,p.level(),target)){animal.sit(false);animal.setTarget(target);}else{var hit=p.pick(32,0,false);var at=BlockPos.containing(hit.getLocation());if(!l.hasChunkAt(at)||!Menagerie.mayChange(l,p,at))return InteractionResult.FAIL;animal.sit(false);animal.getNavigation().moveTo(at.getX()+.5,at.getY()+1,at.getZ()+.5,1.2);}p.getCooldowns().addCooldown(held,10);return InteractionResult.SUCCESS;
            }
            if(Set.of("limestone_spear","extinction_spear","frostmint_spear").contains(name)){
                if(!CavernProjectile.launch(p,held,name,true))return InteractionResult.FAIL;held.consume(1,p);p.getCooldowns().addCooldown(held,20);return InteractionResult.SUCCESS;
            }
            String ammo=switch(name){case "raygun"->"uranium_shard";case "shot_gum"->"gumball_pile";case "sea_staff"->"pearl";case "sugar_staff"->"radiant_essence";case "ink_bomb","glow_ink_bomb","depth_charge","purple_soda_bottle_rocket"->name;default->"";};
            if(ammo.isEmpty())return super.use(l,who,hand);
            int slot=-1;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(Menagerie.item("cavern_"+ammo))){slot=i;break;}boolean heldAmmo=ammo.equals(name);if(slot<0&&!p.isCreative()&&!heldAmmo)return InteractionResult.FAIL;
            if(name.equals("raygun")){var target=aim(p,24);if(target==null||!TwoHanded.allowed(p,p.level(),target))return InteractionResult.FAIL;target.hurtServer(p.level(),p.damageSources().playerAttack(p),10);var from=p.getEyePosition();var delta=target.getEyePosition().subtract(from);for(int i=0;i<24;i++){var point=from.add(delta.scale(i/24.0));p.level().sendParticles(ParticleTypes.ELECTRIC_SPARK,point.x,point.y,point.z,1,0,0,0,0);}}
            else if(!CavernProjectile.launch(p,new ItemStack(Menagerie.item("cavern_"+ammo)),name,false))return InteractionResult.FAIL;
            if(!p.isCreative()){if(heldAmmo)held.shrink(1);else p.getInventory().getItem(slot).shrink(1);}
            p.getCooldowns().addCooldown(held,name.equals("raygun")?25:40);return InteractionResult.SUCCESS;
        }
    }
    static LivingEntity aim(ServerPlayer p,double range){
        var from=p.getEyePosition();var end=from.add(p.getLookAngle().scale(range));var block=p.level().clip(new net.minecraft.world.level.ClipContext(from,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));end=block.getLocation();
        LivingEntity found=null;double nearest=from.distanceToSqr(end);
        for(var mob:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(from,end).inflate(1),m->m!=p&&m.isAlive()).stream().limit(64).toList()){
            var hit=mob.getBoundingBox().inflate(.15).clip(from,end);if(hit.isPresent()&&from.distanceToSqr(hit.get())<nearest){nearest=from.distanceToSqr(hit.get());found=mob;}
        }return found;
    }
    static InteractionResult summon(ServerPlayer p,UseOnContext c,String species){
        var at=c.getClickedPos().relative(c.getClickedFace());if(!Menagerie.mayChange(p.level(),p,at)||p.level().getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL)return InteractionResult.FAIL;
        if(!p.level().getEntitiesOfClass(Creature.class,new AABB(at).inflate(96),m->m.species().source().equals(species)).isEmpty())return InteractionResult.FAIL;
        var mob=Menagerie.TYPES.get("cavern_"+species).create(p.level(),EntitySpawnReason.MOB_SUMMONED);if(mob==null)return InteractionResult.FAIL;
        if(mob.species().aquatic()&&!p.level().getFluidState(at).is(net.minecraft.world.level.material.Fluids.WATER))return InteractionResult.FAIL;
        mob.snapTo(at.getX()+.5,at.getY()+.1,at.getZ()+.5,0,0);if(!p.level().noCollision(mob)||!p.level().addFreshEntity(mob))return InteractionResult.FAIL;
        mob.setPersistenceRequired();mob.setTarget(p);c.getItemInHand().consume(1,p);return InteractionResult.SUCCESS;
    }
}
