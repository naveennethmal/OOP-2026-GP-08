public class Notice {
    private final int noticeId;
    private final String title;
    private final String publishedDate;
    private final String description;

    public Notice(int noticeId, String title, String publishedDate, String description) {
        this.noticeId = noticeId;
        this.title = title;
        this.publishedDate = publishedDate;
        this.description = description;
    }

    public int getNoticeId() { return noticeId; }
    public String getTitle() { return title; }
    public String getPublishedDate() { return publishedDate; }
    public String getDescription() { return description; }
}