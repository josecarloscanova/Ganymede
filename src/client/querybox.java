/*

   querybox

   This class implements a modal dialog that is popped up to generate
   a Query object that will be used by the rest of the ganymede.client
   package to submit the query to the server for handling.

   Once an instance of querybox is constructed, the client code will
   call myShow() to pop up the dialog and retrieve the Query object.

   If the user chooses not to submit a Query after all, myShow() will
   return null.
   
   Created: 23 July 1997
   Version: $Revision: 1.44 $ %D%
   Module By: Erik Grostic
              Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import arlut.csd.ganymede.*;
import arlut.csd.JDataComponent.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.rmi.RemoteException;

import arlut.csd.JDataComponent.*;

import javax.swing.*;
import javax.swing.border.*;

/*------------------------------------------------------------------------------
                                                                           class 
                                                                        querybox

------------------------------------------------------------------------------*/

/**
 *
 * This class implements a modal dialog that is popped up to generate
 * a Query object that will be used by the rest of the ganymede.client
 * package to submit the query to the server for handling.
 *
 * Once an instance of querybox is constructed, the client code will
 * call myShow() to pop up the dialog and retrieve the Query object.
 * 
 * If the user chooses not to submit a Query after all, myShow() will
 * return null.
 *  
 */

class querybox extends JDialog implements ActionListener, ItemListener {
  
  static final boolean debug = false;

  // ---

  JTabbedPane
    tabPane;

  OptionsPanel optionsPanel = null;	// to hold the frame that we popup to get a list of
				// desired fields in the query's results
  gclient gc;

  Hashtable 
    shortHash;			// Key: Base ID    *--*  Value: Corresponding Base

  // the following hashes are accessed through a set of private accessor
  // methods to avoid confusion
  
  private Hashtable baseIDHash = new Hashtable();
  private Hashtable fieldHash = new Hashtable();
  private Hashtable nameHash = new Hashtable();
  private Hashtable myHash = new Hashtable();

  JButton 
    OkButton = new JButton ("Submit"),
    CancelButton = new JButton("Cancel"),
    addButton = new JButton("Add Choices"),
    removeButton = new JButton("Remove Choices");

  GridBagLayout gbl = new GridBagLayout();
  GridBagConstraints gbc = new GridBagConstraints();

  JPanel 
    titledPanel,
    card_panel,
    query_panel = new JPanel();
  JPanel inner_choice = new JPanel();
  JCheckBox editBox = new JCheckBox("Return Only Editable Objects");
  JCheckBox allBox = new JCheckBox("Return all objects of this type");
  JComboBox baseChoice = new JComboBox();


  // This is so we can hind the middle panel when the show all button is clicked
  CardLayout 
    card_layout;

  Vector
    fieldChoices = new Vector(), // A vector of strings for the field choice menus in QueryRow
    Rows = new Vector(),	// store the QueryRows
    fields;			// FieldTemplates for the selectedBase

  BaseDump selectedBase;
  String baseName;
  boolean 
    editOnly = false,
    showAllItems = false;

  Query
    returnVal;

  /* -- */

  /**
   *
   * Primary constructor.
   *
   * @param defaultBase The object base that will be initially selected.
   *                    May be null.
   *
   * @param gc A gclient used to get access to client caches
   *
   * @param parent The frame that this querybox is to be connected to.
   *
   * @param DialogTitle The title for this dialog.
   *
   */

