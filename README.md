# 线程池学习项目：从一个算价请求开始

这个项目陪你逐步读懂 `search-service-calc-price` 的线程池替换。每次只增加一种能力：先跑、预测输出、改代码、解释结果，再回到业务代码找对应位置。

使用 JDK 21，没有第三方依赖。现阶段实现第一课；后续课程是练习路线，还没有预先写好答案。教学参数不代表线上推荐配置。

## 学习场景

请求 REQ-001 包含母房型 A、B、C。每个母房型是一个 Callable，返回一个以分为单位的模拟价格；请求线程取得三个结果并汇总。最终再加入每任务日志、请求上下文、批次超时和独立日志池。

第一课用 sleep 模拟耗时，便于观察调度；它不是实际算价算法，也不能用来预测 CPU 密集算价的性能。真实业务的 ParallelRunner 使用 Callable<Void>，结果写回业务对象；本课先直接返回价格，降低阅读难度。

## 开始运行

```bash
cd /Users/li.zhiqiang/playground/threadpool-lab
bash run.sh serial
bash run.sh pool 2
```

脚本优先使用 JAVA_HOME，否则在 macOS 上寻找 JDK 21，然后使用 javac 编译、java 运行。也可以在 IDEA 中打开 pom.xml，选择 JDK 21，运行 Lesson01.main，参数填 `pool 2`。脚本不需要 Maven 下载插件。

先读 [第一课](lessons/01-submit-and-future.md)，再读 Lesson01.java；暂时不需要研究 Trace.java。

## 练习路线与业务代码对应

| 课次 | 亲手做的事情 | 要解释清楚的问题 | 对应本次迁移 |
| --- | --- | --- | --- |
| 1：任务与线程 | 比较直接 call、批量 submit、Future.get；把等待位置改错再改回来 | 谁执行 call？get 会启动任务吗？任务和线程为什么不是一一对应？ | ParallelRunner 生成 Callable；旧 Parallels 的任务提交 |
| 2：池与队列 | 手写 ThreadPoolExecutor；用 CountDownLatch 固定占住工作线程后再提交任务 | core、max、queue 的接收顺序是什么？无界队列下 max 为什么通常不起作用？ | 旧算价池 CPU×2/无界队列；新池容量配置 |
| 3：拒绝与隔离 | 制造满池；分别观察 AbortPolicy 和 CallerRunsPolicy；增加独立日志池 | 拒绝发生在哪个线程？为什么异步日志不能因 CallerRuns 回到请求线程？ | Provider 的 ABORT；submitLogTask 返回 boolean |
| 4：异常与批次等待 | 改为 invokeAll；让一个任务抛异常；比较提交异常与 Future.get 异常 | ExecutionException 包了什么？全部等待是否等于快速失败？ | 旧 invokeAll；新 ThreadExecutor.executeAllSync |
| 5：超时与取消 | 使用带超时的 invokeAll；观察中断；做一个有明确结束时间但暂不响应中断的任务 | 批次预算和每任务预算有什么区别？cancel(true) 为何不等于强制停止？ | 15 秒批次预算；取消和迟到任务 |
| 6：上下文 | 写 RequestScope 和 ThreadLocal；连续请求复用单个工作线程；补上 finally 恢复 | 父线程的数据为何不能自动读到？哪些数据共享、哪些复制？不清理会怎样？ | SSContext.callWith；RequestScope.forWorker |
| 7：包装与合并 | 写 Callable 装饰器；捕获父上下文、运行时绑定子上下文；按任务下标收集日志 | 包装时和执行时分别在哪个线程？synchronized 保护什么？为何需要 closed？ | ThreadContextualExecutor；ThreadContextHandler |
| 8：整理与对照 | 将上一课代码拆成 Provider、执行门面、Handler；增加模拟链路包装，再读真实 SDK | 每层解决什么问题？FutureTask 在哪层？清理为何必须用 finally？ | 新旧实现对照；SDK、CatAsync.wrap、QConfig 边界 |

第二课起用 CountDownLatch 等协调工具制造可重复的场景，不依赖“多睡一会儿应该就到了”。异常、取消、上下文残留、日志合并等练习需要检查实际结果，不能只以“程序没有报错”为标准。

## 当前实现与生产代码的边界

- 第一课固定使用少量线程，方便看清线程复用；旧算价池实际线程数是 JVM 可见处理器数乘 2。
- 第一课是 submit 后逐个 get，下一步才练习旧实现所用的 invokeAll。
- 第一课使用新建的池并在程序退出前关闭；服务里的池通常跨请求共享，不能每个请求都新建、关闭。
- 当前没有接入公司 SDK、CAT、QConfig，也没有假装用本地模拟替代真实实现。第八课会明确区分模拟包装与真实 CAT 的行为。
- 新实现仍然有真正执行任务的底层线程池；Provider、Handler、门面是在组织选池、提交、上下文和收尾职责。

## 每课的完成标准

你能指出执行线程、解释关键方法、预测一种代码改动的后果，并通过输出验证。先理解正确用法，再用受控实验观察错误；不一次引入 CompletableFuture、虚拟线程或复杂框架。

第一次练习完成后，把两次运行输出及第一课三个问题的回答发给我，我们接着改代码。
