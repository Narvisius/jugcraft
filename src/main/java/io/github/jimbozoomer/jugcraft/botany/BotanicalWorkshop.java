package io.github.jimbozoomer.jugcraft.botany;
import net.minecraft.world.level.ClipContext;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauRecipes;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import org.jspecify.annotations.Nullable;

/** Physical offerings and mana accounting. Every world mutation rechecks the saved operator. */
public final class BotanicalWorkshop extends BlockEntity implements WorldlyContainer,Nameable {
    @Override protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (level == null || !BotanicalGenerators.TYPES.contains(kind())) return;
        var out = TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
        out.putString("kind", kind());
        generator.save(out);
        harvest.save(out);
        components.set(BotanicalFlowerMemory.TYPE, net.minecraft.world.item.component.CustomData.of(out.buildResult()));
    }

    @Override protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter components) {
        super.applyImplicitComponents(components);
        var memory = components.get(BotanicalFlowerMemory.TYPE);
        if (memory == null || level == null || !BotanicalGenerators.TYPES.contains(kind())) return;
        var in = TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), memory.copyTag());
        if (!kind().equals(in.getStringOr("kind", ""))) return;
        generator.load(in);
        harvest.load(in);
        setChanged();
    }
    private NonNullList<ItemStack> items=NonNullList.withSize(17,ItemStack.EMPTY);
    private @Nullable UUID owner;
    private @Nullable BlockPos link;
    private @Nullable BlockPos flowerOrigin;
    public final BotanicalLenses lenses=new BotanicalLenses(this);
    public final BotanicalPotato potato=new BotanicalPotato(this);
    public final BotanicalCacophonium cacophonium=new BotanicalCacophonium(this);
    public final BotanicalPlatforms platforms=new BotanicalPlatforms(this);
    public final BotanicalRedString redString=new BotanicalRedString(this);
    final io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage flux=new io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage(3840,0,480,this::setChanged);
    public boolean pumpActive;public int pumpSignal;
    private int mana,burn;private long lastDrum=Long.MIN_VALUE;
    private float rotatedYaw,rotatedPitch;private boolean rotatedAim;
    private boolean water,tabletReceive=true,redstoneHeld,redstonePending;
    private final int[] daisyTime=new int[8];
    private final BlockState[] daisyState=new BlockState[8];
    private final String[] daisyRecipe=new String[8];
    private int transferTicks,flightMana;
    private @Nullable BlockPos flightTarget;
    final BotanicalPortal portal=new BotanicalPortal(this);
    final BotanicalGenerators generator=new BotanicalGenerators(this);
    final BotanicalHarvest harvest=new BotanicalHarvest(this);
    final BotanicalBubble bubble=new BotanicalBubble(this);
    final BotanicalLife life=new BotanicalLife(this);
    final BotanicalCrates crates=new BotanicalCrates(this);
    final BotanicalEnchanter enchanter=new BotanicalEnchanter(this);
    final BotanicalIncense incense=new BotanicalIncense(this);
    final BotanicalMeter meter=new BotanicalMeter();
    public final BotanicalSparks sparks=new BotanicalSparks(this);
    public final BotanicalLuminizer luminizer=new BotanicalLuminizer(this);
    public final BotanicalAvatar avatar=new BotanicalAvatar(this);
    public final CorporeaDevices corporea=new CorporeaDevices(this);
    public final BotanicalDevices devices=new BotanicalDevices(this);
    public final BotanicalMechanisms mechanisms=new BotanicalMechanisms(this);
    final BotanicalFunctions function=new BotanicalFunctions(this);
    public BotanicalWorkshop(BlockPos pos,BlockState state){super(Jugtania.WORKSHOP,pos,state);}
    @Override public void setLevel(Level level){super.setLevel(level);BotanicalPlanets.track(this);BotanicalFields.track(this);BotanicalSparks.track(this);}
    @Override public void setRemoved(){BotanicalPlanets.forget(this);BotanicalFields.forget(this);BotanicalSparks.forget(this);super.setRemoved();}
    public ServerPlayer actor(){return level instanceof ServerLevel s?Authority.answering(s,owner):null;}
    public String kind(){return ((BotanicalBlock)getBlockState().getBlock()).functionKind();}
    public void owner(UUID id){owner=id;setChanged();}
    /** Effect origin can move; block identity, inventory, ownership and mana links never move. */
    public BlockPos effectivePos(){return flowerOrigin==null?worldPosition:flowerOrigin;}
    private void refreshFlowerOrigin(){
        BlockPos next=null;
        if((generating()||functional()||kind().equals("pure_daisy")||kind().equals("manastar"))&&level instanceof ServerLevel s){
            var actor=actor();var below=worldPosition.below();
            if(actor!=null&&permitted(actor)&&s.hasChunkAt(below)&&s.getBlockEntity(below) instanceof BotanicalWorkshop relay&&relay.kind().equals("red_string_relay")&&relay.permitted(actor)){
                var destination=relay.redString.resolve();
                if(destination!=null&&Authority.mayChange(s,actor,destination)&&io.github.jimbozoomer.jugcraft.finance.ClaimTransfers.allowed(s,worldPosition,destination))next=destination;
            }
        }
        if(!Objects.equals(next,flowerOrigin)){flowerOrigin=next;Arrays.fill(daisyTime,0);changed();}
    }
    @Override public Component getName(){return hasCustomName()?potato.name():getBlockState().getBlock().getName();}
    @Override public Component getCustomName(){return kind().equals("tiny_potato")&&!potato.name().getString().isEmpty()?potato.name():null;}
    public int mana(){return mana;}
    void triggerDrum(ServerPlayer actor){long now=actor.level().getGameTime();if(lastDrum!=Long.MIN_VALUE&&now>=lastDrum&&now-lastDrum<20)return;lastDrum=now;BotanicalHorns.drum(this,actor);setChanged();}
    public Vec3 aim(){if(rotatedAim)return new Vec3(-Math.sin(Math.toRadians(rotatedYaw))*Math.cos(Math.toRadians(rotatedPitch)),Math.sin(Math.toRadians(rotatedPitch)),-Math.cos(Math.toRadians(rotatedYaw))*Math.cos(Math.toRadians(rotatedPitch)));return link==null?new Vec3(0,0,-1):Vec3.atCenterOf(link).subtract(Vec3.atCenterOf(worldPosition)).normalize();}
    void pointAt(Vec3 target){var vector=target.subtract(Vec3.atCenterOf(worldPosition));rotatedAim=true;rotatedYaw=(float)Math.toDegrees(Math.atan2(-vector.x,-vector.z));rotatedPitch=(float)Math.toDegrees(Math.atan2(vector.y,Math.sqrt(vector.x*vector.x+vector.z*vector.z)));link=null;changed();}
    public boolean generating(){return BotanicalGenerators.TYPES.contains(kind());}
    public boolean functional(){return BotanicalFunctions.TYPES.contains(kind());}
    int generate(int amount){int accepted=Math.clamp(amount,0,capacity()-mana-flightMana);mana+=accepted;if(accepted>0)setChanged();return accepted;}
    boolean draw(int amount){if(amount<0||mana<amount)return false;mana-=amount;changed();return true;}
    int fill(int amount){int accepted=Math.clamp(amount,0,capacity()-mana-flightMana);mana+=accepted;if(accepted>0)changed();return accepted;}
    int freeMana(){return capacity()-mana-flightMana;}
    public int capacity(){return kind().equals("avatar")?6400:kind().equals("spawner_claw")?480:kind().equals("enchanter")?20000000:kind().equals("diluted_pool")?10000:generating()?BotanicalGenerators.capacity(kind()):functional()?BotanicalFunctions.capacity(kind()):kind().contains("spreader")?6400:1000000;}
    public boolean permitted(ServerPlayer p){return level instanceof ServerLevel s&&Authority.mayChange(s,p,worldPosition);}
    public void changed(){setChanged();if(level!=null){boolean filled=kind().equals("incense_plate")?incense.burning():kind().equals("alfheim_portal")?portal.active:kind().startsWith("apothecary")?water:mana>0;if(getBlockState().getValue(BotanicalBlock.FILLED)!=filled)level.setBlock(worldPosition,getBlockState().setValue(BotanicalBlock.FILLED,filled),Block.UPDATE_CLIENTS);level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());}}
    public String status(){if(kind().equals("mana_fluxfield"))return "Fluxfield: "+flux.getAmount()+" / 3840 JE · 3 JE per mana · 480 JE/tick";if(kind().equals("pump"))return "Rail → pump → pool · Wand reverses transfer · Redstone pauses";if(kind().equals("enchanter"))return enchanter.status()+sparks.status();if(BotanicalMechanisms.handles(kind()))return mechanisms.status();int count=0;for(int i=0;i<16;i++)count+=items.get(i).getCount();return (kind().startsWith("apothecary")?(water?"Water filled":"Needs water"):"Mana: "+mana+" / "+capacity())+" · Offerings: "+count+(link==null?"":" · Linked: "+link.toShortString())+(items.get(16).isEmpty()?"":" · Result ready")+generator.status()+sparks.status()+(kind().equals("manastar")?meter.status():"");}
    void unbind(){link=null;changed();}
    public boolean bind(ServerPlayer p,BlockPos target){
        if(BotanicalLuminizer.TYPES.contains(kind()))return luminizer.bind(p,target);
        if(!(level instanceof ServerLevel s)||!permitted(p)||target.equals(worldPosition)||target.distSqr(worldPosition)>(kind().contains("spreader")?4096:256)||!s.hasChunkAt(target)||!(s.getBlockEntity(target) instanceof BotanicalWorkshop other)||!other.permitted(p))return false;
        if(!io.github.jimbozoomer.jugcraft.finance.ClaimTransfers.allowed(s,worldPosition,target))return false;
        if(!generating()&&!functional()&&!kind().contains("spreader"))return false;
        if(functional()&&!other.kind().endsWith("pool"))return false;
        if(generating()&&!other.kind().contains("spreader"))return false;
        if(kind().contains("spreader")&&!BotanicalRouting.receiver(other.kind()))return false;
        owner=p.getUUID();link=target.immutable();rotatedAim=false;changed();return true;
    }
    public InteractionResult use(ServerPlayer p,InteractionHand hand){
        if(!permitted(p))return InteractionResult.FAIL;if(owner==null)owner=p.getUUID();var stack=p.getItemInHand(hand);
        if(kind().contains("spreader")||kind().equals("prism")){var result=lenses.use(p,hand);if(result!=InteractionResult.PASS)return result;}
        if(BotanicalSparks.supports(kind())){var result=sparks.use(p,hand);if(result!=InteractionResult.PASS)return result;}
        if(BotanicalRedString.TYPES.contains(kind()))return redString.use(p,hand);
        if(BotanicalLuminizer.TYPES.contains(kind()))return luminizer.use(p,hand);
        if(kind().equals("cacophonium_block")){cacophonium.play(p.level());return InteractionResult.SUCCESS;}
        if(kind().equals("avatar"))return avatar.use(p,hand);
        if(CorporeaDevices.TYPES.contains(kind()))return corporea.use(p,hand);
        if(BotanicalDevices.TYPES.contains(kind()))return devices.use(p,hand);
        if(BotanicalCrates.handles(kind()))return crates.use(p,hand);
        if(kind().equals("enchanter"))return enchanter.use(p,hand);
        if(kind().equals("incense_plate"))return incense.use(p,hand);
        if(BotanicalMechanisms.handles(kind()))return mechanisms.use(p,hand);
        if(stack.isEmpty()){
            if(kind().equals("daffomill")&&p.isShiftKeyDown()){function.wind.rotate(p);return InteractionResult.SUCCESS;}
            if(kind().startsWith("rannuncarpus")&&p.isShiftKeyDown()){function.logistics.toggle(p);return InteractionResult.SUCCESS;}
            if(kind().equals("pollidisiac")&&p.isShiftKeyDown()){function.husbandry.cycle(p);return InteractionResult.SUCCESS;}
            if(kind().startsWith("hopperhock")&&p.isShiftKeyDown()){function.cycleFilter(p);return InteractionResult.SUCCESS;}
            int slot=!items.get(16).isEmpty()?16:-1;if(slot<0&&p.isShiftKeyDown())for(int i=15;i>=0;i--)if(!items.get(i).isEmpty()){slot=i;break;}
            if(slot>=0){var out=items.get(slot);items.set(slot,ItemStack.EMPTY);p.getInventory().placeItemBackInInventory(out,net.minecraft.util.Prediction.SERVER_ONLY);changed();}
            else p.sendOverlayMessage(Component.literal(status()));return InteractionResult.SUCCESS;
        }
        if(kind().startsWith("apothecary")&&(stack.is(Items.WATER_BUCKET)||stack.is(Jugtania.item("water_bowl")))&&!water){water=true;p.setItemInHand(hand,ItemUtils.createFilledResult(stack,p,new ItemStack(stack.is(Items.WATER_BUCKET)?Items.BUCKET:Items.BOWL)));changed();return InteractionResult.SUCCESS;}
        if(kind().endsWith("pool")&&BotanicalTreasures.lotus(stack)>0)return BotanicalTreasures.dissolve(this,stack)?InteractionResult.SUCCESS:InteractionResult.FAIL;
        if(kind().endsWith("pool")&&BotanicalManaItems.capacity(stack)>0){
            int stored=BotanicalManaItems.stored(stack);int moved=tabletReceive?Math.min(mana,BotanicalManaItems.capacity(stack)-stored):stack.is(Jugtania.item("terra_pick"))?0:Math.min(stored,capacity()-mana);
            if(moved>0){BotanicalManaItems.stored(stack,stored+(tabletReceive?moved:-moved));mana+=tabletReceive?-moved:moved;changed();}
            p.sendOverlayMessage(Component.literal("Stored: "+BotanicalManaItems.stored(stack)+" / "+BotanicalManaItems.capacity(stack)+" mana"));return InteractionResult.SUCCESS;
        }
        if(kind().startsWith("apothecary")&&water){
            for(var r:JugtaniaRecipes.of("petal_apothecary"))if(r.data().has("reagent")&&NouveauRecipes.ingredient(r.data().get("reagent"),stack)){
                if(craft(r,true)){if(!p.getAbilities().instabuild)stack.shrink(1);water=false;changed();return InteractionResult.SUCCESS;}
            }
        }
        if(kind().equals("runic_altar")&&stack.is(Jugtania.item("livingrock"))){
            for(var r:JugtaniaRecipes.of("runic_altar"))if(craft(r,true)){if(!p.getAbilities().instabuild)stack.shrink(1);changed();return InteractionResult.SUCCESS;}
        }
        if(!acceptsOfferings())return InteractionResult.PASS;
        for(int i=0;i<16;i++)if(items.get(i).isEmpty()&&canPlaceItem(i,stack)){items.set(i,stack.copyWithCount(1));if(!p.getAbilities().instabuild)stack.shrink(1);changed();return InteractionResult.SUCCESS;}
        p.sendOverlayMessage(Component.literal("Offerings are full. Sneak with an empty hand to take one back."));return InteractionResult.FAIL;
    }
    public void wand(ServerPlayer p){if(!permitted(p))return;if(kind().equals("pump")){level.setBlockAndUpdate(worldPosition,getBlockState().setValue(BotanicalTransport.Pump.FACING,getBlockState().getValue(BotanicalTransport.Pump.FACING).getOpposite()));return;}if(owner==null)owner=p.getUUID();if(BotanicalPlatforms.TYPES.contains(kind())){platforms.wand(p);return;}if(CorporeaDevices.TYPES.contains(kind())){corporea.wand(p);return;}if(BotanicalDevices.TYPES.contains(kind())){devices.wand(p);return;}if(BotanicalCrates.handles(kind())){crates.wand(p);return;}if(kind().equals("enchanter")){enchanter.wand(p);return;}if(mechanisms.wand(p))return;if(kind().equals("alfheim_portal")){portal.activate(p);return;}if(kind().endsWith("pool")&&p.isShiftKeyDown()){tabletReceive=!tabletReceive;changed();p.sendOverlayMessage(Component.literal(tabletReceive?"Pool → tablet":"Tablet → pool"));}else p.sendOverlayMessage(Component.literal(status()));}
    private boolean acceptsOfferings(){return kind().equals("brewery")||kind().startsWith("apothecary")||kind().endsWith("pool")||kind().equals("runic_altar")||kind().equals("terra_plate")||kind().equals("endoflame")||kind().equals("alfheim_portal");}
    boolean craft(JugtaniaRecipes.Recipe recipe,boolean exact){
        if(!(level instanceof ServerLevel s)||mana<recipe.mana()||!recipe.catalyst(s.getBlockState(worldPosition.below())))return false;
        int[] used=recipe.match(items.subList(0,16));if(used==null)return false;
        if(exact)for(int i=0;i<16;i++)if(used[i]!=items.get(i).getCount())return false;
        var output=recipe.output();if(output.isEmpty())return false;var old=items.get(16);
        if(!old.isEmpty()&&(!ItemStack.isSameItemSameComponents(old,output)||old.getCount()+output.getCount()>old.getMaxStackSize()))return false;
        // Reserve all remainders before committing; runes are reusable altar catalysts.
        var after=new ArrayList<ItemStack>();
        for(int i=0;i<16;i++){
            var input=items.get(i);var left=input.copy();boolean rune=recipe.type().equals("runic_altar")&&net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(input.getItem()).getPath().startsWith("jugtania_rune_");
            if(!rune)left.shrink(used[i]);
            if(used[i]>0&&!rune&&input.getItem().getCraftingRemainder()!=null){if(!left.isEmpty())return false;left=input.getItem().getCraftingRemainder().create().copyWithCount(used[i]);}
            after.add(left);
        }
        for(int i=0;i<16;i++)items.set(i,after.get(i));if(old.isEmpty())items.set(16,output);else old.grow(output.getCount());mana-=recipe.mana();
        s.sendParticles(ParticleTypes.ENCHANT,worldPosition.getX()+.5,worldPosition.getY()+.8,worldPosition.getZ()+.5,12,.3,.3,.3,.1);changed();return true;
    }
    public static void tick(Level raw,BlockPos pos,BlockState state,BotanicalWorkshop b){
        b.refreshFlowerOrigin();
        step(raw,pos,state,b);
        // Source overgrowth runs an additional flower step; floating flowers and Hydroangeas are excluded.
        if(b.flowerOrigin==null&&!b.isRemoved()&&raw.getBlockEntity(pos)==b&&!((BotanicalBlock)state.getBlock()).floating()&&!b.kind().equals("hydroangeas")&&(b.generating()||b.functional()||b.kind().equals("pure_daisy"))&&raw.getBlockState(pos.below()).is(Jugtania.block("enchanted_soil")))step(raw,pos,b.getBlockState(),b);
    }
    private static void step(Level raw,BlockPos pos,BlockState state,BotanicalWorkshop b){
        if(!(raw instanceof ServerLevel s))return;
        if(b.kind().equals("fake_air")){b.bubble.restoreWhenInactive(s);return;}
        var actor=Authority.answering(s,b.owner);if(actor==null||!b.permitted(actor))return;
        if(BotanicalRedString.TYPES.contains(b.kind())){b.redString.tick(s,actor);return;}
        if(b.kind().equals("cacophonium_block")){b.cacophonium.tick(s);return;}
        if(b.kind().equals("mana_fluxfield")){BotanicalTransport.flux(b,s,actor);return;}
        if(b.kind().equals("pump")){BotanicalTransport.pump(b,s,actor);return;}
        if(b.kind().equals("tiny_potato")){b.potato.tick(s,actor);return;}
        if(b.kind().equals("creative_pool")&&b.mana<b.capacity()){b.mana=b.capacity();b.changed();}
        b.sparks.tick(s,actor);
        if(b.kind().equals("light_launcher")){BotanicalOddities.launch(b,s,actor);return;}
        if(BotanicalLuminizer.TYPES.contains(b.kind())){b.luminizer.tick(s,actor);return;}
        if(b.kind().equals("avatar")){b.avatar.tick(s,actor);return;}
        if(CorporeaDevices.TYPES.contains(b.kind())){b.corporea.tick(s,actor);return;}
        if(BotanicalDevices.TYPES.contains(b.kind())){b.devices.tick(s,actor);return;}
        if(BotanicalMechanisms.handles(b.kind())){b.mechanisms.tick(s,actor);return;}
        if(BotanicalCrates.handles(b.kind())){b.crates.tick(s,actor);return;}
        if(b.kind().equals("enchanter")){b.enchanter.tick(s,actor);return;}
        if(b.kind().equals("incense_plate")){b.incense.tick(s,actor);return;}
        if(b.kind().equals("manastar")){b.meter.tick(b,s,actor);return;}
        if(b.kind().equals("mana_distributor")){BotanicalRouting.split(b,s,actor);return;}
        if(b.kind().equals("alfheim_portal")){b.portal.tick(s,actor);return;}
        if(b.kind().equals("pure_daisy")){if(s.getGameTime()%20==0)b.daisy(s,actor);return;}
        if(b.kind().equals("endoflame")){
            if(b.burn>0){b.burn--;if(s.getGameTime()%2==0)b.mana=Math.min(b.capacity(),b.mana+3);b.setChanged();}
            if(s.getGameTime()%10==0){b.transfer(s,actor);if(b.burn==0&&b.mana<b.capacity())b.fuel(s,actor);}
            if(b.burn>0&&s.getGameTime()%10==0)s.sendParticles(ParticleTypes.FLAME,pos.getX()+.5,pos.getY()+.7,pos.getZ()+.5,1,.15,.1,.15,0);
            return;
        }
        if(b.kind().contains("spreader")){b.transfer(s,actor);return;}
        if(b.functional()){
            if(s.getGameTime()%10==0&&b.link!=null&&s.hasChunkAt(b.link)&&s.getBlockEntity(b.link) instanceof BotanicalWorkshop pool&&pool.kind().endsWith("pool")&&pool.permitted(actor)&&b.clearLine(s,b.link)){
                int amount=Math.min(b.capacity()-b.mana,pool.mana);if(amount>0&&pool.draw(amount))b.fill(amount);
            }b.function.tick(s,actor);return;
        }
        if(b.generating()){b.generator.tick(s,actor);if(!b.isRemoved()&&s.getGameTime()%10==0)b.transfer(s,actor);return;}
        if(b.kind().endsWith("pool"))b.chargeDroppedTablets(s,actor);
        if(s.getGameTime()%10!=0)return;
        if(b.kind().equals("brewery")){b.collect(s,actor);BotanicalBrewing.tick(b,s);return;}
        if(b.kind().endsWith("pool")){b.collect(s,actor);for(var recipe:JugtaniaRecipes.of("mana_infusion"))if(b.craft(recipe,false))break;}
        if(b.kind().equals("terra_plate")&&b.terraBase(s)){b.collect(s,actor);for(var recipe:JugtaniaRecipes.of("terra_plate"))if(b.craft(recipe,true))break;}
        if(b.kind().startsWith("apothecary")||b.kind().equals("runic_altar"))b.collect(s,actor);
    }
    private void chargeDroppedTablets(ServerLevel s,ServerPlayer actor){
        var bellows=new ArrayList<BotanicalWorkshop>();
        if(tabletReceive)for(var side:Direction.Plane.HORIZONTAL){var pos=worldPosition.relative(side);
            if(s.hasChunkAt(pos)&&s.getBlockEntity(pos) instanceof BotanicalWorkshop b&&b.kind().equals("bellows")&&b.permitted(actor)&&pos.relative(b.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)).equals(worldPosition)&&io.github.jimbozoomer.jugcraft.finance.ClaimTransfers.allowed(s,worldPosition,pos))bellows.add(b);
        }
        int budget=1000*(bellows.size()+1),examined=0;boolean movedAny=false;
        for(var drop:s.getEntitiesOfClass(ItemEntity.class,new AABB(worldPosition).inflate(.1))){
            if(++examined>16||budget<=0)break;var stack=drop.getItem();if(!drop.isAlive()||BotanicalManaItems.capacity(stack)<=0||!Authority.mayChange(s,actor,drop.blockPosition()))continue;
            int stored=BotanicalManaItems.stored(stack),moved=Math.min(budget,tabletReceive?Math.min(mana,BotanicalManaItems.capacity(stack)-stored):stack.is(Jugtania.item("terra_pick"))?0:Math.min(stored,freeMana()));if(moved<=0)continue;
            var updated=stack.copy();BotanicalManaItems.stored(updated,stored+(tabletReceive?moved:-moved));drop.setItem(updated);mana+=tabletReceive?-moved:moved;budget-=moved;movedAny=true;
        }
        if(movedAny){for(var b:bellows)b.mechanisms.activate();changed();}
    }
    private void collect(ServerLevel s,ServerPlayer actor){collect(s,actor,new AABB(worldPosition).inflate(.2,.5,.2));}
    void collect(ServerLevel s,ServerPlayer actor,AABB bounds){
        // At most 16 offerings per pass; never vacuum an entire item stack or distant drops.
        int examined=0;for(var entity:s.getEntitiesOfClass(ItemEntity.class,bounds)){
            if(++examined>16)break;if(!entity.isAlive()||!Authority.mayChange(s,actor,entity.blockPosition()))continue;
            var stack=entity.getItem();if(stack.isEmpty())continue;
            if(kind().endsWith("pool")&&BotanicalManaItems.capacity(stack)>0)continue;
            if(kind().endsWith("pool")&&BotanicalTreasures.lotus(stack)>0){if(BotanicalTreasures.dissolve(this,stack)&&stack.isEmpty())entity.discard();continue;}
            if(kind().startsWith("apothecary")&&water){boolean made=false;for(var r:JugtaniaRecipes.of("petal_apothecary"))if(r.data().has("reagent")&&NouveauRecipes.ingredient(r.data().get("reagent"),stack)&&craft(r,true)){water=false;made=true;break;}if(made){stack.shrink(1);if(stack.isEmpty())entity.discard();changed();continue;}}
            for(int i=0;i<16;i++)if(items.get(i).isEmpty()&&canPlaceItem(i,stack)){items.set(i,stack.copyWithCount(1));stack.shrink(1);if(stack.isEmpty())entity.discard();changed();break;}
        }
    }
    int sparkDemand(){
        if(kind().equals("enchanter"))return enchanter.demand();
        if(kind().endsWith("pool"))return freeMana();
        if(!kind().equals("terra_plate")||!(level instanceof ServerLevel s)||!items.get(16).isEmpty()||!terraBase(s))return 0;
        for(var recipe:JugtaniaRecipes.of("terra_plate")){var used=recipe.match(items.subList(0,16));if(used==null)continue;boolean exact=true;for(int i=0;i<16;i++)if(used[i]!=items.get(i).getCount())exact=false;if(exact)return Math.max(0,recipe.mana()-mana);}
        return 0;
    }
    private boolean terraBase(ServerLevel s){
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var p=worldPosition.offset(x,-1,z);if(!s.hasChunkAt(p))return false;var state=s.getBlockState(p);
            if((Math.abs(x)+Math.abs(z))%2==0?!state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,io.github.jimbozoomer.jugcraft.Jugcraft.id("jugtania_terra_plate_base"))):!state.is(Blocks.LAPIS_BLOCK))return false;
        }return true;
    }
    private void daisy(ServerLevel s,ServerPlayer actor){
        int index=0;for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            if(x==0&&z==0)continue;int i=index++;var p=effectivePos().offset(x,0,z);
            if(!s.hasChunkAt(p)||!Authority.mayChange(s,actor,p)){daisyTime[i]=0;continue;}var old=s.getBlockState(p);JugtaniaRecipes.Recipe found=null;
            for(String type:List.of("pure_daisy","state_copying_pure_daisy"))for(var recipe:JugtaniaRecipes.of(type))if(recipe.blockInput(old)){found=recipe;break;}
            if(found==null){daisyTime[i]=0;daisyState[i]=null;continue;}
            if(!old.equals(daisyState[i])||!found.id().equals(daisyRecipe[i])){daisyTime[i]=0;daisyState[i]=old;daisyRecipe[i]=found.id();}
            daisyTime[i]+=20;setChanged();int duration=found.data().has("time")?Math.clamp(found.data().get("time").getAsInt()*8,20,24000):1200;
            if(daisyTime[i]>=duration&&s.setBlock(p,found.blockOutput(old),3)){daisyTime[i]=0;s.sendParticles(ParticleTypes.HAPPY_VILLAGER,p.getX()+.5,p.getY()+1,p.getZ()+.5,5,.3,.2,.3,0);}
        }
    }
    private static final class FuelProbe extends FurnaceBlockEntity {
        FuelProbe(ServerLevel s,BlockPos p){super(p,Blocks.FURNACE.defaultBlockState());setLevel(s);}
        int duration(ServerLevel s,ItemStack stack){return getBurnDuration(s,stack);}
    }
    private void fuel(ServerLevel s,ServerPlayer actor){
        var probe=new FuelProbe(s,worldPosition);
        for(int i=0;i<16;i++){var stack=items.get(i);int duration=stack.isEmpty()||stack.getItem().getCraftingRemainder()!=null?0:probe.duration(s,stack);if(duration>0){burn=Math.min(32000,duration)/2;stack.shrink(1);changed();return;}}
        int checked=0;for(var drop:s.getEntitiesOfClass(ItemEntity.class,new AABB(effectivePos()).inflate(3))){
            if(++checked>32)break;var stack=drop.getItem();if(!drop.isAlive()||stack.isEmpty()||stack.getItem().getCraftingRemainder()!=null||!Authority.mayChange(s,actor,drop.blockPosition()))continue;
            int duration=probe.duration(s,stack);if(duration<=0)continue;burn=Math.min(32000,duration)/2;stack.shrink(1);if(stack.isEmpty())drop.discard();changed();return;
        }
    }
    private boolean clearLine(ServerLevel s,BlockPos target){
        Vec3 from=Vec3.atCenterOf(worldPosition),to=Vec3.atCenterOf(target);var direction=to.subtract(from).normalize();
        int steps=(int)Math.ceil(from.distanceTo(to)*2);for(int i=0;i<=steps;i++)if(!s.hasChunkAt(BlockPos.containing(from.lerp(to,(double)i/Math.max(1,steps)))))return false;
        if(from.distanceTo(to)<1.8)return true;
        var hit=s.clip(new ClipContext(from.add(direction.scale(.9)),to.subtract(direction.scale(.9)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty()){
            @Override public net.minecraft.world.phys.shapes.VoxelShape getBlockShape(BlockState state,BlockGetter world,BlockPos pos){return state.getBlock() instanceof BotanicalRouting.Detector?net.minecraft.world.phys.shapes.Shapes.empty():super.getBlockShape(state,world,pos);}
        });
        return hit.getType()==HitResult.Type.MISS;
    }
    private void transfer(ServerLevel s,ServerPlayer actor){
        if(kind().contains("spreader")&&flightTarget==null){lenses.tick(s,actor);return;}
        if(kind().equals("redstone_spreader")){boolean signal=s.hasNeighborSignal(worldPosition);if(signal&&!redstoneHeld)redstonePending=true;if(signal!=redstoneHeld){redstoneHeld=signal;setChanged();}}
        if(flightTarget!=null){
            if(--transferTicks>0){setChanged();var point=Vec3.atCenterOf(worldPosition).lerp(Vec3.atCenterOf(flightTarget),1-(double)transferTicks/Math.max(1,(int)Math.ceil(Math.sqrt(flightTarget.distSqr(worldPosition))*2)));if(s.hasChunkAt(BlockPos.containing(point)))BotanicalRouting.trigger(s,actor,BlockPos.containing(point));s.sendParticles(ParticleTypes.END_ROD,point.x,point.y,point.z,1,0,0,0,0);return;}
            if(s.hasChunkAt(flightTarget)&&s.getBlockEntity(flightTarget) instanceof BotanicalWorkshop target&&target.permitted(actor)&&io.github.jimbozoomer.jugcraft.finance.ClaimTransfers.allowed(s,worldPosition,flightTarget)&&clearLine(s,flightTarget))flightMana-=BotanicalRouting.receive(target,actor,flightMana);
            mana=Math.min(capacity(),mana+flightMana);flightMana=0;flightTarget=null;changed();return;
        }
        if(link==null||mana==0||!s.hasChunkAt(link)||!(s.getBlockEntity(link) instanceof BotanicalWorkshop target)||!target.permitted(actor))return;
        if(kind().equals("redstone_spreader")&&!redstonePending)return;
        if(!io.github.jimbozoomer.jugcraft.finance.ClaimTransfers.allowed(s,worldPosition,link)||!clearLine(s,link))return;int amount=Math.min(mana,Math.min(BotanicalRouting.room(target,actor),generating()?mana:kind().equals("gaia_spreader")?640:kind().equals("elven_spreader")?320:160));if(amount<=0)return;
        mana-=amount;if(kind().equals("redstone_spreader"))redstonePending=false;
        if(generating()){target.mana+=amount;target.changed();}else{flightTarget=link;flightMana=amount;transferTicks=Math.max(1,(int)Math.ceil(Math.sqrt(link.distSqr(worldPosition))*2));}
        changed();
    }
    void rotateAim(ServerLevel s,ServerPlayer actor,float yaw){
        rotatedYaw=yaw;rotatedPitch=0;rotatedAim=true;link=null;Vec3 start=Vec3.atCenterOf(worldPosition),direction=aim();
        // Bound the search to loaded blocks and stop at the first physical obstruction.
        BlockPos last=worldPosition;
        for(int i=2;i<=64;i++){
            var candidate=BlockPos.containing(start.add(direction.scale(i*.25)));if(candidate.equals(last))continue;last=candidate;
            if(!s.hasChunkAt(candidate))break;
            if(s.getBlockEntity(candidate) instanceof BotanicalWorkshop receiver&&BotanicalRouting.receiver(receiver.kind())){
                if(receiver.permitted(actor)&&io.github.jimbozoomer.jugcraft.finance.ClaimTransfers.allowed(s,worldPosition,candidate))link=candidate.immutable();break;
            }
            var state=s.getBlockState(candidate);if(!(state.getBlock() instanceof BotanicalRouting.Detector)&&!state.getCollisionShape(s,candidate).isEmpty())break;
        }changed();
    }
    private boolean automation(@Nullable Direction side){if(!(level instanceof ServerLevel s))return false;var p=Authority.answering(s,owner);return p!=null&&permitted(p)&&(side==null||s.hasChunkAt(worldPosition.relative(side))&&Authority.mayChange(s,p,worldPosition.relative(side)));}
    @Override public int[] getSlotsForFace(Direction side){return kind().contains("spreader")||kind().equals("prism")?new int[]{0}:kind().equals("tiny_potato")?new int[]{potato.slot(side)}:kind().equals("cacophonium_block")?new int[0]:kind().equals("avatar")?new int[]{0}:CorporeaDevices.TYPES.contains(kind())?java.util.stream.IntStream.range(0,17).toArray():kind().equals("spark_changer")||kind().equals("open_crate")?new int[]{0}:kind().equals("crafty_crate")?java.util.stream.IntStream.range(0,9).toArray():kind().equals("hourglass")||kind().equals("incense_plate")?new int[]{0}:side==Direction.DOWN?new int[]{16}:java.util.stream.IntStream.range(0,16).toArray();}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,@Nullable Direction side){return canPlaceItem(slot,stack)&&automation(side);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){if(kind().contains("spreader")||kind().equals("prism"))return slot==0&&automation(side);if(kind().equals("tiny_potato"))return slot<6&&automation(side);if(kind().equals("avatar"))return slot==0&&automation(side);if(CorporeaDevices.TYPES.contains(kind()))return automation(side);return !BotanicalCrates.handles(kind())&&(slot==0&&(kind().equals("spark_changer")||mechanisms.extractable()||kind().equals("incense_plate")&&incense.extractable())||slot==16)&&automation(side);}
    @Override public int getContainerSize(){return 17;}
    @Override public int getMaxStackSize(){return kind().contains("spreader")||kind().equals("tiny_potato")||kind().equals("avatar")||kind().equals("spark_changer")||kind().equals("crafty_crate")||kind().equals("brewery")||kind().equals("incense_plate")?1:64;}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return slot>=0&&slot<17?items.get(slot):ItemStack.EMPTY;}
    @Override public ItemStack removeItem(int slot,int count){var result=ContainerHelper.removeItem(items,slot,count);if(!result.isEmpty()){if(kind().equals("hourglass"))mechanisms.reset();else changed();}return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){var result=ContainerHelper.takeItem(items,slot);if(kind().equals("hourglass")&&!result.isEmpty())mechanisms.reset();return result;}
    @Override public void setItem(int slot,ItemStack stack){items.set(slot,stack.copyWithCount(Math.min(stack.getCount(),Math.min(getMaxStackSize(),stack.getMaxStackSize()))));if(kind().equals("hourglass"))mechanisms.reset();else changed();}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return kind().contains("spreader")||kind().equals("prism")?slot==0&&getItem(0).isEmpty()&&!BotanicalLenses.name(stack).isEmpty():kind().equals("tiny_potato")?slot>=0&&slot<6&&getItem(slot).isEmpty():kind().equals("avatar")?slot==0&&getItem(0).isEmpty()&&BotanicalAvatar.rod(stack):kind().equals("spark_changer")?slot==0&&BotanicalDevices.augment(stack):BotanicalCrates.handles(kind())?crates.accepts(slot):kind().equals("incense_plate")?slot==0&&BotanicalIncense.accepts(stack)&&getItem(0).isEmpty():kind().equals("brewery")?(slot==0?BotanicalBrewing.vessel(stack):slot>0&&slot<7&&!BotanicalBrewing.vessel(stack)&&!stack.is(Jugtania.item("brew_vial"))&&!stack.is(Jugtania.item("brew_flask"))):mechanisms.accepts(slot,stack)||slot<16&&acceptsOfferings();}
    @Override public boolean stillValid(Player player){return player instanceof ServerPlayer p&&permitted(p)&&player.distanceToSqr(Vec3.atCenterOf(worldPosition))<=64&&level.getBlockEntity(worldPosition)==this;}
    @Override public void clearContent(){items.clear();changed();}
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){if(level instanceof ServerLevel s){sparks.drop(s);Containers.dropContents(s,pos,this);items.clear();}super.preRemoveSideEffects(pos,state);}
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putLong("flux_energy",flux.getAmount());out.putBoolean("pump_active",pumpActive);out.putInt("pump_signal",pumpSignal);lenses.save(out);out.putFloat("rotated_pitch",rotatedPitch);potato.save(out);cacophonium.save(out);platforms.save(out);redString.save(out);if(flowerOrigin!=null)out.store("flower_origin",BlockPos.CODEC,flowerOrigin);luminizer.save(out);avatar.save(out);corporea.save(out);devices.save(out);crates.save(out);enchanter.save(out);out.putLong("last_drum",lastDrum);sparks.save(out);incense.save(out);mechanisms.save(out);out.putFloat("rotated_yaw",rotatedYaw);out.putBoolean("rotated_aim",rotatedAim);portal.save(out);generator.save(out);function.save(out);harvest.save(out);life.save(out);bubble.save(out);for(int i=0;i<8;i++)if(daisyState[i]!=null){out.store("daisy_state_"+i,BlockState.CODEC,daisyState[i]);out.putInt("daisy_time_"+i,daisyTime[i]);out.putString("daisy_recipe_"+i,daisyRecipe[i]==null?"":daisyRecipe[i]);}ContainerHelper.saveAllItems(out,items);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);if(link!=null)out.store("link",BlockPos.CODEC,link);out.putInt("mana",mana);out.putInt("burn",burn);out.putBoolean("water",water);out.putBoolean("tablet_receive",tabletReceive);out.putBoolean("redstone_held",redstoneHeld);out.putBoolean("redstone_pending",redstonePending);out.putInt("flight_mana",flightMana);if(flightTarget!=null)out.store("flight_target",BlockPos.CODEC,flightTarget);out.putInt("flight_ticks",transferTicks);}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);flux.setAmount(in.getLongOr("flux_energy",0));pumpActive=in.getBooleanOr("pump_active",false);pumpSignal=Math.clamp(in.getIntOr("pump_signal",0),0,15);lenses.load(in);rotatedPitch=in.getFloatOr("rotated_pitch",0);if(!Float.isFinite(rotatedPitch))rotatedPitch=0;rotatedPitch=Math.clamp(rotatedPitch,-90,90);potato.load(in);cacophonium.load(in);platforms.load(in);redString.load(in);flowerOrigin=in.read("flower_origin",BlockPos.CODEC).filter(p->p.distSqr(worldPosition)<=81).orElse(null);luminizer.load(in);avatar.load(in);corporea.load(in);devices.load(in);crates.load(in);enchanter.load(in);lastDrum=in.getLongOr("last_drum",Long.MIN_VALUE);sparks.load(in);incense.load(in);mechanisms.load(in);rotatedYaw=in.getFloatOr("rotated_yaw",0);if(!Float.isFinite(rotatedYaw))rotatedYaw=0;rotatedAim=in.getBooleanOr("rotated_aim",false);portal.load(in);generator.load(in);function.load(in);harvest.load(in);life.load(in);bubble.load(in);for(int i=0;i<8;i++){daisyState[i]=in.read("daisy_state_"+i,BlockState.CODEC).orElse(null);daisyTime[i]=Math.clamp(in.getIntOr("daisy_time_"+i,0),0,24000);daisyRecipe[i]=in.getStringOr("daisy_recipe_"+i,"");}items=NonNullList.withSize(17,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);link=in.read("link",BlockPos.CODEC).filter(p->p.distSqr(worldPosition)<=(kind().contains("spreader")?4096:256)).orElse(null);mana=Math.clamp(in.getIntOr("mana",0),0,capacity());burn=Math.clamp(in.getIntOr("burn",0),0,16000);water=in.getBooleanOr("water",false);tabletReceive=in.getBooleanOr("tablet_receive",true);redstoneHeld=in.getBooleanOr("redstone_held",false);redstonePending=in.getBooleanOr("redstone_pending",false);flightMana=Math.clamp(in.getIntOr("flight_mana",0),0,capacity()-mana);flightTarget=in.read("flight_target",BlockPos.CODEC).filter(p->p.distSqr(worldPosition)<=256).orElse(null);transferTicks=Math.clamp(in.getIntOr("flight_ticks",0),0,32);if(flightTarget==null){mana+=flightMana;flightMana=0;}}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
