package br.com.mls.contactnumbersearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
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

public class NumberSearchActivity extends Activity implements UISignalizer {

	protected static final String CONTACTS_CACHE = "Contacts";
	
	private ListView listView;
	
	List<Map<String, Object>> dataList;

	private static ProgressDialog progressDialog;

	private EditText etPhoneNumber;
	
	NumberSearchOperations operations;
	
	private boolean backwardSearch;
	
	public NumberSearchActivity() {
		operations = new NumberSearchOperations(this);
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    	setProgressBarIndeterminateVisibility(true);
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.number_search_form);
        
        showProgressDialog();
        
        final TextWatcher watcher = new NumberSearchTextWatcher();
        etPhoneNumber = (EditText) findViewById(R.id.etPhoneNumber);
        etPhoneNumber.setOnKeyListener(new OnKeyListener() {			

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// [#18] The number deletion will be treated on TextWatcher
				if (!(KeyEvent.KEYCODE_DEL == keyCode) && KeyEvent.ACTION_UP == event.getAction()) { // on key released
					String chars = etPhoneNumber.getText().toString();
					char currentChar = event.getDisplayLabel();
					
					boolean numberValid = operations.validateEnteredChars(chars, currentChar, keyCode);	
					int specificContactListLength = getSpecificContactListLength(keyCode);
					backwardSearch = KeyEvent.KEYCODE_DEL == keyCode;
					
					// TODO: Eliminates high dependency with event keycode:
					// 		create a boolean value that represents the backward search (when backspace) or forward search the way that it`s ruled by event.keyCode
					return refreshContactList(chars, currentChar, specificContactListLength, numberValid, backwardSearch);
				// below code snippet was needed because the focus on EditText blocks the MenuItem to be shown.
				} else if (KeyEvent.KEYCODE_MENU == event.getKeyCode()) {
					v.clearFocus();
				}
				return false;
			}
		});
        // [#18] workaround for soft keyboard, existing on SamsungY, for example
        etPhoneNumber.addTextChangedListener(watcher);
        
        new AsyncTask<Void, Void, List<Map<String, Object>>>() {

			@Override
			protected List<Map<String, Object>> doInBackground(Void... params) {
				SharedPreferences sharedPreferences = getSharedPreferences(CONTACTS_CACHE, MODE_PRIVATE);
				ContentResolver cr = getContentResolver();
				Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
				if (cachedContactsDiffers(sharedPreferences, cursor)) {
					return getPhoneContactList(cursor);
				} else {
					return getCachedContactList(sharedPreferences.getAll());
				}
			}

			private boolean cachedContactsDiffers(SharedPreferences sharedPreferences, Cursor cursor) {
				return cursor.getCount() != sharedPreferences.getAll().size();
			}

			@Override
			protected void onPostExecute(List<Map<String, Object>> result) {
				super.onPostExecute(result);
				if (listView == null) {
					listView = (ListView) findViewById(R.id.listView1);
				}
	        	listView.setAdapter(new SimpleAdapter(NumberSearchActivity.this, result, R.layout.contact_list, 
	        						new String[] { NumberSearchOperations.CONTACT_NAME_ITEM, NumberSearchOperations.CONTACT_PHONE_ITEM }, 
	        						new int[] { R.id.textView1, R.id.textView2 }));
	        	NumberSearchActivity.this.dataList = result;
	        	
	        	progressDialog.dismiss();
			}
		}.execute();
    }

	private void showProgressDialog() {
		progressDialog = ProgressDialog.show(this,
                getString(R.string.loading_title), getString(R.string.loading_desc_contacts), true);
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
    	showProgressDialog();        
        new AsyncTask<Void, Void, List<Map<String, Object>>>() {
        	@Override
        	protected List<Map<String, Object>> doInBackground(Void... params) {
				ContentResolver cr = getContentResolver();
				Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        		SharedPreferences sharedPreferences = getSharedPreferences(CONTACTS_CACHE, MODE_PRIVATE);
				sharedPreferences.edit().clear();
        		return sharedPreferences.edit().commit() ? getPhoneContactList(cursor) : null;
        	}
        	
        	protected void onPostExecute(java.util.List<java.util.Map<String,Object>> result) {
        		listView.setAdapter(new SimpleAdapter(NumberSearchActivity.this, result, R.layout.contact_list, 
						new String[] { NumberSearchOperations.CONTACT_NAME_ITEM, NumberSearchOperations.CONTACT_PHONE_ITEM }, 
						new int[] { R.id.textView1, R.id.textView2 }));
        		NumberSearchActivity.this.dataList = result;
	       		progressDialog.dismiss();
	       		if (result == null) {
	       			throw new IllegalStateException("Shared preferences failed to clear previous data. Contact list is null");
	       		}
        	}
        }.execute();
	}

	protected List<Map<String, Object>> getCachedContactList(Map<String, ?> cachedContactsMap) {
    	List<Map<String, Object>> contactList = new ArrayList<>();
    	for (String contactName : cachedContactsMap.keySet()) {
    		Map<String, Object> map = new HashMap<>();
    		map.put(NumberSearchOperations.CONTACT_NAME_ITEM, contactName);
    		map.put(NumberSearchOperations.CONTACT_PHONE_ITEM, cachedContactsMap.get(contactName));
    		contactList.add(map);
    	}
    	operations.sortContactListByName(contactList);
		return contactList;
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	protected List<Map<String, Object>> getPhoneContactList(Cursor cursor) {
        SharedPreferences sharedPreferences = getSharedPreferences(CONTACTS_CACHE, MODE_PRIVATE);
        Editor prefEditor = sharedPreferences.edit();
		ContentResolver cr = getContentResolver();
        List<Map<String, Object>> contactList = new ArrayList<>();
		if (cursor.getCount() > 0) {
        	while (cursor.moveToNext()) {
        		Map<String, Object> map = new HashMap<>();
        		String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
        		String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        		// TODO: [#24] verify why merged contacts are passing through this validation
        		if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
        			//Query phone here.  Covered next
        			Cursor phoneCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] { id }, null);
        			
        			map.put(NumberSearchOperations.CONTACT_NAME_ITEM, name);
        			
        			if (phoneCursor == null) {
        				Log.w(this.getClass().getName(), "Null Phone Cursor for contact: " + name);
        				continue;
        			}
        			
        			String phones = "";
        			while (phoneCursor.moveToNext()) {
        				if (!phones.isEmpty()) {
        					phones += NumberSearchOperations.PHONE_NUMBER_SEPARATOR;
        				}
            			map.put(NumberSearchOperations.CONTACT_PHONE_ITEM, phones += phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));        				
        			}
        			
        			prefEditor.putString(name, phones);
        			contactList.add(map);
        		}
        	}
        	prefEditor.apply();
        }
        operations.sortContactListByName(contactList);
		return contactList;
	}

	private boolean refreshContactList(String chars, char currentChar, int specificContactListLength, boolean isNumber, boolean backwardSearch) {
		// TODO: http://stackoverflow.com/questions/3313347/how-to-update-simpleadapter-in-android
		List<Map<String, Object>> dataList;
		Log.d(this.getClass().getName(), "Typed numbers: " + chars);
		if (chars == null || "".equals(chars)) {
			dataList = this.dataList;
		} else {
			dataList = operations.refreshContactDataList(chars, currentChar, specificContactListLength, isNumber, backwardSearch);
		}
		listView.setAdapter(new SimpleAdapter(NumberSearchActivity.this, dataList, R.layout.contact_list, new String[] { NumberSearchOperations.CONTACT_NAME_ITEM, NumberSearchOperations.CONTACT_PHONE_ITEM }, new int[] { R.id.textView1, R.id.textView2 }));
		return true;
	}

	/**
	 * The text field status is based on its background color
	 * @param colorStatus some time of {@link android.graphics.Color}
	 */
	void setPhoneTextFieldViewStatus(int colorStatus) {
		this.etPhoneNumber.setBackgroundColor(colorStatus);
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

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> getSpecificItem(int index, boolean backwardSearch) {
		if (backwardSearch) {
			return this.dataList.get(index);
		} else {
			return (Map<String, Object>) listView.getAdapter().getItem(index);
		}
	}

	@Override
	public void numberValid(boolean isValid) {
		if (isValid) {
			setPhoneTextFieldViewStatus(Color.WHITE);
		} else {
			setPhoneTextFieldViewStatus(Color.RED);
		}
	}
	
	public class NumberSearchTextWatcher implements TextWatcher {
		
		private String beforeText;

		@Override
		public void onTextChanged(CharSequence changedText, int start, int before, int count) {
			String currentText = changedText.toString();
			if (currentText.length() < beforeText.length()) {
				backwardSearch = true;
				refreshContactList(currentText, '\0', dataList.size(), true, backwardSearch);
			}
		}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			beforeText = s.toString();
		}
		
		@Override
		public void afterTextChanged(Editable s) {
			// reset backward search for next iteration
			backwardSearch = false;
		}
	}
}
