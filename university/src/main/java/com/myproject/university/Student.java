package com.myproject.university;

public class Student extends Person {
    private final int course;
    private final String faculty;

    public Student(String name, int course, String faculty) {
        super(name);
        this.course = course;
        this.faculty = faculty;
    }

    public int getCourse() {
        return course;
    }

    public String getFaculty() {
        return faculty;
    }

    @Override
    public String getRole() {
        return "Student";
    }

    @Override
    public String getDescription() {
        return getName() + " is a student of " + faculty + " on course " + course + ".";
    }
}
