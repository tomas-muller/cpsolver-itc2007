package net.sf.cpsolver.itc.tim.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.itc.ItcModel;

/**
 * International Timetabling Competition 2002 Model. The {@link TimModel} is an extension
 * of this model. For more details about this problem see 
 * <a href='http://www.idsia.ch/Files/ttcomp2002/'>http://www.idsia.ch/Files/ttcomp2002/</a>.
 * <br><br>
 * Problems of this model can be solved using ttcomp02 property file, i.e.,
 * <ul> 
 * java -Xmx256m -jar itc2007.jar ttcomp02 data\ttcomp02\competition01.tim competition01.sln
 * </ul>
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
public class TTComp02Model extends ItcModel {
    /** Room constraints */
    protected Vector iRooms = null;
    /** Student constraints */
    protected Vector iStudents = null;
    /** Internal counter of objectives */
    private int iScore[] = new int[] {0,0,0};
    
    /** Constructor */
    public TTComp02Model() {
        super();
        iAssignedVariables = null;
        iUnassignedVariables = null;
        iPerturbVariables = null;
    }
    
    /**
     * Prohibited times are not allowed (there are no availabilities in this model) 
     */
    public boolean isAllowProhibitedTime() {
        return false;
    }
    
    /**
     * Assignments without rooms are not allowed 
     */
    public boolean isAllowNoRoom() {
        return false;
    }

    /**
     * Load problem file (see <a href='http://www.idsia.ch/Files/ttcomp2002/IC_Problem/node7.html'>Input Format</a> for more details).
     */
    public boolean load(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        
        StringTokenizer stk = new StringTokenizer(in.readLine(), " ");
        int nrEvents = Integer.parseInt(stk.nextToken());
        int nrRooms = Integer.parseInt(stk.nextToken());
        int nrFeatures = Integer.parseInt(stk.nextToken());
        int nrStudents = Integer.parseInt(stk.nextToken());
        
        iRooms = new Vector();
        for (int i=0;i<nrRooms;i++)
            iRooms.add(new TimRoom(i, Integer.parseInt(in.readLine())));
        
        for (int i=0;i<nrEvents;i++)
            addVariable(new TimEvent(i));
        
        iStudents = new Vector();
        for (int i=0;i<nrStudents;i++)
            iStudents.add(new TimStudent(i));
        
        for (Enumeration e=iStudents.elements();e.hasMoreElements();) {
            TimStudent student = (TimStudent)e.nextElement();
            for (Enumeration f=variables().elements();f.hasMoreElements();) {
                TimEvent event = (TimEvent)f.nextElement();
                if (1==Integer.parseInt(in.readLine())) {
                    event.students().add(student);
                    student.addVariable(event);
                }
            }
        }
        
        boolean roomFeature[][] = new boolean[nrRooms][nrFeatures];
        for (int i=0;i<nrRooms;i++)
            for (int j=0;j<nrFeatures;j++)
                roomFeature[i][j] = (1==Integer.parseInt(in.readLine()));
        
        boolean eventFeature[][] = new boolean[nrEvents][nrFeatures];
        for (int i=0;i<nrEvents;i++)
            for (int j=0;j<nrFeatures;j++)
                eventFeature[i][j] = (1==Integer.parseInt(in.readLine()));
        
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            TimRoom room = (TimRoom)e.nextElement();
            for (Enumeration f=variables().elements();f.hasMoreElements();) {
                TimEvent event = (TimEvent)f.nextElement();
                if (event.students().size()>room.size()) continue;
                boolean hasAllFeatures = true;
                for (int j=0;j<nrFeatures;j++) {
                    if (eventFeature[(int)event.getId()][j] && !roomFeature[(int)room.getId()][j]) {
                        hasAllFeatures = false; break;
                    }
                }
                if (!hasAllFeatures) continue;
                room.addVariable(event);
                event.rooms().add(room);
            }
        }
                
        for (Enumeration e=iStudents.elements();e.hasMoreElements();) {
            TimStudent student = (TimStudent)e.nextElement();
            addConstraint(student);
        }

        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            TimRoom room = (TimRoom)e.nextElement();
            addConstraint(room);
        }
        
        for (Enumeration f=variables().elements();f.hasMoreElements();) {
            TimEvent event = (TimEvent)f.nextElement();
            event.init(isAllowProhibitedTime(),isAllowNoRoom());
        }
        
        in.close();
        
        return true;
    }
    
    /** List of room constraints */
    public Vector rooms() {
        return iRooms;
    }
    
    /** List of student constraints */
    public Vector students() {
        return iStudents;
    }

    /** Save solution into the given file. Same format is used for TTComp02 (see 
     * <a href='http://www.idsia.ch/Files/ttcomp2002/IC_Problem/Output_format.htm'>Output Format</a>)
     * as well as track 2 of ITC2007 (see <a href='http://www.cs.qub.ac.uk/itc2007/postenrolcourse/course_post_index_files/outputformat.htm'>
     * Output Format</a>).
     */ 
    public boolean save(File file) throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(file));
        
        for (Enumeration f=variables().elements();f.hasMoreElements();) {
            TimEvent event = (TimEvent)f.nextElement();
            TimLocation location = (TimLocation)event.getAssignment();
            out.println(location==null?"-1 -1":location.time()+" "+(location.room()==null?-1:location.room().getId()));
        }
        
        out.flush(); out.close();
        
        return true;
    }
    
    /**
     * Return solution value
     * @param oneDay include single event on a day criteria
     * @param lastTime include last slot of the day criteria
     * @param threeMore include more than two events consecutively criteria
     */
    public int score(boolean oneDay, boolean lastTime, boolean threeMore) {
        int score = 0;
        for (Enumeration e=iStudents.elements();e.hasMoreElements();) {
            TimStudent student = (TimStudent)e.nextElement();
            score += student.score(oneDay, lastTime, threeMore);
        }
        return score;
    }
    
    /**
     * Solution value
     */
    public double getTotalValue() {
        return iScore[0]+iScore[1]+iScore[2];
    }
    
    /** Value of single event on a day criterion */
    public int oneTimePenalty() {
        return iScore[0];
    }

    /** Value of last slot of the day criterion */
    public int lastTimePenalty() {
        return iScore[1];
    }

    /** Value of more than two events consecutively criterion */
    public int treeOrMoreTimesPenalty() {
        return iScore[2];
    }

    /** Solution info -- add values of solutions criteria */
    public Hashtable getInfo() {
        Hashtable info = super.getInfo();
        info.put("One time", String.valueOf(iScore[0]));
        info.put("Last time", String.valueOf(iScore[1]));
        info.put("Three or more times", String.valueOf(iScore[2]));
        return info;
    }
    
    /** Solution info -- add values of solutions criteria (precise computation) */
    public Hashtable getExtendedInfo() {
        Hashtable info = super.getExtendedInfo();
        info.put("One time [p]", String.valueOf(score(true,false,false)));
        info.put("Last time [p]", String.valueOf(score(false,true,false)));
        info.put("Three or more times [p]", String.valueOf(score(false,false,true)));
        return info;
    }
    
    /** CSV header: 
     * 1d for one event a day,
     * lt for use of last time of a day,
     * 3+ for more three or more events consecutively
     */
    public String csvHeader() { 
        return "1d,lt,3+";
    }
    
    /** CSV solution line:
     * one event a day,
     * use of last time of a day,
     * more three or more events consecutively
     */
    public String csvLine() { 
        return 
            iScore[0]+","+
            iScore[1]+","+
            iScore[2];
    }
    
    
    /**
     * Update counters on unassignment of an event
     */
    public void afterUnassigned(long iteration, Value value) {
        super.afterUnassigned(iteration, value);
        TimLocation location = (TimLocation)value;
        int[] score = location.score();
        iScore[0] -= score[0];
        iScore[1] -= score[1];
        iScore[2] -= score[2];
    }
    
    /**
     * Update counters on assignment of an event
     */
    public void beforeAssigned(long iteration, Value value) {
        super.beforeAssigned(iteration, value);
        TimLocation location = (TimLocation)value;
        TimEvent event = (TimEvent)location.variable();
        int[] score = location.score();
        iScore[0] += score[0];
        iScore[1] += score[1];
        iScore[2] += score[2];
    }
    
    /** Load solution from given file */
    public boolean loadSolution(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        
        for (Enumeration f=variables().elements();f.hasMoreElements();) {
            TimEvent event = (TimEvent)f.nextElement();
            StringTokenizer s = new StringTokenizer(in.readLine()," ");
            int time = Integer.parseInt(s.nextToken());
            int roomId = Integer.parseInt(s.nextToken());
            if (time<0) continue;
            TimRoom room = null;
            for (Enumeration g=rooms().elements();room==null && g.hasMoreElements();) {
                TimRoom r = (TimRoom) g.nextElement();
                if (r.getId()==roomId) room = r;
            }
            event.assign(0, new TimLocation(event, time, room));
        }
        
        in.close();
        
        return true;
    }
}
