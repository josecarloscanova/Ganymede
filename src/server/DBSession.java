 /*

   DBSession.java

   The GANYMEDE object storage system.

   Created: 26 August 1996
   Version: $Revision: 1.34 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;

import arlut.csd.JDialog.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       DBSession

------------------------------------------------------------------------------*/

/**
 * 
 * <p>DBSession is the DBStore session class.  All normal database
 * interactions are performed through a DBSession object.  The
 * DBSession object provides a handle for monitoring the operations on
 * the database.. who holds what lock, what actions are performed
 * during a lock / transaction / session.</p>
 * 
 */

final public class DBSession {

  static boolean debug = true;

  public final static void setDebug(boolean val)
  {
    debug = val;
  }

  GanymedeSession GSession;
  DBStore store;

  Vector lockVect = new Vector();

  DBEditSet editSet;
  String lastError;
  String id = null;
  Object key;

  /* -- */

  /**   
   * <p>Constructor for DBSession.</p>
   * 
   * <p>The key passed to the DBSession constructor is intended to be used
   * to allow code to save an identifier in the DBSession.. this might be
   * a thread object or a higher level session object or whatever.  Eventually
   * I expect I'll replace this generic key with some sort of reporting 
   * Interface object.</p>
   *
   * <p>This constructor is intended to be called by the DBStore login() method.</p>
   *
   * @param store The DBStore database this session belongs to.
   * @param adminObject The admin object for the user associated with this session, or null if none (only for system init)
   * @param key An identifying key with meaning to whatever code is using arlut.csd.ganymede
   *
   */
   
  DBSession(DBStore store, GanymedeSession GSession, Object key)
  {
    this.store = store;
    this.key = key;
    this.GSession = GSession;

    editSet = null;
    lastError = null;
  }

  /**
   *
   * Close out this DBSession, aborting any open transaction, and releasing
   * held read locks.
   *
   */

  public synchronized void logout()
  {
    releaseAllReadLocks();

    if (editSet != null)
      {
	abortTransaction();
      }

    this.store = null;
  }

  /**
   *
   * This method is provided so that custom DBEditObject subclasses
   * can get access to methods on our DBStore.
   *
   */

  public DBStore getStore()
  {
    return store;
  }


  /**
   * Create a new object in the database.<br><br>
   *
   * This method creates a slot in the object base of the
   * proper object type.  The created object is associated
   * with the current transaction.  When the transaction 
   * is committed, the created object will inserted into
   * the database, and will become visible to other
   * sessions.<br><br>
   *
   * The created object will be given an object id.
   * The DBEditObject can be queried to determine its
   * invid.
   *
   * This method will return null if the object could
   * not be constructed and initialized for some reason.
   *
   * @param object_type Type of the object to be created
   * @param chosenSlot Invid to create the new object with.
   * normally only used in internal Ganymede code in conjunction with
   * the addition of new kinds of built-in objects during development
   *
   * @see arlut.csd.ganymede.DBStore
   *
   */

  public synchronized DBEditObject createDBObject(short object_type, Invid chosenSlot)
  {
    DBObjectBase base;
    DBEditObject e_object;
    DateDBField df;
    StringDBField sf;
    Date modDate = new Date();
    String result;

    /* -- */

    if (editSet == null)
      {
	throw new RuntimeException("createDBObject called outside of a transaction");
      }

    base = (DBObjectBase) store.objectBases.get(new Short(object_type));

    e_object = base.createNewObject(editSet, chosenSlot);

    if (!base.isEmbedded())
      {
	// set creator info to something non-null

	df = (DateDBField) e_object.getField(SchemaConstants.CreationDateField);
	df.setValue(modDate);

	sf = (StringDBField) e_object.getField(SchemaConstants.CreatorField);

	result = getID();

	if (editSet.description != null)
	  {
	    result += ": " + editSet.description;
	  }

	sf.setValue(result);

	// set modifier info to something non-null

	df = (DateDBField) e_object.getField(SchemaConstants.ModificationDateField);
	df.setValue(modDate);

	sf = (StringDBField) e_object.getField(SchemaConstants.ModifierField);

	result = getID();

	if (editSet.description != null)
	  {
	    result += ": " + editSet.description;
	  }

	sf.setValue(result);
      }

    return e_object;
  }

  /**
   * Create a new object in the database.<br><br>
   *
   * This method creates a slot in the object base of the
   * proper object type.  The created object is associated
   * with the current transaction.  When the transaction 
   * is committed, the created object will inserted into
   * the database, and will become visible to other
   * sessions.<br><br>
   *
   * The created object will be given an object id.
   * The DBEditObject can be queried to determine its
   * invid.
   *
   * This method will return null if the object could
   * not be constructed and initialized for some reason.
   *
   * @param object_type Type of the object to be created
   *
   * @see arlut.csd.ganymede.DBStore
   *
   */

