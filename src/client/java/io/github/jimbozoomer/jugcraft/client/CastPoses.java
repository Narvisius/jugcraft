package io.github.jimbozoomer.jugcraft.client;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.weapons.ArmItem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.spell_engine.client.animation.SpellAnimationStack;

/**
 * One owner for a player's arms (Concordance Chunk A, ticket A-07): a gun or an arm in the main hand owns them, and no
 * cast's pose plays over it (docs/features/arcane-concordance-presentation.md). Client only; nothing here decides
 * anything.
 * <p>
 * A cast and a gun or arm cannot begin together. A Concordance cast needs an instrument in the main hand, and when the
 * main hand's item changes Spell Engine ends the cast (on the client and the server), the server ends a reload
 * (GunShots) and ArmsMotion ends a weapon art. What can outlast the change is the cast's presentation: Spell Engine's
 * release clip (12 to 14 ticks, sent by the server once the cast took effect, so it can arrive after a quick switch)
 * and the five ticks over which it fades out a cast it ended. Player Animation Library lays those over the player model
 * after GunPose and ArmsMotion have posed it. In first person, by Spell Engine's default, it stops the game drawing the
 * first-person hands and held item and draws the player's own model instead, with everything but the held items hidden
 * (its default first-person configuration, which Spell Engine keeps): the gun or arm would be drawn where the clip
 * holds the unseen hand, and the first-person arms that hold the gun and work its reload, its aim, or the arm's stroke
 * would go out of view.
 * <p>
 * So at the end of each client tick, for every player this client sees (its own, in either view, and others):
 * <ul>
 * <li>while a gun or an arm is in the main hand ({@link #armsOwned}), Spell Engine's casting and release clips are
 * stopped if they are playing. Jugcraft's arms are no spell casters (data/jugcraft/spell_assignments opts them out) and
 * Jugcraft gives no gun a spell, so what this stops was left over from the item before. A spell another mod lets a
 * player cast with one in hand would still be cast, without its gesture;</li>
 * <li>the Concordance's own gestures, the circle's channelling and the Vigil's offering, are left off a player whose
 * arms are owned or who holds an arm in the off hand ({@link #gesturesYield}): the circle's by CircleClient's own tick,
 * the Vigil's when it would start (VigilClient) and here, if one is playing when a gun or an arm is taken in hand.</li>
 * </ul>
 * A clip stopped here ends at once rather than fading, so the gun's or the arm's pose takes over in the next frame. A
 * release that reaches this client while a gun or an arm is already in hand is stopped by the end of the next client
 * tick, at most 50 ms later; until then it shows on the body in third person. In first person, if a Spell Engine clip
 * was playing the tick before (as after a quick switch from the wand), until that tick ends the view may show the held
 * gun or arm where the clip holds the unseen hand, with no hands or arms; otherwise the hands are lowered for a moment.
 * (Read in Player Animation Library's source, not seen in a game.)
 */
public final class CastPoses {
	/** Clips and gestures stopped so far (for the client game tests). */
	private static long stopped;

	private CastPoses() {
	}

	static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(CastPoses::tick);
	}

	/** Whether a gun (posed by GunPose) or an arm (posed by ArmsMotion) in the main hand owns this player's arms. */
	public static boolean armsOwned(Player player) {
		Item main = player.getMainHandItem().getItem();
		return main instanceof GunItem || main instanceof ArmItem;
	}

	/** Whether the Concordance's own gestures are left off this player: their arms are owned, or an arm is in the off hand. */
	static boolean gesturesYield(Player player) {
		return armsOwned(player) || player.getOffhandItem().getItem() instanceof ArmItem;
	}

	/** Clips and gestures stopped so far (for the client game tests). */
	public static long stopped() {
		return stopped;
	}

	private static void tick(Minecraft client) {
		ClientLevel level = client.level;
		if (level == null) {
			return;
		}
		for (Player player : level.players()) {
			if (armsOwned(player)) {
				stop(player, SpellAnimationStack.CASTING_ID);
				stop(player, SpellAnimationStack.RELEASE_ID);
			}
			if (gesturesYield(player)) {
				stop(player, VigilClient.LAYER);
			}
		}
	}

	/**
	 * Ends the clip playing on one of the player's animation layers, if one is. Clearing the triggered clip keeps it from
	 * starting again; stopping the layer as well takes it out of play now, rather than when the library next updates it,
	 * so not even one frame keeps the hands and the held item out of the first-person view.
	 */
	private static void stop(Player player, Identifier layer) {
		if (PlayerAnimationAccess.getPlayerAnimationLayer(player, layer) instanceof PlayerAnimationController controller
				&& controller.isPlayingTriggeredAnimation()) {
			controller.stopTriggeredAnimation();
			controller.stop();
			stopped++;
		}
	}
}
