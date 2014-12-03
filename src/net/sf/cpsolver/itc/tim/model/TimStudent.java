package net.sf.cpsolver.itc.tim.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.CanInheritContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.ConstraintListener;


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
public class TimStudent extends ConstraintWithContext<TimEvent, TimLocation, TimStudent.Context> implements CanInheritContext<TimEvent, TimLocation, TimStudent.Context> {

	/**
     * Constructor
     * @param id unique student identifier
     */
	public TimStudent(long id) {
		super();
		iId = id;
	}
	
	/**
	 * Compute conflicts: check whether this student is attending some other event at the given time
	 */
    public void computeConflicts(Assignment<TimEvent, TimLocation> assignment, TimLocation location, Set<TimLocation> conflicts) {
    	TimLocation conflict = getContext(assignment).getLocation(location.time());
    	if (conflict != null && !conflict.variable().equals(location.variable()))
    		conflicts.add(conflict);
	}
    
    /**
     * Check for conflicts: check whether this student is attending some other event at the given time
     */
    public boolean inConflict(Assignment<TimEvent, TimLocation> assignment, TimLocation location) {
    	TimLocation conflict = getContext(assignment).getLocation(location.time());
        return conflict != null && !conflict.variable().equals(location.variable());
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
	public int score(Assignment<TimEvent, TimLocation> assignment, boolean oneDay, boolean lastTime, boolean threeMore) {
		return getContext(assignment).score(oneDay, lastTime, threeMore);
	}
	
	/** String representation */
	public String toString() {
		return "s"+getId();
	}
	
	/** Update student assignment table */
	public void assigned(Assignment<TimEvent, TimLocation> assignment, long iteration, TimLocation location) {
		Context context = getContext(assignment);
		TimLocation conflict = context.getLocation(location.time());
		if (iConstraintListeners != null && conflict != null) {
			Set<TimLocation> confs = new HashSet<TimLocation>();
			confs.add(conflict);
			assignment.unassign(iteration, conflict.variable());
			context.assigned(assignment, location);
			for (ConstraintListener<TimEvent, TimLocation> listener: iConstraintListeners)
				listener.constraintAfterAssigned(assignment, iteration, this, location, confs);
		} else {
			context.assigned(assignment, location);
		}
    }
		
	/** Update student assignment table */
    public void unassigned(Assignment<TimEvent, TimLocation> assignment, long iteration, TimLocation location) {
    	getContext(assignment).unassigned(assignment, location);
    }
        
    /** Return location that is assigned at the given time (if any) */
    public TimLocation getLocation(Assignment<TimEvent, TimLocation> assignment, int time) {
    	return getContext(assignment).getLocation(time);
    }
    
    /** Student assignment table */
    public TimLocation[] getTable(Assignment<TimEvent, TimLocation> assignment) {
        return getContext(assignment).getTable();
    }
    
    /** Compare two students using their ids */
    public int compareTo(Constraint<TimEvent, TimLocation> o) {
        return toString().compareTo(o.toString());
    }
    
    public class Context implements AssignmentConstraintContext<TimEvent, TimLocation> {
        private TimLocation[] iTable = new TimLocation[45];

    	public Context(Assignment<TimEvent, TimLocation> assignment) {
            for (int i = 0; i < 45; i++)
            	iTable[i] = null;
            for (TimEvent event: variables()) {
            	TimLocation location = assignment.getValue(event);
            	if (location != null)
            		iTable[location.time()] = location;
            }
    	}
    	
    	public Context(Assignment<TimEvent, TimLocation> assignment, Context parent) {
    		iTable = Arrays.copyOf(parent.iTable, 45);
    	}

		@Override
		public void assigned(Assignment<TimEvent, TimLocation> assignment, TimLocation location) {
			iTable[location.time()] = location;
		}

		@Override
		public void unassigned(Assignment<TimEvent, TimLocation> assignment, TimLocation location) {
			iTable[location.time()] = null;
		}

		/** Return location that is assigned in this room at given time (if any) */
		public TimLocation getLocation(int time) {
		    return iTable[time];
		}
		
	    /** Student assignment table */
	    public TimLocation[] getTable() {
	        return iTable;
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
    }

	@Override
	public Context createAssignmentContext(Assignment<TimEvent, TimLocation> assignment) {
		return new Context(assignment);
	}

	@Override
	public Context inheritAssignmentContext(Assignment<TimEvent, TimLocation> assignment, Context parentContext) {
		return new Context(assignment, parentContext);
	}
}
