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

public class IndelibleServiceFoundEvent 
{
	private String name;
	private String hostname;
	private int port;
	private String[] txtRecords;
	
	public IndelibleServiceFoundEvent(String name, String hostname, int port, String[] txtRecords)
	{
		this.name = name;
		this.hostname = hostname;
		this.port = port;
		this.txtRecords = txtRecords;
	}

	public String getName() 
	{
		return name;
	}

	public String getHostname()
	{
		return hostname;
	}

	public int getPort() 
	{
		return port;
	}

	public String[] getTxtRecords() 
	{
		return txtRecords;
	}
	
	
}
