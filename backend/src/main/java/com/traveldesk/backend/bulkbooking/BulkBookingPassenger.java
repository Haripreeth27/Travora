package com.traveldesk.backend.bulkbooking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "bulk_booking_passengers")
public class BulkBookingPassenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bulk_booking_id", nullable = false)
    @JsonIgnore
    private BulkBooking bulkBooking;

    @Column(nullable = false)
    private String name;

    private String employeeId;
    private String department;
    private String seatOrCoach;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BulkBooking getBulkBooking() {
        return bulkBooking;
    }

    public void setBulkBooking(BulkBooking bulkBooking) {
        this.bulkBooking = bulkBooking;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSeatOrCoach() {
        return seatOrCoach;
    }

    public void setSeatOrCoach(String seatOrCoach) {
        this.seatOrCoach = seatOrCoach;
    }
}
