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
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * JTail is my attempt at an implementation of the unix tail command in Java
 * using the new NIO.2 classes. Main start class.
 *
 * @author maartenl
 */
public class Jtail
{

    private static Logger logger = Logger.getLogger(Jtail.class.getName());

    /**
     * @param args the command line arguments
     * @throws IOException if something with wrong with the fileIO. For example,
     * the file does not exist, or the watch service on the file borks.
     */
    public static void main(String[] args) throws IOException
    {        
        logger.entering(Jtail.class.getName(), "main");

        if (args.length != 1)
        {
            System.out.println("jtail <filename>");
            System.exit(1);
        }
        String filename = args[0];
        Watcher watcher = new Watcher();
        watcher.watch(filename);
        logger.exiting(Jtail.class.getName(), "main");
    }

}
