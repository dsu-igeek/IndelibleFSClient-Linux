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

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.IndelibleFSClient;
import com.igeekinc.util.CheckCorrectDispatchThread;
import com.igeekinc.util.logging.ErrorLogMessage;

public class IndelibleFSClientLinux extends IndelibleFSClient implements IndelibleServiceListener
{	
	public IndelibleFSClientLinux(CheckCorrectDispatchThread threadChecker) throws Exception 
	{
		super(threadChecker);

	}

	@Override
	protected void initializeBonjour() throws Exception 
	{
		IndelibleAvahiMasterClient.getMasterClient().addIndelibleServiceFoundeListener(this);
	}



	public void indelibleServiceFound(IndelibleServiceFoundEvent foundEvent) 
	{
		System.out.println("Found server "+foundEvent.getName()+"("+foundEvent.getHostname()+":"+foundEvent.getPort()+")");
		try
		{
		    Registry locateRegistry = LocateRegistry.getRegistry(foundEvent.getHostname(), foundEvent.getPort());
    		Logger.getLogger(IndelibleFSClient.class).error("Found registry "+foundEvent.getHostname() + " port = "+foundEvent.getPort());
		    addRegistry(foundEvent.getHostname(), foundEvent.getPort(), locateRegistry);
		} catch (AccessException e)
		{
		    Logger.getLogger(IndelibleFSClient.class).error(new ErrorLogMessage("Caught exception"), e);
		} catch (RemoteException e)
		{
		    Logger.getLogger(IndelibleFSClient.class).error(new ErrorLogMessage("Caught exception"), e);
		} catch (NotBoundException e)
		{
		    Logger.getLogger(IndelibleFSClient.class).error(new ErrorLogMessage("Caught exception"), e);
		}
	}


}
