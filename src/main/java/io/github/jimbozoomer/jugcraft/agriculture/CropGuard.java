package io.github.jimbozoomer.jugcraft.agriculture;

/**
 * Something that walks about and keeps crows off crops as a scarecrow does ({@link Scarecrows#guarded}): a Hay Golem or
 * a Pumpkling. Its radius is in blocks across the ground, at most {@link Scarecrows#LIT}.
 */
public interface CropGuard {
	int guardRadius();
}
