package lab;

import java.util.ArrayList;
import java.util.List;

public class RequestContext {

    private final String requestId;
    // final 固定的是列表引用；列表里的内容仍然可以修改。
    private final List<String> logs = new ArrayList<>();

    public RequestContext(String requestId) {
        this.requestId = requestId;
    }

    /** 创建工作任务上下文：复用不可变的请求 ID，新对象会拥有新的空日志列表。 */
    public RequestContext forWorker() {
        return new RequestContext(requestId);
    }

    public String getRequestId() {
        return requestId;
    }

    public List<String> getLogs() {
        return logs;
    }
}
