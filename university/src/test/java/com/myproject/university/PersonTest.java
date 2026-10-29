package com.myproject.university;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersonTest {

    @Test
    void studentShouldHaveCourseAndDescription() {
        Student student = new Student("Anna", 2, "Computer Science");

        assertEquals("Anna", student.getName());
        assertEquals("Student", student.getRole());
        assertEquals("Anna is a student of Computer Science on course 2.", student.getDescription());
    }

    @Test
    void teacherShouldGradeStudent() {
        Teacher teacher = new Teacher("Petro", "Math");

        assertEquals("Teacher", teacher.getRole());
        assertEquals("Petro teaches Math.", teacher.getDescription());
        assertEquals("Grade 5 for Anna", teacher.gradeStudent("Anna", 5));
    }
}
