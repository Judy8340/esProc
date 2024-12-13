package com.scudata.excel;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import com.scudata.common.CellLocation;
import com.scudata.common.Logger;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.IResource;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;
import com.scudata.resources.AppMessage;

/**
 * Excel file object base class
 *
 */
public abstract class XlsFileObject extends Table implements IResource {
	/** Memory mode */
	public static final byte TYPE_NORMAL = 0;
	/** Streaming read */
	public static final byte TYPE_READ = 1;
	/** Streaming write */
	public static final byte TYPE_WRITE = 2;

	/**
	 * file type. The default is memory mode.
	 */
	protected byte fileType = TYPE_NORMAL;

	/**
	 * Columns
	 */
	/** Sheet name */
	public static final int COL_NAME = 0;
	/** Row count */
	public static final int COL_ROW_COUNT = 1;
	/** Column count */
	public static final int COL_COL_COUNT = 2;

	/**
	 * Column names
	 */
	/** Sheet name */
	private final static String LABEL_SHEET_NAME = "stname";
	/** Row count */
	private final static String LABEL_ROW_COUNT = "nrows";
	/** Column count */
	private final static String LABEL_COL_COUNT = "ncols";

	/**
	 * The container for the sheets. The key is the serial number of the sheet,
	 * and the value is the object of the sheet.
	 */
	protected Map<Integer, SheetObject> sheets = new HashMap<Integer, SheetObject>();
	/**
	 * Prefix of sheet name
	 */
	public final static String PRE_SHEET_NAME = "Sheet";

	/**
	 * Whether the file object is closed
	 */
	protected boolean isClosed = false;

	/**
	 * Constructor
	 */
	public XlsFileObject() {
		super(
				new String[] { LABEL_SHEET_NAME, LABEL_ROW_COUNT,
						LABEL_COL_COUNT });
	}

	/**
	 * Get file type
	 * 
	 * @return
	 */
	public byte getFileType() {
		return fileType;
	}

	/**
	 * Delete sheet by sheet name
	 * 
	 * @param sheetName
	 */
	public void removeSheet(String sheetName) {
		for (int i = 1, len = length(); i <= len; i++) {
			Object labelName = getRecord(i).getFieldValue(COL_NAME);
			if (sheetName.equals(labelName)) {
				delete(i);
				break;
			}
		}
	}

	/**
	 * Get sheet object according to parameter s.
	 * 
	 * @param s
	 *            Sheet serial number or name
	 * @param createSheet
	 *            Whether to create a new sheet when the sheet is not found
	 * @return
	 * @throws Exception
	 */
	public SheetObject getSheetObject(Object s, boolean createSheet)
			throws Exception {
		return getSheetObject(s, createSheet, false);
	}

	/**
	 * Get sheet object according to parameter s.
	 * 
	 * @param s
	 *            Sheet serial number or name
	 * @param createSheet
	 *            Whether to create a new sheet when the sheet is not found
	 * @param deleteOldSheet
	 *            Whether to delete the old sheet when getting the sheet.
	 * 
	 * @return
	 * @throws Exception
	 */
	public abstract SheetObject getSheetObject(Object s, boolean createSheet,
			boolean deleteOldSheet) throws Exception;

	/**
	 * Refresh the information of the sheet.
	 * 
	 * @param si
	 *            SheetInfo
	 */
	public synchronized void resetSheetInfo(SheetInfo si) {
		if (si != null)
			for (int i = 1, len = length(); i <= len; i++) {
				BaseRecord r = getRecord(i);
				if (si.getSheetName().equals(r.getFieldValue(COL_NAME))) {
					r.set(COL_ROW_COUNT, si.getRowCount());
					r.set(COL_COL_COUNT, si.getColCount());
					break;
				}
			}
	}

	/**
	 * Does import support @c
	 * 
	 * @return
	 */
	public abstract boolean supportCursor();

