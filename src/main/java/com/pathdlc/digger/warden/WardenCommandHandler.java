package com.pathdlc.digger.warden;

import com.pathdlc.digger.util.Chat;

import java.util.Locale;

public class WardenCommandHandler {
    private final AutoWardenBot bot;

    public WardenCommandHandler(AutoWardenBot bot) {
        this.bot = bot;
    }

    public boolean handle(String rawMessage) {
        String message = rawMessage.trim();

        if (!message.startsWith(".")) {
            return false;
        }

        String[] args = message.substring(1).trim().split("\\s+");

        if (args.length == 0 || args[0].isBlank()) {
            return false;
        }

        String root = args[0].toLowerCase(Locale.ROOT);

        if (!root.equals("warden") && !root.equals("autowarden")) {
            return false;
        }

        if (args.length < 2) {
            printHelp();
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);

        if (action.equals("start") || action.equals("on")) {
            bot.start();
            return true;
        }

        if (action.equals("stop") || action.equals("off")) {
            bot.stop();
            return true;
        }

        if (action.equals("status")) {
            Chat.info(bot.status());
            return true;
        }

        if (action.equals("radius")) {
            if (args.length < 3) {
                Chat.info("AutoWarden radius = " + bot.getRadius());
                return true;
            }

            try {
                bot.setRadius(Integer.parseInt(args[2]));
            } catch (NumberFormatException exception) {
                Chat.warn("Use: .warden radius <8-96>");
            }

            return true;
        }

        if (action.equals("vertical")) {
            if (args.length < 3) {
                Chat.info("AutoWarden vertical radius = " + bot.getVerticalRadius());
                return true;
            }

            try {
                bot.setVerticalRadius(Integer.parseInt(args[2]));
            } catch (NumberFormatException exception) {
                Chat.warn("Use: .warden vertical <4-32>");
            }

            return true;
        }

        printHelp();
        return true;
    }

    private void printHelp() {
        Chat.info("AutoWarden commands:");
        Chat.info(".warden start / .warden stop / .warden status");
        Chat.info(".warden radius <8-96>");
        Chat.info(".warden vertical <4-32>");
    }
}
