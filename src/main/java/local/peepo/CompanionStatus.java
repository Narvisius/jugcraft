package local.peepo;

public enum CompanionStatus {
    NONE("Not assigned"), READY("Ready"), TRAVELLING("Travelling"), WORKING("Working"),
    RESTING("Resting"), EATING("Eating"), FULL("Full"), NO_INPUT("Missing inputs"), NO_POWER("No power"),
    OCCUPIED("Occupied"), BLOCKED("Path blocked"), UNLOADED("Unloaded"), MISSING("Missing/replaced"),
    UNSUPPORTED("Unsupported job"), SCHEDULED_REST("Off shift"), RECOVERING("Recovering"),
    FORBIDDEN("Access denied"), NO_FOOD("No suitable food"), WAITING("Waiting for budget"), IDLE("Idle"),
    FOLLOWING("Following"), STAYING("Staying"), FETCHING_FOOD("Getting lunch"), OTHER_DIMENSION("Other dimension"),
    NO_HEAT("No heat"), RECIPE_MISSING("Recipe unavailable"), REDSTONE_DISABLED("Disabled by redstone"),
    FETCHING_SUPPLIES("Getting supplies"), COLLECTING_OUTPUT("Collecting output"), DELIVERING("Delivering"), RETURNING_SUPPLIES("Returning supplies"),
    SPOILED_INPUT("Spoiled jars"), PORTER_SETUP("Assign Supply + Output"), SUPPLY_EMPTY("Supply empty"), GROWING("Crops growing"), NO_SEEDS("Needs seeds"), GARDEN_DISABLED("Mob griefing disabled"), GREETING("Greeting"), CHATTING("Chatting"), NO_TOOL("Needs a knife"),
    NEEDS_SHEARS("Needs shears"), NO_BUCKETS("Needs empty buckets"), ANIMALS_WAIT("Waiting for animals"), NEEDS_CUTTER("Needs shears or a knife"), NO_BUNDLES("Needs herb bundles"), DRYING("Herbs drying"), EGGS_WAIT("Waiting for eggs"), POPULATION_SETUP("Choose animal types"), POPULATION_LIMIT("Population at target"), NEEDS_BREEDING_FOOD("Needs breeding food"), NEEDS_WEAPON("Needs sword or axe"), POPULATION_BUSY("Pen too crowded"), AGING("Cider aging"), NEEDS_BOTTLES("Needs bottles"), COOLING("Cooling down"), CANDY_SETTING("Candy setting"), HONEY_GROWING("Waiting for honey"), NEEDS_SMOKE("Needs campfire smoke"), STOCK_SETUP("Stock sale slots first"), NEEDS_WATER("Needs piped water"), WRONG_TEMPERATURE("Wrong temperature"), MAGIC_DISABLED("Magic garden disabled"), DORMANT_GARDEN("Needs awakened beds/device"), NEEDS_VERDANCE("Needs Verdance"), GUANO_WAIT("Waiting for guano"), BAT_HOUSE_DISABLED("Bat houses disabled"), MACHINE_UNFORMED("Machine structure incomplete"), MACHINE_PAUSED("Machine paused"), MACHINE_TOOL("Machine needs a tool"), SERVING_SETUP("Stock table dishes first"), NEEDS_HOE("Needs a hoe"), NEEDS_FERTILIZER("Needs fertilizer"), NO_WATER_SOURCE("Needs source water"), NEEDS_CONTAINERS("Needs empty containers");
    public final String label;
    CompanionStatus(String label){this.label=label;}
    public static CompanionStatus from(int id){return values()[Math.clamp(id,0,values().length-1)];}
    public boolean problem(){return this==NO_WATER_SOURCE || this==NEEDS_CONTAINERS || this==SERVING_SETUP || this==NEEDS_HOE || this==NEEDS_FERTILIZER || this==MACHINE_UNFORMED || this==MACHINE_TOOL || this==BAT_HOUSE_DISABLED || this==MAGIC_DISABLED || this==DORMANT_GARDEN || this==NEEDS_VERDANCE || this==STOCK_SETUP || this==NEEDS_WATER || this==WRONG_TEMPERATURE || this==NEEDS_SMOKE || this==NEEDS_BOTTLES || this==NEEDS_BREEDING_FOOD || this==NEEDS_WEAPON || this==POPULATION_SETUP || this==NEEDS_CUTTER || this==NO_BUNDLES || this==NEEDS_SHEARS || this==NO_BUCKETS || this==NO_TOOL || this==FULL || this==NO_INPUT || this==NO_POWER || this==BLOCKED || this==NO_FOOD || this==MISSING || this==FORBIDDEN || this==NO_HEAT || this==RECIPE_MISSING || this==REDSTONE_DISABLED || this==SPOILED_INPUT || this==NO_SEEDS;}
}
