package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/** Supplied tool functions use native tool components and the carried botanical mana store. */
public final class BotanicalEquipment {
    private BotanicalEquipment(){}
    public interface ManaGear { int manaCost(); }
    public static final Set<String> TOOLS=Set.of("manasteel_pick","manasteel_axe","manasteel_shovel","manasteel_hoe","manasteel_sword","manasteel_shears","soulscribe","ender_dagger","livingwood_bow","crystal_bow","elementium_pickaxe","elementium_shovel","elementium_hoe","elementium_shears");
    private static final ToolMaterial MANASTEEL=new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL,300,6.2F,2,20,TagKey.create(Registries.ITEM,Jugcraft.id("jugtania_manasteel_repair")));
    private static final ToolMaterial ELEMENTIUM=new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL,720,6.2F,2,20,TagKey.create(Registries.ITEM,Jugcraft.id("jugtania_elementium_repair")));
    private static final Set<UUID> CLEARING=new HashSet<>();
    public static Item create(String name,Item.Properties p){
        var material=name.startsWith("elementium_")?ELEMENTIUM:MANASTEEL;
        if(name.endsWith("_shears"))return new ManaShears(name.startsWith("elementium_"),p.durability(material.durability()).enchantable(20).repairable(material.repairItems()));
        if(name.endsWith("_bow"))return new ManaBow(name.equals("crystal_bow"),p.durability(300).enchantable(20).repairable(TagKey.create(Registries.ITEM,Jugcraft.id("jugtania_livingwood_repair"))));
        p=switch(name){
            case "manasteel_pick","elementium_pickaxe"->p.pickaxe(material,1,-2.8F);
            case "manasteel_axe"->p.axe(MANASTEEL,6,-3.1F);
            case "manasteel_shovel","elementium_shovel"->p.shovel(material,1.5F,-3);
            case "manasteel_hoe","elementium_hoe"->p.hoe(material,-2,-1);
            case "soulscribe","ender_dagger"->p.sword(MANASTEEL,3,-1.25F).durability(69);
            default->p.sword(MANASTEEL,3,-2.4F);
        };
        return new Tool(name,p);
    }
    public static long available(Player player){return BotanicalManaItems.available(player);}
    public static boolean pay(ServerPlayer player,int amount){return BotanicalManaItems.pay(player,BotanicalArmor.price(player,amount));}
    public static int damage(ServerPlayer player,ItemStack stack,int amount){
        if(player==null||amount<=0||!(stack.getItem() instanceof ManaGear gear)||gear.manaCost()==0)return amount;
        long price=(long)amount*gear.manaCost();return price<=Integer.MAX_VALUE&&pay(player,(int)price)?0:amount;
    }
    public static void register(){
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((world,actor,pos,state,be)->{
            if(!(actor instanceof ServerPlayer p)||p.isShiftKeyDown()||!p.getMainHandItem().is(Jugtania.item("elementium_shovel"))||!(state.getBlock() instanceof net.minecraft.world.level.block.FallingBlock)||!CLEARING.add(p.getUUID()))return;
            try{
                var held=p.getMainHandItem();
                for(int dy=-12;dy<=12;dy++){
                    if(dy==0)continue;var at=pos.offset(0,dy,0);
                    if(held.isEmpty()||p.getMainHandItem()!=held)break;
                    if(!world.hasChunkAt(at)||!world.getBlockState(at).is(state.getBlock())||!Authority.mayChange(p.level(),p,at))continue;
                    p.gameMode.destroyBlock(at);
                }
            }finally{CLEARING.remove(p.getUUID());}
        });
        // InventoryTick only covers equipped slots on this platform. Scan each carried stack once.
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers()){
            if(!p.isAlive()||p.isSpectator())continue;BotanicalManaItems.tick(p);BotanicalArmor.tick(p);
            for(int i=0;i<p.getInventory().getContainerSize();i++){
                var s=p.getInventory().getItem(i);BotanicalHalos.tick(p,s);BotanicalUtilityItems.tick(p,s);BotanicalBlackHole.tick(p,s);
                if(s.getDamageValue()>0&&s.getItem() instanceof ManaGear gear&&gear.manaCost()>0&&pay(p,gear.manaCost()*2))s.setDamageValue(s.getDamageValue()-1);
            }
        }});
    }
    private static InteractionResult plant(UseOnContext context,boolean torch){
        if(!(context.getPlayer() instanceof ServerPlayer player))return InteractionResult.PASS;
        if(context.getHand()==InteractionHand.MAIN_HAND&&player.getOffhandItem().getItem() instanceof BlockItem)return InteractionResult.PASS;
        var level=player.level();var target=context.getClickedPos();
        if(!Authority.mayChange(level,player,target)||!Authority.mayChange(level,player,target.relative(context.getClickedFace())))return InteractionResult.FAIL;
        for(int i=0;i<player.getInventory().getContainerSize();i++){
            var source=player.getInventory().getItem(i);
            if(!(source.getItem() instanceof BlockItem block)||source.isEmpty())continue;
            boolean suitable=torch?(source.is(Items.TORCH)||source.is(Items.SOUL_TORCH)) : block.getBlock().defaultBlockState().is(BlockTags.SAPLINGS);
            if(!suitable)continue;
            var hit=new BlockHitResult(context.getClickLocation(),context.getClickedFace(),target,context.isInside());
            var result=source.useOn(new UseOnContext(level,player,context.getHand(),source,hit));
            if(result.consumesAction()){player.getInventory().setChanged();return result;}
        }return InteractionResult.PASS;
    }
    public static final class Tool extends Item implements ManaGear {
        private final String name;
        Tool(String name,Properties p){super(p);this.name=name;}
        @Override public int manaCost(){return (name.equals("soulscribe")||name.equals("ender_dagger"))?0:60;}
        @Override public InteractionResult useOn(UseOnContext context){
            if(name.equals("manasteel_pick")||name.equals("elementium_pickaxe")||name.equals("manasteel_axe")){
                var result=plant(context,!name.endsWith("axe")||name.endsWith("pickaxe"));if(result.consumesAction())return result;
            }
            if(context.getPlayer() instanceof ServerPlayer p&&!Authority.mayChange(p.level(),p,context.getClickedPos()))return InteractionResult.FAIL;
            var result=super.useOn(context);
            if(name.equals("elementium_hoe")&&result.consumesAction()&&context.getPlayer() instanceof ServerPlayer p){
                var state=p.level().getBlockState(context.getClickedPos());
                if(state.hasProperty(net.minecraft.world.level.block.FarmlandBlock.MOISTURE))p.level().setBlockAndUpdate(context.getClickedPos(),state.setValue(net.minecraft.world.level.block.FarmlandBlock.MOISTURE,7));
            }return result;
        }
        @Override public void hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){
            super.hurtEnemy(stack,target,attacker);
            if((name.equals("soulscribe")||name.equals("ender_dagger"))&&target instanceof Enderman&&attacker instanceof ServerPlayer p&&Authority.mayStrike(p,target)&&Authority.mayChange(p.level(),p,target.blockPosition()))
                target.hurtServer(p.level(),p.damageSources().playerAttack(p),20);
        }
    }
    public static final class ManaShears extends ShearsItem implements ManaGear {
        private final boolean area;
        ManaShears(boolean area,Properties p){super(p);this.area=area;}
        @Override public int manaCost(){return 30;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){if(!area)return super.use(level,player,hand);player.startUsingItem(hand);return InteractionResult.CONSUME;}
        @Override public ItemUseAnimation getUseAnimation(ItemStack stack){return area?ItemUseAnimation.BOW:super.getUseAnimation(stack);}
        @Override public int getUseDuration(ItemStack stack,LivingEntity user){return area?72000:super.getUseDuration(stack,user);}
        @Override public void onUseTick(Level level,LivingEntity user,ItemStack stack,int remaining){
            if(!area||remaining%5!=0||!(user instanceof ServerPlayer p))return;
            int examined=0;
            for(var target:level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(12))){
                if(++examined>64)break;
                if(!(target instanceof Shearable s)||!s.readyForShearing()||!Authority.mayChange(p.level(),p,target.blockPosition())||!p.hasLineOfSight(target))continue;
                s.shear(p.level(),net.minecraft.sounds.SoundSource.PLAYERS,stack);stack.hurtAndBreak(1,p,p.getUsedItemHand().asEquipmentSlot());break;
            }
        }
        @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){
            if(!(target instanceof Shearable s)||!s.readyForShearing())return InteractionResult.PASS;
            if(player instanceof ServerPlayer p){
                if(!Authority.mayChange(p.level(),p,target.blockPosition()))return InteractionResult.FAIL;
                s.shear(p.level(),net.minecraft.sounds.SoundSource.PLAYERS,stack);stack.hurtAndBreak(1,p,hand.asEquipmentSlot());
            }return InteractionResult.SUCCESS;
        }
    }
    public static final class ManaBow extends BowItem implements ManaGear {
        private final boolean crystal;
        ManaBow(boolean crystal,Properties p){super(p);this.crystal=crystal;}
        @Override public int manaCost(){return crystal?200:40;}
        private int arrowCost(Player p,ItemStack bow){
            boolean infinity=p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(net.minecraft.world.item.enchantment.Enchantments.INFINITY).map(e->net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(e,bow)>0).orElse(false);
            return infinity?100:200;
        }
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(!crystal)return super.use(level,player,hand);
            if(!player.hasInfiniteMaterials()&&available(player)<arrowCost(player,player.getItemInHand(hand)))return InteractionResult.FAIL;
            player.startUsingItem(hand);return InteractionResult.CONSUME;
        }
        @Override protected int getDurabilityUse(ItemStack stack){return crystal?0:super.getDurabilityUse(stack);}
        @Override protected net.minecraft.world.entity.projectile.Projectile createProjectile(Level level,LivingEntity shooter,ItemStack weapon,ItemStack ammo,boolean critical){
            var projectile=super.createProjectile(level,shooter,weapon,ammo,critical);
            if(crystal&&ammo.is(Items.ARROW)&&projectile instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow)arrow.pickup=net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.CREATIVE_ONLY;
            return projectile;
        }
        @Override public boolean releaseUsing(ItemStack stack,Level level,LivingEntity entity,int remaining){
            if(!crystal)return super.releaseUsing(stack,level,entity,remaining);
            if(!(entity instanceof ServerPlayer p))return false;
            float power=BowItem.getPowerForTime((getUseDuration(stack,p)-remaining)*2);if(power<.1F)return false;
            int cost=arrowCost(p,stack);if(!p.hasInfiniteMaterials()&&!pay(p,cost))return false;
            var ammo=p.getProjectile(stack);
            // Ordinary arrows are materialized; tipped/spectral ammunition is consumed natively.
            var projectiles=ammo.isEmpty()||ammo.is(Items.ARROW)?List.of(new ItemStack(Items.ARROW)):draw(stack,ammo,p);
            if(projectiles.isEmpty())return false;
            shoot(p.level(),p,p.getUsedItemHand(),stack,projectiles,power*3,1,power==1,null);
            level.playSound(null,p.getX(),p.getY(),p.getZ(),net.minecraft.sounds.SoundEvents.ARROW_SHOOT,net.minecraft.sounds.SoundSource.PLAYERS,1,1/(p.getRandom().nextFloat()*.4F+1.2F)+power*.5F);
            p.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));return true;
        }
    }
}
