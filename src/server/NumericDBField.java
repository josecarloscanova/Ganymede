/*
   GASH 2

   NumericDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd.DBStore;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  NumericDBField

------------------------------------------------------------------------------*/

public class NumericDBField extends DBField {

  int value;

  /* -- */

  NumericDBField(DataInputStream in, DBObjectBaseField definition) throws IOException
  {
    this.definition = definition;
    receive(in);
  }

  public NumericDBField(int value)
  {
    this.definition = null;
    this.value = value;
  }

  void emit(DataOutputStream out) throws IOException
  {
    out.writeInt(value);
  }

  void receive(DataInputStream in) throws IOException
  {
    value = in.readInt();
  }

  public int value()
  {
    return value;
  }

  public Object key()
  {
    return new Integer(value);
  }
}