  public DBEditObject createDBObject(short object_type)
  {
    return createDBObject(object_type, null);
  }

  /**
   *
   * Pull an object out of the database for editing.<br><br>
   *
   * This method is used to check an object out of the database for editing.
   * Only one session can have a particular object checked out for editing at
   * a time.<br><br>
   *
   * The session has to have a transaction opened before it can pull
   * an object out for editing.
   *
   * @param invid The invariant id of the object to be modified.
   *
   * @see arlut.csd.ganymede.DBObjectBase 
   */

  public DBEditObject editDBObject(Invid invid)
  {
    return editDBObject(invid.getType(), invid.getNum());
  }

  /**
   *
   * Pull an object out of the database for editing.<br><br>
   *
   * This method is used to check an object out of the database for editing.
   * Only one session can have a particular object checked out for editing at
   * a time.<br><br>
   *
   * The session has to have a transaction opened before it can pull a
   * new object out for editing.  If the object specified by <baseID,
   * objectID> is part of the current transaction, the transactional
   * copy will be returned, and no readLock is strictly necessary in
   * that case.
   *
   * @param baseID The short id number of the DBObjectBase containing the object to
   *               be edited.
   *
   * @param objectID The int id number of the object to be edited within the specified
   *                 object base.
   *
   * @see arlut.csd.ganymede.DBObjectBase
   * 
   */

  public synchronized DBEditObject editDBObject(short baseID, int objectID)
  {
    DBObject obj;

    /* -- */

    if (editSet == null)
      {
	throw new RuntimeException("editDBObject called outside of a transaction");
      }

    obj = viewDBObject(baseID, objectID);

    if (obj == null)
      {
	System.err.println("*** couldn't find object, base = " + baseID + ", obj = " + objectID);
	return null;
      }

    if (obj instanceof DBEditObject)
      {
	// we already have a copy checked out.. go ahead and
	// return a reference to our copy

	return (DBEditObject) obj;
      }
    else
      {
	return obj.createShadow(editSet);
      }
  }

  /**
   *
   * Get a reference to a read-only copy of an object in the DBStore.<br><br>
   *
   * If this session has a transaction currently open, this method will return
   * the checked out shadow of invid, if it has been checked out by this
   * transaction.
   *
   * Note that unless the object has been checked out by the current session,
   * this method will return access to the object as it is stored directly
   * in the main datastore hashes.  This means that the object will be
   * read-only and will grant all accesses, as it will have no notion of
   * what session or transaction owns it.  If you need to have access to the
   * object's fields be protected, use GanymedeSession.view_db_object to
   * get the object.
   *
   * @param invid The invariant id of the object to be viewed.
   *
   * @see arlut.csd.ganymede.DBObjectBase
   *
   */

  public synchronized DBObject viewDBObject(Invid invid)
  {
    return viewDBObject(invid.getType(), invid.getNum());
  }

  /**
   *
   * Get a reference to a read-only copy of an object in the DBStore.<br><br>
   *
   * If this session has a transaction currently open, this method will return
   * the checked out shadow of invid, if it has been checked out by this
   * transaction.
   *
   * Note that unless the object has been checked out by the current session,
   * this method will return access to the object as it is stored directly
   * in the main datastore hashes.  This means that the object will be
   * read-only and will grant all accesses, as it will have no notion of
   * what session or transaction owns it.  If you need to have access to the
   * object's fields be protected, use GanymedeSession.view_db_object to
   * get the object.
   *
   * @param baseID The short id number of the DBObjectBase containing the object to
   *               be viewed.
   *
   * @param objectID The int id number of the object to be viewed within the specified
   *                 object base.
   *
   *
   * @see arlut.csd.ganymede.DBObjectBase
   *
   */

  public synchronized DBObject viewDBObject(short baseID, int objectID)
  {
    DBObjectBase base;
    DBObject     obj;
    Short      baseKey;
    Integer      objKey;

    /* -- */

    if (isTransactionOpen())
      {
	obj = editSet.findObject(new Invid(baseID, objectID));

	if (obj != null)
	  {
	    return obj;
	  }
      }

    baseKey = new Short(baseID);
    objKey = new Integer(objectID);

    base = Ganymede.db.getObjectBase(baseKey); // store may not be set at this point if this object is not checked out

    if (base == null)
      {
	return null;
      }

    // depend on the objectHash's thread synchronization here

    obj = (DBObject) base.objectHash.get(objKey);

    // do we want to do any kind of logging here?

    return obj;
  }

