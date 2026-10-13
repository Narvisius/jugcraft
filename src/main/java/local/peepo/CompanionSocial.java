package local.peepo;

import java.util.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.*;

/** Cosmetic, stationary social encounters. A pair holds only mutual runtime references, never world tickets. */
public final class CompanionSocial {
    public static final int NONE=0,WAVE=1,TALK=2,LISTEN=3;
    private static final int CHAT_TICKS=120,TURN_TICKS=30,GREETING_COOLDOWN=1200,CHAT_COOLDOWN=1800;
    private final PeepoEntity npc;
    private PeepoEntity partner;
    private Player greeted;
    private boolean ownerNearby,firstSpeaker,goalActive;
    private long nextScan,nextGreeting,nextConversation,until,started;
    private int spokenTurn=-1;
    // Client spawn packets assign the numeric ID after constructing the entity. UUID already exists here.
    CompanionSocial(PeepoEntity npc){this.npc=npc;nextScan=npc.level().getGameTime()+20+Math.floorMod(npc.getUUID().hashCode(),80);}
    public CompanionStatus activity(){return partner!=null && goalActive?CompanionStatus.CHATTING:greeted!=null?CompanionStatus.GREETING:CompanionStatus.IDLE;}
    private boolean idle(){return npc.preferences.social && npc.socialIdle() && npc.getNavigation().isDone()
        && npc.getDeltaMovement().horizontalDistanceSqr()<.0004 && !npc.isInWater() && !npc.isPassenger() && npc.onGround();}
    private boolean mayConverse(){return idle() && npc.orders.mode()!=CompanionOrders.Mode.FOLLOW.ordinal() && npc.orders.mode()!=CompanionOrders.Mode.STAY.ordinal();}
    private boolean visible(LivingEntity other,double distance){return other!=null && other.isAlive() && !other.isRemoved() && other.level()==npc.level()
        && npc.distanceToSqr(other)<=distance*distance && npc.hasLineOfSight(other);}
    private void face(LivingEntity other){
        float yaw=(float)Math.toDegrees(Math.atan2(-(other.getX()-npc.getX()),other.getZ()-npc.getZ()));
        npc.setYRot(net.minecraft.util.Mth.rotLerp(.25F,npc.getYRot(),yaw));npc.yBodyRot=npc.getYRot();
        npc.getLookControl().setLookAt(other,25,20);
    }
    /** Cancel both halves immediately on a command, menu, unload or higher-priority goal. */
    void cancel(){
        var old=partner;partner=null;greeted=null;goalActive=false;npc.setSocialPose(NONE);
        if(old!=null && old.social.partner==npc){old.social.partner=null;old.social.goalActive=false;old.setSocialPose(NONE);}
    }
    public void tick(){
        long now=npc.level().getGameTime();
        if(partner!=null){
            if(now>=until || partner.social.partner!=npc || !mayConverse() || !partner.social.mayConverse()
                || !visible(partner,3.5) || now-started>10 && (!goalActive || !partner.social.goalActive)){cancel();return;}
            if(goalActive && partner.social.goalActive){
                int turn=(int)((now-started)/TURN_TICKS);boolean speaking=(turn%2==0)==firstSpeaker;
                npc.setSocialPose(speaking?TALK:LISTEN);face(partner);
                if(speaking && spokenTurn!=turn){spokenTurn=turn;npc.playSound(SoundEvents.FROG_AMBIENT,.16F,npc.isJughead()?1.55F:1.35F);}
            }
            return;
        }
        if(greeted!=null){
            if(now>=until || !idle() || !visible(greeted,8) || greeted.isSpectator() || !npc.orders.owner(greeted)){cancel();return;}
            face(greeted);return;
        }
        if(now<nextScan || !idle())return;
        if(!CompanionBudget.social(npc))return;
        nextScan=now+80+Math.floorMod(npc.getId(),20);
        var owner=npc.orders.ownerPlayer();
        boolean near=owner!=null && !owner.isSpectator() && visible(owner,ownerNearby?8:6);
        boolean arriving=near && !ownerNearby;ownerNearby=near;
        if(arriving && now>=nextGreeting){
            greeted=owner;until=now+40;nextGreeting=now+GREETING_COOLDOWN;npc.setSocialPose(WAVE);face(owner);return;
        }
        if(!mayConverse() || now<nextConversation)return;
        var nearby=new ArrayList<PeepoEntity>(16);
        // Cap results before filtering eligibility, including crowded pens. Only already-loaded local entities.
        npc.level().getEntities(EntityTypeTest.forClass(PeepoEntity.class),npc.getBoundingBox().inflate(3),p->true,nearby,16);
        if(nearby.stream().anyMatch(p->p.social.partner!=null))return;
        for(var other:nearby){
            if(other==npc || other.social.greeted!=null || now<other.social.nextConversation || !other.social.mayConverse()
                || npc.distanceToSqr(other)<.49 || !visible(other,3))continue;
            partner=other;other.social.partner=npc;firstSpeaker=true;other.social.firstSpeaker=false;
            started=other.social.started=now;until=other.social.until=now+CHAT_TICKS;
            spokenTurn=other.social.spokenTurn=-1;
            nextConversation=now+CHAT_COOLDOWN+Math.floorMod(npc.getId(),600);
            other.social.nextConversation=now+CHAT_COOLDOWN+Math.floorMod(other.getId(),600);
            return;
        }
    }
    public void save(ValueOutput out){long now=npc.level().getGameTime();out.putInt("SocialGreetingWait",(int)Math.clamp(nextGreeting-now,0,GREETING_COOLDOWN));out.putInt("SocialChatWait",(int)Math.clamp(nextConversation-now,0,CHAT_COOLDOWN+600));}
    public void load(ValueInput in){
        cancel();long now=npc.level().getGameTime();ownerNearby=false;
        nextGreeting=now+Math.clamp(in.getIntOr("SocialGreetingWait",0),0,GREETING_COOLDOWN);
        nextConversation=now+Math.clamp(in.getIntOr("SocialChatWait",0),0,CHAT_COOLDOWN+600);
        nextScan=now+20+Math.floorMod(npc.getId(),80);
    }
    /** Below work/rest/transport/commands, above wandering and ordinary looking. No paths are created. */
    static final class ConversationGoal extends Goal {
        private final PeepoEntity npc;
        ConversationGoal(PeepoEntity npc){this.npc=npc;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        public boolean requiresUpdateEveryTick(){return true;}
        public boolean canUse(){return npc.social.partner!=null && npc.social.mayConverse();}
        public boolean canContinueToUse(){return canUse();}
        public void start(){npc.social.goalActive=true;npc.getNavigation().stop();}
        public void tick(){npc.getNavigation().stop();}
        public void stop(){npc.social.cancel();}
    }
}
