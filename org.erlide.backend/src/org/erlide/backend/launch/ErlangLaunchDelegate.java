/*******************************************************************************
 * Copyright (c) 2005 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Vlad Dumitrescu
 * Jakob Cederlund
 *******************************************************************************/
package org.erlide.backend.launch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.erlide.backend.BackendCore;
import org.erlide.backend.api.BackendData;
import org.erlide.backend.api.IBackend;
import org.erlide.engine.ErlangEngine;
import org.erlide.engine.model.IBeamLocator;
import org.erlide.runtime.ErlRuntimeFactory;
import org.erlide.runtime.api.ErlRuntimeAttributes;
import org.erlide.runtime.api.IErlRuntime;
import org.erlide.runtime.epmd.EpmdWatcher;
import org.erlide.runtime.runtimeinfo.RuntimeInfo;
import org.erlide.util.ErlLogger;
import org.erlide.util.HostnameUtils;

import com.google.common.collect.Maps;

public class ErlangLaunchDelegate extends LaunchConfigurationDelegate {

    protected IBackend backend;

    @Override
    public void launch(final ILaunchConfiguration config, final String mode,
            final ILaunch launch, final IProgressMonitor monitor)
            throws CoreException {
        assertThat(config, is(not(nullValue())));

        RuntimeInfo runtimeInfo = BackendCore.getRuntimeInfoCatalog()
                .getRuntime(
                        config.getAttribute(ErlRuntimeAttributes.RUNTIME_NAME,
                                ""));
        if (runtimeInfo == null) {
            runtimeInfo = BackendCore.getRuntimeInfoCatalog()
                    .getDefaultRuntime();
        }
        if (runtimeInfo == null) {
            // TODO what to do here?
            ErlLogger.error("Can't create backend without a runtime defined!");
            return;
        }
        final String nodeName = config.getAttribute(
                ErlRuntimeAttributes.NODE_NAME, "");
        BackendData data = new BackendData(runtimeInfo, config, mode,
                shouldManageNode(nodeName, BackendCore.getEpmdWatcher()));
        final RuntimeInfo info = data.getRuntimeInfo();
        if (info == null) {
            ErlLogger.error("Could not find runtime '%s'", data
                    .getRuntimeInfo().getName());
            return;
        }
        ErlLogger.debug("doLaunch runtime %s", data.getRuntimeInfo().getName());
        ErlLogger.debug("doLaunch cookie %s (%s)", data.getCookie(),
                data.getCookie());

        data = configureBackend(data, config, mode, launch);

        if (data.isManaged()) {
            setCaptureOutput(launch);
        }
        IErlRuntime runtime;
        if (!isErlangInternalLaunch(launch)) {
            backend = BackendCore.getBackendManager().createExecutionBackend(
                    data);
            runtime = backend.getRuntime();
        } else {
            runtime = ErlRuntimeFactory.createRuntime(data);
            runtime.startAndWait();
        }
        startErtsProcess(launch, data, runtime);
    }

    /*
     * Child classes override this to set specific information
     */
    protected BackendData configureBackend(final BackendData data,
            final ILaunchConfiguration config, final String mode,
            final ILaunch launch) {
        data.setLaunch(launch);
        data.setBeamLocator(ErlangEngine.getInstance().getService(
                IBeamLocator.class));
        return data;
    }

    private void startErtsProcess(final ILaunch launch, final BackendData data,
            final IErlRuntime runtime) {
        final Process process = runtime.getProcess();
        if (process == null) {
            ErlLogger.debug("Error starting process");
            data.setManaged(false);
            return;
        }
        data.setLaunch(launch);
        final Map<String, String> map = Maps.newHashMap();
        map.put("NodeName", data.getNodeName());
        map.put("workingDir", data.getWorkingDir());
        final IProcess erts = DebugPlugin.newProcess(launch, process,
                data.getNodeName(), map);

        ErlLogger.debug("Started erts: %s >> %s", erts.getLabel(),
                data.getNodeName());
    }

    private void setCaptureOutput(final ILaunch launch) {
        // important, we don't want the "normal" console for the erlide backend
        final String captureOutput = System.getProperty(
                "erlide.console.stdout", "false");
        launch.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, captureOutput);
    }

    public static boolean isErlangLaunch(final ILaunch aLaunch) {
        try {
            final ILaunchConfiguration cfg = aLaunch.getLaunchConfiguration();
            final ILaunchConfigurationType type = cfg.getType();
            final String id = type.getIdentifier();
            return IErlangLaunchDelegateConstants.CONFIGURATION_TYPE.equals(id)
                    || IErlangLaunchDelegateConstants.CONFIGURATION_TYPE_INTERNAL
                            .equals(id);
        } catch (final CoreException e) {
            ErlLogger.warn(e);
            return false;
        }
    }

    public static boolean isErlangInternalLaunch(final ILaunch aLaunch) {
        try {
            final ILaunchConfiguration cfg = aLaunch.getLaunchConfiguration();
            final ILaunchConfigurationType type = cfg.getType();
            final String id = type.getIdentifier();
            return IErlangLaunchDelegateConstants.CONFIGURATION_TYPE_INTERNAL
                    .equals(id);
        } catch (final CoreException e) {
            ErlLogger.warn(e);
            return false;
        }
    }

    public static boolean shouldManageNode(final String name,
            final EpmdWatcher epmdWatcher) {
        final int atSignIndex = name.indexOf('@');
        String shortName = name;
        if (atSignIndex > 0) {
            shortName = name.substring(0, atSignIndex);
        }

        boolean isLocal = atSignIndex < 0;
        if (atSignIndex > 0) {
            final String hostname = name.substring(atSignIndex + 1);
            if (HostnameUtils.isThisHost(hostname)) {
                isLocal = true;
            }
        }

        final boolean isRunning = epmdWatcher.hasLocalNode(shortName);
        final boolean result = isLocal && !isRunning;
        return result;
    }

}
