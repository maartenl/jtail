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
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.io.RandomAccessFile;
import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JTail is my attempt at an implementation of the unix tail command in Java
 * using the new NIO.2 classes. Main start class.
 *
 * @author maartenl
 */
public class Jtail
{

    private static final Logger logger = Logger.getLogger(Jtail.class.getName());

    private static long position = 0;

    private static long size = 0;

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event)
    {
        return (WatchEvent<T>) event;
    }

    /**
     * @param args the command line arguments
     * @throws IOException if something with wrong with the fileIO. For example,
     * the file does not exist, or the watch service on the file borks.
     */
    public static void main(String[] args) throws IOException
    {
        ConsoleHandler handler = new ConsoleHandler();
        final Level logLevel = Level.OFF; // Level.ALL;
        handler.setLevel(logLevel);
        logger.addHandler(handler);
        logger.setLevel(logLevel);

        logger.entering(Jtail.class.getName(), "main");
        if (args.length != 1)
        {
            System.out.println("jtail <filename>");
            System.exit(1);
        }
        String filename = args[0];
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
        logger.exiting(Jtail.class.getName(), "main");
    }

    private static void processEvent(WatchEvent<?> event, Path file) throws IOException
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

    private static void tailFile(Path file) throws IOException
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
