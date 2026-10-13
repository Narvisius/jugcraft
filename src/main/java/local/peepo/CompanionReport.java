package local.peepo;

import java.util.Arrays;
import net.minecraft.sounds.SoundEvents;

/** Server status cache refreshed once a second; the open menu sends only changed numeric values. */
public final class CompanionReport {
    private final PeepoEntity npc;
    private final CompanionStatus[] rows=new CompanionStatus[CompanionAssignments.COUNT];
    private CompanionStatus overall=CompanionStatus.IDLE,lunch=CompanionStatus.READY;
    private long nextRefresh,nextAlert,lunchUntil;
    private boolean initialized;
    CompanionReport(PeepoEntity npc){this.npc=npc;Arrays.fill(rows,CompanionStatus.NONE);}
    public int overall(){return overall.ordinal();}
    public int row(int i){return rows[i].ordinal();}
    public void lunch(CompanionStatus value){lunch=value;lunchUntil=value==CompanionStatus.FETCHING_FOOD?npc.level().getGameTime()+120:Long.MAX_VALUE;}
    public CompanionStatus lunch(){return npc.level().getGameTime()<lunchUntil?lunch:CompanionStatus.READY;}
    public void tick(){
        long now=npc.level().getGameTime();if(now<nextRefresh)return;nextRefresh=now+20;
        boolean alert=false;
        for(int i=0;i<rows.length;i++){
            var target=npc.assignments.get(i);CompanionStatus value;
            if(target==null)value=CompanionStatus.NONE;
            else if(!target.local(npc.level()))value=CompanionStatus.OTHER_DIMENSION;
            else if(!npc.level().hasChunkAt(target.at().pos()))value=CompanionStatus.UNLOADED;
            else if(!target.present(npc.level()))value=CompanionStatus.MISSING;
            else if(i==CompanionAssignments.LUNCH){
                value=npc.level().getBlockEntity(target.at().pos()) instanceof LunchBlockEntity food && food.feeds(npc)?lunch():CompanionStatus.FORBIDDEN;
            }else if(i>=CompanionAssignments.SUPPLY){
                value=CompanionStorage.find(npc,target)==null?CompanionStatus.FORBIDDEN:npc.transport.containerStatus(i);
            }else if(i==0){
                var activity=npc.stationStatus(target.at().pos());value=activity!=null?activity:CompanionStatus.READY;
            }else{
                value=CompanionJobs.inspect(npc,target);
                if(value==CompanionStatus.READY){
                    var activity=npc.stationStatus(target.at().pos());
                    value=activity!=null?activity:!npc.preferences.onShift()?CompanionStatus.SCHEDULED_REST:npc.isRecovering()?CompanionStatus.RECOVERING:CompanionStatus.READY;
                }
            }
            alert|=value!=rows[i] && value.problem();rows[i]=value;
        }
        var old=overall;
        overall=npc.isEating()?CompanionStatus.EATING:npc.getRestMode()!=CompanionEnergy.Rest.NONE?CompanionStatus.RESTING:
            npc.social.activity()!=CompanionStatus.IDLE?npc.social.activity():
            npc.orders.mode()==0?CompanionStatus.FOLLOWING:npc.orders.mode()==1?CompanionStatus.STAYING:
            lunch()==CompanionStatus.FETCHING_FOOD?CompanionStatus.FETCHING_FOOD:
            npc.transport.activity()!=CompanionStatus.IDLE?npc.transport.activity():npc.isRecovering()?CompanionStatus.RECOVERING:!npc.preferences.onShift()?CompanionStatus.SCHEDULED_REST:npc.routineStatus();
        if(overall==CompanionStatus.IDLE){
            if(npc.needsAutomaticFood() && lunch().problem())overall=lunch();
            else if(npc.orders.porter())overall=npc.transport.porterStatus();
            else if(npc.orders.mode()==3){
                for(int i=1;i<5;i++)if(rows[i]!=CompanionStatus.NONE){overall=rows[i];break;}
                if(overall==CompanionStatus.IDLE && npc.assignments.supplies.anyRoutes())overall=npc.transport.porterStatus();
            }
        }
        alert|=old!=overall && overall.problem();
        if(initialized && alert && npc.preferences.alerts && now>=nextAlert){npc.playSound(SoundEvents.NOTE_BLOCK_PLING.value(),.4F,1.35F);nextAlert=now+400;}
        initialized=true;
    }
}
