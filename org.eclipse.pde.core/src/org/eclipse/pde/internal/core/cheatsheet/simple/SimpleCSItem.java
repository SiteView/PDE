/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.cheatsheet.simple;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConditionalSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSDescription;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSOnCompletion;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunContainerObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * SimpleCSItem
 *
 */
public class SimpleCSItem extends SimpleCSObject implements ISimpleCSItem {

	
	/**
	 * Element:  onCompletion
	 */
	private ISimpleCSOnCompletion fOnCompletion;
	
	/**
	 * Elements:  action, command, perform-when
	 */
	private ISimpleCSRunContainerObject fExecutable;
	
	/**
	 * Attribute:  skip
	 */
	private boolean fSkip;

	/**
	 * Attribute:  dialog
	 */
	private boolean fDialog;
	
	/**
	 * Element:  description
	 */
	private ISimpleCSDescription fDescription;
	
	/**
	 * Attribute:  title
	 */
	private String fTitle;

	/**
	 * Attribute:  contextId
	 */
	private String fContextId;

	/**
	 * Attribute:  href
	 */
	private String fHref;	
	
	/**
	 * Elements:  subitem
	 */
	private ArrayList fSubItems;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 * @param parent
	 */
	public SimpleCSItem(ISimpleCSModel model, ISimpleCSObject parent) {
		super(model, parent);
		reset();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getContextId()
	 */
	public String getContextId() {
		return fContextId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getDescription()
	 */
	public ISimpleCSDescription getDescription() {
		return fDescription;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getDialog()
	 */
	public boolean getDialog() {
		return fDialog;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getExecutable()
	 */
	public ISimpleCSRunContainerObject getExecutable() {
		return fExecutable;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getHref()
	 */
	public String getHref() {
		return fHref;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getSkip()
	 */
	public boolean getSkip() {
		return fSkip;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getSubItems()
	 */
	public ISimpleCSSubItemObject[] getSubItems() {
		return (ISimpleCSSubItemObject[]) fSubItems
				.toArray(new ISimpleCSSubItemObject[fSubItems.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getTitle()
	 */
	public String getTitle() {
		return fTitle;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setContextId(java.lang.String)
	 */
	public void setContextId(String contextId) {
		String old = fContextId;
		fContextId = contextId;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_CONTEXTID, old, fContextId);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setDescription(java.lang.String)
	 */
	public void setDescription(ISimpleCSDescription description) {
		ISimpleCSObject old = fDescription;		
		fDescription = description;

		if (isEditable()) {
			fireStructureChanged(description, old);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setDialog(boolean)
	 */
	public void setDialog(boolean dialog) {
		Boolean old = Boolean.valueOf(fDialog);
		fDialog = dialog;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_DIALOG, old, Boolean.valueOf(fDialog));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setExecutable(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunContainerObject)
	 */
	public void setExecutable(ISimpleCSRunContainerObject executable) {
		ISimpleCSObject old = fExecutable;		
		fExecutable = executable;

		if (isEditable()) {
			fireStructureChanged(executable, old);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setHref(java.lang.String)
	 */
	public void setHref(String href) {
		String old = fHref;
		fHref = href;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_HREF, old, fHref);
		}		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setSkip(boolean)
	 */
	public void setSkip(boolean skip) {
		Boolean old = Boolean.valueOf(fSkip);
		fSkip = skip;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_SKIP, old, Boolean.valueOf(fSkip));
		}			
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setTitle(java.lang.String)
	 */
	public void setTitle(String title) {
		String old = fTitle;
		fTitle = title;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_TITLE, old, fTitle);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#parse(org.w3c.dom.Element)
	 */
	public void parse(Element element) {
		// Process title attribute
		fTitle = element.getAttribute(ATTRIBUTE_TITLE).trim();
		// Process dialog attribute
		if (element.getAttribute(ATTRIBUTE_DIALOG).compareTo(
				ATTRIBUTE_VALUE_FALSE) == 0) {
			fDialog = false;
		}
		// Process skip attribute
		if (element.getAttribute(ATTRIBUTE_SKIP).compareTo(
				ATTRIBUTE_VALUE_TRUE) == 0) {
			fSkip = true;
		}
		// Process contextId attribute
		fContextId = element.getAttribute(ATTRIBUTE_CONTEXTID).trim();
		// Process href attribute
		fHref = element.getAttribute(ATTRIBUTE_HREF).trim();
		
		// Process children

		NodeList children = element.getChildNodes();
		ISimpleCSModelFactory factory = getModel().getFactory();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String name = child.getNodeName();
				Element childElement = (Element)child;

				if (name.equals(ELEMENT_DESCRIPTION)) {
					fDescription = factory.createSimpleCSDescription(this);
					fDescription.parse(childElement);
				} else if (name.equals(ELEMENT_ACTION)) {
					fExecutable = factory.createSimpleCSAction(this);
					fExecutable.parse(childElement);
				} else if (name.equals(ELEMENT_COMMAND)) {
					fExecutable = factory.createSimpleCSCommand(this);
					fExecutable.parse(childElement);
				} else if (name.equals(ELEMENT_PERFORM_WHEN)) {
					fExecutable = factory.createSimpleCSPerformWhen(this);
					fExecutable.parse(childElement);
				} else if (name.equals(ELEMENT_SUBITEM)) {
					ISimpleCSSubItem subitem = factory.createSimpleCSSubItem(this);
					fSubItems.add(subitem);
					subitem.parse(childElement);
				} else if (name.equals(ELEMENT_REPEATED_SUBITEM)) {
					ISimpleCSRepeatedSubItem subitem = factory.createSimpleCSRepeatedSubItem(this);
					fSubItems.add(subitem);
					subitem.parse(childElement);
				} else if (name.equals(ELEMENT_CONDITIONAL_SUBITEM)) {
					ISimpleCSConditionalSubItem subitem = factory.createSimpleCSConditionalSubItem(this);
					fSubItems.add(subitem);
					subitem.parse(childElement);
				} else if (name.equals(ELEMENT_ONCOMPLETION)) {
					fOnCompletion = factory.createSimpleCSOnCompletion(this);
					fOnCompletion.parse(childElement);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {

		StringBuffer buffer = new StringBuffer();
		String newIndent = indent + XMLPrintHandler.XML_INDENT;

		try {
			// Print item element
			buffer.append(ELEMENT_ITEM); //$NON-NLS-1$
			// Print title attribute
			if ((fTitle != null) && 
					(fTitle.length() > 0)) {
				buffer.append(XMLPrintHandler.wrapAttribute(
						ATTRIBUTE_TITLE, 
						PDETextHelper.translateWriteText(
								fTitle.trim(), SUBSTITUTE_CHARS)));
			}
			// Print dialog attribute
			buffer.append(XMLPrintHandler.wrapAttribute(
					ATTRIBUTE_DIALOG, new Boolean(fDialog).toString()));
			// Print skip attribute
			buffer.append(XMLPrintHandler.wrapAttribute(
					ATTRIBUTE_SKIP, new Boolean(fSkip).toString()));
			// Print contextId attribute
			// Print href attribute
			if ((fContextId != null) &&
					(fContextId.length() > 0)) {
				buffer.append(XMLPrintHandler.wrapAttribute(
						ATTRIBUTE_CONTEXTID, 
						PDETextHelper.translateWriteText(
								fContextId.trim(), SUBSTITUTE_CHARS)));
			} else if ((fHref != null) &&
							(fHref.length() > 0)) {
				buffer.append(XMLPrintHandler.wrapAttribute(
						ATTRIBUTE_HREF, 
						PDETextHelper.translateWriteText(
								fHref.trim(), SUBSTITUTE_CHARS)));
			}
			// Start element
			XMLPrintHandler.printBeginElement(writer, buffer.toString(),
					indent, false);
			// Print description element
			if (fDescription != null) {
				fDescription.write(newIndent, writer);
			}
			// Print action | command | perform-when element
			if (fExecutable != null) {
				fExecutable.write(newIndent, writer);
			}
			// Print subitem | repeated-subitem | conditional-subitem elements
			Iterator iterator = fSubItems.iterator();
			while (iterator.hasNext()) {
				ISimpleCSSubItemObject subitem = (ISimpleCSSubItemObject)iterator.next();
				subitem.write(newIndent, writer);
			}
			// Print onCompletion element
			if (fOnCompletion != null) {
				fOnCompletion.write(newIndent, writer);
			}			
			// End element
			XMLPrintHandler.printEndElement(writer, ELEMENT_ITEM, indent);
			
		} catch (IOException e) {
			// Suppress
			//e.printStackTrace();
		} 				
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#reset()
	 */
	public void reset() {
		fOnCompletion = null;
		fExecutable = null;
		fSkip = false;
		fDialog = true;
		fDescription = null;
		fTitle = null;
		fContextId = null;
		fHref = null;
		fSubItems = new ArrayList();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#addSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public void addSubItem(ISimpleCSSubItemObject subitem) {
		fSubItems.add(subitem);
		
		if (isEditable()) {
			fireStructureChanged(subitem, IModelChangedEvent.INSERT);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getOnCompletion()
	 */
	public ISimpleCSOnCompletion getOnCompletion() {
		return fOnCompletion;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#removeSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public void removeSubItem(ISimpleCSSubItemObject subitem) {
		fSubItems.remove(subitem);
		
		if (isEditable()) {
			fireStructureChanged(subitem, IModelChangedEvent.REMOVE);
		}		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setOnCompletion(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSOnCompletion)
	 */
	public void setOnCompletion(ISimpleCSOnCompletion onCompletion) {
		ISimpleCSObject old = fOnCompletion;		
		fOnCompletion = onCompletion;

		if (isEditable()) {
			fireStructureChanged(onCompletion, old);
		}	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getType()
	 */
	public int getType() {
		return TYPE_ITEM;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getName()
	 */
	public String getName() {
		return fTitle;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getChildren()
	 */
	public List getChildren() {
		ArrayList list = new ArrayList();
		// Add subitems
		if (fSubItems.size() > 0) {
			list.addAll(fSubItems);
		}
		// Add unsupported perform-when if it is set as the executable
		if ((fExecutable != null) &&
				(fExecutable.getType() == ISimpleCSConstants.TYPE_PERFORM_WHEN)) {
			list.add(fExecutable);
		}
		return list;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#addSubItem(int, org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public void addSubItem(int index, ISimpleCSSubItemObject subitem) {
		if (index < 0){
			return;
		}
		if (index >= fSubItems.size()) {
			fSubItems.add(subitem);
		} else {
			fSubItems.add(index, subitem);
		}
		
		if (isEditable()) {
			fireStructureChanged(subitem, IModelChangedEvent.INSERT);
		}
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#indexOfSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public int indexOfSubItem(ISimpleCSSubItemObject subitem) {
		return fSubItems.indexOf(subitem);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#isFirstSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public boolean isFirstSubItem(ISimpleCSSubItemObject subitem) {
		int position = fSubItems.indexOf(subitem);
		if (position == 0) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#isLastSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public boolean isLastSubItem(ISimpleCSSubItemObject subitem) {
		int position = fSubItems.indexOf(subitem);
		int lastPosition = fSubItems.size() - 1;
		if (position == lastPosition) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#removeSubItem(int)
	 */
	public void removeSubItem(int index) {
		
		if ((index < 0) ||
				(index > (fSubItems.size() - 1))) {
			return;
		}
		
		ISimpleCSSubItemObject subitem = (ISimpleCSSubItemObject)fSubItems.remove(index);
		
		if (isEditable()) {
			fireStructureChanged(subitem, IModelChangedEvent.REMOVE);
		}			
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getSubItemCount()
	 */
	public int getSubItemCount() {
		return fSubItems.size();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#hasSubItems()
	 */
	public boolean hasSubItems() {
		if (fSubItems.isEmpty()) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getNextSibling(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public ISimpleCSSubItemObject getNextSibling(ISimpleCSSubItemObject subitem) {
		int position = fSubItems.indexOf(subitem);
		int lastIndex = fSubItems.size() - 1;
		if ((position == -1) ||
				(position == lastIndex)) {
			// Either the subitem was not found or the subitem was found but it is 
			// at the last index
			return null;
		}
		return (ISimpleCSSubItemObject)fSubItems.get(position + 1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getPreviousSibling(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject)
	 */
	public ISimpleCSSubItemObject getPreviousSibling(
			ISimpleCSSubItemObject subitem) {
		int position = fSubItems.indexOf(subitem);
		if ((position == -1) ||
				(position == 0)) {
			// Either the sub item was not found or the subitem was found but it is 
			// at the first index
			return null;
		}
		return (ISimpleCSSubItemObject)fSubItems.get(position - 1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#moveSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject, int)
	 */
	public void moveSubItem(ISimpleCSSubItemObject subitem, int newRelativeIndex) {
		// Get the current index of the task object
		int currentIndex = fSubItems.indexOf(subitem);
		// Ensure the object is found
		if (currentIndex == -1) {
			return;
		}
		// Calculate the new index
		int newIndex = newRelativeIndex + currentIndex;
		// Validate the new index
		if ((newIndex < 0) ||
				(newIndex >= fSubItems.size())) {
			return;
		}
		// Remove the task object
		fSubItems.remove(subitem);
		// Add the task object back at the specified index
		fSubItems.add(newIndex, subitem);
		// Send an insert event
		if (isEditable()) {
			fireStructureChanged(subitem, IModelChangedEvent.INSERT);
		}
	}

}