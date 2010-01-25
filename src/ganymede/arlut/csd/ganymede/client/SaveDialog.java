/*

   SaveDialog.java

   Dialog for saving or mailing a table from dialog.
   
   Created: ??

   Module By: Mike Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
   The University of Texas at Austin

   Contact information

   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.client;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import arlut.csd.JDialog.StringDialog;
import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      SaveDialog

------------------------------------------------------------------------------*/

/**
 * <p>Client dialog for saving or mailing the results of a query operation.</p>
 */

public class SaveDialog extends JDialog implements ActionListener {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.SaveDialog");

  private static final boolean debug = false;

  boolean
    addedFormatChoice = false,
    returnValue = false;
  
  GridBagLayout gbl = new GridBagLayout();
  GridBagConstraints gbc = new GridBagConstraints();

  JButton 
    ok,
    cancel;

  JTextField
    subject = new JTextField(20),
    recipients = new JTextField(20);

  JComboBox
    formats = null;

  // This is the panel that holds everything, layed out by gbl

  JPanel 
    panel;

  formatButtonPanel
    formatPanel;

  Image
    saveImage = arlut.csd.Util.PackageResources.getImageResource(this, "SaveDialog.gif", getClass());

  /* -- */

  /**
   * Main Constructor.
   *
   * @param owner Parent frame
   *
   * @param forMail If true, the dialog will show the recipients field
   * and the ok button will say "mail".  Otherwise, it says "save".
   */

  SaveDialog(Frame owner, boolean forMail)
  {
    // "Email Query Report"
    // "Save Query Report"

    super(owner, forMail ? ts.l("init.mail_title") : ts.l("init.save_title"), true);

    panel = new JPanel(gbl);

    gbc.insets = new Insets(6,6,6,6);
    
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.NONE;

    if (forMail)
      {
	gbc.anchor = GridBagConstraints.WEST;

	gbc.gridy = 0;
	gbc.gridx = 0;
	gbc.gridheight = 2;
	JLabel image = new JLabel(new ImageIcon(saveImage));
	gbl.setConstraints(image, gbc);
	panel.add(image);

	gbc.gridheight = 1;
	gbc.gridx = 1;
	gbc.gridy = 0;
	gbc.fill = GridBagConstraints.NONE;
	// "To:"
	JLabel rec = new JLabel(ts.l("init.to_label"));
	gbl.setConstraints(rec, gbc);
	panel.add(rec);

	gbc.gridx = 2;
	gbc.fill = GridBagConstraints.HORIZONTAL;
	gbc.weightx = 1;
	gbl.setConstraints(recipients, gbc);
	panel.add(recipients);

	gbc.gridy = 1;
	gbc.gridx = 1;
	gbc.weightx = 0;
	gbc.fill = GridBagConstraints.NONE;
	// "Subject:"
	JLabel sub = new JLabel(ts.l("init.subject_label"));
	gbl.setConstraints(sub, gbc);
	panel.add(sub);

	gbc.gridx = 2;
	gbc.weightx = 1;
	gbc.fill = GridBagConstraints.HORIZONTAL;
	// "Query Report"
	subject.setText(ts.l("init.default_subject"));
	gbl.setConstraints(subject, gbc);
	panel.add(subject);
      }

    // Row 3 is for the format choices

    JSeparator sep = new JSeparator();
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridy = 4;
    gbc.gridx = 0;
    gbl.setConstraints(sep, gbc);
    panel.add(sep);

    JPanel buttonPanel = new JPanel();
    // "Mail"
    // "Save"
    ok = new JButton(forMail ? ts.l("init.mail_button") : ts.l("init.save_button"));
    ok.addActionListener(this);

    cancel = new JButton(StringDialog.getDefaultCancel());
    cancel.addActionListener(this);

    buttonPanel.add(ok);
    buttonPanel.add(cancel);

    gbc.gridx = 0;
    gbc.gridy = 5;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(buttonPanel, gbc);
    panel.add(buttonPanel);

    setContentPane(panel);

    pack();

    this.setLocationRelativeTo(owner);
  }

