package net.sf.cpsolver.itc.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import org.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.itc.test.ItcTestClient.TestInstance;

/**
 * Solve given ITC 2007 problems with given parameters. Some parameters can 
 * have multiple values, all combinations of such parameters are solved. A CSV
 * file with results is created. For each set of parameters, it contains best and average values
 * of each instance, seeds of the best value for each instance and overall minimum and average.
 * This file is created in the folder where the race was started, named %Race.Name%.csv 
 * <br><br>
 * Additional parameters:
 * <ul>
 *  <li>Race.Name ... name of the race 
 *  <li>Race.Register ... server:port of the register service ({@link ItcTestRegister}) when it is 
 *  to be used to solve problems on various machines in parallel.
 *  <li>Race.Params ... comma separated list of parameters that have multiple values
 *  <li>Race.NrAttempts ... number of runs of each instance with a set of parameters
 *  <li>Race.NrInstances ... number of instances to run
 *  <li>Race.FirstInstance ... index of the first instance to run (default is 1)
 *  <li>Race.InstanceFile ... input file of an instance (with %% to be replaced by instance number)
 *  <li>Race.InstanceFormat ... number format to be placed in the input file (e.g., 00 for 01, 02, 03, ...)
 * </ul>
 * Values of given parameters can be in the following format
 * <ul>
 *  <li>[a,b,c] ... list of values
 *  <li>[a;b;c] ... list of values
 *  <li>[min...max,step] ... values min, min+step, min+2step, ..., max
 *  <li>combination of above, e.g., [a;b...c,d;e...f,g] for a, b, b+d, b+2d, ..., c, e, e+g, e+2g, ... f
 *  <li>short name of the parameter (for CSV file) may be added at the end of the list, after # character (e.g., 
 *  <code>SimulatedAnnealing.TempLengthCoef=[1,2,5,10#TL]</code>)
 * </ul>
 * <br><br>
 * Example race configuration:
 * <code>
 * #Implementation<br>
 * Model.Class=net.sf.cpsolver.itc.ctt.model.CttModel<br>
 * Model.Extension=out<br>
 * <br>
 * #Termination condition<br>
 * Termination.Class=org.cpsolver.ifs.termination.GeneralTerminationCondition<br>
 * Termination.StopWhenComplete=false<br>
 * Termination.TimeOut=288<br>
 * <br>
 * #Neighbour selection criterion<br>
 * Neighbour.Class=net.sf.cpsolver.itc.heuristics.search.ItcSimulatedAnnealing<br>
 * SimulatedAnnealing.InitialTemperature=[0.5;1.0;1.5;2.0;3.0;4.0#IT]<br>
 * SimulatedAnnealing.CoolingRate=[0.90...0.98,0.02#CR]<br>
 * SimulatedAnnealing.TempLengthCoef=[1,2,5,10#TL]<br>
 * SimulatedAnnealing.ReheatLengthCoef=[2,5,10#RL]<br>
 * SimulatedAnnealing.Neighbours=net.sf.cpsolver.itc.ctt.neighbours.CttTimeMove;net.sf.cpsolver.itc.ctt.neighbours.CttRoomMove;net.sf.cpsolver.itc.ctt.neighbours.CttSwapMove;net.sf.cpsolver.itc.ctt.neighbours.CttCourseRoomChangeMove;net.sf.cpsolver.itc.ctt.neighbours.CttCourseMinDaysMove;net.sf.cpsolver.itc.ctt.neighbours.CttCurriculumCompactnessMove@0.1<br>
 * <br>
 * #Race<br>
 * Race.Register=some.server.my:1200<br>
 * Race.Params=SimulatedAnnealing.InitialTemperature,SimulatedAnnealing.CoolingRate,SimulatedAnnealing.TempLengthCoef,SimulatedAnnealing.ReheatLengthCoef<br>
 * Race.Name=ctt-sa<br>
 * Race.NrAttempts=3<br>
 * Race.NrInstances=7<br>
 * Race.InstanceFile=./data/comp%%.ctt<br>
 * Race.InstanceFormat=00<br>
 * </code>
 * <br><br>
 * Example usage:<br>
 * <pre><code>
 * java -cp itc2007.jar -Xmx256m net.sf.cpsolver.itc.test.ItcRace my.properties
 * </code></pre>
 * where my.properties is a file containing above properties.
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
public class ItcRace {
    private static Logger sLog = Logger.getLogger(ItcRace.class);
    private DataProperties iProperties;
    private List<Param> iParams;
    
    /**
     * Constructor 
     * @param properties given properties
     */
    public ItcRace(DataProperties properties) {
        iProperties = properties;
        iParams = new ArrayList<Param>();
        ItcTestClient.init(iProperties.getProperty("Race.Register"));
        int nrComb = 1;
        Set<String> params = new HashSet<String>();
        for (StringTokenizer s=new StringTokenizer(properties.getProperty("Race.Params",""),",");s.hasMoreTokens();) {
            String name = s.nextToken();
            String value = properties.getProperty(name);
            if (value==null) continue;
            params.add(name);
            Param p = new Param(name, value);
            sLog.warn(p);
            iParams.add(p);
            nrComb *= p.values().size();
        }
        for (Map.Entry<?, ?> entry: properties.entrySet()) {
            String name = (String)entry.getKey();
            String value = ((String)entry.getValue()).trim();
            if (params.contains(name)) continue;
            if (value.indexOf("...")>=0 || (value.startsWith("[") && value.endsWith("]"))) {
                Param p = new Param(name, value);
                sLog.warn(p);
                iParams.add(p);
                nrComb *= p.values().size();
            }
        }
        sLog.warn("Number of combinations: "+nrComb);
        int nrAttempts = iProperties.getPropertyInt("Race.NrAttempts",1);
        sLog.warn("Number of attempts: "+nrAttempts);
        int nrInstances = iProperties.getPropertyInt("Race.NrInstances",0);
        sLog.warn("Number of instances: "+nrInstances);
        long timeout = Long.parseLong(System.getProperty("timeout", iProperties.getProperty("Termination.TimeOut")));
        sLog.warn("Timeout: "+timeout+" seconds");
        DecimalFormat df = new DecimalFormat("0.00");
        double hours = (timeout*nrInstances*nrAttempts*nrComb)/3600.0;
        int days = (int)hours/24;
        hours -= 24*days;
        sLog.warn("Time to run: "+df.format(days)+" days "+df.format(hours)+" hours");
    }
    
    /**
     * Next parameter set 
     */
    public boolean inc() {
        for (Param p: iParams) {
            if (!p.inc()) return true;
        }
        return false;
    }
    
    private String paramNames() {
        StringBuffer sb = new StringBuffer();
        for  (Iterator<Param> i = iParams.iterator(); i.hasNext(); ) {
            Param p = i.next();
            sb.append(p.label());
            if (i.hasNext()) sb.append(",");
        }
        return sb.toString();
    }
    
    private String paramValues() {
        StringBuffer sb = new StringBuffer();
        for  (Iterator<Param> i = iParams.iterator(); i.hasNext(); ) {
            Param p = i.next();
            sb.append(p.value());
            if (i.hasNext()) sb.append(",");
        }
        return sb.toString();
    }

    /**
     * Perform the test 
     */
    public void test() {
        boolean header = true;
        do {
            RaceContext cx = new RaceContext(iProperties, "Race");
            List<RaceTestInstance> instances = new ArrayList<RaceTestInstance>();
            for (int a=0;a<cx.getNrAttemts();a++) {
                for (int i=0;i<cx.getNrInstances();i++) {
                    instances.add(new RaceTestInstance(cx,a,i,iProperties));
                }
            }
            ItcTestClient.test(instances);
            for (int a=0;a<cx.getNrAttemts();a++)
                sLog.warn("  Attempt "+(1+a)+": average="+cx.getAvgValue(a)+", nrTests="+cx.getNrTests(a));
            sLog.warn("Test average value: "+cx.getAvgValue()+" ----------------------------------------------");
            for (int i=0;i<cx.getNrInstances();i++) {
                sLog.warn("  Instance "+cx.getInstance(i)+": average="+cx.getInstanceAvgValue(i)+
                        ", nrTests="+cx.getInstanceNrTests(i)+", min="+cx.getInstanceMinValue(i)+", seed="+cx.getInstanceMinSeed(i));
            }
            try {
                File csvFile = new File(iProperties.getProperty("Race.Name","race")+".csv"); 
                PrintWriter w = new PrintWriter(new FileWriter(csvFile,true));
                if (header) {
                    w.print(paramNames());
                    for (int i=0;i<cx.getNrInstances();i++) {
                        w.print(",Avg"+cx.getInstanceNumber(i));
                        w.print(",#Tst"+cx.getInstanceNumber(i));
                        if (cx.getNrAttemts()>1) w.print(",Min"+cx.getInstanceNumber(i));
                        w.print(",Seed"+cx.getInstanceNumber(i));
                    }
                    w.println(",NrTests,AvgValue"+(cx.getNrAttemts()>1?",TotMin":""));
                    header = false;
                }
                w.print(paramValues());
                double min = 0;
                for (int i=0;i<cx.getNrInstances();i++) {
                    w.print(","+cx.getInstanceAvgValue(i));
                    w.print(","+cx.getInstanceNrTests(i));
                    if (cx.getNrAttemts()>1) {
                        w.print(","+cx.getInstanceMinValue(i));
                        min += cx.getInstanceMinValue(i);
                    }
                    w.print(","+cx.getInstanceMinSeed(i));
                }
                w.println(","+cx.getNrTests()+","+cx.getAvgValue()+(cx.getNrAttemts()>1?","+min:""));
                w.flush(); w.close();
            } catch (IOException e) {
                sLog.error(e.getMessage(),e);
            }
        } while(inc());
    }

    /**
     * Setup logging using log4j 
     */
    public static void setupLogging(File logFile, boolean debug) {
        Logger root = Logger.getRootLogger();
        ConsoleAppender console = new ConsoleAppender(new PatternLayout("%m%n"));//%-5p %c{1}> %m%n
        console.setThreshold(Level.INFO);
        root.addAppender(console);
        try {
            FileAppender file = new FileAppender(
                    new PatternLayout("%d{dd-MMM-yy HH:mm:ss.SSS} [%t] %-5p %c{2}> %m%n"), 
                    logFile.getPath(), 
                    false);
            file.setThreshold(Level.DEBUG);
            root.addAppender(file);
        } catch (IOException e) {
            sLog.fatal("Unable to configure logging, reason: "+e.getMessage(), e);
        }
        if (!debug) root.setLevel(Level.WARN);
    }
    
    /**
     * Main method -- parameter is the property file 
     */
    public static void main(String[] args) {
        try {
            DataProperties properties = new DataProperties();
            properties.load(new FileInputStream(args[0]));
            properties.putAll(System.getProperties());
            setupLogging(new File(properties.getProperty("Race.Name","race")+".log"), false);
            new ItcRace(properties).test();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /** A parameter with multiple values */
    private class Param {
        private String iName;
        private String iLabel = null;
        private List<String> iValues = new ArrayList<String>();
        private Iterator<String> iEnum = null;
        private String iValue;
        
        public Param(String name, String value) {
            iName = name;
            if (value.startsWith("[")) value = value.substring(1);
            if (value.endsWith("]")) value = value.substring(0, value.length()-1);
            if (value.indexOf('#')>=0) {
                iLabel = value.substring(value.indexOf('#')+1);
                value = value.substring(0,value.indexOf('#'));
            }
            String sep = (value.indexOf('|')>=0?"|":value.indexOf(';')>=0 || value.indexOf("..")>=0?";":",");
            String dot = (value.indexOf("...")>=0?"...":"..");
            for (StringTokenizer s=new StringTokenizer(value,sep);s.hasMoreTokens();) {
                String token = s.nextToken();
                if (token.indexOf(dot)>=0) {
                    String min = token.substring(0,token.indexOf(dot));
                    token = token.substring(token.indexOf(dot)+dot.length());
                    String max, step;
                    if (token.indexOf(',')>=0) {
                        max = token.substring(0,token.indexOf(','));
                        step = token.substring(token.indexOf(',')+1);
                    } else {
                        max = token;
                        step = "1.0";
                    }
                    int dec = 0;
                    if (step.indexOf('.')>=0)
                        dec = step.length()-step.indexOf('.')-1;
                    String df = "0.";
                    for (int i=0;i<dec;i++) df+="0";
                    DecimalFormat d = new DecimalFormat(df);
                    double eps = Double.parseDouble(step)/10.0;
                    for (double v=Double.parseDouble(min);v<=Double.parseDouble(max)+eps;v+=Double.parseDouble(step))
                        iValues.add(d.format(v));
                } else iValues.add(token);
            }
            iEnum = iValues.iterator();
            iValue = (String)(iEnum.hasNext()?iEnum.next():null);
            iProperties.setProperty(iName, iValue);
        }
        
        public String value() { return iValue; }
        public List<String> values() { return iValues; }
        public String name() { return iName; }
        public String label() { return (iLabel==null?iName:iLabel); }
        public String toString() {
            return name()+"="+value()+"  # "+(iLabel==null?"":iLabel+":")+values().size()+"/"+values(); 
        }
        public boolean inc() {
            boolean ret = false;
            if (!iEnum.hasNext()) {
                iEnum = iValues.iterator(); ret = true;
            }
            iValue = (String)(iEnum.hasNext()?iEnum.next():null);
            iProperties.setProperty(iName, iValue);
            sLog.warn("Setting "+iName+"="+iValue);
            return ret;
        }
    }
    
    /**
     * Race context
     */
    protected static class RaceContext {
        private double iTotalValue = 0;
        private int iNrTests = 0;
        private int iNrAttempts;
        private int iNrInstances;
        private String iInstanceFile;
        private DecimalFormat iInstanceFormat;
        private long iTimeout;
        private double[] iValuePerInstance;
        private int[] iTestsPerInstance;
        private double[] iMinValuePerInstance;
        private String[] iMinSeedPerInstance;
        private double[] iAttemptValue; 
        private int[] iAtTests;
        private int iFirstInstance;
        
        /** Create a race context from the given properties */
        public RaceContext(DataProperties properties, String name) {
            iNrAttempts = properties.getPropertyInt(name+".NrAttempts",1);
            iNrInstances = properties.getPropertyInt(name+".NrInstances",0);
            iFirstInstance = properties.getPropertyInt(name+".FirstInstance",1);
            iInstanceFile = properties.getProperty(name+".InstanceFile");
            iInstanceFormat = new DecimalFormat(properties.getProperty(name+".InstanceFormat","0"));
            iTimeout = Long.parseLong(System.getProperty("timeout", properties.getProperty("Termination.TimeOut")));
            iValuePerInstance = new double[iNrInstances];
            iTestsPerInstance = new int[iNrInstances];
            iMinValuePerInstance = new double[iNrInstances];
            iMinSeedPerInstance = new String[iNrInstances];
            for (int i=0;i<iNrInstances;i++) { iValuePerInstance[i]=0; iTestsPerInstance[i]=0; iMinValuePerInstance[i]=0; iMinSeedPerInstance[i]=null;}
            iAttemptValue = new double[iNrAttempts]; 
            iAtTests = new int[iNrAttempts];
            for (int i=0;i<iNrAttempts;i++) { iAttemptValue[i]=0; iAtTests[i]=0;}
        }
        
        public int getNrAttemts() {
            return iNrAttempts;
        }
        public int getNrInstances() {
            return iNrInstances;
        }
        public String getInstance(int idx) {
            return iInstanceFile.replaceAll("%%", iInstanceFormat.format(iFirstInstance+idx));
        }
        public String getInstanceNumber(int idx) {
            return iInstanceFormat.format(iFirstInstance+idx);
        }
        public long getTimeout() {
            return iTimeout;
        }
        public long generateSeed() {
            return Math.round(Long.MAX_VALUE * Math.random());
        }
        public double getAvgValue(int attempt) {
            return (iAtTests[attempt]==0?0.0:iAttemptValue[attempt]/iAtTests[attempt]);
        }
        public int getNrTests(int attempt) {
            return iAtTests[attempt];
        }
        public int getNrTests() {
            return iNrTests;
        }
        public double getTotalValue() {
            return iTotalValue;
        }
        public double getAvgValue() {
            return (iNrTests==0?0.0:iTotalValue/iNrTests);
        }
        public double getMinValue() {
            double min = 0;
            for (int i=0;i<iNrInstances;i++) {
                min += iMinValuePerInstance[i];
            }
            return min;
        }
        public double getAvgMinValue() {
            double min = 0; int nrTests = 0;
            for (int i=0;i<iNrInstances;i++) {
                if (iMinSeedPerInstance[i]!=null) {
                    min += iMinValuePerInstance[i]; nrTests++;
                }
            }
            return (nrTests==0?0.0:min/nrTests);
        }
        public double getInstanceTotalValue(int instance) {
            return iValuePerInstance[instance];
        }
        public double getInstanceNrTests(int instance) {
            return iTestsPerInstance[instance];
        }
        public double getInstanceMinValue(int instance) {
            return iMinValuePerInstance[instance];
        }
        public String getInstanceMinSeed(int instance) {
            return iMinSeedPerInstance[instance];
        }
        public double getInstanceAvgValue(int instance) {
            return (iTestsPerInstance[instance]==0?0.0:iValuePerInstance[instance]/iTestsPerInstance[instance]);
        }
        public void update(RaceTestInstance instance, double value) {
            synchronized (this) {
                iTotalValue += value; iNrTests ++;
                iAttemptValue[instance.getAttempt()] += value; iAtTests[instance.getAttempt()] ++;
                iValuePerInstance[instance.getInstanceIndex()] += value; iTestsPerInstance[instance.getInstanceIndex()] ++;
                if (iMinSeedPerInstance[instance.getInstanceIndex()]==null || iMinValuePerInstance[instance.getInstanceIndex()]>value) {
                    iMinSeedPerInstance[instance.getInstanceIndex()]=String.valueOf(instance.getSeed());
                    iMinValuePerInstance[instance.getInstanceIndex()]=value;
                }
            }
        }
    }
    
    /** Race test instance*/
    protected static class RaceTestInstance extends TestInstance {
        private int iAttempt;
        private int iInstanceIdx;
        private RaceContext iContext;
        
        public RaceTestInstance(RaceContext context, int attempt, int instance, DataProperties properties) {
            super(context.getInstance(instance), properties, context.generateSeed(), context.getTimeout());
            iAttempt = attempt;
            iContext = context;
            iInstanceIdx = instance;
        }
        
        public int getAttempt() {
            return iAttempt;
        }
        
        public int getInstanceIndex() {
            return iInstanceIdx;
        }

        public RaceContext getContext() {
            return iContext;
        }
        
        public void before(String server, int retry) {
            if (server.indexOf(':')>=0 && server.indexOf('.')>=0)
                server = server.substring(0,server.indexOf('.'))+server.substring(server.indexOf(':'));
            sLog.warn("  Testing instance "+getInstance()+" (attempt="+getAttempt()+", seed="+getSeed()+", timeout="+getTimeout()+", server="+server+(retry>0?", retry="+retry:"")+")");
        }
        public void after() {
            sLog.warn("  Result of instance "+getInstance()+" ("+getSeed()+") is "+getValue());
            if (getValue()!=null)
                getContext().update(this, getValue().doubleValue());
        }
    }
}
