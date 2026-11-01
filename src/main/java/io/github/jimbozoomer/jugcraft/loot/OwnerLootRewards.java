package io.github.jimbozoomer.jugcraft.loot;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.*;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;

/** Shared server-only journal for block/entity rewards. Personal inventories never become hopper inventories. */
public final class OwnerLootRewards {
    public interface Host {
        boolean valid(Player player);
        Vec3 origin();
        int capacity();
        boolean archaeology();
        Component name();
        void changed();
        void opened(boolean open);
    }
    private static final Codec<Map<UUID,List<ItemStack>>> CONTENTS=Codec.unboundedMap(UUIDUtil.STRING_CODEC,ItemStack.OPTIONAL_CODEC.listOf());
    private static final Codec<Map<UUID,UUID>> OWNERS=Codec.unboundedMap(UUIDUtil.STRING_CODEC,UUIDUtil.STRING_CODEC);
    private static final Codec<Map<UUID,Long>> TIMES=Codec.unboundedMap(UUIDUtil.STRING_CODEC,Codec.LONG);
    private final Host host;
    private final Map<UUID,Inventory> inventories=new HashMap<>();
    private final Map<UUID,UUID> bindings=new HashMap<>();
    private final Map<UUID,Long> rolledAt=new HashMap<>();
    private final Set<UUID> seen=new HashSet<>();
    private ResourceKey<LootTable> table;
    private long seed,firstRoll=-1;
    private boolean loading;
    public OwnerLootRewards(Host host){this.host=host;}
    public boolean configured(){return table!=null;}
    public Set<UUID> seen(){return Set.copyOf(seen);}
    public void configure(ResourceKey<LootTable> table,long seed){if(configured()||!inventories.isEmpty())throw new IllegalStateException("Personal loot already configured");this.table=table;this.seed=seed;host.changed();}
    private static long ticks(String key){try{return Math.clamp(Long.parseLong(JugcraftConfig.textOption(key)),0,20L*60*60*24*365);}catch(NumberFormatException e){return 0;}}
    private UUID initialOwner(UUID player){return JugcraftConfig.option("loot.party_shared")?JugcraftParties.manager().partyOf(player).map(p->p.id()).orElse(player):player;}
    public void open(ServerPlayer player){
        if(!host.valid(player))return;
        if(table==null){player.sendOverlayMessage(Component.literal("No personal loot table is assigned."));return;}
        UUID key=bindings.getOrDefault(player.getUUID(),initialOwner(player.getUUID()));
        if(!key.equals(player.getUUID())&&!JugcraftParties.partyId(player.getUUID()).filter(key::equals).isPresent()){player.sendOverlayMessage(Component.literal("This cache belongs to the party you first visited it with."));return;}
        Inventory inventory=inventories.get(key);long now=player.level().getGameTime(),decay=ticks("loot.decay_ticks"),refresh=ticks("loot.refresh_ticks");
        boolean expired=decay>0&&firstRoll>=0&&now-firstRoll>=decay;
        if(inventory==null&&expired){player.sendOverlayMessage(Component.literal("This reward cache has expired."));return;}
        boolean refill=inventory!=null&&!expired&&refresh>0&&inventory.isEmpty()&&inventory.viewers.isEmpty()&&now-rolledAt.getOrDefault(key,now)>=refresh;
        if(inventory==null||refill){
            if(inventory==null)inventory=new Inventory(key);
            var builder=new LootParams.Builder(player.level()).withParameter(LootContextParams.ORIGIN,host.origin()).withOptionalParameter(LootContextParams.THIS_ENTITY,player).withLuck(player.getLuck());
            var params=builder.create(host.archaeology()?LootContextParamSets.ARCHAEOLOGY:LootContextParamSets.CHEST);
            long generation=rolledAt.containsKey(key)?now:0;
            long roll=seed^key.getMostSignificantBits()^key.getLeastSignificantBits()^generation;
            player.level().getServer().reloadableRegistries().getLootTable(table).fill(inventory,params,roll==0?1:roll);
            inventories.put(key,inventory);rolledAt.put(key,now);if(firstRoll<0)firstRoll=now;
        }
        // First encounter binds a player permanently; changing parties cannot reroll this cache.
        rolledAt.putIfAbsent(key,now);if(firstRoll<0)firstRoll=now;
        bindings.putIfAbsent(player.getUUID(),key);seen.add(player.getUUID());host.changed();
        var contents=inventory;
        player.openMenu(new SimpleMenuProvider((id,inv,p)->new ChestMenu(host.capacity()==9?MenuType.GENERIC_9x1:MenuType.GENERIC_9x3,id,inv,contents,host.capacity()/9),host.name()));
    }
    public void tick(net.minecraft.server.MinecraftServer server){
        for(var inv:inventories.values())inv.viewers.removeIf(id->{var p=server.getPlayerList().getPlayer(id);return p==null||!inv.stillValid(p)||p.containerMenu.slots.stream().noneMatch(s->s.container==inv);});
        host.opened(inventories.values().stream().anyMatch(i->!i.viewers.isEmpty()));
    }
    private final class Inventory extends SimpleContainer {
        final UUID owner;final Set<UUID> viewers=new HashSet<>();
        Inventory(UUID owner){super(host.capacity());this.owner=owner;}
        @Override public boolean stillValid(Player p){return owner.equals(bindings.get(p.getUUID()))&&(owner.equals(p.getUUID())||JugcraftParties.partyId(p.getUUID()).filter(owner::equals).isPresent())&&host.valid(p);}
        @Override public void setChanged(){super.setChanged();if(!loading)host.changed();}
        @Override public void startOpen(ContainerUser u){if(u.getLivingEntity() instanceof Player p&&stillValid(p)){viewers.add(p.getUUID());host.opened(true);}}
        @Override public void stopOpen(ContainerUser u){viewers.remove(u.getLivingEntity().getUUID());host.opened(inventories.values().stream().anyMatch(i->!i.viewers.isEmpty()));}
    }
    public void save(ValueOutput out){
        if(table!=null)out.store("PersonalLootTable",ResourceKey.codec(Registries.LOOT_TABLE),table);
        out.putLong("Seed",seed);out.putLong("FirstRoll",firstRoll);
        var saved=new HashMap<UUID,List<ItemStack>>();inventories.forEach((id,inv)->{var list=new ArrayList<ItemStack>();for(int i=0;i<inv.getContainerSize();i++)list.add(inv.getItem(i));saved.put(id,list);});
        out.store("PersonalInventories",CONTENTS,saved);out.store("LootBindings",OWNERS,bindings);out.store("LootRolledAt",TIMES,rolledAt);out.store("Seen",UUIDUtil.CODEC.listOf(),List.copyOf(seen));
    }
    public void load(ValueInput in){
        table=in.read("PersonalLootTable",ResourceKey.codec(Registries.LOOT_TABLE)).orElse(null);seed=in.getLongOr("Seed",0);firstRoll=in.getLongOr("FirstRoll",-1);
        inventories.clear();bindings.clear();rolledAt.clear();seen.clear();loading=true;
        var savedBindings=in.read("LootBindings",OWNERS);
        try{in.read("PersonalInventories",CONTENTS).orElse(Map.of()).forEach((id,items)->{var inv=new Inventory(id);for(int i=0;i<Math.min(items.size(),host.capacity());i++)inv.setItem(i,items.get(i));inventories.put(id,inv);if(savedBindings.isEmpty()){bindings.put(id,id);seen.add(id);}});}finally{loading=false;}
        bindings.putAll(savedBindings.orElse(Map.of()));rolledAt.putAll(in.read("LootRolledAt",TIMES).orElse(Map.of()));seen.addAll(in.read("Seen",UUIDUtil.CODEC.listOf()).orElse(List.of()));
    }
}
