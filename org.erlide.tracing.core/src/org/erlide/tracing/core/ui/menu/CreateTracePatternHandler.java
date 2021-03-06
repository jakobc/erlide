package org.erlide.tracing.core.ui.menu;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ui.PlatformUI;
import org.erlide.core.erlang.internal.ErlFunction;
import org.erlide.tracing.core.TraceBackend;
import org.erlide.tracing.core.mvc.model.TracePattern;

public class CreateTracePatternHandler extends AbstractHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getSelection();

        if (selection instanceof ITreeSelection) {

            Object firstElement = ((ITreeSelection) selection).getFirstElement();

            if (firstElement instanceof ErlFunction) {
                ErlFunction function = (ErlFunction) firstElement;
                TracePattern tracePattern = new TracePattern(true);
                tracePattern.setFunctionName(function.getFunctionName());
                tracePattern.setModuleName(function.getModule().getModuleName());
                tracePattern.setArity(function.getArity());
                tracePattern.setLocal(true);
                tracePattern.setEnabled(true);
                TraceBackend.getInstance().addTracePattern(tracePattern);
            }
        }
        return null;
    }
}
