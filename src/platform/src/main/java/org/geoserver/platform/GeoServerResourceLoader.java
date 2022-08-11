/*
 *==Description==
 *GeoServer is an open source software server written in Java that allows users
 *          to share and edit geospatial data.Designed for interoperability,
 *          it publishes data from any major spatial data source using open standards.
 *
 *Being a community-driven project, GeoServer is developed, tested, and supported by
 *      a diverse group of individuals and organizations from around the world.
 *
 *GeoServer is the reference implementation of the Open Geospatial Consortium (OGC)
 *          Web Feature Service (WFS) and Web Coverage Service (WCS) standards, as well as
 *          a high performance certified compliant Web Map Service (WMS), compliant
 *          Catalog Service for the Web (CSW) and implementing Web Processing Service (WPS).
 *          GeoServer forms a core component of the Geospatial Web.
 *
 *==License==
 *GeoServer is distributed under the GNU General Public License Version 2.0 license:
 *
 *    GeoServer, open geospatial information server
 *    Copyright (C) 2014-2020 Open Source Geospatial Foundation.
 *    Copyright (C) 2001-2014 OpenPlans
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version (collectively, "GPL").
 *
 *    As an exception to the terms of the GPL, you may copy, modify,
 *    propagate, and distribute a work formed by combining GeoServer with the
 *    EMF and XSD Libraries, or a work derivative of such a combination, even if
 *    such copying, modification, propagation, or distribution would otherwise
 *    violate the terms of the GPL. Nothing in this exception exempts you from
 *    complying with the GPL in all respects for all of the code used other
 *    than the EMF and XSD Libraries. You may include this exception and its grant
 *    of permissions when you distribute GeoServer.  Inclusion of this notice
 *    with such a distribution constitutes a grant of such permissions.  If
 *    you do not wish to grant these permissions, remove this paragraph from
 *    your distribution. "GeoServer" means the GeoServer software licensed
 *    under version 2 or any later version of the GPL, or a work based on such
 *    software and licensed under the GPL. "EMF and XSD Libraries" means
 *    Eclipse Modeling Framework Project and XML Schema Definition software
 *    distributed by the Eclipse Foundation, all licensed
 *    under the Eclipse Public License Version 1.0 ("EPL"), or a work based on
 *    such software and licensed under the EPL.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 51 Franklin Street, Suite 500, Boston, MA 02110-1335  USA
 *
 *==More Information==
 *Visit the website or read the docs.
 */

package org.geoserver.platform;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.web.context.ServletContextAware;

