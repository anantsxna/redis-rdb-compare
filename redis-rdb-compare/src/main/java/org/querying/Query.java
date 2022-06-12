package org.querying;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public abstract class Query {

    public enum QueryType {
        TOP_K_CHILDREN,
        GET_COUNT,
    }

    @NonNull
    private final QueryType queryType;

    private final long startTime;
    private long endTime;
    private int exitCode;
    private final String channelId;
    StringBuilder result;

    public abstract void execute();

    public abstract String result();

    protected void setExitCode(int code) {
        exitCode = code;
    }

    protected int getExitCode() {
        return exitCode;
    }

    protected String getChannelId() {
        return channelId;
    }
}
