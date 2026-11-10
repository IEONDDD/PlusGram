package com.plusgram.app.ai.network;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;

/** Desktop microbenchmark; native allocations and Android ART are not measured. */
public final class ReasoningFilterBenchmark {
    private static volatile long sink;
    private static void run(int backend, String text, int iterations) {
        long result = 0;
        for (int i = 0; i < iterations; i++) {
            ReasoningFilter filter = new ReasoningFilter(backend != 0, backend == 2);
            result += filter.filter(text).length();
            String reasoning = filter.consumeReasoning();
            result += reasoning == null ? 0 : reasoning.length();
            result += filter.flush().length();
        }
        sink = result;
    }
    public static void main(String[] args) {
        System.load(args[0]);
        ThreadMXBean allocations = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        allocations.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        String[] names = {"Java", "Rust JNI", "Adaptive"};
        for (boolean dense : new boolean[] {false, true}) {
            for (int size : new int[] {64, 4096, 65536}) {
                String text = dense ? "a<think>b</think>c".repeat(Math.max(1, size / 18))
                        : "a".repeat(size / 2) + "<think>" + "b".repeat(size / 2) + "</think>c";
                int iterations = size > 10000 ? 500 : 20000;
                for (int backend = 0; backend < names.length; backend++) {
                    run(backend, text, 1000);
                    long bytes = allocations.getThreadAllocatedBytes(thread);
                    long start = System.nanoTime();
                    run(backend, text, iterations);
                    long nanos = System.nanoTime() - start;
                    bytes = allocations.getThreadAllocatedBytes(thread) - bytes;
                    System.out.printf(java.util.Locale.ROOT, "%s %s %d chars: %.2f us/op, %d Java bytes/op%n",
                            dense ? "dense" : "sparse", names[backend], text.length(),
                            nanos / 1000.0 / iterations, bytes / iterations);
                }
            }
        }
    }
}
