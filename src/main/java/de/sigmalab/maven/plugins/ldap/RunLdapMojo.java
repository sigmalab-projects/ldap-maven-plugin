package de.sigmalab.maven.plugins.ldap;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "run", defaultPhase = LifecyclePhase.NONE)
public class RunLdapMojo extends AbstractMojo {

    static final String LDAP_SERVER_INSTANCE = "LDAP_SERVER_INSTANCE";

    @Parameter(property = "ldap.server.port", defaultValue = "53389")
    public Integer port;

    @Parameter(property = "ldap.server.skip", defaultValue = "false")
    public boolean skip;

    // something like 'dc=sigmalab,dc=de'
    @Parameter(property = "ldap.server.root", required = true)
    public String root;

    //
    @Parameter(property = "ldap.server.ldiff", required = true)
    public File ldiff;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skip) {
            getLog().info("LDAP-Server skipped ...");
            return;
        }

        try {

            ApacheDsContainer container = new ApacheDsContainer(root, ldiff);
            container.setPort(port);
            container.afterPropertiesSet();
            container.start();

            getPluginContext().put(LDAP_SERVER_INSTANCE, container);
            getLog().info("LDAP-Server started on port : " + port);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}
