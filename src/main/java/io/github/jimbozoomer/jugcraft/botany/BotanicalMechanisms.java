package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.mixin.BotanicalFurnaceAccess;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.*;

/** Saved, owner-bound workshop actuators. No chunk loading or unattended claim bypass. */
public final class BotanicalMechanisms {
    private final BotanicalWorkshop workshop;
    private int speed=1,elapsed,stroke,pulse;
    private boolean reverse,locked,paused,flip,wasPowered;
    private float angle;
    BotanicalMechanisms(BotanicalWorkshop workshop){this.workshop=workshop;}
    static boolean handles(String kind){return kind.equals("bellows")||kind.equals("turntable")||kind.equals("hourglass");}
    public float compression(){return stroke==0?0:stroke>20?(27-stroke)*.9F/7:stroke*.9F/20;}
    public float fraction(){return total()==0?0:(float)elapsed/total();}
    public boolean flipped(){return flip;}
    public int color(){var s=workshop.getItem(0);return s.is(Items.RED_SAND)?0xFFE95800:s.is(Items.SOUL_SAND)?0xFF5A413F:s.is(Jugtania.item("mana_powder"))?0xFF03ABFF:0xFFFFED49;}
    public int pulse(){return pulse;}
    private int unit(ItemStack s){return s.is(Items.SAND)?20:s.is(Items.RED_SAND)?200:s.is(Items.SOUL_SAND)?1200:s.is(Jugtania.item("mana_powder"))?1:0;}
    private int total(){return unit(workshop.getItem(0))*workshop.getItem(0).getCount();}
    boolean accepts(int slot,ItemStack stack){return workshop.kind().equals("hourglass")&&!locked&&slot==0&&unit(stack)>0;}
    boolean extractable(){return workshop.kind().equals("hourglass")&&!locked;}
    void reset(){elapsed=0;workshop.changed();}
    String status(){return switch(workshop.kind()){
        case "hourglass"->"Hourglass: "+elapsed+" / "+total()+(workshop.getItem(0).is(Jugtania.item("mana_powder"))?" bursts":" ticks")+(locked?" · Contents locked":"")+(paused?" · Paused":"");
        case "turntable"->"Turntable: "+speed+"°/tick · "+(reverse?"counterclockwise":"clockwise");
        default->"Bellows: "+(stroke>0?"working":"ready")+" · Facing "+workshop.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getSerializedName();};}
    boolean wand(ServerPlayer actor){
        switch(workshop.kind()){
            case "turntable"->{if(actor.isShiftKeyDown())reverse=!reverse;else speed=speed%6+1;}
            case "hourglass"->locked=!locked;
            case "bellows"->{var state=workshop.getBlockState();workshop.getLevel().setBlockAndUpdate(workshop.getBlockPos(),state.setValue(BlockStateProperties.HORIZONTAL_FACING,state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise()));}
            default->{return false;}
        }workshop.changed();actor.sendOverlayMessage(Component.literal(status()));return true;
    }
    InteractionResult use(ServerPlayer actor,InteractionHand hand){
        if(workshop.kind().equals("bellows")){activate();return InteractionResult.SUCCESS;}
        if(!workshop.kind().equals("hourglass")){actor.sendOverlayMessage(Component.literal(status()));return InteractionResult.SUCCESS;}
        var held=actor.getItemInHand(hand);var stored=workshop.getItem(0);
        if(locked){actor.sendOverlayMessage(Component.literal(status()+" · Use the wand to unlock."));return InteractionResult.SUCCESS;}
        if(held.isEmpty()&&!stored.isEmpty()){
            var removed=stored.copy();workshop.setItem(0,ItemStack.EMPTY);actor.getInventory().placeItemBackInInventory(removed,net.minecraft.util.Prediction.SERVER_ONLY);reset();return InteractionResult.SUCCESS;
        }
        if(unit(held)==0){actor.sendOverlayMessage(Component.literal("Use sand (1 second each), red sand (10 seconds), soul sand (1 minute), or mana powder (burst counter)."));return InteractionResult.SUCCESS;}
        if(!stored.isEmpty()&&!ItemStack.isSameItemSameComponents(stored,held))return InteractionResult.FAIL;
        int amount=Math.min(actor.isShiftKeyDown()?held.getCount():1,held.getMaxStackSize()-stored.getCount());if(amount<=0)return InteractionResult.FAIL;
        var next=held.copyWithCount(stored.getCount()+amount);workshop.setItem(0,next);if(!actor.getAbilities().instabuild)held.shrink(amount);reset();return InteractionResult.SUCCESS;
    }
    void burst(ServerPlayer actor){if(!workshop.kind().equals("hourglass"))return;if(workshop.getItem(0).is(Jugtania.item("mana_powder"))){if(total()>0&&++elapsed>=total())trigger(actor.level(),actor);}else paused=!paused;workshop.changed();}
    void activate(){if(stroke==0){stroke=27;workshop.changed();if(workshop.getLevel() instanceof ServerLevel level)level.playSound(null,workshop.getBlockPos(),BotanicalSounds.get("bellows"),net.minecraft.sounds.SoundSource.BLOCKS,1,1);}}
    private void trigger(ServerLevel level,ServerPlayer actor){
        elapsed=0;flip=!flip;pulse=4;var pos=workshop.getBlockPos();
        level.setBlockAndUpdate(pos,workshop.getBlockState().setValue(BlockStateProperties.POWERED,true));level.scheduleTick(pos,workshop.getBlockState().getBlock(),4);
        for(var side:Direction.values()){var target=pos.relative(side);if(!allowed(level,actor,target))continue;
            if(level.getBlockEntity(target) instanceof BotanicalWorkshop other&&other.kind().equals("bellows")&&other.permitted(actor))other.mechanisms.activate();
        }workshop.changed();
    }
    private boolean allowed(ServerLevel level,ServerPlayer actor,BlockPos target){return level.hasChunkAt(target)&&Authority.mayChange(level,actor,target)&&ClaimTransfers.allowed(level,workshop.getBlockPos(),target);}
    void tick(ServerLevel level,ServerPlayer actor){
        var pos=workshop.getBlockPos();
        if(workshop.kind().equals("hourglass")){
            if(pulse>0&&--pulse==0){level.setBlockAndUpdate(pos,workshop.getBlockState().setValue(BlockStateProperties.POWERED,false));workshop.changed();}
            if(total()>0&&!paused&&!workshop.getItem(0).is(Jugtania.item("mana_powder"))){if(++elapsed>=total())trigger(level,actor);else if(elapsed%5==0)workshop.changed();else workshop.setChanged();}return;
        }
        if(workshop.kind().equals("turntable")){
            if(level.hasNeighborSignal(pos))return;var above=pos.above();
            if(allowed(level,actor,above)&&level.getBlockEntity(above) instanceof BotanicalWorkshop spreader&&spreader.kind().contains("spreader")&&spreader.permitted(actor)){
                angle=(angle+speed*(reverse?-1:1)+360)%360;spreader.rotateAim(level,actor,angle);workshop.setChanged();
            }return;
        }
        boolean powered=level.hasNeighborSignal(pos);if(powered&&!wasPowered)activate();wasPowered=powered;
        if(stroke<=0)return;
        var target=pos.relative(workshop.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING));
        if(stroke>20&&allowed(level,actor,target)&&level.getBlockEntity(target) instanceof AbstractFurnaceBlockEntity furnace)accelerate(level,furnace);
        stroke--;workshop.changed();
    }
    private void accelerate(ServerLevel level,AbstractFurnaceBlockEntity furnace){
        var access=(BotanicalFurnaceAccess)furnace;if(access.jugcraft$heat()<=0||access.jugcraft$cookTotal()<=0||furnace.getItem(0).isEmpty())return;
        var input=new SingleRecipeInput(furnace.getItem(0));var recipes=level.getServer().getRecipeManager();java.util.Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> match;
        if(furnace instanceof SmokerBlockEntity)match=recipes.getRecipeFor(RecipeType.SMOKING,input,level);
        else if(furnace instanceof BlastFurnaceBlockEntity)match=recipes.getRecipeFor(RecipeType.BLASTING,input,level);
        else if(furnace instanceof FurnaceBlockEntity)match=recipes.getRecipeFor(RecipeType.SMELTING,input,level);else return;
        if(match.isEmpty())return;var result=match.get().value().assemble(input);var output=furnace.getItem(2);
        if(result.isEmpty()||!output.isEmpty()&&(!ItemStack.isSameItemSameComponents(output,result)||output.getCount()+result.getCount()>Math.min(output.getMaxStackSize(),furnace.getMaxStackSize())))return;
        access.jugcraft$cook(Math.min(access.jugcraft$cookTotal()-1,access.jugcraft$cook()+20));access.jugcraft$heat(Math.max(0,access.jugcraft$heat()-10));furnace.setChanged();
    }
    void save(ValueOutput out){out.putInt("mechanism_speed",speed);out.putInt("mechanism_time",elapsed);out.putInt("mechanism_stroke",stroke);out.putInt("mechanism_pulse",pulse);out.putFloat("mechanism_angle",angle);out.putBoolean("mechanism_reverse",reverse);out.putBoolean("mechanism_locked",locked);out.putBoolean("mechanism_paused",paused);out.putBoolean("mechanism_flip",flip);out.putBoolean("mechanism_powered",wasPowered);}
    void load(ValueInput in){speed=Math.clamp(in.getIntOr("mechanism_speed",1),1,6);elapsed=Math.clamp(in.getIntOr("mechanism_time",0),0,76800);stroke=Math.clamp(in.getIntOr("mechanism_stroke",0),0,27);pulse=Math.clamp(in.getIntOr("mechanism_pulse",0),0,4);angle=in.getFloatOr("mechanism_angle",0);if(!Float.isFinite(angle))angle=0;reverse=in.getBooleanOr("mechanism_reverse",false);locked=in.getBooleanOr("mechanism_locked",false);paused=in.getBooleanOr("mechanism_paused",false);flip=in.getBooleanOr("mechanism_flip",false);wasPowered=in.getBooleanOr("mechanism_powered",false);}
    static final class Block extends BotanicalBlock {
        Block(String kind,Properties p){super(kind,p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH).setValue(BlockStateProperties.POWERED,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BlockStateProperties.HORIZONTAL_FACING,BlockStateProperties.POWERED);}
        @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,c.getHorizontalDirection().getOpposite());}
        @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){if(state.getValue(BlockStateProperties.POWERED))level.setBlockAndUpdate(pos,state.setValue(BlockStateProperties.POWERED,false));}
        @Override protected boolean isSignalSource(BlockState state){return kind.equals("hourglass");}
        @Override protected int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){return kind.equals("hourglass")&&state.getValue(BlockStateProperties.POWERED)?15:0;}
        @Override protected RenderShape getRenderShape(BlockState state){return kind.equals("turntable")?RenderShape.MODEL:RenderShape.INVISIBLE;}
    }
}
