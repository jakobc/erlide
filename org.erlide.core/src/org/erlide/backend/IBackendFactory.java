package org.erlide.backend;

import org.erlide.runtime.runtimeinfo.RuntimeInfo;

public interface IBackendFactory {

    IBackend createIdeBackend();

    IBackend createBuildBackend(final RuntimeInfo info);

    IBackend createBackend(final BackendData data);

}
