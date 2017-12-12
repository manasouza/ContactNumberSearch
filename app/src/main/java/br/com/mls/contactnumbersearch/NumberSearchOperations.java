package br.com.mls.contactnumbersearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;
import android.view.KeyEvent;

public class NumberSearchOperations {

	static final String PHONE_NUMBER_SEPARATOR = " / ";
	
	private UISignalizer uiSignalizer;

	static final String CONTACT_PHONE_ITEM = "contactPhone";

	static final String CONTACT_NAME_ITEM = "contactName";

	public NumberSearchOperations(UISignalizer signalizer) {
		this.uiSignalizer = signalizer;
	}
	
	boolean validateEnteredChars(String chars, char currentChar, int keyCode) {
		try {
			if (chars != null && !"".equals(chars)) {
				Long.parseLong(chars);
			}
			//			boolean numberValid = isNumber(currentChar, keyCode);
			boolean numberValid = true;
			uiSignalizer.numberValid(numberValid);
			return numberValid;
		} catch (NumberFormatException nfe) {
			Log.e(this.getClass().getName(), "Invalid number", nfe);
			uiSignalizer.numberValid(false);
			return false;
		}
	}

	boolean isNumber(char currentChar, int keyCode) {
		return keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9 ? true : false;
	}

	/**
	 * @param chars current chars at edit text field
	 * @param currentChar current char inputed by user at edit text field
	 * @param specificContactListLength lenght to be considered. According to app rules, could be different depending on char input/deletion
	 * @param isNumber if the inputed char is a number
	 * @param backwardSearch <code>true</code> if the user is deleting chars. <code>false</code> if it is inputing chars 
	 * @return refreshed contact list based on params
	 */
	List<Map<String, Object>> refreshContactDataList(String chars, char currentChar, int specificContactListLength, boolean isNumber, boolean backwardSearch) {
		List<Map<String, Object>> dataList = new ArrayList<Map<String,Object>>();
		for (int index = 0; index < specificContactListLength; index++) {
			Map<String, Object> newDataMap = new HashMap<String, Object>();
			Map<String, Object> dataMap = uiSignalizer.getSpecificItem(index, backwardSearch);
			String phones = (String) dataMap.get(NumberSearchOperations.CONTACT_PHONE_ITEM);
			// [#24] if it's a merged contact, one of them could come without phone number
			if (phones != null) {
				String onlyNumbersPhone = excludeNonNumberChars(phones);
				if (onlyNumbersPhone != null && onlyNumbersPhone.contains((chars != null && !"".equals(chars)) ? chars : getCurrentChar(currentChar, chars, isNumber))) {
					newDataMap.put(NumberSearchOperations.CONTACT_NAME_ITEM, dataMap.get(NumberSearchOperations.CONTACT_NAME_ITEM));
					newDataMap.put(NumberSearchOperations.CONTACT_PHONE_ITEM, phones);
					dataList.add(newDataMap);
				}
			}
		}
		return dataList;
	}

	String excludeNonNumberChars(String phones) {
		StringBuilder sb = new StringBuilder();
		String[] phoneNumbers = phones.split(PHONE_NUMBER_SEPARATOR);
		for (String number : phoneNumbers) {
			sb.append(number.replaceAll("[^\\d.]", ""));
			sb.append(PHONE_NUMBER_SEPARATOR);
		}
		return sb.toString();
	}

	String getCurrentChar(char currentChar, String cachedChars, boolean isNumber) {
		return isNumber ? String.valueOf(currentChar) : cachedChars;
	}

	
}
