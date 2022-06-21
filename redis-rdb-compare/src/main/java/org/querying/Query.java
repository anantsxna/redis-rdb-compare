package org.querying;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Abstract class for all queries.
 */
@SuperBuilder
public abstract class Query {

    public enum QueryType {
        GET_NEXT,
        GET_COUNT,
    }

    protected static final String INVALID_REQUEST_ID =
        "Invalid Request Id. Use \"/list\" to see all active Request Ids.";

    @NonNull
    private final QueryType queryType;

    private long executionTime;

    @Builder.Default
    @Getter
    @Setter
    private int exitCode = -1;

    @NonNull
    @Getter
    private final String requestId;

    @Builder.Default
    StringBuilder result = new StringBuilder();

    public abstract void execute(); //abstract method for executing the query

    public abstract String result(); //abstract method for returning the result of the query
}
