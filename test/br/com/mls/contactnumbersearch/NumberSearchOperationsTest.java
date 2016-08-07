package br.com.mls.contactnumbersearch;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import android.view.KeyEvent;

public class NumberSearchOperationsTest {

	private NumberSearchOperations numberSearchOperations;
	
	@Before
	public void setUp() {
		numberSearchOperations = new NumberSearchOperations(new UISignalizerMock());
	}
	
	@Test
	public void validateNumberTyped() throws Exception {
		boolean isNumber = numberSearchOperations.validateEnteredChars("3", '5', KeyEvent.KEYCODE_5);
		assertTrue(isNumber);
	}
	
	@Test
	public void validateNonNumberTyped() throws Exception {
		boolean isNumber = numberSearchOperations.validateEnteredChars("3", 'D', KeyEvent.KEYCODE_D);
		assertFalse(isNumber);
	}
	
	@Test
	public void enterFirstChar() throws Exception {
		
//		boolean refreshed = activity.refreshContactList("3", '\u0000', -100);
		
//		assertTrue(refreshed);
	}
	
	@Test
	public void enterSecondCharAndOn() throws Exception {
		
	}
	
	@Test
	public void deleteOneCharAfterThreeEnters() throws Exception {
		
	}
	
	@Test
	public void deleteLastChar() throws Exception {
		
	}

	public class UISignalizerMock implements UISignalizer {
		
		private Map<String,Object> map;
		
		public UISignalizerMock() {
			map = new HashMap<String,Object>();
		}

		@Override
		public void numberValid(boolean isValid) {
			// nothing to do for this test
		}

		@Override
		public Map<String, Object> getSpecificItem(int index, boolean forwardSearch) {
			return map;
		}
	}
}
