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
            System.out.println("jtail: version 1.0, build using jdk7_u45");
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

        Watcher watcher = new Watcher();
        for (String filename : Options.files())
        {
            logger.log(Level.FINER, "Start a watcher with filename {0}.", filename);
            watcher.watch(filename);
        }
        watcher.startWatching();
        logger.exiting(Jtail.class.getName(), "main");
    }
}
