package net.sf.cpsolver.itc.test;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.itc.ItcTest;

/**
 * Solve a given ITC 2007 problem using one of the registered test servers.
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
public class ItcTestClient {
    private static Logger sLog = Logger.getLogger(ItcTestClient.class);
    private static String sRegister = null;
    private static Set<String> sUsedServers = new HashSet<String>();
    
    /** Initialization
     * @param register server:port of the register service (see {@link ItcTestRegister})
     **/
    public static void init(String register) {
        sRegister = register;
        Set<String> servers = getAvailableServers();
        sLog.warn("Available servers: "+servers);
    }
    
    /** List available test servers */
    public static Set<String> getAvailableServers() {
        if (sRegister==null) return null;
        try {
            String host = sRegister.substring(0, sRegister.indexOf(':'));
            int port = Integer.parseInt(sRegister.substring(sRegister.indexOf(':')+1));
            Socket client = new Socket(host, port);
            ItcRemoteIO.writeObject(client, "LIST");
            @SuppressWarnings("unchecked")
			Set<String> servers = (TreeSet<String>)ItcRemoteIO.readObject(client);
            ItcRemoteIO.writeObject(client, null);
            client.close();
            return servers;
        } catch (Exception e) {
            sLog.error(e.getMessage(),e);
            return null;
        }
    }
    
    /** Return an address:port of an available test server (wait untill one is available) */
    protected static String get() {
        synchronized (sUsedServers) {
            while (true) {
                Set<String> av = getAvailableServers();
                if (av==null) {
                    String server = "local";
                    if (!sUsedServers.contains(server)) {
                        sUsedServers.add(server); return server;
                    }
                } else {
                    for (String server: av) {
                        if (!sUsedServers.contains(server)) {
                            sUsedServers.add(server); return server;
                        }
                    }
                }
                try {
                    sUsedServers.wait();
                } catch (InterruptedException e) {}
            }
        }
    }
    
    /** Release a test server (so that some other thread might be able to use it)*/
    protected static void release(String server) {
        synchronized (sUsedServers) {
            sUsedServers.remove(server);
            sUsedServers.notify();
        }
    }
    
    /** Solve given instance */
    public static void test(TestInstance instance) {
        for (int t=0;t<5;t++) {
            try {
                String server = get();
                instance.before(server, t);
                if ("local".equals(server)) {
                    Solution<?,?> solution = ItcTest.test(instance.getInstance(), instance.getProperties(), instance.getSeed(), instance.getTimeout());
                    if (solution!=null)
                        instance.setValue(new Double(solution.getBestValue()+5000*solution.getModel().getBestUnassignedVariables()));
                } else {
                    String host = server.substring(0, server.indexOf(':'));
                    int port = Integer.parseInt(server.substring(server.indexOf(':')+1));
                    Socket client = new Socket(host, port);
                    ItcRemoteIO.writeObject(client, new Object[] {
                        "SOLVE",
                        instance.getInstance(), 
                        instance.getProperties(), 
                        new Long(instance.getSeed()), 
                        new Long(instance.getTimeout())
                    });
                    instance.setValue((Double)ItcRemoteIO.readObject(client));
                    ItcRemoteIO.writeObject(client,null);
                    client.close();
                }
                release(server);
                instance.after();
                return;
            } catch (Exception e) {
                sLog.error(e.getMessage(),e);
            }
        }
    }
    
    /** An instance of ITC 2007 problem to solve */
    public static class TestInstance implements Runnable {
        private String iInstance;
        private DataProperties iProperties;
        private long iSeed;
        private long iTimeout;
        private Double iValue;
        
        /**
         * Constructor
         * @param instance instance name (name of the input file) 
         * @param properties solver parameters
         * @param seed random seed
         * @param timeout time limit (in seconds)
         */
        public TestInstance(String instance, DataProperties properties, long seed, long timeout) {
            iInstance = new String(instance);
            iProperties = new DataProperties(properties);
            iSeed = seed;
            iTimeout = timeout;
        }
        /** Instance name (name of the input file) */
        public String getInstance() {
            return iInstance;
        }
        /** Solver parameters */
        public DataProperties getProperties() {
            return iProperties;
        }
        /** Random seed */
        public long getSeed() {
            return iSeed;
        }
        /** Time limit */
        public long getTimeout() {
            return iTimeout;
        }
        /** Overall value of the solved solution */
        public Double getValue() {
            return iValue;
        }
        /** Set overall value of the solved solution  */
        protected void setValue(Double value) {
            iValue = value;
        }
        /** Called just before an instance is send to a test solver to solve */
        public void before(String server, int retry) {
            if (retry>0) sLog.warn(retry+". retry");
        }
        /** Called just after an instance is send to a test solver to solve */
        public void after() {}
        /** Solve the instance -- calls {@link ItcTestClient#test(TestInstance)} */
        public void run() {
            ItcTestClient.test(this);
        }
    }
    
    /** Solve a collection of test instances {@link TestInstance} */
    public static void test(Collection<? extends TestInstance> instances) {
        List<Thread> threads = new ArrayList<Thread>();
        for (TestInstance instance: instances) {
            Thread t = new Thread(instance);
            t.start();
            threads.add(t);
        }
        for (Thread t: threads) {
            try {
                t.join();
            } catch (InterruptedException x) {}
        }
    }

}
