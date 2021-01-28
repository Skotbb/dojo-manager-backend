package com.dojomanager.data.entities.schedule;

import java.time.DayOfWeek;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import com.dojomanager.data.entities.AbstractEntity;
import com.dojomanager.data.entities.dojo.Dojo;

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


    public WeeklySchedule(DayOfWeek dayOfWeek, int classesPerDay, Dojo dojo) {
        this.dayOfWeek = dayOfWeek;
        this.classesPerDay = classesPerDay;
        this.dojo = dojo;
    }
}
