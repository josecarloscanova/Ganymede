/*

   QueryAndNode.java

   Created: 10 July 1997
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    QueryAndNode

------------------------------------------------------------------------------*/

public class QueryAndNode extends QueryNode {

  static final long serialVersionUID = -3475701914505388243L;

  // ---

  QueryNode child1, child2;
  
  /* -- */

  public QueryAndNode(QueryNode child1, QueryNode child2)
  {
    this.child1 = child1;
    this.child2 = child2;
  }
}

