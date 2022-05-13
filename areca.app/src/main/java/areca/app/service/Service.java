/*
 * Copyright (C) 2022, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package areca.app.service;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Common base for all services.
 *
 * @author Falko Bräutigam
 */
public abstract class Service {

    private static final Log LOG = LogFactory.getLog( Service.class );

    public abstract String label();
}
