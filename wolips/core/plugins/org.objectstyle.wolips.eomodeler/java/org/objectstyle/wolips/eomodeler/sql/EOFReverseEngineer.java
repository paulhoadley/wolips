package org.objectstyle.wolips.eomodeler.sql;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.webobjects.eoaccess.EOAdaptor;
import com.webobjects.eoaccess.EOAdaptorChannel;
import com.webobjects.eoaccess.EOAdaptorContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

public class EOFReverseEngineer {
	private String _adaptorName;

	private NSMutableDictionary _connectionDictionary;

	private EOAdaptor _adaptor;

	private EOAdaptorContext _context;

	private EOAdaptorChannel _channel;

	public EOFReverseEngineer(String adaptorName, Map connectionDictionary) {
		_adaptorName = adaptorName;
		_connectionDictionary = new NSMutableDictionary();
		Iterator entriesIter = connectionDictionary.entrySet().iterator();
		while (entriesIter.hasNext()) {
			Map.Entry entry = (Map.Entry)entriesIter.next();
			if (entry.getValue() != null) {
				_connectionDictionary.setObjectForKey(entry.getValue(), entry.getKey());
			}
		}
		_adaptor = EOAdaptor.adaptorWithName(_adaptorName);
		_adaptor.setConnectionDictionary(_connectionDictionary);
		_adaptor.assertConnectionDictionaryIsValid();
	}

	public void open() {
		_context = _adaptor.createAdaptorContext();
		_channel = _context.createAdaptorChannel();
		_channel.openChannel();
	}

	public void close() {
		if (_channel != null) {
			_channel.closeChannel();
			_channel = null;
		}
	}

	public List reverseEngineerTableNames() {
		open();
		try {
			NSArray tableNamesArray = _channel.describeTableNames();
			LinkedList tableNamesList = new LinkedList();
			Enumeration tableNamesEnum = tableNamesArray.objectEnumerator();
			while (tableNamesEnum.hasMoreElements()) {
				tableNamesList.add(tableNamesEnum.nextElement());
			}
			return tableNamesList;
		} finally {
			close();
		}
	}

	public File reverseEngineerIntoModel() throws IOException {
		List tableNames = reverseEngineerTableNames();
		File eomodelFile = reverseEngineerWithTableNamesIntoModel(tableNames);
		return eomodelFile;
	}

	public File reverseEngineerWithTableNamesIntoModel(List tableNamesList) throws IOException {
		open();
		try {
			NSMutableArray tableNamesArray = new NSMutableArray();
			Iterator tableNamesIter = tableNamesList.iterator();
			while (tableNamesIter.hasNext()) {
				tableNamesArray.addObject(tableNamesIter.next());
			}
			com.webobjects.eoaccess.EOModel eofModel = _channel.describeModelWithTableNames(tableNamesArray);
			File tempFile = File.createTempFile("EntityModeler", "tmp");
			File eomodelFolder = new File(tempFile.getParentFile(), "EM" + System.currentTimeMillis() + ".eomodeld");
			tempFile.delete();
			eofModel.writeToFile(eomodelFolder.getAbsolutePath());
			return eomodelFolder;
		} finally {
			close();
		}
	}
}