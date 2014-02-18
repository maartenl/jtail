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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates TailFile implementations depending on the requirements.
 * <img src="../../../images/TailFileFactory.png"/>
 * @author maartenl
 *
 * @startuml
 * TailFileFactory : +{static} createTailFile(info:FileInfo, bytes:Long , lines:Long , fromBeginning:boolean , showFilenames:boolean ): TailFile
 * TailFileBytes <-- TailFileFactory
 * TailFileLinesFromEnd <-- TailFileFactory
 * TailFileLinesFromBeginning <-- TailFileFactory
 * interface TailFile
 * TailFile <|-- TailFileBytes
 * TailFile <|-- TailFileLinesFromEnd
 * TailFile <|-- TailFileLinesFromBeginning
 * @enduml
 * @author maartenl
 */
public class TailFileFactory
{

    /**
     * Buffer size of 4k.
     */
    private static final int BUFFER_SIZE = 4096;

    private static final Logger logger = Logger.getLogger(TailFileFactory.class.getName());

    public static TailFile createTailFile(FileInfo info, Long bytes, Long lines, boolean fromBeginning, boolean showFilenames)
    {
        if (fromBeginning)
        {
            if (bytes != null)
            {
                logger.finest("creating TailFileBytes - from beginning");
                info.setPosition(bytes);
                return new TailFileBytes(info, showFilenames);
            }
            if (lines != null)
            {
                logger.finest("creating TailFileLinesFromBeginning");
                return new TailFileLinesFromBeginning(info, lines, showFilenames);
            }
            logger.finest("creating nothing - from beginning");
            return null;
        }
        // not from the beginning
        if (bytes != null)
        {
            logger.finest("creating TailFileBytes from end");
            info.setPosition(info.getSize() - bytes);
            return new TailFileBytes(info, showFilenames);
        }
        if (lines != null)
        {
            logger.log(Level.FINEST, "creating TailFileLinesFromEnd {0} {1}", new Object[]
            {
                lines, showFilenames
            });
            return new TailFileLinesFromEnd(info, lines, showFilenames);
        }
        logger.finest("creating nothing - from end");
        return null;
    }

    private static class TailFileBytes implements TailFile
    {

        private final FileInfo info;

        private final boolean showFilenames;

        private TailFileBytes(FileInfo info, boolean showFilenames)
        {
            this.info = info;
            this.showFilenames = showFilenames;
        }

        @Override
        public void tail(PrintStream out) throws FileNotFoundException, IOException
        {
            if (showFilenames)
            {
                out.println("==> " + info.getFilename() + " <==");
            }
            byte[] buffer = new byte[BUFFER_SIZE];
            if (info.getPosition() > info.getSize())
            {
                out.println("jtail: " + info.getFilename() + ": file truncated");
                info.setPosition(0);
            }
            try (RandomAccessFile reader = new RandomAccessFile(info.getFile().toFile(), "r");)
            {
                reader.seek(info.getPosition());
                while ((reader.read(buffer))
                        != -1)
                {
                    out.print(new String(buffer));
                }
                info.setPosition(reader.getFilePointer());
            }
        }
    }

    private static class TailFileLinesFromBeginning implements TailFile
    {

        private final FileInfo info;

        private final boolean showFilenames;

        private long lines;

        private TailFileLinesFromBeginning(FileInfo info, long lines, boolean showFilenames)
        {
            this.info = info;
            this.showFilenames = showFilenames;
            this.lines = lines;
        }

        @Override
        public void tail(PrintStream out) throws FileNotFoundException, IOException
        {
            if (showFilenames)
            {
                out.println("==> " + info.getFilename() + " <==");
            }
            int currentLine = 0;
            boolean found = false;
            try (BufferedReader reader = new BufferedReader(new FileReader(info.getFile().toFile())))
            {
                String lineRead;
                while ((lineRead = reader.readLine()) != null)
                {
                    if (currentLine >= lines)
                    {
                        out.print(lineRead);
                        found = true;
                    }
                    currentLine++;
                }
                if (!found)
                {
                    out.println("jtail: " + info.getFilename() + ": file truncated");
                    info.setPosition(0);
                }
            }
            info.setPosition(info.getSize());
        }
    }

    private static class TailFileLinesFromEnd implements TailFile
    {

        private final FileInfo info;

        private final boolean showFilenames;

        private long lines;

        private TailFileLinesFromEnd(FileInfo info, long lines, boolean showFilenames)
        {
            this.info = info;
            this.showFilenames = showFilenames;
            this.lines = lines;
        }

        @Override
        public void tail(PrintStream out) throws FileNotFoundException, IOException
        {
            if (showFilenames)
            {
                out.println("==> " + info.getFilename() + " <==");
            }
            int linesRead = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            List<String> totalRead = new LinkedList<>();
            if (info.getPosition() > info.getSize())
            {
                out.println("jtail: " + info.getFilename() + ": file truncated");
                info.setPosition(0);
            }
            info.setPosition(info.getSize() - BUFFER_SIZE);
            try (RandomAccessFile reader = new RandomAccessFile(info.getFile().toFile(), "r");)
            {
                do
                {
                    reader.seek(info.getPosition());
                    reader.read(buffer);
                    String s = new String(buffer);
                    String[] split = s.split("\n");
                    logger.log(Level.FINEST, "split: {0}...{1}:{2}", new Object[]{split[0], split.length, split[split.length - 1]});
                    if (totalRead.isEmpty())
                    {
                        linesRead += split.length;
                        for (int i = 0; i < split.length; i++)
                        {
                            totalRead.add(i, split[i]);
                        }
                    } else
                    {
                        totalRead.set(0, split[split.length - 1] + totalRead.get(0));
                        for (int i = 0; i < split.length - 1; i++)
                        {
                            totalRead.add(i, split[i]);
                        }
                    }
                    if (linesRead > lines)
                    {
                        break;
                    }
                    info.setPosition(info.getPosition() - BUFFER_SIZE);
                } while (info.getPosition() != 0);
                // info.setPosition(reader.getFilePointer());

                // write it to the output
                for (long i = lines - 1; i >= 0; i--)
                {
                    if (i <= totalRead.size() - 1)
                    {
                        out.println(totalRead.get((int) (totalRead.size() - 1 - i)));
                    }
                }
                info.setPosition(info.getSize());
            }
        }
    }
}
