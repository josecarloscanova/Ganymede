/*

   serverAdminProxy.java

   serverAdminProxy is a server-side Admin object which buffers console
   status updates, coalescing update events as needed to maximize server
   efficiency.  Each serverAdminProxy object has a background thread which
   communicates with an admin console in the background, allowing the
   Ganymede server's operations to be asynchronous with respect to admin
   console updates.
   
   Created: 31 January 2000
   Release: $Name:  $
   Version: $Revision: 1.7 $
   Last Mod Date: $Date: 2000/02/02 01:06:21 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

import java.util.*;
import java.io.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;

/*------------------------------------------------------------------------------
                                                                           class
                                                                serverAdminProxy

------------------------------------------------------------------------------*/

/**
 * <p>serverAdminProxy is a server-side Admin object which buffers console
 * status updates, coalescing update events as needed to maximize server
 * efficiency.  Each serverAdminProxy object has a background thread which
 * communicates with an admin console in the background, allowing the
 * Ganymede server's operations to be asynchronous with respect to admin
 * console updates.</p>
 *
 * @see arlut.csd.ganymede.adminEvent
 *
 * @version $Revision: 1.7 $ $Date: 2000/02/02 01:06:21 $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class serverAdminProxy implements Admin, Runnable {

  /**
   * <p>Our background communications thread, which is responsible for
   * calling the admin console via RMI.</p>
   */

  private Thread commThread;

  /**
   * <p>Our queue of {@link arlut.csd.ganymede.adminEvent adminEvent} objects.</p>
   */

  private Vector eventBuffer;

  /**
   * <p>How many events we'll queue up before deciding that the
   * admin console isn't responding.</p>
   */

  private int maxBufferSize = 15; // only 10 kinds of things, most of which we coalesce/replace

  /**
   * <p>Our remote reference to the admin console client</p>
   */

  private Admin remoteConsole;

  /**
   * <p>If true, we have been told to shut down, and our
   * background commThread will exit as soon as it can
   * clear its event queue.</p>.</p>
   */

  private boolean done = false;

  /**
   * <p>If our commThread receives a remote exception when communicating
   * with an admin console, this field will become non-null, and no more
   * communications will be attempted with that console.</p>
   */

  private String errorCondition;

  /* -- */

  public serverAdminProxy(Admin remoteConsole)
  {
    this.remoteConsole = remoteConsole;
    eventBuffer = new Vector();

    commThread = new Thread(this, "admin console proxy");
    commThread.start();
  }

  /**
   * <p>Returns true if we are successfully in communications with the
   * attached admin console.</p>
   */

  public boolean isAlive()
  {
    return !done;
  }

  /**
   * <p>This method shuts down the background thread.</p>
   */

  public void shutdown()
  {
    synchronized (eventBuffer)
      {
	this.done = true;
	eventBuffer.notifyAll(); // let the commThread drain and exit
      }
  }

  /**
   * <p>This method is called by the Ganymede server to obtain the username
   * given when the admin console was started.</p>
   */

  public String getName() throws RemoteException
  {
    return remoteConsole.getName();
  }

  /**
   * <p>This method is called by the Ganymede server to obtain the password
   * given when the admin console was started.</p>
   */

  public String getPassword() throws RemoteException
  {
    return remoteConsole.getPassword();
  }

  /**
   *
   * Callback: The server can tell us to disconnect if the server is 
   * going down.
   *
   */

  public void forceDisconnect(String reason) throws RemoteException
  {
    try
      {
	remoteConsole.forceDisconnect(reason);
      }
    catch (RemoteException ex)
      {
      }

    shutdown();
  }

  /**
   * <p>This method is called by the Ganymede server to set the server start
   * date in the admin console.</p>
   */

  public void setServerStart(Date date) throws RemoteException
  {
    adminEvent newEvent = new adminEvent(adminEvent.SETSERVERSTART, date);

    /* -- */

    replaceEvent(newEvent);
  }

  /**
   * <p>This method is called by the Ganymede server to set the last dump
   * date in the admin console.</p>
   */

  public void setLastDumpTime(Date date) throws RemoteException
  {
    adminEvent newEvent = new adminEvent(adminEvent.SETLASTDUMPTIME, date);

    /* -- */

    replaceEvent(newEvent);
  }

  /**
   * <p>This method is called by the Ganymede server to set the number of
   * transactions in the server's journal in the admin console.</p>
   */

  public void setTransactionsInJournal(int trans) throws RemoteException
  {
    adminEvent newEvent = new adminEvent(adminEvent.SETTRANSACTIONS, new Integer(trans));

    /* -- */

    replaceEvent(newEvent);
  }

  /**
   * <p>This method is called by the Ganymede server to set the number of
   * objects checked out in the admin console.</p>
   */

  public void setObjectsCheckedOut(int objs) throws RemoteException
  {
    adminEvent newEvent = new adminEvent(adminEvent.SETOBJSCHECKOUT, new Integer(objs));

    /* -- */

    replaceEvent(newEvent);
  }

  /**
   * <p>This method is called by the Ganymede server to set the number of
   * locks held in the admin console.</p>
   */

  public void setLocksHeld(int locks) throws RemoteException
  {
    adminEvent newEvent = new adminEvent(adminEvent.SETLOCKSHELD, new Integer(locks));

    /* -- */

    replaceEvent(newEvent);
  }

  /**
   * <p>This method is called by the Ganymede server to update the
   * admin console's server state display.</p>
   */

  public void changeState(String state) throws RemoteException
  {
    adminEvent newEvent = new adminEvent(adminEvent.CHANGESTATE, state);

    /* -- */

    replaceEvent(newEvent);
  }

  /**
   * <p>This method is called by the Ganymede server to add to the
   * admin console's log display.</p>
   */

  public void changeStatus(String status) throws RemoteException
  {
    this.changeStatus(status, false);
  }

  /**
   * <p>This method is called by the Ganymede server to add to the
   * admin console's log display.</p>
   */

  public void changeStatus(String status, boolean timeLabelled) throws RemoteException
  {
    adminEvent event;
    String stampedLine;

    /* -- */

    if (!timeLabelled)
      {
	stampedLine = new Date() + " " + status + "\n";
      }
    else
      {
	stampedLine = status;
      }

    synchronized (eventBuffer)
      {
	if (done)
	  {
	    return;
	  }

	for (int i = 0; i < eventBuffer.size(); i++)
	  {
	    event = (adminEvent) eventBuffer.elementAt(i);
	    
	    if (event.method == event.CHANGESTATUS)
	      {
		// coalesce this append to the log message

		String oldText = (String) event.param;
		String newText = oldText + stampedLine;
		event.param = newText;

		return;
	      }
	  }

	// if we didn't find an event to append to, go ahead and add a
	// new CHANGESTATUS log update event

	addEvent(new adminEvent(adminEvent.CHANGESTATUS, stampedLine));
      }
  }

  /**
   * <p>This method is called by the Ganymede server to update the
   * number of admin consoles attached to the server.</p>
   */

  public void changeAdmins(String adminStatus) throws RemoteException
  {
    adminEvent newEvent = new adminEvent(adminEvent.CHANGEADMINS, adminStatus);

    /* -- */

    replaceEvent(newEvent);
  }

  /**
   * <p>This method is called by the Ganymede server to update the
   * admin console's connected user table.</p>
   *
   * @param entries a Vector of {@link arlut.csd.ganymede.AdminEntry AdminEntry}
   * login description objects.
   */

  public void changeUsers(Vector entries) throws RemoteException
  {
    adminEvent newEvent = new adminEvent(adminEvent.CHANGEUSERS, entries);

    /* -- */

    replaceEvent(newEvent);
  }

  /**
   * <p>This method is called by the Ganymede server to update the
   * admin console's task table.</p>
   *
   * @param tasks a Vector of {@link arlut.csd.ganymede.scheduleHandle scheduleHandle}
   * objects describing the tasks registered in the Ganymede server.
   */

  public void changeTasks(Vector tasks) throws RemoteException
  {
    adminEvent newEvent = new adminEvent(adminEvent.CHANGETASKS, tasks);

    /* -- */

    replaceEvent(newEvent);
  }

  /**
   * <p>The serverAdminProxy's background thread's run() method.  This method
   * runs in the admin console proxy thread to read events from this console's
   * serverAdminProxy eventBuffer and send them down to the admin console.</p>
   */

  public void run()
  {
    adminEvent event;

    /* -- */

    while (!done || (eventBuffer.size() != 0))
      {
	synchronized (eventBuffer)
	  {
	    if (eventBuffer.size() == 0)
	      {
		try
		  {
		    eventBuffer.wait();
		  }
		catch (InterruptedException ex)
		  {
		  }
		
		continue;
	      }

	    event = (adminEvent) eventBuffer.elementAt(0);
	    eventBuffer.removeElementAt(0);
	  }

	try
	  {
	    event.dispatch(remoteConsole);

	    errorCondition = null; // we won't execute this if a remote exception is thrown
	  }
	catch (RemoteException ex)
	  {
	    if (errorCondition != null)
	      {
		done = true;	// two remote exceptions in a row, prevent any further events
		return;		// exit the commThread
	      }
	    else
	      {
		errorCondition = Ganymede.stackTrace(ex);
		System.err.println(errorCondition);
	      }
	  }
      }
  }

  /**
   * <p>private helper method in serverAdminProxy, used to add an event to
   * the proxy's event buffer.  If the buffer already contains an event
   * of the same type as newEvent, both events will be sent to the
   * admin console, in chronological order.</p>
   */

  private void addEvent(adminEvent newEvent) throws RemoteException
  {
    synchronized (eventBuffer)
      {
	if (done)
	  {
	    throw new RemoteException("serverAdminProxy: console disconnected");
	  }

	if (eventBuffer.size() >= maxBufferSize)
	  {
	    throwOverflow();
	  }

	eventBuffer.addElement(newEvent);
	
	eventBuffer.notify();
      }
  }

  /**
   * <p>private helper method in serverAdminProxy, used to add an event to
   * the proxy's event buffer.  If the buffer already contains an event
   * of the same type as newEvent, the old event will be replaced with
   * the new, and the admin console will never be notified of the old
   * event's contents.</p>
   */

  private void replaceEvent(adminEvent newEvent) throws RemoteException
  {
    adminEvent oldEvent;

    /* -- */

    synchronized (eventBuffer)
      {
	if (done)
	  {
	    throw new RemoteException("serverAdminProxy: console disconnected");
	  }

	for (int i = 0; i < eventBuffer.size(); i++)
	  {
	    oldEvent = (adminEvent) eventBuffer.elementAt(i);
	    
	    if (oldEvent.method == newEvent.method)
	      {
		eventBuffer.setElementAt(newEvent, i);
		return;
	      }
	  }

	if (eventBuffer.size() >= maxBufferSize)
	  {
	    throwOverflow();
	  }
	
	eventBuffer.addElement(newEvent);
	eventBuffer.notify();
      }
  }

  /**
   * <p>This method throws a remoteException which describes the state
   * of the event buffer.  This is called from addEvent and
   * replaceEvent.  The {@link arlut.csd.ganymede.GanymedeAdmin
   * GanymedeAdmin} code that calls the {@link
   * arlut.csd.ganymede.Admin Admin} proxy methods in serverAdminProxy
   * take repeated remote exceptions as an indication that they need
   * to detach the admin console, which is why we use RemoteException.</p>
   */

  private void throwOverflow() throws RemoteException
  {
    StringBuffer buffer = new StringBuffer();
    
    for (int i = 0; i < eventBuffer.size(); i++)
      {
	buffer.append(i);
	buffer.append(": ");
	buffer.append(eventBuffer.elementAt(i));
	buffer.append("\n");
      }
    
    throw new RemoteException("serverAdminProxy buffer overflow:" + buffer.toString());
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      adminEvent

------------------------------------------------------------------------------*/

/**
 * <p>The adminEvent class is used on the Ganymede server by the
 * {@link arlut.csd.ganymede.serverAdminProxy serverAdminProxy} class, which
 * uses it to queue up method calls to a remote admin console.</p>
 *
 * <p>adminEvent objects are never sent to a remote admin console. rather,
 * they are queued up in the Ganymede server by the serverAdminProxy class so
 * that a background communications thread can read adminEvents off of a queue
 * and make the appropriate RMI calls to an attached admin console.</p>
 */

class adminEvent {

  static final byte FIRST = 0;
  static final byte SETSERVERSTART = 0;
  static final byte SETLASTDUMPTIME = 1;
  static final byte SETTRANSACTIONS = 2;
  static final byte SETOBJSCHECKOUT = 3;
  static final byte SETLOCKSHELD = 4;
  static final byte CHANGESTATE = 5;
  static final byte CHANGESTATUS = 6;
  static final byte CHANGEADMINS = 7;
  static final byte CHANGEUSERS = 8;
  static final byte CHANGETASKS = 9;
  static final byte LAST = 9;

  /* --- */

  /**
   * <p>Identifies what RMI call is going to need to be made to the
   * remote admin console.</p>
   */

  byte method;

  /**
   * <p>Generic RMI call parameter to be sent to the remote admin console.  If
   * an RMI call normally takes more than one parameter, param should be a Vector
   * which contains the parameters internally.</p>
   */

  Object param;

  /* -- */

  public adminEvent(byte method, Object param)
  {
    if (method < FIRST || method > LAST)
      {
	throw new IllegalArgumentException("bad method code: " + method);
      }

    this.method = method;
    this.param = param;
  }

  public String toString()
  {
    StringBuffer result = new StringBuffer();

    switch (method)
      {
      case SETSERVERSTART: 
	result.append("setServerStart");
	break;
	
      case SETLASTDUMPTIME:
	result.append("setLastDumpTime");
	break;

      case SETTRANSACTIONS:
	result.append("setTransactionsInJournal");
	break;

      case SETOBJSCHECKOUT:
	result.append("setObjectsCheckedOut");
	break;

      case SETLOCKSHELD:
	result.append("setLocksHeld");
	break;

      case CHANGESTATE:
	result.append("changeState");
	break;

      case CHANGESTATUS:
	result.append("changeStatus");
	break;

      case CHANGEADMINS:
	result.append("changeAdmins");
	break;

      case CHANGEUSERS:
	result.append("changeUsers");
	break;

      case CHANGETASKS:
	result.append("changeTasks");
	break;
	
      default:
	result.append("??");
      }

    result.append("(");
    result.append(param);
    result.append(")");

    return result.toString();
  }

  public void dispatch(Admin remoteConsole) throws RemoteException
  {
    switch (method)
      {
      case SETSERVERSTART:
	remoteConsole.setServerStart((Date) param);
	break;

      case SETLASTDUMPTIME:
	remoteConsole.setLastDumpTime((Date) param);
	break;

      case SETTRANSACTIONS:
	remoteConsole.setTransactionsInJournal(((Integer) param).intValue());
	break;

      case SETOBJSCHECKOUT:
	remoteConsole.setObjectsCheckedOut(((Integer) param).intValue());
	break;

      case SETLOCKSHELD:
	remoteConsole.setLocksHeld(((Integer) param).intValue());
	break;

      case CHANGESTATE:
	remoteConsole.changeState((String) param);
	break;

      case CHANGESTATUS:
	remoteConsole.changeStatus((String) param);
	break;

      case CHANGEADMINS:
	remoteConsole.changeAdmins((String) param);
	break;

      case CHANGEUSERS:
	remoteConsole.changeUsers((Vector) param);
	break;

      case CHANGETASKS:
	remoteConsole.changeTasks((Vector) param);
	break;
      }
  }
}
