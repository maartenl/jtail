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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A fairly simple data object, with some additional file operations.
 * <img src="../../../images/FileInfo.png"/>
 * @author maartenl
 *
 * @startuml
 * FileInfo : -filename: String
 * FileInfo : -file: Path
 * FileInfo : -position: long
 * FileInfo : +FileInfo(filename: String)
 * FileInfo : +getSize(): Long 
 * FileInfo : +getDirectory(): Path 
 * FileInfo : +getPosition(): long 
 * FileInfo : +setPosition(position: long)
 * FileInfo : +getFile(): Path 
 * FileInfo : +getFilename(): String 
 * @enduml
 * @author maartenl
 */
public class FileInfo
{

    private final String filename;
    
    private Path file;
    
    private long position;

    public FileInfo(String filename) throws IOException
    {
        this.filename = filename;
        file = Paths.get(filename);
        // validation
        File myFile = file.toFile();
        if (!myFile.exists())
        {
            throw new IOException("File " + filename + " does not exist.");
        }
        position = 0;
    }

    public Long getSize()
    {
        return file.toFile().length();
    }

    public Path getDirectory()
    {
        return file.getParent();
    }

    public long getPosition()
    {
        return position;
    }

    /**
     * Sets the position. If position is invalid, will be made valid.
     * @param position 
     */
    public void setPosition(long position)
    {
        if (position > getSize())
        {
            position = getSize();
        }
        if (position < 0)
        {
            position = 0;
        }
        this.position = position;
    }

    public Path getFile()
    {
        return file;
    }

    public String getFilename()
    {
        return filename;
    }
}
