package de.sigmalab.maven.plugins.ldap;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.exception.ExceptionInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;
import org.apache.directory.server.core.operational.OperationalAttributeInterceptor;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.referral.ReferralInterceptor;
import org.apache.directory.server.core.subtree.SubentryInterceptor;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.store.LdifFileLoader;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.name.LdapDN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shameless copy from Spring-Security.
 *
 * @author  jbellmann
 */
public class ApacheDsContainer {

    private static final Logger logger = LoggerFactory.getLogger(ApacheDsContainer.class);

    final DefaultDirectoryService service;

    LdapServer server;

    // TODO, make this configurable
    private File diffFile;

    private File workingDir;

    private boolean running;
// private final String ldifResources;
    private final JdbmPartition partition;
    private final String root;

    // TODO, make this configurable
    private int port = 53389;

    public ApacheDsContainer(final String root, final File diffFile) throws Exception {
// this.ldifResources = ldifs;
        this.diffFile = diffFile;
        service = new DefaultDirectoryService();

        List<Interceptor> list = new ArrayList<Interceptor>();

        list.add(new NormalizationInterceptor());
        list.add(new AuthenticationInterceptor());
        list.add(new ReferralInterceptor());

// list.add( new AciAuthorizationInterceptor() );
// list.add( new DefaultAuthorizationInterceptor() );
        list.add(new ExceptionInterceptor());
// list.add( new ChangeLogInterceptor() );
        list.add(new OperationalAttributeInterceptor());
// list.add( new SchemaInterceptor() );
        list.add(new SubentryInterceptor());
// list.add( new CollectiveAttributeInterceptor() );
// list.add( new EventInterceptor() );
// list.add( new TriggerInterceptor() );
// list.add( new JournalInterceptor() );

        service.setInterceptors(list);
        partition = new JdbmPartition();
        partition.setId("rootPartition");
        partition.setSuffix(root);
        this.root = root;
        service.addPartition(partition);
        service.setExitVmOnShutdown(false);
        service.setShutdownHookEnabled(false);
        service.getChangeLog().setEnabled(false);
        service.setDenormalizeOpAttrsEnabled(true);
    }

    public void afterPropertiesSet() throws Exception {
        if (workingDir == null) {
            String apacheWorkDir = System.getProperty("apacheDSWorkDir");

            if (apacheWorkDir == null) {
                apacheWorkDir = createTempDirectory("apacheds-spring-security-");
            }

            setWorkingDirectory(new File(apacheWorkDir));
        }

        server = new LdapServer();
        server.setDirectoryService(service);
        server.setTransports(new TcpTransport(port));
        start();
    }

    public void destroy() throws Exception {
        stop();
    }

    public void setWorkingDirectory(final File workingDir) {
        // Assert.notNull(workingDir);

        logger.info("Setting working directory for LDAP_PROVIDER: " + workingDir.getAbsolutePath());

        if (workingDir.exists()) {
            throw new IllegalArgumentException("The specified working directory '" + workingDir.getAbsolutePath()
                    + "' already exists. Another directory service instance may be using it or it may be from a "
                    + " previous unclean shutdown. Please confirm and delete it or configure a different "
                    + "working directory");
        }

        this.workingDir = workingDir;

        service.setWorkingDirectory(workingDir);
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public DefaultDirectoryService getService() {
        return service;
    }

    public void start() {
        if (isRunning()) {
            return;
        }

        if (service.isStarted()) {
            throw new IllegalStateException("DirectoryService is already running.");
        }

        logger.info("Starting directory server...");
        try {
            service.startup();
            server.start();
        } catch (Exception e) {
            throw new RuntimeException("Server startup failed", e);
        }

        try {
            service.getAdminSession().lookup(partition.getSuffixDn());
        } catch (LdapNameNotFoundException e) {
            try {
                LdapDN dn = new LdapDN(root);
                // Assert.isTrue(root.startsWith("dc="));

                String dc = root.substring(3, root.indexOf(','));
                ServerEntry entry = service.newEntry(dn);
                entry.add("objectClass", "top", "domain", "extensibleObject");
                entry.add("dc", dc);
                service.getAdminSession().add(entry);
            } catch (Exception e1) {
                logger.error("Failed to create dc entry", e1);
            }
        } catch (Exception e) {
            logger.error("Lookup failed", e);
        }

        running = true;

        try {
            importLdifs();
        } catch (Exception e) {
            logger.error("Failed to import LDIF file(s)", e);
        }
    }

    public void stop() {
        if (!isRunning()) {
            return;
        }

        logger.info("Shutting down directory server ...");
        try {
            server.stop();
            service.shutdown();
        } catch (Exception e) {
            logger.error("Shutdown failed", e);
            return;
        }

        running = false;

        if (workingDir.exists()) {
            logger.info("Deleting working directory " + workingDir.getAbsolutePath());
            deleteDir(workingDir);
        }
    }

    private void importLdifs() throws Exception {

        // Import any ldif files
// Resource[] ldifs;

// if (ctxt == null) {

        // Not running within an app context
// ldifs = new PathMatchingResourcePatternResolver().getResources(ldifResources);
//

        // Note that we can't just import using the ServerContext returned
        // from starting Apache DS, apparently because of the long-running issue DIRSERVER-169.
        // We need a standard context.
        // DirContext dirContext = contextSource.getReadWriteContext();

// if (ldifs == null || ldifs.length == 0) {
// return;
// }
//
// if (ldifs.length == 1) {
// String ldifFile;
//
// try {
//// ldifFile = ldifs[0].getFile().getAbsolutePath();
// } catch (IOException e) {
// // ldifFile = ldifs[0].getURI().toString();
// }

        logger.info("Loading LDIF file: " + this.diffFile.getAbsolutePath());

// LdifFileLoader loader = new LdifFileLoader(service.getAdminSession(), ldifFile);
        LdifFileLoader loader = new LdifFileLoader(service.getAdminSession(), this.diffFile, null);
        loader.execute();
// } else {
// throw new IllegalArgumentException("More than one LDIF resource found with the supplied pattern:"
// + ldifResources);
// }
    }

    private String createTempDirectory(final String prefix) throws IOException {
        String parentTempDir = System.getProperty("java.io.tmpdir");
        String fileNamePrefix = prefix + System.nanoTime();
        String fileName = fileNamePrefix;

        for (int i = 0; i < 1000; i++) {
            File tempDir = new File(parentTempDir, fileName);
            if (!tempDir.exists()) {
                return tempDir.getAbsolutePath();
            }

            fileName = fileNamePrefix + "~" + i;
        }

        throw new IOException("Failed to create a temporary directory for file at "
                + new File(parentTempDir, fileNamePrefix));
    }

    private boolean deleteDir(final File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }

        return dir.delete();
    }

    public boolean isRunning() {
        return running;
    }

}
