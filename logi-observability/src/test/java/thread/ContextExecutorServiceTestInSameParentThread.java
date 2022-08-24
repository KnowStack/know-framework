package thread;

import com.didiglobal.logi.observability.Observability;
import com.didiglobal.logi.observability.conponent.thread.ContextFuture;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.SneakyThrows;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.*;

public class ContextExecutorServiceTestInSameParentThread {

    private static Tracer tracer = Observability.getTracer(ContextExecutorServiceTestInSameParentThread.class.getName());

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

        Span span = tracer.spanBuilder("main").startSpan();
        try (Scope scope = span.makeCurrent()) {
            System.out.println("start function main()");
            //2.）提交附带返回值任务
            Future<String> future = threadPool1.submit(new MyCallable());
            //3.）将范围值作为入参，新线程执行
            threadPool2.submit(new MyRunnable(future));
        } finally {
            span.end();
        }

        Thread.sleep(1000 * 60 * 4);

    }

    static class MyCallable implements Callable<String> {
        @Override
        public String call() throws Exception {
            System.out.println("MyCallable.call()");
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
            ContextFuture contextFuture = (ContextFuture) future;
            String msg = contextFuture.get().toString();
            System.out.println("MyRunnable.run()");
            System.out.println(" parameter is : " + msg);
        }
    }

}