  public querybox (BaseDump defaultBase, gclient gc,
		   Frame parent, String DialogTitle)
  {
    super(parent, DialogTitle, true); // the boolean value is to make the dialog modal

    // ---
      
    JPanel Choice_Buttons = new JPanel();
    JPanel base_panel = new JPanel();
    JPanel outer_choice = new JPanel();
    JPanel query_Buttons = new JPanel();
    JScrollPane choice_pane = new JScrollPane();
    JPanel contentPane = new JPanel();

    /* -- */

    if (debug)
      {
	System.out.println("Hi! I'm your happy query friend!");
      }

    tabPane = new JTabbedPane();

    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add("Center", tabPane);
    this.getContentPane().add("South", Choice_Buttons);

    tabPane.addTab("Query", null, contentPane);

    // Main constructor for the querybox window
    
    this.gc = gc;
    this.selectedBase = defaultBase;

    this.shortHash = gc.getBaseMap();

    // - Define the main window
    
    contentPane.setLayout(new BorderLayout());
    //    contentPane.setBackground(Color.white);
    
    OkButton.addActionListener(this);
    CancelButton.addActionListener(this);
    Choice_Buttons.setLayout(new FlowLayout ());
    Choice_Buttons.add(OkButton);
    Choice_Buttons.add(CancelButton);
    
    editBox.addItemListener(this);
    editBox.setSelected(false);

    allBox.setSelected(false);
    allBox.addItemListener(this);

    query_panel.setLayout(new BorderLayout());
    //    query_panel.setBackground(Color.lightGray); 
    contentPane.add("Center", query_panel); 

    // - Define the inner window with the query choice buttons

    addButton.addActionListener(this);
    removeButton.addActionListener(this);
    query_Buttons.setLayout(new FlowLayout());
    query_Buttons.add(addButton);
    query_Buttons.add(removeButton);
    query_panel.add("South", query_Buttons);  

    // - Define the two inner choice windows

    GridBagLayout bp_gbl = new GridBagLayout();
    GridBagConstraints bp_gbc = new GridBagConstraints();

    base_panel.setLayout(bp_gbl);
     
    // - Create the choice window containing the fields 

    Enumeration enum = shortHash.elements();
      
    while (enum.hasMoreElements())
      {
	BaseDump key = (BaseDump) enum.nextElement();

	// we want to ignore embedded objects -- for now
	    
	if (key.isEmbedded())
	  {
	    // get a base that works...this embedded would cause
	    // problems [null pointer exceptions, that kind of thing]
		
	    continue;
	  }
	else
	  {
	    String choiceToAdd = key.getName();

	    baseChoice.addItem(choiceToAdd);
	    mapNameToBase(choiceToAdd, key);
	  }
	  
	if (selectedBase != null)
	  {
	    baseChoice.setSelectedItem(selectedBase.getName());
	    this.baseName = selectedBase.getName();
	  }
	else 
	  { 
	    // no default given. pick the one that's there.
	    
	    this.selectedBase = getBaseFromName((String) baseChoice.getSelectedItem());
	    this.baseName = selectedBase.getName();
	  }

	// preload our field cache

	mapBaseNamesToTemplates(selectedBase.getTypeID());
      }
      
    //    baseChoice.setBackground(Color.white);
    baseChoice.addItemListener(this);

    bp_gbc.anchor = bp_gbc.WEST;
    bp_gbc.weightx = 1.0;

    bp_gbc.gridx = 0;
    bp_gbl.setConstraints(baseChoice, bp_gbc);
    base_panel.add(baseChoice);

    bp_gbc.anchor = bp_gbc.EAST;
    bp_gbc.gridx = 1;
    bp_gbl.setConstraints(allBox, bp_gbc);
    base_panel.add(allBox);

    bp_gbc.gridx = 2;
    bp_gbl.setConstraints(editBox, bp_gbc);
    base_panel.add(editBox);

    inner_choice.setLayout(gbl);
    //    inner_choice.setBackground(Color.white);
    
    outer_choice.setLayout(new FlowLayout());
    //    outer_choice.setBackground(Color.white);
    outer_choice.add(inner_choice);

    choice_pane.setViewportView(outer_choice);

    card_layout = new CardLayout();
    card_panel = new JPanel(card_layout);
    card_panel.add(choice_pane, "main");
    card_panel.add(new JPanel(), "blank");

    card_layout.show(card_panel, "main");

    // hack for Swing 1.0.2 to prevent TitledBorder from trying to
    // be clever with colors when surrounding a scrollpane
    titledPanel = new JPanel();
    titledPanel.setBorder(new TitledBorder("Query Fields"));
    titledPanel.setLayout(new BorderLayout());
    titledPanel.add("Center", card_panel);
    titledPanel.add("North", base_panel);


    query_panel.add("Center", titledPanel);

    resetFieldChoices();
    addRow();

    optionsPanel = new OptionsPanel(this);

    tabPane.addTab("Fields Returned", null, optionsPanel);
    this.pack();
  }

  /**
   *
   * Alternate Constructor. Used when no default query is provided 
   *
   * @param gc A gclient used to get access to client caches
   *
   * @param parent The frame that this querybox is to be connected to.
   *
   * @param DialogTitle The title for this dialog.
   *
   */

  public querybox (gclient gc,
                   Frame parent,
		   String myTitle) 
  {
    this(null, gc, parent, myTitle);
  } 

  ///////////////////////
  //   Public Methods  //
  ///////////////////////

  /**
   *
   * This is the main interface to the querybox, and is used to
   * synchronously display the querybox and return the Query
   * generated by it.<br><br>
   *
   */

  public Query myshow()
  {
    // Method to set the querybox to visible or invisible
    
    setSize(800,400);
    setVisible(true);		// our thread will wait at this point
    
    return this.returnVal; // once setVisible is set to false
                           // the program executes this return line
  }

  ////////////////////////
  //   Private Methods  //
  ////////////////////////

  /**
   *
   * This method updates the fieldChoices vector to contain a list of
   * Strings corresponding to fields in the selectedBase that can
   * be chosen in QueryRow's.  We are a little fancy here, in that
   * we include fields from embedded objects.
   *
   */

  private void resetFieldChoices()
  {
    Base tempBase;                            // Used when handling embedded objs
    FieldTemplate template;
    Vector EIPfields = new Vector(); // edit-in-place
    Vector Embedded = new Vector();

    /* -- */

    if (debug)
      {
	System.err.println("querybox.resetFieldChoices(): basename = " + baseName);
      }

    fieldChoices.removeAllElements();

    for (int i=0; fields != null && (i < fields.size()); i++) 
      {
	template = (FieldTemplate) fields.elementAt(i);
	    
	if (template.isEditInPlace())
	  {
	    // We're an edit in place.. we want to recurse down
	    // to the bottom of this edit-in-place tree, and
	    // add the terminals to the global Embedded vector

	    // because getEmbedded is recursive, we need to pass
	    // a vector of FieldTemplate's so that getEmbedded
	    // can recurse down with it.  Hence EIPfields.
		 
	    EIPfields.addElement(template);
	    getEmbedded(EIPfields, template.getName(), null, Embedded);
	    EIPfields.removeElement(template);
	  }

	// ignore containing objects and the like...
	
	if (template.getID() == SchemaConstants.OwnerListField ||
	    template.getID() == SchemaConstants.BackLinksField)
	  {
	    continue;
	  }

	String name = template.getName();

	// Keep a shortcut for our later fieldname parsing
	// This was Erik's idea.. 
	    
	mapEmbeddedToField(name, name);

	// And keep a map from the elaborated field name to
	// the field template.
	
	mapNameToTemplate(name, template);
	
	// and to the base
	
	mapNameToId(name, new Short(selectedBase.getTypeID()));
	
	// and finally add to fieldChoices
	
	if (debug)
	  {
	    System.err.println("querybox: adding field " + name + " to choices for base " + 
			       baseName);
	  }
	
	fieldChoices.addElement(name);
      }
    
    // If we wound up with any embedded (edit-in-place) fields from
    // contained objects, add those fields to our embedded map.

    // note that we don't try to get fancy with where these extra
    // field possibilities are added in the fieldChoices vector.
    
    if (!Embedded.isEmpty())
      {
	for (int i = 0; (i < Embedded.size()); i++)
	  {
	    String embedName = (String) Embedded.elementAt(i);

	    // Ok, let's do our string processing for our field name,
	    // once and for all by removing the slashes and saving
	    // the result. Erik again.
		  
	    String noSlash = embedName.substring(embedName.lastIndexOf("/") + 1,
						 embedName.length());

	    // Add the slash-less name to the name hash, with the key
	    // being the slash filled name
		  
	    mapEmbeddedToField(embedName, noSlash);

	    // and finally add to fieldChoices

	    fieldChoices.addElement(embedName);
	  }

	// and we're done with Embedded.  Clear it out.

	Embedded.removeAllElements();
      }

    if (optionsPanel != null)
      {
	optionsPanel.resetBoxes();
      }
  }

