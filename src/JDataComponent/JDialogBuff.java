/*

   JDialogBuff.java

   Serializable resource class for use with StringDialog.java
   
   Created: 27 January 1998
   Version: $Revision: 1.17 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JDialog;

import java.util.*;
import java.awt.Frame;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      JDialogBuff

------------------------------------------------------------------------------*/

/**
 *
 * This class is a serializable description of a dialog object that a server
 * is asking a client to present.  
 *
 * If you don't need to send a dialog definition object across an RMI
 * link, just construct a DialogRsrc directly.  (Note.. this is
 * semi-vestigal code, now, since we just use normal serialization to
 * have JDialogBuff transport its parameters, which could be done
 * directly with DialogRsrc just as well.  There are some things in
 * the DialogRsrc class, like the image cache, that we may not want
 * to mess with.  In any case, all of the Ganymede code expects JDialogBuff,
 * so it's going to stay for now.)
 *
 */

public class JDialogBuff implements java.io.Serializable {

  static final boolean debug = false;

  // ---
  
  String title;
  String text;
  String okText;
  String cancelText;
  String imageName;
  Vector resources;

  /* -- */

  // client side code

  public DialogRsrc extractDialogRsrc(Frame frame)
  { 
    DialogRsrc rsrc;

    /* -- */

    rsrc = new DialogRsrc(frame, title, text, okText, cancelText, imageName);

    rsrc.objects = resources;

    return rsrc;
  }

  // server-side constructors

  /**
   * Constructor for JDialogBuff
   *
   * @param Title String for title of Dialog box.
   * @param Text String for message at top of dialog box.
   *
   */

  public JDialogBuff(String Title, String Text)
  {
    this(Title, Text, "Ok", "Cancel", null);
  }

  /** 
   * Constructor with special "Ok" and "Cancel" strings
   *
   * @param Title String for title of Dialog box.
   * @param Text String for message at top of dialog box.
   * @param OK String for Ok button 
   * @param Cancel String for Cancel button
   */

  public JDialogBuff(String Title, String Text, String OK, String Cancel)
  {
    this(Title, Text, OK, Cancel, null);
  }

  /** 
   * Constructor with special "Ok" and "Cancel" strings
   *
   * @param Title String for title of Dialog box.
   * @param Text String for message at top of dialog box.
   * @param OK String for Ok button 
   * @param Cancel String for Cancel button
   * @param image Filename of image to display next to text
   */

  public JDialogBuff(String Title, String Text, String OK, String Cancel, String image)
  {
    this.title = Title;
    this.text = Text;
    this.okText = OK;
    this.cancelText = Cancel;
    this.imageName = image;
  }

  /**
   *
   * Adds a labeled text field
   *
   * @param string String to use as the label
   */

  public void addString(String string)
  {
    addString(string, (String)null);
  }

  /**
   *
   * Adds a labeled text field
   *
   * @param string String to use as the label
   * @param value Initial value for string
   */

  public void addString(String string, String value)
  {
    resources.addElement(new stringThing(string, value, false));
  }

  /**
   *
   * Adds a labeled multi-line text field
   *
   * @param string String to use as the label
   * @param value Initial value for string
   */

  public void addMultiString(String string, String value)
  {
    resources.addElement(new stringThing(string, value, true));
  }

  /**
   * 
   * Adds a labeled check box field
   *
   * @param string String to use as the label
   */
  
  public void addBoolean(String string)
  {
    addBoolean(string, false);
  }

  /**
   * 
   * Adds a labeled check box field
   *
   * @param string String to use as the label
   * @param value Initial value
   */
  
  public void addBoolean(String string, boolean value)
  {
    resources.addElement(new booleanThing(string, value));
  }

  /**
   * 
   * Adds a labeled date field
   *
   * @param label String to use as the label
   * @param currentDate Date to initialize the date field to
   * @param maxDate Latest date that the user may choose for this field.
   */
  
  public void addDate(String label, Date currentDate, Date maxDate)
  {
    resources.addElement(new dateThing(label, currentDate, maxDate));
  }

  /**
   *
   * Adds a choice field to the dialog
   *
   * @param label String to use as the label
   * @param choices Vector of Strings to add to the choice 
   */
  
  public void addChoice(String label, Vector choices)
  {
    addChoice(label, choices, null);
  }

  /**
   *
   * Adds a choice field to the dialog
   *
   * @param label String to use as the label
   * @param choices Vector of Strings to add to the choice 
   */
  
  public void addChoice(String label, Vector choices, String selectedValue)
  {
    resources.addElement(new choiceThing(label, choices, selectedValue));
  }

  /**
   *
   * Adds a text-hidden password string field to the dialog
   *
   * @param label String to use as label
   */

  public void addPassword(String label)
  {
    addPassword(label, false);
  }

  /**
   *
   * Adds a text-hidden password string field to the dialog
   *
   * @param label String to use as label
   * @param value Initial value
   */

  public void addPassword(String label, boolean isNew)
  {
    resources.addElement(new passwordThing(label, isNew));
  }

  /**
   *
   * This is a convenience function for the server.
   *
   */

  public String getText()
  {
    return text;
  }

}
