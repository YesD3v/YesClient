package com.yesclient.utils;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.gui.FontRenderer;

 
public final class MarqueeText {

    private static final long STEP_MILLIS = 360L;
    private static final long EDGE_PAUSE_MILLIS = 1000L;
    private static final Map<Object, State> STATES = Collections.synchronizedMap(new WeakHashMap<Object, State>());

    private MarqueeText() {
    }

    public static String fit(String text, FontRenderer font, int maxWidth, Object owner) {
        return fit(text, font, maxWidth, owner, "");
    }

     
    public static String fit(String text, FontRenderer font, int maxWidth, Object owner, String formattingPrefix) {
        if (text == null || text.isEmpty() || font == null || maxWidth <= 0) return "";
        String prefix = formattingPrefix == null ? "" : formattingPrefix;
        if (font.getStringWidth(prefix + text) <= maxWidth) return text;

        int maxStart = text.length() - 1;
        while (maxStart > 0 && font.getStringWidth(prefix + text.substring(maxStart - 1)) <= maxWidth) maxStart--;
        if (maxStart <= 0) return stripPrefix(font.trimStringToWidth(prefix + text, maxWidth), prefix);

        Object stateKey = owner == null ? text : owner;
        long now = System.currentTimeMillis();
        State state;
        synchronized (STATES) {
            state = STATES.get(stateKey);
            if (state == null || !text.equals(state.text) || state.maxStart != maxStart) {
                state = new State(text, maxStart, now + EDGE_PAUSE_MILLIS);
                STATES.put(stateKey, state);
            } else {
                while (now >= state.nextStepAt) {
                    state.offset += state.direction;
                    if (state.offset >= maxStart) {
                        state.offset = maxStart;
                        state.direction = -1;
                        state.nextStepAt += EDGE_PAUSE_MILLIS;
                    } else if (state.offset <= 0) {
                        state.offset = 0;
                        state.direction = 1;
                        state.nextStepAt += EDGE_PAUSE_MILLIS;
                    } else {
                        state.nextStepAt += STEP_MILLIS;
                    }
                }
            }
            return stripPrefix(font.trimStringToWidth(prefix + text.substring(state.offset), maxWidth), prefix);
        }
    }

    private static String stripPrefix(String text, String prefix) {
        return !prefix.isEmpty() && text.startsWith(prefix) ? text.substring(prefix.length()) : text;
    }

    private static final class State {
        private final String text;
        private final int maxStart;
        private int offset;
        private int direction = 1;
        private long nextStepAt;

        private State(String text, int maxStart, long nextStepAt) {
            this.text = text;
            this.maxStart = maxStart;
            this.nextStepAt = nextStepAt;
        }
    }
}
