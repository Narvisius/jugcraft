package io.github.jimbozoomer.jugcraft.concordance.logistics;

/** Where a container stands: a block position in a dimension (roadmap step 18). */
public record Place(String dimension, int x, int y, int z) {
	/** Whether {@code other} is in the same dimension and within {@code radius} blocks on every axis. */
	public boolean within(Place other, int radius) {
		return dimension.equals(other.dimension) && Math.abs(x - other.x) <= radius && Math.abs(y - other.y) <= radius
				&& Math.abs(z - other.z) <= radius;
	}

	public String text() {
		return x + " " + y + " " + z;
	}
}
