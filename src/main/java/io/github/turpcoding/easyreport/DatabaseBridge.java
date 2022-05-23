package io.github.turpcoding.easyreport;

import java.sql.*;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseBridge {

    private final EasyReport mainClass = EasyReport.getInstance();
    private Connection connectToDatabase() throws Exception {
        try {
            String hostName = mainClass.getConfig().getString("database.host");
            int port = mainClass.getConfig().getInt("database.port");
            String databaseName = mainClass.getConfig().getString("database.database-name");
            if (hostName.equals("")) {
                throw new Exception("host empty in config.yml");
            }
            // Removes any slash characters from the hostname.
            Pattern pattern = Pattern.compile("/");
            Matcher matcher = pattern.matcher(hostName);
            hostName = matcher.replaceAll("");

            String databaseURL = "jdbc:mysql://" + hostName + ":" + port + "/" + databaseName;

            return DriverManager.getConnection(
                    databaseURL, mainClass.getConfig().getString("database.username"), mainClass.getConfig().getString("database.password"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error thrown when executing DatabaseBridge.connectToDatabase()");
        }
    }

    // Gets data on all the player's reports and returns it as a ArrayList<ArrayList<String>> with each list inside the list representing a filtered row.
    public ArrayList<ArrayList<String>> getReports(String player) throws Exception {
        try {
            ArrayList<ArrayList<String>> queryResults = new ArrayList<>();
            Connection connection = connectToDatabase();
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM reports WHERE lower(reportedPlayer)='" + player.toLowerCase() + "'");

            // Checks if the ResultSet is empty.
            boolean isEmpty = !rs.isBeforeFirst();
            // If so return a special list of lists indicating that the record is empty.
            if (isEmpty) {
                ArrayList<ArrayList<String>> emptyRecord = new ArrayList<>();
                emptyRecord.add(new ArrayList<>(Collections.singletonList("Empty")));
                return emptyRecord;
            }

            while (rs.next()) {
                ArrayList<String> currentRow = new ArrayList<>();
                currentRow.add(rs.getString("reportedPlayer")); // To get the name with correct capitalization.
                currentRow.add(rs.getString("playerThatReported"));
                currentRow.add(rs.getString("reason"));
                currentRow.add(rs.getString("date"));
                queryResults.add(currentRow);
            }
            rs.close();
        return queryResults;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error thrown when executing DatabaseBridge.getReports()");
        }
    }

    public boolean insertInto(String reportedPlayer, String playerThatReported, String reason) {
        try {
            Connection connection = connectToDatabase();
            String date = LocalDate.now(ZoneId.of("America/Los_Angeles")).toString();
            String query = MessageFormat.format(
                    "INSERT INTO reports (reportedPlayer,playerThatReported,reason,date) VALUES ({0},{1},{2},{3});",
                    "'" + reportedPlayer + "'", "'" + playerThatReported + "'", "'" + reason + "'", "'" + date + "'");
            Statement statement = connection.createStatement();
            statement.executeUpdate(query);
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public DatabaseBridge() {}
}