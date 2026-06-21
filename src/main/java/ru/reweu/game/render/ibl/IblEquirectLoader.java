package ru.reweu.game.render.ibl;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL30.GL_RGB16F;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.stb.STBImage.stbi_failure_reason;
import static org.lwjgl.stb.STBImage.stbi_loadf;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memFree;

import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import ru.reweu.game.render.RenderErrorLog;

/**
 * Equirectangular HDR/RGBF текстура: загрузка через STB или процедурный градиент + диск солнца.
 */
public final class IblEquirectLoader {

    private IblEquirectLoader() {
    }

    /**
     * @param hdrPath путь к .hdr файлу или {@code null} — процедурная карта
     * @return id GL_TEXTURE_2D RGB16F, linear
     */
    public static int createEquirectTexture(Path hdrPath) {
        if (hdrPath != null && Files.exists(hdrPath)) {
            try {
                int id = loadHdrFromPath(hdrPath.toString());
                if (id != 0) {
                    return id;
                }
            } catch (Exception e) {
                RenderErrorLog.warn("HDR equirect load failed, using procedural sky: " + hdrPath, e);
            }
        }
        return createProceduralEquirect(
            512, 256,
            new Vector3f(0.45f, 0.55f, 0.35f),
            new Vector3f(1.0f, 0.95f, 0.85f),
            new Vector3f(0.55f, 0.65f, 0.85f),
            new Vector3f(0.25f, 0.22f, 0.18f)
        );
    }

    private static int loadHdrFromPath(String absolutePath) {
        try (var stack = stackPush()) {
            var wx = stack.mallocInt(1);
            var hy = stack.mallocInt(1);
            var ch = stack.mallocInt(1);
            FloatBuffer data = stbi_loadf(absolutePath, wx, hy, ch, 3);
            if (data == null) {
                RenderErrorLog.warn("stbi_loadf failed for " + absolutePath + ": " + stbi_failure_reason());
                return 0;
            }
            int w = wx.get(0);
            int h = hy.get(0);
            int id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F, w, h, 0, GL_RGB, GL_FLOAT, data);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glBindTexture(GL_TEXTURE_2D, 0);
            memFree(data);
            return id;
        }
    }

    /**
     * Градиент небо/земля + мягкое «солнце».
     *
     * @param sunDirection направление к солнцу (нормализованное)
     * @param sunColor     цвет солнца (RGB linear)
     * @param skyColor     цвет неба (RGB linear)
     * @param groundColor  цвет земли (RGB linear)
     */
    public static int createProceduralEquirect(
        int width, int height,
        Vector3f sunDirection, Vector3f sunColor,
        Vector3f skyColor, Vector3f groundColor
    ) {
        Vector3f sunDir = sunDirection;
        Vector3f sunCol = sunColor;
        float skyR = skyColor.x;
        float skyG = skyColor.y;
        float skyB = skyColor.z;
        float grR = groundColor.x;
        float grG = groundColor.y;
        float grB = groundColor.z;

        FloatBuffer buf = MemoryUtil.memAllocFloat(width * height * 3);
        try {
            for (int y = 0; y < height; y++) {
                float v = (y + 0.5f) / height;
                float phi = (float) (v * Math.PI);
                float sinPhi = (float) Math.sin(phi);
                float cosPhi = (float) Math.cos(phi);
                for (int x = 0; x < width; x++) {
                    float u = (x + 0.5f) / width;
                    float theta = (float) (u * Math.PI * 2.0);
                    float sinTheta = (float) Math.sin(theta);
                    float cosTheta = (float) Math.cos(theta);
                    float dx = sinPhi * cosTheta;
                    float dy = cosPhi;
                    float dz = sinPhi * sinTheta;
                    float hemi = dy * 0.5f + 0.5f;
                    float r = grR + (skyR - grR) * hemi;
                    float g = grG + (skyG - grG) * hemi;
                    float b = grB + (skyB - grB) * hemi;
                    r *= 1.2f;
                    g *= 1.2f;
                    b *= 1.2f;
                    float nd = dx * sunDir.x + dy * sunDir.y + dz * sunDir.z;
                    float sun = (float) Math.pow(Math.max(nd, 0f), 52f) * 1.4f;
                    r += sunCol.x * sun;
                    g += sunCol.y * sun;
                    b += sunCol.z * sun;
                    buf.put(r).put(g).put(b);
                }
            }
            buf.flip();
            int id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F, width, height, 0, GL_RGB, GL_FLOAT, buf);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glBindTexture(GL_TEXTURE_2D, 0);
            return id;
        } finally {
            MemoryUtil.memFree(buf);
        }
    }

    public static void deleteTexture(int textureId) {
        if (textureId != 0) {
            glDeleteTextures(textureId);
        }
    }
}
