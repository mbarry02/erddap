/* 
 * TestSSR Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.PixelGrabber;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Properties;

/**
 * This is a Java program to test all of the methods in SSR.
 *
 * @author Robert Simons  bob.simons@noaa.gov  January 2005
 */
public class TestSSR {

    static String classPath = String2.getClassPath();

    /**
     * Run all of the tests which are operating system independent.
     */
    public static void runNonUnixTests() throws Throwable {
        String sar[];

        String2.log("\n*** TestSSR"); 
        String2.log("This must be run in a command line window so passwords can be entered!");

        /*
        //sendSoap
        String soap = SSR.sendSoap(
            "http://services.xmethods.net:80/soap/servlet/rpcrouter",
            "    <ns1:getTemp xmlns:ns1=\"urn:xmethods-Temperature\"\n" +
            "      SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "        <zipcode xsi:type=\"xsd:string\">93950</zipcode>\n" +
            "     </ns1:getTemp>\n",
            "");
        String2.log(soap);
        System.exit(0);
        */

        //percentDecode(String query) 
        Test.ensureEqual(SSR.percentEncode(       "+ :q*~?=&%"), "%2B+%3Aq*%7E%3F%3D%26%25", "percentEncode");
        Test.ensureEqual(SSR.minimalPercentEncode("+ :q*~?=&%"), "%2B%20:q*~?%3D%26%25", "minimalPercentEncode");
        Test.ensureEqual(SSR.percentDecode("%2B+%3aq%2a%7E%3f%3D%26%25"), "+ :q*~?=&%", "percentDecode");

        //sftp
        String2.log("test sftp");
        String password = String2.getPasswordFromSystemIn(
            "cwatch password for coastwatch computer (enter \"\" to skip the test)? ");
        if (password.length() > 0) {
            try {
                String fileName = "Rainbow.cpt";
                StringBuilder cmds = new StringBuilder(
                    "lcd " + SSR.getTempDirectory() + "\n" +
                    "cd /u00/cwatch/bobtemp\n" +  //on coastwatch computer; don't use String2.testU00Dir
                    "get " + fileName);
                File2.delete(SSR.getTempDirectory() + fileName);
                SSR.sftp("coastwatch.pfeg.noaa.gov", "cwatch", password, cmds.toString());
                Test.ensureEqual(File2.length(SSR.getTempDirectory() + fileName), 214, "a");
                File2.delete(SSR.getTempDirectory() + fileName);
            } catch (Exception e) {
                String2.getStringFromSystemIn(MustBe.throwableToString(e) + 
                    "\nUnexpected error.  Press ^C to stop or Enter to continue..."); 
            }
        }

        //SSR.windowsSftp never worked (authentication trouble) and SSR.sftp is
        //  better anyway because it is Java-based and therefore platform independent.
        //SSR.windowsSftp("coastwatch.pfeg.noaa.gov", "cwatch", password, 
        //    "\\temp\\", "/usr/local/jakarta-tomcat-5.5.4/webapps/cwexperimental/WEB-INF/secure/", 
        //    new String[]{"btemplate.xml"}, //send
        //    new String[]{}, 10); //receive

        //dosShell
        String2.log("test dosShell");
        String tempGif = SSR.getContextDirectory() + "images/temp.gif";
        File2.delete(tempGif);
        try {
            Test.ensureEqual(
                String2.toNewlineString(SSR.dosShell(
                    "\"C:\\Program Files (x86)\\ImageMagick-6.8.0-Q16\\convert\" " +
                    SSR.getContextDirectory() + "images/subtitle.jpg " +
                    tempGif, 10).toArray()),
                "", "dosShell a");
            Test.ensureTrue(File2.isFile(tempGif), "dosShell b");
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.getStringFromSystemIn(
                "*** 2012-10-31 Broke with change to M4700. Not fixed yet." +
                "\nPress 'Enter' to continue or ^C to stop...");
        }
        File2.delete(tempGif);

        //cutChar
        String2.log("test cutChar");
        Test.ensureEqual( SSR.cutChar("abcd", 2, 3), "bc", "a");
        Test.ensureEqual( SSR.cutChar("abcd", 1, 4), "abcd", "b");
        Test.ensureEqual( SSR.cutChar("abcd", 0, 4), "abcd", "c");
        Test.ensureEqual( SSR.cutChar("abcd", 1, 5), "abcd", "d");
        Test.ensureEqual( SSR.cutChar("abcd", 3, 3), "c", "e");
        Test.ensureEqual( SSR.cutChar("abcd", 4, 1), "", "f");
        Test.ensureEqual( SSR.cutChar("abcd", -2, 0), "", "g");

        Test.ensureEqual( SSR.cutChar("abcd", 1), "abcd", "a");
        Test.ensureEqual( SSR.cutChar("abcd", 0), "abcd", "b");
        Test.ensureEqual( SSR.cutChar("abcd", -1), "abcd", "c");
        Test.ensureEqual( SSR.cutChar("abcd", 2), "bcd", "d");
        Test.ensureEqual( SSR.cutChar("abcd", 4), "d", "e");
        Test.ensureEqual( SSR.cutChar("abcd", 5), "", "f");

        //make a big chunk of text
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000000; i++) 
            sb.append("This is a really, really, long line of text to text compression speed.\n");
        String longText = sb.toString();
        sb = null;
        String testMB = "  " + (longText.length() / Math2.BytesPerMB) + " MB";

