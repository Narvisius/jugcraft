package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.AABB;

/** The source obsidian/flower/six-pylon ritual copies compatible book enchantments using mana. */
final class BotanicalEnchanter {
    private static final String[][] PATTERN={{"_P_______P_","___________","___________","P_________P","___________","___________","_P_______P_"},{"_F_______F_","___________","____F_F____","F____L____F","____F_F____","___________","_F_______F_"},{"___________","____BBB____","___B_B_B___","___BB0BB___","___B_B_B___","____BBB____","___________"}};
    private final BotanicalWorkshop host;private ItemEnchantments pending=ItemEnchantments.EMPTY;private int cost,finishTicks;private boolean active,turned;
    BotanicalEnchanter(BotanicalWorkshop host){this.host=host;}
    private static boolean layout(ServerLevel level,ServerPlayer actor,BlockPos center,boolean turned,boolean formed){
        var flowers=TagKey.create(Registries.BLOCK,Jugcraft.id("jugtania_enchanter_flowers"));
        for(int layer=0;layer<3;layer++)for(int row=0;row<7;row++)for(int column=0;column<11;column++){
            char key=PATTERN[layer][row].charAt(column);if(key=='_')continue;int x=column-5,z=row-3;var pos=center.offset(turned?-z:x,1-layer,turned?x:z);
            if(!level.hasChunkAt(pos)||!Authority.mayChange(level,actor,pos)||!ClaimTransfers.allowed(level,center,pos))return false;var state=level.getBlockState(pos);
            boolean match=switch(key){case 'P'->state.is(Jugtania.block("mana_pylon"));case 'F'->state.is(flowers);case 'L'->state.is(formed?Jugtania.block("enchanter"):Blocks.LAPIS_BLOCK);default->state.is(Blocks.OBSIDIAN)||state.is(Blocks.CRYING_OBSIDIAN);};if(!match)return false;
        }return true;
    }
    static boolean form(ServerPlayer actor,BlockPos pos){
        var level=actor.level();if(!level.getBlockState(pos).is(Blocks.LAPIS_BLOCK))return false;
        for(boolean turned:new boolean[]{false,true})if(layout(level,actor,pos,turned,false)){
            if(!level.setBlockAndUpdate(pos,Jugtania.block("enchanter").defaultBlockState()))return false;
            if(level.getBlockEntity(pos) instanceof BotanicalWorkshop workshop){workshop.owner(actor.getUUID());workshop.enchanter.turned=turned;workshop.changed();}
            level.playSound(null,pos,BotanicalSounds.get("enchanter_form"),net.minecraft.sounds.SoundSource.BLOCKS,1,1);return true;
        }return false;
    }
    InteractionResult use(ServerPlayer actor,InteractionHand hand){
        if(active){actor.sendOverlayMessage(Component.literal(status()));return InteractionResult.SUCCESS;}
        var stored=host.getItem(0);var held=actor.getItemInHand(hand);
        if(!stored.isEmpty()){actor.getInventory().placeItemBackInInventory(stored.copy(),net.minecraft.util.Prediction.SERVER_ONLY);host.setItem(0,ItemStack.EMPTY);return InteractionResult.SUCCESS;}
        if(held.isEmpty()||held.getCount()!=1||held.is(Items.BOOK)||!held.isEnchantable())return InteractionResult.FAIL;
        host.setItem(0,held.copy());if(!actor.getAbilities().instabuild)held.shrink(1);return InteractionResult.SUCCESS;
    }
    String status(){return active?"Mana enchanting: "+host.mana()+" / "+cost+(finishTicks>0?" · Infusing":""):"Place unenchanted equipment, drop enchanted books nearby, then use the wand. Books are retained.";}
    void wand(ServerPlayer actor){
        if(active){actor.sendOverlayMessage(Component.literal(status()));return;}var item=host.getItem(0);if(item.isEmpty()||!item.isEnchantable())return;
        if(!layout(actor.level(),actor,host.getBlockPos(),turned,true)){actor.sendOverlayMessage(Component.literal("The six-pylon enchanting structure is incomplete."));return;}
        var selected=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);var selectedKeys=new ArrayList<Holder<Enchantment>>();
        var books=actor.level().getEntitiesOfClass(ItemEntity.class,new AABB(host.getBlockPos()).inflate(2,0,2),d->d.isAlive()&&d.getItem().is(Items.ENCHANTED_BOOK)&&Authority.mayChange(actor.level(),actor,d.blockPosition())&&ClaimTransfers.allowed(actor.level(),host.getBlockPos(),d.blockPosition())).stream().limit(64).sorted(Comparator.comparing(d->d.getUUID().toString())).toList();
        for(var book:books){
            var enchantments=book.getItem().getOrDefault(DataComponents.STORED_ENCHANTMENTS,ItemEnchantments.EMPTY);
            for(var entry:enchantments.entrySet()){
                var enchantment=entry.getKey();if(selectedKeys.size()>=16||selectedKeys.contains(enchantment)||!enchantment.value().canEnchant(item)||selectedKeys.stream().anyMatch(other->!Enchantment.areCompatible(enchantment,other)))continue;
                selected.set(enchantment,Math.clamp(entry.getIntValue(),1,enchantment.value().getMaxLevel()));selectedKeys.add(enchantment);
            }
        }
        pending=selected.toImmutable();if(pending.isEmpty()){actor.sendOverlayMessage(Component.literal("No compatible enchanted books found within two blocks."));return;}
        long total=0;for(var entry:pending.entrySet()){var enchant=entry.getKey();int rank=entry.getIntValue();total+=(long)(5000*(15-Math.min(15,enchant.value().getWeight()))*1.05*((3+rank*rank)*.25)*(.9+pending.size()*.05)*(enchant.is(EnchantmentTags.TREASURE)?1.25:1));}
        cost=(int)Math.clamp(total,1,20000000);active=true;finishTicks=0;host.changed();actor.sendOverlayMessage(Component.literal(status()));
    }
    int demand(){return active&&finishTicks==0?Math.max(0,cost-host.mana()):0;}
    void tick(ServerLevel level,ServerPlayer actor){
        if(level.getGameTime()%10==0&&!layout(level,actor,host.getBlockPos(),turned,true)){
            // World edits by the owner are respected; only the ritual's lapis core reverts.
            level.setBlockAndUpdate(host.getBlockPos(),Blocks.LAPIS_BLOCK.defaultBlockState());level.playSound(null,host.getBlockPos(),BotanicalSounds.get("enchanter_fade"),net.minecraft.sounds.SoundSource.BLOCKS,1,1);return;
        }
        if(!active)return;var item=host.getItem(0);if(item.isEmpty()){active=false;pending=ItemEnchantments.EMPTY;cost=0;finishTicks=0;host.changed();return;}
        if(finishTicks==0){if(host.mana()<cost)return;finishTicks=100;host.changed();}
        if(--finishTicks>0){host.setChanged();if(finishTicks%10==0)level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,host.getBlockPos().getX()+.5,host.getBlockPos().getY()+1.4,host.getBlockPos().getZ()+.5,12,.7,.5,.7,.05);return;}
        if(!host.draw(cost))return;var result=item.copy();for(var entry:pending.entrySet())result.enchant(entry.getKey(),entry.getIntValue());host.setItem(0,result);active=false;pending=ItemEnchantments.EMPTY;cost=0;host.changed();
        level.playSound(null,host.getBlockPos(),BotanicalSounds.get("enchanter_enchant"),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
    }
    void save(ValueOutput out){out.store("enchanter_pending",ItemEnchantments.CODEC,pending);out.putInt("enchanter_cost",cost);out.putInt("enchanter_finish",finishTicks);out.putBoolean("enchanter_active",active);out.putBoolean("enchanter_turned",turned);}
    void load(ValueInput in){pending=in.read("enchanter_pending",ItemEnchantments.CODEC).orElse(ItemEnchantments.EMPTY);cost=Math.clamp(in.getIntOr("enchanter_cost",0),0,20000000);finishTicks=Math.clamp(in.getIntOr("enchanter_finish",0),0,100);active=in.getBooleanOr("enchanter_active",false)&&!pending.isEmpty()&&cost>0;turned=in.getBooleanOr("enchanter_turned",false);}
}
