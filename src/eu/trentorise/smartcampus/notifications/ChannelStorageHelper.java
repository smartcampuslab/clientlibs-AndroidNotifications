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
import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import eu.trentorise.smartcampus.android.common.Utils;
import eu.trentorise.smartcampus.communicator.model.Action;
import eu.trentorise.smartcampus.communicator.model.Channel;
import eu.trentorise.smartcampus.storage.db.BeanStorageHelper;

public class ChannelStorageHelper implements BeanStorageHelper<Channel> {

	@SuppressWarnings("unchecked")
	@Override
	public Channel toBean(Cursor cursor) {
		Channel f = new Channel();
		f.setId(cursor.getString(cursor.getColumnIndex("id")));
		f.setLabelIds(Utils.convertJSONToObjects(cursor.getString(cursor.getColumnIndex("labelIds")), String.class));
		f.setActions(Utils.convertJSONToObjects(cursor.getString(cursor.getColumnIndex("actions")), Action.class));
		f.setFilterData(Utils.convertJSONToObject(cursor.getString(cursor.getColumnIndex("filterData")), Map.class));
		f.setSourceType(cursor.getString(cursor.getColumnIndex("sourceType")));
		f.setTitle(cursor.getString(cursor.getColumnIndex("title")));
		f.setFeed(cursor.getInt(cursor.getColumnIndex("feed"))>0);
		return f;
	}

	@Override
	public ContentValues toContent(Channel bean) {
		ContentValues values = new ContentValues();

		if (bean.getLabelIds() == null) bean.setLabelIds(new ArrayList<String>());
		if (bean.getActions() == null) bean.setActions(new ArrayList<Action>());
		if (bean.getFilterData() == null) bean.setFilterData(new HashMap<String, Object>());

		values.put("id", bean.getId());
		values.put("labelIds", Utils.convertToJSON(bean.getLabelIds()));
		values.put("actions", Utils.convertToJSON(bean.getActions()));
		values.put("filterData", Utils.convertToJSON(bean.getFilterData()));
		values.put("title", bean.getTitle());
		values.put("sourceType", bean.getSourceType());
		values.put("feed", bean.isFeed());
		return values;
	}

	@Override
	public Map<String,String> getColumnDefinitions() {
		Map<String,String> defs = new HashMap<String, String>();

		defs.put("actions", "TEXT");
		defs.put("labelIds", "TEXT");
		defs.put("filterData", "TEXT");
		defs.put("title", "TEXT");
		defs.put("sourceType", "TEXT");
		defs.put("feed", "INTEGER");
		return defs;
	}

	@Override
	public boolean isSearchable() {
		return false;
	}

	
}
