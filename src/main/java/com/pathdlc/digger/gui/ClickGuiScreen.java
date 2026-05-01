package com.pathdlc.digger.gui;

import com.pathdlc.digger.render.LiquidGlassRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
    private static final int WIN_WIDTH = 380;
    private static final int WIN_HEIGHT = 280;
    private static final int HEADER_H = 32;
    private static final int TAB_H = 26;
    private static final int MODULE_H = 24;
    private static final int SETTING_H = 18;
    private static final int PAD = 10;
    private static final int SCROLL_SPEED = 12;

    private static final Identifier CUSTOM_FONT =
            Identifier.of("pathdlc_digger", "clickgui");

    private final List<Category> categories = new ArrayList<>();
    private int selectedTab = 0;
    private float openProgress = 0f;
    private int scrollOffset = 0;
    private float[] tabHover;

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
            Runnable clanOn, Runnable clanOff,
            Runnable autoMineOn, Runnable autoMineOff,
            Runnable baseFinderOn, Runnable baseFinderOff,
            Runnable autoFarmOn, Runnable autoFarmOff,
            Runnable autoFishOn, Runnable autoFishOff,
            Runnable autoCraftOn, Runnable autoCraftOff,
            Runnable autoSellOn, Runnable autoSellOff,
            Runnable autoBuyOn, Runnable autoBuyOff) {
        if (!categories.isEmpty()) return;

        Module apple = new Module("Apple", appleOn, appleOff);
        Module dig = new Module("Dig", digOn, digOff);
        Module autoMine = new Module("AutoMine", autoMineOn, autoMineOff);
        autoMine.addSetting(ModuleSetting.choice("Ore",
                new String[]{"Diamond","Emerald","Gold","Iron","Netherite","All Ores"}, 0));
        Module autoFarm = new Module("AutoFarm", autoFarmOn, autoFarmOff);
        autoFarm.addSetting(ModuleSetting.slider("Radius", 4f, 2f, 8f, 1f));
        Category farm = new Category("Farm", 0, 0);
        farm.addModule(apple);
        farm.addModule(dig);
        farm.addModule(autoMine);
        farm.addModule(autoFarm);
        categories.add(farm);

        Module warden = new Module("Warden", wardenOn, wardenOff);
        Module baseFinder = new Module("BaseFinder", baseFinderOn, baseFinderOff);
        baseFinder.addSetting(ModuleSetting.slider("Radius", 64f, 16f, 128f, 16f));
        Module fog = new Module("Fog");
        fog.addSetting(ModuleSetting.choice("Color",
                new String[]{"White","Light Blue","Purple","Red","Green","Dark","Golden"}, 0));
        fog.addSetting(ModuleSetting.slider("Density", 0.5f, 0.0f, 1.0f, 0.1f));
        Category world = new Category("World", 0, 0);
        world.addModule(warden);
        world.addModule(baseFinder);
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
        Module autoFish = new Module("AutoFish", autoFishOn, autoFishOff);
        Category utility = new Category("Utility", 0, 0);
        utility.addModule(hitEffects);
        utility.addModule(aspectRatio);
        utility.addModule(autoFish);
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

        Module autoCraft = new Module("AutoCraft", autoCraftOn, autoCraftOff);
        autoCraft.addSetting(ModuleSetting.choice("Recipe",
                new String[]{"Planks","Sticks","Torches","Bread","Golden Apple"}, 0));
        Module autoSell = new Module("AutoSell", autoSellOn, autoSellOff);
        autoSell.addSetting(ModuleSetting.choice("Mode",
                new String[]{"Buyer","Junk Only","Sell All"}, 0));
        autoSell.addSetting(ModuleSetting.slider("Interval", 30f, 10f, 120f, 5f));
        Module autoBuy = new Module("AutoBuy", autoBuyOn, autoBuyOff);
        autoBuy.addSetting(ModuleSetting.choice("Mode",
                new String[]{"All Items","Skip Back/Close","Buy Buttons Only"}, 0));
        autoBuy.addSetting(ModuleSetting.slider("Delay", 2f, 1f, 10f, 1f));
        autoBuy.addSetting(ModuleSetting.toggle("AutoClose", false));
        Category funtime = new Category("FunTime", 0, 0);
        funtime.addModule(autoCraft);
        funtime.addModule(autoSell);
        funtime.addModule(autoBuy);
        categories.add(funtime);

        tabHover = new float[categories.size()];

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
        openProgress += (1f - openProgress) * 0.18f;
        if (openProgress > 0.99f) openProgress = 1f;
        if (openProgress < 0.01f) return;

        int wx = winX();
        int wy = winY();

        LiquidGlassRenderer.captureAndBlur();

        int dimAlpha = (int)(openProgress * 0x99);
        context.fill(0, 0, width, height, dimAlpha << 24);

        if (LiquidGlassRenderer.isReady()) {
            LiquidGlassRenderer.drawGlassPanel(context,
                    wx, wy, WIN_WIDTH, WIN_HEIGHT, 10f, 0f);
        } else {
            context.fill(wx, wy, wx + WIN_WIDTH, wy + WIN_HEIGHT, 0xEE0C0C16);
        }

        renderHeader(context, wx, wy);
        renderTabs(context, wx, wy + HEADER_H, mouseX, mouseY);

        int contentY = wy + HEADER_H + TAB_H + 1;
        int contentH = WIN_HEIGHT - HEADER_H - TAB_H - 1;

        context.enableScissor(wx, contentY, wx + WIN_WIDTH, contentY + contentH);
        renderModules(context, wx, contentY, mouseX, mouseY);
        context.disableScissor();

        renderScrollbar(context, wx, contentY, contentH);

        drawOutline(context, wx, wy, WIN_WIDTH, WIN_HEIGHT,
                GuiSettings.getAccentColor());

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderHeader(DrawContext context, int wx, int wy) {
        GuiSettings.AccentColor accent = GuiSettings.getAccentColor();

        int accentArgb = 0xFF000000 | ((int)(accent.r * 255) << 16)
                | ((int)(accent.g * 255) << 8) | (int)(accent.b * 255);
        int gradTop = (0x44 << 24) | (accentArgb & 0x00FFFFFF);
        int gradBot = 0x00000000;

        for (int row = 0; row < HEADER_H; row++) {
            float t = (float) row / HEADER_H;
            int a = (int)(0x44 * (1f - t));
            int lineColor = (a << 24) | (accentArgb & 0x00FFFFFF);
            context.fill(wx, wy + row, wx + WIN_WIDTH, wy + row + 1, lineColor);
        }

        String title = "PathDLC";
        int titleW = textRenderer.getWidth(styledText(title));
        drawStyledText(context, title,
                wx + PAD + 2, wy + HEADER_H / 2 - 4, 0xFFFFFFFF);

        int activeCount = 0;
        for (Category cat : categories)
            for (ModuleButton btn : cat.getModules())
                if (btn.getModule().isEnabled()) activeCount++;

        String info = activeCount + " active";
        int infoW = textRenderer.getWidth(styledText(info));
        drawStyledText(context, info,
                wx + WIN_WIDTH - PAD - infoW, wy + HEADER_H / 2 - 4,
                0x88FFFFFF & accent.textColor);

        context.fill(wx + 1, wy + HEADER_H - 1,
                wx + WIN_WIDTH - 1, wy + HEADER_H, 0x22FFFFFF);
    }

    private void renderTabs(DrawContext context, int wx, int tabY,
                             int mouseX, int mouseY) {
        GuiSettings.AccentColor accent = GuiSettings.getAccentColor();
        int tabCount = categories.size();
        if (tabHover == null || tabHover.length != tabCount)
            tabHover = new float[tabCount];

        int tabW = WIN_WIDTH / tabCount;

        context.fill(wx, tabY, wx + WIN_WIDTH, tabY + TAB_H, 0x22000000);

        for (int i = 0; i < tabCount; i++) {
            int tx = wx + i * tabW;
            boolean hovered = mouseX >= tx && mouseX < tx + tabW
                    && mouseY >= tabY && mouseY < tabY + TAB_H;
            boolean active = i == selectedTab;

            float target = (active || hovered) ? 1f : 0f;
            tabHover[i] += (target - tabHover[i]) * 0.2f;

            if (tabHover[i] > 0.01f && !active) {
                int hAlpha = (int)(tabHover[i] * 0x18);
                context.fill(tx, tabY, tx + tabW, tabY + TAB_H,
                        (hAlpha << 24) | 0xFFFFFF);
            }

            String name = categories.get(i).getName();
            int textW = textRenderer.getWidth(styledText(name));
            int textX = tx + tabW / 2 - textW / 2;
            int textY = tabY + TAB_H / 2 - 4;

            int color;
            if (active) {
                color = accent.textColor;
            } else if (hovered) {
                color = 0xFFDDDDDD;
            } else {
                color = 0xFF777777;
            }
            drawStyledText(context, name, textX, textY, color);

            if (active) {
                int lineW = Math.min(tabW - 12, textW + 10);
                int lineX = tx + tabW / 2 - lineW / 2;
                context.fill(lineX, tabY + TAB_H - 2,
                        lineX + lineW, tabY + TAB_H, accent.textColor);

                int glowA = 0x22;
                context.fill(lineX - 2, tabY + TAB_H - 4,
                        lineX + lineW + 2, tabY + TAB_H,
                        (glowA << 24) | (accent.textColor & 0x00FFFFFF));
            }
        }

        context.fill(wx, tabY + TAB_H, wx + WIN_WIDTH,
                tabY + TAB_H + 1, 0x22FFFFFF);
    }

    private void renderModules(DrawContext context, int wx, int contentY,
                                int mouseX, int mouseY) {
        if (selectedTab < 0 || selectedTab >= categories.size()) return;
        Category cat = categories.get(selectedTab);
        GuiSettings.AccentColor accent = GuiSettings.getAccentColor();

        int y = contentY - scrollOffset + 4;

        for (ModuleButton btn : cat.getModules()) {
            Module mod = btn.getModule();

            boolean hovered = mouseX >= wx + PAD
                    && mouseX <= wx + WIN_WIDTH - PAD
                    && mouseY >= y && mouseY <= y + MODULE_H
                    && mouseY >= contentY;
            btn.updateHover(hovered);

            if (mod.isEnabled()) {
                context.fill(wx + 1, y, wx + 3, y + MODULE_H,
                        accent.textColor);

                int bgA = (int)(0x18 + btn.hoverAmount * 0x10);
                context.fill(wx + PAD, y, wx + WIN_WIDTH - PAD, y + MODULE_H,
                        (bgA << 24) | (accent.textColor & 0x00FFFFFF));
            } else if (btn.hoverAmount > 0.01f) {
                int hAlpha = (int)(btn.hoverAmount * 0x15);
                context.fill(wx + PAD, y, wx + WIN_WIDTH - PAD, y + MODULE_H,
                        (hAlpha << 24) | 0xFFFFFF);
            }

            int nameColor = mod.isEnabled() ? 0xFFFFFFFF : 0xFFAAAAAA;
            drawStyledText(context, mod.getName(), wx + PAD + 8, y + 8, nameColor);

            int toggleX = wx + WIN_WIDTH - PAD - 32;
            int toggleY = y + MODULE_H / 2 - 5;
            renderToggleSwitch(context, toggleX, toggleY, mod.isEnabled(), accent);

            if (mod.hasSettings()) {
                String arrow = mod.isSettingsExpanded() ? "v" : ">";
                drawStyledText(context, arrow,
                        wx + WIN_WIDTH - PAD - 48, y + 8, 0xFF555555);
            }

            y += MODULE_H;

            if (mod.isSettingsExpanded() && mod.hasSettings()) {
                for (ModuleSetting setting : mod.getSettings()) {
                    renderSetting(context, setting, wx, y, mouseX, mouseY,
                            accent, contentY);
                    y += SETTING_H;
                }
            }

            context.fill(wx + PAD + 4, y - 1,
                    wx + WIN_WIDTH - PAD - 4, y, 0x0DFFFFFF);
        }
    }

    private void renderToggleSwitch(DrawContext context, int x, int y,
                                      boolean on, GuiSettings.AccentColor accent) {
        int w = 20, h = 10;

        int bgColor = on ? ((0xAA << 24) | (accent.textColor & 0x00FFFFFF))
                          : 0x44333333;
        context.fill(x, y, x + w, y + h, bgColor);
        context.fill(x, y, x + w, y + 1, 0x22FFFFFF);
        context.fill(x, y + h - 1, x + w, y + h, 0x22000000);

        int knobX = on ? x + w - 6 : x + 2;
        context.fill(knobX, y + 2, knobX + 4, y + h - 2, 0xFFFFFFFF);

        if (on) {
            context.fill(knobX - 1, y + 1, knobX + 5, y + h - 1,
                    0x33FFFFFF);
        }
    }

    private void renderSetting(DrawContext context, ModuleSetting setting,
                                int wx, int y, int mouseX, int mouseY,
                                GuiSettings.AccentColor accent, int contentY) {
        int left = wx + PAD + 14;
        int right = wx + WIN_WIDTH - PAD - 6;
        int w = right - left;

        context.fill(left, y, right, y + SETTING_H, 0x0CFFFFFF);

        context.fill(left, y, left + 1, y + SETTING_H, 0x15FFFFFF);

        drawStyledText(context, setting.getName(), left + 6, y + 5, 0xFF888888);

        if (setting.getType() == ModuleSetting.Type.SLIDER) {
            float sliderX = left + w * 0.48f;
            float sliderW = w * 0.44f;
            float sliderY = y + SETTING_H / 2f - 1;
            float norm = setting.getNormalized();

            context.fill((int) sliderX, (int) sliderY,
                    (int)(sliderX + sliderW), (int)(sliderY + 2), 0x33FFFFFF);

            int fillColor = accent.textColor;
            context.fill((int) sliderX, (int) sliderY,
                    (int)(sliderX + sliderW * norm), (int)(sliderY + 2),
                    fillColor);

            int knobX = (int)(sliderX + sliderW * norm);
            context.fill(knobX - 3, (int)(sliderY - 2),
                    knobX + 3, (int)(sliderY + 4), 0xFFFFFFFF);
            context.fill(knobX - 2, (int)(sliderY - 1),
                    knobX + 2, (int)(sliderY + 3), accent.textColor);

            String val = setting.getDisplayValue();
            int valW = textRenderer.getWidth(styledText(val));
            drawStyledText(context, val, right - valW - 4, y + 5, 0xFFCCCCCC);

        } else if (setting.getType() == ModuleSetting.Type.TOGGLE) {
            int toggleX = right - 24;
            int toggleY = y + SETTING_H / 2 - 5;
            renderToggleSwitch(context, toggleX, toggleY,
                    setting.getBool(), accent);

        } else if (setting.getType() == ModuleSetting.Type.CHOICE) {
            String val = setting.getChoiceValue();
            int valW = textRenderer.getWidth(styledText(val));
            int valX = right - valW - 6;

            context.fill(valX - 4, y + 3, right - 2, y + SETTING_H - 3,
                    0x18FFFFFF);
            drawStyledText(context, val, valX, y + 5, 0xFFCCCCCC);
        }
    }

    private void renderScrollbar(DrawContext context, int wx, int contentY,
                                   int contentH) {
        int totalH = getTotalContentHeight();
        if (totalH <= contentH) return;

        int barX = wx + WIN_WIDTH - 4;
        float ratio = (float) contentH / totalH;
        int thumbH = Math.max(12, (int)(contentH * ratio));
        int thumbY = contentY + (int)((float) scrollOffset / (totalH - contentH)
                * (contentH - thumbH));

        context.fill(barX, contentY, barX + 2, contentY + contentH, 0x11FFFFFF);
        context.fill(barX, thumbY, barX + 2, thumbY + thumbH,
                0x55FFFFFF);
    }

    private int getTotalContentHeight() {
        if (selectedTab < 0 || selectedTab >= categories.size()) return 0;
        Category cat = categories.get(selectedTab);
        int h = 8;
        for (ModuleButton btn : cat.getModules()) {
            h += MODULE_H;
            Module mod = btn.getModule();
            if (mod.isSettingsExpanded() && mod.hasSettings()) {
                h += mod.getSettings().size() * SETTING_H;
            }
        }
        return h;
    }

    private void drawOutline(DrawContext ctx, int x, int y, int w, int h,
                              GuiSettings.AccentColor accent) {
        int color = (0x33 << 24) | (accent.textColor & 0x00FFFFFF);
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

        int tabY = wy + HEADER_H;
        if (button == 0 && mouseY >= tabY && mouseY < tabY + TAB_H) {
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
            int contentY = wy + HEADER_H + TAB_H + 1;
            int y = contentY - scrollOffset + 4;

            for (ModuleButton btn : cat.getModules()) {
                Module mod = btn.getModule();

                if (mouseX >= wx + PAD && mouseX <= wx + WIN_WIDTH - PAD
                        && mouseY >= y && mouseY < y + MODULE_H
                        && mouseY >= contentY) {

                    int toggleX = wx + WIN_WIDTH - PAD - 32;
                    if (mouseX >= toggleX) {
                        mod.toggle();
                    } else if (mod.hasSettings()
                            && mouseX >= wx + WIN_WIDTH - PAD - 52
                            && mouseX < toggleX) {
                        mod.toggleSettingsExpanded();
                    } else {
                        mod.toggle();
                    }
                    return true;
                }
                y += MODULE_H;

                if (mod.isSettingsExpanded() && mod.hasSettings()) {
                    for (ModuleSetting setting : mod.getSettings()) {
                        if (mouseY >= y && mouseY < y + SETTING_H
                                && mouseY >= contentY) {
                            handleSettingClick(setting, wx, y, mouseX);
                            return true;
                        }
                        y += SETTING_H;
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
            int left = wx + PAD + 14;
            int right = wx + WIN_WIDTH - PAD - 6;
            int w = right - left;
            float sliderX = left + w * 0.48f;
            float sliderW = w * 0.44f;

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
        if (button == 0) draggingSlider = null;
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
            int contentH = WIN_HEIGHT - HEADER_H - TAB_H - 1;
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