	/**
	 * f.xlswrite(xo,p). Write the Excel object to the file, xo cannot be opened
	 * in the way of @r@w.
	 * 
	 * @param fo
	 * @param p
	 */
	public abstract void xlswrite(FileObject fo, String p);

	/**
	 * xo.xlsimport(). Get the sequence from the first sheet.
	 * 
	 * @param opt
	 *            Options
	 * @return
	 * @throws Exception
	 */
	public Object xlsimport(String opt) throws Exception {
		return xlsimport(null, null, 0, 0, opt);
	}

	/**
	 * xo.xlsimport(Fi,..;s,b:e). Get the sequence from sheet s.
	 * 
	 * @t The first line is the title. When there is a b parameter, the title is
	 *    considered to be in line b.
	 * @c Return a cursor, xo must be opened with @r
	 * 
	 * @param fields
	 *            Fields to be imported
	 * @param s
	 *            sheet name or sheet serial number
	 * @param startRow
	 *            Start row
	 * @param endRow
	 *            End row
	 * @param opt
	 *            Options
	 * @return
	 */
	public Object xlsimport(String[] fields, Object s, int startRow,
			int endRow, String opt) throws Exception {
		if (fileType == TYPE_WRITE) {
			throw new RQException("xlsimport"
					+ AppMessage.get().getMessage("filexls.wimport"));
		}
		SheetObject sx = getSheetObject(s, false);
		Object result = sx.xlsimport(fields, startRow, endRow, opt);
		return result;
	}

	/**
	 * xo.xlsexport(A,x:Fi,..;s). Write sequence to sheet, add sheet if s does
	 * not exist. When xo is opened with @w, A can be a cursor.
	 * 
	 * @t There is a title. When the sheet has content, it is considered that
	 *    the last row with content is the title.
	 * 
	 * @param A
	 *            The sequence to be exported. When xo is opened with @w, A can
	 *            be a cursor.
	 * @param exps
	 *            Field expressions
	 * @param fields
	 *            Fields to be exported
	 * @param s
	 *            sheet name or sheet serial number
	 * @param bTitle
	 *            Has title line
	 * @param isAppend
	 *            Whether to append export
	 * @param startRow
	 *            Start row
	 * @param ctx
	 *            Context
	 * @throws Exception
	 */
	public void xlsexport(SheetObject so, Object A, Expression[] exps,
			String[] fields, Object s, boolean bTitle, boolean isAppend,
			int startRow, Context ctx) throws Exception {
		if (fileType == TYPE_READ) {
			// : xlsopen@r does not support xlsexport
			throw new RQException("xlsexport"
					+ AppMessage.get().getMessage("filexls.rexport"));
		}
		SheetXls sx = (SheetXls) so;
		if (A instanceof Sequence) {
			sx.xlsexport((Sequence) A, exps, fields, bTitle, isAppend,
					startRow, ctx);
		} else if (A instanceof ICursor) {
			sx.xlsexport((ICursor) A, exps, fields, bTitle, isAppend, startRow,
					ctx);
		}
		resetSheetInfo(sx.sheetInfo);
	}

