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
package org.eclipse.che.commons.logback;

import javax.annotation.PostConstruct;

/**
 * Sets default UncaughtExceptionHanlder for all threads
 *
 * @author Mihail Kuznyetsov
 */
public class DefaultUncaughtExceptionHandlerInitializer {
  @PostConstruct
  public void init() {
    Thread.setDefaultUncaughtExceptionHandler(LoggingUncaughtExceptionHandler.getInstance());
  }
}
