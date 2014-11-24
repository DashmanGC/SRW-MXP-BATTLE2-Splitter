/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package srw.mxp.battle2.splitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jonatan
 */
public class Main {

    public static class IndexEntry{
        public String name;
        public int offset;
        public int size;

        public IndexEntry(){
            name = "";
            offset = 0;
            size = 0;
        }

        public IndexEntry(String n, int o, int s){
            name = n;
            offset = o;
            size = s;
        }
    }

    static String filename;
    static String destination = ".";
    static RandomAccessFile f;
    static String file_list = "";
    static byte[] seq;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        /*
         * USE
         * -s <filename> [<destination_folder>] Splits filename's contents on destination
         * -m <filename> <files_list> Merges the list of files in files_list into filename
         */

        boolean show_use = false;

        if (args.length < 2 || args.length > 3){
            show_use = true;
        }

        else{
            String command = args[0];
            filename = args[1];

            if (command.equals("-s")){

                if (args.length == 3)
                    destination = args[2];

                // Try opening the file
                try{
                    f = new RandomAccessFile(filename, "r");
                    // Read the header / index and obtain the offsets
                    readHeader();
                }catch (IOException ex) {
                    System.err.println("ERROR: Couldn't read file.");   // END
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else if (command.equals("-s2")){
                
                if (args.length == 3)
                    destination = args[2];
                
                // Try opening the file
                try{
                    f = new RandomAccessFile(filename, "r");
                    // Read the header / index and obtain the offsets
                    splitDialogues();
                }catch (IOException ex) {
                    System.err.println("ERROR: Couldn't read file.");   // END
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else if (command.equals("-m")){
                if (args.length != 3)
                    show_use = true;
                else{
                    file_list = args[2];
                    // Read the file list and merge the contents into the given filename
                    try{
                        mergeFileList();
                    }catch (IOException ex) {
                        System.err.println("ERROR: Couldn't read file.");   // END
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            else if (command.equals("-m2")){
                if (args.length != 3)
                    show_use = true;
                else{
                    file_list = args[2];
                    // Read the file list and merge the contents into the given filename
                    try{
                        mergeFileListNoTable();
                    }catch (IOException ex) {
                        System.err.println("ERROR: Couldn't read file.");   // END
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            else    // Wrong command
                show_use = true;
        }

        if (show_use){
            System.out.println("ERROR: Wrong number of parameters: " + args.length);
            System.out.println("TO SPLIT:\n java -jar battle2_splitter -s <filename> [<destination_folder>]");
            System.out.println("TO MERGE:\n java -jar battle2_splitter -m <filename> <files_list>");
        }
    }
 
    public static void readHeader() throws IOException{
        // Read the first 4 bytes of the file
        byte[] header = new byte[2048];
        f.read(header);

        // If the first byte isn't 00, we might have a valid file
        if (header[2047] == (byte) 0xff){
            int index_size = 0;
            boolean stop = false;
            
            for (int i = 0; i < 512 && !stop; i += 4){
                if (header[i] == (byte) 0xff){
                    if (header[i+1] == (byte) 0xff)
                        if (header[i+2] == (byte) 0xff)
                            if (header[i+3] == (byte) 0xff)
                                stop = true;
                }
                else
                    index_size++;
            }

            //System.out.println("Index size: " + index_size);

            getIndex4(index_size);

            // Write the file list
            writeFileList();
        }
        // Otherwise, indicate the file is not supported
        else{
            System.err.println("ERROR: Unsupported file."); // END
            f.close();
        }
    }

    
    public static void splitDialogues() throws IOException{
        ArrayList<Integer> offsets = new ArrayList<>();
        
        byte[] aux = new byte[16];
        
        // Locate the offsets of the dialogue files.
        // These files are always 2048-byte alligned and bytes 16 and 18 are always 0xff
        offsets.add(0);
        
        for (int offset = 2048; offset < f.length(); offset += 2048){
            f.seek(offset - 16);
            
            f.read(aux);
            
            if ( aux[0] == 0 && aux[1] == 0 && aux[2] == 0 && aux[3] == 0 && 
                    aux[4] == 0 && aux[5] == 0 && aux[6] == 0 && aux[7] == 0 &&
                    aux[8] == 0 && aux[9] == 0 && aux[10] == 0 && aux[11] == 0 &&
                    aux[12] == 0 && aux[13] == 0 && aux[14] == 0 && aux[15] == 0 )
                offsets.add(offset);
        }
        
        offsets.add( (int) f.length() );
        
        for (int i = 0; i < (offsets.size() - 1); i++){
            String name = "";
            
            if (i < 1000)
                name += "0";
            if (i < 100)
                name += "0";
            if (i < 10)
                name += "0";
            
            name += i;
            
            IndexEntry ie = new IndexEntry(name, offsets.get(i), offsets.get(i + 1) - offsets.get(i));
            
            extractFile(ie, ".BATTLE");
            
            if (i != offsets.size() - 2)
                file_list += "\n";
            
            System.out.println(name + ".BATTLE extracted successfully.");
        }
        
        f.close();
       
        // Write the file list
        writeFileList();
    }
    
    // Takes a 4-byte hex little endian and returns its int value
    public static int byteSeqToInt(byte[] byteSequence){
        if (byteSequence.length != 4)
            return -1;

        int value = 0;
        value += byteSequence[0] & 0xff;
        value += (byteSequence[1] & 0xff) << 8;
        value += (byteSequence[2] & 0xff) << 16;
        value += (byteSequence[3] & 0xff) << 24;
        return value;
    }

    // Receives an int and return its 4-byte value
    public static byte[] int2bytes(int value){
        return new byte[] {
                (byte) value,
                (byte)(value >>> 8),
                (byte)(value >>> 16),
                (byte)(value >>> 24)};
    }

    public static void getIndex4(int num_entries) throws IOException{
        // Prepare an arraylist of IndexEntry
        ArrayList<IndexEntry> entries = new ArrayList<>();

        IndexEntry ie;
        String name = "";
        int offset;
        int next;
        int size = 0;
        boolean go_on = true;

        for (int i = 0; i < num_entries && go_on; i++){
            // Every entry in the index has 4 bytes indicating its offset
            // The last entry points at an "end" file that is 32 bytes long and has nothing
            f.seek( i * 4 );    // Go to the beginning of our current entry
            byte[] entry_block = new byte[8];   // Read the offset of this entry and the next one
            f.read(entry_block);

            if (i < 10)
                name = "000" + i;
            else if (i < 100)
                name = "00" + i;
            else if (i < 1000)
                name = "0" + i;
            else
                name = "" + i;

            seq = new byte[4];
            seq[0] = entry_block[0];
            seq[1] = entry_block[1];
            seq[2] = entry_block[2];
            seq[3] = entry_block[3];
            offset = byteSeqToInt(seq);

            seq[0] = entry_block[4];
            seq[1] = entry_block[5];
            seq[2] = entry_block[6];
            seq[3] = entry_block[7];
            next = byteSeqToInt(seq);

            if (i == num_entries - 1){
                size = (int) f.length() - offset;
                go_on = false;
            }
            else
                size = next - offset;

            ie = new IndexEntry(name, offset, size);

            entries.add(ie);
        }

        // Extract every file in the final list
        for (int i = 0; i < entries.size(); i++){
            //System.out.println(i + " - Offset: " + entries.get(i).offset + " Size: " + entries.get(i).size);
            extractFile(entries.get(i), ".BIN");

            //file_list += entries.get(i).name;
            if (i != entries.size() - 1)
                file_list += "\n";
        }

        // Inform of results
        System.out.println("Finished. Extracted " + entries.size() + " files.");

        f.close();  // END
    }

    public static void extractFile(IndexEntry ie, String extension) throws IOException{
        f.seek( (long) ie.offset);

        seq = new byte[ie.size];

        f.read(seq);

        String path = "";

        //ie.name += ".BIN";
        ie.name += extension;

        if (destination.equals("."))
            path = ie.name;
        else{
        // Check if folder with the name of the pak_file exists. If not, create it.
            path = destination;
            File folder = new File(path);
            if (!folder.exists()){
                boolean success = folder.mkdir();
                if (!success){
                    System.err.println("ERROR: Couldn't create folder.");
                    return;
                }
            }
            path += "/" + ie.name;
        }

        // Create the file inside said folder
        try {
            RandomAccessFile f2 = new RandomAccessFile(path, "rw");

            f2.write(seq);

            f2.close();

            file_list += ie.name;

            //System.out.println(ie.name + " saved successfully.");
        } catch (IOException ex) {
            System.err.println("ERROR: Couldn't write " + ie.name);
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void writeFileList() throws IOException{
        String path = "";
        if (destination.equals("."))
            path = "files.list";
        else    // The folder was created previously
            path = destination + "/files.list";

        PrintWriter pw = new PrintWriter(path);

        pw.print(file_list);

        pw.close();
    }

    public static void mergeFileList() throws IOException{
        BufferedReader br = new BufferedReader(new FileReader(file_list));
        String line;
        //int entry_size = 4;
        int table_size = 2048;
        int total_length = 0;
        ArrayList<IndexEntry> entries = new ArrayList<>();

        IndexEntry ie;

        // Read all filenames in files.list and their sizes
        int actual_length = 0;
        int padded_length = 0;

        while ((line = br.readLine()) != null) {

            f = new RandomAccessFile(line, "r");

            actual_length = (int) f.length();
            // We repurpose the offset value to store the padded length
            ie = new IndexEntry(line, padded_length, actual_length);

            entries.add(ie);
            
            total_length += actual_length;

            f.close();
        }
        br.close();


        //table_size += entries.size() * entry_size;

        total_length += table_size;
        seq = new byte[total_length];   // Here we'll write the full file
        byte[] aux;

        int pointer_table = 0;
        int pointer_data = table_size;  // Data starts right after the table

        // Write the number of entries in the first 4 bytes of the table
        //aux = int2bytes(entries.size());
        //seq[0] = aux[0];
        //seq[1] = aux[1];
        //seq[2] = aux[2];
        //seq[3] = aux[3];
        //pointer_table = 4;
        for (int i = 0; i < 2048; i++)
            seq[i] = (byte) 0xff;

        // Write each of the files into seq and update its pointer in the table
        for (int i = 0; i < entries.size(); i++){
            // Update pointer
            aux = int2bytes(pointer_data);

            seq[pointer_table] = aux[0];
            seq[pointer_table + 1] = aux[1];
            seq[pointer_table + 2] = aux[2];
            seq[pointer_table + 3] = aux[3];

            pointer_table += 4;

            // Write the file into our byte sequence
            aux = new byte[entries.get(i).size];

            f = new RandomAccessFile(entries.get(i).name, "r");
            f.read(aux);
            f.close();

            System.out.println("P.Table: " + pointer_table + " - P.Data: " + pointer_data +
                    " - Length: " + aux.length + " - Total: " + total_length);

            for (int j = 0; j < aux.length; j++)
                seq[pointer_data + j] = aux[j];

            pointer_data += entries.get(i).size;
        }

        // Save the byte sequence to a file
        f = new RandomAccessFile(filename, "rw");
        f.write(seq);
        f.close();

        System.out.println("Finished. File " + filename + " built successfully.");
    }   
    
    public static void mergeFileListNoTable() throws IOException{
        BufferedReader br = new BufferedReader(new FileReader(file_list));
        String line;
        int total_length = 0;
        ArrayList<IndexEntry> entries = new ArrayList<>();

        IndexEntry ie;

        // Read all filenames in files.list and their sizes
        int actual_length = 0;
        int padded_length = 0;

        while ((line = br.readLine()) != null) {

            f = new RandomAccessFile(line, "r");

            actual_length = (int) f.length();
            // We repurpose the offset value to store the padded length
            ie = new IndexEntry(line, padded_length, actual_length);

            entries.add(ie);
            
            total_length += actual_length;

            f.close();
        }
        br.close();

        seq = new byte[total_length];   // Here we'll write the full file
        byte[] aux;

        int pointer_data = 0;

        // Write each of the files into seq and update its pointer in the table
        for (int i = 0; i < entries.size(); i++){
            // Write the file into our byte sequence
            aux = new byte[entries.get(i).size];

            f = new RandomAccessFile(entries.get(i).name, "r");
            f.read(aux);
            f.close();

            System.out.println("P.Data: " + pointer_data +
                    " - Length: " + aux.length + " - Total: " + total_length);

            for (int j = 0; j < aux.length; j++)
                seq[pointer_data + j] = aux[j];

            pointer_data += entries.get(i).size;
        }

        // Save the byte sequence to a file
        f = new RandomAccessFile(filename, "rw");
        f.write(seq);
        f.close();

        System.out.println("Finished. File " + filename + " built successfully.");
    }
}
