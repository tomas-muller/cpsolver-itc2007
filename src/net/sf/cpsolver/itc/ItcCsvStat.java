package net.sf.cpsolver.itc;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Enumeration;

import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.CSVFile.CSVField;
import net.sf.cpsolver.ifs.util.CSVFile.CSVLine;

/**
 * Process given CSV file (or files, one per each problem), output the best achieved solution.
 *  
 * @version
 * ITC2007 1.0<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
public class ItcCsvStat {
    private int iCnt = 0;
    private double iMin, iMax, iSum, iSum2;
    private String iName;
    private int iMinLine;
    protected static DecimalFormat sDF3 = new DecimalFormat("0.000");
    
    /** Constructor
     * @param name instance name
     */
    public ItcCsvStat(String name) {
        iName = name;
    }
    
    /**
     * Add solution value
     */
    public void add(double val) {
        if (iCnt==0) {
            iMinLine = 0;
            iMin = iMax = val;
            iSum = val; iSum2 = val*val;
        } else {
            if (val<iMin) iMinLine = iCnt;
            iMin = Math.min(iMin, val);
            iMax = Math.max(iMax, val);
            iSum += val;
            iSum2 += val*val;
        }
        iCnt++;
    }
    
    /** Minimal given solution value */
    public double getMin() { return iMin; }
    /** Maximal given solution value */
    public double getMax() { return iMax; }
    /** Average solution value */
    public double getAvg() { return iSum/iCnt; }
    /** Line with the minimal solution value */
    public int getMinLine() { return iMinLine; }
     
    /** Return min/max/avg solution value */
    public String toString() {
        return 
            iName+
            ": min="+sDF3.format(iMin)+
            ", max="+sDF3.format(iMax)+
            ", avg="+sDF3.format(iSum/iCnt)+
            ", minLine="+(1+iMinLine)+
            ", lines="+iCnt;
    }

    /**
     * Process all given CSV files, output best achieved solution for each of them
     */
    public static void main(String[] args) {
        try {
            double totalMin = 0;
            for (int i=0;i<args.length;i++) {
                System.out.println("file: "+args[i]);
                CSVFile f = new CSVFile(new File(args[i]));
                ItcCsvStat s = new ItcCsvStat("total");
                for (Enumeration e=f.getLines().elements();e.hasMoreElements();) {
                    CSVLine l = (CSVLine)e.nextElement();
                    s.add(l.getField("total").toDouble());
                }
                System.out.println(s);
                CSVLine l = f.getLine(s.getMinLine());
                for (Enumeration e=f.getHeader().fields();e.hasMoreElements();) {
                    CSVField h = (CSVField)e.nextElement();
                    System.out.println("  "+h+"="+l.getField(h.toString()));
                }
                System.out.println("--------------------------------------------------");
                System.out.println();
                totalMin += s.getMin();
            }
            System.out.println("Total min: "+sDF3.format(totalMin));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
