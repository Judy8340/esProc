package com.scudata.dm.sql;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.scudata.common.Logger;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.sql.simple.IFunction;
import com.scudata.expression.fn.Eval;
import com.scudata.util.Variant;

//������Ϣ������(������Ϣ����ʱ�����Ƽ�������������)
//***��δ�������ݿⲻͬ�汾�еĺ�������
//***�����������̶���ֻ��Ԥ�ȶ����
public class FunInfoManager {
	public static final int COVER = 0; // ����

	public static final int SKIP = 1; // ����

	public static final int ERROR = 2; // ����

	private static TreeMap<FunInfo, FunInfo> funMap = new TreeMap<FunInfo, FunInfo>(); // [FunInfo-FunInfo]


	//<dbtype<name<paramcount:value>>> ��������-1��ʾ���̶�����������-2���ʽ��SPL�ű��Ĳ��̶���������
	public static Map<String, Map<String, Map<Integer, String>>> dbMap = new HashMap<String,Map<String, Map<Integer, String>>>();

	static { // �Զ����غ����ļ�
		try {
			InputStream in = FunInfoManager.class.getResourceAsStream("/com/scudata/dm/sql/function.xml");
			addFrom(in, COVER);
		} catch (Exception e) {
			Logger.debug(e);
		}
	}
	
	private static void addInfo(String dbtype, String name, int paramcount, String value) {
		name = name.toLowerCase();
		dbtype = dbtype.toUpperCase();
		Map<String, Map<Integer, String>> db = dbMap.get(dbtype);
		if (db == null) {
			db = new HashMap<String, Map<Integer, String>>();
			dbMap.put(dbtype, db);
		}
		
		Map<Integer, String> fn = db.get(name);
		if (fn == null) {
			fn = new HashMap<Integer, String>();
			db.put(name, fn);
		}
		
		fn.put(paramcount, value);	
	}

	public static FixedParamFunInfo getFixedParamFunInfo(String name, int pcount) {
		FunInfo key = new FunInfo(name, pcount);
		return (FixedParamFunInfo)funMap.get(key);
	}
	
	// ���ݱ�׼��������������������Һ�����Ϣ�����������޾�ȷƥ��ʱ��ƥ��-1
	public static FunInfo getFunInfo(String name, int pcount) {
		FunInfo key = new FunInfo(name, pcount);
		FunInfo val = funMap.get(key);

		if (val != null) {
			return val;
		} else {
			key.setParamCount(-1);
			return funMap.get(key);
		}
	}

	// ���ú�����Ϣ����֤��������+����������Ψһ
	public  static void setFunInfo(FunInfo fi) {
		funMap.put(fi, fi);
	}

