package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.assay.AlchemyArtifacts;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Native pocket companions: paid use, bounded hotbar effects and shared inventory/protection APIs. */
public final class PocketPets {
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    private static final Map<String,net.minecraft.core.Holder<MobEffect>> BUFFS=Map.ofEntries(
        Map.entry("chicken",MobEffects.SPEED),Map.entry("cheetah",MobEffects.HASTE),Map.entry("ocelot",MobEffects.NIGHT_VISION),
        Map.entry("squid",MobEffects.WATER_BREATHING),Map.entry("magma_cube",MobEffects.FIRE_RESISTANCE),Map.entry("blaze",MobEffects.STRENGTH),
        Map.entry("iron_golem",MobEffects.RESISTANCE),Map.entry("mickerson",MobEffects.RESISTANCE),Map.entry("shield",MobEffects.RESISTANCE),
        Map.entry("spider",MobEffects.JUMP_BOOST),Map.entry("slime",MobEffects.JUMP_BOOST),Map.entry("snow_golem",MobEffects.FIRE_RESISTANCE),
        Map.entry("loot",MobEffects.LUCK),Map.entry("pixie",MobEffects.LUCK),Map.entry("creeper",MobEffects.RESISTANCE),Map.entry("sun",MobEffects.NIGHT_VISION));
    private PocketPets(){}
    public static void register(){
        JsonObject root;try(var in=PocketPets.class.getResourceAsStream("/jugcraft/menagerie/pockets.json")){
            if(in==null)throw new IllegalStateException("Missing pocket catalog");root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(IOException e){throw new IllegalStateException(e);}
        root.entrySet().forEach(e->{var v=e.getValue().getAsJsonObject();String ability=v.get("ability").getAsString();boolean pet=v.get("pet").getAsBoolean();
            Item item=Menagerie.item(e.getKey(),p->{
                if(Set.of("chest","double_chest","feed_bag").contains(ability))return new MenagerieItems.Pouch(p);
                if(pet||Set.of("item_petrifier","holiday_gift","siamese_gift","easter_egg").contains(ability))return new Pet(ability,p.stacksTo(1));
                if(Set.of("banana","rock_candy","candy_cane","egg_nog","holiday_cookie").contains(ability))p.food(new FoodProperties.Builder().nutrition(6).saturationModifier(.6F).build());
                if(ability.equals("solstice_sword"))p.sword(ToolMaterial.DIAMOND,3,-2.4F);
                else if(ability.startsWith("solstice_")){
                    var type=switch(ability){case "solstice_helmet"->net.minecraft.world.item.equipment.ArmorType.HELMET;case "solstice_chestplate"->net.minecraft.world.item.equipment.ArmorType.CHESTPLATE;case "solstice_leggings"->net.minecraft.world.item.equipment.ArmorType.LEGGINGS;default->net.minecraft.world.item.equipment.ArmorType.BOOTS;};
                    var base=net.minecraft.world.item.equipment.ArmorMaterials.IRON;
                    var material=new net.minecraft.world.item.equipment.ArmorMaterial(base.durability(),base.defense(),base.enchantmentValue(),base.equipSound(),base.toughness(),base.knockbackResistance(),base.repairIngredient(),ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,io.github.jimbozoomer.jugcraft.Jugcraft.id("menagerie/solstice")));
                    p.humanoidArmor(material,type);
                }
                return new Item(p);
            });ITEMS.put(ability,item);
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%20!=0)return;for(var p:server.getPlayerList().getPlayers())tick(p);});
    }
    public static Item food(String kind){return switch(kind){
        case "cow","sheep","pig","chicken","ocelot","cheetah","wolf","siamese","lead","saddle","flying_saddle"->Items.WHEAT;
        case "ghast","blaze","magma_cube","creeper","furnace","torch","sun"->Items.COAL;
        case "wither","skull","juggernaut","illuminati","qcm","cloud","black_hole","moon"->Items.QUARTZ;
        case "enderman","nether_portal","end_portal","house"->Items.ENDER_PEARL;
        default->Items.GOLD_NUGGET;
    };}
    private static ItemStack ration(ServerPlayer p,String kind){
        for(int i=0;i<36;i++){var stack=p.getInventory().getItem(i);if(stack.is(food(kind)))return stack;}return ItemStack.EMPTY;
    }
    private static boolean pay(ServerPlayer p,String kind){if(p.isCreative())return true;var ration=ration(p,kind);if(ration.isEmpty())return false;ration.shrink(1);p.getInventory().setChanged();return true;}
    private static boolean hasFood(ServerPlayer p,String kind){return p.isCreative()||!ration(p,kind).isEmpty();}
    private static CompoundTag data(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    private static void tick(ServerPlayer p){
        if(!p.isAlive()||p.isSpectator())return;Set<String> handled=new HashSet<>();
        for(int slot=0;slot<9;slot++){
            ItemStack stack=p.getInventory().getItem(slot);if(!(stack.getItem() instanceof Pet pet)||!handled.add(pet.kind))continue;
            if(!BUFFS.containsKey(pet.kind)&&!Set.of("cloud","flying_saddle","anvil","black_hole","cow").contains(pet.kind))continue;
            var tag=data(stack);long now=p.level().getGameTime(),until=tag.getLongOr("FedUntil",0);
            if(until<=now||until>now+1200){if(!pay(p,pet.kind))continue;tag.putLong("FedUntil",now+1200);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
            var buff=BUFFS.get(pet.kind);if(buff!=null)p.addEffect(new MobEffectInstance(buff,60,0,true,false,true));
            switch(pet.kind){
                case "cloud","flying_saddle"->AlchemyArtifacts.grantRitualFlight(p,40);
                case "cow"->p.getActiveEffects().stream().filter(effect->!effect.getEffect().value().isBeneficial()).map(MobEffectInstance::getEffect).toList().forEach(p::removeEffect);
                case "anvil"->{for(int i=0;i<36;i++){var tool=p.getInventory().getItem(i);if(tool.isDamaged()){tool.setDamageValue(Math.max(0,tool.getDamageValue()-1));break;}}}
                case "black_hole"->gather(p);
                default->{}
            }
        }
    }
    private static void gather(ServerPlayer p){
        var drops=new ArrayList<ItemEntity>();p.level().getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(ItemEntity.class),p.getBoundingBox().inflate(5),e->e.isAlive()&&!e.hasPickUpDelay(),drops,16);
        for(var e:drops){var reserved=((io.github.jimbozoomer.jugcraft.mixin.AlchemicalItemAccess)e).jugcraft$pickupTarget();if(reserved!=null&&!reserved.equals(p.getUUID())||!Authority.mayChange(p.level(),p,e.blockPosition())||e.getOwner()!=null&&!e.getOwner().getUUID().equals(p.getUUID())||!p.hasLineOfSight(e))continue;e.setDeltaMovement(p.position().add(0,.5,0).subtract(e.position()).normalize().scale(.3));e.syncVelocity=true;}
    }
    private static void give(ServerPlayer p,ItemStack stack){if(!p.getInventory().add(stack))p.spawnAtLocation(p.level(),stack);}
    private static boolean teleport(ServerPlayer p,ServerLevel destination,BlockPos at){
        if(!destination.hasChunkAt(at)||!Authority.mayChange(p.level(),p,p.blockPosition())||!Authority.mayChange(destination,p,at)||!destination.getBlockState(at.below()).isSolidRender())return false;
        var box=p.getBoundingBox().move(at.getX()+.5-p.getX(),at.getY()-p.getY(),at.getZ()+.5-p.getZ());if(!destination.noCollision(p,box))return false;
        return p.teleportTo(destination,at.getX()+.5,at.getY(),at.getZ()+.5,Set.of(),p.getYRot(),p.getXRot(),true);
    }
    private static boolean home(ServerPlayer p,ItemStack stack){
        var tag=data(stack);if(p.isShiftKeyDown()){
            if(!Authority.mayChange(p.level(),p,p.blockPosition()))return false;
            tag.putString("HomeDimension",p.level().dimension().identifier().toString());tag.putLong("Home",p.blockPosition().asLong());stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));p.sendSystemMessage(Component.literal("Home recorded. Return works while that location is loaded."),true);return true;
        }
        String key=tag.getStringOr("HomeDimension","");if(key.isEmpty())return false;
        var l=p.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(key)));return l!=null&&teleport(p,l,BlockPos.of(tag.getLongOr("Home",0)));
    }
    private static boolean strike(ServerPlayer p,String kind){
        boolean area=Set.of("creeper","dubstep","pufferfish","moon","juggernaut","black_hole","sun").contains(kind);double range=area?5:24;
        var targets=new ArrayList<LivingEntity>();p.level().getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),p.getBoundingBox().inflate(range),e->e!=p&&e.isAlive(),targets,48);
        int hits=0;
        for(var e:targets){if(hits>=8||!Authority.mayStrike(p,e)||!p.hasLineOfSight(e)||e instanceof MenagerieAnimal m&&m.owner()!=null)continue;
            if(!area&&p.getLookAngle().dot(e.getEyePosition().subtract(p.getEyePosition()).normalize())<.98)continue;
            if(kind.equals("moon")){e.addEffect(new MobEffectInstance(MobEffects.LEVITATION,60),p);}
            else if(kind.equals("item_petrifier")){e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,200,5),p);}
            else{e.hurtServer(p.level(),p.damageSources().playerAttack(p),area?4:6);if(Set.of("ghast","blaze","sun").contains(kind))e.igniteForSeconds(3);if(kind.equals("wither"))e.addEffect(new MobEffectInstance(MobEffects.WITHER,80),p);if(kind.equals("pufferfish"))e.addEffect(new MobEffectInstance(MobEffects.POISON,80),p);}
            hits++;if(!area)break;
        }
        if(hits>0)p.level().sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,p.getX(),p.getEyeY(),p.getZ(),12,.5,.5,.5,.05);return hits>0;
    }
    private static boolean workstation(ServerPlayer p,InteractionHand hand,String kind){
        ItemStack held=p.getItemInHand(hand);ContainerLevelAccess access=ContainerLevelAccess.create(p.level(),p.blockPosition());
        p.openMenu(new SimpleMenuProvider((id,inv,who)->switch(kind){
            case "crafting_table"->new CraftingMenu(id,inv,access){@Override public boolean stillValid(Player who){return p.getItemInHand(hand)==held;}};
            case "enchanting_table"->new EnchantmentMenu(id,inv,access){@Override public boolean stillValid(Player who){return p.getItemInHand(hand)==held;}};
            case "anvil"->new AnvilMenu(id,inv,access){@Override public boolean stillValid(Player who){return p.getItemInHand(hand)==held;}};
            default->new ChestMenu(MenuType.GENERIC_9x3,id,inv,p.getEnderChestInventory(),3){@Override public boolean stillValid(Player who){return p.getItemInHand(hand)==held;}};
        },held.getHoverName()));return true;
    }
    private static boolean smelt(ServerPlayer p){
        for(int i=0;i<9;i++){
            var stack=p.getInventory().getItem(i);if(stack.isEmpty())continue;
            var input=new SingleRecipeInput(stack);var recipe=p.level().recipeAccess().getRecipeFor(RecipeType.SMELTING,input,p.level());
            if(recipe.isPresent()){var result=recipe.get().value().assemble(input);if(result.isEmpty())continue;stack.shrink(1);give(p,result);return true;}
        }return false;
    }
    private static boolean heal(ServerPlayer p){
        boolean changed=p.getHealth()<p.getMaxHealth();p.heal(6);
        for(var a:p.level().getEntitiesOfClass(MenagerieAnimal.class,p.getBoundingBox().inflate(8),a->a.ownedBy(p)).stream().limit(8).toList()){changed|=a.getHealth()<a.getMaxHealth();a.heal(6);}return changed;
    }
    private static boolean activate(ServerPlayer p,InteractionHand hand,String kind){
        var stack=p.getItemInHand(hand);return switch(kind){
            case "crafting_table","enchanting_table","anvil","ender_chest"->workstation(p,hand,kind);
            case "furnace"->smelt(p);
            case "house","nether_portal","end_portal"->home(p,stack);
            case "enderman","silverfish"->{boolean success=false;for(int i=8;i>=2&&!success;i--)success=teleport(p,p.level(),BlockPos.containing(p.position().add(p.getLookAngle().scale(i))));yield success;}
            case "ghast","wither","creeper","dubstep","pufferfish","moon","apple","banana","item_petrifier","qcm","sun"->strike(p,kind);
            case "heart","purplicious_cow"->heal(p);
            case "juggernaut"->{p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,100,2),p);yield true;}
            case "illuminati"->{p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,200),p);yield true;}
            case "cow"->{boolean changed=!p.getActiveEffects().isEmpty();p.removeAllEffects();yield changed;}
            case "sheep"->{give(p,new ItemStack(Items.WOOL.white()));yield true;}
            case "chicken"->{give(p,new ItemStack(Items.EGG));yield true;}
            case "pig"->{give(p,new ItemStack(Items.CARROT));yield true;}
            case "iron_golem"->{give(p,new ItemStack(Items.POPPY));yield true;}
            case "snow_golem"->{give(p,new ItemStack(Items.SNOWBALL,4));yield true;}
            case "slime"->{give(p,new ItemStack(Items.SLIME_BALL));yield true;}
            case "spider"->{give(p,new ItemStack(Items.STRING));yield true;}
            case "quiver"->{give(p,new ItemStack(Items.ARROW,4));yield true;}
            case "mooshroom"->{give(p,new ItemStack(Items.MUSHROOM_STEW));yield true;}
            case "brewing_stand"->{p.addEffect(new MobEffectInstance(List.of(MobEffects.SPEED,MobEffects.HASTE,MobEffects.WATER_BREATHING).get(p.getRandom().nextInt(3)),1200),p);yield true;}
            case "wolf"->{var a=Menagerie.TYPES.get("mount_direwolf").create(p.level(),EntitySpawnReason.MOB_SUMMONED);if(a==null||p.level().getEntitiesOfClass(MenagerieAnimal.class,p.getBoundingBox().inflate(32),m->m.ownedBy(p)).size()>=4)yield false;var at=p.blockPosition().relative(p.getDirection(),2);if(!Authority.mayChange(p.level(),p,at))yield false;a.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);a.tame(p.getUUID());yield p.level().noCollision(a)&&p.level().addFreshEntity(a);}
            case "siamese"->{var at=p.blockPosition().relative(p.getDirection(),2);if(!Authority.mayChange(p.level(),p,at)||p.level().getEntitiesOfClass(net.minecraft.world.entity.animal.feline.Cat.class,p.getBoundingBox().inflate(32),cat->cat.isOwnedBy(p)).size()>=4)yield false;var cat=EntityTypes.CAT.create(p.level(),EntitySpawnReason.MOB_SUMMONED);if(cat==null)yield false;cat.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);cat.tame(p);cat.setComponent(DataComponents.CAT_VARIANT,p.level().registryAccess().lookupOrThrow(Registries.CAT_VARIANT).getOrThrow(net.minecraft.world.entity.animal.feline.CatVariants.SIAMESE));yield p.level().noCollision(cat)&&p.level().addFreshEntity(cat);}
            case "easter_egg"->{give(p,new ItemStack(Items.EGG,4));yield true;}
            case "cloud"->{AlchemyArtifacts.grantRitualFlight(p,200);yield true;}
            case "lead","saddle","flying_saddle"->{boolean found=false;for(var a:p.level().getEntitiesOfClass(MenagerieAnimal.class,p.getBoundingBox().inflate(4),a->a.owner()==null||a.ownedBy(p)).stream().limit(8).toList()){if(!Authority.mayChange(p.level(),p,a.blockPosition()))continue;a.tame(p.getUUID());found=true;break;}yield found;}
            case "biome"->{p.sendSystemMessage(Component.literal("Current habitat: "+p.level().getBiome(p.blockPosition()).unwrapKey().map(k->k.identifier().toString()).orElse("unknown")),true);yield true;}
            case "pingot","dingot"->{int count=0;for(BlockPos pos:BlockPos.betweenClosed(p.blockPosition().offset(-8,-4,-8),p.blockPosition().offset(8,4,8))){if(!p.level().hasChunkAt(pos))continue;var name=BuiltInRegistries.BLOCK.getKey(p.level().getBlockState(pos).getBlock()).getPath();if(name.contains(kind.equals("pingot")?"ore":"chest"))count++;}p.sendSystemMessage(Component.literal("Nearby loaded survey: "+count+(kind.equals("pingot")?" ore blocks":" chests")),true);yield true;}
            case "bed"->{boolean slept=false;for(var at:BlockPos.betweenClosed(p.blockPosition().offset(-4,-2,-4),p.blockPosition().offset(4,2,4))){var state=p.level().getBlockState(at);if(state.getBlock() instanceof AbstractBedBlock bed&&Authority.mayChange(p.level(),p,at)){slept=p.startSleepInBed(bed,state,bed.getBedRule(p.level(),at),at.immutable()).left().isEmpty();break;}}if(!slept)p.sendSystemMessage(Component.literal("Stand within four blocks of an available bed."),true);yield slept;}
            case "jukebox"->{p.level().playSound(null,p.blockPosition(),SoundEvents.NOTE_BLOCK_HARP.value(),SoundSource.PLAYERS,1,.8F+p.getRandom().nextFloat()*.4F);yield true;}
            case "pacman"->{p.getFoodData().eat(6,.4F);yield true;}
            case "black_hole"->{gather(p);yield true;}
            case "dirt","cobblestone","sponge","torch","mooshroom_grow"->terrain(p,kind);
            case "christmas_tree","menorah","mishumaa_saba","politically_correct","holiday_gift","siamese_gift"->{give(p,new ItemStack(Items.COOKIE,4));yield true;}
            case "april_fool","custom","meta"->{p.addEffect(new MobEffectInstance(List.of(MobEffects.JUMP_BOOST,MobEffects.SPEED,MobEffects.LUCK).get(p.getRandom().nextInt(3)),400),p);yield true;}
            default->{var effect=BUFFS.get(kind);if(effect==null)yield false;p.addEffect(new MobEffectInstance(effect,400,1),p);yield true;}
        };
    }
    private static boolean terrain(ServerPlayer p,String kind){
        var hit=p.pick(6,0,false);if(!(hit instanceof BlockHitResult b))return false;var at=b.getBlockPos();if(!Authority.mayChange(p.level(),p,at))return false;
        var state=p.level().getBlockState(at);
        if(kind.equals("dirt")&&state.is(Blocks.COBBLESTONE))return p.level().setBlock(at,Blocks.DIRT.defaultBlockState(),3);
        if(kind.equals("cobblestone")&&state.is(Blocks.DIRT))return p.level().setBlock(at,Blocks.COBBLESTONE.defaultBlockState(),3);
        if(kind.equals("torch")){at=at.relative(b.getDirection());return Authority.mayChange(p.level(),p,at)&&p.level().getBlockState(at).canBeReplaced()&&Blocks.TORCH.defaultBlockState().canSurvive(p.level(),at)&&p.level().setBlock(at,Blocks.TORCH.defaultBlockState(),3);}
        if(kind.equals("sponge")){boolean changed=false;for(var pos:BlockPos.betweenClosed(at.offset(-1,-1,-1),at.offset(1,1,1)))if(p.level().hasChunkAt(pos)&&p.level().getBlockState(pos).is(Blocks.WATER)&&Authority.mayChange(p.level(),p,pos))changed|=p.level().removeBlock(pos,false);return changed;}
        return false;
    }
    public static final class Pet extends Item {
        public final String kind;
        Pet(String kind,Properties p){super(p);this.kind=kind;}
        @Override public InteractionResult use(Level l,Player player,InteractionHand hand){
            if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(p.getCooldowns().isOnCooldown(p.getItemInHand(hand)))return InteractionResult.FAIL;
            if(!hasFood(p,kind)){p.sendSystemMessage(Component.literal("Feed with "+new ItemStack(food(kind)).getHoverName().getString()+" from your inventory."),true);return InteractionResult.FAIL;}
            if(!activate(p,hand,kind))return InteractionResult.PASS;
            pay(p,kind);p.getCooldowns().addCooldown(p.getItemInHand(hand),Set.of("ghast","wither","banana","apple").contains(kind)?30:200);return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack stack,TooltipContext c,TooltipDisplay display,Consumer<Component> lines,TooltipFlag flags){
            lines.accept(Component.literal("Use: "+kind.replace('_',' ')+" · Food: "+new ItemStack(food(kind)).getHoverName().getString()));
            if(BUFFS.containsKey(kind))lines.accept(Component.literal("Hotbar: one food per minute. Use for a short stronger boost."));
            if(Set.of("house","nether_portal","end_portal").contains(kind))lines.accept(Component.literal("Crouch-use to bind a return point; use to return if loaded."));
        }
    }
}

