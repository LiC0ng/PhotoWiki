package jp.ac.titech.sdl.photowiki.db;

import org.litepal.crud.DataSupport;

public class Root extends DataSupport {

    private String batchcomplete;
    private Query  query;

    public String getBatchcomplete() {
        return batchcomplete;
    }

    public void setBatchcomplete(String batchcomplete) {
        this.batchcomplete = batchcomplete;
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }
}
