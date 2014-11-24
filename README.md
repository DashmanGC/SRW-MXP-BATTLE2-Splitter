SRW MX Portable BATTLE2 Splitter
-----------------------------------

This program allows you to split and merge the files inside BATTLE2.BIN for SRW MX Portable, which contains (amongst other things) the battle quotes for the different characters in the game. You'll need to have Java installed in your computer to operate this.

Unlike other split / merge programs, this splitter works on two levels. That means you have to go through two extractions to get to the battle script.


How to extract:

1) Extract BATTLE2.BIN from the ISO and place it in the same folder as the application (battle2_split.jar).
2) In a command / shell window, execute this:

java -jar battle2_splitter.jar -s BATTLE2.BIN <extract_folder>

* Alternatively, use extract_battle.bat to perform this. The BAT file has to be in the same folder where the BIN and the executable are.

3) This will extract 11 BIN files in <extract_folder>. This will take a while, since BATTLE2.BIN is ~500 MB in size.

4) Once the extraction is done, copy the executable into <extract_folder> and execute:

java -jar battle2_splitter.jar -s2 0005.BIN <extract_folder2>

* Alternatively, use extract_battle_script.bat to perform this. Same rules as before.

5) This will generate 300+ BATTLE files inside <extract_folder2> containing the battle quotes. Use the proper tool to edit those.



How to insert:

1) Put the program inside <extract_folder2> along with the generated files and its corresponding LIST file (which was generated with the BATTLE files). Make sure all the files have the same name they had when they were extracted (for example, if you edited a file and saved it as 0001-edit.BATTLE, rename it now as 0001.BATTLE if you want it to be included).

2) Execute

java -jar battle2_splitter.jar -m2 0005.BIN files.list

* Alternatively, use merge_battle_script.bat to do this.

3) This will generate a 0005.BIN file in <extract_folder2>. Replace the 0005.BIN in the first <extract_folder> with the one you just generated, and from there, execute:

java -jar battle2_splitter.jar -m BATTLE2.BIN files.list

* Alternatively, use merge_battle.bat to do this.

4) That will generate a new BATTLE2.BIN inside <extract_folder>, which you can replace in the ISO.


IMPORTANT NOTES:

* Keep backups!

* Files 0000.BIN to 0004.BIN are RAW 8bpp images where the first 1024 bytes are the palette. 0000.BIN has the options for the main logo screen and is 256x256. The other 4 are 512x256 and have the different series' names used during the battle demo (when you leave the game untouched for a while in the logo screen).

* Files 0006.BIN to 0009.BIN contain the backgrounds used in battle, the mech graphics, the character cut-ins and the weapon effects.

* 0010.BIN has palettes