	/**
	 * xo.xlscell(pos1:pos2,sheet;content). Fill in the string content to the
	 * cell a of sheet s. Content can be separated by \n and \t and filled in
	 * adjacent rows and columns respectively. Export to the first sheet when
	 * sheet is omitted. Sheet cannot be opened by @r@w. When there is no
	 * content parameter, the cell text string from pos1 to pos2 is read and
	 * returned. Read to the end without pos2.
	 * 
	 * @i Line insertion and filling, the default is overwriting
	 * 
	 * @param pos1
	 *            CellLocation
	 * @param pos2
	 *            CellLocation
	 * @param sheet
	 *            sheet name or sheet serial number
	 * @param content
	 *            The content string to be written
	 * @param isRowInsert
	 *            Line insertion and filling, the default is overwriting
	 * @param isGraph
	 *            What is written is a graph
	 * @param isW
	 *            Option @w
	 * @param isP
	 *            Option @p
	 * @param isN
	 *            Option @n
	 * @return
	 * @throws Exception
	 */
	public Object xlscell(CellLocation pos1, CellLocation pos2, Object sheet,
			Object content, boolean isRowInsert, boolean isGraph, boolean isW,
			boolean isP, boolean isN) throws Exception {
		SheetObject sheetObject = getSheetObject(sheet, false);
		SheetXls sx = (SheetXls) sheetObject;
		int row1 = pos1.getRow();
		int col1 = pos1.getCol();
		if (pos2 != null) {
			int row2 = pos2.getRow();
			int col2 = pos2.getCol();

			pos1 = new CellLocation(Math.min(row1, row2), Math.min(col1, col2));
			pos2 = new CellLocation(Math.max(row1, row2), Math.max(col1, col2));
		} else {
			if (content == null) {
				// a:��ʾ�����ƣ�ȡ��sheet�����
				pos2 = new CellLocation(sx.sheetInfo.getRowCount(),
						sx.sheetInfo.getColCount());
			} else {
				// a:��ʾ�����ƣ���contentȫ�����ã��������õ���������
				pos2 = new CellLocation(sx.getMaxRowCount(),
						sx.getMaxColCount());
			}
		}
		if (content == null) {
			return sx.getCells(pos1, pos2, isGraph, isW, isP, isN);
		} else {
			sx.setCells(pos1, pos2, content, isRowInsert, isGraph);
			resetSheetInfo(sx.sheetInfo);
			return null;
		}
	}

	/**
	 * Rename sheet
	 * 
	 * @param sheet
	 *            sheet name or sheet serial number
	 * @param newSheetName
	 *            New sheet name
	 * @throws Exception
	 */
	public void rename(Object sheet, String newSheetName) throws Exception {
		SheetObject sheetObject = getSheetObject(sheet, false);
		SheetXls sx = (SheetXls) sheetObject;
		String oldSheetName = sx.sheetInfo.getSheetName();
		sx.rename(newSheetName);
		for (int i = 1, len = length(); i <= len; i++) {
			BaseRecord r = getRecord(i);
			if (oldSheetName.equals(r.getFieldValue(COL_NAME))) {
				r.set(COL_NAME, newSheetName);
				break;
			}
		}
	}

	public void close() {
		try {
			if (!isClosed)
				xlsclose();
		} catch (IOException e) {
			Logger.error(e);
		}
	}

	/**
	 * xo.xlsclose(). Excel objects opened in @r@w need to be closed.
	 */
	public abstract void xlsclose() throws IOException;

	/**
	 * The name of the new sheet
	 * 
	 * @return
	 */
	protected String getNewSheetName() {
		Sequence sheetNames = fieldValues(COL_NAME);
		int index = 1;
		while (sheetNames.contains(PRE_SHEET_NAME + index, false)) {
			index++;
		}
		return PRE_SHEET_NAME + index;
	}

