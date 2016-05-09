package topper;

public class Page {

	private String title;
	private int id = 0;
	private String text;
	private int count;
	private boolean redirect = false;

	public Page() {}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setId(int id) {
		if (this.id == 0) // Prevents revision id being set as article id.
			this.id = id;
	}

	public void setText(String body) {
		this.text = body;
	}

	public void setCount(int count) {
		this.count = count;
	}

    public void setRedirect(boolean redirect) {
        this.redirect = redirect;
    }

	public String getTitle() {
		return this.title;
	}

	public int getId() {
		return id;
	}

	public String getText() {
		return this.text;
	}

	public int getCount() {
		return this.count;
	}

    public boolean isRedirect() {
        return this.redirect;
    }

}
