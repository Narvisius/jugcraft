package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Server-owned action selection for recovered owner animations. */
public final class RecoveredCreatureActions {
    private static final Map<String, Map<String, Integer>> PROFILES = load();
    private static Map<String, Map<String, Integer>> load() {
        var result = new HashMap<String, Map<String, Integer>>();
        try (var in = RecoveredCreatureActions.class.getResourceAsStream("/jugcraft/menagerie/recovered_actions.json")) {
            if (in == null) throw new IllegalStateException("Missing recovered action catalog");
            JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().entrySet().forEach(e -> {
                var actions = new LinkedHashMap<String, Integer>();
                e.getValue().getAsJsonObject().entrySet().forEach(a -> actions.put(a.getKey(), a.getValue().getAsInt()));
                result.put(e.getKey(), Collections.unmodifiableMap(actions));
            });
        } catch (IOException e) { throw new IllegalStateException(e); }
        return Collections.unmodifiableMap(result);
    }
    public static int duration(String species, String action) { return PROFILES.getOrDefault(species, Map.of()).getOrDefault(action, 0); }
    public static void play(MenagerieAnimal animal, String... choices) {
        if (animal.level().isClientSide()) return;
        if(animal.recoveredActionTime(0)>=0&&java.util.Arrays.stream(choices).anyMatch(c->animal.recoveredAction().equals(c)||animal.recoveredAction().startsWith(c+"_")))return;
        var profile = PROFILES.getOrDefault(animal.species().id(), Map.of());
        for (String choice : choices) {
            var matching=profile.keySet().stream().filter(a->a.equals(choice)||a.startsWith(choice+"_")).toList();
            if(!matching.isEmpty()){animal.recoveredAction(matching.get(animal.getRandom().nextInt(matching.size())));return;}
        }
    }
    public static void idle(MenagerieAnimal animal) {
        if (animal.tickCount % 160 == Math.floorMod(animal.getId(), 160) && animal.getTarget() == null
                && !animal.sitting() && animal.getDeltaMovement().horizontalDistanceSqr() < .001
                && animal.recoveredActionTime(0) < 0)
            play(animal, "sniff", "look", "speak", "idle", "blink");
    }
    private RecoveredCreatureActions() {}
}
