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

package com.tools.jtail;

import java.io.IOException;

/**
 * <p>Interface for watching for changes on the file that needs to be tailed. 
 * Is only to be used
 * when the switch '-f' is used.</p>
 * <img src="../../../images/Watcher.png"/>
 *
 * @startuml
 * interface Watcher
 * abstract class FileSystemWatcher
 * abstract class PollingWatcher
 * Watcher <|-- FileSystemWatcher
 * Watcher <|-- PollingWatcher
 * @enduml
 *
 * @author maartenl
 */
public interface Watcher {

    void eventDetected(FileInfo info) throws IOException;

    void startWatching() throws IOException;

    void watch(String filename) throws IOException;
    
}
