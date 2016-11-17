import java.util.*;
import java.sql.*;

public class CircleInt {
    public static void main(String[] args) throws Exception {
        Scanner input = new Scanner(System.in);

        DBConnector jdbc = null;
        try {
            jdbc = new DBConnector("10.11.34.174", "3306", "shelter_mac", "root", "PLATO"); //connect to Database
        } catch (SQLException e) {
            e.printStackTrace();
        }
        while(true) {
            System.out.println("Getting SSI data...");
            ArrayList<TagRead> tagreaddata = jdbc.getSsiData();
            System.out.println("Got SSI data!");
            ArrayList<Tag> tags = new ArrayList<Tag>();
            int curtag = -1;

            for (int i = 0; i < tagreaddata.size(); i++) {
                //    System.out.println(tagreaddata.get(i));
                TagRead tr = tagreaddata.get(i);
                if (curtag == -1 || !tags.get(curtag).getTagId().equals(tr.getTagId()) || !tags.get(curtag).getTagVendor().equals(tr.getTagVendor()) || !tags.get(curtag).getTime().equals(tr.getTimestamp())) {
                    //       System.out.println("New tag");
                    curtag++;
                    Tag tag = new Tag(tr.getTagId(), tr.getTagVendor(), tr.getTimestamp());
                    tag.getTagReads().add(tr);
                    tags.add(tag);
                } else {
                    //      System.out.println("Same tag");
                
                /*rssi =tr.getSsi();
                if((tr.getTagVendor()).equals("rfcode")){
                    if (jdbc.getReaderType(tr.getChannelId(), tr.getChannelVendor()).equals("M250")){
                        System.out.println("distance= " + Math.exp((Math.abs(rssi)-54.537)/(11.965)));
                        
                    }
                    else if(jdbc.getReaderType(tr.getChannelId(), tr.getChannelVendor()).equals("M200")){
                        System.out.println("distance= " + Math.exp((Math.abs(rssi)-66.399)/(11.029)));
                    }
                }
                else if((tr.getTagVendor()).equals("thingmagic")){
                    System.out.println("distance= " + Math.log(((rssi + 80.14)/(35.04)))/-.38);
                    
                }*/

                    tags.get(curtag).getTagReads().add(tr);
                }
                //input.next();
            }
            for (Tag t : tags) {
              //  System.out.println(t);
                if (!t.getTagVendor().equals("alien") && t.getTagReads().size() > 2) {
                    CircleInt.calculateCircle(t, jdbc);
                } else if(!t.getTagVendor().equals("alien")) {
                    PreparedStatement sanityCheck = jdbc.getConnection().prepareStatement("SELECT * FROM locations WHERE tagid=? AND tagvendor=? AND readtime=?");
                   // System.out.println("DELETING: "+t);
                    sanityCheck.setString(1, t.getTagId());
                    sanityCheck.setString(2, t.getTagVendor());
                    sanityCheck.setTimestamp(3, Timestamp.valueOf(t.getTime()));
                    if(!sanityCheck.executeQuery().next()) {
                        for (TagRead tr : t.getTagReads()) {
                    //        System.out.println("DELETING: "+tr);
                            jdbc.deleteTagReads(tr.getTagId(), tr.getTagVendor(), tr.getChannelId(), tr.getChannelVendor(), tr.getTimestamp());
                        }
                    }
                } else {
                    CircleInt.calculateLocationAlien(t, jdbc);
                }
             //   input.nextLine();
            }
            System.out.println("Finished pass, deleting");
            //jdbc.deleteUncalculatedTagReads();
            //Thread.sleep(2000);
        }
    }
/*
for(int i= 0; i< 3; i++){
    System.out.println("reader");
    String reader = input.nextLine();
    System.out.println("rssi");
    rssi = Int.parseint(input.nextInt());
    if (reader.equals("M250")){
        double radius =(Math.exp((Math.abs(rssi)-54.537)/(11.965)));
        System.out.println(radius);
    }
    else if(reader.equals("M200")){
        System.out.println(Math.exp((Math.abs(rssi)-66.399)/(11.029)));
    }
}

System.out.println("X Coordinate for Reader 1");
double x1 = input.nextDouble();
System.out.println("Y Coordinate for Reader 1");
double y1 = input.nextDouble();
System.out.println("Radius for Reader 1");
double r1 = input.nextDouble();

System.out.println("X Coordinate for Reader 2");
double x2 = input.nextDouble();
System.out.println("Y Coordinate for Reader 2");
double y2 = input.nextDouble();
System.out.println("Radius for Reader 2");
double r2 = input.nextDouble();


System.out.println("X coordinate for Reader 3");
double x3 = input.nextDouble();
System.out.println("Y coordinate for Reader 3");
double y3 = input.nextDouble();
System.out.println("Radius for Reader 3");
double r3 = input.nextDouble(); */

