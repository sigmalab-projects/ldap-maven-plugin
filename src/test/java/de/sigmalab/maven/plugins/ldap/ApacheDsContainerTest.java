package de.sigmalab.maven.plugins.ldap;

import java.io.File;

import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author  jbellmann
 */
@Ignore
public class ApacheDsContainerTest {

    @Test
    public void startupContainer() throws Exception {
        String path = getClass().getResource("/testLDAP.ldif").getFile();
        File diffFile = new File(path);
        String root = "dc=sigmalab,dc=de";
        ApacheDsContainer container = new ApacheDsContainer(root, diffFile);
        container.afterPropertiesSet();
        container.start();

        TimeUnit.SECONDS.sleep(10);
        container.stop();
    }

}
