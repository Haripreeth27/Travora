-- ==========================================================
-- Travora Travel Management System
-- Seed Corporate Directory Employees - V2
-- ==========================================================

INSERT INTO employees (employee_id, name, department, designation, location)
VALUES 
    ('AGR0001', 'Suresh Babu Dangeti', 'Petroleum Operations', 'Executive', 'Headquarters'),
    ('AGR0003', 'Ganesh Kakarla', 'Site Operations', 'Site Engineer', 'Field Site 4'),
    ('AGR0007', 'Anil Kukreja', 'Operations', 'Manager', 'Regional Office'),
    ('AGR0009', 'Ramana M', 'Operations', 'Operations Executive', 'Regional Office'),
    ('MISPL0001', 'Umamaheswari Yandapalli', 'Corporate Management', 'Director', 'Executive Suite'),
    ('MISPL0002', 'Kalyan Swaroop Yandapalli', 'Executive Management', 'Managing Director', 'Executive Suite'),
    ('MISPL0003', 'Himani Agarwal', 'Finance & Accounts', 'Finance Manager', 'Finance Center'),
    ('MISPL0007', 'Sirisha Vegaraju', 'Administration', 'Executive Admin', 'Operations Center'),
    ('MISPL0014', 'Vijaya Kumar Bgam', 'Project Engineering', 'Senior Project Engineer', 'Project Site'),
    ('MISPL0016', 'P Satheesh', 'Field Engineering', 'Project Lead', 'Site Alpha'),
    ('MISPL0020', 'Satya Prasad B', 'Procurement', 'Procurement Manager', 'Supply Chain Hub')
ON CONFLICT (employee_id) DO NOTHING;