        //zip without directory info
        String2.log("\n* test zip without dir info" + testMB);
        String middleDir = "gov/noaa/pfel/coastwatch/";
        String zipDir = classPath + middleDir;
        String zipName = "TestSSR.zip";
        String fileName = "TestSSR.txt";
        //write a longText file
        Test.ensureEqual(String2.writeToFile(zipDir + fileName, longText), "", "SSR.zip a");
        //make the zip file
        File2.delete(zipDir + zipName);
        long time1 = System.currentTimeMillis();
        SSR.zip(zipDir + zipName, new String[]{zipDir + fileName}, 10);
        time1 = System.currentTimeMillis() - time1;
        File2.delete(zipDir + fileName);
        //unzip the zip file
        long time2 = System.currentTimeMillis();
        SSR.unzip(zipDir + zipName, zipDir, //note: extract to zipDir, since it doesn't include dir
            false, 10); //false 'ignoreDirectoryInfo', but there is none
        time2 = System.currentTimeMillis() - time2;
        //ensure results are as expected
        String[] results = String2.readFromFile(zipDir + fileName);
        Test.ensureEqual(results[0], "", "SSR.zip b");
        Test.ensureEqual(results[1], longText, "SSR.zip c");
        String2.log("zip+unzip time=" + (time1 + time2) + "  (Java 1.7M4700 967ms, 1.6 4000-11000ms)");
        File2.delete(zipDir + zipName);
        File2.delete(zipDir + fileName);

        //zip with directory info
        String2.log("\n* test zip with dir info" + testMB);
        //write a longText file
        Test.ensureEqual(String2.writeToFile(zipDir + fileName, longText), "", "SSR.zip a");
        //make the zip file
        File2.delete(zipDir + zipName);
        time1 = System.currentTimeMillis();
        SSR.zip(zipDir + zipName, new String[]{zipDir + fileName}, 10, classPath);
        time1 = System.currentTimeMillis() - time1;
        File2.delete(zipDir + fileName);
        //unzip the zip file
        time2 = System.currentTimeMillis();
        SSR.unzip(zipDir + zipName, classPath, //note: extract to classPath, since includes dir
            false, 10); //false 'ignoreDirectoryInfo'
        time2 = System.currentTimeMillis() - time2;
        //ensure results are as expected
        results = String2.readFromFile(zipDir + fileName);
        Test.ensureEqual(results[0], "", "SSR.zip b");
        Test.ensureEqual(results[1], longText, "SSR.zip c");  
        String2.log("zip+unzip (w directory info) time=" + (time1 + time2) + 
            "  (Java 1.7M4700 937, 1.6 ~4000-11000ms)");
        File2.delete(zipDir + zipName);
        File2.delete(zipDir + fileName);