  /**
   *
   * A companion to the prior resetFieldChoices method.
   * It allows fields with references to embedded objects
   * to display the appropriate sub-fields. 
   * 
   * It is a recursive method, and can handle any number
   * of layers of embedding. The fields are stored in
   * a 'global' vector (as strings)
   *
   */
  
  private void getEmbedded(Vector fields, String basePrefix, 
			   Short lowestBase, Vector Embedded)
  {
    FieldTemplate tempField;
    String myName;
    Short tempIDobj;
    short tempID;
      
    /* -- */
    
    // Examine each field and if it's not referring to an embedded,
    // then add it's name + basePrefix to the string vector
    
    for (int j=0; fields != null && (j < fields.size()); j++)
      { 
	tempField = (FieldTemplate) fields.elementAt(j);
	      
	if (tempField.getID() != SchemaConstants.OwnerListField &&
	    tempField.getID() != SchemaConstants.BackLinksField)
	  {
	    // ignore containing objects and the like...

	    myName = tempField.getName();
	    myName = basePrefix + "/" + myName;  // slap on the prefix

	    // save the embedded information in our Embedded vector

	    Embedded.addElement(myName);
		
	    mapNameToTemplate(myName, tempField);
		   
	    // Also, save the information on the target base
	    // in a hashtable
		      
	    // the ID will be used in creating the query for the 
	    // edit-in-place
		    
	    // if tempIDobj isn't null, then we've got 
	    // something beneath an edit in place. Add the
	    // id of the lowest level base to the baseIDHash

	    mapNameToId(myName, lowestBase);
	  }

	if (tempField.isEditInPlace())
	  {
	    // since it does refer to an embedded, call
	    // getEmbedded again, with tempID's templateVector,
	    // basePrefix/tempBase,

	    myName = tempField.getName();
	    myName = basePrefix + "/" + myName;  // slap on the prefix
		  
	    tempID = tempField.getTargetBase();

	    if (tempID >= 0)
	      {
		tempIDobj = new Short(tempID);
		    
		// process embedded fields for target
		    
		getEmbedded(gc.getTemplateVector(tempID), 
			    basePrefix, tempIDobj, Embedded);
	      }
	  }
      }
  }

  /**
   *
   * This internal method takes the current state of the rows in the
   * main query composition panel and generates an appropriate Query
   * structure from them.<br><br>
   *
   * Note that this is a private method.. our 'Ok' handler will call
   * this method before hiding this dialog, at which time myShow will
   * return the Query produced by this method.
   *  
   */
  
  private Query createQuery()
  {
    QueryNode myNode;
    QueryRow row;
    String myBaseName;
    Hashtable baseQueries = new Hashtable();
    Vector baseQVec;
    Query result = null;
    short returnType;

    /* -- */

    returnType = selectedBase.getTypeID();

    // If showAllItems is true, then we need to show everything.

    if (showAllItems)
      {
	return new Query((String)baseChoice.getSelectedItem(), null, editOnly);
      }

    for (int i = 0; i < Rows.size(); i++)
      {
	row = (QueryRow) Rows.elementAt(i);

	try
	  {
	    myBaseName = row.getBase().getName();
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("Whoah, guess Base really was remote! " + ex.getMessage());
	    return null;
	  }

	// We have a hash of base names because of our need to enable
	// querying on fields in embedded objects.  We basically send
	// a set of queries to the server, one per base that we're
	// interested in.  The primary query that we send to the server
	// specifies the return type we're interested in.

	if (baseQueries.get(myBaseName) == null)
	  {
	    baseQVec = new Vector();
	    baseQVec.addElement(row.getQueryNode());
	    baseQueries.put(myBaseName, baseQVec);
	  }
	else
	  {
	    baseQVec = (Vector) baseQueries.get(myBaseName);
	    baseQVec.addElement(row.getQueryNode());
	  }
      }

    // ok, we now have a hash of base names that we're going
    // to need to issue queries on.. 

    Enumeration enum = baseQueries.keys();

    while (enum.hasMoreElements())
      {
	myBaseName = (String) enum.nextElement();
	baseQVec = (Vector) baseQueries.get(myBaseName);

	myNode = (QueryNode) baseQVec.elementAt(0);

	// if baseQVec has more than one element, And in the remainder
	// of the query nodes for this base.

	for (int i = 1; i < baseQVec.size(); i++)
	  {
	    myNode = new QueryAndNode(myNode, (QueryNode) baseQVec.elementAt(i));
	  }

	if (result == null)
	  {
	    result = new Query(myBaseName, myNode, editOnly);
	    result.setReturnType(returnType);
	   
	    if (debug)
	      {
		System.err.println("Creating primary Query on base " + myBaseName);
	      }
	  }
	else
	  {
	    Query adjunctQuery = new Query(myBaseName, myNode, editOnly);
	    adjunctQuery.setReturnType(returnType);

	    result.addQuery(adjunctQuery);

	    if (debug)
	      {
		System.err.println("Creating adjunct Query on base " + myBaseName);
	      }
	  }
      }

    return result;
  }

