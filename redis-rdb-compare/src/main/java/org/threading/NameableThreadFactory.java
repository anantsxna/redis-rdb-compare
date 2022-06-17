package org.threading;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NameableThreadFactory implements ThreadFactory{
    private final AtomicInteger threadsNum = new AtomicInteger();

    private final String namePattern;

    public NameableThreadFactory(String baseName){
        namePattern = baseName + "-%d";
    }

    @Override
    public Thread newThread(@NotNull Runnable runnable){
        return new Thread(runnable, String.format(namePattern, threadsNum.addAndGet(1)));
    }
}