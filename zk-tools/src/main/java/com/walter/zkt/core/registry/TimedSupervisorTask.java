package com.walter.zkt.core.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author walter.tan
 * @date 2022-09-08 15:29
 */
public class TimedSupervisorTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TimedSupervisorTask.class);

    private final ScheduledExecutorService scheduler;
    private final ThreadPoolExecutor executor;
    private final long timeoutMillis;
    private final Runnable task;

    private final AtomicLong delay;
    private final long maxDelay;

    public TimedSupervisorTask(ScheduledExecutorService scheduler, ThreadPoolExecutor executor,
                               int timeout, TimeUnit timeUnit, int expBackOffBound, Runnable task) {
        this.scheduler = scheduler;
        this.executor = executor;
        this.timeoutMillis = timeUnit.toMillis(timeout);
        this.task = task;
        this.delay = new AtomicLong(timeoutMillis);
        this.maxDelay = timeoutMillis * expBackOffBound;
    }

    @Override
    public void run() {
        Future<?> future = null;
        try {
            future = executor.submit(task);
            // block until done or timeout
            future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            delay.set(timeoutMillis);
        } catch (TimeoutException e) {
            long currentDelay = delay.get();
            long newDelay = Math.min(maxDelay, currentDelay * 2);
            delay.compareAndSet(currentDelay, newDelay);

        } catch (RejectedExecutionException e) {
            if (executor.isShutdown() || scheduler.isShutdown()) {
                logger.warn("task supervisor shutting down, reject the task", e);
            } else {
                logger.warn("task supervisor rejected the task", e);
            }
        } catch(InterruptedException e){
            logger.error("task supervisor thread interrupted", e);
            Thread.currentThread().interrupt();
        }catch (Exception e) {
            if (executor.isShutdown() || scheduler.isShutdown()) {
                logger.warn("task supervisor shutting down, can't accept the task");
            } else {
                logger.warn("task supervisor threw an exception", e);
            }
        } finally {
            if (future != null) {
                future.cancel(true);
            }

            if (!scheduler.isShutdown()) {
                scheduler.schedule(this, delay.get(), TimeUnit.MILLISECONDS);
            }
        }
    }
}
