package com.scudata.expression.mfn.xo;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.SubCursor;
import com.scudata.excel.ExcelUtils;
import com.scudata.excel.SheetObject;
import com.scudata.excel.SheetXls;
import com.scudata.excel.XlsFileObject;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.XOFunction;
import com.scudata.resources.AppMessage;
import com.scudata.resources.EngineMessage;

/**
 * xo.xlsexport(A,x:Fi,..;s) ��sheet��д�����У�s���������¼ӣ�xo��@w��ʱA�����α�
 * 
 * @a s�Ѵ���ʱ���ø�ʽ׷��д��ȱʡ������д
 * @t �б��⣬ҳ��������ʱ��Ϊ���һ�������ݵ����Ǳ���
 */
public class XlsExport extends XOFunction {

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
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlsexport"
						+ mm.getMessage("function.invalidParam"));
			}

			param0 = param.getSub(0);
			param1 = param.getSub(1);
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
		boolean isTitle = false, isAppend = false;
		if (opt != null) {
			if (opt.indexOf('t') != -1)
				isTitle = true;
			if (opt.indexOf('a') != -1)
				isAppend = true;
		}
		boolean isW = opt != null && opt.indexOf("w") > -1;
		boolean isP = opt != null && opt.indexOf("p") > -1;
		if (isW) {
			if (isTitle) {
				throw new RQException(AppMessage.get().getMessage(
						"xlsexport.nowt", "t"));
			}
			if (file.supportCursor()) {
				throw new RQException(AppMessage.get().getMessage(
						"xlsexport.nowt", "w"));
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

		// ���sheet����
		ExcelUtils.checkSheetName(s);

		int startRow, maxRowCount;
		SheetObject so = null;
		try {
			// startRowAndMaxRow = xo.getStartRowAndMaxRow(s, isTitle,
			// !isAppend);

			if (file.getFileType() == XlsFileObject.TYPE_READ) {
				throw new RQException("xlsexport"
						+ " : xlsopen@r does not support xlsexport");
			}
			so = file.getSheetObject(s, true, !isAppend);
			SheetXls sx = (SheetXls) so;
			startRow = sx.getStartRow(isTitle);
			maxRowCount = sx.getMaxRowCount();
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
		int maxCount = maxRowCount - startRow;
		if (isTitle)
			maxCount--;
		if (maxCount <= 0) {
			return null;
		}

		Sequence seq = null;
		ICursor cursor = null;
		boolean isStr = false;
		if (isW) {
			if (src != null && src instanceof String) { // ����\n\tƴ�ɵĴ�
				src = com.scudata.expression.mfn.file.XlsExport
						.parseSequence((String) src);
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

		if (src instanceof Sequence) {
			seq = (Sequence) src;
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
				file.xlsexport(so, seq, exps, names, s, startRow, opt, ctx);
			} else {
				file.xlsexport(so, cursor, exps, names, s, startRow, opt, ctx);
			}
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * �Խڵ����Ż�
	 * 
	 * @param ctx
	 *            ����������
	 * @param Node
	 *            �Ż���Ľڵ�
	 */
	public Node optimize(Context ctx) {
		if (param != null) {
			// �Բ������Ż�
			param.optimize(ctx);
		}

		return this;
	}
}