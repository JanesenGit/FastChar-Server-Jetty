package com.fastchar.server.jetty;

import com.fastchar.servlet.FastJavaxServletContainerInitializer;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;

import java.util.HashSet;

public class ServletContextInitializerConfiguration extends AbstractConfiguration {

    @Override
    public void configure(WebAppContext context) throws Exception {
        super.configure(context);
        new FastJavaxServletContainerInitializer().onStartup(new HashSet<>(), context.getServletContext());
    }
}
