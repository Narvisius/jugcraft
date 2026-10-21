package io.github.jimbozoomer.jugcraft.finance;

/**
 * Texas hold'em hand ranking. Cards are 0..51: rank = card % 13 (0 is a two, 12 an ace), suit = card / 13. A hand's
 * score is one int that compares the way poker hands do: the category (high card 0 up to straight flush 8) in the top
 * bits, then up to five ranks that break ties, highest first. Plain Java, so it is tested without a game.
 */
public final class PokerHands {
	public static final String[] CATEGORY = {"High card", "Pair", "Two pair", "Three of a kind", "Straight", "Flush",
			"Full house", "Four of a kind", "Straight flush"};
	private static final String RANKS = "23456789TJQKA";
	private static final String[] RANK_NAMES = {"Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
			"Jack", "Queen", "King", "Ace"};
	private static final char[] SUITS = {'♣', '♦', '♥', '♠'};

	private PokerHands() {
	}

	public static int rank(int card) {
		return card % 13;
	}

	public static int suit(int card) {
		return card / 13;
	}

	/** "A♠", "T♥": rank letter and suit symbol (clubs, diamonds, hearts, spades). */
	public static String text(int card) {
		return card < 0 ? "??" : RANKS.charAt(rank(card)) + String.valueOf(SUITS[suit(card)]);
	}

	public static String rankName(int rank) {
		return RANK_NAMES[rank];
	}

	public static boolean red(int card) {
		return suit(card) == 1 || suit(card) == 2;
	}

	/** The category of a score (0 high card .. 8 straight flush). */
	public static int category(int score) {
		return score >>> 20;
	}

	/** The best five of these cards (five to seven of them). */
	public static int best(int[] cards) {
		int n = cards.length;
		int best = -1;
		int[] five = new int[5];
		for (int a = 0; a < n; a++) {
			for (int b = a + 1; b < n; b++) {
				for (int c = b + 1; c < n; c++) {
					for (int d = c + 1; d < n; d++) {
						for (int e = d + 1; e < n; e++) {
							five[0] = cards[a];
							five[1] = cards[b];
							five[2] = cards[c];
							five[3] = cards[d];
							five[4] = cards[e];
							best = Math.max(best, score5(five));
						}
					}
				}
			}
		}
		return best;
	}

	/** The score of exactly five cards. */
	public static int score5(int[] five) {
		int[] count = new int[13];
		boolean flush = true;
		for (int i = 0; i < 5; i++) {
			count[rank(five[i])]++;
			if (suit(five[i]) != suit(five[0])) {
				flush = false;
			}
		}
		// Straight: five distinct ranks in a row; A-2-3-4-5 counts with the five high.
		int straightHigh = -1;
		for (int high = 12; high >= 4; high--) {
			boolean run = true;
			for (int k = 0; k < 5; k++) {
				if (count[high - k] != 1) {
					run = false;
					break;
				}
			}
			if (run) {
				straightHigh = high;
				break;
			}
		}
		if (straightHigh < 0 && count[12] == 1 && count[0] == 1 && count[1] == 1 && count[2] == 1 && count[3] == 1) {
			straightHigh = 3;
		}
		// Ranks ordered by how many there are, then by rank: the tie-breakers.
		int[] order = new int[5];
		int filled = 0;
		for (int times = 4; times >= 1; times--) {
			for (int r = 12; r >= 0; r--) {
				if (count[r] == times) {
					order[filled++] = r;
				}
			}
		}
		int category;
		if (straightHigh >= 0 && flush) {
			category = 8;
		} else if (count[order[0]] == 4) {
			category = 7;
		} else if (count[order[0]] == 3 && count[order[1]] == 2) {
			category = 6;
		} else if (flush) {
			category = 5;
		} else if (straightHigh >= 0) {
			category = 4;
		} else if (count[order[0]] == 3) {
			category = 3;
		} else if (count[order[0]] == 2 && count[order[1]] == 2) {
			category = 2;
		} else if (count[order[0]] == 2) {
			category = 1;
		} else {
			category = 0;
		}
		int score = category << 20;
		if (category == 8 || category == 4) {
			return score | straightHigh << 16;
		}
		for (int i = 0; i < filled; i++) {
			score |= order[i] << (16 - 4 * i);
		}
		return score;
	}

	/** "Full house, Kings over Fours", "Flush, Ace high", "Pair of Nines". */
	public static String describe(int score) {
		int category = category(score);
		int top = (score >>> 16) & 15;
		int second = (score >>> 12) & 15;
		return switch (category) {
			case 8 -> top == 12 ? "Royal flush" : "Straight flush, " + rankName(top) + " high";
			case 7 -> "Four " + plural(top);
			case 6 -> "Full house, " + plural(top) + " over " + plural(second);
			case 5 -> "Flush, " + rankName(top) + " high";
			case 4 -> "Straight, " + rankName(top) + " high";
			case 3 -> "Three " + plural(top);
			case 2 -> "Two pair, " + plural(top) + " and " + plural(second);
			case 1 -> "Pair of " + plural(top);
			default -> rankName(top) + " high";
		};
	}

	private static String plural(int rank) {
		return rank == 4 ? "Sixes" : rankName(rank) + "s";
	}
}
