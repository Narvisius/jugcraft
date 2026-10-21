package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.academy.AcademyClimatePayload;
import io.github.jimbozoomer.jugcraft.academy.AcademySite;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;

/** Camera-local snow and existing mist particles. Never rewrites weather, biome, sky or terrain. */
public final class AcademyAtmosphere {
	private static final List<Particle> OWN = new ArrayList<>();
	private static List<AcademySite> sites = List.of();
	private static ClientLevel world;
	private static String mode = "normal";
	private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("jugcraft-academy-client.properties");
	private AcademyAtmosphere() {}
	public static void register() {
		if (Files.isRegularFile(CONFIG)) {
			try (var input = Files.newInputStream(CONFIG)) {
				Properties properties = new Properties(); properties.load(input);
				String saved = properties.getProperty("effects", "normal");
				if (List.of("off", "low", "normal").contains(saved)) mode = saved;
			} catch (java.io.IOException e) { Jugcraft.LOGGER.warn("Could not read academy client options", e); }
		}
		ClientPlayNetworking.registerGlobalReceiver(AcademyClimatePayload.TYPE, (payload, context) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.level != null && client.level.dimension().identifier().toString().equals(payload.dimension())) {
				if (world != client.level) clear();
				world = client.level; sites = payload.sites();
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> clear());
		ClientTickEvents.END_CLIENT_TICK.register(AcademyAtmosphere::tick);
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
			var root = ClientCommands.literal("academyfx");
			for (String option : List.of("off", "low", "normal")) {
				root.then(ClientCommands.literal(option).executes(context -> {
					mode = option; removeParticles();
					try { Files.writeString(CONFIG, "effects="+mode+"\n"); }
					catch (java.io.IOException e) { Jugcraft.LOGGER.warn("Could not save academy client options", e); }
					context.getSource().sendFeedback(Component.translatable("academy.jugcraft.effects", mode));
					return 1;
				}));
			}
			dispatcher.register(root);
		});
	}
	private static void clear() { removeParticles(); sites = List.of(); world = null; }
	private static void removeParticles() { OWN.forEach(Particle::remove); OWN.clear(); }
	private static double influence(double x, double y, double z) {
		double value = 0;
		for (AcademySite site : sites) value = Math.max(value, site.influence(x, y, z));
		return value; // overlapping colleges never multiply the budget
	}
	private static void tick(Minecraft client) {
		if (client.level != world) { clear(); return; }
		if (client.level == null || client.player == null || client.isPaused()) return;
		OWN.removeIf(p -> !p.isAlive());
		var camera = client.gameRenderer.mainCamera().position();
		double strength = influence(camera.x, camera.y, camera.z);
		ParticleStatus setting = client.options.particles().get();
		if (mode.equals("off") || setting == ParticleStatus.MINIMAL || strength == 0
				|| !world.canSeeSky(BlockPos.containing(camera))) { removeParticles(); return; }
		boolean low = mode.equals("low") || setting == ParticleStatus.DECREASED;
		int budget = low ? 2 : 6;
		for (int i = 0; i < budget && OWN.size() < 512; i++) {
			var random = world.getRandom();
			double x = camera.x + (random.nextDouble()-0.5)*24;
			double z = camera.z + (random.nextDouble()-0.5)*24;
			double y = camera.y + 2 + random.nextDouble()*8;
			if (random.nextDouble() > strength * influence(x, y, z) || !world.canSeeSky(BlockPos.containing(x,y,z))) continue;
			spawn(client, ParticleTypes.SNOWFLAKE, x, y, z, 0.015, -0.06, 0.005);
		}
		if (OWN.size() < 512 && world.getGameTime() % (low ? 12 : 5) == 0) {
			double x = camera.x+(world.getRandom().nextDouble()-0.5)*12, z = camera.z+(world.getRandom().nextDouble()-0.5)*12;
			double y = camera.y-1;
			if (world.getRandom().nextDouble() < strength*influence(x,y,z) && world.canSeeSky(BlockPos.containing(x,y,z))
					&& world.getBlockState(BlockPos.containing(x,y,z)).isAir()) {
				spawn(client, JugcraftAgriculture.FOG, x, y, z, 0.01, 0, 0);
			}
		}
	}
	private static void spawn(Minecraft client, ParticleOptions type, double x, double y, double z, double dx, double dy, double dz) {
		Particle particle = client.particleEngine.createParticle(type, x, y, z, dx, dy, dz);
		if (particle != null) OWN.add(particle);
	}
}
