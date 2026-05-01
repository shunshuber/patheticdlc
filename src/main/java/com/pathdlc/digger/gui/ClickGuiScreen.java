package com.pathdlc.digger.gui;

import com.pathdlc.digger.render.LiquidGlassRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
    private static final int WIN_WIDTH = 360;
    private static final int WIN_HEIGHT = 300;
    private static final int TAB_HEIGHT = 28;
    private static final int MODULE_HEIGHT = 22;
    private static final int SETTING_HEIGHT = 16;
    private static final int PADDING = 8;
    private static final int SCROLL_SPEED = 10;
    private static final float CORNER_RADIUS = 8f;

    private static final Identifier CUSTOM_FONT =
            Identifier.of("pathdlc_digger", "clickgui");

    private final List<Category> categories = new ArrayList<>();
    private int selectedTab = 0;
    private float openProgress = 0f;
    private int scrollOffset = 0;

    private ModuleSetting draggingSlider;
    private float draggingSliderX;
    private float draggingSliderW;

    public ClickGuiScreen() {
        super(Text.literal("ClickGUI"));
    }

    @Override
    protected void init() {
        super.init();
        openProgress = 0f;
        scrollOffset = 0;
    }

    public void initCategories(
            Runnable appleOn, Runnable appleOff,
            Runnable digOn, Runnable digOff,
            Runnable wardenOn, Runnable wardenOff,
            Runnable clanOn, Runnable clanOff) {
        if (!categories.isEmpty()) return;

        Module apple = new Module("Apple", appleOn, appleOff);
        Module dig = new Module("Dig", digOn, digOff);
        Category farm = new Category("Farm", 0, 0);
        farm.addModule(apple);
        farm.addModule(dig);
        categories.add(farm);

        Module warden = new Module("Warden", wardenOn, wardenOff);
        Module fog = new Module("Fog");
        fog.addSetting(ModuleSetting.choice("Color",
                new String[]{"White","Light Blue","Purple","Red","Green","Dark","Golden"}, 0));
        fog.addSetting(ModuleSetting.slider("Density", 0.5f, 0.0f, 1.0f, 0.1f));
        Category world = new Category("World", 0, 0);
        world.addModule(warden);
        world.addModule(fog);
        categories.add(world);

        Module clan = new Module("Clan", clanOn, clanOff);
        Module killAura = new Module("KillAura");
        killAura.addSetting(ModuleSetting.slider("Range", 4.0f, 2.0f, 6.0f, 0.1f));
        killAura.addSetting(ModuleSetting.toggle("Only Crit", false));
        killAura.addSetting(ModuleSetting.toggle("Attack Mobs", true));
        killAura.addSetting(ModuleSetting.toggle("Attack Players", false));
        Category combat = new Category("Combat", 0, 0);
        combat.addModule(clan);
        combat.addModule(killAura);
        categories.add(combat);

        Module hitEffects = new Module("HitEffects");
        Module aspectRatio = new Module("AspectRatio");
        aspectRatio.addSetting(ModuleSetting.slider("FOV Scale", 1.33f, 1.0f, 2.0f, 0.01f));
        Category utility = new Category("Utility", 0, 0);
        utility.addModule(hitEffects);
        utility.addModule(aspectRatio);
        categories.add(utility);

        Module blockOverlay = new Module("BlockOverlay");
        blockOverlay.addSetting(ModuleSetting.choice("Texture",
                new String[]{"Kitten","Sky","Devil"}, 0));
        Module blockEsp = new Module("BlockESP");
        blockEsp.addSetting(ModuleSetting.slider("Radius", 32f, 8f, 64f, 4f));
        Module motionBlur = new Module("MotionBlur");
        motionBlur.addSetting(ModuleSetting.slider("Strength", 0.5f, 0.1f, 0.9f, 0.05f));
        Category render = new Category("Render", 0, 0);
        render.addModule(blockOverlay);
        render.addModule(blockEsp);
        render.addModule(motionBlur);
        categories.add(render);

        for (Category cat : categories) {
            for (ModuleButton btn : cat.getModules()) {
                ModuleManager.register(btn.getModule());
            }
        }
    }

    private int winX() { return (width - WIN_WIDTH) / 2; }
    private int winY() { return (height - WIN_HEIGHT) / 2; }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        openProgress += (1f - openProgress) * 0.2f;
        if (openProgress > 0.99f) openProgress = 1f;

        float scale = 0.85f + 0.15f * openProgress;
        int alpha = (int)(openProgress * 255);
        if (alpha < 1) return;

        int wx = winX();
        int wy = winY();

        LiquidGlassRenderer.captureAndBlur();

        context.fill(0, 0, width, height, (int)(openProgress * 0x88) << 24);

        if (LiquidGlassRenderer.isReady()) {
            LiquidGlassRenderer.drawGlassPanel(context,
                    wx, wy, WIN_WIDTH, WIN_HEIGHT, CORNER_RADIUS, 0f);
        } else {
            context.fill(wx, wy, wx + WIN_WIDTH, wy + WIN_HEIGHT, 0xDD0A0A12);
        }

        drawBorder(context, wx, wy, WIN_WIDTH, WIN_HEIGHT, 0x44FFFFFF);

        renderTabs(context, wx, wy, mouseX, mouseY);

        int contentY = wy + TAB_HEIGHT + 2;
        int contentH = WIN_HEIGHT - TAB_HEIGHT - 2;

        context.enableScissor(wx, contentY, wx + WIN_WIDTH, contentY + contentH);
        renderModules(context, wx, contentY, mouseX, mouseY);
        context.disableScissor();

        renderScrollbar(context, wx, contentY, contentH);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderTabs(DrawContext context, int wx, int wy,
                             int mouseX, int mouseY) {
        GuiSettings.AccentColor accent = GuiSettings.getAccentColor();
        int tabCount = categories.size();
        int tabW = WIN_WIDTH / tabCount;

        for (int i = 0; i < tabCount; i++) {
            int tx = wx + i * tabW;
            boolean hovered = mouseX >= tx && mouseX < tx + tabW
                    && mouseY >= wy && mouseY < wy + TAB_HEIGHT;
            boolean active = i == selectedTab;

            if (hovered && !active) {
                context.fill(tx, wy, tx + tabW, wy + TAB_HEIGHT, 0x15FFFFFF);
            }

            String name = categories.get(i).getName();
            int textW = textRenderer.getWidth(styledText(name));
            int textX = tx + tabW / 2 - textW / 2;
            int textY = wy + TAB_HEIGHT / 2 - 4;

            int color = active ? accent.textColor : (hovered ? 0xFFDDDDDD : 0xFF888888);
            drawStyledText(context, name, textX, textY, color);

            if (active) {
                context.fill(tx + 4, wy + TAB_HEIGHT - 2,
                        tx + tabW - 4, wy + TAB_HEIGHT, accent.textColor);
            }
        }

        context.fill(wx, wy + TAB_HEIGHT, wx + WIN_WIDTH,
                wy + TAB_HEIGHT + 1, 0x33FFFFFF);
    }

    private void renderModules(DrawContext context, int wx, int contentY,
                                int mouseX, int mouseY) {
        if (selectedTab < 0 || selectedTab >= categories.size()) return;
        Category cat = categories.get(selectedTab);
        GuiSettings.AccentColor accent = GuiSettings.getAccentColor();

        int y = contentY - scrollOffset + 4;

        for (ModuleButton btn : cat.getModules()) {
            Module mod = btn.getModule();

            boolean hovered = mouseX >= wx + PADDING
                    && mouseX <= wx + WIN_WIDTH - PADDING
                    && mouseY >= y && mouseY <= y + MODULE_HEIGHT
                    && mouseY >= contentY;
            btn.updateHover(hovered);

            int bgAlpha = (int)(btn.hoverAmount * 0x22);
            if (bgAlpha > 0) {
                context.fill(wx + PADDING, y,
                        wx + WIN_WIDTH - PADDING, y + MODULE_HEIGHT,
                        (bgAlpha << 24) | 0xFFFFFF);
            }

            int nameColor = mod.isEnabled() ? accent.textColor : 0xFFBBBBBB;
            drawStyledText(context, mod.getName(), wx + PADDING + 6, y + 7, nameColor);

            if (mod.isEnabled()) {
                int dotY = y + MODULE_HEIGHT / 2;
                context.fill(wx + WIN_WIDTH - PADDING - 14, dotY - 2,
                        wx + WIN_WIDTH - PADDING - 10, dotY + 2, accent.textColor);
            }

            if (mod.hasSettings()) {
                String arrow = mod.isSettingsExpanded() ? "v" : ">";
                int arrowX = wx + WIN_WIDTH - PADDING - 24;
                drawStyledText(context, arrow, arrowX, y + 7, 0xFF666666);
            }

            y += MODULE_HEIGHT;

            if (mod.isSettingsExpanded() && mod.hasSettings()) {
                for (ModuleSetting setting : mod.getSettings()) {
                    renderSetting(context, setting, wx, y, mouseX, mouseY,
                            accent, contentY);
                    y += SETTING_HEIGHT;
                }
            }
        }
    }

    private void renderSetting(DrawContext context, ModuleSetting setting,
                                int wx, int y, int mouseX, int mouseY,
                                GuiSettings.AccentColor accent, int contentY) {
        int left = wx + PADDING + 10;
        int right = wx + WIN_WIDTH - PADDING - 4;
        int w = right - left;

        context.fill(left, y, right, y + SETTING_HEIGHT, 0x11FFFFFF);

        drawStyledText(context, setting.getName(), left + 4, y + 4, 0xFF999999);

        if (setting.getType() == ModuleSetting.Type.SLIDER) {
            float sliderX = left + w * 0.5f;
            float sliderW = w * 0.42f;
            float sliderY = y + SETTING_HEIGHT / 2f - 1;
            float norm = setting.getNormalized();

            context.fill((int) sliderX, (int) sliderY,
                    (int)(sliderX + sliderW), (int)(sliderY + 2), 0x44FFFFFF);
            context.fill((int) sliderX, (int) sliderY,
                    (int)(sliderX + sliderW * norm), (int)(sliderY + 2),
                    accent.textColor);

            int knobX = (int)(sliderX + sliderW * norm);
            context.fill(knobX - 2, (int)(sliderY - 2),
                    knobX + 2, (int)(sliderY + 4), 0xFFFFFFFF);

            String val = setting.getDisplayValue();
            int valW = textRenderer.getWidth(styledText(val));
            drawStyledText(context, val, right - valW - 2, y + 4, 0xFFCCCCCC);

        } else if (setting.getType() == ModuleSetting.Type.TOGGLE) {
            String val = setting.getBool() ? "ON" : "OFF";
            int valColor = setting.getBool() ? accent.textColor : 0xFF555555;
            int valW = textRenderer.getWidth(styledText(val));
            drawStyledText(context, val, right - valW - 4, y + 4, valColor);

        } else if (setting.getType() == ModuleSetting.Type.CHOICE) {
            String val = setting.getChoiceValue();
            int valW = textRenderer.getWidth(styledText(val));
            drawStyledText(context, val, right - valW - 4, y + 4, 0xFFCCCCCC);
        }
    }

    private void renderScrollbar(DrawContext context, int wx, int contentY,
                                   int contentH) {
        int totalH = getTotalContentHeight();
        if (totalH <= contentH) return;

        int barX = wx + WIN_WIDTH - 3;
        float ratio = (float) contentH / totalH;
        int thumbH = Math.max(10, (int)(contentH * ratio));
        int thumbY = contentY + (int)((float) scrollOffset / (totalH - contentH)
                * (contentH - thumbH));

        context.fill(barX, contentY, barX + 2, contentY + contentH, 0x22FFFFFF);
        context.fill(barX, thumbY, barX + 2, thumbY + thumbH, 0x66FFFFFF);
    }

    private int getTotalContentHeight() {
        if (selectedTab < 0 || selectedTab >= categories.size()) return 0;
        Category cat = categories.get(selectedTab);
        int h = 8;
        for (ModuleButton btn : cat.getModules()) {
            h += MODULE_HEIGHT;
            Module mod = btn.getModule();
            if (mod.isSettingsExpanded() && mod.hasSettings()) {
                h += mod.getSettings().size() * SETTING_HEIGHT;
            }
        }
        return h;
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h,
                             int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private Text styledText(String text) {
        if (GuiSettings.isCustomFontEnabled()) {
            return Text.literal(text).styled(
                    style -> style.withFont(CUSTOM_FONT));
        }
        return Text.literal(text);
    }

    private void drawStyledText(DrawContext context, String text, int x, int y,
                                 int color) {
        context.drawText(textRenderer, styledText(text), x, y, color, true);
    }

    // ── Input ──────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int wx = winX();
        int wy = winY();

        if (mouseX < wx || mouseX > wx + WIN_WIDTH
                || mouseY < wy || mouseY > wy + WIN_HEIGHT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (button == 0 && mouseY >= wy && mouseY < wy + TAB_HEIGHT) {
            int tabW = WIN_WIDTH / categories.size();
            int clicked = ((int) mouseX - wx) / tabW;
            if (clicked >= 0 && clicked < categories.size()) {
                selectedTab = clicked;
                scrollOffset = 0;
            }
            return true;
        }

        if (button == 0 && selectedTab >= 0 && selectedTab < categories.size()) {
            Category cat = categories.get(selectedTab);
            int contentY = wy + TAB_HEIGHT + 2;
            int y = contentY - scrollOffset + 4;

            for (ModuleButton btn : cat.getModules()) {
                Module mod = btn.getModule();

                if (mouseX >= wx + PADDING && mouseX <= wx + WIN_WIDTH - PADDING
                        && mouseY >= y && mouseY < y + MODULE_HEIGHT
                        && mouseY >= contentY) {

                    if (mod.hasSettings()
                            && mouseX >= wx + WIN_WIDTH - PADDING - 30) {
                        mod.toggleSettingsExpanded();
                    } else {
                        mod.toggle();
                    }
                    return true;
                }
                y += MODULE_HEIGHT;

                if (mod.isSettingsExpanded() && mod.hasSettings()) {
                    for (ModuleSetting setting : mod.getSettings()) {
                        if (mouseY >= y && mouseY < y + SETTING_HEIGHT
                                && mouseY >= contentY) {
                            handleSettingClick(setting, wx, y, mouseX);
                            return true;
                        }
                        y += SETTING_HEIGHT;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleSettingClick(ModuleSetting setting, int wx, int y,
                                      double mouseX) {
        if (setting.getType() == ModuleSetting.Type.TOGGLE) {
            setting.toggleBool();
        } else if (setting.getType() == ModuleSetting.Type.CHOICE) {
            setting.cycleChoice();
        } else if (setting.getType() == ModuleSetting.Type.SLIDER) {
            int left = wx + PADDING + 10;
            int right = wx + WIN_WIDTH - PADDING - 4;
            int w = right - left;
            float sliderX = left + w * 0.5f;
            float sliderW = w * 0.42f;

            float norm = (float)((mouseX - sliderX) / sliderW);
            norm = Math.max(0, Math.min(1, norm));
            setting.setFromNormalized(norm);

            draggingSlider = setting;
            draggingSliderX = sliderX;
            draggingSliderW = sliderW;
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingSlider = null;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                 double deltaX, double deltaY) {
        if (button == 0 && draggingSlider != null) {
            float norm = (float)((mouseX - draggingSliderX) / draggingSliderW);
            norm = Math.max(0, Math.min(1, norm));
            draggingSlider.setFromNormalized(norm);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                   double horizontalAmount, double verticalAmount) {
        int wx = winX();
        int wy = winY();
        if (mouseX >= wx && mouseX <= wx + WIN_WIDTH
                && mouseY >= wy && mouseY <= wy + WIN_HEIGHT) {
            scrollOffset -= (int)(verticalAmount * SCROLL_SPEED);
            int contentH = WIN_HEIGHT - TAB_HEIGHT - 2;
            int totalH = getTotalContentHeight();
            int maxScroll = Math.max(0, totalH - contentH);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
