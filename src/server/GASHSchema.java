/*

   GASHSchema.java

   Schema editing frame to work in conjunction with the
   Admin console.
   
   Created: 24 April 1997
   Version: $Revision: 1.45 $ %D%
   Module By: Jonathan Abbey and Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import arlut.csd.Util.*;
import arlut.csd.DataComponent.*;
import arlut.csd.Dialog.*;

import tablelayout.*;

import java.rmi.*;
import java.rmi.server.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.applet.*;
import java.util.*;

import jdj.PackageResources;

import gjt.Box;
import gjt.Util;
import gjt.RowLayout;
import gjt.ColumnLayout;

import arlut.csd.Tree.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      GASHSchema

------------------------------------------------------------------------------*/

public class GASHSchema extends Frame implements treeCallback, treeDragDropCallback, ActionListener {

  SchemaEdit 
    editor;

  Image
    questionImage,
    treeImages[];

  treeControl 
    tree;

  Category
    rootCategory;

  CatTreeNode
    objects;			// root category node

  treeNode
    namespaces;

  MenuItem
    createCategoryMI = null,
    deleteCategoryMI = null,
    createObjectMI = null,
    deleteObjectMI = null,
    createNameMI = null,
    deleteNameMI = null,
    createFieldMI = null,
    deleteFieldMI = null;
  
  PopupMenu
    categoryMenu = null,
    baseMenu = null,
    fieldMenu = null,
    nameSpaceMenu = null,
    nameSpaceObjectMenu = null;

  CardLayout
    card;

  Panel 
    displayPane,
    buttonPane,
    attribPane,
    attribCardPane,
    emptyPane,
    baseEditPane,
    fieldEditPane,
    namespaceEditPane,
    categoryEditPane;

  BaseEditor
    be;

  BaseFieldEditor
    fe;

  NameSpaceEditor
    ne;

  CategoryEditor
    ce;
  
  boolean
    showingBase,
    showingField;

  Button
    okButton, cancelButton;

  Color
    bgColor = SystemColor.control;

  /* -- */

  public GASHSchema(String title, SchemaEdit editor)
  {
    super(title);

    this.editor = editor;

    questionImage = PackageResources.getImageResource(this, "question.gif", getClass());

    //
    //
    //   **** Set up panels
    //
    //
    
    setLayout(new BorderLayout());

    displayPane = new Panel();
    displayPane.setLayout(new BorderLayout());

    attribPane = new Panel();
    attribPane.setBackground(bgColor);
    attribPane.setLayout(new BorderLayout());

    card = new CardLayout();

    attribCardPane = new Panel();
    attribCardPane.setBackground(bgColor);
    attribCardPane.setLayout(card);

    // set up the base editor

    baseEditPane = new Panel();
    baseEditPane.setBackground(bgColor);
    baseEditPane.setLayout(new BorderLayout());

    be = new BaseEditor(this);
    baseEditPane.add("Center", be);

    // set up the base field editor

    fieldEditPane = new Panel();
    fieldEditPane.setBackground(bgColor);
    fieldEditPane.setLayout(new BorderLayout());

    fe = new BaseFieldEditor(this);
    fieldEditPane.add("Center", fe);

    // set up the name space editor

    namespaceEditPane = new Panel();
    namespaceEditPane.setBackground(bgColor);
    namespaceEditPane.setLayout(new BorderLayout());

    ne = new NameSpaceEditor(this);
    namespaceEditPane.add("Center", ne);

    // set up the category editor

    categoryEditPane = new Panel();
    categoryEditPane.setBackground(bgColor);
    categoryEditPane.setLayout(new BorderLayout());

    ce = new CategoryEditor(this);
    categoryEditPane.add("Center", ce);

    // set up the empty card

    emptyPane = new Panel();
    emptyPane.setBackground(bgColor);

    // Finish attribPane setup

    attribCardPane.add("empty", emptyPane);
    attribCardPane.add("base", baseEditPane);
    attribCardPane.add("field", fieldEditPane);
    attribCardPane.add("name", namespaceEditPane);
    attribCardPane.add("category", categoryEditPane);

    attribPane.add("Center", attribCardPane);

    Box rightBox = new Box(attribPane, "Attributes");

    InsetPanel rightPanel = new InsetPanel(5, 5, 5, 10);
    rightPanel.setLayout(new BorderLayout());
    rightPanel.add("Center", rightBox);

    displayPane.add("Center", rightPanel);

    // Set up button pane

    buttonPane = new Panel();
    buttonPane.setLayout(new RowLayout());

    okButton = new Button("ok");
    okButton.addActionListener(this);

    cancelButton = new Button("cancel");
    cancelButton.addActionListener(this);

    buttonPane.add(okButton);
    buttonPane.add(cancelButton);

    add("South", buttonPane);

    //
    //
    //   **** Set up display tree
    //
    //

    treeImages = new Image[3];

    treeImages[0] = PackageResources.getImageResource(this, "openfolder.gif", getClass());
    treeImages[1] = PackageResources.getImageResource(this, "folder.gif", getClass());
    treeImages[2] = PackageResources.getImageResource(this, "list.gif", getClass());

    tree = new treeControl(new Font("SansSerif", Font.BOLD, 12),
			   Color.black, SystemColor.window, this, treeImages,
			   null);
    tree.setDrag(this, tree.DRAG_LINE | tree.DRAG_ICON);

    //
    //
    //   **** Set up display tree panels
    //
    //

    Box leftBox = new Box(tree, "Schema Objects");

    InsetPanel leftPanel = new InsetPanel(5, 10, 5, 5);
    leftPanel.setLayout(new BorderLayout());
    leftPanel.add("Center", leftBox);

    displayPane.add("West", leftPanel);

    add("Center", displayPane);

    //
    //
    //   **** Set up tree popup menus
    //
    //

    // category menu

    categoryMenu = new PopupMenu();

    createCategoryMI = new MenuItem("Create Category");
    deleteCategoryMI = new MenuItem("Delete Category");
    createObjectMI = new MenuItem("Create Object Type");

    categoryMenu.add(createCategoryMI);
    categoryMenu.add(deleteCategoryMI);
    categoryMenu.add(createObjectMI);

    // namespace menu

    nameSpaceMenu = new PopupMenu("Namespace Menu");
    createNameMI = new MenuItem("Create Namespace");
    nameSpaceMenu.add(createNameMI);

    // namespace object menu

    nameSpaceObjectMenu = new PopupMenu();
    deleteNameMI = new MenuItem("Delete Namespace");
    nameSpaceObjectMenu.add(deleteNameMI);

    // base menu

    baseMenu = new PopupMenu("Base Menu");
    deleteObjectMI = new MenuItem("Delete Object Type");
    createFieldMI = new MenuItem("Create Field");
    baseMenu.add(createFieldMI);
    baseMenu.add(deleteObjectMI);

    // field menu

    fieldMenu = new PopupMenu("Field Menu");
    deleteFieldMI = new MenuItem("Delete Field");
    fieldMenu.add(deleteFieldMI);

    //
    //
    //   **** Set up tree 
    //
    //

    try
      {
	rootCategory = editor.getRootCategory();

	objects = new CatTreeNode(null, rootCategory.getName(), rootCategory,
				  null, true, 0, 1, categoryMenu);

	System.err.println("Created rootCategory node: " + rootCategory.getName());
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't get rootCategory " + ex);
      }

    tree.setRoot(objects);

    // create namespaces node

    namespaces = new treeNode(null, "Namespaces", objects, true, 0, 1, nameSpaceMenu);
    tree.insertNode(namespaces, false);

    // and initialize tree

    initializeDisplayTree();

    pack();
    setSize(800, 600);
    show();

    System.out.println("GASHSchema created");
  }

