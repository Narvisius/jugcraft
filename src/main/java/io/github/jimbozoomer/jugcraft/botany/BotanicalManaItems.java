package io.github.jimbozoomer.jugcraft.botany;

import com.mojang.serialization.Codec;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

/** Shared portable mana accounting. Mirrors debit the real loaded pool, never a cached balance. */
public final class BotanicalManaItems {
    public static DataComponentType<Integer> RESERVE;
    static final Set<String> ITEMS=Set.of("mana_ring","mana_ring_greater","aura_ring","aura_ring_greater","mana_mirror");
    static void register(){RESERVE=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_reserve"),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT).build());}
    static Item create(String name,Item.Properties p){return name.equals("mana_mirror")?new Mirror(p.stacksTo(1)):new Ring(name,p.stacksTo(1));}
    public static int capacity(ItemStack s){if(s.getItem() instanceof BotanicalTools.Tablet)return 500000;if(s.getItem() instanceof Ring r)return r.name.equals("mana_ring")?500000:r.name.equals("mana_ring_greater")?2000000:0;return s.is(Jugtania.item("terra_pick"))?Integer.MAX_VALUE:0;}
    public static int stored(ItemStack s){return Math.clamp(s.getItem() instanceof BotanicalTools.Tablet?BotanicalTools.tablet(s):s.getOrDefault(RESERVE,0),0,capacity(s));}
    public static void stored(ItemStack s,int value){value=Math.clamp(value,0,capacity(s));if(s.getItem() instanceof BotanicalTools.Tablet)BotanicalTools.tablet(s,value);else s.set(RESERVE,value);}
    private record Entry(ItemStack stack,Runnable dirty){}
    private static List<Entry> carried(Player player){
        var list=new ArrayList<Entry>();var seen=Collections.newSetFromMap(new IdentityHashMap<ItemStack,Boolean>());
        for(int i=0;i<player.getInventory().getContainerSize();i++){var s=player.getInventory().getItem(i);if(!s.isEmpty()&&seen.add(s))list.add(new Entry(s,player.getInventory()::setChanged));}
        TrinketsApi.getAttachment(player).forEach((slot,s)->{if(!s.isEmpty()&&!slot.cosmetic()&&slot.canApplyEffects()&&seen.add(s))list.add(new Entry(s,()->slot.set(s)));});return list;
    }
    private static BotanicalWorkshop mirrorPool(ServerPlayer player,ItemStack stack){
        if(!(stack.getItem() instanceof Mirror))return null;var link=stack.get(BotanicalTools.LINK);if(link==null)return null;var level=player.level().getServer().getLevel(link.dimension());
        if(level==null||!level.hasChunkAt(link.pos())||!(level.getBlockEntity(link.pos()) instanceof BotanicalWorkshop pool)||!pool.kind().endsWith("pool")||!pool.permitted(player))return null;
        // Remote use is limited by the same claim endpoints even when no physical cable is required.
        if(level==player.level()&&!ClaimTransfers.allowed(level,link.pos(),player.blockPosition())||!Authority.mayChange(player.level(),player,player.blockPosition()))return null;return pool;
    }
    public static long available(Player player){
        long total=0;var pools=Collections.newSetFromMap(new IdentityHashMap<BotanicalWorkshop,Boolean>());
        for(var e:carried(player)){if(!e.stack.is(Jugtania.item("terra_pick")))total+=stored(e.stack);if(player instanceof ServerPlayer p){var pool=mirrorPool(p,e.stack);if(pool!=null&&pools.add(pool))total+=pool.mana();}}return total;
    }
    public static boolean pay(ServerPlayer player,int amount){
        if(amount<0||!player.isAlive()||player.isSpectator()||available(player)<amount)return false;
        var pools=Collections.newSetFromMap(new IdentityHashMap<BotanicalWorkshop,Boolean>());
        for(var e:carried(player)){
            if(amount==0)break;if(!e.stack.is(Jugtania.item("terra_pick"))){int take=Math.min(amount,stored(e.stack));if(take>0){stored(e.stack,stored(e.stack)-take);amount-=take;e.dirty.run();}}
            var pool=mirrorPool(player,e.stack);if(amount>0&&pool!=null&&pools.add(pool)){int take=Math.min(amount,pool.mana());if(pool.draw(take))amount-=take;}
        }return amount==0;
    }
    public static int fill(Player player,int amount){int before=amount;for(var e:carried(player)){int room=capacity(e.stack)-stored(e.stack),give=Math.min(amount,room);if(give>0){stored(e.stack,stored(e.stack)+give);e.dirty.run();amount-=give;}if(amount<=0)break;}return before-amount;}
    static void tick(ServerPlayer p){
        if(!p.isAlive()||p.isSpectator())return;
        // Each worn aura ring contributes; cosmetic slots never produce mana.
        TrinketsApi.getAttachment(p).forEach((slot,stack)->{if(!slot.cosmetic()&&slot.canApplyEffects()&&stack.getItem() instanceof Ring r){int interval=r.name.equals("aura_ring")?50:r.name.equals("aura_ring_greater")?10:0;if(interval>0&&p.tickCount%interval==0)fill(p,5);}});
    }
    private static final class Ring extends Item implements TrinketCallback {
        final String name;Ring(String name,Properties p){super(p);this.name=name;}
        @Override public boolean canEquip(ItemStack stack,TrinketSlotAccess slot,LivingEntity wearer){return wearer instanceof Player;}
        @Override public boolean isBarVisible(ItemStack stack){return capacity(stack)>0;}
        @Override public int getBarWidth(ItemStack stack){return capacity(stack)==0?0:Math.round(13F*stored(stack)/capacity(stack));}
        @Override public int getBarColor(ItemStack stack){return 0x4BC8E4;}
    }
    private static final class Mirror extends Item {
        Mirror(Properties p){super(p);}
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!p.isShiftKeyDown()||!(p.level().getBlockEntity(c.getClickedPos()) instanceof BotanicalWorkshop b)||!b.kind().endsWith("pool"))return InteractionResult.PASS;
            if(!b.permitted(p))return InteractionResult.FAIL;c.getItemInHand().set(BotanicalTools.LINK,GlobalPos.of(p.level().dimension(),c.getClickedPos()));p.sendOverlayMessage(Component.literal("Mirror linked to this mana pool. It must remain loaded and accessible."));return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult use(net.minecraft.world.level.Level level,Player user,InteractionHand hand){if(user instanceof ServerPlayer p){var pool=mirrorPool(p,p.getItemInHand(hand));p.sendOverlayMessage(Component.literal(pool==null?"No accessible, loaded linked pool.":"Linked pool: "+pool.mana()+" / "+pool.capacity()+" mana"));}return InteractionResult.SUCCESS;}
    }
}
