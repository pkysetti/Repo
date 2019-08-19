package org.blueprism.replicated;

import org.apache.log4j.Logger;

public class MyLogger {
    final static Logger logger = Logger.getLogger(InfinispanReplicated.class);
    /**
     *
     */
    public static void log(String msg, Object... params) {
        logger.info(String.format(msg, params));
    }

    /**
     *
     */
    public static void error(String msg, Object... params) {
        logger.error(String.format(msg, params));
    }

    /**
     *
     */
    public static void error(Exception ex, String msg, Object... params) {
        logger.error(String.format(msg, params), ex);
    }

    /**
     *
     */
    public static void exception(Exception e) {
        logger.error("!!!!! Got exception " + e.getMessage(), e);
    }

    /**
     *
     */
    public static void debug(String msg, Object... params) {
        logger.debug(String.format(msg, params));
    }
}