  /**
   *
   *
   *
   */

  public SchemaEdit getSchemaEdit()
    {
      if (editor == null)
	{
	  System.out.println("editor is null in GASHSchema");
	}

      return editor;
    }

  void initializeDisplayTree()
  {
    try
      {
	recurseDownCategories(objects);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't refresh categories" + ex);
      }

    refreshNamespaces();
    tree.refresh();
  }

  void recurseDownCategories(CatTreeNode node) throws RemoteException
  {
    Vector
      children;

    Category 
      c;

    CategoryNode
      cNode;

    treeNode 
      thisNode,
      prevNode;

    /* -- */

    c = node.getCategory();

    // update the current tree node in case the category was renamed

    node.setText(c.getName());

    // get this category's children

    //    c.resort();
    children = c.getNodes();

    prevNode = null;
    thisNode = node.getChild();

    for (int i = 0; i < children.size(); i++)
      {
	// find the CategoryNode at this point in the server's category tree

	cNode = (CategoryNode) children.elementAt(i);

	prevNode = insertCategoryNode(cNode, prevNode, node);
	
	if (prevNode instanceof CatTreeNode)
	  {
	    recurseDownCategories((CatTreeNode) prevNode);
	  }
      }
  }

  /**
   *
   * Local helper method to place a new CategoryNode (either a Base or a Category)
   * into the schema editor's display tree.
   *   
   */

  treeNode insertCategoryNode(CategoryNode node, treeNode prevNode, treeNode parentNode) throws RemoteException
  {
    treeNode newNode = null;

    /* -- */

    if (node instanceof Base)
      {
	Base base = (Base) node;

	newNode = new BaseNode(parentNode, base.getName(), base, prevNode,
			       false, 2, 2, baseMenu);
      }
    else if (node instanceof Category)
      {
	Category category = (Category) node;

	newNode = new CatTreeNode(parentNode, category.getName(), category,
				  prevNode, true, 0, 1, categoryMenu);
      }

    tree.insertNode(newNode, true);

    if (newNode instanceof BaseNode)
      {
	refreshFields((BaseNode)newNode, false);
      }

    return newNode;
  }

