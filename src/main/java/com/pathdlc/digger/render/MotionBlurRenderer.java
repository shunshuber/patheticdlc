package com.pathdlc.digger.render;

import com.pathdlc.digger.gui.Module;
import com.pathdlc.digger.gui.ModuleManager;
import com.pathdlc.digger.gui.ModuleSetting;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;

public final class MotionBlurRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("PathDLC-MotionBlur");

    private static int program;
    private static int prevFbo, prevTex;
    private static int captureFbo, captureTex;
    private static int quadVao, quadVbo;
    private static int lastWidth, lastHeight;
    private static boolean initialized;
    private static boolean failed;
    private static boolean hasPrevFrame;

    private static int uCurrentFrame, uPrevFrame, uStrength;

    public static void onFrameEnd(int screenWidth, int screenHeight) {
        if (!ModuleManager.isEnabled("MotionBlur")) {
            hasPrevFrame = false;
            return;
        }

        if (!initialized && !failed) {
            try {
                init();
            } catch (Exception e) {
                LOGGER.error("MotionBlur init failed: " + e.getMessage(), e);
                failed = true;
                return;
            }
        }
        if (failed) return;

        if (screenWidth != lastWidth || screenHeight != lastHeight) {
            resizeFbo(screenWidth, screenHeight);
            lastWidth = screenWidth;
            lastHeight = screenHeight;
            hasPrevFrame = false;
        }

        float strength = 0.5f;
        Module mod = ModuleManager.get("MotionBlur");
        if (mod != null) {
            ModuleSetting s = mod.getSetting("Strength");
            if (s != null) strength = s.getFloat();
        }

        int currentFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, currentFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, captureFbo);
        GL30.glBlitFramebuffer(0, 0, screenWidth, screenHeight,
                0, 0, screenWidth, screenHeight,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, currentFbo);

        if (!hasPrevFrame) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, currentFbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevFbo);
            GL30.glBlitFramebuffer(0, 0, screenWidth, screenHeight,
                    0, 0, screenWidth, screenHeight,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, currentFbo);
            hasPrevFrame = true;
            return;
        }

        boolean blendWas = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthWas = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL20.glUseProgram(program);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, captureTex);
        GL20.glUniform1i(uCurrentFrame, 0);

        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
        GL20.glUniform1i(uPrevFrame, 1);

        GL20.glUniform1f(uStrength, strength);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, currentFbo);

        GL30.glBindVertexArray(quadVao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL30.glBindVertexArray(0);

        GL20.glUseProgram(0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, currentFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevFbo);
        GL30.glBlitFramebuffer(0, 0, screenWidth, screenHeight,
                0, 0, screenWidth, screenHeight,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, currentFbo);

        if (!blendWas) GL11.glDisable(GL11.GL_BLEND);
        if (depthWas) GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    private static void init() {
        LOGGER.info("Initializing MotionBlur renderer...");
        program = createProgram(VSH, FSH);
        uCurrentFrame = GL20.glGetUniformLocation(program, "CurrentFrame");
        uPrevFrame = GL20.glGetUniformLocation(program, "PrevFrame");
        uStrength = GL20.glGetUniformLocation(program, "Strength");

        createQuad();

        prevFbo = GL30.glGenFramebuffers();
        prevTex = GL11.glGenTextures();
        captureFbo = GL30.glGenFramebuffers();
        captureTex = GL11.glGenTextures();

        initialized = true;
        LOGGER.info("MotionBlur initialized, program={}", program);
    }

    private static void resizeFbo(int w, int h) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, prevTex, 0);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, captureTex);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, captureFbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, captureTex, 0);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    private static void createQuad() {
        float[] verts = {
                -1, -1, 0, 0,
                 1, -1, 1, 0,
                -1,  1, 0, 1,
                 1,  1, 1, 1
        };
        FloatBuffer buf = BufferUtils.createFloatBuffer(verts.length);
        buf.put(verts).flip();

        quadVao = GL30.glGenVertexArrays();
        quadVbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(quadVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 16, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 16, 8);
        GL30.glBindVertexArray(0);
    }

    private static int createProgram(String vsh, String fsh) {
        int vs = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vs, vsh);
        GL20.glCompileShader(vs);
        if (GL20.glGetShaderi(vs, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(vs);
            throw new RuntimeException("MotionBlur VS compile error: " + log);
        }

        int fs = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fs, fsh);
        GL20.glCompileShader(fs);
        if (GL20.glGetShaderi(fs, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(fs);
            throw new RuntimeException("MotionBlur FS compile error: " + log);
        }

        int prog = GL20.glCreateProgram();
        GL20.glAttachShader(prog, vs);
        GL20.glAttachShader(prog, fs);
        GL20.glBindAttribLocation(prog, 0, "Position");
        GL20.glBindAttribLocation(prog, 1, "UV");
        GL20.glLinkProgram(prog);
        if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) == 0) {
            String log = GL20.glGetProgramInfoLog(prog);
            throw new RuntimeException("MotionBlur link error: " + log);
        }

        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
        return prog;
    }

    private static final String VSH = """
            #version 150
            in vec2 Position;
            in vec2 UV;
            out vec2 texCoord;
            void main() {
                texCoord = UV;
                gl_Position = vec4(Position, 0.0, 1.0);
            }
            """;

    private static final String FSH = """
            #version 150
            uniform sampler2D CurrentFrame;
            uniform sampler2D PrevFrame;
            uniform float Strength;
            in vec2 texCoord;
            out vec4 fragColor;
            void main() {
                vec4 current = texture(CurrentFrame, texCoord);
                vec4 prev = texture(PrevFrame, texCoord);
                fragColor = mix(current, prev, Strength);
            }
            """;

    private MotionBlurRenderer() {}
}
