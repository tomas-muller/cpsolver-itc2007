package net.sf.cpsolver.itc.tim.model;

import java.util.Iterator;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Value;
import net.sf.cpsolver.itc.heuristics.search.ItcTabuSearch.TabuElement;

/**
 * Representation of a location (value).
 * An assignment of an event is a combination of an available slot and a possible room.
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
public class TimLocation extends Value<TimEvent, TimLocation> implements TabuElement {
	private int iTime = 0;
	private TimRoom iRoom = null;
	private int iHashCode = 0;
	
	/**
	 * Constructor
	 * @param event an event
	 * @param time assigned time (slot)
	 * @param room assigned room
	 */
	public TimLocation(TimEvent event, int time, TimRoom room) {
	    super(event);
		iTime = time; iRoom = room;
		iHashCode = 45*(iRoom==null?0:1+(int)iRoom.getId())+iTime;
	}
	
	/** Assigned room */
	public TimRoom room() {
		return iRoom;
	}
	
	/** Assigned time (slot) */
	public int time() {
		return iTime;
	}
	
	/** String representation */
	public String toString() {
	    return variable().getName()+"("+getName()+")";
	}
	
	/** Assignment score (as string) */
	public String scoreStr(Assignment<TimEvent, TimLocation> assignment) {
        int[] score = score(assignment);
        return score[0]+","+score[1]+","+score[2]+","+precedenceViolations(assignment);
	}
	
	/** Location name (room @ time) */
	public String getName() {
	    return room()+"@"+time();//(char)('A'+(time()/9))+(time()%9);
	}
	
	/** Assignment score of this assignment
	 * @return number of students having single event a day, last slot of a day, two consecutive classes
	 */
	public int[] score(Assignment<TimEvent, TimLocation> assignment) {
        int[] score = new int[] {0,0,0};
        TimEvent event = (TimEvent)variable();
        int d = iTime / 9;
        int t = iTime % 9;
        if (t==8) score[1]=event.students().size();
        int dfs = d*9, dls = dfs + 8; 
        for (TimStudent student: event.students()) {
            TimLocation[] table = student.getTable(assignment);
            int eventsADay = 1;
            int left = 0;
            boolean hole = false;
            for (int s=iTime-1;s>=dfs;s--) {
                if (table[s]==null || table[s].variable().equals(event))
                    hole=true;
                else {
                    eventsADay++;
                    if (!hole) left++;
                }
            }
            int right = 0;
            hole = false; 
            for (int s=iTime+1;s<=dls;s++) {
                if (table[s]==null || table[s].variable().equals(event))
                    hole = true;
                else {
                    eventsADay++;
                    if (!hole) right++;
                }
            }
            /*
            for (int x=0;x<9;x++) {
                if (x==t) continue;
                int s = d*9 + x;
                if (table[s]!=null && !table[s].variable().equals(event))
                    eventsADay++;
            }
            int left = 0;
            for (int x=t-1;x>=0;x--) {
                int s = d*9 + x;
                if (table[s]==null || table[s].variable().equals(event)) break;
                left++;
            }
            int right = 0;
            for (int x=t+1;x<9;x++) {
                int s = d*9 + x;
                if (table[s]==null || table[s].variable().equals(event)) break;
                right++;
            }
            */
            if (eventsADay==1) score[0]++;
            if (eventsADay==2) score[0]--;
            if (left+right+1>2) {
                score[2]+=(left+right+1-2)-Math.max(0,left-2)-Math.max(0,right-2);
            }
        }
        return score;
	}
	
	/** True, if the assigned time is the last of a day */
	public boolean isLastTime() {
	    return iTime%9==8;
	}
	
	/** Number of students having this event as the last one of a day */
	public int lastTimePenalty() {
	    return (iTime%9==8?((TimEvent)variable()).students().size():0);
	}
	
	/** Weight for not having a room assigned (if {@link TimLocation#room()} is null) */
	public int noRoomPenalty() {
	    return (room()==null?TimModel.sNoRoomWeight:0);
	}
	
	/** Weighted sum of all criteria (violated soft constraints) for this location */
	public int toInt(Assignment<TimEvent, TimLocation> assignment) {
        TimEvent event = (TimEvent)variable();
        int d = iTime / 9, t = iTime % 9;
        int score = (t==8?event.students().size():0);
        int dfs = d*9, dls = dfs + 8; 
        for (TimStudent student: event.students()) {
            TimLocation[] table = student.getTable(assignment);
            int eventsADay = 1;
            int left = 0;
            boolean hole = false;
            for (int s=iTime-1;s>=dfs;s--) {
                if (table[s]==null || table[s].variable().equals(event))
                    hole=true;
                else {
                    eventsADay++;
                    if (!hole) left++;
                }
            }
            int right = 0;
            hole = false; 
            for (int s=iTime+1;s<=dls;s++) {
                if (table[s]==null || table[s].variable().equals(event))
                    hole = true;
                else {
                    eventsADay++;
                    if (!hole) right++;
                }
            }
            if (eventsADay==1) score++;
            if (eventsADay==2) score--;
            if (left+right+1>2) {
                score+=(left+right+1-2)-Math.max(0,left-2)-Math.max(0,right-2);
            }
        }
	    return score +
	    		TimModel.sPrecedenceViolationWeight * precedenceViolations(assignment) +
	    		(room() == null ? TimModel.sNoRoomWeight  :0) // * event.students().size() : 0)
	    		;
	}
	
    /** Number of violated precedence constraints */
	public int precedenceViolations(Assignment<TimEvent, TimLocation> assignment) {
	    int violations = 0;
	    TimEvent event = (TimEvent)variable();
	    for (Iterator<TimEvent> i=event.predecessors().iterator();i.hasNext();) {
	        TimLocation prev = assignment.getValue(i.next());
	        if (prev!=null && prev.time()>=time()) violations++;//+=Math.min(event.students().size(),((TimEvent)prev.variable()).students().size());
	    }
        for (Iterator<TimEvent> i=event.successors().iterator();i.hasNext();) {
            TimLocation next = assignment.getValue(i.next());
            if (next!=null && time()>=next.time()) violations++;//+=Math.min(event.students().size(),((TimEvent)next.variable()).students().size());
        }
	    return violations;
	}
	
	/** Weighted sum of all criteria (violated soft constraints) for this location */
	public double toDouble(Assignment<TimEvent, TimLocation> assignment) {
	    return toInt(assignment);
	}

	/** Compare two locations for equality */
	public boolean equals(Object another) {
		if (another==null || !(another instanceof TimLocation)) return false;
		TimLocation location = (TimLocation)another;
		return variable().getId()==location.variable().getId() && hashCode()==location.hashCode();
	}
	
	/** Hash code */
	public int hashCode() {
		return iHashCode;
	}
	
	/** Tabu element -- variable and assigned time (room assignment is ignored) */
    public Object tabuElement() {
        return new Long(45*((TimEvent)variable()).getId()+time());
    }
}
