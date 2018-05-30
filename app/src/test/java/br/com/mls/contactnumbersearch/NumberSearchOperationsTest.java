package br.com.mls.contactnumbersearch;
import static org.junit.Assert.*;

import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class NumberSearchOperationsTest {

	private NumberSearchOperations numberSearchOperations;
	private UISignalizerMock signalizer;
	
	@Before
	public void setUp() {
		signalizer = new UISignalizerMock();
		numberSearchOperations = new NumberSearchOperations(signalizer);
	}
	
	@Test
	public void validateNumberTyped() {
		boolean isNumber = numberSearchOperations.validateEnteredChars("3");
		assertTrue(isNumber);
	}
	
	@Test
	public void validateNonNumberTyped() {
		boolean isNumber = numberSearchOperations.validateEnteredChars("3");
		assertFalse(isNumber);
	}
	
	@Test
	public void enterFirstChar() {
		// GIVEN
		boolean backwardSearch = false;
		boolean isNumber = true;
		int specificContactListLength = signalizer.getMapListSize();
		char currentChar = '3';
		String chars = "";
		
		// WHEN
		List<Map<String, Object>> dataList = numberSearchOperations.refreshContactDataList(chars, currentChar, specificContactListLength, isNumber, backwardSearch);

		// THEN
		assertFalse("Should not be empty", dataList.isEmpty());
		assertThat(dataList.size(), is(lessThan(specificContactListLength)));
		for (Map<String, Object> map : dataList) {
			String phone = map.get(NumberSearchOperations.CONTACT_PHONE_ITEM).toString();
			assertThat(phone, containsString(String.valueOf(currentChar)));
		}
	}
	
	@Test
	public void enterSecondCharAndOn() {
		// GIVEN
		boolean backwardSearch = false;
		boolean isNumber = true;
		int specificContactListLength = signalizer.getMapListSize();
		char currentChar = '4';
		String chars = "34";
		
		// WHEN
		List<Map<String, Object>> dataList = numberSearchOperations.refreshContactDataList(chars, currentChar, specificContactListLength, isNumber, backwardSearch);
		
		// THEN
		assertFalse("Should not be empty", dataList.isEmpty());
		assertThat(dataList.size(), is(lessThan(specificContactListLength)));
		for (Map<String, Object> map : dataList) {
			String phone = map.get(NumberSearchOperations.CONTACT_PHONE_ITEM).toString();
			assertThat(phone, containsString(chars));
		}
	}
	
	@Test
	public void deleteOneCharAfterThreeEnters() {
		// GIVEN
		boolean backwardSearch = true;
		boolean isNumber = true;
		int specificContactListLength = signalizer.getMapListSize();
		char currentChar = '\0';
		String chars = "34";

		// WHEN
		List<Map<String, Object>> dataList = numberSearchOperations.refreshContactDataList(chars, currentChar, specificContactListLength, isNumber, backwardSearch);
		
		assertFalse("Should not be empty", dataList.isEmpty());
		for (Map<String, Object> map : dataList) {
			String phone = map.get(NumberSearchOperations.CONTACT_PHONE_ITEM).toString();
			assertThat(phone, containsString(chars.substring(0, (chars.length() - 1))));
		}
	}
	
	@Test
	@Ignore("It's not on scope of numberSearchOperations.refreshContactDataList, but could be in the future. The verify to empty chars is done previously")
	public void deleteLastChar() {
		// GIVEN
		boolean backwardSearch = true;
		boolean isNumber = true;
		int specificContactListLength = signalizer.getMapListSize();
		char currentChar = '\0';
		String chars = "";

		// WHEN
		List<Map<String, Object>> dataList = numberSearchOperations.refreshContactDataList(chars, currentChar, specificContactListLength, isNumber, backwardSearch);

		// THEN
		assertEquals(specificContactListLength, dataList.size());
	}

	@Test
	public void sortConsideringAccentAndLowerCaseChars() {
		// GIVEN
		String contactNameItem = NumberSearchOperations.CONTACT_NAME_ITEM;
		List<String> expectedContactList = Arrays.asList("alberto", "Álvaro", "Antônio", "Élder", "Eliane", "Valdir");

		// GIVEN
	    ArrayList<Map<String, Object>> contactList = new ArrayList<>();
		Map<String, Object> contact1 = new HashMap<>();
		contact1.put(contactNameItem, "Álvaro");
		contactList.add(contact1);
		Map<String, Object> contact2 = new HashMap<>();
		contact2.put(contactNameItem, "Antônio");
		contactList.add(contact2);
		Map<String, Object> contact3 = new HashMap<>();
		contact3.put(contactNameItem, "Élder");
		contactList.add(contact3);
		Map<String, Object> contact4 = new HashMap<>();
		contact4.put(contactNameItem, "Eliane");
		contactList.add(contact4);
		Map<String, Object> contact5 = new HashMap<>();
		contact5.put(contactNameItem, "alberto");
		contactList.add(contact5);
		Map<String, Object> contact6 = new HashMap<>();
		contact6.put(contactNameItem, "Valdir");
		contactList.add(contact6);

		// WHEN
		this.numberSearchOperations.sortContactListByName(contactList);
		List<String> currentContactList = new ArrayList<>();
		for (Map<String, Object> contactMap : contactList) {
			currentContactList.add(contactMap.get(contactNameItem).toString());
		}

		// THEN
		assertEquals(expectedContactList, currentContactList);
	}

	public class UISignalizerMock implements UISignalizer {
		
		private List<Map<String,Object>> mapList;
		
		UISignalizerMock() {
			mapList = new ArrayList<>();
			mapList.add(addContact("Neo", "3245-9087"));
			mapList.add(addContact("Trinity", "(19) 98765-0987"));
			mapList.add(addContact("Morpheus", "019987650000"));
			mapList.add(addContact("Merovingian", "+55 19 9876-8765"));
			mapList.add(addContact("Smith", "+551934568790"));
		}

		private Map<String, Object> addContact(String name, String phone) {
			Map<String, Object> map = new HashMap<>();
			map.put(NumberSearchOperations.CONTACT_NAME_ITEM, name);
			map.put(NumberSearchOperations.CONTACT_PHONE_ITEM, phone);
			return map;
		}

		@Override
		public void numberValid(boolean isValid) {
			// nothing to do for this test
		}

		@Override
		public Map<String, Object> getSpecificItem(int index, boolean forwardSearch) {
			return mapList.get(index);
		}
		
		int getMapListSize() {
			return mapList.size();
		}
	}
}
