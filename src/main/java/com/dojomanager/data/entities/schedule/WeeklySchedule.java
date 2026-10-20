package com.dojomanager.data.entities.schedule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import com.dojomanager.data.entities.AbstractEntity;
import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.people.Student;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Entity
public class WeeklySchedule extends AbstractEntity {
    
    @Getter @Setter
    private DayOfWeek dayOfWeek;

    @Getter @Setter
    private int classesPerDay;

    @Getter @Setter
    @ManyToOne
    private Dojo dojo;

    @Getter @Setter
    private LocalTime startTime;

    @Getter @Setter
    private LocalTime endTime;

    @Getter @Setter
    private String title;

    @Getter @Setter
    private Long instructorId;

    private boolean isTestEvent = false;

    public boolean isTestEvent() {
        return isTestEvent;
    }

    public void setIsTestEvent(boolean isTestEvent) {
        this.isTestEvent = isTestEvent;
    }

    @Getter
    @ManyToMany(mappedBy = "enrolledSchedules")
    @JsonIgnore
    private Set<Student> enrolledStudents = new HashSet<>();

    public WeeklySchedule(DayOfWeek dayOfWeek, int classesPerDay, Dojo dojo) {
        this.dayOfWeek = dayOfWeek;
        this.classesPerDay = classesPerDay;
        this.dojo = dojo;
    }
}
