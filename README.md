
zxs_tap2bas
===========

This program analyses TAP data file for ZX Spectrum. Converts BASIC blocks to listing in human-readable form. 

For more information about BASIC for ZX Spectrum see article
[Sinclair BASIC on Wikipedia](https://en.wikipedia.org/wiki/Sinclair_BASIC)
And for more information about ZX Spectrum see article
[ZX Spectrum](https://en.wikipedia.org/wiki/ZX_Spectrum).

*This is tool for software archaeologists.*

Note: This program is not able to disassemble a machine code data.


<hr>

### Requirements
* Java 7 or newer

### How to built
1. install *Java SE Development Kit (JDK)*, install *Apache Maven*, install *Netbeans IDE*
2. open project in *Netbeans*.
3. be sure you are connected to the Internet
4. select "Clean and Build Project (Shift-F11)" in menu "Run".
5. wait until *maven* download all dependecies and plugins. Then *maven* create build.
6. result should be in directory: *target/dist-package*.

<hr>

### Special characters format

* UDG characters<br>{A}, {B}, ..., {T}

* copyright symbol<br>{(C)}

* pound symbol<br>{pound}

* mosaic graphics symbols<br>{-1}, {-2}, ..., {-8}<br>{+1}, {+2}, ..., {+8}   (mosaic graphics symbol with shift)

* control (*invisible*) characters for text attributes settings
<br>{INK c}
<br>{PAPER c}
<br>{FLASH b}
<br>{BRIGHT b}
<br>{INVERSE b}
<br>{OVER b}
<br>{AT y,x}
<br>{TAB t}
<br>(c = color number; b = value 0 or 1; y,x = coordinates in text mode; t = tab num)

<hr>

### Simple example

    java -jar zxs_tap2bas -i usr_char.tap --onlyBasic -o usr_char.bas


input **usr_char.tap** (as hexdump):

```
          00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0D 0F  0123456789ABCDEF
00000000: 13 00 00 00 20 20 20 20 20 20 20 20 20 20 e0 00  ....          ..
00000010: 00 80 e0 00 80 e2 00 ff 00 0b 11 00 e4 c4 30 30  ..............00
00000020: 30 31 31 30 30 30 0e 00 00 18 00 00 0d 00 0c 11  011000..........
00000030: 00 e4 c4 30 30 31 31 31 31 30 30 0e 00 00 3c 00  ...00111100...<.
00000040: 00 0d 00 0d 11 00 e4 c4 30 31 31 31 31 31 31 30  ........01111110
00000050: 0e 00 00 7e 00 00 0d 00 0e 11 00 e4 c4 31 31 30  ...~.........110
00000060: 31 31 30 31 31 0e 00 00 db 00 00 0d 00 0f 11 00  11011...........
00000070: e4 c4 31 31 31 31 31 31 31 31 0e 00 00 ff 00 00  ..11111111......
00000080: 0d 00 10 11 00 e4 c4 30 30 31 30 30 31 30 30 0e  .......00100100.
00000090: 00 00 24 00 00 0d 00 11 11 00 e4 c4 30 31 30 31  ..$.........0101
000000A0: 31 30 31 30 0e 00 00 5a 00 00 0d 00 12 11 00 e4  1010...Z........
000000B0: c4 31 30 31 30 30 31 30 31 0e 00 00 a5 00 00 0d  .10100101.......
000000C0: 00 13 24 00 eb 49 3d 30 0e 00 00 00 00 00 20 cc  ..$..I=0...... .
000000D0: 37 0e 00 00 07 00 00 3a e3 42 3a f4 c0 22 41 22  7......:.B:.."A"
000000E0: 2b 49 2c 42 3a f3 49 0d 00 14 0c 00 f5 c2 31 34  +I,B:.I.......14
000000F0: 34 0e 00 00 90 00 00 0d de                       4........
```

output **usr_char.bas**:

```basic
11 DATA BIN 00011000
12 DATA BIN 00111100
13 DATA BIN 01111110
14 DATA BIN 11011011
15 DATA BIN 11111111
16 DATA BIN 00100100
17 DATA BIN 01011010
18 DATA BIN 10100101
19 FOR I=0  TO 7:READ B:POKE USR "A"+I,B:NEXT I
20 PRINT CHR$ 144
```

