package com.publiccms.entities.home;
// Generated 2016-11-12 18:33:49 by Hibernate Tools 4.3.1

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import static javax.persistence.GenerationType.IDENTITY;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sanluan.common.generator.annotation.GeneratorColumn;

/**
 * HomeGroup generated by hbm2java
 */
@Entity
@Table(name = "home_group")
public class HomeGroup implements java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @GeneratorColumn(title = "ID")
    private Long id;
    @GeneratorColumn(title = "站点", condition = true)
    @JsonIgnore
    private int siteId;
    @GeneratorColumn(title = "用户", condition = true)
    private long userId;
    @GeneratorColumn(title = "名称")
    private String name;
    @GeneratorColumn(title = "描述")
    private String description;
    @GeneratorColumn(title = "用户数", order = true)
    private int users;
    @GeneratorColumn(title = "创建日期", order = true)
    private Date createDate;

    public HomeGroup() {
    }

    public HomeGroup(int siteId, long userId, String name, int users, Date createDate) {
        this.siteId = siteId;
        this.userId = userId;
        this.name = name;
        this.users = users;
        this.createDate = createDate;
    }

    public HomeGroup(int siteId, long userId, String name, String description, int users, Date createDate) {
        this.siteId = siteId;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.users = users;
        this.createDate = createDate;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)

    @Column(name = "id", unique = true, nullable = false)
    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "site_id", nullable = false)
    public int getSiteId() {
        return this.siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    @Column(name = "user_id", nullable = false)
    public long getUserId() {
        return this.userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    @Column(name = "name", nullable = false)
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "description", length = 300)
    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Column(name = "users", nullable = false)
    public int getUsers() {
        return this.users;
    }

    public void setUsers(int users) {
        this.users = users;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_date", nullable = false, length = 19)
    public Date getCreateDate() {
        return this.createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

}
