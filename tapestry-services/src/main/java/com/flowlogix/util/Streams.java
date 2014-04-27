package com.flowlogix.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * <a href="http://code.google.com/p/flowlogix/wiki/TLUtil"
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
public class Streams
{
    public static String readString(InputStream strm) throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(strm));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }

        return sb.toString();
    }
}
