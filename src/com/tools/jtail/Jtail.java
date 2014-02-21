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
import java.util.ArrayList;
import java.util.List;
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

    private static final int EXIT_VERSION = 2;

    private static final int EXIT_HELP = 1;

    public static final String VERSION = "1.1";

    private static void runTail(FileInfo info) throws IOException
    {
        logger.entering(FileSystemWatcher.class.getName(), "runTail");
        TailFile tailFile = TailFileFactory.createTailFile(info, Options.getBytes(), Options.getLines(), Options.fromBeginning(), Options.showFilenames());
        tailFile.tail(System.out);
        logger.exiting(FileSystemWatcher.class.getName(), "runTail");
    }

    /**
     * @param args the command line arguments
     * @throws IOException if something with wrong with the fileIO. For example,
     * the file does not exist, or the watch service on the file borks.
     */
    public static void main(String[] args) throws IOException
    {
        logger.entering(Jtail.class.getName(), "main");
        if (logger.isLoggable(Level.FINER))
        {
            StringBuilder buffer = new StringBuilder("commandline: jtail ");
            for (String string : args)
            {
                buffer.append(string).append(" ");
            }
            logger.finest(buffer.toString());
            logger.log(Level.FINEST, "Get current path:{0}", FileSystems.getDefault().getPath(".").toAbsolutePath());
        }

        Options.parse(args);

        if (Options.showVersion())
        {
            System.out.println("jtail: version " + VERSION + ", build using jdk7_u45");
            System.exit(EXIT_VERSION);
        }
        if (Options.getNumberOfFiles() == 0 || Options.showHelp())
        {
            if (Options.getNumberOfFiles() == 0)
            {
                logger.log(Level.FINEST, "no files found.");
            }
            System.out.println("Usage: tail [OPTION]... [FILE]...");
            System.out.println("Print the last " + Options.DEFAULT_LINES + " lines of each FILE to standard output.");
            System.out.println("With more than one FILE, precede each with a header giving the file name.");
            System.out.println("");
            System.out.println("Mandatory arguments to long options are mandatory for short options too.");
            Options.printHelp();
            System.out.println("");
            System.out.println("For debugging, try 'java -Djava.util.logging.config.file=logging.properties'.");
            System.out.println("An appropriate logging.properties file is included in the jar.");
            System.exit(EXIT_HELP);
        }
        List<FileInfo> fileInfos = new ArrayList<>();
        for (String filename : Options.files())
        {
            FileInfo info = new FileInfo(filename);
            TailFile tailFile = TailFileFactory.createTailFile(info, Options.getBytes(), Options.getLines(), Options.fromBeginning(), Options.showFilenames());
            tailFile.tail(System.out);
            fileInfos.add(info);
        }
        if (!Options.follow())
        {
            return;
        }
        logger.log(Level.FINER, "Create watcher");
        Watcher watcher = (Options.usePolling() ? new PollingWatcher()
        {

            @Override
            public void eventDetected(FileInfo info) throws IOException
            {
                runTail(info);
            }

        } : new FileSystemWatcher()
        {

            @Override
            public void eventDetected(FileInfo info) throws IOException
            {
                runTail(info);
            }
        });
        for (FileInfo info : fileInfos)
        {
            logger.log(Level.FINER, "Watch filename {0}.", info.getFilename());
            watcher.watch(info);
        }
        watcher.startWatching();
        logger.exiting(Jtail.class.getName(), "main");
    }
}
