package com.personal.htool.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

public class LogUtil
{
    // loggerName
    protected final static String debugLogger = "DebugLog";
    protected final static String infoLogger = "InfoLog";
    protected final static String warnLogger = "WarnLog";
    protected final static String errorLogger = "ErrorLog";

    /**
     * 记录debug日志
     * 
     * @param format
     *            日志格式
     * @param args
     *            日志参数
     */
    public static void logDebug(String format, Object... args)
    {
        try
        {
            Logger logger = LoggerFactory.getLogger(debugLogger);
            logger.debug(format, args);
        }
        catch (Exception ex)
        {
        }
    }

    /**
     * 记录info信息
     * 
     * @param format
     *            日志格式
     * @param args
     *            日志参数
     */
    public static void logInfo(String format, Object... args)
    {
        try
        {
            Logger logger = LoggerFactory.getLogger(infoLogger);
            logger.info(format, args);
        }
        catch (Exception ex)
        {
        }
    }

    /**
     * 记录warn日志
     * 
     * @param format
     *            日志格式
     * @param args
     *            日志参数
     */
    public static void logWarn(String format, Object... args)
    {
        try
        {
            Logger logger = LoggerFactory.getLogger(warnLogger);
            logger.warn(format, args);
        }
        catch (Exception ex)
        {
        }
    }
    
    /**
     * 记录error日志
     * 
     * @param format
     *            日志格式
     * @param args
     *            日志参数
     */
    public static void logError(String format, Object... args)
    {
        try
        {
            Logger logger = LoggerFactory.getLogger(errorLogger);
            logger.error(format, args);
        }
        catch (Exception ex)
        {
        }
    }

    /**
     * 获取Exception堆栈信息
     * 
     * @param e
     *            异常对象
     * @return 异常堆栈信息字符串
     */
    public static String getExceptionStackTrace(Throwable e)
    {
        try
        {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw, true));
            String str = sw.toString();
            return str;
        }
        catch (Exception e2)
        {
            
        }
        return e.toString();
    }
}
