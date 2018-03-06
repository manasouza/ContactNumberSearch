package br.com.mls.contactnumbersearch;

import java.util.Map;

public interface UISignalizer {

	void numberValid(boolean isValid);
	
	Map<String, Object> getSpecificItem(int index, boolean forwardSearch);
}
