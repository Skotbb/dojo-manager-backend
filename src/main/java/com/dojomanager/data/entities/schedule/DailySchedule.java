package com.dojomanager.data.entities.schedule;

import java.time.LocalTime;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import com.dojomanager.data.entities.AbstractEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Entity
public class DailySchedule extends AbstractEntity{
    
    @Getter @Setter
    private double hoursPerClass;

    @Getter @Setter
    private LocalTime startTime;

    @Getter @Setter
    @ManyToOne
    private WeeklySchedule weeklySchedule;


    public DailySchedule(double hoursPerClass, LocalTime startTime, WeeklySchedule weeklySchedule) {
        this.hoursPerClass = hoursPerClass;
        this.startTime = startTime;
        this.weeklySchedule = weeklySchedule;
    }

}
