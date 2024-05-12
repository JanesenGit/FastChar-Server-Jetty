package com.fastchar.server.jetty;

import com.fastchar.core.FastChar;
import com.fastchar.server.SameSite;
import com.fastchar.server.StaticResourceJars;
import com.fastchar.utils.FastStringUtils;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

public class FastJettyConfig {


    private int port = 8080;


    private String host = "0.0.0.0";

    private String contextPath = "/";

    private String docBase;


    private int minResponseSize = 2 * 1024 * 1024;


    private boolean gzip = true;

    private final Threads threadPool = new Threads();

    private boolean http2;

    private SameSite cookieSameSite;


    private final List<URL> resources = new ArrayList<>();
    private final List<Configuration> configurations = new ArrayList<>();
    private final Set<String> webListenerClassNames = new HashSet<>();

    public int getPort() {
        return port;
    }

    public FastJettyConfig setPort(int port) {
        this.port = port;
        return this;
    }

    public String getContextPath() {
        return contextPath;
    }

    public FastJettyConfig setContextPath(String contextPath) {
        this.contextPath = contextPath;
        return this;
    }

    public String getDocBase() {
        return docBase;
    }

    public FastJettyConfig setDocBase(String docBase) {
        this.docBase = docBase;
        return this;
    }

    public FastJettyConfig addConfiguration(Configuration... configurations) {
        this.configurations.addAll(Arrays.asList(configurations));
        return this;
    }

    public FastJettyConfig addWebListeners(String... webListenerClassNames) {
        this.webListenerClassNames.addAll(Arrays.asList(webListenerClassNames));
        return this;
    }

    public int getMinResponseSize() {
        return minResponseSize;
    }

    public FastJettyConfig setMinResponseSize(int minResponseSize) {
        this.minResponseSize = minResponseSize;
        return this;
    }

    public boolean isGzip() {
        return gzip;
    }

    public FastJettyConfig setGzip(boolean gzip) {
        this.gzip = gzip;
        return this;
    }

    public FastJettyConfig addResources(URL path) {
        this.resources.add(path);
        return this;
    }


