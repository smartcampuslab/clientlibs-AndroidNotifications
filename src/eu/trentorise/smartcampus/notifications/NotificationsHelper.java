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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.Bundle;
import eu.trentorise.smartcampus.ac.AACException;
import eu.trentorise.smartcampus.ac.Constants;
import eu.trentorise.smartcampus.ac.SCAccessProvider;
import eu.trentorise.smartcampus.android.common.GlobalConfig;
import eu.trentorise.smartcampus.communicator.CommunicatorConnector;
import eu.trentorise.smartcampus.communicator.CommunicatorConnectorException;
import eu.trentorise.smartcampus.communicator.model.DBNotification;
import eu.trentorise.smartcampus.communicator.model.Notification;
import eu.trentorise.smartcampus.communicator.model.NotificationFilter;
import eu.trentorise.smartcampus.communicator.model.NotificationsConstants;
import eu.trentorise.smartcampus.communicator.model.NotificationsConstants.ORDERING;
import eu.trentorise.smartcampus.network.JsonUtils;
import eu.trentorise.smartcampus.network.RemoteConnector;
import eu.trentorise.smartcampus.network.RemoteConnector.CLIENT_TYPE;
import eu.trentorise.smartcampus.protocolcarrier.exceptions.ConnectionException;
import eu.trentorise.smartcampus.protocolcarrier.exceptions.ProtocolException;
import eu.trentorise.smartcampus.protocolcarrier.exceptions.SecurityException;
import eu.trentorise.smartcampus.storage.BatchModel;
import eu.trentorise.smartcampus.storage.BatchModel.DeleteModel;
import eu.trentorise.smartcampus.storage.DataException;
import eu.trentorise.smartcampus.storage.StorageConfigurationException;
import eu.trentorise.smartcampus.storage.db.StorageConfiguration;
import eu.trentorise.smartcampus.storage.sync.ISynchronizer;
import eu.trentorise.smartcampus.storage.sync.SyncData;
import eu.trentorise.smartcampus.storage.sync.SyncUpdateModel;

public class NotificationsHelper {

	public static final String PARAM_APP_TOKEN = "app_token";
	public static final String PARAM_SYNC_DB_NAME = "sync_db_name";
	public static final String PARAM_SYNC_SERVICE = "sync_service";
	public static final String PARAM_AUTHORITY = "authority";
	private static final String SYNC_DB_NAME = "notifications";
	private static final String SYNC_SERVICE = "core.communicator";
	
	private static String APP_TOKEN = null;
	private static String AUTHORITY = null;
	private static String APP_ID = null; 
	private static int maxMessages = 0;

	private CommunicatorConnector connector = null;
	
	private static NotificationsHelper instance = null;

	private Context mContext;
	private StorageConfiguration sc = null;
	private NotificationsSyncStorage storage = null;

	private boolean loaded = false;
	private ISynchronizer synchronizer = null;

