import java.util.ArrayList;
public class Tag {
    private String tagid;
    private String tagvendor;
    private String time;
    private ArrayList<TagRead> tagreads;
    public Tag(String tagid, String tagvendor, String time){
        this.tagid = tagid;
        this.tagvendor = tagvendor;
        this.tagreads = new ArrayList<TagRead>();
        this.time = time;
    }
    public ArrayList<TagRead> getTagReads(){ return tagreads; }
    public String getTagId(){ return tagid; }
    public String getTime(){ return time; }
    public String getTagVendor() { return tagvendor; }
    public String toString(){
        return "Tag{" +
            "tagvendor=" + tagvendor +
            ", tagid=" + tagid +
            ", tagreads=" + tagreads.size() +
            ", time=" + time +
        '}';
    }
}