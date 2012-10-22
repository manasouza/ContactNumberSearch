package br.com.mls.android.cns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class NumberSearchActivity extends Activity {
	
	private static final String CONTACT_NAME_ITEM = "contactName";
	
	private static final String CONTACT_PHONE_ITEM = "contactPhone";
	
	private ListView listView;
	
	List<Map<String, Object>> dataList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.number_search_form);
        ContentResolver cr = getContentResolver();
        
        final EditText etPhoneNumber = (EditText) findViewById(R.id.etPhoneNumber);
        etPhoneNumber.setOnKeyListener(new OnKeyListener() {			
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO: http://stackoverflow.com/questions/3313347/how-to-update-simpleadapter-in-android
				if (event.getAction() == KeyEvent.ACTION_UP) { // on key released
					List<Map<String, Object>> dataList = new ArrayList<Map<String,Object>>();
					String chars = etPhoneNumber.getText().toString();
					char currentChar = event.getDisplayLabel();
					
					// Validation
					try {
						if (chars != null && !"".equals(chars)) {
							Integer.parseInt(chars);
						}
					} catch (NumberFormatException nfe) {
						Log.e(this.getClass().getName(), "Invalid number", nfe);
						etPhoneNumber.setBackgroundColor(Color.RED);
						return false;
					}
					
					Log.d(this.getClass().getName(), "Typed numbers: " + chars);
					if (chars == null || "".equals(chars)) {
						dataList = NumberSearchActivity.this.dataList;
					} else {
						for (int index = 0; index < listView.getAdapter().getCount(); index++) {
							Map<String, Object> newDataMap = new HashMap<String, Object>();
							Map<String, Object> dataMap = (Map<String, Object>) listView.getAdapter().getItem(index);
							String phones = (String) dataMap.get(CONTACT_PHONE_ITEM);
							if (phones != null && phones.contains((chars != null && !"".equals(chars)) ? chars : String.valueOf(currentChar))) {
								newDataMap.put(CONTACT_NAME_ITEM, dataMap.get(CONTACT_NAME_ITEM));
								newDataMap.put(CONTACT_PHONE_ITEM, phones);
								dataList.add(newDataMap);
							}
						}
					}
					listView.setAdapter(new SimpleAdapter(NumberSearchActivity.this, dataList, R.layout.contact_list, new String[] { CONTACT_NAME_ITEM, CONTACT_PHONE_ITEM }, new int[] { R.id.textView1, R.id.textView2 }));
					return true;
				}
				return false;
			}
		});
        
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cursor.getCount() > 0) {
        	dataList = new ArrayList<Map<String,Object>>();
//			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.contact_list, new int[] {});
    		listView = (ListView) findViewById(R.id.listView1);
        	while (cursor.moveToNext()) {
        		Map<String, Object> map = new HashMap<String, Object>();
        		String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
        		String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        		if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
        			//Query phone here.  Covered next
        			Cursor phoneCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] { id }, null);
        			
        			map.put(CONTACT_NAME_ITEM, name);
        			
        			if (phoneCursor == null) {
        				Log.w(this.getClass().getName(), "Null Phone Cursor for contact: " + name);
        				continue;
        			}
        			
        			String phones = "";
        			while (phoneCursor.moveToNext()) {
        				if (!phones.isEmpty()) {
        					phones += " / ";
        				}
            			map.put(CONTACT_PHONE_ITEM, phones += phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));        				
        			}
        			
        			dataList.add(map);
        		}
        	}
        	listView.setAdapter(new SimpleAdapter(this, dataList, R.layout.contact_list, new String[] { CONTACT_NAME_ITEM, CONTACT_PHONE_ITEM }, new int[] { R.id.textView1, R.id.textView2 }));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.number_search, menu);
        return true;
    }
}
