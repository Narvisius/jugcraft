#version 330
#extension GL_ARB_separate_shader_objects : require
// Owner Jugtania grayscale/grain formula; native fog and skin/overlay sampling.
#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
uniform sampler2D Sampler0;
layout(location=0) in float sphericalDistance;
layout(location=1) in float cylindricalDistance;
layout(location=2) in vec4 vertexColor;
layout(location=3) in vec4 lightMapColor;
layout(location=4) in vec4 overlayColor;
layout(location=5) in vec2 texCoord;
layout(location=0) out vec4 fragColor;
void main(){
 vec4 color=texture(Sampler0,texCoord);if(color.a<0.1)discard;
 color*=vertexColor*ColorModulator;color.rgb=mix(overlayColor.rgb,color.rgb,overlayColor.a);color*=lightMapColor;
 float grain=fract(sin(dot(texCoord,vec2(12.9898,78.233)))*43758.5453);
 color.rgb=vec3((color.r+color.g+color.b)/50.0+JUGCRAFT_GRAIN*grain);
 fragColor=apply_fog(color,sphericalDistance,cylindricalDistance,FogEnvironmentalStart,FogEnvironmentalEnd,FogRenderDistanceStart,FogRenderDistanceEnd,FogColor);
}
