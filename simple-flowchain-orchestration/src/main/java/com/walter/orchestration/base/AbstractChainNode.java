package com.walter.orchestration.base;

import com.walter.orchestration.util.CastUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象调用链的节点
 *
 * @author tyx
 * @date 2021/6/28
 *
 */
@Slf4j
public abstract class AbstractChainNode<I, R> {

    private final String REQ_CONTEXT_KEY = this.getClass().getName() + "$Req";
    private final String RES_CONTEXT_KEY = this.getClass().getName() + "$Res";

    /**
     * 初始化输入参数对象
     * @param nodeParamResolver
     */
    void initParamObject(NodeParamResolver nodeParamResolver, ChainContext context){
        nodeParamResolver.resolve(context);
    }

    /**
     * 调用业务方法
     * @param context
     * @param req
     */
    public abstract R handle(ChainContext context, I req);

    /**
     * 预留的后处理方法
     * @param context
     */
    public void postHandle(ChainContext context){}

    void handle(ChainContext context){
        I req = CastUtil.cast(context.get(getRequestContextKey()));
        R res = handle(context, req);
        context.put(this.getResponseContextKey(), res);
    }

    /**
     * 返回链节点的输入参数 在上下文中的key
     * @return
     */
    public String getRequestContextKey(){
        return REQ_CONTEXT_KEY;
    }

    /**
     * 返回链节点的输出参数 在上下文中的key
     * @return
     */
    public String getResponseContextKey(){
        return RES_CONTEXT_KEY;
    }
}
