package com.myproject.tests;

import com.myproject.university.Student;
import com.myproject.university.Teacher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UniversityModuleTest {
    @Test
    void studentAndTeacherExposeTheirRolesAndDescriptions() {
        Student student = new Student("Olena", 2, "Engineering");
        Teacher teacher = new Teacher("Taras", "Physics");

        assertEquals("Student", student.getRole());
        assertEquals("Olena is a student of Engineering on course 2.", student.getDescription());
        assertEquals("Teacher", teacher.getRole());
        assertEquals("Taras teaches Physics.", teacher.getDescription());
        assertEquals("Grade 5 for Olena", teacher.gradeStudent(student.getName(), 5));
    }

    @Test
    void personRejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new Student(" ", 1, "Science"));
    }
}