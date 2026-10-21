package io.github.jimbozoomer.jugcraft.finance;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.BlockPos;

/** Integration regressions for the Exchange on current main, authored with OpenAI Codex. */
public class ExchangeIntegrationGameTests {
    private static ServerPlayer actor(GameTestHelper h, String name, FinanceBook book) {
        var p = FakePlayer.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)));
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        book.account(p.getUUID(), name);
        check(h, book.grant(p.getUUID(), 10000, 0).ok(), "fund test account");
        return p;
    }
    private static void check(GameTestHelper h, boolean yes, String label) { h.assertTrue(yes, label); }
    @GameTest public void overflowRefundIsOwnedAndSurvivesSave(GameTestHelper h) {
        FinanceBook b = new FinanceBook(); var w = new FinanceBook.MapWallet(); b.useWallet(w);
        UUID a = UUID.randomUUID(), other = UUID.randomUUID(); b.account(a,"Full");b.account(other,"Other");
        w.add(a, FinanceBook.MAX_AMOUNT); long total = b.totalMoney();
        check(h,b.grant(a,50,0).ok(),"grant accepted");
        check(h,b.cash(a)==FinanceBook.MAX_AMOUNT+50 && b.pendingCredits().get(a)==50,"overflow belongs to recipient");
        check(h,b.totalMoney()==total,"grant conserved total money");
        var saved=Ledger.CODEC.encodeStart(NbtOps.INSTANCE,new Ledger(b)).getOrThrow();
        var restored=Ledger.CODEC.parse(NbtOps.INSTANCE,saved).getOrThrow().book(); restored.useWallet(w);
        check(h,restored.cash(a)==b.cash(a),"saved credit restored");
        check(h,restored.pay(a,"Other",50,FinanceBook.Period.ONCE,"credit",0).ok(),"spend saved credit");
        check(h,restored.cash(other)==50 && restored.pendingCredits().isEmpty(),"credit spent once");
        check(h,restored.totalMoney()==total,"payment conserved money");h.succeed();
    }
    @GameTest public void negativeEscrowCannotMintMoney(GameTestHelper h) {
        var b=new FinanceBook();var a=actor(h,"Escrow",b);long total=b.totalMoney();
        b.hold(a.getUUID(),100,0,"hold"); long escrow=b.escrow(); long cash=b.cash(a.getUUID());
        b.release(a.getUUID(),-100,0,"invalid");b.settle(a.getUUID(),a.getUUID(),Long.MIN_VALUE,0,"invalid");
        check(h,b.escrow()==escrow && b.cash(a.getUUID())==cash,"negative releases are inert");
        b.release(a.getUUID(),100,0,"refund");check(h,b.totalMoney()==total && b.escrow()==0,"refund conserved money");h.succeed();
    }
    @GameTest public void marketPurchasesRemainCollectibleWhenFull(GameTestHelper h) {
        var b=new FinanceBook();var seller=actor(h,"Seller",b);var buyer=actor(h,"Buyer",b);var m=new Market();
        seller.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND,7));
        long total=b.totalMoney();check(h,m.list(seller,b,7,3,false,0,0,0).ok(),"list diamonds");
        long id=m.listings().getFirst().id;
        check(h,m.buy(buyer,b,id,7,0).ok(),"purchase");check(h,!m.buy(buyer,b,id,7,0).ok(),"sold listing cannot be bought twice");
        for(int i=0;i<36;i++)buyer.getInventory().setItem(i,new ItemStack(Items.STONE,64));
        check(h,!m.collect(buyer).ok() && m.waiting(buyer.getUUID())>0,"full inventory preserves collection");
        var ops=h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var saved=Market.CODEC.encodeStart(ops,m).getOrThrow(); var restored=Market.CODEC.parse(ops,saved).getOrThrow();
        buyer.getInventory().setItem(0,ItemStack.EMPTY);check(h,restored.collect(buyer).ok(),"collect after save");
        check(h,buyer.getInventory().getItem(0).is(Items.DIAMOND) && buyer.getInventory().getItem(0).getCount()==7,"exact goods returned");
        check(h,!restored.collect(buyer).ok() && b.totalMoney()==total,"no double collection or money creation");h.succeed();
    }
    @GameTest public void requestEscrowAndAuctionRefundsAreConserved(GameTestHelper h) {
        var b=new FinanceBook();var a=actor(h,"Buyer",b);var s=actor(h,"Supplier",b);var m=new Market();long total=b.totalMoney();
        check(h,m.postRequest(a,b,"minecraft:iron_ingot",10,5,0).ok(),"post request");long id=m.requests().getFirst().id;
        s.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_INGOT,4));
        check(h,m.fill(s,b,id,4,0).ok() && b.escrow()==30,"partial request releases exact payment");
        check(h,!m.cancelRequest(s,b,id,false,0).ok(),"stranger cannot cancel request");
        check(h,m.cancelRequest(a,b,id,false,0).ok() && b.escrow()==0,"request refund");
        s.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.GOLD_INGOT,1));
        check(h,m.list(s,b,1,10,true,0,0,0).ok(),"auction list");id=m.listings().getFirst().id;
        check(h,m.bid(a,b,id,10,0).ok(),"auction bid");
        var c=actor(h,"Outbid",b);check(h,m.bid(c,b,id,12,0).ok() && b.escrow()==12,"outbid refund");
        check(h,!m.cancelListing(s,b,id,false,0).ok(),"seller cannot cancel live auction");
        m.newDay(h.getLevel().getServer(),b,1);check(h,b.escrow()==0 && b.totalMoney()==total,"settled once and conserved");h.succeed();
    }
    @GameTest public void companyOrdersAndLandRespectOwners(GameTestHelper h) {
        var b=Ledger.get(h.getLevel().getServer()).book();var a=actor(h,"Founder",b);var buyer=actor(h,"Holder",b);
        var e=Equities.get(h.getLevel().getServer()); String ticker="T"+UUID.randomUUID().toString().substring(0,4).replace('0','G').replace('1','H').replace('2','I').replace('3','J').replace('4','K').replace('5','L').replace('6','M').replace('7','N').replace('8','O').replace('9','P').toUpperCase();
        check(h,e.charter(a,b,"Company "+ticker,ticker,100,2000,5,new byte[256],false,0).ok(),"charter");var corp=e.byTicker(ticker);
        long total=b.totalMoney();check(h,e.order(buyer,b,corp.id,true,10,5,0).ok(),"buy issued shares");
        check(h,e.controller(corp).equals(a.getUUID()) && corp.holdings.get(buyer.getUUID())==10,"ownership and controller");
        check(h,!e.issue(buyer,b,corp.id,10,5,0).ok(),"noncontroller cannot dilute");
        check(h,b.totalMoney()==total,"share trade conservation");
        var land=LandRegistry.get(h.getLevel().getServer());long key=LandRegistry.keyOf(a.blockPosition());
        check(h,land.assign(b,key,b.find(a.getUUID()),0).ok(),"assign claim fixture");
        try {
            check(h,LandRegistry.mayBuild(a,h.getLevel(),a.blockPosition()),"owner permitted");
            check(h,!LandRegistry.mayBuild(buyer,h.getLevel(),a.blockPosition()),"stranger refused");
            check(h,!FinanceService.canAccess(buyer,a.blockPosition(),256),"forged table request respects claim");
            check(h,land.trust(a,b,"Holder",true).ok(),"trust player");
            check(h,LandRegistry.mayBuild(buyer,h.getLevel(),a.blockPosition()),"trusted player permitted");
            check(h,!FinanceService.canAccess(a,new BlockPos(1000000,64,1000000),256),"far request refused before loading");
        } finally { land.abandon(a,b,key,true,0); }
        h.succeed();
    }
    @GameTest public void pokerSidePotsAndLeavingConserveChips(GameTestHelper h) {
        for(int seed=0;seed<30;seed++) {
            long[] paid={0};var g=new PokerGame((id,name,chips)->paid[0]+=chips,new Random(seed));
            UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID();g.sit(0,a,"A",40);g.sit(1,b,"B",100);g.sit(2,c,"C",200);
            for(int t=0;t<1000 && g.phase!=PokerGame.Phase.SHOWDOWN;t++) {
                g.tick();if(g.actor>=0 && g.handRunning())g.act(g.seats[g.actor].player,PokerGame.Move.ALL_IN,0);
                check(h,g.chipsOnTable()+paid[0]==340,"all-in side pots conserved, seed "+seed);
            }
            g.stand(a);g.stand(b);g.stand(c);check(h,g.chipsOnTable()+paid[0]==340,"standing refunds every chip");
        }h.succeed();
    }
    private static CasinoHouse house(FinanceBook b, UUID owner) {
        return new CasinoHouse() {
            public String stake(UUID id,long n) { var r=b.stake(id,n,0); return r.ok()?null:r.message(); }
            public void settle(UUID id,String name,long stake,long out,String what) { b.settleStake(id,owner,stake,out,0,null); }
            public void refund(UUID id,long n) { b.refundStake(id,n,0); }
            public long cover(UUID id) { return b.houseCover(owner,0); }
            public long minBet() { return 1; }
            public long maxBet() { return 10; }
        };
    }
    @GameTest public void casinoCancellationsReturnEachStakeOnce(GameTestHelper h) {
        var b=new FinanceBook();var owner=actor(h,"House",b);var player=actor(h,"Bettor",b);var id=player.getUUID();
        var house=house(b,owner.getUUID());long total=b.totalMoney(), cash=b.cash(id);
        var bj=new Blackjack(house,new Random(1));check(h,bj.bet(id,"Bettor",0,5).ok(),"blackjack bet");bj.cancelAll();bj.cancelAll();
        var r=new Roulette(house,new Random(2));check(h,r.bet(id,"Bettor",Roulette.bit(1),5).ok(),"roulette bet");r.cancelAll();r.cancelAll();
        var c=new Craps(house,new Random(3));check(h,c.bet(id,"Bettor",Craps.Kind.PASS,0,5).ok(),"craps bet");c.cancelAll();c.cancelAll();
        var wheel=new ShowGames.BigSix(house,new Random(4));check(h,wheel.bet(id,"Bettor",0,5).ok(),"wheel bet");wheel.cancelAll();wheel.cancelAll();
        var ba=new ShowGames.Baccarat(house,new Random(5));check(h,ba.bet(id,"Bettor",0,5).ok(),"baccarat bet");ba.cancelAll();ba.cancelAll();
        check(h,b.totalMoney()==total && b.cash(id)==cash && b.escrow()==0,"cancel twice never duplicates or strands stakes");h.succeed();
    }
    @GameTest public void allSlotVariantsConserveMoney(GameTestHelper h) {
        for(var machine:Slots.Machine.values()) {
            var b=new FinanceBook();b.mint(10000000,0);var owner=actor(h,"House",b);var p=actor(h,"SlotPlayer",b);
            b.grant(owner.getUUID(),5000000,0);b.grant(p.getUUID(),100000,0);long total=b.totalMoney();
            var slot=new SlotGame(machine,house(b,owner.getUUID()),new Random(416+machine.ordinal()));
            int accepted=0;
            for(int n=0;n<60;n++) {
                slot.use(p.getUUID(),"SlotPlayer");
                if(slot.spin(p.getUUID(),"SlotPlayer",1,500,0).ok()) accepted++;
                for(int tick=0;tick<200;tick++)slot.tick();
                slot.collect(p.getUUID());slot.release(p.getUUID());
                check(h,b.totalMoney()==total,"slot conservation: "+machine);
            }
            check(h,accepted>0 && b.escrow()==0,"played and settled: "+machine);
        }h.succeed();
    }

    @GameTest public void unconfiguredMarketChestOwnerSurvivesSave(GameTestHelper h) {
        var b=new FinanceBook();var owner=actor(h,"Placer",b);var other=actor(h,"Intruder",b);
        var at=h.absolutePos(BlockPos.ZERO);var state=ExchangeBlocks.MARKET_CHEST.defaultBlockState();
        var chest=new MarketChestBlockEntity(at,state);chest.setLevel(h.getLevel());chest.setOwner(owner);
        check(h,chest.mayUse(owner)&&!chest.mayUse(other),"only placer can configure empty company chest");
        var saved=chest.saveWithoutMetadata(h.getLevel().registryAccess());var copy=new MarketChestBlockEntity(at,state);copy.setLevel(h.getLevel());
        copy.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        check(h,copy.mayUse(owner)&&!copy.mayUse(other),"ownership preserved before first opening");h.succeed();
    }
    @GameTest public void buyRequestNeverErasesCustomItemData(GameTestHelper h) {
        var b=new FinanceBook();var buyer=actor(h,"Buyer",b);var seller=actor(h,"Smith",b);var market=new Market();
        market.postRequest(buyer,b,"minecraft:diamond_sword",1,10,0);long id=market.requests().getFirst().id;
        var sword=new ItemStack(Items.DIAMOND_SWORD);sword.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Keepsake"));
        seller.setItemSlot(EquipmentSlot.MAINHAND,sword);
        check(h,!market.fill(seller,b,id,1,0).ok() && seller.getMainHandItem()==sword,"request must not strip named equipment");
        market.cancelRequest(buyer,b,id,false,0);h.succeed();
    }

    @GameTest public void unpaidWinningsSurviveSaveAndPayExactlyOnce(GameTestHelper h) {
        var b=new FinanceBook();var p=actor(h,"Winner",b);var w=new FinanceBook.MapWallet();
        w.add(p.getUUID(),b.cash(p.getUUID()));b.useWallet(w);
        long total=b.totalMoney();b.stake(p.getUUID(),10,0);
        b.burn(b.reserve(),0);long depleted=b.totalMoney();
        b.settleStake(p.getUUID(),null,10,110,0,null);
        check(h,b.prizeDebts().size()==1 && b.prizeDebts().getFirst().amount()==100,"shortfall is retained");
        check(h,b.bankGameCover()==0 && b.totalMoney()==depleted,"no unfunded new bets or minted currency");
        var encoded=Ledger.CODEC.encodeStart(NbtOps.INSTANCE,new Ledger(b)).getOrThrow();
        var copy=Ledger.CODEC.parse(NbtOps.INSTANCE,encoded).getOrThrow().book();copy.useWallet(w);
        long before=copy.cash(p.getUUID());copy.mint(100,1);
        check(h,copy.payPrizeDebts(1) && copy.cash(p.getUUID())==before+100,"saved winnings paid after funding");
        check(h,copy.prizeDebts().isEmpty() && !copy.payPrizeDebts(1) && copy.totalMoney()==depleted+100,"paid exactly once without money creation");
        h.succeed();
    }

    @GameTest public void paidScratchTicketsCannotBeCopiedOrTransmuted(GameTestHelper h) {
        var book=new ScratchTickets();var card=new ItemStack(ExchangeBlocks.SCRATCH_CARDS.values().iterator().next());
        check(h,!book.redeem(card,0),"alchemy blank is not a paid ticket");book.issue(card,0);var clone=card.copy();
        var save=ScratchTickets.CODEC.encodeStart(NbtOps.INSTANCE,book).getOrThrow();
        var restored=ScratchTickets.CODEC.parse(NbtOps.INSTANCE,save).getOrThrow();
        check(h,!restored.redeem(card,1) && restored.redeem(card,0),"only correct paid card can redeem after load");
        check(h,!restored.redeem(clone,0),"copied paid data never redeems twice");h.succeed();
    }

    @GameTest public void welcomeGuideDoesNotPolluteAutomationInventories(GameTestHelper h) {
        var player=h.makeMockServerPlayerInLevel();
        check(h,player.getInventory().countItem(ExchangeGuide.GUIDE)==0,"a player without an Exchange client must not receive an unreadable welcome item");
        h.succeed();
    }

}
