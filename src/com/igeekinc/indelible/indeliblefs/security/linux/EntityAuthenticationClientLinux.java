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
 
package com.igeekinc.indelible.indeliblefs.security.linux;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import avahi4j.exceptions.Avahi4JException;

import com.igeekinc.indelible.indeliblefs.linux.IndelibleAvahiMasterClient;
import com.igeekinc.indelible.indeliblefs.linux.IndelibleServiceFoundEvent;
import com.igeekinc.indelible.indeliblefs.linux.IndelibleServiceListener;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;

public class EntityAuthenticationClientLinux extends EntityAuthenticationClient implements IndelibleServiceListener
{
	
	public EntityAuthenticationClientLinux() throws KeyStoreException,
			NoSuchAlgorithmException, CertificateException,
			FileNotFoundException, IOException, UnrecoverableKeyException,
			InvalidKeyException, IllegalStateException,
			NoSuchProviderException, SignatureException,
			AuthenticationFailureException, Avahi4JException 
	{

	}
	
	@Override
	protected void initializeBonjour() throws Exception 
	{
		IndelibleAvahiMasterClient.getMasterClient().addIndelibleServiceFoundeListener(this);
	}

	public void indelibleServiceFound(IndelibleServiceFoundEvent foundEvent) 
	{
		String securityServerIDStr = null;
		for (String checkRecord:foundEvent.getTxtRecords())
		{
			if (checkRecord.startsWith(EntityAuthenticationClient.kEntityAuthenticationServerIDKey))
				securityServerIDStr = checkRecord.substring(checkRecord.indexOf("=") + 1);
		}
		System.out.println("Found server "+foundEvent.getName()+"("+foundEvent.getHostname()+":"+foundEvent.getPort()+") security server ID = "+securityServerIDStr);
		serverFound(foundEvent.getHostname(), foundEvent.getPort());
	}

}
