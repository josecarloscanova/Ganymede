/*********************************************************************

This code was released by James Driscoll into the public domain.

See http://www.io.com/~maus/JavaPage.html for the statement of
public domain release.

@version 1.00 7/17/96
@author James Driscoll maus@io.com


Usage -

Version History:
0.8 5/15/96  - First version
0.9 5/16/96  - fixed date code, added localhost to HELO,
               fixed Subject bug
0.91 7/10/96  - Yet another date fix, for European TimeZones.  Man, they
                gotta fix that code...
1.00 7/17/96  - renamed to Qsmtp, as I have plans for the SMTP code,
                and I want to get this out and announced.  Also cleaned it
                up and commented out the DEBUG code (for size, just in case
                the compiler didn't optimize it out on your machine - mine
                didn't (Symantec Cafe Lite, you get what you pay for, and
                I paid for a book)).
1.01 9/18/96  - Fixed the call to getLocalHost local, which 1.02 JDK didn't
                like (Cafe Lite didn't mind, though).  Think I'll be using
                JDK for all compliations from now on.  Also, added a close
                method, since finalize() is not guarenteed to be called(!).
1.1 12/26/96 -  Fixed problem with EOL, I was using the Unix EOL, not the
                network end of line.  A fragile mail server was barfing.
                I can't beleive I wrote this - that's what half a year will do.
                Also, yanked out the debug code.  It annoyed me.
1.11 12/27/97 - Forgot to flush(), println used to do that for me...

-- 

Modifications by Jonathan Abbey (jonabbey@arlut.utexas.edu):

Mods integrated with 1.11 on 19 January 1999

Made this class open and close connection to the mailer during the
sendMsg() method, rather than having to do a separate close() and
recreate a new Qsmtp object to send an additional message.

Modified the sendMsg() to_address parameter to support a vector of
addresses.

Added the sendHTMLmsg() method to allow for sending MIME-attached
html pages.

Added the extraHeaders parameter to sendMsg() to support sendHTMLmsg().

Moved the code to use the 1.1 io and text formatting classes.

***********************************************************************/

import java.net.*;
import java.io.*;
import java.util.*;
import java.text.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                           Qsmtp

------------------------------------------------------------------------------*/

public class Qsmtp {

  static final int DEFAULT_PORT = 25;
  static final String EOL = "\r\n"; // network end of line

  // --

  protected DataInputStream replyStream = null;
  protected BufferedReader reply = null;
  protected PrintWriter send = null;
  protected Socket sock = null;

  private String hostid = null;
  private InetAddress address = null;
  private int port = DEFAULT_PORT;

  /* -- */

  /**
   *   Create a Qsmtp object pointing to the specified host
   *   @param hostid The host to connect to.
   *   @exception UnknownHostException
   *   @exception IOException
   */

  public Qsmtp(String hostid)
  {
    this.hostid = hostid;
  }

  public Qsmtp(String hostid, int port)
  {
    this.hostid = hostid;
    this.port = port;
  }

  public Qsmtp(InetAddress address)
  {
    this(address, DEFAULT_PORT);
  }

  public Qsmtp(InetAddress address, int port)
  {
    this.address = address;
    this.port = port;
  }

  public void sendmsg(String from_address, Vector to_addresses,
		      String subject, String message) throws IOException, ProtocolException 
  {
    sendmsg(from_address, to_addresses, subject, message, null);
  }

