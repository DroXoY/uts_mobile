package com.example.uts;

public class Task {
    private int id;
    private String title;
    private String course;
    private String priority;
    private String deadline;
    private int status;

    public Task(int id, String title, String course, String priority, String deadline, int status) {
        this.id = id;
        this.title = title;
        this.course = course;
        this.priority = priority;
        this.deadline = deadline;
        this.status = status;
    }

    // Getter saja sudah cukup
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getCourse() { return course; }
    public String getPriority() { return priority; }
    public String getDeadline() { return deadline; }
    public int getStatus() { return status; }
}