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

    protected Query(QueryType _queryType) {
        queryType = _queryType;
        startTime = System.currentTimeMillis();
    }
    public abstract void execute();
    public abstract String result();

    protected void setExitCode(int code) {
        exitCode = code;
    }

    protected  int getExitCode() {
        return exitCode;
    }
}
