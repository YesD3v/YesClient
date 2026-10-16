package com.yesclient.module.modules.client;
import java.util.HashMap;
import java.util.Map;

import com.yesclient.YesClient;
import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.BooleanSetting;
import com.yesclient.module.settings.ModeSetting;


 








public class FancyTextModule extends Module {

    private final ModeSetting alphabet = new ModeSetting("Alphabet", "Select text style",
            "Fancy", "Fancy", "Circled", "Parenthesized", "Superscript", "Script", "Enchanted");
    private final BooleanSetting revertCommands = new BooleanSetting("Revert Commands",
            "Convert fancy text back to normal when sending / commands", true);

     
    private static final Map<Character, Character> FANCY_MAP = new HashMap<>();
    private static final Map<Character, Character> CIRCLED_MAP = new HashMap<>();
    private static final Map<Character, Character> PARENTHESIZED_MAP = new HashMap<>();
    private static final Map<Character, Character> SUPERSCRIPT_MAP = new HashMap<>();
    private static final Map<Character, Character> SCRIPT_MAP = new HashMap<>();
    private static final Map<Character, Character> ENCHANTED_MAP = new HashMap<>();

     
    private static final Map<Character, Character> REVERSE_FANCY = new HashMap<>();
    private static final Map<Character, Character> REVERSE_CIRCLED = new HashMap<>();
    private static final Map<Character, Character> REVERSE_PARENTHESIZED = new HashMap<>();
    private static final Map<Character, Character> REVERSE_SUPERSCRIPT = new HashMap<>();
    private static final Map<Character, Character> REVERSE_SCRIPT = new HashMap<>();
    private static final Map<Character, Character> REVERSE_ENCHANTED = new HashMap<>();

     
     
     
    private static final Map<Character, Character> CHAT_UPPER_TO_LOWER = new HashMap<>();

    static {
         
         
         
        for (int i = 0; i < 26; i++) {
            FANCY_MAP.put((char) ('A' + i), (char) (0xFF21 + i));  
            FANCY_MAP.put((char) ('a' + i), (char) (0xFF41 + i));  
        }
        for (int i = 0; i < 10; i++) {
            FANCY_MAP.put((char) ('0' + i), (char) (0xFF10 + i));
        }
        FANCY_MAP.put('!', '\uFF01');
        FANCY_MAP.put('?', '\uFF1F');
        FANCY_MAP.put('.', '\uFF0E');
        FANCY_MAP.put(',', '\uFF0C');
        FANCY_MAP.put(':', '\uFF1A');
        FANCY_MAP.put(';', '\uFF1B');
        FANCY_MAP.put('/', '\uFF0F');

         
         
         
        for (int i = 0; i < 26; i++) {
            CIRCLED_MAP.put((char) ('A' + i), (char) (0x24B6 + i));  
            CIRCLED_MAP.put((char) ('a' + i), (char) (0x24D0 + i));  
        }
        CIRCLED_MAP.put('0', '\u24EA');
        for (int i = 1; i <= 9; i++) {
            CIRCLED_MAP.put((char) ('0' + i), (char) (0x2460 + i - 1));
        }

         
         
         
        for (int i = 0; i < 26; i++) {
            PARENTHESIZED_MAP.put((char) ('a' + i), (char) (0x249C + i));
            PARENTHESIZED_MAP.put((char) ('A' + i), (char) (0x249C + i));
        }
        for (int i = 1; i <= 9; i++) {
            PARENTHESIZED_MAP.put((char) ('0' + i), (char) (0x2474 + i - 1));
        }

         
         
         
         
         
         
        char[] supUpper = {
            '\u1D2C', '\u1D2E', '\u1D9C', '\u1D30', '\u1D31', '\u1DA0',  
            '\u1D33', '\u1D34', '\u1D35', '\u1D36', '\u1D37', '\u1D38',  
            '\u1D39', '\u1D3A', '\u1D3C', '\u1D3E', '\u211A', '\u1D3F',  
            '\u02E2', '\u1D40', '\u1D41', '\u2C7D', '\u1D42', '\u02E3',  
            '\u1D5E', '\u1DBB'                                            
        };
        char[] supLower = {
            '\u2090', '\u1D47', '\u1D9C', '\u1D48', '\u1D49', '\u1DA0',  
            '\u1D4D', '\u02B0', '\u2148', '\u02B2', '\u1D4F', '\u02E1',  
            '\u1D50', '\u207F', '\u1D52', '\u1D56', '\u211A', '\u02B3',  
            '\u02E2', '\u1D57', '\u1D58', '\u1D5B', '\u02B7', '\u1D61',  
            '\u02B8', '\u1DBB'                                            
        };
        for (int i = 0; i < 26; i++) {
            SUPERSCRIPT_MAP.put((char) ('A' + i), supUpper[i]);
            SUPERSCRIPT_MAP.put((char) ('a' + i), supLower[i]);
        }

         
         
         
         
         
        char[] scriptChars = {
            '\u00C5', '\u212C', '\u2102', '\u2145', '\u2130', '\u2131',  
            '\u210A', '\u210B', '\u2139', '\u2149', '\u0138', '\u2112',  
            '\u217F', '\u2115', '\u25EF', '\u2119', '\u211A', '\u211B',  
            '\u1E60', '\u271D', '\u222A', '\u2123', '\uA64D', '\u2573',  
            '\u01B3', '\u2124'                                            
        };
        for (int i = 0; i < 26; i++) {
            SCRIPT_MAP.put((char) ('A' + i), scriptChars[i]);
            SCRIPT_MAP.put((char) ('a' + i), scriptChars[i]);
        }

         
         
         
         
         
        char[] enchantedChars = {
            '\u03B1', '\u0432', '\u03C2', '\u2202', '\u0454', '\u0192',  
            '\u0262', '\u0452', '\u03B9', '\u1D0A', '\uA5E3', '\u2113',  
            '\u043C', '\u03B7', '\u00D8', '\u0569', '\u1C67', '\u027E',  
            '\u054F', '\u0442', '\u03C5', '\u03BD', '\u03C9', '\u2A09',  
            '\u0447', '\u1C6E'                                            
        };
        for (int i = 0; i < 26; i++) {
            ENCHANTED_MAP.put((char) ('A' + i), enchantedChars[i]);
            ENCHANTED_MAP.put((char) ('a' + i), enchantedChars[i]);
        }

         
         
         
        buildReverse(FANCY_MAP, REVERSE_FANCY);
        buildReverse(CIRCLED_MAP, REVERSE_CIRCLED);
        buildReverse(PARENTHESIZED_MAP, REVERSE_PARENTHESIZED);
        buildReverse(SUPERSCRIPT_MAP, REVERSE_SUPERSCRIPT);
        buildReverse(SCRIPT_MAP, REVERSE_SCRIPT);
        buildReverse(ENCHANTED_MAP, REVERSE_ENCHANTED);

         
         
         
         
         
         

         
        for (int i = 0; i < 26; i++) {
            CHAT_UPPER_TO_LOWER.put((char) (0xFF21 + i), (char) (0xFF41 + i));
        }

         
        for (int i = 0; i < 26; i++) {
            CHAT_UPPER_TO_LOWER.put((char) (0x24B6 + i), (char) (0x24D0 + i));
        }

         
        for (int i = 0; i < 26; i++) {
            if (supUpper[i] != supLower[i]) {
                CHAT_UPPER_TO_LOWER.put(supUpper[i], supLower[i]);
            }
        }
    }

