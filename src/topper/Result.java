package topper;

import java.util.Comparator;
import java.util.HashMap;

public class Result {

	private int id;
	private String title;
	private int count;
	private float rank;
	private HashMap<String, Integer> words;

	public Result(int id) {
		this.id = id;
		words = new HashMap<String, Integer>();
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setRank(float rank) {
		this.rank = rank;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public void setWords(HashMap<String, Integer> words) {
		this.words = words;
	}

	public int getId() {
		return id;
	}

	public String getTitle() {
		return this.title;
	}

	public int getCount() {
		return this.count;
	}

	public float getRank() {
		return rank;
	}

	public HashMap<String, Integer> getWords() {
		return words;
	}

	public static Comparator<Result> comparator = new Comparator<Result>() {
		public int compare(Result r1, Result r2) {
			return Float.compare(r2.getRank(), r1.getRank());
		}
	};

	@Override
	public String toString() {
		return " title: " + this.getTitle() + " rank: " + this.getRank() + " words: " + this.getWords();
	}
}
