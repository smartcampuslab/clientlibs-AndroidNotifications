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
package eu.trentorise.smartcampus.notifications;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import android.accounts.Account;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.widget.Toast;
import eu.trentorise.smartcampus.ac.SCAccessProvider;
import eu.trentorise.smartcampus.ac.authenticator.AMSCAccessProvider;
import eu.trentorise.smartcampus.android.common.GlobalConfig;
import eu.trentorise.smartcampus.communicator.model.Notification;
import eu.trentorise.smartcampus.communicator.model.NotificationFilter;
import eu.trentorise.smartcampus.communicator.model.Preference;
import eu.trentorise.smartcampus.communicator.model.NotificationsConstants.ORDERING;
import eu.trentorise.smartcampus.protocolcarrier.exceptions.ConnectionException;
import eu.trentorise.smartcampus.protocolcarrier.exceptions.ProtocolException;
import eu.trentorise.smartcampus.protocolcarrier.exceptions.SecurityException;
import eu.trentorise.smartcampus.storage.BatchModel;
import eu.trentorise.smartcampus.storage.BatchModel.DeleteModel;
import eu.trentorise.smartcampus.storage.DataException;
import eu.trentorise.smartcampus.storage.StorageConfigurationException;
import eu.trentorise.smartcampus.storage.db.StorageConfiguration;
import eu.trentorise.smartcampus.storage.sync.SyncStorage;
import eu.trentorise.smartcampus.storage.sync.SyncUpdateModel;

public class NotificationsHelper {

	private static String APP_TOKEN = null;
	private static String SYNC_DB_NAME = null;
	private static String SYNC_SERVICE = null;
	private static String AUTHORITY = null;

	private static NotificationsHelper instance = null;

	private static SCAccessProvider accessProvider = new AMSCAccessProvider();

	private Context mContext;
	private StorageConfiguration sc = null;
	private NotificationsSyncStorage storage = null;

	private boolean loaded = false;

	public static void init(Context mContext, String appToken, String syncDbName, String syncService, String authority) {
		APP_TOKEN = appToken;
		SYNC_DB_NAME = syncDbName;
		SYNC_SERVICE = syncService;
		AUTHORITY = authority;

		if (instance == null) {
			instance = new NotificationsHelper(mContext);
		}
	}

	public static String getAuthToken() {
		String token = accessProvider.readToken(instance.mContext, null);
		return token;
	}

	private static NotificationsHelper getInstance() throws DataException {
		if (instance == null) {
			throw new DataException("NotificationsHelper is not initialized");
		}

		return instance;
	}

	public static SyncStorage getSyncStorage() throws DataException {
		return getInstance().storage;
	}

	protected NotificationsHelper(Context mContext) {
		super();
		this.mContext = mContext;
		// this.mSyncManager = new SyncManager(mContext,
		// CommSyncStorageService.class);

		this.sc = new NotificationsStorageConfiguration();
		this.storage = new NotificationsSyncStorage(mContext, APP_TOKEN, SYNC_DB_NAME, 2, sc);
	}

	public static void start(boolean local) throws RemoteException, DataException, StorageConfigurationException,
			ConnectionException, ProtocolException, SecurityException {
		if (!local) {
			getInstance().loadData();
		}

		if (!getInstance().loaded) {
			ContentResolver.setSyncAutomatically(
					new Account(eu.trentorise.smartcampus.ac.Constants.getAccountName(getInstance().mContext),
							eu.trentorise.smartcampus.ac.Constants.getAccountType(getInstance().mContext)), AUTHORITY, true);

			ContentResolver.addPeriodicSync(
					new Account(eu.trentorise.smartcampus.ac.Constants.getAccountName(getInstance().mContext),
							eu.trentorise.smartcampus.ac.Constants.getAccountType(getInstance().mContext)), AUTHORITY,
					new Bundle(), Preference.DEF_SYNC_PERIOD * 60);
		}
	}

	private void loadData() throws DataException, StorageConfigurationException, ConnectionException, ProtocolException,
			SecurityException, RemoteException {
		if (loaded) {
			return;
		}

		Collection<Preference> coll = storage.getObjects(Preference.class);
		if (coll == null || coll.isEmpty()) {
			storage.synchronize(getAuthToken(), GlobalConfig.getAppUrl(getInstance().mContext), SYNC_SERVICE);
			coll = storage.getObjects(Preference.class);
		}

		loaded = true;
	}

