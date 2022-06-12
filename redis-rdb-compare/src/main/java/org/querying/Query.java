package org.querying;

public abstract class Query {

    public enum QueryType {
        TOP_K_CHILDREN,
        GET_COUNT,
    }

    private final QueryType queryType;
    private final long startTime;
    private long endTime;
    private int exitCode = -1;
    private String channelId;
    StringBuilder result;

    protected Query(QueryType _queryType, String _channelId) {
        queryType = _queryType;
        startTime = System.currentTimeMillis();
        channelId = _channelId;
        result = new StringBuilder();
    }

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
