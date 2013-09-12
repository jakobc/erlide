package org.erlide.ui.util;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.erlide.engine.model.erlang.ISourceRange;
import org.erlide.engine.model.erlang.ISourceReference;
import org.erlide.engine.model.root.IErlElement;
import org.erlide.ui.ErlideImage;
import org.erlide.ui.editors.erl.outline.ErlangElementImageDescriptor;
import org.erlide.ui.internal.ErlideUIPlugin;

public class ProblemsLabelDecorator implements ILabelDecorator,
        ILightweightLabelDecorator {

    /**
     * This is a special <code>LabelProviderChangedEvent</code> carrying
     * additional information whether the event origins from a maker change.
     * <p>
     * <code>ProblemsLabelChangedEvent</code>s are only generated by <code>
     * ProblemsLabelDecorator</code> s.
     * </p>
     */
    public static class ProblemsLabelChangedEvent extends
            LabelProviderChangedEvent {

        /**
		 * 
		 */
        private static final long serialVersionUID = 1L;
        private final boolean fMarkerChange;

        /**
         * Note: This constructor is for internal use only. Clients should not
         * call this constructor.
         */
        public ProblemsLabelChangedEvent(final IBaseLabelProvider source,
                final IResource[] changedResource, final boolean isMarkerChange) {
            super(source, changedResource);
            fMarkerChange = isMarkerChange;
        }

        /**
         * Returns whether this event origins from marker changes. If
         * <code>false</code> an annotation model change is the origin. In this
         * case viewers not displaying working copies can ignore these events.
         * 
         * @return if this event origins from a marker change.
         */
        public boolean isMarkerChange() {
            return fMarkerChange;
        }

    }

    private static final int ERRORTICK_WARNING = ErlangElementImageDescriptor.WARNING;
    private static final int ERRORTICK_ERROR = ErlangElementImageDescriptor.ERROR;
    private ListenerList fListeners;
    private IProblemChangedListener fProblemChangedListener;

    /*
     * Creates decorator with a shared image registry.
     * 
     * @param registry The registry to use or <code>null</code> to use the
     * erlide plugin's image registry.
     */

    /**
     * Note: This method is for internal use only. Clients should not call this
     * method.
     * 
     * @param obj
     *            the element to compute the flags for
     * 
     * @return the adornment flags
     */
    protected int computeAdornmentFlags(final Object obj) {
        try {
            final ISourceReference r = obj instanceof ISourceReference ? (ISourceReference) obj
                    : null;
            if (obj instanceof IResource) {
                return getErrorTicksFromMarkers((IResource) obj,
                        IResource.DEPTH_INFINITE, r);
            } else if (obj instanceof IErlElement) {
                final IErlElement e = (IErlElement) obj;
                return getErrorTicksFromMarkers(e.getResource(),
                        IResource.DEPTH_INFINITE, r);
            }
        } catch (final CoreException e) {
            if (e.getStatus().getCode() == IResourceStatus.MARKER_NOT_FOUND) {
                return 0;
            }
        }
        return 0;
    }

    public static int getErrorTicksFromMarkers(final IResource res,
            final int depth, final ISourceReference sourceElement)
            throws CoreException {
        if (res == null || !res.isAccessible()) {
            return 0;
        }
        int severity = 0;
        if (sourceElement == null) {
            severity = res.findMaxProblemSeverity(IMarker.PROBLEM, true, depth);
        } else {
            final IMarker[] markers = res.findMarkers(IMarker.PROBLEM, true,
                    depth);
            if (markers != null && markers.length > 0) {
                for (int i = 0; i < markers.length
                        && severity != IMarker.SEVERITY_ERROR; i++) {
                    final IMarker curr = markers[i];
                    if (isMarkerInRange(curr, sourceElement)) {
                        final int val = curr.getAttribute(IMarker.SEVERITY, -1);
                        if (val == IMarker.SEVERITY_WARNING
                                || val == IMarker.SEVERITY_ERROR) {
                            severity = val;
                        }
                    }
                }
            }
        }
        if (severity == IMarker.SEVERITY_ERROR) {
            return ERRORTICK_ERROR;
        } else if (severity == IMarker.SEVERITY_WARNING) {
            return ERRORTICK_WARNING;
        }
        return 0;
    }

    private static boolean isMarkerInRange(final IMarker marker,
            final ISourceReference sourceElement) throws CoreException {
        final int pos = marker.getAttribute(IMarker.CHAR_START, -1);
        if (pos != -1) {
            return isInside(pos, sourceElement);
        }
        final int line = marker.getAttribute(IMarker.LINE_NUMBER, -1);
        if (line != -1) {
            return isInsideLines(line - 1, sourceElement);
        }
        return false;
    }

    // private boolean isInside(Position pos, ISourceReference sourceElement)
    // throws CoreException {
    // return pos != null && isInside(pos.getOffset(), sourceElement);
    // }

    private static boolean isInsideLines(final int line,
            final ISourceReference sourceElement) {
        return line >= sourceElement.getLineStart()
                && line <= sourceElement.getLineEnd();
    }

    /**
     * Tests if a position is inside the source range of an element.
     * 
     * @param pos
     *            Position to be tested.
     * @param sourceElement
     *            Source element (must be a IErlElement)
     * @return boolean Return <code>true</code> if position is located inside
     *         the source element.
     * @throws CoreException
     *             Exception thrown if element range could not be accessed.
     * 
     */
    protected static boolean isInside(final int pos,
            final ISourceReference sourceElement) throws CoreException {
        // if (fCachedRange == null) {
        // fCachedRange= sourceElement.getSourceRange();
        // }
        // ISourceRange range= fCachedRange;
        final ISourceRange range = sourceElement.getSourceRange();
        if (range != null) {
            final int rangeOffset = range.getOffset();
            return rangeOffset <= pos && rangeOffset + range.getLength() > pos;
        }
        return false;
    }

    @Override
    public void decorate(final Object element, final IDecoration decoration) {
        final int adornmentFlags = computeAdornmentFlags(element);
        if (adornmentFlags == ERRORTICK_ERROR) {
            decoration.addOverlay(ErlideImage.OVR_ERROR.getDescriptor());
        } else if (adornmentFlags == ERRORTICK_WARNING) {
            decoration.addOverlay(ErlideImage.OVR_WARNING.getDescriptor());
        }

    }

    @Override
    public void addListener(final ILabelProviderListener listener) {
        if (fListeners == null) {
            fListeners = new ListenerList();
        }
        fListeners.add(listener);
        if (fProblemChangedListener == null) {
            fProblemChangedListener = new IProblemChangedListener() {
                @Override
                public void problemsChanged(final IResource[] changedResources,
                        final boolean isMarkerChange) {
                    fireProblemsChanged(changedResources, isMarkerChange);
                }
            };
            ErlideUIPlugin.getDefault().getProblemMarkerManager()
                    .addListener(fProblemChangedListener);
        }
    }

    void fireProblemsChanged(final IResource[] changedResources,
            final boolean isMarkerChange) {
        if (fListeners != null && !fListeners.isEmpty()) {
            final LabelProviderChangedEvent event = new ProblemsLabelChangedEvent(
                    this, changedResources, isMarkerChange);
            final Object[] listeners = fListeners.getListeners();
            for (int i = 0; i < listeners.length; i++) {
                ((ILabelProviderListener) listeners[i])
                        .labelProviderChanged(event);
            }
        }
    }

    @Override
    public void dispose() {
        if (fProblemChangedListener != null) {
            ErlideUIPlugin.getDefault().getProblemMarkerManager()
                    .removeListener(fProblemChangedListener);
            fProblemChangedListener = null;
        }
        // if (fRegistry != null && fUseNewRegistry) {
        // fRegistry.dispose();
        // }
    }

    @Override
    public boolean isLabelProperty(final Object element, final String property) {
        return true;
    }

    @Override
    public void removeListener(final ILabelProviderListener listener) {
        if (fListeners != null) {
            fListeners.remove(listener);
            if (fListeners.isEmpty() && fProblemChangedListener != null) {
                ErlideUIPlugin.getDefault().getProblemMarkerManager()
                        .removeListener(fProblemChangedListener);
                fProblemChangedListener = null;
            }
        }
    }

    @Override
    public String decorateText(final String text, final Object element) {
        return text;
    }

    @Override
    public Image decorateImage(final Image image, final Object obj) {
        final int adornmentFlags = computeAdornmentFlags(obj);
        if (adornmentFlags != 0) {
            final ImageDescriptor baseImage = new ImageImageDescriptor(image);
            final Rectangle bounds = image.getBounds();
            return ErlideUIPlugin.getImageDescriptorRegistry().get(
                    new ErlangElementImageDescriptor(baseImage, adornmentFlags,
                            new Point(bounds.width, bounds.height)));
        }
        return image;
    }

}
