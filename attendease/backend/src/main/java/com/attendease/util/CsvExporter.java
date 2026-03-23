package com.attendease.util;

import com.attendease.entity.Attendance;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class CsvExporter {

    public static ResponseEntity<byte[]> exportAttendanceToCsv(String sessionTitle, List<Attendance> attendanceList) {
        try {
            // Create CSV format
            CSVFormat csvFormat = CSVFormat.DEFAULT
                    .withHeader("Student Name", "Index Number", "Level", "Latitude", "Longitude", "Join Time");

            // Create ByteArrayOutputStream to hold CSV data
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);

            // Create CSV printer
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            // Write attendance records
            for (Attendance attendance : attendanceList) {
                csvPrinter.printRecord(
                    attendance.getFullname(),
                    attendance.getIndexNumber(),
                    attendance.getLevel(),
                    String.format("%.7f", attendance.getLatitude()),
                    String.format("%.7f", attendance.getLongitude()),
                    attendance.getJoinedAt()
                );
            }

            // Flush and close
            csvPrinter.flush();
            csvPrinter.close();

            // Get bytes
            byte[] csvData = outputStream.toByteArray();

            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", "attendance_" + sessionTitle + ".csv");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvData);

        } catch (IOException e) {
            System.err.println("Error exporting CSV: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}