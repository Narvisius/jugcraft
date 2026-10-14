package local.peepo;

/** Short visual transfer gestures. They never delay or own inventory operations. */
public enum CompanionGesture {
    NONE, CARRY, TAKE, PUT, POUR, TURN_FOOD;
    public static final int TICKS=12;
}
