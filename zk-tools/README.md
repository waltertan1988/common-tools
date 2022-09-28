# zk-tools
一个基于的zookeeper分布式协调工具集

## 功能
### 1. 分布式实例全局ID注册
#### 1.1 说明
在无状态服务集群的一些部署场景下，需要对服务实例在启动时按照某种方式注册一个全局ID，
比如：
* 对于同一个MQ主题，消费者集群下的一些实例只能消费一个分区，另一些实例只能消费另一个分区，但该集群下的所有实例都是复用同一份启动配置文件的。
* 给每个集群中的每个实例在启动时分配一个唯一的编号，如雪花算法的workerId。

**AbstractIdRegistry**类为ID注册的行为提供了统一抽象。
你还可以在所有实例启动完后进行自定义检查，看看最后实际的ID分配情况是否符合你的预期。

#### 1.2 AbstractIdRegistry的具体实现
##### 1.2.1 SequentialReusableIdRegistry
用于分配和管理顺序增长（从0开始）的可重用的全局实例id。

此注册器优先使用最小的**未被占用**的全局id，从而尽最大可能保证已分配过的全局id至少存在一个实例与之对应。
> 注：正常情况下，一个全局id仅对应一个实例。但允许存在一个异常的冲突情况是：一个已经注册过的实例由于网络原因而失去与zookeeper的连接，于此同时另一个实例又进行注册id，此时两个实例将会共用同一个全局id（如下图第5-8步），默认情况下旧实例的心跳检查会对此情况抛出ZktException异常。你可以自行扩展SequentialReusableIdRegistry.HeartbeatHandler来决定在旧实例中如何处理这种冲突情况。

![注册冲突的过程](https://raw.githubusercontent.com/waltertan1988/zk-tools/main/doc/design/registry/SequentialReusableIdRegistry_heartbeat.png "SequentialReusableIdRegistry_heartbeat.png")

1.2.1.1 Zookeeper中的数据结构
```
/{命名空间namespace} [container节点]
----/{主题topic} [container节点]
--------/ips [container节点]
------------/{实例A} => data: {id 0}
------------/{实例B} => data: {id 1}
------------/{实例Z} => data: {id N}
--------/ids [container节点]
------------/{0} => data: {实例A}
------------/{1} => data: {实例B}
------------/{N} => data: {实例Z}
```

1.2.1.2 应用场景
* MQ分区路由的顺序性保证（以RabbitMQ为例）
![RabbitMQ](https://raw.githubusercontent.com/waltertan1988/zk-tools/main/doc/design/registry/SequentialReusableIdRegistry.png "SequentialReusableIdRegistry.png")

##### 1.2.2 SequentialUniqueIdRegistry
用于分配和管理顺序增长（从0开始）的唯一全局实例id。

此注册器优先尽最大努力为每个实例分配不共享的全局id，因此每次调用构造方法时都将会产生出一个自增的新的全局id。
> 注：一旦即将分配的id超过最大阈值maxGlobalId（默认为2147483647），你可以通过设置globalIdExhaustedHandler来决定如何处理，默认情况下会抛出ZktException异常。

1.2.2.1 Zookeeper中的数据结构
```
/{命名空间namespace} [container节点]
----/{主题topic} [persistent节点] => data: {已分配的最大id}
```

1.2.2.2 应用场景
* 雪花算法的workerId

#### 1.3 开始使用
参看：[这里](https://github.com/waltertan1988/zk-tools/tree/main/src/test/java/com/walter/zkt/registry)
