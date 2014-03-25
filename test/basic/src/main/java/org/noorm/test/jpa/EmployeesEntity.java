package org.noorm.test.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 09.04.13
 *         Time: 14:33
 *
 * JPA entity for the EMPLOYEES table used for the NoORM/JPA performance comparison
 */
@Entity
@Table(name="EMPLOYEES")
public class EmployeesEntity {

    @Id
    // The allocation size parameter setting presumes that the increment for the sequence has (al least)
    // the same value, since JPA just fetches one value from the database and calculates all other values internally
    // with an increment of 1.
    @SequenceGenerator(name = "EMPLOYEES_EMPID_GENERATOR", sequenceName = "EMPLOYEES_SEQ", allocationSize = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EMPLOYEES_EMPID_GENERATOR")
    @Column(name="EMPLOYEE_ID", unique=true, nullable=false)
    protected Long employeeId;

    @Column(name="FIRST_NAME")
    protected String firstName;

    @Column(name="LAST_NAME", nullable=false)
    protected String lastName;

    @Column(name="EMAIL", nullable=false)
    protected String email;

    @Column(name="PHONE_NUMBER")
    protected String phoneNumber;

    @Column(name="HIRE_DATE", nullable=false)
    protected Timestamp hireDate;

    @Column(name="JOB_ID", nullable=false)
    protected String jobId;

    @Column(name="SALARY")
    protected Double salary;

    @Column(name="COMMISSION_PCT")
    protected Double commissionPct;

    @Column(name="MANAGER_ID")
    protected Long managerId;

    @Column(name="DEPARTMENT_ID")
    protected Long departmentId;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(final Long pEmployeeId) {
        employeeId = pEmployeeId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String pFirstName) {
        firstName = pFirstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String pLastName) {
        lastName = pLastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String pEmail) {
        email = pEmail;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(final String pPhoneNumber) {
        phoneNumber = pPhoneNumber;
    }

    public Timestamp getHireDate() {
        return hireDate;
    }

    public void setHireDate(final Timestamp pHireDate) {
        hireDate = pHireDate;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(final String pJobId) {
        jobId = pJobId;
    }

    public Double getSalary() {
        return salary;
    }

    public void setSalary(final Double pSalary) {
        salary = pSalary;
    }

    public Double getCommissionPct() {
        return commissionPct;
    }

    public void setCommissionPct(final Double pCommissionPct) {
        commissionPct = pCommissionPct;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(final Long pManagerId) {
        managerId = pManagerId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(final Long pDepartmentId) {
        departmentId = pDepartmentId;
    }

    @Override
    public boolean equals(final Object pObject) {
        if (this == pObject) return true;
        if (pObject == null || !(pObject instanceof EmployeesEntity)) return false;
        if (getEmployeeId() == null) return false;
        final EmployeesEntity other = (EmployeesEntity) pObject;
        return getEmployeeId().equals(other.getEmployeeId());
    }

    @Override
    public int hashCode() {
        if (getEmployeeId() == null) return super.hashCode();
        return getEmployeeId().hashCode();
    }
}
