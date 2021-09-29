package com.inventage.portal.gateway.core.log;

import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.spi.logging.LogDelegate;
import org.slf4j.Logger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.LocationAwareLogger;

import static org.slf4j.spi.LocationAwareLogger.*;

/**
 * Custom implementation to enable RoutingContext information within every log entry.
 */
public class PortalGatewayLogDelegate implements LogDelegate {

    private static final String FQCN = io.vertx.core.logging.Logger.class.getCanonicalName();

    private final org.slf4j.Logger LOGGER;

    PortalGatewayLogDelegate(final String name) {
        LOGGER = org.slf4j.LoggerFactory.getLogger(name);;
    }

    @Override
    public boolean isWarnEnabled() {
        return LOGGER.isWarnEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return LOGGER.isInfoEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return LOGGER.isDebugEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
        return LOGGER.isTraceEnabled();
    }

    @Override
    public void fatal(Object message) {
        log(ERROR_INT, message);
    }

    @Override
    public void fatal(Object message, Throwable t) {
        log(ERROR_INT, message, t);
    }

    @Override
    public void error(Object message) {
        log(ERROR_INT, message);
    }

    @Override
    public void error(Object message, Object... params) {
        log(ERROR_INT, message, null, params);
    }

    @Override
    public void error(Object message, Throwable t) {
        log(ERROR_INT, message, t);
    }

    @Override
    public void error(Object message, Throwable t, Object... params) {
        log(ERROR_INT, message, t, params);
    }

    @Override
    public void warn(Object message) {
        log(WARN_INT, message);
    }

    @Override
    public void warn(Object message, Object... params) {
        log(WARN_INT, message, null, params);
    }

    @Override
    public void warn(Object message, Throwable t) {
        log(WARN_INT, message, t);
    }

    @Override
    public void warn(Object message, Throwable t, Object... params) {
        log(WARN_INT, message, t, params);
    }

    @Override
    public void info(Object message) {
        log(INFO_INT, message);
    }

    @Override
    public void info(Object message, Object... params) {
        log(INFO_INT, message, null, params);
    }

    @Override
    public void info(Object message, Throwable t) {
        log(INFO_INT, message, t);
    }

    @Override
    public void info(Object message, Throwable t, Object... params) {
        log(INFO_INT, message, t, params);
    }

    @Override
    public void debug(Object message) {
        log(DEBUG_INT, message);
    }

    @Override
    public void debug(Object message, Object... params) {
        log(DEBUG_INT, message, null, params);
    }

    @Override
    public void debug(Object message, Throwable t) {
        log(DEBUG_INT, message, t);
    }

    @Override
    public void debug(Object message, Throwable t, Object... params) {
        log(DEBUG_INT, message, t, params);
    }

    @Override
    public void trace(Object message) {
        log(TRACE_INT, message);
    }

    @Override
    public void trace(Object message, Object... params) {
        log(TRACE_INT, message, null, params);
    }

    @Override
    public void trace(Object message, Throwable t) {
        log(TRACE_INT, message, t);
    }

    @Override
    public void trace(Object message, Throwable t, Object... params) {
        log(TRACE_INT, message, t, params);
    }


    private void log(int level, Object message) {
        log(level, message, null);
    }

    private void log(int level, Object message, Throwable t) {
        log(level, message, t, (Object[]) null);
    }

    private void log(int level, Object message, Throwable t, Object... params) {
        String msg = (message == null) ? "NULL" : message.toString();

        // We need to compute the right parameters.
        // If we have both parameters and an error, we need to build a new array [params, t]
        // If we don't have parameters, we need to build a new array [t]
        // If we don't have error, it's just params.
        Object[] parameters = params;
        if (params != null  && t != null) {
            parameters = new Object[params.length + 1];
            System.arraycopy(params, 0, parameters, 0, params.length);
            parameters[params.length] = t;
        } else if (params == null  && t != null) {
            parameters = new Object[] {t};
        }

        writeLog(level, msg, parameters, t);
//        try (CompoundCloseable closeable = CompoundCloseable.create("traceId", "1").add("spanId", "2")) {
//        }
    }

    private void writeLog(int level, String msg, Object[] parameters, Throwable t) {
        if (LOGGER instanceof LocationAwareLogger) {
            // make sure we don't format the objects if we don't log the line anyway
            if (level == TRACE_INT && LOGGER.isTraceEnabled() ||
                    level == DEBUG_INT && LOGGER.isDebugEnabled() ||
                    level == INFO_INT && LOGGER.isInfoEnabled() ||
                    level == WARN_INT && LOGGER.isWarnEnabled() ||
                    level == ERROR_INT && LOGGER.isErrorEnabled()) {
                LocationAwareLogger l = (LocationAwareLogger) LOGGER;
//                FormattingTuple ft = MessageFormatter.arrayFormat(msg, parameters);
                l.log(null, FQCN, level, msg, parameters, t);
            }
        } else {
            switch (level) {
                case TRACE_INT:
                    LOGGER.trace(msg, parameters);
                    break;
                case DEBUG_INT:
                    LOGGER.debug(msg, parameters);
                    break;
                case INFO_INT:
                    LOGGER.info(msg, parameters);
                    break;
                case WARN_INT:
                    LOGGER.warn(msg, parameters);
                    break;
                case ERROR_INT:
                    LOGGER.error(msg, parameters);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown log level " + level);
            }
        }
    }
}