  void refreshFields(BaseNode node, boolean doRefresh) throws RemoteException
  {
    Base base;
    BaseField field, fields[];
    Vector vect;
    BaseNode parentNode;
    FieldNode oldNode, newNode, fNode;
    int i;

    /* -- */

    base = node.getBase();

    vect = base.getFields();

    fields = new BaseField[vect.size()];
    
    for (i = 0; i < fields.length; i++)
      {
	fields[i] = (BaseField) vect.elementAt(i);
      }
    
    // Sort the fields by ID, using a funky anonymous
    // class
    
    (new QuickSort(fields, 
		   new arlut.csd.Util.Compare() 
		   {
		     public int compare(Object a, Object b) 
		       {
			 BaseField aF, bF;
      
			 aF = (BaseField) a;
			 bF = (BaseField) b;
	
			 try
			   {
			     if (aF.getDisplayOrder() < bF.getDisplayOrder())
			       {
				 return -1;
			       }
			     else if (aF.getDisplayOrder() > bF.getDisplayOrder())
			       {
				 return 1;
			       }
			     else
			       {
				 return 0;
			       }
			   }
			 catch (RemoteException ex)
			   {
			     throw new RuntimeException("couldn't compare base fields " + ex);
			   }
		       }
		   }
		   )).sort();

    parentNode = node;
    oldNode = null;
    fNode = (FieldNode) node.getChild();
    i = 0;
	
    while ((i < fields.length) || (fNode != null))
      {
	if (i < fields.length)
	  {
	    field = fields[i];
	  }
	else
	  {
	    field = null;
	  }

	if ((fNode == null) ||
	    ((field != null) && 
	     (field.getID() < fNode.getField().getID())))
	  {
	    // insert a new field node

	    newNode = new FieldNode(parentNode, field.getName(), field,
				    oldNode, false, 2, 2, fieldMenu);

	    tree.insertNode(newNode, true);

	    oldNode = newNode;
	    fNode = (FieldNode) oldNode.getNextSibling();

	    i++;
	  }
	else if ((field == null) ||
		 (field.getID() > fNode.getField().getID()))
	  {
	    // delete a field node

	    if (showingField && (fe.fieldDef == fNode.getField()))
	      {
		card.show(attribCardPane, "empty");
	      }

	    // System.err.println("Deleting: " + fNode.getText());
	    newNode = (FieldNode) fNode.getNextSibling();
	    tree.deleteNode(fNode, false);

	    fNode = newNode;
	  }
	else
	  {
	    fNode.setText(field.getName());
	    // System.err.println("Setting: " + field.getName());

	    oldNode = fNode;
	    fNode = (FieldNode) oldNode.getNextSibling();

	    i++;
	  }
      }

    if (doRefresh)
      {
	tree.refresh();
      }
  }

  void refreshNamespaces()
  { 
    boolean isOpen = namespaces.isOpen();
    
    tree.removeChildren(namespaces, false);

    NameSpace[] spaces = null;

    try 
      {
	spaces = editor.getNameSpaces();
      }
    catch (RemoteException e)
      {
	System.out.println("Exception getting NameSpaces: " + e);
      }
    
    for (int i = 0; i < spaces.length ; i++)
      {
	try 
	  {
	    SpaceNode newNode = new SpaceNode(namespaces, spaces[i].getName(), spaces[i], 
					      null, false, 2, 2, nameSpaceObjectMenu);
	    tree.insertNode(newNode, true);
	  }
	catch (RemoteException e)
	  {
	    System.out.println("Exception getting NameSpaces: " + e);
	  }

      }

    if (isOpen)
      {
	tree.expandNode(namespaces, false);
      }

    tree.refresh();
  }
  

  void editBase(BaseNode node)
  {
    be.editBase(node);
    card.show(attribCardPane,"base");
    showingBase = true;
    showingField = false;

    // attach the button pane to the base editor
    
    validate();
  }

  void editField(FieldNode node)
  {
    System.err.println("in GASHSchema.editField");
    fe.editField(node, false);
    card.show(attribCardPane, "field");
    showingBase = false;
    showingField = true;

    // attach the button pane to the field editor
    
    validate();
  }

  void editNameSpace(SpaceNode node)
  {
    ne.editNameSpace(node);
    card.show(attribCardPane, "name");
    showingBase = false;
    showingField = false;

    validate();
  }

  void editCategory(CatTreeNode node)
  {
    ce.editCategory(node);
    card.show(attribCardPane, "category");

    showingBase = false;
    showingField = false;

    validate();
  }

  // treeCallback methods

  public void treeNodeSelected(treeNode node)
  {
    String a, b;
    treeNode n;

    /* -- */

    if (node == null)
      {
	throw new IllegalArgumentException("null node");
      }

    if (node instanceof BaseNode)
      {
	editBase((BaseNode) node);
      }
    else if (node instanceof FieldNode)
      {
	editField((FieldNode) node);
      }
    else if (node instanceof SpaceNode)
      {
	System.out.println("namespacenode selected");
	editNameSpace((SpaceNode) node);
      }
    else if (node instanceof CatTreeNode)
      {
	editCategory((CatTreeNode) node);
      }
    else
      {
	card.show(attribCardPane, "empty");
      }
  }

