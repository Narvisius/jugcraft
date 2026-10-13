package local.peepo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;

/** Owner-controlled preferences; defaults preserve existing work and recovery behavior. */
public final class CompanionPreferences {
    public static final String[] SCHEDULES={"Auto","Day shift","Night shift"};
    public static final String[] FOODS={"Best meal","Small meal"};
    private final PeepoEntity npc;
    public int schedule,breakAt,resumeAt=80,foodPolicy,carryMeals=2;
    public boolean alerts,social=true;
    CompanionPreferences(PeepoEntity npc){this.npc=npc;}
    public boolean onShift(){
        if(schedule==0 || !(npc.level() instanceof ServerLevel server))return true;
        long time=Math.floorMod(server.getOverworldClockTime(),24000L);
        boolean night=time>=13000 && time<23000;
        return schedule==2?night:!night;
    }
    public boolean canWork(){return onShift() && !npc.isRecovering() && npc.getEnergy()>0;}
    public int foodScore(ItemStack food){
        if(!PeepoEntity.isEdible(food))return Integer.MIN_VALUE;
        int quality=CompanionEnergy.meal(food.get(DataComponents.FOOD)).bonusPerTick();
        return foodPolicy==0?quality:100-quality;
    }
    public boolean command(int button){
        switch(button){
            case 40->schedule=(schedule+1)%3;
            case 41->alerts=!alerts;
            case 42->foodPolicy=(foodPolicy+1)%2;
            case 43->carryMeals=(carryMeals+1)%5;
            case 44->breakAt=Math.max(0,breakAt-10);
            case 45->breakAt=Math.min(Math.min(70,resumeAt-10),breakAt+10);
            case 46->resumeAt=Math.max(breakAt+10,resumeAt-10);
            case 47->resumeAt=Math.min(95,resumeAt+10);
            case 48->{social=!social;npc.social.cancel();}
            default->{return false;}
        }
        npc.updateRecoveryState();if(button==40 || button>=44 && button<=47)npc.resetCompanionRoutine();return true;
    }
    public void save(ValueOutput out){
        out.putInt("WorkSchedule",schedule);out.putInt("BreakAt",breakAt);out.putInt("ResumeAt",resumeAt);
        out.putInt("FoodPolicy",foodPolicy);out.putInt("CarryMeals",carryMeals);out.putBoolean("WorkAlerts",alerts);out.putBoolean("SocialEnabled",social);
    }
    public void load(ValueInput in){
        schedule=Math.clamp(in.getIntOr("WorkSchedule",0),0,2);breakAt=Math.clamp(in.getIntOr("BreakAt",0),0,70);
        resumeAt=Math.clamp(in.getIntOr("ResumeAt",80),breakAt+10,95);foodPolicy=Math.clamp(in.getIntOr("FoodPolicy",0),0,1);
        carryMeals=Math.clamp(in.getIntOr("CarryMeals",2),0,4);alerts=in.getBooleanOr("WorkAlerts",false);social=in.getBooleanOr("SocialEnabled",true);
    }
}
