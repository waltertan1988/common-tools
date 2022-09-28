package com.walter.orchestration.base;

import com.walter.orchestration.util.CastUtil;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ReflectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 抽象的调用链
 *
 * @author tyx
 * @date 2021/6/28
 *
 */
public abstract class AbstractChain implements InitializingBean {

    private List<AbstractChainNode> chainNodeList = new ArrayList<>();

    private Map<Class<? extends AbstractChainNode>, NodeParamResolver> chainNodeParamResolverMap = new HashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        // 注册链节点
        registChainNode();

        // 注册各个ChainNode的入参初始化解析器
        registChainNodeParamResolver(chainNodeParamResolverMap);
    }

    private void registChainNode(){
        chainNodeList.addAll(defineChainNodeList());

        registChainNodeByAnnotation();
    }

    private void registChainNodeByAnnotation(){
        if(!chainNodeList.isEmpty()){
            return;
        }

        List<OrderedWrapper> nodeList = new ArrayList<>();

        ReflectionUtils.doWithLocalFields(this.getClass(), field -> {
            if(field.isAnnotationPresent(ChainOrder.class)){
                int order = field.getAnnotation(ChainOrder.class).value();
                Object f = field.get(this);
                if(f instanceof AbstractChainNode){
                    nodeList.add(new OrderedWrapper(order, CastUtil.cast(f)));
                }
            }
        });

        Collections.sort(nodeList, Comparator.comparingInt(o -> o.order));

        chainNodeList.addAll(nodeList.stream().map(o -> (AbstractChainNode)o.object).collect(Collectors.toList()));
    }

    @AllArgsConstructor
    private class OrderedWrapper{
        private int order;
        private Object object;
    }

    /**
     * 开始调用业务链
     * @param context
     */
    public void start(ChainContext context){

        // 1. 调用链的前处理
        preProcessChain(context);

        // 2.顺序执行链节点方法
        for (AbstractChainNode node : chainNodeList) {
            NodeParamResolver paramResolver = chainNodeParamResolverMap.get(node.getClass());
            if(Objects.nonNull(paramResolver)){
                // 2.1 调用NodeParamResolver对Node的入参进行初始化
                node.initParamObject(paramResolver, context);
            }

            // 2.2 执行节点方法
            node.handle(context);

            // 2.3 节点的后处理方法
            node.postHandle(context);
        }

        // 3. 调用链的后处理
        postProcessChain(context);
    }

    /**
     * 所有链节点开始执行前，将执行此方法（可用于处理该链独有的业务逻辑）
     * @param context
     */
    protected void preProcessChain(ChainContext context){}

    /**
     * 所有链节点执行完毕后，将执行此方法（用于汇总各个节点的处理结果）
     * @param context
     */
    protected void postProcessChain(ChainContext context){}

    /**
     * 覆盖此方法可定义调用链的串联节点列表。注：此方法的顺序会覆盖由@{@link ChainOrder}所定义的顺序
     */
    protected List<AbstractChainNode> defineChainNodeList(){
        return new ArrayList<>();
    }

    /**
     * 注册链节点的入参解析器
     * @param chainNodeParamResolverMap
     */
    protected abstract void registChainNodeParamResolver(Map<Class<? extends AbstractChainNode>, NodeParamResolver> chainNodeParamResolverMap);
}
