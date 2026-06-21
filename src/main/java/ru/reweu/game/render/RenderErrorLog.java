package ru.reweu.game.render;

import static org.lwjgl.opengl.GL11.GL_INVALID_ENUM;
import static org.lwjgl.opengl.GL11.GL_INVALID_OPERATION;
import static org.lwjgl.opengl.GL11.GL_INVALID_VALUE;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.GL_OUT_OF_MEMORY;
import static org.lwjgl.opengl.GL11.GL_STACK_OVERFLOW;
import static org.lwjgl.opengl.GL11.GL_STACK_UNDERFLOW;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL30.GL_INVALID_FRAMEBUFFER_OPERATION;

/**
 * Ошибки рендеринга (OpenGL, шейдеры, FBO) — stderr с префиксом {@code [Render]}.
 */
public final class RenderErrorLog {

    private static final String PREFIX = "[Render] ";

    private RenderErrorLog() {
    }

    public static void warn(String message) {
        System.err.println(PREFIX + message);
    }

    public static void warn(String message, Throwable cause) {
        System.err.println(PREFIX + message);
        cause.printStackTrace(System.err);
    }

    /**
     * Сбрасывает очередь {@link org.lwjgl.opengl.GL11#glGetError()} и логирует каждый код (может быть несколько).
     */
    public static void checkGl(String context) {
        int e;
        while ((e = glGetError()) != GL_NO_ERROR) {
            warn(context + ": OpenGL " + nameForError(e) + " (0x" + Integer.toHexString(e) + ")");
        }
    }

    private static String nameForError(int code) {
        return switch (code) {
            case GL_INVALID_ENUM -> "GL_INVALID_ENUM";
            case GL_INVALID_VALUE -> "GL_INVALID_VALUE";
            case GL_INVALID_OPERATION -> "GL_INVALID_OPERATION";
            case GL_STACK_OVERFLOW -> "GL_STACK_OVERFLOW";
            case GL_STACK_UNDERFLOW -> "GL_STACK_UNDERFLOW";
            case GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY";
            case GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION";
            default -> "UNKNOWN(0x" + Integer.toHexString(code) + ")";
        };
    }

    /**
     * Логирует статус FBO, если кадровый буфер неполный; возвращает {@code true}, если OK.
     */
    public static boolean logIfFramebufferIncomplete(String context, int target) {
        int status = org.lwjgl.opengl.GL30.glCheckFramebufferStatus(target);
        if (status != org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE) {
            warn(context + ": framebuffer status " + framebufferStatusName(status)
                + " (0x" + Integer.toHexString(status) + ")");
            return false;
        }
        return true;
    }

    private static String framebufferStatusName(int status) {
        return switch (status) {
            case org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE -> "GL_FRAMEBUFFER_COMPLETE";
            case org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_UNDEFINED -> "GL_FRAMEBUFFER_UNDEFINED";
            case org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
            case org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
            case org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER";
            case org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER -> "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER";
            case org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_UNSUPPORTED -> "GL_FRAMEBUFFER_UNSUPPORTED";
            case org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> "GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE";
            default -> "UNKNOWN";
        };
    }
}
