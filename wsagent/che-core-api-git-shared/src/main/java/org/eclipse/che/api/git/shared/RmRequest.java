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
package org.eclipse.che.api.git.shared;

import java.util.List;
import org.eclipse.che.dto.shared.DTO;

/**
 * Request to remove files.
 *
 * @author andrew00x
 */
@DTO
public interface RmRequest {
  /** @return files to remove */
  List<String> getItems();

  void setItems(List<String> items);

  RmRequest withItems(List<String> items);

  /** @return is RmRequest represents remove from index only */
  boolean isCached();

  void setCached(boolean isCached);

  RmRequest withCached(boolean cached);

  boolean isRecursively();

  void setRecursively(boolean isRecursively);

  RmRequest withRecursively(boolean isRecursively);
}
