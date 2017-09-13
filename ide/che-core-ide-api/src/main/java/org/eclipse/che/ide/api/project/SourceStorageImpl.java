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
package org.eclipse.che.ide.api.project;

import java.util.Collections;
import java.util.Map;
import org.eclipse.che.api.core.model.project.SourceStorage;

/** @author Vitalii Parfonov */
public class SourceStorageImpl implements SourceStorage {

  private String type;
  private String location;
  private Map<String, String> parameters;

  public SourceStorageImpl(SourceStorage source) {
    type = source.getType();
    location = source.getLocation();
    parameters = source.getParameters();
  }

  public SourceStorageImpl() {}

  @Override
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  @Override
  public Map<String, String> getParameters() {
    if (parameters == null) {
      parameters = Collections.emptyMap();
    }

    return parameters;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }
}
