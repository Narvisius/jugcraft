package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

/** Source crop varieties on native farmland, growth, food, protection and robot harvesting APIs. */
public final class IndustrialCrops {
    public record Variety(String id,String product,String anchor,int light,int tier,String parentA,String parentB) {}
    public static final Map<String,Variety> VARIETIES=new LinkedHashMap<>();
    public static final Map<String,IndustrialCrop> CROPS=new LinkedHashMap<>();
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    public static BlockEntityType<Genes> GENES;
    private IndustrialCrops() {}
    public static void register(){
        try(var stream=IndustrialCrops.class.getClassLoader().getResourceAsStream("jugcraft/jimtafacts/crops.json")){
            if(stream==null)throw new IllegalStateException("Missing industrial crops");
            var data=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var e:data.entrySet()){
                var v=e.getValue().getAsJsonObject();var breed=v.getAsJsonArray("breed");
                VARIETIES.put(e.getKey(),new Variety(e.getKey(),v.get("product").getAsString(),v.get("anchor").getAsString(),v.get("light").getAsInt(),v.get("tier").getAsInt(),breed.get(0).getAsString(),breed.get(1).getAsString()));
            }
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
        VARIETIES.put("cross_sticks",new Variety("cross_sticks","minecraft:air","",0,0,"",""));
        for(var v:VARIETIES.values()){
            var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_crop_"+v.id()));
            var block=new IndustrialCrop(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT).setId(key),v);
            CROPS.put(v.id(),Registry.register(BuiltInRegistries.BLOCK,key,block));
            String id=v.id().equals("cross_sticks")?"jimta_crop_sticks":"jimta_seed_"+v.id();
            var itemKey=ResourceKey.create(Registries.ITEM,Jugcraft.id(id));
            ITEMS.put(id,Registry.register(BuiltInRegistries.ITEM,itemKey,new Seed(block,new Item.Properties().setId(itemKey))));
        }
        GENES=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_crop_genes"),FabricBlockEntityTypeBuilder.create(Genes::new,CROPS.values().toArray(Block[]::new)).build());
        for(String id:List.of("coffee_beans","coffee_powder","hops","terra_wart","weed"))plain(id);
        var hydrationKey=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_hydrating_cell"));
        var hydration=Registry.register(BuiltInRegistries.ITEM,hydrationKey,new HydratingCell(new Item.Properties().setId(hydrationKey).stacksTo(1)));
        ITEMS.put("jimta_hydrating_cell",hydration);
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.ITEM.registerForItems((stack,context)->IndustrialProduction.Cell.storage(context),hydration);
        for(String id:List.of("coffee_mug","dark_coffee_mug","iced_coffee_mug","beer_mug","empty_mug","weed_ex","weeding_trowel")){
            var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_"+id));
            Item item=id.endsWith("coffee_mug")||id.equals("beer_mug")?new Drink(new Item.Properties().setId(key).stacksTo(16),id):new FarmTool(new Item.Properties().setId(key).stacksTo(id.equals("empty_mug")?16:1),id);
            ITEMS.put("jimta_"+id,Registry.register(BuiltInRegistries.ITEM,key,item));
        }
        CropMatron.register();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->ITEMS.values().forEach(out::accept));
    }
    private static void plain(String id){var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_"+id));
        Item item=id.equals("terra_wart")?new Item(new Item.Properties().setId(key)){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                if(level instanceof ServerLevel&&(player.hasEffect(net.minecraft.world.effect.MobEffects.POISON)||player.hasEffect(net.minecraft.world.effect.MobEffects.NAUSEA))){
                    player.removeEffect(net.minecraft.world.effect.MobEffects.POISON);player.removeEffect(net.minecraft.world.effect.MobEffects.NAUSEA);player.getItemInHand(hand).consume(1,player);return InteractionResult.SUCCESS;}
                return InteractionResult.PASS;
            }
        }:new Item(new Item.Properties().setId(key));
        ITEMS.put("jimta_"+id,Registry.register(BuiltInRegistries.ITEM,key,item));}
    public static ItemStack seed(IndustrialCrop crop,Genes genes){
        var s=new ItemStack(ITEMS.get(crop.variety.id().equals("cross_sticks")?"jimta_crop_sticks":"jimta_seed_"+crop.variety.id()));
        if(genes!=null)CustomData.update(DataComponents.CUSTOM_DATA,s,t->{t.putInt("Growth",genes.growth);t.putInt("Gain",genes.gain);t.putInt("Resistance",genes.resistance);});
        return s;
    }
    public static String describe(Genes genes){return "Growth "+genes.growth+" / Gain "+genes.gain+" / Resistance "+genes.resistance+" (0–31)";}
    public static final class Genes extends BlockEntity {
        public int growth=1,gain=1,resistance=1;public UUID owner;
        public Genes(BlockPos pos,BlockState state){super(GENES,pos,state);}
        public void inherit(Genes a,Genes b,RandomSource random){growth=mutate(a.growth,b.growth,random);gain=mutate(a.gain,b.gain,random);resistance=mutate(a.resistance,b.resistance,random);owner=a.owner;setChanged();}
        private static int mutate(int a,int b,RandomSource random){return Math.clamp((a+b)/2+random.nextInt(3)-1,0,31);}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putInt("Growth",growth);out.putInt("Gain",gain);out.putInt("Resistance",resistance);if(owner!=null)out.store("Owner",UUIDUtil.CODEC,owner);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);growth=Math.clamp(in.getIntOr("Growth",1),0,31);gain=Math.clamp(in.getIntOr("Gain",1),0,31);resistance=Math.clamp(in.getIntOr("Resistance",1),0,31);owner=in.read("Owner",UUIDUtil.CODEC).orElse(null);}
    }
    public static final class Seed extends BlockItem {
        Seed(Block b,Properties p){super(b,p);}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<Component> lines,TooltipFlag flag){
            super.appendHoverText(stack,context,display,lines,flag);var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            lines.accept(Component.literal("Growth "+tag.getIntOr("Growth",1)+" / Gain "+tag.getIntOr("Gain",1)+" / Resistance "+tag.getIntOr("Resistance",1)));
            var v=((IndustrialCrop)getBlock()).variety;
            if(v.id().equals("cross_sticks"))lines.accept(Component.literal("Place between two mature crops to crossbreed. Use the Crop Analyzer to inspect their genes."));
            else if(!v.anchor().isEmpty())lines.accept(Component.literal("Needs "+v.anchor()+" ore or a storage block 2–5 blocks below."));
        }
        @Override public InteractionResult place(BlockPlaceContext context){
            var original=context.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            var result=super.place(context);
            if(result.consumesAction()&&context.getLevel() instanceof ServerLevel level&&level.getBlockEntity(context.getClickedPos()) instanceof Genes genes){
                genes.growth=Math.clamp(original.getIntOr("Growth",1),0,31);genes.gain=Math.clamp(original.getIntOr("Gain",1),0,31);genes.resistance=Math.clamp(original.getIntOr("Resistance",1),0,31);genes.owner=context.getPlayer()==null?null:context.getPlayer().getUUID();genes.setChanged();
            }
            return result;
        }
    }
    public static final class IndustrialCrop extends CropBlock implements EntityBlock {
        public final Variety variety;
        IndustrialCrop(Properties p,Variety variety){super(p);this.variety=variety;}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Genes(pos,state);}
        @Override protected ItemLike getBaseSeedId(){return ITEMS.get(variety.id().equals("cross_sticks")?"jimta_crop_sticks":"jimta_seed_"+variety.id());}
        public boolean canRipen(ServerLevel level,BlockPos pos){
            if(level.getRawBrightness(pos,0)<variety.light())return false;
            if(variety.anchor().isEmpty())return true;
            for(int y=2;y<=5;y++){var at=pos.below(y);if(!level.hasChunkAt(at))return false;String id=BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).getPath();if(id.equals(variety.anchor()+"_block")||id.equals(variety.anchor()+"_ore")||id.equals("deepslate_"+variety.anchor()+"_ore"))return true;}
            return false;
        }
        @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
            if(!(level.getBlockEntity(pos) instanceof Genes genes))return;
            if(variety.id().equals("cross_sticks")){if(random.nextInt(6)==0)crossbreed(level,pos,genes,random);return;}
            if(!canRipen(level,pos)||getAge(state)>=7)return;
            int chance=Math.max(2,20+variety.tier()*3-genes.growth/2);
            var soil=level.getBlockState(pos.below());
            if(soil.hasProperty(FarmlandBlock.MOISTURE)&&soil.getValue(FarmlandBlock.MOISTURE)==0)chance+=Math.max(0,16-genes.resistance/2);
            if(random.nextInt(chance)==0)level.setBlock(pos,getStateForAge(getAge(state)+1),Block.UPDATE_CLIENTS);
        }
        @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity,InsideBlockEffectApplier effects,boolean moving){
            super.entityInside(state,level,pos,entity,effects,moving);
            if(!(level instanceof ServerLevel server)||!(entity instanceof LivingEntity target)||target.tickCount%20!=0||getAge(state)<5
                    ||!(level.getBlockEntity(pos) instanceof Genes genes)||!Set.of("venomilia","eatingplant").contains(variety.id()))return;
            var actor=Authority.answering(server,genes.owner);
            if(actor==null||!Authority.mayChange(server,actor,pos)||!ClaimTransfers.allowed(level,pos,target.blockPosition())
                    ||!io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects.mayHarm(actor,target)||!Authority.mayStrike(server,actor,genes.owner,target))return;
            if(target instanceof Player p&&p.isShiftKeyDown())return;
            if(variety.id().equals("venomilia"))target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,100));
            else target.hurtServer(server,server.damageSources().playerAttack(actor),2);
        }
        @Override public boolean isValidBonemealTarget(LevelReader level,BlockPos pos,BlockState state,BonemealSource source){
            return !variety.id().equals("cross_sticks")&&getAge(state)<7&&(!(level instanceof ServerLevel server)||canRipen(server,pos));
        }
        @Override public void performBonemeal(ServerLevel level,RandomSource random,BlockPos pos,BlockState state,BonemealSource source){
            if(!variety.id().equals("cross_sticks")&&canRipen(level,pos))level.setBlock(pos,getStateForAge(Math.min(7,getAge(state)+1)),Block.UPDATE_CLIENTS);
        }
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
            if(!(level instanceof ServerLevel server))return InteractionResult.SUCCESS;
            if(!Authority.mayChange(server,player,pos)||getAge(state)<7||variety.id().equals("cross_sticks"))return InteractionResult.PASS;
            Genes genes=level.getBlockEntity(pos) instanceof Genes g?g:null;
            Block.popResource(server,pos,product(genes));level.setBlock(pos,getStateForAge(2),Block.UPDATE_CLIENTS);
            return InteractionResult.SUCCESS;
        }
        private ItemStack product(Genes genes){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(variety.product())),1+(genes==null?0:genes.gain/12));}
        @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){
            var genes=builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof Genes g?g:null;
            var out=new ArrayList<ItemStack>();out.add(seed(this,genes));
            if(getAge(state)>=7&&!variety.id().equals("cross_sticks"))out.add(product(genes));
            return out;
        }
    }
    public static boolean crossbreed(ServerLevel level,BlockPos pos,Genes destination,RandomSource random){
        var parentCrops=new ArrayList<IndustrialCrop>();var parentGenes=new ArrayList<Genes>();
        var actor=Authority.answering(level,destination.owner);
        if(!Authority.mayChange(level,actor,pos))return false;
        for(Direction dir:Direction.Plane.HORIZONTAL){var at=pos.relative(dir);if(!level.hasChunkAt(at)||!ClaimTransfers.allowed(level,pos,at)||!Authority.mayChange(level,actor,at))continue;
            var state=level.getBlockState(at);if(state.getBlock() instanceof IndustrialCrop crop&&crop.getAge(state)>=5&&level.getBlockEntity(at) instanceof Genes genes&&!crop.variety.id().equals("cross_sticks")){parentCrops.add(crop);parentGenes.add(genes);}}
        if(parentCrops.size()<2)return false;
        var a=parentCrops.get(0);var b=parentCrops.get(1);var possibilities=new ArrayList<IndustrialCrop>();
        for(var crop:CROPS.values()){var v=crop.variety;if((v.parentA().equals(a.variety.id())&&v.parentB().equals(b.variety.id()))||(v.parentA().equals(b.variety.id())&&v.parentB().equals(a.variety.id())))possibilities.add(crop);}
        if(possibilities.isEmpty()){possibilities.add(a);possibilities.add(b);}
        var child=possibilities.get(random.nextInt(possibilities.size()));
        level.setBlock(pos,child.defaultBlockState(),Block.UPDATE_ALL);
        if(level.getBlockEntity(pos) instanceof Genes genes){genes.inherit(parentGenes.get(0),parentGenes.get(1),random);genes.owner=destination.owner;genes.setChanged();}
        return true;
    }
    static final class FarmTool extends Item {
        final String id;FarmTool(Properties p,String id){super(p);this.id=id;}
        @Override public InteractionResult useOn(UseOnContext ctx){
            if(!(ctx.getLevel() instanceof ServerLevel level))return InteractionResult.SUCCESS;
            if(ctx.getPlayer()==null||!Authority.mayChange(level,ctx.getPlayer(),ctx.getClickedPos()))return InteractionResult.FAIL;
            var state=level.getBlockState(ctx.getClickedPos());
            if(!(state.getBlock() instanceof IndustrialCrop crop)||!(level.getBlockEntity(ctx.getClickedPos()) instanceof Genes genes))return InteractionResult.PASS;
            if(id.equals("weed_ex")){if(!crop.variety.id().equals("weed"))return InteractionResult.PASS;level.setBlock(ctx.getClickedPos(),CROPS.get("cross_sticks").defaultBlockState(),Block.UPDATE_ALL);ctx.getItemInHand().consume(1,ctx.getPlayer());return InteractionResult.SUCCESS;}
            if(id.equals("weeding_trowel")){Block.popResource(level,ctx.getClickedPos(),seed(crop,genes));level.setBlock(ctx.getClickedPos(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);return InteractionResult.SUCCESS;}
            return InteractionResult.PASS;
        }
    }
    static final class Drink extends Item {
        final String id;Drink(Properties p,String id){super(p);this.id=id;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            var stack=player.getItemInHand(hand);if(player.getCooldowns().isOnCooldown(stack))return InteractionResult.FAIL;
            player.startUsingItem(hand);return InteractionResult.CONSUME;
        }
        @Override public ItemUseAnimation getUseAnimation(ItemStack stack){return ItemUseAnimation.DRINK;}
        @Override public int getUseDuration(ItemStack stack,LivingEntity user){return 32;}
        @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user){
            if(!(level instanceof ServerLevel)||!(user instanceof Player player))return stack;
            player.getCooldowns().addCooldown(stack,200);player.getFoodData().eat(2,.2F);
            var effect=id.equals("beer_mug")?net.minecraft.world.effect.MobEffects.REGENERATION:net.minecraft.world.effect.MobEffects.SPEED;
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect,600,id.equals("dark_coffee_mug")?1:0));
            if(!player.hasInfiniteMaterials()){stack.shrink(1);var empty=new ItemStack(ITEMS.get("jimta_empty_mug"));if(stack.isEmpty())return empty;if(!player.addItem(empty))player.drop(empty,false,net.minecraft.util.Prediction.SERVER_ONLY);}
            return stack;
        }
    }
}
