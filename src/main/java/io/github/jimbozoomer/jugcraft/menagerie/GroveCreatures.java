package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.*;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

/** Owner woodland assets use native inventories, furnaces, breeding and authority. */
public final class GroveCreatures {
    public static java.util.function.Function<Wearable,Object> renderer=item->null;
    private GroveCreatures(){}
    public static void register(){
        JsonObject catalog;
        try(var in=GroveCreatures.class.getResourceAsStream("/jugcraft/menagerie/grove.json")){
            if(in==null)return;catalog=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(IOException e){throw new IllegalStateException(e);}
        for(var entry:catalog.getAsJsonObject("blocks").entrySet().stream().sorted(java.util.Comparator.comparingInt(e->e.getKey().contains("potted_")?1:0)).toList()){
            String id=entry.getKey(),source=entry.getValue().getAsJsonObject().get("source").getAsString();
            var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id(id));var props=BlockBehaviour.Properties.of().setId(key).strength(1.5F).noOcclusion();Block b;
            if(source.equals("cinder_furnace"))b=new FurnaceBlock(props.strength(3.5F).lightLevel(s->s.getValue(FurnaceBlock.LIT)?13:0));
            else if(source.equals("lizard_egg_block"))b=new LizardEgg(props.randomTicks().strength(.3F));
            else if(source.startsWith("potted_"))b=new FlowerPotBlock(Menagerie.BLOCKS.get("grove_"+source.substring(7)+"_block"),props);
            else b=new LilyPadBlock(props.noCollision().strength(0).sound(SoundType.LILY_PAD)){};
            Registry.register(BuiltInRegistries.BLOCK,key,b);Menagerie.BLOCKS.put(id,b);
            if(b instanceof FurnaceBlock)((FabricBlockEntityType)BlockEntityTypes.FURNACE).addValidBlock(b);
            Menagerie.item(id,p->b instanceof LilyPadBlock?new PlaceOnWaterBlockItem(b,p.useBlockDescriptionPrefix()):new BlockItem(b,p.useBlockDescriptionPrefix()));
        }
        for(var entry:catalog.getAsJsonObject("items").entrySet()){
            String id=entry.getKey();var value=entry.getValue().getAsJsonObject();
            Menagerie.item(id,p->{
                if(value.get("kind").getAsString().equals("spawn"))return new MenagerieItems.Spawn(value.get("species").getAsString(),p);
                String name=value.get("source").getAsString();
                if(name.equals("sporeling_backpack"))return new Wearable(name,EquipmentSlot.CHEST,p.component(DataComponents.CONTAINER,ItemContainerContents.EMPTY));
                if(name.endsWith("flower_crown"))return new Wearable(name,EquipmentSlot.HEAD,p);
                if(name.startsWith("heal_spell_book_"))return new Mending(Integer.parseInt(name.substring(name.length()-1)),p.stacksTo(1));
                if(name.equals("cinder_sword"))return new CinderSword(p.sword(ToolMaterial.IRON,3,-2.4F));
                if(name.equals("cactem_spear"))return new io.github.jimbozoomer.jugcraft.weapons.ThrownArmItem("javelin",new io.github.jimbozoomer.jugcraft.weapons.JugcraftArms.Thrown("javelin","steel",10,2.6F,.035F,7),p.sword(ToolMaterial.IRON,3,-2.6F));
                if(name.equals("entity_net"))return new MenagerieItems.Captured(name,p.stacksTo(1));
                if(name.equals("cindershell_bucket")||name.startsWith("lizard_item_"))return new MenagerieItems.Captured(name,p.stacksTo(1));
                if(name.startsWith("sporeling_")&&name.endsWith("_egg"))return new MenagerieItems.Spawn("grove_sporeling",p,name.contains("nether")?2:0,false);
                if(name.equals("lizard_egg"))return new BlockItem(Menagerie.BLOCKS.get("grove_lizard_egg_block"),p);
                if(name.endsWith("_waterlily"))return new PlaceOnWaterBlockItem(Menagerie.BLOCKS.get("grove_"+name+"_block"),p);
                if(name.equals("apple_slice"))p.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(1).saturationModifier(.3F).build());
                return new Item(p);
            });
        }
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%20==0)for(var p:server.getPlayerList().getPlayers()){
            if(p.getItemBySlot(EquipmentSlot.HEAD).is(Menagerie.item("grove_glowing_flower_crown")))p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,220,0,true,false));
        }});
    }
    public static final class Wearable extends MenagerieItems.Pouch implements GeoItem {
        public final String name;private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        Wearable(String name,EquipmentSlot slot,Item.Properties p){super(p.component(DataComponents.EQUIPPABLE,Equippable.builder(slot).setAsset(ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,Jugcraft.id("menagerie/"+name))).build()));this.name=name;}
        @Override public InteractionResult use(Level level,Player p,InteractionHand hand){
            if(name.equals("sporeling_backpack"))return super.use(level,p,hand);
            return p.getItemInHand(hand).get(DataComponents.EQUIPPABLE).swapWithEquipmentSlot(p.getItemInHand(hand),p);
        }
        public void registerControllers(AnimatableManager.ControllerRegistrar c){}
        public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        public Object getRenderProvider(){return renderer.apply(this);}
    }
    private static final class Mending extends Item {
        private final int tier;Mending(int tier,Properties p){super(p);this.tier=tier;}
        @Override public InteractionResult use(Level level,Player who,InteractionHand hand){
            if(!(who instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            var held=p.getItemInHand(hand);if(p.getCooldowns().isOnCooldown(held)||p.getHealth()>=p.getMaxHealth())return InteractionResult.PASS;
            if(!take(p,Items.GLOWSTONE_DUST)){p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Mending requires one glowstone dust."),true);return InteractionResult.FAIL;}
            p.heal(4*tier);p.getCooldowns().addCooldown(held,200);p.level().sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,p.getX(),p.getY()+1,p.getZ(),3,.3,.4,.3,0);return InteractionResult.SUCCESS;
        }
    }
    private static boolean take(ServerPlayer p,Item item){for(int i=0;i<36;i++){var s=p.getInventory().getItem(i);if(s.is(item)){s.consume(1,p);return true;}}return false;}
    private static final class CinderSword extends Item {
        CinderSword(Properties p){super(p);}
        @Override public void hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){super.hurtEnemy(stack,target,attacker);if(attacker instanceof ServerPlayer p&&io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(p,p.level(),target)){
            target.igniteForSeconds(3);var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.putInt("CinderHeat",Math.min(4,data.getIntOr("CinderHeat",0)+1));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        }}
    }
    public static InteractionResult interact(MenagerieAnimal a,Player player,InteractionHand hand){
        if(!a.species().id().startsWith("grove_"))return InteractionResult.PASS;
        var held=player.getItemInHand(hand);String source=a.species().source();
        if((source.equals("minipad")||source.equals("lilytad"))&&held.is(Items.SHEARS)&&a.owner()!=null&&!a.ownedBy(player))return InteractionResult.FAIL;
        if((source.equals("minipad")||source.equals("lilytad"))&&held.is(Items.SHEARS)&&a.variant()<3){
            if(player instanceof ServerPlayer p){String color=List.of("pink","light_pink","yellow").get(a.variant());a.shearFlowers();a.spawnAtLocation(p.level(),new ItemStack(Menagerie.item("grove_"+color+"_minipad_flower"+(source.equals("minipad")?"_glow":""))));held.hurtAndBreak(1,p,hand.asEquipmentSlot());}return InteractionResult.SUCCESS;
        }
        if(source.equals("cindershell")&&a.ownedBy(player)&&!a.isBaby()){
            if(held.is(Menagerie.item("grove_cinder_furnace"))&&a.getItemBySlot(EquipmentSlot.BODY).isEmpty()){
                if(player instanceof ServerPlayer p){a.equipBody(held.copyWithCount(1));held.consume(1,p);}return InteractionResult.SUCCESS;
            }
            if(player instanceof ServerPlayer p&&a.getItemBySlot(EquipmentSlot.BODY).is(Menagerie.item("grove_cinder_furnace"))&&!held.isEmpty()){
                var input=new SingleRecipeInput(held);var recipe=p.level().recipeAccess().getRecipeFor(RecipeType.SMELTING,input,p.level());
                if(recipe.isPresent()&&!p.getCooldowns().isOnCooldown(held)){
                    if(!take(p,Items.COAL)){p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Cindershell smelting needs one coal."),true);return InteractionResult.FAIL;}
                    var output=recipe.get().value().assemble(input);p.getCooldowns().addCooldown(held,40);held.shrink(1);if(!p.getInventory().add(output))p.spawnAtLocation(p.level(),output);return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }
    private static final class LizardEgg extends Block {
        static final IntegerProperty EGGS=IntegerProperty.create("eggs",1,6),AGE=IntegerProperty.create("age",0,3);
        LizardEgg(Properties p){super(p.randomTicks());registerDefaultState(defaultBlockState().setValue(EGGS,1).setValue(AGE,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(EGGS,AGE);}
        @Override protected boolean canBeReplaced(BlockState state,net.minecraft.world.item.context.BlockPlaceContext c){return !c.isSecondaryUseActive()&&state.getValue(EGGS)<6&&(c.getItemInHand().is(Menagerie.item("grove_lizard_egg"))||c.getItemInHand().is(asItem()));}
        @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){var state=c.getLevel().getBlockState(c.getClickedPos());return state.is(this)?state.setValue(EGGS,Math.min(6,state.getValue(EGGS)+1)):super.getStateForPlacement(c);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(3,0,3,13,7,13);}
        @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){
            if(r.nextInt(12)!=0)return;if(s.getValue(AGE)<3){l.setBlock(p,s.cycle(AGE),3);return;}
            if(l.getEntitiesOfClass(MenagerieAnimal.class,new net.minecraft.world.phys.AABB(p).inflate(12)).size()>=12)return;
            var a=Menagerie.TYPES.get("grove_lizard").create(l,EntitySpawnReason.BREEDING);if(a==null)return;a.setBaby(true);a.snapTo(p.getX()+.5,p.getY()+.5,p.getZ()+.5,0,0);
            if(l.noCollision(a)&&l.addFreshEntity(a)){if(s.getValue(EGGS)>1)l.setBlock(p,s.setValue(EGGS,s.getValue(EGGS)-1),3);else l.removeBlock(p,false);}
        }
    }
}
