package io.github.jimbozoomer.jugcraft.nouveau;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.composer.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.trinket.Wayfaring;
import java.util.*;
import java.util.function.Function;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.Level;

/** Gear uses the established Focus pool, worn slots and effect authority. Owning it is sufficient to use it. */
public final class NouveauGear {
    public static DataComponentType<Integer> ARMOR_TIER,EXPERIENCE;
    public static DataComponentType<String> TOME;
    public static DataComponentType<Boolean> REACTIVE,GLAMOUR,CONJURED_ARROW;
    public static DataComponentType<UUID> ARROW_SHOT;
    public static DataComponentType<List<String>> THREADS;
    public static Function<Armor,Object> armorRenderer=a->null;
    public static java.util.function.Consumer<String> openGuide=name->{};
    private NouveauGear(){}
    private static <T> DataComponentType<T> component(String name,Codec<T> codec){return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("nouveau_"+name),DataComponentType.<T>builder().persistent(codec).networkSynchronized(ByteBufCodecs.fromCodec(codec)).build());}
    public static void register(){
        TOME=component("tome",Codec.STRING);
        ARMOR_TIER=component("armor_tier",Codec.intRange(0,2));EXPERIENCE=component("experience",Codec.intRange(0,10000));
        CONJURED_ARROW=component("conjured_arrow",Codec.BOOL);ARROW_SHOT=component("arrow_shot",UUIDUtil.CODEC);
        REACTIVE=component("reactive",Codec.BOOL);GLAMOUR=component("glamour",Codec.BOOL);THREADS=component("threads",Codec.STRING.listOf().validate(v->v.size()<=3?com.mojang.serialization.DataResult.success(v):com.mojang.serialization.DataResult.error(()->"At most three threads")));
        NouveauThreads.register();NouveauEquipment.register();
        for(var e:NouveauCatalog.ITEM_ENTRIES.values()){
            if(NouveauCatalog.ITEMS.containsKey(e.source()))continue;
            switch(e.kind()){
                case "material" -> NouveauCatalog.registerItem(e,Item::new);
                case "food" -> NouveauCatalog.registerItem(e,p->new Item(p.food(new FoodProperties.Builder().nutrition(e.source().endsWith("pie")?8:5).saturationModifier(.7F).build())));
                case "armor" -> NouveauCatalog.registerItem(e,p->{
                    int tier=e.source().startsWith("sorcerer")?0:e.source().startsWith("battlemage")?2:1;
                    ArmorType type=e.source().endsWith("hood")?ArmorType.HELMET:e.source().endsWith("robes")?ArmorType.CHESTPLATE:e.source().endsWith("leggings")?ArmorType.LEGGINGS:ArmorType.BOOTS;
                    var mat=new ArmorMaterial(22+tier*6,Map.of(ArmorType.HELMET,2+tier,ArmorType.CHESTPLATE,4+tier,ArmorType.LEGGINGS,3+tier,ArmorType.BOOTS,1+tier,ArmorType.BODY,4+tier),
                        20,SoundEvents.ARMOR_EQUIP_LEATHER,tier,0,TagKey.create(Registries.ITEM,Jugcraft.id("nouveau_armor_repair")),ResourceKey.create(EquipmentAssets.ROOT_ID,Jugcraft.id("nouveau/"+e.source().split("_")[0])));
                    return new Armor(e,p.humanoidArmor(mat,type).rarity(Rarity.RARE));
                });
                case "instrument" -> NouveauCatalog.registerItem(e,p->NouveauInstruments.create(e,p));
                case "music_disc" -> NouveauCatalog.registerItem(e,p->new Item(p.stacksTo(1).jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG,Jugcraft.id("nouveau_"+e.source().substring("music_disc_".length()))))));
                case "arrow" -> NouveauCatalog.registerItem(e,ArrowItem::new);
                case "guide","trinket","spellbook","utility","thread" -> NouveauCatalog.registerItem(e,p->new Gear(e,e.source().equals("dowsing_rod")?p.durability(32):p.stacksTo(e.kind().equals("thread")||e.source().endsWith("experience_gem")?64:1)));
                // Creatures, rituals, glyphs, potions and scrolls register their own specialized item behavior.
                default -> {}
            }
        }
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%20==0)for(ServerPlayer p:server.getPlayerList().getPlayers())pulse(p);});
    }
    public static final class Armor extends Item implements GeoItem {
        public final NouveauCatalog.Entry entry;
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        public Armor(NouveauCatalog.Entry e,Properties p){super(p);entry=e;}
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        @Override public Object getRenderProvider(){return armorRenderer.apply(this);}
    }
    public static boolean worn(Player p,String name){
        Item item=NouveauCatalog.ITEMS.get(name);return item!=null&&(p.getMainHandItem().is(item)||p.getOffhandItem().is(item)||Wayfaring.worn(p,s->s.is(item))!=null);
    }
    public static Set<String> threads(Player p){
        Set<String> result=new HashSet<>();for(EquipmentSlot slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
            var stack=p.getItemBySlot(slot);if(stack.getItem() instanceof Armor)result.addAll(stack.getOrDefault(THREADS,List.of()));
        }return result;
    }
    private static void pulse(ServerPlayer p){
        if(!JugNouveau.enabled()||!p.isAlive()||p.isSpectator())return;var threads=threads(p);
        NouveauThreads.pulse(p);
        if(worn(p,"amulet_of_mana_regen")||worn(p,"ring_of_lesser_discount")||worn(p,"ring_of_greater_discount")){
            if(p.tickCount%80==0)p.setAttached(JugcraftConcordance.FOCUS,ConcordanceProgress.focus(p).gain(ConcordanceProgress.now(p.level()),1));
        }
        if(worn(p,"jump_ring"))p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,40,1,true,false));
        if(worn(p,"belt_of_levitation")&&p.isShiftKeyDown()&&!p.onGround()&&!p.hasEffect(NouveauStatuses.EFFECTS.get("gravity"))){boolean ground=false;for(int y=1;y<=8;y++)if(p.level().getBlockState(p.blockPosition().below(y)).isSolidRender()){ground=true;break;}if(ground)p.addEffect(new MobEffectInstance(MobEffects.LEVITATION,25,0,true,false));}
        if(worn(p,"belt_of_unstable_gifts")&&p.tickCount%200==0){var gifts=List.of(MobEffects.SPEED,MobEffects.HASTE,MobEffects.REGENERATION,MobEffects.FIRE_RESISTANCE,MobEffects.NIGHT_VISION,MobEffects.WATER_BREATHING);p.addEffect(new MobEffectInstance(gifts.get(p.getRandom().nextInt(gifts.size())),160+p.getRandom().nextInt(160),p.getRandom().nextInt(2)));}
        int regen=NouveauEquipment.enchantments(p,"mana_regen");if(regen>0&&p.tickCount%Math.max(20,100-20*regen)==0)p.setAttached(JugcraftConcordance.FOCUS,ConcordanceProgress.focus(p).gain(ConcordanceProgress.now(p.level()),1));
        for(var slot:EquipmentSlot.VALUES)if(p.getItemBySlot(slot).getOrDefault(GLAMOUR,false)){p.level().sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,p.getX(),p.getY()+1,p.getZ(),3,.3,.6,.3,.02);break;}
    }
    public static int spellCost(Player p,int base){
        int discount=worn(p,"ring_of_greater_discount")?2:worn(p,"ring_of_lesser_discount")?1:0;
        return Math.max(base>0?1:0,base-discount);
    }
    public static int spellCost(Player p,io.github.jimbozoomer.jugcraft.concordance.compose.Plan plan){int cost=plan.focus();if(NouveauFamiliars.active(p).equals("whirlisprig")){long earth=plan.root().steps().stream().filter(s->NouveauThreads.earth(s.effect())).count();cost=Math.max(1,cost-(int)earth);}return spellCost(p,cost);}
    public static InteractionResult cast(ServerPlayer p,ItemStack stack){
        if(!JugNouveau.enabled()||!p.isAlive()||p.isSpectator())return InteractionResult.FAIL;
        if(stack.has(TOME))return NouveauTomes.cast(p,stack);
        // The visual composer remains the one editor. Holding a supplied spell instrument opens its vocabulary.
        ConcordanceProgress.grant(p,"jugcraft:nouveau_arcana",ResearchState.MASTERED);
        if(p.isShiftKeyDown()||!stack.has(JugcraftConcordance.INSCRIPTION)){
            Composer.receive(p,new ComposerRequestPayload(0,ComposerRequestPayload.OPEN,0,""));return InteractionResult.SUCCESS;
        }
        var instrument=ComposedSpells.instrument(p);if(instrument==null)return InteractionResult.FAIL;
        var result=ComposedSpells.compile(p,instrument,stack.get(JugcraftConcordance.INSCRIPTION).text());if(result.plan()==null)return InteractionResult.FAIL;
        var plan=result.plan();int cost=spellCost(p,plan);if(stack.is(NouveauCatalog.item("caster_tome")))cost=Math.max(1,(cost+1)/2);
        if(stack.is(NouveauCatalog.item("enchanters_mirror")))return NouveauEquipment.castAt(p,stack,NouveauEquipment.impact(p,p),1.5)?InteractionResult.SUCCESS:InteractionResult.FAIL;
        if(ConcordanceProgress.currentFocus(p)<cost||!RateGate.allow(p,"nouveau_cast",plan.cooldown()))return InteractionResult.FAIL;
        var outcome=ComposedSpells.perform(p.level(),p,plan,Cause.of(p.getUUID(),Cause.Origin.SPELL,"jugcraft:nouveau",ConcordanceEffects.nextSerial()),new Ledger(plan.limits()));
        if(outcome.applied())ConcordanceProgress.spendFocus(p,cost);
        return outcome.applied()?InteractionResult.SUCCESS:InteractionResult.FAIL;
    }
    public static final class Gear extends Item implements NouveauInstruments.Visual,eu.pb4.trinkets.api.callback.TrinketCallback {
        public final NouveauCatalog.Entry entry;
        public Gear(NouveauCatalog.Entry e,Properties p){super(p);entry=e;}
        @Override public String source(){return entry.source();}
        @Override public boolean canEquip(ItemStack stack,eu.pb4.trinkets.api.TrinketSlotAccess slot,LivingEntity entity){return entry.kind().equals("trinket")&&entity instanceof Player;}
        @Override public InteractionResult use(Level l,Player player,InteractionHand hand){
            if(entry.kind().equals("guide")&&!player.isShiftKeyDown()){if(l.isClientSide())openGuide.accept(entry.source());return InteractionResult.SUCCESS;}
            if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!JugNouveau.enabled()||p.isSpectator()||!p.isAlive())return InteractionResult.FAIL;
            var stack=p.getItemInHand(hand);String name=entry.source();
            if(entry.kind().equals("guide")&&p.isShiftKeyDown()){p.openMenu(new SimpleMenuProvider((id,inventory,who)->new NouveauFamiliarMenu(id,inventory),net.minecraft.network.chat.Component.literal("Bound Familiars")));return InteractionResult.SUCCESS;}
            if(name.equals("alchemists_crown")){p.openMenu(new SimpleMenuProvider((id,inventory,who)->new NouveauPotionMenu(id,inventory,hand,true),net.minecraft.network.chat.Component.literal("Alchemist’s Crown")));return InteractionResult.SUCCESS;}
            if(name.equals("enchanters_eye")&&!p.isShiftKeyDown()&&stack.has(JugcraftConcordance.INSCRIPTION))return NouveauScrying.castEye(p,stack)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(Set.of("spellbook","instrument").contains(entry.kind()))return cast(p,stack);
            if(entry.kind().equals("thread")){
                var armor=p.getOffhandItem();if(!(armor.getItem() instanceof Armor)||hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
                var list=new ArrayList<>(armor.getOrDefault(THREADS,List.of()));String thread=name.substring(7);
                if((Set.of("gliding","undying").contains(thread)&&armor.getOrDefault(ARMOR_TIER,0)<2)||list.contains(thread)||list.size()>=1+armor.getOrDefault(ARMOR_TIER,0))return InteractionResult.FAIL;
                list.add(thread);armor.set(THREADS,List.copyOf(list));stack.consume(1,p);return InteractionResult.SUCCESS;
            }
            if(name.equals("experience_gem")||name.equals("greater_experience_gem")){
                int cap=name.startsWith("greater")?10000:100,stored=stack.getOrDefault(EXPERIENCE,0);
                int amount=p.isShiftKeyDown()?Math.min(cap-stored,Math.min(100,p.totalExperience)):Math.min(100,stored);if(amount<=0)return InteractionResult.FAIL;
                // Stored experience belongs to each gem. Change one gem, never an entire stack.
                boolean separated=stack.getCount()>1;var gem=separated?stack.split(1):stack;
                p.giveExperiencePoints(p.isShiftKeyDown()?-amount:amount);gem.set(EXPERIENCE,stored+(p.isShiftKeyDown()?amount:-amount));
                if(separated)p.getInventory().placeItemBackInInventory(gem,net.minecraft.util.Prediction.SERVER_ONLY);return InteractionResult.SUCCESS;
            }
            return NouveauUtilities.use(p,stack,name,hand);
        }
        @Override public void inventoryTick(ItemStack stack,ServerLevel level,Entity entity,EquipmentSlot slot){
            if(entry.source().equals("ars_stencil")&&entity.tickCount%20==0)level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).get(TagKey.create(Registries.BANNER_PATTERN,Jugcraft.id("nouveau_pattern_item/nouveau"))).ifPresent(patterns->stack.set(DataComponents.PROVIDES_BANNER_PATTERNS,patterns));
        }
        @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext c){return NouveauUtilities.useOn(c,entry.source());}
    }
}
