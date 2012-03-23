package brooklyn.demo

import java.util.List
import java.util.Map

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import brooklyn.config.BrooklynProperties
import brooklyn.entity.Entity
import brooklyn.entity.basic.AbstractApplication
import brooklyn.entity.basic.Entities
import brooklyn.entity.proxy.nginx.NginxController
import brooklyn.entity.webapp.ControlledDynamicWebAppCluster
import brooklyn.entity.webapp.DynamicWebAppCluster
import brooklyn.entity.webapp.JavaWebAppService
import brooklyn.entity.webapp.jboss.JBoss7Server
import brooklyn.launcher.BrooklynLauncher
import brooklyn.location.Location
import brooklyn.location.basic.CommandLineLocations
import brooklyn.policy.ResizerPolicy
import brooklyn.util.CommandLineUtil

/**
 * Run with:
 *   java -Xmx512m -Xms128m -XX:MaxPermSize=256m -cp target/brooklyn-example-*-with-dependencies.jar brooklyn.demo.WebClusterExample 
 **/
public class WebClusterExample extends AbstractApplication {
    public static final Logger LOG = LoggerFactory.getLogger(WebClusterExample)
    
    static BrooklynProperties config = BrooklynProperties.Factory.newDefault()

    public static final List<String> DEFAULT_LOCATION = [ CommandLineLocations.LOCALHOST ]

    public static final String WAR_PATH = "classpath://hello-world-webapp.war"
    
    public WebClusterExample(Map props=[:]) {
        super(props)
        setConfig(JavaWebAppService.ROOT_WAR, WAR_PATH)
    }
    

    protected JavaWebAppService newWebServer(Map flags, Entity cluster) {
        return new JBoss7Server(flags, cluster).configure(httpPort: "8000+")
    }

    NginxController nginxController = new NginxController(
        domain: 'webclusterexample.brooklyn.local',
        port:8080)

    ControlledDynamicWebAppCluster webCluster = new ControlledDynamicWebAppCluster(this,
        name: "WebApp cluster",
        controller: nginxController,
        initialSize: 1,
        webServerFactory: this.&newWebServer )
    
    ResizerPolicy policy = new ResizerPolicy(DynamicWebAppCluster.AVERAGE_REQUESTS_PER_SECOND).
        setSizeRange(1, 5).
        setMetricRange(10, 100);
    

    public static void main(String[] argv) {
        ArrayList args = new ArrayList(Arrays.asList(argv));
        int port = CommandLineUtil.getCommandLineOptionInt(args, "--port", 8081);
        List<Location> locations = CommandLineLocations.getLocationsById(args ?: [DEFAULT_LOCATION])

        WebClusterExample app = new WebClusterExample(name:'Brooklyn WebApp Cluster example')
            
        BrooklynLauncher.manage(app, port)
        app.start(locations)
        Entities.dumpInfo(app)
        
        app.webCluster.cluster.addPolicy(app.policy)
    }
    
}
