package io.github.jimbozoomer.jugcraft.client.arms;

/**
 * One entity's arms pose for this frame (batch 43, {@link ArmsMotion}): filled when its render state is extracted and
 * read when its model is posed and its held item drawn. One per render state, reused every frame, so posing allocates
 * nothing.
 */
public final class ArmsPose {
	/** Whether this frame's entity holds an arm with motion. */
	public boolean active;
	/** Which bones the kind moves (bit i for {@link ArmsMotion#BONE_NAMES}[i]). */
	public int bones;
	/** How far the pose has blended in over vanilla's, 0 to 1 (it eases in when the arm is taken in hand). */
	public float weight;
	/** The pose: each bone's rx, ry, rz (degrees) and x, y, z (pixels), as if held in the right hand. */
	public final float[] values = new float[ArmsMotion.SIZE];
	/** Two-handed kinds: how far below the main hand the off hand holds the grip or haft (pixels); 0 for one hand. */
	public float twoHanded;
	/** The arm is in the left hand: the pose is mirrored when applied. */
	public boolean leftHanded;
}
