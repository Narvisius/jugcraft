package io.github.jimbozoomer.jugcraft.finance;

import java.util.UUID;

/**
 * How a casino game ({@link Blackjack}, {@link Roulette}, {@link Craps}, a slot spin) moves money: the game's owner is
 * the house. Stakes are set aside from the player's Jugs when bet and settled when the bet is decided: the house keeps
 * what is lost and pays what is won, the central bank lending the house what it lacks ({@link FinanceBook#settleStake}).
 * The games are plain Java and get this from the block entity (or a test).
 */
public interface CasinoHouse {
	/** Sets a stake aside; null when done, otherwise why not. */
	String stake(UUID player, long amount);

	/** Settles a stake: {@code payout} is what the player gets back in all (0 lost, the stake on a push). */
	void settle(UUID player, String name, long stake, long payout, String what);

	/** Hands an unplayed stake back. */
	void refund(UUID player, long amount);

	/** The most the house can pay out now to this player (an owner playing their own game plays the bank). */
	long cover(UUID player);

	long minBet();

	long maxBet();
}
