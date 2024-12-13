package com.scudata.expression.mfn.file;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.UserUtils;
import com.scudata.excel.ExcelTool;
import com.scudata.excel.ExcelUtils;
import com.scudata.excel.XlsxSImporter;
import com.scudata.expression.FileFunction;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.AppMessage;
import com.scudata.resources.EngineMessage;

/**
 * f.xlsimport(Fi,..;s,b:e;p) ����Excel�� sΪҳ������ţ�b,eΪ������e<0���� p������
 * 
 * @t �����Ǳ��⣬��b����ʱ��Ϊ������b��
 * @x ʹ��xlsx��ʽ��ȱʡʹ���ļ���չ���жϣ��жϲ�����xls
 * @c ���س��αֻ֧꣬��xlsx��ʽ����ʱe����С��0
 * @b ȥ��ǰ��Ŀհ��У�@cʱ��֧��
 * @w �������е����У���Ա�Ǹ�ֵ�� ��@t@c@b����
 * @p @w��ת�ã����е����������к��еģ��Ǵ�ʱ����
 * @n ����ʱ��trim��ֻʣ�մ�ʱ����null
 * @s ���سɻس�/tab�ָ��Ĵ�
 */
public class XlsImport extends FileFunction {

	/**
	 * ����
	 */
	public Object calculate(Context ctx) {
		String opt = option;
		checkOptions(opt);
		boolean isCursor = opt != null && opt.indexOf("c") > -1;
		boolean hasTitle = opt != null && opt.indexOf("t") > -1;

		if (param == null) {
			InputStream in = null;
			BufferedInputStream bis = null;
			try {
				boolean isXlsx = ExcelUtils.isXlsxFile(file);
				if (isCursor && !isXlsx) {
					// @c only supports the xlsx format.
					MessageManager mm = AppMessage.get();
					throw new RQException("xlsimport"
							+ mm.getMessage("xlsfile.needxlsx"));
				}

				if (isCursor) {
					XlsxSImporter importer = new XlsxSImporter(file, null, 0,
							0, new Integer(1), opt);
					String cursorOpt = "";
					if (hasTitle)
						cursorOpt += "t";
					return UserUtils.newCursor(importer, cursorOpt);
				} else {
					in = file.getInputStream();
					bis = new BufferedInputStream(in, Env.FILE_BUFSIZE);
					ExcelTool importer = new ExcelTool(in, isXlsx, null);
					return importer.fileXlsImport(opt);
				}
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (IOException e) {
					throw new RQException(e.getMessage(), e);
				}
				try {
					if (bis != null)
						bis.close();
				} catch (IOException e) {
					throw new RQException(e.getMessage(), e);
				}
			}
		}

		String[] fields = null;
		Object s = null;
		int start = 0;
		int end = 0;

		IParam fieldParam;
		String pwd = null;
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2 && param.getSubSize() != 3) { // ����һ��֮ǰ��
				MessageManager mm = EngineMessage.get();
				throw new RQException("xlsimport"
						+ mm.getMessage("function.invalidParam"));
			}

			fieldParam = param.getSub(0);
			IParam param1 = param.getSub(1);
			if (param.getSubSize() == 3) {
				IParam pwdParam = param.getSub(2);
				if (pwdParam != null) {
					Object tmp = pwdParam.getLeafExpression().calculate(ctx);
					if (tmp != null) {
						pwd = tmp.toString();
					}
					if ("".equals(pwd))
						pwd = null;
				}
			}
			if (param1 == null) {
			} else if (param1.isLeaf()) {
				s = param1.getLeafExpression().calculate(ctx);
			} else {
				if (param1.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("xlsimport"
							+ mm.getMessage("function.invalidParam"));
				}

				IParam sParam = param1.getSub(0);
				if (sParam != null) {
					s = sParam.getLeafExpression().calculate(ctx);
				}

				IParam posParam = param1.getSub(1);
				if (posParam == null) {
				} else if (posParam.isLeaf()) { // start
					Object obj = posParam.getLeafExpression().calculate(ctx);
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("xlsimport"
								+ mm.getMessage("function.paramTypeError"));
					}

					start = ((Number) obj).intValue();
				} else { // start:end
					if (posParam.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("xlsimport"
								+ mm.getMessage("function.invalidParam"));
					}

					IParam sub0 = posParam.getSub(0);
					IParam sub1 = posParam.getSub(1);
					if (sub0 != null) {
						Object obj = sub0.getLeafExpression().calculate(ctx);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("xlsimport"
									+ mm.getMessage("function.paramTypeError"));
						}

						start = ((Number) obj).intValue();
					}

