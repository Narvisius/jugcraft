package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Native original workshops: layered finite cauldron recipes, six book slots, pedestal and per-player vaults. */
public final class JimtaWorkshops {
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static BlockEntityType<Node> NODE;
    static final List<JsonObject> RECIPES=new ArrayList<>();
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT=BlockStateProperties.LIT,HANGING=BlockStateProperties.HANGING;
    public static final IntegerProperty LEVEL=IntegerProperty.create("level",1,3);
    public static final List<BooleanProperty> SLOTS=java.util.stream.IntStream.range(0,6).mapToObj(i->BooleanProperty.create("slot_"+i+"_occupied")).toList();
    public static final EnumProperty<VaultState> VAULT=EnumProperty.create("vault_state",VaultState.class);
    public static final BooleanProperty OMINOUS=BlockStateProperties.OMINOUS;
    public static void register(){
        net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld(),net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_ORES,ResourceKey.create(Registries.PLACED_FEATURE,Jugcraft.id("jimtafacts/spellbooks/ore_mithril_placement")));
        var data=JimtaSpellbooks.data("workshops");
        for(var e:data.getAsJsonArray("materials"))material(e.getAsString(),false);
        for(var e:data.getAsJsonArray("elixirs"))material(e.getAsString(),true);
        for(var e:data.getAsJsonArray("blocks")){
            String name=e.getAsString();var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id(JimtaSpellbooks.PREFIX+name));
            var p=BlockBehaviour.Properties.of().setId(key).strength(name.endsWith("vault")?-1:2.5F,name.endsWith("vault")?3600000:6).noOcclusion();
            if(name.startsWith("brazier")||name.equals("firefly_jar"))p.lightLevel(s->s.getValue(LIT)?name.equals("brazier_soul")?10:15:0);
            Block block=name.equals("nether_brick_pillar")?new RotatedPillarBlock(p):name.equals("wisewood_chiseled_bookshelf")?new Shelf(p):name.endsWith("vault")?new Vault(name,p):new Workshop(name,p);
            BLOCKS.put(name,Registry.register(BuiltInRegistries.BLOCK,key,block));
            var itemKey=ResourceKey.create(Registries.ITEM,key.identifier());JimtaSpellbooks.ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,itemKey,name.equals("tyros_statue")?new StatueItem(block,new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()):new BlockItem(block,new Item.Properties().setId(itemKey).useBlockDescriptionPrefix())));
        }
        NODE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimtafacts_spell_workshop"),FabricBlockEntityTypeBuilder.create(Node::new,BLOCKS.values().stream().filter(b->b instanceof EntityBlock).toArray(Block[]::new)).build());
        for(var e:JimtaSpellbooks.data("cauldron_recipes").getAsJsonArray("recipes"))RECIPES.add(e.getAsJsonObject());
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player,world,hand,target,hit)->captureLightning(player,hand,target));
    }
    /** Capture the charged-creeper lightning described by the supplied item guide. */
    public static InteractionResult captureLightning(Player player,InteractionHand hand,Entity target){
        if(!JimtaSpellbooks.enabled()||player.isSpectator()||!player.isAlive()
                ||!player.getItemInHand(hand).is(Items.GLASS_BOTTLE)
                ||!(target instanceof net.minecraft.world.entity.monster.Creeper creeper)||!creeper.isAlive()||!creeper.isPowered())return InteractionResult.PASS;
        if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(p.distanceToSqr(creeper)>36||!Authority.mayChange(p.level(),p,creeper.blockPosition()))return InteractionResult.FAIL;
        // Discharge before awarding; repeated interactions cannot duplicate the charge.
        creeper.getEntityData().set(io.github.jimbozoomer.jugcraft.mixin.JimtaCreeperAccessor.jugcraft$charge(),false);
        p.setItemInHand(hand,ItemUtils.createFilledResult(p.getItemInHand(hand),p,new ItemStack(JimtaSpellbooks.ITEMS.get("lightning_bottle"))));
        return InteractionResult.SUCCESS;
    }
    private static void material(String name,boolean drink){var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(JimtaSpellbooks.PREFIX+name));var p=new Item.Properties().setId(key);JimtaSpellbooks.ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,key,drink?new Elixir(p.stacksTo(4),name):new Item(p)));}
    public static String nativeId(String id){return id.startsWith("irons_spellbooks:")?"jugcraft:"+JimtaSpellbooks.PREFIX+id.substring(id.indexOf(':')+1):id;}
    static Item item(String id){return BuiltInRegistries.ITEM.getValue(Identifier.parse(nativeId(id)));}
    private static void give(ServerPlayer p,ItemStack stack){if(!stack.isEmpty()&&!p.getInventory().add(stack))p.drop(stack,false,net.minecraft.util.Prediction.SERVER_ONLY);}
    public static class Workshop extends Block implements EntityBlock {
        public final String kind;
        Workshop(String kind,Properties p){super(p);this.kind=kind;var state=stateDefinition.any().setValue(FACING,Direction.NORTH);if(state.hasProperty(LIT))state=state.setValue(LIT,true).setValue(HANGING,false).setValue(LEVEL,1);registerDefaultState(state);}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LIT,HANGING,LEVEL);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){var state=defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());return state.hasProperty(HANGING)?state.setValue(HANGING,c.getClickedFace()==Direction.DOWN):state;}
        @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
        @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return !l.isClientSide()&&t==NODE?(world,pos,state,be)->((Node)be).tick():null;}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){super.setPlacedBy(l,p,s,e,stack);if(l.getBlockEntity(p) instanceof Node n&&e!=null){n.owner=e.getUUID();n.changed();}}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player actor,InteractionHand hand,BlockHitResult hit){
            if(!(actor instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!JimtaSpellbooks.enabled()||!Authority.mayChange(p.level(),p,pos))return InteractionResult.FAIL;
            if(kind.startsWith("brazier")){if(stack.is(Items.FLINT_AND_STEEL)||stack.is(Items.FIRE_CHARGE)){if(s.getValue(LIT))return InteractionResult.PASS;l.setBlock(pos,s.setValue(LIT,true),Block.UPDATE_ALL);if(stack.is(Items.FIRE_CHARGE))stack.consume(1,p);else stack.hurtAndBreak(1,p,hand.asEquipmentSlot());return InteractionResult.SUCCESS;}if(stack.is(Items.WATER_BUCKET)){if(!s.getValue(LIT))return InteractionResult.PASS;l.setBlock(pos,s.setValue(LIT,false),Block.UPDATE_ALL);if(!p.getAbilities().instabuild)p.setItemInHand(hand,new ItemStack(Items.BUCKET));return InteractionResult.SUCCESS;}}
            if(l.getBlockEntity(pos) instanceof Node n&&n.interact(p,hand))return InteractionResult.SUCCESS;return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player actor,BlockHitResult hit){
            if(!(actor instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!JimtaSpellbooks.enabled()||!Authority.mayChange(p.level(),p,pos))return InteractionResult.FAIL;
            if(kind.equals("firefly_jar")){l.setBlock(pos,s.cycle(LIT),Block.UPDATE_ALL);return InteractionResult.SUCCESS;}
            if(l.getBlockEntity(pos) instanceof Node n){if(p.isShiftKeyDown()&&n.interact(p,InteractionHand.MAIN_HAND))return InteractionResult.SUCCESS;if(kind.equals("pedestal")){give(p,n.take(0));return InteractionResult.SUCCESS;}if(kind.contains("cauldron")){p.sendOverlayMessage(Component.literal(n.description()));return InteractionResult.SUCCESS;}}
            return InteractionResult.PASS;
        }
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
            if(kind.contains("cauldron"))return Shapes.or(Block.box(2,0,2,14,4,14),Block.box(1,4,1,3,15,15),Block.box(13,4,1,15,15,15),Block.box(3,4,1,13,15,3),Block.box(3,4,13,13,15,15));
            if(kind.equals("pedestal"))return Shapes.or(Block.box(2,0,2,14,3,14),Block.box(5,3,5,11,13,11),Block.box(1,13,1,15,16,15));
            if(kind.equals("book_stack")||kind.equals("armor_pile"))return Block.box(1,0,1,15,8,15);
            if(kind.equals("firefly_jar"))return Block.box(5,0,5,11,10,11);
            if(kind.startsWith("brazier"))return s.getValue(HANGING)?Block.box(2,3,2,14,16,14):Block.box(2,0,2,14,13,14);
            if(kind.equals("cinderous_soul_rune"))return Block.box(0,0,0,16,2,16);
            return Shapes.block();
        }
        @Override protected boolean hasAnalogOutputSignal(BlockState s){return true;}
        @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction d){return l.getBlockEntity(p) instanceof Node n?Math.min(15,n.amount()*15/1000+(n.stacks.stream().anyMatch(x->!x.isEmpty())?1:0)):0;}
    }
    public static final class Vault extends Workshop {
        Vault(String name,Properties p){super(name,p);registerDefaultState(defaultBlockState().setValue(VAULT,VaultState.ACTIVE).setValue(OMINOUS,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){super.createBlockStateDefinition(b);b.add(VAULT,OMINOUS);}
    }
    public static final class Shelf extends Workshop {
        Shelf(Properties p){super("wisewood_chiseled_bookshelf",p);var state=defaultBlockState();for(var slot:SLOTS)state=state.setValue(slot,false);registerDefaultState(state);}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING);for(var slot:SLOTS)b.add(slot);}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player actor,BlockHitResult hit){if(actor instanceof ServerPlayer p&&Authority.mayChange(p.level(),p,pos)&&l.getBlockEntity(pos) instanceof Node n){int i=slot(s,pos,hit);give(p,n.take(i));return InteractionResult.SUCCESS;}return InteractionResult.PASS;}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player actor,InteractionHand hand,BlockHitResult hit){if(!(actor instanceof ServerPlayer p))return InteractionResult.SUCCESS;if(!JimtaSpellbooks.enabled()||!Authority.mayChange(p.level(),p,pos))return InteractionResult.FAIL;int i=slot(s,pos,hit);if(l.getBlockEntity(pos) instanceof Node n){if(!n.stacks.get(i).isEmpty()){give(p,n.take(i));return InteractionResult.SUCCESS;}if(stack.is(ItemTags.BOOKSHELF_BOOKS)||stack.getItem() instanceof JimtaSpellbooks.Book){n.stacks.set(i,stack.copyWithCount(1));stack.consume(1,p);n.changed();return InteractionResult.SUCCESS;}}return InteractionResult.PASS;}
        private static int slot(BlockState s,BlockPos pos,BlockHitResult hit){Vec3 v=hit.getLocation().subtract(Vec3.atLowerCornerOf(pos));double x=switch(s.getValue(FACING)){case NORTH->1-v.x;case SOUTH->v.x;case EAST->1-v.z;default->v.z;};return (v.y>.5?0:3)+Math.clamp((int)(x*3),0,2);}
    }
    public static final class Node extends BlockEntity implements WorldlyContainer,com.geckolib.animatable.GeoBlockEntity {
        private final com.geckolib.animatable.instance.AnimatableInstanceCache cache=com.geckolib.util.GeckoLibUtil.createInstanceCache(this);
        @Override public void registerControllers(com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar controllers){}
        @Override public com.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        public final NonNullList<ItemStack> stacks=NonNullList.withSize(9,ItemStack.EMPTY);
        public final LinkedHashMap<String,Integer> fluids=new LinkedHashMap<>();
        private UUID owner;private final Set<UUID> rewarded=new HashSet<>();private int work;
        private static final Codec<Map<String,Integer>> FLUIDS=Codec.unboundedMap(Codec.STRING,Codec.intRange(0,1000));
        Node(BlockPos p,BlockState s){super(NODE,p,s);}
        public String kind(){return getBlockState().getBlock() instanceof Workshop w?w.kind:"";}
        public int amount(){return fluids.values().stream().mapToInt(Integer::intValue).sum();}
        private boolean canFill(String fluid,int count){return count>=0&&amount()+count<=1000&&(fluids.containsKey(fluid)||fluids.size()<4);}
        private void fill(String fluid,int count){if(count>0)fluids.merge(fluid,count,Integer::sum);}
        private void drain(String fluid,int count){int left=fluids.getOrDefault(fluid,0)-count;if(left<=0)fluids.remove(fluid);else fluids.put(fluid,left);}
        public String description(){return fluids.isEmpty()?"Empty cauldron · pour a potion, ink or water; add a reagent to brew.":fluids.entrySet().stream().map(e->e.getKey().replace("irons_spellbooks:","").replace("minecraft:","")+" "+e.getValue()+" mB").reduce((a,b)->a+" · "+b).orElse("");}
        private boolean permitted(){return level instanceof ServerLevel l&&JimtaSpellbooks.enabled()&&Authority.mayChangeFor(l,owner,worldPosition);}
        private void tick(){if(!(level instanceof ServerLevel l)||!JimtaSpellbooks.enabled())return;
            if(kind().endsWith("vault")){if(getBlockState().getValue(VAULT)!=VaultState.ACTIVE&&++work>=30){work=0;l.setBlock(worldPosition,getBlockState().setValue(VAULT,VaultState.ACTIVE),Block.UPDATE_CLIENTS);}return;}
            if(kind().equals("blood_cauldron")&&permitted()&&l.getGameTime()%20==0&&CampfireBlock.isLitCampfire(l.getBlockState(worldPosition.below()))&&canFill("irons_spellbooks:blood",250)){
                var player=Authority.present(l,owner);if(player!=null)for(var victim:l.getEntitiesOfClass(LivingEntity.class,new AABB(worldPosition).deflate(.1).move(0,.1,0),LivingEntity::isAlive).stream().limit(4).toList()){
                    if(victim.getType().builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts/cant_produce_blood"))))continue;
                    var context=new io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects.Context(l,io.github.jimbozoomer.jugcraft.concordance.effect.Cause.of(owner,io.github.jimbozoomer.jugcraft.concordance.effect.Cause.Origin.ITEM,"jugcraft:jimta_spellbooks_blood_cauldron",io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects.nextSerial()),player,new io.github.jimbozoomer.jugcraft.concordance.effect.Ledger(new io.github.jimbozoomer.jugcraft.concordance.effect.Ledger.Limits(1,4,0)),"cook",Vec3.atCenterOf(worldPosition));
                    if(io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects.apply(context,io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec.of(io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind.DAMAGE,io.github.jimbozoomer.jugcraft.concordance.effect.Intent.HARMFUL,2,0),victim).applied()&&l.getRandom().nextBoolean()&&canFill("irons_spellbooks:blood",250)){fill("irons_spellbooks:blood",250);changed();}
                }
            }
            if(!kind().contains("cauldron")||!permitted()||++work<101)return;work=0;
            for(int i=0;i<4;i++)if(!stacks.get(i).isEmpty()){ItemStack input=stacks.get(i);ItemStack result=process(input,false);if(result!=null&&fit(result)){process(input,true);input.shrink(1);storeOutput(result);changed();break;}}
        }
        private boolean fit(ItemStack result){if(result.isEmpty())return true;for(int i=4;i<9;i++)if(stacks.get(i).isEmpty()||ItemStack.isSameItemSameComponents(stacks.get(i),result)&&stacks.get(i).getCount()+result.getCount()<=result.getMaxStackSize())return true;return false;}
        private void storeOutput(ItemStack result){if(result.isEmpty())return;for(int i=4;i<9;i++){var old=stacks.get(i);if(old.isEmpty()){stacks.set(i,result.copy());return;}if(ItemStack.isSameItemSameComponents(old,result)&&old.getCount()+result.getCount()<=old.getMaxStackSize()){old.grow(result.getCount());return;}}}
        public boolean interact(ServerPlayer p,InteractionHand hand){var stack=p.getItemInHand(hand);if(owner==null)owner=p.getUUID();String kind=kind();
            if(kind.equals("pedestal")){if(stack.isEmpty())return false;var old=take(0);stacks.set(0,stack.copyWithCount(1));stack.consume(1,p);give(p,old);changed();return true;}
            if(kind.endsWith("vault")){String needed=kind.equals("bone_vault")?"bone_key":"decrepit_key";if(!stack.is(JimtaSpellbooks.ITEMS.get(needed))||rewarded.contains(p.getUUID())||rewarded.size()>=10000)return false;
                // Persist eligibility before awarding: each actual player may open this generated vault once.
                rewarded.add(p.getUUID());stack.consume(1,p);changed();level.setBlock(worldPosition,getBlockState().setValue(VAULT,VaultState.EJECTING),Block.UPDATE_CLIENTS);
                var l=(ServerLevel)level;var key=ResourceKey.create(Registries.LOOT_TABLE,Jugcraft.id("jimtafacts/spellbooks/chests/"+(kind.equals("bone_vault")?"catacombs/dead_king_vault":"citadel/citadel_vault")));
                var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(l).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,Vec3.atCenterOf(worldPosition)).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,p).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
                for(var reward:l.getServer().reloadableRegistries().getLootTable(key).getRandomItems(params))give(p,reward);return true;}

            if(!kind.contains("cauldron"))return false;
            if(p.isShiftKeyDown()&&stack.isEmpty()){for(int i=8;i>=4;i--)if(!stacks.get(i).isEmpty()){give(p,take(i));return true;}return false;}
            var result=process(stack,false);if(result!=null){process(stack,true);stack.consume(1,p);give(p,result);changed();return true;}
            return false;
        }
        private static boolean matches(ItemStack input,JsonObject ingredient){if(ingredient.has("item"))return input.is(item(ingredient.get("item").getAsString()));if(ingredient.has("tag"))return input.is(TagKey.create(Registries.ITEM,Identifier.parse(ingredient.get("tag").getAsString())));return false;}
        private static String fluidKey(JsonObject f){String id=f.get("id").getAsString();if(f.has("components")){var components=f.getAsJsonObject("components");if(components.has("minecraft:potion_contents"))id="potion|"+components.getAsJsonObject("minecraft:potion_contents").get("potion").getAsString();}return id;}
        private static ItemStack output(JsonObject row){if(row==null||!row.has("id"))return ItemStack.EMPTY;return new ItemStack(item(row.get("id").getAsString()),row.has("count")?row.get("count").getAsInt():1);}
        /** null means no operation; EMPTY is a successful reagent dissolution without a byproduct. */
        private ItemStack process(ItemStack input,boolean apply){
            if(input.isEmpty())return null;
            if(input.getItem() instanceof JimtaSpellbooks.Scroll&&fluids.getOrDefault("minecraft:water",0)>=250&&(fluids.size()<4||fluids.containsKey("irons_spellbooks:common_ink")||fluids.get("minecraft:water")==250)){
                if(apply){drain("minecraft:water",250);if(level.getRandom().nextBoolean())fill("irons_spellbooks:common_ink",250);}return ItemStack.EMPTY;
            }
            var contents=input.get(DataComponents.POTION_CONTENTS);
            if(input.is(Items.POTION)&&contents!=null&&contents.potion().isPresent()){
                String key="potion|"+contents.potion().get().unwrapKey().map(k->k.identifier().toString()).orElse("");key=key.replace("jugcraft:jimta_spellbooks_","irons_spellbooks:");if(key.equals("potion|minecraft:water"))key="minecraft:water";
                if(!canFill(key,250))return null;if(apply)fill(key,250);return new ItemStack(Items.GLASS_BOTTLE);
            }
            if(input.is(Items.GLASS_BOTTLE)&&!fluids.isEmpty()){
                var last=new ArrayList<>(fluids.keySet()).getLast();if(last.equals("minecraft:water")&&fluids.get(last)>=250){if(apply)drain(last,250);return PotionContents.createItemStack(Items.POTION,Potions.WATER);}if(last.startsWith("potion|")&&fluids.get(last)>=250){var key=ResourceKey.create(Registries.POTION,Identifier.parse(nativeId(last.substring(7))));var holder=level.registryAccess().lookupOrThrow(Registries.POTION).get(key);if(holder.isPresent()){if(apply)drain(last,250);return PotionContents.createItemStack(Items.POTION,holder.get());}}
            }
            for(var row:RECIPES){if(!matches(input,row.getAsJsonObject("input")))continue;String type=row.get("type").getAsString();
                if(type.endsWith("_fill")){var f=row.getAsJsonObject("fluid");String key=fluidKey(f);int amount=f.get("amount").getAsInt();if(!canFill(key,amount))continue;if(apply)fill(key,amount);return output(row.getAsJsonObject("result"));}
                if(type.endsWith("_empty")){var f=row.getAsJsonObject("fluid");String key=fluidKey(f);int amount=f.get("amount").getAsInt();if(fluids.getOrDefault(key,0)<amount)continue;if(apply)drain(key,amount);return output(row.getAsJsonObject("result"));}
                if(type.endsWith("_brew")){var f=row.getAsJsonObject("base_fluid");String key=fluidKey(f);int amount=f.get("amount").getAsInt();if(fluids.getOrDefault(key,0)<amount)continue;var next=new LinkedHashMap<>(fluids);int left=next.get(key)-amount;if(left==0)next.remove(key);else next.put(key,left);for(var r:row.getAsJsonArray("results")){var result=r.getAsJsonObject();next.merge(fluidKey(result),result.get("amount").getAsInt(),Integer::sum);}if(next.size()>4||next.values().stream().mapToInt(Integer::intValue).sum()>1000)continue;if(apply){fluids.clear();fluids.putAll(next);}return output(row.getAsJsonObject("byproduct"));}
            }
            if(level instanceof ServerLevel server)for(var entry:new ArrayList<>(fluids.entrySet())){if(entry.getValue()<250||!entry.getKey().startsWith("potion|"))continue;
                var key=ResourceKey.create(Registries.POTION,Identifier.parse(nativeId(entry.getKey().substring(7))));var holder=server.registryAccess().lookupOrThrow(Registries.POTION).get(key);if(holder.isEmpty())continue;
                var brewing=new net.minecraft.world.item.crafting.BrewingInput(PotionContents.createItemStack(Items.POTION,holder.get()),input);
                var match=server.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.BREWING,brewing,server);if(match.isEmpty())continue;
                var next=match.get().value().assemble(brewing).get(DataComponents.POTION_CONTENTS);if(next==null||next.potion().isEmpty())continue;
                String name="potion|"+next.potion().get().unwrapKey().orElseThrow().identifier().toString().replace("jugcraft:jimta_spellbooks_","irons_spellbooks:");
                if(!fluids.containsKey(name)&&fluids.size()>=4&&entry.getValue()>250)continue;
                if(apply){drain(entry.getKey(),250);fill(name,250);}return input.getItem().getCraftingRemainder()==null?ItemStack.EMPTY:input.getItem().getCraftingRemainder().create();
            }
            return null;
        }
        public boolean forget(UUID player){if(!kind().endsWith("vault")||!rewarded.remove(player))return false;changed();return true;}
        public ItemStack take(int i){var result=stacks.get(i);stacks.set(i,ItemStack.EMPTY);changed();return result;}
        public void changed(){setChanged();if(level!=null){if(kind().equals("wisewood_chiseled_bookshelf")){var s=getBlockState();for(int i=0;i<6;i++)s=s.setValue(SLOTS.get(i),!stacks.get(i).isEmpty());if(s!=getBlockState())level.setBlock(worldPosition,s,Block.UPDATE_CLIENTS);}level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());}}
        @Override public void preRemoveSideEffects(BlockPos p,BlockState s){if(level!=null){for(var stack:stacks)if(!stack.isEmpty())Containers.dropItemStack(level,p.getX(),p.getY(),p.getZ(),stack);stacks.clear();}}
        @Override protected void saveAdditional(ValueOutput o){super.saveAdditional(o);o.store("items",ItemStack.OPTIONAL_CODEC.listOf(),stacks);o.store("fluids",FLUIDS,fluids);if(owner!=null)o.store("owner",UUIDUtil.CODEC,owner);o.store("rewarded",UUIDUtil.CODEC.listOf(),new ArrayList<>(rewarded));}
        @Override protected void loadAdditional(ValueInput i){super.loadAdditional(i);var loaded=i.read("items",ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());for(int n=0;n<Math.min(9,loaded.size());n++)stacks.set(n,loaded.get(n));fluids.clear();for(var e:i.read("fluids",FLUIDS).orElse(Map.of()).entrySet())if(canFill(e.getKey(),e.getValue()))fill(e.getKey(),e.getValue());owner=i.read("owner",UUIDUtil.CODEC).orElse(null);rewarded.clear();rewarded.addAll(i.read("rewarded",UUIDUtil.CODEC.listOf()).orElse(List.of()).stream().limit(10000).toList());}
        @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider r){var out=TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,r);out.store("items",ItemStack.OPTIONAL_CODEC.listOf(),stacks);out.store("fluids",FLUIDS,fluids);return out.buildResult();}
        @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
        @Override public int getContainerSize(){return 9;}@Override public boolean isEmpty(){return stacks.stream().allMatch(ItemStack::isEmpty);}@Override public ItemStack getItem(int slot){return slot>=0&&slot<9?stacks.get(slot):ItemStack.EMPTY;}
        @Override public ItemStack removeItem(int slot,int n){if(slot<0||slot>=9)return ItemStack.EMPTY;var out=stacks.get(slot).split(n);changed();return out;}@Override public ItemStack removeItemNoUpdate(int slot){return slot>=0&&slot<9?take(slot):ItemStack.EMPTY;}
        @Override public void setItem(int slot,ItemStack stack){if(slot>=0&&slot<9){stacks.set(slot,stack);changed();}}@Override public boolean stillValid(Player p){return p instanceof ServerPlayer sp&&Authority.mayChange(sp.level(),sp,worldPosition)&&p.distanceToSqr(Vec3.atCenterOf(worldPosition))<=64;}@Override public void clearContent(){stacks.clear();changed();}
        @Override public int[] getSlotsForFace(Direction side){return kind().contains("cauldron")?(side==Direction.DOWN?new int[]{4,5,6,7,8}:new int[]{0,1,2,3}):new int[0];}
        @Override public boolean canPlaceItemThroughFace(int i,ItemStack s,Direction d){return i<4&&kind().contains("cauldron")&&permitted();}@Override public boolean canTakeItemThroughFace(int i,ItemStack s,Direction d){return i>=4&&kind().contains("cauldron")&&permitted();}
    }
    public static java.util.function.Supplier<Object> statueRenderer=()->null;
    public static final class StatueItem extends BlockItem implements com.geckolib.animatable.GeoItem {
        private final com.geckolib.animatable.instance.AnimatableInstanceCache cache=com.geckolib.util.GeckoLibUtil.createInstanceCache(this);
        StatueItem(Block block,Properties p){super(block,p);}
        @Override public Object getRenderProvider(){return statueRenderer.get();}
        @Override public com.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        @Override public void registerControllers(com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar controllers){}
    }
    private static final class Elixir extends Item {
        final String name;Elixir(Properties p,String name){super(p);this.name=name;}
        @Override public InteractionResult use(Level l,Player p,InteractionHand hand){if(!JimtaSpellbooks.enabled())return InteractionResult.FAIL;p.startUsingItem(hand);return InteractionResult.CONSUME;}
        @Override public int getUseDuration(ItemStack s,LivingEntity e){return 32;}@Override public ItemUseAnimation getUseAnimation(ItemStack s){return ItemUseAnimation.DRINK;}
        @Override public ItemStack finishUsingItem(ItemStack stack,Level l,LivingEntity e){if(l instanceof ServerLevel&&e instanceof ServerPlayer p){switch(name){case "greater_healing_potion"->p.heal(16);case "oakskin_elixir","greater_oakskin_elixir"->p.addEffect(new MobEffectInstance(JimtaSpellStatuses.EFFECTS.get("oakskin"),name.startsWith("greater")?2400:1200,name.startsWith("greater")?6:2));case "invisibility_elixir","greater_invisibility_elixir"->JimtaWardSpells.trueInvisibility(p,name.startsWith("greater")?800:300);case "evasion_elixir","greater_evasion_elixir"->p.addEffect(new MobEffectInstance(JimtaWardSpells.EFFECTS.get("evasion"),1200,name.startsWith("greater")?3:1));case "fire_ale"->{p.addEffect(new MobEffectInstance(MobEffects.NAUSEA,100,3));p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,900));p.addEffect(new MobEffectInstance(MobEffects.HASTE,900,2));}default->{}}
            if(!p.getAbilities().instabuild){stack.shrink(1);if(stack.isEmpty())return new ItemStack(Items.GLASS_BOTTLE);give(p,new ItemStack(Items.GLASS_BOTTLE));}}
            return stack;}
    }
}