  /**
   *
   * Remove an object from the database<br><br>
   *
   * This method method can only be called in the context of an open
   * transaction.  This method will mark an object for deletion.  When
   * the transaction is committed, the object is removed from the
   * database.  If the transaction is aborted, the object remains in
   * the database unchanged.
   *
   * @param invid Invid of the object to be deleted
   * 
   */

  public synchronized ReturnVal deleteDBObject(Invid invid)
  {
    return deleteDBObject(invid.getType(), invid.getNum());
  }

  /**
   *
   * Remove an object from the database<br><br>
   *
   * This method method can only be called in the context of an open
   * transaction.  This method will check an object out of the DBStore
   * and add it to the editset's deletion list.  When the transaction
   * is committed, the object has its remove() method called to do
   * cleanup, and the editSet nulls the object's slot in the DBStore.
   * If the transaction is aborted, the object remains in the database
   * unchanged.
   *
   * @param baseID id of the object base containing the object to be deleted
   * @param objectID id of the object to be deleted
   *  
   */

  public synchronized ReturnVal deleteDBObject(short baseID, int objectID)
  {
    DBObject obj;
    DBEditObject eObj;

    /* -- */

    if (editSet == null)
      {
	throw new RuntimeException("deleteDBObject called outside of a transaction");
      }

    obj = viewDBObject(baseID, objectID);

    if (obj instanceof DBEditObject)
      {
	eObj = (DBEditObject) obj;
      }
    else
      {
	eObj = obj.createShadow(editSet);
      }

    return deleteDBObject(eObj);
  }

  /**
   *
   * Remove an object from the database<br><br>
   *
   * This method method can only be called in the context of an open
   * transaction. Because the object must be checked out (which is the
   * only way to obtain a DBEditObject), no other locking is
   * required. This method will take an object out of the DBStore, do
   * whatever immediate removal logic is required, and mark it as
   * deleted in the transaction.  When the transaction is committed,
   * the object will be expunged from the database.
   *
   * Note that this method does not check to see whether permission
   * has been obtained to delete the object.. that's done in
   * GanymedeSession.delete_db_object().
   *
   * @param eObj An object checked out in the current transaction to be deleted
   *   
   */

  public synchronized ReturnVal deleteDBObject(DBEditObject eObj)
  {
    ReturnVal retVal, retVal2;
    String key;

    /* -- */

    key = "del" + eObj.getLabel();

    switch (eObj.getStatus())
      {
      case DBEditObject.CREATING:
	editSet.checkpoint(key);
	eObj.setStatus(DBEditObject.DROPPING);
	break;

      case DBEditObject.EDITING:
	editSet.checkpoint(key);
	eObj.setStatus(DBEditObject.DELETING);
	break;

      case DBEditObject.DELETING:
      case DBEditObject.DROPPING:

	// already to be deleted

	return null;
      }

    retVal = eObj.remove();

    if (retVal != null && !retVal.didSucceed())
      {
	if (retVal.getCallback() == null)
	  {
	    // oops, irredeemable failure.  rollback.

	    editSet.rollback(key);
	    return retVal;
	  }
	else
	  {
	    // the remove() logic is presenting a wizard
	    // to the user.. turn the client over to
	    // the wizard

	    return retVal;
	  }
      }
    else
      {
	// ok, go ahead and finalize

	retVal2 = eObj.finalizeRemove();

	if (retVal2 != null && !retVal2.didSucceed())
	  {
	    // oops, irredeemable failure.  rollback.
		
	    editSet.rollback(key);
	    return retVal2;
	  }
      }

    return retVal;
  }

  /**
   *
   * Inactivate an object in the database<br><br>
   *
   * This method method can only be called in the context of an open
   * transaction. Because the object must be checked out (which is the only
   * way to obtain a DBEditObject), no other locking is required. This method
   * will take an object out of the DBStore and proceed to do whatever is
   * necessary to cause that object to be 'inactivated'.
   *
   * Note that this method does not check to see whether permission
   * has been obtained to inactivate the object.. that's done in
   * GanymedeSession.inactivate_db_object().
   *
   * @param eObj An object checked out in the current transaction to be inactivated
   *  
   */