  /**
   *
   * This method sets what fields should be returned by the Query.  Note
   * that this should only be called if the user has explicitly requested
   * a non-standard list of return fields, as the server will automatically
   * hide a bunch of undesired fields if we have not called addField() on
   * a newly constructed Query object.
   *
   */
  
  public Query setFields(Query someQuery)
  {
    FieldTemplate tempField;
    String tempString;
    Vector fieldsToReturn = null;

    /* -- */

    if (optionsPanel != null)
      {
	fieldsToReturn = optionsPanel.getReturnFields();
      }
    
    if (fieldsToReturn == null)
      {
	return someQuery;
      }

    for (int i = 0; i < fieldsToReturn.size(); i++)
      {
	tempString = (String) fieldsToReturn.elementAt(i);
	tempField = getTemplateFromName(tempString);
	
	someQuery.addField(tempField.getID());
      }

    return someQuery;
  }

  /**
   *
   * This is the standard ActionListener callback method.  This method
   * catches events from the various buttons used by querybox.
   *
   * @see java.awt.event.ActionListener
   * 
   */
  
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == OkButton) 
      {
	returnVal = createQuery();
	returnVal = setFields(returnVal);
	setVisible(false);	// close down
      } 
    else if (e.getSource() == CancelButton)
      {
	if (debug)
	  {
	    System.out.println("Cancel was pushed");
	  }

	returnVal = null;
	setVisible(false);
      } 

    if (e.getSource() == addButton)
      {
	addRow();
      }

    if (e.getSource() == removeButton)
      {
	if (Rows.size() <= 1)
	  {
	    // need some sort of gui notify here
	    System.out.println("Error: cannot remove any more rows");
	  }  
	else
	  {
	    removeRow();
	  }
      }
  } 

  private void removeRow()
  {
    QueryRow row = (QueryRow) Rows.lastElement();
    row.removeRow();
    Rows.removeElementAt(Rows.size()-1);
    inner_choice.revalidate();
  }

  private void addRow()
  {
    Rows.addElement(new QueryRow(inner_choice, this));
  }

  /**
   *
   * This is the standard ItemListener callback method.  This method
   * catches events from Checkboxes and various choice components.
   * 
   * @see java.awt.event.ItemListener
   *
   */
  
  public void itemStateChanged(ItemEvent e)
  {
    /* -- */

    if (e.getSource() == editBox)
      {
	this.editOnly = editBox.isSelected();

	if (debug)
	  {
	    System.out.println("Edit Box Clicked: " + editOnly);
	  }
      }

    else if (e.getSource() == allBox)
      {
	this.showAllItems = allBox.isSelected();

	if (debug)
	  {
	    System.out.println("Show all items is selected: " + allBox.isSelected());
	  }

	if (showAllItems)
	  {
	    if (debug)
	      {
		System.out.println("Showing blank");
	      }

	    card_layout.show(card_panel, "blank");
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("Showing main.");
	      }

	    card_layout.show(card_panel, "main");
	  }
      }
    else if (e.getSource() == baseChoice)
      {
	if (debug)
	  {
	    System.out.println("Base selected");
	  }

	// First, change the base
	  
	selectedBase = getBaseFromName((String) baseChoice.getSelectedItem());

	this.baseName = selectedBase.getName();

	// update field name map
	
	mapBaseNamesToTemplates(selectedBase.getTypeID());

	// update the fieldChoices vector

	resetFieldChoices();
	  
	// remove all rows in vector of component arrays

	while (Rows.size() > 0)
	  {
	    removeRow();
	  }

	addRow();
      }
  }

  // ***
  //
  // private convenience methods
  //
  // ***

  // we have a map from base name to base id

  /**
   *
   * This method maps the name of a (possibly embedded)
   * field to the Short id of the Base that it
   * belongs to.<br><br>
   *
   * This is used to support embedded fields.. as
   * getEmbedded() recurses down through the
   * embedded base hierarchy under selectedBase,
   * it records the Base for each embedded field
   * as it goes along creating names for the
   * embedded fields.
   *
   */

  private void mapNameToId(String name, Short id)
  {
    if (id != null)
      {
	baseIDHash.put(name, id);
      }
  }

  /**
   *
   * This method returns the Short id of the Base
   * that corresponds to the field with name
   * &lt;name&gt;.<br><br>
   *
   * This is used to support embedded fields.. as
   * getEmbedded() recurses down through the
   * embedded base hierarchy under selectedBase,
   * it records the Base for each embedded field
   * as it goes along creating names for the
   * embedded fields.
   *
   */

  Short getIdFromName(String name)
  {
    return (Short) baseIDHash.get(name);
  }

  private void mapBaseNamesToTemplates(short id)
  {
    FieldTemplate template;

    /* -- */

    fields = gc.getTemplateVector(id);
    
    if (fields != null)
      {
	fieldHash.clear();

	for (int i = 0; i < fields.size(); i++)
	  {
	    template = (FieldTemplate) fields.elementAt(i);
	    mapNameToTemplate(template.getName(), template);
	  }
      }
  }

  // we have a map from fieldname to field template

  void mapNameToTemplate(String name, FieldTemplate template)
  {
    fieldHash.put(name, template);
  }

  FieldTemplate getTemplateFromName(String name)
  {
    return (FieldTemplate) fieldHash.get(name);
  }

  // we have a map from embedded fieldname (with slashes) to the name
  // template after the last slash

  void mapEmbeddedToField(String name, String fieldName)
  {
    nameHash.put(name, fieldName);
  }

  String getFieldFromEmbedded(String name)
  {
    return (String) nameHash.get(name);
  }

  // we have a map from base names to Base

  void mapNameToBase(String name, BaseDump base)
  {
    myHash.put(name, base);
  }

  BaseDump getBaseFromName(String name)
  {
    return (BaseDump) myHash.get(name);
  }

  BaseDump getBaseFromShort(Short id)
  {
    return (BaseDump) shortHash.get(id);
  }

  BaseDump getBaseFromShort(short id)
  {
    return (BaseDump) shortHash.get(new Short(id));
  }
}