    public static void calculateCircle(Tag t, DBConnector jdbc) throws SQLException, DBConnector.ChannelNotFoundException {
        Scanner input = new Scanner(System.in);
        String[] channelid = new String[3];
        double[] x = new double[3];
        double[] y = new double[3];
        double[] r = new double[3];
        int origreader=-1;
        int xreader=-1;
        int j = 0;
        double xloc[] = null;
        boolean inaccurate=false;
        int skippedindex=0;
        ArrayList<Integer> skipped = new ArrayList<Integer>();
        for (int i = 0; i < t.getTagReads().size(); i++) {
            if(j>2) break;
            if(t.getTagVendor().equals("rfcode")) {
                if (i > 0 && t.getTagReads().get(i).getChannelId().contains(jdbc.getReaderId(t.getTagReads().get(i - 1).getChannelId(), t.getTagReads().get(i - 1).getChannelVendor()))) {
        //            System.out.println("Same Reader");
                    skipped.add(i);
                } else {
         //           System.out.println("Different Reader");
                    channelid[j] = t.getTagReads().get(i).getChannelId();
                    x[j] = jdbc.getChannelLocation(t.getTagReads().get(i).getChannelId(), t.getTagReads().get(i).getChannelVendor())[0];
                    y[j] = jdbc.getChannelLocation(t.getTagReads().get(i).getChannelId(), t.getTagReads().get(i).getChannelVendor())[1];
                    r[j] = t.getTagReads().get(i).getDistance();
                    if (r[j] > 20) {
                        if(skippedindex<t.getTagReads().size()) {
                            channelid[j] = t.getTagReads().get(skippedindex).getChannelId();
                            x[j] = jdbc.getChannelLocation(t.getTagReads().get(skippedindex).getChannelId(), t.getTagReads().get(skippedindex).getChannelVendor())[0] + 0.2;
                            y[j] = jdbc.getChannelLocation(t.getTagReads().get(skippedindex).getChannelId(), t.getTagReads().get(skippedindex).getChannelVendor())[1] + 0.2;
                            r[j] = t.getTagReads().get(skippedindex).getDistance();
                            skippedindex++;
                            if(r[j] > 20) {
                                inaccurate=true;
                            }
                        }
                    } else if (r[j] < 1){
                        xloc = new double[2];
                        xloc[0] = x[j];
                        xloc[1] = y[j];
                    }
            //        System.out.printf("Channel: %s, r%d: %f, SSI: %f\n", channelid[j], i, t.getTagReads().get(i).getDistance(), t.getTagReads().get(i).getSsi());
                    if (x[j] == 0 && y[j] == 0) {
                        origreader = j;
                    } else if (y[j] == 0) {
                        xreader = j;
                    }
                    j++;
                }
                //input.nextLine();
            } else {
                channelid[j] = t.getTagReads().get(i).getChannelId();
                x[j] = jdbc.getChannelLocation(t.getTagReads().get(i).getChannelId(), t.getTagReads().get(i).getChannelVendor())[0];
                y[j] = jdbc.getChannelLocation(t.getTagReads().get(i).getChannelId(), t.getTagReads().get(i).getChannelVendor())[1];
                r[j] = t.getTagReads().get(i).getDistance();
           //     System.out.printf("Channel: %s, r%d: %f, SSI: %f\n", channelid[j], i, t.getTagReads().get(i).getDistance(), t.getTagReads().get(i).getSsi());
                if (x[j] == 0 && y[j] == 0) {
                    origreader = j;
                } else if (y[j] == 0) {
                    xreader = j;
                }
                j++;
            }
        }
        if (channelid[2] == null) {
        } else if (!inaccurate && xloc == null && skippedindex < 2) {
            double x1 = x[0];
            double x2 = x[1];
            double x3 = x[2];
            double y1 = y[0];
            double y2 = y[1];
            double y3 = y[2];
            double r1 = r[0];
            double r2 = r[1];
            double r3 = r[2];
            /*System.out.println("Circle Equations");
            System.out.println("(x - " + x1 + ")^2 + (y - " + y1 + ")^2 = " + Math.pow(r1, 2));
            System.out.println("(x - " + x2 + ")^2 + (y - " + y2 + ")^2 = " + Math.pow(r2, 2));
            System.out.println("(x - " + x3 + ")^2 + (y - " + y3 + ")^2 = " + Math.pow(r3, 2));*/


            // Find intersection of three readers

            double a = (2 * x2) - (2 * x1);
            double b = (2 * y2) - (2 * y1);
            double c = ((Math.pow(r1, 2) - (Math.pow(x1, 2) + Math.pow(y1, 2))) - (Math.pow(r2, 2) - (Math.pow(x2, 2) + Math.pow(y2, 2))));
            /*System.out.println("System of linear equations ");
            System.out.println(a + "x + " + b + "y = " + c);*/

            double d = (2 * x3) - (2 * x1);
            double e = (2 * y3) - (2 * y1);
            double f = ((Math.pow(r1, 2) - (Math.pow(x1, 2) + Math.pow(y1, 2))) - (Math.pow(r3, 2) - (Math.pow(x3, 2) + Math.pow(y3, 2))));
            //System.out.println(d + "x + " + e + "y = " + f);


            double g = ((f - (d * (c / a))) / (((d * (-1 * b / a)) + e)));
            double h = ((c - (b * g)) / a);
//            System.out.println("Intersection point:\nX coordinate: " + h + " Y coordinate: " + g);
            if (h > 0 && g > 0) {
                jdbc.addLocationData(t.getTagId(), t.getTagVendor(), t.getTime(), h, g);
            } else {
                PreparedStatement sanityCheck = jdbc.getConnection().prepareStatement("SELECT * FROM locations WHERE tagid=? AND tagvendor=? AND readtime=?");
                sanityCheck.setString(1, t.getTagId());
                sanityCheck.setString(2, t.getTagVendor());
                sanityCheck.setTimestamp(3, Timestamp.valueOf(t.getTime()));
                if(!sanityCheck.executeQuery().next()) {
                    for (TagRead tr : t.getTagReads()) {
                        jdbc.deleteTagReads(tr.getTagId(), tr.getTagVendor(), tr.getChannelId(), tr.getChannelVendor(), tr.getTimestamp());
                    }
                }
            }
        } else if (xloc == null) {
            Random rand = new Random();
            double h = 10 + (20 - 10) * rand.nextDouble();
            double g = 10 + (20 - 10) * rand.nextDouble();
 //           System.out.println("Estimated point:\nX coordinate: " + h + " Y coordinate: " + g);
            if (h > 0 && g > 0) {
                jdbc.addLocationData(t.getTagId(), t.getTagVendor(), t.getTime(), h, g);
            } else {
                PreparedStatement sanityCheck = jdbc.getConnection().prepareStatement("SELECT * FROM locations WHERE tagid=? AND tagvendor=? AND readtime=?");
                sanityCheck.setString(1, t.getTagId());
                sanityCheck.setString(2, t.getTagVendor());
                sanityCheck.setTimestamp(3, Timestamp.valueOf(t.getTime()));
                if(!sanityCheck.executeQuery().next()) {
                    for (TagRead tr : t.getTagReads()) {
                        jdbc.deleteTagReads(tr.getTagId(), tr.getTagVendor(), tr.getChannelId(), tr.getChannelVendor(), tr.getTimestamp());
                    }
                }
            }
        } else {
            double h = xloc[0];
            double g = xloc[1];
  //          System.out.println("Estimated point:\nX coordinate: " + h + " Y coordinate: " + g);
            if (h > 0 && g > 0) {
               jdbc.addLocationData(t.getTagId(), t.getTagVendor(), t.getTime(), h, g);
            } else {
                PreparedStatement sanityCheck = jdbc.getConnection().prepareStatement("SELECT * FROM locations WHERE tagid=? AND tagvendor=? AND readtime=?");
                sanityCheck.setString(1, t.getTagId());
                sanityCheck.setString(2, t.getTagVendor());
                sanityCheck.setTimestamp(3, Timestamp.valueOf(t.getTime()));
                if(!sanityCheck.executeQuery().next()) {
                    for (TagRead tr : t.getTagReads()) {
                        jdbc.deleteTagReads(tr.getTagId(), tr.getTagVendor(), tr.getChannelId(), tr.getChannelVendor(), tr.getTimestamp());
                    }
                }
            }
        }
    }
    public static void calculateLocationAlien(Tag t, DBConnector jdbc) throws SQLException, DBConnector.ChannelNotFoundException {
        TagRead tl = t.getTagReads().get(0);
        double coord[] = jdbc.getChannelLocation(tl.getChannelId(), tl.getChannelVendor());
   //     System.out.println("Location cell:\nX coordinate: " + coord[0] + " Y coordinate: " + coord[1]);
        jdbc.addLocationData(t.getTagId(), t.getTagVendor(), t.getTime(), coord[0], coord[1]);
    }
}


/*send location to database
try{
    jdbc.addLocationData(String tagid, String tagvendor, String timestampstring, double x, double y);
    } catch(SQLException e) {
    e.printStackTrace();
    tagid= hm.get("tagid");
    tagvendor = hm.get("tagvendor");
    timestampstring = hm.get("readtime");
    x= h;
    y= g;
}
} */
