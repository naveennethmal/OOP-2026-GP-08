package com.teclms.technician;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;


public final class DemoData {

    private DemoData() {}

    public static final class AttendanceRow {
        public static final int TOTAL_SESSIONS = 15;

        private final String regNo;
        private final String name;
        private final int attended;
        private final String medical;                 // None / Pending / Approved
        private final StringProperty session = new SimpleStringProperty("Present");

        public AttendanceRow(String regNo, String name, int attended, String medical) {
            this.regNo = regNo;
            this.name = name;
            this.attended = attended;
            this.medical = medical;
        }

        public String regNo() { return regNo; }
        public String name() { return name; }
        public int attended() { return attended; }
        public String medical() { return medical; }
        public StringProperty sessionProperty() { return session; }

        public double rate() { return attended * 100.0 / TOTAL_SESSIONS; }

        public String eligibility() {
            if (rate() >= 80) return "Eligible";
            return switch (medical) {
                case "Approved" -> "Eligible (medical)";
                case "Pending" -> "Medical pending";
                default -> "Not eligible";
            };
        }
    }

    public record MedicalRecord(String regNo, String name, String date, String reason, String status) {}

    public record Notice(String title, String category, String date, String body, boolean isNew) {}


    public record Slot(int day, int period, String code, String name, String room, boolean practical) {}

    public static ObservableList<AttendanceRow> attendance() {
        return FXCollections.observableArrayList(
                new AttendanceRow("TG/2024/2115", "Nimal Perera", 14, "Pending"),
                new AttendanceRow("TG/2024/2100", "Tharushi Weerasinghe", 15, "None"),
                new AttendanceRow("TG/2024/2117", "Kavindu Silva", 12, "None"),
                new AttendanceRow("TG/2024/2266", "Sanduni Fernando", 10, "Pending"),
                new AttendanceRow("TG/2024/2278", "Isuru Bandara", 11, "Approved"),
                new AttendanceRow("TG/2024/2114", "Dilini Rajapaksha", 13, "Approved"),
                new AttendanceRow("TG/2024/2145", "Amal Jayasinghe", 9, "None"),
                new AttendanceRow("TG/2024/2234", "Ruwan Kumara", 10, "Approved"));
    }

    public static ObservableList<MedicalRecord> medicals() {
        return FXCollections.observableArrayList(
                new MedicalRecord("2022/ICT/001", "Nimal Perera", "2026-09-17", "Food poisoning", "Pending"),
                new MedicalRecord("2022/ICT/019", "Sanduni Fernando", "2026-09-15", "Viral fever", "Pending"),
                new MedicalRecord("2022/ICT/023", "Isuru Bandara", "2026-09-10", "Dental surgery", "Approved"),
                new MedicalRecord("2022/ICT/030", "Dilini Rajapaksha", "2026-09-08", "Migraine", "Approved"),
                new MedicalRecord("2021/ICT/012", "Ruwan Kumara", "2026-09-12", "Hospital admission", "Approved"));
    }

    public static List<Notice> notices() {
        return List.of(
                new Notice("Mid-semester examination schedule released", "Exam", "2026-09-25",
                        "The mid-semester examination timetable for Level II Semester I is now available. Check your department timetable for hall allocations.", true),
                new Notice("Lab maintenance on Friday", "General", "2026-09-24",
                        "Computer Lab 02 will be closed on Friday from 1.00 p.m. for scheduled maintenance. Practical sessions move to Lab 03.", true),
                new Notice("Medical submission deadline", "General", "2026-09-20",
                        "Medical certificates must be submitted within seven days of the missed session to the technical officer in charge.", false),
                new Notice("ICT2132 mini project presentations", "Event", "2026-09-18",
                        "Mini project evaluations are on 5th November. Each group has 15 minutes for the presentation and 10 minutes for discussion.", false));
    }

    public static List<Slot> timetable() {
        return List.of(
                new Slot(0, 0, "ICT2113", "Data Structures", "LT 01", false),
                new Slot(0, 2, "ICT2132", "OOP Practicum", "Lab 02", true),
                new Slot(1, 1, "ICT2122", "Web Technologies", "LT 02", false),
                new Slot(2, 0, "ICT2132", "OOP Practicum", "Lab 02", true),
                new Slot(2, 2, "ICT2142", "Database Systems", "LT 01", false),
                new Slot(3, 1, "ICT2113", "Data Structures", "Lab 01", true),
                new Slot(4, 0, "ICT2122", "Web Technologies", "Lab 03", true),
                new Slot(4, 2, "ICT2142", "Database Systems", "LT 03", false));
    }
}
