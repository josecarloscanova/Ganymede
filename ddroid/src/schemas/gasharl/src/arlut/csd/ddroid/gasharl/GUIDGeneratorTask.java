/*

   GUIDGeneratorTask.java

   This task is a simple one-shot intended to create GUID's in users
   that do not have them.
   
   Created: 25 September 2002
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996-2004
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ddroid.gasharl;

import arlut.csd.ddroid.common.*;
import arlut.csd.ddroid.rmi.*;
import arlut.csd.ddroid.server.*;


import org.doomdark.uuid.*;

import java.util.*;
import java.text.*;
import java.io.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                               PasswordAgingTask

------------------------------------------------------------------------------*/

/**
 *
 * This task is a simple one-shot intended to create GUID's in users
 * that do not have them.
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 */

public class GUIDGeneratorTask implements Runnable {

  static final boolean debug = true;

  /* -- */

  GanymedeSession mySession = null;
  Thread currentThread = null;

  /**
   *
   * Just Do It (tm)
   *
   * @see java.lang.Runnable
   *
   */

  public void run()
  {
    boolean transactionOpen = false;

    /* -- */

    currentThread = java.lang.Thread.currentThread();

    Ganymede.debug("GUIDGenerator Task: Starting");

    String error = GanymedeServer.checkEnabled();
	
    if (error != null)
      {
	Ganymede.debug("Deferring GUIDGenerator task - semaphore disabled: " + error);
	return;
      }

    try
      {
	try
	  {
	    mySession = new GanymedeSession("GUIDGeneratorTask");
	  }
	catch (RemoteException ex)
	  {
	    Ganymede.debug("GUIDGenerator Task: Couldn't establish session");
	    return;
	  }

	// we don't want interactive handholding

	mySession.enableWizards(false);

	// and we want forced required fields oversight..

	mySession.enableOversight(true);
	
	ReturnVal retVal = mySession.openTransaction("GUIDGenerator conversion task");

	if (retVal != null && !retVal.didSucceed())
	  {
	    Ganymede.debug("GUIDGenerator Task: Couldn't open transaction");
	    return;
	  }

	transactionOpen = true;
	
	// do the stuff

	if (!createGUIDs())
	  {
	    Ganymede.debug("ConvertNetworks bailed");

	    mySession.abortTransaction();
	    return;
	  }

	retVal = mySession.commitTransaction();

	if (retVal != null && !retVal.didSucceed())
	  {
	    // if doNormalProcessing is true, the
	    // transaction was not cleared, but was
	    // left open for a re-try.  Abort it.

	    if (retVal.doNormalProcessing)
	      {
		Ganymede.debug("GUIDGenerator Task: couldn't fully commit, trying to abort.");

		mySession.abortTransaction();
	      }

	    Ganymede.debug("GUIDGenerator Task: Couldn't successfully commit transaction");
	  }
	else
	  {
	    Ganymede.debug("GUIDGenerator Task: Transaction committed");
	  }

	transactionOpen = false;
      }
    catch (NotLoggedInException ex)
      {
      }
    catch (Throwable ex)
      {
	Ganymede.debug("Caught " + ex.getMessage());
      }
    finally
      {
	if (transactionOpen)
	  {
	    Ganymede.debug("GUIDGenerator Task: Forced to terminate early, aborting transaction");
	  }

	mySession.logout();
      }
  }

  public boolean createGUIDs() throws NotLoggedInException
  {
    Vector users = mySession.getObjects(SchemaConstants.UserBase);
    UUIDGenerator gen = UUIDGenerator.getInstance();
    EthernetAddress myAddress = new EthernetAddress("8:0:20:fd:6b:7");

    /* -- */
    
    for (int i = 0; i < users.size(); i++)
      {
	DBObject user = (DBObject) users.elementAt(i);
	Invid invid = user.getInvid();

	ReturnVal retVal = mySession.edit_db_object(invid);

	if (retVal != null && retVal.didSucceed())
	  {
	    DBEditObject eo = (DBEditObject) retVal.getObject();

	    StringDBField guidField = (StringDBField) eo.getField(userSchema.GUID);

	    if (!guidField.isDefined())
	      {
		org.doomdark.uuid.UUID uuid = gen.generateTimeBasedUUID(myAddress);
		String uuidString = uuid.toString();

		retVal = guidField.setValueLocal(uuidString);

		if (retVal != null && !retVal.didSucceed())
		  {
		    return false;
		  }
	      }
	  }
      }

    return true;
  }
}

