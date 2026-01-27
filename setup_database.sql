-- Quick Database Setup for Engdb Testing
-- Run this to create the correct schema

CREATE DATABASE IF NOT EXISTS engdb;
USE engdb;

-- Drop existing tables if they exist
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS student;

-- Create students table (PLURAL)
CREATE TABLE student (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    age INT,
    department VARCHAR(50)
);

-- Create courses table
CREATE TABLE courses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    credits INT
);

-- Create enrollments table
CREATE TABLE enrollments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT,
    course_id INT,
    grade VARCHAR(2),
    FOREIGN KEY (student_id) REFERENCES student(id),
    FOREIGN KEY (course_id) REFERENCES courses(id)
);

-- Insert sample data
INSERT INTO student (name, age, department) VALUES 
('Alice', 20, 'CS'),
('Bob', 21, 'Math'),
('Charlie', 22, 'CS'),
('Diana', 19, 'Physics');

INSERT INTO courses (name, credits) VALUES
('Database Systems', 3),
('Algorithms', 4),
('Calculus', 3);

INSERT INTO enrollments (student_id, course_id, grade) VALUES
(1, 1, 'A'),
(1, 2, 'B'),
(2, 3, 'A'),
(3, 1, 'B');

-- Verify tables
SHOW TABLES;
SELECT 'Student table:' as '';
SELECT * FROM student;