  public void treeNodeUnSelected(treeNode node, boolean otherNode)
  {
    if (!otherNode)
      {
	card.show(attribCardPane, "empty");
      }

    System.out.println("node " + node.getText() + " unselected");
  }

  public void treeNodeMenuPerformed(treeNode node,
				    java.awt.event.ActionEvent event)
  {
    String nodeText;

    /* -- */

    nodeText = node.getText();

    System.out.println("node " + nodeText + ", action: " + event );

    if (event.getSource() == createCategoryMI)
      {
	try
	  {
	    CatTreeNode cNode = (CatTreeNode) node;
	    Category category = cNode.getCategory();

	    Category newCategory = category.newSubCategory();

	    // we want to insert at the bottom of the base

	    treeNode n = node.getChild();
	    short order = 0;
	    
	    if (n != null)
	      {
		while (n.getNextSibling() != null)
		  {
		    try
		      {
			if (n instanceof BaseNode)
			  {
			    order = (short) (((BaseNode) n).getBase().getDisplayOrder() + 1);
			  }
			else
			  {
			    order = (short) (((CatTreeNode) n).getCategory().getDisplayOrder() + 1);
			  }
		      }
		    catch (RemoteException ex)
		      {
			throw new RuntimeException("couldn't get display order for " + n);
		      }

		    n = n.getNextSibling();
		  }
	      }
	    
	    CatTreeNode newNode = new CatTreeNode(node, newCategory.getName(), newCategory,
						  n, true, 0, 1, categoryMenu);

	    tree.insertNode(newNode, true);

	    editCategory(newNode);
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("couldn't create new base." + ex);
	  }
      }
    else if (event.getSource() == createObjectMI)
      {
	try
	  {
	    CatTreeNode cNode = (CatTreeNode) node;
	    Category category = cNode.getCategory();
	    Base newBase = editor.createNewBase(category);
	    
	    BaseNode newNode = new BaseNode(node, newBase.getName(), newBase,
					    null, false, 2, 2, baseMenu);

	    tree.insertNode(newNode, true);

	    refreshFields(newNode, true);

	    editBase(newNode);
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("couldn't create new base." + ex);
	  }
      }
    else if (event.getSource() == createNameMI)
      {
	System.out.println("Create namespace chosen");

	DialogRsrc dialogResource = new DialogRsrc(this, 
						   "Create new namespace", 
						   "Create a new namespace", 
						   "Create", "Cancel", 
						   questionImage);

	dialogResource.addString("Namespace:");
	dialogResource.addBoolean("Case Insensitive:");

	Hashtable results = new StringDialog(dialogResource).DialogShow();

	String newNameSpace = null;
	Boolean insensitive = null;
	
	//Now check the hash

	if (results == null)
	  {
	    System.out.println("null hashtable, no action taken");
	  }
	else 
	  {
	    System.out.println("Printing the hash:");
	    Enumeration enum = results.keys();

	    while (enum.hasMoreElements()) 
	      {
		//String label = (String)enum.nextElement();
		String label = (String)enum.nextElement();
		Object ob = results.get(label);

		if (ob instanceof String) 
		  {
		    if (label == "Namespace:")
		      {
			System.out.println("Namespace is " + (String)ob);
			newNameSpace = (String)ob;
		      }
		    else
		      {
			System.out.println("Red alert!  unknown string returned: " + (String)ob);
		      }
		  }
		else if (ob instanceof Boolean)
		  {
		    Boolean bool = (Boolean)ob;

		    if (label == "Case Insensitive:")
		      {
			System.out.println("Sensitivity set to: " + bool);
			insensitive = bool;
		      }
		    else 
		      {
			System.out.println("Unknown Boolean returned by Dialog.");
		      }
		  }
		else 
		  {
		    System.out.println("Unknown type returned by Dialog.");
		  }
	      }

	    if ((newNameSpace != null) && (insensitive != null))
	      {
		try
		  {
		    System.out.println("Adding new NameSpace: " + newNameSpace);
		    editor.createNewNameSpace(newNameSpace, insensitive.booleanValue());
		  }
		catch (java.rmi.RemoteException e)
		  {
		    System.out.println("Exception while creating NameSpace: " + e);
		  }
	      }
	  }

	// List out the NameSpaces for testing

	NameSpace[] spaces  = null;

	System.out.println("Actual NameSpaces:");

	try
	  {
	    spaces = editor.getNameSpaces();
	  }
	catch (java.rmi.RemoteException e)
	  {
	    System.out.println("Exception while listing NameSpace: " + e);
	  }
	
	boolean Insensitive = false;
	String name = null;

	for (int i = 0; i < spaces.length ; i++ )
	  {
	    try
	      {
		Insensitive = spaces[i].isCaseInsensitive();
		name = spaces[i].getName();
	      }
	    catch (java.rmi.RemoteException e)
	      {
		System.out.println("Exception while listing NameSpace: " + e);
	      }

	    if (Insensitive)
	      {
		System.out.println("   " + name + " is case insensitive.");
	      }
	    else
	      {
		System.out.println("   " + name + " is not case insensitive.");
	      }
	  }

	refreshNamespaces();

	if (showingField)
	  {
	    fe.refreshFieldEdit();
	  }
	
      }
    else if (event.getSource() == deleteNameMI)
      {
	System.out.println("deleting Namespace");
	treeNode tNode = (treeNode)node;

	DialogRsrc dialogResource = new DialogRsrc(this,
						   "Confirm Name Space Deletion",
						   "Confirm Name Space Deletion",
						   "Delete", "Cancel",
						   questionImage);

	Hashtable results = new StringDialog(dialogResource).DialogShow();

	if (results != null)
	  {
	    try
	      {
		editor.deleteNameSpace(node.getText());
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("Couldn't delete namespace: remote exception " + ex);
	      }

	    refreshNamespaces();

	    if (showingField)
	      {
		fe.refreshFieldEdit();
	      }
	  }
      }
    else if (event.getSource() == deleteObjectMI)
      {
	BaseNode bNode = (BaseNode) node;
	Base b = bNode.getBase();

	// Check to see if this base is removable.  If it's not, then politely
	// inform the user.  Otherwise, pop up a dialog to make them confirm 
	// the deletion.

	boolean isRemovable = false;

	try 
	  {
	    isRemovable = b.isRemovable();
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("exception in isRemovalbe(): " + rx);
	  }

	if (isRemovable)
	  {
	    if (new StringDialog(this,
				 "Confirm deletion of Object",
				 "Are you sure you want to delete this object?",
				 "Confirm",
				 "Cancel").DialogShow() == null)
	      {
		System.out.println("Deletion canceled");
	      }
	    else //Returned confirm
	      {	    
		try
		  {
		    System.err.println("Deleting base " + b.getName());
		    editor.deleteBase(b);
		  }
		catch (RemoteException ex)
		  {
		    throw new RuntimeException("Couldn't delete base: remote exception " + ex);
		  }
		
		tree.deleteNode(node, true);
	      }
	  }
	else
	  {
	    new StringDialog(this,
			     "Error:  Base not removable",
			     "You are not allowed to remove this base.",
			     "Ok",
			     null).DialogShow();
	  }
      }
    else if (event.getSource() == createFieldMI)
      {
	// find the base that asked for the field

	try
	  {
	    BaseNode bNode = (BaseNode) node;
	    System.err.println("Calling editField");

	    // create a name for the new field

	    BaseField bF, bF2;
	    Base b;

	    String newname = "New Field";
	    int j;
	    boolean done;

	    b = bNode.getBase();
	    Vector fieldVect = b.getFields();

	    done = false;

	    j = 0;

	    while (!done)
	      {
		if (j > 0)
		  {
		    newname = "New Field " + (j + 1);
		  }

		done = true;

		for (int i = 0; done && i < fieldVect.size(); i++)
		  {
		    bF2 = (BaseField) fieldVect.elementAt(i);
		    
		    if (bF2.getName().equals(newname))
		      {
			done = false;
		      }
		  }

		j++;
	      }

	    bF = b.createNewField();
	    bF.setName(newname);

	    // we want to insert the child's field node
	    // at the bottom of the base

	    treeNode n = node.getChild();
	    short order = 0;
	    
	    if (n != null)
	      {
		while (n.getNextSibling() != null)
		  {
		    try
		      {
			order = (short) (((FieldNode) n).getField().getDisplayOrder() + 1);
		      }
		    catch (RemoteException ex)
		      {
			throw new RuntimeException("couldn't get display order for " + n);
		      }

		    n = n.getNextSibling();
		  }
	      }

	    try
	      {
		bF.setDisplayOrder(order);
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("couldn't set display order for " + bF);
	      }

	    FieldNode newNode = new FieldNode(node, newname, bF, n,
					      false, 2, 2, fieldMenu);
	    tree.insertNode(newNode, true);
	    editField(newNode);
	    System.err.println("Called editField");
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("couldn't create new field" + ex);
	  }
      }
    else if (event.getSource() == deleteFieldMI)
      {
	FieldNode fNode = (FieldNode) node;
	BaseField field = fNode.getField();
	boolean isEditable = false;
	boolean isRemovable = false;

	try
	  {
	    isRemovable = field.isRemovable();
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Can't get isRemoveable, assuming false: " +rx);
	  }
	if (isRemovable)
	  {
	    try
	      {
		isEditable = field.isEditable();
	      }
	    catch (RemoteException rx)
	      {
		throw new IllegalArgumentException("can't tell if field is editable, assuming false: " + rx);
	      }

	    if (isEditable)
	      {
		System.err.println("deleting field node");

		DialogRsrc dialogResource = new DialogRsrc(this,
							   "Confirm Field Deletion",
							   "Confirm Field Deletion",
							   "Delete", "Cancel",
							   questionImage);

		Hashtable results = new StringDialog(dialogResource).DialogShow();

		if (results != null)
		  {
		    BaseNode bNode = (BaseNode) node.getParent();

		    try
		      {
			if (!bNode.getBase().fieldInUse(fNode.getField()))
			  {
			    bNode.getBase().deleteField(fNode.getField());
			    refreshFields(bNode, true);
			    ne.refreshSpaceList();
			    be.refreshLabelChoice();
			  }
			else
			  {
			    // field in use
			
			    System.err.println("Couldn't delete field.. field in use");
			  }
		      }
		    catch (RemoteException ex)
		      {
			System.err.println("couldn't delete field" + ex);
		      }
		  }
	      }
	    else
	      {
		new StringDialog(this, 
				 "Error: field not editable",
				 "This field is not editable.  You cannot delete it.",
				 "Ok",
				 null).DialogShow();
	      }
	  }
	else
	  {
	    new StringDialog(this,
			     "Error: field not removable",
			     "This field is not removable.",
			     "Ok",
			     null).DialogShow();
	  }
      }
  }

