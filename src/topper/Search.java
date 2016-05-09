package topper;

import com.mongodb.*;
import org.tartarus.snowball.ext.englishStemmer;

import java.io.File;
import java.util.*;

import static java.util.Arrays.asList;

public class Search {

	private static String DBSERVER = "localhost";
	private static String DBNAME = "Topper";

	public void search(String q) {
		long start = System.nanoTime();

		ArrayList<String> terms = normalize(q);
		ArrayList<Result> results;
		int mc = getOccurrenceCount();

		if (mc <= 0) {
			System.out.println("No index could be found, please ensure an index exists.");
			return;
		}

		HashMap<String, Integer> termCounts = getTermCounts(terms);
		results = getMatches(terms);
		results = getPageDetails(results);

		System.out.println("The query was: '" + q + "'");
		System.out.println("The top 10 pages with the highest ranking for the query where(in descending order): ");
		rank(results, termCounts, mc).forEach(System.out::println);

		System.out.printf("The query was processed in: %.3f s %n", (System.nanoTime() - start) / 1000000000.0);
	}

	// Rank documents by the probability that they will produce the answer to the query.
	// Using Querylikelihood model
	public ArrayList<Result> rank(ArrayList<Result> results, HashMap<String, Integer> termsCounts, int mc) {

		float Mc = (float) mc;
		float ans;
		float lam = 0.5f;
		float v;
		for(Result r : results) {
			ans = 1;
			for(String t : termsCounts.keySet()) {
				v = (float) ((r.getWords().containsKey(t)) ? r.getWords().get(t) : 0);
				ans = ans * ( ((((float)termsCounts.get(t)) /Mc) * lam) + ( (v / ((float)r.getCount()) ) * lam) );
			}
			r.setRank(ans);
		}

		results.sort(Result.comparator);
		return new ArrayList<Result>(results.subList(0, 10));
	}

	// get all documents having at least one of these terms in it's text.
	public ArrayList<Result> getMatches(ArrayList<String> terms) {
		HashMap<Integer, Result> pages = new HashMap<>();

		try {
			DB db = new MongoClient(DBSERVER).getDB(DBNAME);
			DBCollection coli = db.getCollection("invindex");

			// Find all occurrences of terms
			DBCursor c = coli.find(new BasicDBObject("word", new BasicDBObject("$in", terms)));

			int id;
			int count;
			String w;
			DBObject o;
			DBObject a;
			BasicDBList l;

			while(c.hasNext()) {
				o = c.next();
				w = (String) o.get("word");
				l = (BasicDBList) o.get("occurrences");

				for (Object i : l) {
					a = (DBObject) i;
					id = (Integer) a.get("id");
					count = (Integer) a.get("count");
					pages.putIfAbsent(id, new Result(id));
					pages.get(id).getWords().put(w, count);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<Result> (pages.values());
	}

	// Get the global word count as mc
	public int getOccurrenceCount() {
		int mc = -1;

		try {
			DB db = new MongoClient(DBSERVER).getDB(DBNAME);

			AggregationOutput o = db.getCollection("pages").aggregate(asList(
					new BasicDBObject("$group", new BasicDBObject("_id", "").append("total", new BasicDBObject("$sum", "$count")))
			));
			mc = (int) o.results().iterator().next().get("total");
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return mc;
	}

	// Get the page details: title and word count
	public ArrayList<Result> getPageDetails(ArrayList<Result> results) {
		try {
			DB db = new MongoClient(DBSERVER).getDB(DBNAME);
			DBCollection colp = db.getCollection("pages");
			DBCursor c;
			DBObject o;
			for(Result r : results) {
				c = colp.find(new BasicDBObject("id", r.getId()));
				o = c.next();
				r.setTitle((String) o.get("title"));
				r.setCount((int) o.get("count"));
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return results;
	}

	// Get the total occurrences of all the terms
	public HashMap<String, Integer> getTermCounts(ArrayList<String> terms) {
		HashMap<String, Integer> termCount = new HashMap<>();

		try{
			DB db = new MongoClient(DBSERVER).getDB(DBNAME);
			int total;

			for(String t : terms) {
				total = (int) db.getCollection("invindex").aggregate(asList(
						new BasicDBObject("$match", new BasicDBObject("word", t)),
						new BasicDBObject("$unwind", "$occurrences"),
						new BasicDBObject("$group", new BasicDBObject("_id", "").append("total", new BasicDBObject("$sum", "$occurrences.count")))
				)).results().iterator().next().get("total");
				termCount.put(t, total);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return termCount;
	}

	public ArrayList<String> normalize(String q) {
		q += " "; // Add an extra space to terminate the query.

		englishStemmer stemmer = new englishStemmer();
		HashSet<String> stopWords = getStopWords();
		ArrayList<String> normQ = new ArrayList<>();

		StringBuilder sb = new StringBuilder();
		boolean ignore = false;

		for (char c : q.toCharArray()) {

			if (c != 39) {
				if (c > 47 && c < 58 || c > 96 && c < 123) // Character c is a number 0-9 or a lower case letter a-z.
					sb.append(c);

				else if (c > 64 && c < 91) // Character c is an uppercase letter A-Z.
					sb.append(Character.toLowerCase(c));

				else if (sb.length() > 0) { // Check if there is a word up until now.

					if (sb.length() > 1) { // Ignore single character "words"

						if (!stopWords.contains(sb.toString())) { // Check if the word is not a stop word.

							stemmer.setCurrent(sb.toString());
							stemmer.stem(); // Stem word s

							String s = sb.toString(); // Retrieve the stemmed word

							if (!normQ.contains(s)) // Check if the normalized query list already contains this term
								normQ.add(s); // If not then add it to the normalized query list
						}
					}
					sb = new StringBuilder();
				}
			}
		}
		return normQ;
	}

	public HashSet<String> getStopWords() {

		HashSet<String> stopWords = new HashSet<>();

		try {
			// Stop words to be removed from indexing
			Scanner txt1 = new Scanner(new File("StopWords/list4.txt"));

			while (txt1.hasNext())
				stopWords.add(txt1.nextLine());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return stopWords;
	}
}
