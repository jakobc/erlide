/*
 * ResourceUtil.java
 * Created on 2004-08-20
 *
 * cvs-id : $Id$
 */
package org.erlide.core.erlang.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

/**
 * <p>
 * contains static helping functionality to work on file resources in the
 * workspace.
 * </p>
 * 
 * @author Leif Frenzel
 * @author Andrei Formiga
 */
public class ResourceUtil {

    /**
     * <p>
     * returns whether the passed resource is an Erlang source file, as
     * recognized by the file extensions '.erl' and '.hrl'.
     * </p>
     */
    public static boolean hasErlangExtension(final IResource resource) {
        final String ext = resource.getFileExtension();
        return ErlideUtil.isModuleExtension(ext);
    }

    /**
     * <p>
     * reads an input stream and returns the contents as String.
     * </p>
     */
    public static String readStream(final InputStream is) throws IOException {
        final StringBuilder sbResult = new StringBuilder();
        final BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = br.readLine();
        while (line != null) {
            sbResult.append(line);
            sbResult.append('\n');
            line = br.readLine();
        }
        br.close();
        is.close();

        return sbResult.toString();
    }

    /**
     * finds the corresponding resource for the specified element. This is
     * element itself, if it is an IResource, or an adapter. Returns null, if no
     * resource could be found.
     */
    public static IResource findResource(final Object element) {
        IResource result = null;
        if (element instanceof IResource) {
            result = (IResource) element;
        } else if (element instanceof IAdaptable) {
            final Object adapter = ((IAdaptable) element)
                    .getAdapter(IResource.class);
            if (adapter instanceof IResource) {
                result = (IResource) adapter;
            }
        }
        return result;
    }

    public static IResource recursiveFindNamedResource(
            final IContainer container, final String name,
            final ContainerFilter filter) throws CoreException {
        if (!container.isAccessible()) {
            return null;
        }
        IResource r = container.findMember(name);
        if (r != null && (filter == null || filter.accept(container))) {
            return r;
        }
        final IResource[] members = container.members();
        for (final IResource element : members) {
            r = element;
            if (r instanceof IContainer) {
                r = recursiveFindNamedResource((IContainer) r, name, filter);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    // FIXME can't we use erlang model instead?
    public static IResource recursiveFindNamedResourceWithReferences(
            final IContainer container, final String name,
            final ContainerFilterCreator filterCreator) throws CoreException {
        final IProject project = container.getProject();
        final ContainerFilter filter = filterCreator
                .createFilterForProject(project);
        final IResource r = recursiveFindNamedResource(container, name, filter);
        if (r != null) {
            return r;
        }
        for (final IProject p : project.getReferencedProjects()) {
            final ContainerFilter pFilter = filterCreator
                    .createFilterForProject(p);
            final IResource r1 = recursiveFindNamedResource(p, name, pFilter);
            if (r1 != null) {
                return r1;
            }
        }
        return null;
    }

    public static IFile getFileFromLocation(final String location) {
        final IWorkspaceRoot wr = ResourcesPlugin.getWorkspace().getRoot();
        final IFile[] f = wr.findFilesForLocationURI(URIUtil.toURI(location));
        if (f.length > 0) {
            return f[0];
        }
        return null;
    }
}
