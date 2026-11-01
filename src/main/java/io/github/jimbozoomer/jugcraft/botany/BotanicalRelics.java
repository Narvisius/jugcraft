package io.github.jimbozoomer.jugcraft.botany;

import com.mojang.serialization.Codec;
import eu.pb4.trinkets.api.*;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.*;

/** Personal Gaia relics; possession is sufficient, with no research gate. */
public final class BotanicalRelics extends Item implements TrinketCallback {
    static final List<String> RELICS=List.of("infinite_fruit","king_key","flugel_eye","thor_ring","odin_ring","loki_ring");
    static final Set<String> ITEMS=Set.of("dice","infinite_fruit","king_key","flugel_eye","thor_ring","odin_ring","loki_ring");
    public static DataComponentType<UUID> OWNER;
    public static DataComponentType<List<GlobalPos>> DESTINATIONS;
    public static DataComponentType<List<BlockPos>> CURSORS;
    public static DataComponentType<GlobalPos> ORIGIN;
    private static AttachmentType<Integer> COLLECTED;
    private static final Map<ServerPlayer,Integer> CHARGES=new WeakHashMap<>();
    private static final Set<UUID> REPLICATING=new HashSet<>();
    private final String kind;
    BotanicalRelics(String kind,Properties p){super(p.stacksTo(1));this.kind=kind;}
    private static <T> DataComponentType<T> component(String name,Codec<T> codec){return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id(name),DataComponentType.<T>builder().persistent(codec).networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(codec)).build());}
    static void register(){
        OWNER=component("botanical_relic_owner",UUIDUtil.CODEC);DESTINATIONS=component("botanical_relic_destinations",GlobalPos.CODEC.listOf(0,32));CURSORS=component("botanical_loki_cursors",BlockPos.CODEC.listOf(0,64));ORIGIN=component("botanical_loki_origin",GlobalPos.CODEC);
        COLLECTED=AttachmentRegistry.<Integer>builder().persistent(Codec.intRange(0,63)).copyOnDeath().buildAndRegister(Jugcraft.id("botanical_collected_relics"));
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((e,source,amount)->!(e instanceof Player p&&worn(p,"odin_ring")&&source.is(TagKey.create(Registries.DAMAGE_TYPE,Jugcraft.id("jugtania_ring_of_odin_immune")))));
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers()){
            TrinketsApi.getAttachment(p).forEach((slot,item)->{if(!slot.cosmetic()&&slot.canApplyEffects()&&item.getItem() instanceof BotanicalRelics)bind(p,item);});
            if(!p.isUsingItem()||!p.getUseItem().is(Jugtania.item("king_key")))CHARGES.remove(p);
            if(p.isAlive()&&worn(p,"odin_ring"))p.clearFire();
        }});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->{CHARGES.clear();REPLICATING.clear();});
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((world,actor,origin,state,node)->{
            if(!(actor instanceof ServerPlayer p)||!p.isShiftKeyDown()||REPLICATING.contains(p.getUUID())||node!=null)return;
            var ring=equipped(p,"loki_ring");if(ring.isEmpty()||ring.has(ORIGIN))return;
            var offsets=ring.getOrDefault(CURSORS,List.<BlockPos>of());int cost=offsets.size()>10?offsets.size():Math.min(offsets.size(),(int)Math.exp(offsets.size()*.25));
            REPLICATING.add(p.getUUID());try{for(var delta:offsets){var at=origin.offset(delta);
                if(!world.hasChunkAt(at)||!world.getWorldBorder().isWithinBounds(at)||p.distanceToSqr(Vec3.atCenterOf(at))>4096||world.getBlockEntity(at)!=null||!Authority.mayChange(p.level(),p,at))continue;
                var current=world.getBlockState(at);if(current.isAir()||current.getDestroySpeed(world,at)<0||!p.hasCorrectToolForDrops(current)||!current.is(state.getBlock()))continue;
                if(!BotanicalManaItems.pay(p,cost))break;p.gameMode.destroyBlock(at);
            }}finally{REPLICATING.remove(p.getUUID());}
        });
        UseBlockCallback.EVENT.register((user,level,hand,hit)->{
            if(!(user instanceof ServerPlayer p)||!p.isShiftKeyDown()||REPLICATING.contains(p.getUUID()))return InteractionResult.PASS;var ring=equipped(p,"loki_ring");if(ring.isEmpty())return InteractionResult.PASS;
            if(p.getItemInHand(hand).isEmpty()&&hand==InteractionHand.MAIN_HAND){
                if(!Authority.mayChange(p.level(),p,hit.getBlockPos()))return InteractionResult.FAIL;
                var center=ring.get(ORIGIN);if(center==null||!center.dimension().equals(level.dimension())){ring.set(ORIGIN,GlobalPos.of(level.dimension(),hit.getBlockPos()));ring.remove(CURSORS);p.sendOverlayMessage(Component.literal("Loki origin selected. Sneak-click other blocks, then the origin to finish."));}
                else if(center.pos().equals(hit.getBlockPos())){ring.remove(ORIGIN);p.sendOverlayMessage(Component.literal("Loki pattern ready. Sneak-place or mine matching blocks to repeat it."));}
                else{var delta=hit.getBlockPos().subtract(center.pos());if(delta.distSqr(BlockPos.ZERO)>64*64)return InteractionResult.FAIL;var offsets=new ArrayList<>(ring.getOrDefault(CURSORS,List.<BlockPos>of()));if(!offsets.remove(delta)&&offsets.size()<64)offsets.add(delta);ring.set(CURSORS,List.copyOf(offsets));p.sendOverlayMessage(Component.literal(offsets.size()+" repeated positions"));}return InteractionResult.SUCCESS;
            }
            if(ring.has(ORIGIN)||!(p.getItemInHand(hand).getItem() instanceof BlockItem))return InteractionResult.PASS;
            int count=0;var positions=ring.getOrDefault(CURSORS,List.<BlockPos>of());int cost=positions.size()>10?positions.size():Math.min(positions.size(),(int)Math.exp(positions.size()*.25));
            REPLICATING.add(p.getUUID());try{for(var offset:positions){var at=hit.getBlockPos().offset(offset);var to=at.relative(hit.getDirection());var material=p.getItemInHand(hand);
                if(material.isEmpty()||BotanicalManaItems.available(p)<cost)break;if(!level.hasChunkAt(at)||!level.hasChunkAt(to)||p.distanceToSqr(Vec3.atCenterOf(at))>4096||!Authority.mayChange(p.level(),p,at)||!Authority.mayChange(p.level(),p,to)||!level.getWorldBorder().isWithinBounds(to))continue;
                // Only placement is repeated; container/command/tool interactions are never replayed.
                var copy=material.copyWithCount(1);var context=new net.minecraft.world.item.context.BlockPlaceContext(new UseOnContext(level,p,hand,copy,new BlockHitResult(hit.getLocation().add(offset.getX(),offset.getY(),offset.getZ()),hit.getDirection(),at,false)));
                var block=((BlockItem)copy.getItem()).getBlock();if(block instanceof net.minecraft.world.level.block.DoorBlock||block instanceof net.minecraft.world.level.block.BedBlock||block instanceof net.minecraft.world.level.block.DoublePlantBlock)continue;
                if(!context.canPlace()||!BotanicalManaItems.pay(p,cost))continue;
                if(((BlockItem)copy.getItem()).place(context).consumesAction()){if(!p.hasInfiniteMaterials())material.shrink(1);count++;}
            }}finally{REPLICATING.remove(p.getUUID());}return count>0?InteractionResult.SUCCESS:InteractionResult.PASS;
        });
    }
    public static boolean replicating(Player p){return REPLICATING.contains(p.getUUID());}
    public static ItemStack loki(Player p){return equipped(p,"loki_ring");}
    static boolean valid(Player p,ItemStack s){var owner=s.get(OWNER);return owner==null||owner.equals(p.getUUID());}
    static boolean bind(ServerPlayer p,ItemStack s){if(!valid(p,s))return false;if(!s.has(OWNER))s.set(OWNER,p.getUUID());return true;}
    public static ItemStack bound(ServerPlayer p,String name){var s=new ItemStack(Jugtania.item(name));s.set(OWNER,p.getUUID());return s;}
    private static ItemStack equipped(Player p,String name){final ItemStack[] found={ItemStack.EMPTY};TrinketsApi.getAttachment(p).forEach((slot,s)->{if(!slot.cosmetic()&&slot.canApplyEffects()&&s.getItem() instanceof BotanicalRelics relic&&relic.kind.equals(name)&&valid(p,s))found[0]=s;});return found[0];}
    public static boolean worn(Player p,String name){return p.isAlive()&&!p.isSpectator()&&!equipped(p,name).isEmpty();}
    public static boolean carries(Player p,String name){for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(s.is(Jugtania.item(name))&&valid(p,s))return true;}return false;}
    @Override public boolean canEquip(ItemStack stack,TrinketSlotAccess slot,LivingEntity wearer){return kind.endsWith("_ring")&&wearer instanceof Player p&&valid(p,stack);}
    @Override public void forEachTrinketModifier(ItemStack stack,TrinketSlotAccess slot,LivingEntity wearer,Identifier id,java.util.function.BiConsumer<Holder<Attribute>,AttributeModifier> out){if(!slot.cosmetic()&&slot.canApplyEffects()&&kind.equals("odin_ring")&&wearer instanceof Player p&&valid(p,stack))out.accept(Attributes.MAX_HEALTH,new AttributeModifier(Jugcraft.id("botanical_odin_health"),20,AttributeModifier.Operation.ADD_VALUE));}
    @Override public InteractionResult use(Level level,Player user,InteractionHand hand){
        var stack=user.getItemInHand(hand);if(!valid(user,stack))return InteractionResult.FAIL;
        if(kind.equals("dice")){if(user instanceof ServerPlayer p&&bind(p,stack)){
            int obtained=p.getAttachedOrElse(COLLECTED,0);var choices=new ArrayList<Integer>();for(int i=0;i<6;i++)if((obtained&(1<<i))==0)choices.add(i);
            if(!choices.isEmpty()){int index=choices.get(p.getRandom().nextInt(choices.size()));p.setItemInHand(hand,bound(p,RELICS.get(index)));p.setAttached(COLLECTED,obtained|1<<index);}
            else{int roll=1+p.getRandom().nextInt(6);var table=p.level().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Jugcraft.id("jugtania_dice/roll_"+roll)));var params=new LootParams.Builder(p.level()).withParameter(LootContextParams.ORIGIN,p.position()).withParameter(LootContextParams.THIS_ENTITY,p).create(LootContextParamSets.GIFT);var rewards=table.getRandomItems(params);stack.shrink(1);for(var reward:rewards)p.getInventory().placeItemBackInInventory(reward,net.minecraft.util.Prediction.SERVER_ONLY);}
            level.playSound(null,p.blockPosition(),BotanicalSounds.get("dice_of_fate"),SoundSource.PLAYERS,1,1);
        }return InteractionResult.SUCCESS;}
        if(kind.equals("infinite_fruit")&&!user.canEat(false))return InteractionResult.PASS;
        if(kind.equals("infinite_fruit")||kind.equals("flugel_eye")||kind.equals("king_key")){if(user instanceof ServerPlayer p){if(!bind(p,stack))return InteractionResult.FAIL;if(kind.equals("king_key"))CHARGES.put(p,0);}user.startUsingItem(hand);return InteractionResult.CONSUME;}
        return InteractionResult.PASS;
    }
    @Override public InteractionResult useOn(UseOnContext c){
        if(!kind.equals("flugel_eye")||c.getPlayer()==null||!c.getPlayer().isShiftKeyDown())return InteractionResult.PASS;if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(!bind(p,c.getItemInHand())||!Authority.mayChange(p.level(),p,c.getClickedPos()))return InteractionResult.FAIL;var entries=new ArrayList<>(c.getItemInHand().getOrDefault(DESTINATIONS,List.<GlobalPos>of()));entries.removeIf(at->at.dimension().equals(p.level().dimension()));if(entries.size()>=32)entries.removeFirst();entries.add(GlobalPos.of(p.level().dimension(),c.getClickedPos().above()));c.getItemInHand().set(DESTINATIONS,List.copyOf(entries));p.sendOverlayMessage(Component.literal("Recall bound for this dimension."));return InteractionResult.SUCCESS;
    }
    @Override public int getUseDuration(ItemStack s,LivingEntity e){return kind.equals("flugel_eye")?40:72000;}
    @Override public ItemUseAnimation getUseAnimation(ItemStack s){return kind.equals("infinite_fruit")?(s.getHoverName().getString().equalsIgnoreCase("das boot")?ItemUseAnimation.DRINK:ItemUseAnimation.EAT):ItemUseAnimation.BOW;}
    @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){
        if(!(entity instanceof ServerPlayer p)||!valid(p,stack))return;
        if(kind.equals("infinite_fruit")&&remaining%5==0){if(!p.canEat(false)||!BotanicalManaItems.pay(p,2500)){p.stopUsingItem();return;}p.getFoodData().eat(2,2.4F);}
        if(kind.equals("king_key")){int spawned=CHARGES.getOrDefault(p,0);if(spawned<20&&BotanicalManaItems.available(p)>=150&&BotanicalRoyalWeapon.summon(p,stack,spawned)){BotanicalManaItems.pay(p,150);CHARGES.put(p,spawned+1);}}
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user){
        if(!kind.equals("flugel_eye")||!(user instanceof ServerPlayer p)||!valid(p,stack))return stack;
        var bound=stack.getOrDefault(DESTINATIONS,List.<GlobalPos>of()).stream().filter(at->at.dimension().equals(level.dimension())).findFirst().orElse(null);if(bound==null)return stack;var at=bound.pos();
        if(!level.hasChunkAt(at)||!level.getWorldBorder().isWithinBounds(at)||!Authority.mayChange(p.level(),p,at)||!Authority.mayChange(p.level(),p,p.blockPosition())){p.sendOverlayMessage(Component.literal("The destination must be loaded and accessible."));return stack;}
        var destination=new Vec3(at.getX()+.5,at.getY()+.1,at.getZ()+.5);var box=p.getBoundingBox().move(destination.subtract(p.position()));
        int price=(int)Math.min(Integer.MAX_VALUE,Math.ceil(p.position().distanceTo(destination)*10));
        if(level.noCollision(p,box)&&!level.containsAnyLiquid(box)&&!level.getBlockState(at.below()).getCollisionShape(level,at.below()).isEmpty()&&BotanicalManaItems.pay(p,price)){p.teleportTo(destination.x,destination.y,destination.z);p.resetFallDistance();level.playSound(null,at,BotanicalSounds.get("flugel_eye_teleport"),SoundSource.PLAYERS,1,1);}return stack;
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,net.minecraft.world.item.component.TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){if(s.has(OWNER))out.accept(Component.literal("Bound to one adventurer"));if(kind.equals("loki_ring"))out.accept(Component.literal("Wear: sneak-click empty handed to mark a repeat pattern (64 positions)."));}
}
