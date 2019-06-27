package jp.ac.titech.sdl.photowiki.db;

import org.litepal.crud.DataSupport;

import java.util.Map;

public class Query extends DataSupport {

    public Map<String, Page> getPages() {
        return pages;
    }

    public void setPages(Map<String, Page> pages) {
        this.pages = pages;
    }

    private Map<String, Page> pages;
}
