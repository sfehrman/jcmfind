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

/**
 * Public interface for executable programs
 * @author Scott Fehrman
 */
//===================================================================
public interface ExecuteIF
//===================================================================
{
   public static final String PROP_SRCH_PATH = "srch.path"; // String
   public static final String PROP_SRCH_TYPE_CLASS = "srch.type.class"; // boolean
   public static final String PROP_SRCH_TYPE_METHOD = "srch.type.method"; // boolean
   public static final String PROP_SRCH_TYPE_PACKAGE = "srch.type.package"; // boolean
   public static final String PROP_SRCH_VALUE = "srch.value"; // String
   public static final String PROP_SRCH_OPERATOR = "srch.oper"; // String (default=contains)
   public static final String PROP_DISP_DEBUG = "disp.debug"; // boolean (default=true)
   public static final String PROP_DISP_ERROR = "disp.error"; // boolean (default=true)
   public static final String PROP_DISP_RESULTS = "disp.results"; // boolean (default=true)
   public static final String PROP_DISP_VERBOSE = "disp.verbose"; // boolean (default=false)

   public void run(String[] args) throws Exception;
}
