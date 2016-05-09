package topper;

import java.util.Scanner;

public class Main {

	public Main() {}

	public static void main(String[] args) {

		Scanner scanner = new Scanner(System.in);

//		Indexer i = new Indexer();
//		i.index();

		System.out.println("Please enter your search query:");
		String q = scanner.nextLine();

		Search s = new Search();
		s.search(q);
	}
}
