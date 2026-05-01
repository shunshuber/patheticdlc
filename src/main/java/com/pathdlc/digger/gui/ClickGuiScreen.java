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

        // ── Combat ──
        Module killAura = new Module("KillAura");
        killAura.addSetting(ModuleSetting.slider("Range", 4.0f, 2.0f, 6.0f, 0.1f));
        killAura.addSetting(ModuleSetting.toggle("Only Crit", false));
        killAura.addSetting(ModuleSetting.toggle("Attack Mobs", true));
        killAura.addSetting(ModuleSetting.toggle("Attack Players", false));
        Module aimAssist = new Module("AimAssist");
        aimAssist.addSetting(ModuleSetting.slider("Speed", 50f, 10f, 100f, 5f));
        aimAssist.addSetting(ModuleSetting.slider("FOV", 90f, 30f, 180f, 10f));
        aimAssist.addSetting(ModuleSetting.toggle("Visible Only", true));
        Module antiKnockback = new Module("AntiKB");
        antiKnockback.addSetting(ModuleSetting.slider("Horizontal", 0f, 0f, 100f, 5f));
        antiKnockback.addSetting(ModuleSetting.slider("Vertical", 0f, 0f, 100f, 5f));
        Module velocity = new Module("Velocity");
        velocity.addSetting(ModuleSetting.choice("Mode", new String[]{"Cancel","Reduce","Reverse"}, 0));
        Module autoArmor = new Module("AutoArmor");
        Module autoTotem = new Module("AutoTotem");
        Module triggerBot = new Module("TriggerBot");
        triggerBot.addSetting(ModuleSetting.slider("Delay", 1f, 0f, 5f, 1f));
        Module reach = new Module("Reach");
        reach.addSetting(ModuleSetting.slider("Distance", 3.5f, 3.0f, 6.0f, 0.1f));
        Module antiBot = new Module("AntiBot");
        antiBot.addSetting(ModuleSetting.choice("Mode", new String[]{"Default","Advanced","FunTime"}, 0));
        Module criticals = new Module("Criticals");
        criticals.addSetting(ModuleSetting.choice("Mode", new String[]{"Packet","Jump","Mini Jump"}, 0));
        Module autoClicker = new Module("AutoClicker");
        autoClicker.addSetting(ModuleSetting.slider("CPS", 12f, 1f, 20f, 1f));
        autoClicker.addSetting(ModuleSetting.toggle("Right Click", false));
        Module clan = new Module("Clan", clanOn, clanOff);
        Category combat = new Category("Combat", 0, 0);
        combat.addModule(killAura);
        combat.addModule(aimAssist);
        combat.addModule(antiKnockback);
        combat.addModule(velocity);
        combat.addModule(autoArmor);
        combat.addModule(autoTotem);
        combat.addModule(triggerBot);
        combat.addModule(reach);
        combat.addModule(antiBot);
        combat.addModule(criticals);
        combat.addModule(autoClicker);
        combat.addModule(clan);
        categories.add(combat);

        // ── Movement ──
        Module speed = new Module("Speed");
        speed.addSetting(ModuleSetting.choice("Mode", new String[]{"Vanilla","Strafe","BHop","Low Hop"}, 0));
        speed.addSetting(ModuleSetting.slider("Speed", 1.5f, 0.5f, 5.0f, 0.1f));
        Module flight = new Module("Flight");
        flight.addSetting(ModuleSetting.choice("Mode", new String[]{"Vanilla","Glide","Jetpack","Creative"}, 0));
        flight.addSetting(ModuleSetting.slider("Speed", 2.0f, 0.5f, 10.0f, 0.5f));
        Module noFall = new Module("NoFall");
        noFall.addSetting(ModuleSetting.choice("Mode", new String[]{"Packet","Spoof","MLG"}, 0));
        Module sprint = new Module("Sprint");
        sprint.addSetting(ModuleSetting.choice("Mode", new String[]{"Legit","Omnidirectional"}, 0));
        Module step = new Module("Step");
        step.addSetting(ModuleSetting.slider("Height", 1f, 0.5f, 2.5f, 0.5f));
        Module noSlowdown = new Module("NoSlow");
        noSlowdown.addSetting(ModuleSetting.toggle("Items", true));
        noSlowdown.addSetting(ModuleSetting.toggle("Soulsand", true));
        noSlowdown.addSetting(ModuleSetting.toggle("Web", true));
        Module elytraFly = new Module("ElytraFly");
        elytraFly.addSetting(ModuleSetting.choice("Mode", new String[]{"Vanilla","Boost","Control"}, 0));
        elytraFly.addSetting(ModuleSetting.slider("Speed", 1.5f, 0.5f, 5.0f, 0.1f));
        Module jesus = new Module("Jesus");
        jesus.addSetting(ModuleSetting.choice("Mode", new String[]{"Solid","Dolphin","Trident"}, 0));
        Module sneak = new Module("Sneak");
        sneak.addSetting(ModuleSetting.choice("Mode", new String[]{"Vanilla","Packet","Legit"}, 0));
        Module spider = new Module("Spider");
        spider.addSetting(ModuleSetting.slider("Speed", 0.5f, 0.1f, 2.0f, 0.1f));
        Module phase = new Module("Phase");
        Module safewalk = new Module("SafeWalk");
        Module bunnyHop = new Module("BunnyHop");
        Module invWalk = new Module("InvWalk");
        Module parkour = new Module("Parkour");
        Module antiVoid = new Module("AntiVoid");
        Module longJump = new Module("LongJump");
        longJump.addSetting(ModuleSetting.slider("Boost", 1.5f, 1.0f, 4.0f, 0.1f));
        Category movement = new Category("Movement", 0, 0);
        movement.addModule(speed);
        movement.addModule(flight);
        movement.addModule(noFall);
        movement.addModule(sprint);
        movement.addModule(step);
        movement.addModule(noSlowdown);
        movement.addModule(elytraFly);
        movement.addModule(jesus);
        movement.addModule(sneak);
        movement.addModule(spider);
        movement.addModule(phase);
        movement.addModule(safewalk);
        movement.addModule(bunnyHop);
        movement.addModule(invWalk);
        movement.addModule(parkour);
        movement.addModule(antiVoid);
        movement.addModule(longJump);
        categories.add(movement);

        // ── Render ──
        Module esp = new Module("ESP");
        esp.addSetting(ModuleSetting.choice("Mode", new String[]{"Box","Glow","2D","Outline"}, 0));
        esp.addSetting(ModuleSetting.toggle("Players", true));
        esp.addSetting(ModuleSetting.toggle("Mobs", true));
        esp.addSetting(ModuleSetting.toggle("Items", false));
        Module tracers = new Module("Tracers");
        tracers.addSetting(ModuleSetting.toggle("Players", true));
        tracers.addSetting(ModuleSetting.toggle("Mobs", false));
        Module nametags = new Module("Nametags");
        nametags.addSetting(ModuleSetting.toggle("Health", true));
        nametags.addSetting(ModuleSetting.toggle("Armor", true));
        nametags.addSetting(ModuleSetting.slider("Scale", 1.5f, 0.5f, 3.0f, 0.1f));
        Module chams = new Module("Chams");
        chams.addSetting(ModuleSetting.choice("Mode", new String[]{"Colored","Textured","Flat"}, 0));
        Module fullBright = new Module("FullBright");
        fullBright.addSetting(ModuleSetting.choice("Mode", new String[]{"Gamma","Night Vision"}, 0));
        Module xray = new Module("XRay");
        xray.addSetting(ModuleSetting.choice("Mode", new String[]{"Default","Ores Only","Custom"}, 0));
        Module blockOverlay = new Module("BlockOverlay");
        blockOverlay.addSetting(ModuleSetting.choice("Texture",
                new String[]{"Kitten","Sky","Devil"}, 0));
        Module blockEsp = new Module("BlockESP");
        blockEsp.addSetting(ModuleSetting.slider("Radius", 32f, 8f, 64f, 4f));
        Module motionBlur = new Module("MotionBlur");
        motionBlur.addSetting(ModuleSetting.slider("Strength", 0.5f, 0.1f, 0.9f, 0.05f));
        Module hitEffects = new Module("HitEffects");
        Module noRender = new Module("NoRender");
        noRender.addSetting(ModuleSetting.toggle("Fire", true));
        noRender.addSetting(ModuleSetting.toggle("Pumpkin", true));
        noRender.addSetting(ModuleSetting.toggle("Totem", true));
        noRender.addSetting(ModuleSetting.toggle("Fog", true));
        noRender.addSetting(ModuleSetting.toggle("Blindness", true));
        Module freecam = new Module("Freecam");
        freecam.addSetting(ModuleSetting.slider("Speed", 1.0f, 0.1f, 5.0f, 0.1f));
        Module waypoints = new Module("Waypoints");
        Module fog = new Module("Fog");
        fog.addSetting(ModuleSetting.choice("Color",
                new String[]{"White","Light Blue","Purple","Red","Green","Dark","Golden"}, 0));
        fog.addSetting(ModuleSetting.slider("Density", 0.5f, 0.0f, 1.0f, 0.1f));
        Module aspectRatio = new Module("AspectRatio");
        aspectRatio.addSetting(ModuleSetting.slider("FOV Scale", 1.33f, 1.0f, 2.0f, 0.01f));
        Module breadcrumbs = new Module("Breadcrumbs");
        Module storageESP = new Module("StorageESP");
        storageESP.addSetting(ModuleSetting.toggle("Chests", true));
        storageESP.addSetting(ModuleSetting.toggle("Ender Chests", true));
        storageESP.addSetting(ModuleSetting.toggle("Shulkers", true));
        Category render = new Category("Render", 0, 0);
        render.addModule(esp);
        render.addModule(tracers);
        render.addModule(nametags);
        render.addModule(chams);
        render.addModule(fullBright);
        render.addModule(xray);
        render.addModule(blockOverlay);
        render.addModule(blockEsp);
        render.addModule(motionBlur);
        render.addModule(hitEffects);
        render.addModule(noRender);
        render.addModule(freecam);
        render.addModule(waypoints);
        render.addModule(fog);
        render.addModule(aspectRatio);
        render.addModule(breadcrumbs);
        render.addModule(storageESP);
        categories.add(render);

        // ── Player ──
        Module autoFish = new Module("AutoFish", autoFishOn, autoFishOff);
        Module noRotate = new Module("NoRotate");
        Module fastPlace = new Module("FastPlace");
        fastPlace.addSetting(ModuleSetting.slider("Delay", 0f, 0f, 4f, 1f));
        Module fastBreak = new Module("FastBreak");
        fastBreak.addSetting(ModuleSetting.slider("Multiplier", 1.5f, 1.0f, 5.0f, 0.1f));
        Module autoTool = new Module("AutoTool");
        Module scaffold = new Module("Scaffold");
        scaffold.addSetting(ModuleSetting.choice("Mode", new String[]{"Normal","Expand","Tower"}, 0));
        scaffold.addSetting(ModuleSetting.toggle("Safe Walk", true));
        Module timer = new Module("Timer");
        timer.addSetting(ModuleSetting.slider("Speed", 1.0f, 0.1f, 5.0f, 0.1f));
        Module blink = new Module("Blink");
        Module antiHunger = new Module("AntiHunger");
        Module autoEat = new Module("AutoEat");
        autoEat.addSetting(ModuleSetting.slider("Health", 10f, 1f, 19f, 1f));
        Module chestStealer = new Module("ChestStealer");
        chestStealer.addSetting(ModuleSetting.slider("Delay", 50f, 0f, 500f, 25f));
        Module inventoryCleaner = new Module("InvCleaner");
        Module autoRespawn = new Module("AutoRespawn");
        Module pingSpoof = new Module("PingSpoof");
        pingSpoof.addSetting(ModuleSetting.slider("Ping", 100f, 0f, 1000f, 50f));
        Module skinBlink = new Module("SkinBlink");
        Module autoDisconnect = new Module("AutoLeave");
        autoDisconnect.addSetting(ModuleSetting.slider("Health", 5f, 1f, 19f, 1f));
        Category player = new Category("Player", 0, 0);
        player.addModule(autoFish);
        player.addModule(noRotate);
        player.addModule(fastPlace);
        player.addModule(fastBreak);
        player.addModule(autoTool);
        player.addModule(scaffold);
        player.addModule(timer);
        player.addModule(blink);
        player.addModule(antiHunger);
        player.addModule(autoEat);
        player.addModule(chestStealer);
        player.addModule(inventoryCleaner);
        player.addModule(autoRespawn);
        player.addModule(pingSpoof);
        player.addModule(skinBlink);
        player.addModule(autoDisconnect);
        categories.add(player);

        // ── World ──
        Module warden = new Module("Warden", wardenOn, wardenOff);
        Module baseFinder = new Module("BaseFinder", baseFinderOn, baseFinderOff);
        baseFinder.addSetting(ModuleSetting.slider("Radius", 64f, 16f, 128f, 16f));
        Module nuker = new Module("Nuker");
        nuker.addSetting(ModuleSetting.slider("Radius", 4f, 1f, 6f, 1f));
        nuker.addSetting(ModuleSetting.choice("Mode", new String[]{"All","Flat","Smash"}, 0));
        Module autoSign = new Module("AutoSign");
        Module fucker = new Module("Fucker");
        fucker.addSetting(ModuleSetting.choice("Block", new String[]{"Bed","Cake","Spawner","Egg"}, 0));
        Module autoFarm = new Module("AutoFarm", autoFarmOn, autoFarmOff);
        autoFarm.addSetting(ModuleSetting.slider("Radius", 4f, 2f, 8f, 1f));
        Module autoMine = new Module("AutoMine", autoMineOn, autoMineOff);
        autoMine.addSetting(ModuleSetting.choice("Ore",
                new String[]{"Diamond","Emerald","Gold","Iron","Netherite","All Ores"}, 0));
        Module apple = new Module("Apple", appleOn, appleOff);
        Module dig = new Module("Dig", digOn, digOff);
        Module tunneller = new Module("Tunneller");
        tunneller.addSetting(ModuleSetting.choice("Size", new String[]{"1x2","2x2","3x3"}, 0));
        Module veinMiner = new Module("VeinMiner");
        Category world = new Category("World", 0, 0);
        world.addModule(warden);
        world.addModule(baseFinder);
        world.addModule(nuker);
        world.addModule(autoSign);
        world.addModule(fucker);
        world.addModule(autoFarm);
        world.addModule(autoMine);
        world.addModule(apple);
        world.addModule(dig);
        world.addModule(tunneller);
        world.addModule(veinMiner);
        categories.add(world);

        // ── Exploit ──
        Module disabler = new Module("Disabler");
        disabler.addSetting(ModuleSetting.choice("Mode", new String[]{"FunTime","Matrix","Vulcan","Grim"}, 0));
        Module fastBow = new Module("FastBow");
        Module ghostHand = new Module("GhostHand");
        Module packetMine = new Module("PacketMine");
        Module autoGapple = new Module("AutoGapple");
        Module portalGodMode = new Module("PortalGod");
        Module tpAura = new Module("TPAura");
        tpAura.addSetting(ModuleSetting.slider("Range", 8f, 3f, 32f, 1f));
        Module boatFly = new Module("BoatFly");
        boatFly.addSetting(ModuleSetting.slider("Speed", 2f, 0.5f, 10f, 0.5f));
        Module nameProtect = new Module("NameProtect");
        Module serverCrasher = new Module("Crasher");
        serverCrasher.addSetting(ModuleSetting.choice("Mode", new String[]{"Packet","Book","Movement"}, 0));
        Category exploit = new Category("Exploit", 0, 0);
        exploit.addModule(disabler);
        exploit.addModule(fastBow);
        exploit.addModule(ghostHand);
        exploit.addModule(packetMine);
        exploit.addModule(autoGapple);
        exploit.addModule(portalGodMode);
        exploit.addModule(tpAura);
        exploit.addModule(boatFly);
        exploit.addModule(nameProtect);
        exploit.addModule(serverCrasher);
        categories.add(exploit);

        // ── FunTime ──
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
        Module autoEvent = new Module("AutoEvent");
        autoEvent.addSetting(ModuleSetting.toggle("Auto Join", true));
        Module salary = new Module("Salary");
        salary.addSetting(ModuleSetting.slider("Interval", 60f, 30f, 300f, 10f));
        Category funtime = new Category("FunTime", 0, 0);
        funtime.addModule(autoCraft);
        funtime.addModule(autoSell);
        funtime.addModule(autoBuy);
        funtime.addModule(autoEvent);
        funtime.addModule(salary);
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
