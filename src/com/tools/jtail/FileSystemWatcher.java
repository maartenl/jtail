/*
 * Copyright (C) 2013 maartenl
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
import java.io.RandomAccessFile;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Watches for changes on the file that needs to be tailed. Is only to be used
 * when the switch '-f' is used.</p>
 * <img src="../../../images/FileSystemWatcher.png"/>
 *
 * @startuml
 * interface java.nio.file.WatchService
 * interface java.nio.file.WatchKey
 * interface Watcher
 * class java.nio.file.FileSystem
 * java.nio.file.FileSystem: + {abstract}newWatchService(): java.nio.file.WatchService 
 * abstract class FileSystemWatcher
 * Watcher <|-- FileSystemWatcher
 * FileSystemWatcher --> java.nio.file.WatchService
 * FileSystemWatcher --> java.nio.file.WatchKey
 * FileSystemWatcher --> java.nio.file.FileSystem
 * FileSystemWatcher --> FileInfo
 * FileSystemWatcher : + {abstract} eventDetected(info: FileInfo) throws IOException
 * FileSystemWatcher : +watch(filename: String)
 * FileSystemWatcher : +startWatching() throws IOException
 * @enduml
 *
 * @author maartenl
 */
public abstract class FileSystemWatcher implements Watcher
{

    private static final Logger logger = Logger.getLogger(FileSystemWatcher.class.getName());

    /**
     * Set of directories that need to be watched.
     */
    private final Set<Path> directories = new HashSet<>();

    /**
     * Files to tail.
     */
    private final Set<FileInfo> files = new HashSet<>();

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event)
    {
        return (WatchEvent<T>) event;
    }

    @Override
    public void watch(String filename)
            throws IOException
    {
        logger.entering(FileSystemWatcher.class.getName(), "watch");

        FileInfo fileInfo = new FileInfo(filename);
        files.add(fileInfo);
        directories.add(fileInfo.getDirectory());
        logger.exiting(FileSystemWatcher.class.getName(), "watch");
    }

    private void processEvent(WatchEvent<?> event) throws IOException
    {
        logger.entering(FileSystemWatcher.class.getName(), "processEvent");
        WatchEvent.Kind kind = event.kind();
        // TBD - provide example of how OVERFLOW event is handled
        if (kind == OVERFLOW)
        {
            return;
        }
        // Context for directory entry event is the file name of entry
        WatchEvent<Path> ev = cast(event);
        Path context = ev.context();
        logger.log(Level.FINEST, "processEvent filename {0}", context.toString());
        for (FileInfo info : files)
        {
            if (info.getFile().getFileName().toString().compareTo(context.toString()) == 0)
            {
                if (event.kind() == ENTRY_DELETE)
                {
                    throw new IOException("File " + info.getFilename() + " has been deleted.");
                } else if (event.kind() == ENTRY_CREATE)
                {
                    throw new IOException("File " + info.getFilename() + " has been created. That's weird.");
                } else if (event.kind() == ENTRY_MODIFY)
                {
                    eventDetected(info);
                } else
                {
                    throw new IOException("Unkown event " + event.kind() + " for file " + info.getFilename() + ".");
                }
            }
        }
        logger.exiting(FileSystemWatcher.class.getName(), "processEvent");
    }

    @Override
    public void startWatching() throws IOException
    {
        logger.entering(FileSystemWatcher.class.getName(), "startWatching");
        // if we wish to follow the file, we need to use the watch service in
        // NIO.2 of Java 7.
        try (WatchService watcher = FileSystems.getDefault().newWatchService())
        {
            Set<WatchKey> keys = new HashSet<>();
            for (Path directory : directories)
            {
                logger.log(Level.FINEST, "Watching directory:{0}", directory.toString());
                // registering for file events
                WatchKey key = directory.register(watcher, ENTRY_MODIFY);
                keys.add(key);
            }
            // processing events
            boolean keepGoing = true;
            logger.log(Level.FINEST, "Entering while-loop.");
            while (keepGoing)
            {
                WatchKey foundKey;
                try
                {
                    foundKey = watcher.take();
                } catch (InterruptedException ex)
                {
                    // waiting interrupted, lets' wait some more
                    logger.log(Level.INFO, null, ex);
                    continue;
                }
                List<WatchEvent<?>> events = foundKey.pollEvents();
                // foundKey == key,... it's the only registered key
                for (WatchEvent<?> event : events)
                {
                    processEvent(event);
                }
                keepGoing = foundKey.reset();
            }
            for (WatchKey key : keys)
            {
                key.cancel();
            }

        }
        logger.exiting(FileSystemWatcher.class.getName(), "startWatching");
    }

}