	public static void init(Context mContext, String appToken, String authority, String appId, int maxMessages) throws Exception {
		if (instance == null) {
			APP_TOKEN = appToken;
			AUTHORITY = authority;
			APP_ID = appId;
			NotificationsHelper.maxMessages = maxMessages;
			instance = new NotificationsHelper(mContext);
			if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.FROYO) {
				RemoteConnector.setClientType(CLIENT_TYPE.CLIENT_WILDCARD);
			}

		}
	}

	public static boolean isInstantiated() {
		return (instance != null);
	}
	
	private static String getAuthToken() throws AACException {
		String token = SCAccessProvider.getInstance(instance.mContext).readToken(instance.mContext);
		return token;
	}

	private static NotificationsHelper getInstance() throws DataException {
		if (instance == null) {
			throw new DataException("NotificationsHelper is not initialized");
		}

		return instance;
	}

	protected NotificationsHelper(Context mContext) throws Exception {
		super();
		this.mContext = mContext;
		// this.mSyncManager = new SyncManager(mContext,
		// CommSyncStorageService.class);

		this.sc = new NotificationsStorageConfiguration();
		this.storage = new NotificationsSyncStorage(mContext, APP_TOKEN, SYNC_DB_NAME, 2, sc, maxMessages);
		String url = GlobalConfig.getAppUrl(mContext);
		if (!url.endsWith("/")) url += "/";
		this.connector = new CommunicatorConnector(url + SYNC_SERVICE, APP_ID); 
		this.synchronizer = new NotificationSynchronizer();
	}

	public static void start(boolean local) throws NameNotFoundException, DataException, StorageConfigurationException, SecurityException, ConnectionException, ProtocolException {
		if (!local) {
			getInstance().loadData();
		}

		if (!getInstance().loaded && AUTHORITY != null) {
			Account a = new Account(Constants.getAccountName(getInstance().mContext),Constants.getAccountType(getInstance().mContext));
			getInstance();
			ContentResolver.setSyncAutomatically(a, AUTHORITY, true);

			getInstance();
			ContentResolver.addPeriodicSync(a, AUTHORITY, new Bundle(), NotificationsConstants.DEF_SYNC_PERIOD * 60);
		}
	}

	private void loadData() throws StorageConfigurationException, DataException, SecurityException, ConnectionException, ProtocolException {
		if (loaded) {
			return;
		}
		getInstance().storage.synchronize(getInstance().synchronizer);
		loaded = true;
	}
	
	public static SyncData synchronize() throws StorageConfigurationException, DataException, SecurityException, ConnectionException, ProtocolException {
		return getInstance().storage.synchronize(getInstance().synchronizer);
	}
	
	
	public static void synchronizeInBG() throws NameNotFoundException, DataException {
		Account a = new Account(Constants.getAccountName(getInstance().mContext),Constants.getAccountType(getInstance().mContext));
		getInstance();
		ContentResolver.requestSync(a, NotificationsHelper.AUTHORITY, new Bundle());
	}

	public static void destroy() throws DataException {
		// getInstance().mSyncManager.disconnect();
	}

	public static List<Notification> getNotifications(NotificationFilter filter, int position, int size, long since) {
		try {
			List<String> params = new ArrayList<String>();
			String query = createQuery(filter, since, params);

			Collection<DBNotification> collection = null;
			if (filter.getOrdering() == null || filter.getOrdering().equals(ORDERING.ORDER_BY_ARRIVAL)) {
				collection = getInstance().storage.query(DBNotification.class, query, params.toArray(new String[params.size()]),
						position, size, "timestamp DESC");
			} else if (filter.getOrdering().equals(ORDERING.ORDER_BY_TITLE)) {
				collection = getInstance().storage.query(DBNotification.class, query, params.toArray(new String[params.size()]),
						position, size, "title ASC");
			} else {
				// TODO: sort!
				collection = getInstance().storage.query(DBNotification.class, query, params.toArray(new String[params.size()]),
						position, size);
			}

			List<Notification> res = new ArrayList<Notification>();
			if (collection != null) {
				for (DBNotification dbn : collection) res.add(dbn.getNotification());
			}
			return res;
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

		if (filter.getSource() != null) {
			query += (query.length() > 0 ? " AND " : "") + "type LIKE '" + filter.getSource() + "'";
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
		getInstance().storage.delete(content.getId(), DBNotification.class);
	}

	public static void toggleRead(Notification content) throws DataException, StorageConfigurationException {
		content.setReaded(!content.isReaded());
		getInstance().storage.update(new DBNotification(content), false);
	}

	public static void markAllAsRead(NotificationFilter filter) throws DataException, StorageConfigurationException {
		filter.setReaded(false);
		List<BatchModel> list = new ArrayList<BatchModel>();
		List<Notification> nList = getNotifications(filter, 0, -1, 0);
		for (Notification n : nList) {
			n.setReaded(true);
			list.add(new SyncUpdateModel.UpdateModel(new DBNotification(n), false, true));
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
				list.add(new DeleteModel(c.getString(0), DBNotification.class));
				c.moveToNext();
			}
		}
		getInstance().storage.batch(list);
	}

	private class NotificationSynchronizer implements ISynchronizer {

		@Override
		public SyncData fetchSyncData(Long version, SyncData in) throws SecurityException, ConnectionException, ProtocolException {
			eu.trentorise.smartcampus.communicator.model.SyncData data = new eu.trentorise.smartcampus.communicator.model.SyncData();
			data.setVersion(version);
			if (in.getDeleted() != null && in.getDeleted().containsKey(DBNotification.class.getName())) {
				data.setDeleted(new HashSet<String>(in.getDeleted().get(DBNotification.class.getName())));
			}
			if (in.getUpdated() != null && in.getUpdated().containsKey(DBNotification.class.getName())) {
				Set<Notification> set = new HashSet<Notification>();
				for (Object dbn : in.getUpdated().get(DBNotification.class.getName())) {
					Notification n = JsonUtils.convert(dbn, DBNotification.class).getNotification();
					set.add(n);
				}
				data.setUpdated(set);
			}
			try {
				data = connector.syncNotificationsByApp(data, getAuthToken());
				SyncData out = new SyncData();
				out.setVersion(data.getVersion());
				if (data.getDeleted() != null && !data.getDeleted().isEmpty()) {
					out.setDeleted(new HashMap<String, List<String>>());
					out.getDeleted().put(DBNotification.class.getName(), new ArrayList<String>(data.getDeleted()));
				}
				if (data.getUpdated() != null && !data.getUpdated().isEmpty()) {
					out.setUpdated(new HashMap<String, List<Object>>());
					List<Object> list = new ArrayList<Object>();
					for (Notification n : data.getUpdated()) {
						list.add(new DBNotification(n));
					}
					out.getUpdated().put(DBNotification.class.getName(), list);
				}
				return out;
			} catch (CommunicatorConnectorException e) {
				throw new ProtocolException(e.getMessage());
			} catch (AACException e) {
				throw new SecurityException(e.getMessage());
			}
		}
		
	}
}
