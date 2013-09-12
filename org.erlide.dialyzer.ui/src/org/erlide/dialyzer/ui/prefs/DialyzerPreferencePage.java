/*******************************************************************************
 * Copyright (c) 2008 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.dialyzer.ui.prefs;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.erlide.backend.BackendCore;
import org.erlide.core.ErlangCore;
import org.erlide.dialyzer.builder.DialyzerPreferences;
import org.erlide.dialyzer.builder.DialyzerUtils;
import org.erlide.dialyzer.builder.DialyzerUtils.DialyzerErrorException;
import org.erlide.dialyzer.builder.ErlideDialyze;
import org.erlide.engine.ErlangEngine;
import org.erlide.engine.model.ErlModelException;
import org.erlide.engine.model.root.ErlElementKind;
import org.erlide.engine.model.root.IErlElement;
import org.erlide.engine.model.root.IErlProject;
import org.erlide.runtime.api.IRpcSite;
import org.erlide.runtime.rpc.RpcException;
import org.erlide.ui.prefs.ProjectSpecificPreferencePage;
import org.erlide.util.ErlLogger;
import org.osgi.service.prefs.BackingStoreException;

import com.ericsson.otp.erlang.OtpErlangObject;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class DialyzerPreferencePage extends ProjectSpecificPreferencePage {

	public class ContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput,
				final Object newInput) {
		}

		@Override
		public Object[] getElements(final Object inputElement) {
			return shownPLTFiles.toArray();
		}

	}

	private static class LabelProvider implements ILabelProvider {

		@Override
		public void addListener(final ILabelProviderListener listener) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean isLabelProperty(final Object element,
				final String property) {
			return true;
		}

		@Override
		public void removeListener(final ILabelProviderListener listener) {
		}

		@Override
		public Image getImage(final Object element) {
			return null;
		}

		@Override
		public String getText(final Object element) {
			if (element instanceof String) {
				final String s = (String) element;
				return s;
			}
			return null;
		}

	}

	private static final int MAX_PLT_FILES = 256;

	DialyzerPreferences prefs;
	private IProject fProject;
	private Button fUseProjectSettings;
	private Link fChangeWorkspaceSettings;
	protected ControlEnableState fBlockEnableState;
	// private final Text pltEdit = null;
	private Combo fromCombo;
	private Button dialyzeCheckbox;
	private Composite prefsComposite;
	private CheckboxTableViewer fPLTTableViewer;
	private Button fAddButton;
	private Button fEditButton;
	private Button fRemoveButton;
	private Button fUpdatePLTButton;
	private Button noCheckPLTCheckbox;
	private Button removeWarningsOnCleanCheckbox;
	private final List<String> shownPLTFiles;

	public DialyzerPreferencePage() {
		super();
		setTitle("Dialyzer options");
		setDescription("Select the options for dialyzer.");
		shownPLTFiles = Lists.newArrayList();
	}

	@Override
	protected Control createContents(final Composite parent) {
		loadPrefs();
		prefsComposite = new Composite(parent, SWT.NONE);
		prefsComposite.setLayout(new GridLayout());

		// final Group group = new Group(prefsComposite, SWT.NONE);
		final Composite group = prefsComposite;// new Composite(prefsComposite,
												// SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		// group.setLayout(new GridLayout(1, false));
		createDialyzeCheckbox(group);
		createPltSelection(group);
		createPltCheck(group);
		createFromSelection(group);
		createPltNoCheckbox(group);
		createRemoveWarningsOnCleanCheckbox(group);
		enableButtons();

		if (isProjectPreferencePage()) {
			final boolean useProjectSettings = hasProjectSpecificOptions(fProject);
			enableProjectSpecificSettings(useProjectSettings);
		}

		performDefaults();

		return prefsComposite;
	}

	private void createRemoveWarningsOnCleanCheckbox(final Composite group) {
		final Composite comp = new Composite(group, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));
		removeWarningsOnCleanCheckbox = new Button(comp, SWT.CHECK);
		removeWarningsOnCleanCheckbox
				.setText("Remove dialyzer warning on clean project");
		new Label(comp, SWT.NONE);
	}

	private void createPltNoCheckbox(final Composite group) {
		final Composite comp = new Composite(group, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));
		noCheckPLTCheckbox = new Button(comp, SWT.CHECK);
		noCheckPLTCheckbox.setText("Do not check PLT on dialyzer run");
		new Label(comp, SWT.NONE);
	}

	private void createPltCheck(final Composite group) {
		final Composite comp = new Composite(group, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));
		fUpdatePLTButton = new Button(comp, SWT.PUSH);
		fUpdatePLTButton.setText("Update PLT");
		fUpdatePLTButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				checkSelectedPltFiles();
			}
		});
		final Label l = new Label(comp, SWT.NONE);
		l.setText("Warning: this can take some time");
	}

	private void createDialyzeCheckbox(final Composite group) {
		final Composite comp = new Composite(group, SWT.NONE);
		// comp.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true,
		// false));
		comp.setLayout(new GridLayout(1, false));
		dialyzeCheckbox = new Button(comp, SWT.CHECK);
		dialyzeCheckbox.setEnabled(false);
		dialyzeCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
			}
		});
		dialyzeCheckbox.setText("Run dialyzer when compiling");
		dialyzeCheckbox.setSelection(prefs.getDialyzeOnCompile());
	}

	private void createFromSelection(final Composite group) {
		final Composite comp = new Composite(group, SWT.NONE);
		// comp.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true,
		// false));
		comp.setLayout(new GridLayout(2, false));
		final Label l = new Label(comp, SWT.NONE);
		l.setText("Analyze from ");
		fromCombo = new Combo(comp, SWT.READ_ONLY);
		fromCombo.setItems(new String[] { "Source", "Binaries" });
		fromCombo.setText(fromCombo.getItem(prefs.getFromSource() ? 0 : 1));
	}

	private void createPltSelection(final Composite group) {
		final Composite composite = new Composite(group, SWT.NONE);
		composite.setLayout(new GridLayout(3, false));
		GridData gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		composite.setLayoutData(gd);
		final Label l = new Label(composite, SWT.NONE);
		l.setText("PLT files (multiple PLT requires Erlang/OTP R14B01 or later)");
		gd = new GridData();
		gd.horizontalSpan = 2;
		l.setLayoutData(gd);
		new Label(composite, SWT.NONE);
		fPLTTableViewer = CheckboxTableViewer.newCheckList(composite,
				SWT.BORDER);
		fPLTTableViewer.setLabelProvider(new LabelProvider());
		fPLTTableViewer.setContentProvider(new ContentProvider());
		fPLTTableViewer.setInput(this);
		// fPLTList = new org.eclipse.swt.widgets.List(composite, SWT.MULTI
		// | SWT.V_SCROLL | SWT.BORDER);
		final Table table = fPLTTableViewer.getTable();
		table.

		addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				enableButtons();
			}
		});
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING
				| GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		gd.horizontalSpan = 2;
		table.setLayoutData(gd);
		gd.heightHint = convertHeightInCharsToPixels(12);
		final Composite buttons = new Composite(composite, SWT.NULL);
		buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		final GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttons.setLayout(layout);
		fAddButton = createButton(buttons, "Add...", new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				addPLTFile();
			}
		});
		fEditButton = createButton(buttons, "Change...",
				new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						changeSelectedPLTFiles();
					}
				});
		fRemoveButton = createButton(buttons, "Remove", new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				removeSelectedPLTFiles();
			}
		});

		if (isProjectPreferencePage()) {
			fAddButton.setVisible(false);
			fEditButton.setVisible(false);
			fRemoveButton.setVisible(false);
		}
	}

	private Button createButton(final Composite buttons, final String text,
			final SelectionListener selectionListener) {
		GridData gd;
		final Button button = new Button(buttons, SWT.PUSH);
		button.setText(text);
		gd = new GridData();
		gd.horizontalAlignment = GridData.HORIZONTAL_ALIGN_FILL;
		button.setLayoutData(gd);
		button.addSelectionListener(selectionListener);
		return button;
	}

	@Override
	protected boolean hasProjectSpecificOptions(final IProject project) {
		try {
			final DialyzerPreferences p = DialyzerPreferences.get(project);
			return p.hasOptionsAtLowestScope();
		} catch (final RpcException e) {
		}
		return false;
	}

	@Override
	protected Label createDescriptionLabel(final Composite parent) {
		createProjectSpecificSettingsCheckBoxAndLink(parent);
		return super.createDescriptionLabel(parent);
	}

	@Override
	protected void enableProjectSpecificSettings(
			final boolean useProjectSpecificSettings) {
		fUseProjectSettings.setSelection(useProjectSpecificSettings);
		enablePreferenceContent(useProjectSpecificSettings);
		fChangeWorkspaceSettings.setEnabled(!useProjectSpecificSettings);
		// doStatusChanged();
	}

	private void enablePreferenceContent(
			final boolean useProjectSpecificSettings) {
		if (useProjectSpecificSettings) {
			if (fBlockEnableState != null) {
				fBlockEnableState.restore();
				fBlockEnableState = null;
			}
		} else {
			if (fBlockEnableState == null) {
				fBlockEnableState = ControlEnableState.disable(prefsComposite);
			}
		}
	}

	protected void enableButtons() {
		final IStructuredSelection selection = (IStructuredSelection) fPLTTableViewer
				.getSelection();
		final int selectionCount = selection.size();
		fEditButton.setEnabled(selectionCount == 1);
		fRemoveButton.setEnabled(selectionCount > 0);
		fUpdatePLTButton.setEnabled(selectionCount > 0);
		fAddButton.setEnabled(shownPLTFiles.size() < MAX_PLT_FILES);
	}

	@Override
	protected String getPreferencePageID() {
		return "org.erlide.ui.preferences.dialyzer";
	}

	@Override
	protected String getPropertyPageID() {
		return "org.erlide.ui.properties.dialyzerPreferencePage";
	}

	boolean optionsAreOk() {
		for (final String s : shownPLTFiles) {
			final File f = new File(s);
			if (!f.exists()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean performOk() {
		try {
			if (fUseProjectSettings != null
					&& !fUseProjectSettings.getSelection()
					&& isProjectPreferencePage()) {
				prefs.removeAllProjectSpecificSettings();
			} else {
				if (fPLTTableViewer != null) {
					prefs.setPltPaths(shownPLTFiles);
					prefs.setEnabledPltPaths(getCheckedPltFiles());
				}
				prefs.setFromSource(fromCombo.getSelectionIndex() == 0);
				prefs.setDialyzeOnCompile(dialyzeCheckbox.getSelection());
				prefs.setNoCheckPLT(noCheckPLTCheckbox.getSelection());
				prefs.setRemoveWarningsOnClean(removeWarningsOnCleanCheckbox
						.getSelection());
				prefs.store();
			}
		} catch (final BackingStoreException e) {
			ErlLogger.warn(e);
		}
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		loadPrefs();
		shownPLTFiles.clear();
		shownPLTFiles.addAll(prefs.getPltPaths());
		if (fPLTTableViewer != null) {
			fPLTTableViewer.refresh();
			fPLTTableViewer.setAllChecked(false);
			for (final String s : prefs.getEnabledPltPaths()) {
				fPLTTableViewer.setChecked(s, true);
			}
		}
		if (fromCombo != null) {
			fromCombo.setText(fromCombo.getItem(prefs.getFromSource() ? 0 : 1));
		}
		if (dialyzeCheckbox != null) {
			// dialyzeCheckbox.setSelection(prefs.getDialyzeOnCompile());
			dialyzeCheckbox.setSelection(false);
		}
		if (noCheckPLTCheckbox != null) {
			noCheckPLTCheckbox.setSelection(prefs.getNoCheckPLT());
		}
		if (removeWarningsOnCleanCheckbox != null) {
			removeWarningsOnCleanCheckbox.setSelection(prefs
					.getRemoveWarningsOnClean());
		}
		super.performDefaults();
	}

	private void loadPrefs() {
		try {
			prefs = DialyzerPreferences.get(fProject);
		} catch (final Exception e) {
			// FIXME apply to status line or setErrorMessage
			ErlLogger.error(e);
		}
	}

	@Override
	public void setElement(final IAdaptable element) {
		fProject = (IProject) element.getAdapter(IResource.class);
		super.setElement(element);
	}

	private String selectPLTDialog(final String s) {
		final FileDialog dialog = new FileDialog(getShell(), SWT.SINGLE);
		dialog.setText("Select PLT file");
		dialog.setFileName(s);
		dialog.setFilterPath(s);
		dialog.setFilterNames(new String[] { "Dialyzer PLT file (*.plt)",
				"Any File" });
		dialog.setFilterExtensions(new String[] { "*.plt", "*.*" });
		final String result = dialog.open();
		return result;
	}

	private void changeSelectedPLTFiles() {
		final IStructuredSelection selection = (IStructuredSelection) fPLTTableViewer
				.getSelection();
		if (selection.size() != 1) {
			return;
		}
		final Object selectedElement = selection.getFirstElement();
		final int i = shownPLTFiles.indexOf(selectedElement);
		if (i == -1) {
			return;
		}
		final String result = selectPLTDialog((String) selectedElement);
		if (result == null) {
			return;
		}
		shownPLTFiles.set(i, result);
		fPLTTableViewer.refresh();
	}

	protected void removeSelectedPLTFiles() {
		final IStructuredSelection selection = (IStructuredSelection) fPLTTableViewer
				.getSelection();
		for (final Object o : selection.toList()) {
			shownPLTFiles.remove(o);
		}
		fPLTTableViewer.refresh();
	}

	protected void addPLTFile() {
		final String result = selectPLTDialog(null);
		if (result == null) {
			return;
		}
		shownPLTFiles.add(result);
		fPLTTableViewer.refresh();
	}

	protected void checkSelectedPltFiles() {
		final Job job = new UpdateDialyzerPLTFileOperation("Checking PLT file",
				getSelectedPltFiles(), getCheckedPltFiles());
		final ISchedulingRule rule = fProject;
		job.setRule(rule);
		job.setUser(true);
		job.setSystem(false);
		job.schedule();
	}

	private List<String> getCheckedPltFiles() {
		final List<String> l = Lists.newArrayList();
		for (final Object o : fPLTTableViewer.getCheckedElements()) {
			l.add((String) o);
		}
		return l;
	}

	private List<String> getSelectedPltFiles() {
		final IStructuredSelection selection = (IStructuredSelection) fPLTTableViewer
				.getSelection();
		final List<String> result = Lists.newArrayListWithCapacity(selection
				.size());
		for (final Object o : selection.toList()) {
			final String s = (String) o;
			result.add(s);
		}
		return result;
	}

	private final class UpdateDialyzerPLTFileOperation extends Job {

		private final List<String> selectedPLTPaths, checkedPltPaths;

		public UpdateDialyzerPLTFileOperation(final String name,
				final List<String> selectedPLTPaths,
				final List<String> checkedPltPaths) {
			super(name);
			this.selectedPLTPaths = selectedPLTPaths;
			this.checkedPltPaths = checkedPltPaths;
		}

		IStatus newErrorStatus(final Throwable throwable) {
			return new Status(IStatus.ERROR, ErlangCore.PLUGIN_ID,
					throwable.getMessage());
		}

		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			try {
				final String alternatePltFileDirectory = DialyzerPreferences
						.getAlternatePLTFileDirectoryFromPreferences();
				checkIfPltFilesShouldBeCopied(alternatePltFileDirectory);
				final IRpcSite backend = BackendCore
						.getBuildOrIdeBackend(fProject);
				for (final String pltPath : selectedPLTPaths) {
					checkPlt(pltPath, alternatePltFileDirectory, monitor,
							backend);
				}
			} catch (final Exception e) {
				return newErrorStatus(e);
			} finally {
				monitor.done();
			}
			return Status.OK_STATUS;
		}

		private void checkIfPltFilesShouldBeCopied(
				final String alternatePltFileDirectory) throws IOException {
			if (alternatePltFileDirectory == null) {
				return;
			}
			final List<String> selected = Lists.newArrayList(selectedPLTPaths);
			boolean changed = false;
			for (final String pltPath : selected) {
				final File f = new File(pltPath);
				if (!f.canWrite()) {
					final String newPath = copyPltFile(pltPath,
							alternatePltFileDirectory);
					selectedPLTPaths.remove(pltPath);
					selectedPLTPaths.remove(newPath);
					shownPLTFiles.remove(newPath);
					shownPLTFiles.add(newPath);
					selectedPLTPaths.add(newPath);
					checkedPltPaths.remove(newPath);
					if (checkedPltPaths.remove(pltPath)) {
						checkedPltPaths.add(newPath);
					}
					changed = true;
				}
			}
			if (changed) {
				getControl().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (!fPLTTableViewer.getControl().isDisposed()) {
							fPLTTableViewer.refresh();
							fPLTTableViewer
									.setSelection(new StructuredSelection(
											selectedPLTPaths));
							fPLTTableViewer.setCheckedElements(checkedPltPaths
									.toArray());
						}
					}
				});
			}
		}

		private String copyPltFile(final String pltPath,
				final String alternatePltFileDirectory) throws IOException {
			IPath path = new Path(pltPath);
			final String name = path.lastSegment();
			path = new Path(alternatePltFileDirectory).append(name);
			Files.copy(new File(pltPath), new File(path.toOSString()));
			return path.toPortableString();
		}

		private void checkPlt(final String pltPath,
				final String alternatePltFileDirectory,
				final IProgressMonitor monitor, final IRpcSite backend)
				throws DialyzerErrorException, ErlModelException, RpcException {
			try {
				monitor.subTask("Checking PLT file " + pltPath);
				List<String> ebinDirs = null;
				if (alternatePltFileDirectory != null) {
					ebinDirs = Lists.newArrayList();
					for (final IErlElement i : ErlangEngine.getInstance()
							.getModel()
							.getChildrenOfKind(ErlElementKind.PROJECT)) {
						final IErlProject project = (IErlProject) i;
						final String ebinDir = project.getWorkspaceProject()
								.getFolder(project.getOutputLocation())
								.getLocation().toString();
						ebinDirs.add(ebinDir);
					}
				}
				final OtpErlangObject result = ErlideDialyze.checkPlt(backend,
						pltPath, ebinDirs);
				DialyzerUtils.checkDialyzeError(result);
			} finally {
				monitor.worked(1);
			}
		}
	}

	public ISchedulingRule createRule(final Set<IProject> projects) {
		ISchedulingRule combinedRule = null;
		for (final IProject project : projects) {
			combinedRule = MultiRule.combine(project, combinedRule);
		}
		return combinedRule;
	}

}