        //gzip without directory info
        for (int rep = 0; rep < 2; rep++) {
            String2.log("\n* test gzip without dir info" + testMB);
            middleDir = "gov/noaa/pfel/coastwatch/";
            String gzipDir = classPath + middleDir;
            String gzipName = "TestSSRG.txt.gz";
            fileName = "TestSSRG.txt";
            //write a longText file
            Test.ensureEqual(String2.writeToFile(gzipDir + fileName, longText), "", "SSR.gz a");
            //make the gzip file
            File2.delete(gzipDir + gzipName);
            time1 = System.currentTimeMillis();
            SSR.gzip(gzipDir + gzipName, new String[]{gzipDir + fileName}, 10); //don't include dir info
            time1 = System.currentTimeMillis() - time1;
            File2.delete(gzipDir + fileName);
            //unzip the gzip file
            time2 = System.currentTimeMillis();
            SSR.unGzip(gzipDir + gzipName, gzipDir, //note: extract to classPath, since doesn't include dir
                true, 10); //false 'ignoreDirectoryInfo'
            time2 = System.currentTimeMillis() - time2;
            //ensure results are as expected
            results = String2.readFromFile(gzipDir + fileName);
            Test.ensureEqual(results[0], "", "SSR.gz b");
            Test.ensureEqual(results[1], longText, "SSR.z c");
            String2.log("gzip+ungzip time=" + (time1 + time2) + 
                "  (Java 1.7M4700 780-880ms, 1.6 ~4000-11000ms)"); 
            File2.delete(gzipDir + gzipName);
            File2.delete(gzipDir + fileName);
        }

        testEmail();
        
        //getURLResponse (which uses getURLInputStream)
        //future: test various compressed url's
        String2.log("test getURLResponse");
        try {
            sar = SSR.getUrlResponse("http://www.pfeg.noaa.gov/"); //"http://www.cohort.com");
            Test.ensureEqual(
                String2.lineContaining(sar, "Disclaimer and Privacy Policy") == -1, //"A free RPN scientific calculator applet") == -1,
                false, "Response=" + String2.toNewlineString(sar));
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.getStringFromSystemIn(
                "\nRecover from failure? Press 'Enter' to continue or ^C to stop...");
        }

        //test non-existent file
        long rTime = System.currentTimeMillis();
        try {
            sar = SSR.getUrlResponse("http://coastwatch.pfeg.noaa.gov/zzz.html");
            throw new Throwable("shouldn't get here.");
        } catch (Exception e) { //not throwable
            String2.log("SSR.getUrlResponse for non existent url time=" + (System.currentTimeMillis() - rTime));
            //String2.getStringFromSystemIn("continue? ");
        } catch (Throwable t) {
            Test.error(t.toString()); //converts it to Exception and stops the testing
        }

        //note there is no continuity (session cookie isn't being sent)
        //but you can put as many params on one line as needed (from any screen)
        //and put edit=... to determine which screen gets returned
        try {
            sar = SSR.getUrlResponse("http://coastwatch.pfeg.noaa.gov/coastwatch/CWBrowser.jsp?edit=Grid+Data");
            String2.log("****beginResponse\n" + String2.toNewlineString(sar) + "\n****endResponse");
            Test.ensureNotEqual(String2.lineContaining(sar, "Download the grid data:"), -1, "e");
        } catch (Exception e) {
            String2.getStringFromSystemIn(MustBe.throwableToString(e) + 
                "\nUnexpected error.  Press ^C to stop or Enter to continue..."); 
        }

        //postHTMLForm     (always right after contact the web site above)
        //I NEVER GOT THIS WORKING. JUST USE 'GET' TESTS ABOVE
//        String2.log("test postHTMLForm");

        //for apache commons version
//        sar = SSR.postHTMLForm("http://coastwatch.pfeg.noaa.gov/cwexperimental/", "CWBrowser.jsp",
//            new String[]{"edit", "Bathymetry"});

        //for devx version
        //sar = SSR.postHTMLForm("http://coastwatch.pfeg.noaa.gov/cwexperimental/CWBrowser.jsp",
        //    new Object[]{"bathymetry", "false", "edit", "Bathymetry"});

        //for java almanac version
        //sar = SSR.postHTMLForm("coastwatch.pfeg.noaa.gov", "/cwexperimental/CWBrowser.jsp",
        //     "edit=Bathymetry");

//        String2.log("****beginResponse\n" + String2.toNewlineString(sar) + "\n****endResponse");
//        Test.ensureNotEqual(String2.lineContaining(sar, "1) Draw bathymetry lines:"), -1, "a");

        //getFirstLineStartsWith
        String2.log("test getFirstLineStartsWith");
        String tFileName = classPath + "testSSR.txt";
        String2.writeToFile(tFileName, "This is\na file\nwith a few lines.");
        Test.ensureEqual(SSR.getFirstLineStartsWith(tFileName, "with "), "with a few lines.", "a");
        Test.ensureEqual(SSR.getFirstLineStartsWith(tFileName, "hi "), null, "b");

