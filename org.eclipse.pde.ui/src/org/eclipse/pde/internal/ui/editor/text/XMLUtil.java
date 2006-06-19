package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;

public abstract class XMLUtil {

	/**
	 * Scans up the node's parents till it reaches
	 * a IPluginExtension or IPluginExtensionPoint (or null)
	 * and returns the result.
	 * 
	 * @param node
	 * @return the IPluginExtension or IPluginExtensionPoint that contains <code>node</code>
	 */
	public static IPluginObject getTopLevelParent(IDocumentRange range) {
		IDocumentNode node = null;
		if (range instanceof IDocumentAttribute)
			node = ((IDocumentAttribute)range).getEnclosingElement();
		else if (range instanceof IDocumentTextNode)
			node = ((IDocumentTextNode)range).getEnclosingElement();
		else if (range instanceof IPluginElement)
			node = (IDocumentNode)range;
		else if (range instanceof IPluginObject)
			// not an attribute/text node/element -> return direct node
			return (IPluginObject)range; 
		
		while (node != null && 
				!(node instanceof IPluginExtension) && 
				!(node instanceof IPluginExtensionPoint))
			node = node.getParentNode();
		
		return node != null ? (IPluginObject)node : null;
	}
	
	private static boolean withinRange(int start, int len, int offset) {
		return start <= offset && offset <= start + len;
	}
	
	public static boolean withinRange(IDocumentRange range, int offset) {
		if (range instanceof IDocumentAttribute)
			return withinRange(
					((IDocumentAttribute)range).getValueOffset(),
					((IDocumentAttribute)range).getValueLength(),
					offset);
		if (range instanceof IDocumentNode)
			return withinRange(
					((IDocumentNode)range).getOffset(),
					((IDocumentNode)range).getLength(),
					offset);
		if (range instanceof IDocumentTextNode)
			return withinRange(
					((IDocumentTextNode)range).getOffset(),
					((IDocumentTextNode)range).getLength(),
					offset);
		return false;
	}
	
	/**
	 * Get the ISchemaElement corresponding to this IDocumentNode
	 * @param node
	 * @param extensionPoint the extension point of the schema, if <code>null</code> it will be deduced
	 * @return the ISchemaElement for <code>node</code>
	 */
	public static ISchemaElement getSchemaElement(IDocumentNode node, String extensionPoint) {
		if (extensionPoint == null) {
			IPluginObject obj = getTopLevelParent(node);
			if (!(obj instanceof IPluginExtension))
				return null;
			extensionPoint = ((IPluginExtension)obj).getPoint();
		}
		ISchema schema = PDECore.getDefault().getSchemaRegistry().getSchema(extensionPoint);
		if (schema == null)
			return null;
		
		ISchemaElement sElement = schema.findElement(node.getXMLTagName());
		return sElement;
	}
	
	/**
	 * Get the ISchemaAttribute corresponding to this IDocumentAttribute
	 * @param attr
	 * @param extensionPoint the extension point of the schema, if <code>null</code> it will be deduced
	 * @return the ISchemaAttribute for <code>attr</code>
	 */
	public static ISchemaAttribute getSchemaAttribute(IDocumentAttribute attr, String extensionPoint) {
		ISchemaElement ele = getSchemaElement(attr.getEnclosingElement(), extensionPoint);
		if (ele == null)
			return null;
		
		return ele.getAttribute(attr.getAttributeName());
	}
	
}
