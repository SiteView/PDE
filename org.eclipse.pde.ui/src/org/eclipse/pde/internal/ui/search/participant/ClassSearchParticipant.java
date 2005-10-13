package org.eclipse.pde.internal.ui.search.participant;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.ui.search.IMatchPresentation;
import org.eclipse.jdt.ui.search.IQueryParticipant;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.jdt.ui.search.PatternQuerySpecification;
import org.eclipse.jdt.ui.search.QuerySpecification;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.eclipse.pde.internal.ui.model.AbstractEditingModel;
import org.eclipse.pde.internal.ui.model.bundle.Bundle;
import org.eclipse.pde.internal.ui.model.bundle.BundleModel;
import org.eclipse.pde.internal.ui.model.bundle.ManifestHeader;
import org.eclipse.pde.internal.ui.model.plugin.FragmentModel;
import org.eclipse.pde.internal.ui.model.plugin.PluginAttribute;
import org.eclipse.pde.internal.ui.model.plugin.PluginModel;
import org.eclipse.pde.internal.ui.model.plugin.PluginModelBase;
import org.eclipse.search.ui.text.Match;
import org.eclipse.text.edits.MalformedTreeException;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class ClassSearchParticipant implements IQueryParticipant {

	private static final int S_PLUGIN = 0;
	private static final int S_FRAGMENT = 1;
	private static final int S_MANIFEST = 2;
	private static final int S_TOTAL = 3;
	private static final String[] SEARCH_FILES = new String[S_TOTAL];
	static {
		SEARCH_FILES[S_PLUGIN] = "plugin.xml"; //$NON-NLS-1$
		SEARCH_FILES[S_FRAGMENT] = "fragment.xml"; //$NON-NLS-1$
		SEARCH_FILES[S_MANIFEST] = "META-INF/MANIFEST.MF"; //$NON-NLS-1$
	}
	private static final int H_IMP = 0;
	private static final int H_EXP = 1;
	private static final int H_BUNACT = 2;
	private static final int H_PLUGCLASS = 3;
	private static final int H_TOTAL = 4;
	private static final String[] SEARCH_HEADERS = new String[H_TOTAL];
	static {
		SEARCH_HEADERS[H_IMP] = Constants.IMPORT_PACKAGE;
		SEARCH_HEADERS[H_EXP] = Constants.EXPORT_PACKAGE;
		SEARCH_HEADERS[H_BUNACT] = Constants.BUNDLE_ACTIVATOR;
		SEARCH_HEADERS[H_PLUGCLASS] = ICoreConstants.PLUGIN_CLASS;
	}
	private static final int S_FOR_TYPES = 0;
	private static final int S_FOR_PACKAGES = 2;
	
	private SearchMatchPresentation fMatchPresentation;
	private ISearchRequestor fSearchRequestor;
	private Pattern fSearchPattern;
	private int fSearchFor;
	
	public ClassSearchParticipant() {
		fMatchPresentation = new SearchMatchPresentation();
	}
	
	public void search(ISearchRequestor requestor,
			QuerySpecification querySpecification, IProgressMonitor monitor)
			throws CoreException {
		
		if (querySpecification.getLimitTo() != 2 && querySpecification.getLimitTo() != 3) 
			return;
		
		String search;
		if (querySpecification instanceof ElementQuerySpecification) {
			search = ((ElementQuerySpecification)querySpecification).getElement().getElementName();
			if (((ElementQuerySpecification)querySpecification).getElement().getElementType() == IJavaElement.TYPE)
				fSearchFor = S_FOR_TYPES;
		} else {
			fSearchFor = ((PatternQuerySpecification)querySpecification).getSearchFor();
			if (fSearchFor != S_FOR_TYPES && fSearchFor != S_FOR_PACKAGES)
				return;
			search = ((PatternQuerySpecification)querySpecification).getPattern();
		}
		fSearchPattern = PatternConstructor.createPattern(search, true);
		fSearchRequestor = requestor;
		
		IPluginModelBase[] pluginModels = PDECore.getDefault().getModelManager().getWorkspaceModels();
		monitor.beginTask("Searching for classes and packages in manifest files", pluginModels.length);
		for (int i = 0; i < pluginModels.length; i++) {
			IProject project = pluginModels[i].getUnderlyingResource().getProject();
			if (!monitor.isCanceled()) {
				searchProject(project, monitor, pluginModels[i].getPluginBase());
			}
		}
	}

	private void searchProject(IProject project, IProgressMonitor monitor, IPluginBase base) throws CoreException {
		for (int i = 0; i < S_TOTAL; i++) {
			IFile file = project.getFile(SEARCH_FILES[i]);
			if (!file.exists()) continue;
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			try {
				manager.connect(file.getFullPath(), monitor);
				ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath());
				IDocument document = buffer.getDocument();
				AbstractEditingModel loadModel = null;
				switch(i) {
				case S_PLUGIN:
					loadModel = new PluginModel(document, false);
					break;
				case S_FRAGMENT:
					loadModel = new FragmentModel(document, false);
					break;
				case S_MANIFEST:
					loadModel = new BundleModel(document, false);
				}	
				if (loadModel == null) continue;
				loadModel.load();
				if (!loadModel.isLoaded()) continue;
				
				if ((i == S_FRAGMENT || i == S_PLUGIN) && loadModel instanceof IPluginModelBase) {
					loadModel.setUnderlyingResource(file);
					PluginModelBase modelBase = (PluginModelBase)loadModel;
					SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
					IPluginExtension[] extensions = modelBase.getPluginBase().getExtensions();
					for (int j = 0; j < extensions.length; j++) {
						ISchema schema = registry.getSchema(extensions[j].getPoint());
						if (schema != null)
							inspectExtension(schema, extensions[j]);
					}
				} else if (i == S_MANIFEST && loadModel instanceof IBundleModel) {
					loadModel.setUnderlyingResource(file);
					Bundle bundle = (Bundle)((IBundleModel)loadModel).getBundle();
					if (bundle != null)
						inspectBundle(bundle, base);
				}
			} finally {
				manager.disconnect(file.getFullPath(), monitor);
			}
		}
	}

	private void inspectExtension(ISchema schema, IPluginParent parent) {
		IPluginObject[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			IPluginElement child = (IPluginElement)children[i];
			ISchemaElement schemaElement = schema.findElement(child.getName());
			if (schemaElement != null) {
				IPluginAttribute[] attributes = child.getAttributes();
				for (int j = 0; j < attributes.length; j++) {
					IPluginAttribute attr = attributes[j];
					ISchemaAttribute attInfo = schemaElement.getAttribute(attr.getName());
					if (attInfo != null 
							&& attInfo.getKind() == IMetaAttribute.JAVA
							&& attr instanceof PluginAttribute) {
						String value = attr.getValue();
						if (fSearchFor == S_FOR_TYPES) 
							value = extractType(value);
						Matcher matcher = fSearchPattern.matcher(value.subSequence(0, value.length()));
						if (matcher.matches()) {
							String group = matcher.group(0);
							int offset = ((PluginAttribute)attr).getValueOffset() + value.indexOf(group) + attr.getValue().indexOf(value);
							int length = group.length();
							fSearchRequestor.reportMatch(new Match(
									new SearchHit(parent, value),
									Match.UNIT_CHARACTER, offset, length));
						}
					}
				}
			}
			inspectExtension(schema, child);
		}
	}

	private void inspectBundle(Bundle bundle, IPluginBase base) {
		for (int i = 0; i < H_TOTAL; i++) {
			if (fSearchFor == S_FOR_TYPES && (i == H_IMP || i == H_EXP))
				continue;
			ManifestHeader header = bundle.getManifestHeader(SEARCH_HEADERS[i]);
			if (header != null) {
				try {
					ManifestElement[] elements = ManifestElement.parseHeader(header.getName(), header.getValue());
					if (elements == null) continue;
					int initOff = 0;
					for (int j = 0; j < elements.length; j++) {
						String value = elements[j].getValue();
						if (fSearchFor == S_FOR_TYPES) 
							value = extractType(value);
						Matcher matcher = fSearchPattern.matcher(value.subSequence(0, value.length()));
						if (matcher.matches()) {
							String group = matcher.group(0);
							int[] offlen;
							try {
								offlen = getOffsetOfElement(header, group, initOff);
								initOff = offlen[0] - header.getOffset();
							} catch (CoreException e) {
								offlen = new int[]{header.getOffset(), header.getLength()};
							}
							fSearchRequestor.reportMatch(new Match(
									new SearchHit(base, value),
									Match.UNIT_CHARACTER, offlen[0], offlen[1]));
						}
					}
				} catch (BundleException e) {
				}
			}
		}
	}
	
	private int[] getOffsetOfElement(ManifestHeader header, String value, int initOff) throws CoreException {
		int offset = 0;
		int length = 0;
		IResource res = header.getModel().getUnderlyingResource();
		if (res instanceof IFile) {
			IFile file = (IFile)res;
			IProgressMonitor monitor = new NullProgressMonitor();
			ITextFileBufferManager pManager = FileBuffers.getTextFileBufferManager();
			try {
				pManager.connect(file.getFullPath(), monitor);
				ITextFileBuffer pBuffer = pManager.getTextFileBuffer(file.getFullPath());
				IDocument pDoc = pBuffer.getDocument();
				int headerOffset = header.getOffset();
				String headerString = pDoc.get(headerOffset, header.getLength());
				int internalOffset = headerString.indexOf(value, initOff);
				if (internalOffset != -1) {
					offset = headerOffset + internalOffset;
				} else {
					offset = headerOffset + header.getName().length() + header.getValue().indexOf(value);
				}
				length = value.length();
			} catch (MalformedTreeException e) {
			} catch (BadLocationException e) {
			} finally {
				pManager.disconnect(file.getFullPath(), monitor);
			}
		}
		return new int[]{offset, length};
	}
	
	private String extractType(String value) {
		int index = value.lastIndexOf(".");
		if (index == -1 || index == value.length() - 1) return value;
		return value.substring(index + 1);
	}
	
	public int estimateTicks(QuerySpecification specification) {
		return 100;
	}

	public IMatchPresentation getUIParticipant() {
		return fMatchPresentation;
	}
}
