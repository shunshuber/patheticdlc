package com.pathdlc.digger.gui;

import com.pathdlc.digger.render.LiquidGlassRenderer;
import com.pathdlc.digger.render.PerformanceSettings;
import com.pathdlc.digger.render.RoundedRectRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClickGuiScreen extends Screen {
    private static final int COL_WIDTH = 130;
    private static final int COL_GAP = 6;
    private static final int HEADER_H = 26;
    private static final int MODULE_H = 18;
    private static final int SETTING_H = 16;
    private static final int PAD = 6;
    private static final int CORNER_R = 8;
    private static final int SCROLL_SPEED = 10;

    private static final Identifier CUSTOM_FONT =
            Identifier.of("pathdlc_digger", "clickgui");

    private final List<Category> categories = new ArrayList<>();
    private float openProgress = 0f;
    private final Map<String, Integer> scrollOffsets = new HashMap<>();
    private final Map<String, Float> colDragY = new HashMap<>();

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
        autoBuy.addSetting(ModuleSetting.choice("Item",
                new String[]{"Diamond","Emerald","Netherite","God Apple","Elytra","Totem","Shulker","Beacon"}, 0));
        autoBuy.addSetting(ModuleSetting.slider("Max Price", 10000f, 100f, 100000f, 500f));
        autoBuy.addSetting(ModuleSetting.slider("Interval", 10f, 3f, 60f, 1f));
        autoBuy.addSetting(ModuleSetting.choice("Search", new String[]{"Browse /ah","Search /ah search"}, 1));
        Category funtime = new Category("FunTime", 0, 0);
        funtime.addModule(autoCraft);
        funtime.addModule(autoSell);
        funtime.addModule(autoBuy);
        categories.add(funtime);

        for (Category cat : categories) {
            for (ModuleButton btn : cat.getModules()) {
                ModuleManager.register(btn.getModule());
            }
        }
    }

    private int totalWidth() {
        return categories.size() * COL_WIDTH + (categories.size() - 1) * COL_GAP;
    }

    private int startX() {
        return (width - totalWidth()) / 2;
    }

    private int startY() {
        return 40;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        openProgress += (1f - openProgress) * 0.15f;
        if (openProgress > 0.99f) openProgress = 1f;
        if (openProgress < 0.01f) return;

        LiquidGlassRenderer.captureAndBlur();

        int dimAlpha = (int)(openProgress * 0x66);
        context.fill(0, 0, width, height, dimAlpha << 24);

        GuiSettings.AccentColor accent = GuiSettings.getAccentColor();

        int sx = startX();
        int sy = startY();

        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            int cx = sx + i * (COL_WIDTH + COL_GAP);
            int colH = getColumnHeight(cat);

            float slideOffset = (1f - openProgress) * (30 + i * 12);
            int cy = sy + (int) slideOffset;

            float alpha = openProgress;

            renderColumn(context, cat, cx, cy, colH, mouseX, mouseY, accent, alpha);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderColumn(DrawContext context, Category cat,
                               int cx, int cy, int colH,
                               int mouseX, int mouseY,
                               GuiSettings.AccentColor accent, float alpha) {
        if (PerformanceSettings.useGlassEffect() && LiquidGlassRenderer.isReady()) {
            LiquidGlassRenderer.drawGlassPanel(context,
                    cx, cy, COL_WIDTH, colH, CORNER_R, 0f,
                    0.08f, accent.r, accent.g, accent.b);
        } else {
            RoundedRectRenderer.draw(context, cx, cy, COL_WIDTH, colH,
                    CORNER_R, 0xCC12131A);
        }

        RoundedRectRenderer.draw(context, cx, cy, COL_WIDTH, HEADER_H,
                CORNER_R, CORNER_R, 0, 0,
                (int)(0x55 * alpha) << 24 | (accent.textColor & 0x00FFFFFF));

        String name = cat.getName();
        int textW = textRenderer.getWidth(styledText(name));
        int textX = cx + COL_WIDTH / 2 - textW / 2;
        drawStyledText(context, name, textX, cy + HEADER_H / 2 - 4,
                0xFFFFFFFF);

        int scroll = scrollOffsets.getOrDefault(cat.getName(), 0);
        int contentY = cy + HEADER_H + 2;
        int maxContentH = height - contentY - 20;

        context.enableScissor(cx, contentY, cx + COL_WIDTH, contentY + maxContentH);

        int y = contentY - scroll;

        for (ModuleButton btn : cat.getModules()) {
            Module mod = btn.getModule();

            boolean hovered = mouseX >= cx + 2 && mouseX <= cx + COL_WIDTH - 2
                    && mouseY >= y && mouseY < y + MODULE_H
                    && mouseY >= contentY && mouseY < contentY + maxContentH;
            btn.updateHover(hovered);

            if (mod.isEnabled()) {
                RoundedRectRenderer.draw(context,
                        cx + 3, y + 1, COL_WIDTH - 6, MODULE_H - 2,
                        4, (int)(0x44 * alpha) << 24 | (accent.textColor & 0x00FFFFFF));
            } else if (btn.hoverAmount > 0.01f) {
                int hAlpha = (int)(btn.hoverAmount * 0x20 * alpha);
                RoundedRectRenderer.draw(context,
                        cx + 3, y + 1, COL_WIDTH - 6, MODULE_H - 2,
                        4, (hAlpha << 24) | 0xFFFFFF);
            }

            int nameColor = mod.isEnabled() ? 0xFFFFFFFF : 0xFFAAAAAA;
            drawStyledText(context, mod.getName(), cx + PAD + 2, y + 5, nameColor);

            if (mod.hasSettings()) {
                String arrow = mod.isSettingsExpanded() ? "v" : ">";
                int arrowColor = mod.isSettingsExpanded() ? accent.textColor : 0xFF555555;
                drawStyledText(context, arrow,
                        cx + COL_WIDTH - PAD - 8, y + 5, arrowColor);
            }

            y += MODULE_H;

            if (mod.isSettingsExpanded() && mod.hasSettings()) {
                for (ModuleSetting setting : mod.getSettings()) {
                    renderSetting(context, setting, cx, y, mouseX, mouseY,
                            accent, alpha, contentY, maxContentH);
                    y += SETTING_H;
                }

                y += 2;
            }
        }

        context.disableScissor();
    }

    private void renderSetting(DrawContext context, ModuleSetting setting,
                                int cx, int y, int mouseX, int mouseY,
                                GuiSettings.AccentColor accent, float alpha,
                                int contentY, int maxContentH) {
        int left = cx + PAD + 4;
        int right = cx + COL_WIDTH - PAD - 2;
        int w = right - left;

        RoundedRectRenderer.draw(context, left - 1, y, w + 2, SETTING_H,
                3, (int)(0x18 * alpha) << 24);

        if (setting.getType() == ModuleSetting.Type.SLIDER) {
            drawStyledText(context, setting.getName(), left + 2, y + 1,
                    0xFF777777);

            float sliderX = left + 2;
            float sliderW = w - 4;
            float sliderY = y + SETTING_H - 5;
            float norm = setting.getNormalized();

            RoundedRectRenderer.draw(context, (int)sliderX, (int)sliderY,
                    (int)sliderW, 3, 2, 0x33FFFFFF);

            int fillW = (int)(sliderW * norm);
            if (fillW > 0) {
                RoundedRectRenderer.draw(context, (int)sliderX, (int)sliderY,
                        fillW, 3, 2, accent.textColor);
            }

            int knobCx = (int)(sliderX + sliderW * norm);
            RoundedRectRenderer.draw(context, knobCx - 3, (int)sliderY - 2,
                    6, 7, 3, 0xFFFFFFFF);

            String val = setting.getDisplayValue();
            int valW = textRenderer.getWidth(styledText(val));
            drawStyledText(context, val, right - valW - 1, y + 1, 0xFFBBBBBB);

        } else if (setting.getType() == ModuleSetting.Type.TOGGLE) {
            drawStyledText(context, setting.getName(), left + 2, y + 4,
                    0xFF777777);

            int sw = 16, sh = 8;
            int tx = right - sw - 2;
            int ty = y + SETTING_H / 2 - sh / 2;

            int bgColor = setting.getBool()
                    ? ((0xBB << 24) | (accent.textColor & 0x00FFFFFF))
                    : 0x44444444;
            RoundedRectRenderer.draw(context, tx, ty, sw, sh, sh / 2, bgColor);

            int knobD = sh - 2;
            int knobX = setting.getBool() ? tx + sw - knobD - 1 : tx + 1;
            RoundedRectRenderer.draw(context, knobX, ty + 1, knobD, knobD,
                    knobD / 2, 0xFFFFFFFF);

        } else if (setting.getType() == ModuleSetting.Type.CHOICE) {
            drawStyledText(context, setting.getName(), left + 2, y + 4,
                    0xFF777777);

            String val = setting.getChoiceValue();
            int valW = textRenderer.getWidth(styledText(val));
            int valX = right - valW - 4;

            RoundedRectRenderer.draw(context, valX - 3, y + 2,
                    valW + 6, SETTING_H - 4, 3, 0x22FFFFFF);
            drawStyledText(context, val, valX, y + 4, 0xFFCCCCCC);
        }
    }

    private int getColumnHeight(Category cat) {
        int h = HEADER_H + 4;
        for (ModuleButton btn : cat.getModules()) {
            h += MODULE_H;
            Module mod = btn.getModule();
            if (mod.isSettingsExpanded() && mod.hasSettings()) {
                h += mod.getSettings().size() * SETTING_H + 2;
            }
        }
        return h + 4;
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
        int sx = startX();
        int sy = startY();

        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            int cx = sx + i * (COL_WIDTH + COL_GAP);
            float slideOffset = (1f - openProgress) * (30 + i * 12);
            int cy = sy + (int) slideOffset;
            int colH = getColumnHeight(cat);

            if (mouseX < cx || mouseX > cx + COL_WIDTH
                    || mouseY < cy || mouseY > cy + colH) {
                continue;
            }

            if (mouseY < cy + HEADER_H) {
                return true;
            }

            int scroll = scrollOffsets.getOrDefault(cat.getName(), 0);
            int contentY = cy + HEADER_H + 2;
            int y = contentY - scroll;

            for (ModuleButton btn : cat.getModules()) {
                Module mod = btn.getModule();

                if (mouseY >= y && mouseY < y + MODULE_H && mouseY >= contentY) {
                    if (button == 0) {
                        if (mod.hasSettings()
                                && mouseX >= cx + COL_WIDTH - PAD - 14) {
                            mod.toggleSettingsExpanded();
                        } else {
                            mod.toggle();
                        }
                    } else if (button == 1 && mod.hasSettings()) {
                        mod.toggleSettingsExpanded();
                    }
                    return true;
                }
                y += MODULE_H;

                if (mod.isSettingsExpanded() && mod.hasSettings()) {
                    for (ModuleSetting setting : mod.getSettings()) {
                        if (mouseY >= y && mouseY < y + SETTING_H
                                && mouseY >= contentY) {
                            handleSettingClick(setting, cx, y, mouseX);
                            return true;
                        }
                        y += SETTING_H;
                    }
                    y += 2;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleSettingClick(ModuleSetting setting, int cx, int y,
                                      double mouseX) {
        if (setting.getType() == ModuleSetting.Type.TOGGLE) {
            setting.toggleBool();
        } else if (setting.getType() == ModuleSetting.Type.CHOICE) {
            setting.cycleChoice();
        } else if (setting.getType() == ModuleSetting.Type.SLIDER) {
            int left = cx + PAD + 4;
            int right = cx + COL_WIDTH - PAD - 2;
            int w = right - left;
            float sliderX = left + 2;
            float sliderW = w - 4;

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
        int sx = startX();
        int sy = startY();

        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            int cx = sx + i * (COL_WIDTH + COL_GAP);
            float slideOffset = (1f - openProgress) * (30 + i * 12);
            int cy = sy + (int) slideOffset;
            int colH = getColumnHeight(cat);

            if (mouseX >= cx && mouseX <= cx + COL_WIDTH
                    && mouseY >= cy && mouseY <= cy + colH) {
                int scroll = scrollOffsets.getOrDefault(cat.getName(), 0);
                scroll -= (int)(verticalAmount * SCROLL_SPEED);

                int contentH = height - cy - HEADER_H - 22;
                int totalH = colH - HEADER_H - 4;
                int maxScroll = Math.max(0, totalH - contentH);
                scroll = Math.max(0, Math.min(maxScroll, scroll));

                scrollOffsets.put(cat.getName(), scroll);
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
