package net.sf.cpsolver.itc.tim.model;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.util.ToolBox;
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
public class TimEvent extends Variable implements Swapable {
    private static Logger sLog = Logger.getLogger(TimEvent.class);
	private Vector iStudents = new Vector();
	private Vector iRooms = new Vector();
	private boolean[] iAvailable = new boolean[45];
	
	private HashSet iPredecessors = new HashSet(), iSuccessors = new HashSet();
	
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
	public Vector students() {
		return iStudents;
	}
	
	/**
	 * Rooms that this event can be placed into
	 */
	public Vector rooms() {
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
	public void init(boolean allowProhibitedTime, boolean allowNoRoom) {
	    if (sLog.isDebugEnabled())
	        sLog.debug("Event "+this+" predecessors:"+predecessors()+", successors:"+successors());
	    for (int slot=getMinStart()-1;slot>=0;slot--) iAvailable[slot]=false;
	    for (int slot=getMaxStart()+1;slot<45;slot++) iAvailable[slot]=false;
        Vector values = new Vector();
		for (int time=firstAvailableTime();time<=lastAvailableTime();time++) {
		    if (!allowProhibitedTime && !isAvailable(time)) continue;
		    if (allowNoRoom) values.addElement(new TimLocation(this, time, null));
			for (Enumeration e=rooms().elements();e.hasMoreElements();)
				values.addElement(new TimLocation(this, time, (TimRoom)e.nextElement()));
		}
		setValues(values);
	}

	/** String representation */
	public String toString() {
	    String pre = "";
	    for (Iterator i=predecessors().iterator();i.hasNext();) {
	        TimEvent e = (TimEvent)i.next();
	        if (pre.length()==0) pre+="[";
	        pre+=e.getName();
	        if (e.getAssignment()!=null) pre+="="+e.getAssignment().getName();
	        if (i.hasNext()) pre+=","; else pre+="]";
	    }
	    String suc = "";
        for (Iterator i=successors().iterator();i.hasNext();) {
            TimEvent e = (TimEvent)i.next();
            if (suc.length()==0) suc+="[";
            suc+=e.getName();
            if (e.getAssignment()!=null) suc+="="+e.getAssignment().getName();
            if (i.hasNext()) suc+=","; else suc+="]";
        }
		return getName()+" <"+getMinStart()+".."+getMaxStart()+"> [vals:"+getDomainSize()+",stds:"+students().size()+",sce:"+nrStudentCorrelatedEvents()+",<:"+pre+",>:"+suc+"]";
	}
	
	/** Event name (e + id) */
	public String getName() {
	    return "e"+getId();
	}
	
	/** First available slot (predecessors are considered) */
	public int firstAvailableTime() {
        int firstTime = firstAvailableSlot();
        for (Iterator i=predecessors().iterator();i.hasNext();) {
            TimEvent predecessor = (TimEvent)i.next();
            TimLocation location = (TimLocation)predecessor.getAssignment();
            firstTime = Math.max(firstTime,(location==null?predecessor.firstAvailableTime():location.time())+1);
        }
        return firstTime;
	}
	
	/** Last available slot (successors are considered) */
    public int lastAvailableTime() {
        int lastTime = lastAvailableSlot();
        for (Iterator i=successors().iterator();i.hasNext();) {
            TimEvent successor = (TimEvent)i.next();
            TimLocation location = (TimLocation)successor.getAssignment();
            lastTime = Math.min(lastTime,(location==null?successor.lastAvailableTime():location.time())-1);
        }
        return lastTime;
    }

    /** Domain size (other events of students attending this event and room availabilities are considered) */
    public int getDomainSize() {
        int size =0;
        int firstTime = firstAvailableTime(); 
        int lastTime = lastAvailableTime();
        time: for (int time=firstTime;time<=lastTime;time++) {
            if (!isAvailable(time)) continue time;
            for (Enumeration e=students().elements();e.hasMoreElements();) {
                TimStudent s = (TimStudent)e.nextElement();
                if (s.getLocation(time)!=null) continue time;
            }
            for (Enumeration e=rooms().elements();e.hasMoreElements();) {
                TimRoom r = (TimRoom)e.nextElement();
                if (r.getLocation(time)==null) size++; 
            }
        }
        return size;
	}
    
    /** Number of available times (other events of students attending this event are considered) */
    public int getNrAvailableTimes() {
        int size =0;
        int firstTime = firstAvailableTime(); 
        int lastTime = lastAvailableTime();
        time: for (int time=firstTime;time<=lastTime;time++) {
            if (!isAvailable(time)) continue time;
            for (Enumeration e=students().elements();e.hasMoreElements();) {
                TimStudent s = (TimStudent)e.nextElement();
                if (s.getLocation(time)!=null) continue time;
            }
            size++; 
        }
        return size;
    }
	
    /** List of predecessor events */
	public Set predecessors() {
	    return iPredecessors;
	}

	/** List of successor events */
    public Set successors() {
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
    
    private HashSet iCorrelatedEvents = null;
    /** Number of correlated events (considering students) */
    public int nrStudentCorrelatedEvents() {
        if (iCorrelatedEvents==null) { 
            iCorrelatedEvents = new HashSet();
            int weightedNrCorrelatedEvents = 0; 
            for (Enumeration e=students().elements();e.hasMoreElements();) {
                TimStudent student = (TimStudent)e.nextElement();
                iCorrelatedEvents.addAll(student.variables());
            }
            iCorrelatedEvents.remove(this);
        }
        return iCorrelatedEvents.size();
    }

    /** Compare two events on the number of correlated events and on domain size vs. number of constraints ratio */
    public int compareTo(Object o) {
        TimEvent e = (TimEvent)o;
        
        int cmp = 0;
        
        cmp = -Double.compare(nrStudentCorrelatedEvents(),e.nrStudentCorrelatedEvents());
        if (cmp!=0) return cmp;
        
        cmp = Double.compare(((double)getDomainSize())/(constraints().size()-rooms().size()),
                ((double)e.getDomainSize())/(e.constraints().size()-e.rooms().size()));
        
        if (cmp!=0) return cmp;
        
        return super.compareTo(o);
    }
    
    /** Find a swap between this and the given events */
    public Neighbour findSwap(Variable another) {
        TimEvent e1 = this;
        TimEvent e2 = (TimEvent)another;
        TimLocation l1 = (TimLocation)e1.getAssignment();
        TimLocation l2 = (TimLocation)e2.getAssignment();
        if (l1==null || l2==null || l1.time()==l2.time()) return null;
        if (!((TTComp02Model)e1.getModel()).isAllowProhibitedTime() && (!e2.isAvailable(l1.time()) || !e1.isAvailable(l2.time()))) return null;
        for (Enumeration e=e1.students().elements();e.hasMoreElements();) {
            TimStudent s = (TimStudent)e.nextElement();
            TimLocation conflict = s.getLocation(l2.time());
            if (conflict!=null && !conflict.equals(l2)) return null;
        }
        for (Enumeration e=e2.students().elements();e.hasMoreElements();) {
            TimStudent s = (TimStudent)e.nextElement();
            TimLocation conflict = s.getLocation(l1.time());
            if (conflict!=null && !conflict.equals(l1)) return null;
        }
        Vector r1 = new Vector(e1.rooms().size());
        for (Enumeration e=e1.rooms().elements();e.hasMoreElements();) {
            TimRoom r = (TimRoom)e.nextElement();
            TimLocation conflict = r.getLocation(l2.time());
            if (conflict==null || conflict.equals(l2)) r1.add(r);
        }
        if (r1.isEmpty() && !((TTComp02Model)e1.getModel()).isAllowNoRoom()) return null;
        Vector r2 = new Vector(e2.rooms().size());
        for (Enumeration e=e2.rooms().elements();e.hasMoreElements();) {
            TimRoom r = (TimRoom)e.nextElement();
            TimLocation conflict = r.getLocation(l1.time());
            if (conflict==null || conflict.equals(l1)) r2.add(r);
        }
        if (r2.isEmpty() && !((TTComp02Model)e1.getModel()).isAllowNoRoom()) return null;
        TimLocation nl1 = new TimLocation(e1, l2.time(), (TimRoom)ToolBox.random(r1));
        TimLocation nl2 = new TimLocation(e2, l1.time(), (TimRoom)ToolBox.random(r2));
        return new ItcLazySwap(nl1, nl2); 
    }
    
    /** Minimal possible starting time (first available slot combined with min start time of all predecessors) */
    public int getMinStart() {
        int min = 0;
        for (Iterator i=predecessors().iterator();i.hasNext();)
            min = Math.max(min, ((TimEvent)i.next()).getMinStart()+1);
        while (min<45 && !iAvailable[min]) min++;
        return min;
    }
    
    /** Maximal possible start time (last available slot combined with max start time of all successors) */
    public int getMaxStart() {
        int max = 44;
        for (Iterator i=successors().iterator();i.hasNext();) {
            max = Math.min(max, ((TimEvent)i.next()).getMaxStart()-1);
        }
        while (max>=0 && !iAvailable[max]) max--;
        return max;
    }

    /** A location was assigned to this even -- notify appropriate constraints. Default implementation
     * is overridden to improve solver speed. */
    public void assign(long iteration, Value value) {
        getModel().beforeAssigned(iteration,value);
        if (iValue!=null) unassign(iteration);
        if (value==null) return;
        iValue = value;
        TimLocation location = (TimLocation)iValue;
        if (location.room()!=null) location.room().assigned(iteration, value);
        for (Enumeration e=students().elements();e.hasMoreElements();) {
            TimStudent student = (TimStudent)e.nextElement();
            student.assigned(iteration, value);
        }
        value.assigned(iteration);
        getModel().afterAssigned(iteration,value);
    }
    
    /** A location was unassigned from this even -- notify appropriate constraints. Default implementation
     * is overridden to improve solver speed. */
    public void unassign(long iteration) {
        if (iValue==null) return;
        getModel().beforeUnassigned(iteration,iValue);
        Value oldValue = iValue;
        iValue = null;
        TimLocation location = (TimLocation)oldValue;
        if (location.room()!=null) location.room().unassigned(iteration, oldValue);
        for (Enumeration e=students().elements();e.hasMoreElements();) {
            TimStudent student = (TimStudent)e.nextElement();
            student.unassigned(iteration, oldValue);
        }
        getModel().afterUnassigned(iteration,oldValue);
    }

}