  public synchronized ReturnVal inactivateDBObject(DBEditObject eObj)
  {
    ReturnVal retVal;
    String key;

    /* -- */

    key = "inactivate" + eObj.getLabel();

    switch (eObj.getStatus())
      {
      case DBEditObject.EDITING:
      case DBEditObject.CREATING:
	break;

      default:
	return Ganymede.createErrorDialog("Server: Error in DBSession.inactivateDBObject()",
					  "Error.. can't inactivate an object that has already been inactivated or deleted");
      }

    editSet.checkpoint(key);

    retVal = eObj.inactivate();

    if (retVal != null && !retVal.didSucceed())
      {
	if (retVal.getCallback() == null)
	  {
	    // oops, irredeemable failure.  rollback.

	    editSet.rollback(key);
	  }
      }

    return retVal;
  }

  /**
   *
   * Returns true if the session's lock is currently locked, false
   * otherwise.
   *
   */

  public boolean isLocked(DBLock lockParam)
  {
    if (lockParam == null)
      {
	throw new IllegalArgumentException("bad param to isLocked()");
      }

    if (!lockVect.contains(lockParam))
      {
	return false;
      }
    else
      {
	return (lockParam.isLocked());
      }
  }

  /**
   *
   * openReadLock establishes a read lock for the DBObjectBase's in bases.<br><br>
   *
   * The thread calling this method will block until the read lock 
   * can be established.  If any of the DBObjectBases in bases have transactions
   * currently committing, the establishment of the read lock will be suspended
   * until all such transactions are committed.<br><br>
   *
   * All viewDBObject calls done within the context of an open read lock
   * will be transaction consistent.  Other sessions may pull objects out for
   * editing during the course of the session's read lock, but no visible changes
   * will be made to those ObjectBases until the read lock is released.
   */

  public synchronized DBReadLock openReadLock(Vector bases) throws InterruptedException
  {
    DBReadLock lock;

    /* -- */

    lock = new DBReadLock(store, bases);

    lockVect.addElement(lock);

    lock.establish(this);

    return lock;
  }

  /**
   *
   * openReadLock establishes a read lock for the entire DBStore.<br><br>
   *
   * The thread calling this method will block until the read lock 
   * can be established.  If transactions on the database are
   * currently committing, the establishment of the read lock will be suspended
   * until all such transactions are committed.<br><br>
   *
   * All viewDBObject calls done within the context of an open read lock
   * will be transaction consistent.  Other sessions may pull objects out for
   * editing during the course of the session's read lock, but no visible changes
   * will be made to those ObjectBases until the read lock is released.
   */

  public synchronized DBReadLock openReadLock() throws InterruptedException
  {
    DBReadLock lock;

    /* -- */

    lock = new DBReadLock(store);
    lockVect.addElement(lock);

    lock.establish(this);

    return lock;
  }

  /**
   *
   * releaseReadLock releases a read lock held by this session.
   *
   */

  public synchronized void releaseReadLock(DBReadLock lock)
  {
    lock.release();
    lockVect.removeElement(lock);
    notifyAll();
  }
  
  /**
   *
   * releaseAllReadLocks() releases all 
   *
   */

  public synchronized void releaseAllReadLocks()
  {
    DBReadLock lock;
    Enumeration enum = lockVect.elements();

    /* -- */
    
    while (enum.hasMoreElements())
      {
	lock = (DBReadLock) enum.nextElement();
	lock.abort();
      }

    lockVect.removeAllElements();

    notifyAll();
  }

  /**
   * openTransaction establishes a transaction context for this session.
   * When this method returns, the session can call editDBObject and 
   * createDBObject to obtain DBEditObjects.  Methods can then be called
   * on the DBEditObjects to make changes to the database.  These changes
   * are actually performed when and if commitTransaction is called.
   *
   * @param describe An optional string containing a comment to be
   * stored in the modification history for objects modified by this
   * transaction.
   *
   * @see arlut.csd.ganymede.DBEditObject
   *
   */ 

  public synchronized void openTransaction(String describe)
  {
    editSet = new DBEditSet(store, this, describe);
  }

  /**
   * commitTransaction causes any changes made during the context of
   * a current transaction to be performed.  Appropriate portions of the
   * database are locked, changes are made to the in-memory image of
   * the database, and a description of the changes is placed in the
   * DBStore journal file.  Event logging / mail notification may
   * take place.<br><br>
   *
   * The session must not hold any locks when commitTransaction is
   * called.  The symmetrical invid references between related objects
   * and the atomic namespace management code should guarantee that no
   * incompatible change is made with respect to any checked out objects
   * while the Bases are unlocked.
   *
   *
   * @return null if the transaction was committed successfully,
   *         a non-null ReturnVal if there was a commit failure.
   *
   * @see arlut.csd.ganymede.DBEditObject
   *
   */

