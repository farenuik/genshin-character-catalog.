package com.myproject.university;

public class Teacher extends Person {
    private final String subject;

    public Teacher(String name, String subject) {
        super(name);
        this.subject = subject;
    }

    public String getSubject() {
        return subject;
    }

    @Override
    public String getRole() {
        return "Teacher";
    }

    @Override
    public String getDescription() {
        return getName() + " teaches " + subject + ".";
    }

    public String gradeStudent(String studentName, int grade) {
        return "Grade " + grade + " for " + studentName;
    }
}
