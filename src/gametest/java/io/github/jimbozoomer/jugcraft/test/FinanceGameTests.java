package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.finance.FinanceBook;
import io.github.jimbozoomer.jugcraft.finance.Ledger;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/** The Exchange's money rules and save format, on a real server. */
public class FinanceGameTests {
	/** Jugs are neither created nor lost by loans, payments, contracts, sales or repayment; only the admin's print adds them. */
	@GameTest
	public void moneyIsConserved(GameTestHelper helper) {
		FinanceBook book = new FinanceBook();
		UUID alice = UUID.randomUUID();
		UUID bob = UUID.randomUUID();
		book.account(alice, "Alice");
		book.account(bob, "Bob");
		book.advanceTo(0);
		long total = book.totalMoney();
		helper.assertTrue(book.starterLoan(alice, 0).ok(), "The starter loan failed");
		helper.assertTrue(!book.borrow(alice, 1, 0).ok(), "Borrowing past the credit limit was allowed");
		equal(helper, book.find(alice).debt, 110L, "Alice owes the loan plus its 10% fee");
		helper.assertTrue(book.pay(alice, "bob", 5, FinanceBook.Period.DAILY, "power", 0).ok(), "Signing a contract failed");
		book.advanceTo(1);
		equal(helper, book.cash(bob), 10L, "Bob's Jugs after two contract payments");
		helper.assertTrue(book.trade(bob, alice, 8, 1, "sale").ok(), "A sale failed");
		equal(helper, book.find(alice).debt, 108L, "A quarter of Alice's sale repaid her loan");
		helper.assertTrue(book.repay(alice, 40, 1).ok(), "Repaying failed");
		equal(helper, book.totalMoney(), total, "Total Jugs");
		helper.succeed();
	}

	/** The ledger survives a save and load unchanged (balances themselves are the town's Jugs, saved there). */
	@GameTest
	public void ledgerSavesAndLoads(GameTestHelper helper) {
		FinanceBook book = new FinanceBook();
		UUID alice = UUID.randomUUID();
		UUID bob = UUID.randomUUID();
		book.account(alice, "Alice");
		book.account(bob, "Bob");
		book.advanceTo(3);
		book.starterLoan(alice, 3);
		book.pay(alice, "Bob", 1, FinanceBook.Period.WEEKLY, "a farm's wheat", 3);

		Tag saved = Ledger.CODEC.encodeStart(NbtOps.INSTANCE, new Ledger(book)).getOrThrow();
		FinanceBook loaded = Ledger.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow().book();
		equal(helper, loaded.currency(), "Jugs", "Currency");
		equal(helper, loaded.find(alice).debt, 110L, "Alice's debt");
		equal(helper, loaded.find(alice).dueDay, 13L, "Alice's due day");
		helper.assertTrue(loaded.find(alice).starterTaken, "The starter loan was not remembered");
		equal(helper, loaded.termDays(), 10, "Loan term");
		equal(helper, loaded.find(bob).name, "Bob", "Bob's name");
		equal(helper, loaded.reserve(), book.reserve(), "Reserve");
		equal(helper, loaded.lastDay(), 3L, "Last day");
		equal(helper, loaded.contracts().size(), 1, "Contracts");
		equal(helper, loaded.contracts().get(0).nextDay, 10L, "Next weekly payment");
		equal(helper, loaded.log().size(), book.log().size(), "Ledger lines");
		helper.succeed();
	}

	private static void equal(GameTestHelper helper, Object actual, Object expected, String what) {
		helper.assertTrue(java.util.Objects.equals(actual, expected), what + ": expected " + expected + ", got " + actual);
	}
}