/*------------------------------------------------------------------------------
                                                                           class 
                                                                        QueryRow

------------------------------------------------------------------------------*/

class QueryRow implements ItemListener {

  static final boolean debug = false;

  // ---

  querybox parent;
  JPanel panel;
  FieldTemplate field = null;

  Vector fields;		// FieldTemplate Vector for the selectedBase

  JComboBox
    fieldChoice = new JComboBox(),
    boolChoice = new JComboBox(),
    compareChoice = new JComboBox();

  JPanel operandContainer= new JPanel();
  JComponent operand = null;

  String fieldName;

  boolean showDoes = false;

  /* -- */

  QueryRow(JPanel panel, querybox parent)
  {
    this.panel = panel;
    this.parent = parent;

    try
      {
	fields = parent.gc.getTemplateVector(parent.selectedBase.getTypeID());
	resetFieldChoices();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("remote exception in QueryRow constructor:" + ex.getMessage());
      }

    GridBagConstraints gbc = parent.gbc;
    GridBagLayout gbl = parent.gbl;

    gbc.anchor = gbc.WEST;
    gbc.gridy = parent.Rows.size();
    gbc.gridx = gbc.RELATIVE;
    gbc.gridheight = 1;
    gbc.gridwidth = 1;

    gbl.setConstraints(fieldChoice, gbc);
    panel.add(fieldChoice);

    gbl.setConstraints(boolChoice, gbc);
    panel.add(boolChoice);

    gbl.setConstraints(compareChoice, gbc);
    panel.add(compareChoice);

    // we have to wrap the operand component in a container so that
    // we can change the operand component later

    operandContainer.setOpaque(false);
    operandContainer.add(operand);

    gbl.setConstraints(operandContainer, gbc);
    panel.add(operandContainer);
  }

  /**
   *
   * Internal method to return a choice menu containing the fields for
   * a particular base
   *
   */
    
  private void resetFieldChoices() throws RemoteException
  {
    // we don't want to be bothered while we configure our components

    fieldChoice.removeItemListener(this);

    // ok, refresh fieldChoice

    if (fieldChoice.getItemCount() > 0)
      {
	try
	  {
	    fieldChoice.removeAllItems();
	  }
	catch (IndexOutOfBoundsException e)
	  {
	    System.out.println("IndexOfOutBounds: " + e);
	  }
      }

    // we want to be able to allow the user to search on fields in
    // embedded objects

    for (int i = 0; i < parent.fieldChoices.size(); i++)
      {
	if (debug)
	  {
	    System.err.println("QueryRow: adding field choice <" + i + ">:" + 
			       parent.fieldChoices.elementAt(i));
	  }

	fieldChoice.addItem(parent.fieldChoices.elementAt(i));
      }

    // now, what field wound up being shown?

    String fieldName = (String) fieldChoice.getSelectedItem();
    FieldTemplate field = parent.getTemplateFromName(fieldName);

    setField(field, fieldName);

    fieldChoice.addItemListener(this);
  }

  /**
   *
   * This method takes care of matters when we change or set our
   * field combo box.  Note that we don't set the fieldChoice
   * contents here, as we assume it will be done by the user
   * or by resetFieldChoices().
   *
   */

  void setField(FieldTemplate field, String fieldName)
  {
    // ok, now update our is/is not, comparator, and operand fields

    this.field = field;

    if (fieldName == null)
      {
	this.fieldName = field.getName();
      }
    else
      {
	this.fieldName = fieldName;
      }

    if (debug)
      {
	System.err.println("QueryRow.setField(" + fieldName + ")");
      }

    resetCompare(field);
    resetBoolean(field, (String) compareChoice.getSelectedItem());
    resetOperand(field, (String) compareChoice.getSelectedItem());
    panel.revalidate();
  }

  /**
   *
   * This method sets up the boolean choice combobox.
   *
   */

  void resetBoolean(FieldTemplate field, String opName)
  {
    boolean does;

    /* -- */

    if (debug)
      {
	System.err.println("QueryRow.resetBoolean(" + field.getName() + ", " + opName + ")");
      }

    // don't show us changing it

    does = opName.equalsIgnoreCase("Start With") ||
      opName.equalsIgnoreCase("End With") ||
      opName.equalsIgnoreCase("Contain");

    if (does && (!showDoes || boolChoice.getItemCount() == 0))
      {
	boolChoice.setVisible(false);
	if (boolChoice.getItemCount() > 0)
	  {
	    boolChoice.removeAllItems();
	  }

	boolChoice.addItem("does");
	boolChoice.addItem("does not");

	boolChoice.setVisible(true);
	showDoes = true;
      }
    else if (!does && (showDoes || boolChoice.getItemCount() == 0))
      {
	boolChoice.setVisible(false);
	if (boolChoice.getItemCount() > 0)
	  {
	    boolChoice.removeAllItems();
	  }

	boolChoice.addItem("is");
	boolChoice.addItem("is not");

	boolChoice.setVisible(true);
	showDoes = false;
      }
  }

