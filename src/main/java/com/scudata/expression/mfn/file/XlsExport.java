package com.scudata.expression.mfn.file;

import java.io.IOException;

import com.scudata.common.ArgumentTokenizer;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.dm.KeyWord;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.SubCursor;
import com.scudata.excel.ExcelTool;
import com.scudata.excel.ExcelUtils;
import com.scudata.expression.Expression;
import com.scudata.expression.FileFunction;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.AppMessage;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * f.xlsexport(A,x:F,��;s;p) ����Excel�ļ���sΪҳ����sʡ��ʱд���һҳ��A�����αꡣxlsx�ļ�д��100�����Զ�ֹͣ
 * 
 * @a ԭ�ļ����ڽ��������һ�еĸ�ʽ����д
 * @t �������⣬ԭ�ļ�����ʱ��Ϊ���һ�������ݵ����Ǳ���
 * @c ʹ����ʽд�����ļ���ԭ�ļ�Ҫ��ȫ���룬����̫��
 * @w A�����е����л�س�/Tab�ָ��Ĵ�����@t@c���⣬��x:F����
 * @p @w��ת�ã����е����������к��еģ��Ǵ�ʱ����
 * @m xlsx����100����ʱ�����µ�sheetд��
 */
public class XlsExport extends FileFunction {

