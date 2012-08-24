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
import android.view.Menu;
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
        			
//        			TextView tv2 = (TextView) findViewById(R.id.textView2);
//        			tv2.setText(id);        			
//        			adapter.add(name);
        			map.put(CONTACT_NAME_ITEM, name);
        			map.put(CONTACT_PHONE_ITEM, id);
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