					if (sub1 != null) {
						Object obj = sub1.getLeafExpression().calculate(ctx);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("xlsimport"
									+ mm.getMessage("function.paramTypeError"));
						}

						end = ((Number) obj).intValue();
					}
				}
			}
		} else {
			fieldParam = param;
		}

		if (fieldParam != null) {
			if (fieldParam.isLeaf()) {
				fields = new String[] { fieldParam.getLeafExpression()
						.getIdentifierName() };
			} else {
				int count = fieldParam.getSubSize();
				fields = new String[count];
				for (int i = 0; i < count; ++i) {
					IParam sub = fieldParam.getSub(i);
					if (sub == null || !sub.isLeaf()) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("xlsimport"
								+ mm.getMessage("function.invalidParam"));
					}

					fields[i] = sub.getLeafExpression().getIdentifierName();
				}
			}
		}

		boolean isXlsx = false;
		try {
			isXlsx = ExcelUtils.isXlsxFile(file);
		} catch (Throwable e1) {
			if (StringUtils.isValidString(file.getFileName())) {
				isXlsx = file.getFileName().toLowerCase().endsWith(".xlsx");
			}
		}
		if (isCursor && !isXlsx) {
			// @c only supports the xlsx format.
			MessageManager mm = AppMessage.get();
			throw new RQException("xlsimport"
					+ mm.getMessage("xlsfile.needxlsx"));
		}

		checkFieldOptions(fields, opt);

		InputStream in = null;
		BufferedInputStream bis = null;
		try {
			if (isCursor) {
				XlsxSImporter importer = new XlsxSImporter(file, fields, start,
						end, s, opt, pwd);
				String cursorOpt = "";
				if (hasTitle)
					cursorOpt += "t";
				return UserUtils.newCursor(importer, cursorOpt);
			} else {
				in = file.getInputStream();
				bis = new BufferedInputStream(in, Env.FILE_BUFSIZE);
				ExcelTool importer = new ExcelTool(in, isXlsx, pwd);
				return importer.fileXlsImport(fields, start, end, s, opt);
			}
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
			try {
				if (bis != null)
					bis.close();
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
		}
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

	public static void checkOptions(String opt) {
		boolean isCursor = opt != null && opt.indexOf("c") > -1;
		boolean hasTitle = opt != null && opt.indexOf("t") > -1;
		boolean removeBlank = opt != null && opt.indexOf("b") > -1;
		if (isCursor && removeBlank) {
			throw new RQException(AppMessage.get().getMessage("xlsimport.nocb"));
		}

		boolean isW = opt != null && opt.indexOf("w") > -1;
		boolean isS = opt != null && opt.indexOf("s") > -1;
		boolean isP = opt != null && opt.indexOf("p") > -1;
		String wOrSText = isW ? "w" : "s";
		if (isW || isS) {
			if (hasTitle || removeBlank || isCursor) {
				throw new RQException(AppMessage.get().getMessage(
						"xlsimport.nowtbc", wOrSText));
			}
		}
		if (!isW) {
			if (isP) {
				// ѡ��@{0}ֻ�ܺ�ѡ��@wͬʱʹ�á�
				throw new RQException(AppMessage.get().getMessage(
						"xlsimport.pnnotw", "p"));
			}
		}
	}

	public static void checkFieldOptions(String[] fields, String opt) {
		boolean isW = opt != null && opt.indexOf("w") > -1;
		boolean isS = opt != null && opt.indexOf("s") > -1;
		String wOrSText = isW ? "w" : "s";
		if (isW || isS) {
			if (fields != null) {
				throw new RQException(AppMessage.get().getMessage(
						"xlsimport.nowfields", wOrSText));
			}
		}
	}
}