  public void sendmsg(String from_address, Vector to_addresses, 
		      String subject, String message,
		      Vector extraHeaders) throws IOException, ProtocolException
  {
    String rstr;
    String sstr;

    InetAddress local;

    /* -- */

    try 
      {
	local = InetAddress.getLocalHost();
      }
    catch (UnknownHostException ioe) 
      {
	System.err.println("No local IP address found - is your network up?");
	throw ioe;
      }

    if (to_addresses == null ||
	to_addresses.size() == 0)
      {
	return;
      }

    // initialize connection to our SMTP mailer

    if (hostid != null)
      {
	sock = new Socket(hostid, port);
      }
    else
      {
	sock = new Socket(address, port);
      }

    replyStream = new DataInputStream(sock.getInputStream());
    reply = new BufferedReader(new InputStreamReader(replyStream));
    send = new PrintWriter(sock.getOutputStream(), true);

    rstr = reply.readLine();

    if (!rstr.startsWith("220")) 
      {
	throw new ProtocolException(rstr);
      }

    while (rstr.indexOf('-') == 3) 
      {
	rstr = reply.readLine();

	if (!rstr.startsWith("220")) 
	  {
	    throw new ProtocolException(rstr);
	  }
      }

    String host = local.getHostName();

    send.print("HELO " + host);
    send.print(EOL);
    send.flush();

    rstr = reply.readLine();
    if (!rstr.startsWith("250")) 
      {
	throw new ProtocolException(rstr);
      }

    sstr = "MAIL FROM: " + from_address ;
    send.print(sstr);
    send.print(EOL);
    send.flush();

    rstr = reply.readLine();
    if (!rstr.startsWith("250")) 
      {
	throw new ProtocolException(rstr);
      }

    for (int i = 0; i < to_addresses.size(); i++)
      {
	sstr = "RCPT TO: " + (String) to_addresses.elementAt(i);
	send.print(sstr);
	send.print(EOL);
	send.flush();

	rstr = reply.readLine();
	if (!rstr.startsWith("250")) 
	  {
	    throw new ProtocolException(rstr);
	  }
      }

    send.print("DATA");
    send.print(EOL);
    send.flush();

    rstr = reply.readLine();
    if (!rstr.startsWith("354")) 
      {
	throw new ProtocolException(rstr);
      }

    send.print("From: " + from_address);
    send.print(EOL);

    StringBuffer targetString = new StringBuffer();

    for (int i = 0; i < to_addresses.size(); i++)
      {
	if (i > 0)
	  {
	    targetString.append(", ");
	  }

	targetString.append((String) to_addresses.elementAt(i));
      }

    send.print("To: " + targetString.toString());
    send.print(EOL);
    send.print("Subject: " + subject);
    send.print(EOL);
    
    // Create Date - we'll cheat by assuming that local clock is right
    
    Date today_date = new Date();
    send.print("Date: " + formatDate(today_date));
    send.print(EOL);
    send.flush();

    // Warn the world that we are on the loose - with the comments header:

    send.print("Comment: Unauthenticated sender");
    send.print(EOL);
    send.print("X-Mailer: JNet Qsmtp");
    send.print(EOL);

    if (extraHeaders != null)
      {
	String header;

	for (int i = 0; i < extraHeaders.size(); i++)
	  {
	    header = (String) extraHeaders.elementAt(i);
	    send.print(header);
	    send.print(EOL);
	    send.flush();
	  }
      }

    // Sending a blank line ends the header part.

    send.print(EOL);

    // Now send the message proper
    send.print(message);
    send.print(EOL);
    send.print(".");
    send.print(EOL);
    send.flush();
    
    rstr = reply.readLine();
    if (!rstr.startsWith("250")) 
      {
	throw new ProtocolException(rstr);
      }

    // close our mailer connection

    send.print("QUIT");
    send.print(EOL);
    send.flush();

    sock.close();
  }

  /**
   *
   * In a perfect world, we'd do a generic MIME-capable mail system here.
   *
   */

  public void sendHTMLmsg(String from_address, Vector to_addresses,
			  String subject, String htmlBody, String htmlFilename,
			  String textBody) throws IOException, ProtocolException 
  {
    Vector MIMEheaders = new Vector();
    String separator = "B24FDA77DFMIMEISNEAT4976B1CA5E8A49";
    StringBuffer buffer = new StringBuffer();

    /* -- */

    MIMEheaders.addElement("MIME-Version: 1.0");
    MIMEheaders.addElement("Content-Type: multipart/mixed; boundary=\"" + separator + "\"");

    buffer.append("This is a multi-part message in MIME format.\n");
    
    if (textBody != null)
      {
	buffer.append("--");
	buffer.append(separator);
	buffer.append("\nContent-Type: text/plain; charset=us-ascii\n");
	buffer.append("Content-Transfer-Encoding: 7bit\n\n");
	buffer.append(textBody);
	buffer.append("\n");
      }

    if (htmlBody != null)
      {
	buffer.append("--");
	buffer.append(separator);
	buffer.append("\nContent-Type: text/html; charset=us-ascii\n");
	buffer.append("Content-Transfer-Encoding: 7bit\n");
	
	if (htmlFilename != null && !htmlFilename.equals(""))
	  {
	    buffer.append("Content-Disposition: inline; filename=\"");
	    buffer.append(htmlFilename);
	    buffer.append("\"\n\n");
	  }
	else
	  {
	    buffer.append("Content-Disposition: inline;\n\n");
	  }

	buffer.append(htmlBody);
	buffer.append("\n");
      }

    buffer.append("--");
    buffer.append(separator);
    buffer.append("--\n\n");

    sendmsg(from_address, to_addresses, subject, buffer.toString(), MIMEheaders);
  }

  /**
   *
   * This method returns a properly mail-formatted date string.
   *
   */

  public static String formatDate(Date date)
  {
    DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", 
						java.util.Locale.US);
    return formatter.format(date);
  }
}
