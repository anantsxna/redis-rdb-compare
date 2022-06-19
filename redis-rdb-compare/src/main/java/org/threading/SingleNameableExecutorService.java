package org.threading;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import java.util.concurrent.ExecutorService;
import lombok.Builder;
import lombok.NonNull;

/**
 * Single, nameable executor service.
 */
@Builder
public class SingleNameableExecutorService {

    @NonNull
    private final String baseName;

    public ExecutorService getExecutorService() {
        return newSingleThreadExecutor(new NameableThreadFactory(baseName));
    }
}
