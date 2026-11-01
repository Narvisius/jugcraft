package io.github.jimbozoomer.jugcraft.enchanting;

import java.util.*;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;

/** Loaded-only, bounded, paid mining; every extra block passes the normal break path. */
public final class OwnerEnchantmentMining {
    private OwnerEnchantmentMining() {}
    private static final TagKey<Block> ORES=tag("ores");
    private static final Map<ServerPlayer,Job> JOBS=new WeakHashMap<>();
    private static final Set<ServerPlayer> BREAKING=Collections.newSetFromMap(new WeakHashMap<>());
    private record Job(ServerLevel level,BlockPos origin,ItemStack tool,String kind,String family,ArrayDeque<BlockPos> pending,Set<BlockPos> seen,int[] remaining){}
    private static TagKey<Block> tag(String name){return TagKey.create(Registries.BLOCK,Identifier.fromNamespaceAndPath("jugcraft","reforged/"+name));}
    public static boolean breaking(ServerPlayer player){return BREAKING.contains(player);}
    public static void register(){
        PlayerBlockBreakEvents.AFTER.register((world,player,pos,old,be)->{
            if(!(player instanceof ServerPlayer p)||!(world instanceof ServerLevel l)||p.hasInfiniteMaterials())return;
            var tool=p.getMainHandItem();OwnerEnchantments.mined(p,tool);
            if(BREAKING.contains(p))return;
            if(OwnerEnchantments.level(p,tool,"replenish")>0&&tool.is(ItemTags.HOES)&&old.getBlock() instanceof CropBlock crop&&crop.isMaxAge(old))replant(l,p,pos,crop);
            if(!p.isShiftKeyDown()||JOBS.containsKey(p)||tool.isEmpty())return;
            int vein=OwnerEnchantments.level(p,tool,"vein_miner"),lumber=OwnerEnchantments.level(p,tool,"lumberjack"),excavator=OwnerEnchantments.level(p,tool,"excavator");
            boolean silk=l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SILK_TOUCH).map(e->EnchantmentHelper.getItemEnchantmentLevel(e,tool)>0).orElse(false);
            String kind;int limit;
            if(vein>0&&!silk&&tool.is(ItemTags.PICKAXES)&&old.is(ORES)){kind="ore";limit=Math.min(64,vein*4);}
            else if(lumber>0&&tool.is(ItemTags.AXES)&&old.is(BlockTags.LOGS)){kind="log";limit=Math.min(64,lumber*8);}
            else if(excavator>0&&tool.is(ItemTags.SHOVELS)&&old.is(BlockTags.MINEABLE_WITH_SHOVEL)){kind="plane";limit=excavator>=2?8:1;}
            else return;
            var queue=new ArrayDeque<BlockPos>();
            if(kind.equals("plane")){
                var right=p.getDirection().getClockWise();
                for(int x=excavator>=2?-1:0;x<=(excavator>=2?1:0);x++)for(int y=excavator>=2?-1:1;y<=1;y++)if(x!=0||y!=0)queue.add(pos.relative(right,x).above(y));
            }else for(var d:Direction.values())queue.add(pos.relative(d));
            var seen=new HashSet<BlockPos>();seen.add(pos.immutable());
            JOBS.put(p,new Job(l,pos.immutable(),tool,kind,family(old),queue,seen,new int[]{limit}));
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            int budget=64;
            for(var entry:List.copyOf(JOBS.entrySet())){
                var p=entry.getKey();var j=entry.getValue();if(j.level.getServer()!=server)continue;
                if(!p.isAlive()||p.hasDisconnected()||p.level()!=j.level||p.getMainHandItem()!=j.tool||j.tool.isEmpty()||p.distanceToSqr(Vec3.atCenterOf(j.origin))>24*24){JOBS.remove(p);continue;}
                int examined=0;
                while(budget>0&&examined++<96&&j.remaining[0]>0&&!j.pending.isEmpty()){
                    var at=j.pending.removeFirst();
                    if(!j.seen.add(at)||j.seen.size()>384||!j.level.hasChunkAt(at)||at.distSqr(j.origin)>16*16)continue;
                    var s=j.level.getBlockState(at);
                    boolean matches=switch(j.kind){case "ore"->s.is(ORES)&&family(s).equals(j.family);case "log"->s.is(BlockTags.LOGS);default->s.is(BlockTags.MINEABLE_WITH_SHOVEL);};
                    if(!matches||s.hasBlockEntity()||s.getDestroySpeed(j.level,at)<0)continue;
                    if(!Authority.mayChange(j.level,p,at)||j.tool!=p.getMainHandItem()||j.tool.isEmpty()){j.pending.clear();break;}
                    budget--;BREAKING.add(p);boolean broken;
                    try{broken=p.gameMode.destroyBlock(at);}finally{BREAKING.remove(p);}
                    if(!broken)continue;j.remaining[0]--;
                    if(!j.kind.equals("plane"))for(var d:Direction.values()){var next=at.relative(d);if(!j.seen.contains(next))j.pending.addLast(next);}
                }
                if(j.pending.isEmpty()||j.remaining[0]<=0||j.seen.size()>384)JOBS.remove(p);
                if(budget==0)break;
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{JOBS.entrySet().removeIf(e->e.getValue().level.getServer()==server);BREAKING.removeIf(p->p.level().getServer()==server);});
    }
    private static String family(BlockState state){var id=BuiltInRegistries.BLOCK.getKey(state.getBlock());return id.getNamespace()+":"+id.getPath().replaceFirst("^(deepslate_|nether_)","");}
    private static void replant(ServerLevel l,ServerPlayer p,BlockPos pos,CropBlock crop){
        if(!l.getBlockState(pos).isAir()||!crop.getStateForAge(0).canSurvive(l,pos)||!Authority.mayChange(l,p,pos))return;
        var seed=new ItemStack(crop.asItem());if(seed.isEmpty())return;
        for(int i=0;i<p.getInventory().getContainerSize();i++){
            var stack=p.getInventory().getItem(i);if(!ItemStack.isSameItem(stack,seed))continue;
            if(l.setBlockAndUpdate(pos,crop.getStateForAge(0))){stack.shrink(1);p.getInventory().setChanged();}return;
        }
    }
    public static List<ItemStack> drops(BlockState state,ServerLevel l,BlockPos pos,Entity entity,ItemInstance instance,List<ItemStack> original){
        if(!(entity instanceof ServerPlayer p)||!(instance instanceof ItemStack tool)||p.hasInfiniteMaterials())return original;
        int smelt=OwnerEnchantments.level(p,tool,"auto_smelt"),bounty=OwnerEnchantments.level(p,tool,"bountiful_harvest"),sifting=OwnerEnchantments.level(p,tool,"sifting");
        if(smelt+bounty+sifting==0)return original;
        var result=new ArrayList<ItemStack>();
        boolean silk=l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SILK_TOUCH).map(e->EnchantmentHelper.getItemEnchantmentLevel(e,tool)>0).orElse(false);
        for(var stack:original){
            var input=new SingleRecipeInput(stack.copyWithCount(1));
            var output=smelt>0&&!silk&&!stack.isEmpty()?l.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,l).map(r->r.value().assemble(input)).orElse(ItemStack.EMPTY):ItemStack.EMPTY;
            if(output.isEmpty())result.add(stack.copy());
            else{int count=Math.min(4096,output.getCount()*stack.getCount());while(count>0){int n=Math.min(count,output.getMaxStackSize());result.add(output.copyWithCount(n));count-=n;}}
        }
        if(bounty>0&&state.getBlock() instanceof CropBlock crop&&crop.isMaxAge(state)&&p.getRandom().nextFloat()<Math.min(.25,.066*bounty)){
            var seed=new ItemStack(crop.asItem());boolean edibleSeed=crop instanceof CarrotBlock||crop instanceof PotatoBlock;
            result.stream().filter(s->!s.isEmpty()&&(edibleSeed==ItemStack.isSameItem(s,seed))).findFirst().ifPresent(s->result.add(s.copyWithCount(1)));
        }
        if(sifting>0&&tool.is(ItemTags.SHOVELS)&&p.getRandom().nextFloat()<Math.min(.25,.12*sifting)){
            for(String kind:List.of("dirt","gravel","sand"))if(state.is(tag("sifting/"+kind))){
                var key=ResourceKey.create(Registries.LOOT_TABLE,Identifier.fromNamespaceAndPath("jugcraft","reforged/sifting/"+kind));
                var params=new LootParams.Builder(l).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(pos)).withParameter(LootContextParams.BLOCK_STATE,state).withParameter(LootContextParams.TOOL,tool).withParameter(LootContextParams.THIS_ENTITY,p).withLuck(p.getLuck()+Math.min(1,.25f*sifting)).create(LootContextParamSets.BLOCK);
                result.addAll(l.getServer().reloadableRegistries().getLootTable(key).getRandomItems(params));break;
            }
        }return result;
    }
}
