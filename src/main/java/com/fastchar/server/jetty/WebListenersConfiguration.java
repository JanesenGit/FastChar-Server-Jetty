package com.fastchar.server.jetty;

import org.eclipse.jetty.servlet.ListenerHolder;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.Source;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;

import java.util.EventListener;
import java.util.Set;

public class WebListenersConfiguration extends AbstractConfiguration {

	private final Set<String> classNames;

	WebListenersConfiguration(Set<String> webListenerClassNames) {
		this.classNames = webListenerClassNames;
	}

	@Override
	public void configure(WebAppContext context) throws Exception {
		ServletHandler servletHandler = context.getServletHandler();
		for (String className : this.classNames) {
			configure(context, servletHandler, className);
		}
	}

	private void configure(WebAppContext context, ServletHandler servletHandler, String className)
			throws ClassNotFoundException {
		ListenerHolder holder = servletHandler.newListenerHolder(new Source(Source.Origin.ANNOTATION, className));
		holder.setHeldClass(loadClass(context, className));
		servletHandler.addListener(holder);
	}

	@SuppressWarnings("unchecked")
	private Class<? extends EventListener> loadClass(WebAppContext context, String className)
			throws ClassNotFoundException {
		ClassLoader classLoader = context.getClassLoader();
		classLoader = (classLoader != null) ? classLoader : getClass().getClassLoader();
		return (Class<? extends EventListener>) classLoader.loadClass(className);
	}

}