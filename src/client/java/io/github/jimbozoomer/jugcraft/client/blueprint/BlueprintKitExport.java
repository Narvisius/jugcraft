package io.github.jimbozoomer.jugcraft.client.blueprint;

import io.github.jimbozoomer.jugcraft.blueprint.BlueprintKit;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;

/**
 * Writes the blueprint design kit to the player's own computer ({@code .minecraft/jugcraft/blueprint_kit/}): the AI
 * guide, the allowed blocks and tags of this game (its mods included) and an example to import. Client side only.
 */
public final class BlueprintKitExport {
	private BlueprintKitExport() {
	}

	static Path folder() {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("jugcraft").resolve("blueprint_kit");
	}

	/** Writes the kit and returns its folder. */
	public static Path write() throws IOException {
		Path folder = folder();
		Files.createDirectories(folder);
		Files.writeString(folder.resolve("ai_guide.md"), BlueprintKit.resource(BlueprintKit.GUIDE), StandardCharsets.UTF_8);
		Files.writeString(folder.resolve("example.jugbp.json"), BlueprintKit.resource(BlueprintKit.EXAMPLE), StandardCharsets.UTF_8);
		Files.writeString(folder.resolve("blocks.txt"), BlueprintKit.blocks(), StandardCharsets.UTF_8);
		Files.writeString(folder.resolve("tags.txt"), BlueprintKit.tags(), StandardCharsets.UTF_8);
		return folder;
	}
}
