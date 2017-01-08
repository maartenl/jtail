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

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardOpenOption.READ;

/**
 * Creates TailFile implementations depending on the requirements.
 * <img src="../../../images/TailFileFactory.png"/>
 * <br/>
 * Uses non-blocking java nio InputStream/ByteChannels (i.e. file can be deleted while opened here).
 *
 * @author maartenl
 *
 * @startuml TailFileFactory : +{static} createTailFile(info:FileInfo,
 * bytes:Long , lines:Long , fromBeginning:boolean , showFilenames:boolean ):
 * TailFile TailFileBytes <-- TailFileFactory TailFileLinesFromEnd <--
 * TailFileFactory TailFileLinesFromBeginning <-- TailFileFactory interface
 * TailFile TailFile <|-- TailFileBytes TailFile <|-- TailFileLinesFromEnd
 * TailFile <|-- TailFileLinesFromBeginning @enduml @author maartenl
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
        if (info.getPosition() != 0)
        {
            // we're already at a proper position,
            // means we're already tailing this one.
            return new TailFileBytes(info, showFilenames);
        }
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
        private static final String encoding = System.getProperty("file.encoding");
        private static final Charset charset = Charset.forName(encoding);
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
            // We use a ByteBuffer to read (BUFER_SIZE)
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            // Position is set to 0
            buffer.clear();
            if (info.getPosition() > info.getSize())
            {
                out.println("jtail: " + info.getFilename() + ": file truncated");
                info.setPosition(0);
            }
            try (SeekableByteChannel reader = Files.newByteChannel(info.getFile(), EnumSet.of(READ))) {
                reader.position(info.getPosition());
                // While the number of bytes from the channel are > 0
                while(reader.read(buffer)>0) {
                    // after reading into the buffer we have to flip it in order to write it to out.
                    buffer.flip();
                    out.print(charset.decode(buffer));
                    // Prepare the buffer for a new read
                    buffer.clear();
                }
                info.setPosition(reader.position());
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
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(info.getFile()))))
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
        private static final String encoding = System.getProperty("file.encoding");
        private static final Charset charset = Charset.forName(encoding);
        private long lines;

        private TailFileLinesFromEnd(FileInfo info, long lines, boolean showFilenames)
        {
            this.info = info;
            this.showFilenames = showFilenames;
            this.lines = lines;
        }

        @Override
        public void tail(PrintStream out) throws FileNotFoundException, IOException {
            if (showFilenames) {
                out.println("==> " + info.getFilename() + " <==");
            }
            // We use a ByteBuffer to read (BUFFER_SIZE)
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            // Position is set to 0
            buffer.clear();
            int linesRead = 0;
            ArrayDeque<String> totalRead = new ArrayDeque<>();
            if (info.getPosition() > info.getSize()) {
                out.println("jtail: " + info.getFilename() + ": file truncated");
                info.setPosition(0);
            }
            info.setPosition(Math.max(info.getSize() - BUFFER_SIZE, 0));
            try (SeekableByteChannel reader = Files.newByteChannel(info.getFile(), EnumSet.of(READ)))
            {
                do
                {
                    reader.position(info.getPosition());
                    reader.read(buffer);
                    buffer.flip();
                    String s = charset.decode(buffer).toString();
                    buffer.clear();
                    String[] split = s.split("\n");
                    logger.log(Level.FINEST, "split: {0}...{1}:{2}", new Object[]
                    {
                        split[0], split.length, split[split.length - 1]
                    });
                    linesRead += split.length;
                    for (int i = split.length-1; i >= 0 && totalRead.size() < lines; i--)
                    {
                        totalRead.addFirst(split[i]);
                    }
                    if (linesRead > lines)
                    {
                        break;
                    }
                    info.setPosition(info.getPosition() - BUFFER_SIZE);
                } while (info.getPosition() > 0);

                // write it to the output
                totalRead.forEach(out::println);
                // set the position to the end of the file
                info.setPosition(info.getSize());
            }
        }
    }
}
