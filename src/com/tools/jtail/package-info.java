/*
 * Copyright (C) 2014 maartenl
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * The only package (for now). Contains all required class files for
 * the jtail application.
 * <img src="../../../images/package-info.png"/>
 * <br/><i>Component Diagram.</i>
 * @author maartenl
 *
 * @startuml
 * [Jtail] --> [jopt-simple]
 * [Jtail] --> [NIO.2]
 * [plantuml.jar]
 * note left of [NIO.2] : For watching the \nFileSystem for changes
 * note right of [jopt-simple] : For interpreting \ncmdline options
 * note right of [plantuml.jar] : For generating\njavadocs only
 *
 * @enduml
 * @author maartenl
 */
package com.tools.jtail;
