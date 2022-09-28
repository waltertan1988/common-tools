package com.walter.orchestration.base;

/**
 * 链节点的入参解析器
 *
 * @author tyx
 * @date 2021/6/28
 *
 */
public interface NodeParamResolver {

    void resolve(ChainContext context);
}
