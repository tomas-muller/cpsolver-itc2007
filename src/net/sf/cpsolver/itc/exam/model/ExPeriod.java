package net.sf.cpsolver.itc.exam.model;

/**
 * Representation of a period. Each period has a day, a time, a length in
 * minutes, and a weight. It is expected that periods are not overlapping.
 * 
 * @version
 * ITC2007 1.0<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not see
 * <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class ExPeriod implements Comparable<ExPeriod> {
    private int iIndex = -1;
    private String iTimeStr;
    private String iDayStr;
    private int iLength;
    private int iDay, iTime;
    private int iWeight;
    private ExPeriod iPrev, iNext;
    
    /**
     * Constructor
     * @param day day
     * @param time start time
     * @param length length in minutes
     * @param weight weight (penalty for use)
     */
    public ExPeriod(String day, String time, int length, int weight) {
        iDayStr = day;
        iTimeStr = time;
        iLength = length;
        iWeight = weight;
    }

    /** Day string as given in constructor */
    public String getDayStr() {
        return iDayStr;
    }
    /** Day index */
    public int getDay() {
        return iDay;
    }
    /** Time string as given in constructor */
    public String getTimeStr() {
        return iTimeStr;
    }
    /** Time index */
    public int getTime() {
        return iTime;
    }
    /** Length in minutes */
    public int getLength() {
        return iLength;
    }
    /** Period index (among all periods of the problem) */
    public int getIndex() {
        return iIndex;
    }
    /** Period weight (penalty for use) */
    public int getWeight() {
        return iWeight;
    }
    /** Previous period */
    public ExPeriod prev() { return iPrev; }
    /** Next period */
    public ExPeriod next() { return iNext; }
    /** Set period index, day index, and time index */
    public void setIndex(int index, int day, int time) {
        iIndex = index;
        iDay = day;
        iTime = time;
    }
    /** Set previous period */
    public void setPrev(ExPeriod prev) { iPrev = prev;}
    /** Set next period */
    public void setNext(ExPeriod next) { iNext = next;}
    /** String representation */
    public String toString() { return getDayStr()+" "+getTimeStr(); }
    /** Hash code -- period index */
    public int hashCode() { return iIndex; }
    /** Compare two periods for equality */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ExPeriod)) return false;
        return getIndex()==((ExPeriod)o).getIndex();
    }
    /** Compare two periods on index */
    public int compareTo(ExPeriod p) {
        return Double.compare(getIndex(), p.getIndex());
    }
}
