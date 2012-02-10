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
package org.erlide.backend.internal;

import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.erlide.backend.BackendData;
import org.erlide.backend.BackendException;
import org.erlide.backend.IErlRuntime;
import org.erlide.launch.debug.model.ErtsProcess;

public class ExternalBackend extends Backend {

    public ExternalBackend(final BackendData data, final IErlRuntime runtime)
            throws BackendException {
        super(data, runtime);
        Assert.isNotNull(getLaunch());
        assignStreamProxyListeners();
    }

    @Override
    public void dispose() {
        try {
            getLaunch().terminate();
        } catch (final DebugException e) {
            e.printStackTrace();
        }

        super.dispose();
    }

    @Override
    public IStreamsProxy getStreamsProxy() {
        {
            final ErtsProcess p = getErtsProcess();
            if (p == null) {
                return null;
            }
            return p.getStreamsProxy();
        }
    }

    private ErtsProcess getErtsProcess() {
        final IProcess[] ps = getLaunch().getProcesses();
        if (ps == null || ps.length == 0) {
            return null;
        }
        return (ErtsProcess) ps[0];
    }

}
