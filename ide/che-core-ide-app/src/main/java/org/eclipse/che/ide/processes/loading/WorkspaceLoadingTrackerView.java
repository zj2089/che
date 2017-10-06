package org.eclipse.che.ide.processes.loading;

import com.google.gwt.user.client.ui.IsWidget;

/**
 * Created by vetal on 9/29/17.
 */
public interface WorkspaceLoadingTrackerView extends IsWidget {

  void showLoadingStarted();

  /**
   * Step 1. Pull machines images.
   */

  void pullMachine(String machine);

  void setMachineImage(String machine, String image);

  void setMachinePullingProgress(String machine, int percents);

  void setMachinePullingProgress(String machine, String value);

  void setMachinePullingComplete(String machine);

  /**
   * Step 2. Starting workspace runtimes.
   */

  void showStartingWorkspaceRuntimes();

  void addStartWorkspaceRuntime(String machine, String image);

  void setStartWorkspaceRuntimeRunning(String machine);

  /**
   * Step 3. Initializing workspace agents.
   */

  void showInitializingWorkspaceAgents();

  /**
   * Step 4. Workspace started.
   */

  void showWorkspaceStarted();

  /**
   * Workspace is already running
   */

  void showWorkspaceIsAlreadyRunning();

}
