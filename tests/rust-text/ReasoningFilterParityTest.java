package com.plusgram.app.ai.network;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.function.Function;

public final class ReasoningFilterParityTest {
    private static int checks;
    private static void equal(Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Mismatch: expected=" + expected + ", actual=" + actual);
        }
    }
    private static void compare(ReasoningFilter[] filters, Function<ReasoningFilter, Object> operation) {
        Object expected = operation.apply(filters[0]);
        equal(expected, operation.apply(filters[1]));
        equal(expected, operation.apply(filters[2]));
    }
    private static void run(String[] chunks, long seed) throws Exception {
        ReasoningFilter[] filters = {
            new ReasoningFilter(false), new ReasoningFilter(true), new ReasoningFilter(true, true)
        };
        Random random = new Random(seed);
        for (String chunk : chunks) {
            compare(filters, filter -> filter.filter(chunk));
            if (random.nextBoolean()) compare(filters, ReasoningFilter::consumeReasoning);
            if (random.nextBoolean()) compare(filters, ReasoningFilter::consumeReasoningSignal);
            if (random.nextInt(11) == 0) compare(filters, ReasoningFilter::flush);
        }
        compare(filters, ReasoningFilter::flush);
        compare(filters, ReasoningFilter::consumeReasoning);
        compare(filters, ReasoningFilter::consumeReasoning);
        compare(filters, ReasoningFilter::consumeReasoningSignal);
        compare(filters, ReasoningFilter::consumeReasoningSignal);
        Field enabled = ReasoningFilter.class.getDeclaredField("useRust");
        enabled.setAccessible(true);
        for (int i = 1; i < filters.length; i++) {
            if (!enabled.getBoolean(filters[i])) throw new AssertionError("Rust silently fell back to Java");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args[0].equals("--production")) {
            ReasoningFilter production = new ReasoningFilter();
            ReasoningFilter java = new ReasoningFilter(false);
            String dense = "a<think>b</think>c".repeat(1000);
            for (String chunk : new String[] {"plain", dense, "<thi", "nk>x</think>", dense}) {
                equal(java.filter(chunk), production.filter(chunk));
                equal(java.consumeReasoning(), production.consumeReasoning());
                equal(java.consumeReasoningSignal(), production.consumeReasoningSignal());
            }
            equal(java.flush(), production.flush());
            Field enabled = ReasoningFilter.class.getDeclaredField("useRust");
            enabled.setAccessible(true);
            equal(true, enabled.getBoolean(production));
            System.out.println("Production library loading and adaptive filtering passed");
            return;
        }
        if (args[0].equals("--fallback")) {
            ReasoningFilter filter = new ReasoningFilter(true);
            equal("ac", filter.filter("a<think>b</think>c"));
            equal("b", filter.consumeReasoning());
            Field enabled = ReasoningFilter.class.getDeclaredField("useRust");
            enabled.setAccessible(true);
            equal(false, enabled.getBoolean(filter));
            equal("d", filter.filter("d"));
            System.out.println("Missing-library Java fallback passed");
            return;
        }
        System.load(args[0]);
        String[] cases = {
            "", "plain", "a<think>b</think>c<think>d</think>e",
            "<think>nested<think>x</think>tail", "<thi", "<think>x</thi",
            "Привет 🦀<think>世界😀</think>end", "\uD800<think>\u0000\uDC00</think>\uDFFF"
        };
        for (String text : cases) {
            for (int boundary = 0; boundary <= text.length(); boundary++) {
                run(new String[] {null, "", text.substring(0, boundary), text.substring(boundary)}, boundary);
            }
        }
        String dense = "a<think>b</think>c".repeat(1000);
        run(new String[] {"prefix<thi", "nk>x</think>", dense, "<think>", dense, "</think>tail"}, 1);
        Random random = new Random(20261110);
        String[] tokens = {"<think>", "</think>", "<", "think", ">", "/", "text", "🙂", "\uD800", "\u0000", "Ж"};
        for (int iteration = 0; iteration < 1000; iteration++) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < 50; i++) text.append(tokens[random.nextInt(tokens.length)]);
            String[] chunks = new String[text.length()];
            for (int i = 0; i < text.length(); i++) chunks[i] = text.substring(i, i + 1);
            run(chunks, iteration);
        }
        System.out.println("Java/Rust JNI and adaptive parity passed: " + checks + " comparisons");
    }
}
