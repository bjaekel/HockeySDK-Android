package net.hockeyapp.android;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;
import java.util.UUID;

import android.util.Log;

/**
 * <h3>Description</h3>
 * 
 * Helper class to catch exceptions. Saves the stack trace
 * as a file and executes callback methods to ask the app for 
 * additional information and meta data (see CrashManagerListener). 
 * 
 * <h3>License</h3>
 * 
 * <pre>
 * Copyright (c) 2009 nullwire aps
 * Copyright (c) 2011-2014 Bit Stadium GmbH
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * </pre>
 *
 * @author Mads Kristiansen
 * @author Glen Humphrey
 * @author Evan Charlton
 * @author Peter Hewitt
 * @author Thomas Dohmke
 **/
public class ExceptionHandler implements UncaughtExceptionHandler {
  private boolean ignoreDefaultHandler = false;
  private CrashManagerListener listener;
  private UncaughtExceptionHandler defaultExceptionHandler;
    /**
     * App identifier from HockeyApp.
     */
    private static String identifier = null;

  public ExceptionHandler(UncaughtExceptionHandler defaultExceptionHandler,
                          CrashManagerListener listener,
                          String inIdentifier,
                          boolean ignoreDefaultHandler) {
    this.defaultExceptionHandler = defaultExceptionHandler;
    this.ignoreDefaultHandler = ignoreDefaultHandler;
    this.listener = listener;
    this.identifier = inIdentifier;
  }

  public void setListener(CrashManagerListener listener) {
    this.listener = listener;
  }
  
  public static void saveException(Throwable exception, CrashManagerListener listener, String inAppIdentifier) {

    try {
      // Create filename from a random uuid
      String filename = UUID.randomUUID().toString();
      String path = Constants.FILES_PATH + "/" + filename + ConstantsFiles.FILE_STACKTRACE;
      Log.d(Constants.TAG, "Writing unhandled exception to: " + path);
      
      // Write the stacktrace to disk
      BufferedWriter write = new BufferedWriter(new FileWriter(path));

      String exceptionString = generateExceptionString(exception, listener);

      write.write(exceptionString);

      write.flush();
      write.close();
      
      if (listener != null) {
        writeValueToFile(limitedString(listener.getUserID()), filename + ConstantsFiles.FILE_USER);
        writeValueToFile(limitedString(listener.getContact()), filename + ConstantsFiles.FILE_CONTACT);
        writeValueToFile(listener.getDescription(), filename + ConstantsFiles.FILE_DESCRIPTION);
      }
      if (inAppIdentifier != null)
      {
          writeValueToFile(inAppIdentifier, filename + ConstantsFiles.FILE_APPIDENTIFIER);
      }
    } 
    catch (Exception another) {
      Log.e(Constants.TAG, "Error saving exception stacktrace!\n", another);
    }
  }

    public static String generateExceptionString(Throwable inException, CrashManagerListener inListener) {
        final Date now = new Date();
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);

        inException.printStackTrace(printWriter);

        String out = "";
        // HockeyApp expects the package name in the first line!
        out += "Package: " + Constants.APP_PACKAGE + "\n";
        out += "Version Code: " + Constants.APP_VERSION + "\n";
        out += "Version Name: " + Constants.APP_VERSION_NAME + "\n";

        if ((inListener == null) || (inListener.includeDeviceData())) {
            out += "Android: " + Constants.ANDROID_VERSION + "\n";
            out += "Manufacturer: " + Constants.PHONE_MANUFACTURER + "\n";
            out += "Model: " + Constants.PHONE_MODEL + "\n";
        }

        if (Constants.CRASH_IDENTIFIER != null && (inListener == null || inListener.includeDeviceIdentifier())) {
            out += "CrashReporter Key: " + Constants.CRASH_IDENTIFIER + "\n";
        }

        out += "Date: " + now + "\n";
        out += "\n";
        out += result.toString();

        return out;
    }

    public void uncaughtException(Thread thread, Throwable exception) {
    if (Constants.FILES_PATH == null) {
      // If the files path is null, the exception can't be stored
      // Always call the default handler instead
      defaultExceptionHandler.uncaughtException(thread, exception);
    }
    else {
      saveException(exception, listener, identifier);

      if (!ignoreDefaultHandler) {
        defaultExceptionHandler.uncaughtException(thread, exception);
      }
      else {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
      }
    }
  }

  private static void writeValueToFile(String value, String filename) {
    try {
      String path = Constants.FILES_PATH + "/" + filename;
      if (value.trim().length() > 0) {
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));
        writer.write(value);
        writer.flush();
        writer.close();
      }
    }
    catch (Exception e) {
    }
  }

  private static String limitedString(String string) {
    if ((string != null) && (string.length() > 255)) {
      string = string.substring(0, 255);
    }
    return string;
  }
}
