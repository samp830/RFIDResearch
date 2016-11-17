import java.sql.*;
import java.util.HashMap;
import java.util.ArrayList;

public class DBConnector {
    private Connection jdbc;
    public DBConnector(String hostname, String port, String database, String user, String pass) throws SQLException, ClassNotFoundException{
        Class.forName("org.mariadb.jdbc.Driver");
        this.jdbc = DriverManager.getConnection("jdbc:mysql://"+hostname+":"+port+"/"+database,user,pass);
    }
    public void close() throws Exception{
        jdbc.close();
    }
    public Connection getConnection() {
        return jdbc;
    }
    public ArrayList<TagRead> getSsiData() throws SQLException, ChannelNotFoundException {
        //PreparedStatement setTime = jdbc.prepareStatement("SET @t=UNIX_TIMESTAMP()");
        //PreparedStatement getLatestReads = jdbc.prepareStatement("SELECT * FROM tagreads WHERE abs(UNIX_TIMESTAMP(readtime)-@t)=(n" +
        //                                                         "    SELECT MIN(abs(@t-UNIX_TIMESTAMP(readtime))) FROM tagreadsn" +
        //                                                         ");");
        //PreparedStatement getLatestReads = jdbc.prepareStatement("SELECT t.* FROM tagreads t WHERE t.readtime=(SELECT MAX(readtime) FROM tagreads) ORDER BY tagid ASC, readtime DESC, channelid ASC");
        PreparedStatement getLatestReads = jdbc.prepareStatement("SELECT a.* FROM tagreads a LEFT JOIN locations b ON a.tagid=b.tagid AND a.tagvendor=b.tagvendor AND a.readtime=b.readtime WHERE b.tagid IS NULL AND b.tagvendor IS NULL AND b.readtime IS NULL ORDER BY tagid ASC, readtime DESC, channelid ASC");
        //PreparedStatement getLatestReads = jdbc.prepareStatement("SELECT * FROM tagreads WHERE UNIX_TIMESTAMP(readtime) BETWEEN UNIX_TIMESTAMP(\"2015-08-04 13:38:00\") AND UNIX_TIMESTAMP(\"2015-08-04 13:48:00\") ORDER BY tagid ASC, readtime DESC, channelid ASC");
        //PreparedStatement getLatestReads = jdbc.prepareStatement("SELECT * FROM tagreads WHERE (tagid=\"RFCMII00002809\" OR tagid=\"RFCMII00053596\") AND readtime=(SELECT MAX(readtime) FROM tagreads) ORDER BY tagid ASC, readtime DESC, channelid ASC");
        /*PreparedStatement getLatestReads = jdbc.prepareStatement("SELECT t.*\n" +
                "FROM tagreads t\n" +
                "WHERE t.readtime = (SELECT MAX(readtime) FROM tagreads WHERE tagvendor=\"thingmagic\") \n" +
                "   OR t.readtime = (SELECT MAX(readtime) FROM tagreads WHERE tagvendor=\"rfcode\") \n" +
                "   OR t.readtime = (SELECT MAX(readtime) FROM tagreads WHERE tagvendor=\"alien\")\n" +
                "ORDER BY readtime, tagvendor, tagid, channelid;");*/
        //PreparedStatement getLatestReads = jdbc.prepareStatement("SELECT * FROM tagreads WHERE channelvendor=\"rfcode\" AND readtime=\"2015-08-06 15:42:15\"");
        //setTime.execute();
        ArrayList<TagRead> data = new ArrayList<TagRead>();
        ResultSet reads = getLatestReads.executeQuery();
        while(reads.next()){
            TagRead indread = new TagRead(reads.getString("tagid"),reads.getString("tagvendor"),
            reads.getString("channelid"),
            reads.getString("channelvendor"),
            reads.getTimestamp("readtime").toString(),
            reads.getDouble("ssi"), this);
            data.add(indread);
        }
        return data;
    }
    public void addLocationData(String tagid, String tagvendor, String timestampstring, double x, double y) throws SQLException {
        PreparedStatement getLocation = jdbc.prepareStatement("SELECT * FROM locations WHERE tagid=? AND tagvendor=? AND readtime=?");
        PreparedStatement addLocation = jdbc.prepareStatement("INSERT INTO locations VALUES (?,?,?,POINT(?,?))");
        PreparedStatement updateLocation = jdbc.prepareStatement("UPDATE locations SET location=POINT(?,?) WHERE tagid=? AND tagvendor=? AND readtime=?");
        getLocation.setString(1, tagid);
        getLocation.setString(2, tagvendor);
        getLocation.setTimestamp(3, Timestamp.valueOf(timestampstring));
        if(!getLocation.executeQuery().next()) {
            addLocation.setString(1, tagid);
            addLocation.setString(2, tagvendor);
            addLocation.setTimestamp(3, Timestamp.valueOf(timestampstring));
            addLocation.setDouble(4, x);
            addLocation.setDouble(5, y);
            addLocation.executeUpdate();
        } else {
            updateLocation.setDouble(1, x);
            updateLocation.setDouble(2, y);
            updateLocation.setString(3, tagid);
            updateLocation.setString(4, tagvendor);
            updateLocation.setTimestamp(5, Timestamp.valueOf(timestampstring));
        }
    }
    public String getReaderType(String channelid, String channelvendor) throws SQLException, ChannelNotFoundException{
        PreparedStatement getReader = jdbc.prepareStatement("SELECT readerid FROM channels WHERE channelid=? AND channelvendor=?");
        getReader.setString(1, channelid);
        getReader.setString(2, channelvendor);
        ResultSet reader = getReader.executeQuery();
        if(!reader.next()) {
            throw new ChannelNotFoundException("No channel "+channelid+" from vendor "+channelvendor+" exists.");
        } else {
            PreparedStatement getReaderType = jdbc.prepareStatement("SELECT readertype FROM readers WHERE readerid=? AND readervendor=?");
            getReaderType.setString(1, reader.getString("readerid"));
            getReaderType.setString(2, channelvendor);
            ResultSet readertype = getReaderType.executeQuery();
            readertype.next();
            return readertype.getString("readertype");
        }
    }
    public String getReaderId(String channelid, String channelvendor) throws SQLException, ChannelNotFoundException{
        PreparedStatement getReader = jdbc.prepareStatement("SELECT readerid FROM channels WHERE channelid=? AND channelvendor=?");
        getReader.setString(1, channelid);
        getReader.setString(2, channelvendor);
        ResultSet reader = getReader.executeQuery();
        if(!reader.next()) {
            throw new ChannelNotFoundException("No channel "+channelid+" from vendor "+channelvendor+" exists.");
        } else {
            return reader.getString("readerid");
        }
    }
    public double[] getChannelLocation(String channelid, String channelvendor) throws SQLException, ChannelNotFoundException{
        PreparedStatement getReader = jdbc.prepareStatement("SELECT X(location) AS \"xcoord\", Y(location) AS \"ycoord\" FROM channels WHERE channelid=? AND channelvendor=?");
        getReader.setString(1, channelid);
        getReader.setString(2, channelvendor);
        ResultSet reader = getReader.executeQuery();
        if(!reader.next()) {
            throw new ChannelNotFoundException("No channel "+channelid+" from vendor "+channelvendor+" exists.");
            } else {
            double[] coord = new double[2];
            coord[0] = reader.getDouble("xcoord");
            coord[1] = reader.getDouble("ycoord");
            return coord;
        }
    }
    public void deleteTagReads(String tagid, String tagvendor, String channelid, String channelvendor, String timestampstring) throws SQLException{
        PreparedStatement deleteTag = jdbc.prepareStatement("DELETE FROM tagreads WHERE tagid=? AND tagvendor=? AND channelid=? AND channelvendor=? AND readtime=?");
        deleteTag.setString(1, tagid);
        deleteTag.setString(2, tagvendor);
        deleteTag.setString(3, channelid);
        deleteTag.setString(4, channelvendor);
        deleteTag.setTimestamp(5, Timestamp.valueOf(timestampstring));
        deleteTag.executeUpdate();
    }
    public void deleteUncalculatedTagReads() throws SQLException{
        PreparedStatement getLatestReads = jdbc.prepareStatement("DELETE a FROM tagreads a LEFT JOIN locations b ON a.tagid=b.tagid AND a.tagvendor=b.tagvendor AND a.readtime=b.readtime WHERE b.tagid IS NULL AND b.tagvendor IS NULL AND b.readtime IS NULL ORDER BY tagid ASC, readtime DESC, channelid ASC");
    }
    public class ChannelNotFoundException extends Exception {
        public ChannelNotFoundException(String msg){ super(msg); }
    }
}