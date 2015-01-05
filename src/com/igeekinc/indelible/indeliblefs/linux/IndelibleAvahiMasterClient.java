/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * @Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.@
 */
 
package com.igeekinc.indelible.indeliblefs.linux;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.event.EventListenerList;

import org.apache.log4j.Logger;

import avahi4j.Address;
import avahi4j.Avahi4JConstants;
import avahi4j.Avahi4JConstants.BrowserEvent;
import avahi4j.Avahi4JConstants.Protocol;
import avahi4j.Client;
import avahi4j.Client.State;
import avahi4j.EntryGroup;
import avahi4j.IClientCallback;
import avahi4j.IEntryGroupCallback;
import avahi4j.IServiceBrowserCallback;
import avahi4j.IServiceResolverCallback;
import avahi4j.ServiceBrowser;
import avahi4j.ServiceResolver;
import avahi4j.ServiceResolver.ServiceResolverEvent;
import avahi4j.exceptions.Avahi4JException;

import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSClient;
import com.igeekinc.indelible.indeliblefs.server.IndelibleFSServerRemote;

public class IndelibleAvahiMasterClient implements IClientCallback, IServiceBrowserCallback, IServiceResolverCallback, IEntryGroupCallback
{
	private static IndelibleAvahiMasterClient singleton = new IndelibleAvahiMasterClient();
	private avahi4j.Client client;
	private EventListenerList listenerList;
	private ServiceBrowser browser;
	private Logger logger = Logger.getLogger(getClass());
	boolean noMoreEvents = false;
	private ArrayList<Process>registerProcesses = new ArrayList<Process>();
	/**
	 * The service's {@link EntryGroup}, which contains the service's details.
	 */
	private EntryGroup group;
	/**
	 * A list of TXT records for this service.
	 */
	private Vector<String> records = new Vector<String>();
	private ArrayList<IndelibleServiceFoundEvent>foundEvents = new ArrayList<IndelibleServiceFoundEvent>();	// Keep them for playback for late arrivals
	/**
	 * Each matching service is resolved by a {@link ServiceResolver} object,
	 * which is kept open and a reference is stored in this list, so it can be
	 * released upon exit
	 */
	private List<ServiceResolver> resolvers = new ArrayList<ServiceResolver>();
	
	public static IndelibleAvahiMasterClient getMasterClient()
	{
		return singleton;
	}
	
