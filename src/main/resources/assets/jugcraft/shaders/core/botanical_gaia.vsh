#version 330
#extension GL_ARB_separate_shader_objects : require
// Owner Jugtania doppleganger displacement, adapted to 26.3 native uniform blocks.
#include <minecraft:light.glsl>
#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:sample_lightmap.glsl>
#include <minecraft:globals.glsl>
layout(location=0) in vec3 Position;
layout(location=1) in vec4 Color;
layout(location=2) in vec2 UV0;
layout(location=3) in ivec2 UV1;
layout(location=4) in ivec2 UV2;
layout(location=5) in vec3 Normal;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
layout(location=0) out float sphericalDistance;
layout(location=1) out float cylindricalDistance;
layout(location=2) out vec4 vertexColor;
layout(location=3) out vec4 lightMapColor;
layout(location=4) out vec4 overlayColor;
layout(location=5) out vec2 texCoord;
float noise(vec2 p){return (fract(sin(dot(p,vec2(12.9898,78.233)))*43758.5453)-0.5)*2.0;}
void main(){
 float seed=noise(vec2(GameTime));
 vec3 displaced=Position+JUGCRAFT_DISFIGURATION*vec3(noise(seed*Position.yz),noise(seed*Position.xz),noise(seed*Position.xy));
 gl_Position=ProjMat*ModelViewMat*vec4(displaced,1.0);
 sphericalDistance=fog_spherical_distance(Position);cylindricalDistance=fog_cylindrical_distance(Position);
 vertexColor=minecraft_mix_light(Light0_Direction,Light1_Direction,Normal,Color);
 lightMapColor=sample_lightmap(Sampler2,UV2);overlayColor=texelFetch(Sampler1,UV1,0);texCoord=UV0;
}
