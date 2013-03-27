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
import eu.trentorise.smartcampus.communicator.model.LabelObject;
import eu.trentorise.smartcampus.communicator.model.Preference;
import eu.trentorise.smartcampus.storage.db.BeanStorageHelper;

public class PreferenceStorageHelper implements BeanStorageHelper<Preference> {

	@Override
	public Preference toBean(Cursor cursor) {
		Preference pref = new Preference();
		pref.setId(cursor.getString(cursor.getColumnIndex("id")));
		pref.setLabels(Utils.convertJSONToObjects(cursor.getString(cursor.getColumnIndex("labels")), LabelObject.class));
		pref.setActions(Utils.convertJSONToObjects(cursor.getString(cursor.getColumnIndex("actions")), Action.class));
		pref.setMaxMessageNumber(cursor.getInt(cursor.getColumnIndex("maxMessageNumber")));
		pref.setSynchronizeAutomatically(cursor.getInt(cursor.getColumnIndex("synchronizeAutomatically")) > 0);
		pref.setSyncPeriod(cursor.getInt(cursor.getColumnIndex("syncPeriod")));
		return pref;
	}

	@Override
	public ContentValues toContent(Preference bean) {
		ContentValues values = new ContentValues();
		
		values.put("id", bean.getId());
		if (bean.getLabels() == null) bean.setLabels(new ArrayList<LabelObject>());
		if (bean.getActions() == null) bean.setActions(new ArrayList<Action>());
		values.put("labels", Utils.convertToJSON(bean.getLabels()));
		values.put("actions", Utils.convertToJSON(bean.getActions()));
		values.put("maxMessageNumber", bean.getMaxMessageNumber());
		values.put("synchronizeAutomatically", bean.isSynchronizeAutomatically() ? 1 : 0);
		values.put("syncPeriod", bean.getSyncPeriod());
		return values;
	}

	@Override
	public Map<String,String> getColumnDefinitions() {
		Map<String,String> defs = new HashMap<String, String>();

		defs.put("actions", "TEXT");
		defs.put("labels", "TEXT");
		defs.put("maxMessageNumber", "INTEGER");
		defs.put("synchronizeAutomatically", "INTEGER");
		defs.put("syncPeriod", "INTEGER");
		return defs;
	}

	@Override
	public boolean isSearchable() {
		return false;
	}

	
}
