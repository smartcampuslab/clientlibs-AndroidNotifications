/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either   express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package eu.trentorise.smartcampus.communicator.model;

import eu.trentorise.smartcampus.storage.BasicObject;

/**
 * @author raman
 *
 */
public class DBNotification extends BasicObject {
	private static final long serialVersionUID = 4610922361257850982L;

	private Notification notification = null;
	
	public DBNotification(Notification notification) {
		super();
		this.notification = notification;
	}

	public DBNotification() {
		super();
		notification = new Notification();
	}


	public Notification getNotification() {
		return notification;
	}

	public void setNotification(Notification notification) {
		this.notification = notification;
	}

	@Override
	public String getId() {
		return notification.getId();
	}

	@Override
	public void setId(String id) {
		notification.setId(id);
	}

	@Override
	public long getVersion() {
		return notification.getVersion();
	}

	@Override
	public void setVersion(long version) {
		notification.setVersion(version);
	}

	@Override
	public long getUpdateTime() {
		return notification.getUpdateTime();
	}

	@Override
	public void setUpdateTime(long updateTime) {
		notification.setUpdateTime(updateTime);
	}

	
}
