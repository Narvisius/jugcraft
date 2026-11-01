package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import java.util.*;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** Original Amendments placement mechanics, integrated with existing items and land permissions. */
public final class OwnerAmendments {
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static BlockEntityType<OwnerAmendmentData> DATA;
    public static Item DYE_BOTTLE;
    private OwnerAmendments(){}
    public static void register(){
        for(var e:OwnerBackpacks.source("newest_amends.json").getAsJsonObject("blocks").entrySet()){
            String name=e.getKey();var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("amends_"+name));
            var props=BlockBehaviour.Properties.of().strength(.5F).noOcclusion().setId(key);
            if(name.contains("lantern"))props.lightLevel(s->name.contains("soul")?10:15).sound(SoundType.LANTERN);
            if(name.contains("carpet"))props.lightLevel(s->s.getValue(OwnerCarpetBlocks.LIGHT)).sound(SoundType.WOOL);
            if(name.startsWith("skull_candle"))props.lightLevel(s->HouseholdBlock.flag(s,"lit")?3*Integer.parseInt(HouseholdBlock.value(s,"candles")):0);
            if(name.startsWith("ceiling_banner"))props.noCollision();
            if(name.contains("cake"))props.sound(SoundType.WOOL);
            Block block=name.equals("carpet_slab")?new OwnerCarpetBlocks.Slab(props):name.equals("carpet_stairs")?new OwnerCarpetBlocks.Stairs(props):HouseholdBlock.create(props,e.getValue().getAsJsonObject());
            Registry.register(BuiltInRegistries.BLOCK,key,block);BLOCKS.put(name,block);
        }
        DATA=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("amends_placement"),FabricBlockEntityTypeBuilder.create(OwnerAmendmentData::new,BLOCKS.values().stream().filter(b->!(b instanceof OwnerAmendmentBlock a&&(a.name.contains("cake")||a.name.equals("water_lily_pad")))).toArray(Block[]::new)).build());
        var dyeKey=ResourceKey.create(Registries.ITEM,Jugcraft.id("amends_dye_bottle"));DYE_BOTTLE=Registry.register(BuiltInRegistries.ITEM,dyeKey,new OwnerAmendmentDyes(new Item.Properties().setId(dyeKey)));
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("amends_dye_bottle"),OwnerAmendmentDyes.Recipe.SERIALIZER);
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(e->e.accept(DYE_BOTTLE));
        OwnerAmendmentCharge.register();
        OwnerLecternEditing.register();
        UseBlockCallback.EVENT.register(OwnerAmendments::use);
    }
    public static BlockState copy(BlockState from,BlockState to){
        for(var p:from.getProperties())if(to.getBlock().getStateDefinition().getProperty(p.getName())!=null)to=HouseholdBlock.set(to,p.getName(),value(from,p));
        return to;
    }
    @SuppressWarnings({"rawtypes","unchecked"}) private static String value(BlockState s,net.minecraft.world.level.block.state.properties.Property p){return p.getName(s.getValue(p));}
    static boolean tool(ItemStack stack){return stack.is(ItemTags.SWORDS)||stack.is(ItemTags.PICKAXES)||stack.is(ItemTags.AXES)||stack.is(ItemTags.SHOVELS)||stack.is(ItemTags.HOES)||stack.is(Items.TRIDENT)||stack.is(Items.SHEARS)||stack.is(Items.FISHING_ROD);}
    static BlockState potted(ItemStack item){
        if(!(item.getItem() instanceof BlockItem plant))return null;
        for(var block:BuiltInRegistries.BLOCK)if(block instanceof FlowerPotBlock pot&&pot.getPotted()==plant.getBlock())return block.defaultBlockState();
        return null;
    }
    static InteractionResult use(Player player,Level level,InteractionHand hand,BlockHitResult hit){
        if(player.isSpectator())return InteractionResult.PASS;
        var sign=OwnerSignDisplay.use(player,level,hand,hit);if(sign!=InteractionResult.PASS)return sign;
        var lily=OwnerLilySupport.place(player,level,hand,hit);if(lily!=InteractionResult.PASS)return lily;
        var cauldron=OwnerAmendmentCauldrons.use(player,level,hand,hit);if(cauldron!=InteractionResult.PASS)return cauldron;
        var pos=hit.getBlockPos();var old=level.getBlockState(pos);var held=player.getItemInHand(hand);var face=hit.getDirection();
        if(player.isShiftKeyDown()&&held.isEmpty()&&level.getBlockEntity(pos) instanceof LecternBlockEntity lectern&&lectern.getBook().is(Items.WRITABLE_BOOK)){
            if(!Menagerie.mayChange(level,player,pos))return InteractionResult.FAIL;
            if(player instanceof net.minecraft.server.level.ServerPlayer server&&!OwnerLecternEditing.open(server,lectern))return InteractionResult.FAIL;
            return InteractionResult.SUCCESS;
        }
        if(old.getBlock() instanceof OwnerAmendmentBlock||level.getBlockEntity(pos) instanceof OwnerAmendmentData)return InteractionResult.PASS;
        if(old.is(Blocks.CAKE)&&held.isEmpty()&&player.canEat(false)){
            if(!Menagerie.mayChange(level,player,pos))return InteractionResult.FAIL;
            if(level.isClientSide())return InteractionResult.SUCCESS;
            var next=HouseholdBlock.set(BLOCKS.get("directional_cake").defaultBlockState(),"bites",Integer.toString(old.getValue(CakeBlock.BITES)));
            next=HouseholdBlock.set(next,"facing",player.getDirection().getOpposite().getName());
            if(!level.setBlock(pos,next,3))return InteractionResult.FAIL;
            return ((OwnerAmendmentBlock)next.getBlock()).useWithoutItem(next,level,pos,player,hit);
        }
        BlockState replacement=null,decoration=Blocks.AIR.defaultBlockState();ItemStack display=ItemStack.EMPTY,secondary=ItemStack.EMPTY;
        if(old.getBlock() instanceof AbstractSkullBlock&&level.getBlockEntity(pos) instanceof SkullBlockEntity skull
                &&held.getItem() instanceof BlockItem addon&&(addon.getBlock() instanceof AbstractSkullBlock||addon.getBlock() instanceof CandleBlock)){
            boolean candle=addon.getBlock() instanceof CandleBlock;
            if(!candle&&!(old.getBlock() instanceof SkullBlock))return InteractionResult.PASS;
            replacement=copy(old,BLOCKS.get(candle?(old.getBlock() instanceof WallSkullBlock?"skull_candle_wall":"skull_candle"):"skull_pile").defaultBlockState());
            display=new ItemStack(old.getBlock());display.applyComponents(skull.collectComponents());
            if(candle)decoration=addon.getBlock().defaultBlockState();else secondary=held.copyWithCount(1);
        }else if(held.getItem() instanceof BannerItem banner&&face==Direction.DOWN){
            pos=pos.below();if(!level.hasChunkAt(pos)||!level.getBlockState(pos).canBeReplaced()||!level.hasChunkAt(pos.below())||!level.getBlockState(pos.below()).canBeReplaced()||!Menagerie.mayChange(level,player,pos.below()))return InteractionResult.PASS;
            replacement=HouseholdBlock.set(BLOCKS.get("ceiling_banner_"+banner.getColor().getName()).defaultBlockState(),"rotation",Integer.toString(Math.floorMod(Math.round(player.getYRot()*16/360F),16)));old=Blocks.AIR.defaultBlockState();display=held.copyWithCount(1);
        }else if(old.is(Blocks.TRIPWIRE_HOOK)&&!old.getValue(TripWireHookBlock.ATTACHED)&&!old.getValue(TripWireHookBlock.POWERED)&&tool(held)){
            replacement=copy(old,BLOCKS.get("tool_hook").defaultBlockState());display=held.copyWithCount(1);
        }else if(held.is(ItemTags.WOOL_CARPETS)&&held.getItem() instanceof BlockItem carpet&&!old.hasBlockEntity()){
            if(old.getBlock() instanceof SlabBlock&&old.getValue(SlabBlock.TYPE)==SlabType.BOTTOM)replacement=copy(old,BLOCKS.get("carpet_slab").defaultBlockState());
            if(old.getBlock() instanceof StairBlock&&old.getValue(StairBlock.HALF)==Half.BOTTOM)replacement=copy(old,BLOCKS.get("carpet_stairs").defaultBlockState());
            if(replacement!=null){if(face==Direction.DOWN||!level.getBlockState(pos.above()).canBeReplaced())return InteractionResult.PASS;decoration=carpet.getBlock().defaultBlockState();String color=BuiltInRegistries.BLOCK.getKey(carpet.getBlock()).getPath().replace("_carpet","");replacement=replacement.setValue(OwnerCarpetBlocks.COLOR,DyeColor.byName(color,DyeColor.WHITE));replacement=replacement.setValue(OwnerCarpetBlocks.LIGHT,old.getLightEmission());}
        }else if(old.is(Blocks.CAKE)&&old.getValue(CakeBlock.BITES)==0&&held.is(Items.CAKE)&&face==Direction.UP){
            replacement=HouseholdBlock.set(BLOCKS.get("double_cake").defaultBlockState(),"facing",player.getDirection().getOpposite().getName());
        }else if(held.is(Items.FLOWER_POT)&&face==Direction.DOWN){
            pos=pos.below();if(!level.hasChunkAt(pos)||!level.getBlockState(pos).canBeReplaced())return InteractionResult.FAIL;
            replacement=BLOCKS.get("hanging_flower_pot").defaultBlockState();old=Blocks.FLOWER_POT.defaultBlockState();
        }else if((held.is(Items.LANTERN)||held.is(Items.SOUL_LANTERN))&&face.getAxis().isHorizontal()){
            pos=pos.relative(face);if(!level.hasChunkAt(pos)||!level.getBlockState(pos).canBeReplaced())return InteractionResult.PASS;
            replacement=HouseholdBlock.set(BLOCKS.get(held.is(Items.SOUL_LANTERN)?"wall_soul_lantern":"wall_lantern").defaultBlockState(),"facing",face.getName());
            var support=level.getBlockState(hit.getBlockPos());replacement=HouseholdBlock.set(replacement,"attachment",support.getBlock() instanceof FenceBlock?"post":support.getBlock() instanceof WallBlock?"wall":support.getBlock() instanceof IronBarsBlock?"beam":"block");
            old=(held.is(Items.SOUL_LANTERN)?Blocks.SOUL_LANTERN:Blocks.LANTERN).defaultBlockState();
        }else if(old.is(Blocks.IRON_CHAIN)&&held.isEmpty())return ringChain(player,level,pos);
        if(replacement==null)return InteractionResult.PASS;
        if(!level.hasChunkAt(pos)||!Menagerie.mayChange(level,player,pos)||!player.mayUseItemAt(pos,face,held))return InteractionResult.FAIL;
        replacement=HouseholdBlock.set(replacement,"waterlogged",Boolean.toString(level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)));
        if(!replacement.canSurvive(level,pos))return InteractionResult.FAIL;
        if(!level.isClientSide()){
            if(!level.setBlock(pos,replacement,3))return InteractionResult.FAIL;
            if(level.getBlockEntity(pos) instanceof OwnerAmendmentData data){data.base=old;data.decoration=decoration;data.displayed=display;data.secondary=secondary;data.changed();}
            held.consume(1,player);var sound=old.getSoundType();level.playSound(null,pos,sound.getPlaceSound(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        }
        return InteractionResult.SUCCESS;
    }
    private static InteractionResult ringChain(Player player,Level level,BlockPos pos){
        for(var direction:new Direction[]{Direction.UP,Direction.DOWN})for(int i=1;i<=16;i++){
            var target=pos.relative(direction,i);if(!level.hasChunkAt(target))break;var state=level.getBlockState(target);
            if(state.getBlock() instanceof BellBlock bell){
                if(!Menagerie.mayChange(level,player,target))return InteractionResult.FAIL;
                if(!level.isClientSide())bell.attemptToRing(player,level,target,player.getDirection());return InteractionResult.SUCCESS;
            }
            if(!state.is(Blocks.IRON_CHAIN))break;
        }return InteractionResult.PASS;
    }
}
