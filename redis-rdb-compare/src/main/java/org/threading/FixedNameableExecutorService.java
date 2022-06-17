package org.threading;

import lombok.Builder;
import lombok.NonNull;
import org.glassfish.grizzly.threadpool.FixedThreadPool;

import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;

@Builder
public class FixedNameableExecutorService {
    @NonNull
    private final String baseName;
    @NonNull
    private final int threadsNum;

    public ExecutorService getExecutorService() {
        return newFixedThreadPool(threadsNum, new NameableThreadFactory(baseName));
    }

}
