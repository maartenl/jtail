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
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * The old fashion way of checking file changes. By periodically querying the
 * filesystem.</p>
 *
 * @author maartenl
 */
public abstract class PollingWatcher implements Watcher
{

    private static final Logger logger = Logger.getLogger(PollingWatcher.class.getName());

    /**
     * Set of directories that need to be watched.
     */
    private final Set<Path> directories = new HashSet<>();

    /**
     * Files to trail.
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
        logger.entering(PollingWatcher.class.getName(), "watch");

        FileInfo fileInfo = new FileInfo(filename);
        files.add(fileInfo);
        directories.add(fileInfo.getDirectory());
        logger.exiting(PollingWatcher.class.getName(), "watch");
    }

    @Override
    public void startWatching() throws IOException
    {
        logger.entering(PollingWatcher.class.getName(), "startWatching");

        // processing events
        boolean keepGoing = true;
        logger.log(Level.FINEST, "Entering while-loop.");
        while (keepGoing)
        {
            try
            {
                Thread.sleep(Options.sleep() * 1000l);
                for (FileInfo info : files)
                {
                    if (info.getPosition() != info.getSize())
                    {
                        eventDetected(info);
                    }
                }
            } catch (InterruptedException ex)
            {
                // waiting interrupted, lets' wait some more
                logger.log(Level.INFO, null, ex);
            }
        }
        logger.exiting(PollingWatcher.class
                .getName(), "startWatching");
    }

}
