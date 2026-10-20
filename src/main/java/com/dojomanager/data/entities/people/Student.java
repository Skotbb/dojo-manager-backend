package com.dojomanager.data.entities.people;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.rank.StudentRank;
import com.dojomanager.data.entities.schedule.WeeklySchedule;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Entity
public class Student extends Person{
    
    @Getter @Setter
    private boolean active = true;

    @Getter @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dojo_id", nullable = false, updatable = false)
    private Dojo dojo;

    @Getter @Setter
    @OneToOne
    @JoinColumn(name = "rank_id")
    private StudentRank rank;

    @Getter
    @ManyToMany
    @JoinTable(
        name = "student_schedule_enrollment",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "schedule_id")
    )
    private Set<WeeklySchedule> enrolledSchedules = new HashSet<>();

    public void setEnrolledSchedules(Set<WeeklySchedule> schedules) {
        this.enrolledSchedules = schedules != null ? schedules : new HashSet<>();
    }

    public void enrollInSchedule(WeeklySchedule schedule) {
        if (schedule != null) {
            this.enrolledSchedules.add(schedule);
        }
    }

    public void unenrollFromSchedule(WeeklySchedule schedule) {
        if (schedule != null) {
            this.enrolledSchedules.remove(schedule);
        }
    }

    public Student(Dojo dojo, StudentRank rank) {
        this.dojo = dojo;
        this.rank = rank;
    }
}
