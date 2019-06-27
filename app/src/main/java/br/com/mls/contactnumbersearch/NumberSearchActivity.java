package br.com.mls.contactnumbersearch;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NumberSearchActivity extends Activity implements UISignalizer {

	protected static final String CONTACTS_CACHE = "Contacts";
	private static final int REQUEST_READ_CONTACTS = 1;

	private ListView listView;

	/**
	 * This is the list with all the contacts and is loaded during initialization or "update contacts" command
	 */
	List<Map<String, Object>> dataList;

	private static ProgressDialog progressDialog;

	private EditText etPhoneNumber;
	
	NumberSearchOperations operations;

	private boolean executingForwardSearch = false;

	public NumberSearchActivity() {
		operations = new NumberSearchOperations(this, new LogUtil());
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == REQUEST_READ_CONTACTS && grantResults.length > 0
				&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			executeContactSearch();
		} else {
			Log.d(this.getClass().getName(), "User has chosen to not grant permission");
			this.finish();
		}
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminateVisibility(true);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.number_search_form);

        final TextWatcher watcher = new NumberSearchTextWatcher();
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etPhoneNumber.setOnKeyListener(new OnKeyListener() {			

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {

				// [#18] The number deletion will be treated on TextWatcher
				if (!(KeyEvent.KEYCODE_DEL == keyCode) && KeyEvent.ACTION_UP == event.getAction()) { // on key released
					String currentText = etPhoneNumber.getText().toString();
					Log.d(this.getClass().getName(), "Typed numbers: " + currentText);
					char currentChar = event.getDisplayLabel();
					boolean numberValid = operations.validateEnteredChars(currentText);
					if (numberValid && !isEmptyValue(currentText) && listView != null && listView.getAdapter() != null) {
						int specificContactListLength = listView.getAdapter().getCount();
						List<Map<String, Object>> updatedDataList = executeForwardSearch(currentText, currentChar, specificContactListLength);
						listView.setAdapter(new SimpleAdapter(NumberSearchActivity.this, updatedDataList,
								R.layout.contact_list, new String[] { NumberSearchOperations.CONTACT_NAME_ITEM, NumberSearchOperations.CONTACT_PHONE_ITEM },
								new int[] { R.id.textView1, R.id.textView2 }));

						executingForwardSearch = false;
						return true;
					}
				// below code snippet was needed because the focus on EditText blocks the MenuItem to be shown.
				} else if (KeyEvent.KEYCODE_MENU == event.getKeyCode()) {
					v.clearFocus();
				} else if (KeyEvent.ACTION_DOWN == event.getAction()) {
					executingForwardSearch = true;
				}
				return false;
			}
		});
        // [#18] workaround for soft keyboard, existing on SamsungY, for example
        etPhoneNumber.addTextChangedListener(watcher);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[] {Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS);
		} else {
			Log.d(this.getClass().getName(), "Permissions granted");
			executeContactSearch();
		}
	}

	private void executeContactSearch() {
		showProgressDialog();

		new AsyncTask<Void, Void, List<Map<String, Object>>>() {

			@Override
			protected List<Map<String, Object>> doInBackground(Void... params) {
				SharedPreferences sharedPreferences = getSharedPreferences(CONTACTS_CACHE, MODE_PRIVATE);
				ContentResolver cr = getContentResolver();
				Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
				try {
					if (cachedContactsDiffers(sharedPreferences, cursor)) {
						return getPhoneContactList(cursor);
					} else {
						return getCachedContactList(sharedPreferences.getAll());
					}
				} finally {
					if (cursor != null) {
						cursor.close();
					}
				}
			}

			@Override
			protected void onPostExecute(List<Map<String, Object>> result) {
				super.onPostExecute(result);
				if (listView == null) {
					listView = findViewById(R.id.listView1);
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

	boolean cachedContactsDiffers(SharedPreferences sharedPreferences, Cursor cursor) {
		if (sharedPreferences.getAll() != null && cursor != null) {
			return cursor.getCount() != sharedPreferences.getAll().size();
		} else {
			Log.d(this.getClass().getName(), String.format("Shared Prefs status: %s / Cursor status: %s", sharedPreferences, cursor));
			if (cursor == null) {
				return false;
			} else if (sharedPreferences.getAll() == null) {
				return true;
			}
		}
		throw new IllegalStateException("Incompatible application state with SharedPrefs and database Cursor");
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
		Cursor phoneCursor = null;
        SharedPreferences sharedPreferences = getSharedPreferences(CONTACTS_CACHE, MODE_PRIVATE);
        Editor prefEditor = sharedPreferences.edit();
		ContentResolver cr = getContentResolver();
        List<Map<String, Object>> contactList = new ArrayList<>();
		if (cursor.getCount() > 0) {
			try {
				while (cursor.moveToNext()) {
					Map<String, Object> map = new HashMap<>();
					String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
					String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
						//Query phone here.  Covered next
						phoneCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);

						map.put(NumberSearchOperations.CONTACT_NAME_ITEM, name);

						if (phoneCursor == null) {
							Log.w(this.getClass().getName(), "Null Phone Cursor for contact: " + name);
							continue;
						}

						StringBuilder phones = new StringBuilder();
						while (phoneCursor.moveToNext()) {
							if (phones.length() > 0) {
								phones.append(NumberSearchOperations.PHONE_NUMBER_SEPARATOR);
							}
							phones.append(phoneCursor
									.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
						}
						map.put(NumberSearchOperations.CONTACT_PHONE_ITEM, phones.toString());
						prefEditor.putString(name, phones.toString());
						contactList.add(map);
					}
				}
				prefEditor.apply();
			} finally {
				if (phoneCursor != null) {
					phoneCursor.close();
				}
			}
        }
        operations.sortContactListByName(contactList);
		return contactList;
	}

	private boolean isEmptyValue(String chars) {
		return chars == null || "".equals(chars);
	}

	/**
	 * The text field status is based on its background color
	 * @param colorStatus some time of {@link android.graphics.Color}
	 */
	void setPhoneTextFieldViewStatus(int colorStatus) {
		this.etPhoneNumber.setBackgroundColor(colorStatus);
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
			Log.d(this.getClass().getName(), "Typed numbers: " + changedText);
			String currentText = changedText.toString();
			if (!isEmptyValue(currentText) && dataList != null && currentText.length() < beforeText.length()) {
				List<Map<String, Object>> updatedDataList = executeBackwardSearch(currentText, dataList.size());
				listView.setAdapter(new SimpleAdapter(NumberSearchActivity.this, updatedDataList,
						R.layout.contact_list, new String[]{NumberSearchOperations.CONTACT_NAME_ITEM, NumberSearchOperations.CONTACT_PHONE_ITEM},
						new int[]{R.id.textView1, R.id.textView2}));
				executingForwardSearch = false;
			// TODO: There must be a refactoring on the operation names. Below operation needs to update
				//	 all dataList reference because it represents a copy/paste event.
				// 	 Given that the operation is similar to what executeBackwardSeach does, for instance it's implemented this way
			} else if (!executingForwardSearch && !isEmptyValue(currentText) && dataList != null) {
				List<Map<String, Object>> updatedDataList = executeBackwardSearch(currentText, dataList.size());
				listView.setAdapter(new SimpleAdapter(NumberSearchActivity.this, updatedDataList,
						R.layout.contact_list, new String[]{NumberSearchOperations.CONTACT_NAME_ITEM, NumberSearchOperations.CONTACT_PHONE_ITEM},
						new int[]{R.id.textView1, R.id.textView2}));
			} else if (isEmptyValue(currentText) && dataList != null) {
				listView.setAdapter(new SimpleAdapter(NumberSearchActivity.this, dataList,
						R.layout.contact_list, new String[]{NumberSearchOperations.CONTACT_NAME_ITEM, NumberSearchOperations.CONTACT_PHONE_ITEM},
						new int[]{R.id.textView1, R.id.textView2}));
				executingForwardSearch = false;
			} else {
				Log.d(this.getClass().getName(), String.format("onTextChanged not performed - currentText: %s / beforeText: %s / dataList: %s",
						currentText, beforeText, dataList));
			}
		}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			beforeText = s.toString();
		}
		
		@Override
		public void afterTextChanged(Editable s) {
			// reset backward search for next iteration
		}
	}

	/**
	 * This search is represented when user is deleting the last char of {@code currentText}
	 * @param currentText
	 * @param size
	 * @return
	 */
	private List<Map<String, Object>> executeBackwardSearch(String currentText, int size) {
		return operations.refreshContactDataList(currentText, '\0', size, true, true);
	}

	/**
	 * This search is represented when user is inputing a new char
	 * @param currentText
	 * @param currentChar
	 * @param size
	 * @return
	 */
	private List<Map<String, Object>> executeForwardSearch(String currentText, char currentChar, int size) {
		return operations.refreshContactDataList(currentText, currentChar, size, true, false);
	}

}