  // action handler

  public void actionPerformed(ActionEvent event)
  {
    System.out.println("event: " + event);

    if (event.getSource() == okButton)
      {
	try
	  {
	    editor.commit();
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("Couldn't commit: " + ex);
	  }

	setVisible(false);
      }
    else if (event.getSource() == cancelButton)
      {
	try
	  {
	    editor.release();
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("Couldn't release: " + ex);
	  }

	setVisible(false);
      }
    else
      {
	System.err.println("Unknown Action Performed in GASHSchema");
      }
  }

  // **
  // The following methods comprise the implementation of arlut.csd.Tree.treeDragDropCallback,
  // and provide the intelligence behind the Schema Editor tree's drag and drop behavior.
  // **

  /**
   *
   * This method determines which nodes may be dragged.
   *
   * @see arlut.csd.Tree.treeDragDropCallback
   */

  public boolean startDrag(treeNode dragNode)
  {
    return ((dragNode instanceof FieldNode) ||
	    (dragNode instanceof BaseNode) ||
	    (dragNode instanceof CatTreeNode &&
	     dragNode != objects));
  }

  /**
   *
   * This method doesn't truly apply to the drag and drop behavior implemented in
   * the Schema Editor.
   *
   * @see arlut.csd.Tree.treeDragDropCallback
   */

