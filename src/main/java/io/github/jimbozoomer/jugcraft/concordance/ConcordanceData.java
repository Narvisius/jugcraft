package io.github.jimbozoomer.jugcraft.concordance;

import com.google.gson.JsonElement;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Loads the Concordance's rules from every data pack's {@code data/<ns>/concordance/<kind>/<name>.json} (kind is
 * research, invocation, working, conversion, component or instrument) on server start and on {@code /reload}. A file that does not parse, or whose
 * references do not resolve, is left out with a logged reason that {@code /jugcraft concordance diagnose} also lists;
 * the rest load. The new rules replace the old in one step, after which every online player's invocations are worked
 * out again ({@link ConcordanceProgress#relearnAll}).
 */
public final class ConcordanceData extends SimpleJsonResourceReloadListener<JsonElement> {
	public static final String FOLDER = "concordance";
	public static final Identifier ID = Jugcraft.id("concordance_rules");
	static final ConcordanceData INSTANCE = new ConcordanceData();
	private static volatile ConcordanceRules rules = ConcordanceRules.EMPTY;

	private ConcordanceData() {
		super(ExtraCodecs.JSON, FileToIdConverter.json(FOLDER));
	}

	/** The rules in force. Never null; empty until the first data load. */
	public static ConcordanceRules rules() {
		return rules;
	}

	/** Replaces the rules (game tests use this to check how bad data is handled). */
	public static void replace(ConcordanceRules replacement) {
		rules = replacement;
	}

	/** Splits {@code <ns>:<kind>/<name>} into a rules source of that kind with id {@code <ns>:<name>}. */
	public static List<ConcordanceRules.Source> sources(Map<Identifier, JsonElement> elements) {
		List<ConcordanceRules.Source> sources = new ArrayList<>();
		elements.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString))).forEach(entry -> {
			String path = entry.getKey().getPath();
			int slash = path.indexOf('/');
			String kind = slash < 0 ? "" : path.substring(0, slash);
			String id = entry.getKey().getNamespace() + ":" + (slash < 0 ? path : path.substring(slash + 1));
			sources.add(new ConcordanceRules.Source(kind, id, entry.getValue()));
		});
		return sources;
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> elements, ResourceManager resourceManager, ProfilerFiller profiler) {
		ConcordanceRules loaded = ConcordanceRules.build(sources(elements));
		for (String problem : loaded.problems()) {
			Jugcraft.LOGGER.error("Arcane Concordance data: {}", problem);
		}
		Jugcraft.LOGGER.info("Arcane Concordance: {} research entries, {} invocations, {} workings, {} components ({} problems)",
				loaded.research().size(), loaded.invocations().size(), loaded.workings().size(), loaded.catalog().components().size(),
				loaded.problems().size());
		rules = loaded;
	}
}
