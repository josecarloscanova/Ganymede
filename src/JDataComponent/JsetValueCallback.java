/*

   JsetValueCallback.java

   Created: 18 June 1996
   Version: $Revision: 1.3 $ %D%
   Module By: Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin
*/

package arlut.csd.JDataComponent;

/*------------------------------------------------------------------------------
                                                                       interface
                                                               JsetValueCallback

------------------------------------------------------------------------------*/

/**
 *  This interface is used to allow callback to be done from the
 *  components in the package to the container which contains them.
 *  The conatiner that is to contain the components needs to provide
 *  implementations for the methods defined in this interface.
 */

public interface JsetValueCallback
{
  public boolean setValuePerformed(JValueObject v) throws java.rmi.RemoteException;
}









