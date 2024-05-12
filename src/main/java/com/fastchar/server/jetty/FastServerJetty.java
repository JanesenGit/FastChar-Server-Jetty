package com.fastchar.server.jetty;

import com.fastchar.core.FastChar;
import com.fastchar.server.ServerStartHandler;
import com.fastchar.utils.FastClassUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

public class FastServerJetty {

    public static FastServerJetty getInstance() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        Class<?> fromClass = FastClassUtils.findClass(stackTrace[2].getClassName());
        if(fromClass == null) {
            throw new NullPointerException("fromClass must not be null");
        }
        FastChar.getPath().setProjectJar(fromClass);
        return new FastServerJetty().initServer();
    }

    public FastServerJetty() {
    }

    private Server server;
    private final ServerStartHandler starterHandler = new ServerStartHandler();

    private FastServerJetty initServer() {
        starterHandler.setStartRunnable(() -> {
            try {
                if (server != null) {
                    server.start();
                }
            } catch (Exception e) {
                stop();
                throw new RuntimeException(e);
            }
        }).setStopRunnable(this::stop);
        return this;
    }

    public synchronized void start(FastJettyConfig jettyConfig) {
        try {
            if (server != null) {
                return;
            }
            FastChar.getConstant().setEmbedServer(true);

            server = new Server(jettyConfig.determineThreadPool());
            jettyConfig.configConnector(server);


            WebAppContext webAppContext = new WebAppContext();
            webAppContext.setContextPath(jettyConfig.getContextPath());
            webAppContext.setMaxFormContentSize(FastChar.getConstant().getAttachMaxPostSize());

            jettyConfig.configureDocumentRoot(webAppContext);
            jettyConfig.configureConfiguration(webAppContext);
            jettyConfig.configureSession(webAppContext);

            webAppContext.setClassLoader(Thread.currentThread().getContextClassLoader());
            webAppContext.setConfigurationDiscovered(true);
            webAppContext.setParentLoaderPriority(true);
            webAppContext.setThrowUnavailableOnStartupException(true);

            jettyConfig.addDefaultServlet(webAppContext);
            jettyConfig.addJspServlet(webAppContext);

            server.setHandler(jettyConfig.addHandlerWrappers(webAppContext));
            this.starterHandler
                    .setPort(jettyConfig.getPort())
                    .setHost(jettyConfig.getHost())
                    .setContextPath(jettyConfig.getContextPath())
                    .start();
        } catch (Exception e) {
            stop();
            throw new RuntimeException(e);
        }
    }


    public synchronized void stop() {
        try {
            if (this.server != null) {
                this.server.stop();
                this.server.destroy();
                this.server = null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