	public static void synchronizeInBG() throws RemoteException, DataException, StorageConfigurationException,
			SecurityException, ConnectionException, ProtocolException {
		ContentResolver.requestSync(new Account(eu.trentorise.smartcampus.ac.Constants.getAccountName(getInstance().mContext),
				eu.trentorise.smartcampus.ac.Constants.getAccountType(getInstance().mContext)), AUTHORITY, new Bundle());
	}

	public static void destroy() throws DataException {
		// getInstance().mSyncManager.disconnect();
	}

	public static void endAppFailure(Activity activity, int id) {
		Toast.makeText(activity, activity.getResources().getString(id), Toast.LENGTH_LONG).show();
		activity.finish();
	}

	public static void showFailure(Activity activity, int id) {
		Toast.makeText(activity, activity.getResources().getString(id), Toast.LENGTH_LONG).show();
	}

	public static List<Notification> getNotifications(NotificationFilter filter, int position, int size, long since) {
		try {
			List<String> params = new ArrayList<String>();
			String query = createQuery(filter, since, params);

			Collection<Notification> collection = null;
			if (filter.getOrdering() == null || filter.getOrdering().equals(ORDERING.ORDER_BY_ARRIVAL)) {
				collection = getInstance().storage.query(Notification.class, query, params.toArray(new String[params.size()]),
						position, size, "timestamp DESC");
			} else if (filter.getOrdering().equals(ORDERING.ORDER_BY_TITLE)) {
				collection = getInstance().storage.query(Notification.class, query, params.toArray(new String[params.size()]),
						position, size, "title ASC");
			} else {
				// TODO: sort!
				collection = getInstance().storage.query(Notification.class, query, params.toArray(new String[params.size()]),
						position, size);
			}

			if (collection.size() > 0)
				return new ArrayList<Notification>(collection);
			return Collections.emptyList();
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	private static String createQuery(NotificationFilter filter, long since, List<String> params) {
		String query = "";
		if (filter.isReaded() != null && filter.isReaded()) {
			query += "readed > 0";
		} else if (filter.isReaded() != null && !filter.isReaded()) {
			query += "readed = 0";
		}

		if (filter.isStarred() != null && filter.isStarred()) {
			query += (query.length() > 0 ? " AND " : "") + "starred > 0";
		} else if (filter.isStarred() != null && !filter.isStarred()) {
			query += (query.length() > 0 ? " AND " : "") + "starred = 0";
		}

		if (filter.getSource() != null && filter.getSource().length() != 0) {
			query += (query.length() > 0 ? " AND " : "") + "type LIKE '%\"" + filter.getSource() + "\"%'";
			params.add(filter.getSearchText());
		}

		if (filter.getChannelId() != null) {
			query += (query.length() > 0 ? " AND " : "") + "channelIds LIKE '%\"" + filter.getChannelId() + "\"%'";
		}

		if (filter.getSearchText() != null && filter.getSearchText().length() != 0) {
			query += (query.length() > 0 ? " AND " : "") + "(notifications MATCH ?)";
			params.add(filter.getSearchText());
		}

		if (since > 0) {
			query += (query.length() > 0 ? " AND " : "") + "(timestamp > " + since + ")";
		}
		return query;
	}

	public static void removeNotification(Notification content) throws DataException, StorageConfigurationException {
		getInstance().storage.delete(content.getId(), Notification.class);
	}

	public static void toggleRead(Notification content) throws DataException, StorageConfigurationException {
		content.setReaded(!content.isReaded());
		getInstance().storage.update(content, false);
	}

	public static void markAllAsRead(NotificationFilter filter) throws DataException, StorageConfigurationException {
		filter.setReaded(false);
		List<BatchModel> list = new ArrayList<BatchModel>();
		List<Notification> nList = getNotifications(filter, 0, -1, 0);
		for (Notification n : nList) {
			n.setReaded(true);
			list.add(new SyncUpdateModel.UpdateModel(n, false, true));
		}
		getInstance().storage.batch(list);
	}

	public static void deleteAll(NotificationFilter filter) throws DataException, StorageConfigurationException {
		List<BatchModel> list = new ArrayList<BatchModel>();

		List<String> params = new ArrayList<String>();
		String where = createQuery(filter, 0, params);

		String query = "SELECT id FROM notifications";
		if (where != null && where.length() > 0) {
			query += " WHERE " + where;
		}

		Cursor c = getInstance().storage.rawQuery(query, params.toArray(new String[params.size()]));
		if (c != null) {
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); i++) {
				list.add(new DeleteModel(c.getString(0), Notification.class));
				c.moveToNext();
			}
		}
		getInstance().storage.batch(list);
	}

}
