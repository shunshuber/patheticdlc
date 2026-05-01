package com.pathdlc.digger.gui;

import com.pathdlc.digger.render.LiquidGlassRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
    private static final int PANEL_WIDTH = 120;
    private static final int HEADER_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 18;
    private static final float HEADER_RADIUS = 8f;
    private static final float BUTTON_RADIUS = 6f;

    private static final Identifier CUSTOM_FONT =
            Identifier.of("pathdlc_digger", "clickgui");

    private static final int SETTINGS_WIDTH = 160;
    private static final int SETTINGS_ROW = 18;

    private final List<Category> categories = new ArrayList<>();
    private Category draggedCategory;
    private boolean draggingSinglePanel;
    private float dragOffsetX, dragOffsetY;

    private float singlePanelX = 10;
    private float singlePanelY = 30;

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
        float spacing = 130;

        Module apple = new Module("Apple", appleOn, appleOff);
        Module dig = new Module("Dig", digOn, digOff);
        Category farm = new Category("Farm", startX, startY);
        farm.addModule(apple);
        farm.addModule(dig);
        categories.add(farm);

        Module warden = new Module("Warden", wardenOn, wardenOff);
        Module fog = new Module("Fog");
        Category world = new Category("World", startX + spacing, startY);
        world.addModule(warden);
        world.addModule(fog);
        categories.add(world);

        Module clan = new Module("Clan", clanOn, clanOff);
        Module hitEffects = new Module("HitEffects");
        Module aspectRatio = new Module("AspectRatio");
        Category utility = new Category("Utility", startX + spacing * 2, startY);
        utility.addModule(clan);
        utility.addModule(hitEffects);
        utility.addModule(aspectRatio);
        categories.add(utility);

        Module blockOverlay = new Module("BlockOverlay");
        Module blockEsp = new Module("BlockESP");
        Category render = new Category("Render", startX + spacing * 3, startY);
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

        if (GuiSettings.getLayout() == GuiSettings.Layout.COLUMNS) {
            renderColumnsLayout(context, mouseX, mouseY);
        } else {
            renderSingleLayout(context, mouseX, mouseY);
        }

        renderSettingsPanel(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    // ── Columns layout (original) ─────────────────────────

    private void renderColumnsLayout(DrawContext context, int mouseX, int mouseY) {
        for (Category cat : categories) {
            renderCategoryPanels(context, cat, mouseX, mouseY);
        }
        for (Category cat : categories) {
            renderCategoryText(context, cat);
        }
    }

    private void renderCategoryPanels(DrawContext context, Category cat,
                                       int mouseX, int mouseY) {
        float catX = cat.x;
        float catY = cat.y;

        boolean catHovered = mouseX >= catX && mouseX <= catX + PANEL_WIDTH
                && mouseY >= catY && mouseY <= catY + HEADER_HEIGHT;
        cat.hoverAmount += ((catHovered ? 1f : 0f) - cat.hoverAmount) * 0.15f;

        drawPanel(context, catX, catY, PANEL_WIDTH, HEADER_HEIGHT,
                HEADER_RADIUS, cat.hoverAmount, false);

        if (!cat.isCollapsed()) {
            int index = 0;
            for (ModuleButton btn : cat.getModules()) {
                float btnY = catY + HEADER_HEIGHT + 1 + index * BUTTON_HEIGHT;
                boolean btnHovered = mouseX >= catX && mouseX <= catX + PANEL_WIDTH
                        && mouseY >= btnY && mouseY <= btnY + BUTTON_HEIGHT;
                btn.updateHover(btnHovered);

                drawPanel(context, catX, btnY, PANEL_WIDTH, BUTTON_HEIGHT,
                        BUTTON_RADIUS, btn.hoverAmount, btn.getModule().isEnabled());
                index++;
            }
        }
    }

    private void renderCategoryText(DrawContext context, Category cat) {
        float catX = cat.x;
        float catY = cat.y;
        GuiSettings.AccentColor accent = GuiSettings.getAccentColor();

        String name = cat.getName();
        int textWidth = textRenderer.getWidth(styledText(name));
        drawStyledText(context, name, (int) (catX + 60 - textWidth / 2),
                (int) (catY + 6), 0xFFFFFFFF);

        String arrow = cat.isCollapsed() ? ">" : "V";
        drawStyledText(context, arrow, (int) (catX + 105), (int) (catY + 6),
                0xFFFFFFFF);

        if (!cat.isCollapsed()) {
            context.fill((int) catX, (int) (catY + HEADER_HEIGHT),
                    (int) (catX + PANEL_WIDTH), (int) (catY + HEADER_HEIGHT + 1),
                    0x33FFFFFF);

            int index = 0;
            for (ModuleButton btn : cat.getModules()) {
                float btnY = catY + HEADER_HEIGHT + 1 + index * BUTTON_HEIGHT;
                Module mod = btn.getModule();
                int textColor = mod.isEnabled() ? accent.textColor : 0xFFCCCCCC;
                drawStyledText(context, mod.getName(), (int) (catX + 8),
                        (int) (btnY + 5), textColor);
                index++;
            }
        }
    }

    // ── Single panel layout ───────────────────────────────

    private void renderSingleLayout(DrawContext context, int mouseX, int mouseY) {
        int totalModules = 0;
        for (Category cat : categories) {
            totalModules += 1 + cat.getModules().size();
        }
        totalModules += categories.size() - 1;

        int panelW = 200;
        int panelH = 24 + totalModules * BUTTON_HEIGHT;
        float px = singlePanelX;
        float py = singlePanelY;

        drawPanel(context, px, py, panelW, panelH, 10f, 0f, false);

        drawStyledText(context, "PathDLC Modules", (int) (px + 10),
                (int) (py + 7), 0xFFFFFFFF);

        context.fill((int) px, (int) (py + 22), (int) (px + panelW),
                (int) (py + 23), 0x33FFFFFF);

        GuiSettings.AccentColor accent = GuiSettings.getAccentColor();
        float rowY = py + 24;

        for (int c = 0; c < categories.size(); c++) {
            Category cat = categories.get(c);

            if (c > 0) {
                context.fill((int) (px + 8), (int) rowY + 8,
                        (int) (px + panelW - 8), (int) rowY + 9, 0x22FFFFFF);
                rowY += BUTTON_HEIGHT;
            }

            drawStyledText(context, cat.getName(),
                    (int) (px + 8), (int) (rowY + 5), 0xFFAAAAAA);
            rowY += BUTTON_HEIGHT;

            for (ModuleButton btn : cat.getModules()) {
                boolean hovered = mouseX >= px && mouseX <= px + panelW
                        && mouseY >= rowY && mouseY <= rowY + BUTTON_HEIGHT;
                btn.updateHover(hovered);

                Module mod = btn.getModule();
                if (mod.isEnabled()) {
                    drawPanel(context, px + 4, rowY, panelW - 8, BUTTON_HEIGHT,
                            4f, btn.hoverAmount, true);
                } else if (hovered) {
                    context.fill((int) (px + 4), (int) rowY,
                            (int) (px + panelW - 4), (int) (rowY + BUTTON_HEIGHT),
                            0x18FFFFFF);
                }

                int textColor = mod.isEnabled() ? accent.textColor : 0xFFCCCCCC;
                drawStyledText(context, mod.getName(),
                        (int) (px + 14), (int) (rowY + 5), textColor);

                String status = mod.isEnabled() ? "ON" : "OFF";
                int statusColor = mod.isEnabled() ? accent.textColor : 0xFF666666;
                int sw = textRenderer.getWidth(styledText(status));
                drawStyledText(context, status,
                        (int) (px + panelW - sw - 10), (int) (rowY + 5),
                        statusColor);

                rowY += BUTTON_HEIGHT;
            }
        }
    }

    // ── Settings panel ────────────────────────────────────

    private float settingsX() {
        return width - SETTINGS_WIDTH - 10;
    }

    private void renderSettingsPanel(DrawContext context, int mouseX, int mouseY) {
        float sx = settingsX();
        float sy = 30;
        int rows = 6;
        int panelH = HEADER_HEIGHT + 1 + rows * SETTINGS_ROW + 4;

        drawPanel(context, sx, sy, SETTINGS_WIDTH, panelH, 8f, 0f, false);

        drawStyledText(context, "Settings", (int) (sx + 10),
                (int) (sy + 6), 0xFFFFFFFF);

        context.fill((int) sx, (int) (sy + HEADER_HEIGHT),
                (int) (sx + SETTINGS_WIDTH), (int) (sy + HEADER_HEIGHT + 1),
                0x33FFFFFF);

        float rowY = sy + HEADER_HEIGHT + 3;
        GuiSettings.AccentColor accent = GuiSettings.getAccentColor();

        drawSettingsRow(context, sx, rowY, "Layout",
                GuiSettings.getLayout().getLabel(), 0xFFFFFFFF, mouseX, mouseY);
        rowY += SETTINGS_ROW;

        drawSettingsRow(context, sx, rowY, "Color",
                accent.getLabel(), accent.textColor, mouseX, mouseY);
        rowY += SETTINGS_ROW;

        drawSettingsRow(context, sx, rowY, "Font",
                GuiSettings.isCustomFontEnabled() ? "Comfortaa" : "Default",
                0xFFFFFFFF, mouseX, mouseY);
        rowY += SETTINGS_ROW;

        drawSettingsRow(context, sx, rowY, "Fog Color",
                FogSettings.getColor().label, 0xFFFFFFFF, mouseX, mouseY);
        rowY += SETTINGS_ROW;

        drawSettingsRow(context, sx, rowY, "Fog Density",
                FogSettings.getDensity().label, 0xFFFFFFFF, mouseX, mouseY);
        rowY += SETTINGS_ROW;

        String modCount = countEnabled() + "/" + countTotal() + " active";
        drawStyledText(context, modCount, (int) (sx + 8), (int) (rowY + 4),
                0xFF888888);
    }

    private void drawSettingsRow(DrawContext context, float sx, float rowY,
                                  String label, String value, int valueColor,
                                  int mouseX, int mouseY) {
        boolean hovered = mouseX >= sx && mouseX <= sx + SETTINGS_WIDTH
                && mouseY >= rowY && mouseY <= rowY + SETTINGS_ROW;
        if (hovered) {
            context.fill((int) sx, (int) rowY, (int) (sx + SETTINGS_WIDTH),
                    (int) (rowY + SETTINGS_ROW), 0x18FFFFFF);
        }

        drawStyledText(context, label, (int) (sx + 8), (int) (rowY + 5),
                0xFFBBBBBB);

        int vw = textRenderer.getWidth(styledText(value));
        drawStyledText(context, value,
                (int) (sx + SETTINGS_WIDTH - vw - 8), (int) (rowY + 5),
                valueColor);
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

    private int countEnabled() {
        int count = 0;
        for (Category cat : categories) {
            for (ModuleButton btn : cat.getModules()) {
                if (btn.getModule().isEnabled()) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countTotal() {
        int count = 0;
        for (Category cat : categories) {
            count += cat.getModules().size();
        }
        return count;
    }

    // ── Input handling ────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (handleSettingsClick(mouseX, mouseY)) {
                return true;
            }

            if (GuiSettings.getLayout() == GuiSettings.Layout.COLUMNS) {
                if (handleColumnsClick(mouseX, mouseY)) {
                    return true;
                }
            } else {
                if (handleSingleClick(mouseX, mouseY)) {
                    return true;
                }
            }
        }

        if (button == 1) {
            if (GuiSettings.getLayout() == GuiSettings.Layout.COLUMNS) {
                for (Category cat : categories) {
                    if (mouseX >= cat.x && mouseX <= cat.x + PANEL_WIDTH
                            && mouseY >= cat.y
                            && mouseY <= cat.y + HEADER_HEIGHT) {
                        cat.toggleCollapsed();
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleSettingsClick(double mouseX, double mouseY) {
        float sx = settingsX();
        float sy = 30;

        if (mouseX < sx || mouseX > sx + SETTINGS_WIDTH) {
            return false;
        }

        float rowY = sy + HEADER_HEIGHT + 3;

        if (mouseY >= rowY && mouseY <= rowY + SETTINGS_ROW) {
            GuiSettings.cycleLayout();
            return true;
        }
        rowY += SETTINGS_ROW;

        if (mouseY >= rowY && mouseY <= rowY + SETTINGS_ROW) {
            GuiSettings.cycleAccentColor();
            return true;
        }
        rowY += SETTINGS_ROW;

        if (mouseY >= rowY && mouseY <= rowY + SETTINGS_ROW) {
            GuiSettings.toggleCustomFont();
            return true;
        }
        rowY += SETTINGS_ROW;

        if (mouseY >= rowY && mouseY <= rowY + SETTINGS_ROW) {
            FogSettings.cycleColor();
            return true;
        }
        rowY += SETTINGS_ROW;

        if (mouseY >= rowY && mouseY <= rowY + SETTINGS_ROW) {
            FogSettings.cycleDensity();
            return true;
        }

        return false;
    }

    private boolean handleColumnsClick(double mouseX, double mouseY) {
        for (Category cat : categories) {
            float catX = cat.x;
            float catY = cat.y;

            if (mouseX >= catX && mouseX <= catX + PANEL_WIDTH
                    && mouseY >= catY && mouseY <= catY + HEADER_HEIGHT) {
                if (mouseX >= catX + 95) {
                    cat.toggleCollapsed();
                } else {
                    draggedCategory = cat;
                    dragOffsetX = (float) mouseX - catX;
                    dragOffsetY = (float) mouseY - catY;
                }
                return true;
            }

            if (!cat.isCollapsed()) {
                int index = 0;
                for (ModuleButton btn : cat.getModules()) {
                    float btnY = catY + HEADER_HEIGHT + 1
                            + index * BUTTON_HEIGHT;
                    if (mouseX >= catX && mouseX <= catX + PANEL_WIDTH
                            && mouseY >= btnY
                            && mouseY <= btnY + BUTTON_HEIGHT) {
                        btn.getModule().toggle();
                        return true;
                    }
                    index++;
                }
            }
        }
        return false;
    }

    private boolean handleSingleClick(double mouseX, double mouseY) {
        int panelW = 200;
        float px = singlePanelX;
        float py = singlePanelY;

        if (mouseX >= px && mouseX <= px + panelW
                && mouseY >= py && mouseY <= py + 24) {
            draggingSinglePanel = true;
            dragOffsetX = (float) mouseX - px;
            dragOffsetY = (float) mouseY - py;
            return true;
        }

        float rowY = py + 24;
        for (int c = 0; c < categories.size(); c++) {
            Category cat = categories.get(c);
            if (c > 0) {
                rowY += BUTTON_HEIGHT;
            }
            rowY += BUTTON_HEIGHT;

            for (ModuleButton btn : cat.getModules()) {
                if (mouseX >= px && mouseX <= px + panelW
                        && mouseY >= rowY && mouseY <= rowY + BUTTON_HEIGHT) {
                    btn.getModule().toggle();
                    return true;
                }
                rowY += BUTTON_HEIGHT;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggedCategory = null;
            draggingSinglePanel = false;
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
        if (button == 0 && draggingSinglePanel) {
            singlePanelX = (float) mouseX - dragOffsetX;
            singlePanelY = (float) mouseY - dragOffsetY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