  public synchronized ReturnVal commitTransaction()
  {
    ReturnVal retVal = null;
    boolean result;

    /* -- */

    // we need to release our readlock, if we have one,
    // so that the commit can establish a writelock..

    // should i make it so that a writelock can be established
    // if the possessor of a readlock doesn't give it up? 

    if (debug)
      {
	System.err.println(key + ": entering commitTransaction");
      }

    if (editSet == null)
      {
	throw new RuntimeException(key + ": commitTransaction called outside of a transaction");
      }

    releaseAllReadLocks();

    while (lockVect.size() != 0)
      {
	Ganymede.debug("DBSession: commitTransaction waiting for read locks to be released");

	try
	  {
	    wait();
	  }
	catch (InterruptedException ex)
	  {
	    Ganymede.debug("DBSession: commitTransaction got an interrupted exception " + 
			   "waiting for read locks to be released." + ex);
	  }

	//	throw new IllegalArgumentException(key + ": commitTransaction(): holding a lock");
      }

    result = editSet.commit();

    if (result)
      {
	Ganymede.debug(key + ": commitTransaction(): editset committed");
      }
    else
      {
	Ganymede.debug(key + ": commitTransaction(): editset failed to commit: transaction aborted");

	retVal = Ganymede.createErrorDialog("Server: Error in DBSession.commitTransaction()",
					    "Error.. transaction could not commit: transaction canceled");
      }

    editSet = null;

    return retVal;		// later on we'll figure out how to do this right
  }

  /**
   * abortTransaction causes all DBEditObjects that were pulled during
   * the course of the session's transaction to be released without affecting
   * the state of the database.  Any changes made to DBObjects pulled for editing
   * by this session during this transaction are abandoned.  Any objects created
   * or destroyed by this session during this transaction are abandoned / unaffected
   * by the actions during the transaction.<br><br>
   *
   * Calling abortTransaction has no affect on any locks held by this session, but
   * generally no locks should be held here.
   *
   *
   * @return null if the transaction was committed successfully,
   *         a non-null ReturnVal if there was a commit failure.
   *
   * @see arlut.csd.ganymede.DBEditObject
   */

  public synchronized ReturnVal abortTransaction()
  {
    if (editSet == null)
      {
	throw new RuntimeException("abortTransaction called outside of a transaction");
      }

    if (editSet.wLock != null)
      {
	if (editSet.wLock.inEstablish)
	  {
	    try
	      {
		editSet.wLock.abort();
	      }
	    catch (NullPointerException ex)
	      {
	      }
	  }
	else
	  {
	    Ganymede.debug("abortTransaction() for " + key + ", can't safely dump writeLock.. can't kill it off");

	    return Ganymede.createErrorDialog("Server: Error in DBSession.abortTransaction()",
					      "Error.. transaction could not abort: can't safely dump writeLock");
	  }
      }

    releaseAllReadLocks();

    while (lockVect.size() != 0)
      {
	Ganymede.debug("DBSession: abortTransaction waiting for read locks to be released");

	try
	  {
	    wait(500);
	  }
	catch (InterruptedException ex)
	  {
	    Ganymede.debug("DBSession: abortTransaction got an interrupted exception " +
			   "waiting for read locks to be released." + ex);
	  }
      }

    editSet.release();
    editSet = null;

    return null;
  }

  /**
   *
   * Returns true if this session has an transaction open
   *
   */

  public boolean isTransactionOpen()
  {
    return (editSet != null);
  }

  /**
   *
   * internal method for setting error messages resulting from session activities.
   *
   * this method may eventually be hooked up to a more general logging
   * mechanism.
   *
   */

  public void setLastError(String error)
  {
    this.lastError = error;

    if (debug)
      {
	Ganymede.debug(key + ": DBSession.setLastError(): " + error);
      }
  }

  public Object getKey()
  {
    return key;
  }

  /**
   *
   * This method is responsible for providing an identifier string
   * for the user who this session belongs to, and is used for
   * logging and what-not.
   *
   */

  public String getID()
  {
    String result = "";
    DBObject obj;

    /* -- */

    if (id != null)
      {
	return id;
      }

    obj = GSession.getUser();

    if (obj != null)
      {
	result = obj.getLabel();
      }

    obj = GSession.getPersona();

    if (obj != null)
      {
	result += ":" + obj.getLabel();
      }

    return result;
  }

  /**
   *
   * This method returns a handle to the Ganymede Session
   * that owns this DBSession.
   *
   */

  public GanymedeSession getGSession()
  {
    return GSession;
  }

  /**
   *
   * This method returns a handle to the objectHook for
   * a particular Invid.
   *
   */

  public DBEditObject getObjectHook(Invid invid)
  {
    DBObjectBase base;

    base = Ganymede.db.getObjectBase(invid.getType());
    return base.objectHook;
  }
  
}