	/**
	 * ����
	 */
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xlsexport"
					+ mm.getMessage("function.missingParam"));
		}

		IParam param0;
		IParam param1 = null;
		String pwd = null;
		if (param.getType() == IParam.Semicolon) { // ;
			if (param.getSubSize() != 2 && param.getSubSize() != 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlsexport"
						+ mm.getMessage("function.invalidParam"));
			}

			param0 = param.getSub(0);
			param1 = param.getSub(1);
			if (param.getSubSize() == 3) {
				IParam pwdParam = param.getSub(2);
				Object tmp = pwdParam.getLeafExpression().calculate(ctx);
				if (tmp != null)
					pwd = tmp.toString();
				if ("".equals(pwd))
					pwd = null;
			}
			if (param0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlsexport"
						+ mm.getMessage("function.invalidParam"));
			}
		} else {
			param0 = param;
		}

		Object src;
		Expression[] exps = null;
		String[] names = null;
		Object s = null;

		if (param0.isLeaf()) {
			src = param0.getLeafExpression().calculate(ctx);
		} else { // series,xi:fi...
			IParam sub = param0.getSub(0);
			if (sub == null || !sub.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlsexport"
						+ mm.getMessage("function.invalidParam"));
			}

			src = sub.getLeafExpression().calculate(ctx);
			int size = param0.getSubSize();
			exps = new Expression[size - 1];
			names = new String[size - 1];
			for (int i = 1; i < size; ++i) {
				sub = param0.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("xlsexport"
							+ mm.getMessage("function.invalidParam"));
				} else if (sub.isLeaf()) {
					exps[i - 1] = sub.getLeafExpression();
				} else {
					if (sub.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("xlsexport"
								+ mm.getMessage("function.invalidParam"));
					}

					IParam p1 = sub.getSub(0);
					if (p1 == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("xlsexport"
								+ mm.getMessage("function.invalidParam"));
					}

					exps[i - 1] = p1.getLeafExpression();
					IParam p2 = sub.getSub(1);
					if (p2 != null) {
						names[i - 1] = p2.getLeafExpression()
								.getIdentifierName();
					}
				}
			}
		}

		if (param1 != null) {
			s = param1.getLeafExpression().calculate(ctx);
		}

		String opt = option;
		boolean isXlsx = false, isTitle = false, isSsxxf = false, isAppend = false, isM = false;
		if (opt != null) {
			if (opt.indexOf('t') != -1)
				isTitle = true;
			if (opt.indexOf('c') != -1)
				isSsxxf = true;
			if (opt.indexOf('a') != -1)
				isAppend = true;
			if (opt.indexOf('m') != -1)
				isM = true;
		}
		boolean isP = opt != null && opt.indexOf("p") > -1;
		boolean isK = opt != null && opt.indexOf("k") > -1;

		boolean isW = opt != null && opt.indexOf("w") > -1;
		if (isW) {
			if (isTitle || isSsxxf) {
				throw new RQException(AppMessage.get().getMessage(
						"xlsexport.nowtc"));
			}

			if (exps != null) {
				throw new RQException(AppMessage.get().getMessage(
						"xlsexport.nowfields"));
			}
		}

		if (!isW) {
			if (isP) {
				// ѡ��@{0}ֻ�ܺ�ѡ��@wͬʱʹ�á�
				throw new RQException(AppMessage.get().getMessage(
						"xlsimport.pnnotw", "p"));
			}
		}

		if (file != null) // ʹ��poi�ķ����жϰ汾
			isXlsx = ExcelUtils.isXlsxFile(file);

		// ���sheet����
		ExcelUtils.checkSheetName(s);

		ExcelTool et = new ExcelTool(file, isTitle, isXlsx, isSsxxf, isAppend,
				s, pwd, isW, isK);

		int maxCount = et.getMaxLineCount();
		if (isTitle)
			maxCount--;

		Sequence seq = null;
		ICursor cursor = null;
		boolean isStr = false;
		if (isW) {
			if (src != null && src instanceof String) { // ����\n\tƴ�ɵĴ�
				src = parseSequence((String) src);
				isStr = true;
			}
		}
		if (src == null) {
			return null;
		}
		if (src instanceof Sequence) {
			seq = (Sequence) src;
			if (!isStr) {// ��������
				if (isP) {
					seq = ExcelUtils.transpose(seq);
					src = seq;
				}
			}
		}
		if (isM) {
			try {
				xlsExportM(src, maxCount, et, s, exps, names, isTitle, isW, ctx);
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			} finally {
				try {
					et.close();
				} catch (IOException e) {
					throw new RQException(e.getMessage(), e);
				}
			}
			return null;
		}

		if (src instanceof Sequence) {
			if (seq.length() > maxCount) {
				cursor = new MemoryCursor(seq, 1, maxCount + 1);
				seq = null;
			}
		} else if (src instanceof ICursor) {
			cursor = (ICursor) src;
			cursor = new SubCursor(cursor, maxCount);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xlsexport"
					+ mm.getMessage("function.paramTypeError"));
		}

		try {
			if (seq != null) {
				et.fileXlsExport(seq, exps, names, isTitle, isW, ctx);
			} else {
				et.fileXlsExport(cursor, exps, names, isTitle, isW, ctx);
			}
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				et.close();
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
		}

		return null;
	}

	/**
	 * �Խڵ����Ż�
	 * @param ctx ����������
	 * @param Node �Ż���Ľڵ�
	 */
	public Node optimize(Context ctx) {
		if (param != null) {
			// �Բ������Ż�
			param.optimize(ctx);
		}

		return this;
	}

	private void xlsExportM(Object src, int maxCount, ExcelTool et, Object s,
			Expression[] exps, String[] names, boolean isTitle, boolean isW,
			Context ctx) throws Exception {
		Sequence seq = null;
		ICursor cursor = null;
		if (src instanceof Sequence) {
			seq = (Sequence) src;
			int totalCount = seq.length();
			if (totalCount > maxCount) {
				String sheetName = s == null ? "Sheet" : (String) s;
				int start = 1;
				int index = 1;
				while (totalCount > 0) {
					cursor = new MemoryCursor(seq, start, start + maxCount);
					if (index > 1)
						et.setSheet(sheetName + index);
					et.fileXlsExport(cursor, exps, names, isTitle, isW, ctx);
					start += maxCount;
					index++;
					totalCount -= maxCount;
				}
			} else {
				et.fileXlsExport(seq, exps, names, isTitle, isW, ctx);
			}
		} else if (src instanceof ICursor) {
			cursor = (ICursor) src;
			String sheetName = s == null ? "Sheet" : (String) s;
			int index = 1;
			ICursor subCursor;
			while (cursor.peek(1) != null) {
				subCursor = new SubCursor(cursor, maxCount);
				if (index > 1)
					et.setSheet(sheetName + index);
				et.fileXlsExport(subCursor, exps, names, isTitle, isW, ctx);
				index++;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xlsexport"
					+ mm.getMessage("function.paramTypeError"));
		}
	}

	/**
	 * ���ַ�������Ϊ����
	 * 
	 * @param str
	 *            Ҫ�������ַ���
	 * @return
	 */
	public static Sequence parseSequence(String str) {
		try {
			str = str.replaceAll("\r\n", "\n");
			str = str.replaceAll("\r", "\n");
		} catch (Exception x) {
		}
		String rowStr;
		String item;
		Sequence seq = new Sequence();
		ArgumentTokenizer rows = new ArgumentTokenizer(str, '\n');
		while (rows.hasMoreTokens()) {
			rowStr = rows.nextToken();
			Sequence subSeq = new Sequence();
			ArgumentTokenizer items = new ArgumentTokenizer(rowStr, '\t');
			while (items.hasMoreTokens()) {
				item = items.nextToken();
				Object val = item;
				if (StringUtils.isValidString(item)) {
					if (item.startsWith(KeyWord.CONSTSTRINGPREFIX)
							&& !item.endsWith(KeyWord.CONSTSTRINGPREFIX)) { // �ַ�������'
						val = item.substring(1);
					} else {
						val = Variant.parseCellValue(item);
					}
				}
				subSeq.add(val);
			}
			seq.add(subSeq);
		}
		return seq;
	}

}