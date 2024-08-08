package ubc.pavlab.rdp.constants;

public class MetricsConstants {

    // JVM Metrics
    public static final String JVM_MEMORY_USED = "jvm.memory.used";
    public static final String JVM_MEMORY_MAX = "jvm.memory.max";
    public static final String JVM_GC_PAUSE = "jvm.gc.pause";

    // Health Metrics
    public static final String HEALTH_STATUS = "health.status";

    // Custom API Metrics
    public static final String API_REQUEST_COUNT = "api.request.count";
    public static final String API_RESPONSE_TIME = "api.response.time";


    public static final String SEARCH_API_REQUEST_COUNT = "search.api.request.count";
    public static final String SEARCH_API_RESPONSE_TIME = "search.api.response.time";

    // User Activity Metrics
    public static final String USER_LOGIN_ATTEMPTS = "user.login.attempts";
    public static final String USER_ACTIVITY_DURATION = "user.activity.duration";
}
