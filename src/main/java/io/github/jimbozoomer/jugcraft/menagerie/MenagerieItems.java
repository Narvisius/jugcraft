package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.assay.AlchemyStorageMenu;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

public final class MenagerieItems {
    private MenagerieItems(){}
    public static Runnable openGuide=()->{};
    private static final Set<String> CAPTURED=Set.of("ant","butterfly","caterpillar","dragonfly","firefly","hedgehog","rat","scorpion","snail","crab");
    private static final Set<String> FOOD=Set.of("anglerfish","bass","blobfish","bushmeat","catfish","clam_meat","crab_meat","drumstick","duck","fish_fillet","lizard_tail","mammoth_meat","piranha","venison","morsel","fat");
    public static Item create(String id,Item.Properties p){
        String source=id.replaceFirst("wildlife_","");
        if(source.equals("knapsack"))return new Pouch(p);
        if(source.equals("whistle"))return new Recall(p.stacksTo(1));
        if(source.equals("capture_net"))return new Item(p.stacksTo(1));
        if(source.equals("queen_ant"))return new Spawn("wildlife_ant",p.stacksTo(1));
        if(source.startsWith("music_disc_"))return new Item(p.stacksTo(1).jukeboxPlayable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.JUKEBOX_SONG,io.github.jimbozoomer.jugcraft.Jugcraft.id("wildlife_"+source.substring(11)))));
        if(source.endsWith("_bucket")||source.endsWith("_starfish")||CAPTURED.contains(source)||MenagerieEcology.CRITTERS.containsKey(source))return new Captured(source,p.stacksTo(1));
        if(source.startsWith("cooked_")||FOOD.contains(source))p.food(new FoodProperties.Builder().nutrition(source.startsWith("cooked_")?6:2).saturationModifier(source.startsWith("cooked_")?.6F:.2F).build());
        return new Item(p);
    }
    public static class Pouch extends Item {
        public Pouch(Properties p){super(p.stacksTo(1).component(DataComponents.CONTAINER,ItemContainerContents.EMPTY));}
        @Override public void inventoryTick(ItemStack stack,net.minecraft.server.level.ServerLevel level,Entity holder,EquipmentSlot slot){
            if(holder.tickCount%20!=0)return;
            boolean filled=stack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).nonEmptyItems().iterator().hasNext();
            var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            if(tag.getBooleanOr("BagFilled",false)!=filled){tag.putBoolean("BagFilled",filled);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
        }
        @Override public InteractionResult use(Level l,Player who,InteractionHand hand){
            if(who instanceof ServerPlayer p){var held=p.getItemInHand(hand);p.openMenu(new SimpleMenuProvider((id,inv,player)->new AlchemyStorageMenu(id,inv,hand,held),held.getHoverName()));}return InteractionResult.SUCCESS;
        }
    }
    public static final class Spawn extends Item {
        private final String species;
        private final int forcedVariant;private final boolean baby;
        public Spawn(String species,Properties p){this(species,p,-1,false);}
        public Spawn(String species,Properties p,int variant,boolean baby){super(p);this.species=species;this.forcedVariant=variant;this.baby=baby;}
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            BlockPos at=c.getClickedPos().relative(c.getClickedFace());
            if(!Authority.mayChange(p.level(),p,at))return InteractionResult.FAIL;
            var animal=Menagerie.TYPES.get(species).create(p.level(),EntitySpawnReason.SPAWN_ITEM_USE);if(animal==null)return InteractionResult.FAIL;
            animal.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),0);
            animal.finalizeSpawn(p.level(),p.level().getCurrentDifficultyAt(at),EntitySpawnReason.SPAWN_ITEM_USE,null);
            if(forcedVariant>=0)animal.variant(forcedVariant);if(baby)animal.setBaby(true);
            if(!p.level().noCollision(animal)||!p.level().addFreshEntity(animal))return InteractionResult.FAIL;
            c.getItemInHand().consume(1,p);return InteractionResult.SUCCESS;
        }
    }
    public static boolean capture(MenagerieAnimal a,Player player,InteractionHand hand){
        var held=player.getItemInHand(hand);String source=a.species().source();String item="";
        String bucket=source;
        if(a.species().id().startsWith("wildlife_")&&held.is(Items.WATER_BUCKET)&&(!source.equals("duck")||a.isBaby())&&Menagerie.ITEMS.containsKey("wildlife_"+bucket+"_bucket"))item="wildlife_"+bucket+"_bucket";
        String portable=source.endsWith("scorpion")?"scorpion":source;
        if(a.species().id().startsWith("wildlife_")&&held.is(Menagerie.ITEMS.get("wildlife_capture_net"))&&a.species().small()&&CAPTURED.contains(portable))item="wildlife_"+portable;
        if(a.species().id().equals("wildlife_starfish")&&held.is(Menagerie.ITEMS.get("wildlife_capture_net")))item="wildlife_"+a.species().variants().get(a.variant()).name()+"_starfish";
        if(a.species().id().startsWith("cavern_")&&held.is(Items.WATER_BUCKET)){
            String name=source.equals("sweetish_fish")?a.species().variants().get(a.variant()).name():source;
            if(Menagerie.ITEMS.containsKey("cavern_"+name+"_bucket"))item="cavern_"+name+"_bucket";
        }
        if(a.species().id().startsWith("beast_")&&held.is(a.species().habitat().equals("nether")?Items.LAVA_BUCKET:Items.WATER_BUCKET)){
            String name=source.equals("catfish")?"small_catfish":source;
            if(Menagerie.ITEMS.containsKey("beast_"+name+"_bucket"))item="beast_"+name+"_bucket";
        }
        if(a.species().id().startsWith("companion_")&&held.is(Items.WATER_BUCKET)&&Menagerie.ITEMS.containsKey("companion_"+source+"_bucket"))item="companion_"+source+"_bucket";
        if(a.species().id().startsWith("grove_")||a.species().id().startsWith("companion_")){
            if(held.is(Items.LAVA_BUCKET)&&source.equals("cindershell")&&a.isBaby())item="grove_cindershell_bucket";
            if(held.is(Menagerie.ITEMS.get("grove_entity_net"))&&a.species().width()<=.8F&&!held.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().contains("Species"))item=source.equals("lizard")?"grove_lizard_item_"+a.species().variants().get(a.variant()).name().replaceFirst("lizard_",""):"grove_entity_net";
        }
        if(a.species().id().startsWith("fauna_")&&Set.of("butterfly","dragonfly","pixie").contains(source)&&held.is(Menagerie.ITEMS.get("fauna_bug_net"))){
            boolean habitat=false;for(int i=0;i<36;i++)if(emptyHabitat(player.getInventory().getItem(i))){habitat=true;break;}
            if(!habitat){if(!player.level().isClientSide())player.sendOverlayMessage(Component.literal("Bring an empty bug habitat to carry this creature."));return true;}
            item="fauna_bug_habitat_"+a.species().variants().get(a.variant()).name()+"_"+source;
        }
        if(item.isEmpty()||!Menagerie.ITEMS.containsKey(item)||a.isVehicle()||a.isPassenger()||a.owner()!=null&&!a.ownedBy(player))return false;
        if(!(player instanceof ServerPlayer p))return true;
        if(!Authority.mayChange(p.level(),p,a.blockPosition()))return false;
        boolean replace=held.is(Items.WATER_BUCKET)||held.is(Items.LAVA_BUCKET)||item.equals("grove_entity_net");
        boolean habitatFreesSlot=false;if(item.startsWith("fauna_bug_habitat_"))for(int i=0;i<36;i++){var habitat=p.getInventory().getItem(i);if(emptyHabitat(habitat)){habitatFreesSlot=habitat.getCount()==1;break;}}
        if(p.getInventory().getFreeSlot()<0&&!replace&&!habitatFreesSlot){p.sendSystemMessage(Component.literal("Make room for the captured animal."),true);return true;}
        if(item.startsWith("fauna_bug_habitat_"))for(int i=0;i<36;i++)if(emptyHabitat(p.getInventory().getItem(i))){p.getInventory().getItem(i).shrink(1);break;}
        ItemStack caught=new ItemStack(Menagerie.item(item));CompoundTag tag=new CompoundTag();
        tag.putString("Species",a.species().id());tag.putInt("Variant",a.variant());tag.putInt("Age",a.getAge());tag.putFloat("Health",a.getHealth());
        if(a.owner()!=null)tag.putString("Keeper",a.owner().toString());
        var saved=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,p.level().registryAccess());a.saveWithoutId(saved);tag.put("StoredAnimal",saved.buildResult());
        caught.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));if(a.hasCustomName())caught.set(DataComponents.CUSTOM_NAME,a.getCustomName());
        if(replace)p.setItemInHand(hand,caught);else p.getInventory().add(caught);
        a.discard();return true;
    }
    private static boolean emptyHabitat(ItemStack stack){
        return stack.is(Menagerie.ITEMS.get("fauna_bug_habitat"))||stack.is(Menagerie.ITEMS.get("fauna_bug_habitat_empty"));
    }
    /** Keeps decorative block placement, but releases a stored living animal. */
    public static final class CapturedDecoration extends BlockItem {
        public CapturedDecoration(net.minecraft.world.level.block.Block block,Properties p){super(block,p);}
        @Override public InteractionResult useOn(UseOnContext c){
            if(c.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().contains("Species"))
                return Captured.release(c,net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).getPath().replaceFirst("wildlife_",""));
            return super.useOn(c);
        }
    }
    public static final class Captured extends Item {
        private final String source;
        Captured(String source,Properties p){super(p);this.source=source;}
        @Override public InteractionResult useOn(UseOnContext c){
            return release(c,source);
        }
        static InteractionResult release(UseOnContext c,String source){
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            var data=c.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();String species=data.getStringOr("Species","");
            String vanilla=data.getStringOr("VanillaCritter","");
            if(MenagerieEcology.CRITTERS.containsKey(vanilla)){
                var at=c.getClickedPos().relative(c.getClickedFace());if(!Authority.mayChange(p.level(),p,at))return InteractionResult.FAIL;
                var entity=MenagerieEcology.CRITTERS.get(vanilla).create(p.level(),EntitySpawnReason.BUCKET);if(entity==null)return InteractionResult.FAIL;
                entity.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);entity.setCustomName(c.getItemInHand().get(DataComponents.CUSTOM_NAME));
                if(entity instanceof AgeableMob a)a.setAge(data.getIntOr("Age",0));
                if(entity instanceof LivingEntity a)a.setHealth(Math.clamp(data.getFloatOr("Health",a.getMaxHealth()),1,a.getMaxHealth()));
                if(entity instanceof Mob m)m.setPersistenceRequired();
                if(!p.level().noCollision(entity)||!p.level().addFreshEntity(entity))return InteractionResult.FAIL;
                c.getItemInHand().shrink(1);return InteractionResult.SUCCESS;
            }
            if(species.isEmpty()&&source.endsWith("_bucket")){
                String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(c.getItemInHand().getItem()).getPath();String prefix=id.substring(0,id.indexOf('_')+1);String animal=source.substring(0,source.length()-7);
                if(animal.endsWith("catfish"))animal="catfish";if(animal.startsWith("sweetish_fish_"))animal="sweetish_fish";
                String candidate=prefix+animal;if(Menagerie.TYPES.containsKey(candidate)){species=candidate;var profile=Menagerie.SPECIES.get(candidate);for(int i=0;i<profile.variants().size();i++)if(source.startsWith(profile.variants().get(i).name())){data.putInt("Variant",i);break;}}
            }
            if(!Menagerie.TYPES.containsKey(species))return InteractionResult.PASS;
            var at=c.getClickedPos().relative(c.getClickedFace());var spec=Menagerie.SPECIES.get(species);
            if(!Authority.mayChange(p.level(),p,at)||spec.aquatic()&&!LavaSwimming.fluid(spec,p.level().getFluidState(at)))return InteractionResult.FAIL;
            var a=Menagerie.TYPES.get(species).create(p.level(),EntitySpawnReason.BUCKET);if(a==null)return InteractionResult.FAIL;
            data.getCompound("StoredAnimal").ifPresent(saved->a.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,p.level().registryAccess(),saved)));
            a.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),0);a.variant(data.getIntOr("Variant",0));a.setAge(data.getIntOr("Age",0));a.setHealth(Math.clamp(data.getFloatOr("Health",a.getMaxHealth()),1,a.getMaxHealth()));
            try{String owner=data.getStringOr("Keeper","");if(!owner.isEmpty())a.tame(UUID.fromString(owner));}catch(IllegalArgumentException ignored){}
            a.setCustomName(c.getItemInHand().get(DataComponents.CUSTOM_NAME));a.setPersistenceRequired();
            if(!p.level().noCollision(a)||!p.level().addFreshEntity(a))return InteractionResult.FAIL;
            // A captured creature is a unique stored animal, including in creative mode.
            c.getItemInHand().shrink(1);if(source.endsWith("_bucket"))p.setItemInHand(c.getHand(),new ItemStack(Items.BUCKET));else if(source.equals("entity_net"))p.setItemInHand(c.getHand(),new ItemStack(Menagerie.item("grove_entity_net")));else if(source.startsWith("bug_habitat_"))p.setItemInHand(c.getHand(),new ItemStack(Menagerie.item("fauna_bug_habitat")));return InteractionResult.SUCCESS;
        }
    }
    public static final class Recall extends Item {
        public Recall(Properties p){super(p);}
        @Override public InteractionResult use(Level l,Player player,InteractionHand hand){
            if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            int found=0;
            for(var a:p.level().getEntitiesOfClass(MenagerieAnimal.class,p.getBoundingBox().inflate(48),a->a.ownedBy(p)&&!a.isVehicle()).stream().limit(16).toList()){
                if(!Authority.mayChange(p.level(),p,a.blockPosition()))continue;a.sit(false);a.getNavigation().moveTo(p,1.2);found++;
            }
            p.sendSystemMessage(Component.literal("Called "+found+" nearby companions. Crouch and interact with one to make it stay."),true);
            p.getCooldowns().addCooldown(p.getItemInHand(hand),40);return InteractionResult.SUCCESS;
        }
    }
    public static final class Guide extends Item {
        public Guide(Properties p){super(p);}
        @Override public InteractionResult use(Level l,Player who,InteractionHand hand){
            if(l.isClientSide())openGuide.run();
            return InteractionResult.SUCCESS;
        }
    }
}
