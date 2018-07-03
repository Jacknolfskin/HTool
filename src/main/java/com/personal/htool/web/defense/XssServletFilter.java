package com.personal.htool.web.defense;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created with idea
 * author:coderSu
 * Date:2017/9/6
 * Time:14:49
 */
public class XssServletFilter implements Filter {

    private FilterConfig filterConfig;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(new XssServletRequestWrapper((HttpServletRequest) request), response);

    }

    @Override
    public void destroy() {
        this.filterConfig = null;
    }

}
