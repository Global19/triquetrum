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
package org.eclipse.triquetrum.workflow.editor.palette.ui;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.palette.PaletteEntry;

public class UserLibraryCreateCommand extends Command {
  
  private PaletteTreeNodeEditPart parentPart;
  private PaletteEntry childEntry;
  private EditPart childPart;

  public UserLibraryCreateCommand() {
    super("Create new entry in the User Library");
  }
  
  public void setParent(PaletteTreeNodeEditPart parent) {
    this.parentPart = parent;
  }
  
  public void setChild(PaletteEntry child) {
    this.childEntry = child;
  }

  @Override
  public boolean canExecute() {
    return childEntry != null && parentPart != null;
  }

  @Override
  public void execute() {
    redo();
  }
  
  @Override
  public void redo() {
    childPart = parentPart.addChild(childEntry);
  }

  @Override
  public void undo() {
    parentPart.removeChild(childPart);
  }
}
