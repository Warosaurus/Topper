package topper;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.mongodb.*;
import org.tartarus.snowball.ext.englishStemmer;

public class Indexer {

	private static String DBSERVER = "localhost";
	private static String DBNAME = "Topper";

	public Indexer() {
	}

	public void index() {
		ArrayList<Page> pages = parse(); // Parse XML pages

		try {
			DB db = new MongoClient(DBSERVER).getDB(DBNAME);

			DBCollection coli = db.getCollection("invindex");
			DBCollection colp = db.getCollection("pages");

			for (Page page : pages) {

				HashMap<String, AtomicInteger> posts = getPostings(page);

				if (!page.isRedirect()) {

					colp.insert(
							new BasicDBObject("id", page.getId())
									.append("title", page.getTitle())
									.append("count", page.getCount())
					);

					for (Map.Entry e : posts.entrySet()) {
						coli.update(
								// The word in the database is stored in the form "word" : [ { docID, wordCount }, ... ]
								new BasicDBObject("word", e.getKey()),
								// Add/push the elements of the array into the occurrences array.
								new BasicDBObject("$push", new BasicDBObject("occurrences", new BasicDBObject("id", page.getId()).append("count", ((AtomicInteger) e.getValue()).get()))),
								true,    // Upsert true, meaning update if key exists or insert.
								false
						);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public HashMap<String, AtomicInteger> getPostings(Page page) {

		englishStemmer stemmer = new englishStemmer();
		HashSet<String> stopWords = getStopWords();
		HashMap<String, AtomicInteger> postings = new HashMap<>();
		int count = 0;

		StringBuilder sb = new StringBuilder();
		boolean ignore = false;

		for (char c : page.getText().toCharArray()) {

			if (c == '<')
				ignore = true;

			if (!ignore) {
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

								if (!postings.containsKey(s)) // Check if the word already exists in the words map.
									postings.put(s, new AtomicInteger(0)); // If the word is not in the map then create an array list for that word
								postings.get(s).incrementAndGet(); // Place the location of the word into the array list for that word
								count++; // Increase the overall word count for the pages
							}
						}
						sb = new StringBuilder();
					}
				}
			}

			if (c == '>')
				ignore = false;
		}
		page.setCount(count);
		return postings;
	}

	// Stop words to be removed from indexing
	public HashSet<String> getStopWords() {

		HashSet<String> stopWords = new HashSet<>();

		try {
			Scanner txt1 = new Scanner(new File("StopWords/list4.txt"));

			while (txt1.hasNext())
				stopWords.add(txt1.nextLine());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return stopWords;
	}

	// Retrieve all pages from XML file
	public ArrayList<Page> parse() {

		File dataFolder = new File("Data/");
		File[] docList = dataFolder.listFiles();

		assert docList != null;
		File file = docList[0];

		XMLInputFactory xmlinf = XMLInputFactory.newInstance();
		ArrayList<Page> pages = new ArrayList<>();

		try {
			InputStream in = new FileInputStream(file);
			XMLStreamReader reader = xmlinf.createXMLStreamReader(in);

			// Ensure the pointer is at the document start.
			assert (reader.getEventType() == XMLEvent.START_DOCUMENT);

			Page page = null;

			while (reader.hasNext()) {
				switch (reader.next()) {

					case XMLEvent.START_DOCUMENT:
						break;
					case XMLEvent.END_DOCUMENT:
						break;

					case XMLEvent.START_ELEMENT:
						if (reader.getLocalName().equals("page"))
							page = new Page();

						else if (reader.getLocalName().equals("title")) {
							assert page != null;
							page.setTitle(reader.getElementText());
						} else if (reader.getLocalName().equals("id")) {
							assert page != null;
							page.setId(Integer.parseInt(reader.getElementText()));
						} else if (reader.getLocalName().equals("text")) {
							assert page != null;
							page.setText(reader.getElementText() + " ");
						} else if (reader.getLocalName().startsWith("redirect")) {
							assert page != null;
							page.setRedirect(true);
						}

						break;

					case XMLEvent.END_ELEMENT:
						if (reader.getLocalName().equals("page")) {
							assert page != null;
							pages.add(page);
						}

						break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return pages;
	}
}
