package com.dojomanager.data.dto.schedule;

import java.util.List;

import com.dojomanager.data.entities.people.Student;
import com.dojomanager.data.entities.schedule.WeeklySchedule;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

public class ScheduleSessionDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("dojoId")
    private Long dojoId;

    @NotBlank(message = "dayOfWeek is required")
    @JsonProperty("dayOfWeek")
    private String dayOfWeek;

    @NotBlank(message = "startTime is required")
    @JsonProperty("startTime")
    private String startTime;

    @NotBlank(message = "endTime is required")
    @JsonProperty("endTime")
    private String endTime;

    @NotBlank(message = "title is required")
    @JsonProperty("title")
    private String title;

    @JsonProperty("instructorId")
    private Long instructorId;

    @JsonProperty("isTestEvent")
    private Boolean isTestEvent = false;

    @JsonProperty("enrolledStudentIds")
    private List<Long> enrolledStudentIds;

    @JsonProperty("enrolledCount")
    private Integer enrolledCount;

    public ScheduleSessionDTO() {
    }

    public ScheduleSessionDTO(Long id, Long dojoId, String dayOfWeek, String startTime, String endTime, String title, Long instructorId) {
        this.id = id;
        this.dojoId = dojoId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.title = title;
        this.instructorId = instructorId;
    }

    public ScheduleSessionDTO(String dayOfWeek, String startTime, String endTime, String title, Long instructorId) {
        this(null, null, dayOfWeek, startTime, endTime, title, instructorId);
    }

    public static ScheduleSessionDTO from(WeeklySchedule entity) {
        if (entity == null) {
            return null;
        }
        ScheduleSessionDTO dto = new ScheduleSessionDTO(
                entity.getId(),
                entity.getDojo() != null ? entity.getDojo().getId() : null,
                entity.getDayOfWeek() != null ? entity.getDayOfWeek().name() : null,
                entity.getStartTime() != null ? entity.getStartTime().toString() : null,
                entity.getEndTime() != null ? entity.getEndTime().toString() : null,
                entity.getTitle(),
                entity.getInstructorId()
        );
        dto.setIsTestEvent(entity.isTestEvent());
        if (entity.getEnrolledStudents() != null) {
            dto.setEnrolledStudentIds(entity.getEnrolledStudents().stream().map(Student::getId).toList());
            dto.setEnrolledCount(entity.getEnrolledStudents().size());
        } else {
            dto.setEnrolledStudentIds(List.of());
            dto.setEnrolledCount(0);
        }
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDojoId() { return dojoId; }
    public void setDojoId(Long dojoId) { this.dojoId = dojoId; }
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
    public Boolean getIsTestEvent() { return isTestEvent != null && isTestEvent; }
    public void setIsTestEvent(Boolean isTestEvent) { this.isTestEvent = isTestEvent != null ? isTestEvent : false; }
    public List<Long> getEnrolledStudentIds() { return enrolledStudentIds; }
    public void setEnrolledStudentIds(List<Long> enrolledStudentIds) { this.enrolledStudentIds = enrolledStudentIds; }
    public Integer getEnrolledCount() { return enrolledCount; }
    public void setEnrolledCount(Integer enrolledCount) { this.enrolledCount = enrolledCount; }
}

