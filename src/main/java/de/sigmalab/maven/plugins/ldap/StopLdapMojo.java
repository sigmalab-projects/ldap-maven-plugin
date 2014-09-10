package de.sigmalab.maven.plugins.ldap;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * @author  jbellmann
 */
@Mojo(name = "shutdown", defaultPhase = LifecyclePhase.NONE)
public class StopLdapMojo extends AbstractMojo {

    @Parameter(property = "ldap.server.skip", defaultValue = "false")
    public boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skip) {
            getLog().info("LDAP-Server skipped ...");
            return;
        }

        getLog().info("Stopping LDAP-Server....");

        ApacheDsContainer container = (ApacheDsContainer) getPluginContext().get(RunLdapMojo.LDAP_SERVER_INSTANCE);
        if (container != null) {
            container.stop();
            getLog().info("LDAP-Server stopped.");
        }

    }

}