	public static final int kTimeToWaitForAvahi = 10000;
	private IndelibleAvahiMasterClient()
	{
		try
		{
			client = new Client(this);
			client.start();
			listenerList = new EventListenerList();
			browser = client.createServiceBrowser(this, Avahi4JConstants.AnyInterface, Protocol.ANY , IndelibleFSServerRemote.kIndelibleFSBonjourServiceName, null, 0);
			long started = System.currentTimeMillis();
			synchronized(this)
			{
				while (!noMoreEvents && System.currentTimeMillis() - started < kTimeToWaitForAvahi)
					wait(100);
			}
		} catch (Avahi4JException e)
		{
			Logger.getLogger(getClass()).error("Failed to create AVAHI client", e);
			throw new InternalError("Failed to create AVAHI client");
		} catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addIndelibleServiceFoundeListener(IndelibleServiceListener newListener)
    {
        listenerList.add(IndelibleServiceListener.class, newListener);
        for (IndelibleServiceFoundEvent foundEvent:foundEvents)
        	newListener.indelibleServiceFound(foundEvent);
    }
    
    public void removeIndelibleServiceFoundListener(IndelibleServiceListener removeListener)
    {
        listenerList.remove(IndelibleServiceListener.class, removeListener);
    }
    
    public void fireChangeEvent(Object source, IndelibleServiceFoundEvent foundEvent)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) 
        {
            if (listeners[i]==IndelibleServiceListener.class) 
            {
                ((IndelibleServiceListener)listeners[i+1]).indelibleServiceFound(foundEvent);
            }
        }
        foundEvents.add(foundEvent);
    }
    
	@Override
	public synchronized void clientStateChanged(State newState) 
	{
		System.out.println("Client's new state: " + newState);
		
	}

	@Override
	public synchronized void groupStateChanged(avahi4j.EntryGroup.State newState) 
	{
		System.out.println("Group's new state: " + newState);
	}
	
	@Override
	public synchronized void serviceCallback(int interfaceNum, Protocol proto,
			BrowserEvent browserEvent, String name, String type,
			String domain, int lookupResultFlag) {
		
		// print event type
		System.out.println(" ****** Service browser event: "+browserEvent);
		
		if (browserEvent == BrowserEvent.NO_MORE)
		{
			noMoreEvents = true;
			this.notifyAll();
		}
		if(browserEvent==BrowserEvent.NEW || browserEvent==BrowserEvent.REMOVE){
			
			// print service details
			System.out.println("Interface: "+interfaceNum + "\nProtocol :"
					+ proto +"\nEvent: " + browserEvent + "\nName: "+name+ "\nType:"
					+ type+ "\nDomain: "+domain+ "\nFlags: " 
					+ Avahi4JConstants.lookupResultToString(lookupResultFlag)
					+ "\n");
			
			// only if it's a new service, resolve it
			if(browserEvent==BrowserEvent.NEW){
				try {
					// ServiceResolvers are kept open and a reference is stored
					// in a list so they can be freed upon exit
					resolvers.add(client.createServiceResolver(this, 
							interfaceNum, proto, name, type, domain, 
							Protocol.ANY, 0));
				} catch (Avahi4JException e) {
					System.out.println("error creating resolver");
					e.printStackTrace();
				}
			}			
		}
	}

	/**
	 * This callback method is invoked when a service is resolved. It prints the
	 * service's hostname and TXT records
	 */
	@Override
	public synchronized void resolverCallback(ServiceResolver resolver, int interfaceNum, 
			Protocol proto,	ServiceResolverEvent resolverEvent, String name, 
			String type, String domain, String hostname, Address address, 
			int port, String txtRecords[], int lookupResultFlag) {

		// print resolved name details
		if(resolverEvent==ServiceResolverEvent.RESOLVER_FOUND) {
			
			if(name==null && type==null && hostname==null) {
				// if null, the service has disappeared, release the resolver
				// and remove it from the list
				resolver.release();
				resolvers.remove(resolver);
			} else {
				System.out.println(" ******  Service RESOLVED:\nInterface: "
						+ interfaceNum + "\nProtocol :"	+ proto + "\nName: " + name 
						+ "\nType: " + type+ "\nHostname: "+ hostname +"\nDomain: "
						+ domain+ "\nAddress: " + address + "\nFlags: " 
						+ Avahi4JConstants.lookupResultToString(lookupResultFlag)
						+ "\nTXT records:");
				IndelibleServiceFoundEvent foundEvent = new IndelibleServiceFoundEvent(name, hostname, port, txtRecords);
				fireChangeEvent(this, foundEvent);
		        Logger.getLogger(IndelibleFSClient.class).info("Found service");
			}
		} 
		else 
		{
			System.out.println("Unable to resolve name");
		}
	}
	
	public synchronized void advertiseRegistry(int registryPort) 
	throws Exception 
	{
		String [] cmds = new String[4];
		cmds[0] = "/usr/bin/avahi-publish-service";
		cmds[1] = "IndelibleFS";
		cmds[2] = IndelibleFSServerRemote.kIndelibleFSBonjourServiceName;
		cmds[3] = Integer.toString(registryPort);
		Process registerProcess = Runtime.getRuntime().exec(cmds);
		registerProcesses.add(registerProcess);
		/*
		// create group
		if (group == null)
			group = client.createEntryGroup(this);
		records.add(IndelibleServer.kSecurityServerIDKey+"="+securityServerID.toString());
		int result = group.addService(Avahi4JConstants.AnyInterface, Protocol.ANY,
				"IndelibleFS", IndelibleFSServer.kIndelibleFSBonjourServiceName, null, null, registryPort, records);
		if (result == Avahi4JConstants.AVAHI_OK) 
		{
			result = group.commit();
			if (result != Avahi4JConstants.AVAHI_OK)
				logger.error("Got "+result+" trying to commit group");
		}
		else
		{
			logger.error("Got "+result+" trying to add service to group");
		}
		*/
	}
	
	public synchronized void advertiseEntityAuthenticationRegistry(int registryPort) 
	throws Exception 
	{
		String [] cmds = new String[4];
		cmds[0] = "/usr/bin/avahi-publish-service";
		cmds[1] = "IndelibleFS";
		cmds[2] = IndelibleFSServerRemote.kIndelibleAuthBonjourServiceName;
		cmds[3] = Integer.toString(registryPort);
		Process registerProcess = Runtime.getRuntime().exec(cmds);
		registerProcesses.add(registerProcess);
	}
}
