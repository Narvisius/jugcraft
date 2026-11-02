package io.github.jimbozoomer.jugcraft.town;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * {@code /npc}, {@code /guard} and {@code /jugcraft npc}: operators (permission level 2) make, edit and remove custom
 * NPCs and guards ({@link CustomNpcs}). The three are the same commands; {@code /guard create} makes a guard. Every node
 * requires an operator, and every command checks again on the server before it changes anything.
 *
 * <p><b>Which NPC.</b> Editing commands take no number. They act on the NPC you selected (making one selects it, as
 * does {@code select}, {@code select <id>} or talking to one while sneaking), or, with none selected, the one you are
 * looking at. Places (a post, a waypoint) are where you stand unless you give a position.
 *
 * <pre>
 * create &lt;name&gt; | guard &lt;name&gt;     make one where you stand, facing your way, and select it
 * select [id] | list | info [id]    select: the NPC you look at, else the nearest within 16 blocks
 * remove [id]                      at once if loaded, otherwise when its chunk next loads
 * name &lt;name&gt;                      skin player &lt;name&gt; | skin town &lt;skin&gt;
 * say &lt;text&gt; | say add &lt;text&gt; | say list | say remove &lt;n&gt; | quiet
 * wander &lt;0-16&gt;                    post [pos]                    patrol add [pos] | list | remove [n] | clear
 * </pre>
 */
public final class CustomNpcCommand {
	private static final int LIST_LIMIT = 40;
	/** How far away an NPC can be looked at to pick it, and how far {@code select} looks for the nearest. */
	private static final double LOOK_REACH = 8, NEAREST_REACH = 16;
	private static final UUID CONSOLE = new UUID(0, 0);

	/** Who has selected which NPC number. In memory; bounded by the number of operators. */
	private static final Map<UUID, Integer> SELECTED = new HashMap<>();
	private static @Nullable MinecraftServer selectedOn;

	private CustomNpcCommand() {
	}

	static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jugcraft").then(tree("npc", CustomNpcs.NPC)));
		dispatcher.register(tree("npc", CustomNpcs.NPC));
		dispatcher.register(tree("guard", CustomNpcs.GUARD));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, Integer> id() {
		return Commands.argument("id", IntegerArgumentType.integer(1)).suggests((context, builder) -> {
			if (CustomNpcs.isOperator(context.getSource())) {
				CustomNpcs.registry(context.getSource().getServer()).entries().keySet().forEach(builder::suggest);
			}
			return builder.buildFuture();
		});
	}