  /**
   *
   * This method sets up the comparison operator combobox.
   *
   */

  void resetCompare(FieldTemplate field)
  {
    if (debug)
      {
	System.err.println("QueryRow.resetCompare(" + field.getName() + ")");
      }

    compareChoice.removeItemListener(this);

    // don't show us changing it

    compareChoice.setVisible(false);
    
    if (compareChoice.getItemCount() > 0)
      {
	compareChoice.removeAllItems();
      }

    if (field.isEditInPlace())
      {
	compareChoice.addItem("Length <");
	compareChoice.addItem("Length >");
	compareChoice.addItem("Length ==");
      }
    else if (field.isArray())
      {
	compareChoice.addItem("Contain");
	compareChoice.addItem("Length <");
	compareChoice.addItem("Length >");
	compareChoice.addItem("Length ==");
      }
    else if (field.isDate())
      {
	compareChoice.addItem("Before");
	compareChoice.addItem("After");
	compareChoice.addItem("Same Day As");
	compareChoice.addItem("Same Week As");
	compareChoice.addItem("Same Month As");
      }
    else if (field.isNumeric())
      {
	compareChoice.addItem("==");
	compareChoice.addItem("<");
	compareChoice.addItem(">");
	compareChoice.addItem("<=");
	compareChoice.addItem(">=");
      }
    else if (field.isBoolean())
      {
	compareChoice.addItem("==");
      }
    else if (field.isIP())
      {
	compareChoice.addItem("==");
	compareChoice.addItem("Start With");
	compareChoice.addItem("End With");
      }
    else if (field.isString())
      {
	compareChoice.addItem("==");
	compareChoice.addItem("== [Case Insensitive]");
	compareChoice.addItem("<");
	compareChoice.addItem(">");
	compareChoice.addItem("<=");
	compareChoice.addItem(">=");
	compareChoice.addItem("Start With");
	compareChoice.addItem("End With");
      }
    else if (field.isInvid())
      {
	compareChoice.addItem("==");
      }

    compareChoice.addItem("Defined");
    compareChoice.setVisible(true);
    compareChoice.addItemListener(this);
  }

  /**
   *
   * This method sets up the operand GUI component.
   *
   */

  void resetOperand(FieldTemplate field, String opName)
  {
    boolean addOperand = false;

    /* -- */

    if (debug)
      {
	System.err.println("QueryRow.resetOperand(" + field.getName() + ", " + opName + ")");
      }

    // when we test for defined, we won't have an operand value

    if (opName.equals("Defined"))
      {
	if (operand != null)
	  {
	    operand.setVisible(false);
	    operandContainer.remove(operand);
	    operand = null;
	  }

	return;
      }

    if (opName.startsWith("Length"))
      {
	if (!(operand instanceof JnumberField))
	  {
	    if (operand != null)
	      {
		operand.setVisible(false);
		operandContainer.remove(operand);
	      }

	    operand = new JnumberField();
	    addOperand = true;
	  }
      }
    else if (field.isDate())
      {
	if (!(operand instanceof JdateField))
	  {
	    if (operand != null)
	      {
		operand.setVisible(false);
		operandContainer.remove(operand);
	      }

	    operand = new JdateField(new Date(), true, false, null, null);
	    addOperand = true;
	  }
      }
    else if (field.isString())
      {
	if (!(operand instanceof JstringField))
	  {
	    if (operand != null)
	      {
		operand.setVisible(false);
		operandContainer.remove(operand);
	      }

	    operand = new JstringField();
	    addOperand = true;
	  }
      }
    else if (field.isNumeric())
      {
	if (!(operand instanceof JnumberField))
	  {
	    if (operand != null)
	      {
		operand.setVisible(false);
		operandContainer.remove(operand);
	      }

	    operand = new JnumberField();
	    addOperand = true;
	  }
      }
    else if (field.isBoolean())
      {
	if (!(operand instanceof JCheckBox))
	  {
	    if (operand != null)
	      {
		operand.setVisible(false);
		operandContainer.remove(operand);
	      }

	    operand = new JCheckBox();
	    addOperand = true;
	  }
      }
    else if (field.isInvid())
      {
	short targetBase = field.getTargetBase();

	if (targetBase < 0)
	  {
	    if (!(operand instanceof JstringField))
	      {
		if (operand != null)
		  {
		    operand.setVisible(false);
		    operandContainer.remove(operand);
		  }
		
		operand= new JstringField();
		addOperand = true;
	      }
	  }
	else
	  {
	    Short Target = new Short(targetBase);
	    objectList list;
	    JInvidChooser invidChooser;
	    Vector choices;

	    /* -- */

	    // we always want to reset the invid chooser

	    if (operand != null)
	      {
		operand.setVisible(false);
		operandContainer.remove(operand);
	      }

	    // get a fully expanded (non-editables included) list of objects
	    // from our parent.

	    list = parent.gc.getObjectList(Target, true);

	    choices = list.getListHandles(false, true); // no inactives, but do want non-editables
	    choices = parent.gc.sortListHandleVector(choices);

	    operand = invidChooser = new JInvidChooser(choices, null, targetBase);

	    addOperand = true;

	    invidChooser.setMaximumRowCount(12);
	    invidChooser.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
	    invidChooser.setEditable(false);
	  }
      }
    else if (field.isIP())
      {
	if (!(operand instanceof JIPField))
	  {
	    if (operand != null)
	      {
		operand.setVisible(false);
		operandContainer.remove(operand);
	      }
	    
	    operand = new JIPField(true, true);
	    addOperand = true;
	  }
      }

    if (operand == null)
      {
	throw new NullPointerException("null operand");
      }

    if (addOperand)
      {
	operandContainer.add(operand);
	operand.setVisible(true);
      }
  }

