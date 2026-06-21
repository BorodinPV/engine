# Engine

Rendering engine module for the game project. Built on LWJGL 3 (OpenGL).

## Features

- **Platform** — GLFW window/context management (`GlfwAppContext`)
- **Rendering** — shader programs, scene frame uniforms, lighting frame management
- **PBR** — GGX microfacet BRDF, BRDF LUT generation, IBL (image-based lighting) with equirectangular environment maps
- **Sky** — sky dome/pass rendering
- **Rain** — particle-based rain effect
- **Shadows** — directional shadow maps with cascade splits
- **Ray Tracing** — compute shader programs for RT effects
- **Instancing** — demo instancing renderer

## Shaders

Located in `src/main/resources/shaders/`:

| Shader | Purpose |
|---|---|
| `sky_pass.{vert,frag}` | Sky dome rendering |
| `rain.{vert,frag}` | Rain particle effect |
| `ibl/` | Environment map capture, irradiance convolution, specular prefilter |
| `include/` | Shared GLSL headers — math constants, PBR GGX, spherical harmonics, tone mapping |

## Dependencies

- LWJGL 3.3.4 (GLFW, OpenGL, STB)
- JOML 1.10.7 (math)
- Lombok 1.18.34