  /**
   * <p>Show the dialog.</p>
   *
   * <p>Use this instead of calling setVisible(true) yourself.  You need to get the boolean
   * return from this method, in order to know if the user pressed "Ok" or "Cancel".
   *
   * @return True if user pressed "Ok".
   */

  public boolean showDialog()
  {
    setVisible(true);

    if (debug)
      {
	System.err.println("Returning " + returnValue);
      }

    return returnValue;
  }

  /**
   * String of recipients for the mail.
   *
   * This is not formatted in any way, so you get whatever the user typed in.
   */

  public String getRecipients()
  {
    return recipients.getText();
  }

  /**
   * Returns the text for the subject of the mail.
   *
   */
  public String getSubject()
  {
    return subject.getText();
  }

  /**
   * Set the choices for the format choices.
   *
   * @param choices Vector of Strings of the different choices.
   *
   * <P>Usually we send in a Vector of strings like "HTML", "Plain text", etc.</p>
   */
  public void setFormatChoices(Vector choices)
  {
    //formats = new JComboBox(choices);

    if (!addedFormatChoice)
      {
	addFormatChoiceButtons(choices);
      }
  }

  /**
   * Returns the choice of format.
   *
   * This will be one of the Strings in setFormatChoicse(), unless
   * something went horribly awry.
   */
  public String getFormat()
  {
    //return (String)formats.getSelectedItem();
    return formatPanel.getSelectedFormat();
  }

  // This one adds the format choice.
  private void addFormatChoice()
  {
    gbc.gridy = 3;
    gbc.gridx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;

    // "Format:"
    JLabel l = new JLabel(ts.l("addFormatChoice.format_label"));
    gbl.setConstraints(l, gbc);
    panel.add(l);

    gbc.gridx = 1;

    gbl.setConstraints(formats, gbc);
    panel.add(formats);

    addedFormatChoice = true;
    pack();
  }

  // This really just adds in a new formatButtonPanel
  private void addFormatChoiceButtons(Vector choices)
  {
    gbc.gridy = 3;
    gbc.gridx = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = 3;

    formatPanel  = new formatButtonPanel(choices);
    gbl.setConstraints(formatPanel, gbc);
    panel.add(formatPanel);

    addedFormatChoice = true;
    pack();
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == ok)
      {
	if (debug)
	  {
	    System.err.println("ok");
	  }

	returnValue = true;
	setVisible(false);
      }
    else if (e.getSource() == cancel)
      {
	if (debug)
	  {
	    System.err.println("cancel");
	  }

	returnValue = false;
	setVisible(false);
      }
  }
}

/**
 * <p>Client panel that holds the choice of formats for
 * {@link arlut.csd.ganymede.client.SaveDialog SaveDialog}.</p>
 */

class formatButtonPanel extends JPanel {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.formatButtonPanel");

  Hashtable modelToLabel = new Hashtable();

  ButtonGroup
    group = new ButtonGroup();

  GridBagLayout
    layout = new GridBagLayout();

  GridBagConstraints
    constraints = new GridBagConstraints();

  formatButtonPanel(Vector choices)
  {
    setLayout(layout);

    String s;

    // "Format"
    setBorder(BorderFactory.createTitledBorder(ts.l("init.title")));
    constraints.gridx = 0;
    constraints.anchor = GridBagConstraints.WEST;

    for (int i = 0; i < choices.size(); i++)
      {
	s = (String)choices.elementAt(i);

	JRadioButton b = new JRadioButton(s);

	if (i == 0)
	  {
	    b.setSelected(true);
	  }

	modelToLabel.put(b.getModel(), s);
	group.add(b);

	constraints.gridy = i;
	layout.setConstraints(b, constraints);
	add(b);
      }
  }

  public String getSelectedFormat()
  {
    return (String)modelToLabel.get(group.getSelection());
  }
}

