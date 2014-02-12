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

import java.io.IOException;
import java.util.Arrays;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * Parsing command line options and provide
 * switches to the rest of the program.
 *
 * @author maartenl
 */
public class Options
{

    private static OptionSet options;

    private static OptionParser parser;

    private static OptionSpec<String> files;

    public static final int DEFAULT_LINES = 10;

    static {
        parser = new OptionParser("F")
        {         
            {
                acceptsAll(Arrays.asList("c", "bytes"), "output the last K bytes; alternatively, use -c +K to output bytes starting with the Kth of each file").withRequiredArg().ofType(Long.class);
                acceptsAll(Arrays.asList("f", "follow"), "output appended data as the file grows; -f, --follow, and --follow=descriptor are equivalent").withOptionalArg().ofType(String.class);
                accepts("F", "output appended data as the file grows; -f, --follow, and --follow=descriptor are equivalent");
                acceptsAll(Arrays.asList("n", "lines"), "output the last K lines, instead of the last 10; or use -n +K to output lines starting with the Kth").withRequiredArg().ofType(Long.class);
                accepts("max-unchanged-stats", "with --follow=name, reopen a FILE which has not changed size after N (default 5) iterations to see if it has been unlinked or renamed (this is the usual case of rotated log files). With inotify, this option is rarely useful. ").withRequiredArg().ofType(Integer.class);
                accepts("pid", "(NOT IMPLEMENTED) with -f, terminate after process ID, PID dies").withRequiredArg().ofType(Long.class);
                acceptsAll(Arrays.asList("q", "quiet", "silent"), "never output headers giving filesnames");
                accepts("retry", "keep trying to open a file even when it is or becomes inaccessible; useful when following by name, i.e., with --follow=name");
                acceptsAll(Arrays.asList("s", "sleep-interval"), "(NOT IMPLEMENTED) with -f, sleep for approximately N seconds (default 1.0) between iterations. With inotify and --pid=P, check process P at least once every N seconds.").withRequiredArg().ofType(Integer.class);
                acceptsAll(Arrays.asList("v", "verbose"), "always output headers giving file names");
                acceptsAll(Arrays.asList("h", "help"), "display this help and exit").forHelp();
                accepts("version", "output version information and exit");
                nonOptions("files to chew on").ofType(String.class).describedAs("input files");
            }
        };
    }

    public static void parse(String[] args)
    {
        options = parser.parse(args);
        files = parser.nonOptions().ofType(String.class);
    }

    public static void printHelp() throws IOException
    {
        parser.printHelpOn(System.out);
    }

    public static String getFile(int i)
    {
        return options.valuesOf(files).get(i);
    }

    public static int getNumberOfFiles()
    {
        return options.valuesOf(files).size();
    }

    public static boolean showHelp()
    {
        return options.has("h");
    }

    public static boolean showVersion()
    {
        return options.has("version");
    }

    public static boolean follow()
    {
        return options.has("f");
    }

    public static Long pid()
    {
        if (!options.has("pid"))
        {
            return null;
        }

        return (Long) options.valueOf("pid");
    }

    public static boolean retry()
    {
        return options.has("retry");
    }

    public static boolean showFilenames()
    {
        // 0 0 -> show
        // 1 0 -> show
        // 0 1 -> hide
        // 1 1 -> ?
        if (options.has("q"))
        {
            if (!options.has("verbose"))
            {
                return false;
            }
            throw new RuntimeException("--verbose and -q are mutually exclusive command line arguments.");
        }
        return true;
    }
}
