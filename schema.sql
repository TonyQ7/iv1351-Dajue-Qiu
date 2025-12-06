BEGIN;

CREATE SCHEMA IF NOT EXISTS dsp;
SET search_path = dsp, public;

-- Reference tables
CREATE TABLE study_period (
  code VARCHAR(2) PRIMARY KEY,
  quarter_num INT NOT NULL CHECK (quarter_num BETWEEN 1 AND 4)
);

CREATE TABLE job_title (
  job_title VARCHAR(80) PRIMARY KEY
);

-- People / Departments
CREATE TABLE person (
  personal_number VARCHAR(32) PRIMARY KEY,
  first_name TEXT NOT NULL,
  last_name  TEXT NOT NULL,
  email      TEXT NOT NULL UNIQUE,
  phone_number TEXT NOT NULL,
  address    TEXT NOT NULL
);

CREATE TABLE department (
  department_id SERIAL PRIMARY KEY,
  department_name TEXT NOT NULL UNIQUE,
  manager_employee_id INT UNIQUE
);

CREATE TABLE employee (
  employee_id SERIAL PRIMARY KEY,
  personal_number VARCHAR(32) NOT NULL UNIQUE REFERENCES person(personal_number) ON UPDATE CASCADE ON DELETE RESTRICT,
  department_id INT NOT NULL REFERENCES department(department_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  job_title VARCHAR(80) NOT NULL REFERENCES job_title(job_title) ON UPDATE CASCADE ON DELETE RESTRICT,
  skill_set TEXT NOT NULL,
  supervisor_id INT NULL REFERENCES employee(employee_id) ON UPDATE CASCADE ON DELETE SET NULL
);

ALTER TABLE department
  ADD CONSTRAINT department_manager_fk
  FOREIGN KEY (manager_employee_id) REFERENCES employee(employee_id) ON UPDATE CASCADE ON DELETE SET NULL;

-- Versioned course layout
CREATE TABLE course_layout (
  course_code VARCHAR(16) PRIMARY KEY,
  course_name TEXT NOT NULL
);

CREATE TABLE course_layout_version (
  layout_version_id SERIAL PRIMARY KEY,
  course_code VARCHAR(16) NOT NULL REFERENCES course_layout(course_code) ON UPDATE CASCADE ON DELETE RESTRICT,
  version_no INT NOT NULL,
  hp NUMERIC(4,1) NOT NULL CHECK (hp > 0),
  min_students INT NOT NULL CHECK (min_students >= 0),
  max_students INT NOT NULL CHECK (max_students >= min_students),
  UNIQUE (course_code, version_no)
);

CREATE TABLE course_instance (
  instance_id VARCHAR(32) PRIMARY KEY,
  course_code VARCHAR(16) NOT NULL REFERENCES course_layout(course_code) ON UPDATE CASCADE ON DELETE RESTRICT,
  layout_version_no INT NOT NULL,
  study_year  INT NOT NULL CHECK (study_year BETWEEN 2000 AND 2100),
  study_period VARCHAR(2) NOT NULL REFERENCES study_period(code) ON UPDATE CASCADE ON DELETE RESTRICT,
  num_students INT NOT NULL CHECK (num_students >= 0),
  FOREIGN KEY (course_code, layout_version_no)
    REFERENCES course_layout_version(course_code, version_no) ON UPDATE CASCADE ON DELETE RESTRICT,
  UNIQUE (course_code, study_year, study_period)
);

-- Teaching activities
CREATE TABLE teaching_activity (
  activity_id SERIAL PRIMARY KEY,
  activity_name TEXT NOT NULL UNIQUE,
  factor NUMERIC(6,2) NOT NULL CHECK (factor > 0),
  is_derived BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE derived_activity_coeffs (
  activity_id INT PRIMARY KEY REFERENCES teaching_activity(activity_id) ON UPDATE CASCADE ON DELETE CASCADE,
  const NUMERIC(12,4) NOT NULL,
  hp_coeff NUMERIC(12,4) NOT NULL,
  students_coeff NUMERIC(12,4) NOT NULL
);

CREATE TABLE planned_activity (
  course_instance_id VARCHAR(32) NOT NULL REFERENCES course_instance(instance_id) ON UPDATE CASCADE ON DELETE CASCADE,
  activity_id INT NOT NULL REFERENCES teaching_activity(activity_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  planned_hours NUMERIC(10,2) NOT NULL CHECK (planned_hours >= 0),
  PRIMARY KEY (course_instance_id, activity_id)
);

-- Convenience view used by the seed data and reports to expose effective (factor-adjusted) hours
-- for every planned activity row. Derived activities still use the stored planned_hours value,
-- which is calculated from coefficients during seed loading.
CREATE OR REPLACE VIEW v_activity_hours AS
SELECT pa.course_instance_id,
       pa.activity_id,
       ta.activity_name,
       pa.planned_hours,
       ta.factor,
       pa.planned_hours * ta.factor AS effective_hours
FROM dsp.planned_activity pa
JOIN dsp.teaching_activity ta ON ta.activity_id = pa.activity_id;

-- Versioned salary
CREATE TABLE employee_salary_history (
  salary_version_id SERIAL PRIMARY KEY,
  employee_id INT NOT NULL REFERENCES employee(employee_id) ON UPDATE CASCADE ON DELETE CASCADE,
  version_no INT NOT NULL,
  salary_hour NUMERIC(10,2) NOT NULL CHECK (salary_hour > 0),
  UNIQUE (employee_id, version_no)
);

-- Allocation Table (Modified for Task 3 Requirements)
CREATE TABLE allocation (
  allocation_id SERIAL PRIMARY KEY, -- Added for Task 3 Deallocation
  employee_id INT NOT NULL REFERENCES employee(employee_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  course_instance_id VARCHAR(32) NOT NULL REFERENCES course_instance(instance_id) ON UPDATE CASCADE ON DELETE CASCADE,
  activity_id INT NOT NULL REFERENCES teaching_activity(activity_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  salary_version_id INT NOT NULL REFERENCES employee_salary_history(salary_version_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  allocated_hours NUMERIC(10,2) NOT NULL CHECK (allocated_hours >= 0),
  is_terminated BOOLEAN NOT NULL DEFAULT FALSE, -- Added for Soft Delete
  UNIQUE (employee_id, course_instance_id, activity_id), -- Maintain logical uniqueness
  FOREIGN KEY (course_instance_id, activity_id)
    REFERENCES planned_activity(course_instance_id, activity_id) ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE allocation_rule (
  rule_id SMALLSERIAL PRIMARY KEY,
  max_instances_per_period INT NOT NULL CHECK (max_instances_per_period >= 1),
  CONSTRAINT single_rule CHECK (rule_id = 1)
);

COMMIT;
