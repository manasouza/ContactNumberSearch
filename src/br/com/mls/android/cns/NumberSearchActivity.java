package br.com.mls.android.cns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class NumberSearchActivity extends Activity {
	
	private static final String CONTACT_NAME_ITEM = "contactName";
	
	private static final String CONTACT_PHONE_ITEM = "contactPhone";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.number_search_form);
        ContentResolver cr = getContentResolver();
        
        EditText etPhoneNumber = (EditText) findViewById(R.id.etPhoneNumber);
        etPhoneNumber.setOnKeyListener(new OnKeyListener() {			
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				return false;
			}
		});
        
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cursor.getCount() > 0) {
        	List<Map<String, Object>> data = new ArrayList<Map<String,Object>>();
//			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.contact_list, new int[] {});
    		ListView listView = (ListView) findViewById(R.id.listView1);
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
        			
        			data.add(map);
        		}
        	}
        	listView.setAdapter(new SimpleAdapter(this, data, R.layout.contact_list, new String[] { CONTACT_NAME_ITEM, CONTACT_PHONE_ITEM }, new int[] { R.id.textView1, R.id.textView2 }));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.number_search, menu);
        return true;
    }
}