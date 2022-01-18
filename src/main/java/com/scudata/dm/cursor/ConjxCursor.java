package com.scudata.dm.cursor;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;

/**
 * �Ѷ���α���������һ���α꣬ȡ��ʱ�����α���ÿ���αֱ꣬�����һ���α�ȡ������
 * �ṹ�Ե�һ���α�Ϊ׼
 * @author 
 *
 */
public class ConjxCursor extends ICursor {
	private ICursor []cursors; // �α�����
	private int curIndex = 0; // ��ǰ���ڶ������α�����

	/**
	 * �������������α����
	 * @param cursors
	 */
	public ConjxCursor(ICursor []cursors) {
		this.cursors = cursors;
		setDataStruct(cursors[0].getDataStruct());
	}
	
	// ���м���ʱ��Ҫ�ı�������
	// �̳�������õ��˱���ʽ����Ҫ�������������½�������ʽ
	protected void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			for (ICursor cursor : cursors) {
				cursor.resetContext(ctx);
			}
			
			super.resetContext(ctx);
		}
	}

	/**
	 * ��ȡָ�����������ݷ���
	 * @param n ����
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		if (cursors.length == curIndex || n < 1) return null;
		Sequence table = cursors[curIndex].fetch(n);
		if (table == null || table.length() < n) {
			curIndex++;
			if (curIndex < cursors.length) {
				if (table == null) {
					return get(n);
				} else {
					Sequence rest;
					if (n == MAXSIZE) {
						rest = get(n);
					} else {
						rest = get(n - table.length());
					}
					
					table = append(table, rest);
				}
			}
		}

		return table;
	}

	/**
	 * ����ָ������������
	 * @param n ����
	 * @return long ʵ������������
	 */
	protected long skipOver(long n) {
		if (cursors.length == curIndex || n < 1) return 0;

		long count = cursors[curIndex].skip(n);
		if (count < n) {
			curIndex++;
			if (curIndex < cursors.length) {
				count += skipOver(n - count);
			}
		}

		return count;
	}

	/**
	 * �ر��α�
	 */
	public synchronized void close() {
		super.close();
		
		for (int i = 0, count = cursors.length; i < count; ++i) {
			cursors[i].close();
		}
	}
	
	/**
	 * �����α�
	 * @return �����Ƿ�ɹ���true���α���Դ�ͷ����ȡ����false�������Դ�ͷ����ȡ��
	 */
	public boolean reset() {
		curIndex = 0;
		for (int i = 0, count = cursors.length; i < count; ++i) {
			if (!cursors[i].reset()) {
				return false;
			}
		}
		
		return true;
	}
}