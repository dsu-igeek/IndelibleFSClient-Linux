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
 
package com.igeekinc.indelible.indeliblefs.utilities;

import java.awt.BorderLayout;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.apache.log4j.Logger;

import avahi4j.Address;
import avahi4j.Avahi4JConstants;
import avahi4j.Avahi4JConstants.BrowserEvent;
import avahi4j.Avahi4JConstants.Protocol;
import avahi4j.Client;
import avahi4j.Client.State;
import avahi4j.IClientCallback;
import avahi4j.IServiceBrowserCallback;
import avahi4j.IServiceResolverCallback;
import avahi4j.ServiceBrowser;
import avahi4j.ServiceResolver;
import avahi4j.ServiceResolver.ServiceResolverEvent;
import avahi4j.exceptions.Avahi4JException;

import com.igeekinc.indelible.indeliblefs.IndelibleFSServer;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;
import com.igeekinc.util.logging.ErrorLogMessage;

public class ListAdvertisedServers implements IClientCallback, IServiceBrowserCallback, IServiceResolverCallback
{
    private JTree listTree;
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Servers");
    DefaultTreeModel treeModel = new DefaultTreeModel(root);
    
	/**
	 * The Avahi4J {@link Client} object
	 */
	private Client client;
	
	/**
	 * A {@link ServiceBrowser} object used to look for "_test._tcp" services
	 */
	private ServiceBrowser browser;
	
	/**
	 * Each matching service is resolved by a {@link ServiceResolver} object,
	 * which is kept open and a reference is stored in this list, so it can be
	 * released upon exit
	 */
	private List<ServiceResolver> resolvers;
		
    public static void main(String [] args)
    {
        try
        {
            ListAdvertisedServers las = new ListAdvertisedServers();
            las.startBonjour();

        } catch (Exception e)
        {
            Logger.getLogger(ListAdvertisedServers.class).error(new ErrorLogMessage("Caught exception"), e);
        }
        while(true)
        {
            try
            {
                Thread.sleep(10000);
            } catch (InterruptedException e)
            {
                Logger.getLogger(ListAdvertisedServers.class).error(new ErrorLogMessage("Caught exception"), e);
            }
        }
    }
    
    void initGUI()
    {
        
    }
    public ListAdvertisedServers() throws Avahi4JException
    {
		resolvers = new ArrayList<ServiceResolver>();
		client = new Client(this);
		client.start();
        JFrame mainFrame = new JFrame();
        listTree = new JTree(treeModel);
        mainFrame.getContentPane().add(listTree, BorderLayout.CENTER);
        mainFrame.pack();
        mainFrame.setVisible(true);
    }
    private void startBonjour() throws Exception
    {
        System.out.println("Searching for servers with name '"+IndelibleFSServer.kIndelibleFSBonjourServiceName+"'");
        //DNSSD.browse(IndelibleFSServer.kIndelibleFSBonjourServiceName, las);
		browser = client.createServiceBrowser(this, Avahi4JConstants.AnyInterface,
				Protocol.ANY , IndelibleFSServer.kIndelibleFSBonjourServiceName, null, 0);
    }

	@Override
	public void serviceCallback(int interfaceNum, Protocol proto,
			BrowserEvent browserEvent, String name, String type,
			String domain, int lookupResultFlag) {
		
		// print event type
		System.out.println(" ****** Service browser event: "+browserEvent);
		
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
	public void resolverCallback(ServiceResolver resolver, int interfaceNum, 
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
				
				String securityServerIDStr = null;
				for (String checkRecord:txtRecords)
				{
					if (checkRecord.startsWith(EntityAuthenticationClient.kEntityAuthenticationServerIDKey))
						securityServerIDStr = checkRecord.substring(checkRecord.indexOf("=") + 1);
				}
		        System.out.println("Found server "+name+"("+hostname+":"+port+") security server ID = "+securityServerIDStr);
		        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(name+"("+hostname+":"+port+") ("+securityServerIDStr+")");
		        try
		        {
		            Registry locateRegistry = LocateRegistry.getRegistry(hostname, port);
		            String [] names = locateRegistry.list();
		            for (String curName:names)
		            {
		                System.out.println("    "+curName);
		                DefaultMutableTreeNode registryNode = new DefaultMutableTreeNode(curName);
		                newNode.add(registryNode);
		            }
		            //SecurityServer foundSecurityServer = (SecurityServer)locateRegistry.lookup(SecurityServer.kIndelibleSecurityServerRMIName);
		        } catch (AccessException e)
		        {
		            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		        } catch (RemoteException e)
		        {
		            Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
		        }
		        synchronized(root)
		        {
		            int numChildren = root.getChildCount();
		            for (int curChildNum = 0; curChildNum < numChildren; curChildNum++)
		            {
		                DefaultMutableTreeNode curNode = (DefaultMutableTreeNode) root.getChildAt(curChildNum);
		                if (curNode.getUserObject().equals(newNode.getUserObject()))
		                {
		                    treeModel.removeNodeFromParent(curNode);
		                    break;  // Assume only one doppleganger
		                }
		            }
		            treeModel.insertNodeInto(newNode, root, root.getChildCount());
		        }
			}
		} else {
			System.out.println("Unable to resolve name");
		}
	}
	
	/**
	 * This callback method is invoked whenever the Avahi4J {@link Client}'s 
	 * state changes. See {@link State} for a list of possible client states.
	 */
	@Override
	public void clientStateChanged(State state) {
		System.out.println("Client state changed to "+state);
	}
}
