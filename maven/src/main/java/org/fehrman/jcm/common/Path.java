/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *      Portions Copyright 2012-2015 fehrman.org
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License. http://opensource.org/licenses/CDDL-1.0
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each file.
 * If applicable, add the following below this CDDL HEADER, with the fields 
 * enclosed by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 */
package org.fehrman.jcm.common;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Scott Fehrman
 */
//===================================================================
public abstract class Path implements PathIF
//===================================================================
{

   private List<NodeIF> _nodes = null;

   public Path()
   {
      _nodes = new LinkedList<NodeIF>();
      return;
   }

   @Override
   public final synchronized void push(NodeIF node)
   {
      _nodes.add(node);
      return;
   }

   @Override
   public final synchronized NodeIF pop()
   {
      NodeIF node = null;

      if (!_nodes.isEmpty())
      {
         node = _nodes.get((_nodes.size() - 1));
         _nodes.remove((_nodes.size() - 1));
      }

      return node;
   }

   @Override
   public final synchronized List<NodeIF> getNodes() // copy of list and its nodes
   {
      List<NodeIF> path = null;

      path = new LinkedList<NodeIF>();

      for (NodeIF node : _nodes)
      {
         path.add(node.copy());
      }

      return path;
   }
   
   @Override
   public final synchronized String toString()
   {
      StringBuilder b = new StringBuilder();

      if (_nodes != null && !_nodes.isEmpty())
      {
         for (NodeIF node : _nodes)
         {
            if (b.length() > 0)
            {
               b.append(" : ");
            }
            b.append(node.toString());
         }
      }

      return b.toString();
   }
   
   @Override
   public final synchronized int size()
   {
      return _nodes.size();
   }
}
