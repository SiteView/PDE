/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.plugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;

public class PluginDocumentNode implements IDocumentNode {
	
	private static final long serialVersionUID = 1L;
	
	private transient IDocumentNode fParent;
	private transient boolean fIsErrorNode;
	private transient int fLength;
	private transient int fOffset;
	private transient IDocumentNode fPreviousSibling;
	private transient int fIndent;
	
	private ArrayList fChildren;
	private TreeMap fAttributes;
	private String fTag;
	private IDocumentTextNode fTextNode;

	// TODO: MP: TEO:  Rename to DocumentElementNode
	// TODO: MP: TEO:  Move to core.text package
	// TODO: MP: TEO:  Regenerate comments
	
	/**
	 * 
	 */
	public PluginDocumentNode() {
		fParent = null;
		fIsErrorNode = false;
		fLength = -1;
		fOffset = -1;
		fPreviousSibling = null;
		fIndent = 0;
		
		fChildren = new ArrayList();
		fAttributes = new TreeMap();
		fTag = null;
		fTextNode = null;		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentNode#getChildNodesList()
	 */
	public ArrayList getChildNodesList() {
		// Not used by text edit operations
		return fChildren;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentNode#getNodeAttributesMap()
	 */
	public TreeMap getNodeAttributesMap() {
		// Not used by text edit operations
		return fAttributes;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentNode#setXMLAttribute(java.lang.String, java.lang.String)
	 */
	public void setXMLAttribute(String name, String value) {
		// Not used by text edit operations
		// NO-OP
		// Use strongly discouraged, use setXMLAttribute(IDocumentAttribute) instead
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentNode#write(boolean)
	 */
	public String write(boolean indent) {
		// Used by text edit operations
		// Subclasses to override
		return ""; //$NON-NLS-1$
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentNode#writeShallow(boolean)
	 */
	public String writeShallow(boolean terminate) {
		// Used by text edit operations
		// Subclasses to override
		return ""; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentNode#getChildNodes()
	 */
	public IDocumentNode[] getChildNodes() {
		// Used by text edit operations
		return (IDocumentNode[]) fChildren.toArray(new IDocumentNode[fChildren.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#indexOf(org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	public int indexOf(IDocumentNode child) {
		// Not used by text edit operations
		return fChildren.indexOf(child);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getChildAt(int)
	 */
	public IDocumentNode getChildAt(int index) {
		// Used by text edit operations
		if (index < fChildren.size())
			return (IDocumentNode)fChildren.get(index);
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.model.IDocumentNode#getParentNode()
	 */
	public IDocumentNode getParentNode() {
		// Used by text edit operations
		return fParent;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.model.IDocumentNode#setParentNode(org.eclipse.pde.internal.ui.neweditor.model.IDocumentNode)
	 */
	public void setParentNode(IDocumentNode node) {
		// Used by text edit operations (indirectly)
		fParent = node;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.model.IDocumentNode#addChildNode(org.eclipse.pde.internal.ui.neweditor.model.IDocumentNode)
	 */
	public void addChildNode(IDocumentNode child) {
		// Used by text edit operations
		addChildNode(child, fChildren.size());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#addChildNode(org.eclipse.pde.internal.ui.model.IDocumentNode, int)
	 */
	public void addChildNode(IDocumentNode child, int position) {
		// Used by text edit operations
		fChildren.add(position, child);
		if (position > 0 && fChildren.size() > 1)
			child.setPreviousSibling((IDocumentNode)fChildren.get(position - 1));
		if (fChildren.size() > 1 && position < fChildren.size() - 1)
			((IDocumentNode)fChildren.get(position + 1)).setPreviousSibling(child);
		child.setParentNode(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#removeChildNode(org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	public IDocumentNode removeChildNode(IDocumentNode child) {
		// Used by text edit operations
		int index = fChildren.indexOf(child);
		if (index != -1) {
			fChildren.remove(child);
			if (index < fChildren.size()) {
				IDocumentNode prevSibling = index == 0 ? null : (IDocumentNode)fChildren.get(index - 1);
				((IDocumentNode)fChildren.get(index)).setPreviousSibling(prevSibling);
				return child;
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.model.IDocumentNode#isErrorNode()
	 */
	public boolean isErrorNode() {
		// Used by text edit operations (indirectly)
		return fIsErrorNode;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.model.IDocumentNode#setIsErrorNode(boolean)
	 */
	public void setIsErrorNode(boolean isErrorNode) {
		// Used by text edit operations
		fIsErrorNode = isErrorNode;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#setOffset(int)
	 */
	public void setOffset(int offset) {
		// Used by text edit operations
		fOffset = offset;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#setLength(int)
	 */
	public void setLength(int length) {
		// Used by text edit operations
		fLength = length;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getOffset()
	 */
	public int getOffset() {
		// Used by text edit operations
		return fOffset;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getLength()
	 */
	public int getLength() {
		// Used by text edit operations
		return fLength;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#setAttribute(org.eclipse.pde.internal.ui.model.IDocumentAttribute)
	 */
	public void setXMLAttribute(IDocumentAttribute attribute) {
		// Used by text edit operations
		fAttributes.put(attribute.getAttributeName(), attribute);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getXMLAttributeValue(java.lang.String)
	 */
	public String getXMLAttributeValue(String name) {
		// Not used by text edit operations
		PluginAttribute attr = (PluginAttribute)fAttributes.get(name);
		return attr == null ? null : attr.getValue();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#setXMLTagName(java.lang.String)
	 */
	public void setXMLTagName(String tag) {
		// Used by text edit operations (indirectly)
		fTag = tag;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getXMLTagName()
	 */
	public String getXMLTagName() {
		// Used by text edit operations
		return fTag;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getDocumentAttribute(java.lang.String)
	 */
	public IDocumentAttribute getDocumentAttribute(String name) {
		// Used by text edit operations
		return (IDocumentAttribute)fAttributes.get(name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getLineIndent()
	 */
	public int getLineIndent() {
		// Used by text edit operations
		return fIndent;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#setLineIndent(int)
	 */
	public void setLineIndent(int indent) {
		// Used by text edit operations
		fIndent = indent;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getAttributes()
	 */
	public IDocumentAttribute[] getNodeAttributes() {
		// Used by text edit operations
		ArrayList list = new ArrayList();
		Iterator iter = fAttributes.values().iterator();
		while (iter.hasNext())
			list.add(iter.next());
		return (IDocumentAttribute[])list.toArray(new IDocumentAttribute[list.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getPreviousSibling()
	 */
	public IDocumentNode getPreviousSibling() {
		// Used by text edit operations
		return fPreviousSibling;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#setPreviousSibling(org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	public void setPreviousSibling(IDocumentNode sibling) {
		// Used by text edit operations
		fPreviousSibling = sibling;
	}
	
	/**
	 * @return
	 */
	public String getIndent() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < fIndent; i++) {
			buffer.append(" "); //$NON-NLS-1$
		}
		return buffer.toString();
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#swap(org.eclipse.pde.internal.ui.model.IDocumentNode, org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	public void swap(IDocumentNode child1, IDocumentNode child2) {
		// Not used by text edit operations
		int index1 = fChildren.indexOf(child1);
		int index2 = fChildren.indexOf(child2);
		
		fChildren.set(index1, child2);
		fChildren.set(index2, child1);
		
		child1.setPreviousSibling(index2 == 0 ? null : (IDocumentNode)fChildren.get(index2 - 1));
		child2.setPreviousSibling(index1 == 0 ? null : (IDocumentNode)fChildren.get(index1 - 1));
		
		if (index1 < fChildren.size() - 1)
			((IDocumentNode)fChildren.get(index1 + 1)).setPreviousSibling(child2);
		
		if (index2 < fChildren.size() - 1)
			((IDocumentNode)fChildren.get(index2 + 1)).setPreviousSibling(child1);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#addTextNode(org.eclipse.pde.internal.ui.model.IDocumentTextNode)
	 */
	public void addTextNode(IDocumentTextNode textNode) {
		// Used by text edit operations
		fTextNode = textNode;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#getTextNode()
	 */
	public IDocumentTextNode getTextNode() {
		// Used by text edit operations
		return fTextNode;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#removeTextNode()
	 */
	public void removeTextNode() {
		// Used by text edit operations
		fTextNode = null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#removeDocumentAttribute(org.eclipse.pde.internal.ui.model.IDocumentAttribute)
	 */
	public void removeDocumentAttribute(IDocumentAttribute attr) {
		// Used by text edit operations
		fAttributes.remove(attr.getAttributeName());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentNode#reconnectRoot(org.eclipse.pde.core.plugin.ISharedPluginModel)
	 */
	public void reconnect(IDocumentNode parent, IModel model) {
		// Not used by text edit operations
		// Reconnect XML document characteristics
		reconnectDocument();
		// Reconnect parent
		// This may not be necessary.  When this node is added to the parent,
		// the parent takes care of this
		reconnectParent(parent);
		// Reconnect previous sibling
		// This may not be necessary.  When this node is added to the parent,
		// the parent takes care of this
		reconnectPreviousSibling(parent);
		// Reconnect text node
		reconnectText();
		// Reconnect attribute nodes
		reconnectAttributes();
		// Reconnect children nodes
		reconnectChildren(model);
	}
	
	/**
	 * @param model
	 * @param schema
	 */
	private void reconnectAttributes() {
		// Get all attributes
		Iterator keys = fAttributes.keySet().iterator();
		// Fill in appropriate transient field values for all attributes
		while (keys.hasNext()) {
			String key = (String)keys.next();
			IDocumentAttribute attribute = (IDocumentAttribute)fAttributes.get(key);
			attribute.reconnect(this);
		}
	}
	
	/**
	 * @param model
	 * @param schema
	 */
	private void reconnectChildren(IModel model) {
		// Fill in appropriate transient field values
		for (int i = 0; i < fChildren.size(); i++) {
			IDocumentNode child = (IDocumentNode)fChildren.get(i);
			// Reconnect child
			child.reconnect(this, model);
		}
	}
	
	/**
	 * 
	 */
	private void reconnectDocument() {
		// Transient field:  Indent
		fIndent = 0;
		// Transient field:  Error Node
		fIsErrorNode = false;
		// Transient field:  Length
		fLength = -1;
		// Transient field:  Offset
		fOffset = -1;
	}
	
	/**
	 * @param parent
	 */
	private void reconnectParent(IDocumentNode parent) {
		// Transient field:  Parent
		fParent = parent;		
	}
	
	/**
	 * @param parent
	 */
	private void reconnectPreviousSibling(IDocumentNode parent) {
		// Transient field:  Previous Sibling
		int childCount = parent.getChildCount();
		if (childCount < 1) {
			fPreviousSibling = null;
		} else {
			// The last item is the previous sibling; since, we have not added
			// overselves to the end of the parents children yet
			fPreviousSibling = (IDocumentNode)parent.getChildAt(childCount - 1);
		}				
	}
	
	/**
	 * 
	 */
	private void reconnectText() {
		// Transient field:  Text Node
		if (fTextNode != null) {
			fTextNode.reconnect(this);
		}		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentNode#getChildCount()
	 */
	public int getChildCount() {
		// Not used by text edit operations
		return fChildren.size();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentNode#isRoot()
	 */
	public boolean isRoot() {
		// Used by text edit operations
		return false;
	}
	
}