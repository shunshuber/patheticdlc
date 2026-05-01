package com.pathdlc.digger.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;

/**
 * GLSL-based rounded rectangle renderer.
 * Draws perfectly smooth rounded rectangles without pixelated corners.
 */
public final class RoundedRectRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("PathDLC-RoundRect");

    private static int program;
    private static int vao, vbo;
    private static boolean initialized;
    private static boolean failed;

    private static int uScreenSize, uRectPos, uRectSize, uRadius, uColor;
    private static int uRadiusTL, uRadiusTR, uRadiusBL, uRadiusBR;

    private static final String VERT_SRC = """
            #version 150
            in vec2 aPos;
            void main() {
                gl_Position = vec4(aPos * 2.0 - 1.0, 0.0, 1.0);
            }
            """;

    private static final String FRAG_SRC = """
            #version 150
            uniform vec2 uScreenSize;
            uniform vec2 uRectPos;
            uniform vec2 uRectSize;
            uniform float uRadiusTL;
            uniform float uRadiusTR;
            uniform float uRadiusBL;
            uniform float uRadiusBR;
            uniform vec4 uColor;
            out vec4 fragColor;

            float roundedBoxSDF(vec2 p, vec2 b, float tl, float tr, float bl, float br) {
                float rx = (p.x > 0.0) ? ((p.y > 0.0) ? tr : br) : ((p.y > 0.0) ? tl : bl);
                vec2 q = abs(p) - b + rx;
                return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rx;
            }

            void main() {
                vec2 fragCoord = gl_FragCoord.xy;
                vec2 center = uRectPos + uRectSize * 0.5;
                vec2 halfSize = uRectSize * 0.5;
                vec2 p = fragCoord - center;

                float dist = roundedBoxSDF(p, halfSize, uRadiusTL, uRadiusTR, uRadiusBL, uRadiusBR);

                float aa = 1.0;
                float alpha = 1.0 - smoothstep(-aa, aa, dist);

                fragColor = uColor * alpha;
            }
            """;

    private RoundedRectRenderer() {}

    private static void init() {
        if (initialized || failed) return;

        try {
            int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            GL20.glShaderSource(vert, VERT_SRC);
            GL20.glCompileShader(vert);
            if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == 0) {
                LOGGER.error("RoundRect vert: {}", GL20.glGetShaderInfoLog(vert));
                failed = true;
                return;
            }

            int frag = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            GL20.glShaderSource(frag, FRAG_SRC);
            GL20.glCompileShader(frag);
            if (GL20.glGetShaderi(frag, GL20.GL_COMPILE_STATUS) == 0) {
                LOGGER.error("RoundRect frag: {}", GL20.glGetShaderInfoLog(frag));
                failed = true;
                return;
            }

            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vert);
            GL20.glAttachShader(program, frag);
            GL20.glLinkProgram(program);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
                LOGGER.error("RoundRect link: {}", GL20.glGetProgramInfoLog(program));
                failed = true;
                return;
            }

            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);

            uScreenSize = GL20.glGetUniformLocation(program, "uScreenSize");
            uRectPos = GL20.glGetUniformLocation(program, "uRectPos");
            uRectSize = GL20.glGetUniformLocation(program, "uRectSize");
            uRadiusTL = GL20.glGetUniformLocation(program, "uRadiusTL");
            uRadiusTR = GL20.glGetUniformLocation(program, "uRadiusTR");
            uRadiusBL = GL20.glGetUniformLocation(program, "uRadiusBL");
            uRadiusBR = GL20.glGetUniformLocation(program, "uRadiusBR");
            uColor = GL20.glGetUniformLocation(program, "uColor");

            vao = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vao);

            float[] quad = {
                    0, 0, 1, 0, 1, 1,
                    0, 0, 1, 1, 0, 1
            };
            FloatBuffer buf = BufferUtils.createFloatBuffer(quad.length);
            buf.put(quad).flip();

            vbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0);

            GL30.glBindVertexArray(0);
            initialized = true;
        } catch (Exception e) {
            LOGGER.error("RoundRect init failed", e);
            failed = true;
        }
    }

    public static void draw(DrawContext context, int x, int y, int w, int h,
                             int radius, int argb) {
        draw(context, x, y, w, h, radius, radius, radius, radius, argb);
    }

    public static void draw(DrawContext context, int x, int y, int w, int h,
                             int tl, int tr, int bl, int br, int argb) {
        if (!PerformanceSettings.useRoundedShader()) {
            context.fill(x, y, x + w, y + h, argb);
            return;
        }

        init();

        if (failed || !initialized) {
            context.fill(x, y, x + w, y + h, argb);
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        int fbW = mc.getWindow().getFramebufferWidth();
        int fbH = mc.getWindow().getFramebufferHeight();
        float scale = (float) mc.getWindow().getScaleFactor();

        float fx = x * scale;
        float fy = fbH - (y + h) * scale;
        float fw = w * scale;
        float fh = h * scale;

        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;

        int prevProg = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        boolean prevBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean prevDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        RenderSystem.defaultBlendFunc();

        GL20.glUseProgram(program);
        GL30.glBindVertexArray(vao);

        GL20.glUniform2f(uScreenSize, fbW, fbH);
        GL20.glUniform2f(uRectPos, fx, fy);
        GL20.glUniform2f(uRectSize, fw, fh);
        GL20.glUniform1f(uRadiusTL, tl * scale);
        GL20.glUniform1f(uRadiusTR, tr * scale);
        GL20.glUniform1f(uRadiusBL, bl * scale);
        GL20.glUniform1f(uRadiusBR, br * scale);
        GL20.glUniform4f(uColor, r, g, b, a);

        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);

        GL30.glBindVertexArray(prevVao);
        GL20.glUseProgram(prevProg);
        if (!prevBlend) GL11.glDisable(GL11.GL_BLEND);
        if (prevDepth) GL11.glEnable(GL11.GL_DEPTH_TEST);
    }
}