	// ��xml�ļ������Ӻ�����Ϣ��sameModeָ����ͬһ����ʱ�Ĵ���ʽ
	private static void addFrom(InputStream in, int sameMode) {
		if (in == null) {
			return;
		}
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder;
		Document xmlDocument;
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			xmlDocument = docBuilder.parse(in);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RQException("Invalid input stream.");
		}
		NodeList nl = xmlDocument.getChildNodes();
		if (nl == null) {
			return;
		}
		Node funInfo = null;
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n.getNodeName().equalsIgnoreCase(ROOT)) {
				funInfo = n;
			}
		}
		if (funInfo == null) {
			return;
		}
		nl = funInfo.getChildNodes();
		boolean isFix;
		for (int i = 0; i < nl.getLength(); i++) {
			Node root = nl.item(i);
			if (root.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			if (!root.getNodeName().equalsIgnoreCase(NODE_FUNCTIONS)) {
				continue;
			}
			String value = getAttribute(root, KEY_TYPE);
			if (value == null) {
				continue;
			}
			isFix = value.equals(TYPE_FIX);
			NodeList funcNodes = root.getChildNodes();
			for (int j = 0; j < funcNodes.getLength(); j++) {
				Node funcNode = funcNodes.item(j);
				if (funcNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				//FunInfo fi;
				if (isFix) {
					String name = getAttribute(funcNode, KEY_NAME);
					String sParamCount = getAttribute(funcNode, KEY_PARAM_COUNT);
					String defValue = getAttribute(funcNode, KEY_VALUE);
					int paramCount;
					try {
						paramCount = Integer.parseInt(sParamCount);
					} catch (Exception ex) {
						throw new RQException("Invalid param count:"
								+ sParamCount);
					}

					NodeList infos = funcNode.getChildNodes();
					for (int u = 0; u < infos.getLength(); u++) {
						Node info = infos.item(u);
						if (info.getNodeType() != Node.ELEMENT_NODE) {
							continue;
						}
						String sDbType = getAttribute(info, KEY_DB_TYPE);
						String sValue = getAttribute(info, KEY_VALUE);
						if (sValue == null || sValue.trim().length() == 0) sValue = defValue;
						if (sDbType==null || sDbType.trim().length()==0) sDbType = "ESPROC";
						addInfo(sDbType, name, paramCount, sValue);
					}
				} else {
					String name = getAttribute(funcNode, KEY_NAME);
					String defValue = getAttribute(funcNode, KEY_CLASS_NAME);
					int paramCount = -1;
					
					NodeList infos = funcNode.getChildNodes();
					for (int u = 0; u < infos.getLength(); u++) {
						Node info = infos.item(u);
						if (info.getNodeType() != Node.ELEMENT_NODE) {
							continue;
						}
						String sDbType = getAttribute(info, KEY_DB_TYPE);
						String sValue = getAttribute(info, KEY_CLASS_NAME);
						
						if (sDbType==null || sDbType.trim().length()==0) sDbType = "ESPROC";
						
						if (sValue == null || sValue.trim().length() == 0) {
							sValue = getAttribute(info, KEY_SCRIPT);
							if (sValue != null && sValue.length() > 0) {
								addInfo(sDbType, name, -2, sValue);
							} else {
								addInfo(sDbType, name, paramCount, defValue);
							}
						} else {
							addInfo(sDbType, name, paramCount, sValue);
						}
						
					}
				}
				
			}
		}
	}

	public static final String ROOT = "STANDARD";

	public static final String NODE_FUNCTIONS = "FUNCTIONS";

	public static final String NODE_FUNCTION = "FUNCTION";

	public static final String KEY_TYPE = "type";

	public static final String TYPE_FIX = "FixParam";

	public static final String TYPE_ANY = "AnyParam";

	public static final String KEY_NAME = "name";

	public static final String KEY_PARAM_COUNT = "paramcount";

	public static final String NODE_INFO = "INFO";

	public static final String KEY_DB_TYPE = "dbtype";

	public static final String KEY_VALUE = "value";

	public static final String KEY_CLASS_NAME = "classname";
	
	public static final String KEY_SCRIPT = "script";

	private static String getAttribute(Node node, String attrName) {
		NamedNodeMap attrs = node.getAttributes();
		if (attrs == null) {
			return null;
		}
		int i = attrs.getLength();
		for (int j = 0; j < i; j++) {
			Node tmp = attrs.item(j);
			String sTmp = tmp.getNodeName();
			if (sTmp.equalsIgnoreCase(attrName)) {
				return tmp.getNodeValue();
			}
		}
		return null;
	}

	// ���������Ϣ
	public static void clear() {
		funMap.clear();
	}

	public static Collection<FunInfo> getAllFunInfo() {
		return funMap.values();
	}
	
	/**
	 * �ж��Ƿ����ָ�����Ƶĺ���
	 * @param dbtype ���ݿ�����
	 * @param name ������
	 * @return
	 */
	public static boolean isFunction(String dbtype, String name) {
		Map<String, Map<Integer, String>> db = dbMap.get(dbtype.toUpperCase());
		if (db == null) {
			return false;
		} else {
			return db.get(name.toLowerCase()) != null;
		}
	}
	
	public static String getFunctionExp(String dbtype, String name, String[] params) {
		//String dbname = DBTypes.getDBTypeName(dbtype);
		Map<String, Map<Integer, String>> thisdb = dbMap.get(dbtype.toUpperCase());
		if (thisdb == null) {
			throw new RQException("unknown database : "+dbtype);
		}
		
		Map<Integer, String> typeFunctionMap = thisdb.get(name.toLowerCase());
		if(typeFunctionMap == null) {
			return null;
		}
		
		int count = params.length;
		String formula = typeFunctionMap.get(count);
		if(formula != null) {
			if(formula.isEmpty()) {
				StringBuffer sb = new StringBuffer();
				sb.append(name);
				sb.append("(");
				for(int i = 0; i < params.length; i++)
				{
					sb.append("?"+(i+1));
					if(i > 0)
					{
						sb.append(",");
					}
				}
				sb.append(")");
				formula = sb.toString();
			} else if(formula.equalsIgnoreCase("N/A")) {
				throw new RQException("�˺���ϵͳ�ݲ�֧��:"+name);
			} else if(count == 1) {
				formula = formula.replace("?1", "?");
				formula = formula.replace("?", "?1");
			}
			
			for(int i = 0; i < params.length; i++) {
				formula = formula.replace("?"+(i+1), params[i]);
			}
			
			return formula;
		} else {
			String className = typeFunctionMap.get(-1);
			if(className != null) {
				try {
					IFunction functionClass = (IFunction)Class.forName(className).newInstance();
					return functionClass.getFormula(params);
				} catch (Exception e) {
					throw new RQException("���طǹ̶����������ĺ������Զ�����ʱ���ִ���", e);
				}
			} else {
				className = typeFunctionMap.get(-2);
				if(className != null) {
					Sequence seq = new Sequence(1);
					seq.add(new Sequence(params));
					Context ctx = new Context();
					Object result = Eval.calc(className, seq, null, ctx);
					return Variant.toString(result);
				}
			}
		}
		
		//throw new RQException("δ֪�ĺ�������, ����:"+name+", ��������:"+params.length);
		return null;
	}

	
	public static void main (String args[]) {
		System.out.println(FunInfoManager.dbMap.size());
	}

}
