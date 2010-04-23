package org.erlide.ui.editors.erl.actions;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.texteditor.ITextEditor;
import org.erlide.ui.ErlideUIPlugin;
import org.erlide.ui.actions.SelectionDispatchAction;
import org.erlide.ui.console.ErlangConsolePage;

public class SendToConsoleAction extends SelectionDispatchAction {

	private final ITextEditor fEditor;

	@Override
	public void run(ITextSelection selection) {
		ErlangConsolePage p = ErlideUIPlugin.getDefault().getConsolePage();
		// make sure we have a console page to send it to
		if (p != null) {
			// if selection is empty, grab the whole line
			if (selection.getLength() == 0) { // don't use isEmpty()!
				selection = ErlangTextEditorAction.extendSelectionToWholeLines(
						fEditor.getDocumentProvider().getDocument(
								fEditor.getEditorInput()), selection);
			}
			// try to make the text a full erlang expression, ending with dot
			String text = selection.getText().trim();
			if (text.endsWith(",") || text.endsWith(";")) { //$NON-NLS-1$ //$NON-NLS-2$
				text = text.substring(0, text.length() - 1);
			}
			if (!text.endsWith(".")) { //$NON-NLS-1$
				text += "."; //$NON-NLS-1$
			}
			text += "\n"; //$NON-NLS-1$
			// send it off to the console
			p.input(text);
		}
		super.run(selection);
	}

	public SendToConsoleAction(final IWorkbenchSite site,
			final ResourceBundle bundle, final String prefix,
			final ITextEditor editor) {
		super(site);
		setText(getString(bundle, prefix + "label")); //$NON-NLS-1$
		setToolTipText(getString(bundle, prefix + "tooltip")); //$NON-NLS-1$
		setDescription(getString(bundle, prefix + "description")); //$NON-NLS-1$
		fEditor = editor;
	}

	protected static String getString(final ResourceBundle bundle,
			final String key) {
		try {
			return bundle.getString(key);
		} catch (MissingResourceException x) {
		}
		return key;
	}

}
