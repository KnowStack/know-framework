package com.didiglobal.knowframework.observability.conponent.mybatis;

import com.didiglobal.knowframework.observability.common.constant.Constant;
import com.didiglobal.knowframework.observability.Observability;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;
import java.util.Properties;

@Slf4j
@Intercepts(
        {
                @Signature(
                        type = Executor.class,
                        method = "query",
                        args = {
                                MappedStatement.class,
                                Object.class,
                                RowBounds.class,
                                ResultHandler.class
                        }
                ),
                @Signature(
                        type = Executor.class,
                        method = "update",
                        args = {
                                MappedStatement.class,
                                Object.class
                        }
                )
        }
)
@Component
public class ObservabilityInterceptor implements Interceptor {

    private Tracer tracer = Observability.getTracer(ObservabilityInterceptor.class.getName());

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String clazzName = invocation.getTarget().getClass().getName();
        String methodName = invocation.getMethod().getName();
        Span span = tracer.spanBuilder(String.format("%s.%s", clazzName, methodName)).startSpan();
        try (Scope scope = span.makeCurrent()) {
            // ?????????????????????args??????????????????????????????
            // 1. ??????MappedStatement??????, ???????????????SQL????????????
            MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
            SqlCommandType commandType = ms.getSqlCommandType();
            // 2. ?????????????????????????????????, ????????????Java Bean, ?????????????????????????????????, ???????????????????????????
            // ????????????, ?????? @Param ?????????????????? Map ??????, ?????????????????? Mybatis ????????? Map ??????
            // ?????? org.apache.ibatis.binding.MapperMethod$ParamMap
            Object parameter = invocation.getArgs()[1];
            // 3. ???????????? sql ??? ???
            String sql = ms.getBoundSql(parameter).getSql();
            span.setAttribute(Constant.ATTRIBUTE_KEY_SQL_STATEMENT, sql);
            span.setAttribute(Constant.ATTRIBUTE_KEY_SQL_TYPE, commandType.name());
            Object result = invocation.proceed();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Throwable ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }
 
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
 
    @Override
    public void setProperties(Properties properties) {
    }

}