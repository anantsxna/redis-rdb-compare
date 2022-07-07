package org.querying;

import com.slack.api.model.block.LayoutBlock;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.messaging.Blocks;

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
    StringBuilder text = new StringBuilder();

    @Builder.Default
    List<LayoutBlock> result = new ArrayList<>();

    public abstract void execute(); //abstract method for executing the query

    public abstract List<LayoutBlock> result(); //abstract method for returning the result of the query
}
