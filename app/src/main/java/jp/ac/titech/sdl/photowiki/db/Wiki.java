package jp.ac.titech.sdl.photowiki.db;

import org.litepal.crud.DataSupport;

public class Wiki extends DataSupport {

    private int pageId;

    private String content;

    private byte[] image;

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }
}
