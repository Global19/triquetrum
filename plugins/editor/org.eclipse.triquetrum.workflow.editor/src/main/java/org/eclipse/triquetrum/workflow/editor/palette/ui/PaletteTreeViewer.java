/*******************************************************************************
 *  Copyright (c) 2016 iSencia Belgium NV.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     Erwin De Ley - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.triquetrum.workflow.editor.palette.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.TreeEditPart;
import org.eclipse.gef.editparts.RootTreeEditPart;
import org.eclipse.gef.palette.PaletteContainer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.triquetrum.workflow.editor.TriqEditorPlugin;
import org.eclipse.triquetrum.workflow.editor.palette.PaletteTreeNode;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.event.Event;

/**
 * Try to merge features of {@link TreeViewer} into the {@link PaletteViewer} and use a {@link FilteredTree} with that
 * as well.
 *
 */
public class PaletteTreeViewer extends PaletteViewer {
  private boolean ignore = false;
  private FigureCanvas figCanvas;

  public PaletteTreeViewer() {
    super();
    setKeyHandler(new GraphicalViewerKeyHandler(this));
    setEditPartFactory(new PaletteTreeEditPartFactory());
    addDragSourceListener(new TreeViewerTransferDragListener(this));
  }

  // hack to use a dummy canvas to please the graphical base classes
  @Override
  protected FigureCanvas getFigureCanvas() {
    if (figCanvas == null) {
      figCanvas = new FigureCanvas(getControl().getParent());
    }
    return figCanvas;
  }

  @Override
  protected void createDefaultRoot() {
    try {
      setRootEditPart(new RootTreeEditPart());
    } catch (ClassCastException e) {
      // to catch the wrong cast to a GraphicalEditPart in the GraphicalViewerImpl.setRootEditPart implementation
    }
  }

  @Override
  public void setPaletteRoot(PaletteRoot root) {
    super.setPaletteRoot(root);
    ((FilteredTree) getControl()).getViewer().setInput(getRootEditPart().getContents());
  }

  /**
   * Creates the default tree and sets it as the control. The default styles will show scrollbars as needed, and allows
   * for multiple selection.
   * <p>
   * Doesn't use the default createControl method name, as that one is made final in the ScrollingGraphicalViewer base
   * class...
   * </p>
   *
   * @param parent
   *          The parent for the Tree
   * @return the control
   */
  public Control createTreeControl(Composite parent) {
    PatternFilter filter = new PatternFilter();
    FilteredTree tree = new FilteredTree(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL, filter, true);
    tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    tree.getViewer().setContentProvider(new PaletteTreeProvider(tree.getViewer()));
    tree.getViewer().setLabelProvider(new PaletteLabelProvider(this));
    setControl(tree);
    return tree;
  }

  protected Tree getTreeControl() {
    return getTreeViewer().getTree();
  }

  protected org.eclipse.jface.viewers.TreeViewer getTreeViewer() {
    final FilteredTree filteredTree = (FilteredTree) getControl();
    return filteredTree.getViewer();
  }

  /**
   * @see org.eclipse.gef.EditPartViewer#findObjectAtExcluding(Point, Collection, EditPartViewer.Conditional)
   */
  @Override
  public EditPart findObjectAtExcluding(Point pt, Collection exclude, Conditional condition) {
    if (getControl() == null)
      return null;

    final Tree tree = getTreeControl();
    Rectangle area = tree.getClientArea();
    if (pt.x < area.x || pt.y < area.y || pt.x >= area.x + area.width || pt.y >= area.y + area.height)
      return null;

    EditPart result = null;
    TreeItem tie = tree.getItem(new org.eclipse.swt.graphics.Point(pt.x, pt.y));

    if (tie != null) {
      result = (EditPart) tie.getData();
    } else if (tree.getData() instanceof EditPart) {
      result = (EditPart) tree.getData();
    }
    while (result != null) {
      if ((condition == null || condition.evaluate(result)) && !exclude.contains(result))
        return result;
      result = result.getParent();
    }
    return null;
  }

  /**
   * @see org.eclipse.gef.ui.parts.AbstractEditPartViewer#fireSelectionChanged()
   */
  @Override
  protected void fireSelectionChanged() {
    super.fireSelectionChanged();
    showSelectionInTree();
  }

