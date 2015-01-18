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
package org.fehrman.jcm.main;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.fehrman.jcm.common.ExecuteIF;

/**
 * Main routine, starting point for the starting the program
 *
 * @author Scott Fehrman
 */
//===================================================================
public class Main
//===================================================================
{

   private static final String CLASS_FIND = "org.fehrman.jcm.find.Find";
   private static final String CMD_FIND = "find";
   private final String CLASS = this.getClass().getSimpleName();
   protected final Logger _logger = Logger.getLogger(this.getClass().getName());

   //----------------------------------------------------------------
   public Main()
   //----------------------------------------------------------------
   {
      _logger.entering(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());
      _logger.exiting(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());
      return;
   }

   //----------------------------------------------------------------
   public static void main(String[] args) throws Exception 
   //----------------------------------------------------------------
   {
      // find /work/Software/Library/OracleIdMgr_11gR2_PS1 handle -h

      Main main = new Main();
//      String[] test = {"find","/work/Software/Library/OracleIdMgr_11gR2_PS1","actionhandler", "-e"};
//      main.run(test);
      main.run(args);
      return;
   }

   //----------------------------------------------------------------
   private void run(String[] args) throws Exception 
   //----------------------------------------------------------------
   {
      String METHOD = CLASS + Thread.currentThread().getStackTrace()[1].getMethodName();
      String className = null;
      String cmdName = null;
      String[] execArgs = null;
      Class cls = null;
      ExecuteIF execute = null;

      _logger.entering(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());
      if (args.length > 0)
      {
         cmdName = args[0];
         if (cmdName.equalsIgnoreCase(CMD_FIND))
         {
            className = CLASS_FIND;
         }
         else
         {
            this.handleError("Command '" + cmdName + "' is not valid");
         }

         if (args.length > 1)
         {
            execArgs = new String[args.length - 1];
            for (int i = 1; i < args.length; i++)
            {
               execArgs[i - 1] = args[i];
            }
         }
         else
         {
            execArgs = new String[0];
         }

         try
         {
            cls = Class.forName(className);
            execute = (ExecuteIF) cls.newInstance();
            execute.run(execArgs);
         }
         catch (Exception ex)
         {
            this.handleError(METHOD + ex.getMessage());
         }
      }
      _logger.exiting(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());
      return;
   }

   //----------------------------------------------------------------
   private void handleError(String msg) throws Exception
   //----------------------------------------------------------------
   {
      _logger.log(Level.SEVERE, (msg == null ? "(null)" : msg));
      throw new Exception();
   }
}