  public boolean iconDragOver(treeNode dragNode, treeNode targetNode)
  {
    if (targetNode.isOpen())
      {
	return false;
      }

    if (dragNode instanceof FieldNode)
      {
	return false;
      }

    if (dragNode instanceof BaseNode)
      {
	return (targetNode instanceof CatTreeNode);
      }

    if (dragNode instanceof CatTreeNode)
      {
	if (!(targetNode instanceof CatTreeNode))
	  {
	    return false;
	  }
	
	CatTreeNode cNode = (CatTreeNode) dragNode;
	CatTreeNode cNode1 = (CatTreeNode) targetNode;

	try
	  {
	    return (!cNode1.getCategory().isUnder(cNode.getCategory()));
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote: " + ex);
	  }
      }
    
    return false;
  }

  /**
   *
   * This method doesn't truly apply to the drag and drop behavior implemented in
   * the Schema Editor.
   *
   * @see arlut.csd.Tree.treeDragDropCallback
   */

  public void iconDragDrop(treeNode dragNode, treeNode targetNode)
  {
  }

  /**
   *
   * Method to control whether the drag line may be moved between a pair of given
   * nodes.
   *
   * @see arlut.csd.Tree.treeDragDropCallback
   */

  public boolean dragLineTween(treeNode dragNode, treeNode aboveNode, treeNode belowNode)
  {
    treeNode parent = dragNode.getParent();

    /* -- */

    if (belowNode == objects)
      {
	return false;
      }

    if (dragNode instanceof FieldNode)
      {
	return (((aboveNode instanceof FieldNode) && (aboveNode != null) && (aboveNode.getParent() == parent)) || 
		((belowNode instanceof FieldNode) && (belowNode != null) && (belowNode.getParent() == parent)));
      }
    else if (dragNode instanceof BaseNode)
      {
	if (belowNode instanceof FieldNode)
	  {
	    return false;
	  }

	if (belowNode == namespaces)
	  {
	    return true;
	  }

	return ((aboveNode instanceof BaseNode) || 
		(aboveNode instanceof CatTreeNode) ||
		(belowNode instanceof BaseNode) || 
		(belowNode instanceof CatTreeNode));
      }
    else if (dragNode instanceof CatTreeNode)
      {
	try
	  {
	    if (belowNode instanceof FieldNode)
	      {
		return false;
	      }

	    if (belowNode == namespaces)
	      {
		return true;
	      }

	    if (aboveNode instanceof CatTreeNode)
	      {
		return !((CatTreeNode) aboveNode).getCategory().isUnder(((CatTreeNode) dragNode).getCategory());
	      }
	    
	    if (belowNode instanceof CatTreeNode)
	      {
		return !((CatTreeNode) belowNode).getCategory().isUnder(((CatTreeNode) dragNode).getCategory());
	      }

	    if (aboveNode instanceof BaseNode)
	      {
		return !((BaseNode) aboveNode).getBase().getCategory().isUnder(((CatTreeNode) dragNode).getCategory());
	      }
  
	    if (belowNode instanceof BaseNode)
	      {
		return !((BaseNode) belowNode).getBase().getCategory().isUnder(((CatTreeNode) dragNode).getCategory());
	      }
		    
	    return false;
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("couldn't get category details for drag " + ex);
	  }
      }
    else
      {
	return false;
      }
  }

