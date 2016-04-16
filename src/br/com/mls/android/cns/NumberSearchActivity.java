package br.com.mls.android.cns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class NumberSearchActivity extends Activity {
	
	private static final String CONTACT_NAME_ITEM = "contactName";
	
	private static final String CONTACT_PHONE_ITEM = "contactPhone";

	protected static final String CONTACTS_CACHE = "Contacts";
	
	private ListView listView;
	
	List<Map<String, Object>> dataList;

	private static ProgressDialog progressDialog;

	private EditText etPhoneNumber;

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    	setProgressBarIndeterminateVisibility(true);
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.number_search_form);
        
        // TODO: i18n
        progressDialog = ProgressDialog.show(this,
                "Please wait...", "Retrieving contacts ...", true);
        
        etPhoneNumber = (EditText) findViewById(R.id.etPhoneNumber);
        etPhoneNumber.setOnKeyListener(new OnKeyListener() {			
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				return refreshContactList(etPhoneNumber.getText().toString(), event);
			}
		});
        
        new AsyncTask<Void, Void, List<Map<String, Object>>>() {

			@Override
			protected List<Map<String, Object>> doInBackground(Void... params) {
				SharedPreferences sharedPreferences = getSharedPreferences(CONTACTS_CACHE, MODE_PRIVATE);
				if (sharedPreferences.getAll().isEmpty()) {
					return getPhoneContactList();
				} else {
					return getCachedContactList(sharedPreferences.getAll());
				}
			}
			
			@Override
			protected void onPostExecute(List<Map<String, Object>> result) {
				super.onPostExecute(result);
				if (listView == null) {
					listView = (ListView) findViewById(R.id.listView1);
				}
	        	listView.setAdapter(new SimpleAdapter(NumberSearchActivity.this, result, R.layout.contact_list, 
	        						new String[] { CONTACT_NAME_ITEM, CONTACT_PHONE_ITEM }, 
	        						new int[] { R.id.textView1, R.id.textView2 }));
	        	NumberSearchActivity.this.dataList = result;
	        	
	        	progressDialog.dismiss();
			}
		}.execute();
    }

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.number_search, menu);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.update_contacts) {
			updateContactList();
		}
		return super.onOptionsItemSelected(item);
	}

    private void updateContactList() {
    	// TODO: i18n
        progressDialog = ProgressDialog.show(this,
                "Please wait...", "Retrieving contacts ...", true);        
        new AsyncTask<Void, Void, List<Map<String, Object>>>() {
        	@Override
        	protected List<Map<String, Object>> doInBackground(Void... params) {
        		SharedPreferences sharedPreferences = getSharedPreferences(CONTACTS_CACHE, MODE_PRIVATE);
				sharedPreferences.edit().clear();
        		return sharedPreferences.edit().commit() ? getPhoneContactList() : null; 
        	}
        	
        	protected void onPostExecute(java.util.List<java.util.Map<String,Object>> result) {
        		listView.setAdapter(new SimpleAdapter(NumberSearchActivity.this, result, R.layout.contact_list, 
						new String[] { CONTACT_NAME_ITEM, CONTACT_PHONE_ITEM }, 
						new int[] { R.id.textView1, R.id.textView2 }));
        		NumberSearchActivity.this.dataList = result;
	       		progressDialog.dismiss();
	       		if (result == null) {
	       			throw new IllegalStateException("Shared preferences failed to clear previous data. Contact list is null");
	       		}
        	};
        }.execute();
	}

	protected List<Map<String, Object>> getCachedContactList(Map<String, ?> cachedContactsMap) {
    	List<Map<String, Object>> contactList = new ArrayList<Map<String,Object>>();
    	for (String contactName : cachedContactsMap.keySet()) {
    		Map<String, Object> map = new HashMap<String, Object>();
    		map.put(CONTACT_NAME_ITEM, contactName);
    		map.put(CONTACT_PHONE_ITEM, cachedContactsMap.get(contactName));
    		contactList.add(map);
    	}
		return contactList;
	}

	protected List<Map<String, Object>> getPhoneContactList() {
        SharedPreferences sharedPreferences = getSharedPreferences(CONTACTS_CACHE, MODE_PRIVATE);
        Editor prefEditor = sharedPreferences.edit();
		ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        List<Map<String, Object>> contactList = new ArrayList<Map<String,Object>>();
        if (cursor.getCount() > 0) {
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
        			
        			prefEditor.putString(name, phones);
        			contactList.add(map);
        		}
        	}
        	prefEditor.apply();
        }
        // Sort by contact names
        Collections.sort(contactList, new Comparator<Map<String, Object>>() {
			@Override
			public int compare(Map<String, Object> lhs, Map<String, Object> rhs) {
				return lhs.get(CONTACT_NAME_ITEM).toString().compareTo(rhs.get(CONTACT_NAME_ITEM).toString());
			}
		});
        // Save to shared preferences
