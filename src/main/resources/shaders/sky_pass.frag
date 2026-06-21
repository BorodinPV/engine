#version 330 core

#include "/shaders/include/tonemap_exposure.glsl"

in vec2 vNdc;

out vec4 FragColor;

uniform mat4 invProjection;
uniform mat4 invView;

uniform vec3 sunDirection;
uniform vec3 sunColor;
uniform float sunIntensity;
uniform vec3 skyAmbientColor;
uniform vec3 groundAmbientColor;
uniform float exposure;
uniform float sunDiscScale;

void main()
{
    vec4 clip = vec4(vNdc, 1.0, 1.0);
    vec4 viewH = invProjection * clip;
    vec3 eyeDir = normalize(viewH.xyz / viewH.w);
    vec4 worldH = invView * vec4(eyeDir, 0.0);
    vec3 dir = normalize(worldH.xyz);

    // Hemisphere gradient: sky above, ground below
    float hemi = dir.y * 0.5 + 0.5;
    float gradT = pow(clamp(hemi, 0.0, 1.0), 0.88);
    vec3 sky = mix(groundAmbientColor, skyAmbientColor, gradT);

    // Sun disc
    vec3 sdir = normalize(sunDirection);
    float nd = max(dot(dir, sdir), 0.0);
    float sunCore = pow(nd, 220.0);
    float sunOn = step(1e-4, sunIntensity);
    vec3 disc = sunColor * sunDiscScale * sunCore * 0.28 * sunOn;

    vec3 linear = (sky + disc) * 1.1;
    FragColor = vec4(tonemapDisplay(linear, exposure), 1.0);
}
