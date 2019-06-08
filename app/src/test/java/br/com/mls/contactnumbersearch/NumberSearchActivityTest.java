package br.com.mls.contactnumbersearch;

import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
//@PrepareForTest({Log.class})
@PrepareForTest({NumberSearchActivityTest.class , Log.class})
public class NumberSearchActivityTest {

    private NumberSearchActivity activity;
    private SharedPreferencesMock sharedPrefs;
    private CursorMock cursor;

    @Before
    public void setUp() {
        activity = new NumberSearchActivity();
        cursor = new CursorMock();
        sharedPrefs = new SharedPreferencesMock();
        PowerMockito.mockStatic(Log.class);
    }

    @Test
    public void cachedContactsDiffersWhenRegistryDiffsOnCache() {
        // GIVEN
        cursor.setCount(133);
        sharedPrefs.setAll(createContactRegistries(50));

        // WHEN
        boolean differs = activity.cachedContactsDiffers(sharedPrefs, cursor);

        // THEN
        assertTrue(differs);
    }

    @Test
    public void executeCursorDatabaseOperationIfSharedPrefsContentNull() {
        // GIVEN null for sharedPreferences.getAll()

        // WHEN
        boolean differs = activity.cachedContactsDiffers(sharedPrefs, cursor);

        // THEN
        assertTrue(differs);
    }

    @Test
    public void executeCachedOperationIfDatabaseCursorNull() {
        // GIVEN null for Cursor

        // WHEN
        boolean differs = activity.cachedContactsDiffers(sharedPrefs, null);

        // THEN
        assertFalse(differs);
    }

    @Test
    public void cachedContactsNotDiffersWhenNoDiffBetweenRegistryAndCache() {
        // GIVEN
        cursor.setCount(133);
        sharedPrefs.setAll(createContactRegistries(133));

        // WHEN
        boolean differs = activity.cachedContactsDiffers(sharedPrefs, cursor);

        // THEN
        assertFalse(differs);
    }

    private HashMap<String, Object> createContactRegistries(int capacity) {
        HashMap<String, Object> map = new HashMap<>(capacity);
        for (int index = 0; index < capacity; index++) {
            map.put("Contact"+index, "YourName");
        }
        return map;
    }


    private static class CursorMock implements Cursor {

        private int count;

        private void setCount(int count) {
            this.count = count;
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public int getPosition() {
            return 0;
        }

        @Override
        public boolean move(int offset) {
            return false;
        }

        @Override
        public boolean moveToPosition(int position) {
            return false;
        }

        @Override
        public boolean moveToFirst() {
            return false;
        }

        @Override
        public boolean moveToLast() {
            return false;
        }

        @Override
        public boolean moveToNext() {
            return false;
        }

        @Override
        public boolean moveToPrevious() {
            return false;
        }

        @Override
        public boolean isFirst() {
            return false;
        }

        @Override
        public boolean isLast() {
            return false;
        }

        @Override
        public boolean isBeforeFirst() {
            return false;
        }

        @Override
        public boolean isAfterLast() {
            return false;
        }

        @Override
        public int getColumnIndex(String columnName) {
            return 0;
        }

        @Override
        public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
            return 0;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return null;
        }

        @Override
        public String[] getColumnNames() {
            return new String[0];
        }

        @Override
        public int getColumnCount() {
            return 0;
        }

        @Override
        public byte[] getBlob(int columnIndex) {
            return new byte[0];
        }

        @Override
        public String getString(int columnIndex) {
            return null;
        }

        @Override
        public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {

        }

        @Override
        public short getShort(int columnIndex) {
            return 0;
        }

        @Override
        public int getInt(int columnIndex) {
            return 0;
        }

        @Override
        public long getLong(int columnIndex) {
            return 0;
        }

        @Override
        public float getFloat(int columnIndex) {
            return 0;
        }

        @Override
        public double getDouble(int columnIndex) {
            return 0;
        }

        @Override
        public int getType(int columnIndex) {
            return 0;
        }

        @Override
        public boolean isNull(int columnIndex) {
            return false;
        }

        @Override
        public void deactivate() {

        }

        @Override
        public boolean requery() {
            return false;
        }

        @Override
        public void close() {

        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void registerContentObserver(ContentObserver observer) {

        }

        @Override
        public void unregisterContentObserver(ContentObserver observer) {

        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public void setNotificationUri(ContentResolver cr, Uri uri) {

        }

        @Override
        public Uri getNotificationUri() {
            return null;
        }

        @Override
        public boolean getWantsAllOnMoveCalls() {
            return false;
        }

        @Override
        public void setExtras(Bundle extras) {

        }

        @Override
        public Bundle getExtras() {
            return null;
        }

        @Override
        public Bundle respond(Bundle extras) {
            return null;
        }
    }

    private static class SharedPreferencesMock implements SharedPreferences {

        private Map<String, ?> cachedMap;

        private void setAll(Map<String, ?> cachedMap) {
            this.cachedMap = cachedMap;
        }

        @Override
        public Map<String, ?> getAll() {
            return cachedMap;
        }

        @Nullable
        @Override
        public String getString(String key, @Nullable String defValue) {
            return null;
        }

        @Nullable
        @Override
        public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
            return null;
        }

        @Override
        public int getInt(String key, int defValue) {
            return 0;
        }

        @Override
        public long getLong(String key, long defValue) {
            return 0;
        }

        @Override
        public float getFloat(String key, float defValue) {
            return 0;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            return false;
        }

        @Override
        public boolean contains(String key) {
            return false;
        }

        @Override
        public Editor edit() {
            return null;
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

        }
    }

}