  /**
   *
   * This method is called when the querybox wants to remove this row.
   * This method takes care of removing all components from panel, but
   * does not take care of removing itself from the querybox Rows Vector.
   *
   */

  void removeRow()
  {
    panel.remove(fieldChoice);
    panel.remove(boolChoice);
    panel.remove(compareChoice);
    panel.remove(operandContainer);
  }

  /**
   *
   * This method returns a reference to the Base that this QueryRow
   * is set to search on.  The Base that an individual QueryRow is
   * set to search on may differ from the selectedBase in parent
   * because we allow searches on fields contained in embedded
   * objects.
   * 
   */

  public Base getBase()
  {
    if (debug)
      {
	System.err.println("querybox: getBase(): fieldName = " + fieldName);
	System.err.println("Id = " + parent.getIdFromName(fieldName));
      }

    return parent.getBaseFromShort(parent.getIdFromName(fieldName));
  }

  /**
   *
   * This method returns a QueryNode corresponding to the current
   * configuration of this QueryRow.
   * 
   */

  public QueryNode getQueryNode()
  {
    QueryNode myNode;
    Object value = null;
    Short baseID;

    String localFieldName = parent.getFieldFromEmbedded(fieldName);

    /* -- */

    if (operand instanceof JnumberField)
      {
	JnumberField numField = (JnumberField) operand;
	value = numField.getValue();
      }
    else if (operand instanceof JdateField)
      {
	JdateField dateField = (JdateField) operand;
	value = dateField.getDate();
      }
    else if (operand instanceof JCheckBox)
      {
	JCheckBox boolField = (JCheckBox) operand;
	value = new Boolean(boolField.isSelected());
      }
    else if (operand instanceof JIPField)
      {
	JIPField ipField = (JIPField) operand;
	value = ipField.getValue();
      }
    else if (operand instanceof JstringField)
      { 
	JstringField stringField = (JstringField) operand;
	value = stringField.getValue();
      }
    else if (operand instanceof JInvidChooser)
      {
	JInvidChooser invidChooser = (JInvidChooser) operand;
	value = invidChooser.getSelectedInvid();
      }
    else if (operand != null)
      {
	System.err.println("Couldn't get a value.. unknown operand type! " + operand.getClass().toString());
      }

    String operator = (String) compareChoice.getSelectedItem();
    byte opValue = QueryDataNode.NONE;
    byte arrayOp = QueryDataNode.NONE;
    
    if (field.isArray())
      {
	if (operator.equals("Contain"))
	  {
	    opValue = QueryDataNode.CONTAINS;
	  } 
	else if (operator.equals("Length =="))
	  {
	    arrayOp = QueryDataNode.LENGTHEQ;
	  } 
	else if (operator.equals("Length >"))
	  {
	    arrayOp = QueryDataNode.LENGTHGR;
	  } 
	else if (operator.equals("Length <"))
	  {
	    arrayOp = QueryDataNode.LENGTHLE;
	  }
	else if (operator.equals("Defined"))
	  {
	    opValue = QueryDataNode.DEFINED;
	  }

	if (opValue == QueryDataNode.NONE && arrayOp == QueryDataNode.NONE)
	  {
	    System.err.println("QueryRow.getQueryNode(): Unknown array comparator");
	    return null;
	  }

	myNode = new QueryDataNode(localFieldName, opValue, arrayOp, value);

	if (debug)
	  {
	    System.err.println("QueryDataNode: " + myNode.toString());
	  }
	    
	// -- if not is true then add a not node
	    
	if (isNot())
	  {
	    myNode = new QueryNotNode(myNode); // if NOT then add NOT node
	  } 
	    
	return myNode;
      }
    else if (!operator.startsWith("Same"))
       {
	// ok, normal scalar field, not a time window comparison

	if (operator.equals("=="))
	  {
	    opValue = QueryDataNode.EQUALS;
	  } 
	else if (operator.equals("<") || operator.equals("Before"))
	  {
	    opValue = QueryDataNode.LESS;
	  } 
	else if (operator.equals("<="))
	  {
	    opValue = QueryDataNode.LESSEQ;
	  } 
	else if (operator.equals(">") || operator.equals("After"))
	  {
	    opValue = QueryDataNode.GREAT;
	  } 
	else if (operator.equals(">="))
	  {
	    opValue = QueryDataNode.GREATEQ;
	  } 
	else if (operator.equals("== [Case Insensitive]"))
	  {
	    opValue = QueryDataNode.NOCASEEQ;
	  }
	else if (operator.equals("Start With"))
	  {
	    opValue = QueryDataNode.STARTSWITH;
	  }
	else if (operator.equals("End With"))
	  {
	    opValue = QueryDataNode.ENDSWITH;
	  }
	else if (operator.equals("Defined"))
	  {
	    opValue = QueryDataNode.DEFINED;
	  }

	if (opValue == 0)
	  {
	    System.err.println("QueryRow.getQueryNode(): Unknown scalar comparator");
	    return null;
	  }
	    
	myNode = new QueryDataNode(localFieldName, opValue, value);

	if (debug)
	  {
	    System.err.println("QueryDataNode: " + myNode.toString());
	  }
	    
	// -- if not is true then add a not node
	    
	if (isNot())
	  {
	    myNode = new QueryNotNode(myNode); // if NOT then add NOT node
	  } 
	    
	return myNode;
      }
    else
      {
	if (!(value instanceof Date))
	  {
	    System.err.println("QueryRow.getQueryNode(): Don't have a proper date value");
	    return null;
	  }

	Date
	  lowDate,
	  hiDate,
	  dateValue = (Date) value;

	Calendar cal = Calendar.getInstance();

	cal.setTime(dateValue);

	cal.set(Calendar.HOUR, 0);
	cal.set(Calendar.MINUTE, 0);
	cal.set(Calendar.SECOND, 0);
	cal.set(Calendar.MILLISECOND, 0);

	if (operator.equals("Same Day As"))
	  {
	    lowDate = cal.getTime();

	    cal.roll(Calendar.DATE, true);

	    hiDate = cal.getTime();
	  }
	else if (operator.equals("Same Week As"))
	  {
	    cal.set(Calendar.DAY_OF_WEEK, 0);
	    lowDate = cal.getTime();
		
	    cal.roll(Calendar.WEEK_OF_YEAR, true);

	    hiDate = cal.getTime();
	  }
	else if (operator.equals("Same Month As"))
	  {
	    cal.set(Calendar.DAY_OF_MONTH, 0);
	    lowDate = cal.getTime();
		
	    cal.roll(Calendar.MONTH, true);

	    hiDate = cal.getTime();
	  }
	else
	  {
	    System.err.println("QueryRow.getQueryNode(): Don't have a proper date comparator");
	    return null;
	  }

	myNode = new QueryAndNode(new QueryDataNode(localFieldName, QueryDataNode.GREATEQ, lowDate),
				  new QueryDataNode(localFieldName, QueryDataNode.LESS, hiDate));
	    
	// -- if not is true then add a not node
	    
	if (isNot())
	  {
	    myNode = new QueryNotNode(myNode); // if NOT then add NOT node
	  } 
	    
	return myNode;
      }
  }