        //getFirstLineMatching
        String2.log("test getFirstLineMatching");
        Test.ensureEqual(SSR.getFirstLineMatching(tFileName, ".*?i.*"),        "This is",           "a"); //find first of many matches
        Test.ensureEqual(SSR.getFirstLineMatching(tFileName, "^a.*"),          "a file",            "b"); //start of line
        Test.ensureEqual(SSR.getFirstLineMatching(tFileName, ".*?\\sfew\\s.*"),"with a few lines.", "c"); //containing
        Test.ensureEqual(SSR.getFirstLineMatching(tFileName, "q"),             null,                "d"); //no match

        Test.ensureTrue(File2.delete(tFileName), "delete " + tFileName);

        //getContextDirectory
        String2.log("test getContextDirectory current=" + SSR.getContextDirectory());
        //there is no way to test this and have it work with different installations
        //test for my computer (comment out on other computers):
        //ensureEqual(String2.getContextDirectory(), "C:/programs/tomcat/webapps/cwexperimental/", "a");
        //wimpy test, but works on all computers
        Test.ensureNotNull(SSR.getContextDirectory(), "contextDirectory");

        //getTempDirectory
        String2.log("test getTempDirectory current=" + SSR.getTempDirectory());
        //wimpy test
        Test.ensureEqual(SSR.getTempDirectory(), SSR.getContextDirectory() + "WEB-INF/temp/", "a");


