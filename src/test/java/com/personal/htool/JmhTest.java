package com.personal.htool;

import org.joda.time.DateTime;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * @author feihu5
 * @date 2018/8/6 16:09
 */
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Threads(8)
@Warmup(iterations = 3)
@Measurement(iterations = 10, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class JmhTest {
    static int millis = 24 * 3600 * 1000;

    public static void main(String[] args) throws Exception {
        Options options = new OptionsBuilder().include(JmhTest.class.getName()).forks(1).build();
        new Runner(options).run();
    }

    @Benchmark
    public void runCalendar() {
        Calendar calendar = Calendar.getInstance();
    }

    @Benchmark
    public void runJoda() {
        DateTime dateTime = new DateTime();
    }

    @Benchmark
    public void runSystem() {
        long result = System.currentTimeMillis() / millis;
    }
}
