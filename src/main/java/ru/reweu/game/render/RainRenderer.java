package ru.reweu.game.render;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.glBlendFunc;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.glVertexAttribPointer;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;

import java.nio.FloatBuffer;
import java.util.Random;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Дождь: тонкие квады (billboard streaks) вокруг камеры, обновление на CPU.
 */
public final class RainRenderer {

    private static final int DROP_COUNT = 2800;
    private static final float SPAWN_RADIUS = 30f;
    private static final float FALL_SPEED_MIN = 20f;
    private static final float FALL_SPEED_MAX = 30f;
    // 6 vertices per quad (2 triangles), 4 floats per vertex (xyz + alpha)
    private static final int VERTS_PER_DROP = 6;
    private static final int FLOATS_PER_DROP = VERTS_PER_DROP * 4;

    private final int vao;
    private final int vbo;
    private final ShaderProgram shader;

    // CPU particle state: [x, y, z, vy] per drop
    private final float[] state = new float[DROP_COUNT * 4];
    private final Random rng = new Random(42);

    public RainRenderer() {
        shader = new ShaderProgram("/shaders/rain.vert", "/shaders/rain.frag");

        for (int i = 0; i < DROP_COUNT; i++) {
            resetDrop(i, true, 0f);
        }

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, DROP_COUNT * FLOATS_PER_DROP * Float.BYTES, GL_DYNAMIC_DRAW);
        // location 0: aPos (vec3), stride = 4 floats
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 4 * Float.BYTES, 0);
        // location 1: aAlpha (float), stride = 4 floats, offset = 3 floats
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 1, GL_FLOAT, false, 4 * Float.BYTES, 3 * Float.BYTES);
        glBindVertexArray(0);
    }

    private void resetDrop(int i, boolean randomY, float cameraY) {
        int idx = i * 4;
        float angle = rng.nextFloat() * (float) Math.PI * 2f;
        float radius = rng.nextFloat() * SPAWN_RADIUS;
        state[idx]     = (float) Math.cos(angle) * radius;
        if (randomY) {
            state[idx + 1] = cameraY - 5f + rng.nextFloat() * 35f;
        } else {
            state[idx + 1] = cameraY + 22f + rng.nextFloat() * 13f;
        }
        state[idx + 2] = (float) Math.sin(angle) * radius;
        state[idx + 3] = -(FALL_SPEED_MIN + rng.nextFloat() * (FALL_SPEED_MAX - FALL_SPEED_MIN));
    }

    public void update(float deltaTime, Vector3f cameraPos) {
        float cx = cameraPos.x;
        float cy = cameraPos.y;
        float cz = cameraPos.z;

        for (int i = 0; i < DROP_COUNT; i++) {
            int idx = i * 4;
            state[idx + 1] += state[idx + 3] * deltaTime;
            if (state[idx + 1] < cy - 5f) {
                resetDrop(i, false, cy);
            }
        }

        // Upload quad vertices: 6 verts per drop (2 triangles)
        // Triangle order: TL, TR, BL, BL, TR, BR
        FloatBuffer buf = memAllocFloat(DROP_COUNT * FLOATS_PER_DROP);
        for (int i = 0; i < DROP_COUNT; i++) {
            int idx = i * 4;
            float x = state[idx] + cx;
            float y = state[idx + 1];
            float z = state[idx + 2] + cz;

            float dx = x - cx;
            float dy = y - cy;
            float dz = z - cz;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            float alpha = 1.0f - Math.min(dist / SPAWN_RADIUS, 1.0f);
            alpha = alpha * alpha;

            // 6 vertices: TL, TR, BL, BL, TR, BR (all share same base pos)
            for (int v = 0; v < 6; v++) {
                buf.put(x).put(y).put(z).put(alpha);
            }
        }
        buf.flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        org.lwjgl.opengl.GL15.glBufferSubData(GL_ARRAY_BUFFER, 0, buf);
        memFree(buf);
    }

    public void render(Matrix4f view, Matrix4f projection) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shader.use();
        shader.setUniform("view", view);
        shader.setUniform("projection", projection);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, DROP_COUNT * 6);
        glBindVertexArray(0);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    public void cleanup() {
        shader.cleanup();
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
    }
}
