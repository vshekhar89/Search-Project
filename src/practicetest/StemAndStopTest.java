/**
 * 
 */
package practicetest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import util.LuceneNLPUtil;

/**
 * @author sumit
 *
 */
public class StemAndStopTest {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		List<String> str = new ArrayList<>();
		str.add("_hrcjz_uxdumbhdxcrug");
		str.add("_lone_mt.htm");
		str.add("_mwlxlwjcu_o_fz");
		str.add("a'c's");
		str.add("a.akamaihd.net");
		str.add("a.t.c's");
		str.add("a___erto's");
		str.add("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaawssomeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
		str.add("worst.wedding.reception.ever");
		str.add("àu");
		str.add("üblere");
		str.add("ò");
		str.add("ñ_ñ");
		str.add("a'coming");
		for (String x : str) {
			System.out.println(LuceneNLPUtil.getRemovedStopAndStem(x,
					LuceneNLPUtil.getDefaultEnglishStopWordList()));
		}

		System.out.println("_et".matches("^[^a-zA-Z0-9].*"));
		System.out.println("__et".trim().replaceAll("\\p{ASCII}*",""));
		
		System.out.println(isCorrect("a.m.a.z.i.n.g"));
	}

	private static boolean isCorrect(String word) {
		System.out.println(isEnglish(word)+"::"+word.matches("^[^a-zA-Z0-9].*"));
		return (isEnglish(word)|| word.trim().length() <= 1 ) && word.matches("^[^\\w\\d].*");
	}

	private static boolean isEnglish(String word) {
		// TODO Auto-generated method stub
		return word.trim().replaceAll("\\p{ASCII}*","").length()==0;
	}

}