	/**
	 * ��xo����Ϊs��sheet�ƶ���xo��������Ϊs���� xo��ʡ�ԣ���ʾsheet������s��Ҳʡ�Ա�ʾɾ���� xo��δʡ�ԣ�s��ʡ�Ա�ʾ��s��ԭ��
	 * 
	 * @param s
	 * @param s1
	 * @param xo1
	 * @param isCopy
	 */
	public void xlsmove(String s, String s1, XlsFileObject xo1, boolean isCopy)
			throws Exception {
		FileXls fileXls = (FileXls) this;
		if (xo1 == null) {
			if (s1 == null) { // ɾ��
				int sheetIndex = fileXls.wb.getSheetIndex(s);
				fileXls.wb.removeSheetAt(sheetIndex);
				removeSheet(s);
			} else {
				if (isCopy) {// �������ڸ���
					fileXls.cloneSheet(s, s1);
				} else { // ����
					rename(s, s1);
				}
			}
		} else {
			// ��xo����Ϊs��sheet�ƶ����Ƶ�xo��
			FileXls fileXls1 = ((FileXls) xo1);
			String toSheetName = StringUtils.isValidString(s1) ? s1 : s;
			// ��������������Ѿ�������ɾ��
			xo1.getSheetObject(toSheetName, true, true);
			int fromSheetIndex = fileXls.wb.getSheetIndex(s);
			int toSheetIndex = fileXls1.wb.getSheetIndex(toSheetName);
			copySheet(fileXls.wb, fileXls.wb.getSheetAt(fromSheetIndex),
					fileXls1.wb, fileXls1.wb.getSheetAt(toSheetIndex),
					moveStyleMap);
			// �ƶ�ʱɾ��ԭsheet
			if (!isCopy) {
				int sheetIndex = fileXls.wb.getSheetIndex(s);
				fileXls.wb.removeSheetAt(sheetIndex);
				removeSheet(s);
			}
		}
	}

	/**
	 * ���������ظ�����ʽ��ͬһ��������ÿ��xlsmove��ʹ��ͬһ����ʽ����������xlsxתxlsʱ�е���ʽû���ƹ���
	 */
	private Map<Integer, CellStyle> moveStyleMap = new HashMap<Integer, CellStyle>();

