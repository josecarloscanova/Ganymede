/*

   UserMail.java

   This module represents a class to store the information to be
   represented in an user's email ref base in the server.
   
   Created: 1 December 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.loader;

import java.util.*;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        UserMail

------------------------------------------------------------------------------*/

public class UserMail {

  static final boolean debug = true;

  // --

  String userName;
  Vector aliases = new Vector();
  Vector targets = new Vector();

  /* -- */

  public UserMail(String line) throws IOException
  {
    StringReader reader = new StringReader(line);
    StreamTokenizer tokens = new StreamTokenizer(reader);
    int token;
    String tmp;

    /* -- */

    tokens.resetSyntax();
    tokens.wordChars(0, Integer.MAX_VALUE);
    tokens.eolIsSignificant(true);
    tokens.ordinaryChar(':');
    tokens.ordinaryChar(',');
    tokens.whitespaceChars(' ', ' ');
    tokens.whitespaceChars('\t', '\t');
    tokens.ordinaryChar('\n');

    // and handle the string

    userName = getNextBit(tokens);

    token = tokens.nextToken();

    // skip :

    token = tokens.nextToken();

    // get all the aliases

    while (tokens.ttype == ',' ||
	   tokens.ttype == StreamTokenizer.TT_WORD)
      {
	if (tokens.ttype == ',')
	  {
	    continue;
	  }

	aliases.addElement(tokens.sval);
      }

    // ok, we should be at : -- get our target list

    while (true)
      {
	tmp = getNextBit(tokens);

	if (tmp != null)
	  {
	    targets.addElement(tmp);
	  }
	else
	  {
	    return;
	  }
      }
  }

  private String getNextBit(StreamTokenizer tokens) throws IOException
  {
    int token;

    /* -- */

    token = tokens.nextToken();

    while (tokens.ttype == ':' || tokens.ttype == ',')
      {
	if (debug)
	  {
	    System.err.println("*");
	  }
	token = tokens.nextToken();
      }

    if (tokens.ttype == StreamTokenizer.TT_WORD)
      {
	if (debug)
	  {
	    System.err.println("returning native word");
	  }
	return tokens.sval;
      }

    return null;
  }

}

