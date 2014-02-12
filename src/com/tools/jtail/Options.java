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
import java.util.logging.Logger;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * Parsing command line options and provide switches to the rest of the program.
 *
 * @author maartenl
 */
public class Options
{

    private static final Logger logger = Logger.getLogger(Options.class.getName());

    private static OptionSet options;

    private static final OptionParser parser;

    private static final OptionSpec<String> files;

    public static final int DEFAULT_LINES = 10;

    static
    {
        parser = new OptionParser("F")
        {

            
            {
                acceptsAll(Arrays.asList("c", "bytes"), "output the last K bytes; alternatively, use -c +K to output bytes starting with the Kth of each file").withRequiredArg().ofType(String.class);
                acceptsAll(Arrays.asList("f", "follow"), "output appended data as the file grows; -f, --follow, and --follow=descriptor are equivalent").withOptionalArg().ofType(String.class);
                accepts("F", "output appended data as the file grows; -f, --follow, and --follow=descriptor are equivalent");
                acceptsAll(Arrays.asList("n", "lines"), "output the last K lines, instead of the last 10; or use -n +K to output lines starting with the Kth").withRequiredArg().ofType(String.class);
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
        files = parser.nonOptions().ofType(String.class);
    }

    public static void parse(String[] args)
    {
        options = parser.parse(args);
    }

    /**
     * Print the help to System.out.
     *
     * @throws IOException
     */
    public static void printHelp() throws IOException
    {
        parser.printHelpOn(System.out);
    }

    /**
     * Returns the filename at position i in the list of filenames provided on
     * the command line.
     *
     * @param i index
     * @return String indicating the filename.
     */
    public static String getFile(int i)
    {
        return options.valuesOf(files).get(i);
    }

    /**
     * Returns the number of files entered at the command line.
     *
     * @return
     */
    public static int getNumberOfFiles()
    {
        final int size = options.valuesOf(files).size();
        logger.exiting(Options.class.getName(), "getNumberOfFiles", size);
        return size;
    }

    /**
     * Do we need to show the help?
     *
     * @return true, show help, false otherwise.
     */
    public static boolean showHelp()
    {
        return options.has("h");
    }

    /**
     * Do we need to show the version?
     *
     * @return true, show version, false otherwise.
     */
    public static boolean showVersion()
    {
        return options.has("version");
    }

    /**
     * output appended data as the file grows; -f, --follow, and
     * --follow=descriptor are equivalent.
     *
     * @return
     */
    public static boolean follow()
    {
        return options.has("f");
    }

    /**
     * (NOT IMPLEMENTED) with -f, terminate after process ID, PID dies.
     *
     * @return
     */
    public static Long pid()
    {
        if (!options.has("pid"))
        {
            return null;
        }

        return (Long) options.valueOf("pid");
    }

    /**
     * keep trying to open a file even when it is or becomes inaccessible;
     * useful when following by name, i.e., with --follow=name
     *
     * @return
     */
    public static boolean retry()
    {
        return options.has("retry");
    }

    /**
     * Show headers containing the filename when new information becomes
     * available.
     *
     * @return
     */
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

    /**
     * More than 1 filename is provided on the command line.
     *
     * @return
     */
    public static boolean hasMultipleFiles()
    {
        return getNumberOfFiles() > 1;
    }

    /**
     * K may have a multiplier suffix: b 512, kB 1000, K 1024, MB 1000*1000, M
     * 1024*1024, GB 1000*1000*1000, G 1024*1024*1024, and so on for T, P, E, Z,
     * Y. For example "10kB" would be 10,000 bytes.
     *
     * @param argument the argument with (or without) a suffix.
     * @return the Long
     */
    private static Long parseLongArgument(String argument)
    {
        if (argument == null)
        {
            throw new NullPointerException("Argument is null!");
        }
        String rest = argument;
        if (argument.startsWith("+"))
        {
            // remove +
            rest = argument.substring(1);
        }
        //  b 512
        if (rest.endsWith("b"))
        {
            return Long.parseLong(rest.substring(0, rest.length() - 1)) * 512l;
        }
        if (rest.endsWith("b"))
        {
            return Long.parseLong(rest.substring(0, rest.length() - 1)) * 512l;
        }
        // kB 1000
        if (rest.endsWith("kB"))
        {
            return Long.parseLong(rest.substring(0, rest.length() - 2)) * 1000l;
        }
        // K 1024
        if (rest.endsWith("K"))
        {
            return Long.parseLong(rest.substring(0, rest.length() - 1)) * 1024l;
        }
        // MB 1000*1000
        if (rest.endsWith("MB"))
        {
            return Long.parseLong(rest.substring(0, rest.length() - 2)) * 1000l * 1000l;
        }
        // M * 1024 * 1024
        if (rest.endsWith("M"))
        {
            return Long.parseLong(rest.substring(0, rest.length() - 1)) * 1024l * 1024l;
        }
        // GB 1000*1000*1000
        if (rest.endsWith("GB"))
        {
            return Long.parseLong(rest.substring(0, rest.length() - 2)) * 1000l * 1000l * 1000l;
        }
        // G 1024*1024*1024
        if (rest.endsWith("G"))
        {
            return Long.parseLong(rest.substring(0, rest.length() - 1)) * 1024l * 1024l * 1024l;
        }
        // T, 
        // P, 
        // E, 
        // Z,  
        // * Y. 
        return Long.parseLong(rest);
    }

    /**
     * If the first character of K (the number of bytes or lines) is a `+',
     * print beginning with the Kth item from the start of each file, otherwise,
     * print the last K items in the file.
     *
     * @param argument
     * @return
     */
    private static boolean isArgumentWithBeginning(String argument)
    {
        return argument == null ? false : argument.startsWith("+");
    }

    /**
     * output the last K lines, instead of the last 10; or use -n +K to output
     * lines starting with the Kth
     *
     * @return
     */
    public static Long getLines()
    {
        if (!options.has("n"))
        {
            return null;
        }
        return parseLongArgument((String) options.valueOf("n"));
    }

    /**
     * Indicates we wish to follow/see the file from the beginning of
     * the x lines or x bytes, instead of the <i>last</i> x lines or
     * x bytes in the file.
     * @return 
     */
    public static boolean fromBeginning()
    {
        if (options.has("n"))
        {
            return isArgumentWithBeginning((String) options.valueOf("n"));
        }
        if (options.has("c"))
        {
            return isArgumentWithBeginning((String) options.valueOf("c"));
        }
        return false;
    }

    /**
     * output the last K bytes; alternatively, use -c +K to output bytes
     * starting with the Kth of each file
     *
     * @return
     */
    public static Long getBytes()
    {
        if (!options.has("c"))
        {
            return null;
        }
        return parseLongArgument((String) options.valueOf("c"));
    }
}