  /**
   * "Hooks up" a Control, i.e. sets it as the control for the RootTreeEditPart, adds necessary listener for proper
   * operation, etc.
   */
  @Override
  protected void hookControl() {
    if (getControl() == null)
      return;

    final Tree tree = getTreeControl();
    tree.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        TreeItem[] ties = tree.getSelection();
        Object newSelection[] = new Object[ties.length];
        for (int i = 0; i < ties.length; i++)
          newSelection[i] = ties[i].getData();
        ignore = true;
        setSelection(new StructuredSelection(newSelection));
        ignore = false;
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }
    });
    TreeEditPart tep = (TreeEditPart) getRootEditPart();
    tep.setWidget(tree);
    try {
      super.hookControl();
    } catch (ClassCastException e) {
      // to catch the wrong cast to graphical widget etc in the super-classes
    }

    hookContextMenu();
  }

  /**
   * Hooks a context menu manager to a selected tree node and adds the relevant actions for the selected node type.
   */
  private void hookContextMenu() {
    MenuManager menuMgr = new MenuManager("#PopupMenu");
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        ISelection selection = getSelection();
        if (selection instanceof IStructuredSelection) {
          IStructuredSelection treeSelection = (IStructuredSelection) selection;
          if (treeSelection.size() == 1) {
            Object selObj = treeSelection.getFirstElement();
            if (selObj instanceof PaletteTreeNodeEditPart) {
              PaletteTreeNodeEditPart sel = (PaletteTreeNodeEditPart) selObj;
              if ("User Library".equals(((PaletteTreeNode) sel.getModel()).getLabel())) {
                manager.add(new AddFolderAction(sel));
              }
            } else if (selObj instanceof PaletteEntryEditPart) {
              PaletteEntryEditPart sel = (PaletteEntryEditPart) selObj;
              if (sel.getParent() != null && (sel.getParent() instanceof PaletteTreeNodeEditPart)) {
                if ("User Library".equals(((PaletteTreeNode) sel.getParent().getModel()).getLabel())) {
                  manager.add(new DeleteAction(sel));
                }
              }
            }
            manager.add(new Separator());
            manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
          }
        }
      }
    });
    Menu menu = menuMgr.createContextMenu(getTreeControl());
    getTreeControl().setMenu(menu);
    // getSite().registerContextMenu(menuMgr, viewer);
  }

  /**
   * @see org.eclipse.gef.ui.parts.AbstractEditPartViewer#reveal(org.eclipse.gef.EditPart)
   */
  @Override
  public void reveal(EditPart part) {
    if (!(part instanceof TreeEditPart))
      return;
    TreeEditPart treePart = (TreeEditPart) part;
    final Tree tree = getTreeControl();
    Widget widget = treePart.getWidget();
    if (widget instanceof TreeItem)
      tree.showItem((TreeItem) widget);
  }

  /**
   * Creates or disposes a DragSource as needed, and sets the supported transfer types. Clients should not need to call or
   * override this method.
   */
  @Override
  protected void refreshDragSourceAdapter() {
    if (getControl() == null)
      return;
    if (getDelegatingDragAdapter().isEmpty())
      setDragSource(null);
    else {
      if (getDragSource() == null)
        setDragSource(new DragSource(getTreeControl(), DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK));
      getDragSource().setTransfer(getDelegatingDragAdapter().getTransfers());
    }
  }

  /**
   * Creates or disposes a DropTarget as needed, and sets the supported transfer types. Clients should not need to call or
   * override this method.
   */
  @Override
  protected void refreshDropTargetAdapter() {
    if (getControl() == null)
      return;
    if (getDelegatingDropAdapter().isEmpty())
      setDropTarget(null);
    else {
      if (getDropTarget() == null)
        setDropTarget(new DropTarget(getTreeControl(), DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK));
      getDropTarget().setTransfer(getDelegatingDropAdapter().getTransfers());
    }
  }

  private void showSelectionInTree() {
    if (ignore || getControl() == null || getControl().isDisposed())
      return;
    List selection = getSelectedEditParts();
    final Tree tree = getTreeControl();
    List<TreeEditPart> treeParts = new ArrayList<>();
    for (int i = 0; i < selection.size(); i++) {
      TreeEditPart part = (TreeEditPart) selection.get(i);
      if (part.getWidget() instanceof TreeItem)
        treeParts.add(part);
    }
    TreeItem[] treeItems = new TreeItem[treeParts.size()];
    for (int i = 0; i < treeParts.size(); i++) {
      TreeEditPart part = treeParts.get(i);
      treeItems[i] = (TreeItem) part.getWidget();
    }
    tree.setSelection(treeItems);
  }

  /**
   * Unhooks a control so that it can be reset. This method deactivates the contents, removes the Control as being the
   * Control of the RootTreeEditPart, etc. It does not remove the listeners because it is causing errors, although that
   * would be a desirable outcome.
   */
  @Override
  protected void unhookControl() {
    if (getControl() == null)
      return;
    super.unhookControl();
    // Ideally, you would want to remove the listeners here
    TreeEditPart tep = (TreeEditPart) getRootEditPart();
    tep.setWidget(null);
  }

  private class AddFolderAction extends Action {
    private PaletteTreeNodeEditPart selectedNode;

    /**
     * @param selectedNode
     */
    public AddFolderAction(PaletteTreeNodeEditPart selectedNode) {
      this.selectedNode = selectedNode;
      setText("Create folder");
      setToolTipText("Creates a new subfolder in the user library");
      // setImageDescriptor(TriqEditorPlugin.getImageDescriptor("icons/delete.gif"));
    }

    @Override
    public void run() {
      PaletteTreeNode entry = (PaletteTreeNode) selectedNode.getModel();
      String libraryName = entry.getLabel();

      AddFolderToUserLibraryDialog dialog = new AddFolderToUserLibraryDialog(getTreeControl().getShell());
      dialog.setBlockOnOpen(true);
      int dialogReturnCode = dialog.open();
      if (Dialog.OK == dialogReturnCode) {
        Map<String, String> properties = new HashMap<>();
        String folderName = dialog.folderName;
        properties.put("displayName", folderName);
        properties.put("libraryName", libraryName);

        Event event = new Event("org/eclipse/triquetrum/workflow/userlibrary/add", properties);
        try {
          TriqEditorPlugin.getDefault().getEventAdminService().sendEvent(event);
          PaletteTreeNode folderTreeNode = new PaletteTreeNode(folderName);
          selectedNode.addChild(folderTreeNode);
          entry.add(folderTreeNode);
        } catch (NullPointerException e) {
          StatusManager.getManager().handle(new Status(IStatus.ERROR, TriqEditorPlugin.getID(),
              "Event bus not available, impossible to trigger a folder addition event for the user library."), StatusManager.BLOCK);
        }
      }
    }
  }

  private class DeleteAction extends Action {
    private PaletteEntryEditPart selectedNode;

    /**
     * @param selectedNode
     */
    public DeleteAction(PaletteEntryEditPart selectedNode) {
      this.selectedNode = selectedNode;
      setText("Delete");
      setToolTipText("Deletes the selected element");
      // setImageDescriptor(TriqEditorPlugin.getImageDescriptor("icons/delete.gif"));
    }

    @Override
    public void run() {
      PaletteEntry entry = (PaletteEntry) selectedNode.getModel();
      String modelName = entry.getLabel();
      String modelClass = entry.getDescription();
      String elementType = "CompositeActor";

      Map<String, String> properties = new HashMap<>();
      properties.put("displayName", modelName);
      properties.put("class", modelClass);
      properties.put("type", elementType);

      Event event = new Event("org/eclipse/triquetrum/workflow/userlibrary/delete", properties);
      try {
        TriqEditorPlugin.getDefault().getEventAdminService().sendEvent(event);
        // The palette's model consists of 2 layers : palette entry editparts that have palette entries as model.
        // And way down there's still the Ptolemy UserLibrary as well.
        // Getting all models synchronized via listeners would be the cleanest, but couldn't get it to work.
        // (org.eclipse.gef.editparts.AbstractEditPart.refreshChildren() always caused errors in reorderChild()
        // because of a null parent somewhere. Too complex for me to understand...)
        // So we do all the deletes ourselves in here...
        ((PaletteTreeNodeEditPart)selectedNode.getParent()).removeChild(selectedNode);
        PaletteContainer container = entry.getParent();
        if(container!=null) {
          container.remove(entry);
        }
      } catch (NullPointerException e) {
        StatusManager.getManager().handle(
            new Status(IStatus.ERROR, TriqEditorPlugin.getID(), "Event bus not available, impossible to trigger a delete event for the user library."),
            StatusManager.BLOCK);
      }
    }
  }

    private static class AddFolderToUserLibraryDialog extends Dialog {
      private Text folderNameField;

      String folderName = "new";

      protected AddFolderToUserLibraryDialog(Shell parentShell) {
        super(parentShell);
        setShellStyle(SWT.RESIZE | SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
      }

      @Override
      protected org.eclipse.swt.graphics.Point getInitialSize() {
        return new org.eclipse.swt.graphics.Point(300, 120);
      }

      @Override
      protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Add folder to User Library");
      }

      // TODO add Ok disable/enable depending on text field contents
      @Override
      protected Control createDialogArea(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        container.setLayout(gridLayout);
        container.setLayoutData(
            new org.eclipse.swt.layout.GridData(org.eclipse.swt.layout.GridData.HORIZONTAL_ALIGN_FILL | org.eclipse.swt.layout.GridData.GRAB_HORIZONTAL));
        container.setFont(parent.getFont());

        final Label folderNameBoxLabel = new Label(container, SWT.NONE);
        final org.eclipse.swt.layout.GridData folderNameBoxLabelLayout = new org.eclipse.swt.layout.GridData(
            org.eclipse.swt.layout.GridData.HORIZONTAL_ALIGN_END);
        folderNameBoxLabel.setLayoutData(folderNameBoxLabelLayout);
        folderNameBoxLabel.setText("Folder name:");

        folderNameField = new Text(container, SWT.BORDER);
        folderNameField.setLayoutData(new org.eclipse.swt.layout.GridData(org.eclipse.swt.layout.GridData.FILL_HORIZONTAL));
        folderNameField.setText(folderName);

        return container;
      }

      @Override
      protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.OK_ID) {
          folderName = folderNameField.getText().trim();
        } else {
          folderName = null;
        }
        super.buttonPressed(buttonId);
      }
    }
}