    public FastJettyConfig addResources(File path) {
        try {
            this.resources.add(path.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return this;
    }


    public boolean isHttp2() {
        return http2;
    }

    public FastJettyConfig setHttp2(boolean http2) {
        this.http2 = http2;
        return this;
    }

    public String getHost() {
        return host;
    }

    public FastJettyConfig setHost(String host) {
        this.host = host;
        return this;
    }

    public SameSite getCookieSameSite() {
        return cookieSameSite;
    }

    public FastJettyConfig setCookieSameSite(SameSite cookieSameSite) {
        this.cookieSameSite = cookieSameSite;
        return this;
    }

    public Threads getThreadPool() {
        return threadPool;
    }

    public static class Threads {

        /**
         * Number of acceptor threads to use. When the value is -1, the default, the
         * number of acceptors is derived from the operating environment.
         */
        private Integer acceptors = -1;

        /**
         * Number of selector threads to use. When the value is -1, the default, the
         * number of selectors is derived from the operating environment.
         */
        private Integer selectors = -1;

        /**
         * Maximum number of threads.
         */
        private Integer max = 1000;

        /**
         * Minimum number of threads.
         */
        private Integer min = 10;

        /**
         * Maximum capacity of the thread pool's backing queue. A default is computed
         * based on the threading configuration.
         */
        private Integer maxQueueCapacity;

        /**
         * Maximum thread idle time.
         */
        private Duration idleTimeout = Duration.ofMillis(60000);

        public Integer getAcceptors() {
            return this.acceptors;
        }

        public void setAcceptors(Integer acceptors) {
            this.acceptors = acceptors;
        }

        public Integer getSelectors() {
            return this.selectors;
        }

        public void setSelectors(Integer selectors) {
            this.selectors = selectors;
        }

        public void setMin(Integer min) {
            this.min = min;
        }

        public Integer getMin() {
            return this.min;
        }

        public void setMax(Integer max) {
            this.max = max;
        }

        public Integer getMax() {
            return this.max;
        }

        public Integer getMaxQueueCapacity() {
            return this.maxQueueCapacity;
        }

        public void setMaxQueueCapacity(Integer maxQueueCapacity) {
            this.maxQueueCapacity = maxQueueCapacity;
        }

        public void setIdleTimeout(Duration idleTimeout) {
            this.idleTimeout = idleTimeout;
        }

        public Duration getIdleTimeout() {
            return this.idleTimeout;
        }

    }


    ThreadPool determineThreadPool() {
        BlockingQueue<Runnable> queue = determineBlockingQueue(this.threadPool.getMaxQueueCapacity());
        int maxThreadCount = (this.threadPool.getMax() > 0) ? this.threadPool.getMax() : 200;
        int minThreadCount = (this.threadPool.getMin() > 0) ? this.threadPool.getMin() : 8;
        int threadIdleTimeout = (this.threadPool.getIdleTimeout() != null) ? (int) this.threadPool.getIdleTimeout().toMillis()
                : 60000;
        return new QueuedThreadPool(maxThreadCount, minThreadCount, threadIdleTimeout, queue);
    }

    BlockingQueue<Runnable> determineBlockingQueue(Integer maxQueueCapacity) {
        if (maxQueueCapacity == null) {
            return null;
        }
        if (maxQueueCapacity == 0) {
            return new SynchronousQueue<>();
        } else {
            return new BlockingArrayQueue<>(maxQueueCapacity);
        }
    }

    public void configConnector(Server server) {
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setSendServerVersion(false);
        List<ConnectionFactory> connectionFactories = new ArrayList<>();
        connectionFactories.add(new HttpConnectionFactory(httpConfiguration));
        if (isHttp2()) {
            connectionFactories.add(new HTTP2CServerConnectionFactory(httpConfiguration));
        }
        ServerConnector connector = new ServerConnector(server, this.threadPool.acceptors, this.threadPool.selectors,
                connectionFactories.toArray(new ConnectionFactory[0]));
        connector.setHost(this.host);
        connector.setPort(this.port);

        server.setConnectors(new Connector[]{connector});
        server.setStopTimeout(0);
    }


    public void configureDocumentRoot(WebAppContext webAppContext) {
        if (FastStringUtils.isEmpty(this.docBase)) {
            this.docBase = FastChar.getPath().getWebRootPath();
        }

        File docBase = new File(this.docBase);
        try {
            List<Resource> resources = new ArrayList<>();
            Resource rootResource = (docBase.isDirectory() ? Resource.newResource(docBase.getCanonicalFile())
                    : JarResource.newJarResource(Resource.newResource(docBase)));

            resources.add(rootResource);
            StaticResourceJars staticResourceJars = new StaticResourceJars();
            List<URL> staticResourceJarsUrls = staticResourceJars.getUrls();
            for (URL resourceJarUrl : staticResourceJarsUrls) {
                for (String internalPath : StaticResourceJars.INTERNAL_PATHS) {
                    String realUrlPath = resourceJarUrl + FastStringUtils.stripStart(internalPath, "/");
                    if ("file".equals(resourceJarUrl.getProtocol())) {
                        File file = new File(resourceJarUrl.toURI());
                        if (file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
                            realUrlPath = "jar:" + resourceJarUrl + "!/" + FastStringUtils.stripStart(internalPath, "/");
                        }
                    }
                    Resource resource = Resource.newResource(realUrlPath);
                    if (resource.exists() && resource.isDirectory()) {
                        resources.add(resource);
                    }
                }
            }
            for (URL url : this.resources) {
                Resource resource = Resource.newResource(url);
                if (resource.exists() && resource.isDirectory()) {
                    resources.add(resource);
                }
            }
            webAppContext.setBaseResource(new ResourceCollection(resources.toArray(new Resource[0])));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Handler addHandlerWrappers(Handler handler) {
        if (isGzip()) {
            GzipHandler gzipHandler = new GzipHandler();
            gzipHandler.setMinGzipSize(this.minResponseSize);
            gzipHandler.setIncludedMimeTypes("text/html", "text/xml", "text/plain", "text/css", "text/javascript",
                    "application/javascript", "application/json", "application/xml");
            for (HttpMethod httpMethod : HttpMethod.values()) {
                gzipHandler.addIncludedMethods(httpMethod.name());
            }
            handler = applyWrapper(handler, gzipHandler);
        }

        if (getCookieSameSite() != null) {
            handler = applyWrapper(handler, new SuppliedSameSiteCookieHandlerWrapper(this));
        }
        return handler;
    }

    private Handler applyWrapper(Handler handler, HandlerWrapper wrapper) {
        wrapper.setHandler(handler);
        return wrapper;
    }


    public void configureConfiguration(WebAppContext context) {
        List<Configuration> list = new ArrayList<>();
        list.add(new ServletContextInitializerConfiguration());
        list.add(new WebXmlConfiguration());
        list.add(new WebInfConfiguration());
        list.add(new MetaInfConfiguration());
        list.add(new FragmentConfiguration());
        list.add(new WebListenersConfiguration(this.webListenerClassNames));
        list.addAll(this.configurations);

        context.setConfigurations(list.toArray(new Configuration[]{}));
    }


    public void configureSession(WebAppContext context) {
        SessionHandler handler = context.getSessionHandler();
        handler.setMaxInactiveInterval(FastChar.getConstant().getSessionMaxInterval());
    }


    public void addDefaultServlet(WebAppContext context) {
        ServletHolder holder = new ServletHolder();
        holder.setName("default");
        holder.setClassName("org.eclipse.jetty.servlet.DefaultServlet");
        holder.setInitParameter("dirAllowed", "false");
        holder.setInitOrder(1);
        context.getServletHandler().addServletWithMapping(holder, "/");
        ServletMapping servletMapping = context.getServletHandler().getServletMapping("/");
        servletMapping.setDefault(true);
    }


    /**
     * Add Jetty's {@code JspServlet} to the given {@link WebAppContext}.
     *
     * @param context the jetty {@link WebAppContext}
     */
    public void addJspServlet(WebAppContext context) {
        ServletHolder holder = new ServletHolder();
        holder.setName("jsp");
        holder.setClassName("org.apache.jasper.servlet.JspServlet");
        holder.setInitParameter("fork", "false");
        holder.setInitOrder(3);
        context.getServletHandler().addServlet(holder);
        ServletMapping mapping = new ServletMapping();
        mapping.setServletName("jsp");
        mapping.setPathSpecs(new String[]{"*.jsp", "*.jspx"});
        context.getServletHandler().addServletMapping(mapping);
    }


}
