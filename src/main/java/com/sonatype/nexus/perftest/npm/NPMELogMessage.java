
package com.sonatype.nexus.perftest.npm;

import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "time",
    "hostname",
    "pid",
    "level",
    "name",
    "message"
})
public class NPMELogMessage {

    @JsonProperty("time")
    private String time;
    @JsonProperty("hostname")
    private String hostname;
    @JsonProperty("pid")
    private Long pid;
    @JsonProperty("level")
    private String level;
    @JsonProperty("name")
    private String name;
    @JsonProperty("message")
    private String message;

    /**
     * No args constructor for use in serialization
     * 
     */
    public NPMELogMessage() {
    }

    /**
     * 
     * @param message
     * @param time
     * @param level
     * @param name
     * @param hostname
     * @param pid
     */
    public NPMELogMessage(String time, String hostname, Long pid, String level, String name, String message) {
        this.time = time;
        this.hostname = hostname;
        this.pid = pid;
        this.level = level;
        this.name = name;
        this.message = message;
    }

    /**
     * 
     * @return
     *     The time
     */
    @JsonProperty("time")
    public String getTime() {
        return time;
    }

    /**
     * 
     * @param time
     *     The time
     */
    @JsonProperty("time")
    public void setTime(String time) {
        this.time = time;
    }

    /**
     * 
     * @return
     *     The hostname
     */
    @JsonProperty("hostname")
    public String getHostname() {
        return hostname;
    }

    /**
     * 
     * @param hostname
     *     The hostname
     */
    @JsonProperty("hostname")
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * 
     * @return
     *     The pid
     */
    @JsonProperty("pid")
    public Long getPid() {
        return pid;
    }

    /**
     * 
     * @param pid
     *     The pid
     */
    @JsonProperty("pid")
    public void setPid(Long pid) {
        this.pid = pid;
    }

    /**
     * 
     * @return
     *     The level
     */
    @JsonProperty("level")
    public String getLevel() {
        return level;
    }

    /**
     * 
     * @param level
     *     The level
     */
    @JsonProperty("level")
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * 
     * @return
     *     The name
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * 
     * @param name
     *     The name
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     * @return
     *     The message
     */
    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    /**
     * 
     * @param message
     *     The message
     */
    @JsonProperty("message")
    public void setMessage(String message) {
        this.message = message;
    }

    

}
