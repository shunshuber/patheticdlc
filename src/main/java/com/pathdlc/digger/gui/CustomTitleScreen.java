package com.pathdlc.digger.gui;

import com.pathdlc.digger.render.LiquidGlassRenderer;
import com.pathdlc.digger.render.PerformanceSettings;
import com.pathdlc.digger.render.RoundedRectRenderer;
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

    private static final int BTN_WIDTH = 220;
    private static final int BTN_HEIGHT = 28;
    private static final int BTN_GAP = 6;
    private static final int BTN_RADIUS = 14;

    private static final String[] LABELS = {
            "Singleplayer", "Multiplayer", "Settings", "Quit Game"
    };

    private float openProgress;
    private final float[] btnHover = new float[4];
    private final float[] btnSlide = new float[4];
    private long startTime;

    private static final int PARTICLE_COUNT = 40;
    private final float[] px = new float[PARTICLE_COUNT];
    private final float[] py = new float[PARTICLE_COUNT];
    private final float[] pvx = new float[PARTICLE_COUNT];
    private final float[] pvy = new float[PARTICLE_COUNT];
    private final float[] psz = new float[PARTICLE_COUNT];
    private final float[] pa = new float[PARTICLE_COUNT];

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
        px[i] = (float)(Math.random() * 2000);
        py[i] = randomY ? (float)(Math.random() * 1200) : -10f - (float)(Math.random() * 50);
        pvx[i] = (float)(Math.random() * 0.25 - 0.125);
        pvy[i] = (float)(Math.random() * 0.3 + 0.08);
        psz[i] = (float)(Math.random() * 2.5 + 1);
        pa[i] = (float)(Math.random() * 0.25 + 0.05);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        LiquidGlassRenderer.captureAndBlur();

        openProgress += (1f - openProgress) * 0.06f;
        if (openProgress > 0.99f) openProgress = 1f;

        long elapsed = System.currentTimeMillis() - startTime;

        renderBackground(context);
        updateParticles();
        renderParticles(context);
        renderTitle(context, elapsed);
        renderButtons(context, mouseX, mouseY, elapsed);
        renderFooter(context);
    }

    private void renderBackground(DrawContext context) {
        context.fill(0, 0, width, height, 0xFF060610);
        context.drawTexture(RenderLayer::getGuiTextured, BACKGROUND,
                0, 0, 0, 0, width, height, width, height);
        context.fill(0, 0, width, height, 0xAA060610);
    }

    private void updateParticles() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            px[i] += pvx[i];
            py[i] += pvy[i];
            if (py[i] > height + 20 || px[i] < -20 || px[i] > width + 20) {
                resetParticle(i, false);
                px[i] = (float)(Math.random() * width);
            }
        }
    }

    private void renderParticles(DrawContext context) {
        int maxParticles = Math.min(PARTICLE_COUNT, PerformanceSettings.getParticleCount());
        for (int i = 0; i < maxParticles; i++) {
            int x = (int)px[i];
            int y = (int)py[i];
            int s = Math.max(1, (int)psz[i]);
            int a = (int)(pa[i] * 255 * openProgress);
            if (a < 1) continue;
            RoundedRectRenderer.draw(context, x, y, s * 2, s * 2, s,
                    (a << 24) | 0x99BBFF);
        }
    }

    private void renderTitle(DrawContext context, long elapsed) {
        float titleProgress = Math.min(1f, elapsed / 600f) * openProgress;
        if (titleProgress < 0.01f) return;

        String title = "PathDLC";
        int titleW = textRenderer.getWidth(title) * 3;
        int titleX = width / 2 - titleW / 6;
        int titleY = height / 2 - (LABELS.length * (BTN_HEIGHT + BTN_GAP)) / 2 - 50;

        int glassW = titleW / 3 + 40;
        int glassH = 30;
        int glassX = width / 2 - glassW / 2;
        int glassY = titleY - 6;

        if (PerformanceSettings.useGlassEffect() && LiquidGlassRenderer.isReady()) {
            LiquidGlassRenderer.drawGlassPanel(context,
                    glassX, glassY, glassW, glassH, 15f, 0f,
                    0.12f, 0.3f, 0.6f, 1.0f);
        } else {
            RoundedRectRenderer.draw(context, glassX, glassY, glassW, glassH,
                    15, 0x88101830);
        }

        int titleA = (int)(titleProgress * 255);
        int titleColor = (titleA << 24) | 0xFFFFFF;

        context.getMatrices().push();
        context.getMatrices().scale(3f, 3f, 1f);
        context.drawText(textRenderer, Text.literal(title),
                titleX / 3, titleY / 3, titleColor, true);
        context.getMatrices().pop();

        int subtitleA = (int)(titleProgress * 120);
        String subtitle = "Digger v1.21.4";
        int subW = textRenderer.getWidth(subtitle);
        context.drawText(textRenderer, Text.literal(subtitle),
                width / 2 - subW / 2, titleY + 22,
                (subtitleA << 24) | 0x8899CC, false);
    }

    private void renderButtons(DrawContext context, int mouseX, int mouseY,
                                long elapsed) {
        int startY = height / 2 - (LABELS.length * (BTN_HEIGHT + BTN_GAP)) / 2 + 10;
        int cx = width / 2 - BTN_WIDTH / 2;

        for (int i = 0; i < LABELS.length; i++) {
            float delay = i * 100f;
            float slideTarget = elapsed > delay ? 1f : 0f;
            btnSlide[i] += (slideTarget - btnSlide[i]) * 0.1f;

            float slide = btnSlide[i];
            if (slide < 0.01f) continue;

            int btnY = startY + i * (BTN_HEIGHT + BTN_GAP);
            int offsetX = (int)((1f - slide) * 80);
            float alphaF = slide * openProgress;

            boolean hovered = slide > 0.5f
                    && mouseX >= cx && mouseX <= cx + BTN_WIDTH
                    && mouseY >= btnY && mouseY <= btnY + BTN_HEIGHT;
            float hTarget = hovered ? 1f : 0f;
            btnHover[i] += (hTarget - btnHover[i]) * 0.15f;

            int drawX = cx + offsetX;

            if (PerformanceSettings.useGlassEffect() && LiquidGlassRenderer.isReady()) {
                LiquidGlassRenderer.drawGlassPanel(context,
                        drawX, btnY, BTN_WIDTH, BTN_HEIGHT,
                        BTN_RADIUS, btnHover[i],
                        0.05f + btnHover[i] * 0.1f, 0.3f, 0.6f, 1.0f);
            } else {
                int bgA = (int)(alphaF * (160 + 40 * btnHover[i]));
                RoundedRectRenderer.draw(context, drawX, btnY,
                        BTN_WIDTH, BTN_HEIGHT, BTN_RADIUS,
                        (bgA << 24) | 0x101830);
            }

            if (btnHover[i] > 0.01f) {
                int glowA = (int)(btnHover[i] * 25 * alphaF);
                RoundedRectRenderer.draw(context, drawX + 2, btnY + 2,
                        BTN_WIDTH - 4, BTN_HEIGHT - 4, BTN_RADIUS - 2,
                        (glowA << 24) | 0x4488CC);
            }

            int textA = (int)(alphaF * 255);
            int textColor = hovered ? (textA << 24) | 0xFFFFFF
                    : (textA << 24) | 0xCCDDEE;
            int labelW = textRenderer.getWidth(LABELS[i]);
            context.drawText(textRenderer, Text.literal(LABELS[i]),
                    drawX + BTN_WIDTH / 2 - labelW / 2,
                    btnY + BTN_HEIGHT / 2 - 4, textColor, true);

            if (btnHover[i] > 0.01f) {
                int lineA = (int)(btnHover[i] * 100 * alphaF);
                int lineW = (int)((BTN_WIDTH - BTN_RADIUS * 2) * btnHover[i]);
                int lineX = drawX + BTN_WIDTH / 2 - lineW / 2;
                RoundedRectRenderer.draw(context, lineX, btnY + BTN_HEIGHT - 3,
                        lineW, 2, 1, (lineA << 24) | 0x6699DD);
            }
        }
    }

    private void renderFooter(DrawContext context) {
        int a = (int)(openProgress * 80);
        int color = (a << 24) | 0x667788;

        context.drawText(textRenderer, Text.literal("PathDLC Digger"),
                6, height - 14, color, false);

        String right = "Minecraft 1.21.4";
        int rw = textRenderer.getWidth(right);
        context.drawText(textRenderer, Text.literal(right),
                width - rw - 6, height - 14, color, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int startY = height / 2 - (LABELS.length * (BTN_HEIGHT + BTN_GAP)) / 2 + 10;
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
