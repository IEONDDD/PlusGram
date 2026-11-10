package com.plusgram.app.ai.network;

import org.telegram.messenger.BuildConfig;

public final class ReasoningFilter {

    private static final String OPEN = "<think>";
    private static final String CLOSE = "</think>";

    private final StringBuilder pending = new StringBuilder();
    private final StringBuilder reasoning = new StringBuilder();
    private boolean inReasoning;
    private boolean reasoningSignal;
    private boolean useRust;
    private final boolean adaptive;

    public ReasoningFilter() {
        this(BuildConfig.RUST_TEXT_ENABLED && RustLibrary.AVAILABLE, true);
    }

    ReasoningFilter(boolean useRust) {
        this(useRust, false);
    }

    ReasoningFilter(boolean useRust, boolean adaptive) {
        this.useRust = useRust;
        this.adaptive = adaptive;
    }

    private static final class RustLibrary {
        static final boolean AVAILABLE = load();

        private static boolean load() {
            try {
                System.loadLibrary("plusgram_text");
                return true;
            } catch (UnsatisfiedLinkError unavailable) {
                return false;
            }
        }
    }

    // The optional library is loaded once, only in builds enabling the pilot.
    // State stays in Java: no persistent native handles or cross-thread JNI references.
    private static native String[] nativeProcess(String input, boolean inReasoning);

    public String consumeReasoning() {
        if (reasoning.length() == 0) {
            return null;
        }
        String text = reasoning.toString();
        reasoning.setLength(0);
        return text;
    }

    public boolean consumeReasoningSignal() {
        boolean signal = reasoningSignal;
        reasoningSignal = false;
        return signal;
    }

    public String filter(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return "";
        }
        pending.append(chunk);
        if (useRust && (!adaptive || shouldUseRust())) {
            String[] result = null;
            try {
                result = nativeProcess(pending.toString(), inReasoning);
            } catch (UnsatisfiedLinkError missingOptionalLibrary) {
                useRust = false;
            }
            // A caught Rust panic returns null before Java state is committed.
            if (result != null) {
                if (result.length != 4 || result[0] == null || result[1] == null
                        || result[2] == null || result[3] == null || result[3].length() != 2) {
                    throw new IllegalStateException("Invalid Rust text result");
                }
                reasoning.append(result[1]);
                pending.setLength(0);
                pending.append(result[2]);
                inReasoning = result[3].charAt(0) != 0;
                reasoningSignal |= result[3].charAt(1) != 0;
                return result[0];
            }
            useRust = false;
        }
        StringBuilder out = new StringBuilder();
        while (pending.length() > 0) {
            if (inReasoning) {
                int close = pending.indexOf(CLOSE);
                if (close < 0) {
                    int keep = tailLength(CLOSE);
                    reasoning.append(pending, 0, pending.length() - keep);
                    pending.delete(0, pending.length() - keep);
                    break;
                }
                reasoning.append(pending, 0, close);
                pending.delete(0, close + CLOSE.length());
                inReasoning = false;
                continue;
            }
            int open = pending.indexOf(OPEN);
            if (open < 0) {
                int keep = tailLength(OPEN);
                out.append(pending, 0, pending.length() - keep);
                pending.delete(0, pending.length() - keep);
                break;
            }
            out.append(pending, 0, open);
            pending.delete(0, open + OPEN.length());
            inReasoning = true;
            reasoningSignal = true;
        }
        return out.toString();
    }

    public String flush() {
        String rest = inReasoning ? "" : pending.toString();
        pending.setLength(0);
        inReasoning = false;
        return rest;
    }

    // JNI pays off for dense, large blocks; normal token-sized chunks stay in Java.
    private boolean shouldUseRust() {
        int length = pending.length();
        return length >= 4096 && hasDenseTags(0, 2048)
                && hasDenseTags(length - 2048, length);
    }

    private boolean hasDenseTags(int start, int end) {
        int count = 0;
        for (int i = start; i <= end - OPEN.length(); i++) {
            if (pending.charAt(i) != '<') {
                continue;
            }
            int j = 1;
            while (j < OPEN.length() && pending.charAt(i + j) == OPEN.charAt(j)) {
                j++;
            }
            if (j == OPEN.length() && ++count >= 32) {
                return true;
            }
        }
        return false;
    }

    private int tailLength(String tag) {
        int max = Math.min(tag.length() - 1, pending.length());
        for (int size = max; size > 0; size--) {
            if (tag.startsWith(pending.substring(pending.length() - size))) {
                return size;
            }
        }
        return 0;
    }
}
