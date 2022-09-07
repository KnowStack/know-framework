package com.didiglobal.logi.observability.thread;

import com.didiglobal.logi.log.ILog;
import com.didiglobal.logi.log.LogFactory;
import com.didiglobal.logi.observability.Observability;
import com.didiglobal.logi.observability.conponent.thread.ContextFuture;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.SneakyThrows;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import java.util.concurrent.*;

public class ContextExecutorServiceTestInDiffParentThread {

    private static Tracer tracer = Observability.getTracer(ContextExecutorServiceTestInDiffParentThread.class.getName());

    private static final ILog logger = LogFactory.getLog(ContextExecutorServiceTestInDiffParentThread.class);

    public static void main(String[] args) throws InterruptedException {

        //1.）封装线程 池
        ExecutorService threadPool1 = Observability.wrap(
                new ThreadPoolExecutor(1, 1,
                        0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>( 100 ),
                        new BasicThreadFactory.Builder().namingPattern("main-1").build())
        );

        ExecutorService threadPool2 = Observability.wrap(
                new ThreadPoolExecutor(1, 1,
                        0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>( 100 ),
                        new BasicThreadFactory.Builder().namingPattern("main-2").build())
        );

        Future<String> future = null;
        Span span = tracer.spanBuilder("main").startSpan();
        try (Scope scope = span.makeCurrent()) {
            logger.info("start function main()");
            //2.）提交附带返回值任务
            future = threadPool1.submit(new MyCallable());
        } finally {
            span.end();
        }

        //3.）将范围值作为入参，新线程执行
        threadPool2.submit(new MyRunnable(future));

        Thread.sleep(1000 * 60 * 4);

    }

    static class MyCallable implements Callable<String> {
        @Override
        public String call() throws Exception {
            logger.info("MyCallable.call()");
            return "SUCCESSFUL";
        }
    }

    static class MyRunnable implements Runnable {
        private Future future;
        public MyRunnable(Future future) {
            this.future = future;
        }
        @SneakyThrows
        @Override
        public void run() {
            try {
                ContextFuture contextFuture = (ContextFuture) future;
                String msg = contextFuture.get().toString();
                contextFuture.getContext().makeCurrent();
                Span span = tracer.spanBuilder("MyRunnable.run()").startSpan();
                try(Scope scope = span.makeCurrent()) {
                    logger.info("MyRunnable.run()");
                    logger.info(" parameter is : " + msg);
                } finally {
                    span.end();
                }
            } catch (Exception ex) {

            }
        }
    }

}
