import java.sql.SQLException;
public class TagRead {
    private String tagid;
    private String tagvendor;
    private String channelid;
    private String channelvendor;
    private String timestamp;
    private double ssi;
    private double distance;
    public TagRead(String tagid, String tagvendor, String channelid, String channelvendor, String timestamp, double ssi, DBConnector jdbc) throws SQLException, DBConnector.ChannelNotFoundException {
        this.tagid = tagid;
        this.tagvendor = tagvendor;
        this.channelid = channelid;
        this.channelvendor = channelvendor;
        this.timestamp = timestamp;
        this.ssi = ssi;
        this.distance = calculateDistance(this, jdbc);
    }
    
    public String getTagId() {
        return tagid;
    }
    
    public String getTagVendor() {
        return tagvendor;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public String getChannelId() {
        return channelid;
    }
    
    public String getChannelVendor() {
        return channelvendor;
    }
    
    public double getSsi() {
        return ssi;
    }
    
    public double getDistance() {
        return distance;
    }
    
    public double calculateDistance(TagRead tr, DBConnector jdbc) throws SQLException, DBConnector.ChannelNotFoundException {
        double rssi = tr.getSsi();
        if((tr.getTagVendor()).equals("rfcode")){
            if (jdbc.getReaderType(tr.getChannelId(), tr.getChannelVendor()).equals("M250")){
                return Math.exp((rssi+53.519)/(-11.71));
                //return Math.exp((rssi+67.114)/(-10.76));
            } else if(jdbc.getReaderType(tr.getChannelId(), tr.getChannelVendor()).equals("M200")){
                if(rssi >= -100) {
                    return Math.exp((rssi+65.589)/(-11.386));
                } else {
                    return (rssi+91.056)/(-0.4193);
                }
                //return Math.exp((rssi+67.114)/(-10.76));
            }
        } else if((tr.getTagVendor()).equals("thingmagic")){
            return (Math.log(((rssi + 80.14)/(35.04)))/-.38)*39.3701;
        }
        return 0;
    }
    
    public String toString() {
        return "TagRead{" +
            "tagvendor=" + tagvendor +
            ", tagid=" + tagid +
            ", channel=" + channelid +
            ", ssi=" + ssi +
            ", Timestamp=" + timestamp +
        '}';
    }
}