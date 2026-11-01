package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.crafting.CraftingChoices;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Open-bottom delivery and nine-position native crafting, with explicit ambiguous-output selection. */
final class BotanicalCrates {
    static final List<String> PATTERNS=List.of("none","1_1","2_2","1_2","2_1","1_3","3_1","2_3","3_2","donut");
    private static final int[] MASKS={511,1,27,9,3,73,7,219,63,495};
    static final IntegerProperty PATTERN=IntegerProperty.create("pattern",0,9);
    private final BotanicalWorkshop host;private String selected="";private boolean confirmed;
    private int previousWidth,previousHeight;private RecipeManager manager;private List<ItemStack> previous=List.of();private List<RecipeHolder<CraftingRecipe>> matches=List.of();
    BotanicalCrates(BotanicalWorkshop host){this.host=host;}
    static boolean handles(String kind){return kind.equals("open_crate")||kind.equals("crafty_crate");}
    private boolean open(){return host.kind().equals("open_crate");}
    private int mask(){return open()?1:MASKS[host.getBlockState().getValue(PATTERN)];}
    boolean accepts(int slot){return slot>=0&&slot<(open()?1:9)&&(mask()&(1<<slot))!=0;}
    InteractionResult use(ServerPlayer actor,InteractionHand hand){
        var held=actor.getItemInHand(hand);String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem()).getPath();
        if(!open()&&id.startsWith("jugtania_pattern_")){
            int pattern=PATTERNS.indexOf(id.substring("jugtania_pattern_".length()));if(pattern<1)return InteractionResult.PASS;
            if(host.getBlockState().getValue(PATTERN)==pattern)pattern=0;
            actor.level().setBlockAndUpdate(host.getBlockPos(),host.getBlockState().setValue(PATTERN,pattern));
            for(int i=0;i<9;i++)if(!accepts(i)&&!host.getItem(i).isEmpty()){actor.getInventory().placeItemBackInInventory(host.getItem(i).copy(),net.minecraft.util.Prediction.SERVER_ONLY);host.setItem(i,ItemStack.EMPTY);}selected="";confirmed=false;host.changed();return InteractionResult.SUCCESS;
        }
        if(held.isEmpty()){
            if(actor.isShiftKeyDown())for(int i=open()?0:8;i>=0;i--)if(!host.getItem(i).isEmpty()){actor.getInventory().placeItemBackInInventory(host.getItem(i).copy(),net.minecraft.util.Prediction.SERVER_ONLY);host.setItem(i,ItemStack.EMPTY);return InteractionResult.SUCCESS;}
            actor.sendOverlayMessage(Component.literal(status(actor.level())));return InteractionResult.SUCCESS;
        }
        for(int slot=0;slot<(open()?1:9);slot++)if(accepts(slot)&&host.getItem(slot).isEmpty()){host.setItem(slot,held.copyWithCount(1));if(!actor.getAbilities().instabuild)held.shrink(1);return InteractionResult.SUCCESS;}return InteractionResult.FAIL;
    }
    private CraftingInput input(){var grid=new ArrayList<ItemStack>();for(int i=0;i<9;i++){var s=host.getItem(i);grid.add(!accepts(i)||s.is(Jugtania.item("placeholder"))?ItemStack.EMPTY:s.copyWithCount(1));}return CraftingInput.of(3,3,grid);}
    private void find(ServerLevel level){
        var nextManager=level.getServer().getRecipeManager();var grid=input();var snapshot=grid.items();boolean changed=previousWidth!=grid.width()||previousHeight!=grid.height()||manager!=nextManager||previous.size()!=snapshot.size();
        if(!changed)for(int i=0;i<snapshot.size();i++)if(previous.get(i).getCount()!=snapshot.get(i).getCount()||!ItemStack.isSameItemSameComponents(previous.get(i),snapshot.get(i))){changed=true;break;}
        if(!changed)return;previousWidth=grid.width();previousHeight=grid.height();manager=nextManager;previous=snapshot.stream().map(ItemStack::copy).toList();
        matches=grid.isEmpty()?List.of():CraftingChoices.recipes(manager).stream().filter(r->r.value().matches(grid,level)&&!r.value().assemble(grid).isEmpty()).sorted(Comparator.comparing(r->r.id().identifier().toString())).limit(1024).toList();
        if(matches.stream().noneMatch(r->r.id().identifier().toString().equals(selected))){selected="";confirmed=false;}
    }
    String status(ServerLevel level){if(open())return "Open Crate: feeds items straight down. Redstone adds a ten-second pickup delay.";find(level);return "Crafty Crate: "+matches.size()+" matching recipe(s)"+(selected.isEmpty()?"":" · "+selected)+(matches.size()>1&&!confirmed?" · Sneak-wand cycles; wand confirms":"");}
    void wand(ServerPlayer actor){
        if(open()){actor.sendOverlayMessage(Component.literal(status(actor.level())));return;}find(actor.level());if(matches.isEmpty()){actor.sendOverlayMessage(Component.literal("No matching recipe. Sneak-empty-hand removes inputs."));return;}
        int index=-1;for(int i=0;i<matches.size();i++)if(matches.get(i).id().identifier().toString().equals(selected))index=i;
        if(actor.isShiftKeyDown()||index<0){index=(index+1)%matches.size();selected=matches.get(index).id().identifier().toString();confirmed=false;}
        if(!actor.isShiftKeyDown())confirmed=true;
        actor.sendOverlayMessage(Component.literal((confirmed?"Locked output: ":"Preview: ")+matches.get(index).value().assemble(input()).getHoverName().getString()+(confirmed?"":" · Use wand normally to confirm")));host.changed();
    }
    private boolean canEject(ServerLevel level,ServerPlayer actor){var pos=host.getBlockPos();return level.hasChunkAt(pos.below())&&Authority.mayChange(level,actor,pos.below())&&ClaimTransfers.allowed(level,pos,pos.below())&&level.noCollision(new AABB(pos.getX()+.375,pos.getY()-.25,pos.getZ()+.375,pos.getX()+.625,pos.getY(),pos.getZ()+.625));}
    void tick(ServerLevel level,ServerPlayer actor){
        if(level.getGameTime()%5!=0||!canEject(level,actor))return;var outputs=new ArrayList<ItemStack>();
        if(open()){if(host.getItem(0).isEmpty())return;outputs.add(host.getItem(0).copy());}
        else{
            for(int i=0;i<9;i++)if(accepts(i)&&host.getItem(i).isEmpty())return;find(level);if(matches.isEmpty()||matches.size()>1&&!confirmed)return;
            var recipe=matches.size()==1?matches.get(0):matches.stream().filter(r->r.id().identifier().toString().equals(selected)).findFirst().orElse(null);if(recipe==null)return;
            var input=input();if(!recipe.value().matches(input,level))return;var made=recipe.value().assemble(input);if(made.isEmpty()||!made.isItemEnabled(level.enabledFeatures()))return;outputs.add(made);
            for(var remainder:recipe.value().getRemainingItems(input))if(!remainder.isEmpty())outputs.add(remainder.copy());
            for(int i=0;i<9;i++)if(host.getItem(i).is(Jugtania.item("placeholder")))outputs.add(host.getItem(i).copy());
        }
        var created=new ArrayList<ItemEntity>();var pos=host.getBlockPos();
        for(var output:outputs){var entity=new ItemEntity(level,pos.getX()+.5,pos.getY()-.25,pos.getZ()+.5,output);entity.setDeltaMovement(Vec3.ZERO);if(open()&&level.hasNeighborSignal(pos))BotanicalDropCooldown.delay(entity,200);
            if(!level.addFreshEntity(entity)){for(var inserted:created)inserted.discard();return;}created.add(entity);
        }
        for(int i=0;i<(open()?1:9);i++)host.setItem(i,ItemStack.EMPTY);host.changed();
    }
    int signal(){int full=0;for(int i=0;i<(open()?1:9);i++){if(accepts(i)&&host.getItem(i).isEmpty())break;full++;}return full;}
    void save(ValueOutput out){out.putString("crate_recipe",selected);out.putBoolean("crate_confirmed",confirmed);}
    void load(ValueInput in){selected=in.getStringOr("crate_recipe","");confirmed=in.getBooleanOr("crate_confirmed",false);manager=null;previous=List.of();matches=List.of();}
    static final class Crate extends BotanicalBlock {
        Crate(String kind,Properties p){super(kind,p);registerDefaultState(defaultBlockState().setValue(PATTERN,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(PATTERN);}
        @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos,Direction side){return level.getBlockEntity(pos) instanceof BotanicalWorkshop b?b.crates.signal():0;}
    }
}
