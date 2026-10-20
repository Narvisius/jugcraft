package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.concordance.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;

/** Supplied Source is expressed as existing Ley Charge: 100 historical source units per Ley. */
public final class NouveauSource {
    private static final Set<NouveauWorkshop> LINKS=Collections.newSetFromMap(new WeakHashMap<>());
    private NouveauSource(){}
    public static void register(){
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity,damage)->{
            if(entity.level() instanceof ServerLevel l&&!(entity instanceof net.minecraft.world.entity.player.Player)&&!(entity instanceof io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerEntity<?>)&&!NouveauSummons.owned(entity))event(l,entity.blockPosition(),"vitalic_sourcelink",Math.min(20,Math.max(1,(int)entity.getMaxHealth()/2)));
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->{LINKS.clear();OBSERVED.clear();NEARBY.clear();});
    }
    public static void naturalGrowth(ServerLevel l,BlockPos at,net.minecraft.world.level.block.state.BlockState before){
        var after=l.getBlockState(at);if(before.equals(after))return;int amount=0;
        if(before.getBlock() instanceof SaplingBlock&&!(after.getBlock() instanceof SaplingBlock))amount=12;
        else if(before.getBlock()==after.getBlock())for(var property:before.getProperties())if(property instanceof IntegerProperty age&&age.getName().equals("age"))amount=Math.max(0,after.getValue(age)-before.getValue(age));
        if(amount>0){boolean magical=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(before.getBlock()).getPath().startsWith("nouveau_");event(l,at,"agronomic_sourcelink",amount*(magical?2:1));}
    }
    public static void breeding(ServerLevel l,BlockPos at){event(l,at,"vitalic_sourcelink",12);}
    public static void brewing(ServerLevel l,ServerPlayer actor,BlockPos at,int doses){event(l,at,"alchemical_sourcelink",Math.clamp(doses,1,3)*4);}
    public static void brewed(ServerLevel l,BlockPos at,int doses){event(l,at,"alchemical_sourcelink",Math.clamp(doses,1,3)*4);}
    private static void event(ServerLevel l,BlockPos at,String kind,int amount){
        if(!JugNouveau.enabled())return;
        // One event pays one collector, with a deterministic nearest-first choice.
        var candidates=LINKS.stream().filter(b->b.getLevel()==l&&!b.isRemoved()&&!b.paused()&&b.kind().equals(kind)&&b.getBlockPos().distSqr(at)<=(kind.equals("agronomic_sourcelink")?225:64)&&l.hasChunkAt(b.getBlockPos())).sorted(Comparator.comparingDouble(b->b.getBlockPos().distSqr(at))).toList();
        for(var b:candidates){var owner=Authority.answering(l,b.owner());if(owner!=null&&b.permitted(owner)&&Authority.mayChange(l,owner,at)&&b.ley()<b.capacity()){b.fill(amount);b.setActive(true);b.status("Collecting nearby activity");break;}}
    }
    private record Donor(NouveauWorkshop workshop,LeyPylonBlockEntity pylon) {
        long held(){return workshop!=null?workshop.ley():pylon.ley();}
        void draw(ServerLevel level,long amount){if(workshop!=null)workshop.draw(amount);else pylon.draw(level,amount);}
    }
    private static List<Donor> donors(ServerLevel l,ServerPlayer actor,NouveauWorkshop consumer){
        List<Donor> result=new ArrayList<>();result.add(new Donor(consumer,null));Set<BlockPos> positions=new LinkedHashSet<>(consumer.links());
        for(Direction d:Direction.values())positions.add(consumer.getBlockPos().relative(d));
        for(BlockPos p:positions){
            if(p.equals(consumer.getBlockPos())||!l.hasChunkAt(p)||!Authority.mayChange(l,actor,p))continue;
            var be=l.getBlockEntity(p);
            if(be instanceof NouveauWorkshop n&&n.permitted(actor))result.add(new Donor(n,null));
            else if(be instanceof LeyPylonBlockEntity pyl&&(pyl.owner()==null||pyl.owner().equals(actor.getUUID())||io.github.jimbozoomer.jugcraft.party.JugcraftParties.sameParty(pyl.owner(),actor.getUUID())))result.add(new Donor(null,pyl));
        }return result;
    }
    public static boolean canPay(ServerLevel l,ServerPlayer p,NouveauWorkshop n,long cost){return cost>=0&&n.permitted(p)&&donors(l,p,n).stream().mapToLong(Donor::held).sum()>=cost;}
    public static boolean pay(ServerLevel l,ServerPlayer p,NouveauWorkshop n,long cost){
        if(!n.permitted(p))return false;var donors=donors(l,p,n);if(cost<0||donors.stream().mapToLong(Donor::held).sum()<cost)return false;
        long remaining=cost;for(var d:donors){long take=Math.min(remaining,d.held());d.draw(l,take);remaining-=take;if(remaining==0)break;}return true;
    }
    public static void tick(ServerLevel level,ServerPlayer actor,NouveauWorkshop be){
        String kind=be.kind();
        if(kind.equals("creative_source_jar")){if(actor.getAbilities().instabuild)be.fill(be.capacity());return;}
        if(kind.contains("sourcelink")){LINKS.add(be);generate(level,actor,be);}
        if(kind.startsWith("relay")||kind.endsWith("sourcelink")||kind.endsWith("source_jar"))transfer(level,actor,be);
    }
    private record Nearby(long until,List<BlockPos> positions){}
    private static final Map<NouveauWorkshop,Nearby> NEARBY=new WeakHashMap<>();
    private static List<BlockPos> nearby(ServerLevel l,NouveauWorkshop b){
        var cached=NEARBY.get(b);if(cached!=null&&cached.until>l.getGameTime())return cached.positions;
        List<BlockPos> found=new ArrayList<>();for(var at:BlockPos.betweenClosed(b.getBlockPos().offset(-5,-3,-5),b.getBlockPos().offset(5,3,5)))if(l.hasChunkAt(at)&&!at.equals(b.getBlockPos())&&(l.getBlockEntity(at) instanceof LeyPylonBlockEntity||l.getBlockEntity(at) instanceof NouveauWorkshop n&&n.kind().endsWith("source_jar")))found.add(at.immutable());
        found.sort(Comparator.comparingDouble(p->p.distSqr(b.getBlockPos())));var result=found.stream().limit(32).toList();NEARBY.put(b,new Nearby(l.getGameTime()+100,result));return result;
    }
    private static void transfer(ServerLevel level,ServerPlayer actor,NouveauWorkshop relay){
        String kind=relay.kind();if(kind.startsWith("relay")&&level.hasNeighborSignal(relay.getBlockPos()))return;
        if(kind.equals("relay_collector")){int left=8;for(var p:nearby(level,relay)){if(left<=0)break;if(!relay.links().contains(p))left-=move(level,actor,relay,p,left,true,false);}}
        var endpoints=new LinkedHashSet<>(relay.links());if(kind.equals("relay_deposit")||kind.endsWith("sourcelink"))endpoints.addAll(nearby(level,relay));
        var ordered=new ArrayList<>(endpoints);boolean split=kind.equals("relay_splitter")||kind.equals("relay_warp");int budget=split?32:8;
        if(split&&!ordered.isEmpty())Collections.rotate(ordered,-Math.floorMod((int)(level.getGameTime()/20),ordered.size()));
        int share=split?Math.max(1,budget/Math.max(1,ordered.size())):budget;
        for(var p:ordered){if(budget<=0)break;budget-=move(level,actor,relay,p,Math.min(budget,share),false,kind.equals("relay_warp")&&p.distSqr(relay.getBlockPos())>900);}
    }
    private static int move(ServerLevel l,ServerPlayer actor,NouveauWorkshop relay,BlockPos at,int budget,boolean pull,boolean warp){
        if(at.equals(relay.getBlockPos())||!l.hasChunkAt(at)||!Authority.mayChange(l,actor,at))return 0;var entity=l.getBlockEntity(at);long moved=0;
        if(entity instanceof NouveauWorkshop other&&other.permitted(actor)){
            if(warp&&!other.kind().equals("relay_warp"))return 0;
            if(pull){moved=relay.fill(Math.min(budget,other.ley()));other.draw(moved);}
            else{long available=Math.min(budget,relay.ley());int lost=warp&&available>1&&l.getRandom().nextInt(10)==0?1:0;moved=other.fill(Math.max(0,available-lost));if(moved>0){relay.draw(moved+lost);moved+=lost;}}
        }else if(entity instanceof LeyPylonBlockEntity pylon&&(pylon.owner()==null||pylon.owner().equals(actor.getUUID())||io.github.jimbozoomer.jugcraft.party.JugcraftParties.sameParty(actor.getUUID(),pylon.owner()))){
            if(pull){moved=relay.fill(Math.min(budget,pylon.ley()));pylon.draw(l,moved);}else{moved=pylon.fill(l,Math.min(budget,relay.ley()));relay.draw(moved);}
        }
        if(moved>0){Vec3 start=Vec3.atCenterOf(relay.getBlockPos()),end=Vec3.atCenterOf(at);for(int i=1;i<=4;i++){var v=start.lerp(end,i/5.0);l.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,v.x,v.y,v.z,1,0,0,0,0);}}return (int)moved;
    }
    private static void generate(ServerLevel level,ServerPlayer actor,NouveauWorkshop be){
        if(be.ley()>=be.capacity())return;
        var input=be.getItem(0);String kind=be.kind();int value=0;ItemStack remainder=ItemStack.EMPTY;
        if(kind.equals("mycelial_sourcelink")&&input.has(DataComponents.FOOD))value=Math.max(1,input.get(DataComponents.FOOD).nutrition());
        if(kind.equals("volcanic_sourcelink")){
            if(input.is(Items.LAVA_BUCKET)){value=100;remainder=new ItemStack(Items.BUCKET);}
            else if(input.is(Items.COAL)||input.is(Items.CHARCOAL))value=8;
            else if(input.is(ItemTags.LOGS))value=2;
        }
        if(kind.equals("vitalic_sourcelink")&&(input.is(NouveauCatalog.item("experience_gem"))||input.is(NouveauCatalog.item("greater_experience_gem")))){
            int stored=input.getOrDefault(NouveauGear.EXPERIENCE,0),take=(int)Math.min(Math.min(stored,10),be.capacity()-be.ley());
            if(take>0){input.set(NouveauGear.EXPERIENCE,stored-take);be.fill(take);be.setChanged();be.setActive(true);be.status("Drawing stored experience");}return;
        }
        if(value>0&&be.capacity()-be.ley()>=value&&(remainder.isEmpty()||be.canOutput(remainder))){
            input.shrink(1);be.fill(value);if(!remainder.isEmpty())be.output(remainder);be.setChanged();be.setActive(true);be.status("Generating Ley Charge");
        }
        if(kind.equals("alchemical_sourcelink"))for(var jar:NouveauAlchemy.jars(level,actor,be))if(jar.doses()>0){
            int worth=0;for(var effect:jar.potion().getAllEffects())worth+=Math.max(1,(effect.getAmplifier()+1)*2+effect.getDuration()/1200);
            worth=Math.clamp(worth,0,64);if(worth>0&&be.capacity()-be.ley()>=worth){jar.drainPotion(1);be.fill(worth);be.setActive(true);be.status("Distilling potion Source");break;}
        }
    }
    // Observations are ephemeral and initialized without a payout after loading.
    // A mature crop cannot repeatedly pay merely by moving a jar or relogging.
    private static final Map<NouveauWorkshop,Map<Long,Integer>> OBSERVED=Collections.synchronizedMap(new WeakHashMap<>());
    private static void observeGrowth(ServerLevel level,ServerPlayer actor,NouveauWorkshop be){
        Map<Long,Integer> before=OBSERVED.computeIfAbsent(be,k->new HashMap<>());
        int start=(int)Math.floorMod(level.getGameTime()/20,81)*9;
        for(int i=0;i<9;i++){
            int n=(start+i)%729;BlockPos p=be.getBlockPos().offset(n%9-4,(n/81)%9-4,(n/9)%9-4);
            if(!level.hasChunkAt(p)||!Authority.mayChange(level,actor,p))continue;
            var s=level.getBlockState(p);int age=-1;
            if(s.getBlock() instanceof CropBlock||s.getBlock() instanceof SweetBerryBushBlock||s.getBlock() instanceof NouveauNature.Archfruit){
                for(var property:s.getProperties())if(property instanceof IntegerProperty a&&a.getName().equals("age")){age=s.getValue(a);break;}
            }
            Integer old=before.put(p.asLong(),age);if(old!=null&&old>=0&&age>old)be.fill(age-old);
        }
    }
}
