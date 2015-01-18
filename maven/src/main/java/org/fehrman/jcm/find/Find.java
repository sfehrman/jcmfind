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
package org.fehrman.jcm.find;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.fehrman.jcm.common.ExecuteIF;
import org.fehrman.jcm.common.NodeIF;
import org.fehrman.jcm.common.PathIF;
import org.fehrman.jcm.common.PathNode;
import org.fehrman.jcm.common.SearchPath;
import org.fehrman.jcm.utilities.StrUtil;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 *
 * @author Scott Fehrman
 */
//===================================================================
public class Find implements ExecuteIF
//===================================================================
{

   private static final int CNT_DIR = 0;
   private static final int CNT_ARCHIVE = 1;
   private static final int CNT_PACKAGE = 2;
   private static final int CNT_CLASS = 3;
   private static final int CNT_METHOD = 4;
   private static final int CNT_MATCH = 5;
   private static final int CNT_ERROR = 6;
   private static final char OPT_CLASS = 'c'; // search class names (default=true)
   private static final char OPT_DEBUG = 'd'; // show debug details
   private static final char OPT_EQUAL = 'e'; // use equal instead of contains
   private static final char OPT_EXCEPTION = 'E'; // hide Exception messages
   private static final char OPT_HELP = 'h'; // show help
   private static final char OPT_METHOD = 'm'; // search method names (default=false)
   private static final char OPT_PACKAGE = 'p'; // search package names (default=false)
   private static final char OPT_RESULTS = 'R'; // hide results
   private static final char OPT_VERBOSE = 'v'; // verbose display (default=false)
   private static final String JAVA_LANG = "java.lang.";
   private static final boolean _throwEx = false;
   private final String CLASS = this.getClass().getSimpleName();
   protected final Logger _logger = Logger.getLogger(this.getClass().getName());

   private enum OPERATOR
   {

      CONTAINS,
      EQUALS
   };

   //----------------------------------------------------------------
   public Find()
   //----------------------------------------------------------------
   {
      return;
   }

   //----------------------------------------------------------------
   @Override
   public void run(String[] args) throws Exception
   //----------------------------------------------------------------
   {
      /*
       * arguments:
       * 0: file path to start searching
       * 1: search value
       */
      int[] cnt =
      {
         0, 0, 0, 0, 0, 0, 0
      }; // dirs, ears|wars|jars, packages, classes, methods, matchs, errors
      long tStart = 0L;
      long tStop = 0L;
      File fileDirScan = null;
      Properties props = null;
      PathIF path = null;

      _logger.entering(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      props = new Properties();
      props.put(PROP_SRCH_TYPE_PACKAGE, false);
      props.put(PROP_SRCH_TYPE_CLASS, false);
      props.put(PROP_SRCH_TYPE_METHOD, false);
      props.put(PROP_DISP_DEBUG, false);
      props.put(PROP_DISP_ERROR, true);
      props.put(PROP_DISP_RESULTS, true);
      props.put(PROP_DISP_VERBOSE, false);
      props.put(PROP_SRCH_OPERATOR, OPERATOR.CONTAINS); // default: search using "contains"

      path = new SearchPath();

      this.parseArgs(args, props);

      fileDirScan = new File(props.getProperty(PROP_SRCH_PATH));

      tStart = System.currentTimeMillis();
      this.processFile(path, cnt, props, fileDirScan);
      tStop = System.currentTimeMillis();

      if ((Boolean) props.get(PROP_DISP_RESULTS))
      {
         System.out.println("Searched "
            + cnt[CNT_DIR] + " Dirs, "
            + cnt[CNT_ARCHIVE] + " Archives, "
            + ((Boolean) props.get(PROP_SRCH_TYPE_PACKAGE) ? cnt[CNT_PACKAGE] + " Packages, " : "(No Packages), ")
            + ((Boolean) props.get(PROP_SRCH_TYPE_CLASS) ? cnt[CNT_CLASS] + " Classes, " : "(No Classes), ")
            + ((Boolean) props.get(PROP_SRCH_TYPE_METHOD) ? cnt[CNT_METHOD] + " Methods, " : "(No Methods), ")
            + "Found: "
            + cnt[CNT_MATCH] + " Matches and "
            + cnt[CNT_ERROR] + " Errors"
            + " in " + (tStop - tStart) + " msec.");
      }

      _logger.exiting(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      return;
   }

   //----------------------------------------------------------------
   private void parseArgs(String[] args, Properties props) throws Exception
   //----------------------------------------------------------------
   {
      boolean hasType = false;
      int paramCnt = 0;
      char[] opts = null;
      String value = null;
      String path = null;

      _logger.entering(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      if (args == null || args.length < 1)
      {
         this.displayHelp("Missing required arguments");
         System.exit(1);
      }

      /*
       * arguments ...
       * if begin with "-" then it's an option
       * else its a parameter
       */

      for (String arg : args)
      {
         if (arg.startsWith("-")) // option
         {
            opts = arg.toCharArray();
            for (int j = 0; j < opts.length; j++)
            {
               if (j == 0) // skip the dash
               {
                  continue;
               }
               else
               {
                  switch (opts[j])
                  {
                     case OPT_HELP:
                        this.displayHelp(null);
                        System.exit(0);
                     case OPT_CLASS:
                        props.put(PROP_SRCH_TYPE_CLASS, true);
                        hasType = true;
                        break;
                     case OPT_METHOD:
                        props.put(PROP_SRCH_TYPE_METHOD, true);
                        hasType = true;
                        break;
                     case OPT_PACKAGE:
                        props.put(PROP_SRCH_TYPE_PACKAGE, true);
                        hasType = true;
                        break;
                     case OPT_DEBUG:
                        props.put(PROP_DISP_DEBUG, true); // default = false
                        break;
                     case OPT_EXCEPTION:
                        props.put(PROP_DISP_ERROR, false); // default = true
                        break;
                     case OPT_RESULTS:
                        props.put(PROP_DISP_RESULTS, false); // default = true;
                        break;
                     case OPT_VERBOSE:
                        props.put(PROP_DISP_VERBOSE, true); // default = false;
                        break;
                     case OPT_EQUAL:
                        props.put(PROP_SRCH_OPERATOR, OPERATOR.EQUALS); // default = "contains"
                        break;
                     default:
                        this.displayHelp("Unsupported option: '" + opts[j] + "'");
                        System.exit(1);
                  }
               }
            }
         }
         else // parameter
         {
            switch (paramCnt)
            {
               case 0: // first ... "search path"
                  path = arg;
                  paramCnt++;
                  break;
               case 1: // second ... "search value"
                  value = arg;
                  paramCnt++;
                  break;
               default: // igonore any other arguments
                  break;
            }
         }
      }

      if (!hasType)
      {
         props.put(PROP_SRCH_TYPE_CLASS, true);
      }

      if (path == null || path.length() < 1)
      {
         this.displayHelp("Missing search path");
         System.exit(1);
      }
      props.put(PROP_SRCH_PATH, path);

      if (value == null || value.length() < 1)
      {
         this.displayHelp("Missing search value");
         System.exit(1);
      }
      props.put(PROP_SRCH_VALUE, value.toLowerCase());

      _logger.exiting(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      return;
   }

   //----------------------------------------------------------------
   private void displayHelp(String msg)
   //----------------------------------------------------------------
   {
      _logger.entering(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      if (msg != null && msg.length() > 0)
      {
         System.out.println(msg);
      }

      System.out.println("\n"
         + "find <search_path> <search_value> ([ -c | -m | -p ]) (-h) (-d) (-e) (-v) (-E) (-R)\n"
         + "  -h : display syntax and options\n"
         + "  -c : search class names (default)\n"
         + "  -m : search method names\n"
         + "  -p : search package names\n"
         + "  -d : show debug data\n"
         + "  -e : use equals search (instead of contains)\n"
         + "  -v : verbose output\n"
         + "  -E : hide exception messages\n"
         + "  -R : hide results summary\n");

      _logger.exiting(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      return;
   }

   //----------------------------------------------------------------
   private void processFile(PathIF path, int[] cnt, Properties props, File file) throws Exception
   //----------------------------------------------------------------
   {
      String METHOD = CLASS + ":processFile(): ";
      String fileName = null;
      File[] subfiles = null;
      InputStream istream = null;
      ZipFile zip = null;

      _logger.entering(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      if (file != null)
      {
         if (file.isDirectory())
         {
            cnt[CNT_DIR]++;
            subfiles = file.listFiles();
            if (subfiles != null)
            {
               for (File subfile : subfiles)
               {
                  this.processFile(path, cnt, props, subfile);
               }
            }
         }
         else // check for archives (zip, ear, war, jar) and classes
         {
            fileName = file.getName();

            path.push(new PathNode(NodeIF.Type.FOLDER, file.getParent()));

            if (this.isArchive(fileName.toLowerCase())) // this is an archive
            {
               try
               {
                  zip = new ZipFile(file);
               }
               catch (Exception ex)
               {
                  cnt[CNT_ERROR]++;
                  if ((Boolean) props.get(PROP_DISP_ERROR))
                  {
                     this.err("EXCEPTION: " + METHOD
                        + "'" + ex.toString()
                        + "', '" + fileName
                        + "', " + this.pathToString(path));
                     zip = null;
                  }
                  if (_throwEx)
                  {
                     throw ex;
                  }
               }

               if (zip != null)
               {
                  cnt[CNT_ARCHIVE]++;
                  path.push(new PathNode(NodeIF.Type.ARCHIVE, fileName));
                  try
                  {
                     this.processZip(path, cnt, props, zip);
                  }
                  catch (Exception ex)
                  {
                     cnt[CNT_ERROR]++;
                     if ((Boolean) props.get(PROP_DISP_ERROR))
                     {
                        this.err("EXCEPTION: " + METHOD
                           + "'" + ex.toString()
                           + "', " + this.pathToString(path));
                     }
                     if (_throwEx)
                     {
                        throw ex;
                     }
                  }
                  finally
                  {
                     zip.close();
                  }
                  path.pop();
               }
            }
            else if (this.isClass(fileName.toLowerCase())) // this is a class
            {
               try
               {
                  istream = new FileInputStream(file);
               }
               catch (Exception ex)
               {
                  cnt[CNT_ERROR]++;
                  if ((Boolean) props.get(PROP_DISP_ERROR))
                  {
                     this.err("EXCEPTION: " + METHOD
                        + "'" + ex.toString()
                        + "', '" + fileName
                        + "', " + this.pathToString(path));
                     istream = null;
                  }
                  if (_throwEx)
                  {
                     throw ex;
                  }
               }
               if (istream != null)
               {
                  try
                  {
                     this.processClass(path, cnt, props, istream);
                  }
                  catch (Exception ex)
                  {
                     cnt[CNT_ERROR]++;
                     if ((Boolean) props.get(PROP_DISP_ERROR))
                     {
                        this.err("EXCEPTION: " + METHOD
                           + "'" + ex.toString()
                           + "', " + this.pathToString(path));
                     }
                     if (_throwEx)
                     {
                        throw ex;
                     }
                  }
                  finally
                  {
                     istream.close();
                  }
               }
            }
            path.pop();
         }
      }

      _logger.exiting(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      return;
   }

   //----------------------------------------------------------------
   private void processZip(PathIF path, int[] cnt, Properties props, ZipFile zipFile) throws Exception
   //----------------------------------------------------------------
   {
      String METHOD = "processZip(ZipFile): ";
      String entryName = null;
      Enumeration enumZip = null;
      ZipEntry zipEntry = null;
      InputStream inputStream = null;
      ZipInputStream zipStream = null;

      _logger.entering(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      enumZip = zipFile.entries();

      while (enumZip.hasMoreElements())
      {
         zipEntry = (ZipEntry) enumZip.nextElement();
         if (zipEntry != null)
         {
            entryName = zipEntry.getName();
            if (this.isArchive(entryName.toLowerCase())) // nested ear, war, jar
            {
               zipStream = new ZipInputStream(zipFile.getInputStream(zipEntry));
               if (zipStream != null)
               {
                  cnt[CNT_ARCHIVE]++;
                  path.push(new PathNode(NodeIF.Type.ARCHIVE, entryName));
                  try
                  {
                     this.processZip(path, cnt, props, zipStream);
                  }
                  catch (Exception ex)
                  {
                     cnt[CNT_ERROR]++;
                     if ((Boolean) props.get(PROP_DISP_ERROR))
                     {
                        this.err("EXCEPTION: " + METHOD
                           + "this.processZip(ZipInputStream): '" + ex.toString()
                           + "', " + this.pathToString(path));
                     }
                     if (_throwEx)
                     {
                        throw ex;
                     }
                  }
                  path.pop();
                  zipStream.close();
               }
               else
               {
                  cnt[CNT_ERROR]++;
                  if ((Boolean) props.get(PROP_DISP_ERROR))
                  {
                     this.err("WARNING: " + METHOD
                        + "ZipInputStream is null, entry='" + entryName
                        + "', " + this.pathToString(path));
                  }
               }
            }
            else if (this.isClass(entryName.toLowerCase()))
            {
               inputStream = zipFile.getInputStream(zipEntry);
               if (inputStream != null)
               {
                  try
                  {
                     this.processClass(path, cnt, props, inputStream);
                  }
                  catch (Exception ex)
                  {
                     cnt[CNT_ERROR]++;
                     if ((Boolean) props.get(PROP_DISP_ERROR))
                     {
                        this.err("EXCEPTION: " + METHOD
                           + "this.processClass(InputStream): '" + ex.toString() + "', "
                           + this.pathToString(path));
                     }
                     if (_throwEx)
                     {
                        throw ex;
                     }
                  }
                  finally
                  {
                     inputStream.close();
                  }
               }
            }
         }
      }
      
      _logger.exiting(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      return;
   }

   //----------------------------------------------------------------
   private void processZip(PathIF path, int[] cnt, Properties props, ZipInputStream zipStream) throws Exception
   //----------------------------------------------------------------
   {
      boolean bDone = false;
      byte[] data = null;
      int read = 0;
      String METHOD = "processZip(ZipInputStream): ";
      String entryName = null;
      ZipEntry zipEntry = null;
      ByteArrayOutputStream baoStream = null;
      InputStream inputStream = null;
      ZipInputStream ziStream = null;

      _logger.entering(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      do
      {
         zipEntry = zipStream.getNextEntry();
         if (zipEntry == null)
         {
            bDone = true;
         }
         else
         {
            entryName = zipEntry.getName().toLowerCase();

            if (this.isClass(entryName) || this.isArchive(entryName))
            {
               baoStream = new ByteArrayOutputStream();
               data = new byte[1024];

               while (zipStream.available() == 1)
               {
                  read = zipStream.read(data);
                  if (read > 0)
                  {
                     baoStream.write(data, 0, read);
                  }
               }

               if (this.isArchive(entryName)) // nested ear, war, jar
               {
                  ziStream = new ZipInputStream(new ByteArrayInputStream(baoStream.toByteArray()));
                  if (ziStream != null)
                  {
                     cnt[CNT_ARCHIVE]++;
                     path.push(new PathNode(NodeIF.Type.ARCHIVE, zipEntry.getName()));

                     try
                     {
                        this.processZip(path, cnt, props, ziStream);
                     }
                     catch (Exception ex)
                     {
                        cnt[CNT_ERROR]++;
                        if ((Boolean) props.get(PROP_DISP_ERROR))
                        {
                           this.err("EXCEPTION: " + METHOD
                              + "processZip: '" + ex.toString() + "', "
                              + this.pathToString(path));
                        }
                        if (_throwEx)
                        {
                           throw ex;
                        }
                     }
                     finally
                     {
                        ziStream.close();
                     }

                     path.pop();
                  }
                  else
                  {
                     cnt[CNT_ERROR]++;
                     if ((Boolean) props.get(PROP_DISP_ERROR))
                     {
                        this.err("WARNING: " + METHOD
                           + "ZipInputStream is null, entry='" + entryName
                           + "', " + this.pathToString(path));
                     }
                  }
               }
               else if (this.isClass(entryName))
               {
                  inputStream = new ByteArrayInputStream(baoStream.toByteArray());
                  if (inputStream != null)
                  {
                     if ((Boolean) props.get(PROP_DISP_DEBUG))
                     {
                        this.out("DEBUG (archive/class): " + METHOD + this.pathToString(path));
                     }

                     try
                     {
                        this.processClass(path, cnt, props, inputStream);
                     }
                     catch (Exception ex)
                     {
                        cnt[CNT_ERROR]++;
                        if ((Boolean) props.get(PROP_DISP_ERROR))
                        {
                           this.err("EXCEPTION: " + METHOD
                              + "processClass: '" + ex.toString() + "', "
                              + this.pathToString(path));
                        }
                        if (_throwEx)
                        {
                           bDone = true;
                           throw ex;
                        }
                     }
                     finally
                     {
                        inputStream.close();
                     }
                  }
                  else
                  {
                     cnt[CNT_ERROR]++;
                     if ((Boolean) props.get(PROP_DISP_ERROR))
                     {
                        this.err("WARNING: " + METHOD
                           + "ByteArrayInputStream is null, entry='" + entryName
                           + "', " + this.pathToString(path));
                     }
                  }
               }

               baoStream.close();
            }
         }
      }
      while (!bDone);

      _logger.exiting(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      return;
   }

   //----------------------------------------------------------------
   @SuppressWarnings("unchecked")
   private void processClass(PathIF path, int[] cnt, Properties props, InputStream istream) throws Exception
   //----------------------------------------------------------------
   {
      boolean match = false;
      boolean verbose = false;
      int lastdot = 0;
      String METHOD = "processClass(InputStream): ";
      String packageName = null;
      String className = null;
      String methodName = null;
      String srchValue = null;
      ClassNode classNode = null;
      ClassReader classReader = null;
      Type classType = null;
      List<MethodNode> methodNodes = null;
      OPERATOR oper = null;

      _logger.entering(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      classNode = new ClassNode();

      classReader = new ClassReader(istream);
      classReader.accept(classNode, 0);

      classType = Type.getObjectType(classNode.name);
      className = classType.getClassName();

      if (!StrUtil.isEmpty(className))
      {
         lastdot = className.lastIndexOf(".");
         if (lastdot > 0 && lastdot < className.length())
         {
            packageName = className.substring(0, lastdot);
            className = className.substring(lastdot + 1);
         }
         else
         {
            packageName = className;
         }

         path.push(new PathNode(NodeIF.Type.PACKAGE, packageName));
         path.push(new PathNode(NodeIF.Type.CLASS, className));

         if ((Boolean) props.get(PROP_DISP_DEBUG))
         {
            this.out("DEBUG: " + METHOD
               + "[" + path.getNodes().size() + "] "
               + this.pathToString(path));
         }

         srchValue = props.getProperty(PROP_SRCH_VALUE);
         verbose = (Boolean) props.get(PROP_DISP_VERBOSE);

         oper = (OPERATOR) props.get(PROP_SRCH_OPERATOR);

         /*
          * PACKAGE NAME
          */

         if ((Boolean) props.get(PROP_SRCH_TYPE_PACKAGE) && !packageName.equals(className))
         {
            cnt[CNT_PACKAGE]++;

            match = false;

            switch (oper)
            {
               case CONTAINS:
                  if (packageName.toLowerCase().contains(srchValue))
                  {
                     match = true;
                  }
                  break;
               case EQUALS:
                  if (packageName.toLowerCase().equals(srchValue))
                  {
                     match = true;
                  }
                  break;
            }

            if (match)
            {
               if (verbose)
               {
                  this.out(this.pathToStringVerbose(path));
               }
               else
               {
                  this.out(this.pathToString(path));
               }
               cnt[CNT_MATCH]++;
            }
         }

         /*
          * CLASS NAME
          */

         if ((Boolean) props.get(PROP_SRCH_TYPE_CLASS))
         {
            cnt[CNT_CLASS]++;

            match = false;

            switch (oper)
            {
               case CONTAINS:
                  if (className.toLowerCase().contains(srchValue))
                  {
                     match = true;
                  }
                  break;
               case EQUALS:
                  if (className.toLowerCase().equals(srchValue))
                  {
                     match = true;
                  }
                  break;
            }

            if (match)
            {
               if (verbose)
               {
                  this.out(this.pathToStringVerbose(path));
               }
               else
               {
                  this.out(this.pathToString(path));
               }
               cnt[CNT_MATCH]++;
            }
         }

         /*
          * METHOD NAME
          */

         if ((Boolean) props.get(PROP_SRCH_TYPE_METHOD))
         {
            methodNodes = classNode.methods;

            if (methodNodes != null)
            {
               for (MethodNode methodNode : methodNodes)
               {
                  methodName = methodNode.name;
                  if (methodName != null)
                  {
                     cnt[CNT_METHOD]++;

                     match = false;

                     switch (oper)
                     {
                        case CONTAINS:
                           if (methodName.toLowerCase().contains(srchValue))
                           {
                              match = true;
                           }
                           break;
                        case EQUALS:
                           if (methodName.toLowerCase().equals(srchValue))
                           {
                              match = true;
                           }
                           break;
                     }

                     if (match)
                     {
                        if (verbose)
                        {
                           path.push(new PathNode(NodeIF.Type.METHOD, this.processMethod(methodNode)));
                           this.out(this.pathToStringVerbose(path));
                           path.pop();
                        }
                        else
                        {
                           path.push(new PathNode(NodeIF.Type.METHOD, methodName));
                           this.out(this.pathToString(path));
                           path.pop();
                        }
                        cnt[CNT_MATCH]++;
                     }
                  }
               }
            }
         }
      }

      path.pop(); // class name
      path.pop(); // package name

      _logger.exiting(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      return;
   }

   //----------------------------------------------------------------
   @SuppressWarnings("unchecked")
   private String processMethod(MethodNode methodNode) throws Exception
   //----------------------------------------------------------------
   {
      StringBuilder methodDescription = new StringBuilder();
      String className = null;

      Type returnType = Type.getReturnType(methodNode.desc);
      Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);

      List<String> thrownInternalClassNames = methodNode.exceptions;

      _logger.entering(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      if ((methodNode.access & Opcodes.ACC_PUBLIC) != 0)
      {
         methodDescription.append("public ");
      }

      if ((methodNode.access & Opcodes.ACC_PRIVATE) != 0)
      {
         methodDescription.append("private ");
      }

      if ((methodNode.access & Opcodes.ACC_PROTECTED) != 0)
      {
         methodDescription.append("protected ");
      }

      if ((methodNode.access & Opcodes.ACC_STATIC) != 0)
      {
         methodDescription.append("static ");
      }

      if ((methodNode.access & Opcodes.ACC_ABSTRACT) != 0)
      {
         methodDescription.append("abstract ");
      }

      if ((methodNode.access & Opcodes.ACC_SYNCHRONIZED) != 0)
      {
         methodDescription.append("synchronized ");
      }

      className = returnType.getClassName();
      if (className != null && className.startsWith(JAVA_LANG))
      {
         methodDescription.append(className.substring(JAVA_LANG.length()));
      }
      else
      {
         methodDescription.append(className);
      }

      methodDescription.append(" ");
      methodDescription.append(methodNode.name);

      methodDescription.append("(");
      for (int i = 0; i < argumentTypes.length; i++)
      {
         Type argumentType = argumentTypes[i];

         if (i > 0)
         {
            methodDescription.append(", ");
         }

         className = argumentType.getClassName();
         if (className != null && className.startsWith(JAVA_LANG))
         {
            methodDescription.append(className.substring(JAVA_LANG.length()));
         }
         else
         {
            methodDescription.append(className);
         }
      }
      methodDescription.append(")");

      if (!thrownInternalClassNames.isEmpty())
      {
         methodDescription.append(" throws ");
         int i = 0;
         for (String thrownInternalClassName : thrownInternalClassNames)
         {
            if (i > 0)
            {
               methodDescription.append(", ");
            }

            className = Type.getObjectType(thrownInternalClassName).getClassName();
            if (className != null && className.startsWith(JAVA_LANG))
            {
               methodDescription.append(className.substring(JAVA_LANG.length()));
            }
            else
            {
               methodDescription.append(className);
            }
            i++;
         }
      }

      _logger.exiting(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      return methodDescription.toString();
   }

   //----------------------------------------------------------------
   private String pathToString(final PathIF path)
   //----------------------------------------------------------------
   {
      StringBuilder b = new StringBuilder();
      List<NodeIF> nodes = null;

      _logger.entering(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      nodes = path.getNodes();
      if (nodes != null && !nodes.isEmpty())
      {
         for (NodeIF node : nodes)
         {
            if (b.length() > 0)
            {
               b.append(" : ");
            }
            b.append(node.toString());
         }
      }

      _logger.exiting(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      return b.toString();
   }

   //----------------------------------------------------------------
   private String pathToStringVerbose(final PathIF path)
   //----------------------------------------------------------------
   {
      int indent = 0;
      StringBuilder b = new StringBuilder();
      List<NodeIF> nodes = null;

      _logger.entering(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      nodes = path.getNodes();
      if (nodes != null && !nodes.isEmpty())
      {
         for (NodeIF node : nodes)
         {
            for (int i = 0; i < indent; i++)
            {
               b.append("   ");
            }
            b.append(node.toString()).append("\n");
            indent++;
         }
      }

      _logger.exiting(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      return b.toString();
   }

   //----------------------------------------------------------------
   private boolean isClass(final String fileName)
   //----------------------------------------------------------------
   {
      boolean isClass = false;

      _logger.entering(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      if (fileName.endsWith(".class") && fileName.indexOf("$") < 0)
      {
         isClass = true;
      }

      _logger.exiting(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      return isClass;
   }

   //----------------------------------------------------------------
   private boolean isArchive(final String fileName)
   //----------------------------------------------------------------
   {
      boolean isArchive = false;

      _logger.entering(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      if (fileName.endsWith(".zip"))
      {
         isArchive = true;
      }
      else if (fileName.endsWith(".ear"))
      {
         isArchive = true;
      }
      else if (fileName.endsWith(".war"))
      {
         isArchive = true;
      }
      else if (fileName.endsWith(".jar"))
      {
         isArchive = true;
      }

      _logger.exiting(CLASS, Thread.currentThread().getStackTrace()[1].getMethodName());

      return isArchive;
   }

   //----------------------------------------------------------------
   private void out(final String str)
   //----------------------------------------------------------------
   {
      System.out.println(str);
      return;
   }

   //----------------------------------------------------------------
   private void err(final String str)
   //----------------------------------------------------------------
   {
      System.err.println(str);
      return;
   }
}
