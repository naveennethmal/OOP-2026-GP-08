CREATE DATABASE IF NOT EXISTS ftms; USE ftms;

CREATE TABLE department(department_id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE);
CREATE TABLE users(
  user_id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL UNIQUE, password_hash CHAR(64) NOT NULL,
  full_name VARCHAR(100) NOT NULL, email VARCHAR(100), phone VARCHAR(20), address VARCHAR(255), profile_picture VARCHAR(255),
  role ENUM('ADMIN','LECTURER','TECHNICAL_OFFICER','UNDERGRADUATE') NOT NULL);
CREATE TABLE course(
  course_code VARCHAR(20) PRIMARY KEY, name VARCHAR(120) NOT NULL, credits INT NOT NULL,
  type ENUM('THEORY','PRACTICAL','BOTH') NOT NULL, semester INT NOT NULL, department_id INT NOT NULL,
  FOREIGN KEY (department_id) REFERENCES department(department_id));
CREATE TABLE notice(
  notice_id INT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(150) NOT NULL, description TEXT, published_date DATE NOT NULL, created_by INT NOT NULL,
  FOREIGN KEY (created_by) REFERENCES users(user_id));
CREATE TABLE lecture_hall(hall_id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50) NOT NULL UNIQUE, capacity INT NOT NULL);
CREATE TABLE timetable(
  id INT AUTO_INCREMENT PRIMARY KEY, course_code VARCHAR(20) NOT NULL, lecturer_id INT NOT NULL, hall_id INT NOT NULL,
  day ENUM('Monday','Tuesday','Wednesday','Thursday','Friday','Saturday') NOT NULL,
  start_time TIME NOT NULL, end_time TIME NOT NULL, session_type ENUM('THEORY','PRACTICAL') NOT NULL,
  CHECK (end_time > start_time),
  FOREIGN KEY (course_code) REFERENCES course(course_code),
  FOREIGN KEY (lecturer_id) REFERENCES users(user_id),
  FOREIGN KEY (hall_id) REFERENCES lecture_hall(hall_id));

-- sample data (all sample passwords = admin123)
INSERT INTO department(name) VALUES ('Information and Communication Technology'),('Bio-system Technology');
INSERT INTO lecture_hall(name,capacity) VALUES ('LH-01',120),('LH-02',80),('Lab-01',40),('Lab-02',40);
INSERT INTO users(username,password_hash,full_name,email,phone,role) VALUES
 ('admin','240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9','System Admin','admin@tec.ruh.ac.lk','0710000000','ADMIN'),
 ('lec1','240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9','Dr. Nimal Perera','nimal@tec.ruh.ac.lk','0711111111','LECTURER'),
 ('lec2','240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9','Ms. Kumari Silva','kumari@tec.ruh.ac.lk','0712222222','LECTURER');
INSERT INTO course VALUES ('ICT2132','Object Oriented Programming Practicum',2,'PRACTICAL',1,1),('ICT2113','Data Structures and Algorithms',3,'BOTH',1,1);
