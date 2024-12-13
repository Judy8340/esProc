package com.scudata.excel;

import java.io.IOException;

/**
 * Sheet object
 *
 */
public abstract class SheetObject {

	/**
	 * Whether the file in xlsx format
	 */
	protected boolean isXls;

	/**
	 * SheetInfo object
	 */
	public SheetInfo sheetInfo;

	/**
	 * Import the excel sheet and return the sequence object.
	 * 
	 * @param fields
	 *            Field names
	 * @param startRow
	 *            Start row
	 * @param endRow
	 *            End row
	 * @param bTitle
	 *            Include title line
	 * @param isCursor
	 *            Whether to return the cursor
	 * @param isN
	 *            Trim when read cell content
	 * @param removeBlank
	 *            Whether to delete blank lines at the beginning and end
	 * @return
	 * @throws Exception
	 */
	public abstract Object xlsimport(String[] fields, int startRow, int endRow,
			String opt)
			throws Exception;

	/**
	 * Close
	 */
	public abstract void close() throws IOException;
}
