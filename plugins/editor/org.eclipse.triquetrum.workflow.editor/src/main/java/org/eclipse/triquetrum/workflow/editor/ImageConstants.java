/*******************************************************************************
 * Copyright (c) 2016,2019 iSencia Belgium NV.
 *  
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Erwin De Ley - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.triquetrum.workflow.editor;

public interface ImageConstants {
  String PRE = TriqEditorPlugin.getID() + ".";

  String IMG_COLOR_CHANGE = PRE + "colorChange";
  String IMG_CONFIGURE = PRE + "configure";

  // outline
  String IMG_OUTLINE_TREE = PRE + "outline.tree";
  String IMG_OUTLINE_THUMBNAIL = PRE + "outline.thumbnail";

  // outline tree item icons
  String IMG_ACTOR = PRE + "actor";
  String IMG_COMPOSITE = PRE + "composite";
  String IMG_DIRECTOR = PRE + "director";
  String IMG_INPUTPORT = PRE + "inputport";
  String IMG_OUTPUTPORT = PRE + "outputport";
  String IMG_CONNECTION = PRE + "connection";
  String IMG_PARAMETER = PRE + "parameter";

  String IMG_PAUSE_WORKFLOW = PRE + "pause";
  String IMG_RUN_WORKFLOW = PRE + "run";
  String IMG_STEP_WORKFLOW = PRE + "step";
  String IMG_STOP_WORKFLOW = PRE + "stop";

  String IMG_FOLDER = PRE + "folder";
}