	/**
	 * ����sheet����һ��excel������
	 * 
	 * @param fromWorkbook
	 * @param fromSheet
	 * @param toWorkbook
	 * @param toSheet
	 * @param styleMap
	 *            ���������ظ�����ʽ��ÿ��xo����һ��
	 */
	private void copySheet(Workbook fromWorkbook, Sheet fromSheet,
			Workbook toWorkbook, Sheet toSheet, Map<Integer, CellStyle> styleMap) {
		boolean fromXlsx = fromSheet instanceof XSSFSheet;
		boolean toXlsx = toSheet instanceof XSSFSheet;
		boolean isSameFileType = fromXlsx == toXlsx;
		// ��ʽ����
		toSheet.setForceFormulaRecalculation(fromSheet
				.getForceFormulaRecalculation());
		// �������ʽ
		SheetConditionalFormatting scf = fromSheet
				.getSheetConditionalFormatting();
		SheetConditionalFormatting toSheetConditionFormat = toSheet
				.getSheetConditionalFormatting();
		for (int i = 0; i < scf.getNumConditionalFormattings(); i++) {
			toSheetConditionFormat.addConditionalFormatting(scf
					.getConditionalFormattingAt(i));
		}
		// ����������Ϣ
		PaneInformation paneInformation = fromSheet.getPaneInformation();
		if (Objects.nonNull(paneInformation)) {
			toSheet.createFreezePane(
					paneInformation.getHorizontalSplitPosition(),
					paneInformation.getVerticalSplitPosition(),
					paneInformation.getHorizontalSplitTopRow(),
					paneInformation.getVerticalSplitLeftColumn());
		}
		// �ϲ���Ԫ��
		int numMergedRegions = fromSheet.getNumMergedRegions();
		for (int i = 0; i < numMergedRegions; i++) {
			CellRangeAddress mergedRegion = fromSheet.getMergedRegion(i);
			toSheet.addMergedRegion(mergedRegion.copy());
		}
		// �����п�
		int physicalNumberOfCells = fromSheet.getRow(0)
				.getPhysicalNumberOfCells();
		for (int i = 0; i < physicalNumberOfCells; i++) {
			toSheet.setColumnWidth(i, 256 * 20);
		}

		// ������
		int maxColumnNum = 0;
		for (int i = fromSheet.getFirstRowNum(), lastRow = fromSheet
				.getLastRowNum(); i <= lastRow; i++) {
			Row fromRow = fromSheet.getRow(i);
			Row toRow = toSheet.createRow(i);
			if (fromRow == null) {
				continue;
			}
			// �и�
			toRow.setHeight(fromRow.getHeight());
			// ������
			toRow.setZeroHeight(fromRow.getZeroHeight());
			// �к�
			toRow.setRowNum(fromRow.getRowNum());
			// �и�ʽ
			CellStyle fromRowStyle = fromRow.getRowStyle();
			CellStyle toRowStyle = cloneCellStyle(fromWorkbook, fromRowStyle,
					toWorkbook, styleMap, isSameFileType);
			if (toRowStyle != null)
				toRow.setRowStyle(toRowStyle);

			// ��ȡ��ǰ���������
			int jn = fromRow.getFirstCellNum() < 0 ? 0 : fromRow
					.getFirstCellNum();
			int lastCol = fromRow.getLastCellNum();
			if (lastCol > maxColumnNum) {
				maxColumnNum = lastCol;
			}
			// ���ø�
			for (int j = jn; j <= lastCol; j++) {
				Cell fromCell = fromRow.getCell(j);
				Cell toCell = toRow.createCell(j);
				if (fromCell == null) {
					continue;
				}
				// ���õ�Ԫ����ʽ
				CellStyle fromCellStyle = fromCell.getCellStyle();
				CellStyle toCellStyle = cloneCellStyle(fromWorkbook,
						fromCellStyle, toWorkbook, styleMap, isSameFileType);
				if (toCellStyle != null)
					toCell.setCellStyle(toCellStyle);
				// ���ø�ֵ
				switch (fromCell.getCellType()) {
				case NUMERIC:
					if (DateUtil.isCellDateFormatted(fromCell)) {
						Date date1 = fromCell.getDateCellValue();
						toCell.setCellValue(date1);
					} else {
						double cellValue1 = fromCell.getNumericCellValue();
						toCell.setCellValue(cellValue1);
					}
					break;
				case STRING:
					toCell.setCellValue(fromCell.getStringCellValue());
					break;
				case BLANK:
					break;
				case ERROR:
					toCell.setCellValue(fromCell.getErrorCellValue());
					break;
				case BOOLEAN:
					toCell.setCellValue(fromCell.getBooleanCellValue());
					break;
				case FORMULA:
					toCell.setCellFormula(fromCell.getCellFormula());
					break;
				default:
					break;
				}
			}
		}

		// ������
		for (int i = 0; i <= maxColumnNum; i++) {
			// �����п�
			toSheet.setColumnWidth(i, fromSheet.getColumnWidth(i));
			// ������
			toSheet.setColumnHidden(i, fromSheet.isColumnHidden(i));

			if (fromSheet.isColumnBroken(i))
				toSheet.setColumnBreak(i);
		}
	}

	/**
	 * ��¡��ʽ
	 * 
	 * @param fromWorkbook
	 *            Դ������
	 * @param fromStyle
	 *            Դ��ʽ
	 * @param toWorkbook
	 *            Ŀ�깤����
	 * @param styleMap
	 *            �洢��ͬ��ʽ��ֹ��δ���
	 * @param isSameFileType
	 *            �������Ƿ���ͬ��ʽ��XLS,XLSX��
	 * @return
	 */
	private CellStyle cloneCellStyle(Workbook fromWorkbook,
			CellStyle fromStyle, Workbook toWorkbook,
			Map<Integer, CellStyle> styleMap, boolean isSameFileType) {
		if (fromStyle == null)
			return null;

		int stHashCode = fromStyle.hashCode();
		CellStyle toStyle = styleMap.get(stHashCode);
		if (toStyle == null) {
			toStyle = toWorkbook.createCellStyle();
			if (isSameFileType) {
				toStyle.cloneStyleFrom(fromStyle);
			} else {
				// poi��֧��xlsx��xls֮��ĸ��ƣ���Ҫ�Լ�ʵ��
				copyCellStyle(fromWorkbook, fromStyle, toWorkbook, toStyle);
			}
			styleMap.put(stHashCode, toStyle);
		}
		return toStyle;
	}

