package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.*;
import static io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects.Result.*;

/** Supplied glyph operations enter through ConcordanceEffects, after its authority and work-ledger checks. */
public final class NouveauGlyphs {
    private NouveauGlyphs(){}
    public static void register(){
        NouveauSpellState.register();
        NouveauElemental.register();NouveauInfusion.register();NouveauFireworks.register();
        NouveauSpellDelivery.register();
        for(var e:NouveauCatalog.ITEM_ENTRIES.values())if(e.kind().equals("glyph"))NouveauCatalog.registerItem(e,p->new Item(p){
            @Override public InteractionResult use(Level l,Player user,InteractionHand hand){
                if(user instanceof ServerPlayer player){
                    if(!JugNouveau.enabled()||player.isSpectator())return InteractionResult.FAIL;
                    ConcordanceProgress.grant(player,"jugcraft:nouveau_arcana",ResearchState.MASTERED);
                    player.sendOverlayMessage(Component.literal("This glyph is available in your spell instrument's constellation."));
                }return InteractionResult.SUCCESS;
            }
        });
    }
    private static String name(EffectSpec e){String s=e.status();return s==null?"":s.substring(s.indexOf(':')+1).replace("nouveau/","");}
    private static ServerPlayer actor(ConcordanceEffects.Context c){return c.actor() instanceof ServerPlayer p?p:Authority.answering(c.level(),c.cause().actor());}
    public static boolean accepts(ServerLevel l,EffectSpec e,BlockPos p){
        if(!l.hasChunkAt(p))return false;String op=name(e);var s=l.getBlockState(p);if(e.augment("sensitive")>0&&Set.of("crush","smelt").contains(op))return true;
        return switch(op){
            case "break","crush","smelt","cut","fell","extract","exchange","gravity","animate_block","intangible" -> !s.isAir()&&s.getDestroySpeed(l,p)>=0&&l.getBlockEntity(p)==null;
            case "grow" -> s.getBlock() instanceof BonemealableBlock;
            case "freeze" -> s.is(Blocks.WATER)||s.is(Blocks.LAVA)||s.is(Blocks.ICE)||s.is(Blocks.PACKED_ICE);
            case "evaporate" -> !s.getFluidState().isEmpty();
            default -> true;
        };
    }
    public static ConcordanceEffects.Result block(ConcordanceEffects.Context c,EffectSpec e,BlockPos at){
        var l=c.level();var p=actor(c);if(p==null)return NOT_ALLOWED;String op=name(e);var s=l.getBlockState(at);int power=Math.max(1,e.magnitude());
        if(!accepts(l,e,at)||!Authority.mayChange(l,p,at))return NOT_ALLOWED;
        if(l.getBlockEntity(at) instanceof NouveauWeaves.Weave weave&&weave.spell(op,power))return APPLIED;
        switch(op){
            case "infuse" -> {return NouveauInfusion.cast(c,e,p,Vec3.atCenterOf(at),null)?APPLIED:NOTHING;}
            case "name" -> {var label=spellName(p);if(label==null)return NOTHING;if(l.getBlockEntity(at) instanceof NouveauWorkshop station&&station.permitted(p)){station.customName(label.getString());return APPLIED;}if(l.getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.BaseContainerBlockEntity container&&container.canOpen(p)){((io.github.jimbozoomer.jugcraft.mixin.NouveauContainerNameAccess)container).jugcraft$setName(label);container.setChanged();return APPLIED;}for(var mob:l.getEntitiesOfClass(LivingEntity.class,new AABB(at).inflate(2),x->!(x instanceof Player)&&x.isAlive()).stream().limit(8).toList())if(Authority.mayChange(l,p,mob.blockPosition())&&c.ledger().admit(c.step(),"entity:"+mob.getUUID(),1).admitted())mob.setCustomName(label);return APPLIED;}
            case "fell" -> {return fell(c,e,p,at)?APPLIED:NOTHING;}
            case "cold_snap","flare","explosion","lightning","fangs" -> {return NouveauElemental.block(c,e,p,at,op)?APPLIED:NOTHING;}
            case "cut" -> {var tool=new ItemStack(e.augment("amplify")>0?Items.DIAMOND_AXE:Items.SHEARS);var context=new net.minecraft.world.item.context.UseOnContext(l,p,InteractionHand.MAIN_HAND,tool,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false)){};return tool.useOn(context).consumesAction()?APPLIED:NOTHING;}
            case "break","extract" -> {
                if(s.getDestroySpeed(l,at)>power*3||op.equals("fell")&&!s.is(BlockTags.LOGS)&&!s.is(BlockTags.LEAVES)&&!s.is(Blocks.CACTUS))return NOTHING;
                var tool=new ItemStack(op.equals("cut")||e.augment("sensitive")>0?Items.SHEARS:power>=3?Items.DIAMOND_PICKAXE:Items.IRON_PICKAXE);
                if(e.augment("fortune")>0)tool.enchant(l.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE),e.augment("fortune"));
                if(op.equals("extract")||e.augment("extract")>0)tool.enchant(l.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),1);
                Block.getDrops(s,l,at,null,p,tool).forEach(drop->Block.popResource(l,at,drop));
                l.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);return APPLIED;
            }
            case "crush","smelt" -> {
                if(e.augment("sensitive")>0)return transmuteItems(l,p,at,op,e.augment("dampen")>0)?APPLIED:NOTHING;
                var input=new ItemStack(s.getBlock());List<ItemStack> results;
                if(op.equals("crush")){var rolled=NouveauRecipes.crush(l,input);if(rolled.isEmpty())return NOTHING;results=rolled.get();}
                else {var result=cooked(l,input,e.augment("dampen")>0);if(result.isEmpty())return NOTHING;results=List.of(result);}
                if(!l.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL))return NOTHING;
                results.forEach(result->Block.popResource(l,at,result));return APPLIED;
            }
            case "grow" -> {if(s.getBlock() instanceof BonemealableBlock crop&&crop.isValidBonemealTarget(l,at,s,net.minecraft.world.level.block.BonemealSource.INTERACTION)){crop.performBonemeal(l,l.getRandom(),at,s,net.minecraft.world.level.block.BonemealSource.INTERACTION);return APPLIED;}return NOTHING;}
            case "freeze" -> {Block ice=s.is(Blocks.WATER)?Blocks.ICE:s.is(Blocks.LAVA)?Blocks.OBSIDIAN:s.is(Blocks.ICE)?Blocks.PACKED_ICE:s.is(Blocks.PACKED_ICE)?Blocks.BLUE_ICE:null;if(s.is(Blocks.WATER)&&e.augment("sensitive")>0)return NouveauSpellState.replace(l,at,Blocks.FROSTED_ICE.defaultBlockState(),e.duration())?APPLIED:NOTHING;return ice!=null&&l.setBlock(at,ice.defaultBlockState(),Block.UPDATE_ALL)?APPLIED:NOTHING;}
            case "evaporate" -> {return l.setBlock(at,s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)?s.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,false):Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL)?APPLIED:NOTHING;}
            case "conjure_water" -> {var dest=s.canBeReplaced()?at:at.above();return Authority.mayChange(l,p,dest)&&l.getBlockState(dest).canBeReplaced()&&l.setBlock(dest,Blocks.WATER.defaultBlockState(),Block.UPDATE_ALL)?APPLIED:NOTHING;}
            case "rotate" -> {var next=s.rotate(e.augment("dampen")>0?Rotation.COUNTERCLOCKWISE_90:Rotation.CLOCKWISE_90);if(e.augment("sensitive")>0){var axis=net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;if(s.hasProperty(axis))next=s.cycle(axis);else if(s.hasProperty(NouveauWorkshopBlock.FACING))next=s.cycle(NouveauWorkshopBlock.FACING);}return !next.equals(s)&&l.setBlock(at,next,Block.UPDATE_ALL)?APPLIED:NOTHING;}
            case "intangible" -> {return NouveauSpellState.replace(l,at,NouveauCatalog.block("intangible_air").defaultBlockState(),e.duration())?APPLIED:NOTHING;}
            case "light" -> {
                int strength=Math.clamp(11+e.magnitude()-e.augment("dampen")*3,0,15);
                if(NouveauFixtures.light(l,at,s,p,strength>0))return APPLIED;
                if(s.getBlock() instanceof NouveauDecor.SourceLamp)return l.setBlock(at,s.setValue(NouveauDecor.SourceLamp.BRIGHTNESS,strength),Block.UPDATE_ALL)?APPLIED:NOTHING;
                var dest=s.canBeReplaced()?at:at.above();if(!l.hasChunkAt(dest)||!Authority.mayChange(l,p,dest)||!l.getBlockState(dest).canBeReplaced())return NOTHING;
                return l.setBlock(dest,NouveauCatalog.block("light_block").defaultBlockState().setValue(LightBlock.LEVEL,strength),Block.UPDATE_ALL)?APPLIED:NOTHING;
            }
            case "ignite" -> {
                if(s.is(Blocks.TNT)){if(!TntBlock.prime(l,at,p,ItemStack.EMPTY))return NOTHING;l.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);return APPLIED;}
                var dest=s.canBeReplaced()?at:at.above();if(!l.hasChunkAt(dest)||!Authority.mayChange(l,p,dest)||!l.getBlockState(dest).canBeReplaced())return NOTHING;
                return NouveauSpellState.replace(l,dest,NouveauCatalog.block("magic_fire").defaultBlockState(),Math.min(200,e.duration()))?APPLIED:NOTHING;
            }
            case "interact" -> {
                if(e.augment("sensitive")>0)return p.getOffhandItem().useOn(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.OFF_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false))).consumesAction()?APPLIED:NOTHING;
                return s.useWithoutItem(l,p,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false)).consumesAction()?APPLIED:NOTHING;
            }
            case "phantom_block","redstone_signal","prestidigitation" -> {
                var dest=s.canBeReplaced()?at:at.above();if(!Authority.mayChange(l,p,dest)||!l.getBlockState(dest).canBeReplaced())return NOT_ALLOWED;
                var replacement=op.equals("redstone_signal")?NouveauCatalog.block("redstone_air").defaultBlockState():NouveauCatalog.block(op.equals("phantom_block")?"mage_block":"particle_block").defaultBlockState();
                if(op.equals("phantom_block")&&e.augment("amplify")>0)return l.setBlock(dest,replacement,Block.UPDATE_ALL)?APPLIED:NOTHING;
                return NouveauSpellState.replace(l,dest,replacement,e.duration())?APPLIED:NOTHING;
            }
            case "place_block","exchange" -> {
                var dest=op.equals("exchange")?at:s.canBeReplaced()?at:at.above();
                if(!Authority.mayChange(l,p,dest)||l.getBlockEntity(dest)!=null)return NOT_ALLOWED;
                var supplies=new ArrayList<>(NouveauDevices.supplies(c,p));if(e.augment("randomize")>0)Collections.shuffle(supplies);
                for(var supply:supplies){
                    if(!NouveauDevices.automated(c)&&supply.slot()>8)continue;var stack=supply.stack();if(!(stack.getItem() instanceof BlockItem item))continue;
                    var previous=l.getBlockState(dest);if(op.equals("exchange")&&previous.getDestroySpeed(l,dest)>Math.max(1,e.magnitude())*3)return NOTHING;
                    if(op.equals("exchange"))l.setBlock(dest,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                    int count=stack.getCount();var context=new net.minecraft.world.item.context.UseOnContext(l,p,InteractionHand.MAIN_HAND,stack,new BlockHitResult(Vec3.atCenterOf(dest.below()),Direction.UP,dest.below(),false)){};
                    if(item.place(new net.minecraft.world.item.context.BlockPlaceContext(context)).consumesAction()){
                        if(NouveauDevices.automated(c)&&stack.getCount()==count)stack.shrink(1);supply.changed();if(op.equals("exchange"))Block.popResource(l,dest,new ItemStack(previous.getBlock()));return APPLIED;
                    }
                    if(op.equals("exchange"))l.setBlock(dest,previous,Block.UPDATE_ALL);
                }return NOTHING;
            }
            case "pickup","toss" -> {return moveItems(c,p,at,op.equals("pickup"),e)?APPLIED:NOTHING;}
            case "craft" -> {if(NouveauDevices.automated(c))return NOTHING;p.openMenu(new SimpleMenuProvider((id,inventory,who)->new CraftingMenu(id,inventory,ContainerLevelAccess.NULL){@Override public boolean stillValid(Player who){return who==p&&who.isAlive()&&!who.isSpectator();}},Component.translatable("container.crafting")));return APPLIED;}
            case "ender_inventory" -> {if(NouveauDevices.automated(c))return NOTHING;p.openMenu(new SimpleMenuProvider((id,inventory,who)->ChestMenu.threeRows(id,inventory,p.getEnderChestInventory()),Component.translatable("container.enderchest")));return APPLIED;}
            case "blink" -> {return NouveauUtilities.teleport(p,GlobalPos.of(l.dimension(),at.above()))?APPLIED:NOTHING;}
            case "gravity","launch","pull","gust","animate_block" -> {if(e.augment("sensitive")>0)return NOTHING;
                if(!op.equals("animate_block")&&NouveauGear.worn(p,"shapers_focus")){Vec3 motion=switch(op){case "launch"->new Vec3(0,.3*Math.min(4,power),0);case "pull"->p.position().subtract(Vec3.atCenterOf(at)).normalize().scale(.3*power);case "gust"->Vec3.atCenterOf(at).subtract(p.position()).normalize().scale(.3*power);default->new Vec3(0,-.3,0);};return NouveauAnimatedBlock.launch(c,p,at,e.duration(),motion,power)?APPLIED:NOTHING;}
                return NouveauSummons.moveBlock(l,p,at,op,power,e.duration())?APPLIED:NOTHING;}
            case "summon_wolves","summon_vex","summon_undead","summon_steed","summon_decoy" -> {return NouveauSummons.summon(l,p,at.above(),op,power,e.duration(),e.augment("split"),e.augment("pierce"))?APPLIED:NOTHING;}
            case "dispel" -> {if(NouveauFixtures.light(l,at,s,p,false))return APPLIED;if(s.is(NouveauCatalog.block("mage_block"))||s.is(NouveauCatalog.block("particle_block"))){l.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);return APPLIED;}return NOTHING;}
            case "wololo" -> {
                if(s.hasBlockEntity())return NOTHING;var key=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock());if(!key.getNamespace().equals("minecraft"))return NOTHING;
                var color=NouveauControls.dye(p.getMainHandItem());for(var old:DyeColor.values())if(key.getPath().startsWith(old.getName()+"_")){
                    var replacement=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(color.getName()+key.getPath().substring(old.getName().length())));if(replacement==null||replacement==Blocks.AIR)return NOTHING;
                    var next=replacement.withPropertiesOf(s);return l.setBlock(at,next,Block.UPDATE_ALL)?APPLIED:NOTHING;
                }return NOTHING;
            }
            case "firework" -> {return NouveauFireworks.create(c,e,p,Vec3.atCenterOf(at))?APPLIED:NOTHING;}
            default -> {return NOTHING;}
        }
    }
    public static ConcordanceEffects.Result creature(ConcordanceEffects.Context c,EffectSpec e,LivingEntity target){
        var p=actor(c);if(p==null)return NOT_ALLOWED;var l=c.level();String op=name(e);int power=Math.max(1,e.magnitude()),duration=Math.max(20,e.duration());
        if(target instanceof NouveauAnimatedBlock moved&&moved.belongs(p)){
            if(Set.of("break","cut","dispel").contains(op)){moved.breakApart(p);return APPLIED;}
            if(op.equals("snare")){moved.settle(l);return APPLIED;}
            if(op.equals("freeze")){moved.change(Blocks.ICE.defaultBlockState());return APPLIED;}
            if(op.equals("crush")){moved.change(Blocks.GRAVEL.defaultBlockState());return APPLIED;}
        }
        switch(op){
            case "conjure_water" -> {target.clearFire();NouveauStatuses.give(target,"soaked",duration,0,p);return APPLIED;}
            case "light" -> {target.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,duration),p);if(target!=p||e.augment("sensitive")>0)target.addEffect(new MobEffectInstance(MobEffects.GLOWING,duration),p);return APPLIED;}
            case "ignite" -> {target.igniteForSeconds(Math.max(1,duration/20));return APPLIED;}
            case "freeze" -> {NouveauStatuses.give(target,"freezing",duration,Math.min(4,power-1),p);return APPLIED;}
            case "hex","snare" -> {return NouveauStatuses.give(target,op.equals("snare")?"snared":"hex",duration,Math.min(4,power-1),p)?APPLIED:NOTHING;}
            case "harm" -> {
                if(e.duration()>200){target.addEffect(new MobEffectInstance(MobEffects.POISON,duration,Math.min(4,power/3)),p);return APPLIED;}
                return target.hurtServer(l,l.damageSources().indirectMagic(p,p),power)?APPLIED:NOTHING;
            }
            case "heal" -> {
                if(target.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD))return ConcordanceEffects.mayHarm(p,target)&&Authority.mayStrike(p,target)&&target.hurtServer(l,l.damageSources().indirectMagic(p,p),power)?APPLIED:NOTHING;
                if(target.getHealth()>=target.getMaxHealth()||!p.isCreative()&&p.getFoodData().getFoodLevel()<2)return NOTHING;
                target.heal(power);if(!p.isCreative())p.causeFoodExhaustion(1);return APPLIED;
            }
            case "bubble" -> {NouveauStatuses.bubble(target,power,duration,p);return APPLIED;}
            case "bounce" -> {return NouveauStatuses.give(target,"bounce",duration,Math.min(4,power-1),p)?APPLIED:NOTHING;}
            case "glide" -> {if(target instanceof Player player){NouveauStatuses.give(player,"glide",duration,0,p);return APPLIED;}return NOTHING;}
            case "gravity","launch","leap","pull","gust" -> {
                if(op.equals("gravity"))NouveauStatuses.give(target,"gravity",duration,Math.min(4,power-1),p);
                Vec3 motion=switch(op){case "gravity"->new Vec3(0,-.4*power,0);case "launch"->new Vec3(0,.4*power,0);case "leap"->target.getLookAngle().scale(.4*power);case "pull"->c.origin().subtract(target.position()).normalize().scale(.3*power);default->target.position().subtract(c.origin()).normalize().scale(.3*power);};
                target.setDeltaMovement(target.getDeltaMovement().add(motion));if(target instanceof ServerPlayer client)client.connection.send(new ClientboundSetEntityMotionPacket(target));return APPLIED;
            }
            case "dispel" -> {
                if(target instanceof BookWyrm wyrm)return wyrm.dispel(p)?APPLIED:NOT_ALLOWED;
                if(target instanceof Starbuncle star&&star.mayServe(p)){star.discard();return APPLIED;}
                if(target instanceof net.minecraft.world.entity.monster.Blaze&&target.isOnFire()&&ConcordanceEffects.mayHarm(p,target)&&Authority.mayStrike(l,p,p.getUUID(),target)){for(var recipe:NouveauRecipes.all())if(recipe.operation().equals("dispel_entity")&&recipe.data().get("entity").getAsString().equals("minecraft:blaze")){var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,net.minecraft.resources.Identifier.parse(recipe.data().get("loot_table").getAsString()));target.dropFromLootTable(l,l.damageSources().playerAttack(p),true,key,item->target.spawnAtLocation(l,item));target.discard();NouveauAdvancements.award(p,"poof_mob");return APPLIED;}}
                if(target instanceof Witch&&target.getHealth()<=target.getMaxHealth()/2&&ConcordanceEffects.mayHarm(p,target)&&Authority.mayStrike(l,p,p.getUUID(),target)){
                    target.spawnAtLocation(l,new ItemStack(NouveauCatalog.item("wixie_shards")));NouveauAdvancements.award(p,"poof_mob");target.discard();return APPLIED;
                }
                if(target instanceof NouveauHelper helper&&p.getUUID().equals(helper.owner())){helper.discard();return APPLIED;}
                return target.removeAllEffects()?APPLIED:NOTHING;
            }
            case "cold_snap","flare","crush","cut","fangs","explosion","lightning","wind_shear","wind_burst" -> {return NouveauElemental.creature(c,e,p,target,op)?APPLIED:NOTHING;}
            case "infuse" -> {return NouveauInfusion.cast(c,e,p,target.position(),target)?APPLIED:NOTHING;}
            case "exchange","blink" -> {
                if(target!=p&&!ConcordanceEffects.mayHarm(p,target))return FRIENDLY;
                var scroll=NouveauDevices.automated(c)?NouveauDevices.supplies(c,p).stream().map(NouveauDevices.Supply::stack).filter(stack->stack.has(NouveauUtilities.DESTINATION)).findFirst().orElse(ItemStack.EMPTY):p.getOffhandItem();var destination=scroll.get(NouveauUtilities.DESTINATION);
                if(op.equals("blink")&&destination!=null)return NouveauUtilities.teleportEntity(p,target,destination)?APPLIED:NOTHING;
                if(target==p)return NouveauUtilities.teleport(p,GlobalPos.of(l.dimension(),BlockPos.containing(p.position().add(p.getLookAngle().scale(Math.min(16,3*power))))))?APPLIED:NOTHING;
                BlockPos a=p.blockPosition(),b=target.blockPosition();if(!Authority.mayChange(l,p,a)||!Authority.mayChange(l,p,b)||target instanceof Player)return NOT_ALLOWED;
                if(!NouveauUtilities.teleport(p,GlobalPos.of(l.dimension(),b)))return NOTHING;target.teleportTo(a.getX()+.5,a.getY(),a.getZ()+.5);return APPLIED;
            }
            case "name" -> {var label=spellName(p);if(label==null)return NOTHING;if(target==p){if(p.getOffhandItem().isEmpty())return NOTHING;p.getOffhandItem().set(DataComponents.CUSTOM_NAME,label);return APPLIED;}if(target instanceof Player)return NOTHING;target.setCustomName(label);return APPLIED;}
            case "rotate" -> {target.setYRot(e.augment("sensitive")>0?p.getYRot():target.getYRot()+(e.augment("dampen")>0?-90:90));target.setYHeadRot(target.getYRot());return APPLIED;}
            case "invisibility" -> {target.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,duration),p);return APPLIED;}
            case "sense_magic" -> {return NouveauStatuses.give(target,"magic_find",duration,0,p)?APPLIED:NOTHING;}
            case "interact" -> {return target.interact(p,e.augment("sensitive")>0?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,target.position()).consumesAction()?APPLIED:NOTHING;}
            case "firework" -> {return NouveauFireworks.create(c,e,p,target.position())?APPLIED:NOTHING;}
            case "prestidigitation" -> {l.sendParticles(ParticleTypes.ENCHANT,target.getX(),target.getY()+1,target.getZ(),24,.4,.6,.4,.05);return APPLIED;}
            case "rewind" -> {return NouveauSummons.rewind(l,p,target)?APPLIED:NOTHING;}
            case "wololo" -> {return NouveauSummons.dye(p,target)?APPLIED:NOTHING;}
            default -> {return block(c,e,target.blockPosition());}
        }
    }
    private static Component spellName(ServerPlayer p){
        for(int i=0;i<9;i++){var tag=p.getInventory().getItem(i);if(tag.is(Items.NAME_TAG)&&tag.has(DataComponents.CUSTOM_NAME))return tag.get(DataComponents.CUSTOM_NAME);}
        return p.getMainHandItem().get(DataComponents.CUSTOM_NAME);
    }
    private static boolean fell(ConcordanceEffects.Context c,EffectSpec effect,ServerPlayer p,BlockPos origin){
        var l=c.level();var first=l.getBlockState(origin);if(!vegetation(first))return false;
        var queue=new ArrayDeque<BlockPos>();var seen=new HashSet<BlockPos>();queue.add(origin);boolean changed=false;int max=Math.min(96,32+effect.augment("radius")*16);
        var tool=new ItemStack(Items.DIAMOND_AXE);if(effect.augment("fortune")>0)tool.enchant(l.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE),effect.augment("fortune"));if(effect.augment("extract")>0)tool.enchant(l.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),1);
        while(!queue.isEmpty()&&seen.size()<512&&max>0){var at=queue.removeFirst();if(!seen.add(at)||at.distSqr(origin)>1024||!l.hasChunkAt(at))continue;var state=l.getBlockState(at);if(!vegetation(state)||state.hasBlockEntity()||state.getDestroySpeed(l,at)>Math.max(1,effect.magnitude())*3||!Authority.mayChange(l,p,at))continue;
            if(!at.equals(origin)&&!c.ledger().admit(c.step(),"block:"+at.asLong(),3).admitted())break;
            if(effect.augment("randomize")==0||l.getRandom().nextBoolean()){Block.getDrops(state,l,at,null,p,tool).forEach(drop->Block.popResource(l,at,drop));l.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);changed=true;max--;}
            for(var d:Direction.values())queue.add(at.relative(d));
        }return changed;
    }
    private static boolean vegetation(BlockState state){return state.is(BlockTags.LOGS)||state.is(BlockTags.LEAVES)||state.is(Blocks.CACTUS)||state.is(Blocks.BAMBOO)||state.is(Blocks.SUGAR_CANE)||state.getBlock() instanceof HugeMushroomBlock;}
    private static boolean transmuteItems(ServerLevel l,ServerPlayer p,BlockPos at,String op,boolean smoking){
        for(var entity:l.getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(1.5),Entity::isAlive).stream().limit(8).toList()){
            if(!Authority.mayChange(l,p,entity.blockPosition()))continue;var input=entity.getItem();List<ItemStack> outputs;
            if(op.equals("crush")){var result=NouveauRecipes.crush(l,input);if(result.isEmpty())continue;outputs=result.get();}
            else{var cooked=cooked(l,input,smoking);if(cooked.isEmpty())continue;outputs=List.of(cooked);}
            input.shrink(1);if(input.isEmpty())entity.discard();else entity.setItem(input);outputs.forEach(item->Block.popResource(l,entity.blockPosition(),item));return true;
        }return false;
    }
    private static boolean moveItems(ConcordanceEffects.Context c,ServerPlayer p,BlockPos at,boolean pickup,EffectSpec effect){
        var l=c.level();
        if(pickup){boolean moved=false;for(var item:l.getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(3),e->e.isAlive()).stream().limit(16).toList()){
            if(!Authority.mayChange(l,p,item.blockPosition())||item.hasPickUpDelay())continue;var pickupOwner=((io.github.jimbozoomer.jugcraft.mixin.AlchemicalItemAccess)item).jugcraft$pickupTarget();if(pickupOwner!=null&&!pickupOwner.equals(p.getUUID()))continue;var stack=item.getItem();int before=stack.getCount();NouveauDevices.collect(c,p,stack);if(stack.isEmpty())item.discard();else item.setItem(stack);moved|=stack.getCount()<before;
        }return moved;}
        var supplies=NouveauDevices.supplies(c,p);var stack=NouveauDevices.automated(c)?supplies.stream().map(NouveauDevices.Supply::stack).filter(s->!s.isEmpty()).findFirst().orElse(ItemStack.EMPTY):p.getOffhandItem();supplies.forEach(NouveauDevices.Supply::changed);if(effect.augment("randomize")>0){var candidates=new ArrayList<ItemStack>();for(var supply:supplies)if(!supply.stack().isEmpty())candidates.add(supply.stack());if(!candidates.isEmpty())stack=candidates.get(l.getRandom().nextInt(candidates.size()));}if(stack.isEmpty())return false;
        int limit=Math.max(1,64>>effect.augment("dampen"));
        var container=NouveauStorage.accessible(l,p,at);if(container!=null){
            for(int i=0;i<container.getContainerSize();i++)if(container.canPlaceItem(i,stack)){
                var slot=container.getItem(i);if(slot.isEmpty()){container.setItem(i,stack.split(Math.min(limit,Math.min(stack.getCount(),stack.getMaxStackSize()))));return true;}
                if(ItemStack.isSameItemSameComponents(slot,stack)&&slot.getCount()<slot.getMaxStackSize()){int n=Math.min(limit,Math.min(stack.getCount(),slot.getMaxStackSize()-slot.getCount()));slot.grow(n);stack.shrink(n);container.setChanged();return true;}
            }return false;
        }
        if(l.getBlockEntity(at) instanceof Container)return false;
        int amount=Math.min(limit,stack.getCount());var drop=new ItemEntity(l,at.getX()+.5,at.getY()+1,at.getZ()+.5,stack.copyWithCount(amount));if(!l.addFreshEntity(drop))return false;stack.shrink(amount);return true;
    }
    private static ItemStack cooked(ServerLevel l,ItemStack stack,boolean smoking){var input=new SingleRecipeInput(stack);var manager=l.getServer().getRecipeManager();return smoking?manager.getRecipeFor(RecipeType.SMOKING,input,l).map(r->r.value().assemble(input)).orElse(ItemStack.EMPTY):manager.getRecipeFor(RecipeType.SMELTING,input,l).map(r->r.value().assemble(input)).orElse(ItemStack.EMPTY);}
}
