package com.pathdlc.digger.gui;

import com.pathdlc.digger.render.LiquidGlassRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
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

    private float openProgress;
    private final float[] btnHover = new float[5];

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
        LiquidGlassRenderer.captureAndBlur();

        openProgress += (1.0f - openProgress) * 0.1f;
        if (openProgress > 0.99f) openProgress = 1.0f;

        renderBackground(context);
        renderTitle(context);
        renderButtons(context, mouseX, mouseY);
        renderFooter(context);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderBackground(DrawContext context) {
        context.drawTexture(RenderLayer::getGuiTextured, BACKGROUND,
                0, 0, 0, 0, width, height, width, height);

        int overlayAlpha = (int) (100 * openProgress);
        int overlayColor = (overlayAlpha << 24) | 0x080810;
        context.fill(0, 0, width, height, overlayColor);
    }

    private void renderTitle(DrawContext context) {
        String title = "PathDLC";
        int tw = textRenderer.getWidth(title) * 2;
        int tx = width / 2 - tw / 2;
        int ty = height / 4 - 20;

        context.getMatrices().push();
        context.getMatrices().translate(width / 2.0, ty, 0);
        context.getMatrices().scale(2f, 2f, 1f);
        context.getMatrices().translate(-width / 2.0, -ty, 0);

        context.drawText(textRenderer, Text.literal(title),
                width / 2 - textRenderer.getWidth(title) / 2,
                ty, 0xFFFFFFFF, true);

        context.getMatrices().pop();

        String subtitle = "Digger Mod";
        int stw = textRenderer.getWidth(subtitle);
        context.drawText(textRenderer, Text.literal(subtitle),
                width / 2 - stw / 2, ty + 24, 0xFF999999, true);
    }

    private void renderButtons(DrawContext context, int mouseX, int mouseY) {
        int startY = height / 2 - 20;
        int cx = width / 2 - BTN_WIDTH / 2;

        String[] labels = {"Singleplayer", "Multiplayer", "Settings", "Quit Game"};

        for (int i = 0; i < labels.length; i++) {
            int btnY = startY + i * (BTN_HEIGHT + BTN_GAP);
            boolean hovered = mouseX >= cx && mouseX <= cx + BTN_WIDTH
                    && mouseY >= btnY && mouseY <= btnY + BTN_HEIGHT;

            float target = hovered ? 1f : 0f;
            btnHover[i] += (target - btnHover[i]) * 0.2f;

            drawGlassButton(context, cx, btnY, BTN_WIDTH, BTN_HEIGHT,
                    BTN_RADIUS, btnHover[i], false);

            int textColor = hovered ? 0xFFFFFFFF : 0xFFDDDDDD;
            int labelW = textRenderer.getWidth(labels[i]);
            context.drawText(textRenderer, Text.literal(labels[i]),
                    cx + BTN_WIDTH / 2 - labelW / 2,
                    btnY + BTN_HEIGHT / 2 - 4, textColor, true);
        }
    }

    private void drawGlassButton(DrawContext context, int x, int y, int w,
                                   int h, float radius, float hover, boolean active) {
        if (LiquidGlassRenderer.isReady()) {
            LiquidGlassRenderer.drawGlassPanel(context, x, y, w, h, radius, hover);
        } else {
            int bgAlpha = (int) (160 + 30 * hover);
            int bg = (bgAlpha << 24) | 0x1A1A2E;
            context.fill(x, y, x + w, y + h, bg);
            context.fill(x, y, x + w, y + 1, 0x44FFFFFF);
            context.fill(x, y, x + 1, y + h, 0x22FFFFFF);
            context.fill(x + w - 1, y, x + w, y + h, 0x22FFFFFF);
            context.fill(x, y + h - 1, x + w, y + h, 0x11FFFFFF);
        }
    }

    private void renderFooter(DrawContext context) {
        String version = "PathDLC Digger v1.0.0";
        context.drawText(textRenderer, Text.literal(version),
                4, height - 12, 0xFF666666, false);

        String mc = "Minecraft 1.21.4";
        int mcw = textRenderer.getWidth(mc);
        context.drawText(textRenderer, Text.literal(mc),
                width - mcw - 4, height - 12, 0xFF666666, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int startY = height / 2 - 20;
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
