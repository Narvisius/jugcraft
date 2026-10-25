package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import io.github.jimbozoomer.jugcraft.town.Town;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Source spider egg course and flamberges, plus a native use for the supplied pheromone art. */
public final class JimtaEncounterItems {
    public static Egg EGG;public static Cooked COOKED;
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    public static AttachmentType<Long> LURE_UNTIL;
    public static void register(){
        LURE_UNTIL=AttachmentRegistry.create(Jugcraft.id("jimtafacts_spider_lure"),b->b.initializer(()->0L).persistent(com.mojang.serialization.Codec.LONG));
        var eggKey=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_spellbooks_ice_spider_egg"));EGG=Registry.register(BuiltInRegistries.BLOCK,eggKey,new Egg(BlockBehaviour.Properties.of().setId(eggKey).strength(1).noOcclusion().sound(SoundType.HONEY_BLOCK)));
        var cookedKey=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_spellbooks_cooked_ice_spider_egg"));COOKED=Registry.register(BuiltInRegistries.BLOCK,cookedKey,new Cooked(BlockBehaviour.Properties.of().setId(cookedKey).strength(.5F).noOcclusion().sound(SoundType.HONEY_BLOCK)));
        item("ice_spider_egg",p->new BlockItem(EGG,p.rarity(Rarity.RARE).useBlockDescriptionPrefix()));
        item("cooked_ice_spider_egg",p->new BlockItem(COOKED,p.stacksTo(1).useBlockDescriptionPrefix()));
        item("prepared_ice_spider_egg",p->new Item(p.stacksTo(1)));item("ice_spider_pheromones",Lure::new);
        for(var name:List.of("keeper_flamberge","legionnaire_flamberge"))item(name,p->{boolean restored=name.startsWith("legionnaire");var attributes=ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,10,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).add(Attributes.ATTACK_SPEED,new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,restored?-2.5:-2.7,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).add(Attributes.ARMOR,new AttributeModifier(Jugcraft.id("jimta_flamberge_guard"),4,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build();return new Item(p.sword(ToolMaterial.NETHERITE,6,-2.7F).durability(restored?2031:1000).repairable(Items.NETHERITE_SCRAP).attributes(attributes).fireResistant().rarity(restored?Rarity.RARE:Rarity.UNCOMMON));});
        // These materials have real combat/recipe consumers and come from their source encounter loot.
        for(var name:List.of("icy_fang","cinder_essence"))if(!JimtaSpellbooks.ITEMS.containsKey(name))item(name,Item::new);
        JimtaSpellbooks.ITEMS.putAll(ITEMS);
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var player:server.getPlayerList().getPlayers()){long at=player.getAttachedOrElse(LURE_UNTIL,0L);if(at>0&&player.level().getGameTime()>=at){player.removeAttached(LURE_UNTIL);if(player.isAlive()&&JimtaSpellbooks.enabled())summon(player,8);}}});
    }
    private static void item(String name,java.util.function.Function<Item.Properties,Item> factory){var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_spellbooks_"+name));ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,key,factory.apply(new Item.Properties().setId(key))));}
    /** Original hunt size eight, capped by nearby monsters and a fixed eight-by-three placement budget. */
    public static int summon(ServerPlayer player,int wanted){
        var level=player.level();if(level.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL||Town.isInside(level,player.blockPosition())||LandRegistry.isClaimed(level,player.blockPosition()))return 0;
        int available=Math.max(0,8-level.getEntitiesOfClass(JimtaEncounterCreatures.IceSpider.class,player.getBoundingBox().inflate(64)).size()),made=0;
        for(int i=0;i<24&&made<Math.min(wanted,available);i++){
            double a=i*Math.PI/4+player.getRandom().nextFloat()*.3,r=12+(i/8)*5;Vec3 at=player.position().add(Math.cos(a)*r,0,Math.sin(a)*r);var pos=BlockPos.containing(at);
            if(!level.hasChunkAt(pos)||Town.isInside(level,pos)||LandRegistry.isClaimed(level,pos)||!Authority.mayChange(level,player,pos))continue;
            var hit=level.clip(new ClipContext(at.add(0,4,0),at.add(0,-8,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));if(hit.getType()!=HitResult.Type.BLOCK)continue;
            var actual=BlockPos.containing(hit.getLocation());if(Town.isInside(level,actual)||LandRegistry.isClaimed(level,actual)||!Authority.mayChange(level,player,actual))continue;
            var spider=JimtaEncounterCreatures.ICE_SPIDER.create(level,EntitySpawnReason.TRIGGERED);if(spider==null)continue;spider.setPos(hit.getLocation());
            if(!level.noCollision(spider)||level.containsAnyLiquid(spider.getBoundingBox()))continue;
            spider.setTarget(player);spider.emerge();if(level.addFreshEntity(spider))made++;
        }return made;
    }
    public static final class Lure extends Item {
        Lure(Properties p){super(p);}
        @Override public InteractionResult use(Level l,Player player,InteractionHand hand){if(!JimtaSpellbooks.enabled())return InteractionResult.PASS;if(player instanceof ServerPlayer p){if(p.getAttachedOrElse(LURE_UNTIL,0L)>0||Town.isInside(l,p.blockPosition())||LandRegistry.isClaimed(l,p.blockPosition()))return InteractionResult.FAIL;p.setAttached(LURE_UNTIL,l.getGameTime()+200);p.getItemInHand(hand).consume(1,p);p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("The pheromones will draw nearby ice spiders in ten seconds."));}return InteractionResult.SUCCESS;}
    }
    public static final class Egg extends Block {
        public static final BooleanProperty FROSTED=BooleanProperty.create("frosted");
        Egg(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FROSTED,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FROSTED);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(FROSTED)?Block.box(2,0,1,14,15,15):Block.box(3,0,2,13,14,14);}
        @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){return l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP);}
        @Override public void playerDestroy(ServerLevel l,ServerPlayer player,BlockPos pos,BlockState s,BlockEntity be,ItemStack tool){super.playerDestroy(l,player,pos,s,be,tool);if(s.getValue(FROSTED)&&JimtaSpellbooks.enabled()&&summon(player,1)>0)l.setBlock(pos,s.setValue(FROSTED,false),Block.UPDATE_ALL);}
    }
    public static final class Cooked extends Block {
        public static final IntegerProperty BITES=IntegerProperty.create("bites",0,3);
        Cooked(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(BITES,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BITES);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.or(Block.box(1,0,1,15,1,15),Block.box(3,1,2,13,15,14));}
        @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){return l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP);}
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos other,BlockState neighbor,RandomSource random){return d==Direction.DOWN&&!canSurvive(s,l,p)?Blocks.AIR.defaultBlockState():super.updateShape(s,l,ticks,p,d,other,neighbor,random);}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player player,BlockHitResult hit){
            if(!JimtaSpellbooks.enabled()||!player.canEat(false))return InteractionResult.PASS;if(player instanceof ServerPlayer p){if(!Authority.mayChange(p.level(),p,pos))return InteractionResult.FAIL;
                p.getFoodData().eat(4,1);var effect=JimtaSpellStatuses.EFFECTS.get("frostbitten_strikes");var current=p.getEffect(effect);p.addEffect(new MobEffectInstance(effect,Math.min(3600,900+(current==null?0:current.getDuration())),9,false,false,true));
                l.playSound(null,pos,SoundEvents.GENERIC_EAT.value(),SoundSource.BLOCKS,1,1);int bites=s.getValue(BITES);if(bites<3)l.setBlock(pos,s.setValue(BITES,bites+1),Block.UPDATE_ALL);else l.removeBlock(pos,false);
            }return InteractionResult.SUCCESS;
        }
        @Override protected boolean hasAnalogOutputSignal(BlockState s){return true;}
        @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction face){return (4-s.getValue(BITES))*2;}
    }
}