  /**
   *
   * This method is called when a drag and drop operation in the Schema Editor's tree is completed.
   *
   * @see arlut.csd.Tree.treeDragDropCallback
   */

  public void dragLineRelease(treeNode dragNode, treeNode aboveNode, treeNode belowNode)
  {
    System.out.println("dragNode = " + dragNode.getText());
    System.out.println("aboveNode = " + aboveNode.getText());
    System.out.println("belowNode = " + belowNode.getText());

    if (dragNode instanceof FieldNode)
      {
	FieldNode oldNode = (FieldNode)dragNode;
	BaseNode parentNode = (BaseNode)oldNode.getParent();
	System.out.println("parent = " + parentNode);
	
	if (aboveNode instanceof FieldNode)
	  {
	    if (aboveNode != dragNode)
	      {
		//Insert below the aboveNode
		FieldNode newNode = new FieldNode(parentNode, oldNode.getText(), oldNode.getField(),
						  aboveNode, false, 2, 2, fieldMenu);
		
		tree.deleteNode(dragNode, false);
		tree.insertNode(newNode, true);
	      }
	    else
	      {
		System.out.println("aboveNode == dragNode, Not moving it");
	      }
	  }
	else if (belowNode instanceof FieldNode)
	  {
	    if (belowNode != dragNode)
	      {
		//First node, insert below parent
		FieldNode newNode = new FieldNode(parentNode, oldNode.getText(), oldNode.getField(),
						  null, false, 2, 2, fieldMenu);
		tree.deleteNode(dragNode, false);
		tree.insertNode(newNode, true);
	      }
	    else
	      {
		System.out.println("belowNode == dragNode, Not moving it");
	      }
	  }
	else
	  {
	    System.err.println("Dropped away from FieldNodes, shouldn't happen");
	  }
	
	// Ok, that mostly works, plugging ahead

	// Renumber the fields of this parent.
	
	FieldNode currentNode = (FieldNode)parentNode.getChild();

	if (currentNode != null)
	  {
	    try
	      {
		short i = 0;

		while (currentNode != null)
		  {
		    currentNode.getField().setDisplayOrder(++i);
		    currentNode = (FieldNode)currentNode.getNextSibling();
		  }
		System.out.println("Reordered " + i + " fields");
	      }
	    catch (RemoteException rx)
	      {
		throw new IllegalArgumentException("exception reordering fields: " + rx);
	      }
	  }
	else
	  {
	    System.err.println("No children to renumber, something not right");
	  }
      }
    else if (dragNode instanceof BaseNode)
      {
	try
	  {
	    BaseNode bn = (BaseNode) dragNode;
	    Base base = bn.getBase();
	    Category parent = base.getCategory();

	    Category newCategory;

	    if (aboveNode instanceof CatTreeNode)
	      {
		newCategory = ((CatTreeNode) aboveNode).getCategory();
	      }
	    else if (aboveNode instanceof BaseNode)
	      {
		newCategory = ((BaseNode) aboveNode).getBase().getCategory();
	      }
	    else
	      {
		// if the node below us is the namespaces node,
		// we're going to be moving this base down to the
		// bottom of the top level category hierarchy

		//		if (belowNode == namespaces)

	      }

	  }
	catch (RemoteException ex)
	  {
	  }
	
      }
    else if (dragNode instanceof CatTreeNode)
      {
      }
  }
}


/*------------------------------------------------------------------------------
                                                                           class
                                                                 NameSpaceEditor

------------------------------------------------------------------------------*/

class NameSpaceEditor extends ScrollPane implements ActionListener {
  
  SpaceNode node;
  NameSpace space;
  stringField nameS;
  List spaceL;
  Checkbox caseCB;
  Panel namePanel;
  componentAttr ca;
  GASHSchema owner;
  String currentNameSpaceLabel = null;
  
  /* -- */

