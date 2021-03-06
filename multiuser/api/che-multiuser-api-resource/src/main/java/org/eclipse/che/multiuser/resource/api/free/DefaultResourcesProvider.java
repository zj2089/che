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
package org.eclipse.che.multiuser.resource.api.free;

import java.util.List;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.multiuser.resource.spi.impl.ResourceImpl;

/**
 * Provides default resources which should be are available for usage by account when admin doesn't
 * override limit by {@link FreeResourcesLimitService}.
 *
 * @author Sergii Leschenko
 */
public interface DefaultResourcesProvider {
  /** Provides default resources are available for usage by account */
  List<ResourceImpl> getResources(String accountId) throws ServerException, NotFoundException;

  /** Returns account type for which this class provides default resources. */
  String getAccountType();
}
