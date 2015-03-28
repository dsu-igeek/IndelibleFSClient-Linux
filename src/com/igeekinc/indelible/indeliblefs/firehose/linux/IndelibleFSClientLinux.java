package com.igeekinc.indelible.indeliblefs.firehose.linux;

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
 
import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.AccessException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSClient;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSFirehoseClient;
import com.igeekinc.indelible.indeliblefs.linux.IndelibleAvahiMasterClient;
import com.igeekinc.indelible.indeliblefs.linux.IndelibleServiceFoundEvent;
import com.igeekinc.indelible.indeliblefs.linux.IndelibleServiceListener;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
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
        	InetSocketAddress address = new InetSocketAddress(foundEvent.getHostname(), foundEvent.getPort());
        	IndelibleFSFirehoseClient client = new IndelibleFSFirehoseClient(address);
        	Logger.getLogger(IndelibleFSClient.class).error("Found advertised server "+foundEvent.getHostname() + " port = "+foundEvent.getPort());
        	addServer(client);
            Logger.getLogger(IndelibleFSClient.class).info("Found service");
		} catch (AccessException e)
		{
		    Logger.getLogger(IndelibleFSClient.class).error(new ErrorLogMessage("Caught exception"), e);
		} catch (RemoteException e)
		{
		    Logger.getLogger(IndelibleFSClient.class).error(new ErrorLogMessage("Caught exception"), e);
		} catch (IOException e)
		{
		    Logger.getLogger(IndelibleFSClient.class).error(new ErrorLogMessage("Caught exception"), e);
		} catch (AuthenticationFailureException e) 
		{
			Logger.getLogger(IndelibleFSClient.class).error(new ErrorLogMessage("Caught exception"), e);
		}
	}


}