//        prefEditor.
		return contactList;
	}

	private boolean refreshContactList(String chars, KeyEvent event) {
		// TODO: http://stackoverflow.com/questions/3313347/how-to-update-simpleadapter-in-android
		if (event.getAction() == KeyEvent.ACTION_UP) { // on key released
			List<Map<String, Object>> dataList = new ArrayList<Map<String,Object>>();
			char currentChar = event.getDisplayLabel();
			int keyCode = event.getKeyCode();
			
			// Validation
			try {
				if (chars != null && !"".equals(chars)) {
					Integer.parseInt(chars);
				}
				setPhoneTextFieldViewStatus(Color.WHITE);
			} catch (NumberFormatException nfe) {
				Log.e(this.getClass().getName(), "Invalid number", nfe);
				setPhoneTextFieldViewStatus(Color.RED);
				return false;
			}
			
			Log.d(this.getClass().getName(), "Typed numbers: " + chars);
			if (chars == null || "".equals(chars)) {
				dataList = NumberSearchActivity.this.dataList;
			} else {
				for (int index = 0; index < getSpecificContactListLength(keyCode); index++) {
					Map<String, Object> newDataMap = new HashMap<String, Object>();
					Map<String, Object> dataMap = getSpecificItem(index, keyCode);
					String phones = (String) dataMap.get(CONTACT_PHONE_ITEM);
					if (phones != null && phones.contains((chars != null && !"".equals(chars)) ? chars : getCurrentChar(currentChar, chars, keyCode))) {
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

	/**
	 * The text field status is based on its background color
	 * @param colorStatus some time of {@link android.graphics.Color}
	 */
	private void setPhoneTextFieldViewStatus(int colorStatus) {
		this.etPhoneNumber.setBackgroundColor(colorStatus);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getSpecificItem(int index, int currentTypedKeyCode) {
		if (KeyEvent.KEYCODE_DEL == currentTypedKeyCode) {
			return this.dataList.get(index);
		} else {
			return (Map<String, Object>) listView.getAdapter().getItem(index);
		}
	}

	/**
	 * Forward search uses the ListViewAdapter size
	 * Backward search uses all registered contacts
	 * @param currentTypedKeyCode
	 * @return
	 */
	private int getSpecificContactListLength(int currentTypedKeyCode) {
		return KeyEvent.KEYCODE_DEL == currentTypedKeyCode ? this.dataList.size() : listView.getAdapter().getCount();
	}

	public static class ProgressListener {

        public void dismiss() {
            progressDialog.dismiss();
        }

    }

	private String getCurrentChar(char currentChar, String cachedChars, int currentTypedKeyCode) {
		return isNumber(currentChar, currentTypedKeyCode) ? String.valueOf(currentChar) : cachedChars;
	}
	

    private boolean isNumber(char currentChar, int keyCode) {
		return keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9 ? true : false;
	}
}
