/*
 * Copyright (c) 2015-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
'use strict';
import {CheResourceLimits} from '../../api/che-resource-limits';
import {CheOrganizationActions} from '../../api/che-organization-actions';
import {CheOrganizationRoles} from '../../api/che-organization-roles';
import {CheTeamRoles} from '../../api/che-team-roles';

/**
 * todo
 */
export class ResourcesService implements che.service.IResourcesService {

  getResourceLimits(): che.resources.ICheResourceLimits {
    return CheResourceLimits;
  }

  getOrganizationActions(): che.resources.ICheOrganizationActions {
    return CheOrganizationActions;
  }

  getOrganizationRoles(): che.resources.ICheOrganizationRoles {
    return CheOrganizationRoles;
  }

  getTeamRoles(): che.resources.ICheTeamRoles {
    return CheTeamRoles;
  }

}