	/** The whole command under {@code root}; its {@code create} makes {@code kind}. */
	private static LiteralArgumentBuilder<CommandSourceStack> tree(String root, String kind) {
		return Commands.literal(root).requires(CustomNpcs::isOperator)
				.then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.greedyString())
						.executes(c -> create(c, kind))))
				.then(Commands.literal("guard").then(Commands.argument("name", StringArgumentType.greedyString())
						.executes(c -> create(c, CustomNpcs.GUARD))))
				.then(Commands.literal("select")
						.executes(CustomNpcCommand::selectHere)
						.then(id().executes(c -> selectNumber(c.getSource(), IntegerArgumentType.getInteger(c, "id")))))
				.then(Commands.literal("list").executes(CustomNpcCommand::list))
				.then(Commands.literal("info")
						.executes(c -> edit(c, npc -> info(c.getSource(), npc)))
						.then(id().executes(c -> {
							Townsfolk npc = numbered(c.getSource(), IntegerArgumentType.getInteger(c, "id"));
							return npc == null ? 0 : info(c.getSource(), npc);
						})))
				.then(Commands.literal("remove")
						.executes(c -> edit(c, npc -> remove(c.getSource(), npc.custom().id())))
						.then(id().executes(c -> operator(c.getSource()) ? remove(c.getSource(), IntegerArgumentType.getInteger(c, "id")) : 0)))
				.then(Commands.literal("name").then(Commands.argument("name", StringArgumentType.greedyString())
						.executes(c -> edit(c, npc -> {
							String name = CustomNpcs.clean(StringArgumentType.getString(c, "name"), CustomNpcs.MAX_NAME);
							if (name.isEmpty()) {
								return fail(c.getSource(), "That name is empty.");
							}
							CustomNpcs.rename(npc, name);
							return ok(c.getSource(), label(npc) + " is renamed.");
						}))))
				.then(Commands.literal("skin")
						.then(Commands.literal("player").then(Commands.argument("player", StringArgumentType.word())
								.executes(c -> edit(c, npc -> playerSkin(c.getSource(), npc, StringArgumentType.getString(c, "player"))))))
						.then(Commands.literal("town").then(Commands.argument("skin", StringArgumentType.word())
								.suggests((context, builder) -> {
									if (CustomNpcs.isOperator(context.getSource())) {
										CustomNpcs.townSkins().stream().sorted().forEach(builder::suggest);
									}
									return builder.buildFuture();
								})
								.executes(c -> edit(c, npc -> {
									String skin = StringArgumentType.getString(c, "skin");
									return CustomNpcs.setTownSkin(npc, skin) ? ok(c.getSource(), label(npc) + " wears " + skin + ".")
											: fail(c.getSource(), "The town has no skin called " + skin + ".");
								})))))
				.then(Commands.literal("say")
						.then(Commands.literal("add").then(Commands.argument("text", StringArgumentType.greedyString())
								.executes(c -> edit(c, npc -> {
									String refused = CustomNpcs.addLine(npc, StringArgumentType.getString(c, "text"));
									return refused != null ? fail(c.getSource(), refused)
											: ok(c.getSource(), label(npc) + " has " + npc.custom().lines().size() + " line(s).");
								}))))
						.then(Commands.literal("list").executes(c -> edit(c, npc -> lines(c.getSource(), npc))))
						.then(Commands.literal("remove").then(Commands.argument("n", IntegerArgumentType.integer(1, CustomNpcs.MAX_LINES))
								.executes(c -> edit(c, npc -> {
									int n = IntegerArgumentType.getInteger(c, "n");
									return CustomNpcs.removeLine(npc, n - 1) ? ok(c.getSource(), label(npc) + ": removed line " + n + ".")
											: fail(c.getSource(), "It has no line " + n + ".");
								}))))
						.then(Commands.argument("text", StringArgumentType.greedyString())
								.executes(c -> edit(c, npc -> {
									CustomNpcs.setLine(npc, StringArgumentType.getString(c, "text"));
									return ok(c.getSource(), label(npc) + " will say: " + String.join(" / ", npc.custom().lines()));
								}))))
				.then(Commands.literal("quiet").executes(c -> edit(c, npc -> {
					CustomNpcs.setLine(npc, "");
					return ok(c.getSource(), label(npc) + " is back to the town's lines.");
				})))
				.then(Commands.literal("wander").then(Commands.argument("blocks", IntegerArgumentType.integer(0, CustomNpcs.MAX_WANDER))
						.executes(c -> edit(c, npc -> {
							CustomNpcs.setWander(npc, IntegerArgumentType.getInteger(c, "blocks"));
							return ok(c.getSource(), label(npc) + " wanders up to " + npc.custom().wander() + " blocks from its post"
									+ (npc.custom().guard() ? " (a guard with no patrol keeps within " + CustomNpcs.GUARD_POST_RANGE + ")." : "."));
						}))))
				.then(Commands.literal("post")
						.executes(c -> edit(c, npc -> post(c.getSource(), npc, BlockPos.containing(c.getSource().getPosition()))))
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(c -> edit(c, npc -> post(c.getSource(), npc, BlockPosArgument.getLoadedBlockPos(c, "pos"))))))
				.then(Commands.literal("patrol")
						.then(Commands.literal("add")
								.executes(c -> edit(c, npc -> waypoint(c.getSource(), npc, BlockPos.containing(c.getSource().getPosition()))))
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(c -> edit(c, npc -> waypoint(c.getSource(), npc, BlockPosArgument.getLoadedBlockPos(c, "pos"))))))
						.then(Commands.literal("list").executes(c -> edit(c, npc -> patrol(c.getSource(), npc))))
						.then(Commands.literal("remove")
								.executes(c -> edit(c, npc -> unwaypoint(c.getSource(), npc, npc.custom().patrol().size())))
								.then(Commands.argument("n", IntegerArgumentType.integer(1, CustomNpcs.MAX_WAYPOINTS))
										.executes(c -> edit(c, npc -> unwaypoint(c.getSource(), npc, IntegerArgumentType.getInteger(c, "n"))))))
						.then(Commands.literal("clear").executes(c -> edit(c, npc -> {
							CustomNpcs.clearPatrol(npc);
							return ok(c.getSource(), label(npc) + " keeps to its post again.");
						}))));
	}

	// ---------------------------------------------------------------- which NPC

	@FunctionalInterface
	private interface Edit {
		int apply(Townsfolk npc) throws CommandSyntaxException;
	}

	/**
	 * Runs an edit on the NPC the source means (selected, else looked at), checking again, on the server, that the
	 * source is an operator.
	 */
	private static int edit(CommandContext<CommandSourceStack> context, Edit edit) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		if (!operator(source)) {
			return 0;
		}
		Townsfolk npc = target(source);
		if (npc == null) {
			return 0;
		}
		return edit.apply(npc);
	}

	private static boolean operator(CommandSourceStack source) {
		if (CustomNpcs.isOperator(source)) {
			return true;
		}
		source.sendFailure(Component.literal("Only operators can make or edit NPCs."));
		return false;
	}

	/** The selected NPC if it is loaded, else the one looked at; null (and the source told why) when neither. */
	private static @Nullable Townsfolk target(CommandSourceStack source) {
		MinecraftServer server = source.getServer();
		Integer selected = selections(server).get(who(source));
		if (selected != null) {
			Townsfolk npc = CustomNpcs.find(server, selected);
			if (npc != null) {
				return npc;
			}
		}
		Townsfolk looked = lookedAt(source);
		if (looked != null) {
			return looked;
		}
		if (selected != null) {
			CustomNpcs.Registry.Entry entry = CustomNpcs.registry(server).entries().get(selected);
			source.sendFailure(Component.literal(entry == null ? "NPC #" + selected + " no longer exists. Select another with /npc select."
					: "NPC #" + selected + " is not loaded (last seen at " + where(entry) + "). Go there, or select another."));
		} else {
			source.sendFailure(Component.literal("Select an NPC first: look at one and run /npc select, or /npc select <id> (/npc list)."));
		}
		return null;
	}

	/** The custom NPC the source's entity is looking at, within reach and in sight. */
	static @Nullable Townsfolk lookedAt(CommandSourceStack source) {
		Entity viewer = source.getEntity();
		if (viewer == null) {
			return null;
		}
		Vec3 eye = viewer.getEyePosition();
		Vec3 end = eye.add(viewer.getViewVector(1.0F).scale(LOOK_REACH));
		Townsfolk best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Townsfolk npc : viewer.level().getEntitiesOfClass(Townsfolk.class, new AABB(eye, end).inflate(1.0), npc -> npc.custom() != null)) {
			var hit = npc.getBoundingBox().inflate(0.3).clip(eye, end);
			if (hit.isPresent() && viewer instanceof net.minecraft.world.entity.LivingEntity living && living.hasLineOfSight(npc)) {
				double distance = hit.get().distanceToSqr(eye);
				if (distance < bestDistance) {
					bestDistance = distance;
					best = npc;
				}
			}
		}
		return best;
	}

	private static UUID who(CommandSourceStack source) {
		return source.getEntity() == null ? CONSOLE : source.getEntity().getUUID();
	}

	private static Map<UUID, Integer> selections(MinecraftServer server) {
		if (selectedOn != server) {
			// Another world (or a restarted server): numbers from before mean nothing here.
			SELECTED.clear();
			selectedOn = server;
		}
		return SELECTED;
	}

	/** Selects an NPC for {@code who} (an operator talking to it while sneaking, or a command). */
	static void select(MinecraftServer server, UUID who, int id) {
		selections(server).put(who, id);
	}

	private static int selectHere(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		if (!operator(source)) {
			return 0;
		}
		Townsfolk npc = lookedAt(source);
		if (npc == null) {
			double best = NEAREST_REACH * NEAREST_REACH;
			for (Townsfolk near : source.getLevel().getEntitiesOfClass(Townsfolk.class,
					AABB.ofSize(source.getPosition(), 2 * NEAREST_REACH, 2 * NEAREST_REACH, 2 * NEAREST_REACH), each -> each.custom() != null)) {
				double distance = near.position().distanceToSqr(source.getPosition());
				if (distance < best) {
					best = distance;
					npc = near;
				}
			}
		}
		if (npc == null) {
			return fail(source, "No custom NPC in sight or within " + (int) NEAREST_REACH + " blocks. Try /npc list and /npc select <id>.");
		}
		select(source.getServer(), who(source), npc.custom().id());
		return info(source, npc);
	}

	private static int selectNumber(CommandSourceStack source, int id) {
		if (!operator(source)) {
			return 0;
		}
		CustomNpcs.Registry.Entry entry = CustomNpcs.registry(source.getServer()).entries().get(id);
		if (entry == null) {
			return fail(source, "There is no NPC #" + id + ".");
		}
		select(source.getServer(), who(source), id);
		Townsfolk npc = CustomNpcs.find(source.getServer(), id);
		return npc != null ? info(source, npc) : ok(source, "Selected NPC #" + id + " " + entry.name() + " (not loaded; last seen at " + where(entry) + ").");
	}

	/** A loaded NPC by number, for {@code info <id>}; null (and the source told why) otherwise. */
	private static @Nullable Townsfolk numbered(CommandSourceStack source, int id) {
		if (!operator(source)) {
			return null;
		}
		Townsfolk npc = CustomNpcs.find(source.getServer(), id);
		if (npc == null) {
			CustomNpcs.Registry.Entry entry = CustomNpcs.registry(source.getServer()).entries().get(id);
			fail(source, entry == null ? "There is no NPC #" + id + "." : "NPC #" + id + " " + entry.name() + " is not loaded (last seen at " + where(entry) + ").");
		}
		return npc;
	}

	// ---------------------------------------------------------------- the commands

	private static int create(CommandContext<CommandSourceStack> context, String kind) {
		CommandSourceStack source = context.getSource();
		if (!operator(source)) {
			return 0;
		}
		String name = CustomNpcs.clean(StringArgumentType.getString(context, "name"), CustomNpcs.MAX_NAME);
		if (name.isEmpty()) {
			return fail(source, "That name is empty.");
		}
		Entity by = source.getEntity();
		Townsfolk npc = CustomNpcs.create(source.getLevel(), source.getPosition(), source.getRotation().y, name, kind, by == null ? null : by.getUUID());
		if (npc == null) {
			return fail(source, "There is no room for it here (it needs a clear, loaded spot the size of a player).");
		}
		select(source.getServer(), who(source), npc.custom().id());
		return ok(source, label(npc) + " stands here and is selected. Next: /npc skin, /npc say, /npc post, "
				+ (npc.custom().guard() ? "/npc patrol add" : "/npc wander") + ".");
	}

	private static int list(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		if (!operator(source)) {
			return 0;
		}
		Map<Integer, CustomNpcs.Registry.Entry> all = new TreeMap<>(CustomNpcs.registry(source.getServer()).entries());
		if (all.isEmpty()) {
			return ok(source, "There are no custom NPCs. Make one with /npc create <name> or /guard create <name>.");
		}
		Integer selected = selections(source.getServer()).get(who(source));
		int shown = 0;
		for (Map.Entry<Integer, CustomNpcs.Registry.Entry> each : all.entrySet()) {
			if (shown++ >= LIST_LIMIT) {
				int rest = all.size() - LIST_LIMIT;
				source.sendSuccess(() -> Component.literal("... and " + rest + " more."), false);
				break;
			}
			CustomNpcs.Registry.Entry entry = each.getValue();
			boolean loaded = CustomNpcs.find(source.getServer(), each.getKey()) != null;
			String mark = each.getKey().equals(selected) ? " (selected)" : "";
			source.sendSuccess(() -> Component.literal("#" + each.getKey() + " " + entry.kind() + " " + entry.name() + " at " + where(entry)
					+ (loaded ? "" : " (not loaded)") + mark), false);
		}
		return all.size();
	}

	private static int remove(CommandSourceStack source, int id) {
		boolean loaded = CustomNpcs.find(source.getServer(), id) != null;
		if (!CustomNpcs.remove(source.getServer(), id)) {
			return fail(source, "There is no NPC #" + id + ".");
		}
		selections(source.getServer()).values().removeIf(selected -> selected == id);
		return ok(source, "NPC #" + id + (loaded ? " is gone." : " will leave when its chunk next loads."));
	}

	private static int info(CommandSourceStack source, Townsfolk npc) {
		CustomNpcs.Data data = npc.custom();
		if (data == null) {
			return fail(source, "That is not a custom NPC.");
		}
		String skin = data.profile() != null ? data.skinName() + " (player)" : data.townSkin() + " (town)";
		String patrol = data.patrol().isEmpty() ? "none" : data.patrol().size() + " waypoint(s), next " + (data.patrolIndex() + 1);
		source.sendSuccess(() -> Component.literal(label(npc) + " at " + npc.blockPosition().toShortString() + " in " + npc.level().dimension().identifier()), false);
		source.sendSuccess(() -> Component.literal("Post " + npc.home().toShortString() + ", wanders " + CustomNpcs.range(data) + ", skin " + skin
				+ ", patrol " + patrol + ", " + (data.lines().isEmpty() ? "the town's lines" : data.lines().size() + " line(s)")), false);
		return data.id();
	}

	private static int playerSkin(CommandSourceStack source, Townsfolk npc, String name) {
		return switch (NpcSkins.request(source.getServer(), npc, name, source.getPlayer())) {
			case APPLIED -> ok(source, label(npc) + " now wears " + npc.custom().skinName() + "'s skin.");
			case STARTED, JOINED -> ok(source, "Looking up " + name + "'s skin for " + label(npc) + "...");
			case BUSY -> fail(source, "Several skins are being looked up already; try again in a few seconds.");
			case INVALID -> fail(source, NpcSkins.validName(name) ? "No skin was found for " + name + " a moment ago; try again in a minute."
					: name + " is not a Minecraft player name.");
			case NOT_CUSTOM -> fail(source, "That is not a custom NPC.");
		};
	}

	private static int lines(CommandSourceStack source, Townsfolk npc) {
		List<String> lines = npc.custom().lines();
		if (lines.isEmpty()) {
			return ok(source, label(npc) + " says the town's lines for the season. Give it its own with /npc say <text>.");
		}
		for (int i = 0; i < lines.size(); i++) {
			String line = (i + 1) + ": " + lines.get(i);
			source.sendSuccess(() -> Component.literal(line), false);
		}
		return lines.size();
	}

	private static int post(CommandSourceStack source, Townsfolk npc, BlockPos at) {
		if (!CustomNpcs.setPost(npc, source.getLevel(), at)) {
			return fail(source, label(npc) + " is in another dimension.");
		}
		return ok(source, label(npc) + "'s post is now " + at.toShortString() + " (" + npc.custom().patrol().size() + " waypoint(s) still in reach).");
	}

	private static int waypoint(CommandSourceStack source, Townsfolk npc, BlockPos at) {
		String refused = CustomNpcs.addWaypoint(npc, source.getLevel(), at);
		if (refused != null) {
			return fail(source, refused);
		}
		return ok(source, label(npc) + ": waypoint " + npc.custom().patrol().size() + " at " + at.toShortString() + ".");
	}

	private static int unwaypoint(CommandSourceStack source, Townsfolk npc, int n) {
		return CustomNpcs.removeWaypoint(npc, n - 1) ? ok(source, label(npc) + ": removed waypoint " + n + ".")
				: fail(source, label(npc) + " has no waypoint " + n + ".");
	}

	private static int patrol(CommandSourceStack source, Townsfolk npc) {
		List<BlockPos> patrol = npc.custom().patrol();
		if (patrol.isEmpty()) {
			return ok(source, label(npc) + " has no patrol; it keeps to its post at " + npc.home().toShortString() + ".");
		}
		int next = npc.custom().patrolIndex();
		for (int i = 0; i < patrol.size(); i++) {
			String line = (i + 1) + ": " + patrol.get(i).toShortString() + (i == next ? " (next)" : "");
			source.sendSuccess(() -> Component.literal(line), false);
		}
		return patrol.size();
	}

	private static String label(Townsfolk npc) {
		CustomNpcs.Data data = npc.custom();
		return (data != null && data.guard() ? "Guard #" : "NPC #") + (data == null ? "?" : data.id()) + " " + npc.getName().getString();
	}

	private static String where(CustomNpcs.Registry.Entry entry) {
		return entry.pos().toShortString() + " in " + entry.dimension();
	}

	private static int ok(CommandSourceStack source, String message) {
		source.sendSuccess(() -> Component.literal(message), true);
		return 1;
	}

	private static int fail(CommandSourceStack source, String message) {
		source.sendFailure(Component.literal(message));
		return 0;
	}
}
