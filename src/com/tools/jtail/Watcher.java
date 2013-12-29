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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Watches for changes on the file that needs to be tailed.
 * @author maartenl
 */
public class Watcher
{

    private Logger logger = Logger.getLogger(Watcher.class.getName());
    
    private static long position = 0;

    private static long size = 0;

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event)
    {
        return (WatchEvent<T>) event;
    }

    public void watch(String filename) throws IOException
    {
        try (WatchService watcher = FileSystems.getDefault().newWatchService())
        {
            Path file = FileSystems.getDefault().getPath(filename);
            Path directory = file.toAbsolutePath().getParent();
            System.out.println("Get current path:" + FileSystems.getDefault().getPath(".").toAbsolutePath());
            System.out.println("Directory:" + directory.toString());
            // registering for file events
            WatchKey key = directory.register(watcher, ENTRY_MODIFY);
            // processing events
            boolean keepGoing = true;
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
                    processEvent(event, file);
                }
                keepGoing = foundKey.reset();
            }
            key.cancel();
        }
    }

    private void processEvent(WatchEvent<?> event, Path file) throws IOException
    {
        logger.entering(Jtail.class.getName(), "processEvent");
        WatchEvent.Kind kind = event.kind();
        // TBD - provide example of how OVERFLOW event is handled
        if (kind == OVERFLOW)
        {
            return;
        }
        // Context for directory entry event is the file name of entry
        WatchEvent<Path> ev = cast(event);
        Path context = ev.context();
        if (file.compareTo(context) == 0)
        {
            if (event.kind() == ENTRY_DELETE)
            {
                throw new IOException("File has been deleted.");
            }
            if (event.kind() == ENTRY_CREATE)
            {
                throw new IOException("File has been created. That's weird.");
            }
            // kind == ENTRY_MODIFY
            tailFile(context);
        }
        logger.exiting(Jtail.class.getName(), "processEvent");
    }

    private void tailFile(Path file) throws IOException
    {
        logger.entering(Jtail.class.getName(), "tailFile");
        byte[] buffer = new byte[1024];
        size = file.toFile().length();
        if (position > size)
        {
            System.out.println("jtail: log.txt: file truncated");
            position = 0;
        }
        try (RandomAccessFile reader = new RandomAccessFile(file.toFile(), "r");)
        {
            reader.seek(position);
            while ((reader.read(buffer))
                    != -1)
            {
                System.out.print(new String(buffer));
            }
            position = reader.getFilePointer();
        }
        size = position;
        logger.exiting(Jtail.class.getName(), "tailFile");
    }

}
