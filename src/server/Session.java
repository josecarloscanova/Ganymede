/*

   Session.java

   Client side remote interface.

   Client side interface definition for the Ganymede Session Object.  The
   Ganymede Session object holds the state for a Ganymede client's session
   with the Ganymede server.  The Ganymede session will also provide the
   primary interface for accessing ganymede db objects.

   Created: 1 April 1996
   Version: $Revision: 1.15 $ %D%
   Module By: Jonathan Abbey  jonabbey@arlut.utexas.edu
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                         Session

------------------------------------------------------------------------------*/

/**
 *   Client side interface definition for the Ganymede Session Object.  The
 *   Ganymede Session object holds the state for a .Ganymede client's session
 *   with the Ganymede server.  The Ganymede session will also provide the
 *   primary interface for accessing ganymede db objects.
 *
 * @version $Revision: 1.15 $ %D%
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 *
 * @see arlut.csd.ganymede.DBSession
 */

public interface Session extends Remote {

  // Client/server interface operations

  String      getLastError() throws RemoteException;
  void        logout() throws RemoteException;

  /**
   *
   * This method returns a list of personae names available
   * to the user logged in.
   *
   */

  Vector      getPersonae() throws RemoteException;

  /**
   *
   * This method may be used to select an admin persona.
   *
   */

  boolean     selectPersona(String persona, String password) throws RemoteException;

  /**
   *
   * This method returns a QueryResult of owner groups that the current
   * persona has access to.  This list is the transitive closure of
   * the list of owner groups in the current persona.  That is, the
   * list includes all the owner groups in the current persona along
   * with all of the owner groups those owner groups own, and so on.
   *
   */

  QueryResult      getOwnerGroups() throws RemoteException;

  /**
   *
   * This method may be used to set the owner groups of any objects
   * created hereafter.
   *
   * @param ownerInvids a Vector of Invid objects pointing to
   * ownergroup objects.
   *
   */

  void        setDefaultOwner(Vector ownerInvids) throws RemoteException;

  /**
   *
   * This method may be used to cause the server to pre-filter any object
   * listing to only show those objects directly owned by owner groups
   * referenced in the ownerInvids list.  This filtering will not restrict
   * the ability of the client to directly view any object that the client's
   * persona would normally have access to, but will reduce clutter and allow
   * the client to present the world as would be seen by administrator personas
   * with just the listed ownerGroups accessible.
   *
   * This method cannot be used to grant access to objects that are accessible
   * by the client's adminPersona.
   *
   * Calling this method with ownerInvids set to null will turn off the filtering.
   *
   * @param ownerInvids a Vector of Invid objects pointing to ownergroup objects.
   *
   */

  void        filterQueries(Vector ownerInvids) throws RemoteException;

  //  Database operations

  /**
   *
   * List types of objects stored and manipulated through the Ganymede server.
   *
   * This method returns a vector of Base remote references.
   *
   * @see arlut.csd.ganymede.Base
   */

  Vector      getTypes() throws RemoteException;

  /**
   *
   * Returns the root of the category tree on the server
   *
   * @see arlut.csd.ganymede.Category
   *
   */

  Category    getRootCategory() throws RemoteException;

  /**
   *
   * Returns a vector of field definition templates, in display order.
   *
   * This vector may be cached, as it is static for this object type.
   *
   * @see arlut.csd.ganymede.FieldTemplate
   *
   */

  Vector      getFieldTemplateVector(short baseId) throws RemoteException;

  /**
   *
   * This method call initiates a transaction on the server.  This
   * call must be executed before any objects are modified (created,
   * edited, inactivated, removed).
   *
   * Currently each client can only have one transaction open.. it
   * is an error to call openTransaction() while another transaction
   * is still open, and an exception will be thrown.
   * 
   */

  void     openTransaction(String description) throws RemoteException;

  /**
   *
   * This method causes all changes made by the client to be 'locked in'
   * to the database.  When commitTransaction() is called, the changes
   * made by the client during this transaction is logged to a journal
   * file on the server, and the changes will become visible to other
   * clients, or to subsequent queries and view_db_object() calls by
   * this client.
   *
   * If the transaction cannot be committed for some reason,
   * commitTransaction() will instead abort the transaction.  In any
   * case, calling commitTransaction() will close the transaction.
   *
   * @return false if the transaction could not be committed.
   *               getLastError() can be called to obtain an explanation
   *               of commit failure.
   * 
   */

  boolean  commitTransaction() throws RemoteException;

  /**
   *
   * This method causes all changes made by the client to be thrown out
   * by the database, and the transaction is closed.
   *
   */
  
  void     abortTransaction() throws RemoteException;

  /**
   *
   * This method provides the hook for doing a
   * fast database dump to a string form.  The StringBuffer
   * returned comprises a formatted dump of all visible
   * fields and objects that match the given query.
   *
   * @see arlut.csd.ganymede.Query
   *
   * @see arlut.csd.ganymede.Session
   *
   */

  DumpResult dump(Query query) throws RemoteException;

  /**
   *
   * List objects in the database meeting the given query criteria.
   *
   * The database will be read-locked during the query, assuring
   * a transaction-consistent view of the database.  The StringBuffer
   * returned comprises a formatted dump of the invid's and
   * labels of the viewable objects matching the provided query.
   *
   */

  QueryResult    query(Query query) throws RemoteException;

  /**
   *
   * This method returns the label for a specific invid.
   *
   */

  String    viewObjectLabel(Invid invid) throws RemoteException;

  /**
   *
   * View an object from the database.  If the return value is null,
   * getLastError() should be called for a description of the problem.
   *
   * view_db_object() can be done at any time, outside of the bounds of
   * any transaction.  view_db_object() returns a snapshot of the object's
   * state at the time the view_db_object() call is processed, and will
   * be transaction-consistent internally.
   *
   * @return the object for viewing
   *
   */

  db_object   view_db_object(Invid invid) throws RemoteException;

  /**
   *
   * Check an object out from the database for editing.  If the return
   * value is null, getLastError() should be called for a description
   * of the problem. 
   *
   * @return the object for editing
   *
   */

  db_object   edit_db_object(Invid invid) throws RemoteException;

  /**
   *
   * Create a new object of the given type. If the return value is null,
   * getLastError() should be called for a description of the problem. 
   *
   * @return the newly created object for editing
   *
   */

  db_object   create_db_object(short type) throws RemoteException;

  /**
   *
   * Clone a new object from object <invid>. If the return value is null,
   * getLastError() should be called for a description of the problem. 
   *
   * Typically, only certain values will be cloned.  What values are
   * retained is up to the specific code module provided for the
   * invid's object type.
   *
   * @return the newly created object for editing, if null check getLastError()
   *
   */

  db_object   clone_db_object(Invid invid) throws RemoteException;

  /**
   *
   * Inactivate an object in the database
   *
   * Objects inactivated will typically be altered to reflect their inactive
   * status, but the object itself might not be purged from the Ganymede
   * server for a defined period of time, to allow other network systems
   * to have time to do accounting, clean up, etc., before a user id or
   * network address is re-used.
   *
   * @return true if the object was inactivated, if false, check getLastError()
   *
   */

  boolean     inactivate_db_object(Invid invid) throws RemoteException;

  /**
   *
   * Remove an object from the database
   *
   * Certain objects cannot be inactivated, but must instead be
   * simply removed on demand.  The active permissions for the client
   * may determine whether a particular type of object may be removed.
   *
   * @return true if the object was removed, if false, check getLastError()
   *
   */

  boolean     remove_db_object(Invid invid) throws RemoteException;
}

