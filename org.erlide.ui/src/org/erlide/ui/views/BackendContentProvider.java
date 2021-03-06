/*******************************************************************************
 * Copyright (c) 2009 * and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available
 * at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     *
 *******************************************************************************/
package org.erlide.ui.views;

import java.util.Collection;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.runtime.backend.ErlideBackend;

public class BackendContentProvider implements IStructuredContentProvider {

    public void dispose() {
        // TODO unsubscribe from backend manager

    }

    public void inputChanged(final Viewer vwr, final Object oldInput,
            final Object newInput) {
        // TODO subscribe to backendmanager events
    }

    public Object[] getElements(final Object inputElement) {
        final Collection<ErlideBackend> bs = ErlangCore.getBackendManager()
                .getAllBackends();
        return bs.toArray(new ErlideBackend[bs.size()]);
    }
}