  /**
   *
   * @return true if this QueryRow negates the basic comparison
   *
   */

  private boolean isNot()
  {
    return (boolChoice.getSelectedItem().equals("is not") ||
	    boolChoice.getSelectedItem().equals("does not"));
  }

  /**
   *
   * This is the standard ItemListener callback method.  This method
   * catches events from Checkboxes and various choice components.
   *
   * @see java.awt.event.ItemListener
   * 
   */
  
  public void itemStateChanged(ItemEvent e)
  {
    // we want to ignore deselect events

    if (e.getStateChange() == e.DESELECTED)
      {
	return;
      }

    if (e.getSource() == fieldChoice)
      {
	setField(parent.getTemplateFromName((String) fieldChoice.getSelectedItem()), 
		 (String) fieldChoice.getSelectedItem());
      }
    else if (e.getSource() == compareChoice)
      {
	String compareOperator = (String) compareChoice.getSelectedItem();
	resetBoolean(field, compareOperator);
	resetOperand(field, compareOperator);
	panel.revalidate();
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class 
                                                                    OptionsFrame

------------------------------------------------------------------------------*/

class OptionsPanel extends JPanel {

  static final boolean debug = false;

  // ---

  querybox parent;

  Hashtable checkboxes = new Hashtable();

  JPanel builtInPanel = new JPanel();
  JPanel customPanel = new JPanel();

  /* -- */

  /**
   *
   * This internal method is used to create a frame which will
   * present a matrix of checkboxes corresponding to the fields
   * available in the specified object base.  The user will
   * be able to select various checkboxes to control which fields
   * are to be returned by the query generated by this querybox.
   *
   */

  OptionsPanel(querybox parent)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints(); 

    /* -- */

    this.parent = parent;

    setLayout(gbl);

    gbc.fill=gbc.BOTH;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;

    builtInPanel.setBorder(new TitledBorder("Built-In Fields"));
    builtInPanel.setLayout(new FlowLayout());

    gbc.gridy = 0;
    gbl.setConstraints(builtInPanel, gbc);

    add(builtInPanel);

    customPanel.setBorder(new TitledBorder("Custom Fields"));
    customPanel.setLayout(new FlowLayout());

    gbc.gridy = 1;
    gbl.setConstraints(customPanel, gbc);

    add(customPanel);

    resetBoxes();
  }

  public void resetBoxes()
  {
    FieldTemplate template;
    JCheckBox newCheck;
    Vector fields;

    /* -- */

    checkboxes.clear();
    customPanel.removeAll();
    builtInPanel.removeAll();

    fields = parent.gc.getTemplateVector(parent.selectedBase.getTypeID());

    for (int i=0; fields != null && (i < fields.size()); i++) 
      {	
	template = (FieldTemplate) fields.elementAt(i);

	newCheck = new JCheckBox(template.getName());
	checkboxes.put(template.getName(), newCheck);

	if (template.isBuiltIn())
	  {
	    builtInPanel.add(newCheck);
	    newCheck.setSelected(false);
	  }
	else
	  {
	    customPanel.add(newCheck);
	    newCheck.setSelected(true);
	  }
      }
  }

  /**
   *
   * This method returns a Vector of Strings indicating the fields that
   * the user has requested be returned by the query engine, or null
   * if the user has left the field checkboxes at the default settings.
   *
   */

  public Vector getReturnFields()
  {
    Vector fieldsToReturn = new Vector();
    Enumeration enum = checkboxes.keys();
    boolean allFields = true;

    /* -- */

    while (enum.hasMoreElements())
      {
	String key = (String) enum.nextElement();
	JCheckBox checkbox = (JCheckBox) checkboxes.get(key);
	
	if (checkbox.isSelected())
	  {
	    fieldsToReturn.addElement(key);
	  }
	else
	  {
	    allFields = false;
	  }
      }

    // if allFields is still true, let the Query mechanism
    // return default fields.
    
    if (allFields)
      {
	fieldsToReturn = null;
      }

    return fieldsToReturn;
  }
}
