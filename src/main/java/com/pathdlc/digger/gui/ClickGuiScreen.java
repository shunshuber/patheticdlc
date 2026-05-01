package com.pathdlc.digger.gui;

import com.pathdlc.digger.render.BlockOverlayRenderer;
import com.pathdlc.digger.render.LiquidGlassRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
    private static final int PANEL_WIDTH = 140;
    private static final int HEADER_HEIGHT = 22;
    private static final int BUTTON_HEIGHT = 18;
    private static final int SETTING_HEIGHT = 14;
    private static final float HEADER_RADIUS = 10f;
    private static final float BUTTON_RADIUS = 6f;

    private static final Identifier CUSTOM_FONT =
            Identifier.of("pathdlc_digger", "clickgui");

    private final List<Category> categories = new ArrayList<>();
    private Category draggedCategory;
    private float dragOffsetX, dragOffsetY;

    private ModuleSetting draggingSlider;
    private float draggingSliderX;
    private float draggingSliderW;

    public ClickGuiScreen() {
        super(Text.literal("ClickGUI"));
    }

    @Override
    protected void init() {
        super.init();
    }

    public void initCategories(
            Runnable appleOn, Runnable appleOff,
            Runnable digOn, Runnable digOff,
            Runnable wardenOn, Runnable wardenOff,
            Runnable clanOn, Runnable clanOff) {
        if (!categories.isEmpty()) return;

        float startX = 10;
        float startY = 30;
        float spacing = PANEL_WIDTH + 10;

        Module apple = new Module("Apple", appleOn, appleOff);
        Module dig = new Module("Dig", digOn, digOff);
        Category farm = new Category("Farm", startX, startY);
        farm.addModule(apple);
        farm.addModule(dig);
        categories.add(farm);

        Module warden = new Module("Warden", wardenOn, wardenOff);
        Module fog = new Module("Fog");
        fog.addSetting(ModuleSetting.choice("Color",
                new String[]{"White","Light Blue","Purple","Red","Green","Dark","Golden"}, 0));
        fog.addSetting(ModuleSetting.slider("Density", 0.5f, 0.0f, 1.0f, 0.1f));
        Category world = new Category("World", startX + spacing, startY);
        world.addModule(warden);
        world.addModule(fog);
        categories.add(world);

        Module clan = new Module("Clan", clanOn, clanOff);
        Module hitEffects = new Module("HitEffects");
        Module aspectRatio = new Module("AspectRatio");
        aspectRatio.addSetting(ModuleSetting.slider("FOV Scale", 1.33f, 1.0f, 2.0f, 0.01f));
        Module killAura = new Module("KillAura");
        killAura.addSetting(ModuleSetting.slider("Range", 4.0f, 2.0f, 6.0f, 0.1f));
        killAura.addSetting(ModuleSetting.toggle("Only Crit", false));
        killAura.addSetting(ModuleSetting.toggle("Attack Mobs", true));
        killAura.addSetting(ModuleSetting.toggle("Attack Players", false));
        Category combat = new Category("Combat", startX + spacing * 2, startY);
        combat.addModule(clan);
        combat.addModule(killAura);
        categories.add(combat);

        Category utility = new Category("Utility", startX + spacing * 3, startY);
        utility.addModule(hitEffects);
        utility.addModule(aspectRatio);
        categories.add(utility);

        Module blockOverlay = new Module("BlockOverlay");
        blockOverlay.addSetting(ModuleSetting.choice("Texture",
                new String[]{"Kitten","Sky","Devil"}, 0));
        Module blockEsp = new Module("BlockESP");
        blockEsp.addSetting(ModuleSetting.slider("Radius", 32f, 8f, 64f, 4f));
        Category render = new Category("Render", startX + spacing * 4, startY);
        render.addModule(blockOverlay);
        render.addModule(blockEsp);
        categories.add(render);

        for (Category cat : categories) {
            for (ModuleButton btn : cat.getModules()) {
                ModuleManager.register(btn.getModule());
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        LiquidGlassRenderer.captureAndBlur();

        for (Category cat : categories) {
            cat.updateExpandProgress();
        }

        for (Category cat : categories) {
            renderCategory(context, cat, mouseX, mouseY);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderCategory(DrawContext context, Category cat,
                                  int mouseX, int mouseY) {
        float catX = cat.x;
        float catY = cat.y;
        GuiSettings.AccentColor accent = GuiSettings.getAccentColor();

        boolean catHovered = mouseX >= catX && mouseX <= catX + PANEL_WIDTH
                && mouseY >= catY && mouseY <= catY + HEADER_HEIGHT;
        cat.hoverAmount += ((catHovered ? 1f : 0f) - cat.hoverAmount) * 0.15f;

        drawPanel(context, catX, catY, PANEL_WIDTH, HEADER_HEIGHT,
                HEADER_RADIUS, cat.hoverAmount, false);

        String name = cat.getName();
        int textWidth = textRenderer.getWidth(styledText(name));
        drawStyledText(context, name, (int) (catX + PANEL_WIDTH / 2 - textWidth / 2),
                (int) (catY + 7), 0xFFFFFFFF);

        String arrow = cat.isCollapsed() ? ">" : "v";
        drawStyledText(context, arrow, (int) (catX + PANEL_WIDTH - 14),
                (int) (catY + 7), 0xFF999999);

        float ep = cat.getExpandProgress();
        if (ep > 0.01f) {
            int lineAlpha = (int) (0x33 * ep);
            int lineColor = (lineAlpha << 24) | 0xFFFFFF;
            context.fill((int) catX + 4, (int) (catY + HEADER_HEIGHT),
                    (int) (catX + PANEL_WIDTH - 4),
                    (int) (catY + HEADER_HEIGHT + 1), lineColor);

            float btnY = catY + HEADER_HEIGHT + 1;
            for (ModuleButton btn : cat.getModules()) {
                Module mod = btn.getModule();

                boolean btnHovered = ep > 0.5f && mouseX >= catX
                        && mouseX <= catX + PANEL_WIDTH
                        && mouseY >= btnY && mouseY <= btnY + BUTTON_HEIGHT;
                btn.updateHover(btnHovered);

                drawPanel(context, catX, btnY, PANEL_WIDTH, BUTTON_HEIGHT,
                        BUTTON_RADIUS, btn.hoverAmount, mod.isEnabled());

                int textColor = mod.isEnabled() ? accent.textColor : 0xFFCCCCCC;
                drawStyledText(context, mod.getName(), (int) (catX + 8),
                        (int) (btnY + 5), textColor);

                if (mod.hasSettings()) {
                    String settingsArrow = mod.isSettingsExpanded() ? "v" : ">";
                    drawStyledText(context, settingsArrow,
                            (int) (catX + PANEL_WIDTH - 14),
                            (int) (btnY + 5), 0xFF777777);
                }

                if (mod.isEnabled()) {
                    context.fill((int) (catX + PANEL_WIDTH - 5),
                            (int) (btnY + 5),
                            (int) (catX + PANEL_WIDTH - 2),
                            (int) (btnY + BUTTON_HEIGHT - 5), accent.textColor);
                }

                btnY += BUTTON_HEIGHT;

                if (mod.isSettingsExpanded() && mod.hasSettings()) {
                    for (ModuleSetting setting : mod.getSettings()) {
                        renderSetting(context, setting, catX, btnY,
                                mouseX, mouseY, accent);
                        btnY += SETTING_HEIGHT;
                    }
                }
            }
        }
    }

    private void renderSetting(DrawContext context, ModuleSetting setting,
                                float catX, float y, int mouseX, int mouseY,
                                GuiSettings.AccentColor accent) {
        float indent = catX + 6;
        float w = PANEL_WIDTH - 12;

        context.fill((int) indent, (int) y, (int) (indent + w),
                (int) (y + SETTING_HEIGHT), 0x22000000);

        drawStyledText(context, setting.getName(),
                (int) (indent + 4), (int) (y + 3), 0xFFAAAAAA);

        if (setting.getType() == ModuleSetting.Type.SLIDER) {
            float sliderX = indent + w * 0.5f;
            float sliderW = w * 0.45f;
            float sliderY = y + SETTING_HEIGHT / 2f - 2;
            float norm = setting.getNormalized();

            context.fill((int) sliderX, (int) (sliderY + 1),
                    (int) (sliderX + sliderW), (int) (sliderY + 3), 0x44FFFFFF);
            context.fill((int) sliderX, (int) (sliderY + 1),
                    (int) (sliderX + sliderW * norm), (int) (sliderY + 3),
                    accent.textColor);

            int knobX = (int) (sliderX + sliderW * norm);
            context.fill(knobX - 2, (int) sliderY,
                    knobX + 2, (int) (sliderY + 4), 0xFFFFFFFF);

            String val = setting.getDisplayValue();
            int valW = textRenderer.getWidth(val);
            drawStyledText(context, val,
                    (int) (indent + w - valW - 2), (int) (y + 3), 0xFFDDDDDD);

        } else if (setting.getType() == ModuleSetting.Type.TOGGLE) {
            String val = setting.getBool() ? "ON" : "OFF";
            int valColor = setting.getBool() ? accent.textColor : 0xFF666666;
            int valW = textRenderer.getWidth(styledText(val));
            drawStyledText(context, val,
                    (int) (indent + w - valW - 4), (int) (y + 3), valColor);

        } else if (setting.getType() == ModuleSetting.Type.CHOICE) {
            String val = setting.getChoiceValue();
            int valW = textRenderer.getWidth(styledText(val));
            drawStyledText(context, val,
                    (int) (indent + w - valW - 4), (int) (y + 3), 0xFFDDDDDD);
        }
    }

    // ── Drawing helpers ───────────────────────────────────

    private void drawPanel(DrawContext context, float x, float y, float w,
                            float h, float radius, float hover, boolean active) {
        GuiSettings.AccentColor accent = GuiSettings.getAccentColor();
        if (LiquidGlassRenderer.isReady()) {
            if (active) {
                LiquidGlassRenderer.drawGlassPanel(context, x, y, w, h, radius,
                        hover, 0.25f, accent.r, accent.g, accent.b);
            } else {
                LiquidGlassRenderer.drawGlassPanel(context, x, y, w, h, radius,
                        hover);
            }
        } else {
            LiquidGlassRenderer.drawFallbackPanel(context, (int) x, (int) y,
                    (int) w, (int) h, active);
        }
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

    // ── Input handling ────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Category cat : categories) {
                float catX = cat.x;
                float catY = cat.y;

                if (mouseX >= catX && mouseX <= catX + PANEL_WIDTH
                        && mouseY >= catY && mouseY <= catY + HEADER_HEIGHT) {
                    if (mouseX >= catX + PANEL_WIDTH - 20) {
                        cat.toggleCollapsed();
                    } else {
                        draggedCategory = cat;
                        dragOffsetX = (float) mouseX - catX;
                        dragOffsetY = (float) mouseY - catY;
                    }
                    return true;
                }

                if (!cat.isCollapsed()) {
                    float btnY = catY + HEADER_HEIGHT + 1;
                    for (ModuleButton btn : cat.getModules()) {
                        Module mod = btn.getModule();

                        if (mouseX >= catX && mouseX <= catX + PANEL_WIDTH
                                && mouseY >= btnY && mouseY <= btnY + BUTTON_HEIGHT) {
                            if (mod.hasSettings()
                                    && mouseX >= catX + PANEL_WIDTH - 20) {
                                mod.toggleSettingsExpanded();
                            } else {
                                mod.toggle();
                            }
                            return true;
                        }
                        btnY += BUTTON_HEIGHT;

                        if (mod.isSettingsExpanded() && mod.hasSettings()) {
                            for (ModuleSetting setting : mod.getSettings()) {
                                if (mouseX >= catX && mouseX <= catX + PANEL_WIDTH
                                        && mouseY >= btnY
                                        && mouseY <= btnY + SETTING_HEIGHT) {
                                    handleSettingClick(setting, catX, btnY,
                                            mouseX, mouseY);
                                    return true;
                                }
                                btnY += SETTING_HEIGHT;
                            }
                        }
                    }
                }
            }
        }

        if (button == 1) {
            for (Category cat : categories) {
                if (mouseX >= cat.x && mouseX <= cat.x + PANEL_WIDTH
                        && mouseY >= cat.y
                        && mouseY <= cat.y + HEADER_HEIGHT) {
                    cat.toggleCollapsed();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleSettingClick(ModuleSetting setting, float catX,
                                      float y, double mouseX, double mouseY) {
        if (setting.getType() == ModuleSetting.Type.TOGGLE) {
            setting.toggleBool();
        } else if (setting.getType() == ModuleSetting.Type.CHOICE) {
            setting.cycleChoice();
        } else if (setting.getType() == ModuleSetting.Type.SLIDER) {
            float indent = catX + 6;
            float w = PANEL_WIDTH - 12;
            float sliderX = indent + w * 0.5f;
            float sliderW = w * 0.45f;

            float norm = (float) ((mouseX - sliderX) / sliderW);
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
            draggedCategory = null;
            draggingSlider = null;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                 double deltaX, double deltaY) {
        if (button == 0 && draggedCategory != null) {
            draggedCategory.x = (float) mouseX - dragOffsetX;
            draggedCategory.y = (float) mouseY - dragOffsetY;
            return true;
        }
        if (button == 0 && draggingSlider != null) {
            float norm = (float) ((mouseX - draggingSliderX) / draggingSliderW);
            norm = Math.max(0, Math.min(1, norm));
            draggingSlider.setFromNormalized(norm);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
