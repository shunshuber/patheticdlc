package com.pathdlc.digger.gui;

import com.pathdlc.digger.render.LiquidGlassRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CustomTitleScreen extends Screen {
    private static final Identifier BACKGROUND =
            Identifier.of("pathdlc_digger", "textures/gui/background.png");

    private static final int BTN_WIDTH = 200;
    private static final int BTN_HEIGHT = 24;
    private static final int BTN_GAP = 4;
    private static final float BTN_RADIUS = 8f;

    private static final String[] LABELS = {
            "Singleplayer", "Multiplayer", "Settings", "Quit Game"
    };

    private float openProgress;
    private final float[] btnHover = new float[4];
    private final float[] btnSlide = new float[4];
    private long startTime;

    private static final int PARTICLE_COUNT = 30;
    private final float[] particleX = new float[PARTICLE_COUNT];
    private final float[] particleY = new float[PARTICLE_COUNT];
    private final float[] particleSpeedX = new float[PARTICLE_COUNT];
    private final float[] particleSpeedY = new float[PARTICLE_COUNT];
    private final float[] particleSize = new float[PARTICLE_COUNT];
    private final float[] particleAlpha = new float[PARTICLE_COUNT];

    public CustomTitleScreen() {
        super(Text.literal("PathDLC"));
    }

    @Override
    protected void init() {
        super.init();
        openProgress = 0f;
        startTime = System.currentTimeMillis();
        for (int i = 0; i < 4; i++) {
            btnSlide[i] = 0f;
            btnHover[i] = 0f;
        }
        initParticles();
    }

    private void initParticles() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            resetParticle(i, true);
        }
    }

    private void resetParticle(int i, boolean randomY) {
        particleX[i] = (float) (Math.random() * 2000);
        particleY[i] = randomY ? (float) (Math.random() * 1200)
                : -10 - (float) (Math.random() * 50);
        particleSpeedX[i] = (float) (Math.random() * 0.3 - 0.15);
        particleSpeedY[i] = (float) (Math.random() * 0.4 + 0.1);
        particleSize[i] = (float) (Math.random() * 3 + 1);
        particleAlpha[i] = (float) (Math.random() * 0.3 + 0.05);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        LiquidGlassRenderer.captureAndBlur();

        openProgress += (1.0f - openProgress) * 0.06f;
        if (openProgress > 0.99f) openProgress = 1.0f;

        long elapsed = System.currentTimeMillis() - startTime;

        renderBackground(context);
        updateParticles();
        renderParticles(context);
        renderTitle(context, elapsed);
        renderButtons(context, mouseX, mouseY, elapsed);
        renderFooter(context);
        renderSeparator(context);
    }

    private void renderBackground(DrawContext context) {
        context.fill(0, 0, width, height, 0xFF080810);

        context.drawTexture(RenderLayer::getGuiTextured, BACKGROUND,
                0, 0, 0, 0, width, height, width, height);

        context.fill(0, 0, width, height, 0xB0080810);
    }

    private void updateParticles() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particleX[i] += particleSpeedX[i];
            particleY[i] += particleSpeedY[i];
            if (particleY[i] > height + 20 || particleX[i] < -20
                    || particleX[i] > width + 20) {
                resetParticle(i, false);
                particleX[i] = (float) (Math.random() * width);
            }
        }
    }

    private void renderParticles(DrawContext context) {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            int px = (int) particleX[i];
            int py = (int) particleY[i];
            int s = (int) particleSize[i];
            int a = (int) (particleAlpha[i] * 255 * openProgress);
            if (a < 1) continue;
            int color = (a << 24) | 0xCCDDFF;
            context.fill(px, py, px + s, py + s, color);
        }
    }

    private void renderTitle(DrawContext context, long elapsed) {
        float titleAlpha = Math.min(1f, openProgress * 1.5f);
        int titleA = (int) (titleAlpha * 255);
        if (titleA < 1) return;

        int ty = height / 4;

        int glassY = ty - 16;
        int glassW = 180;
        int glassH = 50;
        int glassX = width / 2 - glassW / 2;

        if (LiquidGlassRenderer.isReady()) {
            LiquidGlassRenderer.drawGlassPanel(context,
                    glassX, glassY, glassW, glassH, 12f, 0f);
        } else {
            context.fill(glassX, glassY, glassX + glassW, glassY + glassH,
                    0x44101028);
            context.fill(glassX, glassY, glassX + glassW, glassY + 1,
                    0x22FFFFFF);
        }

        context.getMatrices().push();
        context.getMatrices().translate(width / 2.0, ty, 0);
        context.getMatrices().scale(2.5f, 2.5f, 1f);
        context.getMatrices().translate(-width / 2.0, -ty, 0);

        String title = "PathDLC";
        int tw = textRenderer.getWidth(title);
        int titleColor = (titleA << 24) | 0xFFFFFF;
        context.drawText(textRenderer, Text.literal(title),
                width / 2 - tw / 2, ty, titleColor, true);

        context.getMatrices().pop();

        String sub = "Digger Client";
        int subW = textRenderer.getWidth(sub);
        int subA = (int) (titleAlpha * 140);
        int subColor = (subA << 24) | 0xAABBDD;
        context.drawText(textRenderer, Text.literal(sub),
                width / 2 - subW / 2, ty + 22, subColor, true);
    }

    private void renderButtons(DrawContext context, int mouseX, int mouseY,
                                long elapsed) {
        int startY = height / 2 + 5;
        int cx = width / 2 - BTN_WIDTH / 2;

        for (int i = 0; i < LABELS.length; i++) {
            float delay = i * 80f;
            float slideTarget = elapsed > delay ? 1f : 0f;
            btnSlide[i] += (slideTarget - btnSlide[i]) * 0.12f;

            float slide = btnSlide[i];
            if (slide < 0.01f) continue;

            int btnY = startY + i * (BTN_HEIGHT + BTN_GAP);
            int offsetX = (int) ((1f - slide) * 60);

            boolean hovered = slide > 0.5f
                    && mouseX >= cx && mouseX <= cx + BTN_WIDTH
                    && mouseY >= btnY && mouseY <= btnY + BTN_HEIGHT;
            float hTarget = hovered ? 1f : 0f;
            btnHover[i] += (hTarget - btnHover[i]) * 0.18f;

            int drawX = cx + offsetX;
            int alphaScale = (int) (slide * 255);

            if (LiquidGlassRenderer.isReady()) {
                LiquidGlassRenderer.drawGlassPanel(context,
                        drawX, btnY, BTN_WIDTH, BTN_HEIGHT,
                        BTN_RADIUS, btnHover[i]);
            } else {
                drawFallbackButton(context, drawX, btnY,
                        BTN_WIDTH, BTN_HEIGHT, btnHover[i], alphaScale);
            }

            int textA = Math.min(255, alphaScale);
            int textColor;
            if (hovered) {
                textColor = (textA << 24) | 0xFFFFFF;
            } else {
                textColor = (textA << 24) | 0xCCDDEE;
            }

            int labelW = textRenderer.getWidth(LABELS[i]);
            context.drawText(textRenderer, Text.literal(LABELS[i]),
                    drawX + BTN_WIDTH / 2 - labelW / 2,
                    btnY + BTN_HEIGHT / 2 - 4, textColor, true);

            if (btnHover[i] > 0.01f) {
                int lineA = (int) (btnHover[i] * 120);
                int lineColor = (lineA << 24) | 0x6699CC;
                context.fill(drawX, btnY + BTN_HEIGHT - 1,
                        drawX + (int) (BTN_WIDTH * btnHover[i]),
                        btnY + BTN_HEIGHT, lineColor);
            }
        }
    }

    private void drawFallbackButton(DrawContext context, int x, int y,
                                      int w, int h, float hover,
                                      int alphaScale) {
        int bgA = (int) (Math.min(255, alphaScale) * (0.65f + 0.15f * hover));
        int bg = (bgA << 24) | 0x10102A;
        context.fill(x, y, x + w, y + h, bg);

        int rimA = (int) (Math.min(255, alphaScale) * (0.25f + 0.2f * hover));
        int rimTop = (rimA << 24) | 0xAABBDD;
        int rimSide = ((rimA / 2) << 24) | 0x8899BB;
        context.fill(x, y, x + w, y + 1, rimTop);
        context.fill(x, y + 1, x + 1, y + h, rimSide);
        context.fill(x + w - 1, y + 1, x + w, y + h, rimSide);

        if (hover > 0.01f) {
            int glowA = (int) (hover * 15);
            int glow = (glowA << 24) | 0x4477AA;
            context.fill(x + 1, y + 1, x + w - 1, y + h - 1, glow);
        }
    }

    private void renderSeparator(DrawContext context) {
        int sepY = height / 2 - 8;
        int sepW = 120;
        int sepX = width / 2 - sepW / 2;
        int sepA = (int) (openProgress * 30);
        int sepColor = (sepA << 24) | 0x8899BB;
        context.fill(sepX, sepY, sepX + sepW, sepY + 1, sepColor);
    }

    private void renderFooter(DrawContext context) {
        int footA = (int) (openProgress * 100);
        int footColor = (footA << 24) | 0x667788;

        String left = "PathDLC Digger";
        context.drawText(textRenderer, Text.literal(left),
                6, height - 14, footColor, false);

        String right = "Minecraft 1.21.4";
        int rw = textRenderer.getWidth(right);
        context.drawText(textRenderer, Text.literal(right),
                width - rw - 6, height - 14, footColor, false);

        String center = "fabric";
        int cw = textRenderer.getWidth(center);
        context.drawText(textRenderer, Text.literal(center),
                width / 2 - cw / 2, height - 14, footColor, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int startY = height / 2 + 5;
            int cx = width / 2 - BTN_WIDTH / 2;

            for (int i = 0; i < 4; i++) {
                if (btnSlide[i] < 0.5f) continue;
                int btnY = startY + i * (BTN_HEIGHT + BTN_GAP);
                if (mouseX >= cx && mouseX <= cx + BTN_WIDTH
                        && mouseY >= btnY && mouseY <= btnY + BTN_HEIGHT) {
                    handleButton(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleButton(int index) {
        if (client == null) return;
        switch (index) {
            case 0 -> client.setScreen(new SelectWorldScreen(this));
            case 1 -> client.setScreen(new MultiplayerScreen(this));
            case 2 -> client.setScreen(new OptionsScreen(this, client.options));
            case 3 -> client.scheduleStop();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
