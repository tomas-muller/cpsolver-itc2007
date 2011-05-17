package net.sf.cpsolver.itc.tim.model;

import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.ConstraintListener;


/**
 * Representation of a student constraint. It is not allowed to assign two 
 * events that are attended by the same student a the same time. 
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
public class TimStudent extends Constraint<TimEvent, TimLocation> {
    private TimLocation iTable[] = new TimLocation[45];
	
    /**
     * Constructor
     * @param id unique student identifier
     */
	public TimStudent(long id) {
		super();
		iAssignedVariables=null;
		iId = id;
        for (int i=0;i<45;i++)
            iTable[i]=null;
	}
	
	/**
	 * Compute conflicts: check whether this student is attending some other event at the given time
	 */
    public void computeConflicts(TimLocation location, Set<TimLocation> conflicts) {
		if (iTable[location.time()]!=null && !iTable[location.time()].variable().equals(location.variable()))
			conflicts.add(iTable[location.time()]);
	}
    
    /**
     * Check for conflicts: check whether this student is attending some other event at the given time
     */
    public boolean inConflict(TimLocation location) {
        return iTable[location.time()]!=null && !iTable[location.time()].variable().equals(location.variable());
    }
    
    /**
     * Two events that are attended by this student are consistent when assigned to different times
     */
    public boolean isConsistent(TimLocation loc1, TimLocation loc2) {
        return loc1.time()==loc2.time(); 
    }
	
    /**
     * Compute objectives for this student
     * @param oneDay include single event on a day criteria
     * @param lastTime include last slot of the day criteria
     * @param threeMore include more than two events consecutively criteria
     */
	public int score(boolean oneDay, boolean lastTime, boolean threeMore) {
		int score = 0;
		for (int d=0;d<5;d++) {
			int inRow = 0;
			int eventsADay = 0;
			for (int t=0;t<9;t++) {
				int s = d*9 + t;
				if (iTable[s]!=null) {
					inRow ++;
					eventsADay++;
					if (lastTime && t==8) score++;
				} else {
					inRow = 0;
				}
				if (threeMore && inRow>2) score++;
			}
			if (oneDay && eventsADay==1) score++;;
		}
		return score;
	}
	
	/** String representation */
	public String toString() {
		return "s"+getId();
	}

	/** Update student assignment table */
    public void assigned(long iteration, TimLocation value) {
        //super.assigned(iteration, value);
        if (iTable[((TimLocation)value).time()]!=null) {
            Set<TimLocation> confs = new HashSet<TimLocation>();
            confs.add(iTable[((TimLocation)value).time()]);
            iTable[((TimLocation)value).time()].variable().unassign(iteration);
            iTable[((TimLocation)value).time()]=value;
            if (iConstraintListeners!=null)
                for (ConstraintListener<TimLocation> listener: iConstraintListeners)
                	listener.constraintAfterAssigned(iteration, this, value, confs);
        } else {
            iTable[((TimLocation)value).time()]=(TimLocation)value;
        }
    }
        
    /** Update student assignment table */
    public void unassigned(long iteration, TimLocation value) {
        //super.unassigned(iteration, value);
        iTable[((TimLocation)value).time()]=null;
    }
    
    /** Student assignment table */
    public TimLocation[] getTable() {
        return iTable;
    }
    
    /** Return location that is assigned at the given time (if any) */
    public TimLocation getLocation(int time) {
        return iTable[time];
    }
    
    /** Compare two students using their ids */
    public int compareTo(Constraint<TimEvent, TimLocation> o) {
        return toString().compareTo(o.toString());
    }
}
