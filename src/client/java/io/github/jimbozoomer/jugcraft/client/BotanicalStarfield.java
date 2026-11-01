package io.github.jimbozoomer.jugcraft.client;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
/** Original owner shader keeps its hue cycle, with current native portal vertex layout and fog. */
final class BotanicalStarfield {
    private static final com.mojang.renderpearl.api.pipeline.RenderPipeline PIPELINE=RenderPipelines.register(com.mojang.renderpearl.api.pipeline.RenderPipeline.builder(RenderPipelines.END_PORTAL_SNIPPET)
        .withLocation(Jugcraft.id("pipeline/botanical_starfield")).withFragmentShader(Jugcraft.id("core/botanical_starfield")).withShaderDefine("PORTAL_LAYERS",15)
        .withColorTargetState(com.mojang.renderpearl.api.pipeline.ColorTargetState.DEFAULT).withDepthStencilState(RenderPipelines.END_PORTAL.getDepthStencilState()).build());
    static final RenderType TYPE=RenderType.create("jugcraft_botanical_starfield",RenderSetup.builder(PIPELINE)
        .withTexture("Sampler0",Identifier.withDefaultNamespace("textures/environment/end_sky.png"))
        .withTexture("Sampler1",Identifier.withDefaultNamespace("textures/entity/end_portal.png")).createRenderSetup());
}
