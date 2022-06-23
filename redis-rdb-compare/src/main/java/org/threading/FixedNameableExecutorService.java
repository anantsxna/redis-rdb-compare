package org.threading;

import static java.util.concurrent.Executors.newFixedThreadPool;

import java.util.concurrent.ExecutorService;
import lombok.Builder;
import lombok.NonNull;

/**
 * Fixed number of threads, nameable executor service.
 */
@Builder
public class FixedNameableExecutorService {

    @NonNull
    private final String baseName;

    @Builder.Default
    private final int threadsNum = 1;

    public ExecutorService getExecutorService() {
        return newFixedThreadPool(threadsNum, new NameableThreadFactory(baseName));
    }
}
