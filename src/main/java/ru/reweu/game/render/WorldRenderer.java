package ru.reweu.game.render;

import org.joml.Matrix4f;
import ru.reweu.game.render.ibl.EnvironmentIbl;

/**
 * Доступ к шейдеру мира и ресурсам кадра; per-frame униформы — {@link LitFrameUniformCache}.
 */
public final class WorldRenderer implements LitFrameServices {

    public static final int BRDF_LUT_UNIT = 6;

    private final ShaderProgram worldShaderProgram;
    private final DirectionalShadowMap shadowMap;
    private final BrdfLutTexture brdfLut;
    private final EnvironmentIbl environmentIbl;
    private final LitFrameUniformCache worldUniforms;
    private LitFrameUniformCache gltfUniforms;

    private float textureScale = 4.5f;
    private float farPlane = 1000f;
    private LitFrameUniformCache.ShadowUniformConfig shadowConfig =
        new LitFrameUniformCache.ShadowUniformConfig(1f, 1.38f, 0.1f, false, false);

    public WorldRenderer(
        ShaderProgram worldShaderProgram,
        DirectionalShadowMap shadowMap,
        BrdfLutTexture brdfLut,
        EnvironmentIbl environmentIbl
    ) {
        this.worldShaderProgram = worldShaderProgram;
        this.shadowMap = shadowMap;
        this.brdfLut = brdfLut;
        this.environmentIbl = environmentIbl;
        this.worldUniforms = LitFrameUniformCache.forWorld(worldShaderProgram);
    }

    /**
     * Устанавливает параметры, специфичные для приложения (передаются из game-shader каждый кадр).
     */
    public void setAppConfig(float textureScale, float farPlane,
                             LitFrameUniformCache.ShadowUniformConfig shadowConfig) {
        this.textureScale = textureScale;
        this.farPlane = farPlane;
        this.shadowConfig = shadowConfig;
    }

    public ShaderProgram getWorldShaderProgram() {
        return worldShaderProgram;
    }

    public DirectionalShadowMap getShadowMap() {
        return shadowMap;
    }

    public EnvironmentIbl getEnvironmentIbl() {
        return environmentIbl;
    }

    public void prepareFrame(
        LightingFrame lit,
        Matrix4f view,
        Matrix4f projection,
        boolean shadowSamplingEnabled
    ) {
        prepareFrameFor(lit, worldShaderProgram, view, projection, shadowSamplingEnabled);
    }

    public void prepareFrameFor(
        LightingFrame lit,
        ShaderProgram shaderProgram,
        Matrix4f view,
        Matrix4f projection,
        boolean shadowSamplingEnabled
    ) {
        LitFrameUniformCache cache = uniformCacheFor(shaderProgram);
        cache.bind(
            lit,
            shaderProgram,
            view,
            projection,
            shadowMap,
            brdfLut,
            environmentIbl,
            shadowSamplingEnabled,
            textureScale,
            farPlane,
            shadowConfig
        );
    }

    private LitFrameUniformCache uniformCacheFor(ShaderProgram shaderProgram) {
        if (shaderProgram == worldShaderProgram) {
            return worldUniforms;
        }
        if (gltfUniforms == null || gltfUniforms.programId() != shaderProgram.getProgramId()) {
            gltfUniforms = LitFrameUniformCache.forGltf(shaderProgram, worldShaderProgram.getProgramId());
        }
        return gltfUniforms;
    }
}