    private static void buildReverse(Map<Character, Character> forward, Map<Character, Character> reverse) {
        for (Map.Entry<Character, Character> entry : forward.entrySet()) {
            reverse.put(entry.getValue(), entry.getKey());
        }
    }

    public FancyTextModule() {
        super("Fancy Text", "Replaces typed text with fancy Unicode characters", Category.CLIENT, ModuleMode.BOTH);
        addSetting(alphabet);
        addSetting(revertCommands);
    }

     



    public char convertChar(char c) {
        Map<Character, Character> map = getActiveMap();
        Character result = map.get(c);
        return result != null ? result : c;
    }

     


    public String convertString(String input) {
        if (input == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            sb.append(convertChar(input.charAt(i)));
        }
        return sb.toString();
    }

     









    public static String convertForChat(String input) {
        if (input == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            Character lower = CHAT_UPPER_TO_LOWER.get(c);
            sb.append(lower != null ? lower : c);
        }
        return sb.toString();
    }

     



    public static String revertToNormal(String input) {
        if (input == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            Character normal = REVERSE_FANCY.get(c);
            if (normal == null) normal = REVERSE_CIRCLED.get(c);
            if (normal == null) normal = REVERSE_PARENTHESIZED.get(c);
            if (normal == null) normal = REVERSE_SUPERSCRIPT.get(c);
            if (normal == null) normal = REVERSE_SCRIPT.get(c);
            if (normal == null) normal = REVERSE_ENCHANTED.get(c);
            sb.append(normal != null ? normal : c);
        }
        return sb.toString();
    }

     


    public static boolean isCommand(String message) {
        if (message == null || message.isEmpty()) return false;
        char first = message.charAt(0);
        return first == '/' || first == '\uFF0F' || first == '.' || first == '\uFF0E';
    }

    public boolean isRevertCommands() {
        return revertCommands.isEnabled();
    }

    private Map<Character, Character> getActiveMap() {
        String mode = alphabet.getValue();
        switch (mode) {
            case "Circled":        return CIRCLED_MAP;
            case "Parenthesized":  return PARENTHESIZED_MAP;
            case "Superscript":    return SUPERSCRIPT_MAP;
            case "Script":         return SCRIPT_MAP;
            case "Enchanted":      return ENCHANTED_MAP;
            case "Fancy":
            default:               return FANCY_MAP;
        }
    }

    public ModeSetting getAlphabetSetting() {
        return alphabet;
    }
}
