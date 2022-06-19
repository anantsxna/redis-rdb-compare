package org.threading;

import static java.util.concurrent.Executors.newFixedThreadPool;

import java.util.concurrent.ExecutorService;
import lombok.Builder;
import lombok.NonNull;

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
