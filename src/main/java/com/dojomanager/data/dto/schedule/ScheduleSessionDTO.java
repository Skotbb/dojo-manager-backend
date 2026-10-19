package com.dojomanager.data.dto.schedule;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScheduleSessionDTO {
    @JsonProperty("dayOfWeek")
    private String dayOfWeek;

    @JsonProperty("startTime")
    private String startTime;

    @JsonProperty("endTime")
    private String endTime;

    @JsonProperty("title")
    private String title;

    @JsonProperty("instructorId")
    private Long instructorId;

    public ScheduleSessionDTO() {
    }

    public ScheduleSessionDTO(String dayOfWeek, String startTime, String endTime, String title, Long instructorId) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.title = title;
        this.instructorId = instructorId;
    }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getInstructorId() { return instructorId; }
    public void setInstructorId(Long instructorId) { this.instructorId = instructorId; }
}
