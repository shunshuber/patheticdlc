package com.pathdlc.digger.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class LiquidGlassRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("PathDLC-Glass");

    private static int blurProgram;
    private static int glassProgram;
    private static int fboA, texA;
    private static int fboB, texB;
    private static int quadVao, quadVbo;
    private static int lastWidth, lastHeight;
    private static boolean initialized;
    private static boolean shadersFailed;

    private static final int BLUR_ITERATIONS = 3;
    private static final float BLUR_SPREAD = 2.5f;

    private static int uBlurSampler, uBlurDirection;
    private static int uGlassSampler, uGlassScreenSize;
    private static int uGlassPanelPos, uGlassPanelSize;
    private static int uGlassRadius, uGlassHover;
    private static int uGlassAccentColor, uGlassAccentMix;

    public static void init() {
        if (initialized || shadersFailed) {
            return;
        }
        LOGGER.info("LiquidGlassRenderer init called");
        try {
            loadShaders();
            createQuad();
            initialized = true;
            LOGGER.info("LiquidGlassRenderer initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Shader load failed: " + e.getMessage(), e);
            shadersFailed = true;
        }
    }

    public static boolean isReady() {
        return initialized && !shadersFailed && blurProgram != 0 && glassProgram != 0;
    }

    private static void loadShaders() {
        LOGGER.info("Compiling blur shader...");
        blurProgram = createProgram(BLUR_VSH, BLUR_FSH);
        uBlurSampler = GL20.glGetUniformLocation(blurProgram, "Sampler0");
        uBlurDirection = GL20.glGetUniformLocation(blurProgram, "Direction");
        LOGGER.info("Blur shader compiled: program={}", blurProgram);

        LOGGER.info("Compiling glass shader...");
        glassProgram = createProgram(GLASS_VSH, GLASS_FSH);
        uGlassSampler = GL20.glGetUniformLocation(glassProgram, "BlurredScene");
        uGlassScreenSize = GL20.glGetUniformLocation(glassProgram, "ScreenSize");
        uGlassPanelPos = GL20.glGetUniformLocation(glassProgram, "PanelPos");
        uGlassPanelSize = GL20.glGetUniformLocation(glassProgram, "PanelSize");
        uGlassRadius = GL20.glGetUniformLocation(glassProgram, "Radius");
        uGlassHover = GL20.glGetUniformLocation(glassProgram, "HoverAmount");
        uGlassAccentColor = GL20.glGetUniformLocation(glassProgram, "AccentColor");
        uGlassAccentMix = GL20.glGetUniformLocation(glassProgram, "AccentMix");
        LOGGER.info("Glass shader compiled: program={}", glassProgram);
    }

    private static int createProgram(String vsh, String fsh) {
        int vs = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vs, vsh);
        GL20.glCompileShader(vs);
        if (GL20.glGetShaderi(vs, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(vs);
            GL20.glDeleteShader(vs);
            throw new RuntimeException("Vertex shader compile error: " + log);
        }

        int fs = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fs, fsh);
        GL20.glCompileShader(fs);
        if (GL20.glGetShaderi(fs, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(fs);
            GL20.glDeleteShader(vs);
            GL20.glDeleteShader(fs);
            throw new RuntimeException("Fragment shader compile error: " + log);
        }

        int prog = GL20.glCreateProgram();
        GL20.glAttachShader(prog, vs);
        GL20.glAttachShader(prog, fs);
        GL20.glBindAttribLocation(prog, 0, "Position");
        GL20.glBindAttribLocation(prog, 1, "UV");
        GL20.glLinkProgram(prog);
        if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) == 0) {
            String log = GL20.glGetProgramInfoLog(prog);
            GL20.glDeleteProgram(prog);
            GL20.glDeleteShader(vs);
            GL20.glDeleteShader(fs);
            throw new RuntimeException("Shader link error: " + log);
        }

        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
        return prog;
    }

    private static void createQuad() {
        float[] vertices = {
                -1f, -1f, 0f, 0f, 0f,
                 1f, -1f, 0f, 1f, 0f,
                 1f,  1f, 0f, 1f, 1f,
                -1f, -1f, 0f, 0f, 0f,
                 1f,  1f, 0f, 1f, 1f,
                -1f,  1f, 0f, 0f, 1f,
        };

        quadVao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(quadVao);

        quadVbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadVbo);
        FloatBuffer buf = BufferUtils.createFloatBuffer(vertices.length);
        buf.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 5 * 4, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 5 * 4, 3 * 4);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    private static void ensureFramebuffers(int width, int height) {
        if (width == lastWidth && height == lastHeight && fboA != 0) {
            return;
        }

        if (fboA != 0) {
            GL30.glDeleteFramebuffers(fboA);
            GL11.glDeleteTextures(texA);
        }
        if (fboB != 0) {
            GL30.glDeleteFramebuffers(fboB);
            GL11.glDeleteTextures(texB);
        }

        lastWidth = width;
        lastHeight = height;

        fboA = GL30.glGenFramebuffers();
        texA = GL11.glGenTextures();
        setupFramebuffer(fboA, texA, width, height);

        fboB = GL30.glGenFramebuffers();
        texB = GL11.glGenTextures();
        setupFramebuffer(fboB, texB, width, height);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    private static void setupFramebuffer(int fbo, int tex, int width, int height) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, tex, 0);
    }

    public static void captureAndBlur() {
        if (!initialized && !shadersFailed) {
            init();
        }
        if (!isReady()) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        int fbWidth = mc.getWindow().getFramebufferWidth();
        int fbHeight = mc.getWindow().getFramebufferHeight();

        ensureFramebuffers(fbWidth, fbHeight);

        int mainFbo = GL30.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fboA);
        GL30.glBlitFramebuffer(0, 0, fbWidth, fbHeight,
                0, 0, fbWidth, fbHeight, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        boolean prevBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean prevDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);

        GL20.glUseProgram(blurProgram);
        GL30.glBindVertexArray(quadVao);
        GL20.glUniform1i(uBlurSampler, 0);

        for (int i = 0; i < BLUR_ITERATIONS; i++) {
            float spread = BLUR_SPREAD * (i + 1);

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboB);
            GL11.glViewport(0, 0, fbWidth, fbHeight);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texA);
            GL20.glUniform2f(uBlurDirection, spread / fbWidth, 0.0f);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboA);
            GL11.glViewport(0, 0, fbWidth, fbHeight);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texB);
            GL20.glUniform2f(uBlurDirection, 0.0f, spread / fbHeight);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        }

        GL30.glBindVertexArray(prevVao);
        GL20.glUseProgram(prevProgram);
        if (prevBlend) {
            GL11.glEnable(GL11.GL_BLEND);
        }
        if (prevDepth) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mainFbo);
        GL11.glViewport(0, 0, fbWidth, fbHeight);
    }

    public static void drawGlassPanel(DrawContext context, float x, float y,
                                       float w, float h, float radius, float hoverAmount) {
        drawGlassPanel(context, x, y, w, h, radius, hoverAmount,
                0.0f, 0.3f, 0.6f, 1.0f);
    }

    public static void drawGlassPanel(DrawContext context, float x, float y,
                                       float w, float h, float radius,
                                       float hoverAmount, float accentMix,
                                       float accentR, float accentG, float accentB) {
        if (!isReady()) {
            drawFallbackPanel(context, (int) x, (int) y, (int) w, (int) h,
                    accentMix > 0);
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        int fbWidth = mc.getWindow().getFramebufferWidth();
        int fbHeight = mc.getWindow().getFramebufferHeight();
        float scale = (float) mc.getWindow().getScaleFactor();

        float fbX = x * scale;
        float fbY = fbHeight - (y + h) * scale;
        float fbW = w * scale;
        float fbH = h * scale;
        float fbRadius = radius * scale;

        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean prevBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean prevDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        RenderSystem.defaultBlendFunc();

        GL20.glUseProgram(glassProgram);
        GL30.glBindVertexArray(quadVao);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texA);
        GL20.glUniform1i(uGlassSampler, 0);

        GL20.glUniform2f(uGlassScreenSize, fbWidth, fbHeight);
        GL20.glUniform2f(uGlassPanelPos, fbX, fbY);
        GL20.glUniform2f(uGlassPanelSize, fbW, fbH);
        GL20.glUniform1f(uGlassRadius, fbRadius);
        GL20.glUniform1f(uGlassHover, hoverAmount);
        GL20.glUniform3f(uGlassAccentColor, accentR, accentG, accentB);
        GL20.glUniform1f(uGlassAccentMix, accentMix);

        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);

        GL30.glBindVertexArray(prevVao);
        GL20.glUseProgram(prevProgram);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
        if (!prevBlend) {
            GL11.glDisable(GL11.GL_BLEND);
        }
        if (prevDepth) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
    }

    public static void drawFallbackPanel(DrawContext context, int x, int y,
                                          int w, int h, boolean active) {
        int bg = active ? 0xAA1A2E4E : 0xAA1A1A2E;
        context.fill(x, y, x + w, y + h, bg);
        context.fill(x, y, x + w, y + 1, 0x55FFFFFF);
        context.fill(x, y, x + 1, y + h, 0x33FFFFFF);
        context.fill(x + w - 1, y, x + w, y + h, 0x22FFFFFF);
        context.fill(x, y + h - 1, x + w, y + h, 0x11FFFFFF);
    }

    public static void cleanup() {
        if (blurProgram != 0) {
            GL20.glDeleteProgram(blurProgram);
        }
        if (glassProgram != 0) {
            GL20.glDeleteProgram(glassProgram);
        }
        if (fboA != 0) {
            GL30.glDeleteFramebuffers(fboA);
            GL11.glDeleteTextures(texA);
        }
        if (fboB != 0) {
            GL30.glDeleteFramebuffers(fboB);
            GL11.glDeleteTextures(texB);
        }
        if (quadVao != 0) {
            GL30.glDeleteVertexArrays(quadVao);
        }
        if (quadVbo != 0) {
            GL15.glDeleteBuffers(quadVbo);
        }
        blurProgram = 0;
        glassProgram = 0;
        fboA = 0;
        fboB = 0;
        texA = 0;
        texB = 0;
        quadVao = 0;
        quadVbo = 0;
        initialized = false;
        shadersFailed = false;
    }

    // ──────────────────────────────────────
    //  Embedded GLSL shader sources
    // ──────────────────────────────────────

    private static final String BLUR_VSH = """
            #version 150
            in vec3 Position;
            in vec2 UV;
            out vec2 texCoord;
            void main() {
                gl_Position = vec4(Position, 1.0);
                texCoord = UV;
            }
            """;

    private static final String BLUR_FSH = """
            #version 150
            uniform sampler2D Sampler0;
            uniform vec2 Direction;
            in vec2 texCoord;
            out vec4 fragColor;
            void main() {
                vec4 color = vec4(0.0);
                float total = 0.0;
                for (float i = -8.0; i <= 8.0; i += 1.0) {
                    float weight = exp(-(i * i) / 18.0);
                    color += texture(Sampler0, texCoord + Direction * i) * weight;
                    total += weight;
                }
                fragColor = color / total;
            }
            """;

    private static final String GLASS_VSH = """
            #version 150
            in vec3 Position;
            in vec2 UV;
            out vec2 texCoord;
            void main() {
                gl_Position = vec4(Position, 1.0);
                texCoord = UV;
            }
            """;

    private static final String GLASS_FSH = """
            #version 150
            uniform sampler2D BlurredScene;
            uniform vec2 ScreenSize;
            uniform vec2 PanelPos;
            uniform vec2 PanelSize;
            uniform float Radius;
            uniform float HoverAmount;
            uniform vec3 AccentColor;
            uniform float AccentMix;
            in vec2 texCoord;
            out vec4 fragColor;

            float roundedBoxSDF(vec2 p, vec2 b, float r) {
                vec2 d = abs(p) - b + r;
                return length(max(d, 0.0)) - r;
            }

            void main() {
                vec2 pixelPos = gl_FragCoord.xy;
                vec2 center = PanelPos + PanelSize * 0.5;
                vec2 relPos = pixelPos - center;

                float dist = roundedBoxSDF(relPos, PanelSize * 0.5, Radius);
                if (dist > 1.0) discard;

                vec2 uv = gl_FragCoord.xy / ScreenSize;
                vec4 blurred = texture(BlurredScene, uv);

                vec3 glassTint = vec3(0.08, 0.08, 0.14);
                vec3 glassColor = mix(blurred.rgb * 0.7, glassTint, 0.45);
                glassColor = mix(glassColor, AccentColor, AccentMix);
                glassColor += vec3(0.06) * HoverAmount;

                float rimWidth = 1.2;
                float rim = 1.0 - smoothstep(0.0, rimWidth, abs(dist));
                glassColor += vec3(0.25) * rim;

                float alpha = 1.0 - smoothstep(-1.0, 0.5, dist);
                float baseAlpha = 0.7 + 0.08 * HoverAmount;

                fragColor = vec4(glassColor, baseAlpha * alpha);
            }
            """;

    private LiquidGlassRenderer() {
    }
}
