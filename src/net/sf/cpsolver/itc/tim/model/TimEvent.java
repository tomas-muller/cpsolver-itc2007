package net.sf.cpsolver.itc.tim.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.AssignmentComparable;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcLazySwap;
import net.sf.cpsolver.itc.heuristics.neighbour.ItcSwap.Swapable;

/**
 * Representation of an event (variable).
 * An event has a list of students, list of possible rooms (rooms of 
 * enough size containing all required features), list of predecessors and 
 * successors (from precedence constraints), and a time availability matrix.
 * An assignment of an event is {@link TimLocation} that is a combination
 * of an available slot and a possible room.
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
public class TimEvent extends Variable<TimEvent, TimLocation> implements Swapable<TimEvent, TimLocation>, AssignmentComparable<TimEvent, TimEvent, TimLocation> {
    private static Logger sLog = Logger.getLogger(TimEvent.class);
	private List<TimStudent> iStudents = new ArrayList<TimStudent>();
	private List<TimRoom> iRooms = new ArrayList<TimRoom>();
	private boolean[] iAvailable = new boolean[45];
	
	private Set<TimEvent> iPredecessors = new HashSet<TimEvent>(), iSuccessors = new HashSet<TimEvent>();
	private List<TimPrecedence> iPrecedences = new ArrayList<TimPrecedence>();
	
	/**
	 * Constructor
	 * @param id event unique identifier
	 */
	public TimEvent(int id) {
		super();
		iId = id;
		for (int i=0;i<45;i++)
		    iAvailable[i]=true;
	}
	
	/**
	 * Students attending this event
	 */
	public List<TimStudent> students() {
		return iStudents;
	}
	
	/**
	 * Rooms that this event can be placed into
	 */
	public List<TimRoom> rooms() {
		return iRooms;
	}
	
	/**
	 * True if this event can be placed in the given slot
	 */
	public boolean isAvailable(int slot) {
	    if (!iAvailable[slot]) return false;
	    return true;
	}
	
	/**
	 * Set whether this even can be placed in the given slot
	 */
	public void setAvailable(int slot, boolean av) {
	    iAvailable[slot]=av;
	}
	
	/**
	 * Initialization -- compute variable domain
	 */
	public void init(Assignment<TimEvent, TimLocation> assignment, boolean allowProhibitedTime, boolean allowNoRoom) {
	    if (sLog.isDebugEnabled())
	        sLog.debug("Event "+this+" predecessors:"+predecessors()+", successors:"+successors());
	    for (int slot=getMinStart()-1;slot>=0;slot--) iAvailable[slot]=false;
	    for (int slot=getMaxStart()+1;slot<45;slot++) iAvailable[slot]=false;
        List<TimLocation> values = new ArrayList<TimLocation>();
		for (int time=firstAvailableTime(assignment);time<=lastAvailableTime(assignment);time++) {
		    if (!allowProhibitedTime && !isAvailable(time)) continue;
		    if (allowNoRoom) values.add(new TimLocation(this, time, null));
			for (TimRoom room: rooms())
				values.add(new TimLocation(this, time, room));
		}
		setValues(values);
	}
	
	/** String representation */
	public String toString(Assignment<TimEvent, TimLocation> assignment) {
	    String pre = "";
	    for (Iterator<TimEvent> i=predecessors().iterator();i.hasNext();) {
	        TimEvent e = i.next();
	        if (pre.length()==0) pre+="[";
	        pre+=e.getName();
	        TimLocation l = assignment.getValue(e);
	        if (l!=null) pre+="="+l.getName();
	        if (i.hasNext()) pre+=","; else pre+="]";
	    }
	    String suc = "";
        for (Iterator<TimEvent> i=successors().iterator();i.hasNext();) {
            TimEvent e = i.next();
            if (suc.length()==0) suc+="[";
            suc+=e.getName();
            TimLocation l = assignment.getValue(e);
            if (l!=null) suc+="="+l.getName();
            if (i.hasNext()) suc+=","; else suc+="]";
        }
		return getName()+" <"+getMinStart()+".."+getMaxStart()+"> [vals:"+getDomainSize(assignment)+",stds:"+students().size()+",sce:"+nrStudentCorrelatedEvents()+",<:"+pre+",>:"+suc+"]";
	}
	
	/** Event name (e + id) */
	public String getName() {
	    return "e"+getId();
	}
	
	/** First available slot (predecessors are considered) */
	public int firstAvailableTime(Assignment<TimEvent, TimLocation> assignment) {
        int firstTime = firstAvailableSlot();
        for (TimEvent predecessor: predecessors()) {
            TimLocation location = assignment.getValue(predecessor);
            firstTime = Math.max(firstTime,(location==null?predecessor.firstAvailableTime(assignment):location.time())+1);
        }
        return firstTime;
	}
	
	/** Last available slot (successors are considered) */
    public int lastAvailableTime(Assignment<TimEvent, TimLocation> assignment) {
        int lastTime = lastAvailableSlot();
        for (TimEvent successor: successors()) {
            TimLocation location = assignment.getValue(successor);
            lastTime = Math.min(lastTime,(location==null?successor.lastAvailableTime(assignment):location.time())-1);
        }
        return lastTime;
    }

    /** Domain size (other events of students attending this event and room availabilities are considered) */
    public int getDomainSize(Assignment<TimEvent, TimLocation> assignment) {
        int size =0;
        int firstTime = firstAvailableTime(assignment); 
        int lastTime = lastAvailableTime(assignment);
        time: for (int time=firstTime;time<=lastTime;time++) {
            if (!isAvailable(time)) continue time;
            for (TimStudent s: students()) {
                if (s.getLocation(assignment, time)!=null) continue time;
            }
            for (TimRoom r: rooms()) {
                if (r.getLocation(assignment, time)==null) size++; 
            }
        }
        return size;
	}
    
    /** Number of available times (other events of students attending this event are considered) */
    public int getNrAvailableTimes(Assignment<TimEvent, TimLocation> assignment) {
        int size =0;
        int firstTime = firstAvailableTime(assignment); 
        int lastTime = lastAvailableTime(assignment);
        time: for (int time=firstTime;time<=lastTime;time++) {
            if (!isAvailable(time)) continue time;
            for (TimStudent s: students()) {
                if (s.getLocation(assignment, time)!=null) continue time;
            }
            size++; 
        }
        return size;
    }
	
    /** List of predecessor events */
	public Set<TimEvent> predecessors() {
	    return iPredecessors;
	}

	/** List of successor events */
    public Set<TimEvent> successors() {
        return iSuccessors;
    }
    
    /** First available slot */
    public int firstAvailableSlot() {
        for (int i=0;i<45;i++)
            if (isAvailable(i)) return i;
        return 45;
    }
    
    /** Last available slot */
    public int lastAvailableSlot() {
        for (int i=44;i>=0;i--)
            if (isAvailable(i)) return i;
        return -1;
    }
    
    private Set<TimEvent> iCorrelatedEvents = null;
    /** Number of correlated events (considering students) */
    public int nrStudentCorrelatedEvents() {
        if (iCorrelatedEvents==null) { 
            iCorrelatedEvents = new HashSet<TimEvent>();
            for (TimStudent student: students())
                iCorrelatedEvents.addAll(student.variables());
            iCorrelatedEvents.remove(this);
        }
        return iCorrelatedEvents.size();
    }

    /** Compare two events on the number of correlated events and on domain size vs. number of constraints ratio */
    public int compareTo(Assignment<TimEvent, TimLocation> assignment, TimEvent e) {
        int cmp = -Double.compare(nrStudentCorrelatedEvents(),e.nrStudentCorrelatedEvents());
        if (cmp!=0) return cmp;
        
        cmp = Double.compare(((double)getDomainSize(assignment))/(constraints().size()-rooms().size()),
                ((double)e.getDomainSize(assignment))/(e.constraints().size()-e.rooms().size()));
        
        if (cmp!=0) return cmp;
        
        return super.compareTo(e);
    }
    
    /** Find a swap between this and the given events */
    public Neighbour<TimEvent, TimLocation> findSwap(Assignment<TimEvent, TimLocation> assignment, TimEvent another) {
        TimEvent e1 = this;
        TimEvent e2 = another;
        TimLocation l1 = assignment.getValue(e1);
        TimLocation l2 = assignment.getValue(e2);
        if (l1==null || l2==null || l1.time()==l2.time()) return null;
        if (!((TTComp02Model)e1.getModel()).isAllowProhibitedTime() && (!e2.isAvailable(l1.time()) || !e1.isAvailable(l2.time()))) return null;
        for (TimStudent s: e1.students()) {
            TimLocation conflict = s.getLocation(assignment, l2.time());
            if (conflict!=null && !conflict.equals(l2)) return null;
        }
        for (TimStudent s: e2.students()) {
            TimLocation conflict = s.getLocation(assignment, l1.time());
            if (conflict!=null && !conflict.equals(l1)) return null;
        }
        List<TimRoom> r1 = new ArrayList<TimRoom>(e1.rooms().size());
        for (TimRoom r: e1.rooms()) {
            TimLocation conflict = r.getLocation(assignment, l2.time());
            if (conflict==null || conflict.equals(l2)) r1.add(r);
        }
        if (r1.isEmpty() && !((TTComp02Model)e1.getModel()).isAllowNoRoom()) return null;
        List<TimRoom> r2 = new ArrayList<TimRoom>(e2.rooms().size());
        for (TimRoom r: e2.rooms()) {
            TimLocation conflict = r.getLocation(assignment, l1.time());
            if (conflict==null || conflict.equals(l1)) r2.add(r);
        }
        if (r2.isEmpty() && !((TTComp02Model)e1.getModel()).isAllowNoRoom()) return null;
        TimLocation nl1 = new TimLocation(e1, l2.time(), (TimRoom)ToolBox.random(r1));
        TimLocation nl2 = new TimLocation(e2, l1.time(), (TimRoom)ToolBox.random(r2));
        return new ItcLazySwap<TimEvent, TimLocation>(assignment, nl1, nl2); 
    }
    
    /** Minimal possible starting time (first available slot combined with min start time of all predecessors) */
    public int getMinStart() {
        int min = 0;
        for (TimEvent e: predecessors())
            min = Math.max(min, e.getMinStart()+1);
        while (min<45 && !iAvailable[min]) min++;
        return min;
    }
    
    /** Maximal possible start time (last available slot combined with max start time of all successors) */
    public int getMaxStart() {
        int max = 44;
        for (TimEvent e: successors())
            max = Math.min(max, e.getMaxStart()-1);
        while (max>=0 && !iAvailable[max]) max--;
        return max;
    }

    /** A location was assigned to this even -- notify appropriate constraints. Default implementation
     * is overridden to improve solver speed. */
    /*
    public void variableAssigned(Assignment<TimEvent, TimLocation> assignment, long iteration, TimLocation location) {
        if (location==null) return;
        iValue = location;
        if (location.room()!=null) location.room().assigned(assignment, iteration, location);
        for (TimStudent student: students())
            student.assigned(assignment, iteration, location);
    }
    */
    
    /** A location was unassigned from this even -- notify appropriate constraints. Default implementation
     * is overridden to improve solver speed. */
    /*
    public void variableUnassigned(Assignment<TimEvent, TimLocation> assignment, long iteration) {
        if (iValue==null) return;
        TimLocation location = iValue;
        iValue = null;
        if (location.room() != null) location.room().unassigned(assignment, iteration, location);
        for (TimStudent student: students())
            student.unassigned(assignment, iteration, location);
    }
    */
    
    /**
     * Precedence constraints of this event
     */
    public List<TimPrecedence> getPrecedences() {
    	return iPrecedences;
    }
}