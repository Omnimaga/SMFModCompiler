package com.up.smfmc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Ricky
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            String root = args[0];
            Document info = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(root + "/src/package-info.xml");
            Node n = info.getElementsByTagName("package-info").item(0);
            String mod = createModFile(findNodeByName(n.getChildNodes(), "id").getFirstChild().getNodeValue(), findNodeByName(n.getChildNodes(), "version").getFirstChild().getNodeValue(), diffDir(root + "/smf", "."));
            File dz = new File(root + "/dist");
            if (dz.exists()) {
                for (File f : dz.listFiles()) f.delete();
            } else {
                dz.mkdir();
            }
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(root + "/dist/mod.zip"));
            out.putNextEntry(new ZipEntry("package-info.xml"));
            BufferedReader pir = new BufferedReader(new FileReader(root + "/src/package-info.xml"));
            while (pir.ready()) out.write((pir.readLine() + "/n").getBytes());
            out.putNextEntry(new ZipEntry("readme.txt"));
            BufferedReader rir = new BufferedReader(new FileReader(root + "/src/readme.txt"));
            while (rir.ready()) out.write((rir.readLine() + "/n").getBytes());
            out.putNextEntry(new ZipEntry("modification.xml"));
            out.write(mod.getBytes());
            out.close();
        } catch (ParserConfigurationException | SAXException | IOException | DOMException e) {
            e.printStackTrace();
        }
    }
    
    public static Node findNodeByName(NodeList nl, String name) {
        for (int i = 0; i < nl.getLength(); i++) if (nl.item(i).getNodeName().equals(name)) return nl.item(i);
        return null;
    }
    
    public static String createModFile(String id, String version, ArrayList<Diff> diffs) {
        String ret = "<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE modification SYSTEM \"http://www.simplemachines.org/xml/modification\">\n" +
                "<modification xmlns=\"http://www.simplemachines.org/xml/modification\" xmlns:smf=\"http://www.simplemachines.org/\">\n" +
                "	<id>" + id + "</id>\n" +
                "	<version>" + version + "</version>\n";
        for (Diff d : diffs) ret += d.toString() + "\n";
        ret += "</modification>";
        return ret;
    }
    
    public static ArrayList<Diff> diffDir(String root, String dir) throws IOException {
        ArrayList<Diff> diffs = new ArrayList<>();
        for (File f : new File(root + "/" + dir).listFiles()) {
            if (f.isDirectory()) {
                diffs.addAll(diffDir(root, dir + "/" + f.getName()));
            } else {
                diffs.addAll(getDiffs(root, dir + "/" + f.getName()));
            }
        }
        return diffs;
    }
    
    public static ArrayList<Diff> getDiffs(String root, String file) throws IOException {
        if (!new File(root + "/" + file).exists()) return new ArrayList<>();
        ArrayList<String> s1 = new ArrayList<>();
        ArrayList<String> s2 = new ArrayList<>();
        BufferedReader f1r = new BufferedReader(new FileReader("smf/" + file));
        while (f1r.ready()) s1.add(f1r.readLine());
        BufferedReader f2r = new BufferedReader(new FileReader(root + "/" + file));
        while (f2r.ready()) s2.add(f2r.readLine());
        return diff(s1.toArray(new String[0]), s2.toArray(new String[0]), file); 
    }
    
    public static ArrayList<Diff> diff(String[] lines1, String[] lines2, String file) {
        if (linesContain(lines1, "?>")) {
            while (!lines1[lines1.length - 1].equals("?>") || lines1[lines1.length - 1].equals("")) {
                String[] temp = new String[lines1.length - 1];
                System.arraycopy(lines1, 0, temp, 0, lines1.length - 1);
                lines1 = temp;
            }
            String[] temp = new String[lines1.length - 1];
            System.arraycopy(lines1, 0, temp, 0, lines1.length - 1);
            lines1 = temp;
        }
        if (linesContain(lines2, "?>")) {
            while (!lines2[lines2.length - 1].equals("?>") || lines2[lines2.length - 1].equals("")) {
                String[] temp = new String[lines2.length - 1];
                System.arraycopy(lines2, 0, temp, 0, lines2.length - 1);
                lines2 = temp;
            }
            String[] temp = new String[lines2.length - 1];
            System.arraycopy(lines2, 0, temp, 0, lines2.length - 1);
            lines2 = temp;
        }
        ArrayList<Diff> diffs = new ArrayList<>();
        int i1 = 0;
        int i2 = 0;
        while (i1 < lines1.length && i2 < lines2.length) {
            if (!lines1[i1].equals(lines2[i2])) {
                Diff d = null;
                int start = i2;
                //Test for insert
                while (!linesMatch(lines1, lines2, i1, ++i2, 3)) {
                    if (i2 >= lines2.length) {
                        //Test for replace
                        int start1 = i1;
                        i1++;
                        i2 = start;
                        while (!linesMatch(lines1, lines2, i1, ++i2, 3)) {
                            if (i2 >= lines2.length) {
                                i1++;
                                if (i1 >= lines1.length) break;
                                i2 = start;
                            }
                        }
                        if (i2 < lines2.length) {
                            d = new Diff(file, join(lines1, "\n", start1, i1 - 1), join(lines2, "\n", start, i2 - 1), Diff.Method.replace);
                        }
                        //no replace? end then
                        if (d == null) {
                            d = new Diff(file, null, join(lines2, "\n", start, lines2.length - 1), Diff.Method.end);
                        }
                        break;
                    }
                }
                if (d == null) {
                    d = new Diff(file, join(lines1, "\n", Math.max(0, i1 - 3), i1), join(lines2, "\n", start, i2 - 1), Diff.Method.before);
                }
                diffs.add(d);
            }
            i1++; i2++;
        }
        if (i2 < lines2.length) {
            diffs.add(new Diff(file, null, join(lines2, "\n", i2, lines2.length - 1), Diff.Method.end));
        }
        return diffs;
    }
    
    public static String join(String[] arr, String glue, int start, int end) {
        String ret = "";
        for (int x = start; x <= end; x++) ret += arr[x] + glue;
        return ret.substring(0, ret.length() - glue.length());
    }
    
    public static boolean linesMatch(String[] arr1, String[] arr2, int start1, int start2, int length) {
        if (start1 >= arr1.length || start2 >= arr2.length) {
            return false;
        }
        for (int x = 0; x < length; x++) {
            if (start1 + x < arr1.length && start2 + x < arr2.length) {
                if(!arr1[start1 + x].equals(arr2[start2 + x])) {
                    return false;
                }
            }
        }
        return true;
    }
    public static boolean linesContain(String[] arr, String line) {
        for (int x = 0; x < arr.length; x++) {
            if(arr[x].equals(line)) {
                return true;
            }
        }
        return false;
    }
}
