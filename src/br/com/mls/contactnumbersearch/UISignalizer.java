package br.com.mls.contactnumbersearch;

import java.util.Map;

public interface UISignalizer {

	public void numberValid(boolean isValid);
	
	public Map<String, Object> getSpecificItem(int index, boolean forwardSearch);
}