/**
 * Access to resources in GeoServer including configuration information and unmanaged cache or log
 * files.
 *
 * <p>The loader maintains a search path in which it will use to look up resources.
 *
 * <ul>
 *   <li>Configuration is accessed using {@link ResourceStore#get(String)} which provides stream
 *       based access. If required configuration can be unpacked into a file in the data directory.
 *       The most common example is for use as a template.
 *   <li>Files in the data directory can also be used as a temporary cache. These files should be
 *       considered temporary and may need to be recreated (when upgrading or for use on different
 *       nodes in a cluster).
 *   <li>
 * </ul>
 *
 * <p>The {@link #baseDirectory} is a member of this path. Files and directories created by the
 * resource loader are made relative to {@link #baseDirectory}.
 *
 * <pre>
 * <code>
 * File dataDirectory = ...
 * GeoServerResourceLoader loader = new GeoServerResourceLoader( dataDirectory );
 * ...
 * Resource catalog = loader.get("catalog.xml");
 * File log = loader.find("logs/geoserver.log");
 * </code>
 * </pre>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class GeoServerResourceLoader extends DefaultResourceLoader
        implements ResourceStore, ServletContextAware {
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.platform");

    /**
     * ResourceStore used for configuration resources.
     *
     * <p>Initially this is configured to access resources in the base directory, however spring may
     * inject an external implementation (jdbc database blob, github, ...).
     */
    ResourceStore resources;

    /** Base directory used to access unmanaged files. */
    File baseDirectory;

    /**
     * Creates a new resource loader (with no base directory).
     *
     * <p>Used to construct a GeoServerResourceLoader for test cases (and is unable to create
     * resources from relative paths.
     */
    public GeoServerResourceLoader() {
        baseDirectory = null;
        resources = ResourceStore.EMPTY;
    }

    /**
     * Creates a new resource loader.
     *
     * @param baseDirectory The directory in which
     */
    public GeoServerResourceLoader(File baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.resources = new FileSystemResourceStore(baseDirectory);
    }

    /**
     * Creates a new resource loader.
     *
     * @param resourceStore resource store for artifact storage
     */
    public GeoServerResourceLoader(ResourceStore resourceStore) {
        this.resources = resourceStore;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        if (baseDirectory == null) {
            String data = lookupGeoServerDataDirectory(servletContext);
            if (data != null) {
                this.baseDirectory = new File(data);
            } else {
                throw new IllegalStateException("Unable to determine data directory");
            }
        }
        if (resources == ResourceStore.EMPTY && baseDirectory != null) {
            // lookup the configuration resources
            resources = new FileSystemResourceStore(baseDirectory);
        }
    }

    /** @return The base directory. */
    public File getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Sets the base directory.
     *
     * @param baseDirectory base of data directory used for file configuration files
     */
    public void setBaseDirectory(File baseDirectory) {
        this.baseDirectory = baseDirectory;
        if (resources == ResourceStore.EMPTY) {
            resources = new FileSystemResourceStore(baseDirectory);
        }
    }

    @Override
    public Resource get(String path) {
        return resources.get(path);
    }

    @Override
    public boolean move(String path, String target) {
        return resources.move(path, target);
    }

    @Override
    public boolean remove(String path) {
        return resources.remove(path);
    }

    /**
     * Used to look up resources based on user provided url (or path) using the Data Directory as
     * base directory.
     *
     * <p>Convenience method for Resources.fromURL(resources.get(Paths.BASE), url)
     *
     * <p>See {@link Resources#fromURL(Resource, String)}
     */
    public Resource fromURL(String url) {
        return Resources.fromURL(resources.get(Paths.BASE), url);
    }

    /**
     * Used to look up resources based on user provided url using the Data Directory as base
     * directory.
     *
     * <p>Convenience method for Resources.fromURL(resources.get(Paths.BASE), url)
     *
     * <p>See {@link Resources#fromURL(Resource, URL)}
     */
    public Resource fromURL(URL url) {
        return Resources.fromURL(resources.get(Paths.BASE), url);
    }

    /**
     * Used to look up resources based on user provided path using the Data Directory as base
     * directory.
     *
     * <p>Convenience method for Resources.fromPath(resources.get(Paths.BASE), path)
     *
     * <p>See {@link Resources#fromPath(String, Resource)}
     */
    public Resource fromPath(String path) {
        return Resources.fromPath(path, resources.get(Paths.BASE));
    }

    /**
     * Performs file lookup.
     *
     * @param location The name of the resource to lookup, can be absolute or relative.
     * @return The file handle representing the resource, or null if the resource could not be
     *     found.
     * @throws IOException In the event of an I/O error.
     */
    public File find(String location) throws IOException {
        Resource resource = get(Paths.convert(location));
        return Resources.find(resource);
    }

    /**
     * Performs a resource lookup, optionally specifying the containing directory.
     *
     * @param parentFile The containing directory, optionally null.
     * @param location The name of the resource to lookup, can be absolute or relative.
     * @return The file handle representing the resource, or null if the resource could not be
     *     found.
     * @throws IOException In the event of an I/O error.
     */
    public File find(File parentFile, String location) throws IOException {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(
                    "Looking up resource "
                            + location
                            + " with parent "
                            + (parentFile != null ? parentFile.getPath() : "null"));
        }
        Resource resource = get(Paths.convert(getBaseDirectory(), parentFile, location));
        return Resources.find(resource);
    }

    /**
     * Performs a resource lookup.
     *
     * <pre>
     * Example:
     *   File f = resourceLoader.find( "data", "shapefiles", "foo.shp" );
     * </pre>
     *
     * @param location The components of the path of the resource to lookup.
     * @return The file handle representing the resource, or null if the resource could not be
     *     found.
     * @throws IOException Any I/O errors that occur.
     */
    public File find(String... location) throws IOException {
        Resource resource = get(Paths.path(location));
        return Resources.find(resource);
    }

    /**
     * Performs a resource lookup, optionally specifying a containing directory.
     *
     * <pre>
     * Example:
     *   File f = resourceLoader.find( "data", "shapefiles", "foo.shp" );
     * </pre>
     *
     * @param parentFile The parent directory, may be null.
     * @param location The components of the path of the resource to lookup.
     * @return The file handle representing the resource, or null if the resource could not be
     *     found.
     * @throws IOException Any I/O errors that occur.
     */
    public File find(File parentFile, String... location) throws IOException {
        Resource resource = get(Paths.convert(getBaseDirectory(), parentFile, location));
        return Resources.find(resource);
    }

    /** Helper method to build up a file path from components. */
    String concat(String... location) {
        StringBuffer loc = new StringBuffer();
        for (String s : location) {
            loc.append(s).append(File.separator);
        }
        loc.setLength(loc.length() - 1);
        return loc.toString();
    }

    /**
     * Performs a directory lookup, creating the file if it does not exist.
     *
     * @param location The components of the path that make up the location of the directory to find
     *     or create.
     */
    public File findOrCreateDirectory(String... location) throws IOException {
        Resource directory = get(Paths.path(location));
        return directory.dir(); // will create directory as needed
    }

    /**
     * Performs a directory lookup, creating the file if it does not exist.
     *
     * @param parentFile The containing directory, possibly null.
     * @param location The components of the path that make up the location of the directory to find
     *     or create.
     */
    public File findOrCreateDirectory(File parentFile, String... location) throws IOException {
        Resource directory = get(Paths.convert(getBaseDirectory(), parentFile, location));
        return directory.dir(); // will create directory as needed
    }

    /**
     * Performs a directory lookup, creating the file if it does not exist.
     *
     * @param location The location of the directory to find or create.
     * @return The file handle.
     * @throws IOException If any i/o errors occur.
     */
    public File findOrCreateDirectory(String location) throws IOException {
        Resource directory = get(Paths.convert(location));
        return directory.dir(); // will create directory as needed
    }

    /**
     * Performs a directory lookup, creating the file if it does not exist.
     *
     * @param parentFile The containing directory, may be null.
     * @param location The location of the directory to find or create.
     * @return The file handle.
     * @throws IOException If any i/o errors occur.
     */
    public File findOrCreateDirectory(File parentFile, String location) throws IOException {
        Resource directory = get(Paths.convert(getBaseDirectory(), parentFile, location));
        return directory.dir(); // will create directory as needed
    }

    /**
     * Creates a new directory specifying components of the location.
     *
     * <p>Calls through to {@link #createDirectory(String)}
     */
    public File createDirectory(String... location) throws IOException {
        Resource directory = get(Paths.path(location));
        return Resources.createNewDirectory(directory);
    }

    /**
     * Creates a new directory specifying components of the location, and the containing directory.
     *
     * <p>Calls through to {@link #createDirectory(String)}
     *
     * @param parentFile The containing directory, possibly null.
     * @param location The components of the path that make up the location of the directory to
     *     create
     * @return newly created directory
     */
    public File createDirectory(File parentFile, String... location) throws IOException {
        Resource directory = get(Paths.convert(getBaseDirectory(), parentFile, location));
        return Resources.createNewDirectory(directory);
    }

    /**
     * Creates a new directory.
     *
     * <p>Relative paths are created relative to {@link #baseDirectory}. If {@link #baseDirectory}
     * is not set, an IOException is thrown.
     *
     * <p>If <code>location</code> already exists as a file, an IOException is thrown.
     *
     * @param location Location of directory to create, either absolute or relative.
     * @return The file handle of the created directory.
     */
    public File createDirectory(String location) throws IOException {
        Resource directory = get(Paths.convert(location));
        return Resources.createNewDirectory(directory);
    }

    /**
     * Creates a new directory, optionally specifying a containing directory.
     *
     * <p>Relative paths are created relative to {@link #baseDirectory}. If {@link #baseDirectory}
     * is not set, an IOException is thrown.
     *
     * <p>If <code>location</code> already exists as a file, an IOException is thrown.
     *
     * @param parentFile The containing directory, may be null.
     * @param location Location of directory to create, either absolute or relative.
     * @return The file handle of the created directory.
     */
    public File createDirectory(File parentFile, String location) throws IOException {
        Resource directory = get(Paths.convert(getBaseDirectory(), parentFile, location));
        return Resources.createNewDirectory(directory);
    }

    /**
     * Creates a new file.
     *
     * <p>Calls through to {@link #createFile(String)}.
     *
     * @param location The components of the location.
     * @return The file handle of the created file.
     * @throws IOException In the event of an I/O error.
     */
    public File createFile(String... location) throws IOException {
        Resource resource = get(Paths.path(location));
        return Resources.createNewFile(resource);
    }

    /**
     * Creates a new file.
     *
     * <p>Calls through to {@link #createFile(File, String)}
     *
     * @param location Location of file to create, either absolute or relative.
     * @return The file handle of the created file.
     * @throws IOException In the event of an I/O error.
     */
    public File createFile(String location) throws IOException {
        Resource resource = get(Paths.convert(location));
        return Resources.createNewFile(resource);
    }

    /**
     * Creates a new file.
     *
     * <p>Calls through to {@link #createFile(File, String)}
     *
     * @param location Location of file to create, either absolute or relative.
     * @param parentFile The containing directory for the file.
     * @return The file handle of the created file.
     * @throws IOException In the event of an I/O error.
     */
    public File createFile(File parentFile, String... location) throws IOException {
        Resource resource = get(Paths.convert(getBaseDirectory(), parentFile, location));
        return Resources.createNewFile(resource);
    }

    /**
     * Creates a new file.
     *
     * <p>Relative paths are created relative to {@link #baseDirectory}.
     *
     * <p>If {@link #baseDirectory} is not set, an IOException is thrown.
     *
     * <p>If <code>location</code> already exists as a directory, an IOException is thrown.
     *
     * @param location Location of file to create, either absolute or relative.
     * @param parentFile The containing directory for the file.
     * @return The file handle of the created file.
     * @throws IOException In the event of an I/O error.
     */
    public File createFile(File parentFile, String location) throws IOException {
        Resource resource = get(Paths.convert(getBaseDirectory(), parentFile, location));
        return Resources.createNewFile(resource);
    }

    /**
     * Copies a resource located on the classpath to a specified path.
     *
     * <p>The <tt>resource</tt> is obtained from teh context class loader of the current thread.
     * When the <tt>to</tt> parameter is specified as a relative path it is considered to be
     * relative to {@link #getBaseDirectory()}.
     *
     * @param classpathResource The resource to copy.
     * @param location The destination to copy to.
     */
    public void copyFromClassPath(String classpathResource, String location) throws IOException {
        Resource resource = get(Paths.convert(location));
        copyFromClassPath(classpathResource, resource.file());
    }

    /**
     * Copies a resource from the classpath to a specified file.
     *
     * @param classpathResource Path to classpath content to be copied
     * @param target File to copy content into (must be already created)
     */
    public void copyFromClassPath(String classpathResource, File target) throws IOException {
        copyFromClassPath(classpathResource, target, null);
    }

    /**
     * Copies a resource relative to a particular class from the classpath to the specified file.
     *
     * @param classpathResource Path to classpath content to be copied
     * @param target File to copy content into (must be already created)
     * @param scope Class used as base for classpathResource
     */
    public void copyFromClassPath(String classpathResource, File target, Class<?> scope)
            throws IOException {

        byte[] buffer = new byte[4096];
        int read;
        try (InputStream is = getStreamFromResource(classpathResource, scope);
                OutputStream os = new FileOutputStream(target)) {
            while ((read = is.read(buffer)) > 0) os.write(buffer, 0, read);
        } catch (FileNotFoundException targetException) {
            throw new IOException(
                    "Can't write to file "
                            + target.getAbsolutePath()
                            + ". Check write permissions on target folder for user "
                            + System.getProperty("user.name"));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error trying to copy logging configuration file", e);
        }
    }

    private InputStream getStreamFromResource(String classpathResource, Class<?> scope)
            throws IOException {
        InputStream is = null;
        if (scope == null) {
            is =
                    Thread.currentThread()
                            .getContextClassLoader()
                            .getResourceAsStream(classpathResource);
            if (is == null) {
                throw new IOException(
                        "Could not load "
                                + classpathResource
                                + " from scope "
                                + Thread.currentThread().getContextClassLoader().toString()
                                + ".");
            }
        } else {
            is = scope.getResourceAsStream(classpathResource);
            if (is == null) {
                throw new IOException(
                        "Could not load "
                                + classpathResource
                                + " from scope "
                                + scope.toString()
                                + ".");
            }
        }

        return is;
    }

    /**
     * Determines the location of the geoserver data directory based on the following lookup
     * mechanism:
     *
     * <p>1) Java environment variable 2) Servlet context variable 3) System variable
     *
     * <p>For each of these, the methods checks that 1) The path exists 2) Is a directory 3) Is
     * writable
     *
     * @param servContext The servlet context.
     * @return String The absolute path to the data directory, or <code>null</code> if it could not
     *     be found.
     */
    public static String lookupGeoServerDataDirectory(ServletContext servContext) {

        final String[] typeStrs = {
            "Java environment variable ",
            "Servlet context parameter ",
            "System environment variable "
        };

        String requireFileVar = "GEOSERVER_REQUIRE_FILE";
        requireFile(System.getProperty(requireFileVar), typeStrs[0] + requireFileVar);
        requireFile(servContext.getInitParameter(requireFileVar), typeStrs[1] + requireFileVar);
        requireFile(System.getenv(requireFileVar), typeStrs[2] + requireFileVar);

        final String[] varStrs = {"GEOSERVER_DATA_DIR", "GEOSERVER_DATA_ROOT"};

        String dataDirStr = null;
        String msgPrefix = null;

        // Loop over variable names
        for (int i = 0; i < varStrs.length && dataDirStr == null; i++) {

            // Loop over variable access methods
            for (int j = 0; j < typeStrs.length && dataDirStr == null; j++) {
                String value = null;
                String varStr = varStrs[i];
                String typeStr = typeStrs[j];

                // Lookup section
                switch (j) {
                    case 0:
                        value = System.getProperty(varStr);
                        break;
                    case 1:
                        value = servContext.getInitParameter(varStr);
                        break;
                    case 2:
                        value = System.getenv(varStr);
                        break;
                }

                if (value == null || value.equalsIgnoreCase("")) {
                    LOGGER.finer("Found " + typeStr + varStr + " to be unset");
                    continue;
                }

                // Verify section
                File fh = new File(value);

                // Being a bit pessimistic here
                msgPrefix = "Found " + typeStr + varStr + " set to " + value;

                if (!fh.exists()) {
                    LOGGER.warning(msgPrefix + " , but this path does not exist");
                    continue;
                }
                if (!fh.isDirectory()) {
                    LOGGER.warning(msgPrefix + " , which is not a directory");
                    continue;
                }
                if (!fh.canWrite()) {
                    LOGGER.warning(msgPrefix + " , which is not writeable");
                    continue;
                }

                // Sweet, we can work with this
                dataDirStr = value;
            }
        }

        // fall back to embedded data dir
        if (dataDirStr == null) {
            dataDirStr = servContext.getRealPath("/data");
            LOGGER.info("Falling back to embedded data directory: " + dataDirStr);
        }

        return dataDirStr;
    }

    /**
     * Check that required files exist and throw {@link IllegalArgumentException} if they do not.
     *
     * @param files either a single file name or a list of file names separated by {@link
     *     File#pathSeparator}
     * @param source description of source from which file name(s) obtained
     */
    static void requireFile(String files, String source) {
        if (files == null || files.isEmpty()) {
            return;
        } else {
            for (String file : files.split(File.pathSeparator)) {
                if (!(new File(file)).exists()) {
                    throw new IllegalArgumentException(
                            "Missing required file: " + file + " From: " + source + ": " + files);
                }
            }
        }
    }

    @Override
    public ResourceNotificationDispatcher getResourceNotificationDispatcher() {
        return resources.getResourceNotificationDispatcher();
    }

    public ResourceStore getResourceStore() {
        return resources;
    }
}