	/**
	 * XLS��XLSX֮�䲻֧��cloneStyle����Ҫ�Լ�����
	 * 
	 * @param fromWorkbook
	 * @param fromStyle
	 * @param toWorkbook
	 * @param toStyle
	 */
	private void copyCellStyle(Workbook fromWorkbook, CellStyle fromStyle,
			Workbook toWorkbook, CellStyle toStyle) {
		// ˮƽ��ֱ���뷽ʽ
		toStyle.setAlignment(fromStyle.getAlignment());
		toStyle.setVerticalAlignment(fromStyle.getVerticalAlignment());

		// �߿�ͱ߿���ɫ
		toStyle.setBorderBottom(fromStyle.getBorderBottom());
		toStyle.setBorderLeft(fromStyle.getBorderLeft());
		toStyle.setBorderRight(fromStyle.getBorderRight());
		toStyle.setBorderTop(fromStyle.getBorderTop());
		toStyle.setTopBorderColor(fromStyle.getTopBorderColor());
		toStyle.setBottomBorderColor(fromStyle.getBottomBorderColor());
		toStyle.setRightBorderColor(fromStyle.getRightBorderColor());
		toStyle.setLeftBorderColor(fromStyle.getLeftBorderColor());

		// ������ǰ��
		toStyle.setFillBackgroundColor(fromStyle.getFillBackgroundColor());
		toStyle.setFillForegroundColor(fromStyle.getFillForegroundColor());

		toStyle.setDataFormat(fromStyle.getDataFormat());
		toStyle.setFillPattern(fromStyle.getFillPattern());

		if (fromStyle instanceof HSSFCellStyle) {
			HSSFCellStyle style = (HSSFCellStyle) fromStyle;
			Font fromFont = style.getFont(fromWorkbook);
			Font toFont = copyFont(toWorkbook, fromFont);
			toStyle.setFont(toFont);
		} else if (fromStyle instanceof XSSFCellStyle) {
			XSSFCellStyle style = (XSSFCellStyle) fromStyle;
			Font fromFont = style.getFont();
			Font toFont = copyFont(toWorkbook, fromFont);
			toStyle.setFont(toFont);
		}

		toStyle.setHidden(fromStyle.getHidden());
		toStyle.setIndention(fromStyle.getIndention());// ��������
		toStyle.setLocked(fromStyle.getLocked());
		toStyle.setQuotePrefixed(fromStyle.getQuotePrefixed());
		toStyle.setRotation(fromStyle.getRotation());// ��ת
		toStyle.setWrapText(fromStyle.getWrapText());
		toStyle.setShrinkToFit(fromStyle.getShrinkToFit());
	}

	/**
	 * �������壬��XLS,XLSX�޷�clone����
	 * 
	 * @param workbook
	 * @param fromFont
	 * @return
	 */
	private Font copyFont(Workbook workbook, Font fromFont) {
		boolean isBold = fromFont.getBold();
		short color = fromFont.getColor();
		short fontHeight = fromFont.getFontHeight();
		String fontName = fromFont.getFontName();
		boolean isItalic = fromFont.getItalic();
		boolean isStrikeout = fromFont.getStrikeout();
		short typeOffset = fromFont.getTypeOffset();
		byte underline = fromFont.getUnderline();

		Font toFont = workbook.findFont(isBold, color, fontHeight, fontName,
				isItalic, isStrikeout, typeOffset, underline);
		if (toFont == null) {
			toFont = workbook.createFont();
			toFont.setBold(isBold);
			toFont.setColor(color);
			toFont.setFontHeight(fontHeight);
			toFont.setFontName(fontName);
			toFont.setItalic(isItalic);
			toFont.setStrikeout(isStrikeout);
			toFont.setTypeOffset(typeOffset);
			toFont.setUnderline(underline);
		}

		return toFont;
	}
}
