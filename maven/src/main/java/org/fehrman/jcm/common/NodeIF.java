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
 *
 * @author Scott Fehrman
 */
//===================================================================
public interface NodeIF
//===================================================================
{

   public enum Type
   {

      FOLDER, ARCHIVE, PACKAGE, CLASS, METHOD
   }

   public String getName();

   public Type getType();
   
   public NodeIF copy();
}
