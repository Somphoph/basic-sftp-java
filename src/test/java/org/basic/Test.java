 package org.basic;

 import java.io.PrintStream;

 public class Test {
   public static void main(String[] args) { String sourceFileName = "TBL_*_*.txt";
     String name = "TBL_SDFLDJSFKDSF_20170202.txt";
     String[] matchs = sourceFileName.split("\\*");

     boolean match = false;
     int i = 0; for (int idx = 0; i < matchs.length; i++) {
       int fidx = name.indexOf(matchs[i], idx);
       if (fidx > -1) {
         match = true;
         idx = fidx + matchs[i].length();
       } else {
         match = false;
         break;
       }
     }
     System.out.println(match);
   }
 }
