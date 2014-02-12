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
        }

        Options.parse(args);

        if (Options.showVersion())
        {
            System.out.println("jtail: version 1.0, build using jdk7_u45");
            System.exit(2);
        }
        if (Options.getNumberOfFiles() != 1 || Options.showHelp())
        {
            System.out.println("Usage: tail [OPTION]... [FILE]...");
            System.out.println("Print the last " + Options.DEFAULT_LINES + " lines of each FILE to standard output.");
            System.out.println("With more than one FILE, precede each with a header giving the file name.");
            System.out.println("");
            System.out.println("Mandatory arguments to long options are mandatory for short options too.");
            Options.printHelp();
            System.out.println("");
            System.out.println("For debugging, try 'java -Djava.util.logging.config.file=logging.properties'.");
            System.out.println("An appropriate logging.properties file is included in the jar.");
            System.exit(1);
        }
        // retrieve the filename from the command line parameters.
        String filename = Options.getFile(0);

        logger.finer("Start the watcher.");
        Watcher watcher = new Watcher();
        watcher.watch(filename);
        logger.exiting(Jtail.class.getName(), "main");
    }
}
