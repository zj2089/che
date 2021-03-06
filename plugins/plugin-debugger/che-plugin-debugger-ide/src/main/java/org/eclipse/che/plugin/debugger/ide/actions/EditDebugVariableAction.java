/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.plugin.debugger.ide.actions;

import static java.util.Collections.singletonList;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

import com.google.inject.Inject;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.plugin.debugger.ide.DebuggerLocalizationConstant;
import org.eclipse.che.plugin.debugger.ide.DebuggerResources;
import org.eclipse.che.plugin.debugger.ide.debug.DebuggerPresenter;
import org.eclipse.che.plugin.debugger.ide.debug.dialogs.changevalue.ChangeValuePresenter;
import org.eclipse.che.plugin.debugger.ide.debug.dialogs.watch.expression.edit.EditWatchExpressionPresenter;

/**
 * Action which allows change value of selected node in the debugger tree.
 *
 * @author Mykola Morhun
 * @author Oleksandr Andriienko
 */
public class EditDebugVariableAction extends AbstractPerspectiveAction {

  private final ChangeValuePresenter changeValuePresenter;
  private final DebuggerPresenter debuggerPresenter;
  private final EditWatchExpressionPresenter editWatchExpressionPresenter;

  @Inject
  public EditDebugVariableAction(
      DebuggerLocalizationConstant locale,
      DebuggerResources resources,
      ChangeValuePresenter changeValuePresenter,
      EditWatchExpressionPresenter editWatchExpressionPresenter,
      DebuggerPresenter debuggerPresenter) {
    super(
        singletonList(PROJECT_PERSPECTIVE_ID),
        locale.editDebugVariable(),
        locale.editDebugVariableDescription(),
        null,
        resources.editDebugNode());
    this.changeValuePresenter = changeValuePresenter;
    this.debuggerPresenter = debuggerPresenter;
    this.editWatchExpressionPresenter = editWatchExpressionPresenter;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (debuggerPresenter.getSelectedWatchExpression() != null) {
      editWatchExpressionPresenter.showDialog();
    } else if (debuggerPresenter.getSelectedVariable() != null) {
      changeValuePresenter.showDialog();
    }
  }

  @Override
  public void updateInPerspective(ActionEvent event) {
    event
        .getPresentation()
        .setEnabled(
            debuggerPresenter.getSelectedWatchExpression() != null
                || debuggerPresenter.getSelectedVariable() != null);
  }
}
