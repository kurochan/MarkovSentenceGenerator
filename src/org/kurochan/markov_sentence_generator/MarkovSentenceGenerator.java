package org.kurochan.markov_sentence_generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.atilika.kuromoji.Token;
import org.atilika.kuromoji.Tokenizer;

/**
 * マルコフ連鎖を用いて、文章を生成します。
 * 
 * @author kurochan
 * @version 1.0
 */
public class MarkovSentenceGenerator {
	/**
	 * 終端文字
	 */
	private static final String TERMINATOR = "¥";
	/**
	 * 文章生成時に最大何個のトークンを接続するか
	 */
	private static final int MAX_WORD_COUNT = 30;
	/**
	 * トークンの組のリスト
	 */
	public final List<Token[]> stringData;
	/**
	 * マルコフ連鎖の階数
	 */
	private final int rank;
	/**
	 * トークナイザ
	 */
	private Tokenizer tokenizer;

	/**
	 * @param rank
	 *            マルコフ連鎖の階数
	 */
	public MarkovSentenceGenerator(int rank) {
		this.rank = rank;
		this.stringData = new ArrayList<Token[]>();
		tokenizer = Tokenizer.builder().build();
	}

	/**
	 * 生成する文章の元となる文を追加する
	 * 
	 * @param sentence
	 *            文章
	 */
	public void addSentence(String sentence) {
		List<Token> tokens = tokenizer.tokenize(sentence + TERMINATOR);
		trimTokens(tokens);
		for (int i = 0; i < tokens.size() - rank; i++) {
			Token data[] = new Token[rank + 1];
			for (int j = 0; j < data.length; j++) {
				data[j] = tokens.get(i + j);
			}
			stringData.add(data);
		}
	}

	/**
	 * 空白等の不要なトークンのを除去します。
	 * 
	 * @param tokens
	 *            不要なトークンが含まれいているトークンのリスト
	 */
	private void trimTokens(List<Token> tokens) {
		Iterator<Token> itr = tokens.iterator();
		while (itr.hasNext()) {
			Token t = (Token) itr.next();
			if (t.getSurfaceForm().contains(" ")
					|| t.getSurfaceForm().contains("　")) {
				itr.remove();
			}
		}
	}

	/**
	 * 名詞を先頭に含んだトークンの組のうち、一番最初に見つけたものを返します。
	 * 
	 * @param data
	 *            トークンの組のリスト
	 * @return 名詞を先頭に含んだトークンの組
	 */
	private Token[] findHead(List<Token[]> data) {
		Token[] head = null;
		for (Token[] tokens : data) {
			if (tokens[0].getPartOfSpeech().contains("名詞")) {
				head = tokens;
				break;
			}
		}
		return head;
	}

	/**
	 * 2つのトークンの組が接続可能かどうか調べます。
	 * 
	 * @param before
	 *            前のトークン
	 * @param after
	 *            後のトークン
	 * @return 接続可能かどうか
	 */
	private boolean isConnectable(Token[] before, Token[] after) {
		for (int i = 0; i < rank; i++) {
			if (!before[i + 1].getSurfaceForm().equals(
					after[i].getSurfaceForm())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 渡されたトークンの組が文章の終了を表しているかどうかを調べます。
	 * 
	 * @param tokens
	 * @return 終了を表しているかどうか
	 */
	private boolean isEnd(Token[] tokens) {
		return (tokens[rank].getSurfaceForm().equals(".")
				|| tokens[rank].getSurfaceForm().equals("。") || tokens[rank]
				.getSurfaceForm().contains(TERMINATOR));
	}

	/**
	 * 渡されたトークンの組に続いてつながるトークンの組を検索します。
	 * 
	 * @param last
	 *            検索したいトークンの組
	 * @param data
	 *            トークンの組のリスト
	 * @return 見つかったトークンの組
	 */
	private Token[] findNext(Token[] last, List<Token[]> data) {
		for (Token[] tokens : data) {
			if (isConnectable(last, tokens)) {
				return tokens;
			}
		}
		return null;
	}

	/**
	 * マルコフ連鎖を用いた文章を生成します。
	 * 
	 * @return 生成された文章
	 */
	public String generateSentence() {
		Collections.shuffle(stringData);
		List<Token[]> tokensList = new ArrayList<Token[]>();

		Token[] head = findHead(stringData);
		if (head == null) {
			return null;
		}
		tokensList.add(head);

		for (int i = 0; i < MAX_WORD_COUNT; i++) {
			Token[] last = tokensList.get(tokensList.size() - 1);
			if (isEnd(last)) {
				break;
			}
			Collections.shuffle(stringData);
			Token[] next = findNext(last, stringData);
			if (next == null) {
				break;
			}
			tokensList.add(next);
		}
		return mergeTokensList(tokensList);
	}

	/**
	 * トークンの組のリストを結合し、文字列に変換します。
	 * 
	 * @param data
	 *            トークンの組のリスト
	 * @return 結合された文字列
	 */
	private String mergeTokensList(List<Token[]> data) {
		if (data == null || data.size() < 1) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		Token tokens[] = data.get(0);
		for (int i = 0; i <= rank; i++) {
			if (!tokens[i].getSurfaceForm().contains(TERMINATOR)) {
				sb.append(tokens[i].getSurfaceForm());
			}
		}
		for (int i = 1; i < data.size(); i++) {
			tokens = data.get(i);
			if (!tokens[rank].getSurfaceForm().contains(TERMINATOR)) {
				sb.append(tokens[rank].getSurfaceForm());
			}
		}
		return sb.toString();
	}

	/**
	 * 入力されたテキストファイルの内容からマルコフ連鎖を用いて新たな文章を生成します。
	 * 
	 * @param args
	 *            先頭に入力ファイル名、その次にマルコフ連鎖の階数
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("usage: [input text file] [rank]");
			return;
		}

		int rank = Integer.valueOf(args[1]);
		MarkovSentenceGenerator generator = new MarkovSentenceGenerator(rank);

		File inputFile = new File(args[0]);
		try {
			FileReader fr = new FileReader(inputFile);
			@SuppressWarnings("resource")
			BufferedReader br = new BufferedReader(fr);
			String str;
			while ((str = br.readLine()) != null) {
				generator.addSentence(str);
			}
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
			return;
		} catch (IOException e) {
			System.err.println(e.getMessage());
			return;
		}

		String sentence;
		for (int i = 0; i < 10; i++) {
			sentence = generator.generateSentence();
			System.out.println(sentence);
		}
	}
}