package com.kb.youngly.config;

import com.kb.youngly.util.YounglyTime;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;

/** MySQL/MariaDB의 NOW(), CURRENT_DATE, CURRENT_TIMESTAMP를 서비스 가상 시각과 맞춘다. */
@Intercepts({
        @Signature(
                type = StatementHandler.class,
                method = "prepare",
                args = {Connection.class, Integer.class}
        )
})
public class YounglyTimeSqlInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Connection connection = (Connection) invocation.getArgs()[0];
        long sessionTimestamp = YounglyTime.isOverridden() ? YounglyTime.epochSecond() : 0L;

        try (Statement statement = connection.createStatement()) {
            statement.execute("SET timestamp = " + sessionTimestamp);
        }

        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 별도 설정 없음
    }
}
