package com.pathdlc.digger.gui;

import com.pathdlc.digger.render.LiquidGlassRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
    private static final int PANEL_WIDTH = 120;
    private static final int HEADER_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 18;
    private static final float HEADER_RADIUS = 8f;
    private static final float BUTTON_RADIUS = 6f;

    private final List<Category> categories = new ArrayList<>();
    private Category draggedCategory;
    private float dragOffsetX, dragOffsetY;

    public ClickGuiScreen() {
        super(Text.literal("ClickGUI"));
    }

    @Override
    protected void init() {
        super.init();
        if (categories.isEmpty()) {
            initCategories();
        }
    }

    private void initCategories() {
        float startX = 10;
        float startY = 30;
        float spacing = 130;

        Category farm = new Category("Farm", startX, startY);
        farm.addModule(new Module("Apple"));
        farm.addModule(new Module("Dig"));
        categories.add(farm);

        Category world = new Category("World", startX + spacing, startY);
        world.addModule(new Module("Warden"));
        categories.add(world);

        Category utility = new Category("Utility", startX + spacing * 2, startY);
        utility.addModule(new Module("Clan"));
        categories.add(utility);

        Category render = new Category("Render", startX + spacing * 3, startY);
        render.addModule(new Module("Overlay"));
        categories.add(render);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. Capture and blur the background (once, before everything)
        LiquidGlassRenderer.captureAndBlur();

        // 2. Draw glass panels for all categories and modules
        for (Category cat : categories) {
            renderCategoryPanels(context, cat, mouseX, mouseY);
        }

        // 3. Draw text, arrows, separators on top of glass panels
        for (Category cat : categories) {
            renderCategoryText(context, cat);
        }

        // 4. Super at the end
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderCategoryPanels(DrawContext context, Category cat,
                                       int mouseX, int mouseY) {
        float catX = cat.x;
        float catY = cat.y;

        boolean catHovered = mouseX >= catX && mouseX <= catX + PANEL_WIDTH
                && mouseY >= catY && mouseY <= catY + HEADER_HEIGHT;
        float catTarget = catHovered ? 1.0f : 0.0f;
        cat.hoverAmount += (catTarget - cat.hoverAmount) * 0.15f;

        if (LiquidGlassRenderer.isReady()) {
            LiquidGlassRenderer.drawGlassPanel(context, catX, catY,
                    PANEL_WIDTH, HEADER_HEIGHT, HEADER_RADIUS, cat.hoverAmount);
        } else {
            LiquidGlassRenderer.drawFallbackPanel(context, (int) catX, (int) catY,
                    PANEL_WIDTH, HEADER_HEIGHT, false);
        }

        if (!cat.isCollapsed()) {
            int index = 0;
            for (ModuleButton btn : cat.getModules()) {
                float btnY = catY + HEADER_HEIGHT + 1 + index * BUTTON_HEIGHT;
                boolean btnHovered = mouseX >= catX && mouseX <= catX + PANEL_WIDTH
                        && mouseY >= btnY && mouseY <= btnY + BUTTON_HEIGHT;
                btn.updateHover(btnHovered);

                Module mod = btn.getModule();
                if (LiquidGlassRenderer.isReady()) {
                    if (mod.isEnabled()) {
                        LiquidGlassRenderer.drawGlassPanel(context, catX, btnY,
                                PANEL_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS,
                                btn.hoverAmount, 0.25f, 0.3f, 0.6f, 1.0f);
                    } else {
                        LiquidGlassRenderer.drawGlassPanel(context, catX, btnY,
                                PANEL_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS,
                                btn.hoverAmount);
                    }
                } else {
                    LiquidGlassRenderer.drawFallbackPanel(context, (int) catX,
                            (int) btnY, PANEL_WIDTH, BUTTON_HEIGHT,
                            mod.isEnabled());
                }
                index++;
            }
        }
    }

    private void renderCategoryText(DrawContext context, Category cat) {
        float catX = cat.x;
        float catY = cat.y;

        String name = cat.getName();
        int textWidth = textRenderer.getWidth(name);
        context.drawText(textRenderer, name,
                (int) (catX + 60 - textWidth / 2), (int) (catY + 6),
                0xFFFFFFFF, true);

        String arrow = cat.isCollapsed() ? "\u25B6" : "\u25BC";
        context.drawText(textRenderer, arrow,
                (int) (catX + 105), (int) (catY + 6), 0xFFFFFFFF, true);

        if (!cat.isCollapsed()) {
            context.fill((int) catX, (int) (catY + HEADER_HEIGHT),
                    (int) (catX + PANEL_WIDTH), (int) (catY + HEADER_HEIGHT + 1),
                    0x33FFFFFF);

            int index = 0;
            for (ModuleButton btn : cat.getModules()) {
                float btnY = catY + HEADER_HEIGHT + 1 + index * BUTTON_HEIGHT;
                Module mod = btn.getModule();
                int textColor = mod.isEnabled() ? 0xFF88BBFF : 0xFFCCCCCC;
                context.drawText(textRenderer, mod.getName(),
                        (int) (catX + 5), (int) (btnY + 5), textColor, true);
                index++;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
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
        }

        if (button == 1) {
            for (Category cat : categories) {
                float catX = cat.x;
                float catY = cat.y;
                if (mouseX >= catX && mouseX <= catX + PANEL_WIDTH
                        && mouseY >= catY && mouseY <= catY + HEADER_HEIGHT) {
                    cat.toggleCollapsed();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggedCategory = null;
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
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