  NameSpaceEditor(GASHSchema owner)
  {
    if (owner == null)
      {
	throw new IllegalArgumentException("owner must not be null");
      }

    System.err.println("NameSpaceEditor constructed");

    this.owner = owner;

    namePanel = new InsetPanel(10,10,10,10);
    namePanel.setLayout(new TableLayout(false));

    ca = new componentAttr(this, new Font("SansSerif", Font.BOLD, 12),
			   Color.black, Color.white);
      
    nameS = new stringField(20, 100, ca, false, false, null, null);
    addRow(namePanel, nameS, "Namespace:", 0);
      
    caseCB = new Checkbox();
    caseCB.setEnabled(false);
    addRow(namePanel, caseCB, "Case insensitive:", 1);
    
    spaceL = new List(5);
    //spaceL.setEnabled(false);
    addRow(namePanel, spaceL, "Fields in this space:", 2);
    
    add(namePanel);
  }

  public void editNameSpace(SpaceNode node)
  {
    this.node = node;
    space = node.getSpace();
    
    try
      {
	nameS.setText(space.getName());
	caseCB.setState(space.isCaseInsensitive());
	currentNameSpaceLabel = space.getName();
	refreshSpaceList();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Remote Exception gettin gNameSpace attributes " + rx);
      }
  }

  public void actionPerformed(ActionEvent e)
  {
    System.out.println("action Performed in NameSpaceEditor");
  }

  public void refreshSpaceList()
  {
    spaceL.removeAll();
    SchemaEdit se = owner.getSchemaEdit();
    Base[] bases = null;

    try
      {
	bases = se.getBases();
      }
    catch (RemoteException rx)
      {
	throw new IllegalArgumentException("Exception: can't get bases: " + rx);
      }

    Vector fields = null;
    BaseField currentField = null;
    String thisBase = null;
    String thisField = null;
    String thisSpace = null;

    if ((bases == null) || (currentNameSpaceLabel == null))
      {
	System.out.println("bases or currentNameSpaceLabel is null");
      }
    else
      {
	System.out.println("currentNameSpaceLabel= " + currentNameSpaceLabel);
	  
	for (int i = 0; i < bases.length; i++)
	  {
	    try
	      {
		thisBase = bases[i].getName();
		fields = bases[i].getFields();
	      }
	    catch (RemoteException rx)
	      {
		throw new IllegalArgumentException("exception getting fields: " + rx);
	      }

	    if (fields == null)
	      {
		System.out.println("fields == null");
	      }
	    else
	      {
		for (int j = 0; j < fields.size(); j++)
		  {
		    try 
		      {
			currentField = (BaseField)fields.elementAt(j);

			if (currentField.isString())
			  {
			    thisSpace = currentField.getNameSpaceLabel();

			    if ((thisSpace != null) && (thisSpace.equals(currentNameSpaceLabel)))
			      {
				System.out.println("Adding to spaceL: " + thisBase + ":" + currentField.getName());;
				spaceL.addItem(thisBase + ":" + currentField.getName());
			      }
			  }
		      }
		    catch (RemoteException rx)
		      {
			throw new IllegalArgumentException("Exception generating spaceL: " + rx);
		      }
		  }
	      }
	  }
      }
  }

  void addRow(Panel parent, Component comp,  String label, int row)
  {
    Label l = new Label(label);

    parent.add("0 " + row + " lhwHW", l);
    parent.add("1 " + row + " lhwHW", comp);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                  CategoryEditor

------------------------------------------------------------------------------*/

class CategoryEditor extends ScrollPane implements setValueCallback {

  GASHSchema owner;  
  Panel catPanel;
  stringField catNameS;
  CatTreeNode catNode;
  Category category;

  /* -- */

  CategoryEditor(GASHSchema owner)
  {
    componentAttr ca;

    /* -- */

    if (owner == null)
      {
	throw new IllegalArgumentException("owner must not be null");
      }
    
    System.err.println("CategoryEditor constructed");

    this.owner = owner;
    
    catPanel = new InsetPanel(10,10,10,10);
    catPanel.setLayout(new TableLayout(false));
    
    ca = new componentAttr(this, new Font("SansSerif", Font.BOLD, 12),
			   Color.black, Color.white);
    
    catNameS = new stringField(20, 100, ca, true, false, null, null, this);
    addRow(catPanel, catNameS, "Category Label:", 0);
    
    add(catPanel);
  }

  void editCategory(CatTreeNode catNode)
  {
    this.catNode = catNode;
    this.category = catNode.getCategory();

    try
      {
	catNameS.setText(category.getName());
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Remote Exception gettin gNameSpace attributes " + rx);
      }
  }

  public boolean setValuePerformed(ValueObject v)
  {
    if (v.getSource() == catNameS)
      {
	try
	  {
	    if (category.setName((String) v.getValue()))
	      {
		catNode.setText((String) v.getValue());
		return true;
	      }
	    else
	      {
		return false;
	      }
	  }
	catch (RemoteException ex)
	  {
	    return false;
	  }
      }

    return true;		// what the?
  }

  void addRow(Panel parent, Component comp,  String label, int row)
  {
    Label l = new Label(label);
    
    parent.add("0 " + row + " lhwHW", l);
    parent.add("1 " + row + " lhwHW", comp);
  }
}
