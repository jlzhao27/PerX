/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package wordcount;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.Map.Entry;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import mapservice.MapInterface;

/**
 *
 * @author Jason
 */
public class MapWord implements MapInterface<String, File, String, Integer, Integer> {

    private static final long serialVersionUID = 1L;

    public static void main(String[] args) throws MalformedURLException {
        MapWord m = new MapWord();

        File folder = new File("/Users/Jason/Documents/CS5412/txt");
        File[] files = folder.listFiles();
        Map<String, List<Integer>> output = new HashMap<String, List<Integer>>();
        Map<String, Integer> finalOutput = new HashMap<String, Integer>();
        for (File f : files) {
            m.map(f.getName(), f, output);
        }
        SortedSet<String> keySet = new TreeSet<String>(output.keySet());
         try {
            FileOutputStream outputStream = new FileOutputStream(new File("key_results.txt"));
            for (String key : keySet) {
                outputStream.write((key + "\n").getBytes());

            }
            outputStream.flush();
            outputStream.close();
        } catch (Exception ex) {
            Logger.getLogger(MapWord.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (Entry<String, List<Integer>> e : output.entrySet()) {
            m.reduce(e.getKey(), e.getValue(), finalOutput);
        }

        SortedSet<String> sortedSet = new TreeSet<String>(finalOutput.keySet());
        try {
            FileOutputStream outputStream = new FileOutputStream(new File("results.txt"));
            for (String key : sortedSet) {
                outputStream.write((key + ":" + finalOutput.get(key) + "\n").getBytes());

            }
            outputStream.flush();
            outputStream.close();
        } catch (Exception ex) {
            Logger.getLogger(MapWord.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static String strip(String s) {
        String output = "";
        char[] cs = s.trim().toLowerCase().toCharArray();
        for (char c : cs) {
            if (c >= 'a' && c <= 'z') {
                output += c;
            }
        }
        return output;
    }

    @Override
    public void map(String fileName, File file, Map<String, List<Integer>> output) {
        try {
            Scanner reader = new Scanner(file);

            while (reader.hasNext()) {
                String word = strip(reader.next());
                if (word.length() == 0) {
                    continue;
                }
                if (output.containsKey(word)) {
                    output.get(word).add(1);
                } else {
                    List<Integer> newList = new LinkedList<Integer>();
                    newList.add(1);
                    output.put(word, newList);
                }
            }
            reader.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MapWord.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void reduce(String key, List<Integer> values, Map<String, Integer> output) {
        int sum = 0;
        for (Integer i : values) {
            sum += i.intValue();
        }
        output.put(key, sum);

    }
}