        //done 
        String2.log("\nDone. All non-Unix tests passed!");

    }

    /**
     * If this fails with "Connection refused" error, make sure McAffee "Virus Scan Console :
     *   Access Protection Properties : Anti Virus Standard Protections :
     *   Prevent mass mailing worms from sending mail" is un-checked.
     */
    public static void testEmail() throws Exception {

        String emailServer, emailPort, emailProperties, emailUser,
            emailPassword, emailReplyToAddress, emailToAddress;

        //sendEmail (always uses authentication)
        emailServer = String2.getStringFromSystemIn(
            "\n\n*** NOAA email server (e.g., mta.nems.noaa.gov)? ");
        if (emailServer.length() == 0) emailServer = "mta.nems.noaa.gov";

        emailPort = String2.getStringFromSystemIn(
            "email port (e.g., 587)? ");  
        if (emailPort.length() == 0) emailPort = "587";

        emailProperties = String2.getStringFromSystemIn(
            "email properties (e.g., mail.smtp.starttls.enable|true)? ");  
        if (emailProperties.length() == 0) emailProperties = "mail.smtp.starttls.enable|true";

        emailUser = String2.getStringFromSystemIn(
            "email user (e.g., erd.data)? ");
        if (emailUser.length() == 0) emailUser = "erd.data";

        emailPassword = String2.getPasswordFromSystemIn(
            "email password (or \"\" to skip the test)? ");
        
        emailReplyToAddress = String2.getStringFromSystemIn(
            "email Reply To address (e.g., erd.data@noaa.gov)? ");
        if (emailReplyToAddress.length() == 0) emailReplyToAddress = "erd.data@noaa.gov";

        emailToAddress = String2.getStringFromSystemIn(
            "an email To address outside this domain (e.g., info@cohort.com)? ");
        if (emailToAddress.length() == 0) emailToAddress = "info@cohort.com";

        //Non-local destinations like this fail without password/authentication.
        String2.log("test email " + emailToAddress); 
        try {
            if (emailPassword.length() == 0) {
                String2.log("Skipping the non-local email test since no password provided.");
                Math2.incgc(2000);
            } else SSR.sendEmail(emailServer, String2.parseInt(emailPort), emailUser, emailPassword, 
                emailProperties,
                emailReplyToAddress, emailToAddress,
                "Email Test", "This is an email test (non-local) from TestSSR.");
        } catch (Exception e) {
            String2.getStringFromSystemIn(
                MustBe.throwableToString(e) +
                "\nPress ^C to stop or Enter to continue..."); 
        }


        //*** sendEmail via Google   uses starttls authentication
        emailServer = String2.getStringFromSystemIn(
            "\n\n***gmail email server (e.g., smtp.gmail.com)? ");
        if (emailServer.length() == 0) emailServer = "smtp.gmail.com";

        emailPort = String2.getStringFromSystemIn(
            "gmail email port (e.g., 465 or 587 (default))? ");
        if (emailPort.length() == 0) emailPort = "587";

        emailUser = String2.getStringFromSystemIn( //was "bob.simons.noaa";
            "gmail email user (e.g., bob.simons@noaa.gov)? ");
        if (emailUser.length() == 0) emailUser = "bob.simons@noaa.gov"; 

        emailPassword = String2.getPasswordFromSystemIn(
            "gmail email password\n" +
            "(e.g., password (or \"\" to skip this test)? ");
        
        if (emailPassword.length() > 0) {
            emailReplyToAddress = String2.getStringFromSystemIn( //was "bob.simons.noaa@gmail.com"
                "gmail email Reply To address (e.g., bob.simons@noaa.gov)? ");
            if (emailReplyToAddress.length() == 0) emailReplyToAddress = "bob.simons@noaa.gov";

            emailToAddress = String2.getStringFromSystemIn(
                "an email To address (e.g., bob.simons@noaa.gov)? ");
            if (emailToAddress.length() == 0) emailToAddress = "bob.simons@noaa.gov";

            try {
                String2.log("test gmail email " + emailToAddress); 
                SSR.sendEmail(emailServer, String2.parseInt(emailPort), emailUser, emailPassword, 
                    "mail.smtp.starttls.enable|true",
                    emailReplyToAddress, emailToAddress,
                    "gmail email test", "This is a gmail email test from TestSSR.");
            } catch (Exception e) {
                String2.getStringFromSystemIn(
                    MustBe.throwableToString(e) +
                    "\nPress ^C to stop or Enter to continue..."); 
            }

            emailToAddress = String2.getStringFromSystemIn(
                "an email To address outside this domain (e.g., info@cohort.com)? ");
            if (emailToAddress.length() == 0) emailToAddress = "info@cohort.com";

            try {
                String2.log("test gmail email " + emailToAddress); 
                SSR.sendEmail(emailServer, String2.parseInt(emailPort), emailUser, emailPassword, 
                    "mail.smtp.starttls.enable|true",
                    emailReplyToAddress, emailToAddress,
                    "gmail email test", "This is a gmail email test from TestSSR.");
            } catch (Exception e) {
                String2.getStringFromSystemIn(
                    MustBe.throwableToString(e) +
                    "\nPress ^C to stop or Enter to continue..."); 
            }
        }
    }


    /** 
     * Test email. 
     * If this fails with "Connection refused" error, make sure McAffee "Virus Scan Console :
     *   Access Protection Properties : Anti Virus Standard Protections :
     *   Prevent mass mailing worms from sending mail" is un-checked.
     * 
     */
    public static void testEmail(String emailUser, String password) throws Exception {
        //see properties list at
        //http://javamail.kenai.com/nonav/javadocs/com/sun/mail/smtp/package-summary.html

        String title = "Email Test from TestSSR";
        String content = "This is an email test (local) from user=" + emailUser + " in TestSSR.";
        String2.log("\n*** " + content);
        if (false) {
            String emailServer = "smtp.gmail.com";
            int emailPort = 587;
            SSR.sendEmail(emailServer, emailPort, emailUser, password, 
                "mail.smtp.starttls.enable|true", 
                emailUser, emailUser, title, content);
        }
        if (true) {
            String emailServer = "mta.nems.noaa.gov";
            String properties = "mail.smtp.starttls.enable|true";
            int emailPort = 587;
            String2.log("sending email to bob.simons@noaa.gov");
            SSR.sendEmail(emailServer, emailPort, emailUser, password, 
                properties, emailUser, "bob.simons@noaa.gov", title, content);

            String2.log("sending email to info@cohort.com");
            SSR.sendEmail(emailServer, emailPort, emailUser, password, 
                properties, emailUser, "info@cohort.com", title, content);
        }
    }


    /**
     * Run all of the tests which are dependent on Unix.
     */
    public static void runUnixTests() throws Exception {
        //cShell
        String2.log("test cShell");
        //Test.ensureEqual(toNewlineString(SSR.cShell("")), "", "a");

        String2.log("Done. All Unix tests passed!");
    }

    /**
     * Run all of the tests
     */
    public static void main(String args[]) throws Throwable {
        SSR.verbose = true;
        runNonUnixTests();
        
//        runUnixTests();
    }

}


