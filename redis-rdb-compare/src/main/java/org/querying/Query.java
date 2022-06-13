package org.querying;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract class for all queries.
 */
@SuperBuilder
public abstract class Query {

    public enum QueryType {
        TOP_K_CHILDREN,
        GET_COUNT,
    }

    @NonNull
    private final QueryType queryType;

    private long executionTime;

    @Builder.Default
    @Getter
    @Setter
    private int exitCode = -1;

    @NonNull
    @Getter
    private final String channelId;

    @Builder.Default
    StringBuilder result = new StringBuilder();

    public abstract void execute(); //abstract method for executing the query

    public abstract String result(); //abstract method for returning the result of the query
}
