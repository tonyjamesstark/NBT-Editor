package tsp.headdb.ported;

import java.util.regex.Pattern;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

public class Utils {


    public static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    /**
     * Validate a UUID (version 4)
     *
     * @param uuid UUID to be validated
     * @return Returns true if the string is a valid UUID
     */
    public static boolean validateUniqueId(String uuid) {
        return UUID_PATTERN.matcher(uuid).matches();
    }

    public static void sendMessage(String message) {
        Minecraft.getInstance().player.sendSystemMessage(Component.nullToEmpty(colorize(message)));
    }

    public static String colorize(String string) {
        return translateColorCodes(ChatFormatting.GRAY + string);
    }

    /**
     * Bukkit's {@code ChatColor.translateAlternateColorCodes('&', s)}: turns {@code &} into the
     * section sign where a colour code follows it, and leaves it alone where one does not.
     */
    public static String translateColorCodes(String text) {
        StringBuilder output = new StringBuilder();
        boolean colorCode = false;
        for (char c : text.toCharArray()) {
            if (c == '&')
                colorCode = true;
            else {
                if (colorCode) {
                    colorCode = false;
                    if ((c + "").replaceAll("[0-9a-fA-Fk-oK-OrR]", "").isEmpty())
                        output.append('\u00a7');
                    else
                        output.append('&');
                }

                output.append(c);
            }
        }
        if (colorCode)
            output.append('&');
        return output.toString();
    }

    /** Bukkit's {@code ChatColor.stripColor}. */
    public static String stripColor(String text) {
        return text.replaceAll("\\xA7[0-9a-fA-Fk-oK-OrR]", "");
    }

}
