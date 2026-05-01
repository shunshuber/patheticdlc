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

    private static final int BTN_WIDTH = 220;
    private static final int BTN_HEIGHT = 28;
    private static final int BTN_GAP = 6;

    private float openProgress;
    private final float[] btnHover = new float[4];

    public CustomTitleScreen() {
        super(Text.literal("PathDLC"));
    }

    @Override
    protected void init() {
        super.init();
        openProgress = 0f;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        openProgress += (1.0f - openProgress) * 0.1f;
        if (openProgress > 0.99f) openProgress = 1.0f;

        renderBackground(context);
        renderTitle(context);
        renderButtons(context, mouseX, mouseY);
        renderFooter(context);
    }

    private void renderBackground(DrawContext context) {
        context.fill(0, 0, width, height, 0xFF0A0A14);

        context.drawTexture(RenderLayer::getGuiTextured, BACKGROUND,
                0, 0, 0, 0, width, height, width, height);

        context.fill(0, 0, width, height, 0xCC0A0A14);

        int glowAlpha = (int) (15 * openProgress);
        int glowColor = (glowAlpha << 24) | 0x4488FF;
        int cx = width / 2;
        int cy = height / 2;
        int r = 200;
        context.fill(cx - r, cy - r, cx + r, cy + r, glowColor);
    }

    private void renderTitle(DrawContext context) {
        int ty = height / 4 - 10;

        context.getMatrices().push();
        context.getMatrices().translate(width / 2.0, ty, 0);
        context.getMatrices().scale(3f, 3f, 1f);
        context.getMatrices().translate(-width / 2.0, -ty, 0);

        String title = "PathDLC";
        int tw = textRenderer.getWidth(title);
        context.drawText(textRenderer, Text.literal(title),
                width / 2 - tw / 2, ty, 0xFFFFFFFF, true);

        context.getMatrices().pop();

        String subtitle = "Digger Mod v1.0.0";
        int stw = textRenderer.getWidth(subtitle);
        context.drawText(textRenderer, Text.literal(subtitle),
                width / 2 - stw / 2, ty + 30, 0xFF888888, true);
    }

    private void renderButtons(DrawContext context, int mouseX, int mouseY) {
        int startY = height / 2 + 10;
        int cx = width / 2 - BTN_WIDTH / 2;

        String[] labels = {"Singleplayer", "Multiplayer", "Settings", "Quit Game"};

        for (int i = 0; i < labels.length; i++) {
            int btnY = startY + i * (BTN_HEIGHT + BTN_GAP);
            boolean hovered = mouseX >= cx && mouseX <= cx + BTN_WIDTH
                    && mouseY >= btnY && mouseY <= btnY + BTN_HEIGHT;

            float target = hovered ? 1f : 0f;
            btnHover[i] += (target - btnHover[i]) * 0.2f;

            drawButton(context, cx, btnY, BTN_WIDTH, BTN_HEIGHT, btnHover[i]);

            int textColor = hovered ? 0xFFFFFFFF : 0xFFCCCCCC;
            int labelW = textRenderer.getWidth(labels[i]);
            context.drawText(textRenderer, Text.literal(labels[i]),
                    cx + BTN_WIDTH / 2 - labelW / 2,
                    btnY + BTN_HEIGHT / 2 - 4, textColor, true);
        }
    }

    private void drawButton(DrawContext context, int x, int y, int w, int h,
                              float hover) {
        int bgAlpha = (int) (180 + 50 * hover);
        int bg = (bgAlpha << 24) | 0x14142A;
        context.fill(x, y, x + w, y + h, bg);

        int rimAlpha = (int) (80 + 60 * hover);
        int rimTop = (rimAlpha << 24) | 0xFFFFFF;
        int rimSide = ((rimAlpha / 2) << 24) | 0xFFFFFF;
        int rimBottom = ((rimAlpha / 4) << 24) | 0xFFFFFF;

        context.fill(x, y, x + w, y + 1, rimTop);
        context.fill(x, y, x + 1, y + h, rimSide);
        context.fill(x + w - 1, y, x + w, y + h, rimSide);
        context.fill(x, y + h - 1, x + w, y + h, rimBottom);

        if (hover > 0.01f) {
            int accentAlpha = (int) (20 * hover);
            int accent = (accentAlpha << 24) | 0x4488FF;
            context.fill(x + 1, y + 1, x + w - 1, y + h - 1, accent);
        }
    }

    private void renderFooter(DrawContext context) {
        String left = "PathDLC Digger";
        context.drawText(textRenderer, Text.literal(left),
                6, height - 14, 0xFF555555, false);

        String right = "Minecraft 1.21.4";
        int rw = textRenderer.getWidth(right);
        context.drawText(textRenderer, Text.literal(right),
                width - rw - 6, height - 14, 0xFF555555, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int startY = height / 2 + 10;
            int cx = width / 2 - BTN_WIDTH / 2;

            for (int i = 0; i < 4; i++) {
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
