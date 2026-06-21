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
uniform float u_time;

// --- Procedural clouds ---

float hash(vec2 p)
{
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p)
{
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f); // smoothstep
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p)
{
    float v = 0.0;
    float a = 0.5;
    mat2 rot = mat2(0.8, 0.6, -0.6, 0.8);
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = rot * p * 2.0;
        a *= 0.5;
    }
    return v;
}

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

    // --- Clouds (only above horizon) ---
    if (dir.y > 0.01) {
        // Project direction onto cloud plane
        float cloudHeight = 5.0;
        vec2 baseUv = dir.xz / (dir.y + 0.15) * cloudHeight;

        // Wind direction (consistent, slight angle)
        vec2 wind = normalize(vec2(0.7, 0.3));

        // Layer 1: large slow clouds (high altitude)
        vec2 uv1 = baseUv * 0.25 + wind * u_time * 0.05;
        float n1 = fbm(uv1);
        float cloud1 = smoothstep(0.40, 0.68, n1);

        // Layer 2: smaller faster wisps (lower altitude, parallax)
        vec2 uv2 = baseUv * 0.6 + wind * u_time * 0.25;
        float n2 = fbm(uv2 + vec2(5.3, 2.7));
        float cloud2 = smoothstep(0.48, 0.72, n2);

        // Combine layers: large clouds dominate, wisps add detail
        float density = max(cloud1, cloud2 * 0.6);

        // Fade near horizon
        float horizonFade = smoothstep(0.01, 0.2, dir.y);
        density *= horizonFade;

        // --- Volumetric cloud shading ---
        // Sun direction relative to cloud point
        float sunDot = max(dot(dir, sdir), 0.0);
        float sunHeight = max(sdir.y, 0.1);

        // Internal density gradient: thicker center → darker base, brighter top
        float innerDensity = smoothstep(0.35, 0.7, n1);
        float topLight = 1.0 - innerDensity * 0.4;

        // Sun illumination: bright on sun-facing side
        float sunIllumination = pow(sunDot, 1.5) * sunHeight;

        // Cloud base color (dark grey-blue underside)
        vec3 cloudBase = vec3(0.45, 0.48, 0.55);
        // Cloud top color (bright white, warm from sun)
        vec3 cloudTop = mix(vec3(0.92, 0.93, 0.96), sunColor, sunHeight * 0.25);

        // Sub-surface scattering: light bleeds through thin edges
        float sss = pow(sunDot, 4.0) * (1.0 - innerDensity) * 0.35;
        vec3 sssColor = sunColor * sss;

        // Silver lining at cloud edges near sun
        float edge = smoothstep(0.0, 0.3, density) * (1.0 - smoothstep(0.3, 0.8, density));
        float lining = edge * pow(sunDot, 2.0) * 0.5;

        // Final cloud color: gradient from base (dark) to top (bright) + sun effects
        vec3 cloudColor = mix(cloudBase, cloudTop, topLight + sunIllumination * 0.4);
        cloudColor += sssColor;
        cloudColor += sunColor * lining;

        linear = mix(linear, cloudColor, density * 0.92);
    }

    FragColor = vec4(tonemapDisplay(linear, exposure), 1.0);
}
