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

import org.eclipse.jdt.annotation.NonNull;
import org.erlide.backend.api.BackendData;
import org.erlide.backend.api.IBackendManager;
import org.erlide.runtime.api.IErlRuntime;

public class InternalBackend extends Backend {

    public InternalBackend(final BackendData data,
            final @NonNull IErlRuntime runtime,
            final IBackendManager backendManager) {
        super(data, runtime, backendManager);
    }

    @Override
    public void onShutdown() {
        getData().setLaunch(null);
    }
}
